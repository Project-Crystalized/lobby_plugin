package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
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
    HEADPHONES("cosmetic/head/headphones", 1, null, EquipmentSlot.HEAD, "Headphones");
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

    public ItemStack build(){
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("crystalized", itemModel));
        meta.displayName(Component.text(name).color(WHITE).decoration(ITALIC, false));
        ArrayList<Component> desc = new ArrayList<>();
        desc.add(Component.text(slot.toString()).color(BLUE).decoration(ITALIC, false));
        meta.lore(desc);
        EquippableComponent e = meta.getEquippable();
        e.setSlot(slot);
        meta.setEquippable(e);
        item.setItemMeta(meta);
        return item;
    }

   //TODO save what shardcores the player has

    public static void placeCosmetics(Inventory inv, Player p){
        ArrayList<Object[]> cosmetics = LobbyDatabase.fetchCosmetics(p);
        int i = 0;
        ArrayList<Integer> list = new ArrayList<>();
        byte[] b = {0};
        for(Object[] o : cosmetics){
            if(o[0] != b){
                if(InventoryManager.placeOnRightSlot(i, 16) == null){
                    return;
                }
                inv.setItem(InventoryManager.placeOnRightSlot(i, 16), Cosmetic.values()[(Integer)o[1]].build());
                list.add(cosmetics.indexOf(o));
                i++;
            }
        }

        for(Object[] o : cosmetics){
            if(list.contains(cosmetics.indexOf(o))) continue;
            if(InventoryManager.placeOnRightSlot(i, 16) == null){
                return;
            }
            inv.setItem(InventoryManager.placeOnRightSlot(i, 16), Cosmetic.values()[(Integer)o[1]].build());
            i++;
        }
    }
}
