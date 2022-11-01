package net.fantasyrealms.fantasynpc;

import com.github.juliarn.npc.NPCPool;
import com.github.unldenis.hologram.HologramPool;
import de.exlll.configlib.YamlConfigurations;
import lombok.Getter;
import net.fantasyrealms.fantasynpc.commands.FantasyNPCCommand;
import net.fantasyrealms.fantasynpc.config.FConfig;
import net.fantasyrealms.fantasynpc.config.NPCData;
import net.fantasyrealms.fantasynpc.constants.ConfigProperties;
import net.fantasyrealms.fantasynpc.constants.Constants;
import net.fantasyrealms.fantasynpc.manager.ConfigManager;
import net.fantasyrealms.fantasynpc.manager.FNPCManager;
import net.fantasyrealms.fantasynpc.objects.FNPC;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.exception.CommandErrorException;

import java.io.File;
import java.util.stream.Collectors;

public class FantasyNPC extends JavaPlugin {

	@Getter private static FantasyNPC instance;
	@Getter private NPCPool npcPool;
	@Getter private BukkitCommandHandler commandHandler;
	@Getter private HologramPool hologramPool;
	@Getter private FConfig pluginConfig;
	@Getter private NPCData npcData;
	@Getter private static BukkitAudiences ADVENTURE;
	public static final MiniMessage MINIMESSAGE = MiniMessage.miniMessage();

	@Override
	public void onEnable() {
		instance = this;
		ADVENTURE = BukkitAudiences.create(this);

		Constants.LOGO.forEach(getLogger()::info);

		getLogger().info("Loading config...");
		pluginConfig = YamlConfigurations.update(new File(this.getDataFolder(), "config.yml").toPath(), FConfig.class, ConfigProperties.CONFIG);
		npcData = ConfigManager.loadNPCData();

		getLogger().info("Loading commands...");
		commandHandler = BukkitCommandHandler.create(this);

		commandHandler.getAutoCompleter().registerParameterSuggestions(FNPC.class, (args, sender, command) -> npcData.getNpcs().values().stream().map(FNPC::getName).collect(Collectors.toUnmodifiableSet()));
		commandHandler.getAutoCompleter().registerSuggestion("npcNames", (args, sender, command) -> {
			return npcData.getNpcs().values().stream().map(FNPC::getName).collect(Collectors.toList());
		});
		commandHandler.registerValueResolver(FNPC.class, context -> {
			String value = context.pop();
			if (npcData.getNpcs().values().stream().noneMatch(npc -> npc.getName().equalsIgnoreCase(value))) {
				throw new CommandErrorException("Invalid NPC: &e" + value);
			}
			return npcData.getNpcs().values().stream().filter(npc -> npc.getName().equalsIgnoreCase(value)).findFirst().get();
		});

		commandHandler.setHelpWriter((command, actor) -> String.format("&8• &e/%s %s &7- &f%s", command.getPath().toRealString(), command.getUsage(), command.getDescription()));
		commandHandler.register(new FantasyNPCCommand());
		commandHandler.enableAdventure(ADVENTURE);

		getLogger().info("Loading Holograms...");
		this.hologramPool = new HologramPool(this, pluginConfig.getHologram().getSpawnDistance(),
				pluginConfig.getHologram().getMinHitDistance(), pluginConfig.getHologram().getMaxHitDistance());

		getLogger().info("Loading NPCs...");
		this.npcPool = NPCPool.builder(this)
				.spawnDistance(pluginConfig.getNpc().getSpawnDistance())
				.actionDistance(pluginConfig.getNpc().getActionDistance())
				.tabListRemoveTicks(pluginConfig.getNpc().getTabListRemoveTicks())
				.build();
		FNPCManager.loadNPC(npcPool);


		getLogger().info("Thank you for using FantasyNPC!");
	}

	public static void debug(String message) {
		if (FantasyNPC.getInstance().getPluginConfig().isDebug())
			FantasyNPC.getInstance().getLogger().info("[DEBUG] %s".formatted(message));
	}
}
