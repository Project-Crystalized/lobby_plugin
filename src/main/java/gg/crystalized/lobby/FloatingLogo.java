package gg.crystalized.lobby;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FloatingLogo {

    // one logo id per item model, shared by all viewers (ids are per-connection)
    private static final Map<String, Integer> logoIds = new HashMap<>();

    public static void startRefresh() {
        new BukkitRunnable() {
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    hideAllLogos(p);
                    showAllLogos(p);
                }
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 20 * 45, 20 * 45);
    }

    public static void showAllLogos(Player viewer) {
        showLitestrikeLogo(viewer);
        showKnockoffLogo(viewer);
        // cb logo plugs in here later, same shape
    }

    public static void hideAllLogos(Player viewer) {
        for (String key : List.copyOf(logoIds.keySet())) {
            Integer id = logoIds.get(key);
            if (id == null) {
                return;
            }
            User user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
            if (user == null) {
                return;
            }
            user.sendPacket(new WrapperPlayServerDestroyEntities(id));
        }
    }

    public static void showLitestrikeLogo(Player viewer) {
        NPCData ls = LobbyConfig.NPCs.get("ls");
        NPCData ranked = LobbyConfig.NPCs.get("lsranked");
        if (ls == null || ranked == null || ls.loc == null || ranked.loc == null) {
            return;
        }
        if (!ls.loc.getWorld().equals(ranked.loc.getWorld())) {
            return;
        }
        Location mid = ls.loc.clone().add(ranked.loc);
        mid.multiply(0.5);
        mid.setY((ls.loc.getY() + ranked.loc.getY()) / 2 + 3);
        showLogo(viewer, mid, "models/litestrike_logo");
    }

    public static void showKnockoffLogo(Player viewer) {
        NPCData ko = LobbyConfig.NPCs.get("ko");
        if (ko == null || ko.loc == null) {
            return;
        }
        Location top = ko.loc.clone();
        top.setY(top.getY() + 3);
        showLogo(viewer, top, "models/knockoff_logo");
    }

    public static void showLogo(Player viewer, Location loc, String itemModelKey) {
        if (FloodgateApi.getInstance().isFloodgatePlayer(viewer.getUniqueId())) {
            return;
        }
        int id = logoIds.computeIfAbsent(itemModelKey, k -> Nametag.EntityId++);
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("crystalized", itemModelKey));
        item.setItemMeta(meta);
        com.github.retrooper.packetevents.protocol.item.ItemStack packed =
                SpigotConversionUtil.fromBukkitItemStack(item);
        User user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
        if (user == null) {
            return;
        }
        user.sendPacket(new WrapperPlayServerSpawnEntity(id, UUID.randomUUID(), EntityTypes.ITEM_DISPLAY,
                new com.github.retrooper.packetevents.protocol.world.Location(
                        loc.getX(), loc.getY(), loc.getZ(), 0, 0),
                0, 0, new Vector3d()));
        // field numbers follow the display entity table on this version:
        // 12 scale, 15 billboard (3 = center, same as nametags), 16 full brightness, 23 item
        List<EntityData<?>> data = List.of(
                new EntityData<>(12, EntityDataTypes.VECTOR3F, new Vector3f(1, 1, 1)),
                new EntityData<>(15, EntityDataTypes.BYTE, (byte) 1),
                new EntityData<>(16, EntityDataTypes.INT, (15 << 20) | (15 << 4)),
                new EntityData<>(23, EntityDataTypes.ITEMSTACK, packed));
        user.sendPacket(new WrapperPlayServerEntityMetadata(id, data));
    }

    public static void hideLogo(Player viewer, String itemModelKey) {
    }
}
