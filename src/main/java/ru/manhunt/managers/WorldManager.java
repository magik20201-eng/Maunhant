package ru.manhunt.managers;

import org.bukkit.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
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
        WorldCreator gameCreator = new WorldCreator("manhunt_game");
        gameCreator.seed(newSeed);
        gameWorld = gameCreator.createWorld();
        
        if (gameWorld != null) {
            gameWorld.setDifficulty(Difficulty.NORMAL);
            gameWorld.setGameRule(GameRule.KEEP_INVENTORY, false);
            gameWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            gameWorld.setTime(6000); // Устанавливаем день
        }
    }
    
    public void regenerateGameWorlds() {
        plugin.getLogger().info("Регенерация игровых миров...");
        
        // Сохраняем имена старых миров для удаления
        String oldGameWorldName = null;
        if (gameWorld != null) {
            oldGameWorldName = gameWorld.getName();
            Bukkit.unloadWorld(gameWorld, false);
        }
        
        // Полное удаление старых миров с диска
        if (oldGameWorldName != null) {
            deleteWorldFromDisk(oldGameWorldName);
            deleteWorldFromDisk(oldGameWorldName + "_nether");
            deleteWorldFromDisk(oldGameWorldName + "_the_end");
        }
        
        // Создание нового игрового мира с новым сидом
        loadGameWorld();
        
        plugin.getLogger().info("Регенерация миров завершена!");
    }
    
    /**
     * Полностью удаляет мир с диска
     */
    private void deleteWorldFromDisk(String worldName) {
        try {
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (worldFolder.exists() && worldFolder.isDirectory()) {
                plugin.getLogger().info("Удаление мира: " + worldName);
                deleteDirectoryRecursively(worldFolder.toPath());
                plugin.getLogger().info("Мир " + worldName + " успешно удален");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось удалить мир " + worldName + ": " + e.getMessage());
        }
    }
    
    /**
     * Рекурсивно удаляет директорию и все её содержимое
     */
    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(file -> {
                    if (!file.delete()) {
                        plugin.getLogger().warning("Не удалось удалить файл: " + file.getAbsolutePath());
                    }
                });
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