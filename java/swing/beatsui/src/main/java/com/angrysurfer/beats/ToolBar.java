package com.angrysurfer.beats;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
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
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.util.Scale;

public class ToolBar extends JToolBar {
    private final Map<String, JTextField> leftFields = new HashMap<>();
    private final Map<String, JComponent> rightFields = new HashMap<>(); // Changed to JComponent
    private final CommandBus commandBus = CommandBus.getInstance();
    private final TimingBus timingBus = TimingBus.getInstance(); // Add TimingBus reference
    private Session currentSession; // Add field to track current session

    private JPanel transportPanel;
    private JButton playButton;
    private JButton stopButton;
    private JButton recordButton; // Store reference to record button
    private boolean isRecording = false; // Track recording state

    // Add timing-related fields
    private JTextField tickField;
    private JTextField beatField;
    private JTextField barField;
    private JTextField partField;
    private JTextField ticksField;
    private JTextField beatsField;
    private JTextField barsField;

    // Add counters
    private int currentTick = 0;
    private int currentBeat = 0;
    private int currentBar = 0;
    private int currentPart = 0;
    private int totalTicks = 0;
    private int totalBeats = 0;
    private int totalBars = 0;

    // Add field declarations at the top of the class
    private JTextField sessionField;
    private JTextField lengthField;

