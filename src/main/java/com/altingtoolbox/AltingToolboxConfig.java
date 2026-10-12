package com.altingtoolbox;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.api.Skill;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(AltingToolboxConfig.GROUP)
public interface AltingToolboxConfig extends Config
{
	String GROUP = "altingtoolbox";

	enum AlertStyle
	{
		FLASH("Flash"), SOLID("Solid");

		private final String label;

		AlertStyle(String label) { this.label = label; }

		@Override
		public String toString() { return label; }
	}

	enum CombatStat
	{
		STRENGTH(Skill.STRENGTH), ATTACK(Skill.ATTACK), DEFENCE(Skill.DEFENCE),
		RANGED(Skill.RANGED), MAGIC(Skill.MAGIC);

		final Skill skill;

		CombatStat(Skill skill) { this.skill = skill; }

		@Override
		public String toString() { return skill.getName(); }
	}

	@ConfigSection(name = "Display", description = "", position = 0)
	String display = "display";

	@ConfigSection(name = "Combat boost", description = "", position = 1, closedByDefault = true)
	String boost = "boost";

	@ConfigSection(name = "Poison protection", description = "", position = 2, closedByDefault = true)
	String poison = "poison";

	@ConfigSection(name = "Prayer", description = "", position = 3, closedByDefault = true)
	String prayer = "prayer";

	@ConfigSection(name = "Health", description = "", position = 4, closedByDefault = true)
	String health = "health";

	@ConfigSection(name = "Cannon", description = "", position = 5, closedByDefault = true)
	String cannon = "cannon";

	@ConfigSection(name = "Drops", description = "", position = 6, closedByDefault = true)
	String drops = "drops";

	@ConfigItem(keyName = "dimEnabled", name = "Dim screen", description = "", section = display, position = 0)
	default boolean dimEnabled() { return true; }

	@Range(min = 0, max = 255)
	@ConfigItem(keyName = "dimOpacity", name = "Dim opacity", description = "0 is clear; 255 is opaque.", section = display, position = 1)
	default int dimOpacity() { return 200; }

	@Range(min = 0, max = 255)
	@ConfigItem(keyName = "alertOpacity", name = "Alert opacity", description = "0 is clear; 255 is opaque.", section = display, position = 2)
	default int alertOpacity() { return 100; }

	@Range(min = 300, max = 3000)
	@Units(Units.MILLISECONDS)
	@ConfigItem(keyName = "flashPeriod", name = "Flash interval", description = "Time for one full flash cycle.", section = display, position = 3)
	default int flashPeriod() { return 1200; }

	@Range(min = 1, max = 10)
	@Units(Units.SECONDS)
	@ConfigItem(keyName = "alertRotation", name = "Cycle alerts every", description = "When more than one alert is active.", section = display, position = 4)
	default int alertRotation() { return 3; }

	@Range(min = 12, max = 48)
	@ConfigItem(keyName = "textSize", name = "Text size", description = "", section = display, position = 5)
	default int textSize() { return 24; }

	@ConfigItem(keyName = "showAlertText", name = "Show alert text", description = "", section = display, position = 6)
	default boolean showAlertText() { return true; }

	@ConfigItem(keyName = "pauseKey", name = "Pause hotkey", description = "Pauses the overlay and sounds.", section = display, position = 7)
	default Keybind pauseKey() { return new Keybind(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK); }

	@ConfigItem(keyName = "boostEnabled", name = "Enabled", description = "", section = boost, position = 0)
	default boolean boostEnabled() { return true; }

	@ConfigItem(keyName = "combatStat", name = "Stat", description = "", section = boost, position = 1)
	default CombatStat combatStat() { return CombatStat.STRENGTH; }

	@Range(min = 1, max = 125)
	@ConfigItem(keyName = "boostThreshold", name = "Alert at or below", description = "The current boosted level, not the amount above your base level.", section = boost, position = 2)
	default int boostThreshold() { return 109; }

	@ConfigItem(keyName = "boostColor", name = "Colour", description = "", section = boost, position = 3)
	default Color boostColor() { return new Color(60, 210, 100); }

	@ConfigItem(keyName = "boostStyle", name = "Style", description = "", section = boost, position = 4)
	default AlertStyle boostStyle() { return AlertStyle.FLASH; }

	@ConfigItem(keyName = "showBoost", name = "Show current level", description = "", section = boost, position = 5)
	default boolean showBoost() { return false; }

	@ConfigItem(keyName = "poisonEnabled", name = "Enabled", description = "Alerts whenever poison protection is absent, even if you are not poisoned.", section = poison, position = 0)
	default boolean poisonEnabled() { return true; }

	@ConfigItem(keyName = "poisonColor", name = "Colour", description = "", section = poison, position = 1)
	default Color poisonColor() { return new Color(235, 65, 65); }

	@ConfigItem(keyName = "poisonStyle", name = "Style", description = "", section = poison, position = 2)
	default AlertStyle poisonStyle() { return AlertStyle.FLASH; }

	@ConfigItem(keyName = "showPoison", name = "Show protection timer", description = "An estimate until the next poison-state update is observed.", section = poison, position = 3)
	default boolean showPoison() { return false; }

