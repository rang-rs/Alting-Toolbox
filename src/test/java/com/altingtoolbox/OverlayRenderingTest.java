package com.altingtoolbox;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.OverlayLayer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OverlayRenderingTest
{
	private final Client client = mock(Client.class);
	private final AltingToolboxPlugin plugin = mock(AltingToolboxPlugin.class);
	private final AltingToolboxConfig config = new AltingToolboxConfig() { };
	private final AltingToolboxOverlay overlay = new AltingToolboxOverlay(client, plugin, config);

	@Before
	public void setUp()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getCanvasWidth()).thenReturn(100);
		when(client.getCanvasHeight()).thenReturn(100);
	}

	@Test
	public void idleScreenIsDimmed()
	{
		when(plugin.getDisplay()).thenReturn(new ToolboxDisplay(true, List.of(), List.of()));
		Color pixel = render();
		assertEquals(55, pixel.getRed());
		assertEquals(55, pixel.getGreen());
		assertEquals(55, pixel.getBlue());
	}

	@Test
	public void pickupDelayKeepsTheScreenClearUntilItsDeadline()
	{
		when(plugin.getDisplay()).thenReturn(new ToolboxDisplay(true, List.of(), List.of(), System.nanoTime() + 3_000_000_000L));
		assertEquals(Color.WHITE, render());
		when(plugin.getDisplay()).thenReturn(new ToolboxDisplay(true, List.of(), List.of(), System.nanoTime() - 1));
		assertEquals(55, render().getRed());
	}

	@Test
	public void otherAlertsStillRenderDuringPickupDelay()
	{
		ToolboxDisplay.Alert alert = new ToolboxDisplay.Alert("Restore health", Color.RED, AltingToolboxConfig.AlertStyle.SOLID);
		when(plugin.getDisplay()).thenReturn(new ToolboxDisplay(true, List.of(alert), List.of(), System.nanoTime() + 3_000_000_000L));
		Color pixel = render();
		assertEquals(255, pixel.getRed());
		assertEquals(155, pixel.getGreen());
	}

	@Test
	public void solidAlertReplacesTheDim()
	{
		setAlert(AltingToolboxConfig.AlertStyle.SOLID);
		when(plugin.elapsedMillis()).thenReturn(1000L);
		Color pixel = render();
		assertEquals(255, pixel.getRed());
		assertEquals(155, pixel.getGreen());
	}

	@Test
	public void flashOffPhaseLeavesTheGameClear()
	{
		setAlert(AltingToolboxConfig.AlertStyle.FLASH);
		when(plugin.elapsedMillis()).thenReturn(1000L);
		assertEquals(Color.WHITE, render());
		when(plugin.elapsedMillis()).thenReturn(1200L);
		assertEquals(155, render().getGreen());
	}

	@Test
	public void textStaysWhiteAboveDimmingAndSolidAlerts()
	{
		when(client.getCanvasWidth()).thenReturn(800);
		when(client.getCanvasHeight()).thenReturn(600);
		AltingToolboxTextOverlay text = new AltingToolboxTextOverlay(client, plugin, config);
		ToolboxDisplay.Alert alert = new ToolboxDisplay.Alert("Restore health", Color.RED, AltingToolboxConfig.AlertStyle.SOLID);
		for (List<ToolboxDisplay.Alert> alerts : List.of(List.<ToolboxDisplay.Alert>of(), List.of(alert)))
		{
			when(plugin.getDisplay()).thenReturn(new ToolboxDisplay(true, alerts, List.of("30")));
			BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
			for (OverlayLayer layer : OverlayLayer.values())
			{
				Graphics2D graphics = image.createGraphics();
				try
				{
					if (layer == text.getLayer()) { text.render(graphics); }
					if (layer == overlay.getLayer()) { overlay.render(graphics); }
				}
				finally { graphics.dispose(); }
			}
			boolean hasWhiteText = false;
			for (int y = 270; y < 330; y++)
			{
				for (int x = 280; x < 520; x++)
				{
					hasWhiteText |= image.getRGB(x, y) == Color.WHITE.getRGB();
				}
			}
			assertTrue("Text should remain white above the screen tint", hasWhiteText);
			text.getBounds().setLocation(0, 0);
		}
	}

	@Test
	public void loginAndPausedStatesNeverDim()
	{
		when(plugin.getDisplay()).thenReturn(ToolboxDisplay.HIDDEN);
		assertEquals(Color.WHITE, render());
		when(plugin.getDisplay()).thenReturn(new ToolboxDisplay(true, List.of(), List.of()));
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		assertEquals(Color.WHITE, render());
	}

	private void setAlert(AltingToolboxConfig.AlertStyle style)
	{
		ToolboxDisplay.Alert alert = new ToolboxDisplay.Alert("Drink anti-poison", Color.RED, style);
		when(plugin.getDisplay()).thenReturn(new ToolboxDisplay(true, List.of(alert), List.of()));
	}

	private Color render()
	{
		BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, 100, 100);
			overlay.render(graphics);
			return new Color(image.getRGB(50, 50));
		}
		finally { graphics.dispose(); }
	}
}
