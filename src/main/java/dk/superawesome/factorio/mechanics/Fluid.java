package dk.superawesome.factorio.mechanics;

public enum Fluid {

    WATER(3),

    LAVA(2)
    ;

    private final int maxTransfer;

    Fluid(int maxTransfer) {
        this.maxTransfer = maxTransfer;
    }

    public int getMaxTransfer() {
        return maxTransfer;
    }
}
