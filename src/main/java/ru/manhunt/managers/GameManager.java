package ru.manhunt.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.manhunt.ManhuntPlugin;
import ru.manhunt.data.GamePlayer;
import ru.manhunt.enums.GameState;
import ru.manhunt.enums.PlayerRole;
import ru.manhunt.gui.RoleSelectionGUI;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Iterator;
import java.util.stream.Collectors;

public class GameManager implements Listener {
    
    private final ManhuntPlugin plugin;
    private final Map<UUID, GamePlayer> players;
    private GameState gameState;
    private int freezeTime;
    private BukkitTask freezeTask;
    private BukkitTask gameTask;
    private boolean dragonKilled;
    
    public GameManager(ManhuntPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        this.plugin = plugin;
        this.players = new HashMap<>();
        this.gameState = GameState.WAITING;
        this.freezeTime = 60; // По умолчанию 60 секунд
        this.dragonKilled = false;
    }
    
    public void addPlayer(Player player) {
        if (!players.containsKey(player.getUniqueId())) {
            players.put(player.getUniqueId(), new GamePlayer(player));
            
            // Телепорт в лобби мир
            Location lobbySpawn = plugin.getWorldManager().getLobbySpawn();
            player.teleport(lobbySpawn);
            
            // Применяем настройки роли (по умолчанию SPECTATOR)
            players.get(player.getUniqueId()).applyRoleSettings();
            
            // Добавляем игрока в команду зрителей по умолчанию
            plugin.getScoreboardManager().addPlayerToTeam(player, PlayerRole.SPECTATOR);
            
            player.sendMessage("§aДобро пожаловать в Manhunt! Используйте /manhunt для открытия меню.");
        }
    }
    
    public void removePlayer(Player player) {
        // Удаляем игрока из всех команд
        plugin.getScoreboardManager().removePlayerFromAllTeams(player);
        players.remove(player.getUniqueId());
    }
    
    public void setPlayerRoleAndTeam(Player player, PlayerRole newRole) {
        GamePlayer gamePlayer = getGamePlayer(player);
        if (gamePlayer != null) {
            gamePlayer.setRole(newRole);
            plugin.getScoreboardManager().addPlayerToTeam(player, newRole);
        }
    }
    
    public GamePlayer getGamePlayer(Player player) {
        return players.get(player.getUniqueId());
    }
    
    public void openRoleSelection(Player player) {
        if (gameState != GameState.WAITING) {
            player.sendMessage("§cИгра уже началась!");
            return;
        }
        
        new RoleSelectionGUI(plugin, player).open();
    }
    
    public void startGame() {
        if (gameState != GameState.WAITING) {
            return;
        }
        
        // Проверка наличия ролей
        boolean hasSpeedrunner = players.values().stream()
                .anyMatch(gp -> gp.getRole() == PlayerRole.SPEEDRUNNER);
        boolean hasHunter = players.values().stream()
                .anyMatch(gp -> gp.getRole() == PlayerRole.HUNTER);
        
        if (!hasSpeedrunner || !hasHunter) {
            broadcast("§cНедостаточно игроков! Нужен минимум 1 спидранер и 1 охотник.");
            return;
        }
        
        gameState = GameState.STARTING;
        broadcast("§aИгра начинается! Подготовка...");
        
        // Сброс флага убийства дракона
        dragonKilled = false;
        
        // Сброс достижений у всех игроков
        resetAllPlayerAdvancements();
        
        // Регенерация игрового мира
        plugin.getWorldManager().regenerateGameWorlds(() -> {
            // Телепорт всех игроков в игровой мир (выполняется после создания миров)
            Location gameSpawn = plugin.getWorldManager().getGameSpawn();
            for (GamePlayer gamePlayer : players.values()) {
                Player player = gamePlayer.getPlayer();
                player.teleport(gameSpawn);
                if (gamePlayer.getRole() == PlayerRole.JUDGE) {
                    player.setGameMode(GameMode.SPECTATOR); // GM 3
                } else {
                    player.setGameMode(GameMode.SURVIVAL); // GM 0
                }
                player.getInventory().clear();
                player.setHealth(20.0);
                player.setFoodLevel(20);
                
                // Сброс эффектов
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
            }
            
            startHunterFreezePhase();
        });
    }
    
