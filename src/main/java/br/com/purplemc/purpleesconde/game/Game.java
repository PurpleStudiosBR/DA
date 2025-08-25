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

            // LIMPAR TUDO ANTES DE EQUIPAR
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);

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

        // Night vision para escondedores
        for (Player hider : hiders) {
            hider.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1, false, false));
        }

        updateNickAndGlow();
    }

    private void equipSeeker(Player player) {
        player.getInventory().setArmorContents(ItemUtils.getSeekerArmor());
        player.getInventory().setItem(0, ItemUtils.getSeekerWeapon());
        // Aplicar cosméticos
        if (plugin.getCosmeticManager() != null) {
            plugin.getCosmeticManager().applySeekerCosmetics(player);
        }
    }

    private void equipHider(Player player) {
        player.getInventory().setArmorContents(ItemUtils.getHiderArmor());
        player.getInventory().setItem(0, ItemUtils.getHiderWeapon());
        player.getInventory().setItem(1, ItemUtils.getFirework());
        // Aplicar cosméticos
        if (plugin.getCosmeticManager() != null) {
            plugin.getCosmeticManager().applyHiderCosmetics(player);
        }
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
            // Contar kill para quem matou
            Player killer = player.getKiller();
            if (killer != null && seekers.contains(killer)) {
                if (plugin.getLevelManager() != null) {
                    plugin.getLevelManager().addKill(killer);
                }

                // Dar coins por kill
                if (economy != null) {
                    int coinsKill = 10;
                    economy.depositPlayer(killer.getName(), coinsKill);
                    killer.sendMessage("§6+" + coinsKill + " coins §7(Eliminação!)");
                }
            }

            hiders.remove(player);
            seekers.add(player);
            convertedSeekers.add(player);

            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    // LIMPAR TUDO antes de equipar como procurador
                    player.getInventory().clear();
                    player.getInventory().setArmorContents(new ItemStack[4]);

                    equipSeeker(player);
                    player.teleport(arena.getGameMap().getSeekerSpawn());
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1, false, false));
                }
            }, 10L);

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
                    try {
                        player.spigot().respawn();
                    } catch (Exception e) {
                        player.setHealth(20.0);
                    }
                    equipSeeker(player);
                    player.teleport(arena.getGameMap().getSeekerSpawn());
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1, false, false));
                }
            }, 10L);
        }
    }

    // Método para remover jogador do jogo (quando sai da partida)
    public void removePlayer(Player player) {
        boolean wasInGame = false;

        if (hiders.contains(player)) {
            hiders.remove(player);
            wasInGame = true;
            player.sendMessage("§cVocê saiu da partida!");

            // Se não há mais escondedores e há procuradores, procuradores vencem
            if (hiders.isEmpty() && !seekers.isEmpty()) {
                MessageUtils.broadcastToArena(arena, "§cTodos os escondedores saíram! Procuradores venceram!");
                endGame(GameEndReason.ALL_CAUGHT);
                return;
            }
        } else if (seekers.contains(player)) {
            seekers.remove(player);
            wasInGame = true;
            player.sendMessage("§cVocê saiu da partida!");
        }

        if (spectators.contains(player)) {
            removeSpectator(player);
        }

        if (wasInGame) {
            MessageUtils.broadcastToArena(arena, "§e" + player.getName() + " §csaiu da partida.");
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

            // Limpar tudo dos jogadores
            for (Player player : arena.getPlayers()) {
                player.getInventory().clear();
                player.getInventory().setArmorContents(new ItemStack[4]);
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }

            // Mostrar resultados
            if (reason == GameEndReason.ALL_CAUGHT) {
                for (Player player : arena.getPlayers()) {
                    if (isMainSeeker(player)) {
                        MessageUtils.sendTitle(player, "§a§lVITÓRIA", "§fVocê venceu como Procurador!", 10, 60, 10);
                    } else {
                        MessageUtils.sendTitle(player, "§c§lDERROTA", "§fVocê perdeu!", 10, 60, 10);
                    }
                }
            } else if (reason == GameEndReason.TIME_UP) {
                for (Player player : arena.getPlayers()) {
                    if (hiders.contains(player)) {
                        MessageUtils.sendTitle(player, "§a§lVITÓRIA", "§fVocê venceu como Escondedor!", 10, 60, 10);
                    } else {
                        MessageUtils.sendTitle(player, "§c§lDERROTA", "§fVocê perdeu!", 10, 60, 10);
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
        Set<Player> winners = new HashSet<Player>();
        Set<Player> losers = new HashSet<Player>();

        if (seekersWin) {
            winners.add(originalSeeker);
            losers.addAll(hiders);
            for (Player s : seekers) {
                if (!isMainSeeker(s)) {
                    losers.add(s);
                }
            }
        } else {
            winners.addAll(hiders);
            losers.addAll(seekers);
        }

        // DAR XP E COINS PARA VENCEDORES
        for (Player winner : winners) {
            if (plugin.getLevelManager() != null) {
                plugin.getLevelManager().giveXP(winner, 100);
            }
            if (plugin.getDatabaseManager() != null) {
                plugin.getDatabaseManager().addWin(winner);
            }

            // DAR COINS PARA VENCEDORES
            if (economy != null) {
                int coinsWin = 50;
                economy.depositPlayer(winner.getName(), coinsWin);
                winner.sendMessage("§a+100 XP §7(Vitória!) §6+" + coinsWin + " coins");
            } else {
                winner.sendMessage("§a+100 XP §7(Vitória!)");
            }
        }

        // DAR XP E COINS PARA PERDEDORES
        for (Player loser : losers) {
            if (plugin.getLevelManager() != null) {
                plugin.getLevelManager().giveXP(loser, 25);
            }
            if (plugin.getDatabaseManager() != null) {
                plugin.getDatabaseManager().addLoss(loser);
            }

            // DAR COINS PARA PERDEDORES
            if (economy != null) {
                int coinsLoss = 15;
                economy.depositPlayer(loser.getName(), coinsLoss);
                loser.sendMessage("§e+25 XP §7(Participação) §6+" + coinsLoss + " coins");
            } else {
                loser.sendMessage("§e+25 XP §7(Participação)");
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

        // Esconder escondedores de jogadores que NÃO estão na partida
        for (Player hider : hiders) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!arena.getPlayers().contains(target)) {
                    target.hidePlayer(hider);
                }
            }
        }

        // Mostrar procuradores para todos
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
        seekersTeam.setCanSeeFriendlyInvisibles(true);

        hidersTeam = scoreboard.getTeam("hiders");
        if (hidersTeam == null) hidersTeam = scoreboard.registerNewTeam("hiders");
        hidersTeam.setPrefix(ChatColor.GRAY.toString());
        hidersTeam.setCanSeeFriendlyInvisibles(false);

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
        player.setCustomNameVisible(true);
    }

    private void setHiderVisual(Player player) {
        player.setPlayerListName("§7Escondido");
        player.setDisplayName("§7Escondido");

        // ESCONDER APENAS O NAMETAG - ARMADURA CONTINUA VISÍVEL
        player.setCustomNameVisible(false);
        player.setCustomName("");

        // NÃO aplicar efeito de invisibilidade para manter armadura visível e hitbox
    }

    private void resetNickAndGlow(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setPlayerListName(player.getName());
        player.setDisplayName(player.getName());
        player.setCustomName(player.getName());
        player.setCustomNameVisible(true);

        // Garantir remoção de efeitos
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);

        for (Player target : Bukkit.getOnlinePlayers()) {
            target.showPlayer(player);
        }
    }

    public void handleFireworkUse(Player player) {
        if (!isHider(player)) return;

        long now = System.currentTimeMillis();
        long last = lastFireworkUse.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < 30000) {
            long left = (30000 - (now - last)) / 1000;
            MessageUtils.sendActionBar(player, "§cAguarde " + left + "s para usar outro fogo de artifício!");
            return;
        }

        lastFireworkUse.put(player.getUniqueId(), now);

        if (economy != null) {
            int coinsFirework = 5;
            economy.depositPlayer(player.getName(), coinsFirework);
            MessageUtils.sendActionBar(player, "§6+" + coinsFirework + " coins §7(Fogo de Artifício)");
        }

        player.sendMessage("§aFogo de artifício usado! §7Próximo em 30s.");
    }

    // Getters
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
}