package judahzone.dev;


import java.io.File;

/** Time different FFT libraries: run each library against audio file again and again, measuring time taken.
 *
 *  1. warm up the JVM ~ 1 minute.
 *  2. sleep() 20 secs.
 *  3. run spectrum tests ~ 3 minutes each.
 *  4. sleep() 20 secs.
 *  5. run IR tests ~ 3 minutes each.
 *  6. report results.
 *
 *  Needed wrapper:   How FFT is used in JudahZone vs how each library expects to be called.
 *
 *  Specrum tests based off of judahzone.fx.analysis.Transformer
 *
 *  IR tests based off of judahzone.fx.convolution.Convolver
 *
 *  FFT libraries available:
 *
 *  be.tarsos  (already in project)
 *
 *
 *  com.fft  (open project in Eclipse, lots of AI instructions)
 * fast-fourier-transform
 * 2.0.0-SNAPSHOT https://mvnrepository.com/artifact/com.fft/fast-fourier-transform
 *
 *
 *   <groupId>com.fudcom.kjdss</groupId> (open project in Eclipse) KJFFT.java
 *   <artifactId>kjdss-root</artifactId>
 *   <version>1.3.2-SNAPSHOT</version>
 *   <packaging>pom</packaging>
 *
 *  Notes and commented files : net.judah.gui.fft_temp
 *  (I had FFTW native library hooked up pretty easy)
 *
 *  JTransform (not yet integrated)
 *
 */

import java.text.NumberFormat;

import judahzone.data.Recording;
import judahzone.util.MP3;

// import com.fudcom.kjdss.KJFFT;

//import com.fft.core.FFT;
//import com.fft.core.FFTResult;
//import com.fft.utils.FFTUtils;


/**
 * Time different FFT libraries: run each library against audio file again and
 * again, measuring time taken.
 *
 * <p>1. warm up the JVM ~ 1 minute.
 * <p>2. sleep() 20 secs.
 * <p>3. run spectrum tests ~ 3 minutes each.
 * <p>4. sleep() 20 secs.
 * <p>5. run IR tests ~ 3 minutes each.
 * <p>6. report results.
 *
 * <p>Needed wrapper: How FFT is used in JudahZone vs how each library expects
 * to be called.
 *
 * <p>Spectrum tests based off of judahzone.fx.analysis.Transformer
 *
 * <p>IR tests based off of judahzone.fx.convolution.Convolver
 *
 * <p>FFT libraries available:
 *
 * <p>be.tarsos (already in project)
 *
 * <p>com.fft (open project in Eclipse, lots of AI instructions)
 * fast-fourier-transform 2.0.0-SNAPSHOT
 * https://mvnrepository.com/artifact/com.fft/fast-fourier-transform
 *
 * <p>com.fudcom.kjdss (open project in Eclipse) KJFFT.java
 * <artifactId>kjdss-root</artifactId>
 * <version>1.3.2-SNAPSHOT</version>
 * <packaging>pom</packaging>
 *
 * <p>Notes and commented files : net.judah.gui.fft_temp (I had FFTW native
 * library hooked up pretty easy)
 *
 * <p>JTransform (not yet integrated)
 */
public class FFTCompare {

    private static final int FFT_SIZE = 4096;
    private static final int WARMUP_ITERATIONS = 10000;
    private static final int BENCHMARK_ITERATIONS = 50000;

	static final String DEFAULT_FILE = "/home/judah/Music/Stubborn All-Stars - Open Season/Stubborn All-Stars - 09 - Catch that Train.mp3";
	static Recording tape;

