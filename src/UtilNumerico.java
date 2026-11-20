import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class UtilNumerico {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private UtilNumerico() {
    }

    public static double limitar(double valor, double minimo, double maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    public static double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static String moeda(double valor) {
        return "R$ " + String.format(PT_BR, "%.2f", valor);
    }

    public static String numero(double valor, int casas) {
        return String.format(PT_BR, "%." + casas + "f", valor);
    }

    /** Recebe uma fração (0.05 = 5%) e devolve "+5,0%". */
    public static String percentual(double fracao) {
        return String.format(PT_BR, "%+.1f%%", fracao * 100.0);
    }

    /** Aceita "12,5", "12.5" e "1.234,56". Lança NumberFormatException se inválido. */
    public static double lerDecimal(String texto) {
        if (texto == null) {
            throw new NumberFormatException("vazio");
        }
        String t = texto.trim().replace(" ", "");
        if (t.isEmpty()) {
            throw new NumberFormatException("vazio");
        }
        if (t.indexOf(',') >= 0 && t.indexOf('.') >= 0) {
            t = t.replace(".", "").replace(",", ".");
        } else {
            t = t.replace(',', '.');
        }
        if (!t.matches("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)")) {
            throw new NumberFormatException("formato inválido");
        }
        return Double.parseDouble(t);
    }
}