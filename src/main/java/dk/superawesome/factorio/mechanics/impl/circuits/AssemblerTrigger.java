package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.AssemblerTypeChangeEvent;
import dk.superawesome.factorio.api.events.AssemblerTypeRequestEvent;
import dk.superawesome.factorio.api.events.MechanicBuildEvent;
import dk.superawesome.factorio.api.events.MechanicRemoveEvent;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.behaviour.Assembler;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;

import java.util.*;

public class AssemblerTrigger extends SignalTrigger<AssemblerTrigger> implements ThinkingMechanic {

    private final DelayHandler thinkDelayHandler = new DelayHandler(20*60);

    private boolean usePercentage;
    private double minPrice, minPercentage;
    private final List<Assembler> assemblers = new ArrayList<>();

    public AssemblerTrigger(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public void think() {
        if (assemblers.isEmpty()) {
            return;
        }

        // all assembler prices are over the minPrice or minPercentage then the power is off
        powered = true;
        for (Assembler assembler : assemblers) {
            if (assembler.getType() != null) {
                Assembler.Type type = assembler.getType();
                if (!Double.isNaN(minPrice)) {
                    if (type.getPricePerItem() >= minPrice) {
                        powered = false;
                    }
                } else {
                    double percentage = minPercentage;
                    if (type.produces() > type.type().getProduces()) {
                        percentage = (type.produces() / type.type().getProduces() - 1) * 100;
                    } else if (type.produces() < type.type().getProduces()) {
                        percentage = (type.type().getProduces() / type.produces() - 1) * 100;
                    }
                    if (percentage >= minPercentage) {
                        powered = false;
                    }
                }
            }
            if (!powered)
                break;
        }
        triggerLevers();
    }

    @EventHandler
    public void onPriceUpdate(AssemblerTypeRequestEvent event) {
        for (Assembler assembler : assemblers) {
            if (assembler.getType() != null && assembler.getType().isTypesEquals(event.getType())) {
                think();
                break;
            }
        }
    }

    @EventHandler
    public void onAssemblerTypeChange(AssemblerTypeChangeEvent event) {
        if (!assemblers.contains(event.getAssembler())) {
            return;
        }

        for (Assembler assembler : assemblers) {
            if (assembler.getType() != null && assembler.getType().isTypesEquals(event.getNewType())) {
                think();
                break;
            }
        }
    }

    @Override
    public void onBlocksLoaded() {
        // get price from sign or %
        loadPrice((Sign) loc.getBlock().getRelative(getRotation()).getState());

        // check IO after all other mechanics has been loaded & lever has been placed
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            MechanicManager manager = Factorio.get().getMechanicManagerFor(this);
            for (BlockFace face : Routes.RELATIVES) {
                Block block = loc.getBlock().getRelative(face);
                if (block.getType() == Material.LEVER) {
                    levers.add(block);
                    triggerLever(block, true); // locked so assembler types can be updated before triggering
                }

                Mechanic<?> at = manager.getMechanicPartially(block.getLocation());
                if (at instanceof Assembler assembler) {
                    assemblers.add(assembler);
                }
            }
        });

        Bukkit.getScheduler().runTask(Factorio.get(), this::think);
    }


    @EventHandler
    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        super.handleBlockPlace(event);
    }

    @EventHandler
    @Override
    public void onBlockBreak(BlockBreakEvent event)  {
        super.handleBlockBreak(event);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getBlock().equals(loc.getBlock())) {
            Bukkit.getScheduler().runTask(Factorio.get(), () -> loadPrice((Sign) event.getBlock().getState()));
            think();
        }
    }

    private void loadPrice(Sign sign) {
        String line = sign.getSide(Side.FRONT).getLine(2).trim();
        minPrice = Double.NaN;
        minPercentage = Double.NaN;
        try {
            minPrice = Double.parseDouble(line);
        } catch (NumberFormatException e) {
            try {
                minPercentage = Double.parseDouble(line.replace("%", ""));
            } catch (NumberFormatException ignored) {
            }
        }

        if (Double.isNaN(minPrice) && Double.isNaN(minPercentage)) {
            Factorio.get().getMechanicManager(loc.getWorld()).unload(this);
            Buildings.remove(loc.getWorld(), this);

            Player owner = Bukkit.getPlayer(management.getOwner());
            if (owner != null) {
                owner.sendMessage("§cUgyldig pris eller procent!");
            }
        } else {
            sign.getSide(Side.FRONT).setLine(2, line);
            // get all lines except 1 and 2 lines
            for (int i = 3; i < 4; i++) {
                sign.getSide(Side.FRONT).setLine(i, "");
            }

            sign.update();
        }
    }

    @Override
    public MechanicProfile<AssemblerTrigger> getProfile() {
        return Profiles.ASSEMBLER_TRIGGER;
    }

    @Override
    public DelayHandler getThinkDelayHandler() {
        return thinkDelayHandler;
    }

    @EventHandler
    public void onMechanicBuild(MechanicBuildEvent event) {
        if (event.getMechanic() instanceof Assembler assembler) {
            int xDiff = Math.abs(assembler.getLocation().getBlockX() - loc.getBlockX());
            int zDiff = Math.abs(assembler.getLocation().getBlockZ() - loc.getBlockZ());
            int yDiff = Math.abs(assembler.getLocation().getBlockY() - loc.getBlockY());
            if (xDiff <= 1 && zDiff <= 1 && yDiff == 0) {
                assemblers.add(assembler);
            }
        }
    }

    @EventHandler
    public void onMechanicRemove(MechanicRemoveEvent event) {
        if (event.getMechanic() instanceof Assembler assembler) {
            int xDiff = Math.abs(assembler.getLocation().getBlockX() - loc.getBlockX());
            int zDiff = Math.abs(assembler.getLocation().getBlockZ() - loc.getBlockZ());
            int yDiff = Math.abs(assembler.getLocation().getBlockY() - loc.getBlockY());
            if (xDiff <= 1 && zDiff <= 1 && yDiff == 0) {
                assemblers.remove(assembler);
            }
        }
    }
}
