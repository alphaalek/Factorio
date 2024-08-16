package dk.superawesome.factorio.mechanics.transfer;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.mechanics.stackregistry.FluidStack;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public interface FluidContainer extends Container<FluidCollection> {

    default boolean accepts(TransferCollection collection) {
        return collection instanceof FluidCollection;
    }

    default <G extends BaseGui<G>> int put(FluidCollection from, int take, AtomicReference<G> inUse, BiConsumer<G, Integer> doGui, HeapToStackAccess<FluidStack> access) {
        FluidStack fluidStack = from.take(take);
        int add = 0;
        if (fluidStack != null) {
            add = fluidStack.getAmount();

            if (access.get() == null) {
                FluidStack type = fluidStack.clone();
                type.setAmount(1);
                access.set(type);
            }
        }

        if (add > 0) {
            G gui = inUse.get();
            if (gui != null) {
                doGui.accept(gui, add);
            }
        }

        return add;
    }
}
