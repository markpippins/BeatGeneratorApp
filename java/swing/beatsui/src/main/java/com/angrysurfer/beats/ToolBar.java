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
import javax.swing.UIManager;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.util.Scale;

public class ToolBar extends JToolBar {
    private final Map<String, JTextField> leftFields = new HashMap<>();
    private final Map<String, JComponent> rightFields = new HashMap<>(); // Changed to JComponent
    private final CommandBus commandBus = CommandBus.getInstance();
    private Session currentSession; // Add field to track current session

    private JPanel transportPanel;
    private JButton playButton;
    private JButton stopButton;

    public ToolBar() {
        super();
        setFloatable(false);
        setup();

        // Modify the command bus listener
        commandBus.register(new CommandListener() {
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
                        case Commands.SESSION_SELECTED, Commands.SESSION_UPDATED -> {
                            if (action.getData() instanceof Session) {
                                Session session = (Session) action.getData();
                                updateSessionDisplay(session);
                                updateToolbarState(session);
                            }
                        }
                        case Commands.RULE_ADDED -> {
                            // Re-evaluate forward button state when a rule is added
                            if (currentSession != null) {
                                updateToolbarState(currentSession);
                            }
                        }
                        case Commands.SESSION_CREATED -> {
                            if (action.getData() instanceof Session session) {
                                updateSessionDisplay(session);
                                // Force disable forward button for new session
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

        // Then request the initial session state after a short delay
        SwingUtilities.invokeLater(() -> {
            // First ensure SessionManager has an active session
            Session currentSession = SessionManager.getInstance().getActiveSession();
            if (currentSession != null) {
                commandBus.publish(Commands.SESSION_SELECTED, this, currentSession);
            } else {
                commandBus.publish(Commands.SESSION_REQUEST, this);
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

    private JComboBox<Integer> createSessionCombo(String field, int min, int max, int current) {

        JComboBox<Integer> combo = new JComboBox<>();
        for (int i = min; i <= max; i++) {
            combo.addItem(i);
        }
        combo.setSelectedItem(current);
        combo.setMaximumSize(new Dimension(70, 25));
        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && currentSession != null) {
                int value = (Integer) combo.getSelectedItem();
                updateSessionValue(field, value);
            }
        });

        return combo;
    }

    private void updateSessionValue(String field, int value) {
        if (currentSession != null) {
            try {
                switch (field) {
                    case "Ticks" -> currentSession.setTicksPerBeat(value);
                    case "BPM" -> currentSession.setTempoInBPM((float) value); // Cast to float
                    case "B/Bar" -> currentSession.setBeatsPerBar(value);
                    case "Bars" -> currentSession.setBars(value);
                    case "Parts" -> currentSession.setParts(value);
                }
                commandBus.publish(Commands.SESSION_UPDATED, this, currentSession);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void updateSessionDisplay(Session session) {
        if (Objects.isNull(session) || Objects.isNull(session.getId())) {
            System.out.println("ToolBar: Received null session or session ID");
            return;
        }

        System.out.println("ToolBar: Updating display for session " + session.getId() +
                " with " + session.getPlayers().size() + " players");

        this.currentSession = session;

        // Update fields with synchronized block to prevent concurrent modification
        synchronized (this) {
            try {
                // Update left fields
                for (Map.Entry<String, JTextField> entry : leftFields.entrySet()) {
                    String value = switch (entry.getKey()) {
                        case "Tick" -> String.valueOf(session.getTick());
                        case "Beat" -> String.valueOf(session.getBeat());
                        case "Bar" -> String.valueOf(session.getBar());
                        case "Part" -> String.valueOf(session.getPart());
                        case "Players" -> String.valueOf(session.getPlayers().size());
                        case "Ticks" -> String.valueOf(session.getTickCount());
                        case "Beats" -> String.valueOf(session.getBeatCount());
                        case "Bars" -> String.valueOf(session.getBarCount());
                        default -> "0";
                    };
                    entry.getValue().setText(value);
                }

                // Update right fields
                ((JTextField) rightFields.get("Session")).setText(session.getId().toString());
                ((JComboBox<?>) rightFields.get("Ticks")).setSelectedItem(session.getTicksPerBeat());
                ((JComboBox<?>) rightFields.get("BPM")).setSelectedItem(session.getTempoInBPM().intValue());
                ((JComboBox<?>) rightFields.get("B/Bar")).setSelectedItem(session.getBeatsPerBar());
                ((JComboBox<?>) rightFields.get("Bars")).setSelectedItem(session.getBars());
                ((JComboBox<?>) rightFields.get("Parts")).setSelectedItem(session.getParts());
                ((JTextField) rightFields.get("Length")).setText(String.valueOf(session.getPartLength()));
                ((JTextField) rightFields.get("Offset")).setText(String.valueOf(session.getNoteOffset()));

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
                { "Ticks", createSessionCombo("Ticks", 1, 384, 24) },
                { "BPM", createSessionCombo("BPM", 1, 960, 120) },
                { "B/Bar", createSessionCombo("B/Bar", 1, 16, 4) },
                { "Bars", createSessionCombo("Bars", 1, 128, 4) },
                { "Parts", createSessionCombo("Parts", 1, 64, 1) }
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
                { "Session", createTextField("1") },
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
        // Use standard JButton instead of custom painted one
        JButton button = new JButton(text);
        
        button.setToolTipText(tooltip);
        button.setEnabled(true);
        button.setActionCommand(command);
        button.addActionListener(e -> commandBus.publish(command, this));
    
        // Styling copied from TransportPanel
        int size = 32;
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));
        
        // Use same font as TransportPanel
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        
        // Fallback font if needed
        if (!button.getFont().canDisplay('⏮')) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }
    
        // Additional styling from TransportPanel
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setVerticalAlignment(SwingConstants.CENTER);
        
        // Optional: Add a more modern look
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        
        // Optional: Add hover effect with mouse listeners
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(button.getBackground().brighter());
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(UIManager.getColor("Button.background"));
            }
        });
    
        return button;
    }

    private void setupTransportButtons(JPanel transportPanel) {
        // Create buttons with single tooltip since we don't need navigation text
        JButton rewindBtn = createToolbarButton(Commands.TRANSPORT_REWIND, "⏮", "Previous Session");
        JButton pauseBtn = createToolbarButton(Commands.TRANSPORT_PAUSE, "⏸", "Pause");
        JButton recordBtn = createToolbarButton(Commands.TRANSPORT_RECORD, "⏺", "Record");
        JButton stopBtn = createToolbarButton(Commands.TRANSPORT_STOP, "⏹", "Stop");
        JButton playBtn = createToolbarButton(Commands.TRANSPORT_PLAY, "▶", "Play");
        JButton forwardBtn = createToolbarButton(Commands.TRANSPORT_FORWARD, "⏭", "Next Session");

        transportPanel.add(rewindBtn);
        transportPanel.add(pauseBtn);
        transportPanel.add(stopBtn);
        transportPanel.add(recordBtn);
        transportPanel.add(playBtn);
        transportPanel.add(forwardBtn);

        this.playButton = playBtn;
        this.stopButton = stopBtn;
    }

    private void updateToolbarState(Session session) {
        boolean hasActiveSession = Objects.nonNull(session);
        for (Component comp : transportPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;

                switch (button.getActionCommand()) {
                    case Commands.TRANSPORT_REWIND ->
                        button.setEnabled(hasActiveSession && SessionManager.getInstance().canMoveBack());
                    case Commands.TRANSPORT_FORWARD ->
                        button.setEnabled(SessionManager.getInstance().canMoveForward());
                    case Commands.TRANSPORT_PAUSE ->
                        button.setEnabled(false);
                    case Commands.TRANSPORT_PLAY ->
                        button.setEnabled(hasActiveSession && !session.isRunning());
                    case Commands.TRANSPORT_STOP ->
                        button.setEnabled(hasActiveSession && session.isRunning());
                    case Commands.TRANSPORT_RECORD ->
                        button.setEnabled(hasActiveSession); // Enable if we have an active session
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
                commandBus.publish(Commands.SCALE_SELECTED, this, selectedScale);

                // Check for first/last selection
                if (selectedIndex == 0) {
                    commandBus.publish(Commands.FIRST_SCALE_SELECTED, this, selectedScale);
                } else if (selectedIndex == combo.getItemCount() - 1) {
                    commandBus.publish(Commands.LAST_SCALE_SELECTED, this, selectedScale);
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
                commandBus.publish(Commands.ROOT_NOTE_SELECTED, this, (String) combo.getSelectedItem());
        });

        return combo;
    }

}
