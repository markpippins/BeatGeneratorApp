package com.angrysurfer.beats.panel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
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
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.ColorUtils;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerTimelinePanel extends StatusProviderPanel implements IBusListener {

    private static final Color GRID_BACKGROUND = new Color(25, 25, 30);
    private static final Color BAR_LINE_COLOR = new Color(80, 80, 90);
    private static final Color BEAT_LINE_COLOR = new Color(60, 60, 70);
    private static final Color ACTIVE_CELL_COLOR = new Color(0, 180, 120);
    private static final Color MUTED_CELL_COLOR = new Color(180, 60, 60);
    private static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 14);
    private static final int TICKS_PER_BEAT = 4; // Each beat shows 4 ticks

    // Add constants for the rule rows
    private static final int ROW_TICKS = 0;
    private static final int ROW_BEATS = 1;
    private static final int ROW_BARS = 2;
    private static final int ROW_PARTS = 3;
    private static final int TOTAL_ROWS = 4;
    
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
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * TICKS_PER_BEAT;
        
        // Calculate cell size to fit the visible area
        // Use scrollPane's viewport width
        JScrollPane scrollPane = (JScrollPane) gridPanel.getParent().getParent();
        int viewportWidth = scrollPane.getViewport().getWidth();
        
        // Allow horizontal scrolling if too many ticks
        int minCellSize = 5; // Don't let cells get too small
        cellSize = Math.max(minCellSize, viewportWidth / totalTicks);
        
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
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * TICKS_PER_BEAT;
        
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
        for (int tick = 0; tick <= totalTicks; tick++) {
            int x = labelWidth + tick * cellSize;
            
            // Draw bar lines (thickest)
            if (tick % (beatsPerBar * TICKS_PER_BEAT) == 0) {
                g2d.setColor(BAR_LINE_COLOR);
                g2d.setStroke(new BasicStroke(2.0f));
                g2d.drawLine(x, 0, x, TOTAL_ROWS * rowHeight);
            } 
            // Draw beat lines (medium thickness)
            else if (tick % TICKS_PER_BEAT == 0) {
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
        
        // Update player name in header
        nameLabel.setText(player.getName() + " - " + player.getPlayerClassName());
        
        Session session = player.getSession();
        int beatsPerBar = session.getBeatsPerBar();
        int bars = session.getBars();
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * TICKS_PER_BEAT;
        
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
            addRuleCell(tick, ROW_TICKS, activeRules[ROW_TICKS][tick], rowHeight);
            
            // Add beat cell only at beat boundaries (row 1)
            if (tick % TICKS_PER_BEAT == 0) {
                int beatIndex = tick / TICKS_PER_BEAT;
                addRuleCell(tick, ROW_BEATS, activeRules[ROW_BEATS][beatIndex], rowHeight);
            }
            
            // Add bar cell only at bar boundaries (row 2)
            if (tick % (beatsPerBar * TICKS_PER_BEAT) == 0) {
                int barIndex = tick / (beatsPerBar * TICKS_PER_BEAT);
                addRuleCell(tick, ROW_BARS, activeRules[ROW_BARS][barIndex], rowHeight);
            }
            
            // Add part cell at part boundaries (row 3)
            if (tick == 0) {
                addRuleCell(tick, ROW_PARTS, activeRules[ROW_PARTS][0], rowHeight);
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
        // Create label panel on the left
        JPanel labelPanel = new JPanel(new GridLayout(TOTAL_ROWS, 1));
        labelPanel.setBackground(GRID_BACKGROUND);
        labelPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BAR_LINE_COLOR));
        labelPanel.setPreferredSize(new Dimension(80, rowHeight * TOTAL_ROWS));
        
        // Add to the left side of the grid panel
        gridPanel.add(labelPanel);
        labelPanel.setBounds(0, 0, 80, rowHeight * TOTAL_ROWS);
        
        // Create and add labels
        ticksRowLabel = createRowLabel("Ticks");
        beatsRowLabel = createRowLabel("Beats");
        barsRowLabel = createRowLabel("Bars");
        partsRowLabel = createRowLabel("Parts");
        
        labelPanel.add(ticksRowLabel);
        labelPanel.add(beatsRowLabel);
        labelPanel.add(barsRowLabel);
        labelPanel.add(partsRowLabel);
    }
    
    private JLabel createRowLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    }
    
    /**
     * Calculate which ticks/beats/bars have active rules
     * Returns a 2D array: [row type][index] -> active?
     */
    private boolean[][] calculateActiveRules(Player player, Session session) {
        int beatsPerBar = session.getBeatsPerBar();
        int bars = session.getBars();
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * TICKS_PER_BEAT;
        
        // Create arrays for each row type
        boolean[] activeTicks = new boolean[totalTicks];
        boolean[] activeBeats = new boolean[totalBeats];
        boolean[] activeBars = new boolean[bars];
        boolean[] activeParts = new boolean[1]; // Just part 0 and part 1
        
        // Get rules for part 0 and part 1
        Set<Rule> allRules = player.getRules();
        
        // For each tick, check if the player's rules would make it play
        for (int tickIndex = 0; tickIndex < totalTicks; tickIndex++) {
            // Calculate position within the session
            int bar = tickIndex / (beatsPerBar * TICKS_PER_BEAT);
            int beatInBar = (tickIndex % (beatsPerBar * TICKS_PER_BEAT)) / TICKS_PER_BEAT;
            int tickInBeat = tickIndex % TICKS_PER_BEAT;
            int beatIndex = bar * beatsPerBar + beatInBar;
            
            // Convert to session values
            long sessionTick = tickInBeat;
            double sessionBeat = beatInBar + (tickInBeat / (double)TICKS_PER_BEAT);
            long sessionBar = bar;
            
            // Check for tick-specific rules (part 0 and part 1)
            for (Rule rule : allRules) {
                if ((rule.getPart() == 0 || rule.getPart() == 1) && 
                    rule.getOperator() == RULE_TYPE_TICK && 
                    rule.getValue() == sessionTick) {
                    activeTicks[tickIndex] = true;
                }
                
                // Check for beat-specific rules
                if ((rule.getPart() == 0 || rule.getPart() == 1) && 
                    rule.getOperator() == RULE_TYPE_BEAT && 
                    rule.getValue() == Math.floor(sessionBeat)) {
                    activeBeats[beatIndex] = true;
                }
                
                // Check for bar-specific rules
                if ((rule.getPart() == 0 || rule.getPart() == 1) && 
                    rule.getOperator() == RULE_TYPE_BAR && 
                    rule.getValue() == sessionBar) {
                    activeBars[bar] = true;
                }
                
                // Check for part-specific rules (part 0 or part 1)
                if ((rule.getPart() == 0 || rule.getPart() == 1) && 
                    rule.getOperator() == RULE_TYPE_PART) {
                    activeParts[0] = true;
                }
            }
        }
        
        // Combine all results in a 2D array
        boolean[][] results = new boolean[TOTAL_ROWS][];
        results[ROW_TICKS] = activeTicks;
        results[ROW_BEATS] = activeBeats;
        results[ROW_BARS] = activeBars;
        results[ROW_PARTS] = activeParts;
        
        return results;
    }
    
    /**
     * Add a cell to represent a rule in the grid
     */
    private void addRuleCell(int tickIndex, int rowType, boolean isActive, int rowHeight) {
        // Account for label panel width
        int labelWidth = 80;
        
        int x = labelWidth + tickIndex * cellSize + 1; // Add label width
        int y = rowType * rowHeight + rowHeight/4; // Position in correct row with some margin
        
        JPanel cell = new JPanel();
        cell.setBackground(isActive ? ACTIVE_CELL_COLOR : GRID_BACKGROUND);
        
        // Adjust width based on type
        int width;
        if (rowType == ROW_TICKS) {
            width = cellSize - 2;
        } else if (rowType == ROW_BEATS) {
            width = TICKS_PER_BEAT * cellSize - 2;
        } else if (rowType == ROW_BARS) {
            Session session = player.getSession();
            width = session.getBeatsPerBar() * TICKS_PER_BEAT * cellSize - 2;
        } else { // PARTS
            width = player.getSession().getBars() * 
                    player.getSession().getBeatsPerBar() * 
                    TICKS_PER_BEAT * cellSize - 2;
        }
        
        cell.setBounds(x, y, width, rowHeight/2);
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
        
        // Add bar numbers
        for (int bar = 0; bar < bars; bar++) {
            JLabel barLabel = new JLabel(String.valueOf(bar + 1));
            barLabel.setForeground(Color.WHITE);
            barLabel.setFont(new Font("Arial", Font.BOLD, 12));
            
            int barWidth = beatsPerBar * TICKS_PER_BEAT * cellSize;
            // Add labelWidth to x position to account for left panel
            int x = labelWidth + bar * barWidth + barWidth / 2 - 5; // Center in bar
            barLabel.setBounds(x, 0, 20, 20);
            
            timeLabelsPanel.add(barLabel);
        }
        
        // Add beat numbers for each bar
        for (int bar = 0; bar < bars; bar++) {
            for (int beat = 0; beat < beatsPerBar; beat++) {
                JLabel beatLabel = new JLabel(String.valueOf(beat + 1));
                beatLabel.setForeground(Color.LIGHT_GRAY);
                beatLabel.setFont(new Font("Arial", Font.PLAIN, 10));
                
                // Add labelWidth to x position to account for left panel
                int x = labelWidth + (bar * beatsPerBar * TICKS_PER_BEAT + beat * TICKS_PER_BEAT) * cellSize + 
                        (TICKS_PER_BEAT * cellSize / 2) - 3; // Center in beat
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
        if (action.getCommand() == null) return;
        
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
                    if (player != null && action.getData() instanceof Player p && 
                            p.getId().equals(player.getId())) {
                        player = p;
                        updatePlayerGrid();
                    }
                }

                case Commands.SESSION_CHANGED -> {
                    if (player != null) {
                        updatePlayerGrid();
                    }
                }
            }
        });
    }
}
