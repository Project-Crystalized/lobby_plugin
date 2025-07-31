package gg.crystalized.lobby;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;

public class LobbyConfig {
    public static Location spawn;
    public static Location litestrike_hub;
    public static Location ls_leaderboard;
    public static Location ls_ranked_display;
    public static Location ls_ranked_leaderboard;
    public static Location ko_leaderboard;
    public static Location clothing_room;

    public LobbyConfig(){
        try {
            final String directory = Files.readString(Paths.get("./world/lobby_config.json"));
            JsonObject json = JsonParser.parseString(directory).getAsJsonObject();

            JsonElement v = json.get("version");
            if(v.getAsInt() != 1){
                throw new Exception("incorrect lobby_config.json file version, please update your lobby_config.json");
            }

            JsonArray spa = json.get("spawn").getAsJsonArray();
            spawn = new Location(Bukkit.getWorld("world"), spa.get(0).getAsDouble(), spa.get(1).getAsDouble(), spa.get(2).getAsDouble(), spa.get(3).getAsFloat(), spa.get(4).getAsFloat());

            JsonArray lite = json.get("litestrike_hub").getAsJsonArray();
            litestrike_hub = new Location(Bukkit.getWorld("world"), lite.get(0).getAsDouble(), lite.get(1).getAsDouble(), lite.get(2).getAsDouble(), lite.get(3).getAsFloat(), lite.get(4).getAsFloat());

            JsonArray ls_lb = json.get("ls-leaderboard").getAsJsonArray();
            ls_leaderboard = new Location(Bukkit.getWorld("world"), ls_lb.get(0).getAsDouble(), ls_lb.get(1).getAsDouble(), ls_lb.get(2).getAsDouble());

            JsonArray ls_rd = json.get("ls-ranked-display").getAsJsonArray();
            ls_ranked_display = new Location(Bukkit.getWorld("world"), ls_rd.get(0).getAsDouble(), ls_rd.get(1).getAsDouble(), ls_rd.get(2).getAsDouble());

            JsonArray ls_rlb = json.get("ls-ranked-leaderboard").getAsJsonArray();
            ls_ranked_leaderboard = new Location(Bukkit.getWorld("world"), ls_rlb.get(0).getAsDouble(), ls_rlb.get(1).getAsDouble(), ls_rlb.get(2).getAsDouble());

            JsonArray ko_lb = json.get("ko-leaderboard").getAsJsonArray();
            ko_leaderboard = new Location(Bukkit.getWorld("world"), ko_lb.get(0).getAsDouble(), ko_lb.get(1).getAsDouble(), ko_lb.get(2).getAsDouble());

            JsonArray cloth = json.get("litestrike_hub").getAsJsonArray(); //TODO
            clothing_room = new Location(Bukkit.getWorld("world"), cloth.get(0).getAsDouble(), cloth.get(1).getAsDouble(), cloth.get(2).getAsDouble(), cloth.get(3).getAsFloat(), cloth.get(4).getAsFloat());

        }catch(Exception e){
            if(e instanceof NoSuchFileException){
                Bukkit.getLogger().warning("[Lobby_plugin] Couldn't find lobby_config.json. Starting Lobby_plugin in passive mode.");
                Lobby_plugin.getInstance().passive_mode = true;
                return;
            }
            Bukkit.getLogger().log(Level.SEVERE, "Could not load the lobby configuration file!\n Error: " + e);
            e.printStackTrace();
            Bukkit.getLogger().log(Level.SEVERE, "The Plugin will be disabled!");
            Bukkit.getPluginManager().disablePlugin(Lobby_plugin.getInstance());
            throw new RuntimeException(new Exception());
        }

    }
}

class EntityRefresh implements Listener{
    static ArrayList<Location> toBeRefreshed = new ArrayList<>();

    public static void setupEntityRefresh(){
        toBeRefreshed.add(LobbyConfig.ls_leaderboard);
        toBeRefreshed.add(LobbyConfig.ls_ranked_display);
        toBeRefreshed.add(LobbyConfig.ls_ranked_leaderboard);
        toBeRefreshed.add(LobbyConfig.ko_leaderboard);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e){
        if(Lobby_plugin.getInstance().passive_mode){
            return;
        }
        new BukkitRunnable() {
            public void run() {
                refreshEntities(e.getChunk());
            }
        }.runTaskLater(Lobby_plugin.getInstance(), 5);
    }

    public static void refreshEntities(Chunk c){
        //this basically just deletes and places game relevant entities

        for(Location loc : toBeRefreshed) {
            if(!loc.getChunk().equals(c)){
                continue;
            }
            Collection<Entity> l = loc.getWorld().getNearbyEntities(loc, 1, 1, 1);
            for (Entity e : l) {
                if(e instanceof Player){
                    continue; //TODO might have to add a check whether it's from the citizens plugin
                }
                e.remove();
            }
            putWhatBelongsHere(loc);
            toBeRefreshed.remove(loc);
        }

    }

    public static void putWhatBelongsHere(Location loc){
        if(loc.equals(LobbyConfig.ls_leaderboard)){
            new WinLeaderboard(Bukkit.getWorld("world"), "ls");
        }else if(loc.equals(LobbyConfig.ko_leaderboard)){
            new WinLeaderboard(Bukkit.getWorld("world"), "ko");
        }else if(loc.equals(LobbyConfig.ls_ranked_display) || loc.equals(LobbyConfig.ls_ranked_leaderboard)){
            new RankDisplay();
        }

        //TODO citizens
    }
}
