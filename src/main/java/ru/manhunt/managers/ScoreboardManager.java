package ru.manhunt.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ru.manhunt.ManhuntPlugin;
import ru.manhunt.enums.PlayerRole;

public class ScoreboardManager {
    
    private final ManhuntPlugin plugin;
    private final Scoreboard scoreboard;
    private Team speedrunnerTeam;
    private Team hunterTeam;
    private Team judgeTeam;
    
    public ScoreboardManager(ManhuntPlugin plugin) {
        this.plugin = plugin;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        createTeams();
    }
    
    private void createTeams() {
        // Создание команды для спидранеров
        speedrunnerTeam = getOrCreateTeam("manhunt_speedrunner");
        speedrunnerTeam.setPrefix("§a");
        speedrunnerTeam.setSuffix(" §a[Ранер]");
        speedrunnerTeam.setDisplayName("§aСпидранеры");
        
        // Создание команды для охотников
        hunterTeam = getOrCreateTeam("manhunt_hunter");
        hunterTeam.setPrefix("§c");
        hunterTeam.setSuffix(" §c[Охотник]");
        hunterTeam.setDisplayName("§cОхотники");
        
        // Создание команды для судей
        judgeTeam = getOrCreateTeam("manhunt_judge");
        judgeTeam.setPrefix("§6");
        judgeTeam.setSuffix(" §6[Судья]");
        judgeTeam.setDisplayName("§6Судьи");
        
        plugin.getLogger().info("Команды Scoreboard созданы успешно!");
    }
    
    private Team getOrCreateTeam(String name) {
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
        }
        return team;
    }
    
    public void addPlayerToTeam(Player player, PlayerRole role) {
        // Сначала удаляем игрока из всех команд
        removePlayerFromAllTeams(player);
        
        // Затем добавляем в нужную команду
        Team team = getTeamByRole(role);
        if (team != null) {
            team.addEntry(player.getName());
            plugin.getLogger().info("Игрок " + player.getName() + " добавлен в команду " + role.getDisplayName());
        }
    }
    
    public void removePlayerFromAllTeams(Player player) {
        String playerName = player.getName();
        
        if (speedrunnerTeam.hasEntry(playerName)) {
            speedrunnerTeam.removeEntry(playerName);
        }
        if (hunterTeam.hasEntry(playerName)) {
            hunterTeam.removeEntry(playerName);
        }
        if (judgeTeam.hasEntry(playerName)) {
            judgeTeam.removeEntry(playerName);
        }
    }
    
    private Team getTeamByRole(PlayerRole role) {
        switch (role) {
            case SPEEDRUNNER:
                return speedrunnerTeam;
            case HUNTER:
                return hunterTeam;
            case JUDGE:
                return judgeTeam;
            case SPECTATOR:
                return null; // Зрители не добавляются в команды
            default:
                return null;
        }
    }
    
    public void cleanup() {
        // Очистка всех команд при отключении плагина
        if (speedrunnerTeam != null) {
            speedrunnerTeam.unregister();
        }
        if (hunterTeam != null) {
            hunterTeam.unregister();
        }
        if (judgeTeam != null) {
            judgeTeam.unregister();
        }
        
        plugin.getLogger().info("Команды Scoreboard очищены!");
    }
}