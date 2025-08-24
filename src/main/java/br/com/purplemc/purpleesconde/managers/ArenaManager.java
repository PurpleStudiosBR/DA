package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.arena.ArenaState;
import br.com.purplemc.purpleesconde.map.GameMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {

    private final PurpleEsconde plugin;
    private final Map<String, List<Arena>> mapArenas;
    private final Map<Player, Arena> playerArenas;
    private final Map<String, Integer> mapArenaAmount;

    public ArenaManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.mapArenas = new ConcurrentHashMap<String, List<Arena>>();
        this.playerArenas = new ConcurrentHashMap<Player, Arena>();
        this.mapArenaAmount = new ConcurrentHashMap<String, Integer>();
        loadArenas();
    }

    private void loadArenas() {
        mapArenas.clear();
        for (GameMap map : plugin.getMapManager().getMaps()) {
            int amount = mapArenaAmount.getOrDefault(map.getName(), 1);
            List<Arena> arenas = new ArrayList<Arena>();
            for (int i = 1; i <= amount; i++) {
                String arenaId = map.getName() + "_" + i;
                arenas.add(new Arena(plugin, arenaId, map));
            }
            mapArenas.put(map.getName(), arenas);
        }
    }

    public void addArenas(String mapName, int quantity) {
        mapArenaAmount.put(mapName, quantity);
        GameMap gameMap = plugin.getMapManager().getMap(mapName);
        if (gameMap != null) {
            mapArenas.remove(mapName);
            List<Arena> arenas = new ArrayList<Arena>();
            for (int i = 1; i <= quantity; i++) {
                String arenaId = mapName + "_" + i;
                arenas.add(new Arena(plugin, arenaId, gameMap));
            }
            mapArenas.put(mapName, arenas);
        }
    }

    public Arena getAvailableArena(String mapName) {
        List<Arena> arenas = mapArenas.get(mapName);
        if (arenas != null) {
            Arena least = null;
            for (Arena a : arenas) {
                if (a.getState() == ArenaState.WAITING && !a.isFull()) {
                    if (least == null || a.getPlayers().size() < least.getPlayers().size()) {
                        least = a;
                    }
                }
            }
            if (least != null) return least;
        }
        GameMap map = plugin.getMapManager().getMap(mapName);
        if (map != null) {
            Arena arena = new Arena(plugin, mapName + "_" + (getArenasByMap(mapName).size() + 1), map);
            mapArenas.computeIfAbsent(mapName, k -> new ArrayList<Arena>()).add(arena);
            return arena;
        }
        return null;
    }

    public Arena createArena(GameMap map) {
        String mapName = map.getName();
        List<Arena> arenas = mapArenas.computeIfAbsent(mapName, k -> new ArrayList<Arena>());
        String arenaId = mapName + "_" + (arenas.size() + 1);
        Arena arena = new Arena(plugin, arenaId, map);
        arenas.add(arena);
        return arena;
    }

    public void addPlayerToArena(Player player, Arena arena) {
        if (playerArenas.containsKey(player)) {
            removePlayerFromArena(player);
        }
        playerArenas.put(player, arena);
        arena.addPlayer(player);
        player.setFlying(false);
        player.setAllowFlight(false);

        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().setWaitingLobbyScoreboard(player, arena);
        }
    }

    public void removePlayerFromArena(Player player) {
        Arena arena = playerArenas.remove(player);
        if (arena != null) {
            arena.removePlayer(player);

            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().removePlayerScoreboard(player);
            }
        }
    }

    public Arena getPlayerArena(Player player) {
        return playerArenas.get(player);
    }

    public boolean isPlayerInArena(Player player) {
        return playerArenas.containsKey(player);
    }

    public void removeArena(String arenaId) {
        for (List<Arena> arenas : mapArenas.values()) {
            arenas.removeIf(a -> a.getId().equals(arenaId));
        }
    }

    public List<Arena> getArenas() {
        List<Arena> all = new ArrayList<Arena>();
        for (List<Arena> list : mapArenas.values()) {
            all.addAll(list);
        }
        return all;
    }

    public List<Arena> getArenasByMap(String mapName) {
        return mapArenas.getOrDefault(mapName, Collections.emptyList());
    }

    public Arena getRandomArena() {
        List<Arena> available = new ArrayList<Arena>();
        for (List<Arena> arenas : mapArenas.values()) {
            for (Arena arena : arenas) {
                if (arena.getState() == ArenaState.WAITING && !arena.isFull()) {
                    available.add(arena);
                }
            }
        }
        if (available.isEmpty()) {
            List<GameMap> maps = plugin.getMapManager().getMaps();
            if (!maps.isEmpty()) {
                GameMap map = maps.get(new Random().nextInt(maps.size()));
                return createArena(map);
            }
            return null;
        }
        return available.get(new Random().nextInt(available.size()));
    }

    public void sendToMainLobby(Player player) {
        Location mainLobby = plugin.getConfigManager().getMainLobby();
        if (mainLobby != null) {
            player.teleport(mainLobby);
            player.getInventory().clear();
            player.setGameMode(GameMode.ADVENTURE);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setFlying(false);
            player.setAllowFlight(false);

            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().removePlayerScoreboard(player);
            }

            if (plugin.getConfigManager().giveLobbyItems()) {
                plugin.getConfigManager().giveLobbyItems(player);
            }
        } else {
            player.sendMessage("§cLobby principal não configurado!");
        }
    }

    public int getArenasByMapCount(String mapName) {
        return getArenasByMap(mapName).size();
    }

    public void cleanup() {
        for (Arena arena : getArenas()) {
            arena.cleanup();
        }
        mapArenas.clear();
        playerArenas.clear();
        plugin.getLogger().info("Todas as arenas foram limpas");
    }
}