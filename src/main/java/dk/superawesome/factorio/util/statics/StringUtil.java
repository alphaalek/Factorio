package dk.superawesome.factorio.util.statics;

public class StringUtil {

    public static double formatDecimals(double num, int decimals) {
        double pow = Math.pow(10, decimals);
        return Math.floor(num * pow) / pow;
    }
}
