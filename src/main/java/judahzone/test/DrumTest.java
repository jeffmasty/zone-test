package judahzone.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import judahzone.util.AudioMetrics;
import judahzone.util.Constants;
import net.judah.drums.Drum;

public abstract class DrumTest {

	public static final int bufSize = Constants.bufSize();
	public static final float RMS_TOLERANCE = 0.01f;
	public static final float EPS = 1e-6f;
    public static final int MAX_CHURN = 10_000;

	protected float[] sumL = new float[bufSize];
	protected float[] sumR = new float[bufSize];

	protected int churn;
	Drum drum;

	@Test
	public void testLifeCycle() {
		lifeCycle(drum);
	}

	public void lifeCycle(Drum osc) {
		churn = 0;

		float maxRms = 0;
		osc.setFundamental(440);
		osc.trigger(null);

		assertTrue(osc.isPlaying());

		osc.process(sumL, sumR);
		while (osc.isSounding()) {
            churn++;
            if (churn > MAX_CHURN) {
                fail("Osc stuck in infinite loop");
                break;
            }
            osc.process(sumL, sumR);
            float rms = AudioMetrics.rms(sumL);
            if (rms > maxRms) maxRms = rms;
		}
		assertTrue(maxRms > RMS_TOLERANCE, "Osc produced no measurable energy");
		assertFalse(osc.isPlaying());
	}


//	@Test
//	void testBasicAudioGeneration() {
//		Arrays.fill(sumL, 0f);
//		Arrays.fill(sumR, 0f);
//
//		drum.trigger(null);
//		drum.process(sumL, sumR);
//
//		float rmsL = AudioMetrics.rms(sumL);
//		float rmsR = AudioMetrics.rms(sumR);
//
//		assertTrue(rmsL > EPS, "Left channel should produce audio; RMS=" + rmsL);
//		assertTrue(rmsR > EPS, "Right channel should produce audio; RMS=" + rmsR);
//		assertTrue(rmsL < 1.1f, "Left channel should not clip; RMS=" + rmsL);
//		assertTrue(rmsR < 1.1f, "Right channel should not clip; RMS=" + rmsR);
//	}
//
//
//	@Test void testProcessingProducesFiniteOutput() {
//		// Trigger the pluck; this should start excitation and reset internals.
//		drum.trigger(null);
//
//		// Clear mix buffers and then run several process cycles collecting RMS.
//		float maxRms = 0f;
//		for (int i = 0; i < 10; i++) {
//			for (int j = 0; j < sumL.length; j++) { sumL[j] = 0f; sumR[j] = 0f; }
//			drum.process(sumL, sumR);
//			// Use AudioMetrics to compute RMS of the left channel (mix)
//			float rms = AudioMetrics.rms(sumL);
//			assertTrue(Float.isFinite(rms), "RMS must be finite");
//			assertTrue(rms >= 0f, "RMS must be non-negative");
//			if (rms > maxRms) maxRms = rms;
//		}
//
//		// Ensure some energy was produced by the triggered pluck
//		assertTrue(maxRms > EPS, "Pluck produced no measurable energy");
//	}
//
//	@Test
//	void testEnvelopeDecay() {
//
//		drum.trigger(null);
//
//		float[] frame1 = new float[bufSize];
//		float[] frame2 = new float[bufSize];
//		float[] frame3 = new float[bufSize];
//		Arrays.fill(sumR, 0f);
//		Arrays.fill(sumL, 0f);
//
//		drum.process(sumL, sumR);
//
//		while (drum.getEnv().getStage() == Delta.ATK)
//			drum.process(sumL, sumR);  // fast forward to decay stage
//
//		Arrays.fill(sumR, 0f);
//
//		drum.process(frame1, sumR);
//		float rms1 = AudioMetrics.rms(frame1);
//
//		drum.process(frame2, sumR);
//		float rms2 = AudioMetrics.rms(frame2);
//
//		drum.process(frame3, sumR);
//		float rms3 = AudioMetrics.rms(frame3);
//
//		assertTrue(rms2 <= rms1 + RMS_TOLERANCE, "Frame 2 should decay from frame 1; RMS1=" + rms1 + " RMS2=" + rms2);
//		assertTrue(rms3 <= rms2 + RMS_TOLERANCE, "Frame 3 should decay from frame 2; RMS2=" + rms2 + " RMS3=" + rms3);
//	}
//
//	@Test
//	void testNoAudioWhenIdle() {
//		Arrays.fill(sumL, 1f);
//		Arrays.fill(sumR, 1f);
//
//		// Don't trigger; process idle state
//		drum.process(sumL, sumR);
//
//		float rmsL = AudioMetrics.rms(sumL);
//		float rmsR = AudioMetrics.rms(sumR);
//
//		assertTrue(rmsL < RMS_TOLERANCE, "Idle left should be silent; RMS=" + rmsL);
//		assertTrue(rmsR < RMS_TOLERANCE, "Idle right should be silent; RMS=" + rmsR);
//	}

	// warm up an osc, protecting against infinite loops
//	void lifeCycle(DrumOsc osc) {
//		churn = 0;
//
//		float maxRms = 0;
//		osc.setFundamental(440);
//		osc.trigger(null);
//
//		assertTrue(osc.isPlaying());
//
//		osc.process(sumL, sumR);
//		while (osc.isSounding()) {
//            churn++;
//            if (churn > MAX_CHURN) {
//                fail("Osc stuck in infinite loop");
//                break;
//            }
//            float rms = AudioMetrics.rms(sumL);
//            if (rms > maxRms) maxRms = rms;
//		}
//		assertTrue(maxRms > RMS_TOLERANCE, "Osc produced no measurable energy");
//	}

	@Test
	public final void testNoAudioWhenIdle() {
		noIdleAudio(drum);
	}


	public void noIdleAudio(Drum osc) {
		Arrays.fill(sumL, 0);
		Arrays.fill(sumR, 0);

		// Don't trigger; process idle state
		osc.process(sumL, sumR);

		float rmsL = AudioMetrics.rms(sumL);
		float rmsR = AudioMetrics.rms(sumR);

		assertTrue(rmsL < RMS_TOLERANCE, "Idle left should be silent; RMS=" + rmsL);
		assertTrue(rmsR < RMS_TOLERANCE, "Idle right should be silent; RMS=" + rmsR);
	}

}
