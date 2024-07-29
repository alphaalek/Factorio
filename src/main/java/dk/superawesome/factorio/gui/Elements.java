package dk.superawesome.factorio.gui;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.Management;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.items.Container;
import dk.superawesome.factorio.mechanics.items.ItemCollection;
import dk.superawesome.factorio.util.db.Types;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Elements {

    public static GuiElement UPGRADE = new GuiElement() {
        @Override
        public void handle(InventoryClickEvent event, Player player, MechanicGui<?, ?> gui) {
            event.setCancelled(true);
        }

        @Override
        public ItemStack getItem() {
            return new ItemStack(Material.WRITABLE_BOOK);
        }
    };

    public static GuiElement MEMBERS = new GuiElement() {

        private static final String TITLE = "Konfigurér Medlemmer";
        private static final int SIZE = 45;
        private static final int PAGE_SIZE = 9;

        @Override
        public void handle(InventoryClickEvent event, Player player, MechanicGui<?, ?> gui) {
            player.openInventory(createGui(gui.getMechanic()).getInventory());
        }

        private <G extends BaseGui<G>> BaseGui<G> createGui(Mechanic<?, ?> mechanic) {
            return new BaseGuiAdapter<G>(new BaseGui.InitCallbackHolder(), null, SIZE, TITLE, true) {

                {
                    // load the items when the gui is loaded
                    loadItems();
                }

                private int currentPage = 1;

                @Override
                public void loadItems() {
                    for (int i = 0; i < SIZE - PAGE_SIZE; i++) {
                        getInventory().setItem(SIZE - i - 1, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
                    }

                    loadView();
                }

                private List<OfflinePlayer> getMembers() {
                    return mechanic.getManagement().getMembers().stream()
                            .map(Bukkit::getOfflinePlayer)
                            .sorted(Collections.reverseOrder(Comparator.comparing(p -> Optional.ofNullable(p.getName()).orElse(""))))
                            .collect(Collectors.toList());
                }

                private void loadView() {
                    // clear inventory view for new items
                    IntStream.range(0, PAGE_SIZE).forEach(i -> getInventory().setItem(i, null));

                    // evaluate pages
                    int skip = Math.max(0, (currentPage - 1) * PAGE_SIZE);
                    int i = 0;
                    int displayed = 0;
                    List<OfflinePlayer> members = getMembers();
                    while (i < members.size()) {
                        OfflinePlayer player = members.get(i);
                        // skip first pages
                        if (i++ < skip || player == null) {
                            continue;
                        }

                        // show until page ends
                        if (++displayed > PAGE_SIZE) {
                            break;
                        }

                        // display the player head
                        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) stack.getItemMeta();
                        if (meta != null) {
                            meta.setOwningPlayer(player);
                            meta.setDisplayName(player.getName());
                            stack.setItemMeta(meta);
                            getInventory().setItem(displayed - 1, stack);
                        }
                    }

                    getInventory().setItem(displayed, new ItemStack(Material.NAME_TAG));

                    // set arrows for previous/next page buttons
                    getInventory().setItem(PAGE_SIZE, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
                    getInventory().setItem(PAGE_SIZE + 8, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));

                    if (members.size() > i) {
                        getInventory().setItem(PAGE_SIZE, new ItemStack(Material.ARROW));
                    }
                    if (currentPage > 1) {
                        getInventory().setItem(PAGE_SIZE + 8, new ItemStack(Material.ARROW));
                    }
                }

                @Override
                public boolean onClickIn(InventoryClickEvent event) {
                    ItemStack item = event.getCurrentItem();
                    if (item != null && item.getType() == Material.ARROW) {
                        if (event.getSlot() % 9 == 1) {
                            currentPage--;
                        } else if (event.getSlot() % 9 == 8) {
                            currentPage++;
                        }

                        loadView();
                    }

                    Player player = (Player) event.getWhoClicked();
                    if (item != null && item.getType() == Material.NAME_TAG) {
                        if (mechanic.getManagement().hasAccess(player.getUniqueId(), Management.MODIFY_MEMBERS)) {
                            player.sendMessage("§cDu har ikke adgang til at ændre på medlemmerne af maskinen!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.5f);
                            return true;
                        }

                        // actions performed when closing the sign gui
                        List<SignGUIAction> post = Arrays.asList(
                                SignGUIAction.openInventory(Factorio.get(), getInventory()), SignGUIAction.runSync(Factorio.get(), this::loadView));

                        // open a sign gui where the player can input the name of the added member on line 1 + 2
                        SignGUI gui = SignGUI.builder()
                                .setLines("", "", "---------------", "Vælg spillernavn")
                                .setHandler((p, result) -> {
                                    String name = (result.getLine(0).trim() + result.getLine(1).trim());
                                    List<OfflinePlayer> members = getMembers();
                                    // check if this player is already a member
                                    if (
                                        // check owner
                                        (Optional.ofNullable(mechanic.getManagement().getOwner())
                                            .map(Bukkit::getOfflinePlayer)
                                            .map(OfflinePlayer::getName)
                                            .orElse("").equalsIgnoreCase(name)
                                        ||
                                        // check members
                                        members.stream()
                                            .map(OfflinePlayer::getName)
                                            .filter(Objects::nonNull)
                                            .anyMatch(name::equalsIgnoreCase))) {
                                        player.sendMessage("§cSpilleren " + name + " er allerede medlem af maskinen!");
                                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.5f);
                                        return post;
                                    }

                                    OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                                    // check if the target player is valid
                                    if (!target.hasPlayedBefore()) {
                                        player.sendMessage("§cKunne ikke finde spilleren " + name + "!");
                                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.5f);
                                        return post;
                                    }

                                    // finally, add this player as a member of the mechanic
                                    mechanic.getManagement().getMembers().add(target.getUniqueId());
                                    player.sendMessage("§eDu tilføjede spilleren " + name + " som medlem af maskinen.");
                                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.35f, 0.5f);

                                    return post;
                                })
                                .build();
                        gui.open(player);
                    }

                    return true;
                }
            };
        }

        @Override
        public ItemStack getItem() {
            return new ItemStack(Material.NAME_TAG);
        }
    };

    public static GuiElement DELETE = new GuiElement() {
        @Override
        public void handle(InventoryClickEvent event, Player player, MechanicGui<?, ?> gui) {
            event.setCancelled(true);
            player.closeInventory();

            Mechanic<?, ?> mechanic = gui.getMechanic();
            // check if the player has access to remove this mechanic
            if (!mechanic.getManagement().hasAccess(player.getUniqueId(), Management.DELETE)) {
                player.sendMessage("§cDu har ikke adgang til at fjerne denne maskine!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.5f);
                return;
            }

            // check if this mechanic has any items in its inventory
            if (mechanic instanceof ItemCollection && !((ItemCollection)mechanic).isEmpty()
                    || mechanic instanceof Container && !((Container)mechanic).isContainerEmpty()) {
                player.sendMessage("§cRyd maskinens inventar før du sletter den!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.5f);
                return;
            }

            // unload and delete this mechanic
            Factorio.get().getMechanicManager(player.getWorld()).unload(mechanic);
            try {
                Factorio.get().getContextProvider().deleteAt(mechanic.getLocation());
            } catch (SQLException ex) {
                player.sendMessage("§cDer opstod en fejl! Kontakt en staff.");
                Factorio.get().getLogger().log(Level.SEVERE, "Failed to delete mechanic at location " + mechanic.getLocation(), ex);
                return;
            }
            Buildings.remove(player.getWorld(), mechanic);

            // player stuff
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.6f);
            player.sendMessage("§eDu fjernede maskinen " + mechanic.getProfile().getName() + " (Lvl " + mechanic.getLevel() + ") ved " + Types.LOCATION.convert(mechanic.getLocation()) + ".");
        }

        @Override
        public ItemStack getItem() {
            return new ItemStack(Material.RED_WOOL);
        }
    };
}
