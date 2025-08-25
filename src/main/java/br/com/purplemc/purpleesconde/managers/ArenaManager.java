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
        this.mapArenas = new ConcurrentHashMap<>();
        this.playerArenas = new ConcurrentHashMap<>();
        this.mapArenaAmount = new ConcurrentHashMap<>();
        loadArenaAmountsFromConfig();
    }

    private void loadArenaAmountsFromConfig() {
        if (plugin.getConfigManager().getMaps().contains("arena-amounts")) {
            for (String mapName : plugin.getConfigManager().getMaps().getConfigurationSection("arena-amounts").getKeys(false)) {
                int amount = plugin.getConfigManager().getMaps().getInt("arena-amounts." + mapName, 1);
                mapArenaAmount.put(mapName, amount);
            }
        }
    }

    private void saveArenaAmountsToConfig() {
        for (Map.Entry<String, Integer> entry : mapArenaAmount.entrySet()) {
            plugin.getConfigManager().getMaps().set("arena-amounts." + entry.getKey(), entry.getValue());
        }
        plugin.getConfigManager().saveConfig(plugin.getConfigManager().getMaps(), "maps.yml");
    }

    public void loadArenas() {
        mapArenas.clear();
        if (plugin.getMapManager() == null) return;
        for (GameMap map : plugin.getMapManager().getMaps()) {
            int amount = mapArenaAmount.getOrDefault(map.getName(), 1);
            List<Arena> arenas = new ArrayList<>();
            for (int i = 1; i <= amount; i++) {
                String arenaId = map.getName() + "_" + i;

                // Criar cópia independente do GameMap para cada arena
                GameMap independentMap = createIndependentMapCopy(map, i);

                arenas.add(new Arena(plugin, arenaId, independentMap));
            }
            mapArenas.put(map.getName(), arenas);
        }
    }

    public void addArenas(String mapName, int quantity) {
        mapArenaAmount.put(mapName, quantity);
        saveArenaAmountsToConfig();
        GameMap gameMap = plugin.getMapManager().getMap(mapName);
        if (gameMap != null) {
            // Remover arenas existentes
            mapArenas.remove(mapName);

            List<Arena> arenas = new ArrayList<>();
            for (int i = 1; i <= quantity; i++) {
                String arenaId = mapName + "_" + i;

                // Criar cópia independente do GameMap para cada arena
                GameMap independentMap = createIndependentMapCopy(gameMap, i);

                Arena arena = new Arena(plugin, arenaId, independentMap);
                arenas.add(arena);
            }
            mapArenas.put(mapName, arenas);
        }
    }

    private GameMap createIndependentMapCopy(GameMap original, int arenaNumber) {
        // Criar offsets para separar as arenas no mesmo mundo
        // Cada arena fica distante 1000 blocks da anterior
        int offsetX = (arenaNumber - 1) * 1000;
        int offsetZ = (arenaNumber - 1) * 1000;

        Location newWaitingLobby = original.getWaitingLobby().clone().add(offsetX, 0, offsetZ);
        Location newSeekerSpawn = original.getSeekerSpawn().clone().add(offsetX, 0, offsetZ);

        List<Location> newHiderSpawns = new ArrayList<>();
        for (Location spawn : original.getHiderSpawns()) {
            newHiderSpawns.add(spawn.clone().add(offsetX, 0, offsetZ));
        }

        Location newBarrierMin = original.getBarrierMin() != null ?
                original.getBarrierMin().clone().add(offsetX, 0, offsetZ) : null;
        Location newBarrierMax = original.getBarrierMax() != null ?
                original.getBarrierMax().clone().add(offsetX, 0, offsetZ) : null;

        return new GameMap(
                original.getName() + "_arena" + arenaNumber,
                original.getWorld(),
                newWaitingLobby,
                newSeekerSpawn,
                newHiderSpawns,
                newBarrierMin,
                newBarrierMax
        );
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
            // Criar nova arena com offset único
            int nextArenaNumber = getArenasByMap(mapName).size() + 1;
            GameMap independentMap = createIndependentMapCopy(map, nextArenaNumber);
            Arena arena = new Arena(plugin, mapName + "_" + nextArenaNumber, independentMap);
            mapArenas.computeIfAbsent(mapName, k -> new ArrayList<>()).add(arena);
            return arena;
        }
        return null;
    }

    public Arena createArena(GameMap map) {
        String mapName = map.getName();
        List<Arena> arenas = mapArenas.computeIfAbsent(mapName, k -> new ArrayList<>());

        // Criar nova arena com offset único
        int nextArenaNumber = arenas.size() + 1;
        String arenaId = mapName + "_" + nextArenaNumber;

        GameMap independentMap = createIndependentMapCopy(map, nextArenaNumber);
        Arena arena = new Arena(plugin, arenaId, independentMap);
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

            // Se estava em jogo, remover do jogo também
            if (arena.getGame() != null) {
                arena.getGame().removePlayer(player);
            }

            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().setLobbyScoreboard(player);
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
        List<Arena> all = new ArrayList<>();
        for (List<Arena> list : mapArenas.values()) {
            all.addAll(list);
        }
        return all;
    }

    public List<Arena> getArenasByMap(String mapName) {
        return mapArenas.getOrDefault(mapName, Collections.emptyList());
    }

    public Arena getRandomArena() {
        List<Arena> available = new ArrayList<>();
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
            player.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
            player.setGameMode(GameMode.ADVENTURE);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setFlying(false);
            player.setAllowFlight(false);

            // Limpar efeitos
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOW);

            // Resetar visual
            player.setCustomNameVisible(true);
            player.setCustomName(player.getName());
            player.setPlayerListName(player.getName());
            player.setDisplayName(player.getName());

            // Mostrar para todos
            for (Player target : org.bukkit.Bukkit.getOnlinePlayers()) {
                target.showPlayer(player);
            }

            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().setLobbyScoreboard(player);
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