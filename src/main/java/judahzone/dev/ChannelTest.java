package judahzone.dev;

import static judahzone.util.Constants.LEFT_OUT;
import static judahzone.util.Constants.RIGHT_OUT;
import static judahzone.util.Constants.bufSize;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.FX;
import judahzone.api.PlayAudio;
import judahzone.data.Asset;
import judahzone.data.Recording;
import judahzone.fx.Chorus;
import judahzone.fx.Compressor;
import judahzone.fx.Delay;
import judahzone.fx.EQ;
import judahzone.fx.CutFilter;
import judahzone.fx.Freeverb;
import judahzone.fx.Gain;
import judahzone.fx.Overdrive;
import judahzone.fx.op.FXBus;
import judahzone.jnajack.BasicPlayer;
import judahzone.jnajack.ZoneJackClient;
import judahzone.util.AudioTools;
import judahzone.util.MP3;

/** Test out Effects and the ChannelStrip */
public class ChannelTest extends ZoneJackClient {

	private JackPort outL, outR;

	static final String DEFAULT_FILE = "/home/judah/Music/Stubborn All-Stars - Open Season/Stubborn All-Stars - 09 - Catch that Train.mp3";
	private final Recording tape;

	private final FXBus channel;
	private final JTable table;
	private final JPanel bottom = new JPanel();

	// Basic player to play loaded recording into JACK outputs
	private BasicPlayer player;
	private boolean started = false;

	float[] workL = new float[bufSize()];
	float[] workR = new float[bufSize()];

	public ChannelTest(String file) throws JackException, Exception {
	    super("zone-test");
	    File absolute = new File(file);
	    tape = MP3.load(absolute);

	    Asset a = new Asset(absolute.getName(),  absolute, tape, tape.size() * judahzone.util.Constants.bufSize(), Asset.Category.USER);

	    // Prepare player but do not start; we'll start on first process() call
	    player = new BasicPlayer();
	    player.setRecording(a);
	    player.setType(PlayAudio.Type.LOOP);

	     // prepare bus as anonymous instances of the effects so FloatsBus owns them
	    channel = new FXBus(
	        new Gain(),
	        new EQ(),
	        new CutFilter(true),    // hiCut
	        new CutFilter(false),   // loCut
	        new Compressor(),
	        new Delay(),
	        new Overdrive(),
	        new Chorus(),
	        new Freeverb()
	    );

	    // Build UI on EDT
	    JFrame frame = new JFrame("FloatsTest");
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setSize(400, 600);
	    frame.setMinimumSize(new Dimension(300, 400));

	    // Table model backed by StereoBus effects
	    EffectsTableModel model = new EffectsTableModel(channel.listAll());
	    table = new JTable(model);
	    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    table.setFillsViewportHeight(true);

	    // Make first column wider
	    if (table.getColumnModel().getColumnCount() >= 2) {
	        table.getColumnModel().getColumn(0).setPreferredWidth(260);
	        table.getColumnModel().getColumn(1).setPreferredWidth(40);
	    }

	    // Bottom panel: starts empty; will be rebuilt on selection changes
	    // Add outer 16px inset so both labels and sliders sit inside a 16px buffer
	    bottom.setLayout(new GridLayout(1, 1));
	    bottom.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

	    // Selection listener updates bottom panel with sliders for selected effect
	    table.getSelectionModel().addListSelectionListener(e -> {
	        if (!e.getValueIsAdjusting()) {
	            int row = table.getSelectedRow();
	            updateBottomForSelected(model, row);
	        }
	    });
	    // Add mouse listener to toggle the checkbox column
	    table.addMouseListener(new java.awt.event.MouseAdapter() {
	        @Override
	        public void mousePressed(java.awt.event.MouseEvent e) {
	            java.awt.Point p = e.getPoint();
	            int row = table.rowAtPoint(p);
	            int col = table.columnAtPoint(p);
	            if (row < 0 || col != 1) return;

	            // Save current selection so we can restore it (prevents the click from altering selection)
	            int[] sel = table.getSelectedRows();

	            // Read current boolean and toggle by updating the table model directly.
	            Object val = table.getModel().getValueAt(row, col);
	            boolean current = Boolean.TRUE.equals(val);
	            table.getModel().setValueAt(!current, row, col);

	            // Restore previous selection (in case the click changed it)
	            table.clearSelection();
	            for (int r : sel) {
	                if (r >= 0 && r < table.getRowCount()) {
	                    table.addRowSelectionInterval(r, r);
	                }
	            }

	            // Consume event so default selection handling is less likely to interfere
	            e.consume();
	        }
	    });

	    JScrollPane scroll = new JScrollPane(table);

	    // Split the frame so top is table and bottom shows sliders
	    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, bottom);
	    // Reserve 1/3 of vertical space for the table and 2/3 for custom sliders initially
	    split.setResizeWeight(1.0 / 3.0);
	    split.setDividerLocation(1.0 / 3.0);

	    frame.getContentPane().add(split, BorderLayout.CENTER);

