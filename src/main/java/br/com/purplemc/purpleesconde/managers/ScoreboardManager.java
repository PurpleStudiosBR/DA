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
        List<String> lines = plugin.getConfigManager().getScoreboardLines("lobby");
        String title = plugin.getConfigManager().getScoreboardTitle("lobby");
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("lobby", "dummy");
        obj.setDisplayName(colorize(title != null ? title : "§aLobby"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        usedBlankLines.clear();
        for (String line : lines) {
            String processedLine = colorize(applyAllPlaceholders(player, replaceLobbyPlaceholders(player, line)));
            processedLine = fixBlankLine(processedLine, score);
            obj.getScore(processedLine).setScore(score--);
        }
        player.setScoreboard(scoreboard);
        playerScoreboards.put(player, scoreboard);
    }

    public void setWaitingLobbyScoreboard(Player player, Arena arena) {
        List<String> lines = plugin.getConfigManager().getScoreboardLines("waiting");
        String title = plugin.getConfigManager().getScoreboardTitle("waiting");
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("waiting", "dummy");
        obj.setDisplayName(colorize(title != null ? title : "§aWaiting"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        usedBlankLines.clear();
        for (String line : lines) {
            String processedLine = colorize(applyAllPlaceholders(player, replaceWaitingPlaceholders(player, arena, line)));
            processedLine = fixBlankLine(processedLine, score);
            obj.getScore(processedLine).setScore(score--);
        }
        player.setScoreboard(scoreboard);
        playerScoreboards.put(player, scoreboard);
    }

    public void setGameScoreboard(Player player, Game game) {
        List<String> lines = plugin.getConfigManager().getScoreboardLines("game");
        String title = plugin.getConfigManager().getScoreboardTitle("game");
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("game", "dummy");
        obj.setDisplayName(colorize(title != null ? title : "§aGame"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        usedBlankLines.clear();
        for (String line : lines) {
            String processedLine = colorize(applyAllPlaceholders(player, replaceGamePlaceholders(player, game, line)));
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
        int wins = plugin.getDatabaseManager().getWins(player.getUniqueId());
        int losses = plugin.getDatabaseManager().getLosses(player.getUniqueId());
        int games = plugin.getDatabaseManager().getGames(player.getUniqueId());
        int level = plugin.getLevelManager().getPlayerLevel(player);
        int kills = plugin.getLevelManager().getPlayerKills(player);
        String levelDisplay = plugin.getLevelManager().getLevelDisplay(player);
        String progressBar = plugin.getLevelManager().getProgressBar(player);
        String xp = plugin.getLevelManager().getXPInfo(player);

        return line.replace("{wins}", String.valueOf(wins))
                .replace("{losses}", String.valueOf(losses))
                .replace("{games}", String.valueOf(games))
                .replace("{level}", levelDisplay)
                .replace("{xp_bar}", progressBar)
                .replace("{xp}", xp)
                .replace("{kills}", String.valueOf(kills));
    }

    private String replaceWaitingPlaceholders(Player player, Arena arena, String line) {
        String status;
        if (arena.getPlayers().size() < plugin.getConfigManager().getMinPlayersToStart()) {
            status = "§fStatus: §eEsperando...";
        } else if (arena.getState().name().equalsIgnoreCase("STARTING")) {
            status = "§fStatus: §aInicia em §f" + arena.getCountdown() + "s";
        } else {
            status = "§fStatus: §ePreparando...";
        }
        return line.replace("{map}", arena.getGameMap().getName())
                .replace("{players}", String.valueOf(arena.getPlayers().size()))
                .replace("{max_players}", String.valueOf(arena.getMaxPlayers()))
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
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            String unique = "§" + score + " ";
            while (usedBlankLines.contains(unique)) {
                unique = unique + " ";
            }
            usedBlankLines.add(unique);
            return unique;
        }
        return line;
    }
}