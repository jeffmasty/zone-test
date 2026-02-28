package judahzone.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import judahzone.api.Hz;
import judahzone.data.Postage;
import judahzone.data.Shape;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.Gain.GainT;
import judahzone.prism.Envelope.Delta;
import judahzone.util.AudioMetrics;
import judahzone.util.Constants;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;
import net.judah.drums.fm.Algorithm;
import net.judah.drums.fm.DX7;
import net.judah.drums.fm.DX9;
import net.judah.drums.fm.FMOsc;
import net.judah.drums.fm.FMRatio;
import net.judah.drums.fm.FMRatio.RatioSpec;
import net.judah.drums.fm.FMSetup.FMOscSetup;
import net.judah.drums.fm.FMSetup.OpSetup;
import net.judah.drums.fm.Topology;
import net.judah.gui.drums.OscTuning;
import net.judah.midi.Actives;

/** FM Drum initialization, audio generation, parameter modulation, algorithm switching, and edge cases. */
public class FMTest extends DrumTest {

	private DrumSetup setup;
	private Actives mine;
	private FMOsc osc;

	private PitchDetector detector = PitchEstimationAlgorithm.MPM
			.getDetector(Constants.sampleRate(), Constants.bufSize());

	private static final int bufSize = 256;

	@BeforeEach
	void init() {
		setup = new DrumSetup(
			DrumType.Snare,
			new GainT(1, 1, 0.5f, 0.5f),
			new Postage(10, 100),
			new Freqs(new Coord(20, 1), new FilterT(440, 1, 1), new Coord(10000, 1)),
			new Hz(220),
			new String[] {"Algo"}
		);
		mine = new Actives(null, 0);

		OpSetup basic = new OpSetup(0.9f, 0f, 1f, new Postage(10, 40), Shape.SIN);
		OpSetup[] quick = new OpSetup[DX9.OP_COUNT];
		for (int j = 0; j < DX9.OP_COUNT; j++)
			quick[j] = new OpSetup(basic);

		Algorithm a = DX9.getAlgorithms()[6];
		FMOscSetup radio = new FMOscSetup(a, quick);
		osc = new FMOsc(setup, radio, mine);
		drum = osc;
	}

    @Test
    void dx11AlgoTopology() {
        for (int preset = 1; preset <= DX7.names.length; preset++) {
        	try {
            Algorithm algo = DX7.get(preset);
            Algorithm validated = Topology.build(DX7.OP_COUNT, algo.modulatorsFor(), algo.scaleFor(), algo.feedbackFlags());
            assertEquals(DX7.OP_COUNT, validated.evalOrder().length);

            List<String> errors = Topology.validate(algo, DX7.OP_COUNT);
            assertTrue(errors.isEmpty(), "Preset " + preset + " has topology errors: " + errors);

        	} catch (Throwable t) {
        		throw new RuntimeException("Failed to validate DX7 algo for preset " + preset, t);
        	}
        }
    }

    @Test
    void dx9AlgoTopology() {

    	for (int i = 0; i < DX9.ALGO_COUNT; i++) {
    		Algorithm algo = DX9.getAlgorithms()[i];
    		Algorithm validated = Topology.build(DX9.OP_COUNT, algo.modulatorsFor(), algo.scaleFor(), algo.feedbackFlags());
            List<String> errors = Topology.validate(algo, DX9.OP_COUNT);
            assertTrue(errors.isEmpty(), "DX9 " + i + " errors: " + errors);
            assertEquals(DX9.OP_COUNT, validated.evalOrder().length);

    	}

        for (Algorithm algo : DX9.getAlgorithms()) {
            Algorithm validated = Topology.build(DX9.OP_COUNT, algo.modulatorsFor(), algo.scaleFor(), algo.feedbackFlags());
            List<String> errors = Topology.validate(algo, DX9.OP_COUNT);
            assertTrue(errors.isEmpty(), "Preset " + algo + " has topology errors: " + errors);

            assertEquals(DX9.OP_COUNT, validated.evalOrder().length);
        }
    }



	@Test
	void testInitializationState() {
		assertNotNull(osc, "FMOsc should initialize");
		assertEquals(DX9.OP_COUNT, osc.getOpCount(), "Should have 4 operators");
		assertEquals(DX9.getAlgorithms()[6], osc.getAlgo(), "Algorithm 6 should be set");
		assertTrue(osc.getFundamental() > 0, "Frequency should be positive");
	}