    /**
     * Сбрасывает все достижения у всех игроков в игре
     */
    private void resetAllPlayerAdvancements() {
        broadcast("§eСброс достижений игроков...");
        
        for (GamePlayer gamePlayer : players.values()) {
            Player player = gamePlayer.getPlayer();
            resetPlayerAdvancements(player);
        }
        
        broadcast("§aДостижения игроков сброшены!");
    }
    
    /**
     * Сбрасывает все достижения у конкретного игрока
     */
    private void resetPlayerAdvancements(Player player) {
        try {
            Iterator<Advancement> advancements = Bukkit.getServer().advancementIterator();
            
            while (advancements.hasNext()) {
                Advancement advancement = advancements.next();
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                
                // Отзываем все критерии достижения
                for (String criteria : progress.getAwardedCriteria()) {
                    progress.revokeCriteria(criteria);
                }
            }
            
            player.sendMessage("§eВаши достижения были сброшены для новой игры!");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при сбросе достижений игрока " + player.getName() + ": " + e.getMessage());
        }
    }
    
    private void startHunterFreezePhase() {
        gameState = GameState.HUNTERS_FROZEN;
        
        // Заморозка охотников
        for (GamePlayer gamePlayer : players.values()) {
            if (gamePlayer.getRole() == PlayerRole.HUNTER) {
                Player player = gamePlayer.getPlayer();
                gamePlayer.setFrozen(true);
                
                // Эффекты заморозки
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 200, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 1, true, false));
            }
        }
        
        broadcast("§cОхотники заморожены на " + freezeTime + " секунд!");
        broadcast("§aСпидранеры могут начинать!");
        
        // Запуск таймера разморозки
        freezeTask = new BukkitRunnable() {
            int timeLeft = freezeTime;
            
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    unfreezeHunters();
                    this.cancel();
                    return;
                }
                
                if (timeLeft <= 10 || timeLeft % 10 == 0) {
                    broadcast("§eДо разморозки охотников: §c" + timeLeft + " §eсекунд");
                }
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void unfreezeHunters() {
        gameState = GameState.ACTIVE;
        
        for (GamePlayer gamePlayer : players.values()) {
            if (gamePlayer.getRole() == PlayerRole.HUNTER) {
                Player player = gamePlayer.getPlayer();
                gamePlayer.setFrozen(false);
                
                // Удаление эффектов заморозки
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.removePotionEffect(PotionEffectType.JUMP_BOOST);
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.removePotionEffect(PotionEffectType.DARKNESS);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                
                // Выдача компаса
                plugin.getCompassManager().giveCompass(player);
            }
        }
        
        broadcast("§aОхотники разморожены! Охота началась!");
        
        startGameLoop();
    }
    
    private void startGameLoop() {
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.ACTIVE) {
                    this.cancel();
                    return;
                }
                
                // Обновление компасов
                plugin.getCompassManager().updateCompasses();
                
                // Проверка условий победы
                checkWinConditions();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void checkWinConditions() {
        // Проверка жизней спидранера
        boolean speedrunnerAlive = players.values().stream()
                .filter(gp -> gp.getRole() == PlayerRole.SPEEDRUNNER)
                .anyMatch(GamePlayer::isAlive);
        
        if (!speedrunnerAlive) {
            endGame("§cОхотники победили! Все спидранеры мертвы!");
            return;
        }
        
        // Можно добавить другие условия победы (например, убийство дракона)
    }
    
    public void onEnderDragonKilled() {
        if (gameState != GameState.ACTIVE || dragonKilled) {
            return;
        }
        
        dragonKilled = true;
        
        // Обновляем статистику - спидранеры победили
        updatePlayerStatistics(true);
        
        endGame("§aСпидранеры победили! Дракон Края убит!");
    }
    
    private void updatePlayerStatistics(boolean speedrunnersWon) {
        for (GamePlayer gamePlayer : players.values()) {
            if (gamePlayer.getRole() == PlayerRole.SPEEDRUNNER) {
                if (speedrunnersWon) {
                    gamePlayer.addSpeedrunnerWin();
                } else {
                    gamePlayer.addSpeedrunnerLoss();
                }
            }
        }
        
        // Обновляем табло статистики
        plugin.getScoreboardManager().updatePlayerStatsScoreboard();
    }
    
    public void endGame(String reason) {
        if (gameState == GameState.STOPPED) {
            return;
        }
        
        gameState = GameState.ENDING;
        
        // Если игра закончилась не из-за убийства дракона, значит охотники победили
        if (!dragonKilled && gameState != GameState.STOPPED) {
            updatePlayerStatistics(false);
        }
        
        broadcast("§e" + reason);
        broadcast("§aИгра окончена! Телепортируем всех в лобби...");
        
        // Остановка задач
        if (freezeTask != null) {
            freezeTask.cancel();
        }
        if (gameTask != null) {
            gameTask.cancel();
        }
        
        // Телепорт в лобби
        Location lobbySpawn = plugin.getWorldManager().getLobbySpawn();
        for (GamePlayer gamePlayer : players.values()) {
            Player player = gamePlayer.getPlayer();
            player.teleport(lobbySpawn);
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
            
            // Сброс роли и жизней
            gamePlayer.setRole(PlayerRole.SPECTATOR);
            gamePlayer.setLives(1);
            gamePlayer.setFrozen(false);
            
            // Возвращаем игрока в команду зрителей
            plugin.getScoreboardManager().addPlayerToTeam(player, PlayerRole.SPECTATOR);
            
            // Удаление эффектов
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
        }
        
        gameState = GameState.WAITING;
        dragonKilled = false;
    }
    
    public void stopGame() {
        endGame("§cИгра была остановлена администратором!");
    }
    
    public void onPlayerDeath(Player player) {
        GamePlayer gamePlayer = getGamePlayer(player);
        if (gamePlayer == null) return;
        
        if (gamePlayer.getRole() == PlayerRole.SPEEDRUNNER) {
            gamePlayer.removeLife();
            
            if (gamePlayer.isAlive()) {
                player.sendMessage("§eУ вас осталось жизней: §c" + gamePlayer.getLives());
                broadcast("§e" + player.getName() + " §eпотерял жизнь! Осталось: §c" + gamePlayer.getLives());
            } else {
                broadcast("§c" + player.getName() + " §cпотерял все жизни!");
            }
        }
    }
    public void broadcast(String message) {
        for (GamePlayer gamePlayer : players.values()) {
            gamePlayer.getPlayer().sendMessage(message);
        }
    }
    
    // Геттеры и сеттеры
    public GameState getGameState() {
        return gameState;
    }
    
    public int getFreezeTime() {
        return freezeTime;
    }
    
    public void setFreezeTime(int freezeTime) {
        this.freezeTime = Math.max(10, Math.min(600, freezeTime));
    }
    
    public Map<UUID, GamePlayer> getPlayers() {
        return players;
    }
    
    public long getSpeedrunnerCount() {
        return players.values().stream()
                .filter(gp -> gp.getRole() == PlayerRole.SPEEDRUNNER)
                .count();
    }
    
    public long getHunterCount() {
        return players.values().stream()
                .filter(gp -> gp.getRole() == PlayerRole.HUNTER)
                .count();
    }
    
    public long getJudgeCount() {
        return players.values().stream()
                .filter(gp -> gp.getRole() == PlayerRole.JUDGE)
                .count();
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
      Player player = event.getPlayer();
      GamePlayer gamePlayer = getGamePlayer(player);
      if (gamePlayer == null) return;

      // Если игрок участвует в игре и у него НЕТ своей точки респавна (кровать/якорь)
      if ((gamePlayer.getRole() == PlayerRole.SPEEDRUNNER || gamePlayer.getRole() == PlayerRole.HUNTER) 
          && gamePlayer.isAlive() && !event.isBedSpawn() && !event.isAnchorSpawn()) {
          
          Location gameSpawn = plugin.getWorldManager().getGameSpawn();
          if (gameSpawn != null) {
              event.setRespawnLocation(gameSpawn);
          }
        }
    }
}