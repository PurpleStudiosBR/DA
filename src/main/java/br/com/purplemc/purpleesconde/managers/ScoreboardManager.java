package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.game.Game;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;

public class ScoreboardManager {

    private final PurpleEsconde plugin;

    public ScoreboardManager(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    public void setLobbyScoreboard(Player player) {
        List<String> lines = plugin.getConfigManager().getScoreboardLines("lobby");
        String title = plugin.getConfigManager().getScoreboardTitle("lobby");
        setScoreboard(player, title, lines
                .stream()
                .map(line -> replaceLobbyPlaceholders(player, line))
                .toArray(String[]::new), player);
    }

    public void setWaitingLobbyScoreboard(Player player, Arena arena) {
        List<String> lines = plugin.getConfigManager().getScoreboardLines("waiting");
        String title = plugin.getConfigManager().getScoreboardTitle("waiting");
        setScoreboard(player, title, lines
                .stream()
                .map(line -> replaceWaitingPlaceholders(player, arena, line))
                .toArray(String[]::new), player);
    }

    public void setGameScoreboard(Player player, Game game) {
        List<String> lines = plugin.getConfigManager().getScoreboardLines("game");
        String title = plugin.getConfigManager().getScoreboardTitle("game");
        setScoreboard(player, title, lines
                .stream()
                .map(line -> replaceGamePlaceholders(player, game, line))
                .toArray(String[]::new), player);
    }

    public void removePlayerScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public void updateWaitingScoreboard(Arena arena) {
        for (Player player : arena.getPlayers()) {
            setWaitingLobbyScoreboard(player, arena);
        }
    }

    private void setScoreboard(Player player, String title, String[] lines, Player placeholderPlayer) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("pe", "dummy");
        obj.setDisplayName(fixLength(applyAllPlaceholders(placeholderPlayer, title), 32));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.length;
        for (String line : lines) {
            String processedLine = applyAllPlaceholders(placeholderPlayer, line.replace("&", "§"));
            if (processedLine.isEmpty()) processedLine = " ";
            processedLine = fixLength(processedLine, 40);
            if (obj.getScoreboard().getEntries().contains(processedLine)) {
                processedLine = processedLine + getUniqueSuffix(score);
                processedLine = fixLength(processedLine, 40);
            }
            obj.getScore(processedLine).setScore(score--);
        }
        player.setScoreboard(scoreboard);
    }

    private String getUniqueSuffix(int score) {
        return "§" + Integer.toHexString(score % 16);
    }

    private String fixLength(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
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
            status = "§fStatus: §eComeçando em " + arena.getCountdown() + "s";
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
        replaced = replaceLobbyPlaceholders(player, replaced);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            replaced = PlaceholderAPI.setPlaceholders(player, replaced);
        }
        return replaced;
    }
}
