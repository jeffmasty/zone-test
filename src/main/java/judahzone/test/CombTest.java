package judahzone.test;

/*
 *TODO:
 1. Construction & defaults — Partially covered
	2. testConstructionAndBasicApi() checks non-null and that API calls don't throw, but does not assert default delay length, feedback/damp defaults, or buffer allocation.
	3. Suggestion: add explicit asserts for default feedback/damp and for internal buffer length if accessible.
	4. Impulse response / decay — Partially covered
	5. Several tests use a single-sample impulse (testSmallFeedbackStepContinuity, testDelayWrapSafety), but none assert the expected decaying envelope or monotonic decay when feedback < 1.
	6. Suggestion: inject impulse and assert a monotonic (or exponentially decaying) envelope across subsequent samples within a tolerance.
	7. Bounded outputs & stability — Covered
	8. testOutputsFiniteAndBoundedUnderNoise() verifies finiteness and reasonable magnitude bounds across many iterations.
	9. Reset clears state — Covered
	10. testResetClearsState() fills state with impulses, calls reset(), and asserts subsequent outputs are near-zero.
	11. Parameter continuity — Partially covered
	12. testSmallFeedbackStepContinuity() checks a small feedback step for discontinuity. Damp continuity is not tested.
	13. Suggestion: add symmetric test for small damp steps and tighten epsilon if desired.
	14. Delay wrap / indexing — Partially covered
	15. testDelayWrapSafety() exercises two delays and checks outputs are finite; it does not explicitly validate wrap/index correctness or edge-buffer boundaries.
	16. Suggestion: create tests that set delays at exact buffer edges and validate expected sample indices/wrapping behavior or lack of OOB.
	17. processMix vs processReplace semantics — Missing / commented out
	18. The processReplace assertions are commented out. No active test verifies overwrite vs add semantics.
	19. Suggestion: re-enable and complete the commented test to assert replace overwrites and mix adds.
	20. Stereo/decorrelation usage — Covered
	21. testStereoDecorrelatedOutputsDifferButStable() verifies decorrelated offsets produce differing but finite outputs.
	22. Performance / RT-safety smoke — Partially covered
	23. performanceSmokeTest() measures throughput and checks for NaN but does not assert zero-heap-allocations (hard to do in unit tests).
	24. Suggestion: keep the timing check, and consider a separate RT-safety check (profiling or allocating-debug build) to detect allocations on the audio path.*
 *
 *
 *Objectives:
	•  Construction & defaults: verify delay length, feedback/damp defaults, buffer allocation.
	•  Impulse response / decay: feed a single-sample impulse, assert subsequent samples follow expected decaying envelope (monotonic decrease with feedback < 1).
	•  Bounded outputs & stability: run many samples with repeated impulses/noise and assert outputs stay finite and within reasonable bounds.
	•  Reset clears state: fill internal buffer via impulses, call reset(), then assert subsequent outputs are near-zero.
	•  Parameter continuity: small increments to feedback or damp should not cause large discontinuities — compare last sample before change to first after change and require delta < threshold.
	•  Delay wrap / indexing: set delay near buffer edges (min/max), run impulses and ensure no OOB, correct wrap behavior.
	•  processMix vs processReplace semantics: verify mix adds into destination and replace overwrites.
	•  Stereo/decorrelation usage: exercise Comb arrays with offsets (as in Freeverb) to ensure decorrelated outputs differ but remain stable.
	•  Performance / RT-safety smoke: run a large loop asserting no allocations and reasonable throughput (timing assert similar to RampTest).

Test strategy notes:
	•  Use impulses and short noise bursts rather than long musical signals — deterministic and fast.
	•  Use small epsilons for continuity checks (tuned to expected algorithmic smoothing).
	•  Mirror patterns from existing tests (RampTest, FractionalDelayTest, PhaseTest) for consistency.
 * */

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import judahzone.fx.op.Comb;
import judahzone.util.Constants;


public class CombTest {

 // private static final float EPS = 1e-6f;
 // private static final int SR = Constants.sampleRate();
 private static final int N_FRAMES = Constants.bufSize();
 private static final int BASE_DELAY = 1021;

 private Comb comb;

 @BeforeEach
 void setup() {
     comb = new Comb(BASE_DELAY);
     comb.reset();
     comb.setFeedback(0.7f);
     comb.setdamp(0.2f);
 }

 @Test
 void testConstructionAndBasicApi() {
     assertNotNull(comb);
     // Basic calls should not throw
     float[] in = new float[N_FRAMES];
     float[] out = new float[N_FRAMES];
     assertDoesNotThrow(() -> comb.processMix(in, out));
//     assertDoesNotThrow(() -> comb.processReplace(in, out));
     assertDoesNotThrow(() -> comb.reset());
 }

// @Test
// void testProcessReplaceOverwritesAndProcessMixAdds() {
//     float[] in = new float[N_FRAMES];
//     float[] destReplace = new float[N_FRAMES];
//     float[] destMix = new float[N_FRAMES];
//
//     // Fill inputs and destinations with known patterns
//     for (int i = 0; i < N_FRAMES; i++) {
//         in[i] = (i % 16 == 0) ? 1.0f : 0f;
//         destReplace[i] = 0.5f;
//         destMix[i] = 0.5f;
//     }
//
////      comb.processReplace(in, destReplace);
//     comb.processMix(in, destMix);
//
//     // Replace should not preserve previous 0.5 at all indices (some samples changed)
//     boolean anyChangedReplace = false;
//     boolean anyIncreasedMix = false;
//     for (int i = 0; i < N_FRAMES; i++) {
//         if (Math.abs(destReplace[i] - 0.5f) > EPS) anyChangedReplace = true;
//         if (destMix[i] > 0.5f + EPS) anyIncreasedMix = true;
//     }
//     assertTrue(anyChangedReplace, "processReplace should overwrite destination samples");
//     assertTrue(anyIncreasedMix, "processMix should add into destination");
// }

