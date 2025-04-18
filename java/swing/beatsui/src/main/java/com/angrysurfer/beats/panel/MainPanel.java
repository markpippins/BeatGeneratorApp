package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.StatusBar;
import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.StepUpdateEvent;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.SessionManager;

public class MainPanel extends JPanel implements AutoCloseable, IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MainPanel.class.getName());

    static {
        // Enable trace logging for CommandBus events
        System.setProperty("org.slf4j.simpleLogger.log.com.angrysurfer.core.api.CommandBus", "debug");
    }

    private JTabbedPane tabbedPane;
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> gateDials = new ArrayList<>();

    private int latencyCompensation = 20;
    private int lookAheadMs = 40;
    private boolean useAheadScheduling = true;
    private int activeMidiChannel = 15;

    private DrumSequencerPanel drumSequencerPanel;
    private DrumEffectsSequencerPanel drumEffectsSequencerPanel;
    private InternalSynthControlPanel internalSynthControlPanel;
    private MelodicSequencerPanel[] melodicPanels = new MelodicSequencerPanel[8];
    private PopupMixerPanel strikeMixerPanel;
    private MuteButtonsPanel muteButtonsPanel;

    private Point dragStartPoint;

    public MainPanel(StatusBar statusBar) {
        super(new BorderLayout());
        setBorder(new EmptyBorder(2, 5, 2, 5));

        CommandBus.getInstance().register(this);

        setupTabbedPane(statusBar);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupTabbedPane(StatusBar statusBar) {
        tabbedPane = new JTabbedPane();

        internalSynthControlPanel = new InternalSynthControlPanel();

        for (int i = 0; i < melodicPanels.length; i++) {
            melodicPanels[i] = createMelodicSequencerPanel(i + 2);
        }

        tabbedPane.addTab("Drum", createDrumPanel());
        tabbedPane.addTab("Machine", createDrumEffectsPanel());
        tabbedPane.addTab("Mono 1", melodicPanels[0]);
        tabbedPane.addTab("Mono 2", melodicPanels[1]);
        tabbedPane.addTab("Mono 3", melodicPanels[2]);
        tabbedPane.addTab("Mono 4", melodicPanels[3]);
        tabbedPane.addTab("Mono 5", melodicPanels[4]);
        tabbedPane.addTab("Mono 6", melodicPanels[5]);
        tabbedPane.addTab("Mono 7", melodicPanels[6]);
        tabbedPane.addTab("Mono 8", melodicPanels[7]);
        tabbedPane.addTab("Song", createSongPanel());
        tabbedPane.addTab("Synth", internalSynthControlPanel);
        tabbedPane.addTab("Mod Matrix", createModulationMatrixPanel());
        tabbedPane.addTab("Mixer", createMixerPanel());
        tabbedPane.addTab("Euclid", createEuclidPanel());
        tabbedPane.addTab("Players", new SessionPanel());

        // Create combined panel for Instruments + Systems
        tabbedPane.addTab("Instruments", createCombinedInstrumentsSystemPanel());

        tabbedPane.addTab("Launch", new LaunchPanel());
        // Remove the separate Systems tab
        // tabbedPane.addTab("System", new SystemsPanel());

        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        JPanel tabToolbar = new JPanel();
        tabToolbar.setLayout(new BoxLayout(tabToolbar, BoxLayout.X_AXIS));
        tabToolbar.setOpaque(false);

        tabToolbar.add(Box.createVerticalGlue());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        // Add mix button first
        buttonPanel.add(createMixButton());

        // Add existing control buttons
        buttonPanel.add(createAllNotesOffButton());
        buttonPanel.add(createLoopToggleButton()); // Add the new loop toggle button
        buttonPanel.add(createMetronomeToggleButton());
        // buttonPanel.add(createRestartButton());

        // Create mute buttons toolbar early
        JPanel muteButtonsToolbar = createMuteButtonsToolbar();

        // Add the mute buttons toolbar
        add(muteButtonsToolbar, BorderLayout.NORTH);

        tabToolbar.add(Box.createHorizontalStrut(10));

        tabToolbar.add(buttonPanel);
        tabToolbar.add(Box.createVerticalGlue());

        tabbedPane.putClientProperty("JTabbedPane.trailingComponent", tabToolbar);

        // Add mouse motion listener for drag-and-drop functionality
        tabbedPane.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Store the drag start point
                dragStartPoint = e.getLocationOnScreen();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
                if (tabIndex >= 0) {
                    JComponent comp = (JComponent) tabbedPane.getComponentAt(tabIndex);

                    Point p = e.getLocationOnScreen();
                    // Check if dragged far enough from original position
                    // if (isDraggedFarEnough(p, dragStartPoint)) {
                    // Create new frame containing component from this tab
                    String title = tabbedPane.getTitleAt(tabIndex);
                    createDetachedWindow(comp, title, p);

                    // Remove the tab from original pane
                    tabbedPane.remove(tabIndex);
                    // }
                }
            }
        });

        // At the end of the method, update the mute buttons with sequencers
        updateMuteButtonSequencers();
    }

    private boolean isDraggedFarEnough(Point currentPoint, Point startPoint) {
        if (startPoint == null) {
            return false;
        }
        int dx = currentPoint.x - startPoint.x;
        int dy = currentPoint.y - startPoint.y;
        return Math.sqrt(dx * dx + dy * dy) > 20; // Example threshold
    }

    /**
     * Creates a detached window from a tab component and handles reattachment when
     * closed
     */
    private void createDetachedWindow(JComponent comp, String title, Point location) {
        // Create a modeless dialog for the detached tab
        JDialog detachedWindow = new JDialog(SwingUtilities.getWindowAncestor(this), title,
                Dialog.ModalityType.MODELESS);
        detachedWindow.setContentPane(comp);

        // Add window listener to handle reattachment when window is closed
        detachedWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logger.info("Reattaching tab: " + title);

                // Remove component from dialog before adding back to tabbedPane
                detachedWindow.setContentPane(new JPanel());

                // Add the component back to the tabbed pane
                tabbedPane.addTab(title, comp);

                // Select the newly added tab
                tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
            }
        });

        // Size, position and display the window
        detachedWindow.pack();
        detachedWindow.setLocation(location);
        detachedWindow.setVisible(true);
    }

    private Component createEuclidPanel() {
        // Create a main panel with a border
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(30, 30, 30));

        // Title at the top
        JLabel titleLabel = new JLabel("Euclidean Pattern Grid", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Sans Serif", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Create a 2x2 grid of Euclidean Pattern panels
        JPanel gridPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        gridPanel.setBackground(new Color(40, 40, 40));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create 4 compact pattern panels
        String[] patternNames = { "Kick", "Snare", "HiHat", "Perc" };
        int[][] presets = {
                { 16, 4, 0, 0 }, // Pattern 1: 16 steps, 4 fills, no rotation, no width
                { 12, 3, 2, 3 }, // Pattern 2: 12 steps, 3 fills, 2 rotation, width 3
                { 8, 5, 1, 0 }, // Pattern 3: 8 steps, 5 fills, 1 rotation, no width
                { 10, 7, 3, 6 } // Pattern 4: 10 steps, 7 fills, 3 rotation, width 6
        };

        for (int i = 0; i < 4; i++) {
            // Create and configure panel
            EuclideanPatternPanel patternPanel = new EuclideanPatternPanel(true);
            patternPanel.setTitle(patternNames[i]);

            // Configure with preset values
            patternPanel.getStepsDial().setValue(presets[i][0]);
            patternPanel.getHitsDial().setValue(presets[i][1]);
            patternPanel.getRotationDial().setValue(presets[i][2]);
            patternPanel.getWidthDial().setValue(presets[i][3]);

            // Create wrapper with a visible border
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            wrapper.add(patternPanel, BorderLayout.CENTER);

            gridPanel.add(wrapper);
        }

        // Add the grid to the main panel
        mainPanel.add(gridPanel, BorderLayout.CENTER);

        // Force minimum size
        mainPanel.setMinimumSize(new Dimension(600, 500));
        mainPanel.setPreferredSize(new Dimension(800, 600));

        // Debug
        System.out.println("Created Euclidean panel grid with " + gridPanel.getComponentCount() + " components");

        return mainPanel;

    }

    private Component createSongPanel() {
        // Return instance of our new SongPanel
        return new SongPanel();
    }

    /**
     * Creates a combined panel with InstrumentsPanel and SystemsPanel in a vertical
     * JSplitPane
     */
    private JPanel createCombinedInstrumentsSystemPanel() {
        JPanel combinedPanel = new JPanel(new BorderLayout());

        // Create the component panels
        InstrumentsPanel instrumentsPanel = new InstrumentsPanel();
        SystemsPanel systemsPanel = new SystemsPanel();

        // Add titled border to systems panel for visual separation
        systemsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "MIDI Devices",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP));

        // Create a vertical JSplitPane (top-bottom arrangement)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(instrumentsPanel);
        splitPane.setBottomComponent(systemsPanel);

        // Set initial divider position (70% for instruments, 30% for systems)
        splitPane.setDividerLocation(0.7);
        splitPane.setResizeWeight(0.7); // Keep 70% proportion on resize

        // Make the divider slightly more visible
        splitPane.setDividerSize(8);

        // Remove any borders from the split pane itself
        splitPane.setBorder(null);

        // Add the split pane to the combined panel
        combinedPanel.add(splitPane, BorderLayout.CENTER);

        return combinedPanel;
    }

    private Component createDrumPanel() {
        drumSequencerPanel = new DrumSequencerPanel(noteEvent -> {
            logger.debug("Drum note event received: note={}, velocity={}",
                    noteEvent.getNote(), noteEvent.getVelocity());

            // Publish to CommandBus so MuteButtonsPanel can respond
            // Subtract 36 to convert MIDI note to drum index (36=kick, etc.)
            int drumIndex = noteEvent.getNote() - 36;
            CommandBus.getInstance().publish(Commands.DRUM_NOTE_TRIGGERED, drumSequencerPanel.getSequencer(),
                    drumIndex);
        });
        return drumSequencerPanel;
    }

    private Component createDrumEffectsPanel() {
        drumEffectsSequencerPanel = new DrumEffectsSequencerPanel(noteEvent -> {
            // No-op for now
        });
        return drumEffectsSequencerPanel;
    }

    private MelodicSequencerPanel createMelodicSequencerPanel(int channel) {
        return new MelodicSequencerPanel(channel, noteEvent -> {
            logger.debug("Note event received from sequencer: note={}, velocity={}, duration={}",
                    noteEvent.getNote(), noteEvent.getVelocity(), noteEvent.getDurationMs());

            // Get the panel's sequencer to use as the event source
            MelodicSequencer sequencer = null;
            for (MelodicSequencerPanel panel : melodicPanels) {
                if (panel != null && panel.getSequencer().getChannel() == channel) {
                    sequencer = panel.getSequencer();
                    break;
                }
            }

            // Publish to CommandBus so MuteButtonsPanel can respond
            if (sequencer != null) {
                CommandBus.getInstance().publish(Commands.MELODIC_NOTE_TRIGGERED, sequencer, noteEvent);
            }
        });
    }

    private Component createModulationMatrixPanel() {
        // Create a main panel with a border
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create a panel with GridLayout (1 row, 3 columns) with spacing
        JPanel lfoBankPanel = new JPanel(new GridLayout(1, 3, 15, 0));

        // Create three LFO panels with distinct names
        LFOPanel lfo1 = new LFOPanel();
        lfo1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("LFO 1"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        LFOPanel lfo2 = new LFOPanel();
        lfo2.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("LFO 2"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        LFOPanel lfo3 = new LFOPanel();
        lfo3.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("LFO 3"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Add the three LFO panels to the grid
        lfoBankPanel.add(lfo1);
        lfoBankPanel.add(lfo2);
        lfoBankPanel.add(lfo3);

        // Add the grid panel to the main panel
        mainPanel.add(lfoBankPanel, BorderLayout.CENTER);

        // Add a title header
        JLabel titleLabel = new JLabel("Modulation Matrix", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        return mainPanel;
    }

    private Component createMixerPanel() {
        return new MixerPanel(InternalSynthManager.getInstance().getSynthesizer());
    }

    private JPanel createMuteButtonsToolbar() {
        // Create the mute buttons panel
        muteButtonsPanel = new MuteButtonsPanel();

        // We'll update the sequencers after they're created
        return muteButtonsPanel;
    }

    private void updateMuteButtonSequencers() {
        // Set the drum sequencer
        if (drumSequencerPanel != null) {
            DrumSequencer drumSeq = drumSequencerPanel.getSequencer();
            muteButtonsPanel.setDrumSequencer(drumSeq);

            // *** THIS IS THE CRITICAL PART - Set up drum note event publisher ***
            logger.info("Setting up drum note event publisher");
            drumSeq.setNoteEventPublisher(noteEvent -> {
                int drumIndex = noteEvent.getNote() - 36; // Convert MIDI note to drum index
                logger.debug("Publishing drum note event: index={}, velocity={}",
                        drumIndex, noteEvent.getVelocity());
                CommandBus.getInstance().publish(
                        Commands.DRUM_NOTE_TRIGGERED,
                        drumSeq,
                        drumIndex);
            });
        }

        // Set the melodic sequencers
        List<MelodicSequencer> melodicSequencers = new ArrayList<>();
        for (MelodicSequencerPanel panel : melodicPanels) {
            if (panel != null) {
                MelodicSequencer seq = panel.getSequencer();
                melodicSequencers.add(seq);

                // *** THIS IS ALSO CRITICAL - Set up melodic note event publisher ***
                logger.info("Setting up melodic note event publisher for channel {}",
                        seq.getChannel());
                seq.setNoteEventPublisher(noteEvent -> {
                    logger.debug("Publishing melodic note event: note={}, velocity={}",
                            noteEvent.getNote(), noteEvent.getVelocity());
                    CommandBus.getInstance().publish(
                            Commands.MELODIC_NOTE_TRIGGERED,
                            seq,
                            noteEvent);
                });
            }
        }
        muteButtonsPanel.setMelodicSequencers(melodicSequencers);
    }

    public void playNote(int note, int velocity, int durationMs) {
        InternalSynthManager.getInstance().playNote(note, velocity, durationMs, activeMidiChannel);
    }

    public void playDrumNote(int note, int velocity) {
        InternalSynthManager.getInstance().playDrumNote(note, velocity);
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TRANSPORT_START -> {
                // isPlaying = true;
            }

            case Commands.TRANSPORT_STOP -> {
                // isPlaying = false;
            }

            case Commands.SESSION_UPDATED -> {
                // Nothing to do here - sequencer handles timing updates
            }

            case Commands.ROOT_NOTE_SELECTED -> {
                if (action.getData() instanceof String) {
                    String rootNote = (String) action.getData();
                    // Handle root note selection if needed
                }
            }

            case Commands.SCALE_SELECTED -> {
                if (action.getData() instanceof String) {
                    String scaleName = (String) action.getData();
                    // Handle scale selection if needed
                }
            }

            case Commands.SEQUENCER_STEP_UPDATE -> {
                if (action.getData() instanceof StepUpdateEvent) {
                    StepUpdateEvent stepUpdateEvent = (StepUpdateEvent) action.getData();
                    int step = stepUpdateEvent.getNewStep();
                    // Handle step update if needed
                }
            }

            case Commands.TOGGLE_TRANSPORT -> {
                // Instead of manipulating sequencer directly, publish appropriate commands
                // logger.info("Toggling transport state (current state: {})", isPlaying ?
                // "playing" : "stopped");

                if (SessionManager.getInstance().getActiveSession().isRunning()) {
                    // If currently playing, publish stop command
                    logger.info("Publishing TRANSPORT_STOP command");
                    CommandBus.getInstance().publish(Commands.TRANSPORT_STOP, this);
                } else {
                    // If currently stopped, publish start command
                    logger.info("Publishing TRANSPORT_START command");
                    CommandBus.getInstance().publish(Commands.TRANSPORT_START, this);
                }

                // The state will be updated when we receive TRANSPORT_STARTED or
                // TRANSPORT_STOPPED events
            }
        }
    }

    private JToggleButton createMetronomeToggleButton() {
        JToggleButton metronomeButton = new JToggleButton();
        metronomeButton.setText(Symbols.getSymbol(Symbols.METRONOME)); // Unicode metronome symbol
        // Set equal width and height to ensure square shape
        metronomeButton.setPreferredSize(new Dimension(28, 28));
        metronomeButton.setMinimumSize(new Dimension(28, 28));
        metronomeButton.setMaximumSize(new Dimension(28, 28));

        // Remove the rounded rectangle property
        // metronomeButton.putClientProperty("JButton.buttonType", "roundRect");

        // Explicitly set square size and enforce square shape
        metronomeButton.putClientProperty("JButton.squareSize", true);
        metronomeButton.putClientProperty("JComponent.sizeVariant", "regular");

        metronomeButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
        metronomeButton.setHorizontalAlignment(SwingConstants.CENTER);
        metronomeButton.setVerticalAlignment(SwingConstants.CENTER);
        metronomeButton.setMargin(new Insets(0, 0, 0, 0));
        metronomeButton.setToolTipText("Toggle Metronome");

        metronomeButton.addActionListener(e -> {
            boolean isSelected = metronomeButton.isSelected();
            logger.info("Metronome toggled: " + (isSelected ? "ON" : "OFF"));
            CommandBus.getInstance().publish(isSelected ? Commands.METRONOME_START : Commands.METRONOME_STOP, this);
        });

        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                switch (action.getCommand()) {
                    case Commands.METRONOME_STARTED:
                        SwingUtilities.invokeLater(() -> {
                            metronomeButton.setSelected(true);
                            metronomeButton.setBackground(Color.GREEN);
                            metronomeButton.invalidate();
                            metronomeButton.repaint();
                        });
                        break;

                    case Commands.METRONOME_STOPPED:
                        SwingUtilities.invokeLater(() -> {
                            metronomeButton.setSelected(false);
                            metronomeButton.setBackground(Color.RED);
                            metronomeButton.invalidate();
                            metronomeButton.repaint();
                        });
                        break;
                }
            }
        });

        return metronomeButton;
    }

    private JToggleButton createLoopToggleButton() {
        JToggleButton loopButton = new JToggleButton();
        loopButton.setText(Symbols.getSymbol(Symbols.LOOP)); // Unicode loop symbol

        // Set equal width and height to ensure square shape
        loopButton.setPreferredSize(new Dimension(28, 28));
        loopButton.setMinimumSize(new Dimension(28, 28));
        loopButton.setMaximumSize(new Dimension(28, 28));

        // Explicitly set square size and enforce square shape
        loopButton.putClientProperty("JButton.squareSize", true);
        loopButton.putClientProperty("JComponent.sizeVariant", "regular");

        loopButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
        loopButton.setHorizontalAlignment(SwingConstants.CENTER);
        loopButton.setVerticalAlignment(SwingConstants.CENTER);
        loopButton.setMargin(new Insets(0, 0, 0, 0));
        loopButton.setToolTipText("Toggle All Sequencer Looping");

        // Default to selected (looping enabled)
        loopButton.setSelected(true);

        loopButton.addActionListener(e -> {
            boolean isLooping = loopButton.isSelected();
            logger.info("Global looping toggled: {}", isLooping ? "ON" : "OFF");

            // Set looping state for drum sequencer
            if (drumSequencerPanel != null && drumSequencerPanel.getSequencer() != null) {
                drumSequencerPanel.getSequencer().setLooping(isLooping);
            }

            // Set looping state for all melodic sequencers
            for (MelodicSequencerPanel panel : melodicPanels) {
                if (panel != null && panel.getSequencer() != null) {
                    panel.getSequencer().setLooping(isLooping);
                }
            }

            // Set looping state for drum effects sequencer if present
            // if (drumEffectsSequencerPanel != null &&
            // drumEffectsSequencerPanel.getSequencer() != null) {
            // drumEffectsSequencerPanel.getSequencer().setLooping(isLooping);
            // }

            // Visual feedback - change button color based on state
            loopButton.setBackground(isLooping ? new Color(120, 200, 120) : new Color(200, 120, 120));

            // Publish command for other components to respond to
            CommandBus.getInstance().publish(
                    isLooping ? Commands.GLOBAL_LOOPING_ENABLED : Commands.GLOBAL_LOOPING_DISABLED,
                    this);
        });

        // Initial button color - green for enabled looping
        loopButton.setBackground(new Color(120, 200, 120));

        return loopButton;
    }

    private JButton createAllNotesOffButton() {
        JButton notesOffButton = new JButton();
        notesOffButton.setText(Symbols.getSymbol(Symbols.ALL_NOTES_OFF)); // Unicode all notes off symbol
        // Set equal width and height to ensure square shape
        notesOffButton.setPreferredSize(new Dimension(28, 28));
        notesOffButton.setMinimumSize(new Dimension(28, 28));
        notesOffButton.setMaximumSize(new Dimension(28, 28));

        // Remove the rounded rectangle property
        // notesOffButton.putClientProperty("JButton.buttonType", "roundRect");

        // Explicitly set square size and enforce square shape
        notesOffButton.putClientProperty("JButton.squareSize", true);
        notesOffButton.putClientProperty("JComponent.sizeVariant", "regular");

        notesOffButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
        notesOffButton.setHorizontalAlignment(SwingConstants.CENTER);
        notesOffButton.setVerticalAlignment(SwingConstants.CENTER);
        notesOffButton.setMargin(new Insets(0, 0, 0, 0));
        notesOffButton.setToolTipText("All Notes Off - Silence All Sounds");

        notesOffButton.addActionListener(e -> {
            logger.info("All Notes Off button pressed");
            CommandBus.getInstance().publish(Commands.ALL_NOTES_OFF, this);
        });

        return notesOffButton;
    }

    private JButton createRestartButton() {
        JButton restartButton = new JButton("Restart App");
        restartButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    null,
                    "This will restart the application. Any unsaved changes will be lost.\nContinue?",
                    "Restart Application",
                    JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                try {
                    System.exit(0);

                    String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                    File currentJar = new File(
                            MainPanel.class.getProtectionDomain().getCodeSource().getLocation().toURI());

                    if (currentJar.getName().endsWith(".jar")) {
                        ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", currentJar.getPath());
                        builder.start();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                            "Error restarting: " + ex.getMessage(),
                            "Restart Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return restartButton;
    }

    private JButton createMixButton() {
        JButton mixButton = new JButton();
        // Use a mixer icon character instead of text to fit in a square button
        // mixButton.setText("🎛️");
        mixButton.setText(Symbols.getSymbol(Symbols.MIX)); // Unicode mixer sy

        // Set equal width and height to ensure square shape
        mixButton.setPreferredSize(new Dimension(28, 28));
        mixButton.setMinimumSize(new Dimension(28, 28));
        mixButton.setMaximumSize(new Dimension(28, 28));

        // Remove the rounded rectangle property
        // mixButton.putClientProperty("JButton.buttonType", "roundRect");

        // Explicitly set square size and enforce square shape
        mixButton.putClientProperty("JButton.squareSize", true);
        mixButton.putClientProperty("JComponent.sizeVariant", "regular");

        // Style text to match other buttons
        mixButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
        mixButton.setHorizontalAlignment(SwingConstants.CENTER);
        mixButton.setVerticalAlignment(SwingConstants.CENTER);
        mixButton.setMargin(new Insets(0, 0, 0, 0));
        mixButton.setToolTipText("Show Drum Mixer");

        // Add action listener to show mixer dialog
        mixButton.addActionListener(e -> {
            // Get current sequencer
            DrumSequencer sequencer = null;
            if (drumSequencerPanel != null) {
                sequencer = drumSequencerPanel.getSequencer();
            }

            if (sequencer != null) {
                // Create a new PopupMixerPanel and dialog each time
                PopupMixerPanel mixerPanel = new PopupMixerPanel(sequencer);

                // Create dialog to show the mixer
                JDialog mixerDialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                        "Pop-Up Mixer",
                        Dialog.ModalityType.MODELESS); // Non-modal dialog
                mixerDialog.setContentPane(mixerPanel);
                mixerDialog.pack();
                mixerDialog.setLocationRelativeTo(this);
                mixerDialog.setMinimumSize(new Dimension(600, 400));

                // Add window listener to handle dialog closing
                mixerDialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        // Clean up any resources if needed
                        // (Optional) For example, remove any listeners registered to the mixer panel
                    }
                });

                mixerDialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this,
                        "No drum sequencer available",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        return mixButton;
    }

    public int getSelectedTab() {
        return tabbedPane.getSelectedIndex();
    }

    public void setSelectedTab(int index) {
        tabbedPane.setSelectedIndex(index);
    }

    public Component getSelectedComponent() {
        return tabbedPane.getSelectedComponent();
    }

    @Override
    public void close() throws Exception {
        if (tabbedPane != null) {
            for (Component comp : tabbedPane.getComponents()) {
                if (comp instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) comp).close();
                    } catch (Exception e) {
                        logger.error("Error closing component: " + e.getMessage());
                    }
                }
            }
        }
    }
}
