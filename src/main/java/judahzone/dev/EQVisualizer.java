package judahzone.dev;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

import judahzone.fx.EQ;
import judahzone.util.Constants;

/*  Utility class EQVisualizer maps frequency -> X (log axis like Spectrometer), dB -> Y (0 dB centered, positive up),
		and builds a simple band shape (ellipse/rect) for a 3-band EQ overlay.
	Methods are static, allocation-light, and expect the caller to supply band center frequency,
		gain_db and a width parameter (interpreted as octave spread).
	call xForFreq(...), yForDb(...) and bandShape(...) from your paint routine and draw with Graphics2D.
	or paintEQOverlay(...) from your paint routine and draw with Graphics2D.

/**	•  Small helper to map a 3-band EQ into the same coordinate space Spectrometer uses.
	•  - X: log-frequency (minFreq..maxFreq)
	•  - Y: gain in dB, 0 dB is vertically centered, positive goes up
	•  - bandShape: a simple ellipse representing the band's extent on the spectrum */
public final class EQVisualizer {

	private final double minFreq;
	private final double maxFreq;
	private final int width;
	private final int height;
	private final float maxGainDb; // max absolute dB we map to top/bottom

	public EQVisualizer(int width, int height, double minFreq, double maxFreq, float maxGainDb) {
	    this.width = Math.max(1, width);
	    this.height = Math.max(1, height);
	    this.minFreq = Math.max(1e-6, minFreq);
	    this.maxFreq = Math.max(this.minFreq * 1.001, maxFreq);
	    this.maxGainDb = Math.max(1f, maxGainDb);
	}

	// Log mapping consistent with Spectrometer: fraction = log(f/min)/log(max/min)
	public int xForFreq(double freq) {
	    double ratio = maxFreq / minFreq;
	    double logDenom = Math.log(ratio);
	    double safeF = Math.max(freq, minFreq);
	    double frac = Math.log(safeF / minFreq) / logDenom;
	    frac = Math.min(1.0, Math.max(0.0, frac));
	    return (int) Math.round(frac * (width - 1));
	}

	// Map dB to pixel Y. 0 dB -> center, +maxGainDb -> top (0), -maxGainDb -> bottom (height-1)
	public int yForDb(float db) {
	    double clamped = Math.max(-maxGainDb, Math.min(maxGainDb, db));
	    double frac = clamped / maxGainDb; // -1..1
	    double center = (height - 1) / 2.0;
	    // positive db goes up (decreasing y)
	    double y = center - frac * center;
	    return (int) Math.round(Math.min(height - 1, Math.max(0, y)));
	}

	/**
	 * Build a Shape representing the band area for visualization.
	 * widthInOctaves - total octave spread of the band (e.g., 1.0 => +/- 0.5 octaves).
	 * The shape is an ellipse centered at the band's X/Y with horizontal size covering the octave span,
	 * and vertical size proportional to |gain_db|.
	 */
	public Shape bandShape(double centerFreq, float gainDb, float widthInOctaves) {
	    // clamp inputs
	    double octaveHalf = Math.abs(widthInOctaves) / 2.0;
	    // avoid degenerate
	    octaveHalf = Math.max(1e-6, octaveHalf);

	    // compute left/right frequencies by octaves
	    double leftHz = centerFreq / Math.pow(2.0, octaveHalf);
	    double rightHz = centerFreq * Math.pow(2.0, octaveHalf);

	    int xLeft = xForFreq(leftHz);
	    int xRight = xForFreq(rightHz);
	    int xCenter = xForFreq(centerFreq);

	    // horizontal span at least 2 px
	    int w = Math.max(2, Math.abs(xRight - xLeft));
	    int x = xCenter - w / 2;

	    // vertical size scales with absolute gain; small minimum to be visible
	    double gainFrac = Math.min(1.0, Math.abs(gainDb) / maxGainDb);
	    int h = Math.max(4, (int)Math.round(gainFrac * (height * 0.5))); // up to half the component

	    int yCenter = yForDb(gainDb);
	    int y = Math.max(0, Math.min(height - h, yCenter - h / 2));

	    return new Ellipse2D.Float(x, y, w, h);
	}

