import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.swing.*;
public class TesteBanco {
    static void ok(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args)throws Exception{
        Path pasta=Files.createTempDirectory(Paths.get("work"),"teste-banco-");Path db=pasta.resolve("estoque");
        LoteProduto l=new LoteProduto("A1","Banana prata",true,LocalDate.now(),2,8,3,6);
        try(BancoEstoque b=new BancoEstoque(db)){b.inserir(l);b.movimentar("A1",-3);b.configurar("A1",12,"Alta demanda");
            try{b.movimentar("A1",-100);throw new AssertionError();}catch(IllegalArgumentException expected){}
            try{b.inserir(l);throw new AssertionError();}catch(java.sql.SQLException expected){}
        }
        try(BancoEstoque b=new BancoEstoque(db)){l=b.listar().get(0);ok(l.getQuantidadeEstoque()==5);ok(l.getEstoqueMinimo()==12);ok(l.getTemporada().equals("Alta demanda"));ok(l.getDataValidade().equals(LocalDate.now().plusDays(2)));}
        HistoricoDemanda d=new HistoricoDemanda(DayOfWeek.MONDAY,10,8,10);
        LoteProduto normal=new LoteProduto("N","Maçã",false,null,12,60,2,8);
        double preco=new MotorPrecificacao().calcular(normal,d).getPrecoFinal();normal.configurar(10,"Alta demanda");ok(new MotorPrecificacao().calcular(normal,d).getPrecoFinal()>preco);
        normal.configurar(10,"Baixa demanda");ok(new MotorPrecificacao().calcular(normal,d).getPrecoFinal()<preco);
        normal.setDataValidade(LocalDate.now().minusDays(1));ok(normal.getDiasValidadeRestantes()==-1);ok(PainelGestao.alertas(normal).contains("VENCIDO"));
        try{new MotorPrecificacao().calcular(normal,d);throw new AssertionError();}catch(IllegalArgumentException expected){}
        System.setProperty("freshstock.db",db.toString());
        SwingUtilities.invokeAndWait(()->{
            JanelaFreshStock janela=new JanelaFreshStock();janela.setVisible(true);
            JTabbedPane abas=(JTabbedPane)janela.getContentPane().getComponent(0);abas.setSelectedIndex(3);
            java.awt.image.BufferedImage img=new java.awt.image.BufferedImage(janela.getWidth(),janela.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g=img.createGraphics();janela.paint(g);g.dispose();
            try{javax.imageio.ImageIO.write(img,"png",Paths.get("work/painel.png").toFile());}catch(Exception ex){throw new RuntimeException(ex);}
            janela.dispatchEvent(new java.awt.event.WindowEvent(janela,java.awt.event.WindowEvent.WINDOW_CLOSING));
        });
    }
}
