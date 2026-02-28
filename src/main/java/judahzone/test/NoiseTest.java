// Java
package judahzone.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import judahzone.prism.Envelope.Delta;
import judahzone.util.AudioMetrics;
import judahzone.util.Constants;
import net.judah.drums.noise.Ride;
import net.judah.midi.Actives;

public class NoiseTest extends DrumTest {

    private Ride osc;
    private static final float SILENCE_TOL = 1e-3f;
    private final int maxIterations = 1000;

	@BeforeEach
    void init() {
        // create a Noise-based osc (Ride extends NoiseOsc)
        osc = new Ride(new Actives(null, 0));
        drum = osc;
    }

    @Test
    void modifyingEnvelopeAfterLifeShouldNotProduceSound() {
        final int N = Constants.bufSize();
        float[] left = new float[N];
        float[] right = new float[N];

        // Trigger and process until envelope reaches OFF (or until a safety limit)
        osc.trigger(null);
        int it = 0;
        while (osc.getEnv().getStage() != Delta.IDLE && it++ < maxIterations) {
            java.util.Arrays.fill(left, 0f);
            java.util.Arrays.fill(right, 0f);
            osc.process(left, right);
        }

        // Ensure it's effectively silent after life completes
        java.util.Arrays.fill(left, 0f);
        java.util.Arrays.fill(right, 0f);
        osc.process(left, right);
        float rmsAfterLife = AudioMetrics.rms(left);
        assertTrue(rmsAfterLife < SILENCE_TOL, "Osc should be silent after life; RMS=" + rmsAfterLife);

        // Now modify envelope parameters (attack/decay) on the dead unit
        // These calls assume the Envelope API is available on getEnv() as in other tests
        osc.getEnv().setAttackMs(200L);
        osc.getEnv().setDecayMs(400L);

        // Process again and assert still silent (no re-trigger)
        java.util.Arrays.fill(left, 0f);
        java.util.Arrays.fill(right, 0f);
        osc.process(left, right);
        float rmsAfterChange = AudioMetrics.rms(left);
        assertTrue(rmsAfterChange < SILENCE_TOL, "Changing envelope on dead unit should not produce sound; RMS=" + rmsAfterChange);
    }

}
