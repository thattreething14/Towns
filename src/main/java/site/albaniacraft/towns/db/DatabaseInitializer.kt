package site.albaniacraft.towns.db

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin

import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder

import site.albaniacraft.towns.TownManager
import site.albaniacraft.towns.web.ServerThread
import site.albaniacraft.towns.web.StatisticsServlet

import java.io.File
import java.io.IOException
import java.util.logging.Logger


class DatabaseInitializer(private val databaseManager: DatabaseManager, private val plugin: JavaPlugin) {
    private val logger: Logger = Logger.getLogger(DatabaseInitializer::class.java.name)
    fun initializeDatabase() {
        // Store the name of the town and the founder/mayor
        val townTableColumns = listOf(
            "name TEXT PRIMARY KEY NOT NULL",
            "founder TEXT NOT NULL"
        )

        // Store members and town names
        val playerTownsTableColumns = listOf(
            "player_name TEXT NOT NULL",
            "town_name TEXT NOT NULL"
        )

        // Store claimed chunks
        val chunkClaimsTableColumns = listOf(
            "town_name TEXT NOT NULL",
            "chunk_x INTEGER NOT NULL",
            "chunk_z INTEGER NOT NULL"
        )

        // Store invitations for players
        val pendingInvitationsTableColumns = listOf(
            "id INTEGER PRIMARY KEY AUTOINCREMENT",
            "invited_player_name TEXT NOT NULL",
            "sender_name TEXT NOT NULL",
            "town_name TEXT NOT NULL",
            "timestamp INTEGER NOT NULL DEFAULT 0"
        )

        // Store banker locations
        val bankerLocationsTableColumns = listOf(
            "id INTEGER PRIMARY KEY AUTOINCREMENT",
            "town_name TEXT NOT NULL",
            "world TEXT NOT NULL",
            "x REAL NOT NULL",
            "y REAL NOT NULL",
            "z REAL NOT NULL"
        )

        // Store town balances
        val townBalanceTableColumns = listOf(
            "town_name TEXT PRIMARY KEY NOT NULL",
            "balance REAL NOT NULL"
        )

        databaseManager.createTables("town_balances", townBalanceTableColumns)
        databaseManager.createTables("chunks_claimed", chunkClaimsTableColumns)
        databaseManager.createTables("towns", townTableColumns)
        databaseManager.createTables("player_towns", playerTownsTableColumns)
        databaseManager.createTables("pending_invitations", pendingInvitationsTableColumns)
        databaseManager.createTables("banker_locations", bankerLocationsTableColumns)
        val configFile = File(plugin.dataFolder, "web/config.yml")
        if (!configFile.exists()) {
            createDefaultConfig(configFile)
        }

        val config = YamlConfiguration.loadConfiguration(configFile)

        // Get port and path from the config
        val port = config.getInt("web.port", 8228)
        val path = config.getString("web.path", "/townStatistics")
        path?.let { startJettyServer(port, it) }
        logger.info("0.0.0.0:$port$path has been linked.")
    }
    private fun createDefaultConfig(configFile: File) {
        try {
            // sketchy way to copy shit
            val webFolder = File(plugin.dataFolder, "web")
            if (!webFolder.exists()) {
                webFolder.mkdirs()
            }

            if (!configFile.exists()) {
                configFile.createNewFile()

                // copies the default configuration file
                val defaultConfigStream = javaClass.getResourceAsStream("/web/config.yml")
                defaultConfigStream?.use { input ->
                    configFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }
    private fun startJettyServer(port: Int, path: String) {
        val townManager = TownManager(databaseManager)

        val context = ServletContextHandler(ServletContextHandler.SESSIONS)
        context.contextPath = "/"
        val statisticsServlet = StatisticsServlet(townManager)
        val statisticsServletHolder = ServletHolder(statisticsServlet)
        context.addServlet(statisticsServletHolder, path)

        // Creates serverthread and starts jetty server
        val serverThread = ServerThread(port, context)
        serverThread.start()
    }
}