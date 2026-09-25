package gg.crystalized.lobby;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static gg.crystalized.lobby.LobbyDatabase.ownsCosmetic;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.entity.EntityType.MANNEQUIN;
import static org.bukkit.event.inventory.InventoryType.SlotType.ARMOR;
import static org.bukkit.inventory.EquipmentSlot.HEAD;
import static org.bukkit.inventory.EquipmentSlot.OFF_HAND;

public class Cosmetic{
    public static final int DEFAULT_SHARDCORE = 6;
    public static ArrayList<Cosmetic> cosmetics = new ArrayList<>();
    final int id;
    final String itemModel;
    final Integer obtainableLevel;
    final Integer price;
    final EquipmentSlot slot;
    final Component name;

    Cosmetic(int id, String itemModel, Integer obtainableLevel, Integer price, EquipmentSlot slot, Component name) {
        this.id = id;
        this.itemModel = itemModel;
        this.obtainableLevel = obtainableLevel;
        this.price = price;
        this.slot = slot;
        this.name = name;
    }

    public static void createCosmetics(boolean triedAlready){
        try {
            final String directory = Files.readString(Paths.get(LobbyDatabase.dbDir() + "/cosmetics.json"));
            JsonObject json = JsonParser.parseString(directory).getAsJsonObject();
            Map<String, JsonElement> map = json.asMap();
            for (String s : map.keySet()) {
                JsonObject j = map.get(s).getAsJsonObject();
                Cosmetic c = new Cosmetic(j.get("id").getAsInt(), j.get("model").getAsString(), getInt(j.get("level")), getInt(j.get("price")), getSlot(j.get("slot")), Component.translatable(j.get("name").getAsString()));
                cosmetics.add(c);
            }
        }catch(IOException e){
            try{
                if(triedAlready || (!(e instanceof FileNotFoundException || e instanceof NoSuchFileException) && !Objects.equals(e.getMessage(), Files.readString(Paths.get(LobbyDatabase.dbDir() + "/cosmetics.json"))))){
                    Bukkit.getLogger().severe("[Lobby_plugin] Couldn't get cosmetics from json continuing without.");
                    Bukkit.getLogger().severe(e.getMessage());
                    return;
                }
                InputStream in = Lobby_plugin.getInstance().getResource("cosmetics.json");
                OutputStream out = Files.newOutputStream(Paths.get(LobbyDatabase.dbDir() + "/cosmetics.json"));
                try {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }finally{
                    in.close();
                    out.close();
                }
                createCosmetics(true);
            }catch(IOException ex){
                Bukkit.getLogger().severe("[Lobby_plugin] Couldn't copy cosmetics.json into databases folder.");
                Bukkit.getLogger().severe(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static Integer getInt(JsonElement j) {
        try {
            Integer i = j.getAsInt();
            return i;
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    public static EquipmentSlot getSlot(JsonElement json) {
        String s = json.getAsString();
        return switch (s) {
            case "HEAD" -> HEAD;
            case "HAND" -> EquipmentSlot.HAND;
            case "OFF_HAND" -> OFF_HAND;
            default -> null;
        };
    }

    public ItemStack build(OfflinePlayer p, Boolean wearing, Boolean open, boolean viewing) {
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("crystalized", itemModel));
        if (wearing != null && wearing && slot == EquipmentSlot.HAND) {
            meta.displayName(Component.translatable("crystalized.item.shardcore3.name").color(LIGHT_PURPLE).decoration(BOLD, true).decoration(ITALIC, true));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.translatable("crystalized.item.shardcore3.desc").color(WHITE).decoration(ITALIC, false));
            meta.lore(lore);
            if (open) {
                item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(1).build());;
            }
        } else {
            meta.displayName(name.color(WHITE).decoration(ITALIC, false));
            meta.lore(getDescription(wearing, viewing));
        }
        item.setItemMeta(meta);
        if((wearing != null && wearing && slot == EquipmentSlot.HAND) && (!open && !(App.active.get(p) == null) && !App.active.get(p).isEmpty())) item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(2).build());
        if((wearing != null && wearing && slot == EquipmentSlot.HAND) && open) item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(1).build());
        return item;
    }

    public ArrayList<Component> getDescription(Boolean wearing, boolean viewing) {
        ArrayList<Component> desc = new ArrayList<>();
        if (wearing != null && wearing) {
            desc.add(Component.translatable("crystalized.generic.right_click").append(Component.translatable("crystalized.shardcore.shop.action.take_off")).color(WHITE).decoration(ITALIC, false));
        } else if (wearing != null && !wearing) {
            desc.add(Component.translatable("crystalized.generic.right_click").append(Component.translatable("crystalized.shardcore.shop.action.equip")).color(WHITE).decoration(ITALIC, false));
        } else if (obtainableLevel != null) {
            desc.add(Component.translatable("crystalized.generic.right_click").append( Component.translatable("crystalized.shardcore.shop.action.unlock")).append(Component.text(obtainableLevel)).color(WHITE).decoration(ITALIC, false));
        } else if(price != null){
            desc.add(Component.translatable("crystalized.generic.right_click").append( Component.translatable("crystalized.shardcore.shop.action.price")).append(Component.text(price)).color(WHITE).decoration(ITALIC, false));
        }
        if(viewing){
            desc.add(Component.translatable("crystalized.generic.left_click").append( Component.translatable("crystalized.shardcore.shop.action.end_view")).color(WHITE).decoration(ITALIC, false));
        }else {
            desc.add(Component.translatable("crystalized.generic.left_click").append( Component.translatable("crystalized.shardcore.shop.action.view")).color(WHITE).decoration(ITALIC, false));
        }
        desc.add(Component.text(slot.toString()).color(BLUE).decoration(ITALIC, false));
        return desc;
    }

    public static void placeCosmetics(Player p, App a) {
        if(a == null){
            return;
        }
        Inventory inv = Bukkit.getServer().createInventory(null, 54, Component.text("\uA000\uA00A").color(WHITE));
        App.UITemplates.createUI(inv, App.useCases.ShopPage);
        if (a == App.WebButton) {
            //TODO set website URL here
            return;
        }
        int i = ScrollableView.getView(p).page * 15;
        int[] border = {7, 16, 25, 34, 43, 52};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int slot = 29;
        int line = 3;
        HashMap<Cosmetic, Boolean> owned = LobbyDatabase.getOwnedCosmetics(p);
        List<Cosmetic> cos = new ArrayList<>();
        for(Cosmetic c : Cosmetic.getCosmeticsBySlot((EquipmentSlot)a.extra)){
            if (owned.containsKey(c) || (c.price == null && c.obtainableLevel == null)) {
                continue;
            }
            cos.add(c);
        }
        if(i >= cos.size()){
            i = (ScrollableView.getView(p).page -1) * 15;
            ScrollableView.getView(p).page--;
        }
        cos = cos.subList(i, cos.size());
        for (Cosmetic c : cos) {
            if(slot >= border[line]){
                if(line +1 >= nextLine.length) break;
                line++;
                slot = nextLine[line];
            }
            inv.setItem(slot, c.build(p, null, false, CosmeticView.isViewing(p, c)));
            slot++;
        }
        p.openInventory(inv);
    }

    public static ArrayList<Cosmetic> getCosmeticsBySlot(EquipmentSlot slot){
        ArrayList<Cosmetic> cos = new ArrayList<>();
        for (Cosmetic c : cosmetics) {
            if(c.slot == slot){
                cos.add(c);
            }
        }
        return cos;
    }

    public static App getButton(InventoryView view){
        if(Cosmetic.identifyCosmetic(view.getTopInventory().getItem(29)) == null){
            return null;
        }
        EquipmentSlot slot = Cosmetic.identifyCosmetic(view.getTopInventory().getItem(29)).slot;
        if(slot == null){
            return null;
        }
        App ap = null;
        for(App app : App.values()){
            if(app.extra == slot){
                ap = app;
                break;
            }
        }
        return ap;
    }

    // 0 = false
    // 1 = true
    public static Cosmetic identifyCosmetic(ItemStack item) {
        for (Cosmetic c : cosmetics) {
            if (item == null || item.getItemMeta() == null) {
                continue;
            }
            if (Objects.equals(item.getItemMeta().getItemModel(), new NamespacedKey("crystalized", c.itemModel))) {
                return c;
            }
        }
        return null;
    }

    public boolean isWearing(OfflinePlayer p) {
        return LobbyDatabase.isWearing(p, this);
    }

    public boolean ownsCosmetic(OfflinePlayer p) {
        return LobbyDatabase.ownsCosmetic(p, this);
    }

    public static Cosmetic getShardcore(Player p) {
        return LobbyDatabase.getShardcore(p);
    }

    public static Cosmetic getCosmeticById(int id) {
        for (Cosmetic c : cosmetics) {
            if (c.id == id) {
                return c;
            }
        }
        return null;
    }

    public static void giveCosmetics(Player p){
        for(Cosmetic c : LobbyDatabase.getWornCosmetics(p)){
            if(c.slot != EquipmentSlot.HAND){
                Cosmetic.equip(c.slot, p, c.build(p, true, false, CosmeticView.isViewing(p, c)));
            }
        }
    }

    public static void giveCosmeticsInGame(Player p){
        for(Cosmetic c : LobbyDatabase.getWornCosmetics(p)){
            if(c.slot == HEAD){
                Cosmetic.equip(c.slot, p, c.build(p, true, false, CosmeticView.isViewing(p, c)));
            }
        }
    }

    public void clicked(ClickType click, Player p, InventoryType.SlotType type, int slotNumber, Inventory inv) {
        Boolean wearing = null;
        if (click.isRightClick()) {
            if(!ownsCosmetic(p)) {
                if (price == null) {
                    return;
                }

                if (LevelManager.getMoney(p) < price) {
                    p.sendMessage(Component.translatable("crystalized.shardcore.shop.cant_afford").color(RED));
                    return;
                }

                LobbyDatabase.addCosmetic(p, this, false);
                LevelManager.giveMoney(p, price * (-1));
                App.Shop.action(p, p);
                p.sendMessage(Component.translatable("crystalized.shardcore.shop.message.bought").color(WHITE).append(name));
            }else {
                boolean worn = isWearing(p);
                if (worn) {
                    p.sendMessage(Component.translatable("crystalized.shardcore.shop.message.unequipped").color(WHITE).append(name));
                    if (slot != EquipmentSlot.HAND) {
                        equip(slot, p, null);
                    } else {
                        p.getInventory().setItem(4, getCosmeticById(DEFAULT_SHARDCORE).build(p, false, true, CosmeticView.isViewing(p, this)));
                    }

                } else {
                    p.sendMessage(Component.translatable("crystalized.shardcore.shop.message.equipped").color(WHITE).append(name));
                    equip(slot, p, build(p, true, false, CosmeticView.isViewing(p, this)));
                }
                LobbyDatabase.cosmeticSetWearing(p, this, !worn);
                wearing = !worn;
                unEquipAllApartFrom(p);
                if(CosmeticView.findView(p) != null) inv.setItem(4, App.EquipBuy.build(p));
            }
        } else if (click.isLeftClick()) {
            CosmeticView v = CosmeticView.getView(p);
            if(v.isRunning()){
                if(CosmeticView.isViewing(p, this)){
                    v.removeCosmetic();
                }else {
                    v.changeCosmetic(this);
                }
            }else {
                v.startView(this);
            }
            if(type != ARMOR){
                inv.close();
            }
        }

        inv.setItem(slotNumber, build(p, wearing != null ? wearing : isWearing(p), false, CosmeticView.isViewing(p, this)));
    }

    public void unEquipAllApartFrom(Player p){
        LobbyDatabase.unEquipCosmetics(p, this);
    }

    public static void equip(EquipmentSlot slot, Player p, ItemStack item){
        if(slot == HEAD){
            p.getInventory().setHelmet(item);
        }else if(slot == OFF_HAND){
            p.getInventory().setItemInOffHand(item);
        }else if (slot == EquipmentSlot.HAND){
            p.getInventory().setItem(4, item);
        }
    }
}

class CosmeticView{
    public static ArrayList<CosmeticView> views = new ArrayList<>();
    Player p;
    private boolean running = false;
    Cosmetic currentCosmetic = null;
    Mannequin mannequin;
    CosmeticView(Player player){
        this.p = player;
    }
    public void startView(Cosmetic c){
        if(getView(p).running) return;
        Location loc = LobbyConfig.Locations.get("clothing_room").clone();
        this.mannequin = (Mannequin)loc.getWorld().spawnEntity(loc, MANNEQUIN);
        running = true;
        if(c != null){
            currentCosmetic = c;
            mannequin.getEquipment().setItem(c.slot, c.build(p, false, false, isViewing(p, c)), true);
        }
        mannequin.setProfile(ResolvableProfile.resolvableProfile(p.getPlayerProfile()));
        mannequin.customName(Component.translatable("crystalized.generic.you"));

        loc.setX(loc.getX() + 2);
        p.teleport(loc);
        p.lookAt(mannequin, LookAnchor.EYES, LookAnchor.EYES);
        giveItems();
        for(Player player : Bukkit.getOnlinePlayers()){
            player.hideEntity(Lobby_plugin.getInstance(), mannequin);
            player.hideEntity(Lobby_plugin.getInstance(), p);
        }
        p.showEntity(Lobby_plugin.getInstance(), mannequin);
    }

