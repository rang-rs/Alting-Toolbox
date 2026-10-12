package com.altingtoolbox;

import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.Renderable;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;

class CannonActivity
{
	private GameObject observed;
	private int stoppedSince = -1;

	boolean isInactive(Client client)
	{
		GameObject cannon = findCannon(client);
		if (cannon == null)
		{
			clear();
			return false;
		}
		Renderable renderable = cannon.getRenderable();
		Animation animation = renderable instanceof DynamicObject ? ((DynamicObject) renderable).getAnimation() : null;
		if (renderable == null || (animation != null && animation.getId() != AnimationID.CANNON_LEAGUE03_IDLE))
		{
			clear();
			return false;
		}
		int tick = client.getTickCount();
		if (observed != cannon || stoppedSince < 0 || tick < stoppedSince)
		{
			observed = cannon;
			stoppedSince = tick;
		}
		// Allow the short gap between successive turning animations.
		return tick - stoppedSince >= 2;
	}

	private GameObject findCannon(Client client)
	{
		WorldView world = client.getTopLevelWorldView();
		if (world == null) { return null; }
		WorldPoint position = WorldPoint.fromCoord(client.getVarpValue(VarPlayerID.OWNEDMCANNON));
		if (position.getPlane() != world.getPlane()) { return null; }
		int x = position.getX() - world.getBaseX();
		int y = position.getY() - world.getBaseY();
		Tile[][] tiles = world.getScene().getTiles()[position.getPlane()];
		if (x < 0 || x >= tiles.length || y < 0 || y >= tiles[x].length) { return null; }
		Tile tile = tiles[x][y];
		if (tile == null) { return null; }
		for (GameObject object : tile.getGameObjects())
		{
			if (object != null && (object.getId() == ObjectID.DWARF_MULTICANNON1
				|| object.getId() == ObjectID.LEAGUE03_MULTICANNON_ACTIVE)
				&& object.getSceneMinLocation().getX() == x && object.getSceneMinLocation().getY() == y)
			{
				return object;
			}
		}
		return null;
	}

	void clear()
	{
		observed = null;
		stoppedSince = -1;
	}
}
