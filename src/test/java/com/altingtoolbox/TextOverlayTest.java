package com.altingtoolbox;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import net.runelite.api.Client;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TextOverlayTest
{
	private final Client client = mock(Client.class);
	private final AltingToolboxPlugin plugin = mock(AltingToolboxPlugin.class);
	private final AltingToolboxTextOverlay overlay = new AltingToolboxTextOverlay(client, plugin, new AltingToolboxConfig() { });

	@Test
	public void differentMessagesStayAtTheCanvasCenter()
	{
		assertCentered(List.of("30"), 800, 600, 400, 300);
		assertCentered(List.of("Drink anti-poison"), 800, 600, 400, 300);
		assertCentered(List.of("Pick up a very long item name"), 800, 600, 400, 300);
		assertCentered(List.of("30", "Prayer: 99", "Strength: 118"), 800, 600, 400, 300);
	}

	@Test
	public void defaultCenterFollowsCanvasResize()
	{
		assertCentered(List.of("30"), 800, 600, 400, 300);
		assertCentered(List.of("30"), 1000, 700, 500, 350);
	}

	@Test
	public void draggedMessagesKeepTheirMidpoint()
	{
		overlay.setPreferredLocation(new Point(100, 80));
		overlay.getBounds().setLocation(100, 80);
		assertCentered(List.of("30"), 800, 600, 220, 110);
		assertCentered(List.of("Drink anti-poison"), 800, 600, 220, 110);
	}

	private void assertCentered(List<String> lines, int width, int height, int centerX, int centerY)
	{
		when(client.getCanvasWidth()).thenReturn(width);
		when(client.getCanvasHeight()).thenReturn(height);
		when(plugin.getDisplay()).thenReturn(new ToolboxDisplay(true, List.of(), lines));
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.translate(overlay.getBounds().x, overlay.getBounds().y);
			Dimension size = overlay.render(graphics);
			overlay.getBounds().setSize(size);
			assertEquals(centerX, overlay.getBounds().getCenterX(), 0.5);
			assertEquals(centerY, overlay.getBounds().getCenterY(), 0.5);
			int left = width, right = 0, top = height, bottom = 0;
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					if ((image.getRGB(x, y) >>> 24) == 0) { continue; }
					left = Math.min(left, x);
					right = Math.max(right, x);
					top = Math.min(top, y);
					bottom = Math.max(bottom, y);
				}
			}
			assertTrue(left < right);
			assertEquals(centerX, (left + right) / 2.0, 3);
			assertEquals(centerY, (top + bottom) / 2.0, 6);
		}
		finally { graphics.dispose(); }
	}
}