	@Test
	void testBasicAudioGeneration() {
		Arrays.fill(sumL, 0f);
		Arrays.fill(sumR, 0f);

		osc.trigger(null);
		osc.process(sumL, sumR);

		float rmsL = AudioMetrics.rms(sumL);
		float rmsR = AudioMetrics.rms(sumR);

		assertTrue(rmsL > EPS, "Left channel should produce audio; RMS=" + rmsL);
		assertTrue(rmsR > EPS, "Right channel should produce audio; RMS=" + rmsR);
		assertTrue(rmsL < 1.1f, "Left channel should not clip; RMS=" + rmsL);
		assertTrue(rmsR < 1.1f, "Right channel should not clip; RMS=" + rmsR);
	}

	@Test
	void testTriggerStartsEnvelope() {
		Arrays.fill(sumL, 0f);
		Arrays.fill(sumR, 0f);

		osc.trigger(null);
		osc.process(sumL, sumR);
		float rms1 = AudioMetrics.rms(sumL);

		Arrays.fill(sumL, 0f);
		Arrays.fill(sumR, 0f);
		osc.process(sumL, sumR);
		float rms2 = AudioMetrics.rms(sumL);

		assertTrue(rms1 > EPS, "First frame after trigger should have audio");
		assertTrue(rms2 > EPS, "Envelope should continue; envelope plays " + bufSize + " samples");
	}

	@Test
	void testLevelParameterAffectsOutput() {
		// High level
		Arrays.fill(sumL, 0f);
		Arrays.fill(sumR, 0f);
		osc.trigger(null);
		osc.process(sumL, sumR);
		float rmsHigh = AudioMetrics.rms(sumL);

		// Reset and use low level
		init(); // reinit for clean state
		OpSetup lowLevel = new OpSetup(0.1f, 0f, 1f, new Postage(10, 40), Shape.SIN);
		OpSetup[] ops = new OpSetup[DX9.OP_COUNT];
		for (int j = 0; j < DX9.OP_COUNT; j++)
			ops[j] = new OpSetup(lowLevel);
		FMOscSetup setup = new FMOscSetup(DX9.getAlgorithms()[6], ops);
		FMOsc fmLow = new FMOsc(this.setup, setup, mine);

		Arrays.fill(sumL, 0f);
		Arrays.fill(sumR, 0f);
		fmLow.trigger(null);
		fmLow.process(sumL, sumR);
		float rmsLow = AudioMetrics.rms(sumL);

		assertTrue(rmsLow < rmsHigh * 0.5f, "Low level (" + rmsLow + ") should be < high (" + rmsHigh + ") * 0.5");
	}

	@Test
	void testFeedbackModulation() {
		OpSetup withFb = new OpSetup(0.9f, 0.9f, 1f, new Postage(10, 40), Shape.SIN);
		OpSetup noFb = new OpSetup(0.9f, 0f, 1f, new Postage(10, 40), Shape.SIN);

		// With feedback
		OpSetup[] opsFb = new OpSetup[DX9.OP_COUNT];
		for (int j = 0; j < DX9.OP_COUNT; j++)
			opsFb[j] = new OpSetup(withFb);
		FMOscSetup setupFb = new FMOscSetup(DX9.getAlgorithms()[5], opsFb);
		FMOsc fmFb = new FMOsc(setup, setupFb, mine);

		Arrays.fill(sumL, 0f);
		Arrays.fill(sumR, 0f);
		fmFb.trigger(null);
		fmFb.process(sumL, sumR);
		float rmsFb = AudioMetrics.rms(sumL);

		// Without feedback
		OpSetup[] opsNoFb = new OpSetup[DX9.OP_COUNT];
		for (int j = 0; j < DX9.OP_COUNT; j++)
			opsNoFb[j] = new OpSetup(noFb);
		FMOscSetup setupNoFb = new FMOscSetup(DX9.getAlgorithms()[6], opsNoFb);
		FMOsc fmNoFb = new FMOsc(setup, setupNoFb, mine);

		Arrays.fill(sumL, 0f);
		Arrays.fill(sumR, 0f);
		fmNoFb.trigger(null);
		fmNoFb.process(sumL, sumR);
		float rmsNoFb = AudioMetrics.rms(sumL);

		assertTrue(Math.abs(rmsFb - rmsNoFb) > 0.2f * RMS_TOLERANCE, "Feedback should change spectrum; FB=" + rmsFb + " NoFB=" + rmsNoFb);
	}

//	@Test
//	void testRatioModes() {
//		Mode[] modes = {Mode.RATIO_DECIMAL, Mode.YAMAHA_DX, Mode.SEMITONE, Mode.ABS_HZ};
//
//		for (Mode m : modes) {
//			OpSetup op = new OpSetup(0.9f, 0f, 1f, new Postage(10, 40), m, Shape.SIN);
//			OpSetup[] ops = new OpSetup[DX9.OP_COUNT];
//			for (int j = 0; j < DX9.OP_COUNT; j++)
//				ops[j] = new OpSetup(op);
//			FMOscSetup fmSetup = new FMOscSetup(DX9.getAlgorithms()[6], ops);
//			FMOsc fmMode = new FMOsc(setup, fmSetup, mine);
//
//			Arrays.fill(sumL, 0f);
//			Arrays.fill(sumR, 0f);
//			fmMode.trigger(null);
//			fmMode.process(sumL, sumR);
//			float rms = AudioMetrics.rms(sumL);
//
//			assertTrue(rms > EPS, "Mode " + m + " should produce audio; RMS=" + rms);
//			assertTrue(rms < 1.1f, "Mode " + m + " should not clip; RMS=" + rms);
//		}
//	}