    public static void test() {
        System.out.println("Starting FFT Benchmark...");

        File absolute = new File(DEFAULT_FILE);
	    tape = MP3.load(absolute);

        System.out.printf("FFT Size: %d, Warmup Iterations: %s, Benchmark Iterations: %s%n%n",
                FFT_SIZE, NumberFormat.getInstance().format(WARMUP_ITERATIONS),
                NumberFormat.getInstance().format(BENCHMARK_ITERATIONS));

     // Prepare interleaved complex input for in-place libraries (real, imag, real, imag, ...)
        float[] interleaved = new float[FFT_SIZE * 2];
        for (int i = 0; i < FFT_SIZE; i++) {
            interleaved[2 * i] = (float) Math.sin(2 * Math.PI * 440.0 * i / 44100.0); // real
            interleaved[2 * i + 1] = 0f; // imag
        }

        // Prepare padded real input for KJFFT (round up to next power of two)
//        int kjInputSize = 1;
//        while (kjInputSize < FFT_SIZE) kjInputSize <<= 1;
//        float[] kjReal = new float[kjInputSize];
//        for (int i = 0; i < FFT_SIZE; i++) {
//            kjReal[i] = (float) Math.sin(2 * Math.PI * 440.0 * i / 44100.0);
//        }

        // --- Libraries ---
        FFTLibraryWrapper tarsos = new TarsosWrapper(FFT_SIZE); // winner!
        System.out.println("--- Tarsos DSP ---");
        runBenchmark(tarsos, interleaved.clone());

//        FFTLibraryWrapper jtransforms = new JTransformsWrapper(FFT_SIZE);
//        System.out.println("\n--- JTransforms (FloatFFT_1D) ---");
//        runBenchmark(jtransforms, interleaved.clone());

        //System.out.println("\n--- KJFFT ---");
        //FFTLibraryWrapper kjfft = new KJFFTWrapper(FFT_SIZE);
        //runBenchmark(kjfft, kjReal.clone()); // pass padded real buffer (no per-iteration copy inside wrapper)
        //System.out.println("\nBenchmark complete.");

    }

    public static void main(String[] args) {
    	test();
    	test();
    }

    private static void runBenchmark(FFTLibraryWrapper fft, float[] data) {
        System.out.println("Warming up...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            fft.forwardTransform(data);
        }

        System.out.println("Running benchmark...");
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            fft.forwardTransform(data);
        }
        long endTime = System.nanoTime();

        long totalTime = endTime - startTime;
        double avgTime = (double) totalTime / BENCHMARK_ITERATIONS;

        System.out.printf("Total time for %s iterations: %.3f ms%n",
                NumberFormat.getInstance().format(BENCHMARK_ITERATIONS), totalTime / 1_000_000.0);
        System.out.printf("Average time per transform: %.3f ns%n", avgTime);
    }
}

interface FFTLibraryWrapper {
    void forwardTransform(float[] buffer);
    void modulus(float[] buffer, float[] amplitudes);
}

class TarsosWrapper implements FFTLibraryWrapper {
    private final be.tarsos.dsp.util.fft.FFT fft;

    public TarsosWrapper(int fftSize) {
        fft = new be.tarsos.dsp.util.fft.FFT(fftSize);
    }

    @Override
    public void forwardTransform(float[] buffer) {
        fft.forwardTransform(buffer);
    }

    @Override
    public void modulus(float[] buffer, float[] amplitudes) {
        fft.modulus(buffer, amplitudes);
    }
}

//class ComFftWrapper implements FFTLibraryWrapper {
//    private final FFT fft;
//    private final double[] doubleBuffer;
//    private FFTResult result;
//
//    public ComFftWrapper(int fftSize) {
//        // This library uses a factory and works with double[]
//        this.fft = FFTUtils.createFactory().createFFT(fftSize);
//        this.doubleBuffer = new double[fftSize];
//    }
//
//    @Override
//    public void forwardTransform(float[] buffer) {
//        // This library is not in-place and uses double[]
//        for (int i = 0; i < doubleBuffer.length; i++) {
//            doubleBuffer[i] = buffer[i];
//        }
//        result = fft.transform(doubleBuffer);
//    }
//
//    @Override
//    public void modulus(float[] buffer, float[] amplitudes) {
//        if (result == null) {
//            throw new IllegalStateException("forwardTransform must be called before modulus.");
//        }
//        // The 'buffer' parameter is ignored as the result is stored internally.
//        double[] magnitudes = result.getMagnitudes();
//        for (int i = 0; i < amplitudes.length; i++) {
//            amplitudes[i] = (float) magnitudes[i];
//        }
//    }
//}



