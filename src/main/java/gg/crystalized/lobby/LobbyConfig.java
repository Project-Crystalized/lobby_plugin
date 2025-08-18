package gg.crystalized.lobby;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;

public class LobbyConfig {
   public static HashMap<String, Location> Locations = new HashMap<>();
   public static HashMap<String, NPCData> NPCs = new HashMap<>();

    public LobbyConfig(){
        try {
            final String directory = Files.readString(Paths.get("./world/lobby_config.json"));
            JsonObject json = JsonParser.parseString(directory).getAsJsonObject();

            JsonElement v = json.get("version");
            if(v.getAsInt() != 1){
                throw new Exception("incorrect lobby_config.json file version, please update your lobby_config.json");
            }
            Map<String, JsonElement> map = json.asMap();
            ArrayList<String> map2 = new ArrayList<>(map.keySet());

            for(String k : map.keySet()){
                if(!map2.contains(k)){
                    continue;
                }

                if(k.toLowerCase().contains("npc")){
                    NPCData.newNPCData(k, map);
                    map2 = NPCData.deleteRest(map2, k);
                    continue;
                }

                if(k.equals("version")){
                    continue;
                }

                JsonArray array = map.get(k).getAsJsonArray();
                Location loc;
                if(array.size() == 3) {
                    loc = new Location(Bukkit.getWorld("world"), array.get(0).getAsDouble(),  array.get(1).getAsDouble(),  array.get(2).getAsDouble());
                }else{
                    loc = new Location(Bukkit.getWorld("world"), array.get(0).getAsDouble(),  array.get(1).getAsDouble(),  array.get(2).getAsDouble(), array.get(3).getAsFloat(), array.get(4).getAsFloat());
                }

                Locations.put(k, loc);
            }

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
    static ArrayList<Location> toBeRefreshed;

    public static void setupEntityRefresh(){
        ArrayList<Location> list = new ArrayList<>();
        list.addAll(LobbyConfig.Locations.values());
        for(NPCData data : LobbyConfig.NPCs.values()){
            list.add(data.loc);
        }
        toBeRefreshed = list;
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
        ArrayList<Location> delete = new ArrayList<>();
        for(Location loc : toBeRefreshed) {
            if(!loc.getChunk().equals(c)){
                continue;
            }
            Collection<Entity> l = loc.getWorld().getNearbyEntities(loc, 1, 1, 1);
            for (Entity e : l) {
                if(e instanceof Player && !CitizensAPI.getNPCRegistry().isNPC(e)){
                    continue;
                }
                e.remove();
            }
            putWhatBelongsHere(loc);
            delete.add(loc);
        }
        for(Location loc : delete){
            toBeRefreshed.remove(loc);
        }
    }

    public static void putWhatBelongsHere(Location loc){
        for(String s : LobbyConfig.Locations.keySet()) {
            if (!LobbyConfig.Locations.get(s).equals(loc)) {
                continue;
            }
            if (s.toLowerCase().contains("leaderboard") || s.toLowerCase().contains("display")) {
                if (loc.equals(LobbyConfig.Locations.get("ls-leaderboard"))) {
                    new WinLeaderboard(Bukkit.getWorld("world"), "ls");
                } else if (loc.equals(LobbyConfig.Locations.get("ko-leaderboard"))) {
                    new WinLeaderboard(Bukkit.getWorld("world"), "ko");
                } else if (loc.equals(LobbyConfig.Locations.get("ls-ranked-display")) || loc.equals(LobbyConfig.Locations.get("ls-ranked-leaderboard"))) {
                    new RankDisplay();
                }
                return;
            }
        }

        for(NPCData data : LobbyConfig.NPCs.values()){
            if(data.loc.equals(loc)){
                data.spawnNPC();
            }
        }
    }

}

class NPCData{
    Location loc = null;
    String name = null;
    String skinName = null;
    String skinSignature = null;
    String skinValue = null;
    ArrayList<String> dialogue = new ArrayList<>();

    public static void newNPCData(String key, Map<String, JsonElement> map){
        NPCData data = new NPCData();
        String keyName = getKeyName(key, getUse(key));
        data.declareName(keyName, map);
        data.declareSkin(keyName, map);
        data.declareLocation(keyName, map);
        data.declareDialogue(keyName, map);
        LobbyConfig.NPCs.put(keyName, data);
    }

    public static String getKeyName(String key, String already){
        String k = key.replaceAll("-", "");
        k = k.replaceAll("_", "");
        k = k.toLowerCase().replaceAll(already, "");
        k = k.replaceAll("npc", "");
        return k;
    }

    public void declareName(String keyName, Map<String, JsonElement> map){
        for(String k : map.keySet()){
            if(getKeyName(k, "name").toLowerCase().contains(keyName) && k.contains("name")){
                name = map.get(k).getAsString();
                return;
            }
        }
    }

    public void declareSkin(String keyName, Map<String, JsonElement> map){
        for(String k : map.keySet()){
            if(getKeyName(k, "skin").toLowerCase().contains(keyName) && k.contains("skin")){
                JsonArray array = map.get(k).getAsJsonArray();
                skinName = array.get(0).getAsString();
                skinSignature = array.get(1).getAsString();
                skinValue = array.get(1).getAsString();
                return;
            }
        }
    }

    public void declareLocation(String keyName, Map<String, JsonElement> map){
        for(String k : map.keySet()){
            if(getKeyName(k, "location").toLowerCase().equals(keyName)){
                JsonArray array = map.get(k).getAsJsonArray();
                if(array.size() == 3) {
                    loc = new Location(Bukkit.getWorld("world"), array.get(0).getAsDouble(),  array.get(1).getAsDouble(),  array.get(2).getAsDouble());
                }else{
                    loc = new Location(Bukkit.getWorld("world"), array.get(0).getAsDouble(),  array.get(1).getAsDouble(),  array.get(2).getAsDouble(), array.get(3).getAsFloat(), array.get(4).getAsFloat());
                }
                return;
            }
        }
    }

    public void declareDialogue(String keyName, Map<String, JsonElement> map){
        for(String k : map.keySet()){
            if(getKeyName(k, "dialogue").toLowerCase().contains(keyName) && k.contains("dialogue")){
                JsonArray array = map.get(k).getAsJsonArray();
                for(int i = 0; i < array.size(); i++){
                    dialogue.add(array.get(i).getAsString());
                }
                return;
            }
        }
        dialogue = null;
    }

    public static String getUse(String key){
        String re = "";
        if(key.toLowerCase().contains("name")){
            re = "name";
        }else if(key.toLowerCase().contains("skin")){
            re = "skin";
        }else if(key.toLowerCase().contains("location")){
            re = "location";
        }else if(key.toLowerCase().contains("dialogue")){
            re = "dialogue";
        }
        return re;
    }

    public void spawnNPC(){
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name, loc);
        SkinTrait trait = new SkinTrait();
        trait.setSkinPersistent(skinName, skinSignature, skinValue);
        npc.getOrAddTrait(SkinTrait.class);
        npc.spawn(loc);
    }

    public static ArrayList<String> deleteRest(ArrayList<String> map, String key){
        String name = getKeyName(key, getUse(key));
        ArrayList<String> toRemove = new ArrayList<>();
        for(String k : map){
            if(name.equals(getKeyName(k, getUse(k)))){
                toRemove.add(k);
            }
        }

        for(String r : toRemove){
            map.remove(r);
        }

        return map;
    }
}
