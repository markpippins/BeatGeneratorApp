package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.mock.Strike;
import com.angrysurfer.beatsui.widget.Dial;
import com.angrysurfer.beatsui.widget.ToggleSwitch;

public class PlayerEditorPanel extends StatusProviderPanel {
    private final Strike player;
    private final JTextField nameField;
    private final JComboBox<Integer> channelCombo;
    private final JComboBox<Integer> presetCombo;
    private final Dial swingDial;
    private final Dial levelDial;
    private final Dial noteDial;
    private final Dial minVelocityDial;
    private final Dial maxVelocityDial;
    private final Dial probabilityDial;
    private final Dial randomDegreeDial;
    private final Dial ratchetCountDial;
    private final Dial ratchetIntervalDial;
    private final Dial panPositionDial;
    private final ToggleSwitch stickyPresetSwitch;
    private final ToggleSwitch useInternalBeatsSwitch;
    private final ToggleSwitch useInternalBarsSwitch;
    private final ToggleSwitch preserveOnPurgeSwitch;
    private final JSpinner sparseSpinner;

    // New dials
    private final Dial internalBeatsDial;
    private final Dial internalBarsDial;
    private final Dial subDivisionsDial;
    private final Dial beatFractionDial;
    private final Dial fadeInDial;
    private final Dial fadeOutDial;

    // New toggle
    private final ToggleSwitch accentSwitch;

    public PlayerEditorPanel(Strike player) {
        this(player, null);
    }

    public PlayerEditorPanel(Strike player, StatusConsumer statusConsumer) {
        super(new BorderLayout(), statusConsumer);
        this.player = player;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        nameField = new JTextField(player.getName(), 20);

        channelCombo = new JComboBox<>(createChannelOptions());
        channelCombo.setSelectedItem(player.getChannel());

        presetCombo = new JComboBox<>(createPresetOptions());
        presetCombo.setSelectedItem(player.getPreset().intValue());

        swingDial = createDial("Swing", player.getSwing(), 0, 100);
        levelDial = createDial("Level", player.getLevel(), 0, 127);
        noteDial = createDial("Note", player.getNote(), 0, 127);
        minVelocityDial = createDial("Min Velocity", player.getMinVelocity(), 0, 127);
        maxVelocityDial = createDial("Max Velocity", player.getMaxVelocity(), 0, 127);
        probabilityDial = createDial("Probability", player.getProbability(), 0, 100);
        randomDegreeDial = createDial("Random", player.getRandomDegree(), 0, 100);
        ratchetCountDial = createDial("Ratchet #", player.getRatchetCount(), 0, 6);
        ratchetIntervalDial = createDial("Ratchet Int", player.getRatchetInterval(), 1, 16);
        panPositionDial = createDial("Pan", player.getPanPosition(), 0, 127);

        stickyPresetSwitch = createToggleSwitch("Sticky Preset", player.getStickyPreset());
        useInternalBeatsSwitch = createToggleSwitch("Internal Beats", player.getUseInternalBeats());
        useInternalBarsSwitch = createToggleSwitch("Internal Bars", player.getUseInternalBars());
        preserveOnPurgeSwitch = createToggleSwitch("Preserve", player.getPreserveOnPurge());

        sparseSpinner = new JSpinner(new SpinnerNumberModel(player.getSparse(), 0.0, 1.0, 0.1));

        // Initialize new dials
        internalBeatsDial = createDial("Int Beats", player.getInternalBeats(), 1, 16);
        internalBarsDial = createDial("Int Bars", player.getInternalBars(), 1, 16);
        subDivisionsDial = createDial("Subdivisions", player.getSubDivisions(), 1, 16);
        beatFractionDial = createDial("Beat Fraction", player.getBeatFraction(), 1, 16);
        fadeInDial = createDial("Fade In", player.getFadeIn(), 0, 127);
        fadeOutDial = createDial("Fade Out", player.getFadeOut(), 0, 127);

        // Initialize new toggle
        accentSwitch = createToggleSwitch("Accent", player.getAccent());

        layoutComponents();

        setPreferredSize(new Dimension(800, 500));
    }

    private Integer[] createChannelOptions() {
        Integer[] options = new Integer[12];
        for (int i = 0; i < 12; i++) {
            options[i] = i + 1;
        }
        return options;
    }

    private Integer[] createPresetOptions() {
        Integer[] options = new Integer[127];
        for (int i = 0; i < 127; i++) {
            options[i] = i + 1;
        }
        return options;
    }

    private Dial createDial(String name, long value, int min, int max) {
        Dial dial = new Dial();
        dial.setValue((int) value);
        return dial;
    }

    private ToggleSwitch createToggleSwitch(String name, boolean value) {
        ToggleSwitch toggle = new ToggleSwitch();
        toggle.setSelected(value);
        return toggle;
    }

