package dk.superawesome.factorio.util.statics;

public class StringUtil {

    public static double formatDecimals(double num, int decimals) {
        double pow = Math.pow(10, decimals);
        return Math.floor(num * pow) / pow;
    }

    public static String capitalize(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    public static String capitalize(Enum<?> obj) {
        String[] split = obj.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (String s : split) {
            builder.append(capitalize(s)).append(" ");
        }
        return builder.toString().trim();
    }
}
