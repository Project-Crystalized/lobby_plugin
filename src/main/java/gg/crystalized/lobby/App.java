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
    Litestrike(new ItemStack(Material.COAL), new NamespacedKey("crystalized", "ui/scn3/games/litestrike"), useCases.Games, Component.text("Litestrike").color(GREEN).decoration(ITALIC, false), 29,
            p -> p.teleport(LobbyConfig.litestrike_hub)),
    Knockoff(new ItemStack(Material.COAL), new NamespacedKey("crystalized", "ui/scn3/games/knockoff"), useCases.Games, Component.text("Knockoff").color(GOLD).decoration(ITALIC, false), 30,
            p -> {}), //TODO
    Crystalblitz(new ItemStack(Material.COAL), new NamespacedKey("crystalized", "ui/scn3/games/crystalblitz"), useCases.Games, Component.text("Crystal Blitz").color(LIGHT_PURPLE).decoration(ITALIC, false), 31,
            p -> {}), //TODO
    Navigator(new ItemStack(Material.COMPASS), new NamespacedKey("crystalized", "ui/scn3/games"), useCases.Menu, Component.text("Games").color(WHITE).decoration(ITALIC, false), 29,
            p -> p.openInventory(prepareInv("\uA000\uA006", 54, useCases.Games))),
    Navigator_Hotbar(new ItemStack(Material.COMPASS), new NamespacedKey("crystalized", "ui/scn3/games"), useCases.Hotbar, Component.text("Play!").color(GREEN).decoration(BOLD, true), 0,
            p -> p.openInventory(prepareInv("\uA000\uA006", 54, useCases.Games))),
    Profile(new ItemStack(Material.COAL), new NamespacedKey("crystalized", "ui/scn3/profile"), useCases.Menu, Component.text("Profile").color(WHITE).decoration(ITALIC, false), 20,
            p -> p.openInventory(prepareInv("\uA000", 54, useCases.Profile)));


    enum useCases{
        Games,
        Menu,
        Shop,
        Profile,
        Social,
        Hotbar
    }
    final ItemStack item;
    final NamespacedKey model;
    final useCases uses;
    final Component name;
    final Integer slot;
    final AppOperation op;
    App(ItemStack item, NamespacedKey model, useCases uses, Component name, Integer slot, AppOperation op){
        this.item = item;
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
        ItemStack i = item;
        ItemMeta meta = i.getItemMeta();
        meta.setItemModel(model);
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
        Inventory inv = Bukkit.getServer().createInventory(null, slots, name);
        App.buildApps(inv, use);
        return inv;
    }
}

interface AppOperation{
    void action(Player p);
}
