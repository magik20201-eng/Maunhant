package ru.manhunt.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import ru.manhunt.ManhuntPlugin;
import ru.manhunt.data.GamePlayer;
import ru.manhunt.enums.PlayerRole;

public class CompassManager implements Listener {

    private final ManhuntPlugin plugin;

    public CompassManager(ManhuntPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void giveCompass(Player hunter) {
        ItemStack compass = createTrackingCompass();
        hunter.getInventory().addItem(compass);
        hunter.sendMessage("§aВы получили компас слежения!");
    }

    private ItemStack createTrackingCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§cКомпас Охотника");
            meta.setLore(java.util.Arrays.asList(
                "§7Всегда указывает на спидранера",
                "§7Нельзя выбросить или потерять"
            ));
            compass.setItemMeta(meta);
        }

        return compass;
    }

    public void updateCompasses() {
        GamePlayer speedrunner = null;
        for (GamePlayer gp : plugin.getGameManager().getPlayers().values()) {
            if (gp.getRole() == PlayerRole.SPEEDRUNNER && gp.isAlive()) {
                speedrunner = gp;
                break;
            }
        }

        if (speedrunner == null) return;

        Location speedrunnerLoc = speedrunner.getPlayer().getLocation();

        for (GamePlayer gp : plugin.getGameManager().getPlayers().values()) {
            if (gp.getRole() == PlayerRole.HUNTER) {
                updateHunterCompass(gp.getPlayer(), speedrunnerLoc);
            }
        }
    }

    private void updateHunterCompass(Player hunter, Location speedrunnerLoc) {
        for (ItemStack item : hunter.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                CompassMeta meta = (CompassMeta) item.getItemMeta();
                if (meta != null && "§cКомпас Охотника".equals(meta.getDisplayName())) {
                    Location targetLoc = calculateCompassTarget(hunter, speedrunnerLoc);
                    meta.setLodestone(targetLoc);
                    meta.setLodestoneTracked(false);
                    item.setItemMeta(meta);
                    break;
                }
            }
        }
    }

    private Location calculateCompassTarget(Player hunter, Location speedrunnerLoc) {
        World hunterWorld = hunter.getWorld();
        World speedrunnerWorld = speedrunnerLoc.getWorld();

        if (hunterWorld.equals(speedrunnerWorld)) {
            return speedrunnerLoc;
        }

        return findNearestPortal(hunter, speedrunnerWorld);
    }

    private Location findNearestPortal(Player hunter, World targetWorld) {
        World hunterWorld = hunter.getWorld();
        Location hunterLoc = hunter.getLocation();

        Location nearestPortal = null;
        double nearestDistance = Double.MAX_VALUE;

        if (targetWorld.getEnvironment() == World.Environment.NETHER ||
            targetWorld.getEnvironment() == World.Environment.NORMAL) {

            for (int x = -100; x <= 100; x += 16) {
                for (int z = -100; z <= 100; z += 16) {
                    Location checkLoc = hunterLoc.clone().add(x, 0, z);
                    if (isPortalNearby(checkLoc, Material.NETHER_PORTAL)) {
                        double distance = checkLoc.distance(hunterLoc);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestPortal = checkLoc;
                        }
                    }
                }
            }
        }

        if (targetWorld.getEnvironment() == World.Environment.THE_END) {
            for (int x = -100; x <= 100; x += 16) {
                for (int z = -100; z <= 100; z += 16) {
                    Location checkLoc = hunterLoc.clone().add(x, 0, z);
                    if (isPortalNearby(checkLoc, Material.END_PORTAL)) {
                        double distance = checkLoc.distance(hunterLoc);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestPortal = checkLoc;
                        }
                    }
                }
            }
        }

        return nearestPortal != null ? nearestPortal : hunterLoc;
    }

    private boolean isPortalNearby(Location center, Material portalMaterial) {
        for (int x = -8; x <= 8; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -8; z <= 8; z++) {
                    Location checkLoc = center.clone().add(x, y, z);
                    if (checkLoc.getBlock().getType() == portalMaterial) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }

        CompassMeta meta = (CompassMeta) item.getItemMeta();
        return meta != null && "§cКомпас Охотника".equals(meta.getDisplayName());
    }

    // --- Новый код для предотвращения выпадения компаса ---

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        GamePlayer gp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (gp != null && gp.getRole() == PlayerRole.HUNTER) {
            event.getDrops().removeIf(this::isTrackingCompass);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GamePlayer gp = plugin.getGameManager().getPlayers().get(player.getUniqueId());

        if (gp != null && gp.getRole() == PlayerRole.HUNTER && gp.isAlive()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Проверяем, есть ли уже компас охотника в инвентаре
                boolean hasCompass = false;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (isTrackingCompass(item)) {
                        hasCompass = true;
                        break;
                    }
                }

                if (!hasCompass) {
                    giveCompass(player);
                }
            }, 2L);
        }
    }
}
