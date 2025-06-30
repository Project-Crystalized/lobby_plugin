package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public enum Cosmetic {
    // ATTENTION!!!!
    // The order of the cosmetics here MUST NOT BE CHANGED.
    // otherwise it will mess up the database
    HEADPHONES("cosmetic/head/headphones", 1, null, EquipmentSlot.HEAD, "Headphones"),
    BEANIE("cosmetic/head/beanie", 10, null, EquipmentSlot.HEAD, "Beanie"),
    FOX_BERET("cosmetic/head/fox_beret", null, 50, EquipmentSlot.HEAD, "Fox Beret"),
    COWBOY_HAT("cosmetic/head/merica", null, 150, EquipmentSlot.HEAD, "Cowboy Hat"),
    HANDBAG("cosmetic/handheld/handbag", null, 200, EquipmentSlot.OFF_HAND, "Handbag"),
    BLUE_SHARDCORE("shardcorenexus3/blue", null, 200, EquipmentSlot.HAND, "Shardcore 1"),
    GRAY_SHARDCORE("shardcorenexus3/gray", null, 200, EquipmentSlot.HAND, "Shardcore 2"),
    PURPLE_SHARDCORE("shardcorenexus3/purple", null, 200, EquipmentSlot.HAND, "Shardcore 3"),
    AUGUST_SHARDCORE("shardcorenexus3/augustify", null, 200, EquipmentSlot.HAND, "Shardcore 4"),
    ABBY1_SHARDCORE("shardcorenexus3/abby1", null, 200, EquipmentSlot.HAND, "Shardcore 5"),
    SHADOW1_SHARDCORE("shardcorenexus3/shadow1", null, 200, EquipmentSlot.HAND, "Shardcore 6");
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

    public ItemStack build(Boolean wearing, Boolean open){
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("crystalized", itemModel));
        if(wearing != null && wearing && slot == EquipmentSlot.HAND){
            meta.displayName(Component.text("Menu").color(LIGHT_PURPLE).decoration(BOLD, true));
            if(open){
                meta.setCustomModelData(1);
            }
        }else {
            meta.displayName(Component.text(name).color(WHITE).decoration(ITALIC, false));
            meta.lore(getDescription(wearing));
        }
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
            desc.add(Component.text("[Right-click] unlock at level " + obtainableLevel).color(WHITE).decoration(ITALIC, false));
        }else{
            desc.add(Component.text("[Right-click] price: " + price).color(WHITE).decoration(ITALIC, false));
        }
        desc.add(Component.text("[Left-click] view").color(WHITE).decoration(ITALIC, false));
        desc.add(Component.text(slot.toString()).color(BLUE).decoration(ITALIC, false));
        return desc;
    }

   //TODO add translatables to everything

    public static void placeCosmetics(Player p, App a, int page){
        Inventory inv = Bukkit.getServer().createInventory(null, 54, Component.text("\uA000\uA00A").color(WHITE));
        if(a == App.WebButton){
            //TODO set website URL here
            return;
        }
        int i = (page-1)*15;
        for(Cosmetic c : Cosmetic.values()){
            if(c.slot != a.extra || c.ownsCosmetic(p)){
                continue;
            }

            if(InventoryManager.placeOnRightSlot(i, 51, 3, 1, 0) != null) {
                inv.setItem(InventoryManager.placeOnRightSlot(i, 51, 3, 1, 0), c.build(null, false));
            }else{
                break;
            }
            i++;
        }
        p.openInventory(inv);
    }
    //TODO make shardcores equippable

    // 0 = false
    // 1 = true
    public static Cosmetic identifyCosmetic(ItemStack item){
        Cosmetic cos = null;
        for(Cosmetic c : Cosmetic.values()){
            if(c.build(false, false).equals(item)){
                cos = c;
                break;
            }
        }

        if(cos != null){
            return cos;
        }

        for(Cosmetic c : Cosmetic.values()){
            if(c.build(true, false).equals(item)){
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

    public static Cosmetic getShardcore(Player p){
        ArrayList<Object[]> list = LobbyDatabase.fetchCosmetics(p);
        for(Object[] o : list){
            if(Cosmetic.values()[(Integer)o[1]].slot == EquipmentSlot.HAND && ((Integer) o[2]) == 1){
                return Cosmetic.values()[(Integer)o[1]];
            }
        }
        return null;
    }

    public void clicked(ClickType click, Player p, InventoryView view){
        if(click.isRightClick()){
            if(!this.ownsCosmetic(p)){ //TODO test buying
              if(price == null){
                  return;
              }

              if(((Integer)LobbyDatabase.fetchPlayerData(p).get("money")) < price){
                  p.sendMessage(Component.text("You can't afford this cosmetic :(").color(RED));
                  return;
              }

              LobbyDatabase.addCosmetic(p, this, false);

              for(App a : App.values()){
                  if(a.extra instanceof EquipmentSlot && slot == a.extra){
                      int i = 1;
                      for(Cosmetic c : Cosmetic.values()){
                          if(Cosmetic.identifyCosmetic(view.getTopInventory().getItem(InventoryManager.placeOnRightSlot(15, 51, 3, 1, 0))).equals(c)){
                              break;
                          }
                          if(c.slot == slot){
                              i++;
                          }
                      }

                      int page = i/15;
                      placeCosmetics(p, a, page);
                  }
              }
            }
            if(this.isWearing(p)){
                p.sendEquipmentChange(p, slot, null);
            }else {
                if(slot != EquipmentSlot.HAND) {
                    p.sendEquipmentChange(p, slot, build(true, false));
                }else{
                    p.sendEquipmentChange(p, slot, build(true, true));
                }
            }
            LobbyDatabase.cosmeticSetWearing(p, this, !isWearing(p));
        }
        //TODO add view feature
    }

    //TODO make shardcores buyable
}