    public void changeCosmetic(Cosmetic c){
        if(currentCosmetic != null) {
            mannequin.getEquipment().setItem(currentCosmetic.slot, null, true);
        }
        currentCosmetic = c;
        mannequin.getEquipment().setItem(c.slot, c.build(p, false, false, isViewing(p, c)), true);
    }

    public void removeCosmetic(){
        mannequin.getEquipment().setItem(currentCosmetic.slot, null, true);
        currentCosmetic = null;
    }

    public void endView(){
        running = false;
        views.remove(this);
        mannequin.remove();
        mannequin = null;
        p.setGameMode(GameMode.SURVIVAL);
        p.teleport(LobbyConfig.Locations.get("spawn"));
        p.getInventory().clear();
        InventoryManager.giveLobbyItems(p);

        for(Player player : Bukkit.getOnlinePlayers()){
            player.showEntity(Lobby_plugin.getInstance(), p);
        }

        new BukkitRunnable(){
            public void run(){
                Cosmetic.giveCosmetics(p);
            }
        }.runTaskLater(Lobby_plugin.getInstance(), 1);
    }

    public static CosmeticView getView(Player p){
        if(findView(p) == null){
            CosmeticView v = new CosmeticView(p);
            CosmeticView.views.add(v);
            return v;
        }
        return findView(p);
    }

