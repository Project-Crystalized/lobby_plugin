package gg.crystalized.lobby;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.player.AbstractChatEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.FireworkMeta;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static net.kyori.adventure.text.Component.text;

import java.util.HashMap;
import java.util.Set;


public final class PlayerListener implements Listener {
	private LobbyChatRenderer chat_renderer = new LobbyChatRenderer();
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		// NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER,
		// "Litestrike!");
		// npc.spawn(new Location(Bukkit.getWorld("world"), -10.5, -60, -20.5, -90, 0));
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();

		if(!LobbyDatabase.isPlayerInDatabase(p)){
			LobbyDatabase.makeNewLobbyPlayersEntry(p);
			LobbyDatabase.addCosmetic(p, Cosmetic.BLUE_SHARDCORE, true);
			LobbyDatabase.makeNewSettingsEntry(p);
			//TODO Tutorial here maybe?
		}

		e.joinMessage(Ranks.getJoinMessage(p));

		if(Lobby_plugin.getInstance().passive_mode){
			return;
		}

		ScoreboardManager.SetScoreboard(p);
		p.teleport(LobbyConfig.Locations.get("spawn"));
		p.addPotionEffect(
				new PotionEffect(PotionEffectType.HUNGER, PotionEffect.INFINITE_DURATION, 1, false, false, true));
		p.setGameMode(GameMode.ADVENTURE);
		p.getInventory().clear();
		LobbyDatabase.updatePlayerNames(p);
		LobbyDatabase.updateSkin(p);
		LobbyDatabase.updatePlayerNames(p);
		LobbyDatabase.updateSkin(p);

		Ranks.renderNameTags(p);
		Ranks.renderTabList(p);

		LevelManager.updateLevel(p);
		HashMap<String, Object> map = LobbyDatabase.fetchAndDeleteTemporaryData(p);
		if(map.get("xp_amount") != null) {
			LevelManager.giveExperience(p, (Integer) map.get("xp_amount"));
		}
		if(map.get("money_amount") != null) {
			LevelManager.giveMoney(p, (Integer) map.get("money_amount"));
		}
		InventoryManager.giveLobbyItems(p);
		for(Cosmetic c : Cosmetic.values()){
			if(c.isWearing(p) && c.slot != EquipmentSlot.HAND){
				p.sendEquipmentChange(p, c.slot, c.build(true, false));
			}
		}
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
		if(Lobby_plugin.getInstance().passive_mode){
			return;
		}
		e.setCancelled(true);
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if(Lobby_plugin.getInstance().passive_mode){
			return;
		}
		e.setCancelled(true);
	}

	@EventHandler
	public void onRightclick(PlayerInteractEntityEvent e) {
	Player player = e.getPlayer();
		if(Lobby_plugin.getInstance().passive_mode){
			return;
		}
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
		if(Lobby_plugin.getInstance().passive_mode){
			return;
		}
		if (!CitizensAPI.getNPCRegistry().isNPC(e.getEntity())) {
			return;
		}
		NPC npc = CitizensAPI.getNPCRegistry().getNPC(e.getEntity());
		World w = e.getDamager().getWorld();
		for(NPCData data : LobbyConfig.NPCs.values()){
			if(!data.loc.equals(e.getEntity().getLocation())){
				continue;
			}
			data.action((Player) e.getDamager());
		}

	}

	@EventHandler
	public void onHunger(FoodLevelChangeEvent event) {
		if(Lobby_plugin.getInstance().passive_mode){
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler
	public void onChat(AsyncChatEvent e){
		if(Lobby_plugin.getInstance().passive_mode){
			return;
		}
		e.renderer(ChatRenderer.viewerUnaware(chat_renderer));
	}
}
class LobbyChatRenderer implements ChatRenderer.ViewerUnaware{

	@Override
	public Component render(Player player, Component displayName, Component message) {
		Component name = Ranks.getName(player);
		return name.append(Component.text(": ")).append(message);
	}
}
