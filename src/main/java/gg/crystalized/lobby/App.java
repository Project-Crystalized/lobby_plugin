package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public enum App {
    Litestrike(new ItemStack(Material.RAW_COPPER), new NamespacedKey("crystalized", "ui/scn3/games/litestrike"), useCases.Games, Component.text("Litestrike").color(GREEN).decoration(ITALIC, false), 29),
    Knockoff(new ItemStack(Material.RAW_COPPER), new NamespacedKey("crystalized", "ui/scn3/games/knockoff"), useCases.Games, Component.text("Knockoff").color(GOLD).decoration(ITALIC, false), 30),
    Crystalblitz(new ItemStack(Material.RAW_COPPER), new NamespacedKey("crystalized", "ui/scn3/games/crystalblitz"), useCases.Games, Component.text("Crystal Blitz").color(LIGHT_PURPLE).decoration(ITALIC, false), 31),
    Navigator(new ItemStack(Material.COMPASS), new NamespacedKey("crystalized", "ui/scn3/games"), useCases.Menu, Component.text("Games").color(WHITE).decoration(ITALIC, false), 29),
    Navigator_Hotbar(new ItemStack(Material.COMPASS), new NamespacedKey("crystalized", "ui/scn3/games"), useCases.Hotbar, Component.text("Play!").color(GREEN).decoration(BOLD, true), 0);

    enum useCases{
        Games,
        Menu,
        Shop,
        Wardrobe,
        Social,
        Hotbar
    }
    final ItemStack item;
    final NamespacedKey model;
    final useCases uses;
    final Component name;
    final Integer slot;
    App(ItemStack item, NamespacedKey model, useCases uses, Component name, Integer slot){
        this.item = item;
        this.model = model;
        this.uses = uses;
        this.name = name;
        this.slot = slot;
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
}
