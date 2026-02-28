package judahzone.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import judahzone.data.Letter;
import judahzone.data.Postage;

public class LetterTest {

    @Test
    void testPercentToSamplesAndPercentReportingConsistency() {
        // exercise a handful of percent values (0, 1, 10, 25, 100)
        int[] pctVals = {0, 1, 10, 25, 100};
        for (int pct : pctVals) {
            int samples = Letter.percentToSamples(pct, Letter.MAX_ATTACK_MS);
            // zero percent must map to zero samples
            if (pct == 0) {
                assertEquals(0, samples, "0% should yield 0 samples");
            } else {
                // non-zero percent must yield at least one sample (quantized)
                assertTrue(samples >= 1, "non-zero percent should produce >=1 sample");
            }

            // samples->percent must be in 0..100
            int roundPct = Letter.samplesToPercent(samples, Letter.MAX_ATTACK_MS);
            assertTrue(roundPct >= 0 && roundPct <= 100, "samplesToPercent should clamp 0..100");

            // a Letter constructed with percent-based ctor stores the quantized samples
            Letter l = new Letter(pct, Letter.DEFAULT_DECAY_PCT);
            assertEquals(samples, l.attackSamples(), "Letter should store the quantized attack samples");
            // attackPct() is derived from stored samples (may differ from input pct due to quantization)
            assertEquals(roundPct, l.attackPct(), "attackPct() should reflect percent derived from quantized samples");
        }
    }

    @Test
    void testPercent100EqualsMaxMsSamples() {
        int sPct100 = Letter.percentToSamples(100, Letter.MAX_ATTACK_MS);
        int sMsMax = Letter.msToSamples(Letter.MAX_ATTACK_MS);
        assertEquals(sMsMax, sPct100, "100% should map to samples equivalent to MAX_ATTACK_MS");
    }

    @Test
    void testMsToSamplesAndBackRoundtrip() {
        // pick a few ms values to test round-trip fidelity
        int[] msVals = {0, 1, 4, 10, 50, Letter.MAX_DECAY_MS};
        for (int ms : msVals) {
            int samples = Letter.msToSamples(ms);
            long msBack = Letter.samplesToMs(samples);
            // allow off-by-one due to rounding
            assertTrue(Math.abs(msBack - ms) <= 1,
                "ms -> samples -> ms should round-trip within 1ms (was " + ms + " -> " + msBack + ")");
        }
    }

    @Test
    void testPostage() {
    	int atkMs = 500;
    	int dkMs = 999;

    	Postage p = new Postage(atkMs, dkMs);
    	Letter l = new Letter(p);

    	assertEquals(atkMs, l.attackMs(), "Postage attack ms should round-trip through Letter");
    	assertEquals(dkMs, l.decayMs(), "Postage decay ms should round-trip through Letter");


    	Postage p2 = new Postage(Letter.MAX_ATTACK_MS, Letter.MAX_DECAY_MS);
    	Letter l2 = new Letter(p2);

    	assertEquals(100, l2.attackPct(), "Postage with max attack ms should round-trip through Letter");
    	assertEquals(100, l2.decayPct(), "Postage with max decay ms should round-trip through Letter");

	}

    void testMS() {
    	Letter l = new Letter(48000, 48000);
    	assertEquals(1000, l.attackMs(), "48000 samples should correspond to 1000ms at NSR");
    	assertEquals(1000, l.decayMs(), "48000 samples should correspond to 1000ms at NSR");

    	Letter l2 = new Letter(48, 48);
    	assertEquals(1, l2.attackMs(), "48 samples should correspond to 1ms at NSR");
    	assertEquals(1, l2.decayMs(), "48 samples should correspond to 1ms at NSR");

    	Letter l3 = new Letter(24,24);
		assertEquals(0, l3.attackMs(), "24 samples should correspond to 0ms at NSR (rounded down)");
		assertEquals(0, l3.decayMs(), "24 samples should correspond to 0ms at NSR (rounded down)");

    	Letter l4 = new Letter(25, 25);
		assertEquals(1, l4.attackMs(), "25 samples should correspond to 1ms at NSR (rounded up)");
		assertEquals(1, l4.decayMs(), "25 samples should correspond to 1ms at NSR (rounded up)");

    }

    @Test
    void testZeroBehaviorPreserved() {
        Letter l = new Letter(0, 0);
        assertEquals(0, l.attackSamples(), "attack samples should be 0 for 0%");
        assertEquals(0, l.decaySamples(), "decay samples should be 0 for 0%");
        assertEquals(0, l.attackPct(), "attack percent reported should be 0");
        assertEquals(0, l.decayPct(), "decay percent reported should be 0");
    }
}
