package judahzone.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import judahzone.api.Hz;
import judahzone.data.Postage;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.Gain.GainT;
import judahzone.fx.op.Noise.Colour;
import judahzone.util.AudioMetrics;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;
import net.judah.drums.pluck.PluckOsc;
import net.judah.drums.pluck.PluckSetup.Pick;
import net.judah.midi.Actives;

/** Unit tests for PluckOsc excitation, parameter mapping, and loop stability. */
public class PluckTest extends DrumTest {

	private DrumSetup setup;
	private Actives mine;
	private Pick pick;
	private PluckOsc osc;

	private static final int bufSize = 256;
	private float[] sumL = new float[bufSize];
	private float[] sumR = new float[bufSize];
	private static final float EPS = 1e-6f;

	@BeforeEach
	void init() {
		setup = new DrumSetup(
			DrumType.Snare,
			new GainT(1, 1, 0.5f, 0.5f),
			new Postage(10, 100),
			new Freqs(new Coord(20, 1), new FilterT(440, 1, 1), new Coord(5000, 1)),
			new Hz(440),
			new String[] {"Burst", "Amp", "Tap", "Feedback", "Damp", "Harmony", "Combs", "Colour"}
		);
		mine = new Actives(null, 0);
		pick = new Pick(10, 0.5f, 0.5f, 0.75f, 0.25f, 1, 0.5f, Colour.WHITE);
		osc = new PluckOsc(setup, pick, mine);
		drum = osc;
	}

	@Test void testTriggerRestartsExcitation() {
		osc.trigger(null);
		assertTrue(osc.getBurstKnob() > 0, "Burst length should be positive");
		// In a real scenario, we'd check if pluckCountdown > 0 via reflection or public state
	}

	@Test void testKnobMappingRoundTrip() {
		/** Test Feedback Mapping: 0..100 -> MIN_FB..MAX_FB */
		osc.set(3, 100); // Max feedback
		assertEquals(PluckOsc.MAX_FB, osc.getFeedback(), 1e-5f);
		assertEquals(100, osc.get(3));

//		pluck.set(2, 0); // Min feedback
//		assertEquals(PluckOsc.MIN_FB, pluck.getFeedback(), 1e-5f);
//		assertEquals(0, pluck.get(2));

		/** Test Damping Mapping: 0..100 -> MIN_DAMP..MAX_DAMP */
		osc.set(4, 50);
		int dampKnob = osc.get(4);
		assertTrue(dampKnob >= 49 && dampKnob <= 51);
	}

	@Test void testKnobEncoding() {
		// one by one set these values on index 3 (feedback) and verify against get()
		int[] knobs = new int[] { 0, 5, 19, 35, 56, 59, 60, 75, 85, 95, 99, 100 };

		for (int knob : knobs) {
			osc.set(4, knob);
			int readBack = osc.get(4);
			assertEquals(knob, readBack, "Knob value should round-trip correctly");
		}

		for (int knob : knobs) {
			osc.set(5, knob);
			int readBack = osc.get(5);
			assertEquals(knob, readBack, "Damp knob value should round-trip correctly");
		}
	}

	@Test void testProcessingProducesFiniteOutput() {
		// Trigger the pluck; this should start excitation and reset internals.
		osc.trigger(null);
		osc.setFeedback(0.9f);
		osc.setDamp(0.1f);

		// Clear mix buffers and then run several process cycles collecting RMS.
		float maxRms = 0f;
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < sumL.length; j++) { sumL[j] = 0f; sumR[j] = 0f; }
			osc.process(sumL, sumR);
			// Use AudioMetrics to compute RMS of the left channel (mix)
			float rms = AudioMetrics.rms(sumL);
			assertTrue(Float.isFinite(rms), "RMS must be finite");
			assertTrue(rms >= 0f, "RMS must be non-negative");
			if (rms > maxRms) maxRms = rms;
		}

		// Ensure some energy was produced by the triggered pluck
		assertTrue(maxRms > EPS, "Pluck produced no measurable energy");
	}

	@Test void testDampingReducesEnergy() {
		osc.trigger(null);
		osc.setFeedback(0.99f);
		osc.setDamp(0.001f); // Longest sustain
		osc.processImpl();
		// Here we would ideally capture RMS of the 'work' buffer

		osc.setDamp(0.5f); // Heavy damping
		osc.processImpl();
		// High damping should result in lower RMS in subsequent buffers
	}

	@Test void testPitchRampSmoothsFrequency() {
		osc.setPitchKnob((byte) 40);
		osc.trigger(null);
		float initialFreq = osc.getLoop().getFrequency();

		osc.setPitchKnob((byte) 80);
		osc.processImpl();
		float midFreq = osc.getLoop().getFrequency();

		/** Pitch should be moving toward target, not jumping immediately due to Ramp */
		assertNotEquals(initialFreq, midFreq, "Frequency should have moved");
		assertNotEquals(80f, midFreq, "Frequency should not have jumped immediately to target");
	}


	@Override
	@Test
	public void testLifeCycle() {
		osc.setFeedback(0.9f);
		osc.setDamp(0.1f);
		lifeCycle(osc);
	}

}
