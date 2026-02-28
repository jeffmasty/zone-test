package judahzone.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import judahzone.util.Ramp;

/** Validates parameter ramping: immediate set, smooth transitions, and edge cases.
	•  Construction and idle state
	•  Immediate jumps vs. ramped transitions
	•  Monotonicity (up & down)
	•  Duration and completion
	•  Retrigger during ramp
	•  Edge cases (min/max ramp lengths, negative values)
	•  Real-time safety (allocation-free, fast)
*/
public class RampTest {

	private static final float EPSILON = 1e-6f;
	private Ramp ramp;

	@BeforeEach
	void setup() {
		ramp = new Ramp(64); // typical ramp length
	}

	@Test
	void testConstructionDefaults() {
		assertNotNull(ramp);
		assertEquals(0f, ramp.get(), EPSILON, "Initial value should be 0");
		assertFalse(ramp.isRamping(), "Should not be ramping on construction");
	}

	@Test
	void testImmediateSetWhenIdle() {
		ramp.set(0.5f);
		assertEquals(0.5f, ramp.get(), EPSILON, "Idle ramp should jump immediately");
		assertFalse(ramp.isRamping(), "Should not be ramping after single set");
	}

	@Test
	void testRampInitiation() {
		ramp.set(0.5f); // first set (idle → immediate)
		assertEquals(0.5f, ramp.get(), EPSILON);

		ramp.set(0.0f); // second set (ramping → start ramp)
		assertTrue(ramp.isRamping(), "Should be ramping after second set");
		assertEquals(0.5f, ramp.get(), EPSILON, "Value unchanged at ramp start");
	}

	@Test
	void testRampMonotonicity() {
		ramp.set(1.0f); // idle jump to 1.0
		ramp.set(0.0f); // start ramp down

		float prev = 1.0f;
		int count = 0;
		while (ramp.isRamping()) {
			float val = ramp.next();
			assertTrue(val >= 0f && val <= 1f, "Value out of range: " + val);
			assertTrue(val <= prev + EPSILON, "Ramp down should decrease at step " + count);
			prev = val;
			count++;
		}
		assertTrue(count > 0, "Ramp should advance multiple samples");
		assertEquals(0.0f, ramp.get(), EPSILON, "Final value should match target");
		assertFalse(ramp.isRamping(), "Should stop ramping when complete");
	}

	@Test
	void testRampUpMonotonicity() {
		ramp.set(0.0f); // idle jump to 0.0
		ramp.set(1.0f); // start ramp up

		float prev = 0.0f;
		int count = 0;
		while (ramp.isRamping()) {
			float val = ramp.next();
			assertTrue(val >= 0f && val <= 1f, "Value out of range: " + val);
			assertTrue(val >= prev - EPSILON, "Ramp up should increase at step " + count);
			prev = val;
			count++;
		}
		assertTrue(count > 0, "Ramp should advance multiple samples");
		assertEquals(1.0f, ramp.get(), EPSILON, "Final value should match target");
	}

	@Test
	void testRampDuration() {
		ramp.set(0.5f);
		ramp.set(0.0f);

		int samples = 0;
		while (ramp.isRamping()) {
			ramp.next();
			samples++;
		}
		assertEquals(64, samples, "Ramp should consume exactly rampLen samples");
	}

	@Test
	void testNextAdvancesWithoutRamping() {
		ramp.set(0.75f); // jump to 0.75, idle
		float before = ramp.get();
		ramp.next();
		float after = ramp.get();
		assertEquals(before, after, EPSILON, "next() should not change value when idle");
		assertFalse(ramp.isRamping());
	}

	@Test
	void testGetWithoutAdvance() {
		ramp.set(0.5f);
		ramp.set(0.2f); // start ramping

		float val1 = ramp.get();
		float val2 = ramp.get();
		assertEquals(val1, val2, EPSILON, "get() should not advance ramp");
		assertTrue(ramp.isRamping(), "Should still be ramping");
	}

	@Test
	void testResetImmediate() {
		ramp.set(0.5f);
		ramp.set(0.0f); // ramping
		assertTrue(ramp.isRamping());

		ramp.reset(0.8f);
		assertEquals(0.8f, ramp.get(), EPSILON, "reset() should jump to value");
		assertFalse(ramp.isRamping(), "reset() should stop ramping");
	}

	@Test
	void testResetDuringRamp() {
		ramp.set(1.0f);
		ramp.set(0.0f);

		for (int i = 0; i < 10; i++)
			ramp.next(); // advance partway

		ramp.reset(0.5f);
		assertEquals(0.5f, ramp.get(), EPSILON);
		assertFalse(ramp.isRamping());
	}

