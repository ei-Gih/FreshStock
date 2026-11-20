import java.time.LocalDateTime;

public class ResultadoPrecificacao {

    private final String codigoLote;
    private final String nomeProduto;
    private final boolean climaterico;
    private final int diasValidadeRestantes;
    private final int quantidadeEstoque;
    private final double precoCusto;
    private final double precoBase;
    private final double precoFinal;
    private final String nomeEstrategia;
    private final double indiceDemanda;
    private final double fatorValidade;
    private final boolean pisoAplicado;
    private final boolean tetoAplicado;
    private final HistoricoDemanda demanda;
    private final LocalDateTime dataHora;

    public ResultadoPrecificacao(LoteProduto lote, HistoricoDemanda demanda, String nomeEstrategia,
                                 double precoFinal, boolean pisoAplicado, boolean tetoAplicado) {
        this.codigoLote = lote.getCodigoLote();
        this.nomeProduto = lote.getNomeProduto();
        this.climaterico = lote.isClimaterico();
        this.diasValidadeRestantes = lote.getDiasValidadeRestantes();
        this.quantidadeEstoque = lote.getQuantidadeEstoque();
        this.precoCusto = lote.getPrecoCusto();
        this.precoBase = lote.getPrecoBase();
        this.precoFinal = precoFinal;
        this.nomeEstrategia = nomeEstrategia;
        this.indiceDemanda = demanda.calcularIndiceDemanda();
        this.fatorValidade = lote.calcularFatorValidade();
        this.pisoAplicado = pisoAplicado;
        this.tetoAplicado = tetoAplicado;
        this.demanda = demanda;
        this.dataHora = LocalDateTime.now();
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public boolean isClimaterico() {
        return climaterico;
    }

    public int getDiasValidadeRestantes() {
        return diasValidadeRestantes;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public double getPrecoCusto() {
        return precoCusto;
    }

    public double getPrecoBase() {
        return precoBase;
    }

    public double getPrecoFinal() {
        return precoFinal;
    }

    public String getNomeEstrategia() {
        return nomeEstrategia;
    }

    public double getIndiceDemanda() {
        return indiceDemanda;
    }

    public double getFatorValidade() {
        return fatorValidade;
    }

    public boolean isPisoAplicado() {
        return pisoAplicado;
    }

    public boolean isTetoAplicado() {
        return tetoAplicado;
    }

    public HistoricoDemanda getDemanda() {
        return demanda;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    /** Variação do preço final em relação ao preço base (-0.25 = -25%). */
    public double getVariacao() {
        return (precoFinal / precoBase) - 1.0;
    }
}