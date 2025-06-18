package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public enum App {
    Litestrike("ui/scn3/games/litestrike", useCases.Games, new useCases[]{useCases.Navigator},Component.text("Litestrike").color(GREEN).decoration(ITALIC, false), 29, LobbyConfig.litestrike_hub),
    Knockoff("ui/scn3/games/knockoff", useCases.Games, new useCases[]{useCases.Navigator},Component.text("Knockoff").color(GOLD).decoration(ITALIC, false), 30, null), //TODO
    Crystalblitz("ui/scn3/games/crystalblitz", useCases.Games, new useCases[]{useCases.Navigator},Component.text("Crystal Blitz").color(LIGHT_PURPLE).decoration(ITALIC, false), 31,  null), //TODO
    Navigator( "ui/scn3/games", useCases.Navigator, new useCases[]{useCases.Menu, useCases.Hotbar},Component.text("Games").color(WHITE).decoration(ITALIC, false), 29,
            "\uA000\uA006"),
    Profile("ui/scn3/profile", useCases.Profile, new useCases[]{useCases.Menu}, Component.text("Profile").color(WHITE).decoration(ITALIC, false), 24,
            "\uA000\uA008"),
    Friends("ui/scn3/friends", useCases.Friends, new useCases[]{useCases.Menu, useCases.Hotbar}, Component.text("Social").color(WHITE).decoration(ITALIC, false), 30,
            "\uA000\uA005"),
    Maps("ui/scn3/maps", useCases.Map, new useCases[]{useCases.Menu, useCases.Hotbar}, Component.text("Map").color(WHITE).decoration(ITALIC, false), 31,
            "\uA000"), //TODO
    Settings("ui/scn3/settings", useCases.Settings, new useCases[]{useCases.Menu}, Component.text("Settings").color(WHITE).decoration(ITALIC, false), 20,
              "\uA000\uA009"),
    Achieve("ui/scn3/achivements", useCases.Achievements, new useCases[]{useCases.Menu, useCases.Hotbar}, Component.text("Achievements").color(WHITE).decoration(ITALIC, false), 32,
            "\uA000"),//TODO
    Shop("ui/scn3/shop", useCases.Shop, new useCases[]{useCases.Menu}, Component.text("Shop").color(WHITE).decoration(ITALIC, false), 33,
            "\uA000\uA004");


    enum useCases{
        Navigator,
        Games,
        Menu,
        Shop,
        Profile,
        Friends,
        Party,
        Map,
        Settings,
        Achievements,
        Hotbar
    }
    final String model;
    final useCases self;
    final useCases[] uses;
    final Component name;
    final Integer slot;
    final Object extra;

    App(String model,useCases self ,useCases[] uses, Component name, Integer slot, Object extra){
        this.model = model;
        this.self = self;
        this.uses = uses;
        this.name = name;
        this.slot = slot;
        this.extra = extra;
    }

    public static void buildApps(Inventory inv, useCases use){
        int i = 0;
        for(App app : App.values()){
            if(Arrays.asList(app.uses).contains(use)){
                if(use == useCases.Menu) {
                    inv.setItem(app.slot, app.build());
                }else{
                    inv.setItem(InventoryManager.placeOnRightSlot(i, 51, 4, 1, 1),app.build());
                    i++;
                }
            }
        }
    }

    public ItemStack build(){
        ItemStack i = new ItemStack(Material.COAL);
        ItemMeta meta = i.getItemMeta();
        meta.setItemModel(new NamespacedKey("crystalized", model));
        meta.displayName(name);
        i.setItemMeta(meta);
        return i;
    }

    public static App identifyApp(ItemStack item){
        App app = null;
        for(App a : App.values()){
            if(a.build().equals(item)){
                app = a;
                break;
            }
        }
        return app;
    }

    public static Inventory prepareInv(String name, int slots, useCases use){
        Inventory inv = Bukkit.getServer().createInventory(null, slots, Component.text(name).color(WHITE));
        App.buildApps(inv, use);
        return inv;
    }

    public void action(Player p){
        if(extra instanceof Location){
            p.teleport((Location)extra);
        }else if(extra instanceof String){
            p.openInventory(prepareInv((String) extra, 54, self));
        }
    }
}
