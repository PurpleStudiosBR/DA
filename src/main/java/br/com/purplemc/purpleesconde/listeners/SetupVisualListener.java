package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class SetupVisualListener implements Listener {

    private final PurpleEsconde plugin;

    public SetupVisualListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasMetadata("pe-setup")) return;
        ItemStack item = player.getItemInHand();
        if (item == null) return;
        if (item.getType() == Material.BLAZE_ROD) {
            player.setMetadata("pe-setup-pos1", new FixedMetadataValue(plugin, player.getLocation().clone()));
            player.sendMessage("§aPosição 1 da barreira setada.");
        } else if (item.getType() == Material.STICK) {
            player.setMetadata("pe-setup-pos2", new FixedMetadataValue(plugin, player.getLocation().clone()));
            player.sendMessage("§aPosição 2 da barreira setada.");
        } else if (item.getType() == Material.NETHER_STAR) {
            player.removeMetadata("pe-setup", plugin);
            player.removeMetadata("pe-setup-pos1", plugin);
            player.removeMetadata("pe-setup-pos2", plugin);
            player.sendMessage("§aSetup visual finalizado.");
        }
    }
}