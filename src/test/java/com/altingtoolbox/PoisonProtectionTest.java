package com.altingtoolbox;

import org.junit.Test;
import static org.junit.Assert.*;

public class PoisonProtectionTest
{
	@Test
	public void onlyImmunityClearsTheWarning()
	{
		PoisonProtection protection = new PoisonProtection();
		for (int value : new int[] {0, 5, 1000000})
		{
			protection.update(value, 10);
			assertFalse(protection.isProtected());
		}
		protection.update(-1, 10);
		assertTrue(protection.isProtected());
		protection.update(0, 40);
		assertFalse(protection.isProtected());
	}

	@Test
	public void countdownKeepsTheObservedPoisonTickPhase()
	{
		PoisonProtection protection = new PoisonProtection();
		protection.update(-2, 10);
		assertEquals("Anti-poison: 0:36", protection.status(10));
		assertEquals("Anti-poison: 0:30", protection.status(20));
		protection.update(-1, 40);
		assertEquals("Anti-poison: 0:18", protection.status(40));
	}

	@Test
	public void antivenomTransitionsToAntipoisonWithoutAnAlert()
	{
		PoisonProtection protection = new PoisonProtection();
		protection.update(-39, 10);
		assertEquals("Anti-venom: 0:18", protection.status(10));
		protection.update(-38, 40);
		assertTrue(protection.isProtected());
		assertEquals("Anti-poison: 11:24", protection.status(40));
		protection.clear();
		assertFalse(protection.isProtected());
	}
}
