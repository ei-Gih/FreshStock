import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class JanelaFreshStock extends JFrame {
    private final MotorPrecificacao motor;
    private final List<LoteProduto> lotes = new ArrayList<>();
    private final RegistroPrecificacao historico = new RegistroPrecificacao();
    private final ConsultorEstoqueIA consultor = new ConsultorEstoqueIA();
    private ClienteIA clienteIA = new ClienteIA(consultor);

    private BancoEstoque banco;
    private PainelGestao painelGestao;
    private DefaultTableModel modeloLotes;
    private DefaultTableModel modeloPrecos;
    private JComboBox<String> comboLotesSimulacao;
    private JTextArea areaResultadoSimulacao;
    private JTextArea areaDiagnosticoIA;

    public JanelaFreshStock() {
        super("FreshStock (DynamicFresh AI) - Dados inteligentes, produtos sempre frescos");
        Locale.setDefault(new Locale("pt", "BR"));

        this.motor = new MotorPrecificacao();
        try {
            banco = new BancoEstoque(BancoEstoque.caminhoPadrao());
            lotes.addAll(banco.listar());
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível abrir o banco local. Feche outra janela do FreshStock, se houver. Nenhum dado foi apagado.", ex);
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 820);
        setMinimumSize(new java.awt.Dimension(1050, 700));
        setLocationRelativeTo(null);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Lotes e Cadastro", criarAbaLotes());
        abas.addTab("Simular Cenario", criarAbaSimulacao());
        abas.addTab("Precos Calculados", criarAbaPrecos());
        abas.addTab("Diagnostico de IA", criarAbaIA());

        add(abas, BorderLayout.CENTER);
        atualizarTabelasECombos();
        new javax.swing.Timer(60000, e -> atualizarTabelasECombos()).start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                try { banco.close(); } catch (Exception ignored) { }
            }
        });
    }

    private void carregarDadosIniciais() {
        LoteProduto l1 = new LoteProduto("LOT-MRG-01", "Morango Bandeja 250g", false,
                LocalDate.now().minusDays(1), 6, 45, 5.50, 10.00);
        LoteProduto l2 = new LoteProduto("LOT-MRG-02", "Morango Bandeja 250g", false,
                LocalDate.now().minusDays(4), 1, 32, 5.50, 10.00);
        LoteProduto l3 = new LoteProduto("LOT-BNN-01", "Banana Prata (kg)", true,
                LocalDate.now().minusDays(2), 2, 95, 2.20, 4.50);

        lotes.add(l1);
        lotes.add(l2);
        lotes.add(l3);

        registrarSimulacao(l1, new HistoricoDemanda(DayOfWeek.SATURDAY, 10, 48.0, 38.0), "Sabado 10h (Pico)");
        registrarSimulacao(l2, new HistoricoDemanda(DayOfWeek.TUESDAY, 15, 14.0, 22.0), "Terca 15h (Critico)");
        registrarSimulacao(l3, new HistoricoDemanda(DayOfWeek.WEDNESDAY, 18, 35.0, 30.0), "Quarta 18h (Climaterico)");
    }

    private JPanel criarAbaLotes() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        modeloLotes = new DefaultTableModel(
                new Object[]{"Codigo", "Produto", "Climaterico", "Validade (dias)", "Estoque", "Custo (R$)", "Base (R$)"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabelaLotes = new JTable(modeloLotes);
        painel.add(new JScrollPane(tabelaLotes), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("Cadastrar Novo Lote"));

        JTextField txtCodigo = new JTextField();
        JTextField txtNome = new JTextField();
        JCheckBox chkClimaterico = new JCheckBox("Sim");
        JTextField txtValidade = new JTextField("3");
        JTextField txtEstoque = new JTextField("50");
        JTextField txtCusto = new JTextField("4.00");
        JTextField txtBase = new JTextField("8.00");
        JButton btnSalvar = new JButton("Adicionar Lote");

        form.add(new JLabel("Codigo:"));
        form.add(txtCodigo);
        form.add(new JLabel("Produto:"));
        form.add(txtNome);
        form.add(new JLabel("Climaterico:"));
        form.add(chkClimaterico);
        form.add(new JLabel("Dias validade:"));
        form.add(txtValidade);
        form.add(new JLabel("Estoque:"));
        form.add(txtEstoque);
        form.add(new JLabel("Preco custo:"));
        form.add(txtCusto);
        form.add(new JLabel("Preco base:"));
        form.add(txtBase);
        form.add(new JLabel(""));
        form.add(btnSalvar);

        btnSalvar.addActionListener(e -> {
            try {
                String codigo = txtCodigo.getText().trim().toUpperCase();
                String nome = txtNome.getText().trim();
                int validade = Integer.parseInt(txtValidade.getText().trim());
                int estoque = Integer.parseInt(txtEstoque.getText().trim());
                double custo = UtilNumerico.lerDecimal(txtCusto.getText());
                double base = UtilNumerico.lerDecimal(txtBase.getText());

                if (codigo.isEmpty() || nome.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Preencha codigo e nome do produto.");
                    return;
                }

                for (LoteProduto existente : lotes) {
                    if (existente.getCodigoLote().equalsIgnoreCase(codigo))
                        throw new IllegalArgumentException("Este código de lote já existe.");
                }
                LoteProduto novo = new LoteProduto(
                        codigo, nome, chkClimaterico.isSelected(),
                        LocalDate.now(), validade, estoque, custo, base
                );
                banco.inserir(novo);
                lotes.add(novo);
                atualizarTabelasECombos();

                txtCodigo.setText("");
                txtNome.setText("");
                JOptionPane.showMessageDialog(this, "Lote cadastrado com sucesso!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Verifique os campos numericos: " + ex.getMessage());
            }
        });

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton venda = new JButton("Registrar venda / saída");
        JButton repor = new JButton("Registrar reposição");
        JButton regras = new JButton("Estoque mínimo e temporada");
        acoes.add(venda); acoes.add(repor); acoes.add(regras);
        venda.addActionListener(e -> movimentar(tabelaLotes, false));
        repor.addActionListener(e -> movimentar(tabelaLotes, true));
        regras.addActionListener(e -> configurarLote(tabelaLotes));
        painel.add(acoes, BorderLayout.SOUTH);
        painel.add(form, BorderLayout.EAST);
        return painel;
    }

    private JPanel criarAbaSimulacao() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controles = new JPanel(new GridLayout(0, 2, 8, 8));
        comboLotesSimulacao = new JComboBox<>();
        JComboBox<DayOfWeek> comboDia = new JComboBox<>(DayOfWeek.values());
        JTextField txtHora = new JTextField("10");
        JTextField txtVendasHora = new JTextField("40.0");
        JTextField txtMetaHora = new JTextField("30.0");
        JTextField txtRotulo = new JTextField("Simulacao GUI");
        JButton btnSimular = new JButton("Calcular Preco");

        controles.add(new JLabel("Lote:"));
        controles.add(comboLotesSimulacao);
        controles.add(new JLabel("Dia da semana:"));
        controles.add(comboDia);
        controles.add(new JLabel("Hora (0-23):"));
        controles.add(txtHora);
        controles.add(new JLabel("Vendas/hora:"));
        controles.add(txtVendasHora);
        controles.add(new JLabel("Meta/hora:"));
        controles.add(txtMetaHora);
        controles.add(new JLabel("Rotulo cenario:"));
        controles.add(txtRotulo);
        controles.add(new JLabel(""));
        controles.add(btnSimular);

        areaResultadoSimulacao = new JTextArea(10, 50);
        areaResultadoSimulacao.setEditable(false);
        areaResultadoSimulacao.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        btnSimular.addActionListener(e -> {
            int idx = comboLotesSimulacao.getSelectedIndex();
            if (idx < 0 || idx >= lotes.size()) {
                return;
            }
            try {
                LoteProduto lote = lotes.get(idx);
                DayOfWeek dia = (DayOfWeek) comboDia.getSelectedItem();
                int hora = Integer.parseInt(txtHora.getText().trim());
                double vendas = UtilNumerico.lerDecimal(txtVendasHora.getText());
                double meta = UtilNumerico.lerDecimal(txtMetaHora.getText());
                String rotulo = txtRotulo.getText().trim().isEmpty() ? "Cenario" : txtRotulo.getText().trim();

                HistoricoDemanda demanda = new HistoricoDemanda(dia, hora, vendas, meta);
                consultarEmSegundoPlano(btnSimular, areaResultadoSimulacao,
                        java.util.Collections.singletonList(lote),
                        java.util.Collections.singletonList(demanda));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro na simulacao: " + ex.getMessage());
            }
        });

        painel.add(controles, BorderLayout.NORTH);
        painel.add(new JScrollPane(areaResultadoSimulacao), BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarAbaPrecos() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        modeloPrecos = new DefaultTableModel(
                new Object[]{"Codigo", "Produto", "Cenario", "Estrategia", "Base (R$)", "Final (R$)", "Variacao (%)"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JPanel topo = new JPanel(new BorderLayout(10, 10));
        topo.add(new JLabel("Preços atuais por lote • cálculo local automático, considerando validade e temporada"), BorderLayout.CENTER);
        JButton recalcular = new JButton("Recalcular preços");
        recalcular.addActionListener(e -> atualizarTabelasECombos());
        topo.add(recalcular, BorderLayout.EAST);
        painel.add(topo, BorderLayout.NORTH);
        JTable tabela = new JTable(modeloPrecos);
        tabela.setRowHeight(32);
        tabela.setAutoCreateRowSorter(true);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(250);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(220);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(210);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        painel.add(new JLabel("Sem simulação nesta sessão: 8 vendas/h e meta 10. Sugestões não alteram o preço base."), BorderLayout.SOUTH);
        return painel;
    }

    private JPanel criarAbaIA() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRodarIA = new JButton("Executar Diagnostico de IA em Todos os Lotes");
        topo.add(btnRodarIA);

        areaDiagnosticoIA = new JTextArea();
        areaDiagnosticoIA.setEditable(false);
        areaDiagnosticoIA.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JButton configurar = new JButton("Configurar IA");
        topo.add(configurar);
        configurar.addActionListener(e -> {
            javax.swing.JPasswordField chave = new javax.swing.JPasswordField(28);
            JTextField modelo = new JTextField();
            JPanel campos = new JPanel(new GridLayout(0, 1, 6, 6));
            campos.add(new JLabel("Chave da API (somente nesta sessão):"));
            campos.add(chave);
            campos.add(new JLabel("Modelo da IA (vazio = padrão):"));
            campos.add(modelo);
            if (JOptionPane.showConfirmDialog(this, campos, "Configurar IA",
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                char[] segredo = chave.getPassword();
                clienteIA = new ClienteIA(consultor, new String(segredo), modelo.getText());
                java.util.Arrays.fill(segredo, '\0');
                chave.setText("");
                areaDiagnosticoIA.setText(clienteIA.isConfigurada()
                        ? "Chave configurada. Execute o diagnóstico para testar a conexão."
                        : "Sem chave: análise local ativa.");
            }
        });
        areaDiagnosticoIA.setLineWrap(true);
        areaDiagnosticoIA.setWrapStyleWord(true);
        areaResultadoSimulacao.setLineWrap(true);
        areaResultadoSimulacao.setWrapStyleWord(true);
        btnRodarIA.addActionListener(e -> {
            List<LoteProduto> copia = new ArrayList<>(lotes);
            List<HistoricoDemanda> demandas = new ArrayList<>();
            for (LoteProduto lote : copia) {
                HistoricoDemanda demanda = historico.ultimaDemandaDoLote(lote.getCodigoLote());
                if (demanda == null) demanda = new HistoricoDemanda(
                        LocalDate.now().getDayOfWeek(), java.time.LocalTime.now().getHour(), 8, 10);
                demandas.add(demanda);
            }
            consultarEmSegundoPlano(btnRodarIA, areaDiagnosticoIA, copia, demandas);
        });

        JButton atualizar = new JButton("Atualizar alertas");
        topo.add(atualizar);
        atualizar.addActionListener(e -> atualizarTabelasECombos());
        areaDiagnosticoIA.setText("Os alertas acima são atualizados automaticamente. Cadastre seus lotes e configure o mínimo e a temporada na aba Lotes e Cadastro.\nClique em Executar Diagnóstico para receber o parecer complementar da IA.");
        painelGestao = new PainelGestao(topo, areaDiagnosticoIA);
        return painelGestao;
    }

    private double registrarSimulacao(LoteProduto lote, HistoricoDemanda demanda, String rotulo) {
        ResultadoPrecificacao resultado = motor.calcular(lote, demanda);
        historico.registrar(resultado, consultor.gerarParecer(lote, demanda, resultado));
        return resultado.getPrecoFinal();
    }

    private void consultarEmSegundoPlano(JButton botao, JTextArea area,
            List<LoteProduto> selecionados, List<HistoricoDemanda> demandas) {
        botao.setEnabled(false);
        area.setText("Consultando... A janela continua disponível.\n");
        final ClienteIA cliente = clienteIA;
        new javax.swing.SwingWorker<Void, RegistroPrecificacao.Entrada>() {
            protected Void doInBackground() {
                for (int i = 0; i < selecionados.size(); i++) {
                    LoteProduto lote = selecionados.get(i);
                    HistoricoDemanda demanda = demandas.get(i);
                    if (lote.getDiasValidadeRestantes() < 0 || lote.getQuantidadeEstoque() == 0) continue;
                    ResultadoPrecificacao r = motor.calcular(lote, demanda);
                    publish(new RegistroPrecificacao.Entrada(r, cliente.gerarParecer(lote, demanda, r)));
                }
                return null;
            }
            protected void process(List<RegistroPrecificacao.Entrada> entradas) {
                for (RegistroPrecificacao.Entrada entrada : entradas) {
                    ResultadoPrecificacao r = entrada.getResultado();
                    historico.registrar(r, entrada.getParecer());
                    area.append("\n" + r.getCodigoLote() + " — " + r.getNomeProduto()
                            + "\n" + r.getNomeEstrategia() + " | " + UtilNumerico.moeda(r.getPrecoBase())
                            + " → " + UtilNumerico.moeda(r.getPrecoFinal())
                            + " (" + UtilNumerico.percentual(r.getVariacao()) + ")\n"
                            + entrada.getParecer() + "\n");
                }
                atualizarTabelasECombos();
            }
            protected void done() {
                try { get(); }
                catch (Exception ex) { area.append("\nNão foi possível concluir a consulta."); }
                finally { botao.setEnabled(true); }
            }
        }.execute();
    }

    private void atualizarTabelasECombos() {
        if (painelGestao != null) painelGestao.atualizar(lotes, historico);
        int selecionado = comboLotesSimulacao.getSelectedIndex();
        modeloLotes.setRowCount(0);
        comboLotesSimulacao.removeAllItems();
        for (LoteProduto l : lotes) {
            modeloLotes.addRow(new Object[]{
                    l.getCodigoLote(),
                    l.getNomeProduto(),
                    l.isClimaterico() ? "Sim" : "Nao",
                    l.getDiasValidadeRestantes(),
                    l.getQuantidadeEstoque(),
                    String.format("%.2f", l.getPrecoCusto()),
                    String.format("%.2f", l.getPrecoBase())
            });
            comboLotesSimulacao.addItem(l.getCodigoLote() + " - " + l.getNomeProduto());
        }

        if (selecionado >= 0 && selecionado < comboLotesSimulacao.getItemCount())
            comboLotesSimulacao.setSelectedIndex(selecionado);
        modeloPrecos.setRowCount(0);
        for (LoteProduto lote : lotes) {
            HistoricoDemanda demanda = historico.ultimaDemandaDoLote(lote.getCodigoLote());
            boolean padrao = demanda == null;
            if (padrao) demanda = new HistoricoDemanda(LocalDate.now().getDayOfWeek(),
                    java.time.LocalTime.now().getHour(), 8, 10);
            String cenario = padrao ? "Padrão: 8 vendas/h, meta 10" :
                    demanda.getDiaSemanaPt() + " " + demanda.getHora() + "h (simulado)";
            if (lote.getDiasValidadeRestantes() < 0 || lote.getQuantidadeEstoque() == 0) {
                modeloPrecos.addRow(new Object[]{lote.getCodigoLote(), lote.getNomeProduto(), cenario,
                        lote.getDiasValidadeRestantes() < 0 ? "Vencido — retirar da venda" : "Esgotado — repor estoque",
                        UtilNumerico.moeda(lote.getPrecoBase()), "—", "—"});
                continue;
            }
            ResultadoPrecificacao r = motor.calcular(lote, demanda);
            modeloPrecos.addRow(new Object[]{r.getCodigoLote(), r.getNomeProduto(), cenario,
                    r.getNomeEstrategia(), UtilNumerico.moeda(r.getPrecoBase()),
                    UtilNumerico.moeda(r.getPrecoFinal()), UtilNumerico.percentual(r.getVariacao())});
        }
    }

    private LoteProduto selecionado(JTable tabela) {
        int row = tabela.getSelectedRow();
        if (row < 0) throw new IllegalArgumentException("Selecione um lote na tabela primeiro.");
        return lotes.get(tabela.convertRowIndexToModel(row));
    }
    private void recarregar() throws Exception {
        List<LoteProduto> novos = banco.listar();
        lotes.clear(); lotes.addAll(novos); atualizarTabelasECombos();
    }
    private void movimentar(JTable tabela, boolean entrada) {
        try {
            LoteProduto l = selecionado(tabela);
            String texto = JOptionPane.showInputDialog(this, (entrada ? "Quantidade recebida no mesmo lote:" : "Quantidade vendida / retirada:") + "\n" + l.getNomeProduto());
            if (texto == null) return;
            int quantidade = Integer.parseInt(texto.trim());
            if (quantidade <= 0) throw new IllegalArgumentException("Informe um número inteiro positivo.");
            banco.movimentar(l.getCodigoLote(), entrada ? quantidade : -quantidade);
            recarregar();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Não foi possível salvar", JOptionPane.ERROR_MESSAGE); }
    }
    private void configurarLote(JTable tabela) {
        try {
            LoteProduto l = selecionado(tabela);
            JTextField minimo = new JTextField(String.valueOf(l.getEstoqueMinimo()));
            JComboBox<String> temporada = new JComboBox<>(new String[]{"Normal", "Alta demanda", "Baixa demanda"});
            temporada.setSelectedItem(l.getTemporada());
            JPanel campos = new JPanel(new GridLayout(0,1,5,5));
            campos.add(new JLabel("Alertar quando o estoque for igual ou inferior a:")); campos.add(minimo);
            campos.add(new JLabel("Temporada de demanda (informada pelo gestor):")); campos.add(temporada);
            campos.add(new JLabel("Alta: +10%; baixa: -10% antes dos limites. Validade curta tem prioridade."));
            if (JOptionPane.showConfirmDialog(this, campos, l.getNomeProduto(), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            banco.configurar(l.getCodigoLote(), Integer.parseInt(minimo.getText().trim()), (String)temporada.getSelectedItem());
            recarregar();
        } catch(Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Não foi possível salvar", JOptionPane.ERROR_MESSAGE); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
                new JanelaFreshStock().setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "FreshStock — erro ao iniciar", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}