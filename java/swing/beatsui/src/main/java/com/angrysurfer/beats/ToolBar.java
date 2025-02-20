package com.angrysurfer.beats;

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
import javax.swing.SwingUtilities;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.proxy.ProxyTicker;
import com.angrysurfer.core.service.TickerManager;

public class ToolBar extends JToolBar {
    private final Map<String, JTextField> leftFields = new HashMap<>();
    private final Map<String, JComponent> rightFields = new HashMap<>(); // Changed to JComponent
    private final CommandBus actionBus = CommandBus.getInstance();
    private ProxyTicker currentTicker; // Add field to track current ticker

    JPanel transportPanel;

    public ToolBar() {
        super();
        setFloatable(false);
        setup(); // Changed from setupButtons() to setup()

        // First register the listener to handle updates
        actionBus.register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                if (Objects.nonNull(action.getCommand())
                        && Objects.nonNull(action.getData())
                        && action.getData() instanceof ProxyTicker) {

                    switch (action.getCommand()) {
                        case Commands.TICKER_SELECTED, Commands.TICKER_UPDATED -> {
                            ProxyTicker ticker = (ProxyTicker) action.getData();
                            updateTickerDisplay(ticker);
                            updateToolbarState(ticker);
                        }
                    }
                }
            }
        });

        // Then request the initial ticker state after a short delay
        SwingUtilities.invokeLater(() -> {
            // First ensure TickerManager has an active ticker
            ProxyTicker currentTicker = TickerManager.getInstance().getActiveTicker();
            if (currentTicker != null) {
                actionBus.publish(Commands.TICKER_SELECTED, this, currentTicker);
            } else {
                actionBus.publish(Commands.TICKER_REQUEST, this);
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
                actionBus.publish(Commands.TICKER_UPDATED, this, currentTicker);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void updateTickerDisplay(ProxyTicker ticker) {
        if (Objects.isNull(ticker) || Objects.isNull(ticker.getId())) {
            System.out.println("ToolBar: Received null ticker or ticker ID");
            return;
        }

        System.out.println("ToolBar: Updating display for ticker " + ticker.getId() +
                " with " + ticker.getPlayers().size() + " players");

        this.currentTicker = ticker;

        // Update fields with synchronized block to prevent concurrent modification
        synchronized (this) {
            try {
                // Update left fields
                for (Map.Entry<String, JTextField> entry : leftFields.entrySet()) {
                    String value = switch (entry.getKey()) {
                        case "Tick" -> String.valueOf(ticker.getTick());
                        case "Beat" -> String.valueOf(ticker.getBeat());
                        case "Bar" -> String.valueOf(ticker.getBar());
                        case "Part" -> String.valueOf(ticker.getPart());
                        case "Players" -> String.valueOf(ticker.getPlayers().size());
                        case "Ticks" -> String.valueOf(ticker.getTickCount());
                        case "Beats" -> String.valueOf(ticker.getBeatCount());
                        case "Bars" -> String.valueOf(ticker.getBarCount());
                        default -> "0";
                    };
                    entry.getValue().setText(value);
                }

                // Update right fields
                ((JTextField) rightFields.get("Ticker")).setText(ticker.getId().toString());
                ((JComboBox<?>) rightFields.get("Ticks")).setSelectedItem(ticker.getTicksPerBeat());
                ((JComboBox<?>) rightFields.get("BPM")).setSelectedItem(ticker.getTempoInBPM().intValue());
                ((JComboBox<?>) rightFields.get("B/Bar")).setSelectedItem(ticker.getBeatsPerBar());
                ((JComboBox<?>) rightFields.get("Bars")).setSelectedItem(ticker.getBars());
                ((JComboBox<?>) rightFields.get("Parts")).setSelectedItem(ticker.getParts());
                ((JTextField) rightFields.get("Length")).setText(String.valueOf(ticker.getPartLength()));
                ((JTextField) rightFields.get("Offset")).setText(String.valueOf(ticker.getNoteOffset()));

            } catch (Exception e) {
                System.err.println("ToolBar: Error updating display: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // private JPanel createStatusPanel() {
    // // Left status fields panel
    // JPanel leftStatusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    // // leftStatusPanel.setPreferredSize(new
    // Dimension(leftStatusPanel.getPreferredSize().width, 75));
    // String[] leftLabels = { "Tick", "Beat", "Bar", "Part", "Players", "Ticks",
    // "Beats", "Bars" };
    // for (String label : leftLabels) {
    // // Create panel for vertical stacking
    // JPanel fieldPanel = new JPanel();
    // fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));

    // // Create and add label
    // JLabel nameLabel = new JLabel(label);
    // nameLabel.setForeground(Color.GRAY);
    // nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    // fieldPanel.add(nameLabel);

    // // Create and add text field
    // JTextField field = createTextField("0");
    // leftFields.put(label, field);
    // fieldPanel.add(field);

    // leftStatusPanel.add(fieldPanel);
    // leftStatusPanel.add(Box.createHorizontalStrut(5));
    // }
    // return leftStatusPanel;
    // }

    private JPanel createTopLeftStatusPanel() {
        // Left status fields panel
        JPanel leftStatusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        // leftStatusPanel.setPreferredSize(new
        // Dimension(leftStatusPanel.getPreferredSize().width, 75));
        String[] leftLabels = { "Tick", "Beat", "Bar", "Part" };
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
        return leftStatusPanel;
    }

    private JPanel createBottomLeftStatusPanel() {
        // Left status fields panel
        JPanel leftStatusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        // leftStatusPanel.setPreferredSize(new
        // Dimension(leftStatusPanel.getPreferredSize().width, 75));
        String[] leftLabels = { "Players", "Ticks", "Beats", "Bars" };
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
        return leftStatusPanel;
    }

    private JPanel createStatusPanel() {
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

        return rightStatusPanel;
    }

    private JPanel createTopRightStatusPanel() {
        // Right status fields panel
        JPanel rightStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        // rightStatusPanel.setPreferredSize(new
        // Dimension(rightStatusPanel.getPreferredSize().width, 75));
        setBorder(BorderFactory.createEmptyBorder());
        Object[][] rightFieldsArray = {
                // { "Ticker", createTextField("1") },
                { "Ticks", createTickerCombo("Ticks", 1, 384, 24) },
                { "BPM", createTickerCombo("BPM", 1, 960, 120) },
                { "B/Bar", createTickerCombo("B/Bar", 1, 16, 4) },
                { "Bars", createTickerCombo("Bars", 1, 128, 4) },
                { "Parts", createTickerCombo("Parts", 1, 64, 1) },
                // { "Length", createTextField("0") },
                // { "Offset", createTextField("0") }
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

        return rightStatusPanel;
    }

    private JPanel createBottomRightStatusPanel() {
        // Right status fields panel
        JPanel rightStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        // rightStatusPanel.setPreferredSize(new
        // Dimension(rightStatusPanel.getPreferredSize().width, 75));
        setBorder(BorderFactory.createEmptyBorder());
        Object[][] rightFieldsArray = {
                { "Ticker", createTextField("1") },
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

        return rightStatusPanel;
    }

    private void setup() {
        setFloatable(false);
        setPreferredSize(new Dimension(getPreferredSize().width, 90)); // Set proper height for toolbar

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(400, 180));
        leftPanel.setMaximumSize(new Dimension(400, 180));
        leftPanel.setMinimumSize(new Dimension(400, 180));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 4, 5));

        leftPanel.add(createTopLeftStatusPanel(), BorderLayout.NORTH);
        leftPanel.add(createBottomLeftStatusPanel(), BorderLayout.SOUTH);
        add(leftPanel);

        // Transport controls
        transportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        transportPanel.setPreferredSize(new Dimension(transportPanel.getPreferredSize().width, 75));
        setupTransportButtons(transportPanel);
        transportPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        add(transportPanel);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(400, 180));
        rightPanel.setMaximumSize(new Dimension(400, 180));
        rightPanel.setMinimumSize(new Dimension(400, 180));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 4, 5));
        rightPanel.add(createTopRightStatusPanel(), BorderLayout.NORTH);
        rightPanel.add(createBottomRightStatusPanel(), BorderLayout.SOUTH);
        // rightPanel.add(createBottomRightStatusPanel(), BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);
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

    private JButton createToolbarButton(String command, String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setEnabled(true); // Enable all transport buttons
        button.setActionCommand(command);

        button.addActionListener(e -> actionBus.publish(command, this));

        // Increase button size
        int size = 32;
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));

        // Use a font that supports Unicode symbols
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        if (!button.getFont().canDisplay('⏮')) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }

        button.setMargin(new Insets(5, 5, 5, 5));
        return button;
    }

    private void setupTransportButtons(JPanel transportPanel) {
        // Create buttons with single tooltip since we don't need navigation text
        JButton rewindBtn = createToolbarButton(Commands.TRANSPORT_REWIND, "⏮", "Previous Ticker");
        JButton pauseBtn = createToolbarButton(Commands.TRANSPORT_PAUSE, "⏸", "Pause");
        JButton recordBtn = createToolbarButton(Commands.TRANSPORT_RECORD, "⏺", "Record");
        JButton stopBtn = createToolbarButton(Commands.TRANSPORT_STOP, "⏹", "Stop");
        JButton playBtn = createToolbarButton(Commands.TRANSPORT_PLAY, "▶", "Play");
        JButton forwardBtn = createToolbarButton(Commands.TRANSPORT_FORWARD, "⏭", "Next Ticker");

        transportPanel.add(rewindBtn);
        transportPanel.add(pauseBtn);
        transportPanel.add(stopBtn);
        transportPanel.add(recordBtn);
        transportPanel.add(playBtn);
        transportPanel.add(forwardBtn);
    }

    private void updateToolbarState(ProxyTicker ticker) {
        boolean hasActiveTicker = Objects.nonNull(ticker);
        for (Component comp : transportPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;

                switch (button.getActionCommand()) {
                    case Commands.TRANSPORT_REWIND ->
                        button.setEnabled(hasActiveTicker && !ticker.isFirst());
                    case Commands.TRANSPORT_FORWARD ->
                        button.setEnabled(hasActiveTicker && (!ticker.isLast || ticker.isLast() && ticker.isValid()));
                    case Commands.TRANSPORT_PAUSE ->
                        button.setEnabled(false);
                    case Commands.TRANSPORT_PLAY ->
                        button.setEnabled(hasActiveTicker && !ticker.isRunning());
                    case Commands.TRANSPORT_STOP ->
                        button.setEnabled(hasActiveTicker && ticker.isRunning());
                    case Commands.TRANSPORT_RECORD ->
                        button.setEnabled(false);
                }
            }
        }
    }

}
