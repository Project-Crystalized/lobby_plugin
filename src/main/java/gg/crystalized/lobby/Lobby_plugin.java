package gg.crystalized.lobby;

import org.bukkit.plugin.java.JavaPlugin;

public final class Lobby_plugin extends JavaPlugin {

	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
    this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:main");
	}

	@Override
	public void onDisable() {}

	public static Lobby_plugin getInstance() {
		return getPlugin(Lobby_plugin.class);
	}
}
