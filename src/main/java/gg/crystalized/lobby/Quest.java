package gg.crystalized.lobby;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static gg.crystalized.lobby.LobbyDatabase.uuid_to_bytes;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class Quest {
    static Map<UUID, List<Quest>> allQuests = new HashMap<>();
    String questNumber;
    OfflinePlayer player;
    Game game;
    boolean forSeveral;
    Category category;
    int amount;
    Difficulty difficulty;
    boolean claimed;
    boolean done;
    public Quest(OfflinePlayer p, Game game, boolean forSeveral, Category category,int amount){
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

    public Quest(OfflinePlayer p, String questNumber, boolean claimed, boolean done){
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
            if(questNumber.charAt(2) != '-'){
                this.category = Category.getCategories(game).get(Integer.parseInt(Character.toString(questNumber.charAt(2))));
            }else{
                this.category = Category.empty;
            }
            this.amount = Integer.parseInt(questNumber.substring(3));
            this.difficulty = Difficulty.getDifficulty(category.min, category.max, amount, category.baseDiff);
        }
    }

    public static Quest[] rollQuests(Player p){
        allQuests.remove(p.getUniqueId());
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
        allQuests.put(p.getUniqueId(), new ArrayList<>(Arrays.asList(quests)));
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
        allQuests.get(player.getUniqueId()).remove(this);
        allQuests.get(player.getUniqueId()).add(quest);
        LobbyDatabase.replaceQuest(player, this, quest);
        LobbyDatabase.rerollReduce(player); 
    }

    public static ArrayList<Quest> getQuests(OfflinePlayer p){
        List<Quest> quests = allQuests.get(p.getUniqueId());
        return quests == null ? new ArrayList<>() : new ArrayList<>(quests);
    }

    void claim(){
        LobbyDatabase.questClaimed(player, questNumber);
        claimed = true;
        LevelManager.giveExperience(player.getPlayer(), difficulty.exp);
        LevelManager.giveMoney(player.getPlayer(), difficulty.money);

        for(Quest q : getQuests(player)){
            if(q.done && !q.claimed) return;
        }

        App.Quest.deactivateApps(player);
    }

    public ItemStack build(int progress, boolean canReroll){
        if(claimed){
            return null;
        }
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name());
        ArrayList<Component> lore = new ArrayList<>();
        if(done){
            lore.add(Component.translatable("crystalized.shardcore.quests.completed").color(GREEN).decoration(ITALIC, false));
            lore.add(Component.translatable("crystalized.shardcore.quests.claim").color(GREEN).decoration(ITALIC, false));
        }else {
            lore.add(Component.text(progress + "/" + amount).color(WHITE).decoration(ITALIC, false));
        }
        if(canReroll){
            lore.add(Component.translatable("crystalized.shardcore.quests.reroll").color(WHITE).decoration(ITALIC, false));
        }
        lore.add(Component.translatable("crystalized.shardcore.quests.reward").append(Component.text(difficulty.money + "\ue15c   " + difficulty.exp + "xp")).color(WHITE).decoration(ITALIC, false));
        lore.add(Component.translatable("crystalized.shardcore.quests.difficulty").color(GRAY).decoration(ITALIC, false).append(Component.translatable(difficulty.name.toLowerCase()).color(GRAY).decoration(ITALIC, false)));
        meta.lore(lore);
        meta.setItemModel(new NamespacedKey("crystalized", difficulty.model));
        item.setItemMeta(meta);
        if(done) item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(2).build());
        return item;
    }

    public Component name(){
        if(Objects.equals(questNumber, "-1")){
            return Component.translatable("crystalized.shardcore.quests.complete_all").color(difficulty.color).decoration(ITALIC, false);
        }
        List<Component> args = new ArrayList<>();
        args.add(Component.text("" + amount));
        Component c;
        if(questNumber.contains("-")){
            c = Component.translatable("crystalized.shardcore.quests.play_amount", args);
        }else {
            c = Component.translatable(category.translationKey, args);
        }
        if(!forSeveral){
            c = c.append(Component.translatable("crystalized.shardcore.quests.one_game"));
        }
        c = c.append(Component.text(game.name));
        c = c.color(difficulty.color).decoration(ITALIC, false);
        return c;
    }

    public static HashMap<Quest, Integer> getProgresses(Player p){
        HashMap<Quest, Integer> progress = new HashMap<>();
        ArrayList<Quest> quests = getQuests(p);
        ArrayList<Quest> real = new ArrayList<>();
        for(Quest q : quests){
            if(Objects.equals(q.questNumber, "-1")){
                int num = 0;
                for(Quest x : quests){
                    if(x.done) num++;
                }
                progress.put(q, num);
            }else{
                real.add(q);
            }
        }
        if(real.isEmpty()){
            return progress;
        }
        int lastRoll = LobbyDatabase.getLastQuestRoll(p);
        HashMap<Game, ArrayList<Quest>> byGame = new HashMap<>();
        for(Quest q : real){
            byGame.computeIfAbsent(q.game, k -> new ArrayList<>()).add(q);
        }
        for(Map.Entry<Game, ArrayList<Quest>> e : byGame.entrySet()){
            Game g = e.getKey();
            ArrayList<Quest> group = e.getValue();
            try(Connection conn = DriverManager.getConnection(g.URL)){
                StringBuilder cols = new StringBuilder();
                for(int i = 0; i < group.size(); i++){
                    Quest q = group.get(i);
                    if(i > 0) cols.append(", ");
                    cols.append(q.forSeveral ? "SUM(" : "MAX(").append(q.category.columnName).append(") AS q").append(i);
                }
                String sql = "SELECT " + cols + " FROM " + g.playerTableName + " INNER JOIN " + g.tableName + " ON " + g.playerTableName + ".game=" + g.tableName + ".game_id WHERE timestamp > ? AND player_uuid = ?;";
                PreparedStatement prep = conn.prepareStatement(sql);
                prep.setInt(1, lastRoll);
                prep.setBytes(2, uuid_to_bytes(p));
                ResultSet set = prep.executeQuery();
                set.next();
                for(int i = 0; i < group.size(); i++){
                    progress.put(group.get(i), set.getInt("q" + i));
                }
            }catch(SQLException e2){
                for(Quest q : group) progress.put(q, 0);
            }
        }
        return progress;
    }

    public static void checkAndComplete(Player p){
        ArrayList<Quest> quests = getQuests(p);
        ArrayList<Quest> real = new ArrayList<>();
        Quest completeAll = null;
        for(Quest q : quests){
            if(q.done) continue;
            if(Objects.equals(q.questNumber, "-1")){
                completeAll = q;
            }else{
                real.add(q);
            }
        }

        if(!real.isEmpty()){
            HashMap<Quest, Integer> progress = getProgresses(p);

            for(Quest q : real){
                if(progress.get(q) >= q.amount){
                    LobbyDatabase.questCompleted(q.player, q.questNumber);
                    q.done = true;
                    App.Quest.activateApps(p);
                }
            }
        }

        if(completeAll != null){
            int num = 0;
            for(Quest q : quests){
                if(q.done) num++;
            }
            if(num >= completeAll.amount){
                LobbyDatabase.questCompleted(completeAll.player, completeAll.questNumber);
                completeAll.done = true;
                App.Quest.activateApps(p);
            }
        }
    }

    public static Quest identifyQuest(Player p, ItemStack i){
        HashMap<Quest, Integer> progress = getProgresses(p);
        boolean canReroll = LobbyDatabase.canRerollQuest(p);
        for(Quest q : getQuests(p)){
            if(q.claimed) continue;
            if(q.build(progress.get(q), canReroll && !Objects.equals(q.questNumber, "-1")).equals(i)){
                return q;
            }
        }
        return null;
    }

    public static void setQuests(Inventory inv, Player p){
        int[] border = {7, 16, 25, 34, 43, 52};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int slot = 29;
        int line = 3;
        HashMap<Quest, Integer> progress = getProgresses(p);
        boolean canReroll = LobbyDatabase.canRerollQuest(p);
        for(Quest q : getQuests(p)){
            if(Objects.equals(q.questNumber, "-1")){
                inv.setItem(4, q.build(progress.get(q), false));
                continue;
            }
            if(slot >= border[line]){
                line++;
                slot = nextLine[line];
            }
            if(q.claimed || q.build(progress.get(q), canReroll) == null){
                slot = slot + 2;
                continue;
            }
            inv.setItem(slot, q.build(progress.get(q), canReroll));
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
        empty("empty", null, 0, 0, false, Difficulty.EXPERT, ""),
        ls_was_winner("was_winner", Game.ls, 1, 10, false, Difficulty.MEDIUM, "crystalized.shardcore.quests.category.wins"),
        bombs_placed("placed_bombs", Game.ls, 1,3 , true, Difficulty.EASY , "crystalized.shardcore.quests.catagory.bombs_placed"),
        bombs_broken("broken_bombs", Game.ls, 1, 3, true, Difficulty.EASY, "crystalized.shardcore.quests.category.bombs_broken"),
        ls_kills("kills", Game.ls, 1, 7, true, Difficulty.EASY, "crystalized.shardcore.quests.category.kills"),
        ls_assists("assists", Game.ls, 1, 5, true, Difficulty.EASY, "crystalized.shardcore.quests.category.assists"),
        ls_hits_dealt("hits_dealt", Game.ls, 20, 45, true, Difficulty.EASY, "crystalized.shardcore.quests.category.hits_dealt"),
        ls_damage_dealt("damage_dealt", Game.ls, 30, 100, true, Difficulty.EASY, "crystalized.shardcore.quests.category.damage_dealt"),
        ko_games_won("games_won", Game.ko, 1, 10, false, Difficulty.MEDIUM, "crystalized.shardcore.quests.category.wins"),
        ko_kills("kills", Game.ko, 5, 10, true, Difficulty.EASY, "crystalized.shardcore.quests.category.kills"),
        ko_items_used("items_used", Game.ko, 1, 5, true, Difficulty.EASY, "crystalized.shardcore.quests.category.items_used"),
        ko_blocks_placed("blocks_placed", Game.ko, 50, 150, true, Difficulty.EASY, "crystalized.shardcore.quests.category.blocks_placed"),
        ko_blocks_broken("blocks_broken", Game.ko, 20, 50, true, Difficulty.EASY, "crystalized.shardcore.quests.category.blocks_broken"),
        cb_games_won("games_won", Game.cb, 1, 10, false, Difficulty.MEDIUM, "crystalized.shardcore.quests.category.wins"),
        cb_kills("kills", Game.cb, 1, 10, true, Difficulty.EASY, "crystalized.shardcore.quests.category.kills"),
        nexus_kills("nexus_kills", Game.cb, 1, 5, true, Difficulty.MEDIUM, "crystalized.shardcore.quests.category.nexus_kills");

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
        EASY(10, 5, "ui/scn3/quests/quest_easy", "ui/scn3/achivements/locked_easy" ,"crystalized.shardcore.quests.difficulty.easy", DARK_GREEN),
        MEDIUM(30, 10, "ui/scn3/quests/quest_medium", "ui/scn3/achivements/locked_medium" ,"crystalized.shardcore.quests.difficulty.medium", YELLOW),
        HARD(50, 20, "ui/scn3/quests/quest_hard", "ui/scn3/achivements/locked_hard" ,"crystalized.shardcore.quests.difficulty.hard", RED),
        EXPERT(80, 30, "ui/scn3/quests/quest_expert", "ui/scn3/achivements/locked_expert" ,"crystalized.shardcore.quests.difficulty.expert", LIGHT_PURPLE);
        final int money;
        final int exp;
        final String model;
        final String lockedModel;
        final String name;
        final NamedTextColor color;
        Difficulty(int money, int exp, String model, String lockedModel, String name, NamedTextColor color){
            this.money = money;
            this.exp = exp;
            this.model = model;
            this.lockedModel = lockedModel;
            this.name = name;
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


