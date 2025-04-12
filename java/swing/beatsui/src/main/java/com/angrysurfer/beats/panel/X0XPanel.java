// package com.angrysurfer.beats.panel;

// import java.awt.BorderLayout;
// import java.awt.Color;
// import java.awt.Component;
// import java.awt.Dimension;
// import java.awt.FlowLayout;
// import java.util.ArrayList;
// import java.util.List;

// import javax.swing.BorderFactory;
// import javax.swing.Box;
// import javax.swing.BoxLayout;
// import javax.swing.JPanel;
// import javax.swing.JTabbedPane;
// import javax.swing.JToggleButton;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import com.angrysurfer.beats.widget.ColorUtils;
// import com.angrysurfer.beats.widget.Dial;
// import com.angrysurfer.core.api.Command;
// import com.angrysurfer.core.api.CommandBus;
// import com.angrysurfer.core.api.Commands;
// import com.angrysurfer.core.api.IBusListener;
// import com.angrysurfer.core.sequencer.DrumSequencer;
// import com.angrysurfer.core.sequencer.MelodicSequencer;
// import com.angrysurfer.core.sequencer.StepUpdateEvent;
// import com.angrysurfer.core.service.InternalSynthManager;

// import lombok.Getter;
// import lombok.Setter;

// @Getter
// @Setter
// public class X0XPanel extends JPanel implements IBusListener {

//     private static final Logger logger = LoggerFactory.getLogger(X0XPanel.class);

//     // Add this field to store the tabbedPane reference
//     private JTabbedPane tabbedPane;
    
//     // Existing fields
//     private final List<Dial> velocityDials = new ArrayList<>();
//     private final List<Dial> gateDials = new ArrayList<>();
//     private boolean isPlaying = false;
//     private int latencyCompensation = 20;
//     private int lookAheadMs = 40;
//     private boolean useAheadScheduling = true;
//     private int activeMidiChannel = 15;
    
//     private DrumSequencerPanel drumSequencerPanel;
//     private DrumEffectsSequencerPanel drumEffectsSequencerPanel;
//     // Keep the reference for panel access but remove synth management logic
//     private InternalSynthControlPanel internalSynthControlPanel;

//     public X0XPanel() {
//         super(new BorderLayout());

//         // Register with command bus
//         CommandBus.getInstance().register(this);

//         // Set up UI components
//         setup();
//     }

//     @Override
//     public void onAction(Command action) {
//         if (action.getCommand() == null) {
//             return;
//         }

//         switch (action.getCommand()) {
//             case Commands.TRANSPORT_START -> {
//                 isPlaying = true;
//                 // Sequencer handles its own state - nothing to do here
//             }

//             case Commands.TRANSPORT_STOP -> {
//                 isPlaying = false;
//                 // Sequencer handles its own state - nothing to do here
//             }

//             case Commands.SESSION_UPDATED -> {
//                 // Nothing to do here - sequencer handles timing updates
//             }

//             case Commands.ROOT_NOTE_SELECTED -> {
//                 if (action.getData() instanceof String) {
//                     String rootNote = (String) action.getData();
//                     // if (melodicSequencerPanel != null) {
//                     //     melodicSequencerPanel.getSequencer().setRootNote(rootNote);
//                     //     melodicSequencerPanel.getSequencer().updateQuantizer();
//                     // }
//                 }
//             }

//             case Commands.SCALE_SELECTED -> {
//                 if (action.getData() instanceof String) {
//                     String scaleName = (String) action.getData();
//                     // if (melodicSequencerPanel != null) {
//                     //     melodicSequencerPanel.getSequencer().setScale(scaleName);
//                     //     melodicSequencerPanel.getSequencer().updateQuantizer();
//                     // }
//                 }
//             }

//             // Listen for step updates from sequencer to update status display
//             case Commands.SEQUENCER_STEP_UPDATE -> {
//                 if (action.getData() instanceof StepUpdateEvent) {
//                     StepUpdateEvent stepUpdateEvent = (StepUpdateEvent) action.getData();

//                     int step = stepUpdateEvent.getNewStep();
//                     // int patternLength = melodicSequencerPanel.getSequencer().getPatternLength();

//                     // Update status display with current step
//                     // CommandBus.getInstance().publish(Commands.STATUS_UPDATE, this,
//                     //         new StatusUpdate("Step: " + (step + 1) + " of " + patternLength));
//                 }
//             }
//         }
//     }

//     private void setup() {
//         JPanel containerPanel = new JPanel(new BorderLayout());
//         containerPanel.setBorder(BorderFactory.createEmptyBorder());

//         // Store reference to tabbedPane as class field
//         tabbedPane = createX0XPanel();
//         containerPanel.add(tabbedPane, BorderLayout.CENTER);

