package com.technicjelle.bluemapplayercontrol.commands;

import com.technicjelle.bluemapplayercontrol.MapHideService;
import com.technicjelle.bluemapplayercontrol.MapHideService.ForceMode;
import com.technicjelle.bluemapplayercontrol.MapHideService.VisibilityAction;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BMPC implements CommandExecutor, TabCompleter, Listener {
	private static final List<String> SELECTORS = List.of("@a", "@p", "@r", "@s");
	private final MapHideService service;

	public BMPC(MapHideService service) {
		this.service = service;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		if (!service.matchesToggleAlias(event.getMessage(), true)) {
			return;
		}

		event.setCancelled(true);
		if (!event.getPlayer().hasPermission("maphide.player")) {
			service.send(event.getPlayer(), "no-permission");
			return;
		}
		handleSelfAction(event.getPlayer(), "toggle");
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onServerCommand(ServerCommandEvent event) {
		if (!service.matchesToggleAlias(event.getCommand(), false)) {
			return;
		}

		event.setCancelled(true);
		service.send(event.getSender(), "player-only");
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		service.applyJoinVisibility(event.getPlayer());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("maphide.player")) {
			service.send(sender, "no-permission");
			return true;
		}

		if (args.length == 0) {
			return handleSelfAction(sender, "toggle");
		}

		String action = args[0].toLowerCase(Locale.ROOT);
		return switch (action) {
			case "toggle", "show", "hide" -> handleVisibilityAction(sender, action, args);
			case "status" -> handleStatus(sender, args);
			case "config" -> handleConfig(sender, args);
			case "reload" -> handleReload(sender);
			default -> {
				service.send(sender, "usage-bmpc");
				yield true;
			}
		};
	}

	private boolean handleVisibilityAction(CommandSender sender, String action, String[] args) {
		if (args.length == 1) {
			return handleSelfAction(sender, action);
		}
		return handleTargetAction(sender, action, args[1]);
	}

	private boolean handleSelfAction(CommandSender sender, String action) {
		if (!(sender instanceof Player player)) {
			service.send(sender, "player-only");
			return true;
		}
		if (!sender.hasPermission("maphide.player." + action)) {
			service.send(sender, "no-permission");
			return true;
		}

		return service.blueMap().map(api -> switch (action) {
			case "show" -> service.setSelfVisibility(api, player, true);
			case "hide" -> service.setSelfVisibility(api, player, false);
			default -> service.toggleSelf(api, player);
		}).orElseGet(() -> {
			service.send(sender, "bluemap-not-ready");
			return true;
		});
	}

	private boolean handleTargetAction(CommandSender sender, String action, String targetSelector) {
		if (!sender.hasPermission("maphide.admin." + action)) {
			service.send(sender, "no-permission");
			return true;
		}

		return service.blueMap().map(api -> {
			List<Player> targets = service.findTargets(sender, targetSelector);
			if (targets.isEmpty()) {
				service.send(sender, "player-not-found", Map.of("player", targetSelector));
				return true;
			}
			for (Player target : targets) {
				service.setTargetVisibility(api, sender, target, toVisibilityAction(action));
			}
			return true;
		}).orElseGet(() -> {
			service.send(sender, "bluemap-not-ready");
			return true;
		});
	}

	private boolean handleStatus(CommandSender sender, String[] args) {
		if (!sender.hasPermission("maphide.admin.status")) {
			service.send(sender, "no-permission");
			return true;
		}
		if (args.length == 1) {
			sendServerStatus(sender);
			return true;
		}

		return service.blueMap().map(api -> {
			List<Player> targets = service.findTargets(sender, args[1]);
			if (targets.isEmpty()) {
				service.send(sender, "player-not-found", Map.of("player", args[1]));
				return true;
			}
			for (Player target : targets) {
				sendStatus(sender, api, target);
			}
			return true;
		}).orElseGet(() -> {
			service.send(sender, "bluemap-not-ready");
			return true;
		});
	}

	private void sendServerStatus(CommandSender sender) {
		String blueMapVersion = service.blueMap()
				.map(BlueMapAPI::getBlueMapVersion)
				.orElse("not-ready");
		String blueMapApiVersion = service.blueMap()
				.map(BlueMapAPI::getAPIVersion)
				.orElse("not-ready");

		service.send(sender, "status-server-header");
		service.send(sender, "status-server-plugin", Map.of(
				"plugin_version", service.buildInfo("pluginVersion", service.buildInfo("version", "unknown")),
				"target_java", service.buildInfo("javaTarget", "unknown"),
				"target_paper", service.buildInfo("paperTarget", "unknown")
		));
		service.send(sender, "status-server-build", Map.of(
				"build_number", service.buildInfo("buildNumber", "unknown")
		));
		service.send(sender, "status-server-bluemap", Map.of(
				"bluemap_version", blueMapVersion,
				"bluemap_api", blueMapApiVersion
		));
		service.send(sender, "status-server-runtime", Map.of(
				"engine", Bukkit.getName(),
				"minecraft", Bukkit.getMinecraftVersion(),
				"server_version", Bukkit.getVersion()
		));
		service.send(sender, "status-server-java", Map.of(
				"java", System.getProperty("java.version", "unknown"),
				"vendor", System.getProperty("java.vendor", "unknown"),
				"vm", System.getProperty("java.vm.name", "unknown")
		));
		service.send(sender, "status-server-player-usage");
	}

	private void sendStatus(CommandSender sender, BlueMapAPI api, Player target) {
		ForceMode forceMode = service.forceMode(target);
		service.send(sender, "status", Map.of(
				"player", target.getName(),
				"visibility", api.getWebApp().getPlayerVisibility(target.getUniqueId()) ? "visible" : "hidden",
				"forced", Boolean.toString(forceMode != ForceMode.NONE),
				"force_mode", forceMode.placeholder(),
				"world", target.getWorld().getName(),
				"x", Integer.toString(target.getLocation().getBlockX()),
				"y", Integer.toString(target.getLocation().getBlockY()),
				"z", Integer.toString(target.getLocation().getBlockZ()),
				"timer", Long.toString(service.toggleBackRemaining(target.getUniqueId()))
		));
	}

	private VisibilityAction toVisibilityAction(String action) {
		return switch (action) {
			case "show" -> VisibilityAction.SHOW;
			case "hide" -> VisibilityAction.HIDE;
			default -> VisibilityAction.TOGGLE;
		};
	}

	private boolean handleConfig(CommandSender sender, String[] args) {
		if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
			return handleConfigSet(sender, args);
		}
		if (!sender.hasPermission("maphide.admin.config")) {
			service.send(sender, "no-permission");
			return true;
		}
		if (args.length != 1) {
			service.send(sender, "usage-bmpc");
			return true;
		}

		service.send(sender, "config-header");
		service.configValues().entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> service.send(sender, "config-line", Map.of(
						"key", entry.getKey(),
						"value", String.valueOf(entry.getValue())
				)));
		return true;
	}

	private boolean handleConfigSet(CommandSender sender, String[] args) {
		if (!sender.hasPermission("maphide.admin.set")) {
			service.send(sender, "no-permission");
			return true;
		}
		if (args.length < 4) {
			service.send(sender, "usage-bmpc");
			return true;
		}

		String key = args[2];
		String value = String.join(" ", List.of(args).subList(3, args.length));
		if (!service.setConfigValue(key, value)) {
			service.send(sender, "config-unknown-key", Map.of("key", key));
			return true;
		}
		service.send(sender, "config-updated", Map.of("key", key, "value", value));
		return true;
	}

	private boolean handleReload(CommandSender sender) {
		if (!sender.hasPermission("maphide.admin.reload")) {
			service.send(sender, "no-permission");
			return true;
		}
		service.reload();
		service.send(sender, "reloaded");
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!sender.hasPermission("maphide.player")) {
			return Collections.emptyList();
		}

		if (args.length == 1) {
			List<String> options = new ArrayList<>();
			addIfPermitted(sender, options, "toggle", "maphide.player.toggle");
			addIfPermitted(sender, options, "show", "maphide.player.show");
			addIfPermitted(sender, options, "hide", "maphide.player.hide");
			addIfPermitted(sender, options, "status", "maphide.admin.status");
			addIfPermitted(sender, options, "config", "maphide.admin.config", "maphide.admin.set");
			addIfPermitted(sender, options, "reload", "maphide.admin.reload");
			return filter(options, args[0]);
		}

		if (args.length == 2 && List.of("toggle", "show", "hide").contains(args[0].toLowerCase(Locale.ROOT))
				&& sender.hasPermission("maphide.admin." + args[0].toLowerCase(Locale.ROOT))) {
			return playerCompletions(args[1]);
		}

		if (args.length == 2 && args[0].equalsIgnoreCase("status") && sender.hasPermission("maphide.admin.status")) {
			return playerCompletions(args[1]);
		}

		if (args.length == 2 && args[0].equalsIgnoreCase("config") && sender.hasPermission("maphide.admin.set")) {
			return filter(List.of("set"), args[1]);
		}

		if (args.length == 3 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("set")
				&& sender.hasPermission("maphide.admin.set")) {
			return filter(new ArrayList<>(service.configValues().keySet()), args[2]);
		}

		return Collections.emptyList();
	}

	private void addIfPermitted(CommandSender sender, List<String> options, String option, String... permissions) {
		for (String permission : permissions) {
			if (sender.hasPermission(permission)) {
				options.add(option);
				return;
			}
		}
	}

	private List<String> playerCompletions(String token) {
		Set<String> completions = new LinkedHashSet<>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			completions.add(player.getName());
		}
		completions.addAll(SELECTORS);
		return filter(new ArrayList<>(completions), token);
	}

	private List<String> filter(List<String> options, String token) {
		String lowerToken = token.toLowerCase(Locale.ROOT);
		return options.stream()
				.filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lowerToken))
				.toList();
	}
}
