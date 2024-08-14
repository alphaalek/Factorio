package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.util.Array;

import java.util.Optional;

public class MechanicLevel {

    public interface Registry {

        Array<Object> get(int level);

        class Builder {

            public static Builder make(int levels) {
                return new Builder(levels);
            }

            private Builder (int levels) {
                this.levels = levels;
            }

            private final int levels;
            private final Array<Array<Object>> data = new Array<>();

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

            public Registry build() {
                return i -> data.get(i - 1);
            }
        }
    }

    public static MechanicLevel from(Mechanic<?> mechanic, int level) {
        return new MechanicLevel(mechanic, level);
    }

    private final Array<Object> data;
    private final int level;

    public MechanicLevel(Mechanic<?> mechanic, int level) {
        this.data = Optional.ofNullable(mechanic.getProfile().getLevelRegistry()).map(a -> a.get(level)).orElseGet(Array::new);
        this.level = level;
    }

    public int getLevel() {
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
