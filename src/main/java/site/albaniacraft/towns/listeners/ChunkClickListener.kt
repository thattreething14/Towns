package site.albaniacraft.towns.listeners

import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import site.albaniacraft.towns.listeners.DynmapIntegration

class ChunkClickListener(private val dynmapIntegration: DynmapIntegration) : Listener {
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val clickedBlock = event.clickedBlock

        if (clickedBlock != null) {
            val location = clickedBlock.location
            handleChunkClick(player.name, location)
        }
    }
    fun handleChunkClick(playerName: String, location: Location) {
        dynmapIntegration.handleChunkClick(playerName, location)
    }

}
