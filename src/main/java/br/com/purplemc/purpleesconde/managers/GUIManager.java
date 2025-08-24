package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.map.GameMap;
import br.com.purplemc.purpleesconde.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GUIManager implements Listener {

    private final PurpleEsconde plugin;

    public GUIManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMapSelector(Player player) {
        List<GameMap> maps = plugin.getMapManager().getMaps();
        Inventory inv = Bukkit.createInventory(null, 54, "§8Selecionar mapa - Modo Solo");

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
        int mapIndex = 0;

        for (int slot : slots) {
            if (mapIndex >= maps.size()) break;
            GameMap map = maps.get(mapIndex);

            ItemStack item = new ItemStack(Material.MAP);
            ItemMeta meta = item.getItemMeta();

            boolean isFavorite = plugin.getMapManager().isPlayerFavorite(player, map.getName());

            meta.setDisplayName("§e" + map.getName());

            List<String> lore = new ArrayList<String>();
            lore.add("§7Em espera: §a" + getWaitingPlayers(map));
            lore.add("§7Jogando: §c" + getPlayingPlayers(map));
            lore.add("");

            if (isFavorite) {
                lore.add("§6★ Mapa Favorito");
                lore.add("");
            }

            if (plugin.getMapManager().canPlayerSelectMap(player)) {
                lore.add("§aClique para jogar");
            } else {
                lore.add("§cVocê já selecionou um mapa hoje!");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot, item);
            mapIndex++;
        }

        inv.setItem(48, ItemUtils.createGUIItem(Material.PAPER, "§aInformações",
                Arrays.asList("§7Clique Direito para Favoritar um mapa", "§7Clique Esquerdo para Selecionar um mapa")));

        inv.setItem(49, ItemUtils.createGUIItem(Material.ARROW, "§cVoltar", null));

        inv.setItem(50, ItemUtils.createGUIItem(Material.DIAMOND, "§bMapa Favorito Aleatório",
                Arrays.asList("§7Clique para jogar em um", "§7mapa favorito aleatório")));

        player.openInventory(inv);
    }

    @EventHandler
    public void onMapSelectorClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase("§8Selecionar mapa - Modo Solo")) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory().getType() != InventoryType.CHEST) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 48) {
            if (event.isLeftClick() ^ event.isRightClick()) {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        player.sendMessage("§aClique Direito para Favoritar um mapa\n§aClique Esquerdo para Selecionar um mapa");
                    }
                });
            }
            return;
        }
        if (slot == 49) {
            if (event.isLeftClick() ^ event.isRightClick()) {
                player.closeInventory();
            }
            return;
        }
        if (slot == 50) {
            if (event.isLeftClick() ^ event.isRightClick()) {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (!plugin.getMapManager().hasPlayerFavorites(player)) {
                            player.sendMessage("§cVocê não tem nenhum mapa favorito.");
                            return;
                        }
                        GameMap randomFav = plugin.getMapManager().getRandomFavoriteMap(player);
                        if (randomFav == null) {
                            player.sendMessage("§cNão foi possível encontrar um mapa favorito válido.");
                            return;
                        }
                        if (plugin.getMapManager().getMaps().isEmpty()) {
                            player.sendMessage("§aNenhuma sala disponível no momento");
                            return;
                        }
                        plugin.getGameManager().joinGame(player, randomFav.getName());
                    }
                });
            }
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.MAP) return;
        String mapName = clicked.getItemMeta().getDisplayName().replace("§e", "");
        GameMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) {
            player.sendMessage("§cMapa inválido ou não encontrado.");
            return;
        }
        if (plugin.getMapManager().getMaps().isEmpty()) {
            player.sendMessage("§aNenhuma sala disponível no momento");
            return;
        }
        if (event.isRightClick() && !event.isLeftClick()) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (plugin.getMapManager().isPlayerFavorite(player, mapName)) {
                        plugin.getMapManager().removeFavorite(player, mapName);
                        player.sendMessage("§cMapa removido dos favoritos: §e" + mapName);
                    } else {
                        plugin.getMapManager().addFavorite(player, mapName);
                        player.sendMessage("§aMapa adicionado aos favoritos: §e" + mapName);
                    }
                    openMapSelector(player);
                }
            });
        } else if (event.isLeftClick() && !event.isRightClick()) {
            if (!plugin.getMapManager().canPlayerSelectMap(player)) {
                player.sendMessage("§cVocê já selecionou um mapa hoje!");
                return;
            }
            plugin.getGameManager().joinGame(player, mapName);
            plugin.getMapManager().markMapSelected(player);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onMapSelectorDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase("§8Selecionar mapa - Modo Solo")) {
            event.setCancelled(true);
        }
    }

    public void openBedWarsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Esconde Esconde");

        inv.setItem(12, ItemUtils.createGUIItem(Material.ENDER_PEARL, "§aJogar Esconde Esconde",
                Arrays.asList("§7Em espera: §a0", "§7Jogando: §c0", "", "§aClique para jogar")));

        inv.setItem(14, ItemUtils.createGUIItem(Material.SIGN, "§eSelecionar mapa (Padrão)",
                Arrays.asList("§7Selecione seu mapa favorito.", "", "§aClique para selecionar")));

        player.openInventory(inv);
    }

    public void openGameMapInfo(Player player, String mapName) {
        GameMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, "§8" + mapName + " - Modo Padrão");

        ItemStack mapItem = new ItemStack(Material.MAP);
        ItemMeta meta = mapItem.getItemMeta();
        meta.setDisplayName("§e" + mapName);

        List<String> lore = new ArrayList<String>();
        lore.add("§7Modo Padrão");
        lore.add("");
        lore.add("§7Salas disponíveis: §a" + getAvailableRooms(map));
        lore.add("");
        lore.add("§aClique para Jogar");

        meta.setLore(lore);
        mapItem.setItemMeta(meta);

        inv.setItem(13, mapItem);

        inv.setItem(18, ItemUtils.createGUIItem(Material.ARROW, "§cVoltar", null));

        player.openInventory(inv);
    }

    private int getWaitingPlayers(GameMap map) {
        int count = 0;
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getGameMap() != null && arena.getGameMap().getName().equals(map.getName())) {
                count += arena.getWaitingPlayers().size();
            }
        }
        return count;
    }

    private int getPlayingPlayers(GameMap map) {
        int count = 0;
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getGameMap() != null && arena.getGameMap().getName().equals(map.getName()) && arena.getGame() != null) {
                count += arena.getPlayers().size();
            }
        }
        return count;
    }

    private int getAvailableRooms(GameMap map) {
        int waitingRooms = 0;
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getGameMap() != null && arena.getGameMap().getName().equals(map.getName()) && arena.getGame() == null) {
                waitingRooms++;
            }
        }
        return Math.max(1, waitingRooms);
    }

    public boolean isMapSelector(Inventory inventory) {
        return inventory.getTitle().contains("Selecionar mapa");
    }

    public boolean isBedWarsMenu(Inventory inventory) {
        return inventory.getTitle().contains("Esconde Esconde");
    }

    public boolean isGameMapInfo(Inventory inventory) {
        String title = inventory.getTitle();
        return title.contains("Modo Padrão") &&
                !title.contains("Selecionar") &&
                !title.contains("Esconde Esconde");
    }
}