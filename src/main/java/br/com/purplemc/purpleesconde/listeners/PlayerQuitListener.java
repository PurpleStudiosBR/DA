package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerQuitListener implements Listener {

    private final PurpleEsconde plugin;

    public PlayerQuitListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Arena arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena != null) {
            // Se estava em partida ativa, contar como derrota
            if (arena.getGame() != null &&
                    (arena.getGame().isHider(player) || arena.getGame().isSeeker(player))) {

                if (plugin.getDatabaseManager() != null) {
                    plugin.getDatabaseManager().addLoss(player);
                }

                plugin.getLogger().info("Jogador " + player.getName() + " saiu durante partida - contado como derrota");
            }

            // Limpar armadura e inventário antes de remover da arena
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().clear();

            plugin.getArenaManager().removePlayerFromArena(player);
        }

        if (plugin.getSpectatorManager().isSpectator(player)) {
            plugin.getSpectatorManager().removeSpectator(player);
        }

        plugin.getScoreboardManager().removePlayerScoreboard(player);

        // Salvar dados do jogador
        if (plugin.getLevelManager() != null) {
            plugin.getLevelManager().savePlayerData();
            plugin.getLevelManager().removePlayer(player);
        }
    }
}