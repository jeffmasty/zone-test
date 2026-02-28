package judahzone.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import judahzone.api.Curve;
import judahzone.data.Letter;
import judahzone.data.Postage;
import judahzone.prism.Envelope;
import judahzone.prism.Envelope.Delta;
import judahzone.util.AudioMetrics;
import judahzone.util.Constants;
import judahzone.util.Phase;


public class EnvelopeTest {

    private static final float MONO_TOL = 0.05f;
    private static final float EPSILON = 1e-6f;
    private static final int MAX_CHURN = 100_000;

    private Envelope env;
    private Letter letter;

    private float[] work512 = new float[512];
    private float[] buffer256 = new float[256];

    @BeforeEach
    void setup() {
        letter = new Letter();
        env = new Envelope(letter);
        Arrays.fill(work512, 1f);
        Arrays.fill(buffer256, 1f);
    }

    @Test
    void testDefaultConstruction() {
        assertNotNull(env);

        long atk = Math.round(Letter.DEFAULT_ATTACK_PCT * 0.01f * Letter.MAX_ATTACK_MS);
        long dk = Math.round(Letter.DEFAULT_DECAY_PCT * 0.01f * Letter.MAX_DECAY_MS);

        assertEquals(atk, env.getAttackMs(), "Attack ms should match default mapping");
        assertEquals(dk, env.getDecayMs(), "Decay ms should match default mapping");

        assertEquals(Letter.DEFAULT_ATTACK_PCT, env.getAttack(), "Attack percent should match default");
        assertEquals(Letter.DEFAULT_DECAY_PCT, env.getDecay(), "Decay percent should match default");
    }

    @Test
    void testEnvConstructionFromPostage() {
        long halfMaxAtk = Math.round(Letter.MAX_ATTACK_MS * 0.5f);
        long halfMaxDk = Math.round(Letter.MAX_DECAY_MS * 0.5f);

        Postage custom = new Postage(halfMaxAtk, halfMaxDk);
        Envelope e = new Envelope(custom);

        assertEquals(halfMaxAtk, e.getAttackMs(), "Attack ms should match custom mapping");
        assertEquals(halfMaxDk, e.getDecayMs(), "Decay ms should match custom mapping");

        assertEquals(50, e.getAttack(), "Attack percent should be preserved in constructor");
        assertEquals(50, e.getDecay(), "Decay percent should be preserved in constructor");

        assertTrue(e.getAttackSamples() > e.getAttackMs(), "too lo-fi");
        assertTrue(e.getDecaySamples() > e.getDecayMs(), "too lo-fi");

        assertFalse(env.isPlaying());
        env.trigger();
        assertTrue(env.isPlaying(), "Expected isPlaying() to be true after trigger");
        env.process();
        assertTrue(env.isPlaying(), "Expected isPlaying() to remain true after processing");
    }

    @Test
    void testAdsrLetterConstruction() {
        Letter l = new Letter(25, 60, 45, 70);
        assertEquals(25, l.attackPct());
        assertEquals(60, l.decayPct());
        assertEquals(45, l.sustainPct());
        assertEquals(70, l.releasePct());
        assertEquals(45f / 100f, l.sustainLevel());
    }

    @Test
    void testAttackStageSingleSample() {
        env.setAttack(20);
        env.setDecay(20);
        env.trigger();

        float prev = 0f;
        int atkSamples = env.getAttackSamples();
        for (int i = 0; i < atkSamples; i++) {
            float val = env.process();
            assertTrue(val >= 0f && val <= 1f, "Attack value out of range: " + val);
            assertTrue(val >= prev, "Attack should monotonically increase at " + i + " val: " + val);
            prev = val;
        }
    }

