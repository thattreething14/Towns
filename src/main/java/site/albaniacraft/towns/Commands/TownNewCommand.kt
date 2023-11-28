package site.albaniacraft.towns.Commands

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.db.DatabaseManager
import site.albaniacraft.towns.TownManager
import site.albaniacraft.towns.model.Town
import site.albaniacraft.towns.npcs.NPCBanker

class TownNewCommand(
    private val plugin: JavaPlugin,
    private val databaseManager: DatabaseManager,
    private val townManager: TownManager,
    private val npcBanker: NPCBanker
) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(ChatColor.RED.toString() + "Only players can use this command.")
            return true
        }

        val playerName = sender.name

        // Check if the player already belongs to a town
        if (townManager.playerBelongsToTown(playerName)) {
            sender.sendMessage(ChatColor.RED.toString() + "You already belong to a town. You can't create a new one.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(ChatColor.RED.toString() + "Usage: /town new <town_name>")
            return true
        }

        val townName = args[1] // Use the first argument as the town name

        // Check if the player has 100 "Coins" in their inventory
        if (!hasCoins(sender)) {
            sender.sendMessage(ChatColor.RED.toString() + "You need at least 50 Coins to create a new town.")
            return true
        }

        // Create a new town and store it in the database
        val town = Town(townName, playerName)
        // Check if the town name already exists in the database
        if (townManager.townNameExists(townName)) {
            sender.sendMessage(ChatColor.RED.toString() + "A town with the name '$townName' already exists.")
            return true
        }

        // Deduct 100 Coins from the player's inventory
        deductCoins(sender)

        if (townManager.storeTownInDatabase(town)) {
            town.addMember(playerName) // Add the founder as a member
            sender.sendMessage(ChatColor.GREEN.toString() + "You have created a new town named '$townName'.")
            // Claim the chunk the player is in for the new town
            claimChunkForTown(sender, townName)
            val bankerLocation = sender.location
            npcBanker.spawnBanker(town, bankerLocation)
        } else {
            sender.sendMessage(ChatColor.RED.toString() + "Failed to create the town.")
        }

        return true
    }
    private fun hasCoins(player: Player): Boolean {
        val inventory = player.inventory
        val requiredCoins = 50

        var coinCount = 0

        // Count the number of "Coins" in the player's inventory
        for (itemStack in inventory.contents) {
            if (itemStack != null && itemStack.type == Material.GOLD_INGOT && itemStack.hasItemMeta() &&
                itemStack.itemMeta!!.displayName == ChatColor.GOLD.toString() + "Coins"
            ) {
                coinCount += itemStack.amount
            }
        }

        return coinCount >= requiredCoins
    }

    private fun deductCoins(player: Player) {
        val inventory = player.inventory
        val requiredCoins = 50

        var coinsRemaining = requiredCoins

        // Iterate through the player's inventory to deduct the required number of coins
        for (index in 0 until inventory.size) {
            val itemStack = inventory.getItem(index)

            if (itemStack != null && itemStack.type == Material.GOLD_INGOT && itemStack.hasItemMeta() &&
                itemStack.itemMeta!!.displayName == ChatColor.GOLD.toString() + "Coins"
            ) {
                val amount = itemStack.amount

                if (amount <= coinsRemaining) {
                    // Remove the entire ItemStack
                    inventory.setItem(index, null)
                    coinsRemaining -= amount
                } else {
                    // Deduct the required amount from the stack
                    itemStack.amount -= coinsRemaining
                    inventory.setItem(index, itemStack)
                    break
                }
            }
        }

        // Update the player's inventory
        player.updateInventory()
    }

    private fun claimChunkForTown(player: Player, town: String) {
        val chunk = player.location.chunk
        val chunkX = chunk.x
        val chunkZ = chunk.z

        if (!townManager.isChunkClaimedByTown(town, chunkX, chunkZ)) {
            val claimed = townManager.claimChunkForTown(town, chunkX, chunkZ)
            if (claimed) {
                println("Chunk claimed for town '$town': [$chunkX, $chunkZ]")
            }
        }
    }
}

