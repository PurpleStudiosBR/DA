package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.gui.CosmeticGUI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.*;

public class CosmeticManager {

    private final PurpleEsconde plugin;
    private final Map<UUID, String> playerCosmetics;
    private final Map<UUID, Set<String>> playerOwnedCosmetics;
    private final Map<String, CosmeticSet> availableCosmetics;
    private final CosmeticGUI cosmeticGUI;
    private Economy economy;

    public CosmeticManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.playerCosmetics = new HashMap<>();
        this.playerOwnedCosmetics = new HashMap<>();
        this.availableCosmetics = new HashMap<>();
        this.cosmeticGUI = new CosmeticGUI(plugin, this);

        // Integração com economia
        if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            this.economy = plugin.getServer().getServicesManager().getRegistration(Economy.class) != null ?
                    plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider() : null;
        }

        loadCosmetics();
        loadPlayerData();
    }

    private void loadCosmetics() {
        // Cosmético Padrão (GRÁTIS)
        availableCosmetics.put("default", new CosmeticSet(
                "Padrão",
                Color.GREEN,
                Color.RED,
                0, // GRÁTIS
                "§7Cosmético padrão do jogo"
        ));

        // Cosméticos com preços
        availableCosmetics.put("gold", new CosmeticSet(
                "Dourado",
                Color.YELLOW,
                Color.ORANGE,
                500, // 500 coins
                "§6Cosmético dourado exclusivo"
        ));

        availableCosmetics.put("diamond", new CosmeticSet(
                "Diamante",
                Color.AQUA,
                Color.BLUE,
                1000, // 1000 coins
                "§bCosmético diamante premium"
        ));

        availableCosmetics.put("rainbow", new CosmeticSet(
                "Arco-íris",
                Color.fromRGB(255, 0, 255),
                Color.fromRGB(0, 255, 0),
                1500, // 1500 coins
                "§dCosmético arco-íris mágico"
        ));

        availableCosmetics.put("fire", new CosmeticSet(
                "Fogo",
                Color.fromRGB(255, 69, 0),
                Color.fromRGB(139, 0, 0),
                800, // 800 coins
                "§cCosmético flamejante"
        ));

        availableCosmetics.put("ice", new CosmeticSet(
                "Gelo",
                Color.fromRGB(173, 216, 230),
                Color.fromRGB(0, 191, 255),
                800, // 800 coins
                "§fCosmético congelante"
        ));

        availableCosmetics.put("purple", new CosmeticSet(
                "Roxo",
                Color.fromRGB(128, 0, 128),
                Color.fromRGB(75, 0, 130),
                600, // 600 coins
                "§5Cosmético roxo elegante"
        ));

        availableCosmetics.put("vip", new CosmeticSet(
                "VIP",
                Color.fromRGB(255, 215, 0),
                Color.fromRGB(255, 0, 0),
                2000, // 2000 coins
                "§6§lCosmético VIP Exclusivo!"
        ));
    }

    private void loadPlayerData() {
        // Carregar cosméticos selecionados
        if (plugin.getConfigManager().getPlayers().contains("cosmetics.selected")) {
            for (String uuid : plugin.getConfigManager().getPlayers().getConfigurationSection("cosmetics.selected").getKeys(false)) {
                try {
                    UUID playerUUID = UUID.fromString(uuid);
                    String cosmetic = plugin.getConfigManager().getPlayers().getString("cosmetics.selected." + uuid);
                    playerCosmetics.put(playerUUID, cosmetic);
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao carregar cosmético selecionado: " + uuid);
                }
            }
        }

        // Carregar cosméticos comprados
        if (plugin.getConfigManager().getPlayers().contains("cosmetics.owned")) {
            for (String uuid : plugin.getConfigManager().getPlayers().getConfigurationSection("cosmetics.owned").getKeys(false)) {
                try {
                    UUID playerUUID = UUID.fromString(uuid);
                    List<String> owned = plugin.getConfigManager().getPlayers().getStringList("cosmetics.owned." + uuid);
                    playerOwnedCosmetics.put(playerUUID, new HashSet<>(owned));
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao carregar cosméticos comprados: " + uuid);
                }
            }
        }
    }

    public void savePlayerData() {
        // Salvar cosméticos selecionados
        for (Map.Entry<UUID, String> entry : playerCosmetics.entrySet()) {
            plugin.getConfigManager().getPlayers().set("cosmetics.selected." + entry.getKey().toString(), entry.getValue());
        }

        // Salvar cosméticos comprados
        for (Map.Entry<UUID, Set<String>> entry : playerOwnedCosmetics.entrySet()) {
            plugin.getConfigManager().getPlayers().set("cosmetics.owned." + entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }

        plugin.getConfigManager().saveConfig(plugin.getConfigManager().getPlayers(), "players.yml");
    }

    public boolean buyCosmetic(Player player, String cosmeticId) {
        if (!availableCosmetics.containsKey(cosmeticId)) {
            return false;
        }

        if (hasCosmetic(player, cosmeticId)) {
            player.sendMessage("§cVocê já possui este cosmético!");
            return false;
        }

        CosmeticSet cosmetic = availableCosmetics.get(cosmeticId);

        if (cosmetic.price == 0) {
            // Cosmético grátis
            giveCosmetic(player, cosmeticId);
            return true;
        }

        if (economy == null) {
            player.sendMessage("§cSistema de economia não disponível!");
            return false;
        }

        if (!economy.has(player.getName(), cosmetic.price)) {
            player.sendMessage("§cVocê não tem coins suficientes! Precisa de " + cosmetic.price + " coins.");
            return false;
        }

        // Cobrar e dar o cosmético
        economy.withdrawPlayer(player.getName(), cosmetic.price);
        giveCosmetic(player, cosmeticId);

        player.sendMessage("§aCosmético §e" + cosmetic.name + " §acomprado por §6" + cosmetic.price + " coins§a!");
        return true;
    }

    private void giveCosmetic(Player player, String cosmeticId) {
        Set<String> owned = playerOwnedCosmetics.getOrDefault(player.getUniqueId(), new HashSet<>());
        owned.add(cosmeticId);
        playerOwnedCosmetics.put(player.getUniqueId(), owned);
        savePlayerData();
    }

    public boolean hasCosmetic(Player player, String cosmeticId) {
        if (cosmeticId.equals("default")) return true; // Todos têm o padrão
        Set<String> owned = playerOwnedCosmetics.get(player.getUniqueId());
        return owned != null && owned.contains(cosmeticId);
    }

    public boolean setPlayerCosmetic(Player player, String cosmeticId) {
        if (!availableCosmetics.containsKey(cosmeticId)) {
            return false;
        }

        if (!hasCosmetic(player, cosmeticId)) {
            return false;
        }

        playerCosmetics.put(player.getUniqueId(), cosmeticId);
        savePlayerData();

        CosmeticSet cosmetic = availableCosmetics.get(cosmeticId);
        player.sendMessage("§aCosmético alterado para: §e" + cosmetic.name);
        return true;
    }

    public String getPlayerCosmetic(Player player) {
        String selected = playerCosmetics.get(player.getUniqueId());
        // Se não tem o cosmético selecionado, volta para padrão
        if (selected != null && !hasCosmetic(player, selected)) {
            playerCosmetics.put(player.getUniqueId(), "default");
            selected = "default";
        }
        return selected != null ? selected : "default";
    }

    public Set<String> getOwnedCosmetics(Player player) {
        Set<String> owned = new HashSet<>(playerOwnedCosmetics.getOrDefault(player.getUniqueId(), new HashSet<>()));
        owned.add("default"); // Sempre incluir o padrão
        return owned;
    }

    public void applySeekerCosmetics(Player player) {
        String cosmeticId = getPlayerCosmetic(player);
        CosmeticSet cosmetic = availableCosmetics.get(cosmeticId);
        if (cosmetic != null) {
            applyColorToArmor(player, cosmetic.seekerColor);
        }
    }

    public void applyHiderCosmetics(Player player) {
        String cosmeticId = getPlayerCosmetic(player);
        CosmeticSet cosmetic = availableCosmetics.get(cosmeticId);
        if (cosmetic != null) {
            applyColorToArmor(player, cosmetic.hiderColor);
        }
    }

    private void applyColorToArmor(Player player, Color color) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece != null && piece.getType().name().contains("LEATHER")) {
                LeatherArmorMeta meta = (LeatherArmorMeta) piece.getItemMeta();
                meta.setColor(color);
                piece.setItemMeta(meta);
            }
        }
        player.getInventory().setArmorContents(armor);
    }

    public void openCosmeticGUI(Player player) {
        cosmeticGUI.openGUI(player);
    }

    public Map<String, CosmeticSet> getAllCosmetics() {
        return new HashMap<>(availableCosmetics);
    }

    public double getPlayerCoins(Player player) {
        if (economy == null) return 0;
        return economy.getBalance(player.getName());
    }

    // Dar cosmético padrão para jogadores novos
    public void initializePlayer(Player player) {
        if (!playerOwnedCosmetics.containsKey(player.getUniqueId())) {
            Set<String> owned = new HashSet<>();
            owned.add("default");
            playerOwnedCosmetics.put(player.getUniqueId(), owned);
            playerCosmetics.put(player.getUniqueId(), "default");
            savePlayerData();
        }
    }

    // Classe interna para conjuntos de cosméticos
    public static class CosmeticSet {
        public final String name;
        public final Color hiderColor;
        public final Color seekerColor;
        public final int price;
        public final String description;

        public CosmeticSet(String name, Color hiderColor, Color seekerColor, int price, String description) {
            this.name = name;
            this.hiderColor = hiderColor;
            this.seekerColor = seekerColor;
            this.price = price;
            this.description = description;
        }
    }
}