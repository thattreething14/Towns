package site.albaniacraft.towns.Commands.OpOnly

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class TownCoinsGiveCommand(private val plugin: JavaPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Only players can use this command.")
            return true
        }
        if (!sender.isOp) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}Usage: /town coinsgive <number_of_coins>")
            return true
        }

        val numberOfCoins = args[0].toIntOrNull()

        if (numberOfCoins == null || numberOfCoins <= 0) {
            sender.sendMessage("${ChatColor.RED}Please specify a valid positive number of coins.")
            return true
        }
        val coins = ItemStack(Material.GOLD_INGOT)
        val meta = coins.itemMeta

        // Set the display name of the item
        meta?.setDisplayName("${ChatColor.GOLD}Coins")
        coins.itemMeta = meta

        // Set the amount of coins in the ItemStack
        coins.amount = numberOfCoins

        // Give the coins to the player
        sender.inventory.addItem(coins)

        sender.sendMessage("${ChatColor.GREEN}You've received $numberOfCoins Coins!")

        return true
    }
}
