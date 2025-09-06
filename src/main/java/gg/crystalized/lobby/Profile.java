package gg.crystalized.lobby;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
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

import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class Profile {
    public static void prepareProfile(OfflinePlayer p, Inventory inv, Player viewer){
        try {
            HashMap<String, Object> data = LobbyDatabase.fetchPlayerData(p);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL((String)data.get("skin_url")));
            profile.setTextures(textures);
            meta.setPlayerProfile(profile);
            meta.displayName(Ranks.getName(p));
            ArrayList<Component> lore = new ArrayList<>();
            lore.add(Component.text("Level: " + data.get("level")).color(GREEN).decoration(ITALIC, false));
            lore.add(Component.text("Money: " + data.get("money")).color(WHITE).decoration(ITALIC, false));
            meta.lore(lore);
            head.setItemMeta(meta);
            inv.setItem(2, head);
        }catch(MalformedURLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't set head in player profile");
        }
        /*
        for(Cosmetic c : Cosmetic.values()){
            if(c.isWearing(p) && c.ownsCosmetic(viewer)){
                inv.setItem(getCosmeticSlot(c), c.build(c.isWearing(viewer), true));
            }else{
                inv.setItem(getCosmeticSlot(c), c.build(null, true));
            }
        }

         */

        //TODO statistics (21,22,23,24)

        if(p.getPlayer() == null){
            return;
        }
        inv.setItem(3, Ranks.buildItem(p));
        ItemStack[] items = getExpItems(p.getPlayer());
        inv.setItem(4, items[0]);
        inv.setItem(5, items[1]);
        inv.setItem(6, items[2]);
    }

    public static ItemStack[] getExpItems(Player p){
        HashMap<String, Object> data = LobbyDatabase.fetchPlayerData(p);
        int level = (Integer)data.get("level");
        double expToNext = Setting.toDouble(data.get("exp_to_next_lvl"));
        long total = p.getExperiencePointsNeededForNextLevel();
        double a = (total - expToNext) / total;
        double amount = 48 * a;
        ItemStack[] items = new ItemStack[3];
        for(int i = 0; i < 3; i++){
            if(amount / 16 >= 1){
                items[i] = buildExpItem(16, level);
                amount = amount - 16;
            }else{
                items[i] = buildExpItem(Math.round(amount), level);
                amount = 0;
            }
        }
        return items;
    }

    public static ItemStack buildExpItem(long amount, int level){
        String model = "ui/scn3/profile/xp/" + amount;
        NamespacedKey key = new NamespacedKey("crystalized", model);
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(key);
        meta.displayName(Component.text("Level: " + level).color(GREEN).decoration(ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    public static int getCosmeticSlot(Cosmetic c){
        switch(c.slot){
            case EquipmentSlot.HEAD: return 38;
            case EquipmentSlot.OFF_HAND: return 39;
            case EquipmentSlot.HAND: return 40;
        }
        return 41;
    }
}
