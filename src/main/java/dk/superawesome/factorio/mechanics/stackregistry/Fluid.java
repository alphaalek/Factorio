package dk.superawesome.factorio.mechanics.stackregistry;

import dk.superawesome.factorio.util.statics.StringUtil;

public enum Fluid {

    WATER(3),

    LAVA(3),

    SNOW(3),
    ;

    private final int maxTransfer;

    Fluid(int maxTransfer) {
        this.maxTransfer = maxTransfer;
    }

    public int getMaxTransfer() {
        return maxTransfer;
    }

    @Override
    public String toString() {
        return StringUtil.capitalize(this);
    }
}
