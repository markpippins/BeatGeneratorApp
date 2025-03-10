package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.core.api.StatusConsumer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class SystemsPanel extends StatusProviderPanel {
    private final JTable devicesTable;

    public SystemsPanel(StatusConsumer statusConsumer) {
        super(new BorderLayout(), statusConsumer);
        this.devicesTable = createDevicesTable();
        setupLayout();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        add(new JScrollPane(devicesTable), BorderLayout.CENTER);
    }

    private JTable createDevicesTable() {
        String[] columns = {
            "Name", "Description", "Vendor", "Version", "Max Receivers", 
            "Max Transmitters", "Receivers", "Transmitters", "Receiver", "Transmitter"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 8 || column == 9) return Boolean.class;
                if (column >= 4 && column <= 7) return Integer.class;
                return String.class;
            }
        };

        // Load MIDI devices
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                model.addRow(new Object[] {
                    info.getName(),
                    info.getDescription(),
                    info.getVendor(),
                    info.getVersion(),
                    device.getMaxReceivers(),
                    device.getMaxTransmitters(),
                    device.getReceivers().size(),
                    device.getTransmitters().size(),
                    device.getMaxReceivers() != 0,
                    device.getMaxTransmitters() != 0
                });
            }
        } catch (MidiUnavailableException e) {
            setStatus("Error loading MIDI devices: " + e.getMessage());
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 4; i < 8; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        
        // Set fixed widths for numeric columns
        for (int i = 4; i <= 7; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(60);
            table.getColumnModel().getColumn(i).setMaxWidth(60);
        }

        return table;
    }
}