//class JTransformsWrapper implements FFTLibraryWrapper {
//    private final FloatFFT_1D fft;
//    private final int n;
//
//    public JTransformsWrapper(int fftSize) {
//        this.n = fftSize;
//        this.fft = new FloatFFT_1D(fftSize);
//    }
//
//    @Override
//    public void forwardTransform(float[] buffer) {
//        // expects interleaved complex float array of length 2*n
//        fft.complexForward(buffer);
//    }
//
//    @Override
//    public void modulus(float[] buffer, float[] amplitudes) {
//        int len = Math.min(amplitudes.length, n);
//        for (int i = 0; i < len; i++) {
//            float re = buffer[2 * i];
//            float im = buffer[2 * i + 1];
//            amplitudes[i] = (float) Math.hypot(re, im);
//        }
//    }
//}



/**
 * Adapter for KJFFT that implements the test harness FFTLibraryWrapper.
 *
 * - Copies the first N samples from the provided buffer into a reusable input buffer.
 * - Calls KJFFT.calculate(...) which returns magnitude bins.
 * - modulus(...) copies the last computed magnitudes into the caller-provided array.
 */
//class KJFFTWrapper implements FFTLibraryWrapper {
//	private final KJFFT kjfft;
//	private final int kjOutputSize;          // output magnitudes length (ss2)
//	private float[] lastMagnitudes;          // last result from KJFFT.calculate(...)
//
//	public KJFFTWrapper(int fftSize) {
//	    this.kjfft = new KJFFT(fftSize);
//	    this.kjOutputSize = kjfft.getOutputSampleSize();
//	    this.lastMagnitudes = new float[kjOutputSize];
//	}
//
//	@Override
//	public void forwardTransform(float[] buffer) {
//	    // Expect a real-valued buffer (length == kjfft.getInputSampleSize() or <=)
//	    // KJFFT.calculate will handle padding internally; avoid copying here.
//	    lastMagnitudes = kjfft.calculate(buffer);
//	}
//
//	@Override
//	public void modulus(float[] buffer, float[] amplitudes) {
//	    if (lastMagnitudes == null) {
//	        throw new IllegalStateException("forwardTransform must be called before modulus.");
//	    }
//	    int len = Math.min(amplitudes.length, lastMagnitudes.length);
//	    System.arraycopy(lastMagnitudes, 0, amplitudes, 0, len);
//	    if (amplitudes.length > len) {
//	        Arrays.fill(amplitudes, len, amplitudes.length, 0f);
//	    }
//	}
//}



//<repository>
//<id>ossrh</id>
//<url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
//</repository>
//
//FFTW
//* https://github.com/fudcom/kjdss/tree/master
//<dependency>
//	<groupId>com.fudcom.kjdss</groupId>
//	<artifactId>kjdss-root</artifactId>
//	<version>1.3.2-SNAPSHOT</version>
//</dependency>
//<dependency>
//  <groupId>org.bytedeco</groupId>
//  <artifactId>fftw</artifactId>
//  <version>3.3.9-1.5.6</version>
//</dependency>
//<dependency>
//  <groupId>org.bytedeco</groupId>
//  <artifactId>fftw-platform</artifactId>
//  <version>3.3.9-1.5.6</version>
//</dependency>
//<dependency>
//  <groupId>org.bytedeco</groupId>
//  <artifactId>javacpp</artifactId>
//  <version>1.5.6</version>
//</dependency>
//<dependency>
//  <groupId>org.apache.commons</groupId>
//  <artifactId>commons-math3</artifactId>
//  <version>3.6.1</version>
//</dependency>