package fish.yukiemeralis.aurora;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fish.yukiemeralis.aurora.pylons.Pylon;
import fish.yukiemeralis.aurora.pylons.PylonNetwork;
import fish.yukiemeralis.aurora.rpg.AuroraRpgStats;
import fish.yukiemeralis.aurora.rpg.RpgSkillInstance;
import fish.yukiemeralis.aurora.rpg.enums.AuroraSkill;
import fish.yukiemeralis.aurora.rpg.enums.RpgStat;
import fish.yukiemeralis.eden.Eden;
import fish.yukiemeralis.eden.command.EdenCommand;
import fish.yukiemeralis.eden.module.EdenModule;
import fish.yukiemeralis.eden.permissions.ModulePlayerData;
import fish.yukiemeralis.eden.surface2.SimpleComponentBuilder;
import fish.yukiemeralis.eden.surface2.component.GuiComponent;
import fish.yukiemeralis.eden.surface2.component.GuiItemStack;
import fish.yukiemeralis.eden.surface2.enums.DefaultClickAction;
import fish.yukiemeralis.eden.surface2.special.PagedSurfaceGui;
import fish.yukiemeralis.eden.utils.ChatUtils;
import fish.yukiemeralis.eden.utils.ChatUtils.ChatAction;
import fish.yukiemeralis.eden.utils.ItemUtils;
import fish.yukiemeralis.eden.utils.PrintUtils;

public class AuroraCommand extends EdenCommand
{
	public AuroraCommand(EdenModule parent_module)
	{
		super("aur", parent_module);

		addBranch("trees", "pylons", "item", "mob", "skills", "addsp", "stats", "track", "test");

		getBranch("item").addBranch("name", "lore");

		getBranch("pylons").addBranch("name", "material", "password", "clearpassword", "add", "remove");
		getBranch("pylons").getBranch("name").addBranch("<ALL_PYLONS>").addBranch("<NEW_NAME>");
		getBranch("pylons").getBranch("material").addBranch("<ALL_PYLONS>").addBranch("<MATERIAL>");
		getBranch("pylons").getBranch("password").addBranch("<ALL_PYLONS>");
		getBranch("pylons").getBranch("clearpassword").addBranch("<ALL_PYLONS>");
		getBranch("pylons").getBranch("add").addBranch("<ONLINE_PLAYERS>");
		getBranch("pylons").getBranch("remove").addBranch("<ONLINE_PLAYERS>");

		getBranch("track").addBranch("<ALL_STATS>");
		getBranch("test").addBranch("<INTEGER>");
	}

	@EdenCommandHandler(argsCount = 1, description = "Toggles treecapitator.", usage = "aur trees")
	public void edencommand_trees(CommandSender sender, String commandLabel, String[] args)
	{
		ModulePlayerData data = Eden.getPermissionsManager().getPlayerData((Player) sender).getModuleData("Aurora");
		//AuroraModule.getPlayerData((Player) sender).setTreeCapEnabled(!AuroraModule.getPlayerData((Player) sender).isTreeCapEnabled());
		
		if (data.toggleValue("treecapEnabled"))
		{
			PrintUtils.sendMessage(sender, "Enabled treecapitator.");
			return;
		}
			
		PrintUtils.sendMessage(sender, "Disabled treecapitator.");
	}
	