    @Test
    void testDecayStageMonotonicity() {
        env.setAttack(50);
        env.setDecay(50);
        env.trigger();

        int churn = 0;
        while (env.getStage() == Delta.ATK) {
            env.process(); // skip attack
            if (++churn > MAX_CHURN)
                fail("Timeout waiting for attack to finish; current stage=" + env.getStage());
            if (!env.isPlaying() && env.getStage() != Delta.DK)
                fail("Envelope finished before entering decay: stage=" + env.getStage());
        }

        float first = env.process(); // first decay sample
        assertTrue(first > 0.2f && first <= 1.0f,
                "First decay sample should be near 1.0 or reduced by retrig crossfade, got " + first);

        float prev = first;
        int dkSamples = env.getDecaySamples();
        for (int i = 1; i < dkSamples; i++) {
            float val = env.process();
            assertTrue(val >= 0f && val <= 1f, "Decay value out of range: " + val);
            assertTrue(val <= prev + MONO_TOL, "Decay should generally decrease at " + i + ", got " + val);
            prev = val;
        }
        assertFalse(env.isPlaying());
    }

    @Test
    void testWarmDecayStage() {
        env.setAttack(2);
        env.setDecay(50);
        env.trigger();

        int atkSamples = env.getAttackSamples();
        for (int i = 0; i < atkSamples; i++)
            env.process();

        float first = env.process();
        assertTrue(first > 0.2f && first <= 1.0f,
                "First decay sample should be near 1.0 or reduced by retrig crossfade, got " + first);

        float prev = first;
        int dkSamples = env.getDecaySamples();
        for (int i = 1; i < dkSamples; i++) {
            float val = env.process();
            assertTrue(val >= 0f && val <= 1f, "Decay value out of range: " + val);
            assertTrue(val <= prev + MONO_TOL, "Decay should generally decrease, got " + val);
            prev = val;
        }
        assertFalse(env.isPlaying());
    }

    @Test
    void testRetriggerBehavior() {
        env.setAttack(50);
        env.setDecay(30);
        env.trigger();

        for (int i = 0; i < 5; i++)
            env.process();

        float beforeRetrig = env.process();
        assertTrue(beforeRetrig > 0f && beforeRetrig <= 1f, "Value before retrigger valid? " + beforeRetrig);

        env.trigger();
        float retriggered = env.process();
        assertTrue(retriggered > 0f && retriggered <= 1f, "Retriggered value valid? " + retriggered);
        assertTrue(env.isPlaying());
    }

    @Test
    void testFullADCycleSamples() {
        env.setAttack(30);
        env.setDecay(50);
        env.trigger();

        int total = env.getAttackSamples() + env.getDecaySamples();
        float[] samples = new float[total];
        for (int i = 0; i < total; i++)
            samples[i] = env.process();

        for (int i = 1; i < env.getAttackSamples(); i++)
            assertTrue(samples[i] >= samples[i - 1], "Attack should rise");

        for (int i = env.getAttackSamples(); i < total; i++)
            assertTrue(samples[i] <= samples[i - 1] + MONO_TOL, "Decay should fall");

        assertTrue(samples[total - 1] < 0.1f, "Final sample should be near zero: " + samples[total - 1]);
        assertFalse(env.isPlaying());
    }

    @Test
    void testParameterUpdate() {
        env.trigger();
        env.process();

        env.setAttack(50);
        env.setDecay(80);
        assertEquals(50, env.getAttack());
        assertEquals(80, env.getDecay());
        assertTrue(env.getAttackSamples() > 0);
        assertTrue(env.getDecaySamples() > 0);
    }

    @Test
    void testExponentialCurveBehavior() {
        Curve exp = Curve.EXPONENTIAL;

        float start = exp.apply(0.0f);
        assertTrue(start > 0.9f, "Curve start should be near 1.0");

        float mid = exp.apply(0.5f);
        assertTrue(mid > 0f && mid < 1f);
        assertTrue(mid < start);

        float end = exp.apply(1.0f);
        assertTrue(end < 0.1f, "Curve end should approach zero");
    }

    @Test
    void testLinearCurveLinearity() {
        Curve linear = Curve.LINEAR;

        float start = linear.apply(0.0f);
        assertTrue(start > 0.9f, "Linear curve start should be near 1.0");

        float mid = linear.apply(0.5f);
        assertTrue(Math.abs(mid - 0.5f) < EPSILON, "Linear curve mid should be 0.5");

        float end = linear.apply(1.0f);
        assertTrue(end < 0.1f, "Linear curve end should be near 0.0");
    }