    private void layoutComponents() {
        // Top panel for name, channel, preset
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints topGbc = new GridBagConstraints();
        topGbc.insets = new Insets(5, 5, 5, 5);
        topGbc.fill = GridBagConstraints.HORIZONTAL;
        topGbc.weightx = 1.0;

        // Add name field (wider)
        addComponent("Name", nameField, 0, 0, 2, topGbc, topPanel);

        // Add channel and preset combos
        addComponent("Channel", channelCombo, 2, 0, 1, topGbc, topPanel);
        addComponent("Preset", presetCombo, 3, 0, 1, topGbc, topPanel);

        // Center panel for dials in rows
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints dialGbc = new GridBagConstraints();
        dialGbc.insets = new Insets(5, 5, 5, 5);
        dialGbc.fill = GridBagConstraints.NONE;
        dialGbc.anchor = GridBagConstraints.CENTER;

        // First row of dials
        addDialComponent("Swing", swingDial, 0, 0, centerPanel);
        addDialComponent("Level", levelDial, 1, 0, centerPanel);
        addDialComponent("Note", noteDial, 2, 0, centerPanel);
        addDialComponent("Pan", panPositionDial, 3, 0, centerPanel);
        addDialComponent("Min Vel", minVelocityDial, 4, 0, centerPanel);

        // Second row of dials
        addDialComponent("Max Vel", maxVelocityDial, 0, 1, centerPanel);
        addDialComponent("Probability", probabilityDial, 1, 1, centerPanel);
        addDialComponent("Random", randomDegreeDial, 2, 1, centerPanel);
        addDialComponent("Ratchet #", ratchetCountDial, 3, 1, centerPanel);
        addDialComponent("Ratchet Int", ratchetIntervalDial, 4, 1, centerPanel);

        // Third row of dials (new)
        addDialComponent("Int Beats", internalBeatsDial, 0, 2, centerPanel);
        addDialComponent("Int Bars", internalBarsDial, 1, 2, centerPanel);
        addDialComponent("Subdivisions", subDivisionsDial, 2, 2, centerPanel);
        addDialComponent("Beat Fraction", beatFractionDial, 3, 2, centerPanel);

        // Fourth row (fade controls)
        addDialComponent("Fade In", fadeInDial, 0, 3, centerPanel);
        addDialComponent("Fade Out", fadeOutDial, 1, 3, centerPanel);

        // Bottom panel for toggles and sparse spinner
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints toggleGbc = new GridBagConstraints();
        toggleGbc.insets = new Insets(5, 5, 5, 5);
        toggleGbc.fill = GridBagConstraints.NONE;
        toggleGbc.anchor = GridBagConstraints.CENTER;

        // Add toggles in a row
        addToggleComponent("Sticky", stickyPresetSwitch, 0, 0, bottomPanel);
        addToggleComponent("Int Beats", useInternalBeatsSwitch, 1, 0, bottomPanel);
        addToggleComponent("Int Bars", useInternalBarsSwitch, 2, 0, bottomPanel);
        addToggleComponent("Preserve", preserveOnPurgeSwitch, 3, 0, bottomPanel);
        addToggleComponent("Accent", accentSwitch, 4, 0, bottomPanel);

        // Add sparse spinner
        GridBagConstraints sparseGbc = (GridBagConstraints) toggleGbc.clone();
        sparseGbc.fill = GridBagConstraints.HORIZONTAL;
        addComponent("Sparse", sparseSpinner, 5, 0, 1, sparseGbc, bottomPanel);

        // Main layout assembly
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addComponent(String label, JComponent component, int x, int y, int width,
            GridBagConstraints gbc, JPanel targetPanel) {
        gbc = (GridBagConstraints) gbc.clone();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        targetPanel.add(panel, gbc);
    }

    private void addDialComponent(String label, Dial dial, int x, int y, JPanel targetPanel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(dial, BorderLayout.CENTER);
        targetPanel.add(panel, gbc);
    }

    private void addToggleComponent(String label, ToggleSwitch toggle, int x, int y, JPanel targetPanel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(toggle, BorderLayout.CENTER);
        targetPanel.add(panel, gbc);
    }

    public Strike getUpdatedPlayer() {
        // Since we're modifying the original player object directly,
        // we just need to preserve its ID and return it
        Long originalId = player.getId();
        
        // Update all the fields
        player.setName(nameField.getText());
        player.setChannel((Integer) channelCombo.getSelectedItem());
        player.setPreset(((Integer) presetCombo.getSelectedItem()).longValue());
        player.setSwing((long) swingDial.getValue());
        player.setLevel((long) levelDial.getValue());
        player.setNote((long) noteDial.getValue());
        player.setMinVelocity((long) minVelocityDial.getValue());
        player.setMaxVelocity((long) maxVelocityDial.getValue());
        player.setProbability((long) probabilityDial.getValue());
        player.setRandomDegree((long) randomDegreeDial.getValue());
        player.setRatchetCount((long) ratchetCountDial.getValue());
        player.setRatchetInterval((long) ratchetIntervalDial.getValue());
        player.setPanPosition((long) panPositionDial.getValue());

        // New values
        player.setInternalBeats(internalBeatsDial.getValue());
        player.setInternalBars(internalBarsDial.getValue());
        player.setSubDivisions((long) subDivisionsDial.getValue());
        player.setBeatFraction((long) beatFractionDial.getValue());
        player.setFadeIn((long) fadeInDial.getValue());
        player.setFadeOut((long) fadeOutDial.getValue());

        // Toggles
        player.setStickyPreset(stickyPresetSwitch.isSelected());
        player.setUseInternalBeats(useInternalBeatsSwitch.isSelected());
        player.setUseInternalBars(useInternalBarsSwitch.isSelected());
        player.setPreserveOnPurge(preserveOnPurgeSwitch.isSelected());
        player.setAccent(accentSwitch.isSelected());

        // Spinner
        player.setSparse((double) sparseSpinner.getValue());

        // Ensure ID is preserved
        player.setId(originalId);

        return player;
    }
}
