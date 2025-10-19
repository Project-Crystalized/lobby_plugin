package gg.crystalized.lobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.papermc.paper.entity.TeleportFlag;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Arrays.stream;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public enum App {
    Litestrike("ui/scn3/games/litestrike", useCases.Games, new useCases[]{useCases.Navigator},Component.translatable("crystalized.game.litestrike.name").color(GREEN).decoration(ITALIC, false), 29, LobbyConfig.Locations.get("litestrike_hub")),
    Knockoff("ui/scn3/games/knockoff", useCases.Games, new useCases[]{useCases.Navigator},Component.translatable("crystalized.game.knockoff.name").color(GOLD).decoration(ITALIC, false), 30, LobbyConfig.Locations.get("knockoff_hub")),
    Crystalblitz("ui/scn3/games/crystalblitz", useCases.Games, new useCases[]{useCases.Navigator},Component.translatable("crystalized.game.crystalblitz.name").color(LIGHT_PURPLE).decoration(ITALIC, false), 31,  LobbyConfig.Locations.get("crystalblitz_hub")),
    Navigator( "ui/scn3/games", useCases.Navigator, new useCases[]{useCases.Menu, useCases.Hotbar},Component.translatable("crystalized.shardcore.games.name").color(WHITE).decoration(ITALIC, false), 29,
            "\uA000\uA006"),
    Profiles("ui/scn3/profile", useCases.Profiles, new useCases[]{useCases.Menu}, Component.translatable("crystalized.shardcore.profile.name").color(WHITE).decoration(ITALIC, false), 24,
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
    ScrollLeft("ui/invisible", useCases.UI, Component.translatable("crystalized.shardcore.generic.scrollleft").color(WHITE).decoration(ITALIC, false), null),
    ScrollRight("ui/invisible", useCases.UI, Component.translatable("crystalized.shardcore.generic.scrollright").color(WHITE).decoration(ITALIC, false), null),
    ScrollUp("ui/invisible", useCases.UI, Component.translatable("scroll up").color(WHITE).decoration(ITALIC, false), null),
    ScrollDown("ui/invisible", useCases.UI, Component.translatable("scroll down").color(WHITE).decoration(ITALIC, false), null),
    Back("ui/invisible", useCases.UI, Component.text("Back").color(WHITE).decoration(ITALIC, false), null),
    Home("ui/invisible", useCases.UI, Component.translatable("Home").color(WHITE).decoration(ITALIC, false), null),
    Requeue("ui/replay", useCases.Demand, Component.text("Requeue").color(WHITE).decoration(ITALIC, false), 7),
    BackToHub("ui/leave", useCases.Demand, Component.text("Return to Lobby").color(WHITE).decoration(ITALIC, false), 8),
    MsgSetting("ui/scn3/settings/msg", useCases.Set, new useCases[]{useCases.Settings}, Component.text("Allow direct messages").color(WHITE).decoration(ITALIC, false), 30, "dms"),
    FriendRequestSetting("ui/scn3/settings/friend_requests", useCases.Set, new useCases[]{useCases.Settings}, Component.text("Allow friend requests").color(WHITE).decoration(ITALIC, false), 39, "friends_requests"),
    PartyRequestSetting("ui/scn3/settings/party_requests", useCases.Set, new useCases[]{useCases.Settings}, Component.text("Allow party invites").color(WHITE).decoration(ITALIC, false), 48, "party_requests"),
    PigHuntSetting("ui/scn3/settings/pig_hunt", useCases.Set, new useCases[]{useCases.Settings}, Component.text("Toggle pig hunt").color(WHITE).decoration(ITALIC, false), 41, "pig_game"),
    PlayerHeightSetting("ui/scn3/settings/player_height", useCases.Set, new useCases[]{useCases.Settings}, Component.text("Change player height").color(WHITE).decoration(ITALIC, false), 50, "height"),
    PlayerVisibilitySetting("ui/scn3/settings/player_visibility", useCases.Set, new useCases[]{useCases.Settings}, Component.text("Show other players").color(WHITE).decoration(ITALIC, false), 32, "show_players"),
    SettingsToggleOff("ui/scn3/settings/toggle_off", useCases.Set, new useCases[]{useCases.Demand}, Component.text("Off").color(RED).decoration(ITALIC, false), 0,0),
    SettingsToggleOn("ui/scn3/settings/toggle_on", useCases.Set, new useCases[]{useCases.Demand}, Component.text("On").color(GREEN).decoration(ITALIC, false), 0, 1),
    SettingsToggleMid("ui/scn3/settings/toggle_mid", useCases.Set, new useCases[]{useCases.Demand}, Component.text("Mid").color(YELLOW).decoration(ITALIC, false), 0, 0.5),
    ProfileScrollRight("scn3/profile/info/right", useCases.Profiles, Component.text("Right").color(WHITE).decoration(ITALIC, false), 21),
    ProfileScrollLeft("scn3/profile/info/left", useCases.Profiles, Component.text("Left").color(WHITE).decoration(ITALIC, false), 24),
    ProfileLsStats("scn3/profile/info/ls", useCases.Demand, Component.text("Litestrike Statistics").color(WHITE).decoration(ITALIC, false), 0),
    ProfileKoStats("scn3/profile/info/ko", useCases.Demand, Component.text("KnockOff Statistics").color(WHITE).decoration(ITALIC, false), 0),
    ProfileCbStats("scn3/profile/info/ko", useCases.Demand, Component.text("CrystalBlitz Statistics").color(WHITE).decoration(ITALIC, false), 0);


    //how buttons work {top left corner, width, height}
    enum useCases{
        Navigator,
        Games,
        Menu,
        Shop,
        ShopPage,
        Profiles,
        Friends,
        Map,
        Settings,
        Set,
        Achievements,
        Hotbar,
        UI,
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

    App(String model, useCases[] uses, Component name, Integer slot){
        this.model = model;
        this.uses = uses;
        this.name = name;
        this.slot = slot;
    }

    App(String model, useCases use, Component name, Integer slot){
        this.model = model;
        this.use = use;
        this.name = name;
        this.slot = slot;
    }
    public static void buildApps(Inventory inv, useCases use, Player p){
        for(App app : App.values()){
            if(app.uses == null){
                continue;
            }
            if(Arrays.asList(app.uses).contains(use)){
                inv.setItem(app.slot, Setting.addDescription(app, p));
                Setting.placeToggle(p, app, inv);
            }
        }
    }

    public ItemStack build(){
        ItemStack i = new ItemStack(Material.COAL);
        ItemMeta meta = i.getItemMeta();
        meta.setItemModel(new NamespacedKey("crystalized", model));
        meta.displayName(name);
        i.setItemMeta(meta);
        this.addPDC(i);
        return i;
    }

    private void addPDC(ItemStack item){
        NamespacedKey key = new NamespacedKey("crystalized", "app");
        String s;
        if(this == Litestrike){
            s = "litestrike";
        }else if(this == Knockoff){
            s = "knockoff";
        }else if(this == Crystalblitz){
            s = "crystalblitz";
        } else {
            s = "";
        }
        Consumer<PersistentDataContainer> c = pdc -> pdc.set(key, PersistentDataType.STRING, s);
        item.editPersistentDataContainer(c);
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
        UITemplates.createUI(inv, use);
        buildApps(inv, use, p);
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
            }else if(plugins.contains("Crystal_blitz")){
                queue = "crystalblitz";
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
            p.teleport((Location)extra, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        }else if(extra instanceof String){
            Inventory inv = prepareInv((String) extra, 54, self, p);
            if(this == App.Friends){
                FriendsMenu.placeFriends(p, inv);
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Party");
                out.writeUTF("members");
                p.sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
                FriendsMenu.waitingForPartyMembers.put(p, inv);
                return;
            }else if(this == App.Profiles){
                Profile.prepareProfile(p, inv, p);
            }
            p.openInventory(inv);
        }
    }

    enum UITemplates{
        normal(null, null, null, null, null, null),
        shop(21, 23,null, null, null,20),
        friends(21, 23, null, null, 20, null),
        settings(null, null, 20, 29, 47, null),
        profile(null, null, null, null, 51, null);
        final Integer scrollLeft;
        final Integer scrollRight;
        final Integer scrollUp;
        final Integer scrollDown;
        final Integer home;
        final Integer goBack;
        UITemplates(Integer scrollLeft, Integer scrollRight, Integer scrollUp, Integer scrollDown, Integer home, Integer goBack){
            this.scrollLeft = scrollLeft;
            this.scrollRight = scrollRight;
            this.scrollUp = scrollUp;
            this.scrollDown = scrollDown;
            this.home = home;
            this.goBack = goBack;
        }

        public static void createUI(Inventory inv, useCases use){
            if(use == useCases.ShopPage){
                shop.placeUI(inv);
            }else if(use == useCases.Friends){
                friends.placeUI(inv);
            }else if(use == useCases.Settings){
                settings.placeUI(inv);
            }else if(use == useCases.Profiles){
                profile.placeUI(inv);
            }else{
                normal.placeUI(inv);
            }
        }
        public void placeUI(Inventory inv){
            if(scrollLeft != null){
                inv.setItem(scrollLeft, App.ScrollLeft.build());
            }
            if(scrollRight != null){
                inv.setItem(scrollRight, App.ScrollRight.build());
            }
            if(scrollUp != null){
                inv.setItem(scrollUp, App.ScrollUp.build());
            }
            if(scrollDown != null){
                inv.setItem(scrollDown, App.ScrollDown.build());
            }
            if(home != null){
                inv.setItem(home, App.Home.build());
            }
            if(goBack != null){
                inv.setItem(goBack, App.Back.build());
            }
        }
    }
}
