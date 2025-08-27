package ru.manhunt.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Team;
import ru.manhunt.ManhuntPlugin;
import ru.manhunt.data.GamePlayer;
import ru.manhunt.enums.PlayerRole;

public class ScoreboardManager {
    
    private final ManhuntPlugin plugin;
    private final Scoreboard scoreboard;
    private Team speedrunnerTeam;
    private Team hunterTeam;
    private Team judgeTeam;
    private Objective statsObjective;
    
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
        
        // Создание объектива для статистики
        createStatsObjective();
        
        plugin.getLogger().info("Команды Scoreboard созданы успешно!");
    }
    
    private void createStatsObjective() {
        // Удаляем старый объектив если существует
        if (statsObjective != null) {
            statsObjective.unregister();
        }
        
        // Создаем новый объектив для статистики
        statsObjective = scoreboard.registerNewObjective("manhunt_stats", "dummy", "§6§lСтатистика Manhunt");
        statsObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        plugin.getLogger().info("Объектив статистики создан!");
    }
    
    public void updatePlayerStatsScoreboard() {
        if (statsObjective == null) {
            createStatsObjective();
        }
        
        // Очищаем старые записи
        for (String entry : statsObjective.getScoreboard().getEntries()) {
            if (statsObjective.getScore(entry).isScoreSet()) {
                statsObjective.getScoreboard().resetScores(entry);
            }
        }
        
        // Добавляем статистику для каждого игрока
        int score = 15; // Начинаем сверху
        
        // Заголовок
        statsObjective.getScore("§e§l--- Спидранеры ---").setScore(score--);
        statsObjective.getScore("").setScore(score--); // Пустая строка
        
        // Статистика игроков
        for (GamePlayer gamePlayer : plugin.getGameManager().getPlayers().values()) {
            Player player = gamePlayer.getPlayer();
            int wins = gamePlayer.getSpeedrunnerWins();
            int losses = gamePlayer.getSpeedrunnerLosses();
            
            if (wins > 0 || losses > 0) {
                String playerName = player.getName();
                if (playerName.length() > 12) {
                    playerName = playerName.substring(0, 12);
                }
                
                String statsLine = "§a" + playerName + ": §2" + wins + "§7/§c" + losses;
                statsObjective.getScore(statsLine).setScore(score--);
            }
        }
        
        // Если нет статистики, показываем сообщение
        if (score == 13) {
            statsObjective.getScore("§7Нет данных").setScore(score--);
        }
        
        plugin.getLogger().info("Табло статистики обновлено!");
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
        if (statsObjective != null) {
            statsObjective.unregister();
        }
        
        plugin.getLogger().info("Команды Scoreboard очищены!");
    }
}