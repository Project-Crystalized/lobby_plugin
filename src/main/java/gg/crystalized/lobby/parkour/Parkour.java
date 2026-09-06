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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.SulfurCube;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;
import static org.bukkit.Material.COAL;
import static org.bukkit.attribute.Attribute.*;
import static org.bukkit.potion.PotionEffectType.JUMP_BOOST;

public class Parkour {
    public static ArrayList<Parkour> parkours = new ArrayList<>();
    TextColor color;
    String name;
    Location[] checkpoints;
    Location leaderboard;
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
            if(p.checkpoints[0].equals(start)) return p;
        }
        return null;
    }

    public Entity spawnParkourStart(){
        SulfurCube e = (SulfurCube)checkpoints[0].getWorld().spawnEntity(checkpoints[0], EntityType.SULFUR_CUBE);
        e.setWander(false);
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
        return course.checkpoints[lastCheckpoint+1].equals(loc.toBlockLocation());
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
        restartData.displayName(Component.text("Return to Checkpoint"));
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

        List<EntityData<?>> data = List.of(new EntityData(0, EntityDataTypes.BYTE, 0x40));
        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(entityIdNextCheckpoint, data);
        PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(metadata);
    }
}

class Timer{
    BukkitTask task;
    int i = 0;
    int tenth = 0;
    int seconds = 0;
    int minutes = 0;
    int hours = 0;
    public Timer(){
        task = new BukkitRunnable(){

            public void run(){
                if(i == 2){
                    tenth++;
                    i = 0;
                }
                if(tenth == 10){
                    seconds++;
                    tenth = 0;
                }
                if(seconds == 60){
                    minutes++;
                    seconds = 0;
                }
                if(minutes == 60){
                    hours++;
                    minutes = 0;
                }
                i++;

                for(Player p : Bukkit.getOnlinePlayers()){
                    p.sendActionBar(Component.text(buildTimer(tenth, seconds, minutes, hours)).color(YELLOW));
                }
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 0, 1);
    }

    public static String buildTimer(int tenth, int seconds, int minutes, int hours){
        String ten = "" + tenth;
        if(ten.length() < 2){
            ten = "0" + ten;
        }
        String sec = "" + seconds;
        if(sec.length() < 2){
            sec = "0" + sec;
        }
        String min = "" + minutes;
        if(min.length() < 2){
            min = "0" + min;
        }

        String h = "" + hours;
        if(h.length() < 2){
            h = "0" + h;
        }

        return h + ":" + min + ":" + sec + ":" + ten;
    }
}