 @Test
 void testResetClearsState() {
     // Fill internal state by repeatedly injecting impulses
     float[] impulse = new float[N_FRAMES];
     impulse[0] = 1.0f;
     float[] out = new float[N_FRAMES];

     for (int i = 0; i < 128; i++) {
         comb.processMix(impulse, out);
         // zero out impulse only first sample is 1
         impulse[0] = 0f;
     }

     // After reset, outputs for a zero input should be near zero
     comb.reset();
     Arrays.fill(out, 0f);
     float[] zeros = new float[N_FRAMES];
     comb.processMix(zeros, out);

     for (int i = 0; i < N_FRAMES; i++)
         assertTrue(Math.abs(out[i]) < 1e-4f, "reset() should clear internal buffer " + out[i]);



 }

 @Test
 void testOutputsFiniteAndBoundedUnderNoise() {
     Random rng = new Random(12345);
     float[] in = new float[N_FRAMES];
     float[] out = new float[N_FRAMES];

     comb.setFeedback(0.85f);
     comb.setdamp(0.1f);

     for (int iter = 0; iter < 200; iter++) {
    	 Arrays.fill(out, 0f);
         for (int i = 0; i < N_FRAMES; i++)
             in[i] = (rng.nextFloat() * 2f - 1f) * 0.5f; // modest noise
         comb.processMix(in, out);
         for (int i = 0; i < N_FRAMES; i++) {
             assertTrue(Float.isFinite(out[i]), "output must be finite");
             assertTrue(Math.abs(out[i]) < 10f, "output out of reasonable bound: " + out[i]);
         }
     }
 }

 @Test
 void testSmallFeedbackStepContinuity() {
     float[] in = new float[N_FRAMES];
     float[] out = new float[N_FRAMES];

     // single impulse to create a tail
     in[0] = 1.0f;
     comb.setFeedback(0.6f);
     comb.processMix(in, out);
     float last = out[0];

     // a few quiet frames to let state propagate
     for (int i = 0; i < 4; i++) {
         in[0] = 0f;
         comb.processMix(in, out);
         last = out[0];
     }

     // small parameter change
     comb.setFeedback(0.602f);
     comb.processMix(in, out); // first sample after change
     float next = out[0];

     assertTrue(Math.abs(next - last) < 0.2f, "Small feedback step caused large discontinuity");
 }

 @Test
 void testDelayWrapSafety() {
     // Try edge delays used in Freeverb (decorrelated offsets)
     Comb left = new Comb(1021);
     Comb right = new Comb(1021 + 23);
     left.setFeedback(0.7f);
     right.setFeedback(0.7f);
     left.setdamp(0.2f);
     right.setdamp(0.2f);

     float[] in = new float[N_FRAMES];
     float[] outL = new float[N_FRAMES];
     float[] outR = new float[N_FRAMES];

     // single impulse followed by zero input frames; ensure no exceptions and finite outputs
     in[0] = 1.0f;
     left.processMix(in, outL);
     right.processMix(in, outR);

     for (int i = 0; i < N_FRAMES; i++) {
         assertTrue(Float.isFinite(outL[i]));
         assertTrue(Float.isFinite(outR[i]));
     }
 }

 @Test
 void testStereoDecorrelatedOutputsDifferButStable() {
     Comb a = new Comb(BASE_DELAY);
     Comb b = new Comb(BASE_DELAY + 23);
     a.setFeedback(0.75f);
     b.setFeedback(0.75f);
     a.setdamp(0.3f);
     b.setdamp(0.3f);

     float[] in = new float[N_FRAMES];
     float[] outA = new float[N_FRAMES];
     float[] outB = new float[N_FRAMES];

     // excite with a short noise burst (deterministic)
     for (int i = 0; i < 8; i++) in[i] = (i % 2 == 0) ? 0.8f : -0.6f;

     a.processMix(in, outA);
     b.processMix(in, outB);

     Arrays.fill(outA, 0f);
     Arrays.fill(outB, 0f);
     boolean anyDiff = false;
     for (int pass = 0; pass < 4 && !anyDiff; pass++) {
         a.processMix(in, outA);
         b.processMix(in, outB);
         for (int i = 0; i < N_FRAMES; i++) {
             assertTrue(Float.isFinite(outA[i]));
             assertTrue(Float.isFinite(outB[i]));
             if (Math.abs(outA[i] - outB[i]) > 1e-4f) {
                 anyDiff = true;
                 break;
             }
         }
         Arrays.fill(outA, 0f);
         Arrays.fill(outB, 0f);
     }
     assertTrue(anyDiff, "Decorrelated combs should produce differing outputs");
     }

 @Test
 void performanceSmokeTest() {
     final int ITER = 10_000; // moderate count for CI stability
     float[] in = new float[N_FRAMES];
     float[] out = new float[N_FRAMES];

     // populate a short repeating pattern
     for (int i = 0; i < N_FRAMES; i++) in[i] = (i & 1) == 0 ? 0.6f : -0.4f;

     long t0 = System.nanoTime();
     for (int i = 0; i < ITER; i++) {
         comb.processMix(in, out);
         // cheap touch to avoid JIT-elide (read a sample)
         float tmp = out[0];
         if (tmp != tmp) fail("NaN produced");
     }
     long t1 = System.nanoTime();
     double ms = (t1 - t0) / 1_000_000.0;
     // reasonable threshold for CI: should be fast; relaxed to 200ms for portability
     assertTrue(ms < 200, "Comb processing smoke test too slow: " + ms + "ms");
 }
}