	/**
	 * Convenience painter for an EQ band.
	 * Draws a translucent filled band and a stroked center marker and label.
	 */
	public void paintBand(Graphics2D g, double centerFreq, float gainDb, float widthInOctaves, Color fill, Color stroke, String label) {
	    Shape band = bandShape(centerFreq, gainDb, widthInOctaves);

	    Composite oldComp = g.getComposite();
	    Paint oldPaint = g.getPaint();
	    Stroke oldStroke = g.getStroke();
	    Font oldFont = g.getFont();

	    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
	    g.setPaint(fill);
	    g.fill(band);

	    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
	    g.setPaint(stroke);
	    g.setStroke(new BasicStroke(1.5f));
	    g.draw(band);

	    // center marker
	    int xc = xForFreq(centerFreq);
	    int yc = yForDb(gainDb);
	    g.setStroke(new BasicStroke(1f));
	    g.drawLine(xc - 6, yc, xc + 6, yc);
	    g.drawLine(xc, yc - 6, xc, yc + 6);

	    if (label != null && !label.isEmpty()) {
	        g.setFont(oldFont.deriveFont(11f));
	        g.drawString(label, xc + 8, Math.max(12, yc - 6));
	    }

	    g.setPaint(oldPaint);
	    g.setStroke(oldStroke);
	    g.setFont(oldFont);
	    g.setComposite(oldComp);
	}

	// Helper: convert an EQ width parameter (like EQ.getWidth()) into an approximate octave spread.
	// This mapping is heuristic and intentionally simple: MIN_WIDTH -> 0.5 octaves, MAX_WIDTH -> 4 octaves.
	public static float widthToOctaves(float width, float minWidth, float maxWidth) {
	    float t = (width - minWidth) / Math.max(1e-6f, (maxWidth - minWidth));
	    t = Math.max(0f, Math.min(1f, t));
	    return 0.5f + t * 3.5f; // result in [0.5 .. 4.0] octaves
	}

	// Example: convenience factory using Constants' logarithmic helpers to get min/max freqs
	public static EQVisualizer forSpectrometerSize(int w, int h) {
	    // derive min/max from Constants (use same values Spectrometer would use)
	    double minF = 20.0;
	    double maxF = Constants.sampleRate() / 2.0;
	    return new EQVisualizer(w, h, minF, maxF, 12f); // +/-12 dB mapping by default
	}

	// TODO hookup
	public static void paintEQOverlay(Graphics2D g, int width, int height, EQ eq) {
	    // visual helper sized to the spectrometer area
	    EQVisualizer vis = EQVisualizer.forSpectrometerSize(width, height);

	    // band width in octaves (EQ stores bandwidth in the same 0.5..5.0 range used here)
	    float octaveSpread = EQVisualizer.widthToOctaves(eq.getWidth(), 0.5f, 5f);

	    // gains (dB) for each band
	    float gainBass = eq.getGain(EQ.EqBand.Bass);
	    float gainMid  = eq.getGain(EQ.EqBand.Mid);
	    float gainHigh = eq.getGain(EQ.EqBand.High);

	    // reconstruct center frequencies:
	    // Bass frequency is controlled by Settings.LoHz slider -> map back to Hz
	    int loSlider = eq.get(EQ.Settings.LoHz.ordinal());
	    double lowMax = EQ.MID_HZ / 2.0; // same as EQ's LOW_MAX
	    double freqBass = Constants.logarithmic(loSlider, EQ.MIN_HZ, (float) lowMax);

	    // Mid frequency is fixed at EQ.MID_HZ (EQ exposes this constant)
	    double freqMid = EQ.MID_HZ;

	    // High frequency from Settings.HiHz slider -> map back to Hz
	    int hiSlider = eq.get(EQ.Settings.HiHz.ordinal());
	    double hiMin = EQ.MID_HZ * 2.0; // same as EQ's HI_MIN
	    double freqHigh = Constants.logarithmic(hiSlider, (float) hiMin, EQ.MAX_HZ);

	    // Colors for bands (fill, stroke)
	    Color bassFill  = new Color(0x40, 0x80, 0xFF); // bluish
	    Color bassStroke= new Color(0x00, 0x60, 0xC0);
	    Color midFill   = new Color(0xFF, 0xA0, 0x40); // orange
	    Color midStroke = new Color(0xC0, 0x70, 0x20);
	    Color highFill  = new Color(0x80, 0xFF, 0x80); // greenish
	    Color highStroke= new Color(0x40, 0xC0, 0x40);

	    // Paint bands (translucency set inside EQVisualizer.paintBand)
	    vis.paintBand(g, freqBass,  gainBass,  octaveSpread, bassFill,  bassStroke,  "Bass");
	    vis.paintBand(g, freqMid,   gainMid,   octaveSpread, midFill,   midStroke,   "Mid");
	    vis.paintBand(g, freqHigh,  gainHigh,  octaveSpread, highFill,  highStroke,  "High");
	}


}