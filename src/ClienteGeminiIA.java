import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ClienteGeminiIA {

    private static final String VAR_CHAVE = "GEMINI_API_KEY";
    private static final String VAR_MODELO = "GEMINI_MODEL";
    private static final String MODELO_PADRAO = "gemini-2.5-flash";
    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int TIMEOUT_CONEXAO_MS = 5000;
    private static final int TIMEOUT_LEITURA_MS = 20000;
    private static final int LIMITE_TEXTO = 600;

    private final ConsultorEstoqueIA consultorLocal;
    private final String apiKey;
    private final String modelo;

    public ClienteGeminiIA(ConsultorEstoqueIA consultorLocal) {
        this.consultorLocal = consultorLocal;
        String chave = System.getenv(VAR_CHAVE);
        this.apiKey = (chave == null || chave.trim().isEmpty()) ? null : chave.trim();

        String m = System.getenv(VAR_MODELO);
        if (m == null || !m.trim().matches("[A-Za-z0-9._-]+")) {
            m = MODELO_PADRAO;
        }
        this.modelo = m.trim();
    }

    public boolean isConfigurada() {
        return apiKey != null;
    }

    public String getModelo() {
        return modelo;
    }

    /** Tenta o Gemini; em qualquer falha, cai para o consultor local sem interromper o programa. */
    public String gerarParecer(LoteProduto lote, HistoricoDemanda demanda, ResultadoPrecificacao resultado) {
        if (!isConfigurada()) {
            return consultorLocal.gerarParecer(lote, demanda, resultado);
        }
        try {
            String prompt = montarPrompt(lote, demanda, resultado);
            String texto = consultarGemini(prompt);
            return "[IA Gemini] " + texto;
        } catch (Exception e) {
            String motivo = (e.getMessage() == null || e.getMessage().isEmpty())
                    ? e.getClass().getSimpleName() : e.getMessage();
            return "[IA Local | fallback: " + motivo + "] "
                    + consultorLocal.gerarTexto(lote, demanda, resultado);
        }
    }

    private String montarPrompt(LoteProduto lote, HistoricoDemanda demanda, ResultadoPrecificacao r) {
        double risco = consultorLocal.calcularRiscoPerda(lote, demanda);
        StringBuilder sb = new StringBuilder();
        sb.append("Você é consultor de estoque de hortifruti em supermercado. ");
        sb.append("Responda em português do Brasil, sem markdown, em no máximo 3 frases curtas: ");
        sb.append("um diagnóstico do risco de perda e uma ação prática de estoque.\n");
        sb.append("Produto: ").append(lote.getNomeProduto());
        sb.append(lote.isClimaterico() ? " (climatérico)" : " (não climatérico)").append("\n");
        sb.append("Dias de validade restantes: ").append(lote.getDiasValidadeRestantes()).append("\n");
        sb.append("Estoque: ").append(lote.getQuantidadeEstoque()).append(" un\n");
        sb.append("Cenário: ").append(demanda.getDiaSemanaPt()).append(", ")
          .append(demanda.getHora()).append("h\n");
        sb.append("Vendas por hora: ").append(UtilNumerico.numero(demanda.getVendasPorHoraHistoricas(), 1))
          .append(" (meta ").append(UtilNumerico.numero(demanda.getMetaVendasHora(), 1)).append(")\n");
        sb.append("Preço de custo: ").append(UtilNumerico.moeda(lote.getPrecoCusto())).append("\n");
        sb.append("Preço base: ").append(UtilNumerico.moeda(r.getPrecoBase())).append("\n");
        sb.append("Preço ajustado pelo motor: ").append(UtilNumerico.moeda(r.getPrecoFinal()))
          .append(" (estratégia: ").append(r.getNomeEstrategia()).append(")\n");
        sb.append("Risco calculado localmente: ").append(UtilNumerico.numero(risco, 0)).append("/100\n");
        return sb.toString();
    }

    private String consultarGemini(String prompt) throws IOException {
        String corpoJson = "{\"contents\":[{\"parts\":[{\"text\":\"" + escaparJson(prompt) + "\"}]}],"
                + "\"generationConfig\":{\"temperature\":0.3,\"maxOutputTokens\":1024}}";
        byte[] corpo = corpoJson.getBytes(StandardCharsets.UTF_8);

        HttpURLConnection con = null;
        try {
            URL url = new URL(URL_BASE + modelo + ":generateContent");
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(TIMEOUT_CONEXAO_MS);
            con.setReadTimeout(TIMEOUT_LEITURA_MS);
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("x-goog-api-key", apiKey);
            con.setDoOutput(true);
            con.setFixedLengthStreamingMode(corpo.length);

            try (OutputStream os = con.getOutputStream()) {
                os.write(corpo);
            }

            int codigo = con.getResponseCode();
            if (codigo != 200) {
                throw new IOException(descreverErroHttp(codigo));
            }
            String resposta = lerCorpo(con.getInputStream());
            return limparTexto(extrairTexto(resposta));
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private String descreverErroHttp(int codigo) {
        String descricao;
        switch (codigo) {
            case 429:
                descricao = "limite de uso da API atingido (HTTP 429)";
                break;
            case 400:
            case 401:
            case 403:
                descricao = "chave inválida ou sem permissão (HTTP " + codigo + ")";
                break;
            case 404:
                descricao = "modelo não encontrado (HTTP 404)";
                break;
            default:
                descricao = "erro HTTP " + codigo;
                break;
        }
        return descricao;
    }

    private String lerCorpo(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                sb.append(linha).append('\n');
            }
        }
        return sb.toString();
    }

    /** Extrai o primeiro campo "text" da resposta, sem depender de biblioteca JSON. */
    private String extrairTexto(String json) throws IOException {
        int chave = json.indexOf("\"text\"");
        if (chave < 0) {
            throw new IOException("resposta sem texto");
        }
        int doisPontos = json.indexOf(':', chave + 6);
        int inicio = (doisPontos < 0) ? -1 : json.indexOf('"', doisPontos + 1);
        if (inicio < 0) {
            throw new IOException("resposta em formato inesperado");
        }

        StringBuilder sb = new StringBuilder();
        int tamanho = json.length();
        for (int i = inicio + 1; i < tamanho; i++) {
            char c = json.charAt(i);
            if (c == '"') {
                String texto = sb.toString().trim();
                if (texto.isEmpty()) {
                    throw new IOException("resposta vazia");
                }
                return texto;
            }
            if (c == '\\' && i + 1 < tamanho) {
                char proximo = json.charAt(++i);
                switch (proximo) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'u':
                        if (i + 4 < tamanho) {
                            sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                        break;
                    default:
                        sb.append(proximo);
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        throw new IOException("resposta incompleta");
    }

    private String limparTexto(String texto) {
        String limpo = texto.replaceAll("\\s*\\n+\\s*", " ").replace("**", "").trim();
        if (limpo.length() > LIMITE_TEXTO) {
            limpo = limpo.substring(0, LIMITE_TEXTO - 3) + "...";
        }
        return limpo;
    }

    private String escaparJson(String texto) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}