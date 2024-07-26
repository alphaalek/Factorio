package dk.superawesome.factories.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public interface Serializer<T> {

    T deserialize(ByteArrayInputStream stream);

    ByteArrayOutputStream serialize(T val);
}
