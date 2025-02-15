package com.angrysurfer.beatsui.ui.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import com.angrysurfer.beatsui.App;
import com.angrysurfer.beatsui.api.Command;
import com.angrysurfer.beatsui.api.CommandBus;
import com.angrysurfer.beatsui.api.CommandListener;
import com.angrysurfer.beatsui.api.Commands;
import com.angrysurfer.core.proxy.ProxyTicker;

public class ToolBar extends JToolBar {
    private final Map<String, JTextField> leftFields = new HashMap<>();
    private final Map<String, JComponent> rightFields = new HashMap<>(); // Changed to JComponent
    private final CommandBus actionBus = CommandBus.getInstance();
    private ProxyTicker currentTicker; // Add field to track current ticker

    public ToolBar() {
        super();
        setup();
        setupActionBusListener();
    }

    private void setupActionBusListener() {
        actionBus.register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand().equals(Commands.TICKER_SELECTED) ||
                        action.getCommand().equals(Commands.TICKER_UPDATED)) {
                    if (action.getData() instanceof ProxyTicker) {
                        updateTickerDisplay((ProxyTicker) action.getData());
                    }
                }
            }
        });
    }

    private JComboBox<Integer> createTickerCombo(String field, int min, int max, int current) {
        JComboBox<Integer> combo = new JComboBox<>();
        for (int i = min; i <= max; i++) {
            combo.addItem(i);
        }
        combo.setSelectedItem(current);
        combo.setMaximumSize(new Dimension(70, 25));

        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && currentTicker != null) {
                int value = (Integer) combo.getSelectedItem();
                updateTickerValue(field, value);
            }
        });

        return combo;
    }

    private void updateTickerValue(String field, int value) {
        if (currentTicker != null) {
            try {
                switch (field) {
                    case "Ticks" -> currentTicker.setTicksPerBeat(value);
                    case "BPM" -> currentTicker.setTempoInBPM((float) value); // Cast to float
                    case "B/Bar" -> currentTicker.setBeatsPerBar(value);
                    case "Bars" -> currentTicker.setBars(value);
                    case "Parts" -> currentTicker.setParts(value);
                }

                // Save to Redis
                App.getRedisService().saveTicker(currentTicker);

                // Notify other components
                Command action = new Command();
                action.setCommand(Commands.TICKER_UPDATED);
                action.setData(currentTicker);
                actionBus.publish(action);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void updateTickerDisplay(ProxyTicker ticker) {
        
        if (Objects.isNull(ticker) || Objects.isNull(ticker.getId()))
            return;

        this.currentTicker = ticker; // Store current ticker

        // Update left fields
        leftFields.get("Tick").setText(String.valueOf(ticker.getTick()));
        leftFields.get("Beat").setText(String.valueOf(ticker.getBeat()));
        leftFields.get("Bar").setText(String.valueOf(ticker.getBar()));
        leftFields.get("Part").setText(String.valueOf(ticker.getPart()));
        leftFields.get("Players").setText(String.valueOf(ticker.getPlayers().size()));
        leftFields.get("Ticks").setText(String.valueOf(ticker.getTickCount()));
        leftFields.get("Beats").setText(String.valueOf(ticker.getBeatCount()));
        leftFields.get("Bars").setText(String.valueOf(ticker.getBarCount()));

        // Update right fields
        ((JTextField) rightFields.get("Ticker")).setText(ticker.getId().toString());
        ((JComboBox<?>) rightFields.get("Ticks")).setSelectedItem(ticker.getTicksPerBeat());
        ((JComboBox<?>) rightFields.get("BPM")).setSelectedItem(ticker.getTempoInBPM().intValue());
        ((JComboBox<?>) rightFields.get("B/Bar")).setSelectedItem(ticker.getBeatsPerBar());
        ((JComboBox<?>) rightFields.get("Bars")).setSelectedItem(ticker.getBars());
        ((JComboBox<?>) rightFields.get("Parts")).setSelectedItem(ticker.getParts());
        ((JTextField) rightFields.get("Length")).setText(String.valueOf(ticker.getPartLength()));
        ((JTextField) rightFields.get("Offset")).setText(String.valueOf(ticker.getNoteOffset()));
    }

    private void setup() {
        setFloatable(false);
        setPreferredSize(new Dimension(getPreferredSize().width, 80)); // Set proper height for toolbar

        // Left status fields panel
        JPanel leftStatusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftStatusPanel.setPreferredSize(new Dimension(leftStatusPanel.getPreferredSize().width, 75));
        String[] leftLabels = { "Tick", "Beat", "Bar", "Part", "Players", "Ticks", "Beats", "Bars" };
        for (String label : leftLabels) {
            // Create panel for vertical stacking
            JPanel fieldPanel = new JPanel();
            fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));

            // Create and add label
            JLabel nameLabel = new JLabel(label);
            nameLabel.setForeground(Color.GRAY);
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            fieldPanel.add(nameLabel);

            // Create and add text field
            JTextField field = createTextField("0");
            leftFields.put(label, field);
            fieldPanel.add(field);

            leftStatusPanel.add(fieldPanel);
            leftStatusPanel.add(Box.createHorizontalStrut(5));
        }
        add(leftStatusPanel);

        // Transport controls
        JPanel transportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        transportPanel.setPreferredSize(new Dimension(transportPanel.getPreferredSize().width, 75));

        JButton rewindBtn = createToolbarButton("⏮", "Rewind");
        JButton pauseBtn = createToolbarButton("⏸", "Pause");
        JButton recordBtn = createToolbarButton("⏺", "Record");
        JButton stopBtn = createToolbarButton("⏹", "Stop");
        JButton playBtn = createToolbarButton("▶", "Play");
        JButton forwardBtn = createToolbarButton("⏭", "Forward");

        transportPanel.add(rewindBtn);
        transportPanel.add(pauseBtn);
        transportPanel.add(stopBtn);
        transportPanel.add(recordBtn);
        transportPanel.add(playBtn);
        transportPanel.add(forwardBtn);

        // Add padding above transport panel
        transportPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        add(transportPanel);

        // Right status fields panel
        JPanel rightStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightStatusPanel.setPreferredSize(new Dimension(rightStatusPanel.getPreferredSize().width, 75));
        Object[][] rightFieldsArray = {
                { "Ticker", createTextField("1") },
                { "Ticks", createTickerCombo("Ticks", 1, 384, 24) },
                { "BPM", createTickerCombo("BPM", 1, 960, 120) },
                { "B/Bar", createTickerCombo("B/Bar", 1, 16, 4) },
                { "Bars", createTickerCombo("Bars", 1, 128, 4) },
                { "Parts", createTickerCombo("Parts", 1, 64, 1) },
                { "Length", createTextField("0") },
                { "Offset", createTextField("0") }
        };

        for (Object[] field : rightFieldsArray) {
            JPanel fieldPanel = new JPanel();
            fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));

            JLabel nameLabel = new JLabel((String) field[0]);
            nameLabel.setForeground(Color.GRAY);
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            fieldPanel.add(nameLabel);

            JComponent component = (JComponent) field[1];
            component.setAlignmentX(Component.CENTER_ALIGNMENT);
            rightFields.put((String) field[0], component);
            fieldPanel.add(component);

            rightStatusPanel.add(fieldPanel);
            rightStatusPanel.add(Box.createHorizontalStrut(5));
        }

        add(rightStatusPanel);
    }

    private JPanel createTransportPanel() {
        JPanel transportPanel = new JPanel();
        transportPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JButton rewindBtn = createToolbarButton("⏮", "Rewind");
        JButton pauseBtn = createToolbarButton("⏸", "Pause");
        JButton recordBtn = createToolbarButton("⏺", "Record");
        JButton stopBtn = createToolbarButton("⏹", "Stop");
        JButton playBtn = createToolbarButton("▶", "Play");
        JButton forwardBtn = createToolbarButton("⏭", "Forward");

        transportPanel.add(rewindBtn);
        transportPanel.add(pauseBtn);
        transportPanel.add(stopBtn);
        transportPanel.add(recordBtn);
        transportPanel.add(playBtn);
        transportPanel.add(forwardBtn);

        // Create wrapper panel to push transport controls to bottom
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(transportPanel, BorderLayout.SOUTH);

        return wrapperPanel;
    }

    private JTextField createTextField(String initialValue) {
        JTextField field = new JTextField(initialValue);
        field.setColumns(4);
        field.setEditable(false);
        field.setEnabled(false);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setBackground(new Color(240, 240, 240));
        field.setMaximumSize(new Dimension(50, 25));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        return field;
    }

    private JButton createToolbarButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);

        // Increase button size
        int size = 32;
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));

        // Use a font that supports Unicode symbols
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        // Fallback fonts if Segoe UI Symbol isn't available
        if (!button.getFont().canDisplay('⏮')) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }

        // Optional: Add some padding around the text
        button.setMargin(new Insets(5, 5, 5, 5));

        return button;
    }
}
