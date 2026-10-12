package com.altingtoolbox;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Alting Toolbox",
	description = "Dims the screen and alerts you to boosts, health, prayer, poison protection, cannon upkeep and drops",
	tags = {"alt", "cannon", "combat", "notifications", "loot"},
	enabledByDefault = false
)
public class AltingToolboxPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(AltingToolboxPlugin.class);

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private AltingToolboxConfig config;
	@Inject private OverlayManager overlayManager;
	@Inject private AltingToolboxOverlay overlay;
	@Inject private AltingToolboxTextOverlay textOverlay;
	@Inject private KeyManager keyManager;
	@Inject private ItemManager itemManager;
	@Inject private AudioPlayer audioPlayer;
	@Inject private ScheduledExecutorService executor;

	private final GroundDrops groundDrops = new GroundDrops();
	private final PoisonProtection poison = new PoisonProtection();
	private final CannonActivity cannonActivity = new CannonActivity();
	private volatile ToolboxDisplay display = ToolboxDisplay.HIDDEN;
	private volatile boolean running;
	private volatile long soundGeneration;
	private boolean paused;
	private long nextSoundAt;
	private volatile long alertStartedAt;
	private Future<?> soundTask;
	private TileItem alertedDrop;
	private long pickupUndimUntil;

	private final HotkeyListener pauseListener = new HotkeyListener(() -> config.pauseKey())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() ->
			{
				if (!running) { return; }
				paused = !paused;
				refresh();
			});
		}
	};

	@Provides
	AltingToolboxConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(AltingToolboxConfig.class);
	}

	@Override
	protected void startUp()
	{
		running = true;
		alertStartedAt = System.nanoTime();
		overlayManager.add(overlay);
		overlayManager.add(textOverlay);
		keyManager.registerKeyListener(pauseListener);
		clientThread.invoke(() ->
		{
			if (!running) { return; }
			paused = false;
			clearPickupDelay();
			poison.clear();
			cannonActivity.clear();
			groundDrops.configure(config.dropIds());
			scanDrops();
			refresh();
		});
	}

	@Override
	protected void shutDown()
	{
		running = false;
		display = ToolboxDisplay.HIDDEN;
		soundGeneration++;
		overlayManager.remove(overlay);
		overlayManager.remove(textOverlay);
		keyManager.unregisterKeyListener(pauseListener);
		clientThread.invoke(() ->
		{
			if (running) { return; }
			groundDrops.clear();
			poison.clear();
			clearPickupDelay();
			cannonActivity.clear();
			stopSound();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!AltingToolboxConfig.GROUP.equals(event.getGroup())) { return; }
		clientThread.invoke(() ->
		{
			if (!running) { return; }
			if ("dropIds".equals(event.getKey()) || "dropsEnabled".equals(event.getKey()))
			{
				groundDrops.configure(config.dropIds());
				clearPickupDelay();
				scanDrops();
			}
			if ("pickupDimDelay".equals(event.getKey())) { pickupUndimUntil = 0; }
			refresh();
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			scanDrops();
			refresh();
		}
		else
		{
			display = ToolboxDisplay.HIDDEN;
			stopSound();
			groundDrops.clear();
			cannonActivity.clear();
			clearPickupDelay();
			if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
			{
				poison.clear();
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) { refresh(); }

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() == Skill.HITPOINTS || event.getSkill() == Skill.PRAYER
			|| event.getSkill() == config.combatStat().skill)
		{
			refresh();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() == VarPlayerID.POISON || event.getVarpId() == VarPlayerID.DROPCANNON
			|| event.getVarpId() == VarPlayerID.ROCKTHROWER || event.getVarpId() == VarPlayerID.OWNEDMCANNON
			|| event.getVarbitId() == VarbitID.MCANNON_DECAYED)
		{
			refresh();
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		if (config.dropsEnabled() && groundDrops.add(event.getItem(), event.getTile())) { refresh(); }
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		if (!groundDrops.remove(event.getItem())) { return; }
		if (running && !paused && config.dropsEnabled() && event.getItem() == alertedDrop)
		{
			int delay = config.pickupDimDelay();
			pickupUndimUntil = delay <= 0 ? 0 : System.nanoTime() + delay * 1_000_000_000L;
		}
		refresh();
	}

	private void clearPickupDelay()
	{
		alertedDrop = null;
		pickupUndimUntil = 0;
	}

	private void scanDrops()
	{
		groundDrops.clear();
		if (!config.dropsEnabled() || !groundDrops.isConfigured() || client.getGameState() != GameState.LOGGED_IN) { return; }
		WorldView world = client.getTopLevelWorldView();
		if (world == null) { return; }
		for (Tile[][] plane : world.getScene().getTiles())
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null || tile.getGroundItems() == null) { continue; }
					for (TileItem item : tile.getGroundItems()) { groundDrops.add(item, tile); }
				}
			}
		}
	}

	private void refresh()
	{
		Player player = client.getLocalPlayer();
		if (!running || client.getGameState() != GameState.LOGGED_IN || player == null
			|| client.getBoostedSkillLevel(Skill.HITPOINTS) <= 0 || paused)
		{
			display = ToolboxDisplay.HIDDEN;
			stopSound();
			cannonActivity.clear();
			clearPickupDelay();
			return;
		}

		List<ToolboxDisplay.Alert> alerts = new ArrayList<>();
		List<String> status = new ArrayList<>();
		int health = client.getBoostedSkillLevel(Skill.HITPOINTS);
		if (config.healthEnabled())
		{
			if (health <= config.healthThreshold())
			{
				alerts.add(new ToolboxDisplay.Alert("Restore health", config.healthColor(), config.healthStyle()));
			}
			if (config.showHealth()) { status.add("HP: " + health); }
		}

		int prayer = client.getBoostedSkillLevel(Skill.PRAYER);
		if (config.prayerEnabled())
		{
			if (prayer <= config.prayerThreshold())
			{
				alerts.add(new ToolboxDisplay.Alert("Restore prayer", config.prayerColor(), config.prayerStyle()));
			}
			if (config.showPrayer()) { status.add("Prayer: " + prayer); }
		}

		poison.update(client.getVarpValue(VarPlayerID.POISON), client.getTickCount());
		if (config.poisonEnabled())
		{
			if (!poison.isProtected())
			{
				alerts.add(new ToolboxDisplay.Alert("Drink anti-poison", config.poisonColor(), config.poisonStyle()));
			}
			if (config.showPoison()) { status.add(poison.status(client.getTickCount())); }
		}

		Skill combatStat = config.combatStat().skill;
		int boost = client.getBoostedSkillLevel(combatStat);
		if (config.boostEnabled())
		{
			if (boost <= config.boostThreshold())
			{
				alerts.add(new ToolboxDisplay.Alert("Boost " + combatStat.getName().toLowerCase(Locale.ROOT),
					config.boostColor(), config.boostStyle()));
			}
			if (config.showBoost()) { status.add(combatStat.getName() + ": " + boost); }
		}

		boolean placed = client.getVarpValue(VarPlayerID.DROPCANNON) == 4;
		int ammo = client.getVarpValue(VarPlayerID.ROCKTHROWER);
		boolean broken = client.getVarbitValue(VarbitID.MCANNON_DECAYED) == 1;
		if (config.cannonEnabled() && placed)
		{
			if (broken && config.cannonRepairAlert())
			{
				alerts.add(new ToolboxDisplay.Alert("Repair cannon", config.cannonColor(), config.cannonStyle()));
			}
			else if (config.cannonAmmoAlert() && ammo <= config.cannonThreshold())
			{
				alerts.add(new ToolboxDisplay.Alert("Reload cannon", config.cannonColor(), config.cannonStyle()));
			}
			else if (!broken && ammo > 0 && cannonActivity.isInactive(client))
			{
				alerts.add(new ToolboxDisplay.Alert("Cannon inactive", config.cannonColor(), config.cannonStyle()));
			}
			if (config.showCannon()) { status.add(0, ToolboxDisplay.cannonText(config.cannonPrefix(), ammo)); }
		}
		if (!config.cannonEnabled() || !placed || broken || ammo <= 0) { cannonActivity.clear(); }

		TileItem drop = config.dropsEnabled()
			? groundDrops.nearest(player.getWorldLocation(), config.dropRange(), config.ownDropsOnly()) : null;
		alertedDrop = drop;
		if (drop != null)
		{
			String name = Text.removeTags(itemManager.getItemComposition(drop.getId()).getName());
			alerts.add(0, new ToolboxDisplay.Alert("Pick up " + name, config.dropColor(), config.dropStyle()));
		}
		ToolboxDisplay next = new ToolboxDisplay(true, alerts, status, pickupUndimUntil);
		if (!next.hasSameAlerts(display)) { alertStartedAt = System.nanoTime(); }
		display = next;
		updateSound(drop != null);
	}

	private void updateSound(boolean hasDrop)
	{
		int volume = config.soundVolume();
		if (!hasDrop || !config.dropSound() || volume <= 0)
		{
			stopSound();
			return;
		}
		long now = System.nanoTime();
		if (now < nextSoundAt || (soundTask != null && !soundTask.isDone())) { return; }
		nextSoundAt = now + Math.max(1, config.soundInterval()) * 1_000_000_000L;
		long generation = soundGeneration;
		float gain = (float) (20 * Math.log10(Math.min(100, volume) / 100f));
		soundTask = executor.submit(() ->
		{
			if (!running || generation != soundGeneration) { return; }
			try
			{
				audioPlayer.play(Notifier.class, "notification.wav", gain);
			}
			catch (Exception ex)
			{
				log.debug("Unable to play drop alert", ex);
			}
		});
	}

	private void stopSound()
	{
		soundGeneration++;
		nextSoundAt = 0;
		if (soundTask != null)
		{
			soundTask.cancel(false);
			soundTask = null;
		}
	}

	ToolboxDisplay getDisplay() { return display; }

	long elapsedMillis() { return (System.nanoTime() - alertStartedAt) / 1_000_000; }
}
