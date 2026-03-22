package gg.crystalized.lobby;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static gg.crystalized.lobby.LobbyDatabase.uuid_to_bytes;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class Quest {
    static ArrayList<Quest> allQuests = new ArrayList<>();
    String questNumber;
    Player player;
    Game game;
    boolean forSeveral;
    Category category;
    int amount;
    Difficulty difficulty;
    boolean claimed;
    boolean done;
    public Quest(Player p, Game game, boolean forSeveral, Category category,int amount){
        this.player = p;
        this.game = game;
        this.forSeveral = forSeveral;
        this.category = category;
        this.amount = amount;
        this.difficulty = category != null ? Difficulty.getDifficulty(category.min, category.max, amount, category.baseDiff) : Difficulty.HARD;
        this.claimed = false;
        this.done = false;

        questNumber = "-1";
        if(game == null){
            return;
        }

        String number = "" + game.ordinal();
        number = number + (forSeveral ? 1 : 0);
        for (int i = 0; i < Category.getCategories(game).size(); i++) {
            if (Category.getCategories(game).get(i) == category) {
                number = number + i;
                number = number + amount;
                break;
            }
        }

        questNumber = number;
    }

    public Quest(Player p, String questNumber, boolean claimed, boolean done){
        this.questNumber = questNumber;
        this.player = p;
        this.claimed = claimed;
        this.done = done;

        if(Objects.equals(questNumber, "-1")){
            this.difficulty = Difficulty.HARD;
            this.amount = 6;
        }else {
            this.game = Game.values()[Integer.parseInt(Character.toString(questNumber.charAt(0)))];
            this.forSeveral = Integer.parseInt(Character.toString(questNumber.charAt(1))) == 1;
            this.category = Category.getCategories(game).get(Integer.parseInt(Character.toString(questNumber.charAt(2))));
            this.amount = Integer.parseInt(questNumber.substring(3));
            this.difficulty = Difficulty.getDifficulty(category.min, category.max, amount, category.baseDiff);
        }
    }

    public static Quest[] rollQuests(Player p){
        removeQuests(p);
        Quest[] quests = new Quest[7];
        ArrayList<Category> alreadyRolled = new ArrayList<>();
        for(int i = 0; i < quests.length -1; i++) {
            Game game = Game.values()[(int) Math.floor(Math.random() * (Game.values().length))];
            boolean forSeveral = Math.floor(Math.random() * 2) == 1;
            int c = (int) Math.floor(Math.random() * (Category.getCategories(game).size()-1));
            Category category = Category.getCategories(game).get(c);
            int amount = (int) Math.floor(Math.random() * (category.max - category.min + 1) + category.min);

            while (alreadyRolled.contains(category) || (!forSeveral && !category.forOneGame)) {
                game = Game.values()[(int) Math.floor(Math.random() * (Game.values().length))];
                forSeveral = Math.floor(Math.random() * 2) == 1;
                c = (int) Math.floor(Math.random() * Category.getCategories(game).size());
                category = Category.getCategories(game).get(c);
                amount = (int) Math.floor(Math.random() * (category.max - category.min + 1) + category.min);
            }

            alreadyRolled.add(category);
            if(forSeveral){
                amount = amount * 3;
            }
            Quest quest = new Quest(p, game, forSeveral, category, amount);
            quests[i] = quest;
        }
        quests[6] = new Quest(p, null, false, null, 6);
        allQuests.addAll(Arrays.asList(quests));
        return quests;
    }

    public void rerollQuest(){
        Game game = Game.values()[(int) Math.floor(Math.random() * (Game.values().length))];
        boolean forSeveral = Math.floor(Math.random() * 2) == 1;
        int c = (int) Math.floor(Math.random() * (Category.getCategories(game).size()-1));
        Category category = Category.getCategories(game).get(c);
        int amount = (int) Math.floor(Math.random() * (category.max - category.min + 1) + category.min);

        while (this.category == category || (!forSeveral && !category.forOneGame) || this.difficulty != Difficulty.getDifficulty(category.min, category.max, amount, category.baseDiff)) {
            game = Game.values()[(int) Math.floor(Math.random() * (Game.values().length))];
            forSeveral = Math.floor(Math.random() * 2) == 1;
            c = (int) Math.floor(Math.random() * Category.getCategories(game).size());
            category = Category.getCategories(game).get(c);
            amount = (int) Math.floor(Math.random() * (category.max - category.min + 1) + category.min);
        }

        Quest quest = new Quest(player, game, forSeveral, category, amount);
        allQuests.remove(this);
        allQuests.add(quest);
        LobbyDatabase.replaceQuest(player, this, quest);
        LobbyDatabase.rerollReduce(player); 
    }

    public static void removeQuests(Player p){
        for(int i = 0; i < allQuests.size(); i++){
            if(allQuests.get(i).player.equals(p)){
                allQuests.remove(i);
                i--;
            }
        }
    }

    public static ArrayList<Quest> getQuests(Player p){
        ArrayList<Quest> quests = new ArrayList<>();
        for(Quest q : allQuests){
            if(q.player.equals(p)){
                quests.add(q);
            }
        }
        return quests;
    }

    public void claim(){
        LobbyDatabase.questClaimed(player, questNumber);
        claimed = true;
        LevelManager.giveExperience(player, difficulty.exp);
        LevelManager.giveMoney(player, difficulty.money);

        for(Quest q : getQuests(player)){
            if(q.done && !q.claimed) return;
        }

        App.Quest.deactivateApps(player);
    }

    public void complete(){
        LobbyDatabase.questCompleted(player, questNumber);
        done = true;
    }

    public ItemStack build(){
        if(claimed){
            return null;
        }
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name());
        ArrayList<Component> lore = new ArrayList<>();
        if(done){
            lore.add(Component.translatable("Quest completed").color(GREEN).decoration(ITALIC, false));
            lore.add(Component.translatable("Click to claim").color(GREEN).decoration(ITALIC, false));
        }else {
            lore.add(Component.text(getProgress() + "/" + amount).color(WHITE).decoration(ITALIC, false));
        }
        if(LobbyDatabase.canRerollQuest(this)){
            lore.add(Component.text("Click to reroll Quest").color(WHITE).decoration(ITALIC, false));
        }
        lore.add(Component.text("Reward: " + difficulty.money + "[m]   " + difficulty.exp + "xp").color(WHITE).decoration(ITALIC, false));
        lore.add(Component.text("Difficulty: " + difficulty).color(GRAY).decoration(ITALIC, false));
        meta.lore(lore);
        meta.setItemModel(new NamespacedKey("crystalized", difficulty.model));
        item.setItemMeta(meta);
        if(done) item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(2).build());
        return item;
    }

    public Component name(){
        if(Objects.equals(questNumber, "-1")){
            return Component.translatable("Complete all weekly quests").color(difficulty.color).decoration(ITALIC, false);
        }
        List<Component> args = new ArrayList<>();
        args.add(Component.text("" + amount));
        Component c = Component.translatable(category.translationKey, args);
        if(!forSeveral){
            c = c.append(Component.translatable("one game of "));
        }
        c = c.append(Component.text(game.name));
        c = c.color(difficulty.color).decoration(ITALIC, false);
        return c;
    }

    public int getProgress(){
        if(Objects.equals(questNumber, "-1")){
            int num = 0;
            for(Quest q : getQuests(player)){
                if(q.done) num++;
            }
            return num;
        }
        try(Connection conn = DriverManager.getConnection(game.URL)){
            PreparedStatement prep = conn.prepareStatement("SELECT SUM(" + category.columnName + ") AS " + category.columnName + " FROM " + game.playerTableName + " INNER JOIN " + game.tableName + " ON " + game.playerTableName + ".game=" + game.tableName + ".game_id WHERE timestamp > ? AND player_uuid = ?;");
            if(!forSeveral){
                prep = conn.prepareStatement("SELECT MAX(" + category.columnName + ") AS " + category.columnName + " FROM " + game.playerTableName + " INNER JOIN " + game.tableName + " ON " + game.playerTableName + ".game=" + game.tableName + ".game_id WHERE timestamp > ? AND player_uuid = ?;");
            }
            prep.setInt(1, LobbyDatabase.getLastQuestRoll(player));
            prep.setBytes(2, uuid_to_bytes(player));
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
        ArrayList<Quest> quests = getQuests(p);
        for(Quest q : quests){
            if(q.done) continue;
            int progress = q.getProgress();
            if(progress >= q.amount){
                q.complete();
                App.Quest.activateApps(p);
            }
        }
    }

    public static Quest identifyQuest(Player p, ItemStack i){
        for(Quest q : getQuests(p)){
            if(q.claimed) continue;
            if(q.build().equals(i)){
                return q;
            }
        }
        return null;
    }

    public static void setQuests(Inventory inv, Player p){
        int[] border = {8, 17, 26, 35, 44, 53};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int slot = 29;
        int line = 3;
        for(Quest q : getQuests(p)){
            if(slot >= border[line]){
                line++;
                slot = nextLine[line];
            }
            if(Objects.equals(q.questNumber, "-1")){
                inv.setItem(4, q.build());
                continue;
            }
            if(q.claimed || q.build() == null){
                slot = slot + 2;
                continue;
            }
            inv.setItem(slot, q.build());
            slot = slot + 2;
        }
    }

    public enum Game{
        ls("LsGamesPlayers", "LiteStrikeGames","jdbc:sqlite:" + System.getProperty("user.home") + "/databases/litestrike_db.sql", "Litestrike"),
        ko("KoGamesPlayers", "KnockoffGames","jdbc:sqlite:" + System.getProperty("user.home") + "/databases/knockoff_db.sql", "Knockoff"),
        cb("CbGamesPlayers", "CrystalBlitzGames","jdbc:sqlite:" + System.getProperty("user.home") + "/databases/crystalblitz_db.sql", "Crystal Blitz");
        final String playerTableName;
        final String tableName;
        final String URL;
        final String name;
        Game(String playerTableName, String tableName,String URL, String name){
            this.playerTableName = playerTableName;
            this.tableName = tableName;
            this.URL = URL;
            this.name = name;
        }
    }

    public enum Category{
        //IMPORTANT: the order of the categories mustn't change
        ls_was_winner("was_winner", Game.ls, 1, 10, false, Difficulty.MEDIUM, "Win %1$s games of "),
        bombs_placed("placed_bombs", Game.ls, 1,3 , true, Difficulty.EASY , "Place %1$s bombs in "),
        bombs_broken("broken_bombs", Game.ls, 1, 3, true, Difficulty.EASY, "Break %1$s bombs in "),
        ls_kills("kills", Game.ls, 1, 7, true, Difficulty.EASY, "Kill %1$s players in "),
        ls_assists("assists", Game.ls, 1, 5, true, Difficulty.EASY, "Assist with killing %1$s players in "),
        ls_hits_dealt("hits_dealt", Game.ls, 20, 45, true, Difficulty.EASY, "Deal %1$s hits in "),
        ls_damage_dealt("damage_dealt", Game.ls, 30, 100, true, Difficulty.EASY, "Deal %1$s points of damage in "),
        ko_games_won("games_won", Game.ko, 1, 10, false, Difficulty.MEDIUM, "Win %1$s games of "),
        ko_kills("kills", Game.ko, 5, 10, true, Difficulty.EASY, "Kill %1$s players in "),
        ko_items_used("items_used", Game.ko, 1, 5, true, Difficulty.EASY, "Use %1$s items in "),
        ko_blocks_placed("blocks_placed", Game.ko, 50, 150, true, Difficulty.EASY, "Place %1$s blocks in "),
        ko_blocks_broken("blocks_broken", Game.ko, 20, 50, true, Difficulty.EASY, "Break %1$s blocks in "),
        cb_games_won("games_won", Game.cb, 1, 10, false, Difficulty.MEDIUM, "Win %1$s games of "),
        cb_kills("kills", Game.cb, 1, 10, true, Difficulty.EASY, "Kill %1$s player in "),
        nexus_kills("nexus_kills", Game.cb, 1, 5, true, Difficulty.MEDIUM, "Destroy %1$s Nexus shards in ");

        final String columnName;
        final Game game;
        final int min;
        final int max;
        final boolean forOneGame;
        final Difficulty baseDiff;
        final String translationKey;
        Category(String columnName, Game game, int min, int max, boolean forOneGame, Difficulty baseDiff, String translationKey){
            this.columnName = columnName;
            this.game = game;
            this.min = min;
            this.max = max;
            this.forOneGame = forOneGame;
            this.baseDiff = baseDiff;
            this.translationKey = translationKey;
        }

        public static ArrayList<Category> getCategories(Game game){
            ArrayList<Category> list = new ArrayList<>();
            for(Category c : Category.values()){
                if(c.game == game){
                    list.add(c);
                }
            }
            return list;
        }
    }
    enum Difficulty{
        EASY(10, 5, "ui/scn3/quests/quest_easy", DARK_GREEN),
        MEDIUM(30, 10, "ui/scn3/quests/quest_medium",YELLOW),
        HARD(50, 20, "ui/scn3/quests/quest_hard",RED),
        EXPERT(80, 30, "ui/scn3/quests/quest_expert", DARK_RED);
        final int money;
        final int exp;
        final String model;
        final NamedTextColor color;
        Difficulty(int money, int exp, String model, NamedTextColor color){
            this.money = money;
            this.exp = exp;
            this.model = model;
            this.color = color;
        }
        public static Difficulty getDifficulty(int min, int max, int value, Difficulty baseDiff){
            double q2 = (double) (min + max) /2;
            double q1 = (min + q2) /2;
            double q3 = (max + q2) /2;

            if((value >= min && value < q1) || baseDiff == EXPERT){
                return baseDiff;
            }

            if((value >= q1 && value < q2) || baseDiff == HARD){
                return Difficulty.values()[baseDiff.ordinal() +1];
            }

            if((value >= q2 && value < q3) || baseDiff == MEDIUM){
                return Difficulty.values()[baseDiff.ordinal() +2];
            }

            return Difficulty.values()[baseDiff.ordinal() +3];
        }
    }
}