    @Test
    void testCurveMonotonicity() {
        Curve exp = Curve.EXPONENTIAL;
        float prev = 1f;

        for (int progress = 0; progress <= 100; progress++) {
            float normalized = progress / 100.0f;
            float val = exp.apply(normalized);
            assertTrue(val >= 0f && val <= 1f, "Curve out of range at progress " + progress);
            assertTrue(val <= prev, "Exponential curve should decay monotonically at progress " + progress);
            prev = val;
        }
    }

    @Test
    void testZeroAttackZeroDecayImmediateIdle() {
        env.setAttack(0);
        env.setDecay(0);
        env.trigger();

        float val = env.process();
        assertEquals(0f, val);
        assertFalse(env.isPlaying());
    }

    @Test
    void testReleaseViaBufferApi() {
        env.setAttack(10);
        env.setDecay(20);
        env.trigger();

        for (int i = 0; i < 5; i++)
            env.process();

        env.release();
        Arrays.fill(work512, 1.0f);
        int ret = env.process(work512);
        assertEquals(0, ret, "After release, process(buf) returns 0 when not triggered");
        assertFalse(env.isPlaying());
    }

    @Test
    void testAttackDecayMsMapping() {
        env.setAttackMs(50);
        env.setDecayMs(100);

        long atkMs = env.getAttackMs();
        long dkMs = env.getDecayMs();

        assertTrue(Math.abs(atkMs - 50) <= 5, "Attack ms mismatch: " + atkMs);
        assertTrue(Math.abs(dkMs - 100) <= 5, "Decay ms mismatch: " + dkMs);
    }

    @Test
    void testNoRTAllocationsPerformance() {
        env.setAttack(50);
        env.setDecay(50);
        env.trigger();

        long before = System.nanoTime();
        for (int i = 0; i < 48000; i++)
            env.process();
        long after = System.nanoTime();

        double ms = (after - before) / 1_000_000.0;
        assertTrue(ms < 100, "1 second of processing should be fast; took " + ms + "ms");
    }

    @Test
    void testMonoProcessingAcrossBuffers() {
        env.setAttack(20);
        env.setDecay(20);
        env.trigger();

        int atkSamples = env.getAttackSamples();
        int dkSamples = env.getDecaySamples();
        int total = atkSamples + dkSamples;

        float[] buffer = new float[512];
        float[] samples = new float[total];
        int sampleIndex = 0;

        int churn = 0;
        while (env.isPlaying()) {
            Arrays.fill(buffer, 1.0f);
            env.process(buffer); // MONO
            churn++;
            if (churn > MAX_CHURN) {
                fail("Envelope processing seems stuck in an infinite loop: " + env.getStage());
                break;
            }
            int copied = Math.min(buffer.length, total - sampleIndex);
            System.arraycopy(buffer, 0, samples, sampleIndex, copied);
            sampleIndex += copied;
        }

        for (int i = 1; i < atkSamples; i++)
            assertTrue(samples[i] >= samples[i - 1], "Attack phase non-monotonic at " + i);

        for (int i = atkSamples + 1; i < total; i++)
            assertTrue(samples[i] <= samples[i - 1] + MONO_TOL, "Decay phase non-monotonic at " + i);

        assertTrue(samples[total - 1] < 0.1f, "Final sample should near zero: " + samples[total - 1]);
        assertFalse(env.isPlaying());
    }

    @Test
    void testMonoBufferBoundaryContinuity() {
        env.setAttack(40);
        env.setDecay(60);
        env.trigger();

        float[] buf1 = new float[256];
        float[] buf2 = new float[256];
        Arrays.fill(buf1, 1.0f);
        Arrays.fill(buf2, 1.0f);

        env.process(buf1);
        float lastOfBuf1 = buf1[255];

        env.process(buf2);
        float firstOfBuf2 = buf2[0];

        float delta = Math.abs(firstOfBuf2 - lastOfBuf1);
        assertTrue(delta < MONO_TOL, "Buffer boundary discontinuity: " + delta);
    }

