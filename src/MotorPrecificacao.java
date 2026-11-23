public class MotorPrecificacao {

    private static final double MARGEM_MINIMA = 1.05;   // piso normal: custo + 5%
    private static final double PISO_LIQUIDACAO = 0.70; // piso de liquidação: 70% do custo
    private static final double TETO_SOBRE_BASE = 1.30; // nunca passar de 130% do preço base

    private final EstrategiaPrecificacao margemOtima = new PrecificacaoMargemOptima();
    private final EstrategiaPrecificacao urgenciaValidade = new PrecificacaoUrgenciaValidade();
    private final EstrategiaPrecificacao altaRotatividade = new PrecificacaoAltaRotatividade();

    public EstrategiaPrecificacao escolherEstrategia(LoteProduto lote, HistoricoDemanda demanda) {
        double fator = lote.calcularFatorValidade();
        double indice = demanda.calcularIndiceDemanda();

        if (lote.precisaLiquidacaoUrgente() || fator < 0.35) {
            return urgenciaValidade;
        }
        boolean estoqueAlto = lote.getQuantidadeEstoque() >= 100;
        if (estoqueAlto && indice < 0.8 && fator < 0.6) {
            return urgenciaValidade;
        }
        if (indice >= 1.2 && fator >= 0.5) {
            return altaRotatividade;
        }
        return margemOtima;
    }

    public ResultadoPrecificacao calcular(LoteProduto lote, HistoricoDemanda demanda) {
        if (lote.getDiasValidadeRestantes() < 0)
            throw new IllegalArgumentException("Produto vencido: retirar da venda.");
        if (lote.getQuantidadeEstoque() == 0)
            throw new IllegalArgumentException("Estoque esgotado: repor antes de precificar.");
        EstrategiaPrecificacao estrategia = escolherEstrategia(lote, demanda);
        double preco = estrategia.calcularPrecoAjustado(lote, demanda);

        // Temporada representa demanda informada pelo gestor, não um calendário agrícola.
        if (lote.getDiasValidadeRestantes() > 3) {
            if ("Alta demanda".equals(lote.getTemporada())) preco *= 1.10;
            if ("Baixa demanda".equals(lote.getTemporada())) preco *= 0.90;
        }
        boolean liquidacao = (estrategia == urgenciaValidade);
        double piso = lote.getPrecoCusto() * (liquidacao ? PISO_LIQUIDACAO : MARGEM_MINIMA);
        double teto = lote.getPrecoBase() * TETO_SOBRE_BASE;

        boolean pisoAplicado = false;
        boolean tetoAplicado = false;
        if (preco < piso) {
            preco = piso;
            pisoAplicado = true;
        }
        if (preco > teto) {
            preco = teto;
            tetoAplicado = true;
        }
        preco = UtilNumerico.arredondar(preco);

        return new ResultadoPrecificacao(lote, demanda, estrategia.getNomeEstrategia(),
                preco, pisoAplicado, tetoAplicado);
    }
}