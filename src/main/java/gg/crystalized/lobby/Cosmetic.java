package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;

import java.util.ArrayList;
import java.util.HashMap;

import static net.kyori.adventure.text.format.NamedTextColor.BLUE;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public enum Cosmetic {
    // ATTENTION!!!!
    // The order of the cosmetics here MUST NOT BE CHANGED.
    // otherwise it will mess up the database
    HEADPHONES("cosmetic/head/headphones", 1, null, EquipmentSlot.HEAD, "Headphones"),
    BEANIE("cosmetic/head/beanie", 10, null, EquipmentSlot.HEAD, "Beanie"),
    FOX_BERET("cosmetic/head/fox_beret", null, 50, EquipmentSlot.HEAD, "Fox Beret"),
    COWBOY_HAT("cosmetic/head/merica", null, 150, EquipmentSlot.HEAD, "Cowboy Hat"),
    HANDBAG("cosmetic/handheld/handbag", null, 200, EquipmentSlot.OFF_HAND, "Handbag");
    final String itemModel;
    final Integer obtainableLevel;
    final Integer price;
    final EquipmentSlot slot;
    final String name;
    Cosmetic(String itemModel, Integer obtainableLevel, Integer price, EquipmentSlot slot, String name){
        this.itemModel = itemModel;
        this.obtainableLevel = obtainableLevel;
        this.price = price;
        this.slot = slot;
        this.name = name;
    }

    public ItemStack build(Boolean wearing){
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("crystalized", itemModel));
        meta.displayName(Component.text(name).color(WHITE).decoration(ITALIC, false));
        meta.lore(getDescription(wearing));
        item.setItemMeta(meta);
        return item;
    }

    public ArrayList<Component> getDescription(Boolean wearing){
        ArrayList<Component> desc = new ArrayList<>();
        if(wearing != null && wearing){
            desc.add(Component.text("[Right-click] take off").color(WHITE).decoration(ITALIC, false));
        }else if(wearing != null && !wearing){
            desc.add(Component.text("[Right-click] equip").color(WHITE).decoration(ITALIC, false));
        }else if(obtainableLevel != null){
            desc.add(Component.text("[Right-click] unlock at level" + obtainableLevel).color(WHITE).decoration(ITALIC, false));
        }else{
            desc.add(Component.text("[Right-click] price: " + price).color(WHITE).decoration(ITALIC, false));
        }
        desc.add(Component.text("[Left-click] view").color(WHITE).decoration(ITALIC, false));
        desc.add(Component.text(slot.toString()).color(BLUE).decoration(ITALIC, false));
        return desc;
    }

   //TODO add translatables to everything

    public static void placeCosmetics(Player p, int button){
        EquipmentSlot[] eq = {EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD, null, null};
        if(button == 2){
            //TODO set website URL here
            return;
        }
        Inventory inv = Bukkit.getServer().createInventory(null, 54, Component.text("\uA000\uA00A").color(WHITE));
        int i = 0;
        for(Cosmetic c : Cosmetic.values()){
            if(c.slot != eq[button] || c.ownsCosmetic(p)){
                continue;
            }
            inv.setItem(InventoryManager.placeOnRightSlot(i, 51, 4, 1, 1), c.build(null));
        }
        p.openInventory(inv);
    }
    //TODO scrolling
    //TODO make shardcores equippable

    // 0 = false
    // 1 = true
    public static Cosmetic identifyCosmetic(ItemStack item){
        Cosmetic cos = null;
        for(Cosmetic c : Cosmetic.values()){
            if(c.build(false).equals(item)){
                cos = c;
                break;
            }
        }

        if(cos != null){
            return cos;
        }

        for(Cosmetic c : Cosmetic.values()){
            if(c.build(true).equals(item)){
                cos = c;
                break;
            }
        }

        return cos;
    }

    public boolean isWearing(Player p){
        if(!ownsCosmetic(p)){
            return false;
        }
        ArrayList<Object[]> list = LobbyDatabase.fetchCosmetics(p);
        boolean wear = false;
        for(Object[] o : list){
            if((Integer)o[1] != this.ordinal()){
                continue;
            }
            if((Integer)o[2] == 1){
                wear = true;
                break;
            }
        }
        return wear;
    }

    public boolean ownsCosmetic(Player p){
        ArrayList<Object[]> list = LobbyDatabase.fetchCosmetics(p);
        boolean own = false;
        for(Object[] o : list){
            if((Integer)o[1] == this.ordinal()){
                own = true;
                break;
            }
        }
        return own;
    }

    public void clicked(ClickType click, Player p){
        if(click.isRightClick()){
            if(this.isWearing(p)){
                p.sendEquipmentChange(p, this.slot, null);
            }else {
                p.sendEquipmentChange(p, this.slot, this.build(true));
            }
            LobbyDatabase.cosmeticSetWearing(p, this, !this.isWearing(p));
        }
        //TODO add view feature
    }

    //TODO make a buy method
}
