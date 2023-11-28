package site.albaniacraft.towns.model

import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import site.albaniacraft.towns.TownManager

class TownEventListener(private val townManager: TownManager) : Listener {
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val fromChunk = event.from.chunk
        val toChunk = event.to.chunk

        val fromTown = townManager.getTownAtChunk(fromChunk.x, fromChunk.z)
        val toTown = townManager.getTownAtChunk(toChunk.x, toChunk.z)

        if (fromTown != null && toTown == null) {
            // Player left a town
            player.sendTitle("${ChatColor.RED}Entering Wilderness", "", 5, 10, 5)
        } else if (fromTown == null && toTown != null) {
            // Player entered a town
            player.sendTitle("${ChatColor.GREEN}Entering ${toTown.name}", "", 5, 10, 5)
        }
    }
}
