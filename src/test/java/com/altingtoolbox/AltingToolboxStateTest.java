package com.altingtoolbox;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AltingToolboxStateTest
{
	@Mock private Client client;
	@Mock private ClientThread clientThread;
	@Mock(answer = Answers.CALLS_REAL_METHODS) private AltingToolboxConfig config;
	@Mock private OverlayManager overlayManager;
	@Mock private AltingToolboxOverlay overlay;
	@Mock private AltingToolboxTextOverlay textOverlay;
	@Mock private KeyManager keyManager;
	@Mock private ItemManager itemManager;
	@Mock private AudioPlayer audioPlayer;
	@Mock private ScheduledExecutorService executor;
	@Mock private Player player;
	private Future<?> soundTask;
	@InjectMocks private AltingToolboxPlugin plugin;
	private AutoCloseable mocks;

	@Before
	public void setUp()
	{
		mocks = MockitoAnnotations.openMocks(this);
		soundTask = mock(Future.class);
		doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; })
			.when(clientThread).invoke(any(Runnable.class));
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(99);
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(99);
		when(client.getBoostedSkillLevel(Skill.STRENGTH)).thenReturn(118);
		when(client.getVarpValue(VarPlayerID.POISON)).thenReturn(-20);
		doReturn(soundTask).when(executor).submit(any(Runnable.class));
		plugin.startUp();
	}

	@After
	public void tearDown() throws Exception
	{
		plugin.shutDown();
		mocks.close();
	}

	@Test
	public void boostAlertPersistsUntilTheLevelActuallyRecovers()
	{
		when(client.getBoostedSkillLevel(Skill.STRENGTH)).thenReturn(109);
		tick();
		assertEquals(List.of("Boost strength"), messages());
		tick();
		assertEquals(List.of("Boost strength"), messages());
		when(client.getBoostedSkillLevel(Skill.STRENGTH)).thenReturn(118);
		tick();
		assertTrue(messages().isEmpty());
		assertTrue(plugin.getDisplay().visible);
	}

	@Test
	public void prayerThresholdIsInclusiveAndRestoringClearsIt()
	{
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(7);
		tick();
		assertEquals(List.of("Restore prayer"), messages());
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(8);
		tick();
		assertTrue(messages().isEmpty());
	}

	@Test
	public void healthThresholdIsInclusiveAndHealingClearsItImmediately()
	{
		when(config.healthThreshold()).thenReturn(40);
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(40);
		plugin.onStatChanged(new StatChanged(Skill.HITPOINTS, 0, 99, 40));
		assertEquals(List.of("Restore health"), messages());
		tick();
		assertEquals(List.of("Restore health"), messages());
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(41);
		plugin.onStatChanged(new StatChanged(Skill.HITPOINTS, 0, 99, 41));
		assertTrue(messages().isEmpty());
	}

	@Test
	public void healthCanBeDisabledAndDeathHidesTheOverlay()
	{
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(30);
		tick();
		assertEquals(List.of("Restore health"), messages());
		when(config.healthEnabled()).thenReturn(false);
		tick();
		assertTrue(messages().isEmpty());
		when(config.healthEnabled()).thenReturn(true);
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(0);
		tick();
		assertFalse(plugin.getDisplay().visible);
	}

	@Test
	public void beingCuredWithoutImmunityStillRequiresAntipoison()
	{
		when(client.getVarpValue(VarPlayerID.POISON)).thenReturn(0);
		tick();
		assertEquals(List.of("Drink anti-poison"), messages());
		when(client.getVarpValue(VarPlayerID.POISON)).thenReturn(-1);
		tick();
		assertTrue(messages().isEmpty());
	}

	@Test
	public void cannonOnlyAlertsWhilePlacedAndClearsOnRepairAndReload()
	{
		tick();
		assertTrue(messages().isEmpty());
		when(client.getVarpValue(VarPlayerID.DROPCANNON)).thenReturn(4);
		tick();
		assertEquals(List.of("Reload cannon"), messages());
		when(client.getVarbitValue(VarbitID.MCANNON_DECAYED)).thenReturn(1);
		tick();
		assertEquals(List.of("Repair cannon"), messages());
		when(client.getVarbitValue(VarbitID.MCANNON_DECAYED)).thenReturn(0);
		when(client.getVarpValue(VarPlayerID.ROCKTHROWER)).thenReturn(30);
		tick();
		assertTrue(messages().isEmpty());
		assertEquals(List.of("30"), plugin.getDisplay().status);
		when(client.getVarpValue(VarPlayerID.DROPCANNON)).thenReturn(0);
		tick();
		assertTrue(plugin.getDisplay().status.isEmpty());
	}

	@Test
	public void loadedCannonWarnsUntilItRotatesAndRespectsTheCannonToggle()
	{
		DynamicObject renderable = placeStoppedCannon();
		tick();
		assertTrue(messages().isEmpty());
		when(client.getTickCount()).thenReturn(2);
		tick();
		assertEquals(List.of("Cannon inactive"), messages());
		Animation turn = mock(Animation.class);
		when(turn.getId()).thenReturn(AnimationID.MCANNON_N_TURN);
		when(renderable.getAnimation()).thenReturn(turn);
		tick();
		assertTrue(messages().isEmpty());
		when(client.getTickCount()).thenReturn(100);
		tick();
		assertTrue(messages().isEmpty());
		assertEquals(List.of("30"), plugin.getDisplay().status);
		when(renderable.getAnimation()).thenReturn(null);
		tick();
		when(client.getTickCount()).thenReturn(102);
		tick();
		assertEquals(List.of("Cannon inactive"), messages());
		when(config.cannonEnabled()).thenReturn(false);
		tick();
		assertTrue(messages().isEmpty());
	}

	@Test
	public void repairAndReloadTakePrecedenceOverInactivity()
	{
		placeStoppedCannon();
		tick();
		when(client.getTickCount()).thenReturn(2);
		tick();
		assertEquals(List.of("Cannon inactive"), messages());
		when(client.getVarbitValue(VarbitID.MCANNON_DECAYED)).thenReturn(1);
		tick();
		assertEquals(List.of("Repair cannon"), messages());
		when(client.getVarbitValue(VarbitID.MCANNON_DECAYED)).thenReturn(0);
		when(client.getVarpValue(VarPlayerID.ROCKTHROWER)).thenReturn(0);
		tick();
		assertEquals(List.of("Reload cannon"), messages());
		when(client.getVarpValue(VarPlayerID.DROPCANNON)).thenReturn(0);
		tick();
		assertTrue(messages().isEmpty());
	}

	private DynamicObject placeStoppedCannon()
	{
		DynamicObject renderable = mock(DynamicObject.class);
		CannonActivityTest.placeCannon(client, mock(WorldView.class), mock(Tile.class), mock(GameObject.class), renderable);
		when(client.getVarpValue(VarPlayerID.DROPCANNON)).thenReturn(4);
		when(client.getVarpValue(VarPlayerID.ROCKTHROWER)).thenReturn(30);
		return renderable;
	}

	@Test
	public void togglesDisableOnlyTheirOwnAlerts()
	{
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(0);
		when(client.getBoostedSkillLevel(Skill.STRENGTH)).thenReturn(99);
		when(client.getVarpValue(VarPlayerID.POISON)).thenReturn(0);
		tick();
		assertEquals(3, messages().size());
		when(config.prayerEnabled()).thenReturn(false);
		when(config.boostEnabled()).thenReturn(false);
		tick();
		assertEquals(List.of("Drink anti-poison"), messages());
	}

	@Test
	public void disablingCannonWarningsKeepsItsCount()
	{
		when(client.getVarpValue(VarPlayerID.DROPCANNON)).thenReturn(4);
		when(client.getVarbitValue(VarbitID.MCANNON_DECAYED)).thenReturn(1);
		when(config.cannonRepairAlert()).thenReturn(false);
		when(config.cannonAmmoAlert()).thenReturn(false);
		tick();
		assertTrue(messages().isEmpty());
		assertEquals(List.of("0"), plugin.getDisplay().status);
	}

	@Test
	public void pickupClearsTheVisualAndCancelsPendingSound() throws Exception
	{
		ItemSpawned drop = spawnCoins();
		assertEquals(List.of("Pick up Coins"), messages());
		ArgumentCaptor<Runnable> sound = ArgumentCaptor.forClass(Runnable.class);
		verify(executor).submit(sound.capture());
		plugin.onItemDespawned(new ItemDespawned(drop.getTile(), drop.getItem()));
		assertTrue(messages().isEmpty());
		verify(soundTask).cancel(false);
		sound.getValue().run();
		verifyNoInteractions(audioPlayer);
	}

	@Test
	public void pickupKeepsTheScreenClearForTheConfiguredDelay()
	{
		when(config.pickupDimDelay()).thenReturn(5);
		ItemSpawned drop = spawnCoins();
		long before = System.nanoTime();
		plugin.onItemDespawned(new ItemDespawned(drop.getTile(), drop.getItem()));
		long after = System.nanoTime();
		long deadline = plugin.getDisplay().undimUntilNanos;
		assertTrue(deadline >= before + 5_000_000_000L);
		assertTrue(deadline <= after + 5_000_000_000L);
		assertTrue(messages().isEmpty());
		tick();
		assertEquals(deadline, plugin.getDisplay().undimUntilNanos);
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(7);
		tick();
		assertEquals(List.of("Restore prayer"), messages());
	}

	@Test
	public void zeroPickupDelayDimsImmediately()
	{
		when(config.pickupDimDelay()).thenReturn(0);
		ItemSpawned drop = spawnCoins();
		plugin.onItemDespawned(new ItemDespawned(drop.getTile(), drop.getItem()));
		assertEquals(0, plugin.getDisplay().undimUntilNanos);
	}

	@Test
	public void leavingDropRangeDoesNotStartPickupDelay()
	{
		spawnCoins();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3300, 3300, 0));
		tick();
		assertTrue(messages().isEmpty());
		assertEquals(0, plugin.getDisplay().undimUntilNanos);
	}

	@Test
	public void worldHopClearsPickupDelay()
	{
		ItemSpawned drop = spawnCoins();
		plugin.onItemDespawned(new ItemDespawned(drop.getTile(), drop.getItem()));
		assertTrue(plugin.getDisplay().undimUntilNanos > 0);
		GameStateChanged change = new GameStateChanged();
		change.setGameState(GameState.HOPPING);
		plugin.onGameStateChanged(change);
		tick();
		assertEquals(0, plugin.getDisplay().undimUntilNanos);
	}

	@Test
	public void dropSoundUsesTheConfiguredVolume() throws Exception
	{
		when(config.soundVolume()).thenReturn(50);
		spawnCoins();
		ArgumentCaptor<Runnable> sound = ArgumentCaptor.forClass(Runnable.class);
		verify(executor).submit(sound.capture());
		sound.getValue().run();
		verify(audioPlayer).play(eq(Notifier.class), eq("notification.wav"), floatThat(gain -> Math.abs(gain + 6.0206f) < 0.001f));
	}

	@Test
	public void worldHopDiscardsOldDrops()
	{
		spawnCoins();
		when(client.getGameState()).thenReturn(GameState.HOPPING);
		GameStateChanged change = new GameStateChanged();
		change.setGameState(GameState.HOPPING);
		plugin.onGameStateChanged(change);
		assertFalse(plugin.getDisplay().visible);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		tick();
		assertTrue(messages().isEmpty());
	}

	@Test
	public void hotkeyPausesAlertsAndUnpausingRechecksThem()
	{
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(0);
		tick();
		ArgumentCaptor<HotkeyListener> listener = ArgumentCaptor.forClass(HotkeyListener.class);
		verify(keyManager).registerKeyListener(listener.capture());
		listener.getValue().hotkeyPressed();
		assertFalse(plugin.getDisplay().visible);
		listener.getValue().hotkeyPressed();
		assertEquals(List.of("Restore prayer"), messages());
	}

	@Test
	public void shutdownRemovesOverlaysAndSuppressesQueuedSound()
	{
		spawnCoins();
		ArgumentCaptor<Runnable> sound = ArgumentCaptor.forClass(Runnable.class);
		verify(executor).submit(sound.capture());
		plugin.shutDown();
		sound.getValue().run();
		assertFalse(plugin.getDisplay().visible);
		verify(overlayManager).remove(overlay);
		verify(overlayManager).remove(textOverlay);
		verifyNoInteractions(audioPlayer);
	}

	private ItemSpawned spawnCoins()
	{
		when(config.dropIds()).thenReturn("995");
		ConfigChanged configChange = new ConfigChanged();
		configChange.setGroup(AltingToolboxConfig.GROUP);
		configChange.setKey("dropIds");
		plugin.onConfigChanged(configChange);
		TileItem item = mock(TileItem.class);
		when(item.getId()).thenReturn(995);
		when(item.getQuantity()).thenReturn(10);
		when(item.getOwnership()).thenReturn(TileItem.OWNERSHIP_SELF);
		Tile tile = mock(Tile.class);
		when(tile.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
		ItemComposition definition = mock(ItemComposition.class);
		when(definition.getName()).thenReturn("Coins");
		when(itemManager.getItemComposition(995)).thenReturn(definition);
		ItemSpawned event = new ItemSpawned(tile, item);
		plugin.onItemSpawned(event);
		return event;
	}

	private void tick() { plugin.onGameTick(new GameTick()); }

	private List<String> messages()
	{
		return plugin.getDisplay().alerts.stream().map(alert -> alert.message).collect(Collectors.toList());
	}
}
