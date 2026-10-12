package com.altingtoolbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class AltingToolboxTextOverlay extends Overlay
{
	private static final int WIDTH = 240;
	private static final int HEIGHT = 60;

	private final Client client;
	private final AltingToolboxPlugin plugin;
	private final AltingToolboxConfig config;
	private Font font;
	private int fontSize;

	@Inject
	AltingToolboxTextOverlay(Client client, AltingToolboxPlugin plugin, AltingToolboxConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DETACHED);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		ToolboxDisplay display = plugin.getDisplay();
		if (!display.visible) { return null; }
		ToolboxDisplay.Alert alert = display.activeAlert(plugin.elapsedMillis(), config.alertRotation());
		List<String> lines = alert == null ? display.status
			: config.showAlertText() ? List.of(alert.message) : List.of();
		if (lines.isEmpty()) { return null; }
		int size = config.textSize();
		if (font == null || fontSize != size)
		{
			fontSize = size;
			font = FontManager.getRunescapeBoldFont().deriveFont((float) Math.max(12, fontSize));
		}
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();
		if (getPreferredLocation() == null)
		{
			int x = (client.getCanvasWidth() - WIDTH) / 2;
			int y = (client.getCanvasHeight() - HEIGHT) / 2;
			graphics.translate(x - getBounds().x, y - getBounds().y);
			getBounds().setLocation(x, y);
		}
		int y = (HEIGHT - lines.size() * metrics.getHeight()) / 2 + metrics.getAscent();
		for (String line : lines)
		{
			int x = (WIDTH - metrics.stringWidth(line)) / 2;
			graphics.setColor(Color.BLACK);
			graphics.drawString(line, x + 1, y + 1);
			graphics.setColor(Color.WHITE);
			graphics.drawString(line, x, y);
			y += metrics.getHeight();
		}
		// A fixed drag area keeps the midpoint stable when the text changes.
		return new Dimension(WIDTH, HEIGHT);
	}
}
