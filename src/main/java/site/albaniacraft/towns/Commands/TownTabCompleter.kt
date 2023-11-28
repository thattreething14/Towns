package site.albaniacraft.towns.Commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import site.albaniacraft.towns.TownManager
import java.util.ArrayList

class TownTabCompleter(private val townManager: TownManager) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        val completions: MutableList<String> = ArrayList()

        if (cmd.name.equals("town", ignoreCase = true)) {
            if (args.size == 1) {
                val subCommands = listOf("new", "delete", "invite", "deny", "accept", "unclaim", "claim", "info", "leave", "list")
                for (subCommand in subCommands) {
                    if (subCommand.startsWith(args[0].toLowerCase())) {
                        completions.add(subCommand)
                    }
                }
            } else if (args.size == 2 && args[0].equals("new", ignoreCase = true)) {
                // Add your custom town name suggestions here if needed
            }
        }

        return completions
    }
}