package dk.superawesome.factorio.mechanics.stackregistry;

public enum Fluid {

    WATER(3),

    LAVA(3)
    ;

    private final int maxTransfer;

    Fluid(int maxTransfer) {
        this.maxTransfer = maxTransfer;
    }

    public int getMaxTransfer() {
        return maxTransfer;
    }
}
