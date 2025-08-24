package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.managers.GUIManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    private final PurpleEsconde plugin;
    private final GUIManager guiManager;

    public InventoryListener(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.guiManager = new GUIManager(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (plugin.getArenaManager().isPlayerInArena(player)) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            if (arena.getGame() == null) {
                event.setCancelled(true);
                return;
            }
        }

        if (guiManager.isMapSelector(event.getInventory())) {
            event.setCancelled(true);
            handleMapSelectorClick(player, event);
        } else if (guiManager.isBedWarsMenu(event.getInventory())) {
            event.setCancelled(true);
            handleBedWarsMenuClick(player, event);
        } else if (guiManager.isGameMapInfo(event.getInventory())) {
            event.setCancelled(true);
            handleGameMapInfoClick(player, event);
        }
    }

    private void handleMapSelectorClick(Player player, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (item.getType() == Material.MAP) {
            String mapName = item.getItemMeta().getDisplayName().substring(2);

            if (event.getClick().isRightClick()) {
                if (player.hasPermission("purpleesconde.favorite")) {
                    if (plugin.getMapManager().isPlayerFavorite(player, mapName)) {
                        plugin.getMapManager().removeFavorite(player, mapName);
                    } else {
                        plugin.getMapManager().addFavorite(player, mapName);
                    }
                    guiManager.openMapSelector(player);
                }
            } else {
                if (plugin.getMapManager().canPlayerSelectMap(player)) {
                    Arena arena = plugin.getArenaManager().getAvailableArena(mapName);
                    if (arena == null) {
                        arena = plugin.getArenaManager().createArena(plugin.getMapManager().getMap(mapName));
                    }
                    plugin.getArenaManager().addPlayerToArena(player, arena);
                    plugin.getMapManager().markMapSelected(player);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("map.daily-limit"));
                }
                player.closeInventory();
            }
        } else if (item.getType() == Material.DIAMOND) {
            Arena arena = plugin.getArenaManager().getRandomArena();
            if (arena == null) {
                arena = plugin.getArenaManager().createArena(plugin.getMapManager().getRandomFavoriteMap(player));
            }
            if (arena != null) {
                plugin.getArenaManager().addPlayerToArena(player, arena);
                player.closeInventory();
            }
        } else if (item.getType() == Material.ARROW) {
            guiManager.openBedWarsMenu(player);
        }
    }

    private void handleBedWarsMenuClick(Player player, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        if (item.getType() == Material.ENDER_PEARL) {
            Arena arena = plugin.getArenaManager().getRandomArena();
            if (arena != null) {
                plugin.getArenaManager().addPlayerToArena(player, arena);
                player.closeInventory();
            }
        } else if (item.getType() == Material.SIGN) {
            guiManager.openMapSelector(player);
        }
    }

    private void handleGameMapInfoClick(Player player, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        if (item.getType() == Material.MAP) {
            String mapName = item.getItemMeta().getDisplayName().substring(2);
            Arena arena = plugin.getArenaManager().getAvailableArena(mapName);
            if (arena == null) {
                arena = plugin.getArenaManager().createArena(plugin.getMapManager().getMap(mapName));
            }
            plugin.getArenaManager().addPlayerToArena(player, arena);
            player.closeInventory();
        } else if (item.getType() == Material.ARROW) {
            guiManager.openMapSelector(player);
        }
    }
}