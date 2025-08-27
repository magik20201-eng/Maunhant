package ru.manhunt.managers;

import org.bukkit.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Comparator;
import ru.manhunt.ManhuntPlugin;

public class WorldManager {
    
    private final ManhuntPlugin plugin;
    private World lobbyWorld;
    private World gameWorld;
    private World netherWorld;
    private World endWorld;
    
    public WorldManager(ManhuntPlugin plugin) {
        this.plugin = plugin;
        createWorlds();
    }
    
    private void createWorlds() {
        // Создание лобби мира (пустой мир)
        WorldCreator lobbyCreator = new WorldCreator("manhunt_lobby");
        lobbyCreator.generator(new VoidGenerator());
        lobbyCreator.generateStructures(false);
        lobbyWorld = lobbyCreator.createWorld();
        
        if (lobbyWorld != null) {
            lobbyWorld.setSpawnFlags(false, false);
            lobbyWorld.setDifficulty(Difficulty.PEACEFUL);
            lobbyWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            lobbyWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            lobbyWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
            lobbyWorld.setPVP(false); // Запрет PvP в лобби
            lobbyWorld.setTime(6000); // День
            
            // Создание платформы спавна
            Location spawnLoc = new Location(lobbyWorld, 0, 100, 0);
            createSpawnPlatform(spawnLoc);
            lobbyWorld.setSpawnLocation(spawnLoc);
        }
        
        // Игровой мир создается при старте игры
        loadGameWorld();
    }
    
