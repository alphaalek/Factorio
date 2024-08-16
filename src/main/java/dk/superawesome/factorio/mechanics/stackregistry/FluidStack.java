package dk.superawesome.factorio.mechanics.stackregistry;

public class FluidStack {

    private final Fluid fluid;
    private int amount;

    public FluidStack(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean isEmpty() {
        return amount == 0;
    }

    public boolean isFull() {
        return amount == fluid.getMaxTransfer();
    }

    public FluidStack clone() {
        return new FluidStack(fluid, amount);
    }
}
