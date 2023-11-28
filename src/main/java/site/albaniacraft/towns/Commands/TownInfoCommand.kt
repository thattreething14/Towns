package site.albaniacraft.towns.Commands

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.TownManager
import site.albaniacraft.towns.db.DatabaseInitializer
import site.albaniacraft.towns.npcs.NPCBanker

class TownInfoCommand(
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

        if (args.isEmpty()) {
            sender.sendMessage(ChatColor.RED.toString() + "Usage: /town <townName>")
            return true
        }

        val townName = args[1]
        val town = townManager.getTown(townName)
        if (town.name.isEmpty()) {
            sender.sendMessage(ChatColor.RED.toString() + "Town not found.")
            return true
        }
        val claimedChunks = townManager.getClaimedChunksCount(townName)
        val townBalance = npcBanker.getTownBalance(townName)

        sender.sendMessage(ChatColor.YELLOW.toString() + "+-------${town.name}-------+")
        sender.sendMessage(ChatColor.GREEN.toString() + "Mayor: ${town.founder}")
        sender.sendMessage(ChatColor.GREEN.toString() + "Members: ${town.members.joinToString(", ")}")
        sender.sendMessage(ChatColor.GREEN.toString() + "Claimed Chunks: $claimedChunks")
        sender.sendMessage(ChatColor.GREEN.toString() + "Town Balance: $townBalance")
        return true
    }
}
