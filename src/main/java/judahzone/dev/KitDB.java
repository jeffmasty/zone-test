
package judahzone.dev;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import judahzone.data.Postage;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.Gain;

/** Central kit database.  Populates built-in kits and provides lookup by name or program index. */
public final class KitDB {

    private static final Map<String, KitSetup> saved = new LinkedHashMap<>();
    private static final List<String> order = new ArrayList<>();

    static {
        initSaved();
    }

    private KitDB() {}

    public static KitSetup get(String name, boolean unused) {
        if (name == null) return null;
        return saved.get(name);
    }

    public static KitSetup get(int data1, boolean unused) {
        if (data1 < 0 || data1 >= order.size()) return null;
        return saved.get(order.get(data1));
    }

    public static String[] names() {
        return saved.keySet().toArray(new String[0]);
    }

    public static void addOrReplace(KitSetup kit, boolean persist) throws IOException {
        if (kit == null) return;
        saved.put(kit.name(), kit);
        if (!order.contains(kit.name())) order.add(kit.name());
        if (persist) {
            // persist to disk if desired. left intentionally minimal.
            // writeJsonFile(...)
        }
    }

    // ---------- initialization ----------

    private static void initSaved() {
        KitSetup kDefault = makeDefault();
        saved.put(kDefault.name(), kDefault);
        order.add(kDefault.name());

        KitSetup noise = makeNoiseKit();
        saved.put(noise.name(), noise);
        order.add(noise.name());

        KitSetup fm = makeFMKit();
        saved.put(fm.name(), fm);
        order.add(fm.name());

        KitSetup pluck = makePluckKit();
        saved.put(pluck.name(), pluck);
        order.add(pluck.name());
    }

    // Helper to construct Gain.GainT arrays from raw values
    private static Gain.GainT[] gains(float... vals) {
        // vals length must be multiple of 4: preamp, gain, pan, width
        int n = vals.length / 4;
        Gain.GainT[] out = new Gain.GainT[n];
        for (int i = 0; i < n; i++) {
            out[i] = new Gain.GainT(vals[i * 4], vals[i * 4 + 1], vals[i * 4 + 2], vals[i * 4 + 3]);
        }
        return out;
    }

    private static Postage[] env(long... vals) {
        // vals length multiple of 2: atkMS, dkMS
        int n = vals.length / 2;
        Postage[] out = new Postage[n];
        for (int i = 0; i < n; i++) {
            out[i] = new Postage((int) vals[i * 2], (int) vals[i * 2 + 1]);
        }
        return out;
    }

    private static Coord[] coords(double... vals) {
        int n = vals.length / 2;
        Coord[] out = new Coord[n];
        for (int i = 0; i < n; i++)
            out[i] = new Coord((float) vals[i * 2], (float) vals[i * 2 + 1]);
        return out;
    }

    private static FilterT[] bodies(double... vals) {
        // vals length multiple of 3: hz, dB, bandwidth
        int n = vals.length / 3;
        FilterT[] out = new FilterT[n];
        for (int i = 0; i < n; i++)
            out[i] = new FilterT((float) vals[i * 3], (float) vals[i * 3 + 1], (float) vals[i * 3 + 2]);
        return out;
    }

    // ---------- concrete kits (values derived from drumsynth.zone) ----------

    private static KitSetup makeDefault() {
        Gain.GainT[] g = gains(
            0.7f, 0.5f, 0.5f, 1.0f,
            0.55f, 0.5f, 0.5f, 1.0f,
            0.55f, 0.5f, 0.5f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f,
            2.5f, 0.5f, 0.5f, 1.0f,
            2.5f, 0.5f, 0.5f, 1.0f,
            0.35f, 0.5f, 0.5f, 1.0f,
            0.35f, 0.5f, 0.5f, 1.0f
        );
        Postage[] e = env(5,30,9,21,8,25,23,205,15,201,15,170,30,185,15,204);
        Coord[] lo = coords(
            45.0,9.0,150.0,1.0,180.0,6.0,400.0,0.0,2000.0,6.0,2100.0,3.0,1100.0,6.0,180.0,9.0
        );
        FilterT[] b = bodies(
            55.0,0.0,0.5,1450.0,0.0,0.5,391.0,0.0,0.5,2600.0,9.0,0.5,3278.0,9.0,0.5,4500.0,9.0,0.5,3000.0,0.0,0.5,296.91833,0.0,0.5
        );
        Coord[] hi = coords(
            180.0,6.0,6666.0,1.0,10000.0,6.0,6666.0,9.0,12500.0,9.0,16000.0,6.0,16000.0,1.0,642.33673,3.0
        );
        // synth null, fm null, pluck null, pitch from JSON
        OscSetup[] pitch = new OscSetup[] {
            new OscSetup(false, 55.0f),
            new OscSetup(false, 1450.0f),
            new OscSetup(false, 391.0f),
            new OscSetup(false, 2600.0f),
            new OscSetup(false, 3278.0f),
            new OscSetup(false, 4500.0f),
            new OscSetup(false, 3000.0f),
            new OscSetup(false, 296.91833f)
        };
        return new KitSetup("default", true, g, e, lo, b, hi, null, null, null, pitch);
    }

