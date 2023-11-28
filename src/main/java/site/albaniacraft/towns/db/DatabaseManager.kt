package site.albaniacraft.towns.db


import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.sql.Connection
import java.sql.SQLException
class DatabaseManager(private val plugin: JavaPlugin) {
    private val config: HikariConfig = HikariConfig()
    val dataSource: HikariDataSource
    init {
        val databaseFolder = File(plugin.dataFolder, "database") // Create a 'database' folder inside your plugin's data folder
        if (!databaseFolder.exists()) {
            databaseFolder.mkdirs()
        }

        val databaseFile = File(databaseFolder, "database.db") // Define the path to the SQLite database file

        config.jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}" // Use the absolute path to the database file
        config.driverClassName = "org.sqlite.JDBC"
        dataSource = HikariDataSource(config)
    }
    fun getConnection(): Connection {
        try {
            return dataSource.connection
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to get database connection: ${e.message}")
            throw e
        }
    }
    fun close() {
        dataSource.close()
    }
    fun createTables(tableName: String, columns: List<String>) {
        val connection = dataSource.connection
        val statement = connection.createStatement()

        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS $tableName (
                ${columns.joinToString(",\n")}
            )
        """

        statement.executeUpdate(createTableSQL)

        statement.close()
        connection.close()
    }
    fun storePlayerTownAssociation(playerName: String, townName: String) {
        val connection = dataSource.connection

        try {
            val insertStatement = connection.prepareStatement("INSERT INTO player_towns (player_name, town_name) VALUES (?, ?)")
            insertStatement.setString(1, playerName)
            insertStatement.setString(2, townName)

            insertStatement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }
    }
}
