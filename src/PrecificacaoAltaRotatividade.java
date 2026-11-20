public class PrecificacaoAltaRotatividade implements EstrategiaPrecificacao {

    private static final int ESTOQUE_BAIXO = 20;

    @Override
    public double calcularPrecoAjustado(LoteProduto lote, HistoricoDemanda demanda) {
        double indice = demanda.calcularIndiceDemanda();
        double fator = lote.calcularFatorValidade();

        double acrescimo = UtilNumerico.limitar((indice - 1.0) * 0.20, 0.0, 0.25);
        if (lote.getQuantidadeEstoque() <= ESTOQUE_BAIXO) {
            acrescimo += 0.05; // estoque curto + demanda alta
        }
        // Produto menos fresco sustenta menos acréscimo.
        acrescimo *= (0.5 + 0.5 * fator);
        return lote.getPrecoBase() * (1.0 + acrescimo);
    }

    @Override
    public String getNomeEstrategia() {
        return "Alta Rotatividade";
    }
}