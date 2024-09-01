package dk.superawesome.factorio.mechanics;

import java.util.Random;

public class XPDist {

    private final double[] amounts;
    private int lastIndex = -1;

    public XPDist(int amount, double rangeStart, double rangeEnd) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (rangeStart >= rangeEnd) {
            throw new IllegalArgumentException("rangeStart must be less than rangeEnd");
        }

        amounts = new double[amount];
        Random rand = new Random();

        for (int i = 0; i < amount; i++)
            amounts[i] = rand.nextDouble(rangeStart, rangeEnd);
    }

    public double poll() {
        lastIndex = (lastIndex + 1) % amounts.length;
        return amounts[lastIndex];
    }
}