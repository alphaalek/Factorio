package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.util.Array;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MechanicLevel {

    public static final int XP_REQUIRES_MARK = 4;
    public static final int LEVEL_COST_MARK = 5;
    public static final int THINK_DELAY_MARK = 6;

    public interface Registry {

        Array<Object> get(int level);

        List<String> getDescription(int level);

        int getMax();

        class Builder {

            public static Builder make(int levels) {
                return new Builder(levels);
            }

            private Builder (int levels) {
                this.levels = levels;
            }

            private final int levels;
            private final Array<Array<Object>> data = new Array<>();
            private final Array<List<String>> descriptions = new Array<>();

            public <T> Builder mark(int mark, Array<T> levels) {
                T last = null;
                for (int i = 0; i < this.levels; i++) {
                    Array<Object> data = this.data.get(i, Array::new);

                    if (levels.has(i)) {
                        last = levels.get(i);
                    }
                    T l = last;
                    data.set(mark, levels.getOr(i, () -> l));
                }

                return this;
            }

            public Builder setDescription(int level, List<String> description) {
                descriptions.set(level, description);
                return this;
            }

            public Registry build() {
                return new Registry() {
                    @Override
                    public Array<Object> get(int level) {
                        return data.get(level - 1);
                    }

                    @Override
                    public List<String> getDescription(int level) {
                        return descriptions.get(level);
                    }

                    @Override
                    public int getMax() {
                        return levels;
                    }
                };
            }
        }
    }

    public static MechanicLevel from(Mechanic<?> mechanic, int level) {
        return new MechanicLevel(mechanic, level);
    }

    private final Array<Object> data;
    private final List<String> description;
    private final int level;
    private final int max;

    private final Registry registry;

    public MechanicLevel(Mechanic<?> mechanic, int level) {
        this.registry = mechanic.getProfile().getLevelRegistry();
        this.data = Optional.ofNullable(registry).map(a -> a.get(level)).orElseGet(Array::new);
        this.description = Optional.ofNullable(registry).map(a -> a.getDescription(level)).orElseGet(ArrayList::new);
        this.max = Optional.ofNullable(registry).map(Registry::getMax).orElse(1);
        this.level = level;
    }

    public Registry getRegistry() {
        return registry;
    }

    public List<String> getDescription() {
        return description;
    }

    public int getMax() {
        return max;
    }

    public int lvl() {
        return this.level;
    }

    public <T> T get(int mark) {
        return (T) this.data.get(mark);
    }

    public int getInt(int mark) {
        return get(mark);
    }

    public double getDouble(int mark) {
        return get(mark);
    }

    public float getFloat(int mark) {
        return get(mark);
    }

    public long getLong(int mark) {
        return get(mark);
    }

    public byte getByte(int mark) {
        return get(mark);
    }

    public short getShort(int mark) {
        return get(mark);
    }

    public char getChar(int mark) {
        return get(mark);
    }

    public boolean getBoolean(int mark) {
        return get(mark);
    }
}
