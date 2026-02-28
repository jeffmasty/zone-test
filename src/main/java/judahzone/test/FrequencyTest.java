package judahzone.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import judahzone.api.Frequency;
import judahzone.api.Hz;

public class FrequencyTest {

    private static final float EPS = 1e-3f;

    @Test
    void testLUTSizeAndMonotonic() {
        // LUT should be 128 entries and map into [MIN, MAX] monotonic increasing
        assertEquals(128, Frequency.LUT.length, "PITCH_LUT_SIZE must be 128");
        float prev = -Float.MAX_VALUE;
        for (int i = 0; i < Frequency.LUT.length; i++) {
            float v = Frequency.LUT[i];
            assertTrue(v >= Frequency.MIN - EPS && v <= Frequency.MAX + EPS,
                       "LUT value out of bounds at index " + i + ": " + v);
            if (i > 0)
                assertTrue(v >= prev - EPS, "LUT not monotonic at idx " + i + ": " + prev + " -> " + v);
            prev = v;
        }
    }

    @Test
    void testMidiToHzCacheKnownValues() {
        // A4 = MIDI 69 = 440 Hz
        assertEquals(440f, Frequency.midiToHz(69), EPS, "MIDI 69 should map to ~440 Hz");
        // extremes: midi 0 and 127 are within defined MIN/MAX
        assertTrue(Frequency.midiToHz(0) >= Frequency.MIN - EPS, "midi 0 below MIN");
        assertTrue(Frequency.midiToHz(127) <= Frequency.MAX + EPS, "midi 127 above MAX");
    }

    @Test
    void testHzToMidiRoundTrip() {
        // Round-trip several MIDI values -> Hz -> MIDI
        for (int m : new int[] {24, 48, 60, 69, 84, 100, 127}) {
            float hz = Frequency.midiToHz(m);
            int midiRound = Frequency.hzToMidi(hz);
            assertEquals(m, midiRound, "Round-trip midi->hz->midi failed for " + m);
        }
    }

    @Test
    void testHzToMidiValidation() {
        // hzToMidi(frequency, ...) should throw for non-positive frequencies
        assertThrows(IllegalArgumentException.class, () -> Frequency.hzToMidi(0f),
                     "hzToMidi must reject zero");
        assertThrows(IllegalArgumentException.class, () -> Frequency.hzToMidi(-10f),
                     "hzToMidi must reject negative");
    }

    @Test
    void testFrequencyCtorClamps() {
        Hz low = new Hz(1f);
        assertEquals(Frequency.MIN, low.freq(), EPS, "Constructor should clamp below MIN to MIN");

        Hz high = new Hz(20000f);
        assertEquals(Frequency.MAX, high.freq(), EPS, "Constructor should clamp above MAX to MAX");

        Hz ok = new Hz(440f);
        assertEquals(440f, ok.freq(), EPS, "Constructor should preserve in-range value");
    }
}