	@EdenCommandHandler(usage = "aur pylons [name | material | password | clearpassword | add | remove]", description = "Modify a pylon's information.", argsCount = 3)
	public void edencommand_pylons(CommandSender sender, String commandLabel, String[] args)
	{
		Pylon pylon;
		Player player;
		switch (args[1])
		{
			case "name":
				if (args.length < 4)
				{
					PrintUtils.sendMessage(sender, "Usage: /aur pylons name <current name> <new name>");
					return;
				}

				pylon = PylonNetwork.getPylonByNameExact(args[2]);
				
				if (pylon == null)
				{
					PrintUtils.sendMessage(sender, "Could not find a pylon by that name.");
					return;
				}

				// Passwords
				if (pylon.hasPassword())
				{
					if (!pylon.isAllowedPlayer((Player) sender))
					{
						ChatAction pylonRename = new ChatAction()
						{
							@Override
							public void run()
							{
								String input = ChatUtils.receiveResult(sender);
								ChatUtils.deleteResult(sender);

								if (!pylon.comparePassword(input))
								{
									PrintUtils.sendMessage(sender, "Incorrect password for pylon. If you do not remember your password, please contact Yuki_emeralis.");
									PrintUtils.sendMessage(sender, "It is no longer safe to enter your password.");
									return;
								}

								pylon.setName(args[3]);
								PrintUtils.sendMessage(sender, "Updated pylon name.");
							}
						};

						ChatUtils.expectChat(sender, pylonRename);
						PrintUtils.sendMessage(sender, "Please enter the password for this pylon. It is now safe to enter your password.");
						return;
					}
				}
				
				pylon.setName(args[3]);
				PrintUtils.sendMessage(sender, "Updated pylon name.");
					
				break;
			case "material":
				if (args.length < 4)
				{
					PrintUtils.sendMessage(sender, "Usage: /aur pylons material <name> <material>");
					return;
				}

				pylon = PylonNetwork.getPylonByNameExact(args[2]);
				
				if (pylon == null)
				{
					PrintUtils.sendMessage(sender, "Could not find a pylon by that name.");
					return;
				}

				// Passwords
				if (pylon.hasPassword())
				{
					if (!pylon.isAllowedPlayer((Player) sender))
					{
						ChatAction pylonMaterial = new ChatAction()
						{
							@Override
							public void run()
							{
								String input = ChatUtils.receiveResult(sender);
								ChatUtils.deleteResult(sender);

								if (!pylon.comparePassword(input))
								{
									PrintUtils.sendMessage(sender, "Incorrect password for pylon. If you do not remember your password, please contact Yuki_emeralis.");
									PrintUtils.sendMessage(sender, "It is no longer safe to enter your password.");
									return;
								}

								try {
									Material material = Material.valueOf(args[3].toUpperCase());
									if (!material.isItem())
										throw new IllegalArgumentException();
									
									pylon.setMaterial(material);
									
									PrintUtils.sendMessage(sender, "Updated pylon icon. It is no longer safe to enter your password.");
								} catch (IllegalArgumentException e) {
									PrintUtils.sendMessage(sender, "Invalid material type. See https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html for a list of valid materials. It is no longer safe to enter your password.");
									return;
								}
							}
						};

						ChatUtils.expectChat(sender, pylonMaterial);
						PrintUtils.sendMessage(sender, "Please enter the password for this pylon. It is now safe to enter your password.");
						return;
					}
				}
				
				try {
					Material material = Material.valueOf(args[3].toUpperCase());
					if (!material.isItem())
						throw new IllegalArgumentException();
					
					pylon.setMaterial(material);
					
					PrintUtils.sendMessage(sender, "Updated pylon icon.");
				} catch (IllegalArgumentException e) {
					PrintUtils.sendMessage(sender, "Invalid material type. See https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html for a list of valid materials.");
					return;
				}
				break;
			case "password":
				pylon = PylonNetwork.getPylonByNameExact(args[2]);
					
				if (pylon == null)
				{
					PrintUtils.sendMessage(sender, "Could not find a pylon by that name.");
					return;
				}

				ChatAction pylonNewpass = new ChatAction()
				{
					@Override
					public void run()
					{
						String input = ChatUtils.receiveResult(sender);
						ChatUtils.deleteResult(sender);

						pylon.setPassword(input);
						PrintUtils.sendMessage(sender, "Password set. It is no longer safe to enter your password.");
					}
				};

				if (pylon.hasPassword())
				{
					ChatAction pylonCurrentpass = new ChatAction()
					{
						@Override
						public void run()
						{
							String input = ChatUtils.receiveResult(sender);
							ChatUtils.deleteResult(sender);	

							if (!pylon.comparePassword(input))
							{
								PrintUtils.sendMessage(sender, "Incorrect password for pylon. If you do not remember your password, please contact Yuki_emeralis.");
								PrintUtils.sendMessage(sender, "It is no longer safe to enter your password.");
								return;
							}

							PrintUtils.sendMessage(sender, "Enter a new password:");
							ChatUtils.expectChat(sender, pylonNewpass);
						}
					};

					PrintUtils.sendMessage(sender, "Please enter the current password for this pylon. It is now safe to enter your password.");
					ChatUtils.expectChat(sender, pylonCurrentpass);
					return;
				}

				PrintUtils.sendMessage(sender, "Enter a new password:");
				ChatUtils.expectChat(sender, pylonNewpass);

				break;
			case "clearpassword":
				pylon = PylonNetwork.getPylonByNameExact(args[2]);
					
				if (pylon == null)
				{
					PrintUtils.sendMessage(sender, "Could not find a pylon by that name.");
					return;
				}

				ChatAction pylonCurrentpass = new ChatAction()
				{
					@Override
					public void run()
					{
						String input = ChatUtils.receiveResult(sender);
						ChatUtils.deleteResult(sender);	

						if (!pylon.comparePassword(input))
						{
							PrintUtils.sendMessage(sender, "Incorrect password for pylon. If you do not remember your password, please contact Yuki_emeralis.");
							PrintUtils.sendMessage(sender, "It is no longer safe to enter your password.");
							return;
						}

						pylon.clearPassword();
						PrintUtils.sendMessage(sender, "Cleared password. It is no longer safe to enter your password.");
					}
				};

				if (sender instanceof Player)
					if (Eden.getPermissionsManager().isElevated((Player) sender))
					{
						pylon.clearPassword();
						PrintUtils.sendMessage(sender, "Cleared password for pylon due to elevation.");
						return;
					}

				PrintUtils.sendMessage(sender, "Please enter the current password for this pylon. It is now safe to enter your password.");
				ChatUtils.expectChat(sender, pylonCurrentpass);
				break;
			case "add":
				pylon = PylonNetwork.getPylonByNameExact(args[2]);
						
				if (pylon == null)
				{
					PrintUtils.sendMessage(sender, "Could not find a pylon by that name.");
					return;
				}

				if (args.length < 4)
				{
					PrintUtils.sendMessage(sender, "A player must be specified.");
					return;
				}

				player = Bukkit.getPlayerExact(args[3]);

				if (player == null)
				{
					PrintUtils.sendMessage(sender, "Could not find a player by that name.");
					return;
				}

				if (!pylon.hasPassword())
				{
					PrintUtils.sendMessage(sender, "This pylon is public, players cannot be whitelisted.");
					return;
				}

				if (!pylon.isAllowedPlayer((Player) sender))
				{
					ChatAction pylonCurrentpass_ = new ChatAction()
					{
						@Override
						public void run()
						{
							String input = ChatUtils.receiveResult(sender);
							ChatUtils.deleteResult(sender);	

							if (!pylon.comparePassword(input))
							{
								PrintUtils.sendMessage(sender, "Incorrect password for pylon. If you do not remember your password, please contact Yuki_emeralis.");
								PrintUtils.sendMessage(sender, "It is no longer safe to enter your password.");
								return;
							}

							pylon.addAllowedPlayer(player);
							PrintUtils.sendMessage(sender, "Whitelisted player. It is no longer safe to enter your password.");
						}
					};

					ChatUtils.expectChat(sender, pylonCurrentpass_);
					PrintUtils.sendMessage(sender, "Please enter the password for this pylon. It is now safe to enter your password.");
					return;
				}

				pylon.addAllowedPlayer(player);
				PrintUtils.sendMessage(sender, "Whitelisted player. It is no longer safe to enter your password.");
				break;
			case "remove":
				pylon = PylonNetwork.getPylonByNameExact(args[2]);
							
				if (pylon == null)
				{
					PrintUtils.sendMessage(sender, "Could not find a pylon by that name.");
					return;
				}

				if (args.length < 4)
				{
					PrintUtils.sendMessage(sender, "A player must be specified.");
					return;
				}

				player = Bukkit.getPlayerExact(args[3]);

				if (player == null)
				{
					PrintUtils.sendMessage(sender, "Could not find a player by that name.");
					return;
				}

				if (!pylon.hasPassword())
				{
					PrintUtils.sendMessage(sender, "This pylon is public, players cannot be blacklisted.");
					return;
				}

				if (!pylon.isAllowedPlayer((Player) sender))
				{
					ChatAction pylonCurrentpass_ = new ChatAction()
					{
						@Override
						public void run()
						{
							String input = ChatUtils.receiveResult(sender);
							ChatUtils.deleteResult(sender);	

							if (!pylon.comparePassword(input))
							{
								PrintUtils.sendMessage(sender, "Incorrect password for pylon. If you do not remember your password, please contact Yuki_emeralis.");
								PrintUtils.sendMessage(sender, "It is no longer safe to enter your password.");
								return;
							}

							pylon.removeAllowedPlayer(player);
							PrintUtils.sendMessage(sender, "Removed player from whitelist. It is no longer safe to enter your password.");
						}
					};

					ChatUtils.expectChat(sender, pylonCurrentpass_);
					PrintUtils.sendMessage(sender, "Please enter the password for this pylon. It is now safe to enter your password.");
					return;
				}

				pylon.removeAllowedPlayer(player);
				PrintUtils.sendMessage(sender, "Removed player from whitelist. It is no longer safe to enter your password.");
				break;
			default:
				this.sendErrorMessage(sender, args[1], "pylons");
		}
	}

