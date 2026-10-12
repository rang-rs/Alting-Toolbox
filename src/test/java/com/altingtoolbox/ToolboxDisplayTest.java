package com.altingtoolbox;

import java.awt.Color;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolboxDisplayTest
{
	@Test
	public void blankPrefixReallyShowsOnlyTheCount()
	{
		assertEquals("30", ToolboxDisplay.cannonText("", 30));
		assertEquals("0", ToolboxDisplay.cannonText("  ", 0));
		assertEquals("Ammo: 60", ToolboxDisplay.cannonText("Ammo:", 60));
	}

	@Test
	public void simultaneousAlertsEachGetAFullTurn()
	{
		ToolboxDisplay.Alert prayer = new ToolboxDisplay.Alert("Restore prayer", Color.CYAN, AltingToolboxConfig.AlertStyle.FLASH);
		ToolboxDisplay.Alert drop = new ToolboxDisplay.Alert("Pick up Coins", Color.YELLOW, AltingToolboxConfig.AlertStyle.SOLID);
		ToolboxDisplay display = new ToolboxDisplay(true, List.of(prayer, drop), List.of());
		assertSame(prayer, display.activeAlert(0, 3));
		assertSame(prayer, display.activeAlert(2999, 3));
		assertSame(drop, display.activeAlert(3000, 3));
		assertSame(prayer, display.activeAlert(6000, 3));
		assertNull(ToolboxDisplay.HIDDEN.activeAlert(0, 3));
	}
}
