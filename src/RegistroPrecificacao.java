import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegistroPrecificacao {

    public static class Entrada {
        private final ResultadoPrecificacao resultado;
        private final String parecer;
        private final LocalDateTime dataHora;

        public Entrada(ResultadoPrecificacao resultado, String parecer) {
            this.resultado = resultado;
            this.parecer = parecer;
            this.dataHora = LocalDateTime.now();
        }

        public ResultadoPrecificacao getResultado() {
            return resultado;
        }

        public String getParecer() {
            return parecer;
        }

        public LocalDateTime getDataHora() {
            return dataHora;
        }
    }

    private final List<Entrada> entradas = new ArrayList<Entrada>();

    public Entrada registrar(ResultadoPrecificacao resultado, String parecer) {
        Entrada entrada = new Entrada(resultado, parecer);
        entradas.add(entrada);
        return entrada;
    }

    public List<Entrada> getEntradas() {
        return Collections.unmodifiableList(entradas);
    }

    public int tamanho() {
        return entradas.size();
    }

    /** Último cenário de demanda simulado para o lote, ou null se nunca foi simulado. */
    public HistoricoDemanda ultimaDemandaDoLote(String codigoLote) {
        for (int i = entradas.size() - 1; i >= 0; i--) {
            ResultadoPrecificacao r = entradas.get(i).getResultado();
            if (r.getCodigoLote().equalsIgnoreCase(codigoLote)) {
                return r.getDemanda();
            }
        }
        return null;
    }
}