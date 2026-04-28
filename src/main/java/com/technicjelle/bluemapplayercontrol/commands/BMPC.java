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
	private static final int PAGE_SIZE = 7;
	private static final List<String> SELECTORS = List.of("@a", "@p", "@r", "@s");
	private static final List<CommandInfo> COMMAND_INFOS = List.of(
			new CommandInfo("/bmpc", "Legacy self toggle.", "maphide.player, maphide.player.toggle", "maphide.player.toggle"),
			new CommandInfo("/bmpc help [page]", "Shows paged command help.", "maphide.player, maphide.player.help", "maphide.player.help"),
			new CommandInfo("/bmpc info", "Shows plugin info and current basic settings.", "maphide.player, maphide.player.info", "maphide.player.info"),
			new CommandInfo("/bmpc toggle", "Toggles your own BlueMap visibility.", "maphide.player, maphide.player.toggle", "maphide.player.toggle"),
			new CommandInfo("/bmpc show", "Shows your own BlueMap marker.", "maphide.player, maphide.player.show", "maphide.player.show"),
			new CommandInfo("/bmpc hide", "Hides your own BlueMap marker.", "maphide.player, maphide.player.hide", "maphide.player.hide"),
			new CommandInfo("/map hide", "Configurable alias for /bmpc toggle.", "maphide.player, maphide.player.toggle", "maphide.player.toggle"),
			new CommandInfo("/bmpc toggle <player>", "Toggles another online player.", "maphide.admin.toggle", "maphide.admin.toggle"),
			new CommandInfo("/bmpc show <player>", "Shows another online player.", "maphide.admin.show", "maphide.admin.show"),
			new CommandInfo("/bmpc hide <player>", "Hides another online player.", "maphide.admin.hide", "maphide.admin.hide"),
			new CommandInfo("/bmpc status", "Shows legacy server diagnostics.", "maphide.admin.status", "maphide.admin.status"),
			new CommandInfo("/bmpc status <player>", "Shows visibility, forced state, world, coordinates, and timer.", "maphide.admin.status", "maphide.admin.status"),
			new CommandInfo("/bmpc config [page]", "Lists config values.", "maphide.admin.config", "maphide.admin.config"),
			new CommandInfo("/bmpc config set <key> <value>", "Updates a config key and reloads.", "maphide.admin.set", "maphide.admin.set"),
			new CommandInfo("/bmpc reload", "Reloads config and translations.", "maphide.admin.reload", "maphide.admin.reload"),
			new CommandInfo("/bmpc debug [page]", "Lists debug categories.", "maphide.admin.debug", "maphide.admin.debug"),
			new CommandInfo("/bmpc debug status", "Shows server, plugin, and MapHide setting diagnostics.", "maphide.admin.debug.status", "maphide.admin.debug.status"),
			new CommandInfo("/bmpc debug commands [page]", "Lists command routes and permission gates.", "maphide.admin.debug.commands", "maphide.admin.debug.commands"),
			new CommandInfo("/bmpc debug permissions [page]", "Lists permission nodes and sender state.", "maphide.admin.debug.permissions", "maphide.admin.debug.permissions"),
			new CommandInfo("/bmpc debug placeholders [page]", "Lists PlaceholderAPI placeholders.", "maphide.admin.debug.placeholders", "maphide.admin.debug.placeholders")
	);
	private static final List<CommandInfo> DEBUG_COMMANDS = List.of(
			new CommandInfo("/bmpc debug status", "Server, plugin, and MapHide setting diagnostics.", "maphide.admin.debug.status", "maphide.admin.debug.status"),
			new CommandInfo("/bmpc debug commands [page]", "Command routes and permission gates.", "maphide.admin.debug.commands", "maphide.admin.debug.commands"),
			new CommandInfo("/bmpc debug permissions [page]", "Permission nodes and sender state.", "maphide.admin.debug.permissions", "maphide.admin.debug.permissions"),
			new CommandInfo("/bmpc debug placeholders [page]", "PlaceholderAPI registration and placeholder list.", "maphide.admin.debug.placeholders", "maphide.admin.debug.placeholders")
	);
	private static final List<PermissionInfo> PERMISSION_INFOS = List.of(
			new PermissionInfo("maphide.player", "true", "Allows access to /bmpc player commands."),
			new PermissionInfo("maphide.player.help", "true", "Allows /bmpc help."),
			new PermissionInfo("maphide.player.info", "true", "Allows /bmpc info."),
			new PermissionInfo("maphide.player.toggle", "true", "Allows /bmpc, /bmpc toggle, and the configured alias."),
			new PermissionInfo("maphide.player.show", "true", "Allows /bmpc show."),
			new PermissionInfo("maphide.player.hide", "true", "Allows /bmpc hide."),
			new PermissionInfo("maphide.forcehide", "false", "Forces the player hidden when forced permissions are enabled."),
			new PermissionInfo("maphide.forceshow", "false", "Forces the player visible when forced permissions are enabled."),
			new PermissionInfo("maphide.admin", "op", "Parent admin permission."),
			new PermissionInfo("maphide.admin.toggle", "op", "Allows /bmpc toggle <player>."),
			new PermissionInfo("maphide.admin.show", "op", "Allows /bmpc show <player>."),
			new PermissionInfo("maphide.admin.hide", "op", "Allows /bmpc hide <player>."),
			new PermissionInfo("maphide.admin.status", "op", "Allows /bmpc status and /bmpc status <player>."),
			new PermissionInfo("maphide.admin.config", "op", "Allows /bmpc config."),
			new PermissionInfo("maphide.admin.set", "op", "Allows /bmpc config set <key> <value>."),
			new PermissionInfo("maphide.admin.reload", "op", "Allows /bmpc reload."),
			new PermissionInfo("maphide.admin.debug", "op", "Parent debug permission."),
			new PermissionInfo("maphide.admin.debug.status", "op", "Allows /bmpc debug status."),
			new PermissionInfo("maphide.admin.debug.commands", "op", "Allows /bmpc debug commands."),
			new PermissionInfo("maphide.admin.debug.permissions", "op", "Allows /bmpc debug permissions."),
			new PermissionInfo("maphide.admin.debug.placeholders", "op", "Allows /bmpc debug placeholders.")
	);
	private static final List<PlaceholderInfo> PLACEHOLDER_INFOS = List.of(
			new PlaceholderInfo("%maphide_visible%", "true, false, or unknown."),
			new PlaceholderInfo("%maphide_state%", "visible, hidden, or unknown."),
			new PlaceholderInfo("%maphide_forced%", "Whether the player has a force permission."),
			new PlaceholderInfo("%maphide_force_mode%", "hide, show, or none."),
			new PlaceholderInfo("%maphide_default_visibility%", "Current configured default: visible or hidden."),
			new PlaceholderInfo("%maphide_toggle_back_seconds%", "Configured auto-toggle seconds."),
			new PlaceholderInfo("%maphide_toggle_back_remaining%", "Seconds left on the player's active timer."),
			new PlaceholderInfo("%maphide_language%", "Active language code.")
	);
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
			case "help" -> handleHelp(sender, args);
			case "info" -> handleInfo(sender);
			case "status" -> handleStatus(sender, args);
			case "config" -> handleConfig(sender, args);
			case "debug" -> handleDebug(sender, args);
			case "reload" -> handleReload(sender);
			default -> {
				service.send(sender, "usage-bmpc");
				yield true;
			}
		};
	}

	private boolean handleHelp(CommandSender sender, String[] args) {
		if (!sender.hasPermission("maphide.player.help")) {
			service.send(sender, "no-permission");
			return true;
		}
		int page = parsePage(sender, args, 1);
		if (page < 0) {
			return true;
		}
		sendCommandPage(sender, "Command help", COMMAND_INFOS.stream()
				.filter(commandInfo -> canView(sender, commandInfo))
				.toList(), page, "/bmpc help");
		return true;
	}

	private boolean handleInfo(CommandSender sender) {
		if (!sender.hasPermission("maphide.player.info")) {
			service.send(sender, "no-permission");
			return true;
		}
		service.send(sender, "info-header");
		service.send(sender, "info-plugin", Map.of(
				"plugin_version", service.buildInfo("pluginVersion", service.buildInfo("version", "unknown")),
				"build_number", service.buildInfo("buildNumber", "unknown"),
				"target_java", service.buildInfo("javaTarget", "unknown"),
				"target_paper", service.buildInfo("paperTarget", "unknown")
		));
		service.send(sender, "info-settings", Map.of(
				"language", service.language(),
				"alias", service.toggleAliasLabel(),
				"default_visibility", service.defaultVisible() ? "show" : "hide",
				"toggle_back", Integer.toString(service.toggleBackSeconds()),
				"forced", Boolean.toString(service.forcedPermissionsEnabled())
		));
		service.send(sender, "info-help");
		return true;
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
		if (args.length > 2) {
			service.send(sender, "usage-bmpc");
			return true;
		}

		int page = parsePage(sender, args, 1);
		if (page < 0) {
			return true;
		}
		List<Map.Entry<String, Object>> values = service.configValues().entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.toList();
		sendConfigPage(sender, values, page);
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

	private boolean handleDebug(CommandSender sender, String[] args) {
		if (args.length == 1 || (args.length == 2 && isInteger(args[1]))) {
			if (!hasAnyDebugPermission(sender)) {
				service.send(sender, "no-permission");
				return true;
			}
			int page = parsePage(sender, args, 1);
			if (page < 0) {
				return true;
			}
			sendCommandPage(sender, "Debug commands", DEBUG_COMMANDS.stream()
					.filter(commandInfo -> canViewDebug(sender, commandInfo.visiblePermission()))
					.toList(), page, "/bmpc debug");
			return true;
		}

		String subCommand = args[1].toLowerCase(Locale.ROOT);
		return switch (subCommand) {
			case "status" -> handleDebugStatus(sender);
			case "commands" -> handleDebugCommands(sender, args);
			case "permissions" -> handleDebugPermissions(sender, args);
			case "placeholders" -> handleDebugPlaceholders(sender, args);
			default -> {
				service.send(sender, "usage-debug");
				yield true;
			}
		};
	}

	private boolean handleDebugStatus(CommandSender sender) {
		if (!canViewDebug(sender, "maphide.admin.debug.status")) {
			service.send(sender, "no-permission");
			return true;
		}
		sendServerStatus(sender);
		service.send(sender, "debug-status-settings", Map.of(
				"language", service.language(),
				"alias", service.toggleAliasLabel(),
				"default_visibility", service.defaultVisible() ? "show" : "hide",
				"toggle_back", Integer.toString(service.toggleBackSeconds()),
				"forced", Boolean.toString(service.forcedPermissionsEnabled())
		));
		service.send(sender, "debug-status-force", Map.of(
				"hide_node", service.forceHidePermission(),
				"show_node", service.forceShowPermission(),
				"conflict_priority", service.conflictPriority()
		));
		return true;
	}

	private boolean handleDebugCommands(CommandSender sender, String[] args) {
		if (!canViewDebug(sender, "maphide.admin.debug.commands")) {
			service.send(sender, "no-permission");
			return true;
		}
		int page = parsePage(sender, args, 2);
		if (page < 0) {
			return true;
		}
		sendCommandPage(sender, "Command routes", COMMAND_INFOS, page, "/bmpc debug commands");
		return true;
	}

	private boolean handleDebugPermissions(CommandSender sender, String[] args) {
		if (!canViewDebug(sender, "maphide.admin.debug.permissions")) {
			service.send(sender, "no-permission");
			return true;
		}
		int page = parsePage(sender, args, 2);
		if (page < 0) {
			return true;
		}
		sendPermissionPage(sender, PERMISSION_INFOS, page, "/bmpc debug permissions");
		return true;
	}

	private boolean handleDebugPlaceholders(CommandSender sender, String[] args) {
		if (!canViewDebug(sender, "maphide.admin.debug.placeholders")) {
			service.send(sender, "no-permission");
			return true;
		}
		int page = parsePage(sender, args, 2);
		if (page < 0) {
			return true;
		}
		service.send(sender, "debug-placeholders-state", Map.of(
				"placeholderapi", Boolean.toString(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
		));
		sendPlaceholderPage(sender, PLACEHOLDER_INFOS, page, "/bmpc debug placeholders");
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
			addIfPermitted(sender, options, "help", "maphide.player.help");
			addIfPermitted(sender, options, "info", "maphide.player.info");
			addIfPermitted(sender, options, "status", "maphide.admin.status");
			addIfPermitted(sender, options, "config", "maphide.admin.config", "maphide.admin.set");
			addIfPermitted(sender, options, "debug", "maphide.admin.debug",
					"maphide.admin.debug.status",
					"maphide.admin.debug.commands",
					"maphide.admin.debug.permissions",
					"maphide.admin.debug.placeholders");
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

		if (args.length == 2 && args[0].equalsIgnoreCase("debug") && hasAnyDebugPermission(sender)) {
			List<String> options = new ArrayList<>();
			addIfDebugPermitted(sender, options, "status", "maphide.admin.debug.status");
			addIfDebugPermitted(sender, options, "commands", "maphide.admin.debug.commands");
			addIfDebugPermitted(sender, options, "permissions", "maphide.admin.debug.permissions");
			addIfDebugPermitted(sender, options, "placeholders", "maphide.admin.debug.placeholders");
			return filter(options, args[1]);
		}

		if (args.length == 3 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("set")
				&& sender.hasPermission("maphide.admin.set")) {
			return filter(new ArrayList<>(service.configValues().keySet()), args[2]);
		}

		if (args.length == 3 && args[0].equalsIgnoreCase("debug")
				&& List.of("commands", "permissions", "placeholders").contains(args[1].toLowerCase(Locale.ROOT))) {
			return filter(List.of("1", "2", "3"), args[2]);
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

	private void addIfDebugPermitted(CommandSender sender, List<String> options, String option, String permission) {
		if (canViewDebug(sender, permission)) {
			options.add(option);
		}
	}

	private boolean canView(CommandSender sender, CommandInfo commandInfo) {
		return commandInfo.visiblePermission().isBlank() || sender.hasPermission(commandInfo.visiblePermission());
	}

	private boolean canViewDebug(CommandSender sender, String permission) {
		return sender.hasPermission("maphide.admin.debug") || sender.hasPermission(permission);
	}

	private boolean hasAnyDebugPermission(CommandSender sender) {
		return sender.hasPermission("maphide.admin.debug")
				|| sender.hasPermission("maphide.admin.debug.status")
				|| sender.hasPermission("maphide.admin.debug.commands")
				|| sender.hasPermission("maphide.admin.debug.permissions")
				|| sender.hasPermission("maphide.admin.debug.placeholders");
	}

	private int parsePage(CommandSender sender, String[] args, int pageIndex) {
		if (args.length <= pageIndex) {
			return 1;
		}
		try {
			int page = Integer.parseInt(args[pageIndex]);
			if (page <= 0) {
				throw new NumberFormatException();
			}
			return page;
		} catch (NumberFormatException exception) {
			service.send(sender, "invalid-page", Map.of("page", args[pageIndex]));
			return -1;
		}
	}

	private boolean isInteger(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException exception) {
			return false;
		}
	}

	private PageBounds bounds(int totalItems, int requestedPage) {
		int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) PAGE_SIZE));
		int page = Math.min(Math.max(1, requestedPage), totalPages);
		int start = Math.min((page - 1) * PAGE_SIZE, totalItems);
		int end = Math.min(start + PAGE_SIZE, totalItems);
		return new PageBounds(page, totalPages, start, end);
	}

	private void sendCommandPage(CommandSender sender, String title, List<CommandInfo> commands, int page, String pageCommand) {
		PageBounds bounds = bounds(commands.size(), page);
		sendPageHeader(sender, title, bounds);
		for (int i = bounds.start(); i < bounds.end(); i++) {
			CommandInfo commandInfo = commands.get(i);
			service.send(sender, "command-line", Map.of(
					"index", Integer.toString(i + 1),
					"command", commandInfo.command(),
					"description", commandInfo.description(),
					"permission", commandInfo.permission()
			));
		}
		sendPageFooter(sender, pageCommand, bounds);
	}

	private void sendConfigPage(CommandSender sender, List<Map.Entry<String, Object>> values, int page) {
		PageBounds bounds = bounds(values.size(), page);
		sendPageHeader(sender, "Config values", bounds);
		for (int i = bounds.start(); i < bounds.end(); i++) {
			Map.Entry<String, Object> entry = values.get(i);
			service.send(sender, "config-line", Map.of(
					"index", Integer.toString(i + 1),
					"key", entry.getKey(),
					"value", String.valueOf(entry.getValue())
			));
		}
		sendPageFooter(sender, "/bmpc config", bounds);
	}

	private void sendPermissionPage(CommandSender sender, List<PermissionInfo> permissions, int page, String pageCommand) {
		PageBounds bounds = bounds(permissions.size(), page);
		sendPageHeader(sender, "Permission debug", bounds);
		for (int i = bounds.start(); i < bounds.end(); i++) {
			PermissionInfo permissionInfo = permissions.get(i);
			service.send(sender, "permission-line", Map.of(
					"index", Integer.toString(i + 1),
					"permission", permissionInfo.permission(),
					"default", permissionInfo.defaultValue(),
					"has", Boolean.toString(sender.hasPermission(permissionInfo.permission())),
					"description", permissionInfo.description()
			));
		}
		sendPageFooter(sender, pageCommand, bounds);
	}

	private void sendPlaceholderPage(CommandSender sender, List<PlaceholderInfo> placeholders, int page, String pageCommand) {
		PageBounds bounds = bounds(placeholders.size(), page);
		sendPageHeader(sender, "Placeholder debug", bounds);
		for (int i = bounds.start(); i < bounds.end(); i++) {
			PlaceholderInfo placeholderInfo = placeholders.get(i);
			service.send(sender, "placeholder-line", Map.of(
					"index", Integer.toString(i + 1),
					"placeholder", placeholderInfo.placeholder(),
					"description", placeholderInfo.description()
			));
		}
		sendPageFooter(sender, pageCommand, bounds);
	}

	private void sendPageHeader(CommandSender sender, String title, PageBounds bounds) {
		service.send(sender, "page-header", Map.of(
				"title", title,
				"page", Integer.toString(bounds.page()),
				"pages", Integer.toString(bounds.totalPages())
		));
	}

	private void sendPageFooter(CommandSender sender, String pageCommand, PageBounds bounds) {
		if (bounds.totalPages() <= 1) {
			return;
		}
		int next = bounds.page() >= bounds.totalPages() ? bounds.totalPages() : bounds.page() + 1;
		service.send(sender, "page-footer", Map.of(
				"command", pageCommand,
				"next", Integer.toString(next)
		));
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

	private record CommandInfo(String command, String description, String permission, String visiblePermission) {
	}

	private record PermissionInfo(String permission, String defaultValue, String description) {
	}

	private record PlaceholderInfo(String placeholder, String description) {
	}

	private record PageBounds(int page, int totalPages, int start, int end) {
	}
}
