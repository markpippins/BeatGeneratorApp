// package com.angrysurfer.beats.panel;

// import java.awt.BorderLayout;
// import java.awt.Color;
// import java.awt.Dimension;
// import java.awt.FlowLayout;
// import java.awt.Insets;

// import javax.swing.BorderFactory;
// import javax.swing.JButton;
// import javax.swing.JLabel;
// import javax.swing.JPanel;
// import javax.swing.JSlider;
// import javax.swing.SwingConstants;
// import javax.swing.event.ChangeEvent;
// import javax.swing.event.ChangeListener;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import com.angrysurfer.core.api.Command;
// import com.angrysurfer.core.api.CommandBus;
// import com.angrysurfer.core.api.Commands;
// import com.angrysurfer.core.api.IBusListener;
// import com.angrysurfer.core.model.Session;
// import com.angrysurfer.core.sequencer.DrumSequencer;
// import com.angrysurfer.core.service.DrumSequencerManager;
// import com.angrysurfer.core.service.SessionManager;

// import lombok.Getter;
// import lombok.Setter;

// /**
//  * Navigation panel for drum sequencer with transport controls.
//  * This is a standalone component that can be used with any sequencer.
//  */
// @Getter
// @Setter
// public class DrumSequencerNavigationPanel extends JPanel implements IBusListener {

//     private static final Logger logger = LoggerFactory.getLogger(DrumSequencerNavigationPanel.class);
    
//     // UI Components
//     private JButton playButton;
//     private JButton stopButton;
//     private JButton resetButton;
//     private JSlider tempoSlider;
//     private JLabel tempoLabel;
//     private JButton firstButton;
//     private JButton nextButton;
//     private JButton lastButton;
    
//     // Sequencer reference
//     private DrumSequencer sequencer;
    
//     // Manager reference
//     private DrumSequencerManager manager;
//     private Long currentPatternId;
    
//     // UI State
//     private boolean isPlaying = false;
    
//     /**
//      * Create a new navigation panel
//      * 
//      * @param sequencer The sequencer to control
//      */
//     public DrumSequencerNavigationPanel(DrumSequencer sequencer) {
//         super(new BorderLayout(5, 5));
//         this.sequencer = sequencer;
//         this.manager = DrumSequencerManager.getInstance();
//         this.currentPatternId = null;
        
//         // Register with command bus
//         CommandBus.getInstance().register(this);
        
//         initialize();
//     }
    
//     /**
//      * Initialize the UI components
//      */
//     private void initialize() {
//         setBorder(BorderFactory.createTitledBorder("Transport"));
        
//         // Create controls panel for transport buttons
//         JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        
//         // Create transport buttons
//         playButton = createTransportButton("▶", "Play", 40, 25);
//         playButton.addActionListener(e -> startPlayback());
        
//         stopButton = createTransportButton("■", "Stop", 40, 25);
//         stopButton.addActionListener(e -> stopPlayback());
//         stopButton.setEnabled(false); // Disabled initially
        
//         resetButton = createTransportButton("⏮", "Reset", 40, 25);
//         resetButton.addActionListener(e -> resetSequence());
        
//         // Add pattern navigation buttons
//         firstButton = createTransportButton("⏮", "First Pattern", 40, 25);
//         firstButton.addActionListener(e -> goToFirstPattern());
        
//         nextButton = createTransportButton("⏭", "Next Pattern", 40, 25);
//         nextButton.addActionListener(e -> goToNextPattern());
        
//         lastButton = createTransportButton("⏭⏭", "Last Pattern", 40, 25);
//         lastButton.addActionListener(e -> goToLastPattern());
        
//         // Add buttons to controls panel
//         controlsPanel.add(playButton);
//         controlsPanel.add(stopButton);
//         controlsPanel.add(resetButton);
        
//         // Add a small separator
//         JLabel separator = new JLabel("|");
//         separator.setForeground(Color.GRAY);
//         controlsPanel.add(separator);
        
//         // Add the navigation buttons
//         controlsPanel.add(firstButton);
//         controlsPanel.add(nextButton);
//         controlsPanel.add(lastButton);
        
//         // Add tempo slider
//         JPanel tempoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
//         tempoPanel.add(new JLabel("Tempo:"));
        
//         // Get current tempo from session
//         Session session = SessionManager.getInstance().getActiveSession();
//         Float currentBPM = session.getTempoInBPM();
        
//         // Create tempo slider
//         tempoSlider = new JSlider(SwingConstants.HORIZONTAL, 60, 180, currentBPM.intValue());
//         tempoSlider.setPreferredSize(new Dimension(120, 25));
//         tempoSlider.setMajorTickSpacing(30);
//         tempoSlider.setPaintTicks(true);
        
//         // Add change listener to update tempo in real-time
//         tempoSlider.addChangeListener(new ChangeListener() {
//             @Override
//             public void stateChanged(ChangeEvent e) {
//                 int bpm = tempoSlider.getValue();
//                 updateTempo(bpm);
//             }
//         });
        
