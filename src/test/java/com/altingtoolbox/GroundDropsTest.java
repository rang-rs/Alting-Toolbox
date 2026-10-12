package com.altingtoolbox;

import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GroundDropsTest
{
	private final GroundDrops drops = new GroundDrops();
	private final WorldPoint player = new WorldPoint(3200, 3200, 0);

	@Test
	public void tracksEachStackUntilTheLastOneLeaves()
	{
		drops.configure("995, 4151");
		Tile tile = tileAt(player);
		TileItem first = item(995);
		TileItem second = item(995);
		drops.add(first, tile);
		drops.add(second, tile);
		assertNotNull(drops.nearest(player, 20, false));
		drops.remove(first);
		assertSame(second, drops.nearest(player, 20, false));
		drops.remove(second);
		assertNull(drops.nearest(player, 20, false));
	}

	@Test
	public void respectsRangeAndPlaneAsThePlayerMoves()
	{
		drops.configure("995");
		TileItem item = item(995);
		drops.add(item, tileAt(new WorldPoint(3220, 3200, 0)));
		assertSame(item, drops.nearest(player, 20, false));
		assertNull(drops.nearest(player, 19, false));
		assertNull(drops.nearest(new WorldPoint(3200, 3200, 1), 64, false));
		assertSame(item, drops.nearest(new WorldPoint(3210, 3200, 0), 10, false));
	}

	@Test
	public void privateLootBecomesEligibleWhenItIsPublic()
	{
		drops.configure("995");
		TileItem item = item(995);
		when(item.getOwnership()).thenReturn(TileItem.OWNERSHIP_OTHER);
		when(item.isPrivate()).thenReturn(true);
		drops.add(item, tileAt(player));
		assertNull(drops.nearest(player, 20, false));
		when(item.isPrivate()).thenReturn(false);
		assertSame(item, drops.nearest(player, 20, false));
		assertNull(drops.nearest(player, 20, true));
		when(item.getOwnership()).thenReturn(TileItem.OWNERSHIP_GROUP);
		assertSame(item, drops.nearest(player, 20, true));
	}

	@Test
	public void configurationAcceptsSeparatorsAndIgnoresInvalidIds()
	{
		drops.configure("995,\n4151; 995 invalid -1 9999999999999999999");
		assertTrue(drops.add(item(995), tileAt(player)));
		assertTrue(drops.add(item(4151), tileAt(player)));
		assertFalse(drops.add(item(526), tileAt(player)));
		drops.configure("");
		assertFalse(drops.isConfigured());
		assertNull(drops.nearest(player, 20, false));
	}

	@Test
	public void aPartialPickupDoesNotClearTheDrop()
	{
		drops.configure("995");
		TileItem item = item(995);
		when(item.getQuantity()).thenReturn(50);
		drops.add(item, tileAt(player));
		when(item.getQuantity()).thenReturn(25);
		assertSame(item, drops.nearest(player, 20, false));
		when(item.getQuantity()).thenReturn(0);
		assertNull(drops.nearest(player, 20, false));
	}

	private TileItem item(int id)
	{
		TileItem item = mock(TileItem.class);
		when(item.getId()).thenReturn(id);
		when(item.getQuantity()).thenReturn(1);
		when(item.getOwnership()).thenReturn(TileItem.OWNERSHIP_SELF);
		return item;
	}

	private Tile tileAt(WorldPoint point)
	{
		Tile tile = mock(Tile.class);
		when(tile.getWorldLocation()).thenReturn(point);
		return tile;
	}
}
