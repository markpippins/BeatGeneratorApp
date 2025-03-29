package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.UIHelper;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;

/**
 * Panel that displays session timing information (left side of toolbar)
 */
public class SessionDisplayPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(SessionDisplayPanel.class);

    private final Map<String, JTextField> fields = new HashMap<>();
    private final TimingBus timingBus = TimingBus.getInstance();
    private final CommandBus commandBus = CommandBus.getInstance();

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
    private JTextField partCountField;

    private UIHelper uiHelper = UIHelper.getInstance();

    public SessionDisplayPanel() {
        super(new BorderLayout());
        setup();
        setupTimingListener();
        setupCommandListener();
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

        // Create label
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel nameLabel = new JLabel(label);
        nameLabel.setForeground(Color.GRAY);
        labelPanel.add(nameLabel);

        // Create text field
        JTextField field = uiHelper.createDisplayField("0");
        fields.put(label, field);

        fieldPanel.add(labelPanel, BorderLayout.NORTH);
        fieldPanel.add(field, BorderLayout.CENTER);

        return fieldPanel;
    }

    public void setSession(Session session) {
        this.currentSession = session;

        if (session == null) {
            resetDisplayCounters();
            return;
        }

        // Update session ID
        sessionField.setText(String.valueOf(session.getId()));

        // Update player count
        fields.get("Players").setText(String.valueOf(session.getPlayers().size()));

        // Always sync with the session's current values
        syncWithSession();
    }

    /**
     * Sync display fields with current session state
     */
    private void syncWithSession() {
        if (currentSession == null) {
            resetDisplayCounters();
            return;
        }

        // Get current position values directly from session (0-based internally)
        tickField.setText(String.valueOf(currentSession.getTick()));
        beatField.setText(String.valueOf(currentSession.getBeat()));
        barField.setText(String.valueOf(currentSession.getBar()));
        partField.setText(String.valueOf(currentSession.getPart()));

        tickCountField.setText(String.valueOf(currentSession.getTickCount()));
        beatCountField.setText(String.valueOf(currentSession.getBeatCount()));
        barCountField.setText(String.valueOf(currentSession.getBarCount()));
        partCountField.setText(String.valueOf(currentSession.getPartCount()));

        // Update player count
        fields.get("Players").setText(String.valueOf(currentSession.getPlayers().size()));

        repaint();
    }

    /**
     * Listen for timing events to update display in real-time
     */
    private void setupTimingListener() {
        timingBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null || currentSession == null)
                    return;

                SwingUtilities.invokeLater(() -> {
                    switch (action.getCommand()) {
                    case Commands.TIMING_TICK -> {
                        if (action.getData() instanceof Number tickVal) {
                            System.out.format("Tick event: %d\n", action.getData());

                            // Update current tick position
                            tickField.setText(String.valueOf(tickVal));
                            tickCountField.setText(String.valueOf(currentSession.getTickCount()));

                            tickField.invalidate();
                            tickField.repaint(); 

                            tickCountField.invalidate();
                            tickCountField.repaint(); 
                        }
                    }
                    case Commands.TIMING_BEAT -> {
                        if (action.getData() instanceof Number beatVal) {
                            System.out.format("Beat event: %s\n", ((Number) action.getData()).toString());   

                            // Update current beat position
                            beatField.setText(String.valueOf(beatVal));
                            beatCountField.setText(String.valueOf(currentSession.getBeatCount()));

                            beatField.invalidate();
                            beatField.repaint(); 

                            beatCountField.invalidate();
                            beatCountField.repaint(); 
                        }
                    }
                    case Commands.TIMING_BAR -> {
                        if (action.getData() instanceof Number barVal) {
                            System.out.format("Bar event: %d\n", action.getData());
                            
                            // Update current bar position
                            barField.setText(String.valueOf(barVal));
                            barCountField.setText(String.valueOf(currentSession.getBarCount()));

                            barField.invalidate();
                            barField.repaint(); 

                            barCountField.invalidate();
                            barCountField.repaint(); 
                        }
                    }
                    case Commands.TIMING_PART -> {
                        if (action.getData() instanceof Number partVal) {
                            System.out.format("Part event: %d\n", action.getData());

                            // Update current part position
                            partField.setText(String.valueOf(partVal));
                            partCountField.setText(String.valueOf(currentSession.getPartCount()));

                            partField.invalidate();
                            partField.repaint(); 

                            partCountField.invalidate();
                            partCountField.repaint(); 
                        }
                    }
                    case Commands.TIMING_RESET -> {
                        resetDisplayCounters();
                    }
                    }
                });
            }
        });
    }

    /**
     * Listen for session and transport commands
     */
    private void setupCommandListener() {
        commandBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                SwingUtilities.invokeLater(() -> {
                    switch (action.getCommand()) {
                    // Session state changes - full reset and sync
                    case Commands.SESSION_CREATED, Commands.SESSION_SELECTED, Commands.SESSION_LOADED -> {
                        if (action.getData() instanceof Session session) {
                            // Complete reset and sync with new session
                            currentSession = session;
                            resetDisplayCounters();
                            syncWithSession();
                        }
                    }

                    // Session updates - just sync values
                    case Commands.SESSION_UPDATED -> {
                        if (action.getData() instanceof Session session) {
                            currentSession = session;
                            syncWithSession();
                        }
                    }

                    // Transport state changes
                    case Commands.TRANSPORT_STOP -> {
                        // Commands.TRANSPORT_RESET -> {
                        // Reset counters and sync with stopped session
                        resetDisplayCounters();
                        if (currentSession != null) {
                            syncWithSession();
                        }
                    }

                    case Commands.TRANSPORT_PLAY -> {
                        // Get active session and sync before playing
                        currentSession = SessionManager.getInstance().getActiveSession();
                        if (currentSession != null) {
                            resetDisplayCounters(); // Start from zero
                            syncWithSession();
                        }
                    }

                    // Make sure we catch session stopping
                    case Commands.SESSION_STOPPED -> {
                        resetDisplayCounters();
                        if (currentSession != null) {
                            syncWithSession();
                        }
                    }
                    }
                });
            }
        });
    }

    /**
     * Reset the display counters (for when session stops/resets)
     */
    private void resetDisplayCounters() {
        // Reset position displays to 0 (not 1)
        tickField.setText("0");
        beatField.setText("0");
        barField.setText("0");
        partField.setText("0");

        // Reset counters too if no session
        if (currentSession == null) {
            tickCountField.setText("0");
            beatCountField.setText("0");
            barCountField.setText("0");
            partCountField.setText("0");
            fields.get("Players").setText("0");
            sessionField.setText("0");
        } else {
            // Otherwise show session's configuration
            syncWithSession();
        }

        repaint();
    }
}