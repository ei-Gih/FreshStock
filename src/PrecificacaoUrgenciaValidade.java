public class PrecificacaoUrgenciaValidade implements EstrategiaPrecificacao {

    @Override
    public double calcularPrecoAjustado(LoteProduto lote, HistoricoDemanda demanda) {
        double fator = lote.calcularFatorValidade();
        double indice = demanda.calcularIndiceDemanda();

        // Quanto menos fresco, maior o desconto.
        double desconto = 0.05 + 0.45 * (1.0 - fator);
        if (lote.precisaLiquidacaoUrgente()) {
            desconto += 0.10;
        }
        // Demanda forte já ajuda a escoar; demanda fraca pede desconto maior.
        if (indice >= 1.2) {
            desconto *= 0.8;
        } else if (indice < 0.8) {
            desconto *= 1.15;
        }
        desconto = UtilNumerico.limitar(desconto, 0.05, 0.60);
        return lote.getPrecoBase() * (1.0 - desconto);
    }

    @Override
    public String getNomeEstrategia() {
        return "Urgência de Validade";
    }
}