//         add(containerPanel);
//     }

//     private MelodicSequencerPanel[] melodicPanels = new MelodicSequencerPanel[4] ;

//     private JTabbedPane createX0XPanel() {
//         JTabbedPane tabbedPane = new JTabbedPane();

//         // Create the synth control panel first to get the synthesizer
//         internalSynthControlPanel = new InternalSynthControlPanel();
//         melodicPanels[0] = createMelodicSequencerPanel(2);
//         melodicPanels[1] = createMelodicSequencerPanel(3);
//         melodicPanels[2] = createMelodicSequencerPanel(4);
//         melodicPanels[3] = createMelodicSequencerPanel(5);

//         // Add tabs
//         tabbedPane.addTab("Poly", createDrumPanel());
//         tabbedPane.addTab("Poly FX", createDrumEffectsPanel());
//         tabbedPane.addTab("Mono 1", melodicPanels[0]);
//         tabbedPane.addTab("Mono 2", melodicPanels[1]);
//         tabbedPane.addTab("Mono 3", melodicPanels[2]);
//         tabbedPane.addTab("Mono 4", melodicPanels[3]);
        
//         tabbedPane.addTab("Synth", internalSynthControlPanel);
//         tabbedPane.addTab("Poly Sequencer", createChordSequencerPanel());
//         tabbedPane.addTab("Mixer", createMixerPanel());
        
//         // Add trailing toolbar with mute buttons to the tabbed pane
//         JPanel tabToolbar = createMuteButtonsToolbar();
//         tabbedPane.putClientProperty("JTabbedPane.trailingComponent", tabToolbar);

//         return tabbedPane;
//     }

//     /**
//      * Creates a toolbar with mute buttons for drum pads and melodic sequencers
//      *
//      * @return A JPanel containing the mute buttons
//      */
//     private JPanel createMuteButtonsToolbar() {
//         // Main container with vertical centering
//         JPanel tabToolbar = new JPanel();
//         tabToolbar.setLayout(new BoxLayout(tabToolbar, BoxLayout.X_AXIS));
//         tabToolbar.setOpaque(false);

//         // Add vertical glue for centering
//         tabToolbar.add(Box.createVerticalGlue());

//         // Create panel for the buttons with small margins
//         JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
//         buttonPanel.setOpaque(false);
//         buttonPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 8));

//         // Create 16 mute buttons for drum pads
//         for (int i = 0; i < 16; i++) {
//             JToggleButton muteButton = createMuteButton(i, true);
//             buttonPanel.add(muteButton);
//         }

//         // Add a small separator
//         buttonPanel.add(Box.createHorizontalStrut(8));

//         // Create 4 mute buttons for melodic sequencers
//         for (int i = 0; i < 4; i++) {
//             JToggleButton muteButton = createMuteButton(i, false);
//             buttonPanel.add(muteButton);
//         }

//         // Add button panel to toolbar
//         tabToolbar.add(buttonPanel);
//         tabToolbar.add(Box.createVerticalGlue());

//         return tabToolbar;
//     }

//     /**
//      * Creates a single mute button with proper styling
//      *
//      * @param index The index of the button (0-15 for drums, 0-3 for melodic)
//      * @param isDrum Whether this is a drum mute button (true) or melodic
//      * (false)
//      * @return A styled toggle button
//      */
//     private JToggleButton createMuteButton(int index, boolean isDrum) {
//         JToggleButton muteButton = new JToggleButton();

//         // Make buttons very compact
//         Dimension size = new Dimension(16, 16);
//         muteButton.setPreferredSize(size);
//         muteButton.setMinimumSize(size);
//         muteButton.setMaximumSize(size);

//         // Set appearance
//         muteButton.setText("");
//         muteButton.setToolTipText("Mute " + (isDrum ? "Drum " : "Synth ") + (index + 1));

//         // Use FlatLaf styling for rounded look
//         // muteButton.putClientProperty("JButton.buttonType", "roundRect");
//         muteButton.putClientProperty("JButton.squareSize", true);

//         // Apply different colors for drum vs melodic buttons
//         Color defaultColor = isDrum
//                 ? ColorUtils.fadedOrange.darker()
//                 : ColorUtils.coolBlue.darker();
//         Color activeColor = isDrum
//                 ? ColorUtils.fadedOrange
//                 : ColorUtils.coolBlue;

//         muteButton.setBackground(defaultColor);

//         // Add action listener for mute functionality
//         final int buttonIndex = index;
//         final boolean isDrumButton = isDrum;

//         muteButton.addActionListener(e -> {
//             boolean isMuted = muteButton.isSelected();
//             muteButton.setBackground(isMuted ? activeColor : defaultColor);

