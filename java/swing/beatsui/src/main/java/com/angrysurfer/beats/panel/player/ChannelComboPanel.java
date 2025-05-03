package com.angrysurfer.beats.panel.player;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.widget.ChannelCombo;
import com.angrysurfer.core.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
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
     * Create a new DrumSequenceGeneratorPanel
     * 
     */
    public ChannelComboPanel() {
        UIUtils.setWidgetPanelBorder(this, "Channel");
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
        initializeComponents();
    }

    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        channelCombo = new ChannelCombo();
        //channelCombo.setPreferredSize(new Dimension(UIUtils.MEDIUM_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
        channelCombo.setToolTipText("Player MIDI Channel");

        editButton = new JButton(Symbols.getSymbol(Symbols.GRID));
        editButton.setToolTipText("Edit..");
        editButton.setPreferredSize(new Dimension(UIUtils.SMALL_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
        editButton.setMargin(new Insets(2, 2, 2, 2));
        editButton.addActionListener(e -> {

            // Publish event to refresh UI
            //CommandBus.getInstance().publish(
            //        Commands.DRUM_GRID_REFRESH_REQUESTED,
            //        this,
            //        null);
        });

        // Add components to panel
        add(channelCombo);
        add(editButton);
    }
}