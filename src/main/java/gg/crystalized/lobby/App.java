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

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public enum App {
    Litestrike("ui/scn3/games/litestrike", useCases.Games, Component.text("Litestrike").color(GREEN).decoration(ITALIC, false), 29,
            p -> p.teleport(LobbyConfig.litestrike_hub)),
    Knockoff("ui/scn3/games/knockoff", useCases.Games, Component.text("Knockoff").color(GOLD).decoration(ITALIC, false), 30,
            p -> {}), //TODO
    Crystalblitz("ui/scn3/games/crystalblitz", useCases.Games, Component.text("Crystal Blitz").color(LIGHT_PURPLE).decoration(ITALIC, false), 31,
            p -> {}), //TODO
    Navigator( "ui/scn3/games", useCases.Menu, Component.text("Games").color(WHITE).decoration(ITALIC, false), 29,
            p -> p.openInventory(prepareInv("\uA000\uA006", 54, useCases.Games, p))),
    Navigator_Hotbar("ui/scn3/games", useCases.Hotbar, Component.text("Play!").color(GREEN).decoration(BOLD, true), 0,
            p -> p.openInventory(prepareInv("\uA000\uA006", 54, useCases.Games, p))),
    Profile("ui/scn3/profile", useCases.Menu, Component.text("Profile").color(WHITE).decoration(ITALIC, false), 20,
            p -> p.openInventory(prepareInv("\uA000", 54, useCases.Profile, p))), //TODO
    Friends("ui/scn3/friends", useCases.Menu, Component.text("Social").color(WHITE).decoration(ITALIC, false), 30,
            p -> p.openInventory(prepareInv("\uA000\uA005", 54, useCases.Social, p))),
    Friends_Hotbar("ui/scn3/friends", useCases.Hotbar, Component.text("Social").color(WHITE).decoration(ITALIC, false), 1,
            p -> p.openInventory(prepareInv("\uA000\uA005", 54, useCases.Social, p))),
    Maps("ui/scn3/maps", useCases.Menu, Component.text("Map").color(WHITE).decoration(ITALIC, false), 31,
            p -> p.openInventory(prepareInv("\uA000", 54, useCases.Map, p))), //TODO
    Maps_Hotbar("ui/scn3/maps", useCases.Hotbar, Component.text("Your Location").color(WHITE).decoration(ITALIC, false), 2,
            p -> p.openInventory(prepareInv("\uA000", 54, useCases.Map, p))), //TODO
    Settings("ui/scn3/settings", useCases.Menu, Component.text("Settings").color(WHITE).decoration(ITALIC, false), 24,
            p -> p.openInventory(prepareInv("\uA000", 54, useCases.Settings, p))), //TODO
    Achieve("ui/scn3/achivements", useCases.Menu, Component.text("Achievements").color(WHITE).decoration(ITALIC, false), 32,
            p -> p.openInventory(prepareInv("\uA000", 54, useCases.Achievements, p))),//TODO
    Achieve_Hotbar("ui/scn3/achivements", useCases.Hotbar, Component.text("Achievements").color(WHITE).decoration(ITALIC, false), 3,
            p -> p.openInventory(prepareInv("\uA000", 54, useCases.Achievements, p))),//TODO
    Shop("ui/scn3/shop", useCases.Menu, Component.text("Shop").color(WHITE).decoration(ITALIC, false), 33,
            p -> p.openInventory(prepareInv("\uA000\uA004", 54, useCases.Shop, p)));


    enum useCases{
        Games,
        Menu,
        Shop,
        Profile,
        Social,
        Map,
        Settings,
        Achievements,
        Hotbar
    }
    final String model;
    final useCases uses;
    final Component name;
    final Integer slot;
    final AppOperation op;
    App(String model, useCases uses, Component name, Integer slot, AppOperation op){
        this.model = model;
        this.uses = uses;
        this.name = name;
        this.slot = slot;
        this.op = op;
    }

    public static void buildApps(Inventory inv, useCases use){
        for(App app : App.values()){
            if(app.uses == use){
                inv.setItem(app.slot, app.build());
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

    public static Inventory prepareInv(String name, int slots, useCases use, Player p){
        Inventory inv = Bukkit.getServer().createInventory(null, slots, Component.text(name).color(WHITE));
        App.buildApps(inv, use);
        if(use == useCases.Shop){
            Cosmetic.placeCosmetics(inv, p);
        }
        return inv;
    }
}

interface AppOperation{
    void action(Player p);
}
