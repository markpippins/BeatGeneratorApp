package com.angrysurfer.beats;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.util.Scale;
import com.formdev.flatlaf.ui.FlatComboBoxUI;

public class ToolBar extends JToolBar {
    private final Map<String, JTextField> leftFields = new HashMap<>();
    private final Map<String, JComponent> rightFields = new HashMap<>(); // Changed to JComponent
    private final CommandBus actionBus = CommandBus.getInstance();
    private Ticker currentTicker; // Add field to track current ticker

    private JPanel transportPanel;
    private JButton playButton;
    private JButton stopButton;

    public ToolBar() {
        super();
        setFloatable(false);
        setup();

        // Modify the command bus listener
        actionBus.register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                // Skip if this ToolBar is the sender
                if (action.getSender() == ToolBar.this) {
                    return;
                }

                if (Objects.nonNull(action.getCommand())) {
                    JComboBox<String> scaleCombo = (JComboBox<String>) rightFields.get("Scale");

                    switch (action.getCommand()) {
                        case Commands.NEXT_SCALE_SELECTED -> {
                            int nextIndex = scaleCombo.getSelectedIndex() + 1;
                            if (nextIndex < scaleCombo.getItemCount()) {
                                scaleCombo.setSelectedIndex(nextIndex);
                            }
                        }
                        case Commands.PREV_SCALE_SELECTED -> {
                            int prevIndex = scaleCombo.getSelectedIndex() - 1;
                            if (prevIndex >= 0) {
                                scaleCombo.setSelectedIndex(prevIndex);
                            }
                        }
                        case Commands.TICKER_SELECTED, Commands.TICKER_UPDATED -> {
                            if (action.getData() instanceof Ticker) {
                                Ticker ticker = (Ticker) action.getData();
                                updateTickerDisplay(ticker);
                                updateToolbarState(ticker);
                            }
                        }
                        case Commands.RULE_ADDED -> {
                            // Re-evaluate forward button state when a rule is added
                            if (currentTicker != null) {
                                updateToolbarState(currentTicker);
                            }
                        }
                        case Commands.TICKER_CREATED -> {
                            if (action.getData() instanceof Ticker ticker) {
                                updateTickerDisplay(ticker);
                                // Force disable forward button for new ticker
                                for (Component comp : transportPanel.getComponents()) {
                                    if (comp instanceof JButton &&
                                            Commands.TRANSPORT_FORWARD.equals(((JButton) comp).getActionCommand())) {
                                        comp.setEnabled(false);
                                        break;
                                    }
                                }
                            }
                        }
                        case Commands.TRANSPORT_STATE_CHANGED -> {
                            if (action.getData() instanceof Boolean isPlaying) {
                                SwingUtilities.invokeLater(() -> {
                                    playButton.setEnabled(!isPlaying);
                                    stopButton.setEnabled(isPlaying);
                                });
                            }
                        }
                    }
                }
            }
        });

        // Then request the initial ticker state after a short delay
        SwingUtilities.invokeLater(() -> {
            // First ensure TickerManager has an active ticker
            Ticker currentTicker = SessionManager.getInstance().getActiveTicker();
            if (currentTicker != null) {
                actionBus.publish(Commands.TICKER_SELECTED, this, currentTicker);
            } else {
                actionBus.publish(Commands.TICKER_REQUEST, this);
            }
        });

        // Set initial button states
        playButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    // TODO: Investigate custom UI for combo boxes
    // FlatComboBoxUI comboBoxUI = new FlatComboBoxUI() {
    // @Override
    // protected JButton createArrowButton() {
    // JButton button = super.createArrowButton();
    // button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    // return button;
    // }
    // };

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

    private void updateTickerDisplay(Ticker ticker) {
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

    private JPanel createTopLeftStatusPanel() {
        JPanel leftStatusPanel = new JPanel(new GridLayout(0, 4, 4, 0)); // 4 columns, variable rows, 10px gap
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String[] leftLabels = { "Tick", "Beat", "Bar", "Part" };
        for (String label : leftLabels) {
            JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
            fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            JLabel nameLabel = new JLabel(label);
            nameLabel.setForeground(Color.GRAY);
            labelPanel.add(nameLabel);

            JTextField field = createTextField("0");
            leftFields.put(label, field);

            fieldPanel.add(labelPanel, BorderLayout.NORTH);
            fieldPanel.add(field, BorderLayout.CENTER);

            leftStatusPanel.add(fieldPanel);
        }
        return leftStatusPanel;
    }

    private JPanel createBottomLeftStatusPanel() {
        JPanel leftStatusPanel = new JPanel(new GridLayout(0, 4, 4, 0)); // 4 columns, variable rows, 10px gap
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String[] leftLabels = { "Players", "Ticks", "Beats", "Bars" };
        for (String label : leftLabels) {
            JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
            fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            JLabel nameLabel = new JLabel(label);
            nameLabel.setForeground(Color.GRAY);
            labelPanel.add(nameLabel);

            JTextField field = createTextField("0");
            leftFields.put(label, field);

            fieldPanel.add(labelPanel, BorderLayout.NORTH);
            fieldPanel.add(field, BorderLayout.CENTER);

            leftStatusPanel.add(fieldPanel);
        }
        return leftStatusPanel;
    }

    private JPanel createTopRightStatusPanel() {
        JPanel rightStatusPanel = new JPanel(new GridLayout(0, 5, 4, 0)); // 5 columns, variable rows, 10px horizontal
                                                                          // gap
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Object[][] rightFieldsArray = {
                { "Ticks", createTickerCombo("Ticks", 1, 384, 24) },
                { "BPM", createTickerCombo("BPM", 1, 960, 120) },
                { "B/Bar", createTickerCombo("B/Bar", 1, 16, 4) },
                { "Bars", createTickerCombo("Bars", 1, 128, 4) },
                { "Parts", createTickerCombo("Parts", 1, 64, 1) }
        };

        for (Object[] field : rightFieldsArray) {
            JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
            fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            JLabel nameLabel = new JLabel((String) field[0]);
            nameLabel.setForeground(Color.GRAY);
            labelPanel.add(nameLabel);

            JComponent component = (JComponent) field[1];
            rightFields.put((String) field[0], component);

            fieldPanel.add(labelPanel, BorderLayout.NORTH);
            fieldPanel.add(component, BorderLayout.CENTER);

            rightStatusPanel.add(fieldPanel);
        }

        return rightStatusPanel;
    }

    private JPanel createBottomRightStatusPanel() {
        JPanel rightStatusPanel = new JPanel(new GridLayout(0, 5, 4, 0)); // 5 columns, variable rows, 10px horizontal
                                                                          // gap
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Object[][] rightFieldsArray = {
                { "Root", createRootNoteCombo() },
                { "Scale", createScaleCombo() },
                { "Ticker", createTextField("1") },
                { "Length", createTextField("0") },
                { "Offset", createTextField("0") }
        };

        for (Object[] field : rightFieldsArray) {
            JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
            fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            JLabel nameLabel = new JLabel((String) field[0]);
            nameLabel.setForeground(Color.GRAY);
            labelPanel.add(nameLabel);

            JComponent component = (JComponent) field[1];
            rightFields.put((String) field[0], component);

            fieldPanel.add(labelPanel, BorderLayout.NORTH);
            fieldPanel.add(component, BorderLayout.CENTER);

            rightStatusPanel.add(fieldPanel);
        }

        return rightStatusPanel;
    }

    private JPanel createLabeledControl(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        JLabel l = new JLabel(label);
        l.setHorizontalAlignment(JLabel.LEFT); // Align label to the left
        panel.add(l, BorderLayout.NORTH);
        panel.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        panel.add(component, BorderLayout.SOUTH);
        panel.setMinimumSize(new Dimension(60, 80));
        panel.setMaximumSize(new Dimension(60, 80));
        return panel;
    }

    static final int STATUS_PANEL_WIDTH = 450;

    private void setup() {
        setFloatable(false);
        setPreferredSize(new Dimension(getPreferredSize().width, 110)); // Changed from 90 to 110

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(STATUS_PANEL_WIDTH, 180));
        leftPanel.setMaximumSize(new Dimension(STATUS_PANEL_WIDTH, 180));
        leftPanel.setMinimumSize(new Dimension(STATUS_PANEL_WIDTH, 180));
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
        rightPanel.setPreferredSize(new Dimension(STATUS_PANEL_WIDTH, 180));
        rightPanel.setMaximumSize(new Dimension(STATUS_PANEL_WIDTH, 180));
        rightPanel.setMinimumSize(new Dimension(STATUS_PANEL_WIDTH, 180));
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
        JButton button = new JButton(text) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Calculate center position
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(getText());
                int textHeight = fm.getHeight();
                int x = (getWidth() - textWidth) / 2;
                int y = (getHeight() + textHeight) / 2 - fm.getDescent();

                // Draw background if button is pressed/selected
                if (getModel().isPressed()) {
                    g2.setColor(getBackground().darker());
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                // Draw the text
                g2.setColor(getForeground());
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };

        button.setToolTipText(tooltip);
        button.setEnabled(true);
        button.setActionCommand(command);
        button.addActionListener(e -> actionBus.publish(command, this));

        // Adjust size and font
        int size = 32;
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));

        // Fallback font if needed
        if (!button.getFont().canDisplay('⏮')) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }

        button.setMargin(new Insets(0, 0, 0, 0));
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setFocusPainted(false);

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

        this.playButton = playBtn;
        this.stopButton = stopBtn;
    }

    private void updateToolbarState(Ticker ticker) {
        boolean hasActiveTicker = Objects.nonNull(ticker);
        for (Component comp : transportPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;

                switch (button.getActionCommand()) {
                    case Commands.TRANSPORT_REWIND ->
                        button.setEnabled(hasActiveTicker && SessionManager.getInstance().canMoveBack());
                    case Commands.TRANSPORT_FORWARD ->
                        button.setEnabled(SessionManager.getInstance().canMoveForward());
                    case Commands.TRANSPORT_PAUSE ->
                        button.setEnabled(false);
                    case Commands.TRANSPORT_PLAY ->
                        button.setEnabled(hasActiveTicker && !ticker.isRunning());
                    case Commands.TRANSPORT_STOP ->
                        button.setEnabled(hasActiveTicker && ticker.isRunning());
                    case Commands.TRANSPORT_RECORD ->
                        button.setEnabled(hasActiveTicker); // Enable if we have an active ticker
                }
            }
        }
    }

    private JComboBox<String> createScaleCombo() {
        String[] scaleNames = Scale.SCALE_PATTERNS.keySet()
                .stream()
                .sorted()
                .toArray(String[]::new);

        JComboBox<String> combo = new JComboBox<>(scaleNames);
        combo.setSelectedItem("Chromatic");
        combo.setMaximumSize(new Dimension(160, 25));
        combo.setPreferredSize(new Dimension(160, 25));
        combo.setMinimumSize(new Dimension(160, 25));
        combo.setAlignmentX(Component.CENTER_ALIGNMENT);
        combo.setEnabled(true);

        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedScale = (String) combo.getSelectedItem();
                int selectedIndex = combo.getSelectedIndex();

                // Simply pass the scale name directly
                actionBus.publish(Commands.SCALE_SELECTED, this, selectedScale);

                // Check for first/last selection
                if (selectedIndex == 0) {
                    actionBus.publish(Commands.FIRST_SCALE_SELECTED, this, selectedScale);
                } else if (selectedIndex == combo.getItemCount() - 1) {
                    actionBus.publish(Commands.LAST_SCALE_SELECTED, this, selectedScale);
                }
            }
        });

        return combo;
    }

    private JComboBox<String> createRootNoteCombo() {
        String[] keys = {
                "C",
                // "C♯/D♭",
                "D",
                // "D♯/E♭",
                "E",
                "F",
                // "F♯/G♭",
                "G",
                // "G♯/A♭",
                "A",
                // "A♯/B♭",
                "B"
        };

        JComboBox<String> combo = new JComboBox<>(keys);
        combo.setSelectedItem("C");
        combo.setMaximumSize(new Dimension(70, 25));
        combo.setPreferredSize(new Dimension(70, 25));
        combo.setMinimumSize(new Dimension(70, 25));
        combo.setAlignmentX(Component.CENTER_ALIGNMENT);
        combo.setEnabled(true);

        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
                actionBus.publish(Commands.ROOT_NOTE_SELECTED, this, (String) combo.getSelectedItem());
        });

        return combo;
    }

}