	@Test
	void testAlgorithmSwitching() {
		for (int algoIdx = 0; algoIdx < DX9.ALGO_COUNT; algoIdx++) {
			OpSetup basic = new OpSetup(0.9f, 0f, 1f, new Postage(10, 40), Shape.SIN);
			OpSetup[] ops = new OpSetup[DX9.OP_COUNT];
			for (int j = 0; j < DX9.OP_COUNT; j++)
				ops[j] = new OpSetup(basic);

			Algorithm algo = DX9.getAlgorithms()[algoIdx];
			FMOscSetup fmSetup = new FMOscSetup(algo, ops);
			FMOsc fmAlgo = new FMOsc(setup, fmSetup, mine);

			Arrays.fill(sumL, 0f);
			Arrays.fill(sumR, 0f);
			fmAlgo.trigger(null);
			fmAlgo.process(sumL, sumR);
			float rms = AudioMetrics.rms(sumL);

			assertTrue(rms > EPS, "Algorithm " + algoIdx + " (" + DX9.names[algoIdx] + ") should produce audio; RMS=" + rms);
		}
	}

	@Test
	void testWaveformShapes() {
		Shape[] shapes = { Shape.SIN, Shape.TRI, Shape.SAW, /* Shape.SQR, removed from FM synth DC */ Shape.RND};

		for (Shape s : shapes) {
			OpSetup op = new OpSetup(0.9f, 0f, 1f, new Postage(10, 40), s);
			OpSetup[] ops = new OpSetup[DX9.OP_COUNT];
			for (int j = 0; j < DX9.OP_COUNT; j++)
				ops[j] = new OpSetup(op);
			FMOscSetup fmSetup = new FMOscSetup(DX9.getAlgorithms()[6], ops);
			FMOsc fmShape = new FMOsc(setup, fmSetup, mine);

			Arrays.fill(sumL, 0f);
			Arrays.fill(sumR, 0f);
			fmShape.trigger(null);
			fmShape.process(sumL, sumR);
			float rms = AudioMetrics.rms(sumL);

			assertTrue(rms > EPS, "Shape " + s + " should produce audio; RMS=" + rms);
		}
	}

	@Test
	void testEnvelopeDecay() {
		osc.trigger(null);

		float[] frame1 = new float[bufSize];
		float[] frame2 = new float[bufSize];
		float[] frame3 = new float[bufSize];
		Arrays.fill(sumR, 0f);
		Arrays.fill(sumL, 0f);

		osc.process(sumL, sumR);

		while (osc.getEnv().getStage() == Delta.ATK)
			osc.process(sumL, sumR);  // fast forward to decay stage

		Arrays.fill(sumR, 0f);

		osc.process(frame1, sumR);
		float rms1 = AudioMetrics.rms(frame1);

		osc.process(frame2, sumR);
		float rms2 = AudioMetrics.rms(frame2);

		osc.process(frame3, sumR);
		float rms3 = AudioMetrics.rms(frame3);

		assertTrue(rms2 <= rms1 + RMS_TOLERANCE, "Frame 2 should decay from frame 1; RMS1=" + rms1 + " RMS2=" + rms2);
		assertTrue(rms3 <= rms2 + RMS_TOLERANCE, "Frame 3 should decay from frame 2; RMS2=" + rms2 + " RMS3=" + rms3);
	}

