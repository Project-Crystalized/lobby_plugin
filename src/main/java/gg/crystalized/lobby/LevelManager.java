package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLevelChangeEvent;

import java.sql.*;
import java.util.HashMap;

import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class LevelManager implements Listener {

    public static void giveExperience(Player p, int exp){
        p.giveExp(exp);
        try(Connection conn = DriverManager.getConnection(LobbyDatabase.URL)){
            String insertData = "UPDATE LobbyPlayers SET exp_to_next_lvl = ?, level = ? WHERE player_uuid = ?";
            PreparedStatement prep = conn.prepareStatement(insertData);
            prep.setFloat(1, p.getExp());
            prep.setInt(2, p.getLevel());
            prep.setBytes(3, LobbyDatabase.uuid_to_bytes(p));
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("Couldn't update xp or level of " + p.getName());
        }
    }

    public static void updateLevel(Player p){
        HashMap<String, Object> map = LobbyDatabase.fetchPlayerData(p);
        if(map == null) return;
        p.setLevel((Integer)map.get("level"));
        p.setExp(getDouble((map.get("exp_to_next_lvl"))).floatValue());
    }

    public static Double getDouble(Object o){
        Double val = null;
        if (o instanceof Number) {
            val = ((Number) o).doubleValue();
        }
        return val;
    }
    @EventHandler
    public static void levelUp(PlayerLevelChangeEvent event){
        if(Lobby_plugin.getInstance().passive_mode){
            return;
        }
        Player p = event.getPlayer();
        if(!(event.getOldLevel() < event.getNewLevel())){
            return;
        }
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 1, 1); //TODO add better soundeffect
        p.sendActionBar(Component.text("LEVEL UP!").color(AQUA).decoration(BOLD, true)); //TODO make a better like thing for this idk how to call it

        for(Cosmetic c : Cosmetic.values()){
            if(c.obtainableLevel == null){
                continue;
            }
            if(c.obtainableLevel <= event.getNewLevel()){
                LobbyDatabase.addCosmetic(p, c, false);
            }
        }
    }

    public static int getMoney(Player p){
        HashMap<String, Object> map = LobbyDatabase.fetchPlayerData(p);
        return (Integer) map.get("money");
    }

    public static void giveMoney(Player p, int amount){
        try(Connection conn = DriverManager.getConnection(LobbyDatabase.URL)){
            String insertData = "UPDATE LobbyPlayers SET money = ? WHERE player_uuid = ?";
            PreparedStatement prep = conn.prepareStatement(insertData);
            prep.setInt(1, getMoney(p) + amount);
            prep.setBytes(2, LobbyDatabase.uuid_to_bytes(p));
            prep.executeUpdate();
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't update xp or level of " + p.getName());
        }
    }
}
