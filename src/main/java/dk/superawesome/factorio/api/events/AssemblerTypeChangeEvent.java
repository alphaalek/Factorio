package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.mechanics.impl.accessible.Assembler;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AssemblerTypeChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Assembler assembler;

    private final Assembler.Types oldType;
    private final Assembler.Types newType;

    public AssemblerTypeChangeEvent(Assembler assembler, Assembler.Types oldType, Assembler.Types newType) {
        this.assembler = assembler;
        this.oldType = oldType;
        this.newType = newType;
    }

    public Assembler getAssembler() {
        return assembler;
    }

    public Assembler.Types getOldType() {
        return oldType;
    }

    public Assembler.Types getNewType() {
        return newType;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
