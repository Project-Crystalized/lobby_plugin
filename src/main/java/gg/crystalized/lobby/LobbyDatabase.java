package gg.crystalized.lobby;
import java.nio.ByteBuffer;
import java.sql.*;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import javax.swing.text.DateFormatter;

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
            + "skin_url    STRING,"
            + "first_login   INTEGER,"
            + "last_login   INTEGER,"
            + "times_logged_in   INTEGER,"
            + "last_quest_roll    INTEGER,"
            + "quest_rerolls    INTEGER"
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

    String createQuestsTable = "CREATE TABLE IF NOT EXISTS Quests ("
            + "player_uuid        BLOB,"
            + "quest        TEXT,"
            + "done      INTEGER,"
            + "claimed    INTEGER"
            +");";

        try (Connection conn = DriverManager.getConnection(URL)) {
            Statement stmt = conn.createStatement();
            stmt.execute(createLobbyPlayerTable);
            stmt.execute(createFriendsTable);
            stmt.execute(createCosmeticsTable);
            stmt.execute(createSettingsTable);
            stmt.execute(createTemporaryData);
            stmt.execute(createQuestsTable);
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

    public static boolean loggedInToday(Player player){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT last_login FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(player));
            ResultSet set = prep.executeQuery();
            set.next();
            int seconds = set.getInt("last_login");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate lastLogin = LocalDate.parse(new Date(Long.parseLong("" + seconds) * 1000).toString(), formatter);
            LocalDate currentDate = LocalDate.now();
            return lastLogin.getDayOfYear() == currentDate.getDayOfYear() && lastLogin.getYear() == currentDate.getYear();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            //Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
            return false;
        }
    }

    public static boolean loggedInYesterday(Player player){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT last_login FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(player));
            ResultSet set = prep.executeQuery();
            set.next();
            int seconds = set.getInt("last_login");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate lastLogin = LocalDate.parse(new Date(Long.parseLong("" + seconds) * 1000).toString(), formatter);
            LocalDateTime currentDateTime = LocalDateTime.now();
            return lastLogin.getDayOfYear() == currentDateTime.getDayOfYear() -1 && lastLogin.getYear() == currentDateTime.getYear();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            //Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
            return false;
        }
    }

    public static void updateLastLogin(Player player){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("UPDATE LobbyPlayers SET last_login = unixepoch() WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(player));
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            //Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
        }
    }

    public static void updateLoginTimes(Player player){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("UPDATE LobbyPlayers SET times_logged_in = ? WHERE player_uuid = ?;");
            prep.setInt(1, getTimesLoggedIn(player) + 1);
            prep.setBytes(2, uuid_to_bytes(player));
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            //Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
        }
    }

    public static int getTimesLoggedIn(Player player){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT times_logged_in FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(player));
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt("times_logged_in");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            //Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
            return 0;
        }
    }

    public static String getPlayerName(OfflinePlayer player){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT player_name FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(player));
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getString("player_name");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            //Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
            return "null";
        }
    }

    public static boolean wasFirstLogin(Player player){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT last_login, first_login FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(player));
            ResultSet set = prep.executeQuery();
            set.next();
            int last = set.getInt("last_login");
            int first = set.getInt("first_login");
            return last == first;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            //Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
            return false;
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
                Object[] o = new Object[3];
                for (int i = 1; i <= count; i++) {
                    o[i-1] = set.getObject(data.getColumnLabel(i));
                }
                list.add(o);
            }
            return list;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get friend data for " + p.getName() + " UUID: " + p.getUniqueId());
            return null;
        }
    }

    public static ArrayList<Object[]> fetchCosmetics(OfflinePlayer p){
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
            PreparedStatement prep = conn.prepareStatement("INSERT INTO Cosmetics(player_uuid, cosmetic_id, currently_wearing) VALUES(?, ?, ?);");
            prep.setBytes(1, uuid_to_bytes(p));
            prep.setInt(2, c.id);
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
            PreparedStatement prep = conn.prepareStatement("UPDATE Cosmetics SET currently_wearing = ? WHERE player_uuid = ? AND cosmetic_id = ?;");
            int i = 0;
            if(wearing){
               i = 1;
            }
            prep.setInt(1, i);
            prep.setBytes(2, uuid_to_bytes(p));
            prep.setInt(3, c.id);
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
            String makeNewEntry = "INSERT INTO LobbyPlayers(player_uuid, player_name,exp_to_next_lvl, level, money, online, rank_id, pay_rank_id, skin_url, first_login, last_login, times_logged_in, last_quest_roll, quest_rerolls)"
                    + "VALUES (?, ?, 0, 0, 0, 0, 0, 0, ?, unixepoch(), unixepoch(), 1, unixepoch(), ?);";
            PreparedStatement prepared = conn.prepareStatement(makeNewEntry);
            prepared.setBytes(1, uuid_to_bytes(p));
            prepared.setString(2, p.getName());
            prepared.setString(3, p.getPlayerProfile().getTextures().getSkin().toString());
            prepared.setInt(4, Ranks.getPayRank(p) == 6 ? 1 : Ranks.getPayRank(p) == 7 ? 2 : 0);
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

    public static boolean ownsCosmetic(OfflinePlayer p, Cosmetic c){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT COUNT(*) AS count FROM Cosmetics WHERE player_uuid = ? AND cosmetic_id = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            prep.setInt(2, c.id);
            if(prep.executeQuery().getInt("count") > 0){
                return true;
            }
            return false;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't whether player owns cosmetic");
            return false;
        }
    }

    public static boolean isWearing(OfflinePlayer p, Cosmetic c){
        if(!ownsCosmetic(p, c)){
            return false;
        }
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT currently_wearing FROM Cosmetics WHERE player_uuid = ? AND cosmetic_id = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            prep.setInt(2, c.id);
            if(prep.executeQuery().getInt("currently_wearing") == 1){
                return true;
            }
            return false;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't whether player owns cosmetic");
            return false;
        }
    }

    public static Cosmetic getShardcore(OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT cosmetic_id FROM Cosmetics WHERE player_uuid = ? AND currently_wearing = 1;");
            prep.setBytes(1, uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            while(set.next()){
                if(Cosmetic.getCosmeticById(set.getInt("cosmetic_id")) == null){
                    continue;
                }

                if(Cosmetic.getCosmeticById(set.getInt("cosmetic_id")).slot == EquipmentSlot.HAND){
                    return Cosmetic.getCosmeticById(set.getInt("cosmetic_id"));
                }
            }
            return null;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't couldn't get shardcore");
            return null;
        }
    }

    public static void rollOrFetchQuests(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT last_quest_roll FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            int seconds = set.getInt("last_quest_roll");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate lastRoll = LocalDate.parse(new Date(Long.parseLong("" + seconds) * 1000).toString(), formatter);
            LocalDate currentDate = LocalDate.now();
            if(currentDate.getDayOfYear() - lastRoll.getDayOfYear() >= 7 || currentDate.getYear() != lastRoll.getYear()){
                PreparedStatement pr = conn.prepareStatement("UPDATE LobbyPlayers SET last_quest_roll = unixepoch() WHERE player_uuid = ?;");
                pr.setBytes(1, uuid_to_bytes(p));
                pr.executeUpdate();
                rollQuests(p);
                return;
            }
            fetchQuests(p);
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't roll or fetch quests");
        }
    }

    public static void rollQuests(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            Quest.removeQuests(p);
            PreparedStatement prep = conn.prepareStatement("DELETE FROM Quests WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            prep.executeUpdate();
            Quest[] quests = Quest.rollQuests(p);
            PreparedStatement pr = conn.prepareStatement("INSERT INTO Quests(player_uuid, quest, done, claimed) VALUES (?, ?, 0, 0);");
            for(Quest q : quests){
                pr.setBytes(1, uuid_to_bytes(p));
                pr.setString(2, q.questNumber);
                pr.executeUpdate();
            }

            PreparedStatement pre = conn.prepareStatement("UPDATE Quests SET quest_rerolls = ? WHERE player_uuid = ?;");
            pre.setInt(1, Ranks.getPayRank(p) == 6 ? 1 : Ranks.getPayRank(p) == 7 ? 2 : 0);
            pre.setBytes(2, uuid_to_bytes(p));
            pre.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't roll quests");
        }
    }

    public static void fetchQuests(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM Quests WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            while(set.next()){
                String number = set.getString("quest");
                int done = set.getInt("done");
                int claimed = set.getInt("claimed");
                Quest.allQuests.add(new Quest(p, number, claimed == 1, done == 1));
            }
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't fetch quests");
        }
    }

    public static void replaceQuest(Player p, Quest old, Quest nevv){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("UPDATE Quests SET quest = ? WHERE player_uuid = ? AND quest = ?;");
            prep.setString(1, nevv.questNumber);
            prep.setBytes(2, uuid_to_bytes(p));
            prep.setString(3, old.questNumber);
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't replace quest");
        }
    }

    public static int getLastQuestRoll(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT last_quest_roll FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt("last_quest_roll");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get last quest roll");
        }
        return -1;
    }

    public static boolean canRerollQuest(Quest q){
        if(Objects.equals(q.questNumber, "-1")) return false;
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT quest_rerolls FROM LobbyPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(q.player));
            ResultSet set = prep.executeQuery();
            set.next();
            if (set.getInt("quest_rerolls") > 0){
                return true;
            }
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't reroll quests");
        }
        return false;
    }

    public static void rerollReduce(Player p){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("UPDATE LobbyPlayers SET quest_rerolls = quest_rerolls -1 WHERE player_uuid = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't reroll reduce");
        }
    }

    public static void setQuestRerolls(OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("UPDATE LobbyPlayers SET quest_rerolls = ? WHERE player_uuid = ?;");
            prep.setInt(1, Ranks.getPayRank(p) == 6 ? 1 : Ranks.getPayRank(p) == 7 ? 2 : 0);
            prep.setBytes(2, uuid_to_bytes(p));
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get rerolls");
        }
    }

    public static void questCompleted(Player p, String quest){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("UPDATE Quests SET done = 1 WHERE player_uuid = ? AND quest = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            prep.setString(2, quest);
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't complete quest");
        }
    }

    public static void questClaimed(Player p, String quest){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("UPDATE Quests SET claimed = 1 WHERE player_uuid = ? AND quest = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            prep.setString(2, quest);
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't claim quest");
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
