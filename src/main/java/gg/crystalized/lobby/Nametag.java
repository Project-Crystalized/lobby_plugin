package gg.crystalized.lobby;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.entity.EntityType.TEXT_DISPLAY;
import static org.bukkit.entity.TextDisplay.TextAlignment.CENTER;

public class Nametag {
    Player holder;
    //0 = levels & money, 1 = name, 2 = rank
    Component[] components = new Component[3];
    int[] displayIds = new int[3];
    int[] armorIds = new int[3];
    ArrayList<Nametag> tooFarAway = new ArrayList<>();
    BukkitTask locationChecker = null;
    static ArrayList<Nametag> nametags = new ArrayList<>();
    public static int EntityId = 1000000;
    public Nametag(Player holder) {
        this.holder = holder;
        components[0] = Component.text("lvl: " + holder.getLevel()).color(GREEN).decoration(ITALIC, false).append(Component.text("   " + "[m]: " + LevelManager.getMoney(holder)).color(LIGHT_PURPLE).decoration(ITALIC, false));
        components[1] = Ranks.getColoredName(holder);
        components[2] = Ranks.getRankWithName(holder);

        for(int i = 0; i < 3; i++) {
            displayIds[i] = EntityId;
            makeDisplay(false, holder, i);
            EntityId++;
            armorIds[i] = EntityId;
            makeArmorStand(false, holder, i);
            setPassengers(false, holder, i);
            EntityId++;
        }
        nametags.add(this);
    }

    public Nametag(Player holder, Player recipient,Component[] components){
        //for API
        this.holder = holder;
        this.components = components;
        this.displayIds = new int[components.length];
        this.armorIds = new int[components.length];

        for(int i = 0; i < components.length; i++) {
            displayIds[i] = EntityId;
            makeDisplay(false, holder, i);
            EntityId++;
            armorIds[i] = EntityId;
            makeArmorStand(false, holder, i);
            setPassengers(false, holder, i);
            EntityId++;
        }
        nametags.add(this);
    }

    private void renderNametag(Player recipient){
        for(int i = 0; i < components.length; i++){
            makeDisplay(true, recipient, i);
            makeArmorStand(true, recipient, i);
            setPassengers(true, recipient, i);
        }
    }

    public static void renderAllNametags(Player p){
        for(Nametag tag : nametags){
            tag.renderNametag(p);
        }
    }

    private static void sendToEveryoneApartFrom(Player p, PacketWrapper<?> wrapper){
        for(Player player : Bukkit.getOnlinePlayers()){
            if (p.equals(player)) continue;
            PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(wrapper);
        }
    }

    public static Nametag getNametag(Player p){
        for(Nametag tag : nametags){
            if(tag.holder.equals(p)){
                return tag;
            }
        }
        return null;
    }

