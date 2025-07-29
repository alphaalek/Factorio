package dk.superawesome.factorio.mechanics;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

public record Snapshot(int level, double xp, Management management, String strData) {

    public static Snapshot create(AbstractMechanic<?> mechanic) throws Exception {
        Optional<ByteArrayOutputStream> dataOptional = mechanic.saveData();
        String strData = "";
        if (dataOptional.isPresent()) {
            ByteArrayOutputStream data = dataOptional.get();
            strData = MechanicStorageContext.encode(data);
        }
        return new Snapshot(mechanic.getLevel().lvl(), mechanic.getXP(), mechanic.getManagement().copy(), strData);

    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Snapshot(int level_, double xp_, Management management_, String strData_)) {
            return level_ == this.level
                    && xp_ == this.xp
                    && management_.equals(this.management)
                    && strData_.equals(this.strData);
        }
        return false;
    }
}