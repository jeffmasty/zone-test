/*
 *      _______                       _____   _____ _____
 *     |__   __|                     |  __ \ / ____|  __ \
 *        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
 *        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
 *        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
 *        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
 *
 * -------------------------------------------------------------
 *
 * TarsosDSP is developed by Joren Six at IPEM, University Ghent
 *
 * -------------------------------------------------------------
 *
 *  Info: http://0110.be/tag/TarsosDSP
 *  Github: https://github.com/JorenSix/TarsosDSP
 *  Releases: http://0110.be/releases/TarsosDSP/
 *
 *  TarsosDSP includes modified source code by various authors,
 *  for credits and info, see README.
 *
 */


package judahzone.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import judahzone.util.Constants;


public abstract class TestUtilities {

    /**
     * Constructs and returns a buffer of a two seconds long pure sine of 440Hz
     * sampled at 44.1kHz.
     *
     * @return A buffer of a two seconds long pure sine (440Hz) sampled at
     *         44.1kHz.
     */
    public static float[] audioBufferSine() {
        final double sampleRate = 44100.0;
        final double f0 = 440.0;
        final double amplitudeF0 = 0.5;
        final double seconds = 4.0;
        return audioBufferSine(sampleRate,f0,(int) (sampleRate*seconds), amplitudeF0);
    }

    public static float[] audioBufferSine(double sampleRate, double f0, int numberOfSamples, double amplitudeF0) {
        final float[] buffer = new float[numberOfSamples];
        for (int sample = 0; sample < buffer.length; sample++) {
            final double time = sample / sampleRate;
            buffer[sample] = (float) (amplitudeF0 * Math.sin(2 * Math.PI * f0 * time));
        }
        return buffer;
    }

    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     *         of a flute played double forte at A6 (theoretically 440Hz) without vibrato
     */
    public static float[] audioBufferFlute() {
        int lengthInSamples = 4096;
        String file = "flute.novib.ff.A4.wav";
        return audioBufferFile(file,lengthInSamples);
    }