    public ToolBar() {
        super();
        setFloatable(false);
        setup();

        // Modify the command bus listener
        commandBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                // Skip if this ToolBar is the sender
                if (action.getSender() == ToolBar.this) {
                    return;
                }

                if (Objects.nonNull(action.getCommand())) {
                    JComboBox<String> scaleCombo = (JComboBox<String>) rightFields.get("Scale");

                    switch (action.getCommand()) {
                        case Commands.NEXT_SCALE_SELECTED:
                            int nextIndex = scaleCombo.getSelectedIndex() + 1;
                            if (nextIndex < scaleCombo.getItemCount()) {
                                scaleCombo.setSelectedIndex(nextIndex);
                            }
                            break;
                        case Commands.PREV_SCALE_SELECTED:
                            int prevIndex = scaleCombo.getSelectedIndex() - 1;
                            if (prevIndex >= 0) {
                                scaleCombo.setSelectedIndex(prevIndex);
                            }
                            break;
                        case Commands.SESSION_SELECTED:
                        case Commands.SESSION_UPDATED:
                            if (action.getData() instanceof Session) {
                                Session session = (Session) action.getData();
                                updateSessionDisplay(session);
                                updateToolbarState(session);
                            }
                            break;
                        case Commands.RULE_ADDED:
                            // Re-evaluate forward button state when a rule is added
                            if (currentSession != null) {
                                updateToolbarState(currentSession);
                            }
                            break;
                        case Commands.SESSION_CREATED:
                            if (action.getData() instanceof Session) {
                                Session session = (Session) action.getData();
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
                            break;
                        case Commands.TRANSPORT_STATE_CHANGED:
                            if (action.getData() instanceof Boolean) {
                                Boolean isPlaying = (Boolean) action.getData();
                                SwingUtilities.invokeLater(() -> {
                                    playButton.setEnabled(!isPlaying);
                                    stopButton.setEnabled(isPlaying);
                                });
                            }
                            break;
                        case Commands.TRANSPORT_RECORD_START:
                            isRecording = true;
                            updateRecordButtonAppearance();
                            break;
                        case Commands.TRANSPORT_RECORD_STOP:
                            isRecording = false;
                            updateRecordButtonAppearance();
                            break;
                        case Commands.TRANSPORT_STOP:
                            // Also stop recording when transport is stopped
                            isRecording = false;
                            updateRecordButtonAppearance();
                            // resetTimingCounters();
                            break;
                    }
                }
            }
        });

        // Add new TimingBus listener
        setupTimingBusListener();

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

    /**
     * Set up listener for timing events to update counter fields
     */
    private void setupTimingBusListener() {
        timingBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) {
                    return;
                }

                // Get the action from the command
                String cmd = action.getCommand();
                
                try {
                    // Log every event for debugging
                    System.out.println("ToolBar received timing event: " + cmd);
                    
                    switch (cmd) {
                        case Commands.TIMING_TICK -> {
                            // Use SwingUtilities.invokeLater for thread safety when updating UI
                            SwingUtilities.invokeLater(() -> {
                                if (currentSession != null) {
                                    updateTickFields(currentSession);
                                    System.out.println("Updated tick fields: " + currentSession.getTick());
                                }
                            });
                        }
                        case Commands.TIMING_BEAT -> {
                            SwingUtilities.invokeLater(() -> {
                                if (currentSession != null) {
                                    updateBeatFields(currentSession);
                                    System.out.println("Updated beat fields: " + currentSession.getBeat());
                                }
                            });
                        }
                        case Commands.TIMING_BAR -> {
                            SwingUtilities.invokeLater(() -> {
                                if (currentSession != null) {
                                    updateBarFields(currentSession);
                                    System.out.println("Updated bar fields: " + currentSession.getBar());
                                }
                            });
                        }
                        case Commands.TIMING_PART -> {
                            SwingUtilities.invokeLater(() -> {
                                if (currentSession != null) {
                                    updatePartFields(currentSession);
                                    System.out.println("Updated part fields: " + currentSession.getPart());
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    System.err.println("ToolBar: Error handling timing event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * Update tick-related fields
     */
    private void updateTickFields(Session session) {
        try {
            leftFields.get("Tick").setText(String.valueOf(session.getTick()));
            leftFields.get("Ticks").setText(String.valueOf(session.getTickCount()));
        } catch (Exception e) {
            System.err.println("Error updating tick fields: " + e.getMessage());
        }
    }
    
    /**
     * Update beat-related fields
     */
    private void updateBeatFields(Session session) {
        try {
            leftFields.get("Beat").setText(String.valueOf(session.getBeat()));
            leftFields.get("Beats").setText(String.valueOf(session.getBeatCount()));
        } catch (Exception e) {
            System.err.println("Error updating beat fields: " + e.getMessage());
        }
    }
    
    /**
     * Update bar-related fields
     */
    private void updateBarFields(Session session) {
        try {
            leftFields.get("Bar").setText(String.valueOf(session.getBar()));
            leftFields.get("Bars").setText(String.valueOf(session.getBarCount()));
        } catch (Exception e) {
            System.err.println("Error updating bar fields: " + e.getMessage());
        }
    }
    
    /**
     * Update part-related fields
     */
    private void updatePartFields(Session session) {
        try {
            leftFields.get("Part").setText(String.valueOf(session.getPart()));
            // There's no "Parts" counter field, but we could add one if desired
        } catch (Exception e) {
            System.err.println("Error updating part fields: " + e.getMessage());
        }
    }

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
                    case "PPQ" -> currentSession.setTicksPerBeat(value);
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
                // Update left timing fields using our helper methods
                updateTickFields(session);
                updateBeatFields(session);
                updateBarFields(session);
                updatePartFields(session);
                
                // Update Players field separately
                leftFields.get("Players").setText(String.valueOf(session.getPlayers().size()));

                // Update right fields - keep this part unchanged
                ((JTextField) rightFields.get("Session")).setText(session.getId().toString());
                ((JComboBox<?>) rightFields.get("PPQ")).setSelectedItem(session.getTicksPerBeat());
                ((JComboBox<?>) rightFields.get("BPM")).setSelectedItem(session.getTempoInBPM().intValue());
                ((JComboBox<?>) rightFields.get("B/Bar")).setSelectedItem(session.getBeatsPerBar());
                ((JComboBox<?>) rightFields.get("Bars")).setSelectedItem(session.getBars());
                ((JComboBox<?>) rightFields.get("Parts")).setSelectedItem(session.getParts());
                ((JTextField) rightFields.get("Length")).setText(String.valueOf(session.getPartLength()));
                ((JTextField) rightFields.get("Offset")).setText(String.valueOf(session.getNoteOffset()));

                // Update the combo boxes with current session values
                if (rightFields.get("Offset") instanceof JComboBox<?> offsetCombo) {
                    offsetCombo.setSelectedItem(session.getNoteOffset().intValue());
                }
                if (rightFields.get("Length") instanceof JComboBox<?> lengthCombo) {
                    lengthCombo.setSelectedItem(session.getPartLength().intValue());
                }

            } catch (Exception e) {
                System.err.println("ToolBar: Error updating display: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private JPanel createTopLeftStatusPanel() {
        JPanel leftStatusPanel = new JPanel(new GridLayout(0, 5, 4, 0)); // Changed to 5 columns
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String[] leftLabels = { "Session", "Tick", "Beat", "Bar", "Part" }; // Added "Session" at start
        for (String label : leftLabels) {
            JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
            fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            JLabel nameLabel = new JLabel(label);
            nameLabel.setForeground(Color.GRAY);
            labelPanel.add(nameLabel);

            JTextField field = createTextField("0");
            leftFields.put(label, field);

            // Store references to timing fields
            switch (label) {
                case "Session" -> sessionField = field;
                case "Tick" -> tickField = field;
                case "Beat" -> beatField = field;
                case "Bar" -> barField = field;
                case "Part" -> partField = field;
            }

            fieldPanel.add(labelPanel, BorderLayout.NORTH);
            fieldPanel.add(field, BorderLayout.CENTER);

            leftStatusPanel.add(fieldPanel);
        }

        // Set up timing bus listener
        setupTimingListener();

        return leftStatusPanel;
    }

    private JTextField createTimingField(String name) {
        JTextField field = new JTextField(3);
        field.setEditable(false);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setText("0");
        field.setName(name);
        return field;
    }

    private JPanel createLabeledField(String labelText, JTextField field) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        panel.add(new JLabel(labelText));
        panel.add(field);
        return panel;
    }

    private void setupTimingListener() {
        // Listen for timing events
        timingBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) return;

                SwingUtilities.invokeLater(() -> {
                    switch (action.getCommand()) {
                        case Commands.TIMING_TICK -> {
                            currentTick = (currentTick + 1) % (currentSession != null ? currentSession.getTicksPerBeat() : 24);
                            totalTicks++;
                            updateTimingDisplays();
                        }
                        case Commands.TIMING_BEAT -> {
                            currentBeat = (currentBeat + 1) % (currentSession != null ? currentSession.getBeatsPerBar() : 4);
                            totalBeats++;
                            updateTimingDisplays();
                        }
                        case Commands.TIMING_BAR -> {
                            currentBar = (currentBar + 1) % (currentSession != null ? currentSession.getBars() : 4);
                            totalBars++;
                            updateTimingDisplays();
                        }
                        case Commands.TIMING_PART -> {
                            currentPart = (currentPart + 1);
                            updateTimingDisplays();
                        }
                        case Commands.TRANSPORT_STOP, Commands.TRANSPORT_PLAY -> {
                            resetTimingCounters();
                        }
                    }
                });
            }
        });

        // Listen for session events
        commandBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) return;

                SwingUtilities.invokeLater(() -> {
                    switch (action.getCommand()) {
                        case Commands.SESSION_SELECTED, Commands.SESSION_LOADED, Commands.SESSION_UPDATED -> {
                            if (action.getData() instanceof Session session) {
                                currentSession = session;
                                sessionField.setText(String.valueOf(session.getId()));
                                // Update length field if needed
                                lengthField.setText(String.valueOf(session.getPartLength()));
                            }
                        }
                    }
                });
            }
        });
    }

    private void resetTimingCounters() {
        currentTick = 0;
        currentBeat = 0;
        currentBar = 0;
        currentPart = 0;
        totalTicks = 0;
        totalBeats = 0;
        totalBars = 0;
        updateTimingDisplays();
    }

    private void updateTimingDisplays() {
        // Update current position displays (1-based)
        tickField.setText(String.format("%d", currentTick + 1));
        beatField.setText(String.format("%d", currentBeat + 1));
        barField.setText(String.format("%d", currentBar + 1));
        partField.setText(String.format("%d", currentPart + 1));

        // Update total counts
        ticksField.setText(String.format("%d", totalTicks));
        beatsField.setText(String.format("%d", totalBeats));
        barsField.setText(String.format("%d", totalBars));
    }

    private JPanel createBottomLeftStatusPanel() {
        JPanel leftStatusPanel = new JPanel(new GridLayout(0, 5, 4, 0)); // Changed to 5 columns
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String[] leftLabels = { "Players", "Ticks", "Beats", "Bars", "Length" }; // Added "Length" at end
        for (String label : leftLabels) {
            JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
            fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            JLabel nameLabel = new JLabel(label);
            nameLabel.setForeground(Color.GRAY);
            labelPanel.add(nameLabel);

            JTextField field = createTextField("0");
            leftFields.put(label, field);

            // Store references to total count fields
            switch (label) {
                case "Ticks" -> ticksField = field;
                case "Beats" -> beatsField = field;
                case "Bars" -> barsField = field;
                case "Length" -> lengthField = field;
            }

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
                { "PPQ", createSessionCombo("PPQ", 1, 384, 24) },
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
        JPanel rightStatusPanel = new JPanel(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create fields for bottom right panel
        JComboBox<String> rootNoteCombo = createRootNoteCombo();
        rightFields.put("Root", rootNoteCombo);

        JComboBox<String> scaleCombo = createScaleCombo();
        scaleCombo.setPreferredSize(new Dimension(180, 25)); // Make scale combo wider
        rightFields.put("Scale", scaleCombo);

        // Create offset combo box (values from -12 to +12)
        JComboBox<Integer> offsetCombo = new JComboBox<>();
        for (int i = -12; i <= 12; i++) {
            offsetCombo.addItem(i);
        }
        offsetCombo.setSelectedItem(0);
        offsetCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && currentSession != null) {
                currentSession.setNoteOffset(Double.valueOf(((Integer) offsetCombo.getSelectedItem())));
                commandBus.publish(Commands.SESSION_UPDATED, this, currentSession);
            }
        });
        rightFields.put("Offset", offsetCombo);

        // Create length combo box (values from 1 to 32)
        JComboBox<Integer> lengthCombo = new JComboBox<>();
        for (int i = 1; i <= 32; i++) {
            lengthCombo.addItem(i);
        }
        lengthCombo.setSelectedItem(4);
        lengthCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && currentSession != null) {
                currentSession.setPartLength((Integer) lengthCombo.getSelectedItem());
                commandBus.publish(Commands.SESSION_UPDATED, this, currentSession);
            }
        });
        rightFields.put("Length", lengthCombo);

        // Create field panels with labels and add them with GridBagConstraints
        String[] rightLabels = { "Root", "Scale", "Offset", "Length" }; // Swapped Offset and Length
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 2, 0, 2);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridy = 0;

        for (int i = 0; i < rightLabels.length; i++) {
            String label = rightLabels[i];
            JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
            fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            JLabel nameLabel = new JLabel(label);
            nameLabel.setForeground(Color.GRAY);
            labelPanel.add(nameLabel);

            fieldPanel.add(labelPanel, BorderLayout.NORTH);
            fieldPanel.add((JComponent) rightFields.get(label), BorderLayout.CENTER);

            // Special handling for Scale to span 2 columns
            if (label.equals("Scale")) {
                gbc.gridwidth = 2;
            } else {
                gbc.gridwidth = 1;
            }

            gbc.gridx = (label.equals("Offset") || label.equals("Length")) ? 
                        i + 1 : i; // Adjust position for Offset and Length due to Scale's double width

            rightStatusPanel.add(fieldPanel, gbc);
        }

        return rightStatusPanel;
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
        
        // Create record button with special handling - don't use the standard createToolbarButton
        recordButton = new JButton("⏺");
        recordButton.setToolTipText("Record");
        recordButton.setEnabled(true);
        recordButton.setActionCommand(Commands.TRANSPORT_RECORD);
        
        // Set same styling as other buttons
        int size = 32;
        recordButton.setPreferredSize(new Dimension(size, size));
        recordButton.setMinimumSize(new Dimension(size, size));
        recordButton.setMaximumSize(new Dimension(size, size));
        recordButton.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        if (!recordButton.getFont().canDisplay('⏺')) {
            recordButton.setFont(new Font("Dialog", Font.PLAIN, 18));
        }
        recordButton.setMargin(new Insets(0, 0, 0, 0));
        recordButton.setFocusPainted(false);
        recordButton.setVerticalAlignment(SwingConstants.CENTER);
        recordButton.setBorderPainted(false);
        recordButton.setContentAreaFilled(true);
        recordButton.setOpaque(true);
        
        // Custom action listener for record button
        recordButton.addActionListener(e -> {
            toggleRecordingState();
            
            // Send the appropriate command
            if (isRecording) {
                commandBus.publish(Commands.TRANSPORT_RECORD_START, this);
            } else {
                commandBus.publish(Commands.TRANSPORT_RECORD_STOP, this);
            }
        });
        
        // No hover effects for record button - we'll handle its color through updateRecordButtonAppearance
        
        JButton stopBtn = createToolbarButton(Commands.TRANSPORT_STOP, "⏹", "Stop");
        JButton playBtn = createToolbarButton(Commands.TRANSPORT_PLAY, "▶", "Play");
        JButton forwardBtn = createToolbarButton(Commands.TRANSPORT_FORWARD, "⏭", "Next Session");

        transportPanel.add(rewindBtn);
        transportPanel.add(pauseBtn);
        transportPanel.add(stopBtn);
        transportPanel.add(recordButton);
        transportPanel.add(playBtn);
        transportPanel.add(forwardBtn);

        this.playButton = playBtn;
        this.stopButton = stopBtn;
        
        // Initial update to ensure correct color
        updateRecordButtonAppearance();
    }

    /**
     * Toggle recording state and update button appearance
     */
    private void toggleRecordingState() {
        isRecording = !isRecording;
        updateRecordButtonAppearance();
    }

    /**
     * Update record button appearance based on recording state
     */
    private void updateRecordButtonAppearance() {
        if (isRecording) {
            recordButton.setBackground(Color.RED);
            recordButton.setForeground(Color.WHITE);
        } else {
            recordButton.setBackground(UIManager.getColor("Button.background"));
            recordButton.setForeground(UIManager.getColor("Button.foreground"));
        }
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
        
        // Increase width to 195px (160px + 35px from Root)
        combo.setMaximumSize(new Dimension(195, 25));
        combo.setPreferredSize(new Dimension(195, 25));
        combo.setMinimumSize(new Dimension(195, 25));
        
        combo.setAlignmentX(Component.CENTER_ALIGNMENT);
        combo.setEnabled(true);

        // Existing listener code
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
        
        // Reduce width to 35px (half of current 70px)
        combo.setMaximumSize(new Dimension(35, 25));
        combo.setPreferredSize(new Dimension(35, 25));
        combo.setMinimumSize(new Dimension(35, 25));
        
        combo.setAlignmentX(Component.CENTER_ALIGNMENT);
        combo.setEnabled(true);

        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
                commandBus.publish(Commands.ROOT_NOTE_SELECTED, this, (String) combo.getSelectedItem());
        });

        return combo;
    }

}
