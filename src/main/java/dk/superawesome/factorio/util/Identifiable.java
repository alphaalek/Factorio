package dk.superawesome.factorio.util;

import java.util.function.Supplier;

public interface Identifiable extends Supplier<Integer> {

    int getID();

    default Integer get() {
        return getID();
    }

    static Identifiable of(Enum<?> enumVal) {
        return enumVal::ordinal;
    }
}
