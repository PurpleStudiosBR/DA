package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.game.Game;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScoreboardManager {

    private final PurpleEsconde plugin;
    private final Map<Player, Scoreboard> playerScoreboards;
    private final Set<String> usedBlankLines = new HashSet<>();

    public ScoreboardManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
    }

    public void setLobbyScoreboard(Player player) {
        displayScoreboard(player, "lobby", null, null);
    }

    public void setWaitingLobbyScoreboard(Player player, Arena arena) {
        displayScoreboard(player, "waiting", arena, null);
    }

    public void setGameScoreboard(Player player, Game game) {
        displayScoreboard(player, "game", null, game);
    }

    private void displayScoreboard(Player player, String type, Arena arena, Game game) {
        List<String> lines = plugin.getConfigManager().getScoreboardLines(type);
        String title = plugin.getConfigManager().getScoreboardTitle(type);
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective(type, "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(colorize(title != null ? title : "§aScore"));

        int score = lines.size();
        usedBlankLines.clear();
        for (String line : lines) {
            String processedLine = line;

            if (type.equals("lobby")) {
                processedLine = replaceLobbyPlaceholders(player, processedLine);
            } else if (type.equals("waiting") && arena != null) {
                processedLine = replaceWaitingPlaceholders(player, arena, processedLine);
            } else if (type.equals("game") && game != null) {
                processedLine = replaceGamePlaceholders(player, game, processedLine);
            }

            processedLine = applyAllPlaceholders(player, colorize(processedLine));
            processedLine = fixBlankLine(processedLine, score);
            obj.getScore(processedLine).setScore(score--);
        }
        player.setScoreboard(scoreboard);
        playerScoreboards.put(player, scoreboard);
    }

    public void removePlayerScoreboard(Player player) {
        playerScoreboards.remove(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void updateWaitingScoreboard(Arena arena) {
        for (Player player : arena.getPlayers()) {
            setWaitingLobbyScoreboard(player, arena);
        }
    }

    public void updateGameScoreboard(Game game) {
        for (Player player : game.getArena().getPlayers()) {
            setGameScoreboard(player, game);
        }
    }

    private String replaceLobbyPlaceholders(Player player, String line) {
        int wins = 0;
        int losses = 0;
        int games = 0;
        int level = 0;
        int kills = 0;
        String levelDisplay = "";
        String progressBar = "";
        String xp = "";

        if (plugin.getDatabaseManager() != null) {
            wins = plugin.getDatabaseManager().getWins(player.getUniqueId());
            losses = plugin.getDatabaseManager().getLosses(player.getUniqueId());
            games = plugin.getDatabaseManager().getGames(player.getUniqueId());
        }
        if (plugin.getLevelManager() != null) {
            level = plugin.getLevelManager().getPlayerLevel(player);
            kills = plugin.getLevelManager().getPlayerKills(player);
            levelDisplay = plugin.getLevelManager().getLevelDisplay(player);
            progressBar = plugin.getLevelManager().getProgressBar(player);
            xp = plugin.getLevelManager().getXPInfo(player);
        }

        return line.replace("{wins}", String.valueOf(wins))
                .replace("{losses}", String.valueOf(losses))
                .replace("{games}", String.valueOf(games))
                .replace("{level}", levelDisplay)
                .replace("{xp_bar}", progressBar)
                .replace("{xp}", xp)
                .replace("{kills}", String.valueOf(kills))
                .replace("{players}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max_players}", String.valueOf(plugin.getConfigManager().getMaxPlayersPerArena()))
                .replace("{map}", "-")
                .replace("{state}", "Lobby")
                .replace("{time}", "-");
    }

    private String replaceWaitingPlaceholders(Player player, Arena arena, String line) {
        String status;
        if (arena.getPlayers().size() < plugin.getConfigManager().getMinPlayersToStart()) {
            status = "§fStatus: §eEsperando...";
        } else if (arena.getState().name().equalsIgnoreCase("STARTING")) {
            status = "Iniciando em §f" + arena.getCountdown() + "s";
        } else {
            status = "§fStatus: §ePreparando...";
        }
        return line.replace("{map}", arena.getGameMap().getName())
                .replace("{players}", String.valueOf(arena.getPlayers().size()))
                .replace("{max_players}", String.valueOf(plugin.getConfigManager().getMaxPlayersPerArena()))
                .replace("{status}", status);
    }

    private String replaceGamePlaceholders(Player player, Game game, String line) {
        int seekers = game.getSeekers().size();
        int hiders = game.getHiders().size();
        String tempo = formatGameTime(game.getTimeLeft());

        return line.replace("{tempo}", tempo)
                .replace("{seekers}", String.valueOf(seekers))
                .replace("{hiders}", String.valueOf(hiders))
                .replace("{map}", game.getArena().getGameMap().getName());
    }

    private String formatGameTime(int time) {
        int min = time / 60;
        int sec = time % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private String applyAllPlaceholders(Player player, String text) {
        String replaced = text;
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            replaced = PlaceholderAPI.setPlaceholders(player, replaced);
        }
        return replaced;
    }

    private String colorize(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }

    private String fixBlankLine(String line, int score) {
        if (line == null) line = "";
        String trimmed = line.replace("§", "").trim();
        if (trimmed.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(" ");
            for (int i = 0; i < score; i++) {
                sb.append(" ");
            }
            String blank = sb.toString();
            if (usedBlankLines.contains(blank)) {
                blank = blank + " ";
            }
            usedBlankLines.add(blank);
            return blank;
        }
        return line;
    }
}