package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.PlayersTable;
import com.angrysurfer.beats.widget.PlayersTableModel;
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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayersPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(PlayersPanel.class.getName());

    private final PlayersTable table;
    private final StatusConsumer status;
    private final ButtonPanel buttonPanel;
    private final ContextMenuHelper contextMenu;

    private boolean hasActiveSession = false; 
    private JButton controlButton;
    private JButton saveButton;
    private JButton copyButton;
    private JButton refreshButton;
    private JButton muteButton;

    public PlayersPanel(StatusConsumer status) {
        super(new BorderLayout());
        this.status = status;
        this.table = new PlayersTable();  // Use our new PlayersTable class
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

        setupLayout();
        setupKeyboardShortcuts();
        setupConsolidatedBusListener();
        setupButtonListeners();
        setupContextMenu();
        setupSelectionListener(); // Add this line

        // Check for active session and enable controls immediately
        Session currentSession = SessionManager.getInstance().getActiveSession();
        if (currentSession != null) {
            logger.info("Found active session on construction: " + currentSession.getId());
            hasActiveSession = true;
            enableControls(true);
            refreshPlayers(currentSession.getPlayers());
            
            // Auto-select first player if available
            SwingUtilities.invokeLater(this::selectFirstPlayerIfNoneSelected);
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
                CommandBus.getInstance().publish(Commands.PLAYER_TABLE_REFRESH_REQUEST, this);
            }
        });


        // Create copy button
        copyButton = new JButton("Copy...");
        copyButton.setEnabled(true);
        copyButton.addActionListener(e -> {
            if (SessionManager.getInstance().getActiveSession() != null) {
                CommandBus.getInstance().publish(Commands.PLAYER_COPY_EDIT_REQUEST, this);
            }
        });


        // Add control button to the right of the button panel
        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftButtonPanel.add(refreshButton);
        leftButtonPanel.add(saveButton);
        leftButtonPanel.add(copyButton);
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
        muteButton = new JButton("Mute");
        muteButton.addActionListener(e -> {
            Player player = getSelectedPlayer();
            if (player != null) {
                logger.info("Forcing PLAYER_SELECTED event for: " + player.getName());

                // Update button states
                player.setMuted(!player.isMuted());
                updatePlayerRow(player);
            } else {
                logger.error("No player selected to force event");
            }
        });
        rightButtonPanel.add(muteButton);

        topPanel.add(buttonWrapper, BorderLayout.NORTH);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
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
        controlButton.setEnabled(hasSelection && getSelectedPlayer() != null
                && getSelectedPlayer().getInstrument() != null
                && !getSelectedPlayer().getInstrument().getControlCodes().isEmpty());

        copyButton.setEnabled(hasSelection);
        muteButton.setEnabled(hasSelection);

        saveButton.setEnabled(hasActiveSession);
    }

    private void setupConsolidatedBusListener() {
        IBusListener consolidatedListener = new IBusListener() {
            @Override
            public void onAction(Command action) {
                // Ignore null commands or events sent by this panel
                if (action.getCommand() == null || action.getSender() == PlayersPanel.this) {
                    return;
                }

                String cmd = action.getCommand();
                try {
                    switch (cmd) {
                        case Commands.TIME_TICK:
                        case Commands.PLAYER_TABLE_REFRESH_REQUEST:
                        case Commands.SESSION_SELECTED:
                        case Commands.SESSION_CHANGED:
                        case Commands.SESSION_LOADED: {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (action.getData() instanceof Session) {
                                        final Session session = (Session) action.getData();
                                        long now = System.currentTimeMillis();
                                        if (session.getId() != null &&
                                                session.getId().equals(lastProcessedSessionId) &&
                                                (now - lastSessionEventTime) < EVENT_THROTTLE_MS) {
                                            logger.debug("PlayersPanel: Ignoring duplicate session event: " + session.getId());
                                            return;
                                        }
                                        lastProcessedSessionId = session.getId();
                                        lastSessionEventTime = now;
        
                                        logger.info("PlayersPanel: Updating with new session: " + session.getId());
                                        SwingUtilities.invokeLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                enableControls(true);
                                                refreshPlayers(session.getPlayers());
                                                selectFirstPlayerIfNoneSelected();
                                            }
                                        });
                                    }
                                        }
                            });
                            break;
                        }
                        case Commands.PLAYER_ROW_REFRESH: {
                            if (action.getData() instanceof Player) {
                                Player player = (Player) action.getData();
                                logger.info("Refreshing player row for: " + player.getName());
                                updatePlayerRow(player);
                            }
                            break;
                        }
                        case Commands.PLAYER_ADDED: {
                            logger.info("Player added, refreshing table");
                            Session activeSession = SessionManager.getInstance().getActiveSession();
                            if (activeSession != null) {
                                refreshPlayers(activeSession.getPlayers());
                                if (action.getData() instanceof Player) {
                                    Player player = (Player) action.getData();
                                    selectPlayerById(player.getId());
                                } else {
                                    selectLastPlayer();
                                }
                            }
                            break;
                        }
                        case Commands.SHOW_PLAYER_EDITOR_OK: {
                            logger.info("Player edited, refreshing table");
                            Session activeSession = SessionManager.getInstance().getActiveSession();
                            if (activeSession != null && action.getData() instanceof Player) {
                                Player player = (Player) action.getData();
                                refreshPlayers(activeSession.getPlayers());
                                selectPlayerById(player.getId());
                            }
                            break;
                        }
                        case Commands.PLAYER_DELETED: {
                            logger.info("Player(s) deleted, refreshing table");
                            Session activeSession = SessionManager.getInstance().getActiveSession();
                            if (activeSession != null) {
                                refreshPlayers(activeSession.getPlayers());
                                if (table.getRowCount() > 0) {
                                    int rowToSelect = Math.min(lastSelectedRow, table.getRowCount() - 1);
                                    table.setRowSelectionInterval(rowToSelect, rowToSelect);
                                    handlePlayerSelection(rowToSelect);
                                } else {
                                    CommandBus.getInstance().publish(Commands.PLAYER_UNSELECTED, PlayersPanel.this, null);
                                }
                            }
                            break;
                        }
                        case Commands.NEW_VALUE_LEVEL:
                        case Commands.NEW_VALUE_NOTE:
                        case Commands.NEW_VALUE_SWING:
                        case Commands.NEW_VALUE_PROBABILITY:
                        case Commands.NEW_VALUE_VELOCITY_MIN:
                        case Commands.NEW_VALUE_VELOCITY_MAX:
                        case Commands.NEW_VALUE_RANDOM:
                        case Commands.NEW_VALUE_PAN:
                        case Commands.NEW_VALUE_SPARSE:
                        case Commands.PLAYER_UPDATED: {
                            if (action.getSender() instanceof Player) {
                                Player player = (Player) action.getSender();
                                logger.info("Updating player row due to " + cmd + " for: " + player.getName());
                                updatePlayerRow(player);
                            }
                            break;
                        }
                        default: {
                            // Optionally log or ignore other commands
                            break;
                        }
                    } // end switch
                } catch (Exception e) {
                    logger.error("Error processing command: " + e.getMessage(), e);
                }
            }
        };

        // Register the single listener on both buses
        TimingBus.getInstance().register(consolidatedListener);
        CommandBus.getInstance().register(consolidatedListener);
    }

    // Add a field to track the last selected row
    private int lastSelectedRow = -1;

    // Update the problematic methods

    private void selectPlayerById(Long playerId) {
        if (playerId == null)
            return;

        PlayersTableModel model = table.getPlayersTableModel();
        int idColIndex = model.getColumnIndex(PlayersTableModel.COL_ID);

        for (int i = 0; i < model.getRowCount(); i++) {
            Long id = (Long) model.getValueAt(i, idColIndex);
            if (playerId.equals(id)) {
                table.setRowSelectionInterval(i, i);
                table.setLastSelectedRow(i);
                table.handlePlayerSelection(i);
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
                    "Are you sure you want to delete the selected player" + 
                    (selectedPlayers.length > 1 ? "s" : "") + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                // Create a list of player IDs instead of player objects
                List<Long> playerIds = new ArrayList<>();
                for (Player player : selectedPlayers) {
                    if (player != null && player.getId() != null) {
                        playerIds.add(player.getId());
                        logger.info("Adding player to delete list: " + player.getName() + 
                                " (ID: " + player.getId() + ")");
                    }
                }
                
                // Send the IDs instead of player objects
                CommandBus.getInstance().publish(Commands.PLAYER_DELETE_REQUEST, this, 
                        playerIds.toArray(Long[]::new));
                logger.info("Sent delete request for " + playerIds.size() + " players");
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

        int nameColIndex = table.getColumnIndex(PlayersTableModel.COL_NAME);

        // Search by name and then verify by ID
        for (int i = 0; i < table.getRowCount(); i++) {
            String playerName = (String) table.getValueAt(i, nameColIndex);
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

        return table.getPlayerAtRow(selectedRow);
    }

    private Player[] getSelectedPlayers() {
        int[] selectedRows = table.getSelectedRows();
        List<Player> players = new ArrayList<>();

            for (int row : selectedRows) {
            Player player = table.getPlayerAtRow(row);
            if (player != null) {
                players.add(player);
            }
        }
        
        return players.toArray(new Player[0]);
    }

    /**
     * Refreshes the players table, preserving existing rows when possible
     * and only adding/removing players as needed
     */
    public void refreshPlayers(Set<Player> players) {
        // Add check to prevent recursion
        if (isRefreshing) {
            logger.debug("Already refreshing players, skipping");
            return;
        }
        
        isRefreshing = true;
        try {
            logger.info("Refreshing players table with " + (players != null ? players.size() : 0) + " players");
            PlayersTableModel tableModel = table.getPlayersTableModel();
            
            if (players == null || players.isEmpty()) {
                // If no players, just clear the table
                tableModel.setRowCount(0);
                return;
            }
            
            // Create a map of player IDs to players for the new set
            Map<Long, Player> newPlayerMap = new HashMap<>();
            for (Player player : players) {
                if (player != null && player.getId() != null) {
                    newPlayerMap.put(player.getId(), player);
                }
            }
            
            // Get existing players in the table
            Set<Long> existingPlayerIds = new HashSet<>();
            int idColIndex = tableModel.getColumnIndex(PlayersTableModel.COL_ID);
            
            // First update existing rows and mark for deletion
            List<Integer> rowsToRemove = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Long playerId = (Long) tableModel.getValueAt(i, idColIndex);
                
                if (playerId != null) {
                    existingPlayerIds.add(playerId);
                    
                    if (newPlayerMap.containsKey(playerId)) {
                        // Player still exists - update this row
                        Player updatedPlayer = newPlayerMap.get(playerId);
                        updatePlayerRow(updatedPlayer);
                        // Remove from map since we've handled it
                        newPlayerMap.remove(playerId);
                    } else {
                        // Player no longer exists - mark row for removal
                        rowsToRemove.add(i);
                    }
                }
            }
            
            // Remove rows for deleted players (in reverse order to maintain indices)
            Collections.sort(rowsToRemove, Collections.reverseOrder());
            for (int rowIndex : rowsToRemove) {
                tableModel.removeRow(rowIndex);
            }
            
            // Add rows for new players
            if (!newPlayerMap.isEmpty()) {
                List<Player> newPlayers = new ArrayList<>(newPlayerMap.values());
                Collections.sort(newPlayers, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                
                for (Player newPlayer : newPlayers) {
                    logger.info("Adding new player to table: " + newPlayer.getName() + " (ID: " + newPlayer.getId() + ")");
                    tableModel.addPlayerRow(newPlayer);
                }
                
                // Re-sort the entire table
                table.sortTable();
            }
            
            // If removing players removed the selection, select another player if available
            if (!rowsToRemove.isEmpty() && table.getSelectedRow() < 0 && table.getRowCount() > 0) {
                selectFirstPlayerIfNoneSelected();
            }
        } finally {
            isRefreshing = false;
        }
    }

    /**
     * Update a single player row in the table
     * 
     * @param player The player to update
     */
    private void updatePlayerRow(Player player) {
        table.updatePlayerRow(player);
    }

    private void handlePlayerSelection(int row) {
        if (row >= 0) {
            lastSelectedRow = row;
        }

        try {
            Player player = null;

            if (row >= 0) {
                // Get player directly from table helper method (which now uses ID)
                player = table.getPlayerAtRow(row);
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
            logger.error("Error in player selection: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Helper method to get player at a specific row
    public Player getPlayerAtRow(int row) {
        if (row < 0 || table.getRowCount() <= row) {
            return null;
        }
        
        try {
            // Convert view index to model index in case of sorting/filtering
            int modelRow = table.convertRowIndexToModel(row);
            
            // Get the player name from the Name column
            String playerName = (String) table.getValueAt(
                    modelRow, table.getColumnIndex(PlayersTableModel.COL_NAME));
            
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
            logger.error("Error getting player at row: " + e.getMessage());
        }
        
        return null;
    }

    // Add this method to PlayersPanel class to setup selection listener
    private void setupSelectionListener() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Only handle when selection is complete
                // Update button states when selection changes
                updateButtonStates();
            }
        });
    }

    // Add this method to PlayersPanel class
    private void selectFirstPlayerIfNoneSelected() {
        // Only select if we have players but no selection yet
        if (table.getRowCount() > 0 && table.getSelectedRow() < 0) {
            logger.info("Auto-selecting first player");
            table.setRowSelectionInterval(0, 0);
            table.handlePlayerSelection(0);
        }
    }

    // Add this field to the class
    private boolean isRefreshing = false;

    // Add this field to PlayersPanel
    private Long lastProcessedSessionId = null;
    private long lastSessionEventTime = 0;
    private static final long EVENT_THROTTLE_MS = 100;
}
