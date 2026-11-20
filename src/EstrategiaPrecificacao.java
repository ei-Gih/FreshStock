public interface EstrategiaPrecificacao {

    double calcularPrecoAjustado(LoteProduto lote, HistoricoDemanda demanda);

    String getNomeEstrategia();
}