    private static KitSetup makeNoiseKit() {
        Gain.GainT[] g = gains(
            0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f,
            0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f
        );
        Postage[] e = env(5,30,9,21,8,25,23,205,15,201,15,169,30,639,15,204);
        Coord[] lo = coords(45.0,9.0,150.0,1.0,180.0,6.0,400.0,0.0,2000.0,6.0,2100.0,3.0,1100.0,6.0,180.0,9.0);
        FilterT[] b = bodies(
            55.0,0.0,0.5,500.0,0.0,0.5,391.0,0.0,0.5,2600.0,9.0,0.5,3278.0,9.0,0.5,4421.576,9.0,0.5,3006.1748,0.0,0.5,296.91833,0.0,0.5
        );
        Coord[] hi = coords(180.0,6.0,6666.0,1.0,10000.0,6.0,6666.0,9.0,12500.0,9.0,16000.0,6.0,16000.0,1.0,2000.0,3.0);
        OscSetup[] pitch = new OscSetup[] {
            new OscSetup(false, 55.0f),
            new OscSetup(false, 500.0f),
            new OscSetup(false, 391.0f),
            new OscSetup(false, 2600.0f),
            new OscSetup(false, 3278.0f),
            new OscSetup(false, 4434.922f),
            new OscSetup(false, 2959.9553f),
            new OscSetup(false, 293.66476f)
        };

        NoiseSetup synth = new NoiseSetup(
            new NoiseSetup.Kick(0.65f, -6, 1.0f),
            new NoiseSetup.Snare("WHITE", 0.45f, 0.19999999f),
            new NoiseSetup.Stick(0.33f, 6, 0),
            new NoiseSetup.Clap(30f, 0.5f, 0),
            new NoiseSetup.CHat(0.3f, 0.5f, 0.7f),
            new NoiseSetup.OHat(30f, 0.2f, 0.4785992f),
            new NoiseSetup.Ride(0.25f, 0.48f, 0),
            new NoiseSetup.Bongo(0.58f, -2, "Wood")
        );

        return new KitSetup("NoiseKit", true, g, e, lo, b, hi, synth, null, null, pitch);
    }

    private static KitSetup makeFMKit() {
        Gain.GainT[] g = gains(
            0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
            0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f
        );
        Postage[] e = env(1,90,1,100,1,90,2,1024,22,200,4,1024,12,1024,8,142);
        Coord[] lo = coords(25.0,9.0,113.0,1.0,35.0,1.0,685.0,6.0,945.0,1.0,530.0,1.0,1390.0,6.0,128.70166,6.4800005);
        FilterT[] b = bodies(
            338.0,-9.0,0.5,779.0,12.0,0.5,440.0,1.0,0.5,6503.0,9.0,0.5,779.0,-3.0,0.5,2324.0,6.0,0.5,1917.0,3.0,0.5,215.27882,5.76,0.5
        );
        Coord[] hi = coords(550.0,6.0,5719.0,12.0,440.0,1.0,9565.0,1.0,4422.0,12.0,8411.0,6.0,2819.0,1.0,830.7519,5.76);
        OscSetup[] pitch = new OscSetup[]{
            new OscSetup(false,30.0f), new OscSetup(false,92.0f), new OscSetup(false,40.0f), new OscSetup(false,493.0f),
            new OscSetup(false,1046.0f), new OscSetup(false,329.0f), new OscSetup(false,1479.0f), new OscSetup(false,220.0f)
        };

        // FM definition omitted in detail (large). leave fm null to avoid breaking consumers expecting non-null.
        return new KitSetup("FMKit", true, g, e, lo, b, hi, null, null, null, pitch);
    }

