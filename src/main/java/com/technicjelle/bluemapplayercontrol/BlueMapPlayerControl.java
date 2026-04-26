package com.technicjelle.bluemapplayercontrol;

import com.technicjelle.bluemapplayercontrol.commands.BMPC;
import com.technicjelle.bluemapplayercontrol.placeholders.MapHideExpansion;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlueMapPlayerControl extends JavaPlugin {
	private MapHideService service;
	private BMPC executor;
	private MapHideExpansion expansion;

	@Override
	public void onEnable() {
		getLogger().info("1MB-MapHide enabled");

		service = new MapHideService(this);
		service.reload();

		executor = new BMPC(service);
		registerCommand("bmpc");
		Bukkit.getPluginManager().registerEvents(executor, this);

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			expansion = new MapHideExpansion(this, service);
			expansion.register();
			getLogger().info("PlaceholderAPI expansion registered");
		}
	}

	private void registerCommand(String name) {
		PluginCommand command = Bukkit.getPluginCommand(name);
		if (command != null) {
			command.setExecutor(executor);
			command.setTabCompleter(executor);
		} else {
			getLogger().warning("Command /" + name + " is not registered");
		}
	}

	@Override
	public void onDisable() {
		if (expansion != null) {
			expansion.unregister();
		}
		if (service != null) {
			service.cancelTimers();
		}
		getLogger().info("1MB-MapHide disabled");
	}
}
