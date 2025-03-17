package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beats.ColorUtils;
import com.angrysurfer.beats.model.PlayersTableModel;
import com.angrysurfer.beats.renderer.PlayerRowRenderer;
import com.angrysurfer.beats.service.UIHelper;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
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
    private final PlayersTableModel tableModel;

    // Use the model's column arrays instead of redefining them
    private static final int[] BOOLEAN_COLUMNS = PlayersTableModel.getBooleanColumns();
    private static final int[] NUMERIC_COLUMNS = PlayersTableModel.getNumericColumns();

    private boolean hasActiveSession = false; 
    private JButton controlButton;
    private JButton saveButton;
    private JButton refreshButton;

    // Add to the class fields section
    private final Set<String> flashingPlayerNames = new HashSet<>();
    private Timer flashTimer;
    private final Color FLASH_COLOR = new Color(255, 255, 200); // Light yellow flash
    private final int FLASH_DURATION_MS = 500; // Flash duration in milliseconds

    public PlayersPanel(StatusConsumer status) {
        super(new BorderLayout());
        this.status = status;
        this.tableModel = new PlayersTableModel();
        this.table = new JTable(tableModel);
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
        setupKeyboardShortcuts();
        setupCommandBusListener();
        setupButtonListeners();
        setupContextMenu();
        setupTableSelectionListener();

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

        // Create save button
        saveButton = new JButton("Save");
        saveButton.setEnabled(true);
        saveButton.addActionListener(e -> {
            if (SessionManager.getInstance().getActiveSession() != null) {
                CommandBus.getInstance().publish(Commands.SAVE_SESSION, this);
            }
        });

        
        // Create refresh button
        refreshButton = new JButton("Refresh");
        refreshButton.setEnabled(true);
        refreshButton.addActionListener(e -> {
            if (SessionManager.getInstance().getActiveSession() != null) {
                CommandBus.getInstance().publish(Commands.PLAYERS_REFRESH_REQUEST, this);
            }
        });

        // Add control button to the right of the button panel
        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftButtonPanel.add(refreshButton);
        leftButtonPanel.add(saveButton);
        buttonWrapper.add(leftButtonPanel, BorderLayout.WEST);

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
        JButton muteButton = new JButton("Mute");
        muteButton.addActionListener(e -> {
            Player player = getSelectedPlayer();
            if (player != null) {
                logger.info("Forcing PLAYER_SELECTED event for: " + player.getName());

                // Update button states
                player.setMuted(!player.isMuted());
                updatePlayerRow(player);
            } else {
                logger.warning("No player selected to force event");
            }
        });
        rightButtonPanel.add(muteButton);

        topPanel.add(buttonWrapper, BorderLayout.NORTH);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void setupTable() {
        // Set minimum and preferred widths for Name and Instrument columns
        table.getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_NAME)).setMinWidth(100);
        table.getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_INSTRUMENT)).setMinWidth(100);

        // Set relative widths for columns to control resize behavior
        table.getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_NAME)).setPreferredWidth(200);
        table.getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_INSTRUMENT)).setPreferredWidth(150);

        // Set fixed widths for other columns to prevent them from growing
        for (int i = 2; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setMaxWidth(80);
            table.getColumnModel().getColumn(i).setPreferredWidth(60);
        }

        // Configure table appearance
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
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

        table.getTableHeader().getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_NAME))
                .setHeaderRenderer(leftHeaderRenderer);
        table.getTableHeader().getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_INSTRUMENT))
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
                    SwingUtilities.invokeLater(
                            () -> UIHelper.getInstance().saveColumnOrder(table, Constants.PLAYER,
                                    PlayersTableModel.COLUMNS));
                }
            }

            public void columnAdded(TableColumnModelEvent e) {
            }

            public void columnRemoved(TableColumnModelEvent e) {
            }

            public void columnMarginChanged(ChangeEvent e) {
            }

            public void columnSelectionChanged(ListSelectionEvent e) {
            }
        });

        // Save initial column order and restore it
        SwingUtilities.invokeLater(
                () -> UIHelper.getInstance().saveColumnOrder(table, Constants.PLAYER, PlayersTableModel.COLUMNS));
        SwingUtilities.invokeLater(
                () -> UIHelper.getInstance().restoreColumnOrder(table, Constants.PLAYER, PlayersTableModel.COLUMNS));

        // Set custom renderer for player rows
        PlayerRowRenderer rowRenderer = new PlayerRowRenderer(this);
        for (int i = 0; i < table.getColumnCount(); i++) {
            final int colIndex = i;
            if (isInArray(BOOLEAN_COLUMNS, colIndex)) {
                table.getColumnModel().getColumn(i).setCellRenderer(
                        new DefaultTableCellRenderer() {
                            private final JCheckBox checkbox = new JCheckBox();
                            {
                                checkbox.setHorizontalAlignment(JCheckBox.CENTER);
                            }

                            @Override
                            public Component getTableCellRendererComponent(JTable table, Object value,
                                    boolean isSelected, boolean hasFocus, int row, int column) {

                                Player player = getPlayerAtRow(row);
                                Color bgColor = table.getBackground();

                                if (player != null && player.isPlaying()) {
                                    bgColor = ColorUtils.dustyAmber;
                                    if (isSelected) {
                                        bgColor = bgColor.darker();
                                    }
                                } else if (isSelected) {
                                    bgColor = table.getSelectionBackground();
                                }

                                if (value instanceof Boolean) {
                                    checkbox.setSelected((Boolean) value);
                                    checkbox.setBackground(bgColor);
                                    return checkbox;
                                }

                                return rowRenderer.getTableCellRendererComponent(
                                        table, value, isSelected, hasFocus, row, column);
                            }
                        });
            } else {
                table.getColumnModel().getColumn(i).setCellRenderer(rowRenderer);
            }
        }
    }

    private boolean isInArray(int[] array, int value) {
        for (int i : array) {
            if (i == value)
                return true;
        }
        return false;
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
        TimingBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                String cmd = action.getCommand();
                try {
                    switch (cmd) {
                        case Commands.BASIC_TIMING_TICK, Commands.PLAYERS_REFRESH_REQUEST -> {
                            SwingUtilities.invokeLater(() -> {
                                refreshPlayers(SessionManager.getInstance().getActiveSession().getPlayers());
                            });
                        }
                    }
                } catch (Exception e) {
                    logger.severe("Error processing command: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        CommandBus.getInstance().register(new IBusListener() {
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
        if (playerName == null)
            return;

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int nameColIndex = tableModel.getColumnIndex(PlayersTableModel.COL_NAME);

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

        int nameColIndex = tableModel.getColumnIndex(PlayersTableModel.COL_NAME);

        // Search by name and then verify by ID
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String playerName = (String) tableModel.getValueAt(i, nameColIndex);
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
            String playerName = (String) tableModel.getValueAt(modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_NAME));
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
                String playerName = (String) tableModel.getValueAt(modelRow,
                        tableModel.getColumnIndex(PlayersTableModel.COL_NAME));
                currentSession.getPlayers().stream()
                        .filter(p -> p.getName().equals(playerName))
                        .findFirst()
                        .ifPresent(p -> players.add(p));
            }
        }
        return players.toArray(new Player[0]);
    }

    public void refreshPlayers(Set<Player> players) {
        logger.info("Refreshing players table with " + (players != null ? players.size() : 0) + " players");
        tableModel.setRowCount(0);

        if (players != null && !players.isEmpty()) {
            List<Player> sortedPlayers = new ArrayList<>(players);
            Collections.sort(sortedPlayers, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

            for (Player player : sortedPlayers) {
                logger.info("Adding player to table: " + player.getName() + " (ID: " + player.getId() + ")");
                tableModel.addPlayerRow(player);
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
            PlayersTableModel model = (PlayersTableModel) table.getModel();

            // Update all cells in the row
            int modelRow = table.convertRowIndexToModel(rowIndex);

            // Update each column with fresh data
            model.setValueAt(player.getName(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_NAME));
            model.setValueAt(player.getNote(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_NOTE));
            model.setValueAt(player.getLevel(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_LEVEL));
            model.setValueAt(player.isMuted(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_MUTE));
            model.setValueAt(player.getProbability(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_PROBABILITY));
            model.setValueAt(player.getSparse(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_SPARSE));
            model.setValueAt(player.getSwing(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_SWING));
            model.setValueAt(player.getRandomDegree(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_RANDOM));
            model.setValueAt(player.getMinVelocity(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_MIN_VEL));
            model.setValueAt(player.getMaxVelocity(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_MAX_VEL));
            model.setValueAt(player.getPreset(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_PRESET));
            model.setValueAt(player.getPanPosition(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_PAN));

            // Special handling for instrument column
            model.updateInstrumentCell(model.getDataVector().get(modelRow),
                    tableModel.getColumnIndex(PlayersTableModel.COL_INSTRUMENT), player);

            // Notify the model that data has changed
            model.fireTableRowsUpdated(modelRow, modelRow);

            // Flash the row to indicate update
            flashPlayerRow(player);

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
                String playerName = (String) tableModel.getValueAt(modelRow,
                        tableModel.getColumnIndex(PlayersTableModel.COL_NAME));

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

    // Helper method to get player at a specific row
    public Player getPlayerAtRow(int row) {
        if (row < 0 || tableModel.getRowCount() <= row) {
            return null;
        }

        try {
            // Convert view index to model index in case of sorting/filtering
            int modelRow = table.convertRowIndexToModel(row);

            // Get the player name from the Name column
            String playerName = (String) tableModel.getValueAt(
                    modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_NAME));

            // Get the current session
            Session currentSession = SessionManager.getInstance().getActiveSession();

            if (currentSession != null && currentSession.getPlayers() != null) {
                // Find the player with the matching name
                return currentSession.getPlayers().stream()
                        .filter(p -> playerName.equals(p.getName()))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            logger.severe("Error getting player at row: " + e.getMessage());
        }

        return null;
    }

    /**
     * Flash the row for the given player
     * 
     * @param player The player whose row should flash
     */
    private void flashPlayerRow(Player player) {
        if (player == null || player.getName() == null) {
            return;
        }

        // Add player to flashing set
        flashingPlayerNames.add(player.getName());
        
        // Cancel existing timer if one is running
        if (flashTimer != null && flashTimer.isRunning()) {
            flashTimer.stop();
        }
        
        // Create new timer to end the flash effect
        flashTimer = new Timer(FLASH_DURATION_MS, e -> {
            // Clear flashing players
            flashingPlayerNames.clear();
            
            // Repaint the table
            table.repaint();
            
            // Stop the timer
            ((Timer)e.getSource()).stop();
        });
        
        // Start the timer
        flashTimer.setRepeats(false);
        flashTimer.start();
        
        // Immediately repaint to show flash
        table.repaint();
    }

    /**
     * Check if a player is currently flashing
     * 
     * @param playerName The player name to check
     * @return True if the player's row is flashing
     */
    public boolean isPlayerFlashing(String playerName) {
        return flashingPlayerNames.contains(playerName);
    }
}
