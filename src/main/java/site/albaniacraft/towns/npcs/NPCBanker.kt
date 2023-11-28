package site.albaniacraft.towns.npcs

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

import site.albaniacraft.towns.TownManager
import site.albaniacraft.towns.db.DatabaseManager
import site.albaniacraft.towns.model.Town

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
// Todo: fix all 49 warning lmao
class NPCBanker(private val plugin: JavaPlugin, private val databaseManager: DatabaseManager, private val townManager: TownManager) : Listener {
    private val pendingDeposits = mutableMapOf<Player, Int>()
    private val pendingWithdrawals = mutableMapOf<Player, Int>()
    private fun scheduleOrphanedBankerCheck() {
        val intervalTicks = 20 * 60
        Bukkit.getScheduler().runTaskTimer(
            plugin,
            Runnable { checkAndRemoveOrphanedBankers() },
            0L,
            intervalTicks.toLong()
        )
    }

    init {
        scheduleOrphanedBankerCheck()
    }
    fun spawnBanker(town: Town, location: Location) {
        val banker = location.world.spawnEntity(location, EntityType.VILLAGER) as Villager
        banker.customName = "${ChatColor.GOLD}Banker"
        banker.isCustomNameVisible = true
        banker.setAI(false)
        storeBankerLocation(town.name, location)
        createTownBalanceIfNotExists(town.name)
    }
    private fun createTownBalanceIfNotExists(townName: String) {
        val connection = databaseManager.getConnection()

        try {
            val insertSql = "INSERT OR IGNORE INTO town_balances (town_name, balance) VALUES (?, 0.0)"
            val insertStatement: PreparedStatement = connection.prepareStatement(insertSql)
            insertStatement.setString(1, townName)
            insertStatement.executeUpdate()

            val updateSql = "UPDATE town_balances SET town_name = town_name WHERE town_name = ?"
            val updateStatement: PreparedStatement = connection.prepareStatement(updateSql)
            updateStatement.setString(1, townName)
            updateStatement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }
    }


    private fun storeBankerLocation(townName: String, location: Location) {
        val connection = databaseManager.getConnection()

        try {
            val sql = "INSERT INTO banker_locations (town_name, world, x, y, z) VALUES (?, ?, ?, ?, ?)"
            val statement: PreparedStatement = connection.prepareStatement(sql)
            statement.setString(1, townName)
            statement.setString(2, location.world.name)
            statement.setDouble(3, location.x)
            statement.setDouble(4, location.y)
            statement.setDouble(5, location.z)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }
    }

    private fun purgeBankerLocation(townName: String) {
        val connection = databaseManager.getConnection()

        try {
            val sql = "DELETE FROM banker_locations WHERE town_name = ?"
            val statement: PreparedStatement = connection.prepareStatement(sql)
            statement.setString(1, townName)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }
    }
    private fun checkAndRemoveOrphanedBankers() { //fix goofy ass function name :skull:
        val allTowns = townManager.getAllTowns()
        val existingBankers = mutableSetOf<UUID>()

        for (townName in allTowns) {
            val location = getBankerLocation(townName)
            if (location != null) {
                val chunk = location.chunk
                val entities = chunk.entities
                for (entity in entities) {
                    if (entity.type == EntityType.VILLAGER && entity.customName == "${ChatColor.GOLD}Banker") {
                        existingBankers.add(entity.uniqueId)
                    }
                }
            }
        }

        for (world in Bukkit.getWorlds()) {
            val loadedChunks = world.loadedChunks
            for (chunk in loadedChunks) {
                val entities = chunk.entities
                for (entity in entities) {
                    if (entity.type == EntityType.VILLAGER && entity.customName == "${ChatColor.GOLD}Banker") {
                        if (entity.uniqueId !in existingBankers) {
                            entity.remove()
                        }
                    }
                }
            }
        }
    }
    // Use this method to despawn a banker entity
    fun despawnBanker(townName: String) {
        val location = getBankerLocation(townName)

        if (location != null) {
            val world = Bukkit.getWorld(location.world.name)
            val x = location.x
            val y = location.y
            val z = location.z

            // Find and remove the banker entity at the stored location
            val entities = world?.getNearbyEntities(Location(world, x, y, z), 1.0, 1.0, 1.0)

            if (entities != null) {
                for (entity in entities) {
                    if (entity is Villager && entity.customName == "${ChatColor.GOLD}Banker") {
                        entity.remove()
                        break
                    }
                }
            }
            purgeBankerLocation(townName)
        }
    }

