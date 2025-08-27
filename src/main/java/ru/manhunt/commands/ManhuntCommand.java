package ru.manhunt.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.manhunt.ManhuntPlugin;

public class ManhuntCommand implements CommandExecutor {
    
    private final ManhuntPlugin plugin;
    
    public ManhuntCommand(ManhuntPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭта команда доступна только игрокам!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Добавляем игрока в игру если его там нет
        if (plugin.getGameManager().getGamePlayer(player) == null) {
            plugin.getGameManager().addPlayer(player);
        }
        
        // Открываем меню выбора роли
        plugin.getGameManager().openRoleSelection(player);
        
        return true;
    }
}