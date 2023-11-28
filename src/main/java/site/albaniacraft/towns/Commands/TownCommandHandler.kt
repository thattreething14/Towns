package site.albaniacraft.towns.Commands

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.Commands.OpOnly.TownCoinsGiveCommand
import site.albaniacraft.towns.TownManager
import site.albaniacraft.towns.db.DatabaseInitializer
import site.albaniacraft.towns.db.DatabaseManager
import site.albaniacraft.towns.npcs.NPCBanker

class TownCommandHandler(
    private val plugin: JavaPlugin,
    private val townManager: TownManager,
    private val databaseInitializer: DatabaseInitializer,
    private val databaseManager: DatabaseManager,
    private val npcBanker: NPCBanker
) : CommandExecutor {
    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.BLUE}====== Town Commands ======")
            sender.sendMessage("${ChatColor.BLUE}/town new - Create a new town")
            sender.sendMessage("${ChatColor.BLUE}/town delete - Delete your town")
            sender.sendMessage("${ChatColor.BLUE}/town invite - Invite a player to your town")
            sender.sendMessage("${ChatColor.BLUE}/town accept - Accept a town invitation")
            sender.sendMessage("${ChatColor.BLUE}/town deny - Deny a town invitation")
            sender.sendMessage("${ChatColor.BLUE}/town info - Get information about a town")
            sender.sendMessage("${ChatColor.BLUE}/town claim - Claim the chunk you are in for your town")
            sender.sendMessage("${ChatColor.BLUE}/town unclaim - Unclaim the chunk you are in for your town")
            return true
        }
        when (args[0].toLowerCase()) {
            "new" -> {
                if (sender.hasPermission("Towns.new")) {
                    val townNewCommand = TownNewCommand(plugin, DatabaseManager(plugin), townManager, npcBanker)
                    return townNewCommand.onCommand(sender, cmd, label, args)
                } else {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                }
            }
            "delete" -> {
                if (sender.hasPermission("Towns.delete")) {
                    val townDeleteCommand = TownDeleteCommand(plugin, townManager, databaseInitializer, npcBanker)
                    return townDeleteCommand.onCommand(sender, cmd, label, args)
                } else {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                }
            }
            "invite" -> {
                if (sender.hasPermission("Towns.invite")) {
                    val townInviteCommand = TownInviteCommand(plugin, townManager, databaseManager)
                    return townInviteCommand.onCommand(sender, cmd, label, args)
                } else {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                }
            }
            "accept" -> {
                if (sender.hasPermission("Towns.accept")) {
                    val townAcceptCommand = TownAcceptDenyCommand(plugin, townManager, databaseManager)
                    return townAcceptCommand.onCommand(sender, cmd, label, args)
                } else {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                }
            }
            "deny" -> {
                if (sender.hasPermission("Towns.deny")) {
                    val townDenyCommand = TownAcceptDenyCommand(plugin, townManager, databaseManager)
                    return townDenyCommand.onCommand(sender, cmd, label, args)
                } else {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                }
            }
            "info" -> {
                if (sender.hasPermission("Towns.info")) {
                    val townInfoCommand = TownInfoCommand(plugin, townManager, databaseInitializer, npcBanker)
                    return townInfoCommand.onCommand(sender, cmd, label, args)
                } else {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                }
            }
            "claim" -> {
                if (sender.hasPermission("Towns.claim")) {
                    val townClaimCommand = TownClaimCommand(townManager, npcBanker)
                    return townClaimCommand.onCommand(sender, cmd, label, args)
                } else {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                }
            }
            "unclaim" -> {
                if (sender.hasPermission("Towns.unclaim")) {
                    val townUnclaimCommand = TownUnclaimCommand(townManager)
                    return townUnclaimCommand.onCommand(sender, cmd, label, args)
                } else {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                }
            }
            "leave" -> {
                if (sender.hasPermission("Towns.leave")) {
                    val townLeaveCommand = TownLeaveCommand(townManager)
                    return townLeaveCommand.onCommand(sender, cmd, label, args)
                } else {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                }
            }
            "coinsgive" -> {
                // Check if the sender is an operator (op)
                if (sender.isOp) {
                    val townCoinsGiveCommand = TownCoinsGiveCommand(plugin)
                    return townCoinsGiveCommand.onCommand(sender, cmd, label, args.drop(1).toTypedArray())
                } else {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                }
            }
            "list" -> {
                if (sender.hasPermission("Towns.list")) {
                    val townListCommand = TownListCommand(plugin, townManager)
                    return townListCommand.onCommand(sender, cmd, label, args)
                } else {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
                }
            }
            else -> {
                sender.sendMessage("Unknown subcommand: ${args[0]}")
            }
        }

        return true
    }
}
