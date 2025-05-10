package com.angrysurfer.beats.panel.player;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.panel.PlayerAwarePanel;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.ChannelCombo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel for generating random patterns in the drum player
 */
public class ChannelComboPanel extends PlayerAwarePanel {
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
        channelCombo.setPreferredSize(new Dimension(UIHelper.MEDIUM_CONTROL_WIDTH * 2, UIHelper.CONTROL_HEIGHT));
        channelCombo.setToolTipText("Player MIDI Channel");

        editButton = new JButton(Symbols.get(Symbols.GRID));
        editButton.setToolTipText("Edit...");
        
        // Match button size and margins to other panels
        editButton.setPreferredSize(new Dimension(24, 24));
        editButton.setMargin(new Insets(2, 2, 2, 2));
        
        editButton.addActionListener(e -> {
            // Publish event to refresh UI
            //CommandBus.getInstance().publish(
            //        Commands.DRUM_GRID_REFRESH_REQUESTED,
            //        this,
            //        null);
        });

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

    @Override
    public void handlePlayerActivated() {
        channelCombo.setCurrentPlayer(getPlayer());
    }

    @Override
    public void handlePlayerUpdated() {
        channelCombo.setCurrentPlayer(getPlayer());
    }
}