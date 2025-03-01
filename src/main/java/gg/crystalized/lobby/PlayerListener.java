package gg.crystalized.lobby;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static net.kyori.adventure.text.Component.text;

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
		p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, PotionEffect.INFINITE_DURATION, 1, false, false, true));
		p.setGameMode(GameMode.ADVENTURE);
		p.getInventory().clear();
		GivePlayerSpawnItems(p);
		RankDisplay.update_display();

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
			((Player) e.getDamager()).sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
		} else if (npc.getName().equals("Knockoff")) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Connect");
			out.writeUTF("knockoff");
			((Player) e.getDamager()).sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
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
