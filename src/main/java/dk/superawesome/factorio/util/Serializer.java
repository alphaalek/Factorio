package dk.superawesome.factorio.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface Serializer<T> {

    T deserialize(ByteArrayInputStream stream) throws IOException;

    ByteArrayOutputStream serialize(T val) throws IOException;
}