    @Test
    void testMonoRetriggerCrossfadeBufferApi() {
        env.setAttack(15);
        env.setDecay(40);
        env.trigger();

        float[] buf = new float[512];
        Arrays.fill(buf, 1.0f);

        for (int i = 0; i < 6; i++)
            env.process(buf);

        float beforeRetrig = buf[0];
        assertTrue(beforeRetrig >= 0f && beforeRetrig <= 1f, "Mid-decay value: " + beforeRetrig);

        env.trigger();
        Arrays.fill(buf, 1.0f);
        env.process(buf);

        float afterRetrig = buf[0];
        assertTrue(afterRetrig > 0f && afterRetrig <= 1f, "Retriggered value valid? " + afterRetrig);
        assertTrue(env.isPlaying());
    }

    @Test
    void testFastAttackBehavior() {
        env.setAttack(1);
        env.setDecay(1);
        env.trigger(0.0f);

        int atkSamples = env.getAttackSamples();
        assertTrue(atkSamples > 0, "Expected attack sample count > 0 for attack==1");

        float prev = -1f;
        for (int i = 0; i < atkSamples; i++) {
            float v = env.process();
            assertTrue(v >= 0f && v <= 1f, "Attack value out of range: " + v);
            if (i > 0)
                assertTrue(v >= prev - EPSILON, "Attack should generally increase at sample " + i);
            prev = v;
        }

        float decayFirst = env.process();
        assertTrue(decayFirst >= 0f && decayFirst <= 1f, "Decay start value in range: " + decayFirst);
        assertTrue(env.isPlaying(), "Envelope should be playing during decay");
    }

    @Test
    void testFastestAttackMs() {
        env.setAttackMs(1);
        env.setDecayMs(50);
        env.trigger(0.0f);

        int resolvedAtk = env.getAttackSamples();
        int resolvedDk = env.getDecaySamples();

        float first = env.process();

        if (resolvedAtk == 0 && resolvedDk == 0) {
            assertEquals(0f, first, "Both atk and decay are zero → process() returns 0");
            assertFalse(env.isPlaying(), "Envelope should not be playing when both stages are zero");
            return;
        }

        assertTrue(first >= 0f && first <= 1f, "First sample in range: " + first);

        float second = env.process();

        if (resolvedAtk == 0) {
            assertTrue(second < first + MONO_TOL, "Decay should not jump above first sample: " + second);
            assertTrue(env.isPlaying(), "Envelope should be playing during decay");
        } else if (resolvedAtk == 1) {
            assertTrue(first > 0.9f && first <= 1.0f, "Single-sample attack should produce near-unity first sample: " + first);
            assertTrue(second <= first + MONO_TOL, "Decay should begin at or below last attack sample: " + second);
            assertTrue(env.isPlaying(), "Envelope should be playing during decay");
        } else {
            if (env.getStage() == Delta.ATK) {
                assertTrue(second >= first - EPSILON, "During attack ramp values should not decrease: " + second);
            } else {
                assertTrue(second <= first + MONO_TOL, "After attack completes decay should not exceed last attack value: " + second);
            }
            assertTrue(env.isPlaying(), "Envelope should be playing");
        }
    }

    @Test
    void testSyntheticSineIntegration() {
        env.setAttackMs(Letter.MAX_ATTACK_MS);
        env.setDecayMs(Letter.MAX_ATTACK_MS);

        int totalSamples = env.getAttackSamples() + env.getDecaySamples();
        assertTrue(totalSamples > 0, "Total envelope length must be > 0");

        env.trigger();

        final int SR = Constants.sampleRate();
        final int N_FRAMES = Constants.bufSize();
        final double FREQ = 440.0;
        final double AMP = 0.5;

        float[] left = new float[N_FRAMES];
        float[] right = new float[N_FRAMES];
        double phase = 0.0;

        int processed = 0;
        boolean sawAttack = false;
        boolean sawDecay = false;
        float prevRms = 0f;
        float peakRms = 0f;

        while (processed < totalSamples && env.isPlaying()) {
            phase = TestUtilities.SineWave.fill(left, FREQ, SR, AMP, phase);
            System.arraycopy(left, 0, right, 0, left.length);

            env.process(left);

            float postRms = (AudioMetrics.rms(left) + AudioMetrics.rms(right)) * 0.5f;
            peakRms = Math.max(peakRms, postRms);

            Delta stage = env.getStage();
            if (stage == Delta.ATK) {
                sawAttack = true;
                assertTrue(postRms >= prevRms - 0.01f,
                    "ATTACK phase should grow or hold; prev=" + prevRms + " curr=" + postRms);
            } else if (stage == Delta.DK) {
                if (sawDecay)
                    assertTrue(postRms <= prevRms + 0.01f,
                        "DECAY at " + processed + " should shrink or hold; prev=" + prevRms + " curr=" + postRms);
                else
                    sawDecay = true;
            }

            prevRms = postRms;
            processed += N_FRAMES;
        }

        assertTrue(sawAttack, "Attack phase observed");
        assertTrue(sawDecay, "Decay phase observed");
        assertTrue(peakRms > 0.01f, "Peak RMS should be non-negligible");
        assertFalse(env.isPlaying(), "Envelope should stop after decay");
    }

