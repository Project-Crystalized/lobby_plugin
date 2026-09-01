package gg.crystalized.lobby.parkour;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

public class ParkourConfig {

    public ParkourConfig(){
        try {
            final String directory = Files.readString(Paths.get("./world/parkour_config.json"));
            JsonObject json = JsonParser.parseString(directory).getAsJsonObject();

            JsonElement v = json.get("version");
            if(v.getAsInt() != 1){
                throw new Exception("incorrect lobby_config.json file version, please update your lobby_config.json");
            }

            Map<String, JsonElement> map = json.asMap();
            ArrayList<String> objects = new ArrayList<>(map.keySet());

            for(String s : objects){
                JsonObject course = json.getAsJsonObject(s);
                JsonArray leaderboard = course.get("leaderboard").getAsJsonArray();
                new Parkour(TextColor.fromHexString(course.get("color").getAsString()), course.get("name").getAsString(), getCheckpoints(course.get("checkpoints")), new Location(Bukkit.getWorld("world"), leaderboard.get(0).getAsInt(), leaderboard.get(1).getAsInt(), leaderboard.get(2).getAsInt()));
            }
        }catch(Exception e){
            Bukkit.getLogger().info("[Lobby_plugin] Couldn't find parkour_config.json. Continuing without.");
        }
    }

    public Location[] getCheckpoints(JsonElement course){
        JsonArray array = course.getAsJsonArray();
        Location[] checkpoints = new Location[array.size()];
        for(int i = 0; i < array.size(); i++){
            JsonArray checkpoint = array.get(i).getAsJsonArray();
            checkpoints[i] = new Location(Bukkit.getWorld("world"), checkpoint.get(0).getAsInt(), checkpoint.get(1).getAsInt(), checkpoint.get(2).getAsInt());
        }
        return checkpoints;
    }
}
