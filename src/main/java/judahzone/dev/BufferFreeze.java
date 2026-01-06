package judahzone.dev;
// language: java
public final class BufferFreeze {
    private final float[] buf;
    private final int bufLen;
    private int writePos = 0;
    private int readPos = 0;
    private boolean frozen = false;
    private int loopLen = 1024; // default loop length in samples

    public BufferFreeze(int maxBufferLen) {
        this.bufLen = Math.max(1, maxBufferLen);
        this.buf = new float[bufLen];
        this.loopLen = Math.min(loopLen, bufLen);
    }

    public void setLoopLength(int samples) {
        loopLen = Math.min(Math.max(1, samples), bufLen);
    }

    // Engage freeze: capture last loopLen samples by setting readPos
    // Must be called from non-audio thread in sync with audio design (or using lock-free flags).
    public void freeze() {
        frozen = true;
        // align readPos to the start of the just-written window
        readPos = (writePos - loopLen) % bufLen;
        if (readPos < 0) readPos += bufLen;
    }

    public void unfreeze() {
        frozen = false;
    }

    // Allocation-free process. Writes incoming audio into circular buffer.
    // When frozen, outputs the looping buffer; otherwise passes input through.
    public void process(float[] in, float[] out, int frames) {
        for (int i = 0; i < frames; i++) {
            float input = in[i];
            // keep buffer always filled (ring write)
            buf[writePos] = input;
            writePos++;
            if (writePos >= bufLen) writePos = 0;

            if (frozen) {
                float sample = buf[readPos];
                out[i] = sample;
                readPos++;
                if (readPos >= bufLen) readPos = 0;
            } else {
                out[i] = input;
            }
        }
    }
}
