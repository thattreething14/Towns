package site.albaniacraft.towns.Commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import site.albaniacraft.towns.TownManager

class TownLeaveCommand(private val townManager: TownManager) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can use this command.")
            return true
        }

        val playerName = sender.name

        val playerTown = townManager.getPlayerTown(playerName)

        if (playerTown == null) {
            sender.sendMessage("You are not a member of any town.")
            return true
        }

        val town = townManager.getTown(playerTown)

        if (town.isMayor(playerName)) {
            sender.sendMessage("Mayors cannot leave the town.")
            return true
        }

        townManager.removePlayerFromTown(playerName, playerTown)
        sender.sendMessage("You have left the town.")

        return true
    }
}

