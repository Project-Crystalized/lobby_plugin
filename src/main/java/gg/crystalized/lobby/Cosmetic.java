package gg.crystalized.lobby;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.entity.LookAnchor;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.trait.MirrorTrait;
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_PASSENGERS;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.event.inventory.InventoryType.SlotType.ARMOR;

public class Cosmetic {
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

    public static void createCosmetics(InputStream stream) {
        JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        Map<String, JsonElement> map = json.asMap();
        for (String s : map.keySet()) {
            JsonObject j = map.get(s).getAsJsonObject();
            Cosmetic c = new Cosmetic(j.get("id").getAsInt(), j.get("model").getAsString(), getInt(j.get("level")), getInt(j.get("price")), getSlot(j.get("slot")), Component.translatable(j.get("name").getAsString()));
            cosmetics.add(c);
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

    public ItemStack build(Boolean wearing, Boolean open) {
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("crystalized", itemModel));
        if (wearing != null && wearing && slot == EquipmentSlot.HAND) {
            meta.displayName(Component.translatable("crystalized.item.shardcore3.name").color(LIGHT_PURPLE).decoration(BOLD, true).decoration(ITALIC, true));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.translatable("crystalized.item.shardcore3.desc").color(WHITE).decoration(ITALIC, false));
            meta.lore(lore);
            if (open) {
                meta.setCustomModelData(1);
            }
        } else {
            meta.displayName(name.color(WHITE).decoration(ITALIC, false));
            meta.lore(getDescription(wearing));
        }
        item.setItemMeta(meta);
        return item;
    }

    public ArrayList<Component> getDescription(Boolean wearing) {
        ArrayList<Component> desc = new ArrayList<>();
        if (wearing != null && wearing) {
            desc.add(Component.text("[Right-click] take off").color(WHITE).decoration(ITALIC, false));
        } else if (wearing != null && !wearing) {
            desc.add(Component.text("[Right-click] equip").color(WHITE).decoration(ITALIC, false));
        } else if (obtainableLevel != null) {
            desc.add(Component.text("[Right-click] unlock at level " + obtainableLevel).color(WHITE).decoration(ITALIC, false));
        } else {
            desc.add(Component.text("[Right-click] price: " + price).color(WHITE).decoration(ITALIC, false));
        }
        desc.add(Component.text("[Left-click] view").color(WHITE).decoration(ITALIC, false));
        desc.add(Component.text(slot.toString()).color(BLUE).decoration(ITALIC, false));
        return desc;
    }

    //TODO add translatables to everything

