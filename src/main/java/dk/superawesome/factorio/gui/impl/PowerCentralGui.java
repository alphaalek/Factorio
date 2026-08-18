package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.Elements;
import dk.superawesome.factorio.gui.GuiElement;
import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.impl.power.PowerCentral;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

import static dk.superawesome.factorio.util.statics.StringUtil.formatNumber;

public class PowerCentralGui extends MechanicGui<PowerCentralGui, PowerCentral> {

    private static Graph getConsumptionGraph(Inventory inventory, PowerCentral mechanic) {
        return new Graph(inventory,
                new DoubleSupplier[] {
                    mechanic::pollRecentConsumption,
                    mechanic::pollRecentProduction
                },
                e -> {
                    ItemStack itemStack = new ItemBuilder(Material.LIGHT_GRAY_WOOL)
                        .setName("§eForbrug: " + formatNumber(e[0]) + "W")
                        .addLore("§ePotentiel forbrug: " + formatNumber(mechanic.getRecentMax()) + "W")
                        .addLore("§eProduktion: " + formatNumber(e[1]) + "W")
                        .addLore("§eEnergi: " + formatNumber(mechanic.getEnergy()) + "J")
                        .addLore("§eKapacitet: " + formatNumber(mechanic.getCapacity()) + "J")
                        .build();
                    PowerCentralGui gui = mechanic.<PowerCentralGui>getGuiInUse().get();
                    if (gui != null) {
                        gui.updateDataSlot(itemStack);
                    }
                    return itemStack;
                }
        );
    }

    private static final GuiElement ACTIVATE_BUTTON = new GuiElement() {

        @Override
        public void handle(InventoryClickEvent event, Player player, MechanicGui<?, ?> gui) {
            PowerCentral pc = (PowerCentral) gui.getMechanic();
            if (pc.isActivated()) {
                pc.setActivated(false);
                player.sendMessage("§cDu har nu deaktiveret Power Central!");
            } else {
                pc.setActivated(true);
                player.sendMessage("§aDu har nu aktiveret Power Central!");
            }

            player.closeInventory();
        }

        @Override
        public ItemStack getItem(Mechanic<?> mechanic) {
            return new ItemBuilder(Material.REDSTONE_TORCH).setName(((PowerCentral)mechanic).isActivated() ? "§cDeaktivér Power Central" : "§aAktivér Power Central").build();
        }
    };

    private final List<BukkitTask> tasks = new ArrayList<>();

    public PowerCentralGui(PowerCentral mechanic, AtomicReference<PowerCentralGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder(), mechanic + " " + (mechanic.isActivated() ? "§a(Til)" : "§c(Fra)"));
        initCallback.call();

        this.tasks.add(
                Bukkit.getScheduler().runTaskTimer(Factorio.get(), getConsumptionGraph(getInventory(), getMechanic()), 0L, 10L));

