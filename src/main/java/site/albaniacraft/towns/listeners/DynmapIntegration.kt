package site.albaniacraft.towns.listeners

import org.dynmap.DynmapAPI
import org.dynmap.markers.AreaMarker
import org.dynmap.markers.MarkerSet
import site.albaniacraft.towns.model.Town
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.java.JavaPlugin
import org.dynmap.markers.Marker
import site.albaniacraft.towns.TownManager
import site.albaniacraft.towns.npcs.NPCBanker

class DynmapIntegration(private val plugin: JavaPlugin, private val townManager: TownManager, private val npcBanker: NPCBanker) :
    Listener {
    private val markerAttributes: MutableMap<String, Map<String, Any>> = mutableMapOf()
    private var dynmapAPI: DynmapAPI? = null
    private var markerSet: MarkerSet? = null

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onDynmapEnable(event: PluginEnableEvent) {
        // Check if Dynmap is enabled
        if (event.plugin.name.equals("Dynmap", ignoreCase = true)) {
            // Dynmap is enabled, initialize your Dynmap-related code here
            dynmapAPI = event.plugin as? DynmapAPI

            // Ensure Dynmap is loaded
            if (dynmapAPI == null) {
                plugin.logger.warning("Dynmap not found. DynmapIntegration will not work.")
            } else {
                // Create a marker set for claimed chunks
                val localMarkerSet = dynmapAPI!!.markerAPI.getMarkerSet("claimed_chunks")
                if (localMarkerSet == null) {
                    dynmapAPI!!.markerAPI.createMarkerSet("claimed_chunks", "Claimed Chunks", null, true)
                } else {
                    markerSet = localMarkerSet // Update the markerSet
                }
            }

            // Now you can proceed with the rest of your initialization
            Bukkit.getScheduler().runTaskTimer(plugin, this::updateClaimedChunksOnMap, 0L, 120L)
        }
    }
    fun updateClaimedChunksOnMap() {
        val localMarkerSet = markerSet // Create a local reference

        if (localMarkerSet == null) return

        // Clear existing markers
        localMarkerSet.markers.forEach { it.deleteMarker() }

        // Fetch all towns
        val towns = townManager.getAllTowns()

        // Create markers for claimed chunks of each town
        for (townName in towns) {
            val town = townManager.getTownByName(townName) // Try to get the town by name

            if (town != null) {
                val claimedChunks = townManager.getClaimedChunksForTown(townName)

                if (claimedChunks.isNotEmpty()) {
                    createTownClaimedChunkMarkers(town, claimedChunks)
                }
            } else {
                // Town doesn't exist, so remove its markers
                removeTownMarkers(townName)
            }
        }
    }

    fun removeTownMarkers(townName: String) {
        println("Removing markers for town: $townName")

        val markerSetName = "town_${townName}_chunk"

        val dynmap = plugin.server.pluginManager.getPlugin("Dynmap")
        if (dynmap is DynmapAPI) {
            val markers = dynmap.markerAPI
            val markerSet = markers.getMarkerSet(markerSetName)

            if (markerSet != null) {
                println("Marker set found: $markerSetName")

                val markersToRemove = mutableListOf<Marker>()

                for (marker in markerSet.markers) {
                    if (marker.label == townName) {
                        println("Removing marker for town: $townName")
                        markersToRemove.add(marker)
                    }
                }

                markersToRemove.forEach { marker ->
                    marker.deleteMarker()
                }
            } else {
                println("Marker set not found: $markerSetName")
            }
        }
    }
    private fun createTownClaimedChunkMarkers(town: Town, claimedChunks: List<Pair<Int, Int>>) {
        val worldName = plugin.server.worlds[0].name // Change this to the name of your world
        val markerList = mutableListOf<AreaMarker>()

        for ((chunkX, chunkZ) in claimedChunks) {
            val cornerX1 = chunkX.toDouble() * 16.0
            val cornerX2 = (chunkX.toDouble() + 1.0) * 16.0
            val cornerZ1 = chunkZ.toDouble() * 16.0
            val cornerZ2 = (chunkZ.toDouble() + 1.0) * 16.0

            val chunkMarker = markerSet?.createAreaMarker(
                town.name + "_chunk_" + chunkX + "_" + chunkZ,
                town.name,
                false,
                worldName,
                doubleArrayOf(cornerX1, cornerX2),
                doubleArrayOf(cornerZ1, cornerZ2),
                false
            )
            if (chunkMarker != null) {
                val description = buildString {
                    appendLine("Town: ${town.name}")
                    appendLine("Mayor: ${town.founder}")
                    appendLine("Members: ${town.members.joinToString(", ")}")
                    appendLine("Claimed Chunks: ${townManager.getClaimedChunksCount(town.name)}")
                    appendLine("Town Balance: ${npcBanker.getTownBalance(town.name)}")
                }

                chunkMarker.description = description
                markerList.add(chunkMarker)
            }
        }
    }
    fun handleChunkClick(playerName: String, location: Location) {
        val localMarkerSet = markerSet // Create a local reference

        if (localMarkerSet == null) return

        val localMarkers = localMarkerSet.markers // Create a local reference

        for (marker in localMarkers) {
            if (isLocationWithinArea(location, marker)) {
                val townName = marker.label
                val attributes = markerAttributes[townName]

                if (attributes != null) {
                    // Access attributes like chunk boundaries here
                    val chunkX1 = attributes["chunkX1"] as Double
                    val chunkX2 = attributes["chunkX2"] as Double
                    val chunkZ1 = attributes["chunkZ1"] as Double
                    val chunkZ2 = attributes["chunkZ2"] as Double

                    // You can handle what happens when a claimed chunk is clicked here
                    // For example, send a message to the player with the town name
                    plugin.server.getPlayer(playerName)?.sendMessage("You clicked on a chunk claimed by $townName")
                    return // Exit the loop after handling the click for one marker
                }
            }
        }
    }

    private fun isLocationWithinArea(location: Location, marker: Marker): Boolean {
        val worldName = location.world.name
        val x = location.x
        val z = location.z

        if (marker is AreaMarker) {
            // Assuming it's a rectangle with 4 corners
            val minX = marker.getCornerX(0)
            val maxX = marker.getCornerX(1)
            val minZ = marker.getCornerZ(0)
            val maxZ = marker.getCornerZ(1)

            // Check if the location is within the boundaries of the rectangle defined by the corners
            return (worldName == (marker as Marker).world &&  // Explicitly specify the type
                    x >= minX && x <= maxX &&
                    z >= minZ && z <= maxZ)
        }

        return false
    }

}