package site.albaniacraft.towns.model

import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import site.albaniacraft.towns.TownManager
class TownProtectionListener(private val townManager: TownManager) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock
        val action = event.action

        // Check if the event involves a block interaction
        if (block == null) {
            return
        }

        // Get the chunk coordinates of the block
        val chunkX = block.chunk.x
        val chunkZ = block.chunk.z

        // Check if the player is a member of a town
        val playerName = player.name
        val playerTown = townManager.getPlayerTown(playerName)
        if (playerTown == null) {
            return  // Player doesn't belong to any town, no protection needed
        }

        // Check if the block is claimed by a town
        val townClaimed = townManager.getTownAtChunk(chunkX, chunkZ)

        if (townClaimed == null) {
            return  // Block is not claimed, no protection needed
        }

        // Check if the player is a member of the town that claimed the chunk
        if (playerTown != townClaimed.name) {
            // Cancel both right-click and left-click interactions
            if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
                event.isCancelled = true
                player.sendMessage("${ChatColor.RED}You can't interact with this block in a claimed town.")
            }
        }
    }
}
