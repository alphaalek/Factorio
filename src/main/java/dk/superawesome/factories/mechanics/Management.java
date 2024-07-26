package dk.superawesome.factories.mechanics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Management {

    public static class Serializer implements dk.superawesome.factories.util.Serializer<Management> {

        @Override
        public Management deserialize(ByteArrayInputStream stream) {
            return null;
        }

        @Override
        public ByteArrayOutputStream serialize(Management val) {
            return null;
        }
    }
}
