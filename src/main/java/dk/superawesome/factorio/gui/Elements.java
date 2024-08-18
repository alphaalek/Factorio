package dk.superawesome.factorio.gui;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.MechanicRemoveEvent;
import dk.superawesome.factorio.mechanics.AccessibleMechanic;
import dk.superawesome.factorio.mechanics.Management;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.stream.Collectors;

public class Elements {

    public static GuiElement UPGRADE = new GuiElement() {
        @Override
        public void handle(InventoryClickEvent event, Player player, MechanicGui<?, ?> gui) {
            event.setCancelled(true);
            player.sendMessage("§cDet er ikke muligt at opgradere maskinen.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.5f);
        }

        @Override
        public ItemStack getItem() {
            return new ItemBuilder(Material.WRITABLE_BOOK).setName("§eOpgradér Maskine").build();
        }
    };

    public static GuiElement MEMBERS = new GuiElement() {

        private static final String TITLE = "Konfigurér Medlemmer";
        private static final int SIZE = 36;

        @Override
        public void handle(InventoryClickEvent event, Player player, MechanicGui<?, ?> gui) {
            if (gui.getMechanic() instanceof AccessibleMechanic accessible) {
                player.openInventory(createGui(gui.getMechanic(), accessible).getInventory());
            }
        }

        private <G extends BaseGui<G>> BaseGui<G> createGui(Mechanic<?> mechanic, AccessibleMechanic accesible) {
            return new PaginatedGui<G, OfflinePlayer>(new BaseGui.InitCallbackHolder(), null, SIZE, TITLE, true, 3 * 9 - 1) {

                {
                    // call init callback when loaded
                    initCallback.call();
                }

                @Override
                public List<OfflinePlayer> getValues() {
                    List<OfflinePlayer> members = new ArrayList<>();
                    members.add(Bukkit.getOfflinePlayer(mechanic.getManagement().getOwner()));
                    mechanic.getManagement().getMembers().stream()
                            .map(Bukkit::getOfflinePlayer)
                            .sorted(Collections.reverseOrder(Comparator.comparing(p -> Optional.ofNullable(p.getName()).orElse(""))))
                            .forEach(members::add);

                    return members;
                }

                @Override
                public ItemStack getItemFrom(OfflinePlayer player) {
                    ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) stack.getItemMeta();
                    if (meta != null) {
                        meta.setOwningPlayer(player);
                        meta.setDisplayName(player.getName());
                        stack.setItemMeta(meta);
                    }

                    return stack;
                }

                @Override
                public void onClose(Player player, boolean anyViewersLeft) {
                    Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                        if (player.isOnline() && !player.getOpenInventory().getType().isCreatable()) {
                            accesible.openInventory(mechanic, player);
                        }
                    });
                }

                protected void loadView() {
                    super.loadView();

                    int i = -1;
                    while (getInventory().getItem(++i) != null) {
                        // empty body
                    }
                    getInventory().setItem(i, new ItemBuilder(Material.NAME_TAG).setName("§eTilføj Medlem").build());
                }

                @Override
                public boolean onClickIn(InventoryClickEvent event) {
                    ItemStack item = event.getCurrentItem();
                    if (item != null && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                        ((Player)event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.5f);
                    }

                    // check access to modify members
                    Player player = (Player) event.getWhoClicked();
                    if (item != null && (item.getType() == Material.NAME_TAG || item.getType() == Material.PLAYER_HEAD)) {
                        if (!mechanic.getManagement().hasAccess(player, Management.MODIFY_MEMBERS)) {
                            player.sendMessage("§cDu har ikke adgang til at ændre på medlemmerne af maskinen!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.5f);
                            return true;
                        }
                    }

                    // add member
                    if (item != null && item.getType() == Material.NAME_TAG) {
                        // actions performed when closing the sign gui
                        List<SignGUIAction> post = Arrays.asList(
                                SignGUIAction.openInventory(Factorio.get(), getInventory()), SignGUIAction.runSync(Factorio.get(), this::loadView));

                        // open a sign gui where the player can input the name of the added member on line 1 + 2
                        SignGUI gui = SignGUI.builder()
                                .setLines("", "", "---------------", "Vælg spillernavn")
                                .setHandler((p, result) -> {
                                    String name = (result.getLine(0).trim() + result.getLine(1).trim());
                                    if (!name.isEmpty()) {
                                        List<OfflinePlayer> members = getValues();
                                        // check if this player is already a member
                                        if (members.stream()
                                                .map(OfflinePlayer::getName)
                                                .filter(Objects::nonNull)
                                                .anyMatch(name::equalsIgnoreCase)) {
                                            player.sendMessage("§cSpilleren " + name + " er allerede medlem af maskinen!");
                                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                                            return post;
                                        }

                                        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                                        // check if the target player is valid
                                        if (!target.hasPlayedBefore()) {
                                            player.sendMessage("§cKunne ikke finde spilleren " + name + "!");
                                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                                            return post;
                                        }

                                        // finally, add this player as a member of the mechanic
                                        mechanic.getManagement().getMembers().add(target.getUniqueId());
                                        player.sendMessage("§eDu tilføjede spilleren " + name + " som medlem af maskinen.");
                                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.35f, 0.5f);
                                    }

                                    return post;
                                })
                                .build();
                        gui.open(player);
                    }

                    // remove member
                    if (item != null && item.getType() == Material.PLAYER_HEAD && item.hasItemMeta()) {
                        String name = item.getItemMeta().getDisplayName();
                        UUID uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
                        mechanic.getManagement().getMembers().remove(uuid);
                        loadView();

                        player.sendMessage("§eDu fjernede spilleren " + name + " som medlem fra maskinen!");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    }

                    super.onClickIn(event);

                    return true;
                }
            };
        }

        @Override
        public ItemStack getItem() {
            return new ItemBuilder(Material.NAME_TAG).setName("§eKonfigurér Medlemmer").build();
        }
    };

    public static GuiElement DELETE = new GuiElement() {
        @Override
        public void handle(InventoryClickEvent event, Player player, MechanicGui<?, ?> gui) {
            event.setCancelled(true);
            player.closeInventory();

            Mechanic<?> mechanic = gui.getMechanic();
            // check if the player has access to remove this mechanic
            if (!mechanic.getManagement().hasAccess(player, Management.DELETE)) {
                player.sendMessage("§cDu har ikke adgang til at fjerne maskinen!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                return;
            }

            // check if this mechanic has any items in its inventory
            if (mechanic instanceof ItemCollection && !((ItemCollection) mechanic).isTransferEmpty()
                    || mechanic instanceof Container && !((Container<?>) mechanic).isContainerEmpty()) {
                player.sendMessage("§cRyd maskinens inventar før du sletter den!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                return;
            }

            // call mechanic remove event to event handlers
            MechanicRemoveEvent removeEvent = new MechanicRemoveEvent(player, mechanic);
            Bukkit.getPluginManager().callEvent(removeEvent);
            if (removeEvent.isCancelled()) {
                // this event was cancelled. (why though?)
                return;
            }

            Factorio.get().getMechanicManager(player.getWorld()).removeMechanic(player, mechanic);
        }

        @Override
        public ItemStack getItem() {
            return new ItemBuilder(Material.RED_WOOL).setName("§cSlet Maskine").build();
        }
    };
}
