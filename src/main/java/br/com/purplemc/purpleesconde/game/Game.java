package br.com.purplemc.purpleesconde.game;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.utils.ItemUtils;
import br.com.purplemc.purpleesconde.utils.MessageUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class Game {

    private final PurpleEsconde plugin;
    private final Arena arena;
    private final Set<Player> seekers;
    private final Set<Player> hiders;
    private final Set<Player> spectators;
    private Player originalSeeker;
    private GameState state;
    private BukkitTask gameTask;
    private int timeLeft;
    private int hidingTime;
    private BukkitTask actionBarTask;
    private BukkitTask countdownTask;
    private Scoreboard scoreboard;
    private Team seekersTeam;
    private Team hidersTeam;
    private final Set<Player> convertedSeekers = new HashSet<Player>();
    private BukkitTask seekerReleaseTask;
    private boolean seekersReleased = false;
    private boolean rewardsGiven = false;
    private final Map<UUID, Long> lastFireworkUse = new HashMap<UUID, Long>();
    private Economy economy;

    public Game(PurpleEsconde plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.seekers = new HashSet<Player>();
        this.hiders = new HashSet<Player>();
        this.spectators = new HashSet<Player>();
        this.state = GameState.WAITING;
        this.timeLeft = plugin.getConfigManager().getGameDuration();
        this.hidingTime = plugin.getConfigManager().getHidingTime();
        this.rewardsGiven = false;
        if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            this.economy = plugin.getServer().getServicesManager().getRegistration(Economy.class) == null ? null : plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        }
    }

    public void start() {
        if (arena.getPlayers().size() < 2) {
            return;
        }
        state = GameState.STARTING;
        selectRoles();
        prepareGame();
        startHidingCountdown();
    }

    private void selectRoles() {
        List<Player> players = new ArrayList<Player>(arena.getPlayers());
        Collections.shuffle(players);
        originalSeeker = players.get(0);
        seekers.add(originalSeeker);
        for (int i = 1; i < players.size(); i++) {
            hiders.add(players.get(i));
        }
        MessageUtils.broadcastToArena(arena, plugin.getConfigManager().getMessage("game.roles-selected"));
    }

    private void prepareGame() {
        for (Player player : arena.getPlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.getInventory().clear();
            if (seekers.contains(player)) {
                equipSeeker(player);
                player.teleport(arena.getGameMap().getSeekerSpawn());
            } else {
                equipHider(player);
                player.teleport(getRandomHiderSpawn());
            }
            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().setGameScoreboard(player, this);
            }
        }
        for (Player hider : hiders) {
            hider.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1, false, false));
        }
        updateNickAndGlow();
    }

    private void equipSeeker(Player player) {
        player.getInventory().setArmorContents(ItemUtils.getSeekerArmor());
        player.getInventory().setItem(0, ItemUtils.getSeekerWeapon());
    }

    private void equipHider(Player player) {
        player.getInventory().setArmorContents(ItemUtils.getHiderArmor());
        player.getInventory().setItem(0, ItemUtils.getHiderWeapon());
        player.getInventory().setItem(1, ItemUtils.getFirework());
    }

    private void startHidingCountdown() {
        state = GameState.HIDING;
        seekersReleased = false;
        final int[] countdown = {hidingTime};
        for (Player seeker : seekers) {
            seeker.sendMessage(plugin.getConfigManager().getMessage("game.wait-hiding"));
            seeker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * hidingTime, 99, true, false));
            seeker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * hidingTime, 255, true, false));
        }
        for (Player hider : hiders) {
            hider.sendMessage(plugin.getConfigManager().getMessage("game.hide-now"));
        }
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (countdown[0] <= 0) {
                    countdownTask.cancel();
                    releaseSeekers();
                    startGamePhase();
                    return;
                }
                if (countdown[0] <= 10) {
                    for (Player player : arena.getPlayers()) {
                        player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
                    }
                }
                for (Player seeker : seekers) {
                    MessageUtils.sendActionBar(seeker, "§eAguarde " + countdown[0] + "s para procurar!");
                }
                for (Player hider : hiders) {
                    MessageUtils.sendActionBar(hider, "§eA partida começa em: §c" + countdown[0] + "s");
                }
                countdown[0]--;
            }
        }, 0L, 20L);
    }

    private void releaseSeekers() {
        seekersReleased = true;
        for (Player seeker : seekers) {
            seeker.sendMessage("§aVocê pode procurar agora!");
            seeker.removePotionEffect(PotionEffectType.BLINDNESS);
            seeker.removePotionEffect(PotionEffectType.SLOW);
            seeker.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1, false, false));
        }
    }

    private void startGamePhase() {
        state = GameState.INGAME;
        MessageUtils.broadcastToArena(arena, plugin.getConfigManager().getMessage("game.start"));
        startActionBar();
        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                timeLeft--;
                if (plugin.getScoreboardManager() != null) {
                    for (Player p : arena.getPlayers()) {
                        plugin.getScoreboardManager().setGameScoreboard(p, Game.this);
                    }
                }
                if (timeLeft <= 0) {
                    endGame(GameEndReason.TIME_UP);
                } else if (hiders.isEmpty()) {
                    endGame(GameEndReason.ALL_CAUGHT);
                }
            }
        }, 20L, 20L);
    }

    public void onPlayerDeath(final Player player) {
        if (hiders.contains(player)) {
            hiders.remove(player);
            seekers.add(player);
            convertedSeekers.add(player);
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    equipSeeker(player);
                    player.teleport(arena.getGameMap().getSeekerSpawn());
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1, false, false));
                }
            }, 60L);
            if (plugin.getLevelManager() != null) {
                plugin.getLevelManager().addKill(player);
            }
            player.sendMessage(plugin.getConfigManager().getMessage("game.became-seeker"));
            MessageUtils.broadcastToArena(arena,
                    plugin.getConfigManager().getMessage("game.player-became-seeker")
                            .replace("{player}", player.getName()));
            if (hiders.isEmpty()) {
                endGame(GameEndReason.ALL_CAUGHT);
            }
            updateNickAndGlow();
        } else if (seekers.contains(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    player.spigot().respawn();
                    equipSeeker(player);
                    player.teleport(arena.getGameMap().getSeekerSpawn());
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1, false, false));
                }
            }, 60L);
        }
    }

    public void addSpectator(Player player) {
        spectators.add(player);
        if (plugin.getSpectatorManager() != null) {
            plugin.getSpectatorManager().setSpectator(player, arena);
        }
    }

    public void removeSpectator(Player player) {
        spectators.remove(player);
        if (plugin.getSpectatorManager() != null) {
            plugin.getSpectatorManager().removeSpectator(player);
        }
    }

    private void endGame(GameEndReason reason) {
        state = GameState.ENDING;
        if (gameTask != null) {
            gameTask.cancel();
        }
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        if (!rewardsGiven) {
            rewardsGiven = true;
            giveRewards(reason == GameEndReason.ALL_CAUGHT);
            for (Player player : arena.getPlayers()) {
                player.getInventory().setArmorContents(new ItemStack[4]);
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
            if (reason == GameEndReason.ALL_CAUGHT) {
                for (Player player : arena.getPlayers()) {
                    if (isMainSeeker(player)) {
                        MessageUtils.sendTitle(player, "§aVITÓRIA", "§fVocê venceu como Procurador!", 10, 60, 10);
                    } else if (seekers.contains(player)) {
                        MessageUtils.sendTitle(player, "§cDERROTA", "§fVocê perdeu!", 10, 60, 10);
                    } else {
                        MessageUtils.sendTitle(player, "§cDERROTA", "§fVocê perdeu!", 10, 60, 10);
                    }
                }
            } else if (reason == GameEndReason.TIME_UP) {
                for (Player player : arena.getPlayers()) {
                    if (hiders.contains(player)) {
                        MessageUtils.sendTitle(player, "§aVITÓRIA", "§fVocê venceu como Escondedor!", 10, 60, 10);
                    } else {
                        MessageUtils.sendTitle(player, "§cDERROTA", "§fVocê perdeu!", 10, 60, 10);
                    }
                }
            }
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    for (Player player : arena.getPlayers()) {
                        resetNickAndGlow(player);
                        plugin.getArenaManager().sendToMainLobby(player);
                        if (plugin.getScoreboardManager() != null) {
                            plugin.getScoreboardManager().setLobbyScoreboard(player);
                        }
                    }
                    arena.reset();
                }
            }, 100L);
        }
    }

    private void giveRewards(boolean seekersWin) {
        Set<Player> winners;
        Set<Player> losers = new HashSet<Player>();
        if (seekersWin) {
            winners = new HashSet<Player>();
            winners.add(originalSeeker);
            for (Player s : seekers) {
                if (!isMainSeeker(s)) {
                    losers.add(s);
                }
            }
            losers.addAll(hiders);
        } else {
            winners = new HashSet<Player>(hiders);
            losers.addAll(seekers);
        }
        for (Player winner : winners) {
            if (plugin.getLevelManager() != null) {
                plugin.getLevelManager().giveXP(winner, 50);
            }
            if (plugin.getDatabaseManager() != null) {
                plugin.getDatabaseManager().addWin(winner);
            }
        }
        for (Player loser : losers) {
            if (plugin.getLevelManager() != null) {
                plugin.getLevelManager().giveXP(loser, 10);
            }
            if (plugin.getDatabaseManager() != null) {
                plugin.getDatabaseManager().addLoss(loser);
            }
        }
        for (Player player : arena.getPlayers()) {
            if (plugin.getDatabaseManager() != null) {
                plugin.getDatabaseManager().addGame(player.getUniqueId());
            }
        }
    }

    private void startActionBar() {
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                int total = seekers.size() + hiders.size();
                double seekersPercent = total > 0 ? (100.0 * seekers.size() / total) : 0;
                double hidersPercent = total > 0 ? (100.0 * hiders.size() / total) : 0;
                String percStr = "§cProcuradores: §f" + String.format("%.0f", seekersPercent) + "% §7| §aEscondedores: §f" + String.format("%.0f", hidersPercent) + "%";
                for (Player player : arena.getPlayers()) {
                    String timeMessage = MessageUtils.formatTime(timeLeft);
                    MessageUtils.sendActionBar(player, percStr + " §f| Tempo: §a" + timeMessage);
                }
            }
        }, 0L, 20L);
    }

    private Location getRandomHiderSpawn() {
        List<Location> spawns = arena.getGameMap().getHiderSpawns();
        return spawns.get(new Random().nextInt(spawns.size()));
    }

    private void updateNickAndGlow() {
        setupTeams();
        for (Player p : arena.getPlayers()) {
            if (isHider(p)) {
                setHiderVisual(p);
            } else if (isSeeker(p)) {
                setSeekerVisual(p);
            } else {
                resetNickAndGlow(p);
            }
        }
        for (Player hider : hiders) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.equals(hider)) target.hidePlayer(hider);
            }
        }
        for (Player seeker : seekers) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                target.showPlayer(seeker);
            }
        }
    }

    private void setupTeams() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        seekersTeam = scoreboard.getTeam("seekers");
        if (seekersTeam == null) seekersTeam = scoreboard.registerNewTeam("seekers");
        seekersTeam.setPrefix(ChatColor.RED.toString());
        hidersTeam = scoreboard.getTeam("hiders");
        if (hidersTeam == null) hidersTeam = scoreboard.registerNewTeam("hiders");
        hidersTeam.setPrefix(ChatColor.GRAY.toString());

        for (Player op : Bukkit.getOnlinePlayers()) {
            seekersTeam.removePlayer(op);
            hidersTeam.removePlayer(op);
        }
        for (Player p : seekers) seekersTeam.addPlayer(p);
        for (Player p : hiders) hidersTeam.addPlayer(p);
        for (Player p : arena.getPlayers()) p.setScoreboard(scoreboard);
    }

    private void setSeekerVisual(Player player) {
        player.setPlayerListName(ChatColor.RED + player.getName());
        player.setDisplayName(ChatColor.RED + player.getName());
    }

    private void setHiderVisual(Player player) {
        player.setPlayerListName("§7Escondido");
        player.setDisplayName("§7Escondido");
        player.setCustomName(player.getName());
        player.setCustomNameVisible(true);
    }

    private void resetNickAndGlow(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setPlayerListName(player.getName());
        player.setDisplayName(player.getName());
        player.setCustomName(player.getName());
        player.setCustomNameVisible(true);
        for (Player target : Bukkit.getOnlinePlayers()) {
            target.showPlayer(player);
        }
    }

    public boolean isMainSeeker(Player player) {
        return player != null && player.equals(originalSeeker);
    }

    public GameState getState() {
        return state;
    }

    public Set<Player> getSeekers() {
        return seekers;
    }

    public Set<Player> getHiders() {
        return hiders;
    }

    public Set<Player> getSpectators() {
        return spectators;
    }

    public boolean isSeeker(Player player) {
        return seekers.contains(player);
    }

    public boolean isHider(Player player) {
        return hiders.contains(player);
    }

    public boolean isSpectator(Player player) {
        return spectators.contains(player);
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public Arena getArena() {
        return arena;
    }

    public void handleFireworkUse(Player player) {
        if (!isHider(player)) return;
        long now = System.currentTimeMillis();
        long last = lastFireworkUse.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 10000) {
            long left = (10000 - (now - last)) / 1000;
            MessageUtils.sendActionBar(player, "§cAguarde " + left + "s para usar outro fogo de artifício!");
            return;
        }
        lastFireworkUse.put(player.getUniqueId(), now);
        if (economy != null) {
            economy.depositPlayer(player.getName(), 1.0);
            MessageUtils.sendActionBar(player, ChatColor.translateAlternateColorCodes('&', "&6+1 Coins"));
        }
    }
}