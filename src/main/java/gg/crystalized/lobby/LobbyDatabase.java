package gg.crystalized.lobby;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class LobbyDatabase {
    public static final String URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/lobby_db.sql";
    public static void setup_databases(){
    String createLobbyPlayerTable = "CREATE TABLE IF NOT EXISTS LobbyPlayers ("
            + "player_uuid 			BLOB UNIQUE,"
            + "player_name 			STRING,"
            + "exp_to_next_lvl       INTEGER,"
            + "level 	INTEGER,"
            + "money     INTEGER,"
            + "online     INTEGER,"
            + "rank_id     INTEGER,"
            + "pay_rank_id   INTEGER,"
            + "skin_url    STRING"
            + ");";

    String createFriendsTable = "CREATE TABLE IF NOT EXISTS Friends ("
            + "player_uuid 			BLOB,"
            + "friend_uuid 	BLOB,"
            + "date   STRING"
            + ");";

    //cosmetics will have an id
    String createCosmeticsTable = "CREATE TABLE IF NOT EXISTS Cosmetics ("
            + "player_uuid        BLOB,"
            + "cosmetic_id        INTEGER,"
            + "currently_wearing   INTEGER"
            +");";

    String createSettingsTable = "CREATE TABLE IF NOT EXISTS Settings ("
            + "player_uuid        BLOB UNIQUE,"
            + "dms   INTEGER,"
            + "pig_game   INTEGER,"
            + "show_players   INTEGER,"
            + "height   INTEGER,"
            + "friends_requests   INTEGER,"
            + "party_requests   INTEGER"
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
            stmt.execute(createSettingsTable);
            stmt.execute(createTemporaryData);
        } catch (SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("continuing without database");
        }
    }

    public static HashMap<String, Object> fetchPlayerData(OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            ResultSetMetaData data = set.getMetaData();
            int count = data.getColumnCount();
            HashMap<String, Object> map = new HashMap<>();
            for(int i = 1; i <= count; i++){
                map.put(data.getColumnLabel(i), set.getObject(data.getColumnLabel(i)));
            }
            return map;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
            return null;
        }
    }

    public static HashMap<String, Object> fetchPlayerData(byte[] p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, p);
            ResultSet set = prep.executeQuery();
            set.next();
            ResultSetMetaData data = set.getMetaData();
            int count = data.getColumnCount();
            HashMap<String, Object> map = new HashMap<>();
            for(int i = 1; i <= count; i++){
                map.put(data.getColumnLabel(i), set.getObject(data.getColumnLabel(i)));
            }
            return map;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            //Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
            return null;
        }
    }

    public static HashMap<String, Object> fetchPlayerData(String p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM LobbyPlayers WHERE player_name = ?;");
            prep.setString(1, p);
            ResultSet set = prep.executeQuery();
            set.next();
            ResultSetMetaData data = set.getMetaData();
            int count = data.getColumnCount();
            HashMap<String, Object> map = new HashMap<>();
            for(int i = 1; i <= count; i++){
                map.put(data.getColumnLabel(i), set.getObject(data.getColumnLabel(i)));
            }
            return map;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            //Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
            return null;
        }
    }

    public static ArrayList<Object[]> fetchFriends(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM Friends WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            ResultSetMetaData data = set.getMetaData();
            int count = data.getColumnCount();
            ArrayList<Object[]> list = new ArrayList<>();
            while(set.next()) {
                while(set.next()) {
                    Object[] o = new Object[3];
                    for (int i = 1; i <= count; i++) {
                        o[i-1] = set.getObject(data.getColumnLabel(i));
                    }
                    list.add(o);
                }
            }
            return list;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get friend data for " + p.getName() + " UUID: " + p.getUniqueId());
            return null;
        }
    }

    public static ArrayList<Object[]> fetchCosmetics(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM Cosmetics WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            ResultSetMetaData data = set.getMetaData();
            int count = data.getColumnCount();
            ArrayList<Object[]> list = new ArrayList<>();
            while(set.next()) {
                Object[] o = new Object[3];
                for (int i = 1; i <= count; i++) {
                    o[i-1] = set.getObject(data.getColumnLabel(i));
                }
                list.add(o);
            }
            return list;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get cosmetic data for " + p.getName() + "UUID: " + p.getUniqueId());
            return null;
        }
    }

    public static void addCosmetic(Player p, Cosmetic c, boolean wearing){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("INSERT INTO Cosmetics(player_uuid, cosmetic_id, currently_wearing) VALUES(?, ?, ?)");
            prep.setBytes(1, uuid_to_bytes(p));
            prep.setInt(2, c.ordinal());
            int i = 0;
            if(wearing){
                i = 1;
            }
            prep.setInt(3, i);
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("failed adding cosmetic to database");
        }
    }
    // 0 = false
    // 1 = true
    public static void cosmeticSetWearing(Player p, Cosmetic c, boolean wearing){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("UPDATE Cosmetics SET currently_wearing = ? WHERE player_uuid = ? AND cosmetic_id = ?");
            int i = 0;
            if(wearing){
               i = 1;
            }
            prep.setInt(1, i);
            prep.setBytes(2, uuid_to_bytes(p));
            prep.setInt(3, c.ordinal());
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("failed set wearing of cosmetic in database");
        }
    }

    public static HashMap<String, Object> fetchAndDeleteTemporaryData(Player p){
        //CAREFUL: This makes it so the temporary data can only be retrieved once -> handle with care!!
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM TemporaryData WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            ResultSet set =  prep.executeQuery();
            PreparedStatement prepared = conn.prepareStatement("DELETE FROM TemporaryData WHERE player_uuid = ?;");
            prepared.setBytes(1, uuid_to_bytes(p));
            prepared.executeUpdate();
            set.next();
            ResultSetMetaData data = set.getMetaData();
            int count = data.getColumnCount();
            HashMap<String, Object> map = new HashMap<>();
            for(int i = 1; i <= count; i++){
                map.put(data.getColumnLabel(i), set.getObject(data.getColumnLabel(i)));
            }
            return map;
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
            Bukkit.getLogger().warning("couldn't check existence in database for " + p.getName() + " UUID: " + p.getUniqueId());
            return false;
        }
    }

    public static void makeNewLobbyPlayersEntry(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            String makeNewEntry = "INSERT INTO LobbyPlayers(player_uuid, player_name,exp_to_next_lvl, level, money, online, rank_id, pay_rank_id, skin_url)"
                    + "VALUES (?, ?, 0, 0, 0, 0, 0, 0, ?)";
            PreparedStatement prepared = conn.prepareStatement(makeNewEntry);
            prepared.setBytes(1, uuid_to_bytes(p));
            prepared.setString(2, p.getName());
            prepared.setString(3, p.getPlayerProfile().getTextures().getSkin().toString());
            prepared.executeUpdate();
        }catch(SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't make database entry for " + p.getName() + " UUID: " + p.getUniqueId());
        }
    }

    public static void makeNewSettingsEntry(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            String makeNewEntry = "INSERT INTO Settings(player_uuid, dms, pig_game, show_players, height, friends_requests, party_requests)"
                    + "VALUES (?, 1 ,0 ,1 ,0.5 ,1, 1)";
            PreparedStatement prepared = conn.prepareStatement(makeNewEntry);
            prepared.setBytes(1, uuid_to_bytes(p));
            prepared.executeUpdate();
        }catch(SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't make database entry for " + p.getName() + " UUID: " + p.getUniqueId());
        }
    }

    public static HashMap<String, Object> fetchSettings(OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM Settings WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            ResultSetMetaData data = set.getMetaData();
            int count = data.getColumnCount();
            HashMap<String, Object> map = new HashMap<>();
            for(int i = 1; i <= count; i++){
                map.put(data.getColumnLabel(i), set.getObject(data.getColumnLabel(i)));
            }
            return map;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get settings data for " + p.getName() + "UUID: " + p.getUniqueId());
            return null;
        }
    }

    public static void updateSetting(Player p, String dbSettingName, double value){
        try(Connection conn = DriverManager.getConnection(URL)){
            String makeNewEntry = "UPDATE Settings SET "+ dbSettingName + " = ? WHERE player_uuid = ?";
            PreparedStatement prepared = conn.prepareStatement(makeNewEntry);
            prepared.setDouble(1, value);
            prepared.setBytes(2, uuid_to_bytes(p));
            prepared.executeUpdate();
        }catch(SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't make database entry for " + p.getName() + " UUID: " + p.getUniqueId());
        }
    }

    public static void updatePlayerNames(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            String makeNewEntry = "UPDATE LobbyPlayers SET player_name = ? WHERE player_uuid = ?";
            PreparedStatement prepared = conn.prepareStatement(makeNewEntry);
            prepared.setString(1, p.getName());
            prepared.setBytes(2, uuid_to_bytes(p));
            prepared.executeUpdate();
        }catch(SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't make database entry for " + p.getName() + " UUID: " + p.getUniqueId());
        }
    }

    public static void updateSkin(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            String makeNewEntry = "UPDATE LobbyPlayers SET skin_url = ? WHERE player_uuid = ?";
            PreparedStatement prepared = conn.prepareStatement(makeNewEntry);
            prepared.setString(1, p.getPlayerProfile().getTextures().getSkin().toString());
            prepared.setBytes(2, uuid_to_bytes(p));
            prepared.executeUpdate();
        }catch(SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't make database entry for " + p.getName() + " UUID: " + p.getUniqueId());
        }
    }

    public static void setOnline(Player p, boolean online){
        try(Connection conn = DriverManager.getConnection(URL)){
            String makeNewEntry = "UPDATE LobbyPlayers SET online = ? WHERE player_uuid = ?";
            PreparedStatement prepared = conn.prepareStatement(makeNewEntry);
            int on = 0;
            if(online) on = 1;
            prepared.setInt(1, on);
            prepared.setBytes(2, uuid_to_bytes(p));
            prepared.executeUpdate();
        }catch(SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't make database entry for " + p.getName() + " UUID: " + p.getUniqueId());
        }
    }

    public static void setRank(OfflinePlayer p, int rankID){
        try(Connection conn = DriverManager.getConnection(URL)){
            String makeNewEntry = "UPDATE LobbyPlayers SET rank_id = ? WHERE player_uuid = ?";
            PreparedStatement prepared = conn.prepareStatement(makeNewEntry);
            prepared.setInt(1, rankID);
            prepared.setBytes(2, uuid_to_bytes(p));
            prepared.executeUpdate();
        }catch(SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't make database entry for " + p.getName() + " UUID: " + p.getUniqueId());
        }
    }

    public static void setPayedRank(OfflinePlayer p, int rankID){
        try(Connection conn = DriverManager.getConnection(URL)){
            String makeNewEntry = "UPDATE LobbyPlayers SET pay_rank_id = ? WHERE player_uuid = ?";
            PreparedStatement prepared = conn.prepareStatement(makeNewEntry);
            prepared.setInt(1, rankID);
            prepared.setBytes(2, uuid_to_bytes(p));
            prepared.executeUpdate();
        }catch(SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't make database entry for " + p.getName() + " UUID: " + p.getUniqueId());
        }
    }

    public static boolean areFriends(Player p, Player friend){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT COUNT(*) AS count FROM Friends WHERE player_uuid = ? AND friend_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            prep.setBytes(2, uuid_to_bytes(friend));
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

    public static byte[] uuid_to_bytes(Player p) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        UUID uuid = p.getUniqueId();
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static byte[] uuid_to_bytes(OfflinePlayer p) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        UUID uuid = p.getUniqueId();
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
