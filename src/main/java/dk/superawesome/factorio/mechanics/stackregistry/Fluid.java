package dk.superawesome.factorio.mechanics.stackregistry;

import dk.superawesome.factorio.util.statics.StringUtil;

public enum Fluid {

    WATER(1,3),

    LAVA(3, 3),

    SNOW(3,3),
    ;

    private final int minTransfer;
    private final int maxTransfer;

    Fluid(int minTransfer, int maxTransfer) {
        this.minTransfer = minTransfer;
        this.maxTransfer = maxTransfer;
    }

    public int getMinTransfer() {
        return minTransfer;
    }

    public int getMaxTransfer() {
        return maxTransfer;
    }

    @Override
    public String toString() {
        return StringUtil.capitalize(this);
    }
}
