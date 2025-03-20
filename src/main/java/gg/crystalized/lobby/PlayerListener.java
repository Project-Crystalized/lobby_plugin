package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static net.kyori.adventure.text.Component.text;

import java.awt.image.ImageFilter;
import java.util.HashMap;
import java.util.Map;

public final class PlayerListener implements Listener {

	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		// NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER,
		// "Litestrike!");
		// npc.spawn(new Location(Bukkit.getWorld("world"), -10.5, -60, -20.5, -90, 0));
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		e.joinMessage(text(""));
		Location spawn = new Location(Bukkit.getWorld("world"), 0, -60, 0, 180, 0);

		ScoreboardManager.SetScoreboard(p);
		p.teleport(spawn);
		p.addPotionEffect(
				new PotionEffect(PotionEffectType.HUNGER, PotionEffect.INFINITE_DURATION, 1, false, false, true));
		p.setGameMode(GameMode.ADVENTURE);
		p.getInventory().clear();
		GivePlayerSpawnItems(p);

		p.sendPlayerListHeaderAndFooter(
				// Header
				text("\nProject Crystalized Lobby\n").color(NamedTextColor.LIGHT_PURPLE),

				// Footer
				text("\ncrystalized.cc\n").color(NamedTextColor.DARK_GRAY));
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		e.quitMessage(text(""));
	}

	@EventHandler
	public void onInteractBlock(PlayerInteractEvent e) {
		e.setCancelled(true);
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		e.setCancelled(true);
	}

	@EventHandler
	public void onRightclick(PlayerInteractEntityEvent e) {
	Player player = e.getPlayer();
		if (Commands.player_pig_counters.get(player) == null) {
		Commands.player_pig_counters.put(player, 0);
		}
			if (Commands.pig_trackerA.contains(e.getRightClicked())){
				Commands.pig_trackerA.remove(e.getRightClicked());
				int i = Commands.player_pig_counters.get(player);
				i = i+1;
				Commands.player_pig_counters.put(player, i);
				e.getPlayer().getWorld().sendMessage(Component.text(e.getPlayer().getName() + " found a PIG!!!").color(NamedTextColor.DARK_AQUA));
				e.getPlayer().getWorld().sendActionBar(Component.text("You have found " + i + " pigs so far!").color(NamedTextColor.AQUA));
				e.getRightClicked().remove();
				Firework fw = e.getPlayer().getWorld().spawn(e.getPlayer().getLocation(), Firework.class);
				FireworkMeta fwm = fw.getFireworkMeta();
				fwm.setPower(2);
				FireworkEffect effect1 = FireworkEffect.builder().with(FireworkEffect.Type.BALL).withTrail()
					.withColor(Color.RED).withColor(Color.BLUE).build();
				FireworkEffect effect2 = FireworkEffect.builder().with(FireworkEffect.Type.CREEPER).withFlicker()
					.withTrail()
					.withColor(Color.GREEN).withColor(Color.AQUA).build();
				fwm.addEffects(effect1, effect2);
				fw.setFireworkMeta(fwm);
		}
		e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerHit(EntityDamageByEntityEvent e) {
		if (!CitizensAPI.getNPCRegistry().isNPC(e.getEntity())) {
			return;
		}
		NPC npc = CitizensAPI.getNPCRegistry().getNPC(e.getEntity());
		World w = e.getDamager().getWorld();
		if (npc.getName().equals("Litestrike!")) {
			e.getDamager().teleport(new Location(w, -29, -61, -112, -144, 0));
		} else if (npc.getName().equals("Litestrike")) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Connect");
			out.writeUTF("litestrike");
			((Player) e.getDamager()).sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main",
					out.toByteArray());
		} else if (npc.getName().equals("Knockoff")) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Connect");
			out.writeUTF("knockoff");
			((Player) e.getDamager()).sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main",
					out.toByteArray());
		}
	}

	@EventHandler
	public void onHunger(FoodLevelChangeEvent event) {
		event.setCancelled(true);
	}

	public void GivePlayerSpawnItems(Player p) {
		p.getInventory().clear();
		// This is the method to give players items like the shardcore nexus
	}
}