//             // Apply mute to the appropriate sequencer
//             if (isDrumButton) {
//                 if (drumSequencerPanel != null) {
//                     // Mute drum pad
//                     toggleDrumMute(buttonIndex, isMuted);
//                 }
//             } else {
//                 // Mute melodic sequencer
//                 toggleMelodicMute(buttonIndex, isMuted);
//             }
//         });

//         return muteButton;
//     }

//     /**
//      * Toggle mute state for a specific drum pad
//      *
//      * @param drumIndex The index of the drum pad to mute/unmute (0-15)
//      * @param muted Whether to mute (true) or unmute (false)
//      */
//     private void toggleDrumMute(int drumIndex, boolean muted) {
//         logger.info("{}muting drum {}", muted ? "" : "Un", drumIndex + 1);
//         if (drumSequencerPanel != null) {
//             // Use the sequencer's API to mute the specific drum
//             DrumSequencer sequencer = drumSequencerPanel.getSequencer();
//             if (sequencer != null) {
//                 sequencer.setVelocity(drumIndex, muted ? 0 : 100);
//             }
//         }
//     }

//     /**
//      * Toggle mute state for a melodic sequencer
//      *
//      * @param seqIndex The index of the melodic sequencer (0-3)
//      * @param muted Whether to mute (true) or unmute (false)
//      */
//     private void toggleMelodicMute(int seqIndex, boolean muted) {
//         logger.info("{}muting melodic sequencer {}", muted ? "" : "Un", seqIndex + 1);
//         if (melodicPanels[seqIndex] != null) {
//             // Use the sequencer's API to mute the specific melodic sequencer
//             MelodicSequencer sequencer = melodicPanels[seqIndex].getSequencer();
//             if (sequencer != null) {
//                 sequencer.setLevel(muted ? 0 : 100);
//             }
//         }
//         // Calculate the actual tab index (Mono 1 starts at tab index 2)
//         // int tabIndex = seqIndex + 2;

//         // if (tabIndex >= 0 && tabIndex < 6) {
//         //     Component comp = tabbedPane.getComponentAt(tabIndex);
//         //     if (comp instanceof MelodicSequencerPanel) {
//         //         MelodicSequencerPanel panel = (MelodicSequencerPanel) comp;
//         //         MelodicSequencer sequencer = panel.getSequencer();
//         //         if (sequencer != null) {
//         //             // Set volume to 0 when muted, 100 when unmuted
//         //             sequencer.setLevel(muted ? 0 : 100);
//         //         }
//         //     }
//         // }
//     }

//     private Component createMixerPanel() {
//         // Get synthesizer from InternalSynthManager instead of SynthControlPanel
//         return new MixerPanel(InternalSynthManager.getInstance().getSynthesizer());
//     }

//     private Component createChordSequencerPanel() {
//         return new JPanel();
//     }

//     private Component createDrumPanel() {
//         drumSequencerPanel = new DrumSequencerPanel(noteEvent -> {
//             // This callback is now only for UI feedback, not for playing sounds
//             logger.debug("Drum note event received: note={}, velocity={}", 
//                     noteEvent.getNote(), noteEvent.getVelocity());
            
//             // You could add UI feedback here, like flashing the corresponding drum pad
//         });
//         return drumSequencerPanel;
//     }

//     private Component createInternalSynthControlPanel() {
//         internalSynthControlPanel = new InternalSynthControlPanel();
//         return internalSynthControlPanel;
//     }

//     private Component createDrumEffectsPanel() {
//         drumEffectsSequencerPanel = new DrumEffectsSequencerPanel(noteEvent -> {
//             // playDrumNote(noteEvent.getNote(), noteEvent.getVelocity());
//         });
//         return drumEffectsSequencerPanel;
//     }

//     private MelodicSequencerPanel createMelodicSequencerPanel(int channel) {
//         MelodicSequencerPanel melodicSequencerPanel = new MelodicSequencerPanel(channel, noteEvent -> {
//             // This callback should only be used for UI updates if needed
//             // The actual note playing is handled inside the sequencer
//             logger.debug("Note event received from sequencer: note={}, velocity={}, duration={}",
//                     noteEvent.getNote(), noteEvent.getVelocity(), noteEvent.getDurationMs());
//         });

//         return melodicSequencerPanel;
//     }

//     public void playNote(int note, int velocity, int durationMs) {
//         // Delegate to InternalSynthManager
//         InternalSynthManager.getInstance().playNote(note, velocity, durationMs, activeMidiChannel);
//     }

//     public void playDrumNote(int note, int velocity) {
//         // Delegate to InternalSynthManager
//         InternalSynthManager.getInstance().playDrumNote(note, velocity);
//     }
// }
