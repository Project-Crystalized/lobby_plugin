package gg.crystalized.lobby;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LevelManager {
    public static void giveExperience(Player p, float exp){
        p.setExp(p.getExp() + exp);
        try(Connection conn = DriverManager.getConnection(LobbyDatabase.URL)){
            String insertData = "UPDATE LobbyPlayer SET exp_to_next_lvl = ?, level = ? WHERE player_uuid = ?";
            PreparedStatement prep = conn.prepareStatement(insertData);
            prep.setInt(1, (int) p.getExp());
            prep.setInt(2, p.getLevel());
            prep.setBytes(3, LobbyDatabase.uuid_to_bytes(p));
            prep.executeQuery();
        }catch(SQLException e){
            Bukkit.getLogger().warning("Couldn't update xp or level of " + p.getName());
        }
    }
}
