package site.albaniacraft.towns.listeners

import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import site.albaniacraft.towns.TownManager

class ChatListener(private val townManager: TownManager) : Listener {
    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val playerName = player.name
        // Get the player's town name, or use "Explorer" if they have no town
        val townName = townManager.getPlayerTown(playerName) ?: "Explorer"

        // Format the chat message with [townname] playerName: message
        event.format = ChatColor.DARK_GRAY.toString() + "[" + ChatColor.DARK_BLUE + townName + ChatColor.DARK_GRAY + "] " +
                ChatColor.WHITE + playerName + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + event.message
    }
}

