package gg.crystalized.lobby;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class FriendsMenu {
    public static HashMap<Player, Inventory> waitingForPartyMembers = new HashMap<>();
    public static NamespacedKey key = new NamespacedKey("crystalized", "friends_menu");
    public static ItemStack buildFriend(String name, String skin, String date, int online){
        try {
            URL skinURL = new URL(skin);
            ItemStack friend = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta skull = (SkullMeta) friend.getItemMeta();
            PlayerProfile profile = (PlayerProfile) Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures texture = profile.getTextures();
            texture.setSkin(skinURL);
            profile.setTextures(texture);
            skull.setPlayerProfile(profile);
            friend.setItemMeta(skull);
            ItemMeta meta = friend.getItemMeta();
            meta.displayName(Ranks.getName(Bukkit.getOfflinePlayer(name)).decoration(ITALIC, false));
            ArrayList<Component> lore = new ArrayList<>();
            lore.add(Component.text("Friends since: " + date).color(GRAY).decoration(ITALIC, false));
            lore.add(Component.text("[Left-click] View Profile").color(YELLOW).decoration(ITALIC, false));
            lore.add(Component.text("[Right-click] Invite to party").color(YELLOW).decoration(ITALIC, false));
            lore.add(Component.text("[Shift-click] Remove from friends").color(YELLOW).decoration(ITALIC, false));
            if(online == 1) {
                lore.add(Component.text("online").color(GREEN));
            }else if(online == 0){
                lore.add(Component.text("offline").color(RED));
            }
            meta.lore(lore);
            friend.setItemMeta(meta);
            Consumer<PersistentDataContainer> c = pdc -> pdc.set(key, PersistentDataType.STRING, name);
            friend.editPersistentDataContainer(c);
            return friend;
        }catch(MalformedURLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("failed to build friend");
        }
        return null;
    }

    public static void placeFriends(Player p, Inventory inv){
        ArrayList<Object[]> list = LobbyDatabase.fetchFriends(p);
        int i = 0;
        for(Object[] o : list){
            HashMap<String, Object> data = LobbyDatabase.fetchPlayerData((byte[]) o[1]);
            ItemStack stack = buildFriend((String)data.get("player_name"), (String)data.get("skin_url"), (String) o[2], (Integer) data.get("online"));
            if(InventoryManager.placeOnRightSlot(i, 51, 3, 1, 0) != null) {
                inv.setItem(InventoryManager.placeOnRightSlot(i, 51, 3, 1, 0), stack);
            }
            i++;
        }
    }

    public static void clickedFriend(ItemStack item, Player p, ClickType click){
        String name = item.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if(click.isShiftClick()){
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Friend");
            out.writeUTF("remove");
            out.writeUTF(name);
            p.sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
            App.Friends.action(p);
        }else if(click.isLeftClick()) {
            App.Profile.action(Bukkit.getOfflinePlayer(name).getPlayer());
        }else if(click.isRightClick()){
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Party");
            out.writeUTF("invite");
            out.writeUTF(name);
            p.sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
            App.Friends.action(p);
        }
    }

    public static String removeRank(String name){
        if(!Ranks.isRankSymbol(name.charAt(0))){
            return name;
        }
        return name.substring(2);
    }

    public static void placePartyMembers(ArrayList<String> members, Player p, Inventory inv){
        int i = 0;
        while(i <= 4 && i < members.size()){
            Player pl = Bukkit.getPlayer(members.get(i));
            if(pl.equals(p) && i == 0){
                pl = null;
            }
            ItemStack item = buildPartyMember(pl, p);
            inv.setItem(i+3, item);
            i++;
        }
        p.openInventory(inv);
    }

    public static ItemStack buildPartyMember(Player p, Player viewer){
        boolean forLeader = false;
        if(p == null){
            forLeader = true;
            p = viewer;
        }
        try {
            HashMap<String, Object> data = LobbyDatabase.fetchPlayerData((p));
            URL skinURL = new URL((String)data.get("skin_url"));
            ItemStack member = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta skull = (SkullMeta) member.getItemMeta();
            PlayerProfile profile = (PlayerProfile) Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures texture = profile.getTextures();
            texture.setSkin(skinURL);
            profile.setTextures(texture);
            skull.setPlayerProfile(profile);
            member.setItemMeta(skull);
            ItemMeta meta = member.getItemMeta();
            meta.displayName(Ranks.getName(p).decoration(ITALIC, false));
            ArrayList<Component> lore = new ArrayList<>();
            lore.add(Component.text("[Left-click] View Profile").color(TextColor.fromHexString("#f299da")).decoration(ITALIC, false));
            if(!LobbyDatabase.areFriends(p, viewer)) {
                lore.add(Component.text("[Right-click] Send friend request").color(TextColor.fromHexString("#f299da")).decoration(ITALIC, false));
            }
            if(forLeader) {
                lore.add(Component.text("[Shift-click] Remove from Party").color(TextColor.fromHexString("#f299da")).decoration(ITALIC, false));
            }
            if((Integer)data.get("online") == 1) {
                lore.add(Component.text("online").color(GREEN));
            }else if((Integer)data.get("online") == 0){
                lore.add(Component.text("offline").color(RED));
            }
            meta.lore(lore);
            String name = p.getName();
            Consumer<PersistentDataContainer> c = pdc -> pdc.set(key, PersistentDataType.STRING, name);
            member.editPersistentDataContainer(c);
            member.setItemMeta(meta);
            return member;
        }catch(MalformedURLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("failed to build party member");
        }
        return null;
    }

    public static void clickedPartyMember(Player p, ItemStack item, ClickType click){
        String name = item.getPersistentDataContainer().get(key, PersistentDataType.STRING);;
        if(click.isShiftClick()){
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Party");
            out.writeUTF("remove");
            out.writeUTF(name);
            p.sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
            App.Friends.action(p);
        }else if(click.isLeftClick()) {
            App.Profile.action((Player)Bukkit.getOfflinePlayer(name));
        }else if(click.isRightClick()){
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Friend");
            out.writeUTF("add");
            out.writeUTF(name);
            p.sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
            App.Friends.action(p);
        }
    }
}
