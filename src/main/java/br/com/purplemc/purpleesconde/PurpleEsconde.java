package br.com.purplemc.purpleesconde;

import br.com.purplemc.purpleesconde.api.PurpleEscondeAPI;
import br.com.purplemc.purpleesconde.commands.PurpleEscondeCommand;
import br.com.purplemc.purpleesconde.commands.CosmeticCommand;
import br.com.purplemc.purpleesconde.database.DatabaseManager;
import br.com.purplemc.purpleesconde.listeners.CitizensNpcListener;
import br.com.purplemc.purpleesconde.listeners.PlayerChatListener;
import br.com.purplemc.purpleesconde.listeners.PlayerCommandListener;
import br.com.purplemc.purpleesconde.listeners.PlayerDamageListener;
import br.com.purplemc.purpleesconde.listeners.PlayerDeathListener;
import br.com.purplemc.purpleesconde.listeners.PlayerInteractListener;
import br.com.purplemc.purpleesconde.listeners.PlayerJoinListener;
import br.com.purplemc.purpleesconde.listeners.PlayerQuitListener;
import br.com.purplemc.purpleesconde.listeners.BlockListener;
import br.com.purplemc.purpleesconde.listeners.PlayerListener;
import br.com.purplemc.purpleesconde.listeners.SpectatorListener;
import br.com.purplemc.purpleesconde.listeners.SetupListener;
import br.com.purplemc.purpleesconde.listeners.CosmeticListener;
import br.com.purplemc.purpleesconde.managers.ArenaManager;
import br.com.purplemc.purpleesconde.managers.GameManager;
import br.com.purplemc.purpleesconde.managers.GUIManager;
import br.com.purplemc.purpleesconde.managers.LevelManager;
import br.com.purplemc.purpleesconde.managers.MapManager;
import br.com.purplemc.purpleesconde.managers.ScoreboardManager;
import br.com.purplemc.purpleesconde.managers.SpectatorManager;
import br.com.purplemc.purpleesconde.managers.CosmeticManager;
import br.com.purplemc.purpleesconde.utils.ConfigManager;
import br.com.purplemc.purpleesconde.utils.MessageUtils;
import br.com.purplemc.purpleesconde.utils.ItemUtils;
import br.com.purplemc.purpleesconde.placeholders.EscondePlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PurpleEsconde extends JavaPlugin {

    private static PurpleEsconde instance;
    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private MapManager mapManager;
    private GUIManager guiManager;
    private SpectatorManager spectatorManager;
    private ScoreboardManager scoreboardManager;
    private LevelManager levelManager;
    private DatabaseManager databaseManager;
    private CosmeticManager cosmeticManager;
    private PurpleEscondeAPI api;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("===== INICIANDO PURPLEESCONDE =====");

        try {
            // Inicializar managers na ordem correta
            getLogger().info("Carregando configurações...");
            configManager = new ConfigManager(this);

            getLogger().info("Carregando mapas...");
            mapManager = new MapManager(this);

            getLogger().info("Inicializando gerenciadores...");
            arenaManager = new ArenaManager(this);
            gameManager = new GameManager(this);
            guiManager = new GUIManager(this);
            spectatorManager = new SpectatorManager(this);
            scoreboardManager = new ScoreboardManager(this);
            levelManager = new LevelManager(this);
            databaseManager = new DatabaseManager(this);
            cosmeticManager = new CosmeticManager(this);
            api = new PurpleEscondeAPI(this);

            // Inicializar ItemUtils
            getLogger().info("Inicializando utilitários...");
            ItemUtils.init(this);

            // Carregar arenas
            getLogger().info("Carregando arenas...");
            arenaManager.loadArenas();

            // Registrar comandos
            getLogger().info("Registrando comandos...");
            getCommand("purpleesconde").setExecutor(new PurpleEscondeCommand(this));
            getCommand("ed").setExecutor(new PurpleEscondeCommand(this));
            getCommand("edbuild").setExecutor(new PurpleEscondeCommand(this));

            if (getCommand("cosmetic") != null) {
                getCommand("cosmetic").setExecutor(new CosmeticCommand(this));
            }

            // Registrar listeners
            getLogger().info("Registrando listeners...");
            Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
            Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
            Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(this, guiManager), this);
            Bukkit.getPluginManager().registerEvents(new PlayerChatListener(this), this);
            Bukkit.getPluginManager().registerEvents(new PlayerCommandListener(this), this);
            Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this), this);
            Bukkit.getPluginManager().registerEvents(new PlayerDamageListener(this), this);
            Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
            Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
            Bukkit.getPluginManager().registerEvents(new SpectatorListener(this), this);
            Bukkit.getPluginManager().registerEvents(new SetupListener(this), this);
            Bukkit.getPluginManager().registerEvents(new CosmeticListener(this), this);

            // Integração com Citizens
            if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
                Bukkit.getPluginManager().registerEvents(new CitizensNpcListener(this), this);
                getLogger().info("Citizens integration enabled!");
            }

            // Integração com PlaceholderAPI
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                new EscondePlaceholder(this).register();
                getLogger().info("PlaceholderAPI integration enabled!");
            }

            // Verificar integração com Vault
            if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                getLogger().info("Vault integration enabled!");
            } else {
                getLogger().warning("Vault não encontrado! Sistema de economia não funcionará.");
            }

            getLogger().info("===== PURPLEESCONDE HABILITADO COM SUCESSO! =====");
            getLogger().info("Mapas carregados: " + mapManager.getMaps().size());
            getLogger().info("Arenas criadas: " + arenaManager.getArenas().size());

        } catch (Exception e) {
            getLogger().severe("ERRO CRÍTICO durante inicialização:");
            e.printStackTrace();
            getLogger().severe("Plugin será desabilitado devido ao erro acima.");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("===== DESABILITANDO PURPLEESCONDE =====");

        try {
            if (gameManager != null) {
                getLogger().info("Parando jogos ativos...");
                gameManager.stopAllGames();
            }
            if (arenaManager != null) {
                getLogger().info("Limpando arenas...");
                arenaManager.cleanup();
            }
            if (databaseManager != null) {
                getLogger().info("Salvando banco de dados...");
                databaseManager.save();
            }
            if (cosmeticManager != null) {
                getLogger().info("Salvando cosméticos...");
                cosmeticManager.savePlayerData();
            }
            if (levelManager != null) {
                getLogger().info("Salvando dados de level...");
                levelManager.savePlayerData();
            }
        } catch (Exception e) {
            getLogger().severe("Erro ao desabilitar plugin:");
            e.printStackTrace();
        }

        getLogger().info("===== PURPLEESCONDE DESABILITADO =====");
    }

    // Getters
    public static PurpleEsconde getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public SpectatorManager getSpectatorManager() {
        return spectatorManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }

    public PurpleEscondeAPI getAPI() {
        return api;
    }
}