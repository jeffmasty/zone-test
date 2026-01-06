package judahzone.dev;
// language: java
public final class SampleAndHold {
    private float held = 0f;
    private int samplesRemaining = 0;
    private int holdPeriod = 1; // how many output samples to hold each sampled value

    public void setHoldPeriod(int samples) {
        this.holdPeriod = Math.max(1, samples);
    }

    // Trigger a new hold using supplied value (safe to call from control thread)
    public void trigger(float sample) {
        held = sample;
        samplesRemaining = holdPeriod;
    }

    // process must be allocation-free and fast; in/out are same length
    public void process(float[] in, float[] out, int frames) {
        for (int i = 0; i < frames; i++) {
            if (samplesRemaining <= 0) {
                // sample a new value from input and start holding
                held = in[i];
                samplesRemaining = holdPeriod;
            }
            out[i] = held;
            samplesRemaining--;
        }
    }
}
