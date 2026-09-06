package gg.crystalized.lobby.parkour;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.HashMap;
import java.util.UUID;

public class ParkourDatabase {
    public static final String URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/lobby_db.sql";
    //IMPORTANT the best_time is being stored as tenth of a second
    public static void setup_parkour_table() {
        String createTable = "CREATE TABLE IF NOT EXISTS ParkourTimes ("
                + "player_uuid 			BLOB,"
                + "best_time 			INTEGER,"
                + "course 			STRING,"
                + "date 			INTEGER);";
        try (Connection conn = DriverManager.getConnection(URL)) {
            Statement stmt = conn.createStatement();
            stmt.execute(createTable);
        } catch (SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("continuing without parkour table");
        }
    }

    public static int getBestTime(OfflinePlayer p, String courseName){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT best_time FROM ParkourTimes WHERE player_uuid = ? AND course = ?;");
            prep.setBytes(1, uuid_to_bytes(p));
            prep.setString(2, courseName);
            ResultSet set = prep.executeQuery();
            if(!set.isBeforeFirst()) return 0;
            set.next();
            return set.getInt("best_time");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get data for " + p.getName() + "UUID: " + p.getUniqueId());
        }
        return 0;
    }

    public static void insert(OfflinePlayer p, String courseName, int best_time){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("INSERT INTO ParkourTimes(player_uuid, best_time, course, date) VALUES(?, ?, ?, unixepoch());");
            prep.setBytes(1, uuid_to_bytes(p));
            prep.setInt(2, best_time);
            prep.setString(3, courseName);
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't insert parkour data for " + p.getName() + "UUID: " + p.getUniqueId());
        }
    }

    public static void update(OfflinePlayer p, String courseName, int best_time){
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("UPDATE ParkourTimes SET best_time = ?, date = unixepoch() WHERE player_uuid = ? AND courseName = ?;");
            prep.setInt(1, best_time);
            prep.setBytes(2, uuid_to_bytes(p));
            prep.setString(3, courseName);
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't update parkour data for " + p.getName() + "UUID: " + p.getUniqueId());
        }
    }

    public static void saveRun(ParkourRun run){
        int timeInTenths = run.timer.tenth + (run.timer.seconds * 10) + (run.timer.minutes * 60 * 10) + (run.timer.hours * 60 * 60 * 10);
        int best_time = getBestTime(run.p, run.course.name);
        if(best_time == 0){
            insert(run.p, run.course.name, timeInTenths);
            return;
        }
        if(best_time >= timeInTenths) return;
        update(run.p, run.course.name, timeInTenths);
    }

    public static byte[] uuid_to_bytes(OfflinePlayer p) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        UUID uuid = p.getUniqueId();
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
