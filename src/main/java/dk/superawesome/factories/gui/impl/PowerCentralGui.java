package dk.superawesome.factories.gui.impl;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.gui.MechanicGui;
import dk.superawesome.factories.mechanics.impl.PowerCentral;
import dk.superawesome.factories.util.helper.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

public class PowerCentralGui extends MechanicGui<PowerCentralGui, PowerCentral> {

    private static Graph getConsumptionGraph(Inventory inventory, PowerCentral mechanic) {
        return new Graph(inventory,
                () -> mechanic.pollRecentConsumption() * 2,
                e -> new ItemBuilder(Material.LIGHT_GRAY_WOOL).setName("Forbrug: " + e + "W").setLore(new String[]{"Energi: " + mechanic.getEnergy() + "J"}).build()
        );
    }

    private static Graph getProductionGraph(Inventory inventory, PowerCentral mechanic) {
        return new Graph(inventory,
                () -> mechanic.pollRecentProduction() * 2,
                e -> new ItemBuilder(Material.GRAY_WOOL).setName("Produktion: " + e + "W").setLore(new String[]{"Energi: " + mechanic.getEnergy() + "J"}).build()
        );
    }

    private final List<BukkitTask> tasks = new ArrayList<>();

    public PowerCentralGui(PowerCentral mechanic, AtomicReference<PowerCentralGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder(), "Power Central (Capacity: " + mechanic.getCapacity() + "J)");
        initCallback.call();

        //this.tasks.add(
        //        Bukkit.getScheduler().runTaskTimer(Factories.get(), getProductionGraph(getInventory(), getMechanic()), 0L, 10L));
        this.tasks.add(
                Bukkit.getScheduler().runTaskTimer(Factories.get(), getConsumptionGraph(getInventory(), getMechanic()), 0L, 10L));

        getMechanic().setHasGraph(true);
    }

    private static class Graph implements Runnable {

        private static final int WIDTH = 9;
        private static final int COLLECT_WIDTH = 12;
        private static final int GRADE_SHIFT = ~0 & 0x0F;

        private final int[] columns = new int[WIDTH];
        private double[] states = new double[COLLECT_WIDTH];

        private final DoubleSupplier currentState;
        private final Function<Double, ItemStack> item;
        private final Inventory inventory;

        public Graph(Inventory inventory, DoubleSupplier currentState, Function<Double, ItemStack> item) {
            this.inventory = inventory;
            this.currentState = currentState;
            this.item = item;

            for (int i = 0; i < COLLECT_WIDTH; i++) {
                this.states[i] = -1;
            }
        }

        @Override
        public void run() {
            double[] states = new double[COLLECT_WIDTH];
            double min = -1;
            double max = 0;
            for (int i = 1; i < COLLECT_WIDTH + 1; i++) {
                double state = i < this.states.length ? this.states[i] : this.currentState.getAsDouble();
                if (state > max) {
                    max = state;
                }
                if (state < min || min == -1) {
                    min = state;
                }
                states[i - 1] = state;
            }
            this.states = states;

            // evaluating the grade difference by the average between min and max values
            // adding 0.1, so we are bound to lower value when rounding to nearest grade
            double diff = (max - min) / 5 + .1;

            // start iterating over the columns and placing grades
            for (int i = 0; i < WIDTH; i++) {
                double state = states[COLLECT_WIDTH - WIDTH + i];
                int grade = 2;
                if (state != -1 && max > min) {
                    double d = (state - min) / diff;
                    grade = (int) Math.round(d);
                }

                ItemStack item = this.item.apply(state);
                boolean smoothed = false;
                if (state != -1) {
                    setSlots(i, null);

                    if (i > 0 && columns[i - 1] != grade) {
                        int lowestPrev = columns[i - 1] & GRADE_SHIFT;
                        int highestPrev = getHighestGrade(columns[i - 1]);

                        // check if we have to smooth out the graph
                        if (grade < lowestPrev || grade > highestPrev) {
                            int g = 0;
                            int current = grade;
                            // ensure correct low-to-high
                            if (grade > highestPrev) {
                                current = highestPrev + 1;
                            }

                            // mask grades
                            for (;;) {
                                g = (g << 4) | current;

                                if (grade > highestPrev && current == grade) {
                                    break;
                                } else if (grade < lowestPrev && lowestPrev - current <= 1) {
                                    break;
                                }

                                current++;
                            }

                            columns[i] = g;
                            smoothed = true;
                        }
                    }
                }

                // this column wasn't smoothed out, just use the original grade
                if (!smoothed) {
                    columns[i] = grade;
                }

                // finally set the graph slots for this column
                if (state != -1) {
                    setSlots(i, item);
                }
            }
        }

        private int getHighestGrade(int val) {
            int d = val;
            int prev = 0, j = 0;
            while (d > 0) {
                prev = d & GRADE_SHIFT;
                d >>= (4 * ++j);
            }

            return prev;
        }

        private int getSlot(int i, int grade) {
            return WIDTH * (4 - Math.min(4, grade)) + i;
        }

        private void setSlots(int i, ItemStack item) {
            int g = columns[i];
            int j = 0;
            for (;;) {
                int grade = g & GRADE_SHIFT;
                if (grade == 0 && j > 0) {
                    break;
                }
                g >>= (4 * ++j);

                // check if this slot is used by the graph if it's being removed
                int slot = getSlot(i, grade);
                ItemStack itemAt = inventory.getItem(slot);
                if (item == null
                        && itemAt != null
                        && itemAt.getType() != this.item.apply(0d).getType()) {
                    continue;
                }

                // set the item for this graph
                inventory.setItem(slot, item);
            }
        }
    }

    @Override
    public void loadItems() {
        for (int i = 0; i < 9; i++) {
            getInventory().setItem(i + 9 * 5, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }
        super.loadItems();

        getMechanic().setEnergy(getMechanic().getEnergy() + 10);
    }

    @Override
    public void loadInputOutputItems() {

    }

    @Override
    public void onClose() {
        for (BukkitTask task : this.tasks) {
            task.cancel();
        }

        getMechanic().setHasGraph(false);
    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        return true;
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        return true;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        return true;    
    }

    @Override
    public void onClickPost(InventoryClickEvent event) {

    }
}
