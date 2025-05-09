package com.angrysurfer.beats.panel.player;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.ChannelCombo;
import com.angrysurfer.core.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;

/**
 * Panel for generating random patterns in the drum player
 */
public class ChannelComboPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ChannelComboPanel.class);

    // UI components
    private ChannelCombo channelCombo;
    private JButton editButton;

    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        channelCombo = new ChannelCombo();
        
        // Standardize size to match other controls
        channelCombo.setPreferredSize(new Dimension(UIHelper.MEDIUM_CONTROL_WIDTH + 10, UIHelper.CONTROL_HEIGHT));
        channelCombo.setToolTipText("Player MIDI Channel");

        editButton = new JButton(Symbols.getSymbol(Symbols.GRID));
        editButton.setToolTipText("Edit...");
        
        // Match button size and margins to other panels
        editButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        editButton.setMargin(new Insets(2, 2, 2, 2));
        
        editButton.addActionListener(e -> {
            // Publish event to refresh UI
            //CommandBus.getInstance().publish(
            //        Commands.DRUM_GRID_REFRESH_REQUESTED,
            //        this,
            //        null);
        });

        // Add label for better UI consistency
        JLabel channelLabel = new JLabel("Ch:");
        
        // Add components to panel with compact spacing
        add(channelLabel);
        add(channelCombo);
        add(editButton);
    }

    /**
     * Create a new ChannelComboPanel with standardized layout
     */
    public ChannelComboPanel() {
        UIHelper.setWidgetPanelBorder(this, "Channel");
        
        // Use compact spacing to match other panels
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
        
        initializeComponents();
    }
}