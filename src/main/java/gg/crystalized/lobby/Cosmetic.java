package gg.crystalized.lobby;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.entity.LookAnchor;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.trait.SkinTrait;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.codehaus.plexus.util.IOUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static gg.crystalized.lobby.LobbyDatabase.ownsCosmetic;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.event.inventory.InventoryType.SlotType.ARMOR;

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
            final String directory = Files.readString(Paths.get(System.getProperty("user.home") + "/databases/cosmetics.json"));
            JsonObject json = JsonParser.parseString(directory).getAsJsonObject();
            Map<String, JsonElement> map = json.asMap();
            for (String s : map.keySet()) {
                JsonObject j = map.get(s).getAsJsonObject();
                Cosmetic c = new Cosmetic(j.get("id").getAsInt(), j.get("model").getAsString(), getInt(j.get("level")), getInt(j.get("price")), getSlot(j.get("slot")), Component.translatable(j.get("name").getAsString()));
                cosmetics.add(c);
            }
        }catch(IOException e){
            try{
                if(triedAlready || (!(e instanceof FileNotFoundException || e instanceof NoSuchFileException) && !Objects.equals(e.getMessage(), Files.readString(Paths.get(System.getProperty("user.home") + "/databases/cosmetics.json"))))){
                    Bukkit.getLogger().severe("[Lobby_plugin] Couldn't get cosmetics from json continuing without.");
                    Bukkit.getLogger().severe(e.getMessage());
                    return;
                }
                InputStream in = Lobby_plugin.getInstance().getResource("cosmetics.json");
                OutputStream out = Files.newOutputStream(Paths.get(System.getProperty("user.home") + "/databases/cosmetics.json"));
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
            case "HEAD" -> EquipmentSlot.HEAD;
            case "HAND" -> EquipmentSlot.HAND;
            case "OFF_HAND" -> EquipmentSlot.OFF_HAND;
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
        List<Cosmetic> cos = new ArrayList<>();
        for(Cosmetic c : Cosmetic.getCosmeticsBySlot((EquipmentSlot)a.extra)){
            if (c.ownsCosmetic(p) || (c.price == null && c.obtainableLevel == null)) {
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
        for(Cosmetic c : Cosmetic.cosmetics){
            if(c.isWearing(p) && c.slot != EquipmentSlot.HAND){
                p.sendEquipmentChange(p, c.slot, c.build(p, true, false, CosmeticView.isViewing(p, c)));
            }
        }
    }

    public static void giveCosmeticsInGame(Player p){
        for(Cosmetic c : Cosmetic.cosmetics){
            if(c.isWearing(p) && c.slot == EquipmentSlot.HEAD){
                p.sendEquipmentChange(p, c.slot, c.build(p, true, false, CosmeticView.isViewing(p, c)));
            }
        }
    }

    public void clicked(ClickType click, Player p, InventoryType.SlotType type, int slotNumber, Inventory inv) {
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
                if (isWearing(p)) {
                    p.sendMessage(Component.translatable("crystalized.shardcore.shop.message.unequipped").color(WHITE).append(name));
                    if (slot != EquipmentSlot.HAND) {
                        p.sendEquipmentChange(p, slot, null);
                    } else {
                        p.getInventory().setItem(4, getCosmeticById(DEFAULT_SHARDCORE).build(p, false, true, CosmeticView.isViewing(p, this)));
                    }

                } else {
                    p.sendMessage(Component.translatable("crystalized.shardcore.shop.message.equipped").color(WHITE).append(name));
                    if (slot != EquipmentSlot.HAND) {
                        p.sendEquipmentChange(p, slot, build(p, true, false, CosmeticView.isViewing(p, this)));
                    } else {
                        p.getInventory().setItem(4, build(p, true, true, CosmeticView.isViewing(p, this)));
                    }
                }
                LobbyDatabase.cosmeticSetWearing(p, this, !isWearing(p));
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

        if(type != ARMOR){
            rebuild(inv, slotNumber, p);
        }
    }

    public void unEquipAllApartFrom(Player p){
        for(Cosmetic c : cosmetics){
            if(equals(c)){
                continue;
            }
            if(slot == c.slot){
                LobbyDatabase.cosmeticSetWearing(p, c, false);
            }
        }
    }

    public void rebuild(Inventory inv, int slot, Player p){
        inv.setItem(slot, build(p, isWearing(p), false, CosmeticView.isViewing(p, this)));
    }
}

class CosmeticView{
    public static ArrayList<CosmeticView> views = new ArrayList<>();
    Player p;
    private boolean running = false;
    Cosmetic currentCosmetic = null;
    NPC mannequin;
    CosmeticView(Player player){
        Location loc = LobbyConfig.Locations.get("clothing_room");
        this.mannequin = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "You", loc);
        this.p = player;
    }
    public void startView(Cosmetic c){
        running = true;
        if(c != null){
            currentCosmetic = c;
            mannequin.getOrAddTrait(Equipment.class).set(getEquipmentSlot(c.slot), c.build(p, false, false, isViewing(p, c)));
        }
        Location loc = LobbyConfig.Locations.get("clothing_room").clone();
        SkinTrait skin = mannequin.getOrAddTrait(SkinTrait.class);
        skin.setSkinPersistent(p);
        skin.setSkinName(p.getName(), true);
        mannequin.spawn(loc);

        loc.setX(loc.getX() + 2);
        p.teleport(loc);
        p.lookAt(mannequin.getEntity(), LookAnchor.EYES, LookAnchor.EYES);
        giveItems();
        for(Player player : Bukkit.getOnlinePlayers()){
            player.hideEntity(Lobby_plugin.getInstance(), mannequin.getEntity());
        }
        p.showEntity(Lobby_plugin.getInstance(), mannequin.getEntity());
    }

    public void changeCosmetic(Cosmetic c){
        currentCosmetic = c;
        Map<Equipment.EquipmentSlot, ItemStack> equipment = mannequin.getOrAddTrait(Equipment.class).getEquipmentBySlot();
        for(Equipment.EquipmentSlot key : equipment.keySet()){
            mannequin.getOrAddTrait(Equipment.class).set(key, null);
        }
        mannequin.getOrAddTrait(Equipment.class).set(getEquipmentSlot(c.slot), c.build(p, false, false, isViewing(p, c)));
    }

    public void removeCosmetic(){
        currentCosmetic = null;
        Map<Equipment.EquipmentSlot, ItemStack> equipment = mannequin.getOrAddTrait(Equipment.class).getEquipmentBySlot();
        for(Equipment.EquipmentSlot key : equipment.keySet()){
            mannequin.getOrAddTrait(Equipment.class).set(key, null);
        }
    }

    public void endView(){
        running = false;
        views.remove(this);
        mannequin.despawn();
        p.setGameMode(GameMode.SURVIVAL);
        p.teleport(LobbyConfig.Locations.get("spawn"));
        p.getInventory().clear();
        InventoryManager.giveLobbyItems(p);
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
        List<Cosmetic> cos = new ArrayList<>();
        for(Cosmetic c : Cosmetic.getCosmeticsBySlot((EquipmentSlot)a.extra)){
            if (!c.ownsCosmetic(p)) {
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
            inv.setItem(slot, c.build(p, c.isWearing(p), false, CosmeticView.isViewing(p, c)));
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

    private static Equipment.EquipmentSlot getEquipmentSlot(EquipmentSlot slot){
        for(Equipment.EquipmentSlot equip : Equipment.EquipmentSlot.values()){
            if(equip.toBukkit().equals(slot)){
                return equip;
            }
        }
        return null;
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

        if (currentCosmetic.isWearing(p)) {
            p.sendMessage(Component.translatable("crystalized.shardcore.shop.message.unequipped").color(WHITE).append(currentCosmetic.name));
            if (currentCosmetic.slot != EquipmentSlot.HAND) {
                p.sendEquipmentChange(p, currentCosmetic.slot, null);
            } else {
                p.getInventory().setItem(4, Cosmetic.getCosmeticById(Cosmetic.DEFAULT_SHARDCORE).build(p, false, true, CosmeticView.isViewing(p, currentCosmetic)));
            }
        } else {
            p.sendMessage(Component.translatable("crystalized.shardcore.shop.message.equipped").color(WHITE).append(currentCosmetic.name));
            if (currentCosmetic.slot != EquipmentSlot.HAND) {
                p.sendEquipmentChange(p, currentCosmetic.slot, currentCosmetic.build(p, true, false, CosmeticView.isViewing(p, currentCosmetic)));
            } else {
                p.getInventory().setItem(4, currentCosmetic.build(p, true, true, CosmeticView.isViewing(p, currentCosmetic)));
            }
        }
        LobbyDatabase.cosmeticSetWearing(p, currentCosmetic, !currentCosmetic.isWearing(p));
        currentCosmetic.unEquipAllApartFrom(p);
        if(CosmeticView.findView(p) != null) p.getInventory().setItem(4, App.EquipBuy.build(p));
    }
}