    public static File fluteFile(){
        String file = "flute.novib.ff.A4.wav";
        final URL url = ClassLoader.getSystemResource(file);
        try {
            return new File(new URI(url.toString()));
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static File ccirFile(){
        final URL url = ClassLoader.getSystemResource("CCIR_04221.ogg");
        try {
            return new File(new URI(url.toString()));
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static File onsetsAudioFile(){
        String file = "NR45.wav";
        final URL url = ClassLoader.getSystemResource(file);
        try {
            return new File(new URI(url.toString()));
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static File sineOf4000Samples(){
        String file = "4000_samples_of_440Hz_at_44.1kHz.wav";
        final URL url = ClassLoader.getSystemResource(file);
        try {
            return new File(new URI(url.toString()));
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     *         of a flute played double forte at B6 (theoretically 1975.53Hz) without vibrato
     */
    public static float[] audioBufferHighFlute() {
        int lengthInSamples = 4096;
        String file = "flute.novib.ff.B6.wav";
        return audioBufferFile(file,lengthInSamples);
    }

    /**
     * Reads the contents of a file.
     *
     * @param name
     *            the name of the file to read
     * @return the contents of the file if successful, an empty string
     *         otherwise.
     */
    public static String readFile(final String name) {
        FileReader fileReader = null;
        final StringBuilder contents = new StringBuilder();
        try {
            final File file = new File(name);
            if (!file.exists()) {
                throw new IllegalArgumentException("File " + name + " does not exist");
            }
            fileReader = new FileReader(file);
            final BufferedReader reader = new BufferedReader(fileReader);
            String inputLine = reader.readLine();
            while (inputLine != null) {
                contents.append(inputLine).append("\n");
                inputLine = reader.readLine();
            }
            reader.close();
        } catch (final IOException i1) {
            throw new RuntimeException(i1);
        }
        return contents.toString();
    }

    /**
     * Reads the contents of a file in a jar.
     *
     * @param path
     *            the path to read e.g. /package/name/here/help.html
     * @return the contents of the file when successful, an empty string
     *         otherwise.
     */
    public static String readFileFromJar(final String path) {
        final StringBuilder contents = new StringBuilder();
        final URL url = ClassLoader.getSystemResource(path);
        URLConnection connection;
        try {
            connection = url.openConnection();
            final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
            final BufferedReader reader = new BufferedReader(inputStreamReader);
            String inputLine;
            inputLine = reader.readLine();
            while (inputLine != null) {
                contents.append(new String(inputLine.getBytes(), "UTF-8")).append("\n");
                inputLine = reader.readLine();
            }
            reader.close();
        } catch (final IOException | NullPointerException e) {
            throw new RuntimeException(e);
        }
        return contents.toString();
    }

    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     *         of a piano played double forte at A4 (theoretically 440Hz)
     */
    public static float[] audioBufferPiano() {
        int lengthInSamples = 4096;
        String file = "piano.ff.A4.wav";
        return audioBufferFile(file,lengthInSamples);
    }

    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     *         of a piano played double forte at C3 (theoretically 130.81Hz)
     */
    public static float[] audioBufferLowPiano() {
        int lengthInSamples = 4096;
        String file = "piano.ff.C3.wav";
        return audioBufferFile(file,lengthInSamples);
    }

    private static float[] audioBufferFile(String file,int lengthInSamples){
        float[] buffer = new float[lengthInSamples];
        try {
            final URL url = ClassLoader.getSystemResource(file);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            AudioFormat format = audioStream.getFormat();
            TarsosDSPAudioFloatConverter converter = TarsosDSPAudioFloatConverter.getConverter(JVMAudioInputStream.toTarsosDSPFormat(format));
            byte[] bytes = new byte[lengthInSamples * format.getSampleSizeInBits()];
            audioStream.read(bytes);
            converter.toFloatArray(bytes, buffer);
        } catch (IOException e) {
            throw new Error("Test audio file should be present.");
        } catch (UnsupportedAudioFileException e) {
            throw new Error("Test audio file format should be supported.");
        }
        return buffer;
    }

    /**
     *
     * @return a half a second long silent buffer (all zeros), at 44.1kHz.
     */
    public static float[] audioBufferSilence() {
        final double sampleRate = 44100.0;
        final double seconds = 0.5;
        final float[] buffer = new float[(int) (seconds * sampleRate)];
        return buffer;
    }

    /* SIZE: 1024
	•  Precision: 1024 entries ≈ 10 bits → ~0.1 dB error (acceptable for audio).
	•  Memory: ~4 KB (1024 × 4 bytes float). Negligible; fits cache L1.
 	•  Modulation: One multiply per sample to scale output by index/level
 	•  Callers: use interpolation: Linear or 3-tap Lagrange adds ~2–3 cycles, eliminates audible stepping.
 */
public static class SineWave {

	private static final double defaultFrequency = 440.;
	private static final double defaultAmplitude = 0.75;
	private static final double defaultSR = Constants.sampleRate();

    public static double[] generateSineWave(double amplitude, double frequency, double samplingRate, int numSamples) {
        double[] sineWave = new double[numSamples];
        double angularFrequency = 2 * Math.PI * frequency / samplingRate;

        for (int i = 0; i < numSamples; i++) {
            sineWave[i] = amplitude * Math.sin(angularFrequency * i);
        }

        return sineWave;
    }

    public static void test() {
        int numSamples = 1024; // Number of samples
        double[] sineWave = generateSineWave(defaultAmplitude, defaultFrequency, defaultSR, numSamples);

        // Print the generated sine wave samples
        for (int i = 0; i < numSamples; i++) {
            System.out.println("Sample " + i + ": " + sineWave[i]);
        }
    }

    // test2: benchmark LUT+interpolation vs direct Math.sin


    /**Fill `buf` with a Math.sin wave at freqHz. Returns the updated phase (radians) to use
	 * for the next call so the tone is continuous across buffers.
	 * @param buf       float[] buffer to fill (length = FFT_SIZE)
	 * @param freqHz    desired frequency in Hz
	 * @param sampleRate sample rate in Hz (e.g. 48000.0)
	 * @param amplitude amplitude (0..1)
	 * @return new phase in radians to pass to the next call
	 */
	public static double fill(float[] buf, double freqHz, double sampleRate, double amplitude, double phase) {

		if (buf == null || buf.length == 0) return phase;
	    final double twoPi = 2.0 * Math.PI;
	    // phase increment per sample (radians)
	    final double phaseInc = twoPi * freqHz / sampleRate;

	    // keep amplitude safe
	    final double a = Math.max(0.0, Math.min(1.0, amplitude));

	    for (int i = 0; i < buf.length; i++) {
	        buf[i] = (float) (a * Math.sin(phase));
	        phase += phaseInc;
	        // wrap phase into [0, 2PI) to avoid unbounded growth
	        if (phase >= twoPi) phase -= twoPi * Math.floor(phase / twoPi);
	    }
	    return phase;
	}

	// Math.sin
	public static double fill(float[] buf) {
    	return fill(buf, defaultFrequency, defaultSR, defaultAmplitude, 0);
    }

	// Math.sin
    public static double fill(float[] buf, double phase) { // chain
    	return fill(buf, defaultFrequency, defaultSR, defaultAmplitude, phase);
    }

}


}