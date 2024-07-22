package dk.superawesome.factories.gui.impl;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.gui.MechanicGui;
import dk.superawesome.factories.mechanics.impl.PowerCentral;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleSupplier;

public class PowerCentralGui extends MechanicGui<PowerCentralGui, PowerCentral> {

    private final BukkitTask task;

    public PowerCentralGui(PowerCentral mechanic, AtomicReference<PowerCentralGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();

        this.task = Bukkit.getScheduler().runTaskTimer(Factories.get(), new Graph(getInventory(), () -> getMechanic().getEnergy()), 0L, 10L);
       // Bukkit.getScheduler().runTaskTimer(Factories.get(), () -> getMechanic().setEnergy(getMechanic().getEnergy() + 10), 0L, 20L);
    }

    private static class Graph implements Runnable {

        private static final int GRADE_SHIFT = ~0 & 0x0F;

        private final int[] columns = new int[9];
        private double[] states = new double[9];

        private final DoubleSupplier currentState;
        private final Inventory inventory;

        public Graph(Inventory inventory, DoubleSupplier currentState) {
            this.inventory = inventory;
            this.currentState = currentState;

            for (int i = 0; i < 9; i++) {
                this.states[i] = -1;
            }
        }

        @Override
        public void run() {
            double[] states = new double[9];

            double min = -1;
            double max = 0;
            for (int i = 1; i < 10; i++) {
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

            double diff = (max - min) / 5 + .1; // adding .1 so we are bound to lower value

            for (int i = 0; i < 9; i++) {
                double state = states[i];
                int grade = 2;
                if (state != -1 && max > min) {
                    double d = (state - min) / diff;
                    grade = (int) Math.round(d);
                }

                boolean smoothed = false;
                if (state != -1) {
                    setSlots(i, null);

                    // check if we have to smooth out the graph
                    if (i > 0 && columns[i - 1] != grade) {
                        int g = 0;
                        for (int j = grade; j < grade + Math.abs((columns[i - 1] & GRADE_SHIFT) - grade); j++) {
                            Bukkit.getLogger().info(j + "       " + grade + " " + Math.abs((columns[i - 1] & GRADE_SHIFT) - grade));
                            g = (g << 4) | j;

                            // fix cluster graph
                            // check if the previous slot has any item, AND the previous column has multiple grades
                            if (inventory.getItem(getSlot(i - 1, j)) != null && columns[i - 1] != j) {
                                inventory.setItem(getSlot(i - 1, j), null);
                            }
                        }
                        columns[i] = g;
                        smoothed = true;
                    }
                }

                if (!smoothed) {
                    columns[i] = grade;
                }

                // finally set the graph slots for this column
                if (state != -1) {
                    setSlots(i, new ItemStack(Material.GRAY_WOOL));
                }
            }
        }

        private int getSlot(int i, int grade) {
            return 9 * (4 - Math.min(4, grade)) + i;
        }

        private void setSlots(int i, ItemStack item) {
            int g = columns[i];
            int j = 0;
            for (;;) {
                int grade = g & GRADE_SHIFT;
                if (grade == 0 && j > 0) {
                    break;
                }

                inventory.setItem(getSlot(i, grade), item);
                g >>= (4 * ++j);
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
        this.task.cancel();
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
