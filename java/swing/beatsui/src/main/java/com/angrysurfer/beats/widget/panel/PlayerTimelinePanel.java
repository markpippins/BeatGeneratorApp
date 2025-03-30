package com.angrysurfer.beats.widget.panel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.Comparison;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.InternalSynthManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerTimelinePanel extends StatusProviderPanel implements IBusListener {

    private static final Color GRID_BACKGROUND = Color.WHITE;
    private static final Color BAR_LINE_COLOR = new Color(100, 100, 120);
    private static final Color BEAT_LINE_COLOR = new Color(160, 160, 180);
    private static final Color ACTIVE_CELL_COLOR = new Color(41, 128, 185); // Cool blue color
    private static final Color COUNT_CELL_COLOR = Color.YELLOW; // Yellow for count rules
    private static final Color LABEL_PANEL_BACKGROUND = new Color(20, 20, 25); // Keep left panel dark
    private static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 14);
    private static final int TICKS_PER_BEAT = 4; // Each beat shows 4 ticks

    // Add constants for the rule rows
    private static final int ROW_TICK = 0;
    private static final int ROW_TICK_COUNT = 1;
    private static final int ROW_BEAT = 2;
    private static final int ROW_BEAT_COUNT = 3;
    private static final int ROW_BAR = 4;
    private static final int ROW_BAR_COUNT = 5;
    private static final int ROW_PART = 6;
    private static final int ROW_PART_COUNT = 7;
    private static final int TOTAL_ROWS = 8;

    // Add labels for the rows
    private JLabel ticksRowLabel;
    private JLabel beatsRowLabel;
    private JLabel barsRowLabel;
    private JLabel partsRowLabel;

    private StatusConsumer statusConsumer;
    private Player player;
    private JPanel gridPanel;
    private JLabel nameLabel;
    private JPanel timeLabelsPanel;
    private Map<Point, JPanel> gridCells = new HashMap<>();
    private boolean[] activeBeatMap;
    private int cellSize = 20; // Default, but will be calculated based on width
    private ComponentAdapter resizeListener;

    // Add these constants at the top of the class with the other constants
    private static final int RULE_TYPE_TICK = 0;
    private static final int RULE_TYPE_BEAT = 1;
    private static final int RULE_TYPE_BAR = 2;
    private static final int RULE_TYPE_PART = 3;

    public PlayerTimelinePanel(StatusConsumer statusConsumer) {
        super(new BorderLayout(), statusConsumer);
        this.statusConsumer = statusConsumer;
        setBackground(ColorUtils.coolBlue);

        // Set minimum height of 300px while allowing width to be flexible
        setMinimumSize(new Dimension(200, 300));

        // Initialize the panel components
        initComponents();

        // Register for player selection events
        CommandBus.getInstance().register(this);
    }

    private void initComponents() {
        // Create header with player name (keep existing code)
        nameLabel = new JLabel("No Player Selected");
        nameLabel.setFont(HEADER_FONT);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(nameLabel, BorderLayout.NORTH);

        // Create scrollable grid panel (modified approach)
        gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGridLines(g);
            }
        };
        gridPanel.setLayout(null);
        gridPanel.setBackground(GRID_BACKGROUND);

        // Create a panel to hold both the grid and the time labels
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(GRID_BACKGROUND);

        // Add the grid panel to the content panel
        contentPanel.add(gridPanel, BorderLayout.CENTER);

        // Create time labels panel at the bottom with fixed height
        timeLabelsPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(gridPanel.getPreferredSize().width, 30);
            }
        };
        timeLabelsPanel.setLayout(null);
        timeLabelsPanel.setBackground(ColorUtils.coolBlue);

        // Add time labels below the grid but inside the scrollable area
        contentPanel.add(timeLabelsPanel, BorderLayout.SOUTH);

        // Create the scroll pane with the content panel
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);

        // Make sure there's always room for the time labels
        scrollPane.setColumnHeaderView(new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(10, 35); // Reserve space for the labels
            }
        });

        add(scrollPane, BorderLayout.CENTER);

        // Add resize listener to adjust cell size
        resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateGridSize();
            }
        };
        addComponentListener(resizeListener);
    }

    /**
     * Calculate cell size based on available width and update the grid
     */
    private void updateGridSize() {
        if (player == null || player.getSession() == null) {
            return;
        }

        Session session = player.getSession();
        int beatsPerBar = session.getBeatsPerBar();
        int bars = session.getBars();
        int ticksPerBeat = session.getTicksPerBeat(); // Use actual ticksPerBeat from session
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * ticksPerBeat; // Calculate total ticks based on session value

        // Calculate cell size to fit the visible area
        JViewport scrollPane = (JViewport) gridPanel.getParent().getParent();
        int viewportWidth = scrollPane.getWidth();

        // Ensure at least 1 pixel per tick for accuracy
        cellSize = Math.max(1, viewportWidth / totalTicks); // Minimum 1px instead of 5px

        // Update the grid with new cell size
        updatePlayerGrid();
    }

    /**
     * Draw vertical lines for bars, beats, and ticks
     */
    private void drawGridLines(Graphics g) {
        if (player == null || player.getSession() == null) {
            return;
        }

        Session session = player.getSession();
        int beatsPerBar = session.getBeatsPerBar();
        int bars = session.getBars();
        int ticksPerBeat = session.getTicksPerBeat(); // Get from session
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * ticksPerBeat; // Use session value

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Account for label panel width
        int labelWidth = 80;

        // Draw horizontal row dividers
        int rowHeight = cellSize * 2;
        for (int i = 0; i <= TOTAL_ROWS; i++) {
            int y = i * rowHeight;
            g2d.setColor(BAR_LINE_COLOR);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawLine(0, y, getWidth(), y);
        }

        // Draw vertical tick, beat, and bar lines
        for (int tick = 1; tick <= totalTicks; tick++) {
            int x = labelWidth + tick * cellSize;

            // Draw bar lines (thickest)
            if (tick % (beatsPerBar * ticksPerBeat) == 0) {
                g2d.setColor(BAR_LINE_COLOR);
                g2d.setStroke(new BasicStroke(2.0f));
                g2d.drawLine(x, 0, x, TOTAL_ROWS * rowHeight);
            }
            // Draw beat lines (medium thickness)
            else if (tick % ticksPerBeat == 0) {
                g2d.setColor(BEAT_LINE_COLOR);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawLine(x, 0, x, TOTAL_ROWS * rowHeight);
            }
            // Draw tick lines (thinnest)
            else {
                g2d.setColor(new Color(40, 40, 45));
                g2d.setStroke(new BasicStroke(0.5f));
                g2d.drawLine(x, 0, x, rowHeight); // Only in
            }
        }
    }

    /**
     * Update the grid display for the current player
     */
    public void updatePlayerGrid() {
        if (player == null || player.getSession() == null) {
            nameLabel.setText("No Player Selected");
            clearGrid();
            return;
        }

        // Build a detailed description of the player
        StringBuilder playerInfo = new StringBuilder();

        // Start with player name and class
        playerInfo.append(player.getName());

        // Add instrument information if available
        if (player.getInstrument() != null) {
            playerInfo.append(" - ").append(player.getInstrument().getName());

            // Add device name if it's different from instrument name
            String deviceName = player.getInstrument().getDeviceName();
            if (deviceName != null && !deviceName.isEmpty() && !deviceName.equals(player.getInstrument().getName())) {
                playerInfo.append(" (").append(deviceName).append(")");
            }

            // Get preset name if available
            if (player.getPreset() != null) {
                Long instrumentId = player.getInstrument().getId();
                Long presetNumber = player.getPreset().longValue();

                // For channel 9 (MIDI channel 10), show drum name instead of preset
                if (player.getChannel() == 9) {
                    // Get drum name for the note
                    String drumName = InternalSynthManager.getInstance().getDrumName(player.getRootNote().intValue());
                    playerInfo.append(" - ").append(drumName);
                } else {
                    // For other channels, show preset name
                    String presetName = InternalSynthManager.getInstance().getPresetName(instrumentId, presetNumber);

                    if (presetName != null && !presetName.isEmpty()) {
                        playerInfo.append(" - ").append(presetName);
                    }
                }
            }
        }

        // Update the name label with all the information
        nameLabel.setText(playerInfo.toString());

        // Rest of the existing updatePlayerGrid method...
        Session session = player.getSession();
        int beatsPerBar = session.getBeatsPerBar();
        int bars = session.getBars();
        int ticksPerBeat = session.getTicksPerBeat();
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * ticksPerBeat;

        // Clear existing grid
        clearGrid();

        // Set grid panel size - use multiple rows for different rule types
        int rowHeight = cellSize * 2; // Space for each rule type row
        int gridHeight = rowHeight * TOTAL_ROWS + 40; // Add some padding

        gridPanel.setPreferredSize(new Dimension(totalTicks * cellSize, gridHeight));

        // Add row labels on the left side
        addRowLabels(rowHeight);

        // Calculate which ticks/beats/bars have active rules
        boolean[][] activeRules = calculateActiveRules(player, session);

        // Add cell indicators for each type
        for (int tick = 0; tick < totalTicks; tick++) {
            // Add tick rule cell (row 0)
            addRuleCell(tick, ROW_TICK, activeRules[ROW_TICK][tick], rowHeight);

            // Add tick count rule cell (row 1)
            addRuleCell(tick, ROW_TICK_COUNT, activeRules[ROW_TICK_COUNT][tick], rowHeight);

            // Add beat cell only at beat boundaries (row 2)
            if (tick % ticksPerBeat == 0) {
                int beatIndex = tick / ticksPerBeat;
                addRuleCell(tick, ROW_BEAT, activeRules[ROW_BEAT][beatIndex], rowHeight);
                addRuleCell(tick, ROW_BEAT_COUNT, activeRules[ROW_BEAT_COUNT][beatIndex], rowHeight);
            }

            // Add bar cell only at bar boundaries (row 4)
            if (tick % (beatsPerBar * ticksPerBeat) == 0) {
                int barIndex = tick / (beatsPerBar * ticksPerBeat);
                addRuleCell(tick, ROW_BAR, activeRules[ROW_BAR][barIndex], rowHeight);
                addRuleCell(tick, ROW_BAR_COUNT, activeRules[ROW_BAR_COUNT][barIndex], rowHeight);
            }

            // Add part cell at part boundaries (row 6)
            if (tick == 0) {
                addRuleCell(tick, ROW_PART, activeRules[ROW_PART][0], rowHeight);
                addRuleCell(tick, ROW_PART_COUNT, activeRules[ROW_PART_COUNT][0], rowHeight);
            }
        }

        // Create time indicators
        updateTimeLabels(beatsPerBar, bars);

        // Repaint everything
        revalidate();
        repaint();
    }

    /**
     * Add labels for each row
     */
    private void addRowLabels(int rowHeight) {
        // Create label panel on the left - keep it dark
        JPanel labelPanel = new JPanel(null); // Use null layout for precise positioning
        labelPanel.setBackground(LABEL_PANEL_BACKGROUND);
        labelPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BAR_LINE_COLOR));
        labelPanel.setPreferredSize(new Dimension(80, rowHeight * TOTAL_ROWS));

        // Add to the left side of the grid panel
        gridPanel.add(labelPanel);
        labelPanel.setBounds(0, 0, 80, rowHeight * TOTAL_ROWS);

        // Create labels for each group (one label per two rows)
        JLabel tickLabel = createRowLabel("Tick");
        JLabel ticksLabel = createRowLabel("Ticks");
        JLabel beatLabel = createRowLabel("Beat");
        JLabel beatsLabel = createRowLabel("Beats");
        JLabel barLabel = createRowLabel("Bar");
        JLabel barsLabel = createRowLabel("Bars");
        JLabel partLabel = createRowLabel("Part");
        JLabel partsLabel = createRowLabel("Parts");

        // Position the labels in the middle of their respective two-row groups
        tickLabel.setBounds(0, rowHeight * ROW_TICK, 80, rowHeight);
        ticksLabel.setBounds(0, rowHeight * ROW_TICK_COUNT, 80, rowHeight);
        beatLabel.setBounds(0, rowHeight * ROW_BEAT, 80, rowHeight);
        beatsLabel.setBounds(0, rowHeight * ROW_BEAT_COUNT, 80, rowHeight);
        barLabel.setBounds(0, rowHeight * ROW_BAR, 80, rowHeight);
        barsLabel.setBounds(0, rowHeight * ROW_BAR_COUNT, 80, rowHeight);
        partLabel.setBounds(0, rowHeight * ROW_PART, 80, rowHeight);
        partsLabel.setBounds(0, rowHeight * ROW_PART_COUNT, 80, rowHeight);

        // Add labels to panel
        labelPanel.add(tickLabel);
        labelPanel.add(ticksLabel);
        labelPanel.add(beatLabel);
        labelPanel.add(beatsLabel);
        labelPanel.add(barLabel);
        labelPanel.add(barsLabel);
        labelPanel.add(partLabel);
        labelPanel.add(partsLabel);
    }

    private JLabel createRowLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    }

    /**
     * Calculate which ticks/beats/bars have active rules Returns a 2D array: [row
     * type][index] -> active?
     */
    private boolean[][] calculateActiveRules(Player player, Session session) {
        int beatsPerBar = session.getBeatsPerBar();
        int bars = session.getBars();
        int ticksPerBeat = session.getTicksPerBeat();
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * ticksPerBeat;

        // Create arrays for ALL row types (standard AND count)
        boolean[][] results = new boolean[TOTAL_ROWS][];
        results[ROW_TICK] = new boolean[totalTicks];
        results[ROW_TICK_COUNT] = new boolean[totalTicks];
        results[ROW_BEAT] = new boolean[totalBeats];
        results[ROW_BEAT_COUNT] = new boolean[totalBeats];
        results[ROW_BAR] = new boolean[bars];
        results[ROW_BAR_COUNT] = new boolean[bars];
        results[ROW_PART] = new boolean[1];
        results[ROW_PART_COUNT] = new boolean[1];

        // Get all player rules
        Set<Rule> allRules = player.getRules();

        // Process standard position rules
        for (int tickIndex = 0; tickIndex < totalTicks; tickIndex++) {
            // Calculate position within the session
            int bar = tickIndex / (beatsPerBar * ticksPerBeat);
            int beatInBar = (tickIndex % (beatsPerBar * ticksPerBeat)) / ticksPerBeat;
            int tickInBeat = tickIndex % ticksPerBeat;
            int beatIndex = bar * beatsPerBar + beatInBar;

            // Convert to session values (1-based)
            long sessionTick = tickInBeat + 1;
            double sessionBeat = beatInBar + 1;
            long sessionBar = bar + 1;

            // Check for tick/beat/bar/part rules
            for (Rule rule : allRules) {
                if (rule.getPart() != 0 && rule.getPart() != 1)
                    continue;

                // Use integer constants from Comparison class instead of strings
                switch (rule.getComparison()) {
                // Standard position rules
                case Comparison.TICK: // Use constant instead of "TICK"
                    if (rule.getValue().doubleValue() == sessionTick) {
                        results[ROW_TICK][tickIndex] = true;
                    }
                    break;
                case Comparison.BEAT: // Use constant instead of "BEAT"
                    if (rule.getValue().doubleValue() == sessionBeat) {
                        results[ROW_BEAT][beatIndex] = true;
                    }
                    break;
                case Comparison.BAR: // Use constant instead of "BAR"
                    if (rule.getValue().doubleValue() == sessionBar) {
                        results[ROW_BAR][bar] = true;
                    }
                    break;
                case Comparison.PART: // Use constant instead of "PART"
                    results[ROW_PART][0] = true;
                    break;
                }
            }
        }

        // Process COUNT rules separately
        for (Rule rule : allRules) {
            if (rule.getPart() != 0 && rule.getPart() != 1)
                continue;

            // Use integer constants instead of strings
            switch (rule.getComparison()) {
            case Comparison.TICK_COUNT: // Use constant instead of "TICK_COUNT"
                // For tick count rules, mark where they would match
                for (int i = 0; i < totalTicks; i++) {
                    int tickValue = i + 1; // 1-based counting
                    if (rule.matches(tickValue)) {
                        results[ROW_TICK_COUNT][i] = true;
                    }
                }
                break;

            case Comparison.BEAT_COUNT: // Use constant instead of "BEAT_COUNT"
                // For beat count rules
                for (int i = 0; i < totalBeats; i++) {
                    int beatValue = i + 1; // 1-based counting
                    if (rule.matches(beatValue)) {
                        results[ROW_BEAT_COUNT][i] = true;
                    }
                }
                break;

            case Comparison.BAR_COUNT: // Use constant instead of "BAR_COUNT"
                // For bar count rules
                for (int i = 0; i < bars; i++) {
                    int barValue = i + 1; // 1-based counting
                    if (rule.matches(barValue)) {
                        results[ROW_BAR_COUNT][i] = true;
                    }
                }
                break;

            case Comparison.PART_COUNT: // Use constant instead of "PART_COUNT"
                // Part count rules generally apply to the whole part
                results[ROW_PART_COUNT][0] = true;
                break;
            }
        }

        return results;
    }

    /**
     * Add a cell to represent a rule in the grid
     */
    private void addRuleCell(int tickIndex, int rowType, boolean isActive, int rowHeight) {
        if (!isActive)
            return; // Only add cells for active rules

        // Account for label panel width
        int labelWidth = 80;

        int x = labelWidth + tickIndex * cellSize + 1; // Add label width
        int y = rowType * rowHeight + rowHeight / 4; // Position in correct row with some margin

        JPanel cell = new JPanel();

        // Use yellow for count rules, blue for standard rules
        boolean isCountRule = (rowType == ROW_TICK_COUNT || rowType == ROW_BEAT_COUNT || rowType == ROW_BAR_COUNT
                || rowType == ROW_PART_COUNT);
        cell.setBackground(isActive ? (isCountRule ? COUNT_CELL_COLOR : ACTIVE_CELL_COLOR) : GRID_BACKGROUND);

        // Adjust width based on type
        int width;
        if (rowType == ROW_TICK) {
            width = cellSize - 2;
        } else if (rowType == ROW_BEAT) {
            int ticksPerBeat = player.getSession().getTicksPerBeat();
            width = ticksPerBeat * cellSize - 2;
        } else if (rowType == ROW_BAR) {
            Session session = player.getSession();
            int ticksPerBeat = session.getTicksPerBeat();
            width = session.getBeatsPerBar() * ticksPerBeat * cellSize - 2;
        } else { // PARTS
            width = player.getSession().getBars() * player.getSession().getBeatsPerBar()
                    * player.getSession().getTicksPerBeat() * cellSize - 2;
        }

        cell.setBounds(x, y, width, rowHeight / 2);
        cell.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

        gridPanel.add(cell);
        gridCells.put(new Point(tickIndex, rowType), cell);
    }

    /**
     * Update time labels at the bottom with bars and beats
     */
    private void updateTimeLabels(int beatsPerBar, int bars) {
        timeLabelsPanel.removeAll();

        // Account for label panel width
        int labelWidth = 80;

        // Add bar numbers - show 1-based values to match Session's 1-based counting
        for (int bar = 0; bar < bars; bar++) {
            JLabel barLabel = new JLabel(String.valueOf(bar + 1)); // Display 1-based bar numbers
            barLabel.setForeground(Color.WHITE);
            barLabel.setFont(new Font("Arial", Font.BOLD, 12));

            int barWidth = beatsPerBar * player.getSession().getTicksPerBeat() * cellSize;
            // Add labelWidth to x position to account for left panel
            int x = labelWidth + bar * barWidth + barWidth / 2 - 5; // Center in bar
            barLabel.setBounds(x, 0, 20, 20);

            timeLabelsPanel.add(barLabel);
        }

        // Add beat numbers for each bar - show 1-based values to match Session
        for (int bar = 0; bar < bars; bar++) {
            for (int beat = 0; beat < beatsPerBar; beat++) {
                JLabel beatLabel = new JLabel(String.valueOf(beat + 1)); // Display 1-based beat numbers
                beatLabel.setForeground(Color.LIGHT_GRAY);
                beatLabel.setFont(new Font("Arial", Font.PLAIN, 10));

                // Add labelWidth to x position to account for left panel
                int x = labelWidth
                        + (bar * beatsPerBar * player.getSession().getTicksPerBeat()
                                + beat * player.getSession().getTicksPerBeat()) * cellSize
                        + (player.getSession().getTicksPerBeat() * cellSize / 2) - 3; // Center in beat
                beatLabel.setBounds(x, 10, 10, 10);

                timeLabelsPanel.add(beatLabel);
            }
        }
    }

    /**
     * Clear the grid display
     */
    private void clearGrid() {
        gridPanel.removeAll();
        gridCells.clear();
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null)
            return;

        SwingUtilities.invokeLater(() -> {
            switch (action.getCommand()) {
            case Commands.PLAYER_SELECTED -> {
                if (action.getData() instanceof Player p) {
                    player = p;
                    updatePlayerGrid();
                }
            }
            case Commands.PLAYER_UNSELECTED -> {
                player = null;
                updatePlayerGrid();
            }
            case Commands.PLAYER_UPDATED -> {
                if (player != null && action.getData() instanceof Player p && p.getId().equals(player.getId())) {
                    player = p;
                    updatePlayerGrid();
                }
            }
            // Add handler for note changes
            case Commands.NEW_VALUE_NOTE, Commands.PRESET_UP, Commands.PRESET_DOWN, Commands.PLAYER_ROW_REFRESH -> updateNameLabel();

            case Commands.SESSION_CHANGED -> {
                if (player != null) {
                    updatePlayerGrid();
                }
            }
            }
        });
    }

    /**
     * Updates just the player name label without redrawing the entire grid
     */
    private void updateNameLabel() {
        if (player == null) {
            nameLabel.setText("No Player Selected");
            return;
        }

        StringBuilder playerInfo = new StringBuilder();

        // Start with player name
        playerInfo.append(player.getName());

        // Add instrument information if available
        if (player.getInstrument() != null) {
            playerInfo.append(" - ").append(player.getInstrument().getName());

            // Add device name if it's different from instrument name
            String deviceName = player.getInstrument().getDeviceName();
            if (deviceName != null && !deviceName.isEmpty() && !deviceName.equals(player.getInstrument().getName())) {
                playerInfo.append(" (").append(deviceName).append(")");
            }

            // Get preset name if available
            if (player.getPreset() != null) {
                Long instrumentId = player.getInstrument().getId();
                Long presetNumber = player.getPreset().longValue();

                // For channel 9 (MIDI channel 10), show drum name instead of preset
                if (player.getChannel() == 9) {
                    // Get drum name for the note
                    String drumName = InternalSynthManager.getInstance().getDrumName(player.getRootNote().intValue());
                    playerInfo.append(" - ").append(drumName);
                } else {
                    // For other channels, show preset name
                    String presetName = InternalSynthManager.getInstance().getPresetName(instrumentId, presetNumber);

                    if (presetName != null && !presetName.isEmpty()) {
                        playerInfo.append(" - ").append(presetName);
                    }
                }
            }
        }

        // Update the name label with all the information
        nameLabel.setText(playerInfo.toString());

        // Request focus to ensure the UI updates
        nameLabel.repaint();
    }
}
