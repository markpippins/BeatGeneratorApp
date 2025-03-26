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
import com.angrysurfer.core.api.CommandBus;
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
    private JTextField tickCountField;
    private JTextField beatCountField;
    private JTextField barCountField;
    private JTextField partCountField; // For parts count

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
                case "Ticks" -> tickCountField = field;
                case "Beats" -> beatCountField = field;
                case "Bars" -> barCountField = field;
                case "Parts" -> partCountField = field;
            }
            
            panel.add(fieldPanel);
        }
        
        return panel;
    }
    
    private JPanel createFieldPanel(String label) {
        JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
        fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Create labeluuuuuuuuuuuuuu
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
                SwingUtilities.invokeLater(() -> {
                    switch (action.getCommand()) {
                        // Existing cases for TIME_TICK, TIME_BEAT, TIME_BAR...
                        
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
                        case Commands.TRANSPORT_STOP -> {
                            // Clear all timing counters
                            resetTimingCounters();
                        }
                        case Commands.TRANSPORT_PLAY -> {
                            // Reset counters before starting playback
                            resetTimingCounters();
                        }
                        case Commands.TIME_RESET -> {
                            // Reset everything when timing is reset
                            resetTimingCounters();
                        }
                    }
                });
            }
        });
        
        // Also listen to command bus for transport states
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() != null) {
                    // Make extra sure we catch transport stop from command bus too
                    if (action.getCommand().equals(Commands.TRANSPORT_STOP)) {
                        SwingUtilities.invokeLater(() -> resetTimingCounters());
                    }
                }
            }
        });
    }
    
    private void resetTimingCounters() {
        // Reset all counters to 0
        currentTick = 0;
        currentBeat = 0;
        currentBar = 0;
        currentPart = 0;
        totalTicks = 0;
        totalBeats = 0;
        totalBars = 0;
        totalParts = 0;
        
        // Reset the players field if it exists
        JTextField playersField = fields.get("Players");
        if (playersField != null) {
            playersField.setText("0");
        }
        
        // Make sure to update all displays
        updateTimingDisplays();
        
        // Force an extra UI refresh
        SwingUtilities.invokeLater(this::repaint);
    }
    
    private void updateTimingDisplays() {
        // Update current position displays (1-based)
        tickField.setText(String.format("%d", currentTick + 1));
        beatField.setText(String.format("%d", currentBeat + 1));
        barField.setText(String.format("%d", currentBar + 1));
        partField.setText(String.format("%d", currentPart + 1));

        // Update total counts
        tickCountField.setText(String.format("%d", totalTicks));
        beatCountField.setText(String.format("%d", totalBeats));
        barCountField.setText(String.format("%d", totalBars));
        
        // Show the cumulative part count
        partCountField.setText(String.format("%d", totalParts));
        
        // Request a UI refresh to ensure all changes are visible
        this.repaint();
    }
}