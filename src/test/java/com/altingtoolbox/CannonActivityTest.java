package com.altingtoolbox;

import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.Model;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CannonActivityTest
{
	private final Client client = mock(Client.class);
	private final WorldView world = mock(WorldView.class);
	private final GameObject cannon = mock(GameObject.class);
	private final DynamicObject rotating = mock(DynamicObject.class);
	private final Tile tile = mock(Tile.class);
	private final CannonActivity activity = new CannonActivity();

	@Before
	public void setUp()
	{
		placeCannon(client, world, tile, cannon, rotating);
	}

	static void placeCannon(Client client, WorldView world, Tile tile, GameObject cannon, DynamicObject renderable)
	{
		Scene scene = mock(Scene.class);
		when(client.getTopLevelWorldView()).thenReturn(world);
		when(client.getVarpValue(VarPlayerID.OWNEDMCANNON)).thenReturn((3200 << 14) | 3200);
		when(world.getBaseX()).thenReturn(3200);
		when(world.getBaseY()).thenReturn(3200);
		when(world.getScene()).thenReturn(scene);
		Tile[][][] tiles = new Tile[4][2][2];
		tiles[0][0][0] = tile;
		when(scene.getTiles()).thenReturn(tiles);
		when(tile.getGameObjects()).thenReturn(new GameObject[]{cannon});
		when(cannon.getId()).thenReturn(ObjectID.DWARF_MULTICANNON1);
		when(cannon.getSceneMinLocation()).thenReturn(new Point(0, 0));
		when(cannon.getRenderable()).thenReturn(renderable);
	}

	@Test
	public void loadedStationaryCannonAlertsAndStartingRotationClearsIt()
	{
		when(cannon.getRenderable()).thenReturn(mock(Model.class));
		assertFalse(activity.isInactive(client));
		when(client.getTickCount()).thenReturn(2);
		assertTrue(activity.isInactive(client));
		when(cannon.getRenderable()).thenReturn(rotating);
		animate(AnimationID.MCANNON_N_TURN);
		assertFalse(activity.isInactive(client));
	}

	@Test
	public void everyTurnWithoutFiringStaysActiveRegardlessOfAmmo()
	{
		when(client.getVarpValue(VarPlayerID.ROCKTHROWER)).thenReturn(30);
		for (int id = AnimationID.MCANNON_N_TURN; id <= AnimationID.MCANNON_NW_TURN; id++)
		{
			animate(id);
			when(client.getTickCount()).thenReturn(id * 100);
			assertFalse(activity.isInactive(client));
		}
	}

	@Test
	public void temporaryAnimationGapDoesNotAlertButStoppingDoes()
	{
		animate(AnimationID.MCANNON_N_FIRE);
		assertFalse(activity.isInactive(client));
		when(rotating.getAnimation()).thenReturn(null);
		when(client.getTickCount()).thenReturn(1);
		assertFalse(activity.isInactive(client));
		when(client.getTickCount()).thenReturn(2);
		assertFalse(activity.isInactive(client));
		animate(AnimationID.MCANNON_NE_TURN);
		assertFalse(activity.isInactive(client));
		when(rotating.getAnimation()).thenReturn(null);
		assertFalse(activity.isInactive(client));
		when(client.getTickCount()).thenReturn(4);
		assertTrue(activity.isInactive(client));
	}

	@Test
	public void ornamentedIdleCannonAlertsButAimingDoesNot()
	{
		when(cannon.getId()).thenReturn(ObjectID.LEAGUE03_MULTICANNON_ACTIVE);
		animate(AnimationID.CANNON_LEAGUE03_IDLE);
		assertFalse(activity.isInactive(client));
		when(client.getTickCount()).thenReturn(2);
		assertTrue(activity.isInactive(client));
		animate(AnimationID.CANNON_LEAGUE03_AIM_N);
		assertFalse(activity.isInactive(client));
	}

	@Test
	public void missingOrOtherPlayersCannonsDoNotAlert()
	{
		assertFalse(activity.isInactive(client));
		when(client.getTickCount()).thenReturn(2);
		when(cannon.getSceneMinLocation()).thenReturn(new Point(1, 0));
		assertFalse(activity.isInactive(client));
		when(tile.getGameObjects()).thenReturn(new GameObject[0]);
		assertFalse(activity.isInactive(client));
		when(world.getBaseX()).thenReturn(4000);
		assertFalse(activity.isInactive(client));
		when(world.getBaseX()).thenReturn(3200);
		when(world.getPlane()).thenReturn(1);
		assertFalse(activity.isInactive(client));
	}

	private void animate(int id)
	{
		Animation animation = mock(Animation.class);
		when(animation.getId()).thenReturn(id);
		when(rotating.getAnimation()).thenReturn(animation);
	}
}
