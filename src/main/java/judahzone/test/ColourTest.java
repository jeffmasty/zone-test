package judahzone.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import judahzone.fx.MonoFilter;
import judahzone.fx.op.Noise;
import judahzone.fx.op.Noise.Colour;
import judahzone.util.AudioMetrics;
import judahzone.util.Constants;

public class ColourTest {

	// private static final int SR = Constants.sampleRate();
	private static final int N_FRAMES = Constants.bufSize();

	private static final float MIN_RMS = 1e-6f;
	private static final float MAX_RMS = 1.0f;

	public static final int lowHz = 50;
	public static final int hiHz = 11_000;

	@Test
	void testEachColourRMS() {
		Noise gen = new Noise(1); // single oversample factor
		for (Colour c : Colour.values()) {
			gen.setColour(c);
			float[] buf = new float[N_FRAMES];
			gen.fill(buf, 0, buf.length);
			float rms = AudioMetrics.rms(buf);
			assertFalse(Float.isNaN(rms), "RMS is NaN for colour " + c);
			assertTrue(Float.isFinite(rms), "RMS is infinite for colour " + c);
			assertTrue(rms > MIN_RMS, "RMS too small for colour " + c + ": " + rms);
			assertTrue(rms < MAX_RMS, "RMS too large for colour " + c + ": " + rms);
		}
	}

	@Test
	void testFilteredColours() {
		// simple audible band: 50..11k Hz
		MonoFilter lowCut = new MonoFilter(MonoFilter.Type.LoCut, lowHz, 1);
		MonoFilter hiCut = new MonoFilter(MonoFilter.Type.HiCut, hiHz, 1);

		Noise gen = new Noise(1);

		float variation = 0.1f;
		float min = Noise.TARGET_RMS - variation;
		float max = Noise.TARGET_RMS + variation;

		for (Colour c : Colour.values()) {
			gen.setColour(c);
			float[] buf = new float[N_FRAMES];
			gen.fill(buf, 0, buf.length);

			// Process buffers in-place through filters (hi then low)
			hiCut.process(buf, null);
			lowCut.process(buf, null);

			float rms = AudioMetrics.rms(buf);
			assertFalse(Float.isNaN(rms), "Filtered RMS is NaN for colour " + c);
			assertTrue(Float.isFinite(rms), "Filtered RMS is infinite for colour " + c);
			assertTrue(rms > min, "Filtered RMS too small for colour " + c + ": " + rms);
			assertTrue(rms < max, "Filtered RMS too large for colour " + c + ": " + rms);
			// System.out.println("Filtered colour " + c + " has RMS: " + rms);
		}
	}
}