    public void getWardrobe(App a){
        if(a == null){
            return;
        }
        String titlePart = "\uA00F";
        if(currentCosmetic != null || a != App.Wardrobe){
            titlePart = "\uA010";
        }

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("\uA000" + titlePart).color(WHITE));
        App.UITemplates.createUI(inv, App.useCases.ShopPage);
        int i = ScrollableView.getView(p).page * 15;
        int[] border = {7, 16, 25, 34, 43, 52};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int slot = 29;
        int line = 3;
        HashMap<Cosmetic, Boolean> owned = LobbyDatabase.getOwnedCosmetics(p);
        List<Cosmetic> cos = new ArrayList<>();
        for(Cosmetic c : Cosmetic.getCosmeticsBySlot((EquipmentSlot)a.extra)){
            if (!owned.containsKey(c)) {
                continue;
            }
            cos.add(c);
        }
        if(i > cos.size()){
            i = (ScrollableView.getView(p).page -1) * 15;
            ScrollableView.getView(p).page--;
        }
        cos = cos.subList(i, cos.size());
        for (Cosmetic c : cos) {
            if(slot >= border[line]){
                if(line +1 >= nextLine.length) break;
                line++;
                slot = nextLine[line];
            }
            inv.setItem(slot, c.build(p, owned.get(c), false, CosmeticView.isViewing(p, c)));
            slot++;
        }
        p.openInventory(inv);
    }

