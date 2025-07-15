package gg.crystalized.lobby;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class FriendsMenu {
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
            meta.displayName(Component.text(name).color(WHITE).decoration(ITALIC, false));
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
        if(click.isLeftClick()) {
            String name = ((TextComponent)item.getItemMeta().displayName()).content();
            App.Profile.action((Player)Bukkit.getOfflinePlayer(name));
        }else if(click.isRightClick()){

        }
        //TODO do everything here (party invs, friend removing)
    }
}
