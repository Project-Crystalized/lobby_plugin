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
            + "level 	INTEGER,"
            + "money     INTEGER,"
            + "skin_url    STRING"
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

        try (Connection conn = DriverManager.getConnection(URL)) {
            Statement stmt = conn.createStatement();
            stmt.execute(createLobbyPlayerTable);
            stmt.execute(createFriendsTable);
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
            Bukkit.getLogger().warning("couldn't get friend data for " + p.getName() + "UUID: " + p.getUniqueId());
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
    private static byte[] uuid_to_bytes(Player p) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        UUID uuid = p.getUniqueId();
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
