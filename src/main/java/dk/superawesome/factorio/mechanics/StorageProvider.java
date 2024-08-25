package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.util.Array;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.function.Function;

public interface StorageProvider<M extends Mechanic<M>> {

    interface StorageContext<M extends Mechanic<M>> {

        List<Integer> getSlots();

        Storage createStorage(M mechanic);
    }

    class Builder<M extends Mechanic<M>> {

        public static <M extends Mechanic<M>> Builder<M> makeContext() {
            return new Builder<>();
        }

        private final Array<StorageContext<M>> contexts = new Array<>();

        public Builder<M> set(int context, List<Integer> slots, Function<M, Storage> create) {
            contexts.set(context, new StorageContext<>() {
                @Override
                public List<Integer> getSlots() {
                    return slots;
                }

                @Override
                public Storage createStorage(M mechanic) {
                    return create.apply(mechanic);
                }
            });

            return this;
        }

        public StorageProvider<M> build() {
            return new StorageProvider<>() {
                @Override
                public Storage createStorage(Mechanic<?> mechanic, int context) {
                    return contexts.get(context).createStorage((M) mechanic);
                }

                @Override
                public List<Integer> getSlots(int context) {
                    return contexts.get(context).getSlots();
                }
            };
        }
    }

    Storage createStorage(Mechanic<?> mechanic, int context);

    List<Integer> getSlots(int context);
}
