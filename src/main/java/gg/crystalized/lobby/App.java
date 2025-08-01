package gg.crystalized.lobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.stream;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public enum App {
    Litestrike("ui/scn3/games/litestrike", useCases.Games, new useCases[]{useCases.Navigator},Component.translatable("crystalized.game.litestrike.name").color(GREEN).decoration(ITALIC, false), 29, LobbyConfig.litestrike_hub),
    Knockoff("ui/scn3/games/knockoff", useCases.Games, new useCases[]{useCases.Navigator},Component.translatable("crystalized.game.knockoff.name").color(GOLD).decoration(ITALIC, false), 30, LobbyConfig.litestrike_hub), //TODO change locs
    Crystalblitz("ui/scn3/games/crystalblitz", useCases.Games, new useCases[]{useCases.Navigator},Component.translatable("crystalized.game.crystalblitz.name").color(LIGHT_PURPLE).decoration(ITALIC, false), 31,  LobbyConfig.litestrike_hub), //TODO
    Navigator( "ui/scn3/games", useCases.Navigator, new useCases[]{useCases.Menu, useCases.Hotbar},Component.translatable("crystalized.shardcore.games.name").color(WHITE).decoration(ITALIC, false), 29,
            "\uA000\uA006"),
    Profile("ui/scn3/profile", useCases.Profile, new useCases[]{useCases.Menu}, Component.translatable("crystalized.shardcore.profile.name").color(WHITE).decoration(ITALIC, false), 24,
            "\uA000\uA008"),
    Friends("ui/scn3/friends", useCases.Friends, new useCases[]{useCases.Menu, useCases.Hotbar}, Component.translatable("crystalized.shardcore.party.name").color(WHITE).decoration(ITALIC, false), 30,
            "\uA000\uA005"),
    Maps("ui/scn3/maps", useCases.Map, new useCases[]{useCases.Menu, useCases.Hotbar}, Component.translatable("crystalized.shardcore.maps.name").color(WHITE).decoration(ITALIC, false), 31,
            "\uA000"), //TODO
    Settings("ui/scn3/settings", useCases.Settings, new useCases[]{useCases.Menu}, Component.translatable("crystalized.shardcore.settings.name").color(WHITE).decoration(ITALIC, false), 20,
              "\uA000\uA009"),
    Achieve("ui/scn3/achivements", useCases.Achievements, new useCases[]{useCases.Menu, useCases.Hotbar}, Component.translatable("crystalized.shardcore.achivements.name").color(WHITE).decoration(ITALIC, false), 32,
            "\uA000"),//TODO
    Shop("ui/scn3/shop", useCases.Shop, new useCases[]{useCases.Menu}, Component.translatable("crystalized.shardcore.shop.name").color(WHITE).decoration(ITALIC, false), 33,
            "\uA000\uA004"),
    HatsButton("ui/invisible", useCases.ShopPage, useCases.Shop, Component.translatable("crystalized.shardcore.shop.hats").color(WHITE).decoration(ITALIC, false), new int[]{28, 4, 1},
            EquipmentSlot.HEAD),
    HandButton("ui/invisible", useCases.ShopPage, useCases.Shop, Component.translatable("crystalized.shardcore.shop.handheld").color(WHITE).decoration(ITALIC, false), new int[]{37, 4, 2},
            EquipmentSlot.OFF_HAND),
    WebButton("ui/invisible", useCases.ShopPage, useCases.Shop, Component.text("Web-store").color(WHITE).decoration(ITALIC, false), new int[]{32, 3, 1},
            "put URL to Website here"), //TODO
    ShardButton("ui/invisible", useCases.ShopPage, useCases.Shop, Component.translatable("crystalized.shardcore.shop.scn3").color(WHITE).decoration(ITALIC, false), new int[]{41, 3, 2},
            EquipmentSlot.HAND),
    ScrollLeft("ui/invisible", new useCases[]{useCases.ShopPage, useCases.Friends}, Component.translatable("crystalized.shardcore.generic.scrollleft").color(WHITE).decoration(ITALIC, false), 21),
    ScrollRight("ui/invisible", new useCases[]{useCases.ShopPage, useCases.Friends}, Component.translatable("crystalized.shardcore.generic.scrollright").color(WHITE).decoration(ITALIC, false), 23),
    Back("ui/invisible", new useCases[]{useCases.ShopPage}, Component.text("Back").color(WHITE).decoration(ITALIC, false), 20),
    Requeue("ui/replay", useCases.Demand, Component.text("Requeue").color(WHITE).decoration(ITALIC, false), 7),
    BackToHub("ui/leave", useCases.Demand, Component.text("Return to Lobby").color(WHITE).decoration(ITALIC, false), 8);
    //how buttons work {top left corner, width, height}
    enum useCases{
        Navigator,
        Games,
        Menu,
        Shop,
        ShopPage,
        Profile,
        Friends,
        Party,
        Map,
        Settings,
        Achievements,
        Hotbar,
        Demand
    }
    final String model;
    useCases self;
    useCases[] uses;
    useCases use;
    final Component name;
    public Integer slot;
    int[] slots;
    Object extra;

    App(String model,useCases self ,useCases[] uses, Component name, Integer slot, Object extra){
        this.model = model;
        this.self = self;
        this.uses = uses;
        this.name = name;
        this.slot = slot;
        this.extra = extra;
    }

    App(String model,useCases self ,useCases use, Component name, int[] slots, Object extra){
        this.model = model;
        this.self = self;
        this.use = use;
        this.name = name;
        this.slots = slots;
        this.extra = extra;
    }

    App(String model, useCases[] uses, Component name, int slot){
        this.model = model;
        this.uses = uses;
        this.name = name;
        this.slot = slot;
    }

    App(String model, useCases use, Component name, int slot){
        this.model = model;
        this.use = use;
        this.name = name;
        this.slot = slot;
    }
    public static void buildApps(Inventory inv, useCases use){
        int i = 0;
        for(App app : App.values()){
            if(app.uses == null){
                continue;
            }
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
        if(this == Requeue){
            ArrayList<String> plugins = new ArrayList<>();
            stream(Bukkit.getServer().getPluginManager().getPlugins()).forEach(pl -> plugins.add(pl.getName()));
            String queue = null;
            if(plugins.contains("Litestrike")){
                queue = "litestrike";
            }else if(plugins.contains("Knockoff")){
                queue = "knockoff";
            }

            if(queue == null){
                return;
            }

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(queue);
            out.writeUTF("false");
            p.sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
            return;
        }

        if(this == BackToHub){
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("lobby");
            out.writeUTF("false");
            p.sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
        }

        if(extra instanceof Location){
            if(Lobby_plugin.getInstance().passive_mode){
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(this.toString().toLowerCase());
                out.writeUTF("true");
                p.sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
                return;
            }
            p.teleport((Location)extra);
        }else if(extra instanceof String){
            Inventory inv = prepareInv((String) extra, 54, self);
            if(this == App.Friends){
                FriendsMenu.placeFriends(p, inv);
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Party");
                out.writeUTF("members");
                p.sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
                FriendsMenu.waitingForPartyMembers.put(p, inv);
                return;
            }else if(this == App.Profile){
                InventoryManager.prepareProfile(p, inv);
            }
            p.openInventory(inv);
        }
    }
}
