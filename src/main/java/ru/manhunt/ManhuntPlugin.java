package ru.manhunt;

import org.bukkit.plugin.java.JavaPlugin;
import ru.manhunt.managers.GameManager;
import ru.manhunt.managers.WorldManager;
import ru.manhunt.managers.CompassManager;
import ru.manhunt.managers.ScoreboardManager;
import ru.manhunt.commands.ManhuntCommand;
import ru.manhunt.listeners.PlayerListener;

public class ManhuntPlugin extends JavaPlugin {
    
    private GameManager gameManager;
    private WorldManager worldManager;
    private CompassManager compassManager;
    private ScoreboardManager scoreboardManager;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Инициализация менеджеров
        this.worldManager = new WorldManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.gameManager = new GameManager(this);
        this.compassManager = new CompassManager(this);
        
        // Регистрация команд
        getCommand("manhunt").setExecutor(new ManhuntCommand(this));
        getCommand("mh").setExecutor(new ManhuntCommand(this));
        
        // Регистрация слушателей событий
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        getLogger().info("Manhunt Plugin успешно запущен!");
    }
    
    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.stopGame();
        }
        if (scoreboardManager != null) {
            scoreboardManager.cleanup();
        }
        getLogger().info("Manhunt Plugin остановлен!");
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public WorldManager getWorldManager() {
        return worldManager;
    }
    
    public CompassManager getCompassManager() {
        return compassManager;
    }
    
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}