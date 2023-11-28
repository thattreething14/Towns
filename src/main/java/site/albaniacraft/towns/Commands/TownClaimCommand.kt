package site.albaniacraft.towns.Commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import site.albaniacraft.towns.TownManager
import site.albaniacraft.towns.npcs.NPCBanker

class TownClaimCommand(
    private val townManager: TownManager,
    private val npcBanker: NPCBanker
) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
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

        if (townManager.isChunkClaimedOtherThanTown(chunkX, chunkZ, playerTown)) {
            sender.sendMessage("This chunk is already claimed by another town.")
            return true
        }
        if (townManager.isChunkClaimedByTown(playerTown, chunkX, chunkZ)) {
            sender.sendMessage("This chunk is already claimed by your town.")
            return true
        }
        val adjacentClaimedChunks = townManager.getAdjacentClaimedChunks(playerTown, chunkX, chunkZ)

        if (adjacentClaimedChunks.isEmpty()) {
            sender.sendMessage("You can only claim chunks attached to other claimed chunks by your town.")
            return true
        }

        val claimed = townManager.claimChunkForTown(playerTown, chunkX, chunkZ)

        if (claimed) {
            val claimCost = 16.0


            val townBalance = npcBanker.getTownBalance(playerTown)

            if (townBalance >= claimCost) {
    
                npcBanker.updateTownBalance(playerTown, -claimCost)
                sender.sendMessage("Chunk claimed for your town. [$chunkX,$chunkZ]")
            } else {
    
                townManager.unclaimChunkForTown(playerTown, chunkX, chunkZ)
                sender.sendMessage("Failed to claim the chunk. Insufficient town funds.")
            }
        } else {
            sender.sendMessage("Failed to claim the chunk. It may already be claimed or there was an error.")
   
            println("Failed to claim chunk [$chunkX,$chunkZ] for town $playerTown}")
        }

        return true
    }
}