	@Test
	void testRetriggerBehavior() {
		float[] f1 = new float[bufSize];
		float[] f2 = new float[bufSize];
		Arrays.fill(sumR, 0f);

		osc.trigger(null);
		osc.process(f1, sumR);
		float rms1 = AudioMetrics.rms(f1);

		// Skip several frames to let envelope decay
		for (int i = 0; i < 5; i++) {
			Arrays.fill(f2, 0f);
			osc.process(f2, sumR);
		}
		float rmsDecayed = AudioMetrics.rms(f2);

		// Retrigger
		osc.trigger(null);
		Arrays.fill(f2, 0f);
		osc.process(f2, sumR);
		float rmsRetrig = AudioMetrics.rms(f2);

		assertTrue(rms1 > rmsDecayed * 0.5f, "Should have decayed; initial=" + rms1 + " decayed=" + rmsDecayed);
		assertTrue(rmsRetrig > rmsDecayed * 0.7f, "Retrigger should restart envelope; retrig=" + rmsRetrig);
	}

	@Test
	void testFrequencyResolution() {
		float baseFreq = 440f;

		FMRatio ratio = new FMRatio(1);

		// Two different frequency setups; verify phase increments differ
		float inc1 = ratio.phaseInc(baseFreq);
		float inc2 = ratio.phaseInc(baseFreq * 2f);

		assertTrue(inc2 > inc1, "Higher frequency should have larger phase increment");
		assertTrue(Math.abs(inc2 - inc1 * 2f) < EPS, "Double frequency should double phase increment");
	}

	@Test
	void testNoBufferUnderrun() {
		// Process many frames; verify no null or exception
		for (int i = 0; i < 100; i++) {
			Arrays.fill(sumL, 0f);
			Arrays.fill(sumR, 0f);
			osc.process(sumL, sumR);

			for (float s : sumL)
				assertTrue(Float.isFinite(s), "Sample should be finite; got " + s);
			for (float s : sumR)
				assertTrue(Float.isFinite(s), "Sample should be finite; got " + s);
		}
	}

	@Test
	void testParameterImmediateUpdate() {
		// Trigger and get baseline
		Arrays.fill(sumL, 0f);
		Arrays.fill(sumR, 0f);
		osc.trigger(null);
		osc.process(sumL, sumR);
		float rmsBaseline = AudioMetrics.rms(sumL);

		// Modify setup (e.g., change ratio globally) and reprocess
		OpSetup modified = new OpSetup(0.5f, 0f, 2f, new Postage(10, 40), Shape.SIN);
		OpSetup[] ops = new OpSetup[DX9.OP_COUNT];
		for (int j = 0; j < DX9.OP_COUNT; j++)
			ops[j] = new OpSetup(modified);
		FMOscSetup modSetup = new FMOscSetup(DX9.getAlgorithms()[6], ops);
		FMOsc fmMod = new FMOsc(setup, modSetup, mine);

		Arrays.fill(sumL, 0f);
		Arrays.fill(sumR, 0f);
		fmMod.trigger(null);
		fmMod.process(sumL, sumR);
		float rmsModified = AudioMetrics.rms(sumL);

		assertTrue(Math.abs(rmsBaseline - rmsModified) > RMS_TOLERANCE * 0.5f, "Modified parameters should change output");
	}

	@Test
	void testStereoBallance() {
		osc.trigger(null);
		Arrays.fill(sumL, 0f);
		Arrays.fill(sumR, 0f);
		osc.process(sumL, sumR);

		float rmsL = AudioMetrics.rms(sumL);
		float rmsR = AudioMetrics.rms(sumR);
		float ratio = Math.abs(rmsL - rmsR) / Math.max(rmsL, rmsR);

		assertTrue(ratio < 0.3f, "Stereo imbalance " + ratio + " too high; L=" + rmsL + " R=" + rmsR);
	}

