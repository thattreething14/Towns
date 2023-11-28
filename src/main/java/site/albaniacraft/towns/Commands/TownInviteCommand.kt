package site.albaniacraft.towns.Commands

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.TownManager
import site.albaniacraft.towns.db.DatabaseManager
import java.sql.SQLException

class TownInviteCommand(
    private val plugin: JavaPlugin,
    private val townManager: TownManager,
    private val databaseManager: DatabaseManager
) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(ChatColor.RED.toString() + "Only players can use this command.")
            return true
        }

        val playerName = sender.name

        if (args.size < 2) {
            sender.sendMessage(ChatColor.RED.toString() + "Usage: /town invite <player>")
            return true
        }

        val invitedPlayerName = args[1]

        if (!townManager.playerBelongsToTown(playerName)) {
            sender.sendMessage(ChatColor.RED.toString() + "You are not a member of any town.")
            return true
        }

        val townName = townManager.getPlayerTown(playerName)

        if (townName != null) {
            if (townManager.isTownFounder(townName, playerName)) {
                val invitedPlayer = Bukkit.getPlayerExact(invitedPlayerName)

                if (invitedPlayer != null) {
                    if (townManager.playerBelongsToTown(invitedPlayer.name)) {
                        sender.sendMessage(ChatColor.RED.toString() + "${invitedPlayer.name} is already a member of a town.")
                    } else {
                        // Store the invitation in the database
                        if (storeInvitation(playerName, invitedPlayer.name, townName)) {
                            sender.sendMessage(ChatColor.GREEN.toString() + "Invitation sent to ${invitedPlayer.name} to join $townName town.")
                            invitedPlayer.sendMessage(ChatColor.GREEN.toString() + "You have received an invitation to join the town '$townName'. Use '/town accept' or '/town deny' to respond.")
                        } else {
                            sender.sendMessage(ChatColor.RED.toString() + "Failed to send an invitation to ${invitedPlayer.name}.")
                        }
                    }
                } else {
                    sender.sendMessage(ChatColor.RED.toString() + "Player '$invitedPlayerName' not found or not online.")
                }
            }
        }
        return false
    }

    private fun storeInvitation(senderName: String, invitedPlayerName: String, townName: String): Boolean {
        val connection = databaseManager.getConnection()

        try {
            val currentTimestamp = System.currentTimeMillis()

            // Create and execute the SQL INSERT statement
            val statement = connection.prepareStatement("INSERT INTO pending_invitations (invited_player_name, sender_name, town_name, timestamp) VALUES (?, ?, ?, ?)")
            statement.setString(1, invitedPlayerName)
            statement.setString(2, senderName)
            statement.setString(3, townName)
            statement.setLong(4, currentTimestamp)
            statement.executeUpdate()
            statement.close()

            return true
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        } finally {
            connection.close()
        }
    }
}