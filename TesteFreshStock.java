import java.time.*;
import javax.swing.*;
import java.lang.reflect.*;

public class TesteFreshStock {
    static void verificar(boolean condicao) {
        if (!condicao) throw new AssertionError("Falha na verificação");
    }
    static void invalido(Runnable acao) {
        try { acao.run(); } catch (IllegalArgumentException ok) { return; }
        throw new AssertionError("Valor inválido aceito");
    }
    public static void main(String[] args) throws Exception {
        HistoricoDemanda d = new HistoricoDemanda(DayOfWeek.MONDAY, 10, 8, 10);
        LoteProduto l = new LoteProduto("T1", "Banana", true, LocalDate.now(), 1, 100, 3, 6);
        MotorPrecificacao m = new MotorPrecificacao();
        ResultadoPrecificacao r = m.calcular(l,d);
        verificar(r.getPrecoFinal() >= 2.10 && r.getPrecoFinal() <= 7.8);
        verificar(m.escolherEstrategia(l,d) instanceof PrecificacaoUrgenciaValidade);
        verificar(Math.abs(UtilNumerico.lerDecimal("1.234,56") - 1234.56) < .001);
        invalido(() -> new HistoricoDemanda(DayOfWeek.MONDAY, 24, 1, 2));
        invalido(() -> new HistoricoDemanda(DayOfWeek.MONDAY, 10, Double.NaN, 2));
        invalido(() -> new LoteProduto("x", "x", false, null, 1, 1, Double.POSITIVE_INFINITY, 2));
        ClienteGeminiIA local = new ClienteGeminiIA(new ConsultorEstoqueIA(), "", "");
        verificar(local.gerarParecer(l,d,r).contains("IA Local"));
        final JanelaFreshStock[] janela = new JanelaFreshStock[1];
        SwingUtilities.invokeAndWait(() -> {
            janela[0] = new JanelaFreshStock();
            janela[0].setVisible(true);
            verificar(janela[0].isShowing());
        });
        SwingUtilities.invokeAndWait(() -> janela[0].dispose());
        System.out.println("OK: cálculo, urgência, decimais, validação, fallback e abertura da janela.");
        if (args.length > 0 && args[0].equals("--online")) {
            Method consulta = ClienteGeminiIA.class.getDeclaredMethod("consultarGemini", String.class);
            consulta.setAccessible(true);
            String texto = (String) consulta.invoke(new ClienteGeminiIA(new ConsultorEstoqueIA()), "Responda apenas OK");
            verificar(!texto.trim().isEmpty());
            System.out.println("OK: cliente Java/Gemini respondeu ao teste genérico, sem dados de estoque.");
        }
    }
}
