package gg.crystalized.lobby.statistics;

import gg.crystalized.lobby.LobbyDatabase;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class StatsDatabase {

    public static final String LS_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/litestrike_db.sql";

    public static HashMap<String, Object> getLsGame(int id){
        try(Connection conn = DriverManager.getConnection(LS_URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM LiteStrikeGames WHERE game_id = ?;");
            prep.setInt(1, id);
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
            Bukkit.getLogger().warning("couldn't get stat data for ls game: " + id);
            return null;
        }
    }

    public static ArrayList<HashMap<String, Object>> getLsPlayerStats(OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(LS_URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM LiteStrikeGames WHERE player_uuid = ?;");
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            ResultSetMetaData data = set.getMetaData();
            int count = data.getColumnCount();
            ArrayList<HashMap<String, Object>> list = new ArrayList<>();
            while(set.next()) {
                HashMap<String, Object> map = new HashMap<>();
                for (int i = 1; i <= count; i++) {
                    map.put(data.getColumnLabel(i), set.getObject(data.getColumnLabel(i)));
                }
                list.add(map);
            }
            return list;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get stat data for ls player: " + p.getName());
            return null;
        }
    }
}
