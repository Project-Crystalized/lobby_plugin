package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerLevelChangeEvent;

import java.sql.*;

import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class LevelManager{
    public static void giveExperience(Player p, int exp){
        p.giveExp(exp);
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
    @EventHandler
    private static void levelUp(PlayerLevelChangeEvent event){
        Player p = event.getPlayer();
        if(!(event.getOldLevel() < event.getNewLevel())){
            return;
        }
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 1, 1); //TODO add better soundeffect
        p.sendActionBar(Component.text("LEVEL UP!").color(AQUA).decoration(BOLD, true));
        //TODO add more cosmetics and then some back to this
    }
}
