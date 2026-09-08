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
                + "date 			INTEGER,"
                + "UNIQUE(player_uuid, course));";
        try (Connection conn = DriverManager.getConnection(URL)) {
            Statement stmt = conn.createStatement();
            stmt.execute(createTable);
        } catch (SQLException e) {
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("continuing without parkour table");
        }
    }

    public static void saveRun(ParkourRun run){
        int timeInTenths = run.timer.tenth + (run.timer.seconds * 10) + (run.timer.minutes * 60 * 10) + (run.timer.hours * 60 * 60 * 10);
        try(Connection conn = DriverManager.getConnection(URL)){
            PreparedStatement prep = conn.prepareStatement("INSERT INTO ParkourTimes(player_uuid, best_time, course, date) VALUES(?, ?, ?, unixepoch()) "
                    + "ON CONFLICT(player_uuid, course) DO UPDATE SET best_time = excluded.best_time, date = unixepoch() "
                    + "WHERE excluded.best_time < ParkourTimes.best_time;");
            prep.setBytes(1, uuid_to_bytes(run.p));
            prep.setInt(2, timeInTenths);
            prep.setString(3, run.course.name);
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't save parkour data for " + run.p.getName() + "UUID: " + run.p.getUniqueId());
        }
    }

    public static byte[] uuid_to_bytes(OfflinePlayer p) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        UUID uuid = p.getUniqueId();
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
