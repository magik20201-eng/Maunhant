package ru.manhunt.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.manhunt.ManhuntPlugin;
import ru.manhunt.data.GamePlayer;
import ru.manhunt.enums.PlayerRole;

import java.util.Arrays;

public class RoleSelectionGUI {
    
    private final ManhuntPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    
    public RoleSelectionGUI(ManhuntPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 27, "§6Выбор роли - Manhunt");
        
        setupGUI();
    }
    
    private void setupGUI() {
        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
        
        // Получаем текущее количество игроков для каждой роли
        long speedrunnerCount = plugin.getGameManager().getSpeedrunnerCount();
        long hunterCount = plugin.getGameManager().getHunterCount();
        long judgeCount = plugin.getGameManager().getJudgeCount();
        
        // Роль спидранера
        ItemStack speedrunnerItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta speedrunnerMeta = speedrunnerItem.getItemMeta();
        if (speedrunnerMeta != null) {
            speedrunnerMeta.setDisplayName("§aСпидранер");
            speedrunnerMeta.setLore(Arrays.asList(
                "§7Ваша цель - убить дракона",
                "§7и выжить от охотников!",
                "§7Текущее: §e" + speedrunnerCount + " игроков",
                "",
                gamePlayer.getRole() == PlayerRole.SPEEDRUNNER ? "§a✓ Выбрано" : "§eНажмите для выбора"
            ));
            speedrunnerItem.setItemMeta(speedrunnerMeta);
        }
        inventory.setItem(11, speedrunnerItem);
        
        // Роль охотника
        ItemStack hunterItem = new ItemStack(Material.BOW);
        ItemMeta hunterMeta = hunterItem.getItemMeta();
        if (hunterMeta != null) {
            hunterMeta.setDisplayName("§cОхотник");
            hunterMeta.setLore(Arrays.asList(
                "§7Ваша цель - поймать",
                "§7и убить спидранера!",
                "§7Текущее: §e" + hunterCount + " игроков",
                "",
                gamePlayer.getRole() == PlayerRole.HUNTER ? "§a✓ Выбрано" : "§eНажмите для выбора"
            ));
            hunterItem.setItemMeta(hunterMeta);
        }
        inventory.setItem(13, hunterItem);
        
        // Роль судьи
        ItemStack judgeItem = new ItemStack(Material.STICK);
        ItemMeta judgeMeta = judgeItem.getItemMeta();
        if (judgeMeta != null) {
            judgeMeta.setDisplayName("§6Судья");
            judgeMeta.setLore(Arrays.asList(
                "§7Управление игрой",
                "§7и настройками",
                "§7Текущее: §e" + judgeCount + " игроков",
                "",
                gamePlayer.getRole() == PlayerRole.JUDGE ? "§a✓ Выбрано" : "§eНажмите для выбора"
            ));
            judgeItem.setItemMeta(judgeMeta);
        }
        inventory.setItem(15, judgeItem);
        
        // Настройки для спидранера
        if (gamePlayer.getRole() == PlayerRole.SPEEDRUNNER) {
            addSpeedrunnerSettings(gamePlayer);
        }
        
        // Настройки для судьи
        if (gamePlayer.getRole() == PlayerRole.JUDGE) {
            addJudgeSettings();
        }
        
        // Кнопка старта игры
        ItemStack startItem = new ItemStack(Material.EMERALD);
        ItemMeta startMeta = startItem.getItemMeta();
        if (startMeta != null) {
            startMeta.setDisplayName("§aНачать игру");
            startMeta.setLore(Arrays.asList(
                "§7Запустить!"
            ));
            startItem.setItemMeta(startMeta);
        }
        inventory.setItem(22, startItem);
    }
    
    private void addSpeedrunnerSettings(GamePlayer gamePlayer) {
        // Уменьшить жизни
        ItemStack minusLives = new ItemStack(Material.RED_CONCRETE);
        ItemMeta minusLivesMeta = minusLives.getItemMeta();
        if (minusLivesMeta != null) {
            minusLivesMeta.setDisplayName("§c- Жизнь");
            minusLivesMeta.setLore(Arrays.asList(
                "§7Уменьшить количество жизней",
                "§7Текущее: §e" + gamePlayer.getLives()
            ));
            minusLives.setItemMeta(minusLivesMeta);
        }
        inventory.setItem(19, minusLives);
        
        // Количество жизней
        ItemStack livesItem = new ItemStack(Material.TOTEM_OF_UNDYING, gamePlayer.getLives());
        ItemMeta livesMeta = livesItem.getItemMeta();
        if (livesMeta != null) {
            livesMeta.setDisplayName("§eЖизни: " + gamePlayer.getLives());
            livesMeta.setLore(Arrays.asList(
                "§7Количество жизней спидранера"
            ));
            livesItem.setItemMeta(livesMeta);
        }
        inventory.setItem(20, livesItem);
        
        // Увеличить жизни
        ItemStack plusLives = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta plusLivesMeta = plusLives.getItemMeta();
        if (plusLivesMeta != null) {
            plusLivesMeta.setDisplayName("§a+ Жизнь");
            plusLivesMeta.setLore(Arrays.asList(
                "§7Увеличить количество жизней",
                "§7Текущее: §e" + gamePlayer.getLives()
            ));
            plusLives.setItemMeta(plusLivesMeta);
        }
        inventory.setItem(21, plusLives);
    }
    
    private void addJudgeSettings() {
        int freezeTime = plugin.getGameManager().getFreezeTime();
        
        // Уменьшить время заморозки
        ItemStack minusTime = new ItemStack(Material.RED_CONCRETE);
        ItemMeta minusTimeMeta = minusTime.getItemMeta();
        if (minusTimeMeta != null) {
            minusTimeMeta.setDisplayName("§c- Время");
            minusTimeMeta.setLore(Arrays.asList(
                "§7Уменьшить время заморозки",
                "§7Текущее: §e" + freezeTime + " сек"
            ));
            minusTime.setItemMeta(minusTimeMeta);
        }
        inventory.setItem(1, minusTime);
        
        // Время заморозки
        ItemStack timeItem = new ItemStack(Material.CLOCK);
        ItemMeta timeMeta = timeItem.getItemMeta();
        if (timeMeta != null) {
            timeMeta.setDisplayName("§eВремя заморозки: " + freezeTime + " сек");
            timeMeta.setLore(Arrays.asList(
                "§7Время заморозки охотников"
            ));
            timeItem.setItemMeta(timeMeta);
        }
        inventory.setItem(2, timeItem);
        
        // Увеличить время заморозки
        ItemStack plusTime = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta plusTimeMeta = plusTime.getItemMeta();
        if (plusTimeMeta != null) {
            plusTimeMeta.setDisplayName("§a+ Время");
            plusTimeMeta.setLore(Arrays.asList(
                "§7Увеличить время заморозки",
                "§7Текущее: §e" + freezeTime + " сек"
            ));
            plusTime.setItemMeta(plusTimeMeta);
        }
        inventory.setItem(3, plusTime);
        
        // Остановка игры
        ItemStack stopItem = new ItemStack(Material.BARRIER);
        ItemMeta stopMeta = stopItem.getItemMeta();
        if (stopMeta != null) {
            stopMeta.setDisplayName("§cОстановить игру");
            stopMeta.setLore(Arrays.asList(
                "§7Принудительно остановить игру"
            ));
            stopItem.setItemMeta(stopMeta);
        }
        inventory.setItem(26, stopItem);
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
}
