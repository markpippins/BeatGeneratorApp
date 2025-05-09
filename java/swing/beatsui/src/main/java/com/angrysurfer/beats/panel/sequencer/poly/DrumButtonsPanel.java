package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.panel.sequencer.mono.MelodicSequencerPanel;
import com.angrysurfer.beats.widget.DrumButton;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.event.PlayerRefreshEvent;
import com.angrysurfer.core.event.PlayerSelectionEvent;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.service.DeviceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel containing drum pad buttons for drum sequencer
 */
public class DrumButtonsPanel extends JPanel {
    
    private static final Logger logger = LoggerFactory.getLogger(DrumButtonsPanel.class);

    private final List<DrumButton> drumButtons = new ArrayList<>();
    private final DrumSequencer sequencer;
    private Consumer<Integer> drumSelectedCallback;
    private int selectedPadIndex = -1;
    private boolean isHandlingSelection = false;

    /**
     * Create a drum pad button panel
     * 
     * @param sequencer The drum sequencer to control
     * @param drumSelectedCallback Callback for when a drum is selected
     */
    public DrumButtonsPanel(DrumSequencer sequencer, Consumer<Integer> drumSelectedCallback) {
        this.sequencer = sequencer;
        this.drumSelectedCallback = drumSelectedCallback;
        initialize();
    }
    
    /**
     * Initialize the panel
     */
    private void initialize() {
        // IMPORTANT: Use identical layout to match sequence column spacing exactly
        setLayout(new GridLayout(1, 16, 5, 0));
        
        // IMPORTANT: Match border exactly to sequence panel
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Create 16 drum buttons
        for (int i = 0; i < 16; i++) {
            // Create a container for each button that matches column width exactly
            JPanel buttonContainer = new JPanel();
            buttonContainer.setLayout(new GridBagLayout()); // For perfect centering
            
            DrumButton drumButton = new DrumButton();
            drumButton.setName("DrumButton-" + i);
            drumButton.setToolTipText("Pad " + (i + 1));
            drumButton.setText(Integer.toString(i + 1));
            // drumButton.setExclusive(true);
            
            final int index = i;
            drumButton.addActionListener(e -> selectDrumPad(index));
            
            // Add to container with perfect centering
            buttonContainer.add(drumButton);
            drumButtons.add(drumButton);
            
            // Add the container directly to this panel
            add(buttonContainer);
        }
        
        // Initialize drum pads with named labels
        initializeDrumPads();
    }
    
    /**
     * Select a drum pad and notify callback
     * 
     * @param padIndex The index of the pad to select
     */
    public void selectDrumPad(int padIndex) {
        // Only process if actually changing selection
        if (padIndex != selectedPadIndex) {
            // Clear previous selection
            if (selectedPadIndex >= 0 && selectedPadIndex < drumButtons.size()) {
                drumButtons.get(selectedPadIndex).setSelected(false);
                drumButtons.get(selectedPadIndex).repaint();
            }

            // Set new selection
            selectedPadIndex = padIndex;
            
            // Update drum button visual state
            if (padIndex >= 0 && padIndex < drumButtons.size()) {
                DrumButton button = drumButtons.get(padIndex);
                button.setSelected(true);
                button.repaint();
                
                // Notify callback of selection change
                if (drumSelectedCallback != null) {
                    drumSelectedCallback.accept(padIndex);
                }
            }
        }
    }

    /**
     * Visually select a pad without triggering callbacks
     */
    public void selectDrumPadNoCallback(int padIndex) {
        // Clear previous selection
        if (selectedPadIndex >= 0 && selectedPadIndex < drumButtons.size()) {
            drumButtons.get(selectedPadIndex).setSelected(false);
            drumButtons.get(selectedPadIndex).repaint();
        }

        // Set new selection
        selectedPadIndex = padIndex;
        
        // Update drum button visual state
        if (padIndex >= 0 && padIndex < drumButtons.size()) {
            DrumButton button = drumButtons.get(padIndex);
            button.setSelected(true);
            button.repaint();
        }
    }
    
