package br.com.purplemc.purpleesconde.utils;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.List;

public class ItemUtils {

    public static ItemStack createItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(displayName);
        }

        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
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
        return createItem(Material.BED, "§cSair da Partida",
                Arrays.asList("§7Clique com botão direito", "§7para voltar ao lobby"));
    }
}