    private void loadGameWorld() {
        // Генерируем новый случайный сид для каждой игры
        long newSeed = new Random().nextLong();
        
        // Создание обычного мира
        WorldCreator gameCreator = new WorldCreator("manhunt_game");
        gameCreator.seed(newSeed);
        gameCreator.generateStructures(true);
        gameWorld = gameCreator.createWorld();
        
        if (gameWorld != null) {
            gameWorld.setDifficulty(Difficulty.NORMAL);
            gameWorld.setGameRule(GameRule.KEEP_INVENTORY, false);
            gameWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            gameWorld.setTime(6000); // Устанавливаем день
        }
        
        // Создание мира Ада с тем же сидом
        WorldCreator netherCreator = new WorldCreator("manhunt_game_nether");
        netherCreator.seed(newSeed);
        netherCreator.environment(World.Environment.NETHER);
        netherCreator.generateStructures(true);
        netherWorld = netherCreator.createWorld();
        
        if (netherWorld != null) {
            netherWorld.setDifficulty(Difficulty.NORMAL);
            netherWorld.setGameRule(GameRule.KEEP_INVENTORY, false);
            netherWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        }
        
        // Создание мира Края с тем же сидом
        WorldCreator endCreator = new WorldCreator("manhunt_game_the_end");
        endCreator.seed(newSeed);
        endCreator.environment(World.Environment.THE_END);
        endCreator.generateStructures(true);
        endWorld = endCreator.createWorld();
        
        if (endWorld != null) {
            endWorld.setDifficulty(Difficulty.NORMAL);
            endWorld.setGameRule(GameRule.KEEP_INVENTORY, false);
            endWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            
            // Принудительно спавним дракона Края если его нет
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                ensureEnderDragonExists(endWorld);
            }, 20L); // Задержка в 1 секунду для полной загрузки мира
        }
        
        plugin.getLogger().info("Созданы игровые миры с сидом: " + newSeed);
    }
    
    /**
     * Проверяет наличие дракона Края и спавнит его если необходимо
     */
    private void ensureEnderDragonExists(World endWorld) {
        if (endWorld == null || !endWorld.getEnvironment().equals(World.Environment.THE_END)) {
            return;
        }
        
        // Проверяем, есть ли дракон в мире
        boolean dragonExists = endWorld.getEntitiesByClass(org.bukkit.entity.EnderDragon.class).size() > 0;
        
        if (!dragonExists) {
            plugin.getLogger().info("Дракон Края не найден, принудительно спавним...");
            
            // Получаем центральную локацию острова Края (0, 64, 0)
            Location dragonSpawn = new Location(endWorld, 0, 64, 0);
            
            // Спавним дракона
            endWorld.spawnEntity(dragonSpawn, org.bukkit.entity.EntityType.ENDER_DRAGON);
            
            plugin.getLogger().info("Дракон Края успешно заспавнен!");
        } else {
            plugin.getLogger().info("Дракон Края уже существует в мире");
        }
    }
    
    public void regenerateGameWorlds() {
        plugin.getLogger().info("Регенерация игровых миров...");
        
        // Сохраняем имена старых миров для удаления
        String oldGameWorldName = null;
        String oldNetherWorldName = null;
        String oldEndWorldName = null;
        
        if (gameWorld != null) {
            oldGameWorldName = gameWorld.getName();
            // Телепортируем всех игроков из мира перед выгрузкой
            teleportPlayersFromWorld(gameWorld);
            plugin.getLogger().info("Выгружаем игровой мир: " + oldGameWorldName);
            boolean unloaded = Bukkit.unloadWorld(gameWorld, false);
            plugin.getLogger().info("Игровой мир выгружен: " + unloaded);
        }
        
        if (netherWorld != null) {
            oldNetherWorldName = netherWorld.getName();
            teleportPlayersFromWorld(netherWorld);
            plugin.getLogger().info("Выгружаем мир Ада: " + oldNetherWorldName);
            boolean unloaded = Bukkit.unloadWorld(netherWorld, false);
            plugin.getLogger().info("Мир Ада выгружен: " + unloaded);
        }
        
        if (endWorld != null) {
            oldEndWorldName = endWorld.getName();
            teleportPlayersFromWorld(endWorld);
            plugin.getLogger().info("Выгружаем мир Края: " + oldEndWorldName);
            boolean unloaded = Bukkit.unloadWorld(endWorld, false);
            plugin.getLogger().info("Мир Края выгружен: " + unloaded);
        }
        
        // Удаление старых миров с диска с задержкой
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (oldGameWorldName != null) {
                deleteWorldFromDisk(oldGameWorldName);
            }
            if (oldNetherWorldName != null) {
                deleteWorldFromDisk(oldNetherWorldName);
            }
            if (oldEndWorldName != null) {
                deleteWorldFromDisk(oldEndWorldName);
            }
            
            // Создание новых миров после удаления старых
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                loadGameWorld();
            }, 10L);
            
        }, 5L); // Задержка в 0.25 секунды для полной выгрузки
        
        plugin.getLogger().info("Регенерация миров завершена!");
    }
    
    /**
     * Телепортирует всех игроков из указанного мира в лобби
     */
    private void teleportPlayersFromWorld(World world) {
        if (world == null || lobbyWorld == null) return;
        
        Location lobbySpawn = lobbyWorld.getSpawnLocation();
        for (Player player : world.getPlayers()) {
            player.teleport(lobbySpawn);
            plugin.getLogger().info("Игрок " + player.getName() + " телепортирован из мира " + world.getName());
        }
    }
    
    /**
     * Полностью удаляет мир с диска
     */
    private void deleteWorldFromDisk(String worldName) {
        try {
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (worldFolder.exists() && worldFolder.isDirectory()) {
                plugin.getLogger().info("Удаление мира: " + worldName);
                plugin.getLogger().info("Путь к миру: " + worldFolder.getAbsolutePath());
                deleteDirectoryRecursively(worldFolder.toPath());
                plugin.getLogger().info("Мир " + worldName + " успешно удален");
            }
                plugin.getLogger().info("Папка мира " + worldName + " не найдена или не является директорией");
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось удалить мир " + worldName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Рекурсивно удаляет директорию и все её содержимое
     */
    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            plugin.getLogger().info("Начинаем рекурсивное удаление: " + path.toString());
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(file -> {
                    plugin.getLogger().info("Удаляем файл/папку: " + file.getAbsolutePath());
                    if (!file.delete()) {
                        plugin.getLogger().warning("Не удалось удалить файл: " + file.getAbsolutePath());
                    } else {
                        plugin.getLogger().info("Успешно удален: " + file.getAbsolutePath());
                    }
                });
        } else {
            plugin.getLogger().info("Путь не существует: " + path.toString());
        }
    }
    
    private void createSpawnPlatform(Location center) {
        Material platform = Material.QUARTZ_BLOCK;
        Material barrier = Material.BARRIER;
        
        // Платформа 5x5
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location loc = center.clone().add(x, -1, z);
                loc.getBlock().setType(platform);
            }
        }
        
        // Барьеры по краям
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                    for (int y = 0; y <= 3; y++) {
                        Location loc = center.clone().add(x, y, z);
                        loc.getBlock().setType(barrier);
                    }
                }
            }
        }
    }
    
    public Location getLobbySpawn() {
        return lobbyWorld != null ? lobbyWorld.getSpawnLocation() : null;
    }
    
    public Location getGameSpawn() {
        if (gameWorld != null) {
            Location spawn = gameWorld.getSpawnLocation();
            // Поиск безопасного места спавна
            spawn = gameWorld.getHighestBlockAt(spawn.getBlockX(), spawn.getBlockZ()).getLocation().add(0, 1, 0);
            return spawn;
        }
        return null;
    }
    
    public World getLobbyWorld() {
        return lobbyWorld;
    }
    
    public World getGameWorld() {
        return gameWorld;
    }
    
    public World getNetherWorld() {
        return netherWorld;
    }
    
    public World getEndWorld() {
        return endWorld;
    }
    
    // Генератор пустого мира
    public static class VoidGenerator extends ChunkGenerator {
        @Override
        public void generateNoise(WorldInfo worldInfo, java.util.Random random, int chunkX, int chunkZ, ChunkData chunkData) {
            // Пустой чанк
        }
        
        @Override
        public void generateSurface(WorldInfo worldInfo, java.util.Random random, int chunkX, int chunkZ, ChunkData chunkData) {
            // Пустая поверхность
        }
        
        @Override
        public void generateBedrock(WorldInfo worldInfo, java.util.Random random, int chunkX, int chunkZ, ChunkData chunkData) {
            // Без бедрока
        }
        
        @Override
        public void generateCaves(WorldInfo worldInfo, java.util.Random random, int chunkX, int chunkZ, ChunkData chunkData) {
            // Без пещер
        }
    }
}