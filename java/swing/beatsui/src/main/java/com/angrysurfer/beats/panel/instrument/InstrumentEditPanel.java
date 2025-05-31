package com.angrysurfer.beats.panel.instrument;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.service.InstrumentManager;
import lombok.Getter;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class InstrumentEditPanel extends JPanel {
    private final InstrumentWrapper instrument;
    private final JTextField nameField;
    private final JComboBox<String> deviceCombo;
    private final JComboBox<String> channelCombo; // Add channel combobox
    private final JSpinner lowestNoteSpinner;
    private final JSpinner highestNoteSpinner;
    private final JCheckBox availableCheckBox;
    private final JCheckBox initializedCheckBox;
    private final List<MidiDevice.Info> deviceInfos;
    private final JTextArea ownerTextArea;

    public InstrumentEditPanel(InstrumentWrapper instrument) {
        super(new GridBagLayout());
        this.instrument = instrument;
        this.deviceInfos = new ArrayList<>();

        setMinimumSize(new Dimension(300, 350)); // Increase size for new field
        setMaximumSize(new Dimension(300, 350));
        setPreferredSize(new Dimension(300, 350));

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

        // Create channel selection
        channelCombo = new JComboBox<>();
        for (int i = 1; i <= 16; i++) {
            channelCombo.addItem(i + (i == 10 ? " (Drums)" : ""));
        }

        // Set current channel if available
        if (instrument.getChannel() != null) {
            channelCombo.setSelectedIndex(instrument.getChannel());
        } else {
            channelCombo.setSelectedIndex(0);
        }

        // Fix spinner initialization with default values
        lowestNoteSpinner = new JSpinner(new SpinnerNumberModel(
                instrument.getLowestNote() != null ? instrument.getLowestNote() : 0,
                0, 127, 1));

        highestNoteSpinner = new JSpinner(new SpinnerNumberModel(
                instrument.getHighestNote() != null ? instrument.getHighestNote() : 127,
                0, 127, 1));

        availableCheckBox = new JCheckBox("Available", false);
        initializedCheckBox = new JCheckBox("Initialized", instrument.isInitialized());

        // Add owner information text area (read-only)
        ownerTextArea = new JTextArea(3, 20);
        ownerTextArea.setEditable(false);
        ownerTextArea.setLineWrap(true);
        ownerTextArea.setWrapStyleWord(true);
        ownerTextArea.setBackground(new Color(245, 245, 245));
        ownerTextArea.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Populate owner information
        populateOwnerInfo();

        setupLayout();
    }

    private void populateDeviceCombo() {
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxReceivers() != 0) {
                    deviceInfos.add(info);
                    deviceCombo.addItem(info.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Populate the owner information text area
     */
    private void populateOwnerInfo() {
        if (instrument == null || instrument.getId() == null) {
            ownerTextArea.setText("New instrument - not owned by any players");
            return;
        }

        String ownerInfo = InstrumentManager.getInstance().determineInstrumentOwner(instrument);

        if (ownerInfo == null || ownerInfo.isEmpty() || "None".equals(ownerInfo)) {
            ownerTextArea.setText("This instrument is not currently assigned to any players");
        } else {
            ownerTextArea.setText("Currently used by: " + ownerInfo);

            // Set background to light yellow to highlight that it's in use
            ownerTextArea.setBackground(new Color(255, 255, 220));
        }
    }

    private void setupLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        addFormField("Name:", nameField, gbc, 0);
        addFormField("Device Name:", deviceCombo, gbc, 1);
        addFormField("Channel:", channelCombo, gbc, 2);
        addFormField("Lowest Note:", lowestNoteSpinner, gbc, 3);
        addFormField("Highest Note:", highestNoteSpinner, gbc, 4);
        addFormField("", availableCheckBox, gbc, 5);
        addFormField("", initializedCheckBox, gbc, 6);

        // Add ownership information field with a title
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        add(new JLabel("Ownership:"), gbc);

        // Add the text area in a scroll pane
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0; // Allow it to expand vertically
        JScrollPane scrollPane = new JScrollPane(ownerTextArea);
        scrollPane.setPreferredSize(new Dimension(280, 60));
        add(scrollPane, gbc);
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
        instrument.setName(nameField.getText().trim());
        instrument.setDeviceName((String) deviceCombo.getSelectedItem());
        int selectedIndex = channelCombo.getSelectedIndex();
        instrument.setChannel(selectedIndex);
        return instrument;
    }
}
