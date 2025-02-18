package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.ToggleSwitch;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.service.InstrumentManager;

public class PlayerEditPanel extends StatusProviderPanel {
    private static final Logger logger = Logger.getLogger(PlayerEditPanel.class.getName());
    private final ProxyStrike player;
    private final JComboBox<ProxyInstrument> instrumentCombo;
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

    public PlayerEditPanel(ProxyStrike player, StatusConsumer statusConsumer) {
        super(new BorderLayout(), statusConsumer);
        this.player = player;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Initialize components
        instrumentCombo = createInstrumentCombo();
        channelCombo = createChannelCombo();
        presetCombo = createPresetCombo();
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

        layoutComponents();
        setPreferredSize(new Dimension(475, 800));
    }

    // Helper methods for creating components
    private JComboBox<ProxyInstrument> createInstrumentCombo() {
        JComboBox<ProxyInstrument> combo = new JComboBox<>(
            InstrumentManager.getInstance().getAvailableInstruments().toArray(new ProxyInstrument[0])
        );
        return combo;
    }

    private JComboBox<Integer> createChannelCombo() {
        Integer[] channels = new Integer[16];
        for (int i = 0; i < 16; i++) channels[i] = i + 1;
        return new JComboBox<>(channels);
    }

    private JComboBox<Integer> createPresetCombo() {
        Integer[] presets = new Integer[128];
        for (int i = 0; i < 128; i++) presets[i] = i;
        return new JComboBox<>(presets);
    }

    private Dial createDial(String name, long value, int min, int max) {
        Dial dial = new Dial();
        dial.setValue((int) value);
        return dial;
    }

    private ToggleSwitch createToggleSwitch(String name, boolean value) {
        ToggleSwitch toggle = new ToggleSwitch();
        toggle.setSelected(value);
        toggle.setPreferredSize(new Dimension(60, 30));
        return toggle;
    }

    private void layoutComponents() {
        // ... layout code stays the same ...
    }

    public ProxyStrike getUpdatedPlayer() {
        // Update player with current UI values
        player.setInstrument((ProxyInstrument) instrumentCombo.getSelectedItem());
        player.setChannel((Integer) channelCombo.getSelectedItem());
        player.setPreset(((Integer) presetCombo.getSelectedItem()).longValue());
        // ... set other properties ...
        return player;
    }
}