	// TODO This
	@EdenCommandHandler(usage = "aur item [name | lore] data <formatting>", description = "Item meta changing. This command is unsafe.", argsCount = 4)
	public void edencommand_item(CommandSender sender, String commandLabel, String[] args)
	{
		String data, formatting = "";

		switch (args[1])
		{
			case "name":
				// Single formatting:                          /aur item name Item_name lo
				// Gradient formatting w/o regular formatting: /aur item name Item_name 00FF00 FF00FF
				// Gradient formatting w/ regular formatting:  /aur item name Item_name lo 00FF00 FF00FF
				// Replace _ with spaces, and & with §

				data = args[2].replaceAll("_", " ").replaceAll("&", "§");

				if (args.length == 5) // Gradient formatting
				{
					data = "§r" + ChatUtils.of(data, args[3], args[4]);
				} else if (args.length == 6) { // Gradient formatting + regular formatting
					for (String str : args[3].split(""))
						formatting = formatting + "§" + str;

					data = "§r" + ChatUtils.of(data, args[4], args[5], formatting);
				} else { // Regular formatting
					for (String str : args[3].split(""))
						formatting = formatting + "§" + str;
					
					data = "§r" + formatting + data;
				}

				ItemUtils.applyName(((Player) sender).getInventory().getItemInMainHand(), data);
				break;
			case "lore":
				// Single formatting:            /aur item lore slot lore_lore_lore formatting
				// Gradient formatting:          /aur item lore slot lore_lore_lore hex1 hex2
				// Gradient + normal formatting: /aur item lore slot lore_lore_lore formatting hex1 hex2
				data = args[3].replaceAll("_", " ").replaceAll("&", "§");

				if (args.length == 5) { // Regular formatting
					for (String str : args[4].split(""))
						formatting = formatting + "§" + str;

					data = "§r" + formatting + data;
				} else if (args.length == 6) { // Gradient formatting
					data = "§r" + ChatUtils.of(data, args[4], args[5]);
				} else { // Gradient formatting + regular formatting
					for (String str : args[4].split(""))
						formatting = formatting + "§" + str;

					data = "§r" + ChatUtils.of(data, args[5], args[6], formatting);
				}

				ItemMeta meta = ((Player) sender).getInventory().getItemInMainHand().getItemMeta();
				List<String> lore = meta.getLore();

				if (lore == null)
					lore = new ArrayList<>();
				int slot = Integer.parseInt(args[2]);

				if (lore.size() < slot)
					for (int i = 0; i <= slot - lore.size(); i++)
						lore.add(" ");

				lore.set(slot - 1, data);
				meta.setLore(lore);
				((Player) sender).getInventory().getItemInMainHand().setItemMeta(meta);
				break;
			default:
				this.sendErrorMessage(sender, args[1], "item");
				return;
		}
	}

