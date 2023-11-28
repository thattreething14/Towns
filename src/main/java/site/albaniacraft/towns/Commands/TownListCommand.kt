package site.albaniacraft.towns.Commands

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.TownManager

class TownListCommand(private val plugin: JavaPlugin, private val townManager: TownManager) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(ChatColor.RED.toString() + "Only players can use this command.")
            return true
        }

        val towns = townManager.getAllTowns()

        if (towns.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN.toString() + "There are no towns.")
        } else {
            sender.sendMessage(ChatColor.YELLOW.toString() + "List of Towns:")
            for (town in towns) {
                sender.sendMessage(ChatColor.GREEN.toString() + town)
            }
        }

        return true
    }
}
