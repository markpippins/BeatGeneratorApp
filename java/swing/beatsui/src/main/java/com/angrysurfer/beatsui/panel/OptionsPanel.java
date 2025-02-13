package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beatsui.App;
import com.angrysurfer.beatsui.Utils;
import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.mock.Instrument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionsPanel extends StatusProviderPanel {

    public OptionsPanel() {
        this(null);
    }

    public OptionsPanel(StatusConsumer statusConsumer) {
        super(new BorderLayout(), statusConsumer);
        setup();
    }

    private void setup() {
        setLayout(new BorderLayout());
        add(createOptionsPanel(), BorderLayout.CENTER);
    }

    private static class Config {
        String port;
        String device;
        boolean available;
        int channels;
        int low;
        int high;

        public Config(String port, String device, boolean available) {
            this.port = port;
            this.device = device;
            this.available = available;
            this.channels = 16;
            this.low = 0;
            this.high = 127;
        }

        public Object[] toRow() {
            return new Object[] { port, device, available, channels, low, high };
        }
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create vertical split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5); // Equal split

        // Top section - MIDI Devices table
        JTable devicesTable = createDevicesTable();
        splitPane.setTopComponent(new JScrollPane(devicesTable));

        // Bottom section - Multiple tables
        JPanel bottomPanel = new JPanel(new GridLayout(1, 3, 5, 0));

        // Instrument configurations table (moved from top)
        JTable configsTable = createConfigsTable();
        bottomPanel.add(new JScrollPane(configsTable));

        // Placeholder panels for future tables
        bottomPanel.add(new JPanel());
        bottomPanel.add(new JPanel());

        splitPane.setBottomComponent(bottomPanel);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JTable createDevicesTable() {
        String[] columns = {
            "Name", "Description", "Vendor", "Version", "Max Receivers", 
            "Max Transmitters", "Receivers", "Transmitters", "Receiver", "Transmitter"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells read-only
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 8 || column == 9)
                    return Boolean.class; // Receiver and Transmitter columns
                if (column >= 4 && column <= 7)
                    return Integer.class;
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
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 4; i < 8; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(150); // Name
        table.getColumnModel().getColumn(1).setPreferredWidth(200); // Description
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Vendor

        return table;
    }

    private JTable createConfigsTable() {
        String[] columns = { 
            "Name", "Device Name", "Available", "Lowest Note", "Highest Note", "Initialized" 
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells read-only for now
            }

            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 2: // Available
                    case 5: // Initialized
                        return Boolean.class;
                    case 3: // Lowest Note
                    case 4: // Highest Note
                        return Integer.class;
                    default:
                        return String.class;
                }
            }
        };

        // Load data from Redis
        List<Instrument> instruments = App.getRedisService().findAllInstruments();
        for (Instrument instrument : instruments) {
            model.addRow(new Object[] {
                instrument.getName(),
                instrument.getDeviceName(),
                false, // instrument.isAvailable(),
                instrument.getLowestNote(),
                instrument.getHighestNote(),
                instrument.isInitialized()
            });
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 3; i <= 4; i++) { // Center-align note columns
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100); // Name
        table.getColumnModel().getColumn(1).setPreferredWidth(200); // Device Name

        return table;
    }

}