    @Test
    void testRatioKnobs() {
    	FMRatio r = new FMRatio(1);

    	float last = 0;
    	for (int i = 0; i < FMRatio.length(); i++) {
    		// System.out.println("Ratio " + i + ": " + FMRatio.get(i).ratio() + " (" + FMRatio.get(i).toString() + ")");
    		float cur = FMRatio.get(i).ratio();
    		assertTrue(cur > last, "Ratios should be strictly increasing: " + cur + " vs. " + last + " @ " + i);
    		last = cur;
		}

    	for (int i = OscTuning.FIRST_MIDI; i <= OscTuning.LAST_MIDI; i++) {
    		r.setRatioKnob(i);
    		RatioSpec spec = r.spec();
    		r.setRatioKnob(r.getRatioKnob());
    		assertEquals(spec, r.spec(), spec + " != " + r.spec() + " @ " + i);
    	}

    	int half = (FMRatio.length() - 1) / 2;
    	r.set(FMRatio.get(half).ratio());
    	assertEquals(50, r.getRatioKnob());

    }


	@Test
	void testOperatorFundamental() { // robust checks for operator phase increments / ratio mapping
	    final float REL_TOL = 1e-6f;
	    osc.setFundamental(220f);
	    osc.trigger(null);

	    int opCount = osc.getOpCount();
	    for (int i = 0; i < opCount; i++) {
	        var op = osc.getOp(i);
	        assertNotNull(op, "Operator " + i + " should exist");
	        FMRatio ratio = op.getRatio();
	        assertNotNull(ratio, "Operator " + i + " ratio should exist");

	        float base = osc.getFundamental();

	        float inc1 = ratio.phaseInc(base);
	        assertTrue(Float.isFinite(inc1) && inc1 > 0f, "Invalid phaseInc for op " + i + ": " + inc1);

	        // Increasing the ratio knob should increase phase increment (monotonic behavior).
	        int origKnob = ratio.getRatioKnob();
	        int bump = Math.min(origKnob + 12, 127);
	        ratio.setRatioKnob(bump);
	        float inc2 = ratio.phaseInc(base);

	        // Accept either strictly greater or meaningfully different within tolerance.
	        assertTrue(Float.isFinite(inc2) && (inc2 > inc1 || Math.abs(inc2 - inc1) > REL_TOL * Math.max(1f, inc1)),
	                   "phaseInc did not increase for op " + i + " after raising ratio knob; " + inc1 + " -> " + inc2);

	        // Restore knob to avoid side-effects
	        ratio.setRatioKnob(origKnob);
	    }
	}

//	// SQR removed from FM synths
//	@Test void testSquareShape() {
//	// check operator audio on SQR shape for RMS and DC/standingWave through Ratio range
//
//		DCBlock dcBlock = new DCBlock(Constants.sampleRate(), 20f);
//
//		OpSetup square = new OpSetup(0.5f, 0f, 1f, new Postage(10, 40), Mode.RATIO_DECIMAL, Shape.SQR);
//		OpSetup[] ops = new OpSetup[DX9.OP_COUNT];
//		for (int j = 0; j < DX9.OP_COUNT; j++)
//			ops[j] = new OpSetup(square);
//		FMOscSetup fmSetup = new FMOscSetup(DX9.getAlgorithms()[6], ops);
//		FMOsc fmSquare = new FMOsc(setup, fmSetup, mine);
//		fmSquare.getGain().setPreamp(0.5f);
//
//		Arrays.fill(sumL, 0f);
//		Arrays.fill(sumR, 0f);
//		fmSquare.trigger(null);
//		fmSquare.process(sumL, sumR);
//		float rms = AudioMetrics.rms(sumL);
//
//		assertTrue(rms > EPS, "Square shape should produce audio; RMS=" + rms);
//
//		// additionally ensure no large DC offset and all samples finite
//		float mean = 0f;
//		for (int i = 0; i < sumL.length; i++) {
//
//		    assertTrue(Float.isFinite(sumL[i]), "Sample should be finite in SQR output");
//		    sumL[i] = dcBlock.process(sumL[i]);
//		    mean += sumL[i];
//		}
////		for (float s : sumL) {
////		    assertTrue(Float.isFinite(s), "Sample should be finite in SQR output");
////		    s = dcBlock.process(s);
////		    mean += s;
////		}
//		mean /= sumL.length;
//		assertTrue(Math.abs(mean) < 1e-2f, "Mean (DC) for square output too large: " + mean);
//	}

