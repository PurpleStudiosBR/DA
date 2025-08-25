package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class CosmeticListener implements Listener {

    private final PurpleEsconde plugin;

    public CosmeticListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() != Material.NETHER_STAR) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.contains("Cosméticos")) {
            event.setCancelled(true);
            plugin.getCosmeticManager().openCosmeticGUI(player);
        }
    }
}