    public void giveItems(){
        Inventory inv = p.getInventory();
        inv.setItem(0, App.Wardrobe.build(p));
        inv.setItem(1, App.Shop.build(p));
        inv.clear(2);
        inv.clear(3);
        inv.setItem(8, App.LeaveWardrobe.build(p));
        inv.setItem(4, App.EquipBuy.build(p));
    }

    public static CosmeticView findView(Player p){
        if(p == null){
            return null;
        }
        for(CosmeticView view : views){
            if(view.p.equals(p)){
                return view;
            }
        }
        return null;
    }

    public boolean isRunning(){
        return running;
    }
    public static boolean isViewing(Player p, Cosmetic c){
        if(findView(p) == null || !findView(p).isRunning()){
            return false;
        }

        if(findView(p).currentCosmetic == null){
            return false;
        }

        if(findView(p).currentCosmetic.equals(c)){
            return true;
        }
        return false;
    }

    public void equipOrBuy(Player p){
        if(!ownsCosmetic(p, currentCosmetic)) {
            if (currentCosmetic.price == null) {
                return;
            }

            if (LevelManager.getMoney(p) < currentCosmetic.price) {
                p.sendMessage(Component.translatable("crystalized.shardcore.shop.cant_afford").color(RED));
                return;
            }

            LobbyDatabase.addCosmetic(p, currentCosmetic, false);
            LevelManager.giveMoney(p, currentCosmetic.price * (-1));
            p.sendMessage(Component.translatable("crystalized.shardcore.shop.message.bought").color(WHITE).append(currentCosmetic.name));
            App.Shop.action(p, p);
        }

        boolean wearing = currentCosmetic.isWearing(p);
        if (wearing) {
            p.sendMessage(Component.translatable("crystalized.shardcore.shop.message.unequipped").color(WHITE).append(currentCosmetic.name));
            if (currentCosmetic.slot != EquipmentSlot.HAND) {
                Cosmetic.equip(currentCosmetic.slot, p, null);
            } else {
                p.getInventory().setItem(4, Cosmetic.getCosmeticById(Cosmetic.DEFAULT_SHARDCORE).build(p, false, true, CosmeticView.isViewing(p, currentCosmetic)));
            }
        } else {
            p.sendMessage(Component.translatable("crystalized.shardcore.shop.message.equipped").color(WHITE).append(currentCosmetic.name));
            if (currentCosmetic.slot != EquipmentSlot.HAND) {
                Cosmetic.equip(currentCosmetic.slot, p, currentCosmetic.build(p, true, false, CosmeticView.isViewing(p, currentCosmetic)));
            } else {
                p.getInventory().setItem(4, currentCosmetic.build(p, true, true, CosmeticView.isViewing(p, currentCosmetic)));
            }
        }
        LobbyDatabase.cosmeticSetWearing(p, currentCosmetic, !wearing);
        currentCosmetic.unEquipAllApartFrom(p);
        if(CosmeticView.findView(p) != null) p.getInventory().setItem(4, App.EquipBuy.build(p));
    }
}
