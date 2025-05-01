package com.angrysurfer.beats.panel.instrument;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.PlayerManager;

/**
 * Panel for creating a new instrument and assigning it to a player
 */
public class PlayerInstrumentPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(PlayerInstrumentPanel.class);
    private final CommandBus commandBus = CommandBus.getInstance();
    
    private JTextField nameField;
    private JComboBox<String> deviceCombo;
    private JSpinner channelSpinner;
    private JSpinner lowestNoteSpinner;
    private JSpinner highestNoteSpinner;
    private JCheckBox internalCheckbox;
    private JButton okButton;
    private JButton cancelButton;
    
    private final Player player;
    private InstrumentWrapper newInstrument;
    
    /**
     * Create a new panel for creating and assigning an instrument to a player
     */
    public PlayerInstrumentPanel(Player player) {
        super(new BorderLayout());
        this.player = player;
        
        // Create a new instrument (defaults will be set in initComponents)
        this.newInstrument = new InstrumentWrapper();
        
        initComponents();
        layoutComponents();
    }
    
    /**
     * Initialize the UI components
     */
    private void initComponents() {
        // Name field
        nameField = new JTextField(20);
        
        // Device selection
        deviceCombo = new JComboBox<>();
        for (String device : DeviceManager.getInstance().getAvailableOutputDeviceNames()) {
            deviceCombo.addItem(device);
        }
        
        // Channel spinner (0-15)
        channelSpinner = new JSpinner(new SpinnerNumberModel(
            player != null ? player.getChannel() : 0, 0, 15, 1));
        
        // Note range spinners
        lowestNoteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 127, 1));
        highestNoteSpinner = new JSpinner(new SpinnerNumberModel(127, 0, 127, 1));
        
        // Internal synth checkbox
        internalCheckbox = new JCheckBox("Internal Synth");
        
        // Set default values from player if available
        if (player != null) {
            // Suggested name based on player
            nameField.setText(player.getName() + " Instrument");
            
            // Set channel from player
            channelSpinner.setValue(player.getChannel());
            
            // Select internal synth by default for simplicity
            internalCheckbox.setSelected(true);
            
            // Update UI based on internal selection
            updateUIForInternalSelection();
        }
        
        // Buttons
        okButton = new JButton("Create & Assign");
        cancelButton = new JButton("Cancel");
        
        // Action listeners
        internalCheckbox.addActionListener(this::handleInternalChanged);
        okButton.addActionListener(this::handleOkButton);
        cancelButton.addActionListener(e -> handleCancelButton());
    }
    
    /**
     * Layout the components with GridBagLayout
     */
    private void layoutComponents() {
        // Main content panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Name field
        contentPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(nameField, gbc);
        
        // Device selection
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(new JLabel("Device:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(deviceCombo, gbc);
        
        // Channel spinner
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(new JLabel("Channel:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(channelSpinner, gbc);
        
        // Note range
        gbc.gridx = 0;
        gbc.gridy++;
        contentPanel.add(new JLabel("Lowest Note:"), gbc);
        gbc.gridx = 1;
        contentPanel.add(lowestNoteSpinner, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        contentPanel.add(new JLabel("Highest Note:"), gbc);
        gbc.gridx = 1;
        contentPanel.add(highestNoteSpinner, gbc);
        
        // Internal synth checkbox
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(internalCheckbox, gbc);
        
        // Player info
        if (player != null) {
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            JLabel playerLabel = new JLabel(
                "Will be assigned to player: " + player.getName() + 
                " (Channel: " + player.getChannel() + ")");
            playerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            contentPanel.add(playerLabel, gbc);
        }
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        // Add panels to main layout
        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Handle changes to the internal synth checkbox
     */
    private void handleInternalChanged(ActionEvent e) {
        updateUIForInternalSelection();
    }
    
    /**
     * Update UI based on internal synth selection
     */
    private void updateUIForInternalSelection() {
        boolean isInternal = internalCheckbox.isSelected();
        
        // For internal synth, disable device selection
        deviceCombo.setEnabled(!isInternal);
        
        // For internal synth, note range is fixed
        lowestNoteSpinner.setEnabled(!isInternal);
        highestNoteSpinner.setEnabled(!isInternal);
    }
    
    /**
     * Handle OK button click - create and assign the instrument
     */
    private void handleOkButton(ActionEvent e) {
        // Validate input
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a name for the instrument", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // Create the instrument
            newInstrument.setName(nameField.getText().trim());
            
            // Set properties based on UI
            if (internalCheckbox.isSelected()) {
                newInstrument.setInternal(true);
                newInstrument.setDeviceName("Java Sound Synthesizer");
            } else {
                newInstrument.setInternal(false);
                newInstrument.setDeviceName((String) deviceCombo.getSelectedItem());
            }
            
            // Set channel, note range
            newInstrument.setChannel((Integer) channelSpinner.getValue());
            newInstrument.setLowestNote((Integer) lowestNoteSpinner.getValue());
            newInstrument.setHighestNote((Integer) highestNoteSpinner.getValue());
            
            // Initialize empty collections
            if (newInstrument.getControlCodes() == null) {
                newInstrument.setControlCodes(new ArrayList<>());
            }
            
            // Set default values
            newInstrument.setAvailable(true);
            newInstrument.setInitialized(true);
            
            // Save the instrument
            RedisService.getInstance().saveInstrument(newInstrument);
            
            // Register with InstrumentManager
            InstrumentManager.getInstance().updateInstrument(newInstrument);
            
            // If we have a player, assign the instrument to it
            if (player != null) {
                // Update player's instrument
                player.setInstrument(newInstrument);
                player.setInstrumentId(newInstrument.getId());
                
                // Save player changes
                PlayerManager.getInstance().savePlayerProperties(player);
                
                // Apply instrument settings
                PlayerManager.getInstance().applyPlayerInstrument(player);
                
                // Notify about player update
                commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                
                // Notify about instrument assignment
                commandBus.publish(Commands.PLAYER_INSTRUMENT_CHANGED, this,
                    new Object[] { player.getId(), newInstrument.getId() });
                
                // Log success
                commandBus.publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("PlayerInstrumentPanel", "Success", 
                        "Created and assigned instrument: " + newInstrument.getName())
                );
            }
            
            // Close dialog
            SwingUtilities.getWindowAncestor(this).dispose();
            
        } catch (Exception ex) {
            logger.error("Error creating instrument: {}", ex.getMessage(), ex);
            
            JOptionPane.showMessageDialog(this,
                "Error creating instrument: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            
            commandBus.publish(
                Commands.STATUS_UPDATE,
                this,
                new StatusUpdate("PlayerInstrumentPanel", "Error", 
                    "Failed to create instrument: " + ex.getMessage())
            );
        }
    }
    
    /**
     * Handle cancel button - close dialog
     */
    private void handleCancelButton() {
        SwingUtilities.getWindowAncestor(this).dispose();
    }
}