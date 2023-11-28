package site.albaniacraft.towns.Commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import site.albaniacraft.towns.TownManager

class TownUnclaimCommand(private val townManager: TownManager) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Check if the command sender is a player
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

        val chunk = sender.location.chunk
        val chunkX = chunk.x
        val chunkZ = chunk.z

        if (!townManager.isChunkClaimedByTown(playerTown, chunkX, chunkZ)) {
            sender.sendMessage("This chunk is not claimed by your town.")
            return true
        }

        val unclaimed = townManager.unclaimChunkForTown(playerTown, chunkX, chunkZ)

        if (unclaimed) {
            sender.sendMessage("Chunk unclaimed for your town. [$chunkX,$chunkZ]")
        } else {
            sender.sendMessage("Failed to unclaim the chunk. It may not be claimed by your town or there was an error.")
            // You can add more detailed error logging here
            println("Failed to unclaim chunk [$chunkX,$chunkZ] for town $playerTown")
        }

        return true
    }
}