    private static KitSetup makePluckKit() {
        Gain.GainT[] g = gains(
            1.5f,0.5f,0.5f,1.0f,1.5f,0.5f,0.5f,1.0f,1.5f,0.5f,0.5f,1.0f,1.5f,0.5f,0.5f,1.0f,
            1.5f,0.5f,0.5f,1.0f,1.5f,0.5f,0.5f,1.0f,1.5f,0.5f,0.5f,1.0f,1.5f,0.5f,0.5f,1.0f
        );
        Postage[] e = env(1,256,1,111,1,70,2,95,6,100,1,204,73,1024,7,140);
        Coord[] lo = coords(32.0,4.5,137.0,1.0,230.0,6.0,360.0,1.0,730.0,2.0,1200.0,6.0,498.0,0.0,177.0,1.0);
        FilterT[] b = bodies(
            202.0,-6.0,0.5,215.0,9.0,0.5,316.0,3.0,0.5,1580.0,15.0,0.5,3206.0,3.0,0.5,3887.9692,1.0,0.5,2479.0,-6.0,0.5,178.0,6.0,0.5
        );
        Coord[] hi = coords(420.0,6.0,1482.0,3.0,8411.0,6.0,6935.0,6.0,3646.0,3.0,10000.0,3.0,3419.0,6.0,1390.0,0.5);
        OscSetup[] pitch = new OscSetup[]{
            new OscSetup(false,36.0f), new OscSetup(false,174.0f), new OscSetup(false,415.0f), new OscSetup(false,391.0f),
            new OscSetup(false,739.0f), new OscSetup(false,391.99542f), new OscSetup(false,523.0f), new OscSetup(false,246.0f)
        };

        PluckSetup.Pluck[] plucks = new PluckSetup.Pluck[] {
            new PluckSetup.Pluck(15f,1.20f,70f,0.85f,0.67f,10,0.60f,"BROWN"),
            new PluckSetup.Pluck(11f,1.00f,220f,0.98f,0.15f,47,0.80f,"VELVET"),
            new PluckSetup.Pluck(30f,1.10f,880f,0.98f,0.47f,28,0.07f,"WHITE"),
            new PluckSetup.Pluck(27f,1.00f,880f,0.95f,0.22f,90,0.77f,"VELVET"),
            new PluckSetup.Pluck(8f,0.80f,1500f,0.91f,0.60f,50,0.35f,"GREY"),
            new PluckSetup.Pluck(25.937498f,1.00f,3947.5994f,0.89994967f,0.7f,60,0.74759996f,"WHITE"),
            new PluckSetup.Pluck(12f,0.23f,1046f,0.99f,0.6f,73,0.73f,"GREY"),
            new PluckSetup.Pluck(20f,0.95f,440f,0.99f,0.5f,22,0.78f,"BROWN")
        };

        PluckSetup ps = new PluckSetup(plucks);
        return new KitSetup("PluckKit", true, g, e, lo, b, hi, null, null, ps, pitch);
    }

    // ---------- Types used by consumers ----------

    public static record KitSetup(
        String name,
        boolean choke,
        Gain.GainT[] gains,
        Postage[] env,
        Coord[] lowCut,
        FilterT[] body,
        Coord[] hiCut,
        NoiseSetup synth,
        Object fm, // placeholder for FM structure if needed
        PluckSetup pluck,
        OscSetup[] pitch
    ) {
        public KitSetup withNoise(NoiseSetup s, OscSetup[] p) {
            return new KitSetup(name, choke, gains, env, lowCut, body, hiCut, s, fm, pluck, p);
        }
        public KitSetup withPluck(PluckSetup p, OscSetup[] pitch) {
            return new KitSetup(name, choke, gains, env, lowCut, body, hiCut, synth, fm, p, pitch);
        }
        public KitSetup withFm(Object fmDef, OscSetup[] pitch) {
            return new KitSetup(name, choke, gains, env, lowCut, body, hiCut, synth, fmDef, pluck, pitch);
        }
    }

    public static record OscSetup(boolean tracking, float fundamental) {
        public OscSetup(boolean tracking, double f) { this(tracking, (float) f); } // convenience
    }

    public static final class NoiseSetup {
        public final Kick kick;
        public final Snare snare;
        public final Stick stick;
        public final Clap clap;
        public final CHat chat;
        public final OHat ohat;
        public final Ride ride;
        public final Bongo bongo;

        public NoiseSetup(Kick kick, Snare snare, Stick stick, Clap clap, CHat chat, OHat ohat, Ride ride, Bongo bongo) {
            this.kick = kick; this.snare = snare; this.stick = stick; this.clap = clap; this.chat = chat; this.ohat = ohat; this.ride = ride; this.bongo = bongo;
        }

        public Kick kick() { return kick; }
        public Snare snare() { return snare; }
        public Stick stick() { return stick; }
        public Clap clap() { return clap; }
        public CHat chat() { return chat; }
        public OHat ohat() { return ohat; }
        public Ride ride() { return ride; }
        public Bongo bongo() { return bongo; }

        public static record Kick(float strike, int bend, float tone) {}
        public static record Snare(String colour, float mix, float rattle) {}
        public static record Stick(float strike, int bend, int room) {}
        public static record Clap(float shimmer, float colour, int room) {}
        public static record CHat(float shimmer, float wobble, float bright) {}
        public static record OHat(float shimmer, float wobble, float density) {}
        public static record Ride(float shimmer, float noise, int room) {}
        public static record Bongo(float strike, int bend, String membrane) {}
    }

    public static final class PluckSetup {
        private final Pluck[] plucks;
        public PluckSetup(Pluck[] plucks) { this.plucks = plucks; }
        public Pluck[] plucks() { return plucks; }
        public static record Pluck(float burst, float amp, float tap, float feedback, float bright, int room, float combs, String colour) {}
    }

}