    @Test
    void detectPitch() {
        final int SR = Constants.sampleRate();
        final int N_FRAMES = Constants.bufSize();
        final double FREQ = 440.0;
        final double AMP = 0.5;

        // 1) Pure synthetic sine -> expect ~440 Hz
        float[] left = new float[N_FRAMES];
        double phase = 0.0;
        phase = TestUtilities.SineWave.fill(left, FREQ, SR, AMP, phase);
        PitchDetectionResult r = detector.getPitch(left);
        double detected = r.getPitch();
        assertTrue(detected > 0, "Detector failed to find pitch for synthetic sine");
        assertTrue(Math.abs(detected - FREQ) < 1.0, "Synthetic sine should detect ~" + FREQ + " Hz, got " + detected);

        // Helper to create and render an FM voice for a given operator ratio
        Function<Float, float[]> renderVoice = (ratio) -> {
            OpSetup base = new OpSetup(0.9f, 0f, ratio, new Postage(0, 1000), Shape.SIN);
            OpSetup[] ops = new OpSetup[DX9.OP_COUNT];
            for (int i = 0; i < ops.length; i++) ops[i] = new OpSetup(base);
            FMOsc voice = new FMOsc(setup, new FMOscSetup(DX9.getAlgorithms()[3], ops), mine); // algo 3 = four parallel carriers
            voice.setFundamental((float) FREQ);
            float[] l = new float[N_FRAMES];
            float[] rgt = new float[N_FRAMES];
            Arrays.fill(l, 0f);
            Arrays.fill(rgt, 0f);
            voice.trigger(null);
            voice.process(l, rgt);
            return l;
        };

        // 2) Unmodulated carriers at 1:1 -> expect ~440 Hz
        float[] out1 = renderVoice.apply(1f);
        PitchDetectionResult pr1 = detector.getPitch(out1);
        double p1 = pr1.getPitch();
        assertTrue(p1 > 0, "No pitch detected for 1:1 carriers");
        assertTrue(Math.abs(p1 - FREQ) < 10.0, "1:1 carriers should be near " + FREQ + " Hz, got " + p1);

        // 3) Unmodulated carriers at 2:1 -> expect ~880 Hz
        float[] out2 = renderVoice.apply(2f);
        PitchDetectionResult pr2 = detector.getPitch(out2);
        double p2 = pr2.getPitch();
        assertTrue(p2 > 0, "No pitch detected for 2:1 carriers");
        assertTrue(Math.abs(p2 - (FREQ * 2.0)) < 20.0, "2:1 carriers should be near " + (FREQ * 2.0) + " Hz, got " + p2);
    }

    @Test
    void logFeedbackSensitiveAlgorithms() {
    	int count = 0;
        for (int i = 0; i < DX9.ALGO_COUNT; i++) {
        	Algorithm algorithm = DX9.getAlgorithms()[i];
            float rmsFb = feedbackRms(algorithm, 0.5f);
            float rmsNoFb = feedbackRms(algorithm, 0f);
            float delta = Math.abs(rmsFb - rmsNoFb);
            if (delta <= 0.2f * RMS_TOLERANCE) {
            	count++;
                System.out.println("Feedback silent for " + i + "; FB=" + rmsFb + " NoFB=" + rmsNoFb);
            }
        }


        assertTrue(count < DX9.ALGO_COUNT / 2, "Too many algorithms insensitive to feedback: " + count +
        		" out of " + DX9.ALGO_COUNT + " Dx9 algoz");
    }


    private float feedbackRms(Algorithm algorithm, float feedback) {
        OpSetup base = new OpSetup(0.9f, feedback, 1f, new Postage(10, 40), Shape.SIN);
        OpSetup[] ops = new OpSetup[DX9.OP_COUNT];
        for (int i = 0; i < ops.length; i++)
            ops[i] = new OpSetup(base);
        FMOsc voice = new FMOsc(setup, new FMOscSetup(algorithm, ops), mine);

        float[] left = new float[bufSize];
        float[] right = new float[bufSize];
        Arrays.fill(left, 0f);
        Arrays.fill(right, 0f);
        voice.trigger(null);
        voice.process(left, right);
        return AudioMetrics.rms(left);
    }

}
