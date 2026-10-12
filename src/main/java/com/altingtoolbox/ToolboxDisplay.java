package com.altingtoolbox;

import java.awt.Color;
import java.util.List;

final class ToolboxDisplay
{
	static final ToolboxDisplay HIDDEN = new ToolboxDisplay(false, List.of(), List.of());
	final boolean visible;
	final List<Alert> alerts;
	final List<String> status;
	final long undimUntilNanos;

	ToolboxDisplay(boolean visible, List<Alert> alerts, List<String> status)
	{
		this(visible, alerts, status, 0);
	}

	ToolboxDisplay(boolean visible, List<Alert> alerts, List<String> status, long undimUntilNanos)
	{
		this.visible = visible;
		this.alerts = List.copyOf(alerts);
		this.status = List.copyOf(status);
		this.undimUntilNanos = undimUntilNanos;
	}

	Alert activeAlert(long elapsedMillis, int rotationSeconds)
	{
		if (alerts.isEmpty()) { return null; }
		int index = (int) ((Math.max(0, elapsedMillis) / (Math.max(1, rotationSeconds) * 1000L)) % alerts.size());
		return alerts.get(index);
	}

	boolean hasSameAlerts(ToolboxDisplay other)
	{
		if (alerts.size() != other.alerts.size()) { return false; }
		for (int i = 0; i < alerts.size(); i++)
		{
			if (!alerts.get(i).message.equals(other.alerts.get(i).message)) { return false; }
		}
		return true;
	}

	static String cannonText(String prefix, int ammo)
	{
		String label = prefix.trim();
		return label.isEmpty() ? Integer.toString(ammo) : label + " " + ammo;
	}

	static final class Alert
	{
		final String message;
		final Color color;
		final AltingToolboxConfig.AlertStyle style;

		Alert(String message, Color color, AltingToolboxConfig.AlertStyle style)
		{
			this.message = message;
			this.color = color;
			this.style = style;
		}
	}
}