    /**
     * Initialize all drum pads to ensure they have proper MIDI connections
     */
    public void initializeDrumPads() {
        // Make sure all drums are properly prepared
        sequencer.ensureDeviceConnections();
        
        // Names for each drum part - these will be used in tooltips
        String[] drumNames = {
                "Kick", "Snare", "Closed HH", "Open HH",
                "Tom 1", "Tom 2", "Tom 3", "Crash",
                "Ride", "Rim", "Clap", "Cow",
                "Clave", "Shaker", "Perc 1", "Perc 2"
        };

        // Apply numbered labels and tooltips to each pad with beat indicators
        for (int i = 0; i < drumButtons.size(); i++) {
            DrumButton button = drumButtons.get(i);

            // Set the pad number (1-based)
            // button.setPadNumber(i + 1);

            // Set main beat flag for pads 1, 5, 9, 13 (zero-indexed as 0, 4, 8, 12)
            // button.setMainBeat(i == 0 || i == 4 || i == 8 || i == 12);

            // Set detailed tooltip
            String drumName = (i < drumNames.length) ? drumNames[i] : "Drum " + (i + 1);
            button.setToolTipText(drumName + " - Click to edit effects for this drum");
        }
    }
    
    /**
     * Get the currently selected pad index
     */
    public int getSelectedPadIndex() {
        return selectedPadIndex;
    }
    
    /**
     * Get the list of drum buttons
     */
    public List<DrumButton> getDrumButtons() {
        return drumButtons;
    }

    public int getButtonCount() {
        return drumButtons.size();
    }

    /**
     * Adjust the vertical position of the drum buttons to align with the sequence columns
     * @param offset Vertical offset in pixels (positive moves down, negative moves up)
     */
    public void adjustVerticalPosition(int offset) {
        if (offset != 0) {
            // Get current border
            EmptyBorder currentBorder = (EmptyBorder)getBorder();
            Insets currentInsets = currentBorder.getBorderInsets();
            
            // Create new border with adjusted top inset
            setBorder(new EmptyBorder(
                currentInsets.top + offset,  // Adjust top inset
                currentInsets.left,
                currentInsets.bottom,
                currentInsets.right
            ));
            
            revalidate();
        }
    }

    private void handleDrumPadSelected(int padIndex) {
        // Don't process if already selected or we're in the middle of handling a selection
        if (padIndex == selectedPadIndex || isHandlingSelection) {
            return;
        }

        try {
            // Set flag to prevent recursive calls
            isHandlingSelection = true;

            selectedPadIndex = padIndex;
            sequencer.setSelectedPadIndex(padIndex);

            // Get the player for this pad index
            if (padIndex >= 0 && padIndex < sequencer.getPlayers().length) {
                Player player = sequencer.getPlayers()[padIndex];

                if (player != null) {
                    // Make sure device connection is active
                    if (player.getInstrument() != null) {
                        // Ensure device is connected and open
                        if (player.getInstrument().getDevice() == null || !player.getInstrument().getDevice().isOpen()) {
                            player.getInstrument().setDevice(DeviceManager.getMidiDevice(player.getInstrument().getDeviceName()));
                            
                            // Ensure device is open
                            if (player.getInstrument().getDevice() != null && !player.getInstrument().getDevice().isOpen()) {
                                try {
                                    player.getInstrument().getDevice().open();
                                } catch (Exception e) {
                                    logger.info("Error opening MIDI device: " + e.getMessage());
                                }
                            }
                        }
                    
                        // Apply instrument preset BEFORE playing the note
                        // Send a player-specific refresh event instead of using global active player
                        PlayerRefreshEvent event = new PlayerRefreshEvent(player);
                        CommandBus.getInstance().publish(
                            Commands.PLAYER_REFRESH_EVENT,
                            this,
                            event
                        );
                        
                        // Play a test note 
                        player.drumNoteOn(player.getRootNote());
                    } 
                    
                    // Create a selection event for UI purposes only
                    PlayerSelectionEvent selectionEvent = new PlayerSelectionEvent(player);
                    CommandBus.getInstance().publish(
                        Commands.PLAYER_SELECTION_EVENT,
                        this,
                        selectionEvent
                    );
                }
            }

            // Update UI and notification
            // ... rest of the method ...
        }
        finally {
            isHandlingSelection = false;
        }
    }
}