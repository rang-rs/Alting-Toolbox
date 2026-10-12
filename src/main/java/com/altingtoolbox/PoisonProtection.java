package com.altingtoolbox;

final class PoisonProtection
{
	private static final int POISON_TICKS = 30;
	private int value;
	private int nextTick = -1;

	void update(int newValue, int tick)
	{
		if (newValue != value || nextTick < 0)
		{
			if (newValue == 0) { nextTick = -1; }
			else if (nextTick <= tick) { nextTick = tick + POISON_TICKS; }
			value = newValue;
		}
	}

	boolean isProtected() { return value < 0; }

	String status(int tick)
	{
		if (!isProtected()) { return value > 0 ? "Poison: affected" : "Poison: unprotected"; }
		boolean venomProtection = value < -38;
		int remainingSteps = venomProtection ? -(value + 38) : -value;
		int ticks = Math.max(0, nextTick - tick + (remainingSteps - 1) * POISON_TICKS);
		int seconds = (ticks * 3 + 4) / 5;
		return (venomProtection ? "Anti-venom: " : "Anti-poison: ")
			+ seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}

	void clear()
	{
		value = 0;
		nextTick = -1;
	}
}
