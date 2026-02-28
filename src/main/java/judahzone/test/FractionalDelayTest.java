package judahzone.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import judahzone.util.Interpolation;
import net.judah.drums.pluck.PluckDelay;

/** Unit tests for PluckDelay (fractional delay / interpolation / reset). */
public class FractionalDelayTest {

	// private static final float EPS = 1e-6f;
	// private static final int SR = Constants.sampleRate();

	private PluckDelay delay;

	@BeforeEach
	void setup() {
		delay = new PluckDelay();
	}

	@Test void testFractionalInterpolationProducesFiniteBoundedOutputs() {
		delay.reset();
		delay.setDelaySamples(32.3f);

		final int N = 128;
		for (int i = 0; i < N; i++) {
			boolean inject = i < 6;
			float out = delay.process(inject ? 1.0f : 0f, 0.9f, 0.05f);
			assertTrue(Float.isFinite(out));
			assertTrue(out >= -2f && out <= 2f);
		}

		delay.setDelaySamples(32.6f);
		for (int i = 0; i < N; i++) {
			float out = delay.process(0f, 0.9f, 0.05f);
			assertTrue(Float.isFinite(out));
		}
	}

	@Test void testResetClearsBuffer() {
		delay.reset();
		delay.setDelaySamples(40.7f);
		for (int i = 0; i < 32; i++)
			delay.process(i < 4 ? 1.0f : 0f, 0.9f, 0.1f);

		delay.reset();
		for (int i = 0; i < 64; i++) {
			float v = delay.process(0f, 0.9f, 0.1f);
			assertTrue(Math.abs(v) < 1e-5f, "reset() should clear internal buffer");
		}
	}

	@Test void testFractionalInterpolationContinuityAtTransition() {
		delay.reset();
		delay.setDelaySamples(48.3f);
		delay.setInterpolation(Interpolation.CUBIC);

		float last = 0f;
		for (int i = 0; i < 256; i++) {
			boolean inject = i < 10;
			last = delay.process(inject ? 1.0f : 0f, 0.9f, 0.05f);
		}

		/** Change parameter: Ramp ensures this is smooth internally */
		delay.setDelaySamples(48.31f);
		float next = delay.process(0f, 0.9f, 0.05f);

		/** Continuity: boundary jump should be smaller than threshold */
		float delta = Math.abs(next - last);
		assertTrue(delta < 0.05f, "Fractional change produced large discontinuity: " + delta);
	}

//	@Test void testTapSmallStepContinuityAtTransition() {
//		delay.reset();
//		delay.setDelaySamples(32.3f);
//		// delay.setTap(0.5f);
//
//		float last = 0f;
//		for (int i = 0; i < 128; i++)
//			last = delay.process(i < 4 ? 1f : 0f, i < 4, 0.9f, 0.05f);
//
//		// delay.setTap(0.502f);
//		float next = delay.process(0f, false, 0.9f, 0.05f);
//
//		float delta = Math.abs(next - last);
//		assertTrue(delta < 0.05f, "Small tap step caused large discontinuity: " + delta);
//	}
}
