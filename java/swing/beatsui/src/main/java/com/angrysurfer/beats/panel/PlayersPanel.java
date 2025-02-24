package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.model.IPlayer;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;
import com.angrysurfer.core.service.RedisService;
import com.angrysurfer.core.service.TickerManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayersPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(PlayersPanel.class.getName());
    private final JTable table;
    private final StatusConsumer status;
    private final RulesPanel ruleTablePanel;
    private final ButtonPanel buttonPanel;
    private final ContextMenuHelper contextMenu;

    // Column name constants
    private static final String COL_NAME = "Name";
    private static final String COL_INSTRUMENT = "Instrument";
    private static final String COL_CHANNEL = "Channel";
    private static final String COL_SWING = "Swing";
    private static final String COL_LEVEL = "Level";
    private static final String COL_NOTE = "Note";
    private static final String COL_MIN_VEL = "Min Vel";
    private static final String COL_MAX_VEL = "Max Vel";
    private static final String COL_PRESET = "Preset";
    private static final String COL_STICKY = "Sticky";
    private static final String COL_PROBABILITY = "Prob";
    private static final String COL_RANDOM = "Random";
    private static final String COL_RATCHET_COUNT = "Ratchet #";
    private static final String COL_RATCHET_INTERVAL = "Ratchet Int";
    private static final String COL_INT_BEATS = "Int Beats";
    private static final String COL_INT_BARS = "Int Bars";
    private static final String COL_PAN = "Pan";
    private static final String COL_PRESERVE = "Preserve";
    private static final String COL_SPARSE = "Sparse";

    private static final Set<String> COLUMNS = new LinkedHashSet<>(Arrays.asList(
            COL_NAME, COL_INSTRUMENT, COL_CHANNEL, COL_PRESET, COL_NOTE, COL_LEVEL,
            COL_PAN, COL_MIN_VEL, COL_MAX_VEL, COL_SWING,
            COL_PROBABILITY, COL_RANDOM, COL_SPARSE,
            COL_RATCHET_COUNT, COL_RATCHET_INTERVAL, COL_INT_BEATS,
            COL_INT_BARS, COL_STICKY, COL_PRESERVE));

    // Replace array with LinkedHashSet using constants
    // private static final Set<String> COLUMNS = new LinkedHashSet<>(Arrays.asList(
    // COL_NAME, COL_INSTRUMENT, COL_CHANNEL, COL_SWING, COL_LEVEL, COL_NOTE,
    // COL_MIN_VEL, COL_MAX_VEL, COL_PRESET, COL_STICKY, COL_PROBABILITY,
    // COL_RANDOM, COL_RATCHET_COUNT, COL_RATCHET_INTERVAL, COL_INT_BEATS,
    // COL_INT_BARS, COL_PAN, COL_PRESERVE, COL_SPARSE));

    private static final Set<String> STRING_COLUMN_NAMES = Set.of(COL_NAME, COL_INSTRUMENT);

    // Define boolean column names using constants
    private static final Set<String> BOOLEAN_COLUMN_NAMES = Set.of(
            COL_STICKY,
            COL_INT_BEATS,
            COL_INT_BARS,
            COL_PRESERVE);

    // Convert boolean column names to indices
    private static final int[] BOOLEAN_COLUMNS = BOOLEAN_COLUMN_NAMES.stream()
            .mapToInt(name -> new ArrayList<>(COLUMNS).indexOf(name))
            .toArray();

    private static final int[] STRING_COLUMNS = STRING_COLUMN_NAMES.stream()
            .mapToInt(name -> new ArrayList<>(COLUMNS).indexOf(name))
            .toArray();

    // Helper method to get columns as array when needed
    private static String[] getColumnNames() {
        return COLUMNS.toArray(new String[0]);
    }

    // Rest of column indices remain the same

    private static final int[] NUMERIC_COLUMNS = initNumericColumns();

    private static int[] initNumericColumns() {
        // Create a set of boolean column indices for quick lookup
        Set<Integer> booleanCols = Arrays.stream(BOOLEAN_COLUMNS).boxed().collect(Collectors.toSet());
        Set<Integer> stringCols = Arrays.stream(STRING_COLUMNS).boxed().collect(Collectors.toSet());

        // Create list to hold numeric column indices
        List<Integer> numericCols = new ArrayList<>();

        // Check each column except Name(0) and Instrument(1) which are strings
        for (int i = 0; i < getColumnNames().length; i++) {
            // If it's not a boolean column, it's numeric
            if (!booleanCols.contains(i) && !stringCols.contains(i)) {
                numericCols.add(i);
            }
        }

        return numericCols.stream().mapToInt(Integer::intValue).toArray();
    }

    private boolean hasActiveTicker = false; // Add this field
    private JButton controlButton; // Add this field

    public PlayersPanel(StatusConsumer status, RulesPanel ruleTablePanel) {
        super(new BorderLayout());
        this.status = status;
        this.ruleTablePanel = ruleTablePanel;
        this.table = new JTable();
        this.buttonPanel = new ButtonPanel(
                Commands.PLAYER_ADD_REQUEST,
                Commands.PLAYER_EDIT_REQUEST,
                Commands.PLAYER_DELETE_REQUEST);
        this.contextMenu = new ContextMenuHelper(
                Commands.PLAYER_ADD_REQUEST,
                Commands.PLAYER_EDIT_REQUEST,
                Commands.PLAYER_DELETE_REQUEST);

        // Always enable Add functionality
        buttonPanel.setAddEnabled(true);
        contextMenu.setAddEnabled(true);

        setupTable();
        setupLayout();
        setupKeyboardShortcuts(); // Add this line
        setupCommandBusListener();
        setupButtonListeners();
        setupContextMenu();

        // Check for active ticker and enable controls immediately
        ProxyTicker currentTicker = TickerManager.getInstance().getActiveTicker();
        if (currentTicker != null) {
            logger.info("Found active ticker on construction: " + currentTicker.getId());
            hasActiveTicker = true;
            enableControls(true);
            refreshPlayers(currentTicker.getPlayers());
        }
    }

    private void enableControls(boolean enabled) {
        logger.info("Setting controls enabled: " + enabled);
        buttonPanel.setAddEnabled(enabled);
        contextMenu.setAddEnabled(enabled);
        updateButtonStates();
    }

    private void setupLayout() {
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // Create a wrapper panel for the button panel with BorderLayout
        JPanel buttonWrapper = new JPanel(new BorderLayout());
        buttonWrapper.add(buttonPanel, BorderLayout.CENTER);
        
        // Create control button
        controlButton = new JButton("Control");
        controlButton.setEnabled(false);
        controlButton.addActionListener(e -> {
            ProxyStrike selectedPlayer = getSelectedPlayer();
            if (selectedPlayer != null) {
                CommandBus.getInstance().publish(Commands.EDIT_PLAYER_PARAMETERS, this, selectedPlayer);
            }
        });
        
        // Add control button to the right of the button panel
        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightButtonPanel.add(controlButton);
        buttonWrapper.add(rightButtonPanel, BorderLayout.EAST);
        
        topPanel.add(buttonWrapper, BorderLayout.NORTH);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void saveColumnOrder() {
        try {
            FrameState state = RedisService.getInstance().loadFrameState();
            if (state != null) {
                List<String> columnOrder = new ArrayList<>();
                // Get visible column order
                for (int i = 0; i < table.getColumnCount(); i++) {
                    int modelIndex = table.convertColumnIndexToModel(i);
                    String columnName = getColumnNames()[modelIndex];
                    columnOrder.add(columnName);
                }
                
                // Only save if we have all columns
                if (columnOrder.size() == COLUMNS.size()) {
                    logger.info("Saving column order: " + String.join(", ", columnOrder));
                    state.setPlayerColumnOrder(columnOrder);
                    RedisService.getInstance().saveFrameState(state);
                } else {
                    logger.warning("Column order incomplete, not saving");
                }
            }
        } catch (Exception e) {
            logger.severe("Error saving column order: " + e.getMessage());
        }
    }

    public void restoreColumnOrder() {
        try {
            FrameState state = RedisService.getInstance().loadFrameState();
            List<String> savedOrder = state != null ? state.getPlayerColumnOrder() : null;
            
            if (savedOrder != null && !savedOrder.isEmpty() && savedOrder.size() == COLUMNS.size()) {
                logger.info("Restoring column order: " + String.join(", ", savedOrder));
                
                // Create a map of column names to their current positions
                Map<String, Integer> currentOrder = new HashMap<>();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    currentOrder.put(getColumnNames()[i], i);
                }

                // Move each column to its saved position
                for (int i = 0; i < savedOrder.size(); i++) {
                    String colName = savedOrder.get(i);
                    Integer currentPos = currentOrder.get(colName);
                    if (currentPos != null && currentPos != i) {
                        table.getColumnModel().moveColumn(currentPos, i);
                        // Update the currentOrder map after moving
                        for (Map.Entry<String, Integer> entry : currentOrder.entrySet()) {
                            if (entry.getValue() == i) {
                                currentOrder.put(entry.getKey(), currentPos);
                                break;
                            }
                        }
                        currentOrder.put(colName, i);
                    }
                }
            } else {
                logger.info("No valid column order found to restore");
            }
        } catch (Exception e) {
            logger.severe("Error restoring column order: " + e.getMessage());
        }
    }

    private void setupTable() {

        DefaultTableModel model = new DefaultTableModel(getColumnNames(), 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                // Return Boolean.class for boolean columns
                for (int booleanColumn : BOOLEAN_COLUMNS) {
                    if (column == booleanColumn) {
                        return Boolean.class;
                    }
                }
                return Object.class; // Changed from super.getColumnClass(column)
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };

        table.setModel(model);

        // Set minimum and preferred widths for Name and Instrument columns
        table.getColumnModel().getColumn(0).setMinWidth(100); // Name column min
        table.getColumnModel().getColumn(1).setMinWidth(100); // Instrument column min

        // Set relative widths for columns to control resize behavior
        table.getColumnModel().getColumn(0).setPreferredWidth(200); // Name gets more space
        table.getColumnModel().getColumn(1).setPreferredWidth(150); // Instrument gets less space

        // Set fixed widths for other columns to prevent them from growing
        for (int i = 2; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setMaxWidth(80);
            table.getColumnModel().getColumn(i).setPreferredWidth(60);
        }

        // Configure table appearance
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS); // Changed from default
        table.setAutoCreateRowSorter(true);

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int numericColumn : NUMERIC_COLUMNS) {
            table.getColumnModel().getColumn(numericColumn).setCellRenderer(centerRenderer);
        }

        // Set default checkbox renderer for Boolean columns
        for (int booleanColumn : BOOLEAN_COLUMNS) {
            table.getColumnModel().getColumn(booleanColumn).setCellRenderer(
                    new DefaultTableCellRenderer() {
                        private final JCheckBox checkbox = new JCheckBox();
                        {
                            checkbox.setHorizontalAlignment(JCheckBox.CENTER);
                        }

                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value,
                                boolean isSelected, boolean hasFocus, int row, int column) {
                            if (value instanceof Boolean) {
                                checkbox.setSelected((Boolean) value);
                                checkbox.setBackground(isSelected ? table.getSelectionBackground()
                                        : table.getBackground());
                                return checkbox;
                            }
                            return super.getTableCellRendererComponent(table, value, isSelected,
                                    hasFocus, row, column);
                        }
                    });
        }

        // Add left alignment for Name and Instrument column headers
        DefaultTableCellRenderer leftHeaderRenderer = new DefaultTableCellRenderer();
        leftHeaderRenderer.setHorizontalAlignment(JLabel.LEFT);
        
        table.getTableHeader().getColumnModel().getColumn(getColumnIndex(COL_NAME))
            .setHeaderRenderer(leftHeaderRenderer);
        table.getTableHeader().getColumnModel().getColumn(getColumnIndex(COL_INSTRUMENT))
            .setHeaderRenderer(leftHeaderRenderer);

        // Add double-click listener
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    ProxyStrike selectedPlayer = getSelectedPlayer();
                    if (selectedPlayer != null) {
                        CommandBus.getInstance().publish(Commands.PLAYER_EDIT_REQUEST, this, selectedPlayer);
                    }
                }
            }
        });

        // Add column reordering listener
        table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnMoved(TableColumnModelEvent e) {
                if (e.getFromIndex() != e.getToIndex()) {
                    logger.info("Column moved from " + e.getFromIndex() + " to " + e.getToIndex());
                    // Add slight delay to ensure column move is complete
                    SwingUtilities.invokeLater(() -> saveColumnOrder());
                }
            }
            // Implement other required methods with empty bodies
            public void columnAdded(TableColumnModelEvent e) {}
            public void columnRemoved(TableColumnModelEvent e) {}
            public void columnMarginChanged(ChangeEvent e) {}
            public void columnSelectionChanged(ListSelectionEvent e) {}
        });

        // Save initial column order
        SwingUtilities.invokeLater(() -> saveColumnOrder());
        
        // Restore column order after table is fully set up
        SwingUtilities.invokeLater(() -> restoreColumnOrder());
    }

    private void setupButtonListeners() {
        buttonPanel.addActionListener(e -> {
            logger.info("Button clicked: " + e.getActionCommand());
            switch (e.getActionCommand()) {
                case Commands.PLAYER_ADD_REQUEST -> {
                    CommandBus.getInstance().publish(Commands.PLAYER_ADD_REQUEST, this, null);
                }
                case Commands.PLAYER_EDIT_REQUEST -> {
                    // Get directly selected player (no array)
                    ProxyStrike selectedPlayer = getSelectedPlayer();
                    if (selectedPlayer != null) {
                        CommandBus.getInstance().publish(Commands.PLAYER_EDIT_REQUEST, this, selectedPlayer);
                    }
                }
                case Commands.PLAYER_DELETE_REQUEST -> {
                    ProxyStrike[] selectedPlayers = getSelectedPlayers();
                    if (selectedPlayers.length > 0) {
                        CommandBus.getInstance().publish(e.getActionCommand(), this, selectedPlayers);
                    }
                }
            }
        });
    }

    private void setupContextMenu() {
        contextMenu.install(table);
        
        // Add separator and Controls menu item
        contextMenu.addSeparator();
        contextMenu.addMenuItem("Control", e -> {
            ProxyStrike selectedPlayer = getSelectedPlayer();
            if (selectedPlayer != null) {
                CommandBus.getInstance().publish(Commands.EDIT_PLAYER_PARAMETERS, this, selectedPlayer);
            }
        });

        contextMenu.addActionListener(e -> {
            String command = e.getActionCommand();
            logger.info("Context menu command: " + command);

            // Always allow Add command
            if (command.equals(Commands.PLAYER_ADD_REQUEST)) {
                CommandBus.getInstance().publish(command, this, null);
                return;
            }

            // Only check selection for Edit/Delete
            ProxyStrike selectedPlayer = getSelectedPlayer();
            if (selectedPlayer != null) {
                CommandBus.getInstance().publish(command, this, selectedPlayer);
            }
        });
    }

    private void updateButtonStates() {
        boolean hasSelection = table.getSelectedRow() >= 0;

        // Update existing button states
        buttonPanel.setEditEnabled(hasSelection);
        buttonPanel.setDeleteEnabled(hasSelection);
        contextMenu.setEditEnabled(hasSelection);
        contextMenu.setDeleteEnabled(hasSelection);
        
        // Update control button state
        controlButton.setEnabled(hasSelection);
    }

    private void setupCommandBusListener() {
        CommandBus.getInstance().register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                switch (action.getCommand()) {
                    case Commands.TICKER_SELECTED, Commands.TICKER_LOADED -> {
                        if (action.getData() instanceof ProxyTicker ticker) {
                            hasActiveTicker = true;
                            enableControls(true);
                            refreshPlayers(ticker.getPlayers());

                            // Auto-select first player if available
                            if (!ticker.getPlayers().isEmpty()) {
                                table.setRowSelectionInterval(0, 0);
                                ProxyStrike firstPlayer = (ProxyStrike) ticker.getPlayers().iterator().next();
                                CommandBus.getInstance().publish(Commands.PLAYER_SELECTED, this, firstPlayer);
                            }
                        }
                    }
                    case Commands.TICKER_UPDATED -> {
                        if (action.getData() instanceof ProxyTicker ticker) {
                            refreshPlayers(ticker.getPlayers());

                            // Auto-select the newly added player if it was an add operation
                            if (action.getSender() instanceof PlayerEditPanel) {
                                selectLastPlayer();
                            }
                        }
                    }
                    case Commands.TICKER_UNSELECTED -> {
                        hasActiveTicker = false;
                        enableControls(false);
                        refreshPlayers(null);
                    }
                    case Commands.PLAYER_EDIT_CANCELLED -> {
                        if (action.getData() instanceof ProxyTicker ticker) {
                            refreshPlayers(ticker.getPlayers());
                        }
                    }
                    case Commands.PLAYER_DELETE_REQUEST -> {
                        if (action.getData() instanceof ProxyStrike player) {
                            ProxyTicker currentTicker = TickerManager.getInstance().getActiveTicker();
                            if (currentTicker != null) {
                                CommandBus.getInstance().publish(Commands.PLAYER_UNSELECTED, this);
                                refreshPlayers(currentTicker.getPlayers());
                            }
                        }
                    }
                    case Commands.PLAYER_ROW_INDEX_REQUEST -> {
                        if (action.getData() instanceof ProxyStrike requestedPlayer) {
                            int selectedRow = table.getSelectedRow();
                            CommandBus.getInstance().publish(
                                    Commands.PLAYER_ROW_INDEX_RESPONSE,
                                    this,
                                    selectedRow);
                        }
                    }
                    case Commands.PLAYER_UPDATED -> {
                        if (action.getData() instanceof ProxyStrike player) {
                            updatePlayerRow(player);
                        }
                    }
                    case Commands.TICKER_DELETED -> {
                        // Clear current display and disable controls if no new ticker is active
                        ProxyTicker currentTicker = TickerManager.getInstance().getActiveTicker();
                        if (currentTicker == null) {
                            hasActiveTicker = false;
                            enableControls(false);
                            refreshPlayers(null);
                        } else {
                            // Update with new active ticker's data
                            hasActiveTicker = true;
                            enableControls(true);
                            refreshPlayers(currentTicker.getPlayers());
                        }
                    }
                    case Commands.WINDOW_CLOSING -> {
                        saveColumnOrder();
                    }
                }
            }
        });

        // Combine selection listeners into one
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                boolean hasSelection = selectedRow >= 0;

                // Update edit/delete buttons
                buttonPanel.setEditEnabled(hasSelection);
                buttonPanel.setDeleteEnabled(hasSelection);
                contextMenu.setEditEnabled(hasSelection);
                contextMenu.setDeleteEnabled(hasSelection);

                // Notify about player selection/deselection
                if (hasSelection) {
                    ProxyStrike selectedPlayer = getSelectedPlayer();
                    if (selectedPlayer != null) {
                        CommandBus.getInstance().publish(Commands.PLAYER_SELECTED, this, selectedPlayer);
                    }
                } else {
                    CommandBus.getInstance().publish(Commands.PLAYER_UNSELECTED, this);
                }
            }
        });
    }

    private void setupKeyboardShortcuts() {
        // Make the table focusable
        table.setFocusable(true);

        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    handleDelete();
                }
            }
        });
    }

    private void handleDelete() {
        ProxyStrike[] selectedPlayers = getSelectedPlayers();
        if (selectedPlayers.length > 0) {
            int confirm = JOptionPane.showConfirmDialog(
                    PlayersPanel.this,
                    "Are you sure you want to delete the selected player(s)?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                CommandBus.getInstance().publish(
                        Commands.PLAYER_DELETE_REQUEST,
                        PlayersPanel.this,
                        selectedPlayers);
            }
        }
    }

    private void selectLastPlayer() {
        if (table.getRowCount() > 0) {
            int lastRow = table.getRowCount() - 1;
            table.setRowSelectionInterval(lastRow, lastRow);
            ProxyStrike selectedPlayer = getSelectedPlayer();
            if (selectedPlayer != null) {
                CommandBus.getInstance().publish(Commands.PLAYER_SELECTED, this, selectedPlayer);
            }
        }
    }

    private int findPlayerRowIndex(ProxyStrike player) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String playerName = (String) model.getValueAt(i, 0);
            if (playerName.equals(player.getName())) {
                return i;
            }
        }
        return -1;
    }

    private ProxyStrike getSelectedPlayer() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            int modelRow = table.convertRowIndexToModel(row);
            String playerName = (String) table.getModel().getValueAt(modelRow, 0);
            ProxyTicker currentTicker = TickerManager.getInstance().getActiveTicker();
            if (currentTicker != null) {
                return (ProxyStrike) currentTicker.getPlayers().stream()
                        .filter(p -> p.getName().equals(playerName))
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    private ProxyStrike[] getSelectedPlayers() {
        int[] selectedRows = table.getSelectedRows();
        List<ProxyStrike> players = new ArrayList<>();
        ProxyTicker currentTicker = TickerManager.getInstance().getActiveTicker();

        if (currentTicker != null) {
            for (int row : selectedRows) {
                int modelRow = table.convertRowIndexToModel(row);
                String playerName = (String) table.getModel().getValueAt(modelRow, 0);
                currentTicker.getPlayers().stream()
                        .filter(p -> p.getName().equals(playerName))
                        .findFirst()
                        .ifPresent(p -> players.add((ProxyStrike) p));
            }
        }
        return players.toArray(new ProxyStrike[0]);
    }

    // Column indices for reference:
    // 0 - Name
    // 1 - Instrument
    // 2 - Channel
    // 3 - Swing
    // 4 - Level
    // 5 - Note
    // 6 - Min Vel
    // 7 - Max Vel
    // 8 - Preset
    // 9 - Sticky (Boolean)
    // 10 - Prob
    // 11 - Random
    // 12 - Ratchet #
    // 13 - Ratchet Int
    // 14 - Int Beats (Boolean)
    // 15 - Int Bars (Boolean)
    // 16 - Pan
    // 17 - Preserve (Boolean)
    // 18 - Sparse

    // Add this new method
    private int getColumnIndex(String columnName) {
        return new ArrayList<>(COLUMNS).indexOf(columnName);
    }

    public void refreshPlayers(Set<IPlayer> players) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        if (players != null) {
            List<IPlayer> sortedPlayers = new ArrayList<>(players);
            Collections.sort(sortedPlayers, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

            for (IPlayer p : sortedPlayers) {
                ProxyStrike player = (ProxyStrike) p;
                Object[] newRowData = new Object[COLUMNS.size()];

                // Map each property to its correct column position
                newRowData[getColumnIndex(COL_NAME)] = player.getName();
                newRowData[getColumnIndex(COL_INSTRUMENT)] = player.getInstrument() != null
                        ? player.getInstrument().getName()
                        : "";
                newRowData[getColumnIndex(COL_CHANNEL)] = player.getChannel();
                newRowData[getColumnIndex(COL_SWING)] = player.getSwing();
                newRowData[getColumnIndex(COL_LEVEL)] = player.getLevel();
                newRowData[getColumnIndex(COL_NOTE)] = player.getNote();
                newRowData[getColumnIndex(COL_MIN_VEL)] = player.getMinVelocity();
                newRowData[getColumnIndex(COL_MAX_VEL)] = player.getMaxVelocity();
                newRowData[getColumnIndex(COL_PRESET)] = player.getPreset();
                newRowData[getColumnIndex(COL_STICKY)] = false; // player.isSticky();
                newRowData[getColumnIndex(COL_PROBABILITY)] = player.getProbability();
                newRowData[getColumnIndex(COL_RANDOM)] = player.getRandomDegree();
                newRowData[getColumnIndex(COL_RATCHET_COUNT)] = player.getRatchetCount();
                newRowData[getColumnIndex(COL_RATCHET_INTERVAL)] = player.getRatchetInterval();
                newRowData[getColumnIndex(COL_INT_BEATS)] = false; // player.isIntervalBeats();
                newRowData[getColumnIndex(COL_INT_BARS)] = false; // player.isIntervalBars();
                newRowData[getColumnIndex(COL_PAN)] = player.getPanPosition();
                newRowData[getColumnIndex(COL_PRESERVE)] = false; // player.isPreserve();
                newRowData[getColumnIndex(COL_SPARSE)] = player.getSparse();

                model.addRow(newRowData);
            }
        }
    }

    private void updatePlayerRow(ProxyStrike player) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int rowIndex = findPlayerRowIndex(player);

        if (rowIndex != -1) {
            // Update each column according to its current position
            model.setValueAt(player.getName(), rowIndex, getColumnIndex(COL_NAME));
            model.setValueAt(player.getInstrument() != null ? player.getInstrument().getName() : "",
                    rowIndex, getColumnIndex(COL_INSTRUMENT));
            model.setValueAt(player.getChannel(), rowIndex, getColumnIndex(COL_CHANNEL));
            model.setValueAt(player.getSwing(), rowIndex, getColumnIndex(COL_SWING));
            model.setValueAt(player.getLevel(), rowIndex, getColumnIndex(COL_LEVEL));
            model.setValueAt(player.getNote(), rowIndex, getColumnIndex(COL_NOTE));
            model.setValueAt(player.getMinVelocity(), rowIndex, getColumnIndex(COL_MIN_VEL));
            model.setValueAt(player.getMaxVelocity(), rowIndex, getColumnIndex(COL_MAX_VEL));
            model.setValueAt(player.getPreset(), rowIndex, getColumnIndex(COL_PRESET));
            // model.setValueAt(player.isSticky(), rowIndex, getColumnIndex(COL_STICKY));
            model.setValueAt(player.getProbability(), rowIndex, getColumnIndex(COL_PROBABILITY));
            model.setValueAt(player.getRandomDegree(), rowIndex, getColumnIndex(COL_RANDOM));
            model.setValueAt(player.getRatchetCount(), rowIndex, getColumnIndex(COL_RATCHET_COUNT));
            model.setValueAt(player.getRatchetInterval(), rowIndex, getColumnIndex(COL_RATCHET_INTERVAL));
            // model.setValueAt(player.isIntervalBeats(), rowIndex,
            // getColumnIndex(COL_INT_BEATS));
            // model.setValueAt(player.isIntervalBars(), rowIndex,
            // getColumnIndex(COL_INT_BARS));
            model.setValueAt(player.getPanPosition(), rowIndex, getColumnIndex(COL_PAN));
            // model.setValueAt(player.isPreserve(), rowIndex,
            // getColumnIndex(COL_PRESERVE));
            model.setValueAt(player.getSparse(), rowIndex, getColumnIndex(COL_SPARSE));
        }
    }
}
