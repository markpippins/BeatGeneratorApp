package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

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

    private static final Logger logger = Logger.getLogger(PlayersPanel.class.getName());

    private final PlayersTable table;
    private final StatusConsumer status;
    private final ButtonPanel buttonPanel;
    private final ContextMenuHelper contextMenu;

    private boolean hasActiveSession = false; 
    private JButton controlButton;
    private JButton saveButton;
    private JButton refreshButton;

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
        setupCommandBusListener();
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

    // Update the problematic methods

    private void selectPlayerByName(String playerName) {
        if (playerName == null)
            return;

        PlayersTableModel model = table.getPlayersTableModel();
        int nameColIndex = model.getColumnIndex(PlayersTableModel.COL_NAME);

        for (int i = 0; i < model.getRowCount(); i++) {
            String name = (String) model.getValueAt(i, nameColIndex);
            if (playerName.equals(name)) {
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

    public void refreshPlayers(Set<Player> players) {
        logger.info("Refreshing players table with " + (players != null ? players.size() : 0) + " players");
        PlayersTableModel tableModel = table.getPlayersTableModel();
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
        table.updatePlayerRow(player);
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
                String playerName = (String) table.getValueAt(modelRow,
                        table.getColumnIndex(PlayersTableModel.COL_NAME));

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
            logger.severe("Error getting player at row: " + e.getMessage());
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
}
