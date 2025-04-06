package gg.crystalized.lobby;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;

public class LevelManager {
    public static void giveExperience(Player p, float exp){
        float exp2 = p.getExp() + exp - 1;
        if(p.getExp() + exp >= 1){
            p.setLevel(p.getLevel() + 1);
            p.setExp(exp2);
        }else {
            p.setExp(p.getExp() + exp);
        }
        try(Connection conn = DriverManager.getConnection(LobbyDatabase.URL)){
            String insertData = "UPDATE LobbyPlayers SET exp_to_next_lvl = ?, level = ? WHERE player_uuid = ?";
            PreparedStatement prep = conn.prepareStatement(insertData);
            prep.setInt(1, (int) p.getExp());
            prep.setInt(2, p.getLevel());
            prep.setBytes(3, LobbyDatabase.uuid_to_bytes(p));
            prep.executeQuery();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("Couldn't update xp or level of " + p.getName());
        }
    }

    public static void updateLevel(Player p){
        try {
            ResultSet set = LobbyDatabase.fetchPlayerData(p);
            while(set.next()) {
                p.setLevel(set.getInt("level"));
                p.setExp(set.getInt("exp_to_next_lvl"));
            }
        }catch(SQLException exept){
            Bukkit.getLogger().warning(exept.getMessage());
            Bukkit.getLogger().warning("couldn't update level for " + p.getName());
        }
    }
}