        getMechanic().setHasGraph(true);
    }

    private static class Graph implements Runnable {

        private static final int WIDTH = 9;
        private static final int COLLECT_WIDTH = 12;
        private static final int GRADE_MASK = ~0 & 0x0F;

        private final int[] columns = new int[WIDTH];
        private double[] states = new double[COLLECT_WIDTH];

        private final DoubleSupplier[] currentStates;
        private final Function<Double[], ItemStack> item;
        private final Inventory inventory;

        public Graph(Inventory inventory, DoubleSupplier[] currentStates, Function<Double[], ItemStack> item) {
            this.inventory = inventory;
            this.currentStates = currentStates;
            this.item = item;

            for (int i = 0; i < COLLECT_WIDTH; i++) {
                this.states[i] = -1;
            }
        }

        @Override
        public void run() {
            double[] states = new double[COLLECT_WIDTH];
            double min = -1, max = 0;

            for (int i = 1; i < COLLECT_WIDTH + 1; i++) {
                double state = i < this.states.length ? this.states[i] : this.currentStates[0].getAsDouble();
                if (state > max) {
                    max = state;
                }
                if (state < min || min == -1) {
                    min = state;
                }
                states[i - 1] = state;
            }

            // evaluating the grade difference by the average between min and max values
            // adding 0.1, so we are bound to lower value when rounding to nearest grade
            double diff = (max - min) / 5 + .01;

            double other = this.currentStates[1].getAsDouble();
            // start iterating over the columns and placing grades
            ItemStack prevItem = null;
            for (int i = 0; i < WIDTH; i++) {
                double state = states[COLLECT_WIDTH - WIDTH + i];
                int grade = -1;
                if (state != -1) {
                    double d = (state - min) / diff;
                    grade = (int) Math.floor(d);
                }

                // remove graph items for this column at last tick
                if (this.states[COLLECT_WIDTH - WIDTH - 1 + i] == -1) {
                    setSlots(i, 1, null);
                } else {
                    setSlots(i, columns[i], null);
                }

                ItemStack item = this.item.apply(new Double[] { Math.max(0, state), Math.max(0, other) });
                // smooth out graph
                if (grade != -1 && i > 0 && Math.abs(getHighestGrade(columns[i - 1]) - grade - 1) > 1) {
                    int lowestPrev = getLowestGrade(columns[i - 1]);
                    int highestPrev = getHighestGrade(columns[i - 1]);

                    grade++; // make 1-index based
                    int g = 0, current = grade, prev = columns[i - 1];
                    // mask grades
                    for (;;) {
                        if (current < lowestPrev && current != grade) {
                            // mask prev grade if decrease
                            columns[i - 1] = (realify(columns[i - 1]) << 4) | current;
                        } else {
                            g = (g << 4) | current;
                        }

                        if (grade > highestPrev) {
                            current--;
                            if (current == highestPrev) {
                                break;
                            }
                        } else if (grade < lowestPrev) {
                            current++;
                            if (current == lowestPrev) {
                                break;
                            }
                        }
                    }

                    if (prev != columns[i - 1]) {
                        setSlots(i - 1, columns[i - 1], prevItem);
                    }

                    columns[i] = g > 0 ? g : grade;
                } else if (grade != -1) {
                    columns[i] = grade + 1; // make 1-index based
                } else {
                    columns[i] = -1;
                }

                if (grade != -1) {
                    // finally set the graph slots for this column
                    setSlots(i, columns[i], item);
                } else {
                    setSlots(i, 1, item);
                }

                prevItem = item;
            }

            // update new states after ticking
            this.states = states;
        }

        private int realify(int val) {
            return val == -1 ? 1 : val;
        }

        private int getLowestGrade(int val) {
            int d = val, prev = 1, j = 0;
            while (d > 1) {
                prev = d & GRADE_MASK;
                d >>= (4 * ++j);
            }

            return prev;
        }

        private int getHighestGrade(int val) {
            return realify(val) & GRADE_MASK;
        }

        private int getSlot(int i, int grade) {
            return WIDTH * (4 - Math.min(4, grade - 1)) + i;
        }

        private void setSlots(int i, int column, ItemStack item) {
            int g = column;
            for (;;) {
                int grade = g & GRADE_MASK;
                if (grade == 0) {
                    break;
                }
                g >>= 4;

                // set the item for this graph
                int slot = getSlot(i, grade);
                inventory.setItem(slot, item);
            }
        }
    }

    @Override
    public void loadItems() {
        for (int i = 0; i < 9; i++) {
            getInventory().setItem(i + 9 * 5, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        super.loadItems();
    }

    @Override
    public void updateItems() {

    }

    private void updateDataSlot(ItemStack itemStack) {
        ItemStack item = itemStack.clone();
        item.setType(Material.LIME_WOOL);
        getInventory().setItem(49, item);
    }

    @Override
    public void onClose(Player player, boolean anyViewersLeft) {
        if (!anyViewersLeft) {
            for (BukkitTask task : this.tasks) {
                task.cancel();
            }

            getMechanic().setHasGraph(false);
        }

        super.onClose(player, anyViewersLeft);
    }

    protected List<GuiElement> getGuiElements() {
        return Arrays.asList(Elements.DELETE, Elements.MEMBERS, Elements.UPGRADE, ACTIVATE_BUTTON);
    }
}
