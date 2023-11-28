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

class TownAcceptDenyCommand(
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

        if (args.isEmpty()) {
            sender.sendMessage(ChatColor.RED.toString() + "Usage: /town accept <townName> OR /town deny <townName>")
            return true
        }

        val subCommand = args[0].toLowerCase() // Convert to lowercase for case-insensitive comparison
        if (subCommand.equals("accept", ignoreCase = true) || subCommand.equals("deny", ignoreCase = true)) {
            if (args.size != 2) {
                sender.sendMessage(ChatColor.RED.toString() + "Usage: /town $subCommand <townName>")
                return true
            }

            val townName = args[1]
            val invitation = getPendingInvitation(playerName)

            if (invitation == null) {
                sender.sendMessage(ChatColor.RED.toString() + "No pending invitation to $subCommand.")
                return true
            }
            if (System.currentTimeMillis() - invitation.timestamp > 60000) {
                // Invitation has expired (1 minute = 60000 milliseconds)
                sender.sendMessage(ChatColor.RED.toString() + "The invitation has expired.")
                removePendingInvitation(playerName)
                return true
            }

            if (subCommand.equals("accept", ignoreCase = true)) {
                acceptInvitation(playerName, townName)
            } else {
                denyInvitation(playerName, townName)
            }
        } else {
            sender.sendMessage(ChatColor.RED.toString() + "Invalid sub-command. Use '/town accept <townName>' or '/town deny <townName>'.")
        }

        return true
    }

    private fun getPendingInvitation(playerName: String): PendingInvitation? {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT * FROM pending_invitations WHERE invited_player_name = ?")
            statement.setString(1, playerName)
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val senderName = resultSet.getString("sender_name")
                val townName = resultSet.getString("town_name")
                val timestamp = resultSet.getLong("timestamp")
                return PendingInvitation(playerName, townName, senderName, timestamp)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return null
    }

    private fun removePendingInvitation(playerName: String) {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("DELETE FROM pending_invitations WHERE invited_player_name = ?")
            statement.setString(1, playerName)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }
    }

    private fun acceptInvitation(playerName: String, townName: String) {
        val invitation = getPendingInvitation(playerName)

        if (invitation == null) {
            // No pending invitation to accept
            val sender = Bukkit.getPlayerExact(playerName)
            sender?.sendMessage(ChatColor.RED.toString() + "No pending invitation to accept.")
            return
        }

        if (invitation.townName != townName) {
            // Mismatched invitation
            val sender = Bukkit.getPlayerExact(invitation.senderName)
            sender?.sendMessage(ChatColor.RED.toString() + "Invalid or mismatched invitation to join the town.")
            return
        }

        // Add the invited player to the town in the database
        if (townManager.addPlayerToTown(playerName, townName)) {
            val sender = Bukkit.getPlayerExact(invitation.senderName)

            if (sender != null) {
                sender.sendMessage(ChatColor.GREEN.toString() + "$playerName has accepted the invitation to join the town $townName.")
            }

            // Notify all town members about the new player joining
            val town = townManager.getTown(townName)
            town.members.forEach { memberName ->
                val member = Bukkit.getPlayerExact(memberName)
                if (member != null) {
                    member.sendMessage(ChatColor.GREEN.toString() + "$playerName has joined the town.")
                }
            }
        }

        // Remove the pending invitation after it's accepted
        removePendingInvitation(playerName)
    }

    private fun denyInvitation(playerName: String, townName: String) {
        val invitation = getPendingInvitation(playerName)

        if (invitation == null) {
            // No pending invitation to deny
            val sender = Bukkit.getPlayerExact(playerName)
            sender?.sendMessage(ChatColor.RED.toString() + "No pending invitation to deny.")
            return
        }

        if (invitation.townName != townName) {
            // Mismatched invitation
            val sender = Bukkit.getPlayerExact(invitation.senderName)
            sender?.sendMessage(ChatColor.RED.toString() + "Invalid or mismatched invitation to join the town.")
            return
        }

        val sender = Bukkit.getPlayerExact(invitation.senderName)

        if (sender != null) {
            sender.sendMessage(ChatColor.RED.toString() + "$playerName has denied the invitation to join the town $townName.")
        }

        // Remove the pending invitation after it's denied
        removePendingInvitation(playerName)
    }

    data class PendingInvitation(
        val invitedPlayerName: String,
        val townName: String,
        val senderName: String,
        val timestamp: Long
    )

    // You may need to include your databaseManager here
    // private val databaseManager: DatabaseManager
}
