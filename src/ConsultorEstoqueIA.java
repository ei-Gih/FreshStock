public class ConsultorEstoqueIA {

    private static final double HORAS_LOJA_POR_DIA = 12.0;

    /** Unidades que provavelmente não serão vendidas antes de vencer, no ritmo atual. */
    public double estimarPerdaUnidades(LoteProduto lote, HistoricoDemanda demanda) {
        double horasDisponiveis = Math.max(0.0, lote.getDiasValidadeEfetivos()) * HORAS_LOJA_POR_DIA;
        double vendaPrevista = demanda.getVendasPorHoraHistoricas() * horasDisponiveis;
        return Math.max(0.0, lote.getQuantidadeEstoque() - vendaPrevista);
    }

    /** Score de 0 a 100. */
    public double calcularRiscoPerda(LoteProduto lote, HistoricoDemanda demanda) {
        int estoque = lote.getQuantidadeEstoque();
        if (estoque <= 0) {
            return 0.0;
        }
        double perda = estimarPerdaUnidades(lote, demanda);
        double componentePerda = (perda / estoque) * 50.0;
        double componenteValidade = (1.0 - lote.calcularFatorValidade()) * 40.0;
        double componenteDemanda = UtilNumerico.limitar(1.0 - demanda.calcularIndiceDemanda(), 0.0, 1.0) * 10.0;

        double risco = componentePerda + componenteValidade + componenteDemanda;
        if (lote.precisaLiquidacaoUrgente()) {
            risco += 20.0;
        }
        return UtilNumerico.limitar(risco, 0.0, 100.0);
    }

    public String classificarRisco(double risco) {
        String nivel;
        if (risco < 25.0) {
            nivel = "BAIXO";
        } else if (risco < 50.0) {
            nivel = "MODERADO";
        } else if (risco < 75.0) {
            nivel = "ALTO";
        } else {
            nivel = "CRÍTICO";
        }
        return nivel;
    }

    /** Texto do parecer, sem prefixo de origem. */
    public String gerarTexto(LoteProduto lote, HistoricoDemanda demanda, ResultadoPrecificacao resultado) {
        double risco = calcularRiscoPerda(lote, demanda);
        double perda = estimarPerdaUnidades(lote, demanda);
        String nivel = classificarRisco(risco);

        StringBuilder sb = new StringBuilder();
        sb.append("Risco de perda ").append(nivel)
          .append(" (").append(UtilNumerico.numero(risco, 0)).append("/100)");
        if (perda >= 0.5) {
            sb.append(", perda estimada de ").append(UtilNumerico.numero(perda, 0)).append(" un");
        }
        sb.append(". ");

        if (lote.getQuantidadeEstoque() <= 0) {
            sb.append("Sem estoque: programe reposição conforme a demanda.");
            return sb.toString();
        }

        if (risco >= 75.0) {
            sb.append("Liquide agora a ").append(UtilNumerico.moeda(resultado.getPrecoFinal()))
              .append(", destaque na frente da loja e avalie doação ou uso em produtos processados.");
        } else if (risco >= 50.0) {
            sb.append("Aplique ").append(UtilNumerico.moeda(resultado.getPrecoFinal()))
              .append(", reposicione em ponto de destaque, reavalie em 2 horas e suspenda novos pedidos.");
        } else if (risco >= 25.0) {
            sb.append("Monitore a cada turno e não reponha acima da demanda.");
        } else {
            sb.append("Mantenha preço e ritmo de reposição.");
        }

        if (lote.isClimaterico()) {
            sb.append(" Climatérico: venda primeiro os mais maduros e afaste de itens sensíveis a etileno.");
        }

        double vendasHora = demanda.getVendasPorHoraHistoricas();
        if (vendasHora > 0 && demanda.calcularIndiceDemanda() >= 1.2) {
            double horasCobertura = lote.getQuantidadeEstoque() / vendasHora;
            if (horasCobertura < 3.0) {
                sb.append(" Estoque cobre ~").append(UtilNumerico.numero(horasCobertura, 1))
                  .append(" h no ritmo atual: programe reposição.");
            }
        }
        return sb.toString();
    }

    public String gerarParecer(LoteProduto lote, HistoricoDemanda demanda, ResultadoPrecificacao resultado) {
        return "[IA Local] " + gerarTexto(lote, demanda, resultado);
    }
}