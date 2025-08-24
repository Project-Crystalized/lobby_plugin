package gg.crystalized.lobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

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
    static ArrayList<Chunk> loadedChunks = new ArrayList<>();

    public static void setupEntityRefresh(){
        ArrayList<Location> list = new ArrayList<>();
        list.addAll(LobbyConfig.Locations.values());
        for(NPCData data : LobbyConfig.NPCs.values()){
            list.add(data.loc);
        }
        toBeRefreshed = list;
        loadChunks();
    }

    public static void loadChunks() {
        ArrayList<Location> delete = new ArrayList<>();
        for (Location loc : toBeRefreshed) {
            CompletableFuture<Chunk> future = Bukkit.getWorld("world").getChunkAtAsyncUrgently(loc);
            Runnable r = new Runnable() {
                public void run() {
                    refreshEntities(loc);
                }
            };
            CompletionStage<Chunk> c = future.minimalCompletionStage();
            future.runAfterBoth(c, r);
            delete.add(loc);
        }
        for (Location loc : delete) {
            toBeRefreshed.remove(loc);
        }
    }

    public static void refreshEntities(Location loc) {
        new BukkitRunnable() {
            public void run() {
                Collection<Entity> l = loc.getNearbyEntities(1, 1, 1);
                for (
                        Entity e : l) {
                    if (CitizensAPI.getNPCRegistry().isNPC(e)) {
                        e.remove();
                    }

                    if (e.getType() == EntityType.TEXT_DISPLAY) {
                        e.remove();
                    }
                }

                putWhatBelongsHere(loc);
            }
        }.runTaskLater(Lobby_plugin.getInstance(), 1);
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
    Object action = null;

    public static void newNPCData(String key, Map<String, JsonElement> map){
        NPCData data = new NPCData();
        String keyName = getKeyName(key, getUse(key));
        data.declareName(keyName, map);
        data.declareSkin(keyName, map);
        data.declareLocation(keyName, map);
        data.declareAction(keyName, map);
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

    public void declareAction(String keyName, Map<String, JsonElement> map){
        for(String k : map.keySet()){
            if(getKeyName(k, "dialogue").toLowerCase().contains(keyName) && k.contains("dialogue")){
                JsonArray array = map.get(k).getAsJsonArray();
                action = new ArrayList<>();
                for(int i = 0; i < array.size(); i++){
                    ((ArrayList)action).add(array.get(i).getAsString());
                }
                return;
            }

            if(getKeyName(k, "teleport").toLowerCase().contains(keyName) && k.contains("teleport")){
                JsonArray array = map.get(k).getAsJsonArray();
                if(array.size() == 3) {
                    action = new Location(Bukkit.getWorld("world"), array.get(0).getAsDouble(),  array.get(1).getAsDouble(),  array.get(2).getAsDouble());
                }else{
                    action = new Location(Bukkit.getWorld("world"), array.get(0).getAsDouble(),  array.get(1).getAsDouble(),  array.get(2).getAsDouble(), array.get(3).getAsFloat(), array.get(4).getAsFloat());
                }
                return;
            }

            if(getKeyName(k, "connect").toLowerCase().contains(keyName) && k.contains("connect")){
                action = map.get(k).getAsString();
                return;
            }
        }
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
        }else if(key.toLowerCase().contains("connect")){
            re = "connect";
        }else if(key.toLowerCase().contains("teleport")){
            re = "teleport";
        }
        return re;
    }

    public void spawnNPC(){
        try {
            NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name, loc);
            SkinTrait trait = npc.getOrAddTrait(SkinTrait.class);
            trait.setSkinPersistent(skinName, skinSignature, skinValue);
            npc.spawn(loc);
        }catch(IllegalArgumentException e){}
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

    public void action(Player p){
        if(action instanceof String){
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF((String)action);
            out.writeUTF("true");
            p.sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main",
                    out.toByteArray());
        }else if(action instanceof Location){
            p.teleport((Location) action);
        }else if(action instanceof ArrayList){
            Component c = Component.text("[" + name + "] ").color(TextColor.fromHexString("#bf8032"));
            for(String s : (ArrayList<String>)action){
                Component mess = Component.text(s).color(WHITE);
                p.sendMessage(c.append(mess));
            }
        }
    }
}
