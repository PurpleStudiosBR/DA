package br.com.purplemc.purpleesconde.gui;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.managers.CosmeticManager;
import br.com.purplemc.purpleesconde.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CosmeticGUI implements Listener {

    private final PurpleEsconde plugin;
    private final CosmeticManager cosmeticManager;
    private final Set<UUID> openGUIs;

    public CosmeticGUI(PurpleEsconde plugin, CosmeticManager cosmeticManager) {
        this.plugin = plugin;
        this.cosmeticManager = cosmeticManager;
        this.openGUIs = new HashSet<>();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Loja de Cosméticos");

        Map<String, CosmeticManager.CosmeticSet> cosmetics = cosmeticManager.getAllCosmetics();
        String currentCosmetic = cosmeticManager.getPlayerCosmetic(player);
        Set<String> ownedCosmetics = cosmeticManager.getOwnedCosmetics(player);
        double playerCoins = cosmeticManager.getPlayerCoins(player);

        int slot = 10;
        for (Map.Entry<String, CosmeticManager.CosmeticSet> entry : cosmetics.entrySet()) {
            String id = entry.getKey();
            CosmeticManager.CosmeticSet cosmetic = entry.getValue();

            ItemStack item;
            boolean owned = ownedCosmetics.contains(id);
            boolean isSelected = currentCosmetic.equals(id);
            boolean canAfford = playerCoins >= cosmetic.price;

            if (owned) {
                if (isSelected) {
                    item = new ItemStack(Material.WOOL, 1, (short) 5); // Verde - Selecionado
                } else {
                    item = new ItemStack(Material.WOOL, 1, (short) 11); // Azul - Possuído
                }
            } else {
                if (canAfford) {
                    item = new ItemStack(Material.WOOL, 1, (short) 4); // Amarelo - Pode comprar
                } else {
                    item = new ItemStack(Material.WOOL, 1, (short) 14); // Vermelho - Não pode comprar
                }
            }

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + cosmetic.name);

            List<String> lore = new ArrayList<>();
            lore.add(cosmetic.description);
            lore.add("");

            if (cosmetic.price == 0) {
                lore.add("§aGRÁTIS");
            } else {
                lore.add("§6Preço: " + cosmetic.price + " coins");
            }

            lore.add("");

            if (isSelected) {
                lore.add("§a✓ Equipado");
            } else if (owned) {
                lore.add("§eClique para equipar");
            } else if (canAfford) {
                lore.add("§aClique para comprar");
            } else {
                lore.add("§c✗ Coins insuficientes");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot, item);
            slot++;

            if (slot == 17) slot = 19; // Pular para próxima linha
            if (slot == 26) slot = 28; // Pular para terceira linha
        }

        // Informações do jogador
        ItemStack playerInfo = ItemUtils.createItem(Material.GOLD_INGOT, "§6Suas Moedas",
                Arrays.asList("§fVocê tem: §6" + (int)playerCoins + " coins",
                        "§7Jogue partidas para ganhar mais!"));
        inv.setItem(4, playerInfo);

        // Item para fechar
        ItemStack close = ItemUtils.createItem(Material.BARRIER, "§cFechar", Arrays.asList("§7Clique para fechar"));
        inv.setItem(49, close);

        player.openInventory(inv);
        openGUIs.add(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!openGUIs.contains(player.getUniqueId())) return;
        if (!event.getView().getTitle().equals("§8Loja de Cosméticos")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        ItemStack clicked = event.getCurrentItem();

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.GOLD_INGOT) {
            // Atualizar informação de coins
            openGUI(player);
            return;
        }

        if (clicked.getType() == Material.WOOL) {
            String itemName = clicked.getItemMeta().getDisplayName();

            // Encontrar o cosmético baseado no nome
            for (Map.Entry<String, CosmeticManager.CosmeticSet> entry : cosmeticManager.getAllCosmetics().entrySet()) {
                CosmeticManager.CosmeticSet cosmetic = entry.getValue();
                if (itemName.contains(cosmetic.name)) {
                    String cosmeticId = entry.getKey();

                    if (cosmeticManager.hasCosmetic(player, cosmeticId)) {
                        // Já possui, equipar
                        cosmeticManager.setPlayerCosmetic(player, cosmeticId);
                        player.closeInventory();
                        player.sendMessage("§aCosmético equipado: §e" + cosmetic.name);
                    } else {
                        // Não possui, tentar comprar
                        if (cosmeticManager.buyCosmetic(player, cosmeticId)) {
                            cosmeticManager.setPlayerCosmetic(player, cosmeticId);
                            player.closeInventory();
                        } else {
                            player.sendMessage("§cNão foi possível comprar este cosmético!");
                        }
                    }
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        openGUIs.remove(event.getPlayer().getUniqueId());
    }
}