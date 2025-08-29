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
            
            // Устанавливаем спавн подальше от центра
            Location endSpawn = new Location(endWorld, 100, 64, 0);
            endWorld.setSpawnLocation(endSpawn);
            
            // Принудительно спавним дракона Края если его нет
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                ensureEnderDragonExists(endWorld);
            }, 60L); // Задержка в 3 секунды для полной загрузки мира
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
        
        // Проверяем наличие дракона в мире
        boolean dragonExists = !endWorld.getEntitiesByClass(org.bukkit.entity.EnderDragon.class).isEmpty();
        
        if (!dragonExists) {
            plugin.getLogger().info("Дракон Края не найден, спавним дракона...");
            
            // Спавним дракона в центре острова Края (стандартная позиция)
            Location dragonSpawn = new Location(endWorld, 0, 128, 0);
            
            // Убеждаемся что чанк загружен
            Chunk dragonChunk = endWorld.getChunkAt(dragonSpawn);
            dragonChunk.load(true);
            dragonChunk.setForceLoaded(true);
            
            // Загружаем соседние чанки для стабильности
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Chunk chunk = endWorld.getChunkAt(dragonChunk.getX() + dx, dragonChunk.getZ() + dz);
                    chunk.load(true);
                    chunk.setForceLoaded(true);
                }
            }
            
            // Увеличенная задержка для полной загрузки всех чанков
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                spawnEnderDragonWithAI(endWorld, dragonSpawn);
            }, 100L); // 5 секунд задержки для полной загрузки
        } else {
            plugin.getLogger().info("Дракон Края уже существует в мире");
        }
    }
    
    /**
     * Спавнит дракона Края с полным ИИ
     */
    private void spawnEnderDragonWithAI(World endWorld, Location spawnLoc) {
        try {
            // Очищаем область от возможных препятствий
            for (int x = -5; x <= 5; x++) {
                for (int y = -5; y <= 10; y++) {
                    for (int z = -5; z <= 5; z++) {
                        Location clearLoc = spawnLoc.clone().add(x, y, z);
                        if (clearLoc.getBlock().getType() != Material.AIR && 
                            clearLoc.getBlock().getType() != Material.END_STONE) {
                            clearLoc.getBlock().setType(Material.AIR);
                        }
                    }
                }
            }
            
            // Спавним дракона
            org.bukkit.entity.EnderDragon dragon = (org.bukkit.entity.EnderDragon) endWorld.spawnEntity(spawnLoc, org.bukkit.entity.EntityType.ENDER_DRAGON);
            
            if (dragon != null) {
                plugin.getLogger().info("Дракон заспавнен, настраиваем ИИ...");
                
                // Базовые настройки дракона
                dragon.setMaxHealth(200.0);
                dragon.setHealth(200.0);
                dragon.setRemoveWhenFarAway(false);
                dragon.setPersistent(true);
                dragon.setAI(true);
                
                // Сброс состояния дракона
                dragon.setTarget(null);
                dragon.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                
                // Принудительная активация через несколько тиков
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (dragon.isValid()) {
                        dragon.setAI(true);
                        
                        // Попытка "разбудить" дракона, заставив его заметить игроков
                        for (Player player : endWorld.getPlayers()) {
                            if (player.getLocation().distance(dragon.getLocation()) < 100) {
                                dragon.setTarget(player);
                                break;
                            }
                        }
                        
                        plugin.getLogger().info("ИИ дракона активирован повторно с целью!");
                    }
                }, 60L); // 3 секунды
                
                // Еще одна попытка через больший интервал
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (dragon.isValid()) {
                        dragon.setAI(true);
                        
                        // Принудительное движение дракона
                        org.bukkit.util.Vector randomVelocity = new org.bukkit.util.Vector(
                            (Math.random() - 0.5) * 0.5,
                            0.2,
                            (Math.random() - 0.5) * 0.5
                        );
                        dragon.setVelocity(randomVelocity);
                        
                        plugin.getLogger().info("Дракон принудительно активирован с движением!");
                    }
                }, 120L); // 6 секунд
                
                plugin.getLogger().info("Дракон Края успешно заспавнен!");
            } else {
                plugin.getLogger().warning("Не удалось заспавнить дракона Края!");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при спавне дракона: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Создает портал в Незер в указанной локации
     */
    private void createNetherPortal(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        
        // Находим подходящую высоту для портала
        if (world.getEnvironment() == World.Environment.NETHER) {
            // В Незере ищем безопасное место
            for (int checkY = Math.max(10, y - 5); checkY <= Math.min(120, y + 10); checkY++) {
                Location checkLoc = new Location(world, x, checkY, z);
                boolean canPlace = true;
                
                // Проверяем область 4x5x3 для портала
                for (int dx = -1; dx <= 2; dx++) {
                    for (int dy = 0; dy <= 4; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            Location blockLoc = checkLoc.clone().add(dx, dy, dz);
                            Material blockType = blockLoc.getBlock().getType();
                            if (blockType == Material.LAVA || blockType == Material.BEDROCK) {
                                canPlace = false;
                                break;
                            }
                        }
                        if (!canPlace) break;
                    }
                    if (!canPlace) break;
                }
                
                if (canPlace) {
                    y = checkY;
                    break;
                }
            }
        } else {
            // В обычном мире используем высоту поверхности
            y = Math.max(y, world.getHighestBlockYAt(x, z) + 1);
        }
        
        // Очищаем область для портала
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Location clearLoc = new Location(world, x + dx, y + dy, z + dz);
                    if (clearLoc.getBlock().getType() != Material.BEDROCK) {
                        clearLoc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        
        // Создаем основание портала если нужно
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Location baseLoc = new Location(world, x + dx, y - 1, z + dz);
                if (baseLoc.getBlock().getType() == Material.AIR || baseLoc.getBlock().getType() == Material.LAVA) {
                    baseLoc.getBlock().setType(world.getEnvironment() == World.Environment.NETHER ? 
                        Material.NETHERRACK : Material.STONE);
                }
            }
        }
        
        // Создаем рамку портала из обсидиана
        Material obsidian = Material.OBSIDIAN;
        Material portal = Material.NETHER_PORTAL;
        
        // Нижняя и верхняя части рамки
        for (int dx = 0; dx <= 1; dx++) {
            world.getBlockAt(x + dx, y, z).setType(obsidian);
            world.getBlockAt(x + dx, y + 4, z).setType(obsidian);
        }
        
        // Боковые части рамки
        for (int dy = 1; dy <= 3; dy++) {
            world.getBlockAt(x - 1, y + dy, z).setType(obsidian);
            world.getBlockAt(x + 2, y + dy, z).setType(obsidian);
        }
        
        // Задержка перед созданием портала для стабильности
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Внутренняя часть портала
            for (int dx = 0; dx <= 1; dx++) {
                for (int dy = 1; dy <= 3; dy++) {
                    world.getBlockAt(x + dx, y + dy, z).setType(portal);
                }
            }
            plugin.getLogger().info("Портал активирован в мире " + world.getName());
        }, 5L);
        
        plugin.getLogger().info("Создан портал в мире " + world.getName() + " на координатах: " + x + ", " + y + ", " + z);
    }
    
    public void regenerateGameWorlds(Runnable callback) {
        plugin.getLogger().info("Регенерация игровых миров...");
        
        // Сохраняем имена старых миров для удаления
        String oldGameWorldNameTemp = null;
        String oldNetherWorldNameTemp = null;
        String oldEndWorldNameTemp = null;
        
        if (gameWorld != null) {
            oldGameWorldNameTemp = gameWorld.getName();
            // Телепортируем всех игроков из мира перед выгрузкой
            teleportPlayersFromWorld(gameWorld);
            plugin.getLogger().info("Выгружаем игровой мир: " + oldGameWorldNameTemp);
            boolean unloaded = Bukkit.unloadWorld(gameWorld, false);
            plugin.getLogger().info("Игровой мир выгружен: " + unloaded);
        }
        
        if (netherWorld != null) {
            oldNetherWorldNameTemp = netherWorld.getName();
            teleportPlayersFromWorld(netherWorld);
            plugin.getLogger().info("Выгружаем мир Ада: " + oldNetherWorldNameTemp);
            boolean unloaded = Bukkit.unloadWorld(netherWorld, false);
            plugin.getLogger().info("Мир Ада выгружен: " + unloaded);
        }
        
        if (endWorld != null) {
            oldEndWorldNameTemp = endWorld.getName();
            teleportPlayersFromWorld(endWorld);
            plugin.getLogger().info("Выгружаем мир Края: " + oldEndWorldNameTemp);
            boolean unloaded = Bukkit.unloadWorld(endWorld, false);
            plugin.getLogger().info("Мир Края выгружен: " + unloaded);
        }
        
        // Создаем final копии для использования в lambda
        final String finalOldGameWorldName = oldGameWorldNameTemp;
        final String finalOldNetherWorldName = oldNetherWorldNameTemp;
        final String finalOldEndWorldName = oldEndWorldNameTemp;
        
        // Удаление старых миров с диска с задержкой
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (finalOldGameWorldName != null) {
                deleteWorldFromDisk(finalOldGameWorldName);
            }
            if (finalOldNetherWorldName != null) {
                deleteWorldFromDisk(finalOldNetherWorldName);
            }
            if (finalOldEndWorldName != null) {
                deleteWorldFromDisk(finalOldEndWorldName);
            }
            
            // Создание новых миров после удаления старых
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                loadGameWorld();
                if (callback != null) {
                    callback.run();
                }
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