package dk.superawesome.factorio.util.db;

public interface Type<T> {

    T from(String text);

    String convert(T val);
}
