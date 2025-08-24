package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.arena.ArenaState;
import br.com.purplemc.purpleesconde.map.GameMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {

    private final PurpleEsconde plugin;
    private final Map<String, Arena> arenas;
    private final Map<Player, Arena> playerArenas;

    public ArenaManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.arenas = new ConcurrentHashMap<>();
        this.playerArenas = new ConcurrentHashMap<>();
    }

    public Arena createArena(GameMap gameMap) {
        String arenaId = generateArenaId(gameMap.getName());
        Arena arena = new Arena(plugin, arenaId, gameMap);
        arenas.put(arenaId, arena);
        return arena;
    }

    public Arena createArenaWithId(GameMap gameMap, String arenaId) {
        Arena arena = new Arena(plugin, arenaId, gameMap);
        arenas.put(arenaId, arena);
        return arena;
    }

    public Arena getAvailableArena(String mapName) {
        return arenas.values().stream()
                .filter(arena -> arena.getGameMap().getName().equals(mapName))
                .filter(arena -> arena.getState() == ArenaState.WAITING)
                .filter(arena -> !arena.isFull())
                .findFirst()
                .orElse(null);
    }

    public Arena getRandomArena() {
        List<Arena> availableArenas = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.getState() == ArenaState.WAITING && !arena.isFull()) {
                availableArenas.add(arena);
            }
        }
        if (availableArenas.isEmpty()) {
            GameMap randomMap = plugin.getMapManager().getRandomMap();
            if (randomMap != null) {
                return createArena(randomMap);
            }
            return null;
        }
        return availableArenas.get(new Random().nextInt(availableArenas.size()));
    }

    public void addPlayerToArena(Player player, Arena arena) {
        if (playerArenas.containsKey(player)) {
            removePlayerFromArena(player);
        }
        playerArenas.put(player, arena);
        arena.addPlayer(player);
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    public void removePlayerFromArena(Player player) {
        Arena arena = playerArenas.remove(player);
        if (arena != null) {
            arena.removePlayer(player);
            if (arena.getPlayers().isEmpty() && arena.getState() != ArenaState.INGAME) {
                removeArena(arena.getId());
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
        Arena arena = arenas.remove(arenaId);
        if (arena != null) {
            arena.cleanup();
            for (Player player : arena.getPlayers()) {
                playerArenas.remove(player);
            }
        }
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public Arena getArena(String id) {
        return arenas.get(id);
    }

    public void cleanup() {
        for (Arena arena : new ArrayList<>(arenas.values())) {
            arena.cleanup();
        }
        arenas.clear();
        playerArenas.clear();
    }

    private String generateArenaId(String mapName) {
        String baseId = mapName + "_";
        int counter = 1;
        while (arenas.containsKey(baseId + counter)) {
            counter++;
        }
        return baseId + counter;
    }

    public void sendToMainLobby(Player player) {
        Location mainLobby = plugin.getConfigManager().getMainLobby();
        if (mainLobby != null) {
            player.teleport(mainLobby);
            player.getInventory().clear();
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setFlying(false);
            player.setAllowFlight(false);
            if (plugin.getConfigManager().giveLobbyItems()) {
                plugin.getConfigManager().giveLobbyItems(player);
            }
        }
    }
}