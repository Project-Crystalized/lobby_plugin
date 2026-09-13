package gg.crystalized.lobby.parkour;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import gg.crystalized.lobby.InventoryManager;
import gg.crystalized.lobby.Lobby_plugin;
import gg.crystalized.lobby.Nametag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.bukkit.Material.COAL;
import static org.bukkit.attribute.Attribute.*;
import static org.bukkit.potion.PotionEffectType.JUMP_BOOST;

public class Parkour {
    public static ArrayList<Parkour> parkours = new ArrayList<>();
    public TextColor color;
    public String name;
    Location[] checkpoints;
    public Location leaderboard;
    Entity checkpointEntity;

    public Parkour(TextColor color, String name, Location[] checkpoints, Location leaderboard) {
        this.color = color;
        this.name = name;
        this.checkpoints = checkpoints;
        this.leaderboard = leaderboard;
        checkpointEntity = spawnParkourStart();
        parkours.add(this);

    }

    public static Parkour findParkour(Location start){
        for(Parkour p : parkours){
            if(p.checkpoints[0].getBlockX() == start.getBlockX() && p.checkpoints[0].getBlockY() == start.getBlockY() && p.checkpoints[0].getBlockZ() == start.getBlockZ()) return p;
        }
        return null;
    }

    public static Parkour findParkourByLeaderboard(Location leaderboard){
        for(Parkour p : parkours){
            if(p.leaderboard.getBlockX() == leaderboard.getBlockX() && p.leaderboard.getBlockY() == leaderboard.getBlockY() && p.leaderboard.getBlockZ() == leaderboard.getBlockZ()) return p;
        }
        return null;
    }

    public Entity spawnParkourStart(){
        ArmorStand e = (ArmorStand)checkpoints[0].getWorld().spawnEntity(checkpoints[0], EntityType.ARMOR_STAND);
        e.setMarker(true);
        return e;
    }

    public static void hideParkourStarts(Player p){
        for(Parkour parkour : parkours){
            p.hideEntity(Lobby_plugin.getInstance(), parkour.checkpointEntity);
        }
    }

    public static void showParkourStarts(Player p){
        for(Parkour parkour : parkours){
            p.showEntity(Lobby_plugin.getInstance(), parkour.checkpointEntity);
        }
    }
}

class ParkourRun{
    public static ArrayList<ParkourRun> running = new ArrayList<>();
    Player p;
    Parkour course;
    Timer timer;
    int lastCheckpoint;
    int entityIdNextCheckpoint;

    public ParkourRun(Player p, Parkour course) {
        this.p = p;
        this.course = course;
        this.lastCheckpoint = 0;
        timer = new Timer();
        entityIdNextCheckpoint = Nametag.EntityId;
        Nametag.EntityId++;
        giveItemsAndRemoveAbilities();
        showOrHideCheckpoint();
        Parkour.hideParkourStarts(p);
        running.add(this);
    }

    public static ParkourRun getRun(Player p){
        for(ParkourRun run : running){
            if(run.p.equals(p)) return run;
        }
        return null;
    }

    public boolean isNextCheckpoint(Location loc){
        return course.checkpoints[lastCheckpoint+1].getBlockX() == loc.toBlockLocation().getBlockX() && course.checkpoints[lastCheckpoint+1].getBlockY() == loc.toBlockLocation().getBlockY() && course.checkpoints[lastCheckpoint+1].getBlockZ() == loc.toBlockLocation().getBlockZ();
    }

    private void giveItemsAndRemoveAbilities(){
        ItemStack end = new ItemStack(COAL);
        ItemMeta endData = end.getItemMeta();
        endData.displayName(Component.text("End parkour"));
        end.setItemMeta(endData);

        ItemStack check = new ItemStack(COAL);
        ItemMeta checkData = check.getItemMeta();
        checkData.displayName(Component.text("Return to Checkpoint"));
        check.setItemMeta(checkData);

        ItemStack restart = new ItemStack(COAL);
        ItemMeta restartData = restart.getItemMeta();
        restartData.displayName(Component.text("Restart parkour"));
        restart.setItemMeta(restartData);

        p.getInventory().setItem(8, end);
        p.getInventory().setItem(7, restart);
        p.getInventory().setItem(6, check);

        p.setAllowFlight(false);
        AttributeInstance bounce = p.getAttribute(BOUNCINESS);
        AttributeInstance airDrag = p.getAttribute(AIR_DRAG_MODIFIER);
        AttributeInstance friction = p.getAttribute(FRICTION_MODIFIER);
        if(p.getPotionEffect(JUMP_BOOST) != null){
            bounce.removeModifier(new NamespacedKey("crystalized", "rank"));
            airDrag.removeModifier(new NamespacedKey("crystalized", "rank"));
            friction.removeModifier(new NamespacedKey("crystalized", "rank"));
            p.removePotionEffect(JUMP_BOOST);
        }
    }

    public void onCheckpoint(){
        lastCheckpoint++;
        showOrHideCheckpoint();
        if(lastCheckpoint == course.checkpoints.length-1){
            stop(true);
        }
        //TODO particles and sounds
    }

    public void stop(boolean finished){
        p.getInventory().clear();
        InventoryManager.giveLobbyItems(p);
        timer.task.cancel();
        Parkour.showParkourStarts(p);
        running.remove(this);
        if(finished) ParkourDatabase.saveRun(this);
    }

    public void showOrHideCheckpoint(){
        if(lastCheckpoint + 1 > course.checkpoints.length -1){
            WrapperPlayServerDestroyEntities wrapper = new WrapperPlayServerDestroyEntities(entityIdNextCheckpoint);
            PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(wrapper);
            return;
        }
        Location loc = course.checkpoints[lastCheckpoint+1];
        WrapperPlayServerSpawnEntity entity = new WrapperPlayServerSpawnEntity(entityIdNextCheckpoint, UUID.randomUUID(), EntityTypes.SULFUR_CUBE, new com.github.retrooper.packetevents.protocol.world.Location
                (loc.getX(), loc.getY(), loc.getZ(), 0, 0), 0, 0, new Vector3d());
        PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(entity);

        List<EntityData<?>> data = List.of(new EntityData(0, EntityDataTypes.BYTE, ((Integer)0x40).byteValue()));
        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(entityIdNextCheckpoint, data);
        PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(metadata);
    }
}