	@EdenCommandHandler(usage = "aur mobs <mobclass>", description = "Summons a custom mob.", argsCount = 2)
	public void edencommand_mob(CommandSender sender, String commandLabel, String[] args)
	{
		ItemStack held = ((Player) sender).getInventory().getItemInMainHand();
		ItemUtils.applyEnchantment(held, Enchantment.MENDING, 1);
		/**
		Class<? extends EntityInsentient> mobClass;
		try {
			mobClass = (Class<? extends EntityInsentient>) Class.forName("fish.yukiemeralis.aurora.mobs." + args[1]);
		} catch (ClassNotFoundException e) {
			PrintUtils.sendMessage(sender, "Could not find a custom mob named \"" + args[1] + "\".");
			return;
		}

		Location target = ((Player) sender).getTargetBlock(null, 30).getLocation().add(0, 1, 0);
		try {
			Constructor<? extends EntityInsentient> constructor = mobClass.getConstructor(Location.class);
			EntityInsentient mob = constructor.newInstance(target);

			((CraftWorld) target.getWorld()).getHandle().addEntity(mob);
			mob.setLocation(target.getX(), target.getY(), target.getZ(), 0.0f, 0.0f);
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			PrintUtils.sendMessage(sender, "Invalid constructor given to this mob.");
		}
		*/
	}

	private static final GuiItemStack CLOSE_BUTTON = SimpleComponentBuilder.build(Material.BARRIER, "§r§c§lClose", (e) -> e.getWhoClicked().closeInventory(), "§7§oClose this GUI");