    public static void placeCosmetics(Player p, App a, int page) {
        Inventory inv = Bukkit.getServer().createInventory(null, 54, Component.text("\uA000\uA00A").color(WHITE));
        App.UITemplates.createUI(inv, App.useCases.ShopPage);
        if (a == App.WebButton) {
            //TODO set website URL here
            return;
        }
        int i = (page - 1) * 15;
        for (Cosmetic c : cosmetics) {
            if (c.slot != a.extra || c.ownsCosmetic(p)) {
                continue;
            }

            if (InventoryManager.placeOnRightSlot(i, 51, 3, 1, 0) != null) {
                inv.setItem(InventoryManager.placeOnRightSlot(i, 51, 3, 1, 0), c.build(null, false));
            } else {
                break;
            }
            i++;
        }
        p.openInventory(inv);
    }
    //TODO make shardcores equippable

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
                p.sendEquipmentChange(p, c.slot, c.build(true, false));
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
                    p.sendMessage(Component.text("You can't afford this cosmetic :(").color(RED));
                    return;
                }

                LobbyDatabase.addCosmetic(p, this, false);
                LevelManager.giveMoney(p, price * (-1));
                App.Shop.action(p);
            }else {
                if (isWearing(p)) {
                    if (slot != EquipmentSlot.HAND) {
                        p.sendEquipmentChange(p, slot, null);
                    } else {
                        p.getInventory().setItem(4, getCosmeticById(DEFAULT_SHARDCORE).build(false, true));
                    }

                } else {
                    if (slot != EquipmentSlot.HAND) {
                        p.sendEquipmentChange(p, slot, build(true, false));
                    } else {
                        p.getInventory().setItem(4, build(true, true));
                    }
                }
                LobbyDatabase.cosmeticSetWearing(p, this, !isWearing(p));
                unEquipAllApartFrom(p);
                if(type == ARMOR){
                    return;
                }
                rebuild(inv, slotNumber, p);
            }
        } else if (click.isLeftClick()) {
            CosmeticView v = CosmeticView.getView(p);
            if(v.isRunning()){
                v.changeCosmetic(this);
            }else {
                v.startView(this);
            }
        }
    }

    private void unEquipAllApartFrom(Player p){
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
        inv.setItem(slot, build(isWearing(p), false));
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
            mannequin.getOrAddTrait(Equipment.class).set(getEquipmentSlot(c.slot), c.build(false, false));
        }
        Location loc = LobbyConfig.Locations.get("clothing_room").clone();
        SkinTrait skin = mannequin.getOrAddTrait(SkinTrait.class);
        skin.setSkinPersistent(p);
        skin.setSkinName(p.getName(), true);
        mannequin.spawn(loc);

        loc.setX(loc.getX() + 2);
        p.teleport(loc, RETAIN_PASSENGERS);
        p.lookAt(mannequin.getEntity(), LookAnchor.EYES, LookAnchor.EYES);
        giveItems();
        for(Player player : Bukkit.getOnlinePlayers()){
            player.hideEntity(Lobby_plugin.getInstance(), mannequin.getEntity());
        }
        p.showEntity(Lobby_plugin.getInstance(), mannequin.getEntity());
    }

    public void changeCosmetic(Cosmetic c){
        mannequin.getOrAddTrait(Equipment.class).set(getEquipmentSlot(c.slot), c.build(false, false));
    }

    public void endView(){
        running = false;
        views.remove(this);
        mannequin.despawn();
        p.setGameMode(GameMode.SURVIVAL);
        p.teleport(LobbyConfig.Locations.get("spawn"), RETAIN_PASSENGERS);
        p.getInventory().clear();
        InventoryManager.giveLobbyItems(p);
        Cosmetic.giveCosmetics(p);
    }

    public static CosmeticView getView(Player p){
        if(findView(p) == null){
            CosmeticView v = new CosmeticView(p);
            CosmeticView.views.add(v);
            return v;
        }
        return findView(p);
    }
    //TODO add ability to remove cosmetic from mannequin
    public Inventory getWardrobe(App a, int page){
        String titlePart = "\uA00F";
        if(currentCosmetic != null || a != App.Wardrobe){
            titlePart = "\uA010";
        }

        if(titlePart.equals("\uA00F")){
            return App.prepareInv("\uA000" + titlePart, 54, App.useCases.Wardrobe, p);
        }

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("\uA000" + titlePart).color(WHITE));
        App.UITemplates.createUI(inv, App.useCases.ShopPage);
        int i = (page - 1) * 15;
        EquipmentSlot slot = (EquipmentSlot)a.extra;
        if(currentCosmetic != null){
            slot = currentCosmetic.slot;
        }
        for (Cosmetic c : Cosmetic.cosmetics) {
            if (c.slot != slot || !c.ownsCosmetic(p)) {
                continue;
            }

            if (InventoryManager.placeOnRightSlot(i, 51, 3, 1, 0) != null) {
                inv.setItem(InventoryManager.placeOnRightSlot(i, 51, 3, 1, 0), c.build(LobbyDatabase.isWearing(p,c), false));
            } else {
                break;
            }
            i++;
        }
        return inv;
    }

    public void giveItems(){
        Inventory inv = p.getInventory();
        inv.setItem(0, App.Wardrobe.build());
        inv.setItem(1, App.Shop.build());
        inv.clear(2);
        inv.clear(3);
        inv.setItem(8, App.LeaveWardrobe.build());
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
}