	    frame.setLocationRelativeTo(null);
	    frame.setVisible(true);
	    start(); // super calls initialize(), makeConnections()
	}

	// Rebuild bottom panel for the selected row (one slider per param)
	private void updateBottomForSelected(EffectsTableModel model, int row) {
	    SwingUtilities.invokeLater(() -> {
	        bottom.removeAll();
	        if (row < 0 || row >= model.getRowCount()) {
	            bottom.revalidate();
	            bottom.repaint();
	            return;
	        }
	        FX fx = model.getEffectAt(row);
	        int paramCount = fx.getParamCount();
	        bottom.setLayout(new GridLayout(Math.max(1, paramCount), 1, 4, 4));
	        List<String> names = fx.getSettingNames();
	        for (int i = 0; i < paramCount; i++) {
	            String name = (i < names.size() ? names.get(i) : ("param" + i));
	            JPanel rowPanel = new JPanel(new BorderLayout(8, 0));
	            JLabel lbl = new JLabel(name);
	            JSlider slider = new JSlider(0, 100, fx.get(i));
	            slider.setMajorTickSpacing(25);
	            slider.setMinorTickSpacing(5);
	            slider.setPaintTicks(true);

	            // Removed per-slider inset; the outer bottom panel provides the 16px buffer
	            // add listener after initial value set to avoid extra writes during setup
	            final int idx = i;
	            slider.addChangeListener(e -> {
	                // Only apply when user finished adjusting (avoids flooding with intermediate values)
	                if (!slider.getValueIsAdjusting()) {
	                    fx.set(idx, slider.getValue());
	                }
	            });
	            rowPanel.add(lbl, BorderLayout.WEST);
	            rowPanel.add(slider, BorderLayout.CENTER);
	            bottom.add(rowPanel);
	        }
	        bottom.revalidate();
	        bottom.repaint();
	    });
	}

	// Table model showing Effect name and active checkbox bound to StereoBus
	private class EffectsTableModel extends AbstractTableModel {
	    private final List<FX> effects;
	    private final String[] cols = { "Effect", "On" };

	    EffectsTableModel(List<FX> effects) {
	        this.effects = effects;
	    }

	    @Override
	    public int getRowCount() {
	        return effects.size();
	    }

	    @Override
	    public int getColumnCount() {
	        return cols.length;
	    }

	    @Override
	    public String getColumnName(int column) {
	        return cols[column];
	    }

	    @Override
	    public Class<?> getColumnClass(int columnIndex) {
	        return columnIndex == 1 ? Boolean.class : String.class;
	    }

	    @Override
	    public boolean isCellEditable(int rowIndex, int columnIndex) {
	        return columnIndex == 1; // only checkbox editable
	    }

	    @Override
	    public Object getValueAt(int rowIndex, int columnIndex) {
	        FX fx = effects.get(rowIndex);
	        return columnIndex == 0 ? fx.getName() : channel.isActive(fx);
	    }

	    @Override
	    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    	    if (columnIndex != 1) return;
    	    FX fx = effects.get(rowIndex);
    	    boolean wantOn = Boolean.TRUE.equals(aValue);
    	    channel.setActive(fx, wantOn);
    	    // no selection changes here — checkbox toggles only the effect state
//	        SwingUtilities.invokeLater(() -> {
//	            if (rowIndex >= 0 && rowIndex < getRowCount()) {
//	                table.setRowSelectionInterval(rowIndex, rowIndex);
//	                fireTableRowsUpdated(rowIndex, rowIndex);
//	            }
//	        });
	    }

	    public FX getEffectAt(int rowIndex) {
	        return effects.get(rowIndex);
	    }
	}

	public static void main(String[] args) {
	    SwingUtilities.invokeLater(() -> {
	        try {
	            String file = DEFAULT_FILE;
	            if (args != null && args.length > 0) {
	                file = args[0];
	            }
	            new ChannelTest(file);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    });
	}

	@Override
	protected void initialize() throws Exception {
	    outL = jackclient.registerPort("left", AUDIO, JackPortIsOutput);
	    outR = jackclient.registerPort("right", AUDIO, JackPortIsOutput);
	}

	@Override
	protected void makeConnections() throws JackException {
	    // main output
	    jack.connect(jackclient, outL.getName(), LEFT_OUT);
	    jack.connect(jackclient, outR.getName(), RIGHT_OUT);
	}

	@Override
	public boolean process(JackClient client, int nframes) {
//	    // Get FloatBuffers for JACK output ports
//	    FloatBuffer left = outL.getFloatBuffer();
//	    FloatBuffer right = outR.getFloatBuffer();
//	    // Clear outputs first
//	    AudioTools.silence(left);
//	    AudioTools.silence(right);

//		float[][] frame = Memory.STEREO.getFrame();
//		float[] left = frame[0];
//		float[] right = frame[1];

	    // Start playback on the first process() invocation
	    if (!started) {
	        player.play(true);
	        started = true;
	    }

	    // Let BasicPlayer mix the loaded tape into the output buffers.
	    // BasicPlayer will loop if configured to do so (we tried to set Type.LOOP in ctor).
	    player.process(workL, workR);

	    // Let our channel process the mixed output buffers in-place
	    // TODO channel.process(workL, workR);



	    // send back to jack
	    AudioTools.copy(workL, outL.getFloatBuffer().rewind());
	    AudioTools.copy(workR, outR.getFloatBuffer().rewind());
	    return true;
	}


	}