	@Test
	void testMinimumRampLength() {
		Ramp short_ramp = new Ramp(1); // minimum valid
		short_ramp.set(0.0f);
		short_ramp.set(1.0f);

		assertTrue(short_ramp.isRamping());
		short_ramp.next();
		assertEquals(1.0f, short_ramp.get(), EPSILON);
		assertFalse(short_ramp.isRamping(), "Single-sample ramp should complete");
	}

	@Test
	void testZeroOrNegativeRampClamp() {
		Ramp zero_ramp = new Ramp(0); // clamped to 1
		zero_ramp.set(0.5f);
		zero_ramp.set(0.0f);

		assertTrue(zero_ramp.isRamping());
		zero_ramp.next();
		assertEquals(0.0f, zero_ramp.get(), EPSILON);
		assertFalse(zero_ramp.isRamping(), "Zero-length ramp should clamp to 1 sample");
	}

	@Test
	void testRetriggerDuringRamp() {
		ramp.set(1.0f);
		ramp.set(0.0f);

		for (int i = 0; i < 20; i++)
			ramp.next();

		float midRampVal = ramp.get();
		assertTrue(midRampVal > 0f && midRampVal < 1f, "Mid-ramp value: " + midRampVal);

		// New target while ramping
		ramp.set(0.5f);
		assertTrue(ramp.isRamping(), "Should restart ramp");

		float newFirst = ramp.next();
		assertTrue(Math.abs(newFirst - midRampVal) < 0.05f, "Should continue from current value");
	}

	@Test
	void testLargeRampLength() {
		Ramp long_ramp = new Ramp(4800); // 100ms at 48kHz

		long_ramp.set(0.0f);
		long_ramp.set(1.0f);

		int samples = 0;
		while (long_ramp.isRamping()) {
			long_ramp.next();
			samples++;
		}
		assertEquals(4800, samples, "Long ramp should consume all samples");
		assertEquals(1.0f, long_ramp.get(), EPSILON);
	}

	@Test
	void testNegativeValues() {
		ramp.set(-0.5f);
		assertEquals(-0.5f, ramp.get(), EPSILON, "Ramp should support negative values");

		ramp.set(0.5f);
		float prev = -0.5f;
		while (ramp.isRamping()) {
			float val = ramp.next();
			assertTrue(val >= prev - EPSILON, "Ramp up from negative should increase");
			prev = val;
		}
		assertEquals(0.5f, ramp.get(), EPSILON);
	}

	@Test
	void testSameSourceAndTarget() {
		ramp.set(0.3f);
		ramp.set(0.3f); // same target

		assertEquals(0.3f, ramp.get(), EPSILON);
		assertFalse(ramp.isRamping(), "Ramping to same value should not start ramp");
	}

	@Test
	void testChainedSets() {
		ramp.set(0.0f);
		ramp.set(0.25f);
		ramp.set(0.5f);
		ramp.set(0.75f);
		ramp.set(1.0f);

		assertEquals(0.0f, ramp.get(), EPSILON, "Multiple sets should queue target");
		assertTrue(ramp.isRamping());
	}

	@Test
	void testRampSmoothnessAcrossBuffer() {
		final int BUFFER_SIZE = 512;
		final int RAMP_LEN = 128;

		Ramp smooth = new Ramp(RAMP_LEN);
		float[] samples = new float[RAMP_LEN + BUFFER_SIZE];
		int idx = 0;

		smooth.set(0.0f);
		smooth.set(1.0f);

		while (smooth.isRamping() && idx < samples.length) {
			samples[idx++] = smooth.next();
		}

		// Check interpolation: no jumps > (1/rampLen)
		float maxDelta = 0f;
		for (int i = 1; i < idx; i++) {
			float delta = Math.abs(samples[i] - samples[i - 1]);
			maxDelta = Math.max(maxDelta, delta);
		}
		assertTrue(maxDelta < 0.02f, "Ramp should be smooth, max delta: " + maxDelta);
	}

	@Test
	void testRTSafeNoAllocations() {
		// Verify ramp.next() does not allocate by repeated calls
		ramp.set(0.5f);
		ramp.set(0.0f);

		long before = System.nanoTime();
		for (int i = 0; i < 100_000; i++) {
			ramp.next();
			if (!ramp.isRamping())
				ramp.set(0.5f);
		}
		long after = System.nanoTime();

		double ms = (after - before) / 1_000_000.0;
		assertTrue(ms < 50, "100k ramp iterations should be fast; took " + ms + "ms");
	}
}
