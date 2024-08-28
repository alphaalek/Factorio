package dk.superawesome.factorio.mechanics;

import java.util.Random;

public class XPDist {

    private final double[] amounts;

    private int lastIndex;

    public XPDist(int amount, double rangeStart, double rangeEnd) {
        if (amount == 0) {
            throw new IllegalArgumentException("0 amount");
        }

        Random rand = new Random();
        amounts = new double[amount];
        for (int i = 0; i < amount; i++) {
            amounts[i] = rand.nextDouble(rangeStart, rangeEnd);
        }
    }

    public double poll() {
        lastIndex++;
        if (lastIndex == amounts.length) {
            lastIndex = 0;
        }

        return amounts[lastIndex];
    }
}
