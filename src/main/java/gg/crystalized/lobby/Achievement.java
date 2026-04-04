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
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
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

    public static void getAchievementsFromJson(InputStream stream){
        JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        Map<String, JsonElement> map = json.asMap();
        Type listType = new TypeToken<LinkedList<Integer>>() {}.getType();
        for (String s : map.keySet()) {
            JsonObject j = map.get(s).getAsJsonObject();
            AchieveTemplate a = new AchieveTemplate(j.get("id").getAsString(), new Gson().fromJson(j.get("stages"), listType), j.get("reward_money").getAsInt(), j.get("reward_xp").getAsInt(), Component.translatable(j.get("name").getAsString()));
            templates.add(a);
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
        if(a.size() == templates.size()){
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
        meta.displayName(temp.name.color(GOLD));
        ArrayList<Component> lore = new ArrayList<>();
        lore.add(name().color(WHITE));
        if(done){
            lore.add(Component.translatable("Achievement completed").color(GREEN).decoration(ITALIC, false));
            lore.add(Component.translatable("Click to claim reward").color(GREEN).decoration(ITALIC, false));
        }else {
            lore.add(Component.text(getProgress() + "/" + amount).color(WHITE).decoration(ITALIC, false));
        }
        //TODO add getReward and getXp
        //TODO change model when achievement is claimed
        lore.add(Component.text("Reward: " + temp.reward_money + "[m]   " + temp.reward_xp + "xp").color(WHITE).decoration(ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        if(done) item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(2).build());
        return item;
    }

    @Override
    public void claim(){
        LevelManager.giveExperience(player, temp.reward_xp);
        LevelManager.giveMoney(player, temp.reward_money);
        stage++;
        if(stage < temp.stages.size()) {
            LobbyDatabase.progressStage(player, this);
            questNumber = temp.id + temp.stages.get(stage);
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
            Bukkit.getLogger().warning("template: " + t.id);
            Bukkit.getLogger().warning("id: " + id);
            if(t.id.equals(id.trim())){
                return t;
            }
        }
        return null;
    }
}
