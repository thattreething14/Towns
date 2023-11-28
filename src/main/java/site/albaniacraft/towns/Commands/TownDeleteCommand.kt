package site.albaniacraft.towns.Commands

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.TownManager
import site.albaniacraft.towns.db.DatabaseInitializer
import site.albaniacraft.towns.npcs.NPCBanker

class TownDeleteCommand(
    private val plugin: JavaPlugin,
    private val townManager: TownManager,
    private val databaseInitializer: DatabaseInitializer,
    private val npcBanker: NPCBanker
) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(ChatColor.RED.toString() + "Only players can use this command.")
            return true
        }

        if (args.size != 1 || args[0].equals("delete", ignoreCase = true).not()) {
            sender.sendMessage(ChatColor.RED.toString() + "Usage: /town delete")
            return true
        }

        val playerName = sender.name

        if (!townManager.playerBelongsToTown(playerName)) {
            sender.sendMessage(ChatColor.RED.toString() + "You are not a member of any town.")
            return true
        }

        val townName = townManager.getPlayerTown(playerName)

        if (townName != null && playerName != null) {
            if (townManager.isTownFounder(townName, playerName)) {
                // Remove all members from the town
                val town = townManager.getTown(townName)
                for (member in town.members) {
                    townManager.removePlayerFromTown(member, townName)
                }
                townManager.deleteTown(townName)
                npcBanker.despawnBanker(townName)
                townManager.deleteTownBalance(townName)

                Bukkit.broadcastMessage(ChatColor.GREEN.toString() + "Town '$townName' has been deleted!")
            } else {
                sender.sendMessage(ChatColor.RED.toString() + "Only the town founder can delete the town.")
            }
        } else {
            // Handle the case where townName or playerName is null
            sender.sendMessage(ChatColor.RED.toString() + "Invalid town or player name.")
        }

        return true
    }
}