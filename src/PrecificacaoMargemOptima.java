public class PrecificacaoMargemOptima implements EstrategiaPrecificacao {

    @Override
    public double calcularPrecoAjustado(LoteProduto lote, HistoricoDemanda demanda) {
        double indice = demanda.calcularIndiceDemanda();
        // Ajuste fino: -5% (demanda fraca) até +8% (demanda forte).
        double ajuste = UtilNumerico.limitar((indice - 1.0) * 0.10, -0.05, 0.08);
        return lote.getPrecoBase() * (1.0 + ajuste);
    }

    @Override
    public String getNomeEstrategia() {
        return "Margem Ótima";
    }
}