    private fun getBankerLocation(townName: String): Location? {
        val connection = databaseManager.getConnection()
        var location: Location? = null

        try {
            val sql = "SELECT world, x, y, z FROM banker_locations WHERE town_name = ?"
            val statement: PreparedStatement = connection.prepareStatement(sql)
            statement.setString(1, townName)
            val resultSet: ResultSet = statement.executeQuery()

            if (resultSet.next()) {
                val worldName = resultSet.getString("world")
                val x = resultSet.getDouble("x")
                val y = resultSet.getDouble("y")
                val z = resultSet.getDouble("z")
                location = Location(Bukkit.getWorld(worldName), x, y, z)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return location
    }
    private fun handleDeposit(player: Player, amount: Int) {
        // Check if the player has the specified amount of gold ingots (coins) in their inventory
        val coinsAmount = getPlayerCoins(player)

        if (coinsAmount >= amount) {
            // Remove the specified amount of coins from the player's inventory
            removePlayerCoins(player, amount)

            // Add the deposited coins to the town balance database
            val townName = townManager.getPlayerTown(player.name)
            if (townName != null) {
                // Update the town's balance in the database
                updateTownBalance(townName, amount.toDouble())

                // Inform the player about the successful deposit
                player.sendMessage("${ChatColor.GREEN}Deposited $amount coins into your town's balance.")
            } else {
                // Handle the case when the player is not in a town
                player.sendMessage("${ChatColor.RED}You are not a member of any town.")
            }
        } else {
            // Handle the case when the player does not have enough coins to deposit
            player.sendMessage("${ChatColor.RED}You don't have enough coins to deposit $amount.")
        }
    }


    private fun getPlayerCoins(player: Player): Int {
        var coinsAmount = 0
        for (item in player.inventory.contents) {
            if (item != null && item.type == Material.GOLD_INGOT) {
                val itemMeta = item.itemMeta
                if (itemMeta != null && itemMeta.displayName == "${ChatColor.GOLD}Coins") {
                    coinsAmount += item.amount
                }
            }
        }
        return coinsAmount
    }

    private fun removePlayerCoins(player: Player, amount: Int) {
        var amountToRemove = amount
        for (item in player.inventory.contents) {
            if (item != null && item.type == Material.GOLD_INGOT) {
                val itemMeta = item.itemMeta
                if (itemMeta != null && itemMeta.displayName == "${ChatColor.GOLD}Coins") {
                    if (item.amount <= amountToRemove) {
                        player.inventory.removeItem(item)
                        amountToRemove -= item.amount
                    } else {
                        item.amount -= amountToRemove
                        break
                    }
                }
            }
        }
    }

    fun updateTownBalance(townName: String, amount: Double): Boolean {
        val connection = databaseManager.getConnection()

        try {
            val currentBalance = getTownBalance(townName)

            if (currentBalance + amount >= 0.0) {
                val updateStatement = connection.prepareStatement("UPDATE town_balances SET balance = ? WHERE town_name = ?")
                updateStatement.setDouble(1, currentBalance + amount)
                updateStatement.setString(2, townName)
                updateStatement.executeUpdate()
                updateStatement.close()
                return true
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return false
    }
    private fun openBankerGUI(player: Player) {
        // Create an inventory for the Banker GUI
        val inventory = Bukkit.createInventory(null, 9, "${ChatColor.GOLD}Banker")

        // Create the "Deposit" option
        val depositItem = ItemStack(Material.GOLD_INGOT)
        val depositMeta = depositItem.itemMeta
        depositMeta.setDisplayName("${ChatColor.GOLD}Deposit")
        depositItem.itemMeta = depositMeta

        // Create the "Withdraw" option
        val withdrawItem = ItemStack(Material.GOLD_BLOCK) // Change the item type as needed
        val withdrawMeta = withdrawItem.itemMeta
        withdrawMeta.setDisplayName("${ChatColor.GOLD}Withdraw")
        withdrawItem.itemMeta = withdrawMeta

        // Add the items to the GUI inventory
        inventory.setItem(3, depositItem) // Place "Deposit" option in slot 3
        inventory.setItem(5, withdrawItem) // Place "Withdraw" option in slot 5

        // Fetch and display the town's balance
        val townName = townManager.getPlayerTown(player.name)
        if (townName != null) {
            val balance = getTownBalance(townName)
            val balanceItem = ItemStack(Material.GOLD_NUGGET)
            val balanceMeta = balanceItem.itemMeta
            balanceMeta.setDisplayName("${ChatColor.GOLD}Balance: ${ChatColor.GREEN}$balance")
            balanceItem.itemMeta = balanceMeta
            inventory.setItem(4, balanceItem) // Place "Balance" item in slot 4
        }

        // Open the GUI for the player
        player.openInventory(inventory)
    }

    fun getTownBalance(townName: String): Double {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT balance FROM town_balances WHERE town_name = ?")
            statement.setString(1, townName)
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                return resultSet.getDouble("balance")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return 0.0 // Default balance if not found
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked

        if (entity.type == EntityType.VILLAGER && entity.customName == "${ChatColor.GOLD}Banker") {
            // Retrieve the town name associated with this banker
            val playerName = player.name
            val townName = townManager.getPlayerTown(playerName)

            if (townName != null && townManager.isPlayerMemberOfTown(playerName, townName)) {
                player.sendMessage(ChatColor.GREEN.toString() + "Banker: Hi!")
                openBankerGUI(player)
            } else {
                player.sendMessage(ChatColor.RED.toString() + "You are not a member of this town.")
            }
        }
    }
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val slot = event.slot

        // Check if the slot is the one you want to protect (e.g., slot 4)
        val item = event.currentItem
        val townName = townManager.getPlayerTown(player.name)
        val balance = townName?.let { getTownBalance(it) }
        if (item != null && item.hasItemMeta()) {
            val itemMeta = item.itemMeta

            if (itemMeta != null && itemMeta.hasDisplayName()) {
                val itemName = itemMeta.displayName

                when (itemName) {
                    "${ChatColor.GOLD}Deposit" -> {
                        // Handle the deposit button click
                        player.sendMessage("${ChatColor.YELLOW}Please specify the amount to deposit in chat. btw this plugin was coded by a crappy developer so if you accidently clicked this without any money on you just put 1")
                        pendingDeposits[player] = 0 // Initialize pending deposit to 0
                        event.isCancelled = true // Prevent taking the item out
                    }
                    "${ChatColor.GOLD}Withdraw" -> {
                        // Handle the withdraw button click
                        player.sendMessage("${ChatColor.YELLOW}Please specify the amount to withdraw in chat.btw this plugin was coded by a crappy developer so if you accidently clicked this without any money on you just put 1")
                        pendingWithdrawals[player] = 0 // Initialize pending withdrawal to 0
                        event.isCancelled = true // Prevent taking the item out
                    }

                    "${ChatColor.GOLD}Balance: ${ChatColor.GREEN}$balance" ->
                        // Handle the balance button click
                        event.isCancelled = true
                    else -> {
                        // Handle other items in the banker's inventory
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player

        // Check if the player has a pending deposit request
        if (pendingDeposits.containsKey(player)) {
            // Attempt to parse the chat message as an integer
            val amountStr = event.message
            val amount = amountStr.toIntOrNull()

            if (amount != null && amount > 0) {
                // Valid amount entered by the player
                val pendingAmount = pendingDeposits[player]!!
                // Update the pending deposit amount
                pendingDeposits[player] = pendingAmount + amount

                // Handle the deposit with the updated amount
                handleDeposit(player, pendingDeposits[player]!!)
                // Remove the pending deposit request
                pendingDeposits.remove(player)
            } else {
                // Invalid amount entered by the player, notify them
                player.sendMessage("${ChatColor.RED}Invalid amount. Please enter a positive integer amount.")
            }

            // Cancel the chat event to prevent the message from being sent
            event.isCancelled = true
        } else if (pendingWithdrawals.containsKey(player)) {
        // Attempt to parse the chat message as an integer
        val amountStr = event.message
        val amount = amountStr.toIntOrNull()

        if (amount != null && amount > 0) {
            // Valid amount entered by the player
            val pendingAmount = pendingWithdrawals[player]!!

            // Check if the town has enough balance for withdrawal
            val townName = townManager.getPlayerTown(player.name)
            if (townName != null) {
                val townBalance = getTownBalance(townName)
                if (townBalance >= amount) {
                    // Remove the withdrawn coins from the town's balance
                    updateTownBalance(townName, -amount.toDouble())

                    // Add the withdrawn coins to the player's inventory
                    givePlayerCoins(player, amount)

                    // Inform the player about the successful withdrawal
                    player.sendMessage("${ChatColor.GREEN}Withdrawn $amount coins from your town's balance.")
                } else {
                    // Insufficient balance in the town
                    player.sendMessage("${ChatColor.RED}The town does not have enough coins for withdrawal.")
                }
            } else {
                // Handle the case when the player is not in a town
                player.sendMessage("${ChatColor.RED}You are not a member of any town.")
            }
            // Remove the pending withdrawal request
            pendingWithdrawals.remove(player)
        } else {
            // Invalid amount entered by the player, notify them
            player.sendMessage("${ChatColor.RED}Invalid amount. Please enter a positive integer amount.")
        }

        event.isCancelled = true
    }
}
    // describes coin
    private fun givePlayerCoins(player: Player, amount: Int) {
        val coinsItem = ItemStack(Material.GOLD_INGOT, amount)
        val coinsMeta = coinsItem.itemMeta

        coinsMeta.setDisplayName("${ChatColor.GOLD}Coins")

        coinsItem.itemMeta = coinsMeta

        player.inventory.addItem(coinsItem)
    }


    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity

        if (entity.type == EntityType.VILLAGER && entity.customName == "${ChatColor.GOLD}Banker") {
            event.isCancelled = true
        }
    }
}
