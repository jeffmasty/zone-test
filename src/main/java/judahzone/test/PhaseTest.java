package judahzone.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import judahzone.util.Phase;

/** Validates phase accumulation, wrap-around, and retrigger blending continuity. */

// TODO test blend ctor
public class PhaseTest {

	private Phase p;
	private static final float EPSILON = 1e-6f;

	@BeforeEach
	void setup() { p = new Phase(); }

	@Test void testAccumulationAndWrap() {
		p.next(0.7f);
		assertEquals(0.7f, p.get(), EPSILON);
		p.next(0.4f); // should wrap: 1.1 -> 0.1
		assertEquals(0.1f, p.get(), EPSILON);
	}

	@Test void testHardReset() {
		p.next(0.5f);
		p.reset();
		assertEquals(0f, p.get());
	}

//	@Test void testTriggerContinuity() {
//		float inc = 0.01f;
//		for (int i = 0; i < 50; i++) p.next(inc);
//
//		float preTrigger = p.get();
//		int blendLen = 100;
//		p.trigger(blendLen);
//
//		// Immediately after trigger, next() should be very close to preTrigger
//		float postTriggerFirst = p.next(inc);
//		assertTrue(Math.abs(postTriggerFirst - preTrigger) < inc * 2,
//			"Phase jumped too far during retrigger blend");
//	}

//	@Test void testBlendCompletion() {
//		p.next(0.5f);
//		p.trigger();
//
//		// Advance past duration
//		for (int i = 0; i < blendLen + 1; i++) p.next(0.01f);
//
//		// At this point, ghost phase should be inactive and we should be on the new ramp
//		// New ramp started at 0 and advanced (blendLen + 1) * 0.01
//		float expected = (blendLen + 1) * 0.01f;
//		assertEquals(expected, p.get(), EPSILON);
//	}

	@Test void testSineLUTBoundaries() {
		assertEquals(0f, Phase.sin(0f), EPSILON);
		assertEquals(1f, Phase.sin(0.25f), 1e-3f);
		assertEquals(0f, Phase.sin(0.5f), EPSILON);
		assertEquals(-1f, Phase.sin(0.75f), 1e-3f);
		assertEquals(0f, Phase.sin(0.99999f), 1e-3f); // Near wrap
	}

	@Test void testSineInterpolation() {
		float low = Phase.sin(0.1f);
		float mid = Phase.sin(0.1005f);
		float high = Phase.sin(0.101f);

		assertTrue(mid > low && mid < high, "Linear interpolation failed to produce intermediate value");
	}
}
