package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
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

import com.angrysurfer.beats.service.UIHelper;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.BusListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.util.Constants;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayersPanel extends JPanel {

    private static final Logger logger = Logger.getLogger(PlayersPanel.class.getName());

    private final JTable table;
    private final StatusConsumer status;
    private final ButtonPanel buttonPanel;
    private final ContextMenuHelper contextMenu;

    // Column name constants
    private static final String COL_NAME = "Name";
    private static final String COL_INSTRUMENT = "Instrument";
    private static final String COL_CHANNEL = "Channel";
    private static final String COL_SWING = "Swing";
    private static final String COL_LEVEL = "Level";
    // private static final String COL_MUTE = "Mute"; // Add this column name
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
            COL_NAME, COL_INSTRUMENT, COL_CHANNEL, COL_PRESET, COL_NOTE, COL_LEVEL, // COL_MUTE,
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
            // COL_MUTE,
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

    private boolean hasActiveSession = false; // Add this field
    private JButton controlButton; // Add this field

    public PlayersPanel(StatusConsumer status) {
        super(new BorderLayout());
        this.status = status;
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
        setupTableSelectionListener(); // Add this line

        // Check for active session and enable controls immediately
        Session currentSession = SessionManager.getInstance().getActiveSession();
        if (currentSession != null) {
            logger.info("Found active session on construction: " + currentSession.getId());
            hasActiveSession = true;
            enableControls(true);
            refreshPlayers(currentSession.getPlayers());
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
            Player selectedPlayer = getSelectedPlayer();
            if (selectedPlayer != null && selectedPlayer.getInstrument() != null
                    && !selectedPlayer.getInstrument().getControlCodes().isEmpty()) {
                CommandBus.getInstance().publish(Commands.EDIT_PLAYER_PARAMETERS, this, selectedPlayer);
            }
        });

        // Add control button to the right of the button panel
        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightButtonPanel.add(controlButton);
        buttonWrapper.add(rightButtonPanel, BorderLayout.EAST);

        // Add to setupLayout method
        JButton debugButton = new JButton("Force Selection Event");
        debugButton.addActionListener(e -> {
            Player player = getSelectedPlayer();
            if (player != null) {
                logger.info("Forcing PLAYER_SELECTED event for: " + player.getName());

                // First set the active player in PlayerManager
                PlayerManager.getInstance().setActivePlayer(player);

                // Then publish the selection event
                CommandBus.getInstance().publish(Commands.PLAYER_SELECTED, this, player);

                // Update button states
                updateButtonStates();
            } else {
                logger.warning("No player selected to force event");
            }
        });
        rightButtonPanel.add(debugButton);

        topPanel.add(buttonWrapper, BorderLayout.NORTH);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
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
                    Player selectedPlayer = getSelectedPlayer();
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
                    SwingUtilities.invokeLater(
                            () -> UIHelper.getInstance().saveColumnOrder(table, Constants.PLAYER, COLUMNS));
                }
            }

            // Implement other required methods with empty bodies
            public void columnAdded(TableColumnModelEvent e) {
            }

            public void columnRemoved(TableColumnModelEvent e) {
            }

            public void columnMarginChanged(ChangeEvent e) {
            }

            public void columnSelectionChanged(ListSelectionEvent e) {
            }
        });

        // Save initial column order
        SwingUtilities.invokeLater(() -> UIHelper.getInstance().saveColumnOrder(table, Constants.PLAYER, COLUMNS));

        // Restore column order after table is fully set up
        SwingUtilities.invokeLater(() -> UIHelper.getInstance().restoreColumnOrder(table, Constants.PLAYER, COLUMNS));

        // Replace both selection listeners with this single implementation in
        // setupTable()
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                boolean hasSelection = selectedRow >= 0;

                // Update button states
                updateButtonStates();

                // Handle player selection with a single approach
                if (hasSelection) {
                    Player selectedPlayer = getSelectedPlayer();
                    if (selectedPlayer != null) {
                        logger.info("PlayersPanel: Selected player " + selectedPlayer.getName() +
                                " (ID: " + selectedPlayer.getId() + ")");

                        // 1. Update PlayerManager first (source of truth)
                        PlayerManager.getInstance().setActivePlayer(selectedPlayer);

                        // 2. Then publish the event for other components
                        CommandBus.getInstance().publish(Commands.PLAYER_SELECTED, this, selectedPlayer);
                    }
                } else {
                    logger.info("PlayersPanel: No player selected");

                    // Clear active player in PlayerManager
                    PlayerManager.getInstance().setActivePlayer(null);

                    // Notify other components
                    CommandBus.getInstance().publish(Commands.PLAYER_UNSELECTED, this);
                }
            }
        });

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
                    Player selectedPlayer = getSelectedPlayer();
                    if (selectedPlayer != null) {
                        CommandBus.getInstance().publish(Commands.PLAYER_EDIT_REQUEST, this, selectedPlayer);
                    }
                }
                case Commands.PLAYER_DELETE_REQUEST -> {
                    Player[] selectedPlayers = getSelectedPlayers();
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
            Player selectedPlayer = getSelectedPlayer();
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
            Player selectedPlayer = getSelectedPlayer();
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
        CommandBus.getInstance().register(new BusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                String cmd = action.getCommand();
                try {
                    switch (cmd) {
                        case Commands.SESSION_SELECTED, Commands.SESSION_LOADED -> {
                            if (action.getData() instanceof Session session) {
                                refreshPlayers(session.getPlayers());
                            }
                        }

                        // Handle player row refresh requests
                        case Commands.PLAYER_ROW_REFRESH -> {
                            if (action.getData() instanceof Player player) {
                                logger.info("Refreshing player row: " + player.getName());
                                updatePlayerRow(player);
                            }
                        }

                        // Handle individual property changes
                        case Commands.NEW_VALUE_LEVEL, Commands.NEW_VALUE_NOTE,
                                Commands.NEW_VALUE_SWING, Commands.NEW_VALUE_PROBABILITY,
                                Commands.NEW_VALUE_VELOCITY_MIN, Commands.NEW_VALUE_VELOCITY_MAX,
                                Commands.NEW_VALUE_RANDOM, Commands.NEW_VALUE_PAN,
                                Commands.NEW_VALUE_SPARSE, Commands.PLAYER_UPDATED -> {

                            // For these commands, the sender is the player that was updated
                            if (action.getSender() instanceof Player player) {
                                logger.info("Updating player row from " + cmd + ": " + player.getName());
                                updatePlayerRow(player);
                            }
                        }

                        // Handle player operations completed
                        case Commands.PLAYER_ADDED -> {
                            logger.info("Player added, refreshing table");
                            // Refresh table with current session players
                            Session session = SessionManager.getInstance().getActiveSession();
                            if (session != null) {
                                refreshPlayers(session.getPlayers());
                                
                                // Select the newly added player if available in data
                                if (action.getData() instanceof Player player) {
                                    selectPlayerByName(player.getName());
                                } else {
                                    // If no specific player, select the last one
                                    selectLastPlayer();
                                }
                            }
                        }
                        
                        case Commands.SHOW_PLAYER_EDITOR_OK -> {
                            logger.info("Player edited, refreshing table");
                            Session session = SessionManager.getInstance().getActiveSession();
                            if (session != null) {
                                refreshPlayers(session.getPlayers());
                                
                                // Reselect the edited player
                                if (action.getData() instanceof Player player) {
                                    selectPlayerByName(player.getName());
                                }
                            }
                        }
                        
                        case Commands.PLAYER_DELETED -> {
                            logger.info("Player(s) deleted, refreshing table");
                            Session session = SessionManager.getInstance().getActiveSession();
                            if (session != null) {
                                refreshPlayers(session.getPlayers());
                                
                                // Select closest available player or clear selection
                                if (table.getRowCount() > 0) {
                                    // Try to select same row index if possible
                                    int rowToSelect = Math.min(lastSelectedRow, table.getRowCount() - 1);
                                    if (rowToSelect >= 0) {
                                        table.setRowSelectionInterval(rowToSelect, rowToSelect);
                                        handlePlayerSelection(rowToSelect);
                                    }
                                } else {
                                    // No players left
                                    CommandBus.getInstance().publish(Commands.PLAYER_UNSELECTED, this);
                                }
                            }
                        }

                        // Other cases...
                    }
                } catch (Exception e) {
                    logger.severe("Error processing command: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // Add a field to track the last selected row
    private int lastSelectedRow = -1;

    // Add a helper method to select a player by name
    private void selectPlayerByName(String playerName) {
        if (playerName == null) return;
        
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int nameColIndex = getColumnIndex(COL_NAME);
        
        for (int i = 0; i < model.getRowCount(); i++) {
            String name = (String) model.getValueAt(i, nameColIndex);
            if (playerName.equals(name)) {
                table.setRowSelectionInterval(i, i);
                lastSelectedRow = i;
                handlePlayerSelection(i);
                // Ensure the row is visible
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                return;
            }
        }
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
        Player[] selectedPlayers = getSelectedPlayers();
        if (selectedPlayers.length > 0) {
            int confirm = JOptionPane.showConfirmDialog(
                    PlayersPanel.this,
                    "Are you sure you want to delete the selected player(s)?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                // Send just the selected players array
                CommandBus.getInstance().publish(Commands.PLAYER_DELETE_REQUEST, this, selectedPlayers);
                logger.info("Sent delete request for " + selectedPlayers.length + " players");
            }
        }
    }

    private void selectLastPlayer() {
        if (table.getRowCount() > 0) {
            int lastRow = table.getRowCount() - 1;
            table.setRowSelectionInterval(lastRow, lastRow);
            Player selectedPlayer = getSelectedPlayer();
            if (selectedPlayer != null) {
                CommandBus.getInstance().publish(Commands.PLAYER_SELECTED, this, selectedPlayer);
            }
        }
    }

    /**
     * Find the row index for a player in the table
     * 
     * @param player Player to locate
     * @return Row index or -1 if not found
     */
    private int findPlayerRowIndex(Player player) {
        if (player == null)
            return -1;

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int nameColIndex = getColumnIndex(COL_NAME);

        // Search by name and then verify by ID
        for (int i = 0; i < model.getRowCount(); i++) {
            String playerName = (String) model.getValueAt(i, nameColIndex);
            if (player.getName().equals(playerName)) {
                return i;
            }
        }

        return -1; // Not found
    }

    private Player getSelectedPlayer() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            logger.info("No row selected in players table");
            return null;
        }

        try {
            // Convert view index to model index in case of sorting/filtering
            int modelRow = table.convertRowIndexToModel(selectedRow);

            // Get the player name from the Name column
            String playerName = (String) table.getModel().getValueAt(modelRow, getColumnIndex(COL_NAME));
            logger.info("Selected player name from table: " + playerName);

            // Get the current session
            Session currentSession = SessionManager.getInstance().getActiveSession();

            if (currentSession != null && currentSession.getPlayers() != null) {
                // Find the player with the matching name
                for (Player player : currentSession.getPlayers()) {
                    if (playerName.equals(player.getName())) {
                        logger.info("Found matching player in session: " + player.getName() +
                                " (ID: " + player.getId() + ")");
                        return player;
                    }
                }
                logger.warning("No player found with name: " + playerName);
            } else {
                logger.warning("No active session or no players in session");
            }
        } catch (Exception e) {
            logger.severe("Error getting selected player: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private Player[] getSelectedPlayers() {
        int[] selectedRows = table.getSelectedRows();
        List<Player> players = new ArrayList<>();
        Session currentSession = SessionManager.getInstance().getActiveSession();

        if (currentSession != null) {
            for (int row : selectedRows) {
                int modelRow = table.convertRowIndexToModel(row);
                String playerName = (String) table.getModel().getValueAt(modelRow, 0);
                currentSession.getPlayers().stream()
                        .filter(p -> p.getName().equals(playerName))
                        .findFirst()
                        .ifPresent(p -> players.add(p));
            }
        }
        return players.toArray(new Strike[0]);
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

    // Update the method signature to accept a Vector instead of Object[]
    private void updateInstrumentCell(Vector rowData, int columnIndex, Player player) {
        String instrumentName = "";

        try {
            if (player.getInstrumentId() != null) {
                Instrument instrument = InstrumentManager.getInstance()
                        .getInstrumentFromCache(player.getInstrumentId());
                if (instrument != null) {
                    instrumentName = instrument.getName();

                    // Initialize device if needed
                    if (Objects.isNull(instrument.getDevice()) && Objects.nonNull(instrument.getDeviceName())) {
                        try {
                            // Get device from DeviceManager
                            javax.sound.midi.MidiDevice device = DeviceManager
                                    .getMidiDevice(instrument.getDeviceName());

                            if (device != null) {
                                // Set device on instrument
                                instrument.setDevice(device);
                                logger.info("Initialized device for instrument: " + instrument.getName() +
                                        " with device: " + device.getDeviceInfo().getName());
                            } else {
                                logger.warning("Device not found: " + instrument.getDeviceName() +
                                        " for instrument: " + instrument.getName());
                            }
                        } catch (Exception e) {
                            logger.warning("Error initializing device for instrument " +
                                    instrument.getName() + ": " + e.getMessage());
                        }
                    }

                    // Set the instrument on the player
                    player.setInstrument(instrument);
                }
            }
            // Use Vector.set() instead of array assignment
            rowData.set(columnIndex, instrumentName);
        } catch (Exception e) {
            logger.severe("Error updating instrument cell: " + e.getMessage());
            // Use Vector.set() instead of array assignment
            rowData.set(columnIndex, "Error");
        }
    }

    // Add this new overloaded method for Object[] arrays
    private void updateInstrumentCell(Object[] rowData, int columnIndex, Player player) {
        String instrumentName = "";

        try {
            if (player.getInstrumentId() != null) {
                Instrument instrument = InstrumentManager.getInstance()
                        .getInstrumentFromCache(player.getInstrumentId());
                if (instrument != null) {
                    instrumentName = instrument.getName();

                    // Initialize device if needed
                    if (Objects.isNull(instrument.getDevice()) && Objects.nonNull(instrument.getDeviceName())) {
                        try {
                            // Get device from DeviceManager
                            javax.sound.midi.MidiDevice device = DeviceManager
                                    .getMidiDevice(instrument.getDeviceName());

                            if (device != null) {
                                // Set device on instrument
                                instrument.setDevice(device);
                                logger.info("Initialized device for instrument: " + instrument.getName() +
                                        " with device: " + device.getDeviceInfo().getName());
                            } else {
                                logger.warning("Device not found: " + instrument.getDeviceName() +
                                        " for instrument: " + instrument.getName());
                            }
                        } catch (Exception e) {
                            logger.warning("Error initializing device for instrument " +
                                    instrument.getName() + ": " + e.getMessage());
                        }
                    }

                    // Set the instrument on the player
                    player.setInstrument(instrument);
                }
            }
            // Use array assignment for Object[] arrays
            rowData[columnIndex] = instrumentName;
        } catch (Exception e) {
            logger.severe("Error updating instrument cell: " + e.getMessage());
            rowData[columnIndex] = "Error";
        }
    }

    public void refreshPlayers(Set<Player> players) {
        logger.info("Refreshing players table with " + (players != null ? players.size() : 0) + " players");
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        if (players != null && !players.isEmpty()) {
            List<Player> sortedPlayers = new ArrayList<>(players);
            Collections.sort(sortedPlayers, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

            for (Player p : sortedPlayers) {
                Player player = p;
                logger.info("Adding player to table: " + player.getName() + " (ID: " + player.getId() + ")");
                Object[] newRowData = new Object[COLUMNS.size()];

                // Map each property to its correct column position
                newRowData[getColumnIndex(COL_NAME)] = player.getName();
                updateInstrumentCell(newRowData, getColumnIndex(COL_INSTRUMENT), player);

                newRowData[getColumnIndex(COL_CHANNEL)] = player.getChannel();
                newRowData[getColumnIndex(COL_SWING)] = player.getSwing();
                newRowData[getColumnIndex(COL_LEVEL)] = player.getLevel();
                // newRowData[getColumnIndex(COL_MUTE)] = player.isMuted();
                newRowData[getColumnIndex(COL_NOTE)] = player.getNote();
                newRowData[getColumnIndex(COL_MIN_VEL)] = player.getMinVelocity();
                newRowData[getColumnIndex(COL_MAX_VEL)] = player.getMaxVelocity();
                newRowData[getColumnIndex(COL_PRESET)] = player.getPreset();
                newRowData[getColumnIndex(COL_STICKY)] = player.getStickyPreset();
                newRowData[getColumnIndex(COL_PROBABILITY)] = player.getProbability();
                newRowData[getColumnIndex(COL_RANDOM)] = player.getRandomDegree();
                newRowData[getColumnIndex(COL_RATCHET_COUNT)] = player.getRatchetCount();
                newRowData[getColumnIndex(COL_RATCHET_INTERVAL)] = player.getRatchetInterval();
                newRowData[getColumnIndex(COL_INT_BEATS)] = player.getInternalBeats();
                newRowData[getColumnIndex(COL_INT_BARS)] = player.getInternalBars();
                newRowData[getColumnIndex(COL_PAN)] = player.getPanPosition();
                newRowData[getColumnIndex(COL_PRESERVE)] = player.getPreserveOnPurge();
                newRowData[getColumnIndex(COL_SPARSE)] = player.getSparse();

                model.addRow(newRowData);
            }
        }
    }

    /**
     * Update a single player row in the table
     * 
     * @param player The player to update
     */
    private void updatePlayerRow(Player player) {
        if (player == null)
            return;

        try {
            // Find row index for this player
            int rowIndex = findPlayerRowIndex(player);
            if (rowIndex == -1) {
                logger.warning("Player not found in table: " + player.getName());
                return;
            }

            // Get the model
            DefaultTableModel model = (DefaultTableModel) table.getModel();

            // Update all cells in the row
            int modelRow = table.convertRowIndexToModel(rowIndex);

            // Update each column with fresh data
            model.setValueAt(player.getName(), modelRow, getColumnIndex(COL_NAME));
            model.setValueAt(player.getNote(), modelRow, getColumnIndex(COL_NOTE));
            model.setValueAt(player.getLevel(), modelRow, getColumnIndex(COL_LEVEL));
            // model.setValueAt(player.isMuted(), modelRow, getColumnIndex(COL_MUTE));
            model.setValueAt(player.getProbability(), modelRow, getColumnIndex(COL_PROBABILITY));
            model.setValueAt(player.getSparse(), modelRow, getColumnIndex(COL_SPARSE));
            model.setValueAt(player.getSwing(), modelRow, getColumnIndex(COL_SWING));
            model.setValueAt(player.getRandomDegree(), modelRow, getColumnIndex(COL_RANDOM));
            model.setValueAt(player.getMinVelocity(), modelRow, getColumnIndex(COL_MIN_VEL));
            model.setValueAt(player.getMaxVelocity(), modelRow, getColumnIndex(COL_MAX_VEL));
            model.setValueAt(player.getPreset(), modelRow, getColumnIndex(COL_PRESET));
            model.setValueAt(player.getPanPosition(), modelRow, getColumnIndex(COL_PAN));

            // Special handling for instrument column
            updateInstrumentCell(model.getDataVector().get(modelRow), getColumnIndex(COL_INSTRUMENT), player);

            // Notify the model that data has changed
            model.fireTableRowsUpdated(modelRow, modelRow);

            logger.info("Updated row " + rowIndex + " for player: " + player.getName());
        } catch (Exception e) {
            logger.severe("Error updating player row: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePlayerSelection(int row) {
        if (row >= 0) {
            lastSelectedRow = row;
        }
        
        try {
            Player player = null;

            if (row >= 0) {
                // Convert row index to model index if table is sorted
                int modelRow = table.convertRowIndexToModel(row);
                // Get player name from the first column
                String playerName = (String) table.getModel().getValueAt(modelRow, getColumnIndex(COL_NAME));

                // Find player in the current session
                Session session = SessionManager.getInstance().getActiveSession();
                if (session != null && session.getPlayers() != null) {
                    for (Player p : session.getPlayers()) {
                        if (p.getName().equals(playerName)) {
                            player = p;
                            break;
                        }
                    }
                }
            }

            // Log selection for debugging
            logger.info("Player selection changed: "
                    + (player != null ? player.getName() + " (ID: " + player.getId() + ")" : "null"));

            // First update PlayerManager's state directly
            PlayerManager.getInstance().setActivePlayer(player);

            // Then explicitly publish events - CRITICAL STEP!
            if (player != null) {
                logger.info("Publishing PLAYER_SELECTED event for: " + player.getName());
                CommandBus.getInstance().publish(Commands.PLAYER_SELECTED, this, player);
            } else {
                logger.info("Publishing PLAYER_UNSELECTED event");
                CommandBus.getInstance().publish(Commands.PLAYER_UNSELECTED, this, null);
            }
        } catch (Exception ex) {
            logger.severe("Error in player selection: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // In PlayersPanel, find the table selection listener method (likely called
    // setupTableSelectionListener)
    private void setupTableSelectionListener() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Only handle when selection is complete
                int selectedRow = table.getSelectedRow();
                handlePlayerSelection(selectedRow);
                updateButtonStates();
            }
        });
    }
}
