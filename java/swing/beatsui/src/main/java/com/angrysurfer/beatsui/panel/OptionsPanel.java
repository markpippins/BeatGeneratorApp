package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.GridLayout;

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

import com.angrysurfer.beatsui.UIUtils;

public class OptionsPanel extends JPanel {

    public OptionsPanel() {
        super(new BorderLayout());
        setup();
    }

    private void setup() {
        setLayout(new BorderLayout());
        add(createOptionsPanel(), BorderLayout.CENTER);
    }

    private static class Device {
        String name;
        String description;
        String vendor;
        String version;
        int maxReceivers;
        int maxTransmitters;
        int receivers;
        int transmitters;
        boolean receiver;
        boolean transmitter;

        public Device(String name, String description, String vendor) {
            this.name = name;
            this.description = description;
            this.vendor = vendor;
            this.version = "1.0";
            this.maxReceivers = 1;
            this.maxTransmitters = 1;
            this.receivers = 0;
            this.transmitters = 0;
            this.receiver = true;
            this.transmitter = true;
        }

        public Object[] toRow() {
            return new Object[] {
                    name, description, vendor, version, maxReceivers, maxTransmitters,
                    receivers, transmitters, receiver, transmitter
            };
        }
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

        // Top section - Devices table
        JTable devicesTable = createDevicesTable();
        splitPane.setTopComponent(new JScrollPane(devicesTable));

        // Bottom section - Multiple tables
        JPanel bottomPanel = new JPanel(new GridLayout(1, 3, 5, 0));

        // Configs table
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

        // Add sample data
        Device[] sampleDevices = {
                new Device("Midi Port 1", "Internal MIDI Port", "System"),
                new Device("USB Device", "External USB MIDI Device", "Roland"),
                new Device("Virtual Port", "Virtual MIDI Connection", "System")
        };

        for (Device device : sampleDevices) {
            model.addRow(device.toRow());
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Center-align numeric columns (but not boolean columns)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 4; i < 8; i++) { // Only center Max Receivers through Transmitters
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Set preferred column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100); // Name
        table.getColumnModel().getColumn(1).setPreferredWidth(200); // Description
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Vendor

        return table;
    }

    private JTable createConfigsTable() {
        String[] columns = { "Port", "Device", "Available", "Channels", "Low", "High" };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 1 && column != 2; // Device and Available are read-only
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 2)
                    return Boolean.class;
                if (column >= 3)
                    return Integer.class;
                return String.class;
            }
        };

        // Add sample data
        Config[] sampleConfigs = {
                new Config("Port 1", "Midi Port 1", true),
                new Config("USB-1", "USB Device", true),
                new Config("Virtual", "Virtual Port", false)
        };

        for (Config config : sampleConfigs) {
            model.addRow(config.toRow());
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Set up port combo box
        String[] ports = { "Port 1", "USB-1", "Virtual", "Network" };
        JComboBox<String> portCombo = new JComboBox<>(ports);
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(portCombo));

        // Set up channels combo box (1-16)
        UIUtils.setupColumnEditor(table, "Channels", 1, 16);

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 3; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        return table;
    }

}
