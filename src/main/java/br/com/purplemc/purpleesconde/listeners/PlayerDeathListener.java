package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final PurpleEsconde plugin;

    public PlayerDeathListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Arena arena = plugin.getArenaManager().getPlayerArena(player);

        if (arena != null && arena.getGame() != null) {
            event.setDeathMessage(null);
            event.setDroppedExp(0);
            event.getDrops().clear();

            // Respawn automático sem tela de morte
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        player.spigot().respawn();
                    } catch (Exception e) {
                        // Fallback caso spigot().respawn() não funcione
                        player.setHealth(20.0);
                        player.teleport(arena.getGameMap().getWaitingLobby());
                    }
                }
            }, 1L);

            arena.getGame().onPlayerDeath(player);
        }
    }
}