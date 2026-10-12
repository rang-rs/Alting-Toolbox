package com.altingtoolbox;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;

final class GroundDrops
{
	private final Map<TileItem, Tile> items = new IdentityHashMap<>();
	private Set<Integer> ids = Set.of();

	void configure(String text)
	{
		Set<Integer> parsed = new HashSet<>();
		for (String token : text.split("[,;\\s]+"))
		{
			try
			{
				int id = Integer.parseInt(token);
				if (id > 0) { parsed.add(id); }
			}
			catch (NumberFormatException ignored) { }
		}
		ids = parsed;
		clear();
	}

	boolean isConfigured() { return !ids.isEmpty(); }

	boolean add(TileItem item, Tile tile)
	{
		if (!ids.contains(item.getId())) { return false; }
		items.put(item, tile);
		return true;
	}

	boolean remove(TileItem item) { return items.remove(item) != null; }

	void clear() { items.clear(); }

	TileItem nearest(WorldPoint player, int range, boolean ownOnly)
	{
		TileItem nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (Map.Entry<TileItem, Tile> entry : items.entrySet())
		{
			TileItem item = entry.getKey();
			int owner = item.getOwnership();
			boolean mine = owner == TileItem.OWNERSHIP_SELF || owner == TileItem.OWNERSHIP_GROUP;
			if (item.getQuantity() <= 0 || (ownOnly && !mine) || (item.isPrivate() && !mine)) { continue; }
			int distance = player.distanceTo(entry.getValue().getWorldLocation());
			if (distance <= range && (distance < nearestDistance
				|| (distance == nearestDistance && nearest != null && item.getId() < nearest.getId())))
			{
				nearest = item;
				nearestDistance = distance;
			}
		}
		return nearest;
	}
}
