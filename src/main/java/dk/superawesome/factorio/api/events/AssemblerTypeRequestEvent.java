package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.mechanics.impl.accessible.Assembler;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AssemblerTypeRequestEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Assembler.Types type;

    private int requires;
    private double produces;

    public AssemblerTypeRequestEvent(Assembler.Types type) {
        this.type = type;
        this.requires = type.getRequires();
        this.produces = type.getProduces();
    }

    public Assembler.Types getType() {
        return type;
    }

    public int getRequires() {
        return requires;
    }

    public void setRequires(int i) {
        this.requires = i;
    }

    public double getProduces() {
        return produces;
    }

    public void setProduces(double i) {
        this.produces = i;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
