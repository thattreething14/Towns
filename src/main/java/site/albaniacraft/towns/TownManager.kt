package site.albaniacraft.towns

import site.albaniacraft.towns.db.DatabaseManager
import site.albaniacraft.towns.model.Town
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class TownManager(private val databaseManager: DatabaseManager) {
    private val towns: MutableList<Town> = mutableListOf()
    fun deleteTownBalance(townName: String) {
        val connection = databaseManager.getConnection()

        try {
            val sql = "DELETE FROM town_balances WHERE town_name = ?"
            val statement: PreparedStatement = connection.prepareStatement(sql)
            statement.setString(1, townName)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }
    }
    fun getClaimedChunksForTown(townName: String): List<Pair<Int, Int>> {
        val connection = databaseManager.getConnection()
        val claimedChunks = mutableListOf<Pair<Int, Int>>()

        try {
            val statement = connection.prepareStatement("SELECT chunk_x, chunk_z FROM chunks_claimed WHERE town_name = ?")
            statement.setString(1, townName)
            val resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val chunkX = resultSet.getInt("chunk_x")
                val chunkZ = resultSet.getInt("chunk_z")
                claimedChunks.add(chunkX to chunkZ)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return claimedChunks
    }

    fun getTownAtChunk(chunkX: Int, chunkZ: Int): Town? {
        val connection = databaseManager.getConnection()

        try {
            val statement: PreparedStatement = connection.prepareStatement(
                "SELECT town_name FROM chunks_claimed WHERE chunk_x = ? AND chunk_z = ?"
            )
            statement.setInt(1, chunkX)
            statement.setInt(2, chunkZ)

            val resultSet: ResultSet = statement.executeQuery()

            if (resultSet.next()) {
                val townName: String = resultSet.getString("town_name")
                return getTownByName(townName)
            } else {
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return null
    }



    fun getTownByName(townName: String): Town? {
        val connection = databaseManager.getConnection()

        try {
            val statement: PreparedStatement = connection.prepareStatement(
                "SELECT name, founder FROM towns WHERE name = ?"
            )
            statement.setString(1, townName)

            val resultSet: ResultSet = statement.executeQuery()

            if (resultSet.next()) {
                val name: String = resultSet.getString("name")
                val founder: String = resultSet.getString("founder")
                return Town(name, founder)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return null
    }
    fun addPlayerToTown(playerName: String, townName: String): Boolean {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("INSERT INTO player_towns (player_name, town_name) VALUES (?, ?)")
            statement.setString(1, playerName)
            statement.setString(2, townName)
            statement.executeUpdate()
            statement.close()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            connection.close()
        }
    }
    fun getClaimedChunksCount(townName: String): Int {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT COUNT(*) AS chunk_count FROM chunks_claimed WHERE town_name = ?")
            statement.setString(1, townName)
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt("chunk_count")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return 0
    }
    fun getAdjacentClaimedChunks(townName: String, chunkX: Int, chunkZ: Int): List<Pair<Int, Int>> {
        val adjacentChunks = mutableListOf<Pair<Int, Int>>()

        // gets adjacent coords
        val neighbors = arrayOf(
            Pair(chunkX - 1, chunkZ),
            Pair(chunkX + 1, chunkZ),
            Pair(chunkX, chunkZ - 1),
            Pair(chunkX, chunkZ + 1)
        )

        for (neighbor in neighbors) {
            val neighborX = neighbor.first
            val neighborZ = neighbor.second

            if (isChunkClaimedByTown(townName, neighborX, neighborZ)) {
                adjacentChunks.add(neighborX to neighborZ)
            }
        }

        return adjacentChunks
    }
    fun townNameExists(townName: String): Boolean {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT name FROM towns WHERE name = ?")
            statement.setString(1, townName)
            val resultSet = statement.executeQuery()

            return resultSet.next()
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        } finally {
            connection.close()
        }
    }

    fun playerBelongsToTown(playerName: String): Boolean {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT COUNT(*) FROM player_towns WHERE player_name = ?")
            statement.setString(1, playerName)
            val resultSet = statement.executeQuery()

        
            if (resultSet.next()) {
                val count = resultSet.getInt(1)
                return count > 0
            }
            return false
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        } finally {
            connection.close()
        }
    }
    fun storeTownInDatabase(town: Town): Boolean {

        val connection = databaseManager.getConnection()

        try {

            val statement = connection.prepareStatement("INSERT INTO towns (name, founder) VALUES (?, ?)")
            statement.setString(1, town.name)
            statement.setString(2, town.founder)
            statement.executeUpdate()
            statement.close()


            databaseManager.storePlayerTownAssociation(town.founder, town.name)


            storeTownMembers(town)

            return true 
        } catch (e: Exception) {
            e.printStackTrace()
            return false 
        } finally {
            connection.close()
        }
    }

    private fun storeTownMembers(town: Town) {
        val connection = databaseManager.getConnection()

        try {
            val insertStatement = connection.prepareStatement("INSERT INTO town_members (town_name, member_name) VALUES (?, ?)")

            for (memberName in town.members) {
                insertStatement.setString(1, town.name)
                insertStatement.setString(2, memberName)
                insertStatement.addBatch()
            }

            insertStatement.executeBatch()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }
    }
    fun isChunkClaimedByTown(townName: String, chunkX: Int, chunkZ: Int): Boolean {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT COUNT(*) AS chunk_count FROM chunks_claimed WHERE town_name = ? AND chunk_x = ? AND chunk_z = ?")
            statement.setString(1, townName)
            statement.setInt(2, chunkX)
            statement.setInt(3, chunkZ)
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val chunkCount = resultSet.getInt("chunk_count")
                return chunkCount > 0
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return false
    }
    fun claimChunkForTown(townName: String, chunkX: Int, chunkZ: Int): Boolean {
        val connection = databaseManager.getConnection()

        try {

            if (!townNameExists(townName)) {

                return false
            }

    
            if (isChunkClaimedByTown(townName, chunkX, chunkZ)) {
     
                return false
            }

            val statement = connection.prepareStatement("INSERT INTO chunks_claimed (town_name, chunk_x, chunk_z) VALUES (?, ?, ?)")
            statement.setString(1, townName)
            statement.setInt(2, chunkX)
            statement.setInt(3, chunkZ)
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

    fun isPlayerMemberOfTown(playerName: String, townName: String): Boolean {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT COUNT(*) AS member_count FROM player_towns WHERE player_name = ? AND town_name = ?")
            statement.setString(1, playerName)
            statement.setString(2, townName)
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val memberCount = resultSet.getInt("member_count")
                return memberCount > 0
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return false
    }
    fun deleteTown(townName: String) {
        val connection = databaseManager.getConnection()

        try {

   
            val deleteChunksStatement = connection.prepareStatement("DELETE FROM chunks_claimed WHERE town_name = ?")
            deleteChunksStatement.setString(1, townName)
            deleteChunksStatement.executeUpdate()
            deleteChunksStatement.close()

            // Delete player-town 
            val deletePlayerTownStatement = connection.prepareStatement("DELETE FROM player_towns WHERE town_name = ?")
            deletePlayerTownStatement.setString(1, townName)
            deletePlayerTownStatement.executeUpdate()
            deletePlayerTownStatement.close()

            // Delete the town
            val deleteTownStatement = connection.prepareStatement("DELETE FROM towns WHERE name = ?")
            deleteTownStatement.setString(1, townName)
            deleteTownStatement.executeUpdate()
            deleteTownStatement.close()

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }
    }

    fun unclaimChunkForTown(townName: String, chunkX: Int, chunkZ: Int): Boolean {
        val connection = databaseManager.getConnection()

        try {

            val checkStatement = connection.prepareStatement("SELECT town_name FROM chunks_claimed WHERE chunk_x = ? AND chunk_z = ?")
            checkStatement.setInt(1, chunkX)
            checkStatement.setInt(2, chunkZ)
            val resultSet = checkStatement.executeQuery()

            if (resultSet.next()) {
                val claimingTown = resultSet.getString("town_name")

                if (claimingTown == townName) {

                    val unclaimStatement = connection.prepareStatement("DELETE FROM chunks_claimed WHERE chunk_x = ? AND chunk_z = ?")
                    unclaimStatement.setInt(1, chunkX)
                    unclaimStatement.setInt(2, chunkZ)
                    unclaimStatement.executeUpdate()
                    unclaimStatement.close()
                    return true
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return false
    }

    fun isChunkClaimedOtherThanTown(chunkX: Int, chunkZ: Int, townName: String): Boolean {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT town_name FROM chunks_claimed WHERE chunk_x = ? AND chunk_z = ?")
            statement.setInt(1, chunkX)
            statement.setInt(2, chunkZ)
            val resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val claimingTown = resultSet.getString("town_name")
                if (claimingTown != townName) {
                    return true
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return false
    }

    fun isTownFounder(townName: String, founderName: String): Boolean {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT founder FROM towns WHERE name = ?")
            statement.setString(1, townName)
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val founder = resultSet.getString("founder")
                return founder == founderName
            }

            return false
        } catch (e: SQLException) {
            e.printStackTrace()

            return false
        } finally {
            connection.close()
        }
    }
    fun getTown(townName: String): Town {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT * FROM towns WHERE name = ?")
            statement.setString(1, townName)
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val name = resultSet.getString("name")
                val founder = resultSet.getString("founder")
                val town = Town(name, founder)
                loadTownMembers(town)
                return town
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }
        return Town("", "")
    }
    private fun loadTownMembers(town: Town) {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT player_name FROM player_towns WHERE town_name = ?")
            statement.setString(1, town.name)
            val resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val memberName = resultSet.getString("player_name")
                town.addMember(memberName)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }
    }

    fun removePlayerFromTown(playerName: String, townName: String) {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("DELETE FROM player_towns WHERE player_name = ? AND town_name = ?")
            statement.setString(1, playerName)
            statement.setString(2, townName)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }
    }
    fun getPlayerTown(playerName: String): String? {
        val connection = databaseManager.getConnection()

        try {
            val statement = connection.prepareStatement("SELECT town_name FROM player_towns WHERE player_name = ?")
            statement.setString(1, playerName)
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                return resultSet.getString("town_name")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return null
    }
    fun getTownMembers(townName: String): List<String> {
        val town = getTownByName(townName)

        if (town != null) {
            return town.members
        }

        return emptyList()
    }
    // this function is basically pointless btw lol
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

        return 0.0 
    }

    fun getAllTowns(): List<String> {
        val connection = databaseManager.getConnection()
        val townNames: MutableList<String> = mutableListOf()

        try {
            val statement = connection.prepareStatement("SELECT name FROM towns")
            val resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val name = resultSet.getString("name")
                townNames.add(name)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return townNames
    }
}
