import java.time.DayOfWeek;

public class HistoricoDemanda {

    private final DayOfWeek diaSemana;
    private final int hora;
    private final double vendasPorHoraHistoricas;
    private final double metaVendasHora;

    public HistoricoDemanda(DayOfWeek diaSemana, int hora,
                            double vendasPorHoraHistoricas, double metaVendasHora) {
        if (diaSemana == null) {
            throw new IllegalArgumentException("Dia da semana é obrigatório.");
        }
        if (hora < 0 || hora > 23) {
            throw new IllegalArgumentException("Hora deve estar entre 0 e 23.");
        }
        if (!Double.isFinite(vendasPorHoraHistoricas) || vendasPorHoraHistoricas < 0) {
            throw new IllegalArgumentException("Vendas por hora não pode ser negativa.");
        }
        if (!Double.isFinite(metaVendasHora) || metaVendasHora <= 0) {
            throw new IllegalArgumentException("Meta de vendas por hora deve ser maior que zero.");
        }
        this.diaSemana = diaSemana;
        this.hora = hora;
        this.vendasPorHoraHistoricas = vendasPorHoraHistoricas;
        this.metaVendasHora = metaVendasHora;
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public int getHora() {
        return hora;
    }

    public double getVendasPorHoraHistoricas() {
        return vendasPorHoraHistoricas;
    }

    public double getMetaVendasHora() {
        return metaVendasHora;
    }

    /** Vendas históricas divididas pela meta (1.0 = no ritmo da meta). Limitado a 0..3. */
    public double calcularIndiceDemanda() {
        return UtilNumerico.limitar(vendasPorHoraHistoricas / metaVendasHora, 0.0, 3.0);
    }

    public String getDiaSemanaPt() {
        String nome;
        switch (diaSemana) {
            case MONDAY:
                nome = "Segunda";
                break;
            case TUESDAY:
                nome = "Terça";
                break;
            case WEDNESDAY:
                nome = "Quarta";
                break;
            case THURSDAY:
                nome = "Quinta";
                break;
            case FRIDAY:
                nome = "Sexta";
                break;
            case SATURDAY:
                nome = "Sábado";
                break;
            case SUNDAY:
                nome = "Domingo";
                break;
            default:
                nome = diaSemana.toString();
                break;
        }
        return nome;
    }
}