	@ConfigItem(keyName = "prayerEnabled", name = "Enabled", description = "", section = prayer, position = 0)
	default boolean prayerEnabled() { return true; }

	@Range(min = 0, max = 99)
	@ConfigItem(keyName = "prayerThreshold", name = "Alert at or below", description = "", section = prayer, position = 1)
	default int prayerThreshold() { return 7; }

	@ConfigItem(keyName = "prayerColor", name = "Colour", description = "", section = prayer, position = 2)
	default Color prayerColor() { return new Color(60, 200, 240); }

	@ConfigItem(keyName = "prayerStyle", name = "Style", description = "", section = prayer, position = 3)
	default AlertStyle prayerStyle() { return AlertStyle.FLASH; }

	@ConfigItem(keyName = "showPrayer", name = "Show prayer points", description = "", section = prayer, position = 4)
	default boolean showPrayer() { return false; }

	@ConfigItem(keyName = "healthEnabled", name = "Enabled", description = "", section = health, position = 0)
	default boolean healthEnabled() { return true; }

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "healthThreshold", name = "Alert at or below", description = "", section = health, position = 1)
	default int healthThreshold() { return 30; }

	@ConfigItem(keyName = "healthColor", name = "Colour", description = "", section = health, position = 2)
	default Color healthColor() { return new Color(255, 80, 130); }

	@ConfigItem(keyName = "healthStyle", name = "Style", description = "", section = health, position = 3)
	default AlertStyle healthStyle() { return AlertStyle.FLASH; }

	@ConfigItem(keyName = "showHealth", name = "Show hitpoints", description = "", section = health, position = 4)
	default boolean showHealth() { return false; }

	@ConfigItem(keyName = "cannonEnabled", name = "Enabled", description = "", section = cannon, position = 0)
	default boolean cannonEnabled() { return true; }

	@ConfigItem(keyName = "cannonAmmoAlert", name = "Ammo alert", description = "", section = cannon, position = 1)
	default boolean cannonAmmoAlert() { return true; }

	@Range(min = 0, max = 59)
	@ConfigItem(keyName = "cannonThreshold", name = "Alert at or below", description = "Cannonballs remaining in your cannon.", section = cannon, position = 2)
	default int cannonThreshold() { return 0; }

	@ConfigItem(keyName = "cannonRepairAlert", name = "Repair alert", description = "", section = cannon, position = 3)
	default boolean cannonRepairAlert() { return true; }

	@ConfigItem(keyName = "cannonColor", name = "Colour", description = "", section = cannon, position = 4)
	default Color cannonColor() { return new Color(0x777777); }

	@ConfigItem(keyName = "cannonStyle", name = "Style", description = "", section = cannon, position = 5)
	default AlertStyle cannonStyle() { return AlertStyle.FLASH; }

	@ConfigItem(keyName = "showCannon", name = "Show cannonballs", description = "", section = cannon, position = 6)
	default boolean showCannon() { return true; }

	@ConfigItem(keyName = "cannonPrefix", name = "Prefix", description = "Leave empty to show only the number.", section = cannon, position = 7)
	default String cannonPrefix() { return ""; }

	@ConfigItem(keyName = "dropsEnabled", name = "Enabled", description = "", section = drops, position = 0)
	default boolean dropsEnabled() { return true; }

	@ConfigItem(keyName = "dropIds", name = "Item IDs", description = "Comma-separated item IDs. Spaces and newlines also work.", section = drops, position = 1)
	default String dropIds() { return ""; }

	@Range(min = 1, max = 64)
	@ConfigItem(keyName = "dropRange", name = "Range in tiles", description = "", section = drops, position = 2)
	default int dropRange() { return 20; }

	@ConfigItem(keyName = "ownDropsOnly", name = "Only my drops", description = "Includes group-owned drops.", section = drops, position = 3)
	default boolean ownDropsOnly() { return false; }

	@ConfigItem(keyName = "dropColor", name = "Colour", description = "", section = drops, position = 4)
	default Color dropColor() { return new Color(255, 220, 50); }

	@ConfigItem(keyName = "dropStyle", name = "Style", description = "", section = drops, position = 5)
	default AlertStyle dropStyle() { return AlertStyle.FLASH; }

	@ConfigItem(keyName = "dropSound", name = "Play sound", description = "", section = drops, position = 6)
	default boolean dropSound() { return true; }

	@Range(min = 0, max = 100)
	@ConfigItem(keyName = "soundVolume", name = "Sound volume", description = "", section = drops, position = 7)
	default int soundVolume() { return 60; }

	@Range(min = 1, max = 60)
	@Units(Units.SECONDS)
	@ConfigItem(keyName = "soundInterval", name = "Repeat sound every", description = "While a matching drop remains nearby.", section = drops, position = 8)
	default int soundInterval() { return 5; }

	@Range(min = 0, max = 30)
	@Units(Units.SECONDS)
	@ConfigItem(keyName = "pickupDimDelay", name = "Pickup dim delay", description = "Time to reposition after picking up a tracked drop. Set to 0 to dim immediately.", section = drops, position = 9)
	default int pickupDimDelay() { return 3; }
}