    public static void reloadNametag(Player p){
        removeNametag(p);
        renderAllNametags(p);
        Nametag tag = new Nametag(p);
        if(tag.locationChecker != null){
            return;
        }

        tag.locationChecker = new BukkitRunnable(){
            public void run(){
                tag.locationChecker();
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 20, 20);
    }

    public void locationChecker(){
        for(Player p : Bukkit.getOnlinePlayers()){
            int maxDistance = 30;
            Nametag tag = getNametag(p);
            if(p.getLocation().distance(holder.getLocation()) <= maxDistance){
                if(!tooFarAway.contains(tag)) continue;
                tooFarAway.remove(tag);
                renderNametag(holder);
                continue;
            }
            if(tooFarAway.contains(tag)) continue;
            tooFarAway.add(tag);
            User user = PacketEvents.getAPI().getPlayerManager().getUser(holder);
            for(int id :  ArrayUtils.addAll(tag.armorIds, tag.displayIds)) {
                WrapperPlayServerDestroyEntities wrapper = new WrapperPlayServerDestroyEntities(id);
                if (user != null) {
                    user.sendPacket(wrapper);
                }
            }
        }
    }

    public static void removeNametag(Player p){
        //removes player's own nametag for others and all nametags for the player
        Nametag remove = null;
        for(Nametag tag : nametags) {
            for (int id : ArrayUtils.addAll(tag.armorIds, tag.displayIds)) {
                WrapperPlayServerDestroyEntities wrapper = new WrapperPlayServerDestroyEntities(id);
                User user = PacketEvents.getAPI().getPlayerManager().getUser(p);
                if(tag.holder.equals(p)){
                    sendToEveryoneApartFrom(p, wrapper);
                    remove = tag;
                    continue;
                }
                if(user != null) {
                    user.sendPacket(wrapper);
                }
            }
        }
        nametags.remove(remove);
        if(remove == null){
            return;
        }
        remove.locationChecker.cancel();
    }

    //sendToPlayer = true -> packet is sent to only p
    //sendToPlayer = false -> packet is sent to everyone but p
    private void makeDisplay(boolean sendToPlayer, Player p, int i){
        WrapperPlayServerSpawnEntity entity = new WrapperPlayServerSpawnEntity(displayIds[i], UUID.randomUUID(), EntityTypes.TEXT_DISPLAY, new com.github.retrooper.packetevents.protocol.world.Location
                (holder.getLocation().getX(), holder.getLocation().getY(), holder.getLocation().getZ(), 0, 0), 0, 0, new Vector3d());
        if(sendToPlayer) PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(entity); else sendToEveryoneApartFrom(p, entity);

        Integer num = 3;
        List<EntityData<?>> data = List.of(new EntityData(15, EntityDataTypes.BYTE, num.byteValue()), new EntityData(23, EntityDataTypes.ADV_COMPONENT, components[i]));
        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(displayIds[i], data);
        if(sendToPlayer) PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(metadata); else sendToEveryoneApartFrom(p, metadata);
    }

    private void makeArmorStand(boolean sendToPlayer, Player p, int i){
        WrapperPlayServerSpawnEntity armor = new WrapperPlayServerSpawnEntity(armorIds[i], UUID.randomUUID(), EntityTypes.ARMOR_STAND, new Location
                (holder.getLocation().getX(), holder.getLocation().getY(), holder.getLocation().getZ(), 0, 0), 0, 0, new Vector3d());
        if(sendToPlayer) PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(armor); else sendToEveryoneApartFrom(p, armor);

        WrapperPlayServerUpdateAttributes attribute = new WrapperPlayServerUpdateAttributes(armorIds[i], List.of(new WrapperPlayServerUpdateAttributes.Property(Attributes.SCALE, 0.15, List.of(new WrapperPlayServerUpdateAttributes.PropertyModifier(Attributes.SCALE.getName(), 0, WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.ADDITION)))));
        if(sendToPlayer) PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(attribute); else sendToEveryoneApartFrom(p, attribute);

        List<EntityData<?>> data= List.of(new EntityData(0, EntityDataTypes.BYTE, ((Integer)0x20).byteValue()));
        WrapperPlayServerEntityMetadata meta = new WrapperPlayServerEntityMetadata(armorIds[i], data);
        if(sendToPlayer) PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(meta); else sendToEveryoneApartFrom(p, meta);
    }

    private void setPassengers(boolean sendToPlayer, Player p, int i){
        WrapperPlayServerSetPassengers passengers;
        if(i == 0){
            passengers = new WrapperPlayServerSetPassengers(holder.getEntityId(), new int[]{armorIds[i]});
        }else {
            passengers = new WrapperPlayServerSetPassengers(displayIds[i-1], new int[]{armorIds[i]});
        }
        if(sendToPlayer) PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(passengers); else sendToEveryoneApartFrom(p, passengers);
        WrapperPlayServerSetPassengers pass = new WrapperPlayServerSetPassengers(armorIds[i], new int[]{displayIds[i]});
        if(sendToPlayer) PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(pass); else sendToEveryoneApartFrom(p, pass);
    }
}
