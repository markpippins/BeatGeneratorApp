package com.angrysurfer.beats.panel;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.angrysurfer.core.model.InstrumentWrapper;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
class InstrumentEditPanel extends JPanel {
    private final InstrumentWrapper instrument;
    private final JTextField nameField;
    private final JComboBox<String> deviceCombo; // Changed from JTextField
    private final JSpinner lowestNoteSpinner;
    private final JSpinner highestNoteSpinner;
    private final JCheckBox availableCheckBox;
    private final JCheckBox initializedCheckBox;
    private final List<MidiDevice.Info> deviceInfos; // Store device info objects

    public InstrumentEditPanel(InstrumentWrapper instrument) {
        super(new GridBagLayout());
        this.instrument = instrument;
        this.deviceInfos = new ArrayList<>();
        
        setMinimumSize(new Dimension(200, 200));
        setMaximumSize(new Dimension(200, 200));
        setPreferredSize(new Dimension(200, 200));

        nameField = new JTextField(instrument.getName(), 20);

        // Setup device combo box
        deviceCombo = new JComboBox<>();
        populateDeviceCombo();

        // Update device selection based on instrument
        if (instrument != null && instrument.getDeviceName() != null) {
            for (int i = 0; i < deviceCombo.getItemCount(); i++) {
                if (deviceCombo.getItemAt(i).equals(instrument.getDeviceName())) {
                    deviceCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Fix spinner initialization with default values
        lowestNoteSpinner = new JSpinner(new SpinnerNumberModel(
                instrument.getLowestNote() != null ? instrument.getLowestNote() : 0,
                0, 127, 1));

        highestNoteSpinner = new JSpinner(new SpinnerNumberModel(
                instrument.getHighestNote() != null ? instrument.getHighestNote() : 127,
                0, 127, 1));

        availableCheckBox = new JCheckBox("Available", false); // instrument.isAvailable());
        initializedCheckBox = new JCheckBox("Initialized", instrument.isInitialized());

        setupLayout();
    }

    private void populateDeviceCombo() {
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                // Only add devices that can receive MIDI (have receivers)
                if (device.getMaxReceivers() != 0) {
                    deviceInfos.add(info);
                    deviceCombo.addItem(info.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        addFormField("Name:", nameField, gbc, 0);
        addFormField("Device Name:", deviceCombo, gbc, 1);
        addFormField("Lowest Note:", lowestNoteSpinner, gbc, 2);
        addFormField("Highest Note:", highestNoteSpinner, gbc, 3);
        addFormField("", availableCheckBox, gbc, 4);
        addFormField("", initializedCheckBox, gbc, 5);
    }

    private void addFormField(String label, JComponent field, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        if (!label.isEmpty()) {
            add(new JLabel(label), gbc);
        }

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(field, gbc);
    }

    public InstrumentWrapper getUpdatedInstrument() {
        instrument.setName(nameField.getText());
        // Get the selected device name from combo
        int selectedIndex = deviceCombo.getSelectedIndex();
        if (selectedIndex >= 0) {
            instrument.setDeviceName(deviceInfos.get(selectedIndex).getName());
        }
        instrument.setLowestNote((Integer) lowestNoteSpinner.getValue());
        instrument.setHighestNote((Integer) highestNoteSpinner.getValue());
        instrument.setAvailable(availableCheckBox.isSelected());
        instrument.setInitialized(initializedCheckBox.isSelected());
        return instrument;
    }
}
