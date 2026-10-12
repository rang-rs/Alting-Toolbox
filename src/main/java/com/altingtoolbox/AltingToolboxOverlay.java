package com.altingtoolbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class AltingToolboxOverlay extends Overlay
{
	private final Client client;
	private final AltingToolboxPlugin plugin;
	private final AltingToolboxConfig config;

	@Inject
	AltingToolboxOverlay(Client client, AltingToolboxPlugin plugin, AltingToolboxConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGHEST);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		ToolboxDisplay display = plugin.getDisplay();
		if (!display.visible || client.getGameState() != GameState.LOGGED_IN) { return null; }
		long elapsed = plugin.elapsedMillis();
		ToolboxDisplay.Alert alert = display.activeAlert(elapsed, config.alertRotation());
		Color color;
		int opacity;
		if (alert == null)
		{
			if (!config.dimEnabled() || System.nanoTime() < display.undimUntilNanos) { return null; }
			color = Color.BLACK;
			opacity = config.dimOpacity();
		}
		else
		{
			int flashPeriod = Math.max(300, config.flashPeriod());
			if (alert.style == AltingToolboxConfig.AlertStyle.FLASH
				&& elapsed % flashPeriod >= flashPeriod / 2)
			{
				return null;
			}
			color = alert.color;
			opacity = config.alertOpacity();
		}
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, opacity))));
		graphics.fillRect(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
		return null;
	}
}
