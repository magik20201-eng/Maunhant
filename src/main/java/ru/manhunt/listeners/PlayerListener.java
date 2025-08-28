package ru.manhunt.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.PortalType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.EnderDragon;
import ru.manhunt.ManhuntPlugin;
import ru.manhunt.data.GamePlayer;
import ru.manhunt.enums.PlayerRole;
import ru.manhunt.gui.RoleSelectionGUI;

public class PlayerListener implements Listener {
    
    private final ManhuntPlugin plugin;
    
    public PlayerListener(ManhuntPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().addPlayer(player);
        
        player.sendMessage("§a=== Добро пожаловать в Manhunt! ===");
        player.sendMessage("§eИспользуйте §6/manhunt §eдля открытия меню!");
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().removePlayer(player);
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Проверяем, убит ли дракон Края
        if (event.getEntity() instanceof EnderDragon) {
            plugin.getGameManager().onEnderDragonKilled();
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
        
        if (gamePlayer != null && gamePlayer.isFrozen()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.getGameManager().onPlayerDeath(player);
        
        // Сохранение компаса после смерти
        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
        if (gamePlayer != null && gamePlayer.getRole() == PlayerRole.HUNTER) {
            // Добавляем компас в дропы
            for (ItemStack item : event.getDrops()) {
                if (plugin.getCompassManager().isTrackingCompass(item)) {
                    return; // Компас уже есть в дропах
                }
            }
            
            // Если компаса нет в дропах, удаляем его из дропов и добавим при возрождении
            event.getDrops().removeIf(item -> plugin.getCompassManager().isTrackingCompass(item));
        }
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
        
        if (gamePlayer != null) {
            // Возвращаем компас охотнику после респавна (если у него нет кровати/якоря)
            if (gamePlayer.getRole() == PlayerRole.HUNTER && gamePlayer.isAlive()) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getCompassManager().giveCompass(player);
                }, 1L);
            }
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        
        // Запрещаем выбрасывать компас
        if (plugin.getCompassManager().isTrackingCompass(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cВы не можете выбросить компас охотника!");
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Проверка GUI выбора роли
        if (event.getInventory().getHolder() == null && 
            "§6Выбор роли - Manhunt".equals(event.getView().getTitle())) {
            
            event.setCancelled(true);
            handleRoleSelectionClick(player, event);
            return;
        }
        
        // Разрешаем перемещать компас в инвентаре, но запрещаем выбрасывать
        ItemStack item = event.getCurrentItem();
        if (plugin.getCompassManager().isTrackingCompass(item) && 
            event.getAction().toString().contains("DROP")) {
            event.setCancelled(true);
            player.sendMessage("§cВы не можете выбросить компас охотника!");
        }
    }
    
    private void handleRoleSelectionClick(Player player, InventoryClickEvent event) {
        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
        if (gamePlayer == null) return;
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 11: // Спидранер
                plugin.getGameManager().setPlayerRoleAndTeam(player, PlayerRole.SPEEDRUNNER);
                player.sendMessage("§aВы выбрали роль: §aСпидранер");
                break;
                
            case 13: // Охотник
                plugin.getGameManager().setPlayerRoleAndTeam(player, PlayerRole.HUNTER);
                player.sendMessage("§aВы выбрали роль: §cОхотник");
                break;
                
            case 15: // Судья
                plugin.getGameManager().setPlayerRoleAndTeam(player, PlayerRole.JUDGE);
                player.sendMessage("§aВы выбрали роль: §6Судья");
                break;
                
            case 19: // Уменьшить жизни (только спидранер)
                if (gamePlayer.getRole() == PlayerRole.SPEEDRUNNER) {
                    gamePlayer.setLives(gamePlayer.getLives() - 1);
                    player.sendMessage("§eЖизни: " + gamePlayer.getLives());
                }
                break;
                
            case 21: // Увеличить жизни (только спидранер)
                if (gamePlayer.getRole() == PlayerRole.SPEEDRUNNER) {
                    gamePlayer.setLives(gamePlayer.getLives() + 1);
                    player.sendMessage("§eЖизни: " + gamePlayer.getLives());
                }
                break;
                
            case 1: // Уменьшить время заморозки (только судья)
                if (gamePlayer.getRole() == PlayerRole.JUDGE) {
                    plugin.getGameManager().setFreezeTime(plugin.getGameManager().getFreezeTime() - 10);
                    player.sendMessage("§eВремя заморозки: " + plugin.getGameManager().getFreezeTime() + " сек");
                }
                break;
                
            case 3: // Увеличить время заморозки (только судья)
                if (gamePlayer.getRole() == PlayerRole.JUDGE) {
                    plugin.getGameManager().setFreezeTime(plugin.getGameManager().getFreezeTime() + 10);
                    player.sendMessage("§eВремя заморозки: " + plugin.getGameManager().getFreezeTime() + " сек");
                }
                break;
                
            case 22: // Начать игру
                if (gamePlayer.getRole() == PlayerRole.JUDGE || player.isOp()) {
                    player.closeInventory();
                    plugin.getGameManager().startGame();
                } else {
                    player.sendMessage("§cТолько судья может начать игру!");
                }
                return;
                
            case 26: // Остановить игру
                if (gamePlayer.getRole() == PlayerRole.JUDGE || player.isOp()) {
                    player.closeInventory();
                    plugin.getGameManager().stopGame();
                } else {
                    player.sendMessage("§cТолько судья может остановить игру!");
                }
                return;
        }
        
        // Обновляем GUI
        new RoleSelectionGUI(plugin, player).open();
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
        
        if (gamePlayer != null) {
            // Форматируем сообщение с ролью и цветом
            String rolePrefix = gamePlayer.getRole().getTitleSuffix();
            String colorCode = gamePlayer.getRole().getColorCode();
            
            // Формат: ЦветНик [Роль]: сообщение
            String format = colorCode + "%1$s " + rolePrefix + "§f: %2$s";
            event.setFormat(format);
        }
    }
    
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        
        // Проверяем, что это портал в Энд
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            // Получаем наш сгенерированный мир Края
            World endWorld = plugin.getWorldManager().getEndWorld();
            
            if (endWorld != null) {
                // Устанавливаем место назначения на спавн нашего мира Края
                Location endSpawn = endWorld.getSpawnLocation();
                event.setTo(endSpawn);
                
                player.sendMessage("§eВы телепортируетесь в мир Края Manhunt!");
                plugin.getLogger().info("Игрок " + player.getName() + " телепортирован в мир Края плагина");
            } else {
                plugin.getLogger().warning("Мир Края плагина не найден для игрока " + player.getName());
            }
        }
    }
}