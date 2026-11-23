import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import java.time.*;

/** Alertas locais imediatos; A IA complementa a análise sob demanda. */
public class PainelGestao extends JPanel {
    private final JPanel indicadores=new JPanel(new GridLayout(1,4,12,0));
    private final DefaultTableModel modelo=new DefaultTableModel(new String[]{"Prioridade / alertas","Produto / lote","Estoque","Validade","Temporada de demanda","Preço base","Sugestão","Ação recomendada"},0) {
        public boolean isCellEditable(int r,int c){return false;}
    };
    private final Color fundo=new Color(242,246,244), verde=new Color(22,100,73);
    public PainelGestao(JPanel ferramentas,JTextArea parecer) {
        super(new BorderLayout(0,14)); setBackground(fundo);
        setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
        JPanel cabecalho=new JPanel(new BorderLayout(0,12)); cabecalho.setOpaque(false);
        JPanel textos=new JPanel(new GridLayout(0,1,0,5)); textos.setOpaque(false);
        JLabel titulo=new JLabel("Seu estoque, sob acompanhamento"); titulo.setFont(new Font("Segoe UI",Font.BOLD,25)); titulo.setForeground(verde);
        textos.add(titulo); textos.add(new JLabel("Alertas automáticos • Validade real • Preços sugeridos para apoiar sua decisão"));
        cabecalho.add(textos,BorderLayout.NORTH); indicadores.setOpaque(false); cabecalho.add(indicadores,BorderLayout.CENTER);
        ferramentas.setOpaque(false); cabecalho.add(ferramentas,BorderLayout.SOUTH); add(cabecalho,BorderLayout.NORTH);
        JTable tabela=new JTable(modelo); tabela.setRowHeight(66); tabela.setFont(new Font("Segoe UI",Font.PLAIN,12));
        tabela.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12)); tabela.setShowVerticalLines(false); tabela.setAutoCreateRowSorter(true);
        tabela.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foco,int row,int col){
                Component c=super.getTableCellRendererComponent(t,v,sel,foco,row,col);
                String status=String.valueOf(t.getValueAt(row,0));
                if(!sel){c.setBackground(status.contains("VENCIDO")?new Color(255,229,229):status.contains("VENCE")?new Color(255,245,220):row%2==0?Color.WHITE:new Color(245,249,247));c.setForeground(new Color(35,53,45));}
                String texto=String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
                setText("<html><div style='width:"+Math.max(35,t.getColumnModel().getColumn(col).getWidth()-18)+"px'>"+texto+"</div></html>");
                setBorder(BorderFactory.createEmptyBorder(4,8,4,8));setToolTipText(String.valueOf(v));return c;
            }
        });
        tabela.getColumnModel().getColumn(0).setPreferredWidth(200); tabela.getColumnModel().getColumn(1).setPreferredWidth(190);
        tabela.getColumnModel().getColumn(7).setPreferredWidth(260);
        tabela.getColumnModel().getColumn(6).setPreferredWidth(165);
        JPanel lista=new JPanel(new BorderLayout(0,8)); lista.setOpaque(false);
        lista.add(new JLabel("Prioridades do dia — passe o mouse sobre uma célula para ler o texto completo"),BorderLayout.NORTH);
        lista.add(new JScrollPane(tabela),BorderLayout.CENTER);
        JPanel detalhes=new JPanel(new BorderLayout(0,6));detalhes.setOpaque(false);
        detalhes.add(new JLabel("Consultor IA • parecer complementar (o botão acima consulta a IA)"),BorderLayout.NORTH);
        parecer.setFont(new Font("Segoe UI",Font.PLAIN,14));parecer.setMargin(new Insets(12,12,12,12));
        detalhes.add(new JScrollPane(parecer),BorderLayout.CENTER);
        lista.setMinimumSize(new Dimension(300,150));detalhes.setMinimumSize(new Dimension(300,150));
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,lista,detalhes);split.setResizeWeight(.6);split.setDividerLocation(270);split.setBorder(null);
        add(split,BorderLayout.CENTER);
        JLabel nota=new JLabel("Temporada: demanda informada por você, sem previsão externa. Sugestões não alteram preços automaticamente.");
        nota.setFont(new Font("Segoe UI",Font.PLAIN,12));add(nota,BorderLayout.SOUTH);
    }
    private JPanel card(String titulo,int valor,Color cor) {
        JPanel p=new JPanel(new BorderLayout(0,8));p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(4,0,0,0,cor),BorderFactory.createEmptyBorder(10,14,10,14)));
        JLabel n=new JLabel(String.valueOf(valor));n.setFont(new Font("Segoe UI",Font.BOLD,28));n.setForeground(cor);p.add(n,BorderLayout.CENTER);p.add(new JLabel(titulo),BorderLayout.SOUTH);return p;
    }
    public static String alertas(LoteProduto l) {
        String s=l.getDiasValidadeRestantes()<0?"VENCIDO":l.getDiasValidadeRestantes()<=3?"VENCE EM BREVE":"";
        if(l.getQuantidadeEstoque()<=l.getEstoqueMinimo()) s+=(s.isEmpty()?"":" • ")+(l.getQuantidadeEstoque()==0?"ESGOTADO":"ESTOQUE BAIXO");
        return s.isEmpty()?"EM DIA":s;
    }
    public void atualizar(List<LoteProduto> lotes,RegistroPrecificacao historico) {
        modelo.setRowCount(0);int baixos=0,curtos=0,vencidos=0;
        List<LoteProduto> ordenados=new java.util.ArrayList<>(lotes);
        ordenados.sort(java.util.Comparator.comparingInt(l -> l.getDiasValidadeRestantes()));
        for(LoteProduto l:ordenados){
            int dias=l.getDiasValidadeRestantes();boolean baixo=l.getQuantidadeEstoque()<=l.getEstoqueMinimo();
            if(baixo)baixos++;if(dias<0)vencidos++;else if(dias<=3)curtos++;
            String sugestao="—",acao;
            if(dias<0)acao="Retirar da venda e registrar descarte.";
            else if(l.getQuantidadeEstoque()==0)acao="Repor estoque; sem preço sugerido.";
            else {
                HistoricoDemanda d=historico.ultimaDemandaDoLote(l.getCodigoLote());
                if(d==null)d=new HistoricoDemanda(LocalDate.now().getDayOfWeek(),LocalTime.now().getHour(),8,10);
                ResultadoPrecificacao r=new MotorPrecificacao().calcular(l,d);
                sugestao=UtilNumerico.moeda(r.getPrecoFinal())+" ("+UtilNumerico.percentual(r.getVariacao())+")";
                acao=dias<=3?"Priorizar venda antes do vencimento.":"Alta demanda".equals(l.getTemporada())?"Planejar reposição e avaliar aumento.":"Baixa demanda".equals(l.getTemporada())?"Reduzir compras e avaliar promoção.":"Acompanhar giro e margem.";
                if(baixo)acao="Repor: saldo no mínimo. "+acao;
            }
            modelo.addRow(new Object[]{alertas(l),l.getNomeProduto()+" / "+l.getCodigoLote(),l.getQuantidadeEstoque()+" / mín. "+l.getEstoqueMinimo(),dias<0?"Vencido há "+(-dias)+" d":dias==0?"Vence hoje":dias+" dias",l.getTemporada(),UtilNumerico.moeda(l.getPrecoBase()),sugestao,acao});
        }
        indicadores.removeAll();indicadores.add(card("Lotes cadastrados",lotes.size(),verde));indicadores.add(card("Estoque baixo / esgotado",baixos,new Color(190,117,18)));indicadores.add(card("Vencem em até 3 dias",curtos,new Color(205,95,30)));indicadores.add(card("Vencidos • retirar",vencidos,new Color(175,48,55)));indicadores.revalidate();indicadores.repaint();
    }
}
