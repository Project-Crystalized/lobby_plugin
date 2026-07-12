package gg.crystalized.lobby;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
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
    int stage; //subtracted by 1, stage 1 is 0, stage 2 is 1, so on.
    int progress; //percentage
    AchieveTemplate temp;

    public Achievement(OfflinePlayer player, AchieveTemplate temp, int progress, int stage, boolean done, boolean claimed){
        //super(player, temp.id + temp.stages.get(stage), done, claimed); //causes an exception
        super(player, "-1" , done, claimed);
        this.stage = stage;
        this.temp = temp;
        this.amount = 100; //max percentage
        this.difficulty = temp.difficulty; //dumb
        this.progress = progress;
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
                templates.add(new AchieveTemplate(j.get("databaseid").getAsString(), j.get("name").getAsString(), j.get("difficulty").getAsString(), achievementCategories.general, j));
            }
            for (JsonElement e : categories.get("litestrike").getAsJsonArray()) {
                JsonObject j = e.getAsJsonObject();
                templates.add(new AchieveTemplate(j.get("databaseid").getAsString(), j.get("name").getAsString(), j.get("difficulty").getAsString(), achievementCategories.ls, j));
            }
            for (JsonElement e : categories.get("knockoff").getAsJsonArray()) {
                JsonObject j = e.getAsJsonObject();
                templates.add(new AchieveTemplate(j.get("databaseid").getAsString(), j.get("name").getAsString(), j.get("difficulty").getAsString(), achievementCategories.ko, j));
            }
            for (JsonElement e : categories.get("crystalblitz").getAsJsonArray()) {
                JsonObject j = e.getAsJsonObject();
                templates.add(new AchieveTemplate(j.get("databaseid").getAsString(), j.get("name").getAsString(), j.get("difficulty").getAsString(), achievementCategories.cb, j));
            }

            Lobby_plugin.getInstance().getLogger().log(Level.INFO, "Loaded " + templates.size() + " achievements from json.");
        }catch(IOException e){
            Bukkit.getLogger().severe("[Lobby_plugin] Couldn't get achievements from json continuing without, Error;");
            e.printStackTrace();
        }
    }

    public static void createNewAchievements(Player p){
        for(AchieveTemplate temp : templates){
            LobbyDatabase.addAchievement(p, new Achievement(p, temp, 0, 1, false, false));
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

    //for plugins to use
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
            LobbyDatabase.addAchievement(p, new Achievement(p, t, 0, 1, false, false));
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

    public static void resyncInfo(OfflinePlayer p) {
        for (Achievement a : achievements) {
            if (a.player.equals(p)) {
                try(Connection conn = DriverManager.getConnection(LobbyDatabase.URL)) {
                    PreparedStatement prep = conn.prepareStatement("SELECT * FROM Achievements WHERE player_uuid = ?;");
                    prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));
                    ResultSet set = prep.executeQuery();
                    while (set.next()) {
                        if (set.getString("id").equals(a.temp.id)) {
                            int done = set.getInt("done");
                            a.done = done == 1;

                            int claimed = set.getInt("claimed");
                            a.claimed = claimed == 1;

                            a.stage = set.getInt("stage");
                            makeIconsBlink(p, a);
                        }
                    }
                } catch (SQLException ex) {
                    Lobby_plugin.getInstance().getLogger().warning(ex.toString());
                }
            }
        }
    }

    @Override
    public ItemStack build(){
        boolean showIcon = stage > 0 && !claimed;
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        //meta.displayName(temp.name.color(temp.difficulty.color));
        meta.displayName(temp.name.color(difficulty.color).append(Component.text(" | " + toRomanNumeral(stage))));
        if (showIcon && !done) {
            meta.setItemModel(new NamespacedKey("crystalized", "ui/scn3/achivements/" + temp.internalName));
        } else {
            meta.setItemModel(new NamespacedKey("crystalized", difficulty.lockedModel));
        }

        ArrayList<Component> lore = new ArrayList<>();
        lore.add(temp.description.color(WHITE));
        lore.add(Component.text("Difficulty: ").color(WHITE).append(Component.translatable(difficulty.name).color(difficulty.color)).decoration(ITALIC, false));
        lore.add(Component.empty());
        if (stage == temp.stages.getLast()) { //achieve fully done, claimed didn't work for this
            //TODO different tooltip style
            lore.add(Component.translatable("Achievement fully completed!").color(GOLD).decoration(ITALIC, false));
            lore.add(Component.translatable("Well done!").color(GOLD).decoration(ITALIC, false));
            lore.add(Component.empty());
        } else if (done) {
            lore.add(Component.translatable("Achievement completed").color(GREEN).decoration(ITALIC, false));
            lore.add(Component.translatable("Click to claim reward").color(GREEN).decoration(ITALIC, false));
            lore.add(Component.empty());
        }
        lore.add(Component.text("Progress: " + getProgress() + "/" + amount + "%").color(WHITE).decoration(ITALIC, false));
        lore.add(Component.text("Reward: " + getMoney() + "[m]   " + getXp() + "xp").color(WHITE).decoration(ITALIC, false));
        lore.add(Component.text("Stage: " + (stage + 1) + "/" + (temp.stages.size() + 1) ).color(WHITE).decoration(ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        if (done && !claimed) item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(1).build());
        return item;
    }

    //no access modifier to prevent other plugins calling this directly - Callum
    @Override
    void claim(){
        LevelManager.giveExperience(player.getPlayer(), getXp());
        LevelManager.giveMoney(player.getPlayer(), getMoney());
        if (stage != temp.stages.getLast() - 1) {
            stage++;
            LobbyDatabase.progressStage(player, this);
            done = false;
            //TODO placeholder sound
            player.getPlayer().playSound(player.getPlayer(), "minecraft:entity.experience_orb.pickup", 1, 1);
            amount = 100; //dumb shit
            setProgress(0);
        } else {
            //TODO placeholder sound, different than the other one
            player.getPlayer().playSound(player.getPlayer(), "minecraft:entity.player.levelup", 1, 1);
            stage = temp.stages.getLast();
            claimed = true;
        }
        App.Achieve.deactivateApps(player);
        deactivateIconsBlink(player, this);
        LobbyDatabase.updateAchievementDone(player, this);
        LobbyDatabase.updateAchievementClaimed(player, this);
        for(Achievement a : getAchievements(player)){
            if(a.done && !a.claimed) return;
        }
    }

    //no access modifier to prevent other plugins calling this directly - Callum
    @Override
    void complete(){
        done = true;
        LobbyDatabase.updateAchievementDone(player, this);
        showNotif();
    }

    private void showNotif() {
        Player p = Bukkit.getPlayer(player.getName());
        NamespacedKey tempkey = new NamespacedKey("crystalized", "preperaingachievement_" + p.getUniqueId().toString().toLowerCase() + "_" + temp.id);
        if (Bukkit.getServer().getAdvancement(tempkey) != null) {return;}

        //send chat message
        p.sendRichMessage("");
        p.sendMessage(Component.translatable("crystalized.achievement.chat", List.of(
                temp.name.append(Component.text(" " + toRomanNumeral(stage))).color(WHITE).
                        hoverEvent(HoverEvent.showText(temp.description.color(WHITE)))
        )).color(GOLD));

        //sound
        if (difficulty.equals(Difficulty.EXPERT)) {
            p.playSound(p, "crystalized:effect.achievement_obtain_expert", 0.25F, 1);
        } else {
            p.playSound(p, "crystalized:effect.achievement_obtain", 1, 1);
        }

        //weird shit to send toast messages
        try {
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
        progress = percentage;

        //save to database
        try(Connection conn = DriverManager.getConnection(LobbyDatabase.URL)) {
            PreparedStatement prep = conn.prepareStatement("UPDATE Achievements SET progress = ? WHERE player_uuid = ? AND id = ?;");
            prep.setInt(1, progress);
            prep.setBytes(2, LobbyDatabase.uuid_to_bytes(player));
            prep.setString(3, temp.id);
            prep.executeUpdate();
            //Bukkit.getServer().sendRichMessage("Saved Achievement progress for " + temp.internalName + " | " + percentage);
        } catch (SQLException ex) {
            Lobby_plugin.getInstance().getLogger().warning(ex.toString());
        }

        checkAndComplete(player.getPlayer());
    }

    //for plugins to use
    public void addProgress(int percentageToAdd) {
        setProgress(getProgress() + percentageToAdd);
    }

    @Override
    public int getProgress(){
        try(Connection conn = DriverManager.getConnection(LobbyDatabase.URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM Achievements WHERE player_uuid = ?;");
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(player));
            ResultSet set = prep.executeQuery();
            while (set.next()) {
                if (set.getString("id").equals(temp.id)) {
                    progress = set.getInt("progress");
                    break;
                }
            }
        } catch (SQLException ex) {
            Lobby_plugin.getInstance().getLogger().warning(ex.toString());
            progress = 0;
        }
        return progress;
    }

    public static void checkAndComplete(Player p){
        ArrayList<Achievement> a = getAchievements(p);
        for(Achievement ach : a){
            if(ach.done) continue;
            int progress = ach.getProgress();
            //int progress = ach.progress;
            if(progress >= ach.amount){
                ach.complete();
                makeIconsBlink(p, ach);
            }
        }
    }

    private static void makeIconsBlink(OfflinePlayer p, Achievement ach) {
        //TODO this works, but deactivateIconsBlink doesn't work when claiming, disabling for now to stop confusion - Callum
        /*if (ach.done && !ach.claimed) {
            switch (ach.temp.category) {
                case general -> {App.AchieveGeneralCategory.activateApps(p);}
                case ls -> {App.AchieveLsCategory.activateApps(p);}
                case ko -> {App.AchieveKoCategory.activateApps(p);}
                case cb -> {App.AchieveCbCategory.activateApps(p);}
            }
            App.Achieve.activateApps(p);
        }*/
    }

    private static void deactivateIconsBlink(OfflinePlayer p, Achievement ach) {
        /*if (ach.done && !ach.claimed) {
            switch (ach.temp.category) {
                case general -> {App.AchieveGeneralCategory.deactivateApps(p);}
                case ls -> {App.AchieveLsCategory.deactivateApps(p);}
                case ko -> {App.AchieveKoCategory.deactivateApps(p);}
                case cb -> {App.AchieveCbCategory.deactivateApps(p);}
            }
            App.Achieve.deactivateApps(p);
        }*/
    }

    public int getMoney(){
        //return (int) Math.round(temp.reward_money * Math.pow(1.1, stage));
        return (int) Math.round(temp.reward_money * Math.pow(2.2, stage));
    }

    public int getXp(){
        //return (int) Math.round(temp.reward_xp * Math.pow(1.1, stage));
        return (int) Math.round(temp.reward_xp * Math.pow(2.2, stage));
    }

    private String toRomanNumeral(int in) {
        int i = in + 1; //shifted up by 1, because idk how to properly get stages to start at 1
        switch (i) {
            case 0 -> {return "0";}
            case 1 -> {return "I";}
            case 2 -> {return "II";}
            case 3 -> {return "III";}
            case 4 -> {return "IV";}
            case 5 -> {return "V";}
            case 6 -> {return "VI";}
            case 7 -> {return "VII";}
            case 8 -> {return "VIII";}
            case 9 -> {return "IX";}
            case 10 -> {return "X";}
            default -> {return "?";}
        }
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

    public AchieveTemplate(String id, String name, String difficulty, Achievement.achievementCategories category, JsonObject json) {
        this.internalName = name;
        this.id = id;
        this.difficulty = Quest.Difficulty.valueOf(difficulty);
        if (this.difficulty.equals(Quest.Difficulty.EXPERT)) {
            this.stages = List.of(1, 2, 3, 4); //doing an expert achievement 10 times is way too painful
        } else {
            this.stages = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        }
        this.reward_money = this.difficulty.money;
        this.reward_xp = this.difficulty.exp;
        this.name = Component.translatable("crystalized.achievement." + name + ".name").decoration(ITALIC, false);
        this.description = Component.translatable("crystalized.achievement." + name + ".desc").decoration(ITALIC, false);
        this.category = category;

        if (json.has("replaceStages")) {
            List<JsonElement> list = json.get("replaceStages").getAsJsonArray().asList();
            this.stages = new ArrayList<>();
            for (JsonElement e : list) {
                this.stages.add(e.getAsInt());
            }
        }
        if (json.has("replaceRewardMoney")) {
            this.reward_money = json.get("replaceRewardMoney").getAsInt();
        }
        if (json.has("replaceRewardXP")) {
            this.reward_xp = json.get("replaceRewardXP").getAsInt();
        }
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