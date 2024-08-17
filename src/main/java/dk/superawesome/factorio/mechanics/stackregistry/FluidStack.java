package dk.superawesome.factorio.mechanics.stackregistry;

public class FluidStack {

    private Fluid fluid;
    private int amount;

    public FluidStack(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public void setFluid(Fluid fluid) {
        this.fluid = fluid;
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
        return fluid != null && amount == fluid.getMaxTransfer();
    }
}
