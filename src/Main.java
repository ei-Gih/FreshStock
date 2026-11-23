import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner ENTRADA = new Scanner(System.in);
    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private static final List<LoteProduto> lotes = new ArrayList<LoteProduto>();
    private static final MotorPrecificacao motor = new MotorPrecificacao();
    private static final ConsultorEstoqueIA consultor = new ConsultorEstoqueIA();
    private static final ClienteIA clienteIA = new ClienteIA(consultor);
    private static final RegistroPrecificacao registro = new RegistroPrecificacao();
    private static int contadorLote = 0;

    public static void main(String[] args) {
        if (args.length == 0 || !"--terminal".equals(args[0])) {
            JanelaFreshStock.main(args);
            return;
        }
        carregarDadosIniciais();
        exibirCabecalho();

        boolean executando = true;
        while (executando) {
            exibirMenu();
            int opcao = lerInteiro("Escolha uma opção: ", 0, 5);
            switch (opcao) {
                case 1:
                    cadastrarLote();
                    break;
                case 2:
                    simularCenario();
                    break;
                case 3:
                    listarPrecos();
                    break;
                case 4:
                    listarLotes();
                    break;
                case 5:
                    diagnosticoTodos();
                    break;
                case 0:
                    executando = false;
                    break;
                default:
                    break;
            }
        }
        System.out.println("Até logo! Produtos sempre frescos.");
    }

    // ------------------------------------------------------------------ telas

    private static void exibirCabecalho() {
        System.out.println(linha());
        System.out.println("FreshStock (DynamicFresh AI)");
        System.out.println("Dados inteligentes, produtos sempre frescos");
        System.out.println(linha());
        if (clienteIA.isConfigurada()) {
            System.out.println("IA conectada, com análise local em caso de indisponibilidade.");
        } else {
            System.out.println("IA: local (IA_API_KEY não definida).");
        }
        System.out.println("Carregados " + lotes.size() + " lotes e " + registro.tamanho() + " simulações iniciais.");
    }

    private static void exibirMenu() {
        System.out.println();
        System.out.println(linha());
        System.out.println("1 - Cadastrar novo lote de hortifruti");
        System.out.println("2 - Simular cenário de demanda e validade");
        System.out.println("3 - Listar preços calculados");
        System.out.println("4 - Listar lotes cadastrados");
        System.out.println("5 - Executar diagnóstico de IA em todos os lotes");
        System.out.println("0 - Sair");
        System.out.println(linha());
    }

    // ----------------------------------------------------------------- opções

    private static void cadastrarLote() {
        System.out.println();
        System.out.println("== Novo lote ==");
        String nome = lerTexto("Nome do produto: ");
        boolean climaterico = lerSimNao("É climatérico (ex.: banana, mamão)? (s/n): ");
        LocalDate dataEntrada = lerData("Data de entrada (dd/MM/aaaa, Enter = hoje): ");
        int dias = lerInteiro("Dias de validade restantes (0 a 90): ", 0, 90);
        int estoque = lerInteiro("Quantidade em estoque (1 a 100000): ", 1, 100000);
        double custo = lerDecimal("Preço de custo (R$): ", 0.01);
        double base = lerDecimal("Preço base de venda (R$, mínimo = custo): ", custo);

        contadorLote++;
        String codigo = gerarCodigo(contadorLote);
        try {
            LoteProduto lote = new LoteProduto(codigo, nome, climaterico, dataEntrada, dias, estoque, custo, base);
            lotes.add(lote);
            System.out.println("Lote " + codigo + " cadastrado com sucesso.");
        } catch (IllegalArgumentException e) {
            contadorLote--;
            System.out.println("Não foi possível cadastrar: " + e.getMessage());
        }
    }

    private static void simularCenario() {
        System.out.println();
        System.out.println("== Simulação de cenário ==");
        LoteProduto lote = selecionarLote();
        if (lote == null) {
            return;
        }

        int dia = lerInteiro("Dia da semana (1=Seg, 2=Ter, 3=Qua, 4=Qui, 5=Sex, 6=Sáb, 7=Dom): ", 1, 7);
        int hora = lerInteiro("Hora (0 a 23): ", 0, 23);
        double vendas = lerDecimal("Vendas por hora históricas (un/h): ", 0.0);
        double meta = lerDecimal("Meta de vendas por hora (un/h): ", 0.01);
        int dias = lerInteiroOuPadrao("Dias de validade para simular (Enter = " + lote.getDiasValidadeRestantes()
                + "): ", 0, 90, lote.getDiasValidadeRestantes());

        HistoricoDemanda demanda = new HistoricoDemanda(DayOfWeek.of(dia), hora, vendas, meta);
        LoteProduto loteSimulado = (dias == lote.getDiasValidadeRestantes()) ? lote : lote.comDiasValidade(dias);

        System.out.println("Consultando IA...");
        RegistroPrecificacao.Entrada entrada = processar(loteSimulado, demanda, true);
        imprimirEntrada(entrada);
    }

    private static void listarPrecos() {
        System.out.println();
        System.out.println("== Preços calculados (" + registro.tamanho() + ") ==");
        if (registro.tamanho() == 0) {
            System.out.println("Nenhuma simulação registrada ainda.");
            return;
        }
        System.out.printf("%-3s %-11s %-7s %-20s %-21s %10s %10s %8s%n",
                "#", "Data/Hora", "Lote", "Produto", "Estratégia", "Base", "Ajustado", "Var.");
        int indice = 1;
        for (RegistroPrecificacao.Entrada e : registro.getEntradas()) {
            ResultadoPrecificacao r = e.getResultado();
            System.out.printf("%-3d %-11s %-7s %-20s %-21s %10s %10s %8s%n",
                    indice,
                    e.getDataHora().format(FORMATO_DATA_HORA),
                    r.getCodigoLote(),
                    truncar(r.getNomeProduto(), 20),
                    r.getNomeEstrategia(),
                    UtilNumerico.moeda(r.getPrecoBase()),
                    UtilNumerico.moeda(r.getPrecoFinal()),
                    UtilNumerico.percentual(r.getVariacao()));
            System.out.println("    " + e.getParecer());
            indice++;
        }
    }

    private static void listarLotes() {
        System.out.println();
        System.out.println("== Lotes cadastrados (" + lotes.size() + ") ==");
        if (lotes.isEmpty()) {
            System.out.println("Nenhum lote cadastrado.");
            return;
        }
        System.out.printf("%-7s %-20s %-6s %-11s %8s %8s %10s %10s %7s%n",
                "Lote", "Produto", "Clim.", "Entrada", "Validade", "Estoque", "Custo", "Base", "Fator");
        for (LoteProduto l : lotes) {
            System.out.printf("%-7s %-20s %-6s %-11s %8s %8d %10s %10s %7s%n",
                    l.getCodigoLote(),
                    truncar(l.getNomeProduto(), 20),
                    l.isClimaterico() ? "sim" : "não",
                    l.getDataEntrada().format(FORMATO_DATA),
                    l.getDiasValidadeRestantes() + " d",
                    l.getQuantidadeEstoque(),
                    UtilNumerico.moeda(l.getPrecoCusto()),
                    UtilNumerico.moeda(l.getPrecoBase()),
                    UtilNumerico.numero(l.calcularFatorValidade(), 2));
        }
    }

    private static void diagnosticoTodos() {
        System.out.println();
        System.out.println("== Diagnóstico de IA em todos os lotes ==");
        if (lotes.isEmpty()) {
            System.out.println("Nenhum lote cadastrado.");
            return;
        }
        System.out.println("Usa o último cenário simulado de cada lote (ou o horário atual com demanda padrão).");
        System.out.println("Consultando IA...");
        for (LoteProduto lote : lotes) {
            HistoricoDemanda demanda = registro.ultimaDemandaDoLote(lote.getCodigoLote());
            if (demanda == null) {
                LocalDateTime agora = LocalDateTime.now();
                demanda = new HistoricoDemanda(agora.getDayOfWeek(), agora.getHour(), 8.0, 10.0);
            }
            RegistroPrecificacao.Entrada entrada = processar(lote, demanda, true);
            ResultadoPrecificacao r = entrada.getResultado();
            double risco = consultor.calcularRiscoPerda(lote, demanda);

            System.out.println(linha());
            System.out.println(lote.getCodigoLote() + " | " + lote.getNomeProduto()
                    + " | Risco: " + consultor.classificarRisco(risco)
                    + " (" + UtilNumerico.numero(risco, 0) + "/100)"
                    + " | Preço sugerido: " + UtilNumerico.moeda(r.getPrecoFinal())
                    + " (" + UtilNumerico.percentual(r.getVariacao()) + ")");
            System.out.println(entrada.getParecer());
        }
        System.out.println(linha());
    }

    // ----------------------------------------------------------------- núcleo

    private static RegistroPrecificacao.Entrada processar(LoteProduto lote, HistoricoDemanda demanda,
                                                          boolean usarIA) {
        ResultadoPrecificacao resultado = motor.calcular(lote, demanda);
        String parecer = usarIA
                ? clienteIA.gerarParecer(lote, demanda, resultado)
                : consultor.gerarParecer(lote, demanda, resultado);
        return registro.registrar(resultado, parecer);
    }

    private static void imprimirEntrada(RegistroPrecificacao.Entrada entrada) {
        ResultadoPrecificacao r = entrada.getResultado();
        HistoricoDemanda d = r.getDemanda();

        System.out.println(linha());
        System.out.println("Lote " + r.getCodigoLote() + " | " + r.getNomeProduto()
                + (r.isClimaterico() ? " (climatérico)" : ""));
        System.out.println("Validade: " + r.getDiasValidadeRestantes() + " dia(s) | Estoque: "
                + r.getQuantidadeEstoque() + " un");
        System.out.println("Cenário: " + d.getDiaSemanaPt() + ", " + d.getHora() + "h | Vendas/h: "
                + UtilNumerico.numero(d.getVendasPorHoraHistoricas(), 1)
                + " (meta " + UtilNumerico.numero(d.getMetaVendasHora(), 1) + ")");
        System.out.println("Índice de demanda: " + UtilNumerico.numero(r.getIndiceDemanda(), 2)
                + " | Fator de validade: " + UtilNumerico.numero(r.getFatorValidade(), 2));
        System.out.println("Estratégia: " + r.getNomeEstrategia());
        System.out.println("Preço base: " + UtilNumerico.moeda(r.getPrecoBase())
                + " -> Preço ajustado: " + UtilNumerico.moeda(r.getPrecoFinal())
                + " (" + UtilNumerico.percentual(r.getVariacao()) + ")");
        if (r.isPisoAplicado()) {
            System.out.println("Obs.: preço levado ao piso de segurança/liquidação.");
        }
        if (r.isTetoAplicado()) {
            System.out.println("Obs.: preço limitado ao teto sobre o preço base.");
        }
        System.out.println("Parecer: " + entrada.getParecer());
        System.out.println(linha());
    }

    private static void carregarDadosIniciais() {
        LocalDate hoje = LocalDate.now();

        adicionarLoteInicial("Banana prata", true, hoje.minusDays(2), 3, 120, 3.20, 6.50,
                DayOfWeek.SATURDAY, 10, 7.0, 9.0);
        adicionarLoteInicial("Maçã Fuji", false, hoje.minusDays(3), 12, 200, 4.50, 8.90,
                DayOfWeek.TUESDAY, 15, 9.0, 8.0);
        adicionarLoteInicial("Mamão formosa", true, hoje.minusDays(4), 2, 40, 3.80, 7.50,
                DayOfWeek.WEDNESDAY, 11, 3.0, 6.0);
        adicionarLoteInicial("Alface crespa", false, hoje.minusDays(1), 2, 60, 1.20, 3.50,
                DayOfWeek.THURSDAY, 18, 5.0, 8.0);
        adicionarLoteInicial("Tomate italiano", false, hoje.minusDays(1), 5, 90, 4.00, 7.90,
                DayOfWeek.FRIDAY, 17, 14.0, 10.0);
    }

    private static void adicionarLoteInicial(String nome, boolean climaterico, LocalDate entrada, int dias,
                                             int estoque, double custo, double base,
                                             DayOfWeek dia, int hora, double vendas, double meta) {
        contadorLote++;
        LoteProduto lote = new LoteProduto(gerarCodigo(contadorLote), nome, climaterico, entrada,
                dias, estoque, custo, base);
        lotes.add(lote);
        processar(lote, new HistoricoDemanda(dia, hora, vendas, meta), false);
    }

    private static String gerarCodigo(int numero) {
        return String.format("LT-%03d", numero);
    }

    private static LoteProduto selecionarLote() {
        if (lotes.isEmpty()) {
            System.out.println("Nenhum lote cadastrado. Cadastre um lote primeiro.");
            return null;
        }
        System.out.println("Lotes disponíveis:");
        for (LoteProduto l : lotes) {
            System.out.println("  " + l.getCodigoLote() + " - " + l.getNomeProduto()
                    + " (" + l.getDiasValidadeRestantes() + " d, " + l.getQuantidadeEstoque() + " un)");
        }
        while (true) {
            String codigo = lerLinha("Código do lote (Enter para cancelar): ");
            if (codigo.isEmpty()) {
                return null;
            }
            for (LoteProduto l : lotes) {
                if (l.getCodigoLote().equalsIgnoreCase(codigo)) {
                    return l;
                }
            }
            System.out.println("Lote não encontrado. Tente de novo.");
        }
    }

    // ------------------------------------------------------- entrada segura

    private static String lerLinha(String mensagem) {
        System.out.print(mensagem);
        if (!ENTRADA.hasNextLine()) {
            System.out.println();
            System.out.println("Entrada encerrada. Saindo...");
            System.exit(0);
        }
        return ENTRADA.nextLine().trim();
    }

    private static String lerTexto(String mensagem) {
        while (true) {
            String texto = lerLinha(mensagem);
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("Campo obrigatório.");
        }
    }

    private static int lerInteiro(String mensagem, int minimo, int maximo) {
        while (true) {
            String texto = lerLinha(mensagem);
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.println("Digite um número entre " + minimo + " e " + maximo + ".");
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Digite um número inteiro.");
            }
        }
    }

    private static int lerInteiroOuPadrao(String mensagem, int minimo, int maximo, int padrao) {
        while (true) {
            String texto = lerLinha(mensagem);
            if (texto.isEmpty()) {
                return padrao;
            }
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.println("Digite um número entre " + minimo + " e " + maximo + ".");
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Digite um número inteiro ou Enter.");
            }
        }
    }

    private static double lerDecimal(String mensagem, double minimo) {
        while (true) {
            String texto = lerLinha(mensagem);
            try {
                double valor = UtilNumerico.lerDecimal(texto);
                if (valor >= minimo) {
                    return valor;
                }
                System.out.println("O valor deve ser no mínimo " + UtilNumerico.numero(minimo, 2) + ".");
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Use vírgula ou ponto (ex.: 12,50).");
            }
        }
    }

    private static boolean lerSimNao(String mensagem) {
        while (true) {
            String texto = lerLinha(mensagem).toLowerCase();
            if (texto.equals("s") || texto.equals("sim")) {
                return true;
            }
            if (texto.equals("n") || texto.equals("nao") || texto.equals("não")) {
                return false;
            }
            System.out.println("Responda com s ou n.");
        }
    }

    private static LocalDate lerData(String mensagem) {
        while (true) {
            String texto = lerLinha(mensagem);
            if (texto.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(texto, FORMATO_DATA);
            } catch (DateTimeParseException e) {
                System.out.println("Data inválida. Use dd/MM/aaaa (ex.: 20/11/2026).");
            }
        }
    }

    // --------------------------------------------------------------- utilidades

    private static String linha() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 70; i++) {
            sb.append('-');
        }
        return sb.toString();
    }

    private static String truncar(String texto, int limite) {
        if (texto.length() <= limite) {
            return texto;
        }
        return texto.substring(0, limite - 1) + ".";
    }
}