    // ADSR-specific tests (from ADSRTest)

    @Test
    void testSustainStageBufferHold() {
        Letter adsr = new Letter(25, 60, 45, 70);
        Envelope e = new Envelope(adsr);
        float[] buf = new float[256];
        Arrays.fill(buf, 1f);

        e.trigger();
        int churn = 0;
        while (e.getStage() == Delta.ATK || e.getStage() == Delta.DK) {
            Arrays.fill(buf, 1f);
            e.process(buf);
            if (++churn > MAX_CHURN)
                fail("Timeout waiting for SUSTAIN stage; current stage=" + e.getStage());
            if (!e.isPlaying() && e.getStage() != Delta.SUS)
                fail("Envelope finished before entering sustain: stage=" + e.getStage());
        }
        assertEquals(Delta.SUS, e.getStage());
        Arrays.fill(buf, 1f);
        int processed = e.process(buf);
        assertTrue(processed > 0);
        float sustainValue = buf[0];
        assertTrue(sustainValue >= adsr.sustainLevel() - MONO_TOL);
        assertTrue(sustainValue <= 1f);
        assertTrue(e.isPlaying());
    }

    @Test
    void testReleaseBufferTransitions() {
        Letter adsr = new Letter(25, 60, 45, 70);
        Envelope e = new Envelope(adsr);
        float[] buf = new float[256];
        Arrays.fill(buf, 1f);

        e.trigger();

        int churn = 0;
        final int MAX_CHURN_LOCAL = 100_000;
        // wait for sustain but fail fast if it never arrives
        while (e.getStage() != Delta.SUS) {
            Arrays.fill(buf, 1f);
            e.process(buf);
            if (++churn > MAX_CHURN_LOCAL)
                fail("Timeout waiting for SUSTAIN stage; current stage=" + e.getStage());
            if (!e.isPlaying() && e.getStage() != Delta.SUS)
                fail("Envelope finished before entering sustain: stage=" + e.getStage());
        }

        e.release();

        float prev = Float.MAX_VALUE;
        churn = 0;
        while (e.isPlaying()) {
            Arrays.fill(buf, 1f);
            e.process(buf);
            float current = buf[0];
            if (prev != Float.MAX_VALUE)
                assertTrue(current <= prev + MONO_TOL);
            prev = current;
            if (++churn > MAX_CHURN_LOCAL)
                fail("Timeout waiting for envelope to finish release");
        }

        assertFalse(e.isPlaying());
        assertEquals(Delta.IDLE, e.getStage());
        assertTrue(prev <= 1f);
        assertTrue(prev >= 0f);
    }

    @Test
    void testADSRMonoReturnBehavior() {
        // 1) full-span ADSR: envelope longer than buffer, returns N_FRAMES
        Letter adsr = new Letter(25, 60, 45, 70);
        Envelope e = new Envelope(adsr);
        e.trigger();
        Arrays.fill(buffer256, 1f);
        int fullRet = e.process(buffer256);
        assertEquals(buffer256.length, fullRet, "ADSR with sustain should return full N_FRAMES while playing");
        assertTrue(e.isPlaying());

        // 2) short AD (no sustain): finishes mid-buffer
        final int atkSamples = 10;
        final int dkSamples = 15;
        Envelope shortAD = new Envelope(new Letter(atkSamples, dkSamples, 0f, 0));
        Arrays.fill(buffer256, 1f);
        shortAD.trigger();

        int ret = shortAD.process(buffer256);
        assertEquals(atkSamples + dkSamples, ret,
            "Short AD should return exact total when finishing mid-buffer");
        assertFalse(shortAD.isPlaying(), "Should be idle after AD completes");

        for (int i = 0; i < ret; i++)
            assertTrue(buffer256[i] >= 0f && buffer256[i] <= 1f);

        for (int i = ret; i < buffer256.length; i++)
            assertEquals(0f, buffer256[i], 0f);

        // 3) idle envelope returns 0
        Envelope idle = new Envelope(new Letter(1, 1, 0f, 0));
        Arrays.fill(buffer256, 1f);
        int idleRet = idle.process(buffer256);
        assertEquals(0, idleRet, "Idle envelope returns 0");
        for (int i = 0; i < buffer256.length; i++)
            assertEquals(0f, buffer256[i], 0f);
    }

