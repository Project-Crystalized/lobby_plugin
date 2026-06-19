package gg.crystalized.lobby;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.codehaus.plexus.util.IOUtil;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class Achievement extends Quest{
    static ArrayList<AchieveTemplate> templates = new ArrayList<>();
    static ArrayList<Achievement> achievements = new ArrayList<>();
    int stage;
    AchieveTemplate temp;

    public Achievement(OfflinePlayer player, AchieveTemplate temp, int stage, boolean done, boolean claimed){
        //super(player, temp.id + temp.stages.get(stage), done, claimed); //causes an exception
        super(player, "-1" , done, claimed);
        this.stage = stage;
        this.temp = temp;
        this.amount = 100; //percentage
        this.difficulty = temp.difficulty; //dumb
    }

    public enum achievementCategories{
        general("general"),
        ls("litestrike"),
        ko("knockoff"),
        cb("crystalblitz"),
        //br("battleroyale")
        ;

        String jsonname; //achievements.json
        achievementCategories(String jsonname) {
            this.jsonname = jsonname;
        }
    }

    public static void getAchievementsFromJson(){
        try {
            String string = IOUtil.toString(Lobby_plugin.getInstance().getResource("achievements.json"));
            JsonObject json = JsonParser.parseString(string).getAsJsonObject();
            JsonObject categories = json.get("achievements").getAsJsonObject();

            //probably a better way of doing this, JsonObject doesn't work with for loops so this is the next best rn - Callum
            //These are split just to make the json look nice
            for (JsonElement e : categories.get("general").getAsJsonArray()) {
                JsonObject j = e.getAsJsonObject();
                templates.add(new AchieveTemplate(j.get("databaseid").getAsString(), j.get("name").getAsString(), j.get("difficulty").getAsString(), achievementCategories.general));
            }
            for (JsonElement e : categories.get("litestrike").getAsJsonArray()) {
                JsonObject j = e.getAsJsonObject();
                templates.add(new AchieveTemplate(j.get("databaseid").getAsString(), j.get("name").getAsString(), j.get("difficulty").getAsString(), achievementCategories.ls));
            }
            for (JsonElement e : categories.get("knockoff").getAsJsonArray()) {
                JsonObject j = e.getAsJsonObject();
                templates.add(new AchieveTemplate(j.get("databaseid").getAsString(), j.get("name").getAsString(), j.get("difficulty").getAsString(), achievementCategories.ko));
            }
            for (JsonElement e : categories.get("crystalblitz").getAsJsonArray()) {
                JsonObject j = e.getAsJsonObject();
                templates.add(new AchieveTemplate(j.get("databaseid").getAsString(), j.get("name").getAsString(), j.get("difficulty").getAsString(), achievementCategories.cb));
            }

            Lobby_plugin.getInstance().getLogger().log(Level.INFO, "Loaded " + templates.size() + " achievements from json.");
        }catch(IOException e){
            Bukkit.getLogger().severe("[Lobby_plugin] Couldn't get achievements from json continuing without, Error;");
            e.printStackTrace();
        }
    }

    public static void createNewAchievements(Player p){
        for(AchieveTemplate temp : templates){
            LobbyDatabase.addAchievement(p, new Achievement(p, temp, 0, false, false));
        }
    }

    //can confuse devs for other plugins, making this a private method
    private static ArrayList<Achievement> getAchievements(OfflinePlayer p){
        getFromDatabase(p);
        ArrayList<Achievement> achieve = new ArrayList<>();
        for(Achievement a : achievements){
            if (a.player.equals(p)) {
                achieve.add(a);
            }
        }

        return achieve;
    }

    //for api
    public static Achievement getAchievement(String internalName, OfflinePlayer p) {
        for (Achievement a : getAchievements(p)) {
            if (a.temp.internalName.equals(internalName)) {
                return a;
            }
        }
        return null;
    }

    public static void getFromDatabase(OfflinePlayer p){
        if(dontGetAchieve(p)) return;
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

    public static boolean dontGetAchieve(OfflinePlayer p){
        for(Achievement a : achievements){
            if(a.player.getUniqueId().equals(p.getUniqueId())){
                return true;
            }
        }
        return false;
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
        meta.displayName(temp.name.color(temp.difficulty.color));
        if (claimed) {
            meta.setItemModel(new NamespacedKey("crystalized", "ui/scn3/achivements/" + temp.internalName));
        } else {
            meta.setItemModel(new NamespacedKey("crystalized", difficulty.lockedModel));
        }

        ArrayList<Component> lore = new ArrayList<>();
        lore.add(temp.description.color(WHITE));
        lore.add(Component.empty());
        if(done){
            lore.add(Component.translatable("Achievement completed").color(GREEN).decoration(ITALIC, false));
            lore.add(Component.translatable("Click to claim reward").color(GREEN).decoration(ITALIC, false));
        }else {
            lore.add(Component.text("Progress: " + getProgress() + "/" + amount + "%").color(WHITE).decoration(ITALIC, false));
        }
        lore.add(Component.text("Reward: " + getMoney() + "[m]   " + getXp() + "xp").color(WHITE).decoration(ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        if(done && !claimed) item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(1).build());
        return item;
    }

    //no access modifier to prevent other plugins calling this directly - Callum
    @Override
    void claim(){
        LevelManager.giveExperience(player.getPlayer(), getXp());
        LevelManager.giveMoney(player.getPlayer(), getMoney());
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

    //no access modifier to prevent other plugins calling this directly - Callum
    @Override
    void complete(){
        done = true;
        LobbyDatabase.updateAchievementDone(player, this);
        showNotif();
    }

    private void showNotif() {
        Player p = player.getPlayer();
        if (p == null) {
            return;
        }
        //send chat message
        p.sendRichMessage("");
        p.sendMessage(Component.translatable("crystalized.achievement.chat", List.of(temp.name)).color(GOLD));
        p.sendRichMessage("");

        //weird shit to send toast messages
        try {
            NamespacedKey tempkey = new NamespacedKey("crystalized", "preperaingachievement_" + p.getUniqueId().toString().toLowerCase() + "_" + temp.id);

            // The String in this method is the json format for advancements in datapacks, if vanilla changes this in future updates, expect this to fuck up
            // This is the only way we can load advancements without using a library, NMS or a datapacks, yes this is stupid and looks stupid - Callum
            // https://minecraft.wiki/w/Advancement_definition#File_format
            Advancement a = Bukkit.getUnsafe().loadAdvancement(tempkey, """
                    {
                    	"display": {
                    		"icon":{"id":"coal", "components": {"minecraft:item_model":"crystalized:ui/scn3/achivements/""" + temp.internalName + "\"}}," + """
                    		"title": {"translate": \"crystalized.achievement.""" + temp.internalName + ".name\"}, \"description\": {\"translate\": \"crystalized.achievement." + temp.internalName + ".desc\"}," + """
                    		"announce_to_chat": false,
                    		"show_toast": true,
                    		"hidden": false
                    	},
                    	"criteria": {"thing": {"trigger":"minecraft:impossible"}}
                    }
            """
            );
            p.getAdvancementProgress(a).awardCriteria("thing");

            //we need to delay this, otherwise the notification wont send
            new BukkitRunnable() {
                public void run() {
                    p.getAdvancementProgress(a).revokeCriteria("thing");
                    Bukkit.getUnsafe().removeAdvancement(tempkey);
                    Bukkit.getServer().reloadData(); //for the above to work
                    cancel();
                }
            }.runTaskTimer(Lobby_plugin.getInstance(), 2, 1);

        } catch (Exception ex) {
            Lobby_plugin.getInstance().getLogger().warning("Could not send Advancement Toast, is the lobby plugin up to date?");
            ex.printStackTrace();
        }
    }

    //for plugins to use
    public void setProgress(int percentage) {
        //TODO this method and save to achievement database

        //sanity check for afterwords, keep this in incase plugins are setting this to 100 (immediately or overtime) - Callum
        checkAndComplete(player.getPlayer());
    }

    //for plugins to use
    public void addProgress(int percentageToAdd) {
        setProgress(getProgress() + percentageToAdd);
    }

    @Override
    public int getProgress(){
        //TODO rewrite this so it doesn't depend on game databases, instead getting progress from the achievement database own
        /*try(Connection conn = DriverManager.getConnection(game.URL)){
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
        }*/
        return 25; //placeholder for now
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

    public static void setAchievements(Inventory inv, OfflinePlayer p, achievementCategories category){
        int[] border = {7, 16, 25, 34, 43, 52};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int slot = 29;
        int line = 3;
        List<Achievement> temp = getAchievements(p);
        List<Achievement> list = new ArrayList<>();

        //filter out unrelated achievements to the category
        for (Achievement a : temp) {
            if (a.temp.category.equals(category)) {
                list.add(a);
            }
        }

        //send shit
        for(Achievement a : list){
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

    String internalName;
    String id;
    List<Integer> stages;
    int reward_money;
    int reward_xp;
    Component name;
    Component description;
    Achievement.achievementCategories category;
    Quest.Difficulty difficulty;

    public AchieveTemplate(String id, String name, String difficulty, Achievement.achievementCategories category) {
        this.internalName = name;
        this.id = id;
        //I honestly dont get this, but the old achievements json had this repeated lol - Callum
        this.stages = List.of(1, 50, 100, 200);
        this.difficulty = Quest.Difficulty.valueOf(difficulty);
        this.reward_money = this.difficulty.money;
        this.reward_xp = this.difficulty.exp;
        this.name = Component.translatable("crystalized.achievement." + name + ".name").decoration(ITALIC, false);
        this.description = Component.translatable("crystalized.achievement." + name + ".desc").decoration(ITALIC, false);
        this.category = category;
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