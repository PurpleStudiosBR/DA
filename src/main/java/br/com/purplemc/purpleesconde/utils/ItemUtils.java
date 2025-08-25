package br.com.purplemc.purpleesconde.utils;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.game.Game;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ItemUtils implements Listener {

    private static Plugin pluginInstance;

    public static void init(Plugin plugin) {
        pluginInstance = plugin;
        plugin.getServer().getPluginManager().registerEvents(new ItemUtils(), plugin);
    }

    public static ItemStack createItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(displayName);
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createGUIItem(Material material, String displayName, List<String> lore) {
        return createItem(material, displayName, lore);
    }

    public static ItemStack[] getSeekerArmor() {
        ItemStack[] armor = new ItemStack[4];

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        helmetMeta.setColor(Color.RED);
        helmetMeta.setDisplayName("§cCapacete do Procurador");
        helmet.setItemMeta(helmetMeta);

        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        chestMeta.setColor(Color.RED);
        chestMeta.setDisplayName("§cPeitoral do Procurador");
        chestplate.setItemMeta(chestMeta);

        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta legsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        legsMeta.setColor(Color.RED);
        legsMeta.setDisplayName("§cCalças do Procurador");
        leggings.setItemMeta(legsMeta);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setColor(Color.RED);
        bootsMeta.setDisplayName("§cBotas do Procurador");
        boots.setItemMeta(bootsMeta);

        armor[3] = helmet;
        armor[2] = chestplate;
        armor[1] = leggings;
        armor[0] = boots;

        return armor;
    }

    public static ItemStack[] getHiderArmor() {
        ItemStack[] armor = new ItemStack[4];

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        helmetMeta.setColor(Color.GREEN);
        helmetMeta.setDisplayName("§aCapacete do Escondedor");
        helmet.setItemMeta(helmetMeta);

        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        chestMeta.setColor(Color.GREEN);
        chestMeta.setDisplayName("§aPeitoral do Escondedor");
        chestplate.setItemMeta(chestMeta);

        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta legsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        legsMeta.setColor(Color.GREEN);
        legsMeta.setDisplayName("§aCalças do Escondedor");
        leggings.setItemMeta(legsMeta);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setColor(Color.GREEN);
        bootsMeta.setDisplayName("§aBotas do Escondedor");
        boots.setItemMeta(bootsMeta);

        armor[3] = helmet;
        armor[2] = chestplate;
        armor[1] = leggings;
        armor[0] = boots;

        return armor;
    }

    public static ItemStack getWaitingBed() {
        return createItem(Material.BED, "§cSair da partida", Arrays.asList("§7Clique para sair da arena"));
    }

    public static ItemStack getSeekerWeapon() {
        ItemStack weapon = new ItemStack(Material.STICK);
        ItemMeta meta = weapon.getItemMeta();
        meta.setDisplayName("§cBastão do Procurador");
        meta.setLore(Arrays.asList("§7Arma especial do procurador"));
        weapon.setItemMeta(meta);
        weapon.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 10);
        return weapon;
    }

    public static ItemStack getHiderWeapon() {
        ItemStack weapon = new ItemStack(Material.STICK);
        ItemMeta meta = weapon.getItemMeta();
        meta.setDisplayName("§aBastão do Escondedor");
        meta.setLore(Arrays.asList("§7Para se defender"));
        weapon.setItemMeta(meta);
        weapon.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
        return weapon;
    }

    public static ItemStack getFirework() {
        ItemStack firework = createItem(Material.FIREWORK, "§eFogos de Artifício", Arrays.asList("§7Use para criar distrações!"));
        firework.setAmount(64);
        return firework;
    }

    public static void giveHiderFireworks(Player player) {
        ItemStack fireworks = getFirework();
        player.getInventory().addItem(fireworks);
    }

    public static ItemStack getLobbyJoinItem() {
        return createItem(Material.ENDER_PEARL, "§aJogar Esconde-Esconde", Arrays.asList("§7Clique para entrar em uma partida!"));
    }

    public static ItemStack getMapSelectorItem() {
        return createItem(Material.SIGN, "§eSelecionar Mapa", Arrays.asList("§7Escolha seu mapa favorito!"));
    }

    public static ItemStack getSpectatorCompass() {
        return createItem(Material.COMPASS, "§bTeleporte de Espectador", Arrays.asList("§7Clique para teleportar para jogadores"));
    }

    public static ItemStack getBackItem() {
        return createItem(Material.ARROW, "§cVoltar", null);
    }

    public static boolean isSpecialItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        String displayName = item.getItemMeta().getDisplayName();
        return displayName != null && (
                displayName.contains("Sair da partida") ||
                        displayName.contains("Jogar Esconde-Esconde") ||
                        displayName.contains("Selecionar Mapa") ||
                        displayName.contains("Teleporte de Espectador") ||
                        displayName.contains("Voltar")
        );
    }

    @EventHandler
    public void onHiderFireworkUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FIREWORK) return;
        if (!player.isOnline() || !player.isValid()) return;
        if (pluginInstance instanceof PurpleEsconde) {
            PurpleEsconde plugin = (PurpleEsconde) pluginInstance;
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            if (arena == null || arena.getGame() == null) return;
            Game game = arena.getGame();
            game.handleFireworkUse(player);
        }
        BukkitRunnable removeItem = new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack[] contents = player.getInventory().getContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack slot = contents[i];
                    if (slot != null && slot.getType() == Material.FIREWORK) {
                        if (slot.getAmount() > 1) {
                            slot.setAmount(slot.getAmount() - 1);
                        } else {
                            contents[i] = null;
                        }
                        break;
                    }
                }
                player.getInventory().setContents(contents);
            }
        };
        removeItem.runTaskLater(pluginInstance, 1L);
    }
}