    // Additional small sanity test for sum() if present (EnvelopeBasics referenced sum())
    @Test
    void testSumMatchesAttackPlusDecayIfPresent() {
        env.setAttackMs(Letter.MAX_ATTACK_MS);
        env.setDecayMs(Letter.MAX_ATTACK_MS);

        int totalSamples = env.getAttackSamples() + env.getDecaySamples();
        // Envelope may or may not have sum(); guard with reflection to avoid compile errors across versions.
        try {
            var m = Envelope.class.getMethod("sum");
            Object res = m.invoke(env);
            if (res instanceof Integer)
                assertEquals(totalSamples, (Integer) res, "sum() should equal attack + decay");
        } catch (NoSuchMethodException nsme) {
            // older/newer Envelope doesn't provide sum(), that's acceptable — skip assertion
        } catch (Exception e) {
            fail("Unexpected reflection error checking sum(): " + e);
        }
    }

	@Test
	void phaseContinuityOnRetrigger() {
		Letter l = new Letter();
		Envelope env = new Envelope(l);
		Phase p = new Phase();
		env.addPhase(p);

		final float SR = Constants.sampleRate();
		final float FREQ = 440f;
		final float inc = FREQ / SR;

		// Advance phase to establish a non-zero last sample
		for (int i = 0; i < 100; i++) p.next(inc);
		float lastSample = Phase.sin(p.get());

		// 6 ms blend: capture the immediate next sample
		env.trigger();
		float firstAfterRetrig = Phase.sin(p.next(inc));
		float delta = Math.abs(firstAfterRetrig - lastSample);
		assertTrue(delta < EPSILON, "Phase discontinuity too large: " + delta);

		// Reset and warm to a steady phase again
		p.reset();
		for (int i = 0; i < 100; i++) p.next(inc);
		lastSample = Phase.sin(p.get());

		// 25 ms blend for synths
		env.trigger(25f);
		float firstAfterLongBlend = Phase.sin(p.next(inc));
		float deltaLong = Math.abs(firstAfterLongBlend - lastSample);
		assertTrue(deltaLong < EPSILON, "Synth blend discontinuity too large: " + deltaLong);
	}

	@Test
	public void sweepWhileIdleDoesNotRetrigger() {
		Envelope env = new Envelope(new Postage(10, 30)); // short AD, no release
		float[] buf = new float[Constants.bufSize()];
		// seed buffer with ones to detect changes
		for (int i = 0; i < buf.length; i++) buf[i] = 1f;

		// trigger and consume until envelope completes
		env.trigger();
		int processed;
		do {
			// refill test source so envelope multiply is visible
			for (int i = 0; i < buf.length; i++) buf[i] = 1f;
			processed = env.process(buf);
		} while (processed > 0 && env.isPlaying());

		// At this point envelope should be idle
		assertFalse(env.isPlaying(), "Envelope should be idle after life-cycle");

		// change attack/decay while idle (user sweep)
		env.setAttack(80);
		env.setDecay(60);

		// refill buffer with ones and process again: should be silent (no retrigger)
		for (int i = 0; i < buf.length; i++) buf[i] = 1f;
		int after = env.process(buf);

		// expect no processed frames and buffer zeroed
		assertEquals(0, after, "No frames should be processed after idle param sweep");
		for (float v : buf)
			assertEquals(0f, v, 1e-6f, "Buffer should be zeroed after idle param sweep");
	}


}
