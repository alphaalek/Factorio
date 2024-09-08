package dk.superawesome.factorio.mechanics;

import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Management {

    public static int OPEN = 1;
    public static int LEVEL_UP = 2;
    public static int DELETE = 4;
    public static int MODIFY_MEMBERS = 8;
    public static int MODIFY_SIGN = 16;
    public static int MOVE = 32;

    public static int MEMBER_ACCESS = OPEN | LEVEL_UP | MODIFY_SIGN;
    public static int OWNER_ACCESS = MEMBER_ACCESS | DELETE | MODIFY_MEMBERS | MOVE;

    public static class Serializer implements dk.superawesome.factorio.util.Serializer<Management> {

        private final MechanicSerializer mechanicSerializer;

        public Serializer(MechanicSerializer mechanicSerializer) {
            this.mechanicSerializer = mechanicSerializer;
        }

        @Override
        public Management deserialize(ByteArrayInputStream stream) throws IOException {
            UUID owner = mechanicSerializer.readUUID(stream);

            Set<UUID> members = new HashSet<>();
            int size = mechanicSerializer.readInt(stream);
            for (int i = 0; i < size; i++) {
                UUID member = mechanicSerializer.readUUID(stream);
                if (member != null) {
                    members.add(member);
                }
            }

            if (owner != null) {
                return new Management(owner, members);
            } else {
                return null;
            }
        }

        @Override
        public ByteArrayOutputStream serialize(Management management) throws IOException {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            mechanicSerializer.writeUUID(stream, management.getOwner());
            mechanicSerializer.writeInt(stream, management.getMembers().size());
            for (UUID member : management.getMembers()) {
                mechanicSerializer.writeUUID(stream, member);
            }

            return stream;
        }
    }

    private final UUID owner;
    private final Set<UUID> members;

    public Management(UUID owner, Set<UUID> members) {
        this.owner = owner;
        this.members = members;
    }

    public Management(UUID owner) {
        this(owner, new HashSet<>());
    }

    public UUID getOwner() {
        return this.owner;
    }

    public Set<UUID> getMembers() {
        return this.members;
    }

    public boolean hasAccess(Player player, int access) {
        // give operators access to all mechanics
        if (player.isOp()) {
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (this.owner.equals(uuid)) {
            return (OWNER_ACCESS & access) > 0;
        } else if (this.members.contains(uuid)) {
            return (MEMBER_ACCESS & access) > 0;
        } else {
            return false;
        }
    }
}
