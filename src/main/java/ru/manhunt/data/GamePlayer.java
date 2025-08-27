package ru.manhunt.data;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import ru.manhunt.enums.PlayerRole;

public class GamePlayer {
    
    private final Player player;
    private PlayerRole role;
    private int lives;
    private boolean frozen;
    
    public GamePlayer(Player player) {
        this.player = player;
        this.role = PlayerRole.SPECTATOR;
        this.lives = 1;
        this.frozen = false;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public PlayerRole getRole() {
        return role;
    }
    
    public void setRole(PlayerRole role) {
        this.role = role;
        applyRoleSettings();
    }
    
    public int getLives() {
        return lives;
    }
    
    public void setLives(int lives) {
        this.lives = Math.max(1, Math.min(10, lives));
    }
    
    public void removeLife() {
        this.lives = Math.max(0, this.lives - 1);
    }
    
    public boolean isAlive() {
        return lives > 0;
    }
    
    public boolean isFrozen() {
        return frozen;
    }
    
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
    
    public void applyRoleSettings() {
        // Установка игрового режима в зависимости от роли
        switch (role) {
            case SPEEDRUNNER:
            case HUNTER:
                player.setGameMode(GameMode.ADVENTURE);
                break;
            case JUDGE:
                player.setGameMode(GameMode.ADVENTURE);
                break;
            case SPECTATOR:
                player.setGameMode(GameMode.ADVENTURE);
                break;
        }
    }
}
