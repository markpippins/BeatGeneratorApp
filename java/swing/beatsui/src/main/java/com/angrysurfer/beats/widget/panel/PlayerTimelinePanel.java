package com.angrysurfer.beats.widget.panel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Comparison;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.InternalSynthManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerTimelinePanel extends JPanel implements IBusListener {

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

    /**
     * Create an empty placeholder timeline that will be filled in when a player is selected
     */
    public PlayerTimelinePanel() {
        super(new BorderLayout());
        
        setBackground(ColorUtils.coolBlue);

        // Set a fixed preferred size for stable initial layout
        setPreferredSize(new Dimension(800, 400));
        
        // Create the empty grid with initial placeholders
        initEmptyComponents();
        
        // Register for player selection events
        CommandBus.getInstance().register(this);
    }

    /**
     * Initialize empty components with placeholders
     */
    private void initEmptyComponents() {
        // Create header with player name
        nameLabel = new JLabel("Select a player to view timeline");
        nameLabel.setFont(HEADER_FONT);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(ColorUtils.coolBlue);
        infoPanel.add(nameLabel, BorderLayout.CENTER);
        
        add(infoPanel, BorderLayout.NORTH);
        
        // Create main grid panel with fixed cell size
        cellSize = 20; // Use a consistent fixed cell size
        
        gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (player != null) {
                    drawGridLines(g);
                }
            }
        };
        gridPanel.setLayout(null);
        gridPanel.setBackground(GRID_BACKGROUND);
        
        // Create a content panel with fixed row height
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(GRID_BACKGROUND);
        contentPanel.add(gridPanel, BorderLayout.CENTER);
        
        // Create empty time labels panel
        timeLabelsPanel = new JPanel();
        timeLabelsPanel.setLayout(null);
        timeLabelsPanel.setBackground(ColorUtils.coolBlue);
        timeLabelsPanel.setPreferredSize(new Dimension(800, 30));
        
        contentPanel.add(timeLabelsPanel, BorderLayout.SOUTH);
        
        // Add scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Set the player and update the timeline display
     */
    public void setPlayer(Player player) {
        this.player = player;
        
        if (player == null) {
            // Show empty placeholder
            nameLabel.setText("Select a player to view timeline");
            clearGrid();
        } else {
            // Show timeline with fixed row heights
            updateTimelineWithFixedRowHeights();
        }
        
        revalidate();
        repaint();
    }

    /**
     * Update the timeline with fixed row heights for consistency
     */
    private void updateTimelineWithFixedRowHeights() {
        if (player == null || player.getSession() == null) {
            return;
        }
        
        // Clear existing content
        clearGrid();
        
        // Update player name
        updateNameLabel();
        
        Session session = player.getSession();
        int beatsPerBar = session.getBeatsPerBar();
        int bars = session.getBars();
        int ticksPerBeat = session.getTicksPerBeat();
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * ticksPerBeat;
        
        // Use halved row height for consistency - changed from 40 to 20
        int rowHeight = 20;
        
        // Set grid size
        int gridWidth = totalTicks * cellSize + 85;
        int gridHeight = rowHeight * TOTAL_ROWS;
        gridPanel.setPreferredSize(new Dimension(gridWidth, gridHeight));
        
        // Add row labels with fixed height
        addRowLabelsWithFixedHeight(rowHeight);
        
        // Calculate active rules
        boolean[][] activeRules = calculateActiveRules(player, session);
        
        // Add rule cells with fixed height
        for (int tick = 0; tick < totalTicks; tick++) {
            // Tick rules
            addRuleCell(tick, ROW_TICK, activeRules[ROW_TICK][tick], rowHeight);
            addRuleCell(tick, ROW_TICK_COUNT, activeRules[ROW_TICK_COUNT][tick], rowHeight);
            
            // Beat rules at beat boundaries
            if (tick % ticksPerBeat == 0) {
                int beatIndex = tick / ticksPerBeat;
                addRuleCell(tick, ROW_BEAT, activeRules[ROW_BEAT][beatIndex], rowHeight);
                addRuleCell(tick, ROW_BEAT_COUNT, activeRules[ROW_BEAT_COUNT][beatIndex], rowHeight);
            }
            
            // Bar rules at bar boundaries
            if (tick % (beatsPerBar * ticksPerBeat) == 0) {
                int barIndex = tick / (beatsPerBar * ticksPerBeat);
                addRuleCell(tick, ROW_BAR, activeRules[ROW_BAR][barIndex], rowHeight);
                addRuleCell(tick, ROW_BAR_COUNT, activeRules[ROW_BAR_COUNT][barIndex], rowHeight);
            }
            
            // Part rules at beginning
            if (tick == 0) {
                addRuleCell(tick, ROW_PART, activeRules[ROW_PART][0], rowHeight);
                addRuleCell(tick, ROW_PART_COUNT, activeRules[ROW_PART_COUNT][0], rowHeight);
            }
        }
        
        // Update time labels
        updateTimeLabels(beatsPerBar, bars);
    }

    /**
     * Add row labels with fixed height
     */
    private void addRowLabelsWithFixedHeight(int rowHeight) {
        // Create label panel on the left
        JPanel labelPanel = new JPanel(null);
        labelPanel.setBackground(LABEL_PANEL_BACKGROUND);
        labelPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BAR_LINE_COLOR));
        labelPanel.setPreferredSize(new Dimension(80, rowHeight * TOTAL_ROWS));
        
        // Add to grid
        gridPanel.add(labelPanel);
        labelPanel.setBounds(0, 0, 80, rowHeight * TOTAL_ROWS);
        
        // Create labels
        String[] labelTexts = {"Tick", "Ticks", "Beat", "Beats", "Bar", "Bars", "Part", "Parts"};
        for (int i = 0; i < TOTAL_ROWS; i++) {
            JLabel label = createRowLabel(labelTexts[i]);
            label.setBounds(0, i * rowHeight, 80, rowHeight);
            labelPanel.add(label);
        }
    }

    private JLabel createRowLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        // Use a smaller font for the reduced row height
        label.setFont(new Font("Arial", Font.BOLD, 10));
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

        // UPDATED: Position cell exactly at the row with no margin
        int x = labelWidth + tickIndex * cellSize + 1; // Add label width with 1px offset
        int y = rowType * rowHeight; // No margin, align to row top

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

        // UPDATED: Set height to full row height
        cell.setBounds(x, y, width, rowHeight);
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
                    updateTimelineWithFixedRowHeights();
                }
            }
            case Commands.PLAYER_UNSELECTED -> {
                player = null;
                clearGrid();
                nameLabel.setText("Select a player to view timeline");
            }
            case Commands.PLAYER_UPDATED -> {
                if (player != null && action.getData() instanceof Player p && p.getId().equals(player.getId())) {
                    player = p;
                    updateTimelineWithFixedRowHeights();
                }
            }
            // Add handler for note changes
            case Commands.NEW_VALUE_NOTE, Commands.PRESET_UP, Commands.PRESET_DOWN, Commands.PLAYER_ROW_REFRESH -> updateNameLabel();

            case Commands.SESSION_CHANGED -> {
                if (player != null) {
                    updateTimelineWithFixedRowHeights();
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
            nameLabel.setText("Select a player to view timeline");
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

    public void scrollToCurrentPosition() {
        // Scroll to make the current position visible
        // This is particularly useful after resizing
        Rectangle visibleRect = new Rectangle(0, 0, 10, 10);
        scrollRectToVisible(visibleRect);
    }

    /**
     * Draw the grid lines (vertical for beats/bars, horizontal for rows)
     */
    private void drawGridLines(Graphics g) {
        if (player == null || player.getSession() == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Session session = player.getSession();
        int beatsPerBar = session.getBeatsPerBar();
        int bars = session.getBars();
        int ticksPerBeat = session.getTicksPerBeat();
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * ticksPerBeat;

        // Account for label panel width
        int labelWidth = 80;
        int rowHeight = 20; // Match the fixed row height we use elsewhere
        
        // Draw horizontal row dividers
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1));
        for (int i = 1; i < TOTAL_ROWS; i++) {
            int y = i * rowHeight;
            g2d.drawLine(labelWidth, y, labelWidth + totalTicks * cellSize, y);
        }
        
        // Draw vertical beat lines
        for (int beat = 0; beat <= totalBeats; beat++) {
            int x = labelWidth + beat * ticksPerBeat * cellSize;
            
            if (beat % beatsPerBar == 0) {
                // Draw bar lines with thicker stroke
                g2d.setColor(BAR_LINE_COLOR);
                g2d.setStroke(new BasicStroke(2));
            } else {
                // Draw beat lines with thinner stroke
                g2d.setColor(BEAT_LINE_COLOR);
                g2d.setStroke(new BasicStroke(1));
            }
            
            g2d.drawLine(x, 0, x, rowHeight * TOTAL_ROWS);
        }
        
        // Draw vertical tick lines (thinner)
        g2d.setColor(new Color(220, 220, 220)); // Very light gray
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{1, 2}, 0));
        for (int tick = 0; tick <= totalTicks; tick++) {
            // Skip lines that are already drawn as beat or bar lines
            if (tick % ticksPerBeat != 0) {
                int x = labelWidth + tick * cellSize;
                g2d.drawLine(x, 0, x, rowHeight * TOTAL_ROWS);
            }
        }
    }
}
