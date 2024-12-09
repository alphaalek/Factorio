package dk.superawesome.factorio.mechanics.impl.relative;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.*;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.accessible.Assembler;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class AssemblerTrigger extends SignalTrigger<AssemblerTrigger> implements ThinkingMechanic {

    private final DelayHandler thinkDelayHandler = new DelayHandler(20 * 10);

    private boolean usePrice, usePercentage;
    private double minPrice, minPercentage;
    private final Set<Assembler> assemblers = new HashSet<>();

    public AssemblerTrigger(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void think() {
        // if all assembler prices are above the minPrice or minPercentage, power is off
        powered = false;

        for (Assembler assembler : assemblers) {
            if (assembler.getType() != null) {
                Assembler.Type type = assembler.getType();

                if (usePrice && type.getPricePerItem() < minPrice) {
                    powered = true;
                    break;
                } else if (usePercentage) {
                    double percentage = 0;
                    if (type.getProduces() > type.getType().getProduces()) {
                        percentage = (type.getProduces() / type.getType().getProduces() - 1) * 100;
                    } else if (type.getProduces() < type.getType().getProduces()) {
                        percentage = (type.getType().getProduces() / type.getProduces() - 1) * 100 * -1;
                    }

                    if (percentage < minPercentage) {
                        powered = true;
                        break;
                    }
                }
            }
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
        for (Assembler assembler : assemblers) {
            if (assembler.getType() != null && assembler.getType().isTypesEquals(event.getNewType())) {
                think();
                break;
            }
        }
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
    @Override
    public void onLeverPull(PlayerInteractEvent event) {
        super.handleLeverPull(event);
        Bukkit.getScheduler().runTask(Factorio.get(), this::think);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        assemblers.clear();
        loadPrice(getSign(), by);

        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            setupRelativeBlocks(at -> triggerLever(at, true), at -> {
                if (at instanceof Assembler assembler) {
                    assemblers.add(assembler);
                }
            });

            think();
        });
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getBlock().equals(getSign().getBlock())) {
            Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                loadPrice((Sign) event.getBlock().getState(), event.getPlayer());
                think();
            });
        }
    }

    private void loadPrice(Sign sign, Player by) {
        usePercentage = false;
        usePrice = false;
        int i = 0;
        for (String line : Arrays.copyOfRange(sign.getSide(Side.FRONT).getLines(), 1, 4)) {
            try {
                // check for absolute price
                minPrice = Double.parseDouble(line);
                usePrice = true;
            } catch (NumberFormatException e) {
                if (line.endsWith("%")) {
                    try {
                        // check for percentage
                        minPercentage = Double.parseDouble(line.substring(0, line.length() - 1));
                        usePercentage = true;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            i++;

            if (usePrice || usePercentage) {
                // clear all other lines
                for (int j = 1; j < 4; j++) {
                    if (j != i) {
                        sign.getSide(Side.FRONT).setLine(j, "");
                    }
                }
                sign.update();

                // we found a match, so just break
                break;
            }
        }


        // check if no valid filter found for this line
        if (!usePrice && !usePercentage) {
            Factorio.get().getMechanicManagerFor(this).deleteMechanic(this);

            if (by != null) {
                by.sendMessage("§cUgyldig pris eller procent!");
            }
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
    public void onMechanicLoad(MechanicLoadEvent event) {
        if (event.getMechanic() instanceof Assembler assembler) {
            MechanicManager manager = Factorio.get().getMechanicManagerFor(this);
            BlockUtil.forRelative(loc.getBlock(), block -> {
                if (manager.getMechanicAt(block.getLocation()) == event.getMechanic()) {
                    assemblers.add(assembler);
                }
            });
        }
    }

    @EventHandler
    public void onMechanicRemove(MechanicRemoveEvent event) {
        if (event.getMechanic() instanceof Assembler assembler) {
            assemblers.remove(assembler);
        }
    }
}
