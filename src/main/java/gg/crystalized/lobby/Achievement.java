package gg.crystalized.lobby;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

import static gg.crystalized.lobby.LobbyDatabase.uuid_to_bytes;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class Achievement extends Quest{
    static ArrayList<AchieveTemplate> templates = new ArrayList<>();
    static ArrayList<Achievement> achievements = new ArrayList<>();
    int stage;
    AchieveTemplate temp;

    public Achievement(Player player, AchieveTemplate temp, int stage, boolean done, boolean claimed){
        super(player, temp.id + temp.stages.get(stage), done, claimed);
        this.stage = stage;
        this.temp = temp;
    }

    public static void getAchievementsFromJson(){
        try {
            final String directory = Files.readString(Paths.get(System.getProperty("user.home") + "/databases/achievements.json"));
            JsonObject json = JsonParser.parseString(directory).getAsJsonObject();
            Map<String, JsonElement> map = json.asMap();
            Type listType = new TypeToken<LinkedList<Integer>>() {
            }.getType();
            for (String s : map.keySet()) {
                JsonObject j = map.get(s).getAsJsonObject();
                AchieveTemplate a = new AchieveTemplate(j.get("id").getAsString(), new Gson().fromJson(j.get("stages"), listType), j.get("reward_money").getAsInt(), j.get("reward_xp").getAsInt(), Component.translatable(j.get("name").getAsString()));
                templates.add(a);
            }
        }catch(IOException e){
            Bukkit.getLogger().severe("[Lobby_plugin] Couldn't get achievements from json continuing without.");
        }
    }

    public static void createNewAchievements(Player p){
        for(AchieveTemplate temp : templates){
            LobbyDatabase.addAchievement(p, new Achievement(p, temp, 0, false, false));
        }
    }

    public static ArrayList<Achievement> getAchievements(Player p){
        ArrayList<Achievement> achieve = new ArrayList<>();
        for(Achievement a : achievements){
            if (a.player.equals(p)) {
                achieve.add(a);
            }
        }
        return achieve;
    }

    public static void removeAchievements(Player p){
        for(Achievement a : getAchievements(p)){
            achievements.remove(a);
        }
    }

    public static void getFromDatabase(Player p){
        ArrayList<Achievement> achieve = LobbyDatabase.getAchievements(p);
        ArrayList<AchieveTemplate> a = (ArrayList<AchieveTemplate>) templates.clone();
        if(achieve == null){
            return;
        }
        if(achieve.size() == templates.size()){
            achievements.addAll(achieve);
            return;
        }

        for(AchieveTemplate t : templates){
            for(Achievement ach : achieve){
                String id = ach.temp.id;
                if(id.equals(t.id)){
                    a.remove(t);
                }
            }
        }

        for(AchieveTemplate t : a){
            LobbyDatabase.addAchievement(p, new Achievement(p, t, 0, false, false));
        }
        achievements.addAll(LobbyDatabase.getAchievements(p));
    }

    public static Achievement identifyAchievement(Player p, ItemStack i){
        for(Achievement a : getAchievements(p)){
            if(a.claimed) continue;
            if(a.build().equals(i)){
                return a;
            }
        }
        return null;
    }

    @Override
    public ItemStack build(){
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(temp.name.color(GOLD).append(Component.text(" " + "|".repeat(1 + stage))));
        ArrayList<Component> lore = new ArrayList<>();
        lore.add(name().color(WHITE));
        if(done){
            lore.add(Component.translatable("Achievement completed").color(GREEN).decoration(ITALIC, false));
            lore.add(Component.translatable("Click to claim reward").color(GREEN).decoration(ITALIC, false));
        }else {
            lore.add(Component.text(getProgress() + "/" + amount).color(WHITE).decoration(ITALIC, false));
        }

        //TODO change model when achievement is claimed
        lore.add(Component.text("Reward: " + getMoney() + "[m]   " + getXp() + "xp").color(WHITE).decoration(ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        if(done) item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(2).build());
        return item;
    }

    @Override
    public void claim(){
        LevelManager.giveExperience(player, getXp());
        LevelManager.giveMoney(player, getMoney());
        stage++;
        if(stage < temp.stages.size()) {
            LobbyDatabase.progressStage(player, this);
            questNumber = temp.id + temp.stages.get(stage);
            amount = temp.stages.get(stage);
            done = false;
        }else{
            claimed = true;
        }
        LobbyDatabase.updateAchievementDone(player, this);
        LobbyDatabase.updateAchievementClaimed(player, this);
        for(Achievement a : getAchievements(player)){
            if(a.done && !a.claimed) return;
        }

        App.Achieve.deactivateApps(player);
    }

    @Override
    public void complete(){
        done = true;
        LobbyDatabase.updateAchievementDone(player, this);
    }

    @Override
    public int getProgress(){
        try(Connection conn = DriverManager.getConnection(game.URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT SUM(" + category.columnName + ") AS " + category.columnName + " FROM " + game.playerTableName + " INNER JOIN " + game.tableName + " ON " + game.playerTableName + ".game=" + game.tableName + ".game_id WHERE player_uuid = ?;");
            if(!forSeveral){
                prep = conn.prepareStatement("SELECT MAX(" + category.columnName + ") AS " + category.columnName + " FROM " + game.playerTableName + " INNER JOIN " + game.tableName + " ON " + game.playerTableName + ".game=" + game.tableName + ".game_id WHERE player_uuid = ?;");
            }
            if(questNumber.contains("-")){
                prep = conn.prepareStatement("SELECT COUNT(game) AS " + category.columnName + " FROM " + game.playerTableName + " WHERE player_uuid = ?;");
            }
            prep.setBytes(1, uuid_to_bytes(player));
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt(category.columnName);
        }catch(SQLException e){
            //Bukkit.getLogger().warning(e.getMessage());
            //Bukkit.getLogger().warning("couldn't get progress");
        }
        return 0;
    }

    public static void checkAndComplete(Player p){
        ArrayList<Achievement> a = getAchievements(p);
        for(Achievement ach : a){
            if(ach.done) continue;
            int progress = ach.getProgress();
            if(progress >= ach.amount){
                ach.complete();
                App.Achieve.activateApps(p);
            }
        }
    }

    public int getMoney(){
        return (int) Math.round(temp.reward_money * Math.pow(1.1, stage));
    }

    public int getXp(){
        return (int) Math.round(temp.reward_xp * Math.pow(1.1, stage));
    }

    public static void setAchievements(Inventory inv, Player p){
        int[] border = {8, 17, 26, 35, 44, 53};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int slot = 29;
        int line = 3;
        for(Achievement a : getAchievements(p)){
            if(slot >= border[line]){
                line++;
                if(line >= nextLine.length) return; 
                slot = nextLine[line];
            }
            if(a.build() == null){
                slot++;
                continue;
            }
            inv.setItem(slot, a.build());
            slot++;
        }
    }
}

class AchieveTemplate{
    String id;
    List<Integer> stages;
    int reward_money;
    int reward_xp;
    Component name;

    public AchieveTemplate(String id, List<Integer> stages, int reward_money, int reward_xp, Component name) {
        this.id = id;
        this.stages = stages;
        this.reward_money = reward_money;
        this.reward_xp = reward_xp;
        this.name = name;
    }

    public static AchieveTemplate getAchieveTemplate(String id){
        for(AchieveTemplate t : Achievement.templates){
            if(t.id.equals(id.trim())){
                return t;
            }
        }
        return null;
    }
}
