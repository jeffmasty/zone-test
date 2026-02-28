package judahzone.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import judahzone.data.Shape;
import judahzone.util.AudioMetrics;

public class ShapeTest {

	private static final int LENGTH = Shape.LENGTH;

	@Test
	void testWaveformIntegrity() {
		for (Shape shape : Shape.values()) {
			float[] wave = shape.getWave();
			assertEquals(LENGTH, wave.length, "Shape " + shape + " should have length " + LENGTH);
			for (float sample : wave) {
				assertTrue(Float.isFinite(sample), "Sample not finite for " + shape);
				assertTrue(sample >= -1.0f && sample <= 1.0f, "Sample out of range for " + shape + ": " + sample);
			}
		}
	}

	@Test
	void printRMSEachWave() {
		for (Shape shape : Shape.values()) {
			float rms = AudioMetrics.rms(shape.getWave());
			assertTrue(Float.isFinite(rms), "RMS should be finite for " + shape);
			assertTrue(rms > 0f, "RMS should be positive for " + shape);
			System.out.println("Shape " + shape + " RMS: " + rms);
		}
	}

	@Test
	void testWaveformSymmetry() {
		for (Shape shape : Shape.values()) {
			float[] wave = shape.getWave();
			boolean hasPositive = false;
			boolean hasNegative = false;
			for (float sample : wave) {
				if (sample > 0f) hasPositive = true;
				if (sample < 0f) hasNegative = true;
				if (hasPositive && hasNegative) break;
			}
			assertTrue(hasPositive, "Shape " + shape + " should contain positive samples");
			assertTrue(hasNegative, "Shape " + shape + " should contain negative samples");
		}
	}
}
