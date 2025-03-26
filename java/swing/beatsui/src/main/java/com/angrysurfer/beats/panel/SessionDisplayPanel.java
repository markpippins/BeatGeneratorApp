package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Session;

/**
 * Panel that displays session timing information (left side of toolbar)
 */
public class SessionDisplayPanel extends JPanel {
    private final Map<String, JTextField> fields = new HashMap<>();
    private final TimingBus timingBus = TimingBus.getInstance();
    private Session currentSession;

    // Timing fields
    private JTextField sessionField;
    private JTextField tickField;
    private JTextField beatField;
    private JTextField barField;
    private JTextField partField;
    private JTextField ticksField;
    private JTextField beatsField;
    private JTextField barsField;
    private JTextField lengthField; // For parts count

    // Counter state
    private int currentTick = 0;
    private int currentBeat = 0;
    private int currentBar = 0;
    private int currentPart = 0;
    private int totalTicks = 0;
    private int totalBeats = 0;
    private int totalBars = 0;
    private int totalParts = 0;

    public SessionDisplayPanel() {
        super(new BorderLayout());
        setup();
        setupTimingListener();
    }
    
    private void setup() {
        // Create top and bottom panels
        add(createTopPanel(), BorderLayout.NORTH);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 5, 4, 0)); 
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String[] labels = { "Session", "Tick", "Beat", "Bar", "Part" };
        for (String label : labels) {
            JPanel fieldPanel = createFieldPanel(label);
            JTextField field = (JTextField) fieldPanel.getComponent(1);
            
            // Store field references
            switch (label) {
                case "Session" -> sessionField = field;
                case "Tick" -> tickField = field;
                case "Beat" -> beatField = field;
                case "Bar" -> barField = field;
                case "Part" -> partField = field;
            }
            
            panel.add(fieldPanel);
        }
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 5, 4, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String[] labels = { "Players", "Ticks", "Beats", "Bars", "Parts" };
        for (String label : labels) {
            JPanel fieldPanel = createFieldPanel(label);
            JTextField field = (JTextField) fieldPanel.getComponent(1);
            
            // Store field references
            switch (label) {
                case "Ticks" -> ticksField = field;
                case "Beats" -> beatsField = field;
                case "Bars" -> barsField = field;
                case "Parts" -> lengthField = field;
            }
            
            panel.add(fieldPanel);
        }
        
        return panel;
    }
    
    private JPanel createFieldPanel(String label) {
        JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
        fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Create label
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel nameLabel = new JLabel(label);
        nameLabel.setForeground(Color.GRAY);
        labelPanel.add(nameLabel);
        
        // Create text field
        JTextField field = createTextField("0");
        fields.put(label, field);
        
        fieldPanel.add(labelPanel, BorderLayout.NORTH);
        fieldPanel.add(field, BorderLayout.CENTER);
        
        return fieldPanel;
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
    
    public void setSession(Session session) {
        this.currentSession = session;
        
        if (session == null) {
            resetTimingCounters();
            return;
        }
        
        // Update session ID
        sessionField.setText(String.valueOf(session.getId()));
        
        // Update player count
        fields.get("Players").setText(String.valueOf(session.getPlayers().size()));
        
        // Update current position to match the session's position
        currentTick = (int)(session.getTick() - 1);
        currentBeat = (int)Math.floor(session.getBeat() - 1);
        currentBar = (int)(session.getBar() - 1);
        currentPart = (int)(session.getPart() - 1);
        
        // Update totals
        totalTicks = session.getTicksPerBeat() * session.getBeatsPerBar();
        totalBeats = session.getBeatsPerBar();
        totalBars = session.getBars();
        totalParts = session.getParts();
        
        // Update displays
        updateTimingDisplays();
    }
    
    private void setupTimingListener() {
        timingBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) return;

                SwingUtilities.invokeLater(() -> {
                    switch (action.getCommand()) {
                        case Commands.TIME_TICK -> {
                            currentTick = (currentTick + 1) % (currentSession != null ? currentSession.getTicksPerBeat() : 24);
                            totalTicks++;
                            updateTimingDisplays();
                        }
                        case Commands.TIME_BEAT -> {
                            currentBeat = (currentBeat + 1) % (currentSession != null ? currentSession.getBeatsPerBar() : 4);
                            totalBeats++;
                            updateTimingDisplays();
                        }
                        case Commands.TIME_BAR -> {
                            currentBar = (currentBar + 1) % (currentSession != null ? currentSession.getBars() : 4);
                            totalBars++;
                            updateTimingDisplays();
                        }
                        case Commands.TIME_PART -> {
                            // Use the part value from the event directly (1-based)
                            if (action.getData() instanceof Number partVal) {
                                // Store as 0-based but received as 1-based
                                currentPart = partVal.intValue() - 1;
                                
                                // Increment total parts played count
                                totalParts++;
                                updateTimingDisplays();
                            }
                        }
                        case Commands.TRANSPORT_STOP, Commands.TRANSPORT_PLAY -> {
                            resetTimingCounters();
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
        totalParts = 0;
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
        
        // Show the cumulative part count
        lengthField.setText(String.format("%d", totalParts));
    }
}