	@EdenCommandHandler(usage = "aur skills", description = "Opens the skills GUI.", argsCount = 1)
	public void edencommand_skills(CommandSender sender, String commandLabel, String[] args)
	{
		if (!(sender instanceof Player))
			return;

		List<GuiComponent> data = new ArrayList<>() {{
			for (AuroraSkill skill : AuroraSkill.values())
				add(new RpgSkillInstance(skill, (Player) sender).generate());
		}};

		List<GuiComponent> topBar = new ArrayList<>();
		int points = AuroraRpgStats.getSkillPoints((Player) sender);

		topBar.add(CLOSE_BUTTON);
		GuiItemStack pointsItem = SimpleComponentBuilder.build(Material.GOLD_NUGGET, "§r§6§l" + points + " skill " + PrintUtils.plural(points, "point", "points") + " available", (e) -> {}, "§7§oSkill points are earned by leveling", "§7§oup any stat.");

		topBar.add(pointsItem);

		new PagedSurfaceGui(27, "Skills", (HumanEntity) sender, 0, data, topBar, DefaultClickAction.CANCEL, InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF)
			.display((HumanEntity) sender);
	}

	@EdenCommandHandler(usage = "aur addsp", description = "/!\\ Add a skill point.", argsCount = 1)
	public void edencommand_addsp(CommandSender sender, String commandLabel, String[] args)
	{
		ModulePlayerData data = Eden.getPermissionsManager().getPlayerData((Player) sender).getModuleData("AuroraRPG");

		data.incrementInt("skillpoints", 1);
	}

	@EdenCommandHandler(usage = "aur stats", description = "View your RPG stats.", argsCount = 1)
	public void edencommand_stats(CommandSender sender, String commandLabel, String[] args)
	{
		if (!(sender instanceof Player))
			return;

		ModulePlayerData data = Eden.getPermissionsManager().getPlayerData((Player) sender).getModuleData("AuroraRPG");

		PrintUtils.sendMessage(sender, "§e---§6=§c[ §bYour stats §c]§6=§e---");

		for (RpgStat stat : RpgStat.values())
			PrintUtils.sendMessage(sender, "§e" + stat.getFriendlyName() + "§7 lv. " + data.getInt(stat.dataName()) + " | §aExp§7: " + data.getInt(stat.expName()) + "/" + stat.getRequiredExpAtLevel(data.getInt(stat.dataName())));
	}

	@EdenCommandHandler(usage = "aur track <stat>", description = "Tracks stat progression.", argsCount = 1)
	public void edencommand_track(CommandSender sender, String commandLabel, String[] args)
	{
		if (!(sender instanceof Player))
			return;

		if (args.length == 1)
		{
			if (!AuroraRpgStats.hasBar((Player) sender))
			{
				PrintUtils.sendMessage(sender, "§cYou aren't tracking and stats right now.");
				return;
			}

			PrintUtils.sendMessage(sender, "Turned off tracking for stat §e" + AuroraRpgStats.getBarData((Player) sender).getA().getFriendlyName() + "§7.");
			AuroraRpgStats.removeBar((Player) sender);
			return;
		}

		// Clean up the old bar
		if (AuroraRpgStats.hasBar((Player) sender))
			AuroraRpgStats.removeBar((Player) sender);

		try {
			RpgStat stat = RpgStat.valueOf(args[1].toUpperCase());

			AuroraRpgStats.registerNewBar((Player) sender, stat);

			PrintUtils.sendMessage(sender, "Now tracking stat \"§e" + stat.getFriendlyName() + "§7\".");
		} catch (IllegalArgumentException e) {
			PrintUtils.sendMessage(sender, "§cInvalid stat name \"" + args[1] + "\".");
		}
	}

	@EdenCommandHandler(usage = "aur test <number>", description = "Runs a suite of tests.", argsCount = 2)
	public void edencommand_test(CommandSender sender, String commandLabel, String[] args)
	{
		try { Integer.parseInt(args[1]); } catch (NumberFormatException e) { return; }

		PrintUtils.sendMessage(sender, "Beginning test with " + args[1] + "iterations. Note that because of the luck-based nature of skills, run-to-run variance may exist. Increasing the number of iterations will decrease variance at the cost of time.");

		Player player = (Player) sender;
		Mob target = (Mob) player.getWorld().spawnEntity(player.getTargetBlockExact(10).getLocation(), EntityType.PIG);

		Event[] testEvents = new Event[] {
			new EntityDamageByEntityEvent(target, player, DamageCause.CUSTOM, new HashMap<>(), new HashMap<>()),
			new EntityDamageByEntityEvent(player, target, DamageCause.CUSTOM, new HashMap<>(), new HashMap<>()),
			new ProjectileLaunchEvent(player.launchProjectile(Arrow.class)),
			new EntityTargetEvent(target, player, TargetReason.CUSTOM),
			new PlayerRespawnEvent(player, player.getLocation(), true, false),
		};

		for (int i = 0; i < Integer.parseInt(args[1]); i++)
			for (Event e : testEvents)
				Eden.getInstance().getServer().getPluginManager().callEvent(e);
	}
}
