package gg.crystalized.lobby;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.logging.Level;

public class LobbyConfig {
    public static Location spawn;
    public static Location litestrike_hub;
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

            JsonArray cloth = json.get("litestrike_hub").getAsJsonArray();
            clothing_room = new Location(Bukkit.getWorld("world"), cloth.get(0).getAsDouble(), cloth.get(1).getAsDouble(), cloth.get(2).getAsDouble(), cloth.get(3).getAsFloat(), cloth.get(4).getAsFloat());

        }catch(Exception e){
            if(e instanceof NoSuchFileException){
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