//         tempoPanel.add(tempoSlider);
        
//         // Add BPM label
//         tempoLabel = new JLabel(currentBPM + " BPM");
//         tempoPanel.add(tempoLabel);
        
//         // Add panels to main layout
//         add(controlsPanel, BorderLayout.WEST);
//         add(tempoPanel, BorderLayout.CENTER);
//     }
    
//     /**
//      * Create a transport button with consistent styling
//      */
//     private JButton createTransportButton(String text, String tooltip, int width, int height) {
//         JButton button = new JButton(text);
//         button.setToolTipText(tooltip);
//         button.setPreferredSize(new Dimension(width, height));
//         button.setMargin(new Insets(2, 2, 2, 2));
//         return button;
//     }
    
//     /**
//      * Start playback
//      */
//     private void startPlayback() {
//         if (isPlaying) return;
        
//         logger.info("Starting playback");
//         CommandBus.getInstance().publish(Commands.TRANSPORT_START, this, null);
        
//         // Update button states
//         playButton.setEnabled(false);
//         stopButton.setEnabled(true);
        
//         isPlaying = true;
//     }
    
//     /**
//      * Stop playback
//      */
//     private void stopPlayback() {
//         if (!isPlaying) return;
        
//         logger.info("Stopping playback");
//         CommandBus.getInstance().publish(Commands.TRANSPORT_STOP, this, null);
        
//         // Update button states
//         playButton.setEnabled(true);
//         stopButton.setEnabled(false);
        
//         isPlaying = false;
//     }
    
//     /**
//      * Reset sequence
//      */
//     private void resetSequence() {
//         logger.info("Resetting sequence");
        
//         // If playing, stop first
//         if (isPlaying) {
//             stopPlayback();
//         }
        
//         // Reset the sequencer
//         sequencer.reset();
        
//         // Notify other components
//         CommandBus.getInstance().publish(Commands.TRANSPORT_RESET, this, null);
//     }
    
//     /**
//      * Update tempo
//      */
//     private void updateTempo(int bpm) {
//         // Update the label
//         tempoLabel.setText(bpm + " BPM");
        
//         // Update the session
//         Session session = SessionManager.getInstance().getActiveSession();
//         session.setTempoInBPM(bpm);
        
//         // Publish tempo change
//         CommandBus.getInstance().publish(Commands.UPDATE_TEMPO, this, (float) bpm);
//     }
    
//     /**
//      * Handle command bus messages
//      */
//     @Override
//     public void onAction(Command action) {
//         if (action == null || action.getCommand() == null) {
//             return;
//         }

//         switch (action.getCommand()) {
//             case Commands.TRANSPORT_START -> {
//                 // Only update UI if we're not the sender
//                 if (action.getSender() != this) {
//                     playButton.setEnabled(false);
//                     stopButton.setEnabled(true);
//                     isPlaying = true;
//                 }
//             }
            
//             case Commands.TRANSPORT_STOP -> {
//                 // Only update UI if we're not the sender
//                 if (action.getSender() != this) {
//                     playButton.setEnabled(true);
//                     stopButton.setEnabled(false);
//                     isPlaying = false;
//                 }
//             }
            
//             case Commands.UPDATE_TEMPO -> {
//                 // Only update UI if we're not the sender
//                 if (action.getSender() != this && action.getData() instanceof Float) {
//                     float bpm = (Float) action.getData();
//                     tempoSlider.setValue((int) bpm);
//                     tempoLabel.setText((int) bpm + " BPM");
//                 }
//             }
//         }
//     }
    
//     /**
//      * Navigate to the first pattern
//      */
//     private void goToFirstPattern() {
//         logger.info("Going to first pattern");
        
//         // Stop playback if currently playing
//         if (isPlaying) {
//             stopPlayback();
//         }
        
//         // Navigate to first pattern using manager
//         currentPatternId = manager.loadFirstPattern(sequencer);
        
//         // Notify other components
//         CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_SELECTED, this, 0);
//     }
    
//     /**
//      * Navigate to the next pattern
//      */
//     private void goToNextPattern() {
//         logger.info("Going to next pattern");
        
//         // Navigate to next pattern using manager
//         if (currentPatternId != null) {
//             currentPatternId = manager.loadNextPattern(sequencer, currentPatternId);
//         } else {
//             currentPatternId = manager.loadFirstPattern(sequencer);
//         }
        
//         // Notify other components
//         CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_SELECTED, this, 
//                 currentPatternId != null ? currentPatternId.intValue() : 0);
//     }
    
//     /**
//      * Navigate to the last pattern
//      */
//     private void goToLastPattern() {
//         logger.info("Going to last pattern");
        
//         // Stop playback if currently playing
//         if (isPlaying) {
//             stopPlayback();
//         }
        
//         // Navigate to last pattern using manager
//         currentPatternId = manager.loadLastPattern(sequencer);
        
//         // Notify other components
//         CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_SELECTED, this, 
//                 currentPatternId != null ? currentPatternId.intValue() : 0);
//     }
// }