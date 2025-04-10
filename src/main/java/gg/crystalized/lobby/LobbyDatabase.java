package gg.crystalized.lobby;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LobbyDatabase {
    public static final String URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/lobby_db.sql";
    public static void setup_databases(){
    String createLobbyPlayerTable = "CREATE TABLE IF NOT EXISTS LobbyPlayers ("
            + "player_uuid 			BLOB UNIQUE,"
            + "exp_to_next_lvl       INTEGER,"
            + "level 	INTEGER,"
            + "money     INTEGER,"
            + "skin_url    STRING,"
            + "shardcore_id     INTEGER"
            + ");";

    String createFriendsTable = "CREATE TABLE IF NOT EXISTS Friends ("
            + "player_uuid 			BLOB,"
            + "friend_uuid 	BLOB,"
            + "date   INTEGER"
            + ");";

    //cosmetics will have an id
    String createCosmeticsTable = "CREATE TABLE IF NOT EXISTS Cosmetics ("
            + "player_uuid        BLOB,"
            + "cosmetic_id        INTEGER"
            +");";

    String createTemporaryData = "CREATE TABLE IF NOT EXISTS TemporaryData ("
            + "player_uuid        BLOB UNIQUE,"
            + "xp_amount        INTEGER,"
            + "money_amount      INTEGER"
            +");";

        try (Connection conn = DriverManager.getConnection(URL)) {
            Statement stmt = conn.createStatement();
            stmt.execute(createLobbyPlayerTable);
            stmt.execute(createFriendsTable);
            stmt.execute(createCosmeticsTable);
            stmt.execute(createTemporaryData);
        } catch (SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("continueing without database");
        }
    }

    public static ResultSet fetchPlayerData(Player p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            return prep.executeQuery();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
            return null;
        }
    }

    public static ResultSet fetchFriends(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM Friends WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            return prep.executeQuery();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get friend data for " + p.getName() + " UUID: " + p.getUniqueId());
            return null;
        }
    }

    public static ResultSet fetchCosmetics(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM Cosmetics WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            return prep.executeQuery();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get cosmetic data for " + p.getName() + "UUID: " + p.getUniqueId());
            return null;
        }
    }

    public static ResultSet fetchAndDeleteTemporaryData(Player p){
        //CAREFUL: This makes it so the temporary data can only be retrieved once -> handle with care!!
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM TemporaryData WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            ResultSet set =  prep.executeQuery();
            PreparedStatement prepared = conn.prepareStatement("DELETE FROM TemporaryData WHERE player_uuid = ?;");
            prepared.setBytes(1, uuid_to_bytes(p));
            prepared.executeUpdate();
            return set;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get cosmetic data for " + p.getName() + "UUID: " + p.getUniqueId());
            return null;
        }
    }

    public static boolean isPlayerInDatabase(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT COUNT(*) AS count FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            if(prep.executeQuery().getInt("count") > 0){
                return true;
            }
            return false;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't check excistence in database for " + p.getName() + " UUID: " + p.getUniqueId());
            return false;
        }
    }

    public static void makeNewLobbyPlayersEntry(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            String makeNewEntry = "INSERT INTO LobbyPlayers(player_uuid, exp_to_next_lvl, level, money, skin_url, shardcore_id)"
                    + "VALUES (?, 0, 0, 0, ?, 1)";
            PreparedStatement prepared = conn.prepareStatement(makeNewEntry);
            prepared.setBytes(1, uuid_to_bytes(p));
            prepared.setString(2, p.getPlayerProfile().getTextures().getSkin().toString());
        }catch(SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't make database entry for " + p.getName() + " UUID: " + p.getUniqueId());
        }
    }
    public static byte[] uuid_to_bytes(Player p) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        UUID uuid = p.getUniqueId();
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
