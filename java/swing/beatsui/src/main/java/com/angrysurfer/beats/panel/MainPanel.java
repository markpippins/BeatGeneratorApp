package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.StatusBar;
import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.NoteEvent;
import com.angrysurfer.core.sequencer.StepUpdateEvent;
import com.angrysurfer.core.service.InternalSynthManager;

public class MainPanel extends JPanel implements AutoCloseable, IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MainPanel.class.getName());
    
    private JTabbedPane tabbedPane;
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> gateDials = new ArrayList<>();
    private boolean isPlaying = false;
    private int latencyCompensation = 20;
    private int lookAheadMs = 40;
    private boolean useAheadScheduling = true;
    private int activeMidiChannel = 15;
    
    private DrumSequencerPanel drumSequencerPanel;
    private DrumEffectsSequencerPanel drumEffectsSequencerPanel;
    private InternalSynthControlPanel internalSynthControlPanel;
    private MelodicSequencerPanel[] melodicPanels = new MelodicSequencerPanel[4];
    private PopupMixerPanel strikeMixerPanel;
    private MuteButtonsPanel muteButtonsPanel;

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

        tabbedPane.addTab("Drum Sequencer", createDrumPanel());
        tabbedPane.addTab("Drum Machine", createDrumEffectsPanel());
        tabbedPane.addTab("Mono Sequencer 1", melodicPanels[0]);
        tabbedPane.addTab("Mono Sequencer 2", melodicPanels[1]);
        tabbedPane.addTab("Mono Sequencer 3", melodicPanels[2]);
        tabbedPane.addTab("Mono Sequencer 4", melodicPanels[3]);
        tabbedPane.addTab("Synth", internalSynthControlPanel);
        tabbedPane.addTab("Poly Sequencer", createChordSequencerPanel());
        tabbedPane.addTab("Mixer", createMixerPanel());
        
        tabbedPane.addTab("Players", new SessionPanel());
        tabbedPane.addTab("Instruments", new InstrumentsPanel());
        tabbedPane.addTab("Launch", new LaunchPanel());
        tabbedPane.addTab("System", new SystemsPanel());
        
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
        buttonPanel.add(createMetronomeToggleButton());
        // buttonPanel.add(createRestartButton());

        // Create mute buttons toolbar early
        JPanel muteButtonsToolbar = createMuteButtonsToolbar();

        // Add the mute buttons toolbar
        tabToolbar.add(muteButtonsToolbar);
        
        tabToolbar.add(Box.createHorizontalStrut(10));

        tabToolbar.add(buttonPanel);
        tabToolbar.add(Box.createVerticalGlue());

        tabbedPane.putClientProperty("JTabbedPane.trailingComponent", tabToolbar);

        // At the end of the method, update the mute buttons with sequencers
        updateMuteButtonSequencers();
    }
    
    private Component createDrumPanel() {
        drumSequencerPanel = new DrumSequencerPanel(noteEvent -> {
            logger.debug("Drum note event received: note={}, velocity={}", 
                    noteEvent.getNote(), noteEvent.getVelocity());
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
        });
    }
    
    private Component createChordSequencerPanel() {
        return new JPanel();
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
            muteButtonsPanel.setDrumSequencer(drumSequencerPanel.getSequencer());
        }
        
        // Set the melodic sequencers
        List<MelodicSequencer> melodicSequencers = new ArrayList<>();
        for (MelodicSequencerPanel panel : melodicPanels) {
            if (panel != null) {
                melodicSequencers.add(panel.getSequencer());
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
                isPlaying = true;
            }

            case Commands.TRANSPORT_STOP -> {
                isPlaying = false;
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
        }
    }

    private JToggleButton createMetronomeToggleButton() {
        JToggleButton metronomeButton = new JToggleButton();
        metronomeButton.setText("🕰️");
        // metronomeButton.setBackground(ColorUtils.mutedRed);
        metronomeButton.setPreferredSize(new Dimension(28, 28));
        metronomeButton.setMinimumSize(new Dimension(28, 28));
        metronomeButton.setMaximumSize(new Dimension(28, 28));
        metronomeButton.putClientProperty("JButton.buttonType", "roundRect");
        metronomeButton.putClientProperty("JButton.squareSize", true);
        metronomeButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
        metronomeButton.setHorizontalAlignment(SwingConstants.CENTER);
        metronomeButton.setVerticalAlignment(SwingConstants.CENTER);
        metronomeButton.setMargin(new Insets(0, 0, 0, 0));
        metronomeButton.setToolTipText("Toggle Metronome");
        
        metronomeButton.addActionListener(e -> {
            boolean isSelected = metronomeButton.isSelected();
            logger.info("Metronome toggled: " + (isSelected ? "ON" : "OFF"));
            CommandBus.getInstance().publish(isSelected ? Commands.METRONOME_START : Commands.METRONOME_STOP, this);
            // metronomeButton.setText(isSelected ? "⏱" : "⏱");
            // metronomeButton.setBackground(isSelected ? ColorUtils.mutedOlive : ColorUtils.mutedRed);
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

    private JButton createAllNotesOffButton() {
        JButton notesOffButton = new JButton();
        notesOffButton.setText("🚨");
        notesOffButton.setPreferredSize(new Dimension(28, 28));
        notesOffButton.setMinimumSize(new Dimension(28, 28));
        notesOffButton.setMaximumSize(new Dimension(28, 28));
        notesOffButton.putClientProperty("JButton.buttonType", "roundRect");
        notesOffButton.putClientProperty("JButton.squareSize", true);
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
                    File currentJar = new File(MainPanel.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    
                    if(currentJar.getName().endsWith(".jar")) {
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
        JButton mixButton = new JButton("Mix");
        mixButton.setToolTipText("Show Drum Mixer");
        
        // Style to match other controls
        mixButton.setPreferredSize(new Dimension(60, 28));
        mixButton.putClientProperty("JButton.buttonType", "roundRect");
        
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
