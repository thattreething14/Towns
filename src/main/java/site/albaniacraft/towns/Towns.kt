package site.albaniacraft.towns

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configurator

import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

import site.albaniacraft.towns.Commands.TownCommandHandler
import site.albaniacraft.towns.Commands.TownTabCompleter
import site.albaniacraft.towns.db.DatabaseInitializer
import site.albaniacraft.towns.db.DatabaseManager
import site.albaniacraft.towns.listeners.ChatListener
import site.albaniacraft.towns.listeners.ChunkClickListener
import site.albaniacraft.towns.listeners.DynmapIntegration
import site.albaniacraft.towns.model.TownEventListener
import site.albaniacraft.towns.model.TownProtectionListener
import site.albaniacraft.towns.npcs.NPCBanker

class Towns : JavaPlugin() {
    private lateinit var townManager: TownManager
    private lateinit var databaseManager: DatabaseManager
    private lateinit var databaseInitializer: DatabaseInitializer
    private lateinit var npcBanker: NPCBanker
    private lateinit var dynmapIntegration: DynmapIntegration
    override fun onEnable() {
        Configurator.setLevel("org.eclipse.jetty", Level.OFF)
        Configurator.setLevel("com.zaxxer.hikari", Level.OFF)
        val context = LogManager.getContext(false) as LoggerContext
        context.updateLoggers()
        databaseManager = DatabaseManager(this)
        townManager = TownManager(databaseManager)
        npcBanker = NPCBanker(this, databaseManager, townManager)
        server.pluginManager.registerEvents(npcBanker, this)
        // Initialize databaseManager first
        databaseInitializer = DatabaseInitializer(databaseManager, this)
        server.pluginManager.registerEvents(TownEventListener(townManager), this)
        // Initialize the database tables if they don't exist
        databaseInitializer.initializeDatabase()
        logger.info("Town Config has been registered uwu.")
        dynmapIntegration = DynmapIntegration(this, townManager, npcBanker)
        object : BukkitRunnable() {
            override fun run() {
                dynmapIntegration.updateClaimedChunksOnMap()
            }
        }.runTaskTimer(this, 0L, 1200L)

        GoofyAHHHasciisign()

        getCommand("town")?.tabCompleter = TownTabCompleter(townManager)
        val townCommandHandler = TownCommandHandler(this, townManager, databaseInitializer, databaseManager, npcBanker)
        getCommand("town")?.setExecutor(townCommandHandler)
        server.pluginManager.registerEvents(ChatListener(townManager), this)
        val townProtectionListener = TownProtectionListener(townManager)
        server.pluginManager.registerEvents(townProtectionListener, this)
        val chunkClickListener = ChunkClickListener(dynmapIntegration)
        server.pluginManager.registerEvents(chunkClickListener, this)
        logger.info("knocking off towny since 2023")
    }
    fun GoofyAHHHasciisign() {
        val art = """
        ████████╗░█████╗░░██╗░░░░░░░██╗███╗░░██╗░██████╗
        ╚══██╔══╝██╔══██╗░██║░░██╗░░██║████╗░██║██╔════╝
        ░░░██║░░░██║░░██║░╚██╗████╗██╔╝██╔██╗██║╚█████╗░
        ░░░██║░░░██║░░██║░░████╔═████║░██║╚████║░╚═══██╗
        ░░░██║░░░╚█████╔╝░░╚██╔╝░╚██╔╝░██║░╚███║██████╔╝
        ░░░╚═╝░░░░╚════╝░░░░╚═╝░░░╚═╝░░╚═╝░░╚══╝╚═════╝░
            ${ChatColor.DARK_PURPLE}By Thattreething14 
        """.trimIndent()

        server.consoleSender.sendMessage("\n$art")
    }
    override fun onDisable() {
        databaseManager.close()
    }
}