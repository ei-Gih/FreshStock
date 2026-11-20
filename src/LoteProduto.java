import java.time.LocalDate;

public class LoteProduto {

    // Acima deste número de dias efetivos, o produto é considerado 100% fresco.
    private static final double HORIZONTE_FRESCOR_DIAS = 7.0;
    // Climatéricos amadurecem (e estragam) mais rápido: contam só 70% dos dias.
    private static final double REDUCAO_CLIMATERICO = 0.7;
    private static final double LIMITE_LIQUIDACAO_DIAS = 1.5;

    private final String codigoLote;
    private final String nomeProduto;
    private final boolean climaterico;
    private final LocalDate dataEntrada;
    private final int diasValidadeRestantes;
    private int quantidadeEstoque;
    private final double precoCusto;
    private final double precoBase;

    public LoteProduto(String codigoLote, String nomeProduto, boolean climaterico,
                       LocalDate dataEntrada, int diasValidadeRestantes,
                       int quantidadeEstoque, double precoCusto, double precoBase) {
        if (codigoLote == null || codigoLote.trim().isEmpty()) {
            throw new IllegalArgumentException("Código do lote é obrigatório.");
        }
        if (nomeProduto == null || nomeProduto.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do produto é obrigatório.");
        }
        if (diasValidadeRestantes < 0) {
            throw new IllegalArgumentException("Dias de validade não pode ser negativo.");
        }
        if (quantidadeEstoque < 0) {
            throw new IllegalArgumentException("Quantidade em estoque não pode ser negativa.");
        }
        if (precoCusto <= 0) {
            throw new IllegalArgumentException("Preço de custo deve ser maior que zero.");
        }
        if (precoBase < precoCusto) {
            throw new IllegalArgumentException("Preço base não pode ser menor que o preço de custo.");
        }
        this.codigoLote = codigoLote.trim();
        this.nomeProduto = nomeProduto.trim();
        this.climaterico = climaterico;
        this.dataEntrada = (dataEntrada == null) ? LocalDate.now() : dataEntrada;
        this.diasValidadeRestantes = diasValidadeRestantes;
        this.quantidadeEstoque = quantidadeEstoque;
        this.precoCusto = precoCusto;
        this.precoBase = precoBase;
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

    public LocalDate getDataEntrada() {
        return dataEntrada;
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

    /** Dias de validade "úteis": climatéricos perdem tempo biológico mais rápido. */
    public double getDiasValidadeEfetivos() {
        return climaterico ? diasValidadeRestantes * REDUCAO_CLIMATERICO : diasValidadeRestantes;
    }

    public void registrarVenda(int unidades) {
        if (unidades <= 0) {
            throw new IllegalArgumentException("A venda deve ter pelo menos 1 unidade.");
        }
        if (unidades > quantidadeEstoque) {
            throw new IllegalArgumentException("Estoque insuficiente: disponível " + quantidadeEstoque + " un.");
        }
        quantidadeEstoque -= unidades;
    }

    /** Retorna de 0.0 (vencendo) a 1.0 (totalmente fresco). */
    public double calcularFatorValidade() {
        return UtilNumerico.limitar(getDiasValidadeEfetivos() / HORIZONTE_FRESCOR_DIAS, 0.0, 1.0);
    }

    public boolean precisaLiquidacaoUrgente() {
        return getDiasValidadeEfetivos() <= LIMITE_LIQUIDACAO_DIAS;
    }

    /** Cópia do lote com outra validade, usada para simular cenários sem alterar o lote real. */
    public LoteProduto comDiasValidade(int novosDias) {
        LoteProduto copia = new LoteProduto(codigoLote, nomeProduto, climaterico, dataEntrada,
                novosDias, quantidadeEstoque, precoCusto, precoBase);
        return copia;
    }
}