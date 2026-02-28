package judahzone.dev;

import java.security.InvalidParameterException;

import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;

public class WindowTest {

	public static void error() {
		final int FFT_SIZE = 4096; // any power of 2 should work
	    final FFT noWindow = new FFT(FFT_SIZE); // pass
	    final FFT withWindow = new FFT(FFT_SIZE, new HammingWindow()); // cause of later error

	    final float[] test1 = new float[FFT_SIZE * 2];
	    final float[] test2 = new float[FFT_SIZE * 2];
	    float[] magnitudes; // FFT_SIZE / 2
	    float max; // test var

		// generate a 440 Hz tone at amplitude 0.6
		final float[] sinWave = new float[FFT_SIZE];
		final double S_RATE = 48000.0; // any should work
	    final double TWO_PI = 2.0 * Math.PI;
		final double hz = 440.0;
	    final double amplitude = 0.6;
	    final double step = TWO_PI * hz / S_RATE;
		double phase = 0;
	    for (int i = 0; i < sinWave.length; i++) {
	        sinWave[i] = (float) (amplitude * Math.sin(phase));
	        phase += step;
	        if (phase >= TWO_PI) // clamp
	        	phase -= TWO_PI * Math.floor(phase / TWO_PI);
	    }

	    // copy audio into first half of test arrays  (zeros are in last half)
		System.arraycopy(sinWave, 0, test1, 0, FFT_SIZE);
		System.arraycopy(sinWave, 0, test2, 0, FFT_SIZE);

		max = 0;
		noWindow.forwardTransform(test1);
		magnitudes = new float[FFT_SIZE / 2];
		noWindow.modulus(test1, magnitudes);
		for (int i = 0 ; i < magnitudes.length; i++) {
		    float m = magnitudes[i];
		    if (!Float.isFinite(m) || m < 0f)
		    	throw new InvalidParameterException(i + ": " + m);
		    if (m > max)
		    	max = m;
		}
		assert max > 0;
		System.out.println("no-window transform passed: " + max);

		max = 0;
		withWindow.forwardTransform(test2); // <-- window causes error
		magnitudes = new float[FFT_SIZE / 2];
		withWindow.modulus(test2, magnitudes);
		for (int i = 0 ; i < magnitudes.length; i++) {
		    float m = magnitudes[i];
		    if (!Float.isFinite(m) || m < 0f)
		    	throw new InvalidParameterException(i + ": " + m);
		    if (m > max)
		    	max = m;
		}
		assert max > 0;
		System.out.println("window transform passed: " + max);
	}

}
