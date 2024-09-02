package dk.superawesome.factorio.util.statics;

public class MathUtil {

    public static double getIncreaseDifference(double from, double to) {
        return StringUtil.formatDecimals(Math.abs((to - from) / from), 2);
    }

    public static double getIncreaseDifference(double from, double to, boolean percentage) {
        return percentage ? getIncreaseDifference(from, to) * 100 : getIncreaseDifference(from, to);
    }

    public static double getDecreaseDifference(double from, double to) {
        return StringUtil.formatDecimals(Math.abs((from - to) / from), 2);
    }

    public static double getDecreaseDifference(double from, double to, boolean precentage) {
        return precentage ? getDecreaseDifference(from, to) * 100 : getDecreaseDifference(from, to);
    }

    public static double ticksToMs(double ticks) {
        return ticks * 50;
    }
}
