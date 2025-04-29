package com.angrysurfer.beats.panel.sequencer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.beats.widget.MuteButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingUpdate;

/**
 * A panel with 16 tilt dials that respond to bar changes via TimingBus
 */
public class MuteSequencerPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(MuteSequencerPanel.class);
    private static final int BUTTON_COUNT = 16;
    private static final boolean DEFAULT_VALUE = false;

    private List<MuteButton> muteButtons = new ArrayList<>(BUTTON_COUNT);
    private List<JPanel> buttonContainers = new ArrayList<>(BUTTON_COUNT);
    private int currentBar = 0;
    private MelodicSequencer sequencer;

    public MuteSequencerPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Mute Sequence"));
        setPreferredSize(new Dimension(getPreferredSize().width, 60));

        JPanel buttonsPanel = new JPanel(new GridLayout(1, BUTTON_COUNT, 2, 0));
        JPanel labelsPanel = new JPanel(new GridLayout(1, BUTTON_COUNT, 2, 0));

        buttonsPanel.setBackground(getBackground());
        labelsPanel.setBackground(getBackground());

        // Create the dials and labels
        for (int i = 0; i < BUTTON_COUNT; i++) {
            // Create a dial
            MuteButton button = createMuteButton(i);
            muteButtons.add(button);

            // Create container for dial with spacing
            JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 1));
            // buttonContainer.setPreferredSize(new Dimension(80, 80));
            buttonContainer.add(button);

            buttonContainers.add(buttonContainer);
            buttonsPanel.add(buttonContainer);

            // Create label for dial
            JLabel label = new JLabel(String.valueOf(i + 1), JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(10f));
            labelsPanel.add(label);
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(buttonsPanel, BorderLayout.CENTER);
        mainPanel.add(labelsPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // Register with the TimingBus
        TimingBus.getInstance().register(this);
    }

    private MuteButton createMuteButton(int index) {
        // Create a scale degree dial with the appropriate range
        MuteButton button = new MuteButton();

        // Set size smaller than default
        button.setPreferredSize(new Dimension(24, 24));

        // Add change listener
        button.addChangeListener(e -> {
            boolean muted = button.isSelected();

            // // Store the mute value in the sequencer
            // if (sequencer != null) {

            // }

            // // If this is the current active bar, apply mute immediately
            if (index == (currentBar % BUTTON_COUNT)) {
                applyMutes();
            }
        });

        return button;
    }

    @Override
    public void onAction(Command action) {
        if (action.getData() instanceof TimingUpdate update) {
            // Check if it's a bar change
            if (update.bar() != currentBar) {
                currentBar = update.bar() - 1;
                highlightCurrentBar();
                applyMutes();
            }
        }
    }

    /**
     * Apply the tilt value for the current bar to the sequencer
     */
    private void applyMutes() {
        // Get current bar within the 16-bar pattern
        int barIndex = currentBar % BUTTON_COUNT;

        // Get the mute value for the current bar
        boolean muted = getMuteValue(barIndex);

        // Apply the mute to the sequencer
        if (sequencer != null) {

        }
    }

    private void highlightCurrentBar() {
        // Map the bar to a step (potentially with wrap-around)
        int dialToHighlight = currentBar % BUTTON_COUNT;

        // Update the appearance of all dial containers
        for (int i = 0; i < buttonContainers.size(); i++) {
            JPanel container = buttonContainers.get(i);

            if (i == dialToHighlight) {
                // Highlight the current step
                container.setBorder(BorderFactory.createLineBorder(UIUtils.coolBlue, 1));
            } else {
                // Reset highlighting
                container.setBorder(null);
            }

            container.repaint();
        }
    }

    /**
     * Get the tilt value for a specific step
     */
    public boolean getMuteValue(int step) {
        if (step >= 0 && step < muteButtons.size()) {
            return muteButtons.get(step).isSelected();
        }

        return DEFAULT_VALUE;
    }

    /**
     * Set the tilt value for a specific step
     */
    public void setTiltValue(int step, int value) {
        // if (step >= 0 && step < muteButtons.size()) {
        // muteButtons.get(step).setValue(Math.max(MIN_VALUE, Math.min(MAX_VALUE,
        // value)), false);
        // }
    }

    /**
     * Synchronize the dial positions with the sequencer tilt values
     */
    public void syncWithSequencer() {
        if (sequencer == null) {
            logger.warn("Cannot sync MuteSequencerPanel - sequencer is null");
            return;
        }

        logger.debug("Syncing tilt panel with sequencer values");

        // Get values directly from sequencer
        // List<Integer> tiltValues = sequencer.getHarmonicTiltValues();
        // if (tiltValues != null && !tiltValues.isEmpty()) {
        //     logger.info("Received {} tilt values from sequencer", tiltValues.size());

        //     // Loop through and update dial values
        //     for (int i = 0; i < Math.min(muteButtons.size(), tiltValues.size()); i++) {
        //         int tiltValue = tiltValues.get(i);
        //         logger.debug("Setting dial {} to value {}", i, tiltValue);

        //         // Ensure the value is in range
        //         int safeValue = Math.max(MIN_VALUE, Math.min(MAX_VALUE, tiltValue));
        //         if (safeValue != tiltValue) {
        //             logger.warn("Tilt value {} out of range, clamping to {}", tiltValue, safeValue);
        //             tiltValue = safeValue;
        //         }

        //         // Set the value without triggering change listeners
        //         // muteButtons.get(i).setValue(tiltValue, false);
        //     }
        // } else {
        //     logger.warn("No harmonic tilt values available from sequencer");
        // }

        // Highlight the current bar
        highlightCurrentBar();

        // Force repaint to ensure visual updates
        // for (Dial dial : muteButtons) {
        // dial.repaint();
        // }

        // Force complete panel repaint
        revalidate();
        repaint();
    }
}