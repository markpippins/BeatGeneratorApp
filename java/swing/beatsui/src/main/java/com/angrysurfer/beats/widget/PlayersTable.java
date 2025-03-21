package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;

import com.angrysurfer.beats.ColorUtils;
import com.angrysurfer.beats.service.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.util.Constants;

public class PlayersTable extends JTable {
    private static final Logger logger = Logger.getLogger(PlayersTable.class.getName());

    private final PlayersTableModel tableModel;
    private final Set<Long> flashingPlayerIds = new HashSet<>();
    private Timer flashTimer;
    private final Color FLASH_COLOR = ColorUtils.coolBlue; // new Color(255, 255, 200); // Light yellow flash
    private final int FLASH_DURATION_MS = 500; // Flash duration in milliseconds
    private int lastSelectedRow = -1;

    private static final int[] BOOLEAN_COLUMNS = PlayersTableModel.getBooleanColumns();
    private static final int[] NUMERIC_COLUMNS = PlayersTableModel.getNumericColumns();

    public PlayersTable() {
        this.tableModel = new PlayersTableModel();
        setModel(tableModel);

        setupTable();
        setupSelectionListener();
        setupMouseListener(); // Add this line
    }

    public PlayersTableModel getPlayersTableModel() {
        return tableModel;
    }

    private void setupTable() {
        // Hide the ID column
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_ID)).setMinWidth(30);
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_ID)).setMaxWidth(30);
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_ID)).setWidth(0);
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_ID)).setPreferredWidth(30);
        
        // Set minimum and preferred widths for Name and Instrument columns  
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_NAME)).setMinWidth(100);
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_INSTRUMENT)).setMinWidth(100);

        // Set relative widths for columns
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_NAME)).setPreferredWidth(200);
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_INSTRUMENT)).setPreferredWidth(150);

        // Set fixed widths for other columns
        for (int i = 2; i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setMaxWidth(80);
            getColumnModel().getColumn(i).setPreferredWidth(60);
        }

        // Configure table appearance
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setAutoCreateRowSorter(true);

        // Set default checkbox renderer for Boolean columns
        setupBooleanColumnRenderers();

        // Add left alignment for header columns
        setupHeaderRenderers();

        // Add column reordering listener
        setupColumnReorderingListener();

        // Save and restore column order
        SwingUtilities.invokeLater(
                () -> UIHelper.getInstance().saveColumnOrder(this, Constants.PLAYER, PlayersTableModel.COLUMNS));
        SwingUtilities.invokeLater(
                () -> UIHelper.getInstance().restoreColumnOrder(this, Constants.PLAYER, PlayersTableModel.COLUMNS));

        // Set custom renderer for all rows - this handles centering numeric values internally
        setupCustomRowRenderer();
    }

    private void setupBooleanColumnRenderers() {
        for (int booleanColumn : BOOLEAN_COLUMNS) {
            getColumnModel().getColumn(booleanColumn).setCellRenderer(
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

                                // Determine background color
                                Color bgColor = table.getBackground();
                                Player player = getPlayerAtRow(row);

                                if (isPlayerFlashing(player)) {
                                    bgColor = isSelected ? FLASH_COLOR.darker() : FLASH_COLOR;
                                } else if (player != null && player.isPlaying()) {
                                    bgColor = isSelected ? ColorUtils.charcoalGray.darker() : ColorUtils.charcoalGray;
                                } else if (isSelected) {
                                    bgColor = table.getSelectionBackground();
                                }

                                checkbox.setBackground(bgColor);
                                return checkbox;
                            }
                            return super.getTableCellRendererComponent(table, value, isSelected,
                                    hasFocus, row, column);
                        }
                    });
        }
    }

    private void setupHeaderRenderers() {
        DefaultTableCellRenderer leftHeaderRenderer = new DefaultTableCellRenderer();
        leftHeaderRenderer.setHorizontalAlignment(JLabel.LEFT);

        getTableHeader().getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_NAME))
                .setHeaderRenderer(leftHeaderRenderer);
        getTableHeader().getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_INSTRUMENT))
                .setHeaderRenderer(leftHeaderRenderer);
    }

    private void setupColumnReorderingListener() {
        getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnMoved(TableColumnModelEvent e) {
                if (e.getFromIndex() != e.getToIndex()) {
                    logger.info("Column moved from " + e.getFromIndex() + " to " + e.getToIndex());
                    SwingUtilities.invokeLater(
                            () -> UIHelper.getInstance().saveColumnOrder(PlayersTable.this, Constants.PLAYER,
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
    }

    private void setupCustomRowRenderer() {
        PlayerRowRenderer rowRenderer = new PlayerRowRenderer(this);
        for (int i = 0; i < getColumnCount(); i++) {
            if (!isInArray(BOOLEAN_COLUMNS, i)) {
                getColumnModel().getColumn(i).setCellRenderer(rowRenderer);
            }
        }
    }

    private void setupSelectionListener() {
        getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Only handle when selection is complete
                int selectedRow = getSelectedRow();
                handlePlayerSelection(selectedRow);
            }
        });
    }

    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        Player player = getPlayerAtRow(row);
                        if (player != null) {
                            // Edit the player on double-click
                            logger.info("Double-clicked player: " + player.getName());
                            CommandBus.getInstance().publish(Commands.PLAYER_EDIT_REQUEST, this, player);
                        }
                    }
                }
            }
        });
    }

    public void handlePlayerSelection(int row) {
        if (row >= 0) {
            lastSelectedRow = row;
        }

        try {
            Player player = null;

            if (row >= 0) {
                player = getPlayerAtRow(row);
            }

            // Log selection for debugging
            logger.info("Player selection changed: "
                    + (player != null ? player.getName() + " (ID: " + player.getId() + ")" : "null"));

            // First update PlayerManager's state directly
            PlayerManager.getInstance().setActivePlayer(player);

            // Then explicitly publish events
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

    public Player getPlayerAtRow(int row) {
        if (row < 0 || tableModel.getRowCount() <= row) {
            return null;
        }

        try {
            // Convert view index to model index in case of sorting/filtering
            int modelRow = convertRowIndexToModel(row);

            // Get the player ID from the ID column
            Long playerId = (Long) tableModel.getValueAt(
                    modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_ID));

            // Get the current session
            Session currentSession = SessionManager.getInstance().getActiveSession();

            if (currentSession != null && currentSession.getPlayers() != null) {
                // Find the player with the matching ID
                return currentSession.getPlayers().stream()
                        .filter(p -> playerId.equals(p.getId()))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            logger.severe("Error getting player at row: " + e.getMessage());
        }

        return null;
    }

    public void flashPlayerRow(Player player) {
        if (player == null || player.getId() == null) {
            return;
        }

        // Add player to flashing set
        flashingPlayerIds.add(player.getId());

        // Cancel existing timer if one is running
        if (flashTimer != null && flashTimer.isRunning()) {
            flashTimer.stop();
        }

        // Create new timer to end the flash effect
        flashTimer = new Timer(FLASH_DURATION_MS, e -> {
            // Clear flashing players
            flashingPlayerIds.clear();

            // Repaint the table
            repaint();

            // Stop the timer
            ((Timer) e.getSource()).stop();
        });

        // Start the timer
        flashTimer.setRepeats(false);
        flashTimer.start();

        // Immediately repaint to show flash
        repaint();
    }

    public boolean isPlayerFlashing(Player player) {
        return player != null && player.getId() != null &&
                flashingPlayerIds.contains(player.getId());
    }

    public boolean isPlayerFlashing(Long playerId) {
        return flashingPlayerIds.contains(playerId);
    }

    private boolean isInArray(int[] array, int value) {
        for (int i : array) {
            if (i == value)
                return true;
        }
        return false;
    }

    public void updatePlayerRow(Player player) {
        if (player == null)
            return;

        try {
            // Find row index for this player
            int rowIndex = findPlayerRowIndex(player);
            if (rowIndex == -1) {
                logger.warning("Player not found in table: " + player.getName());
                return;
            }

            // Update all cells in the row
            int modelRow = convertRowIndexToModel(rowIndex);

            // Update each column with fresh data
            tableModel.setValueAt(player.getName(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_NAME));
            tableModel.setValueAt(player.getNote(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_NOTE));
            tableModel.setValueAt(player.getLevel(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_LEVEL));
            tableModel.setValueAt(player.isMuted(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_MUTE));
            tableModel.setValueAt(player.getProbability(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_PROBABILITY));
            tableModel.setValueAt(player.getSparse(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_SPARSE));
            tableModel.setValueAt(player.getSwing(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_SWING));
            tableModel.setValueAt(player.getRandomDegree(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_RANDOM));
            tableModel.setValueAt(player.getMinVelocity(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_MIN_VEL));
            tableModel.setValueAt(player.getMaxVelocity(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_MAX_VEL));
            tableModel.setValueAt(player.getPreset(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_PRESET));
            tableModel.setValueAt(player.getPanPosition(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_PAN));

            // Special handling for instrument column
            tableModel.updateInstrumentCell(tableModel.getDataVector().get(modelRow),
                    tableModel.getColumnIndex(PlayersTableModel.COL_INSTRUMENT), player);

            // Notify the model that data has changed
            tableModel.fireTableRowsUpdated(modelRow, modelRow);

            // Flash the row to indicate update
            flashPlayerRow(player);

            logger.info("Updated row " + rowIndex + " for player: " + player.getName());
        } catch (Exception e) {
            logger.severe("Error updating player row: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int findPlayerRowIndex(Player player) {
        if (player == null || player.getId() == null)
            return -1;

        int idColIndex = tableModel.getColumnIndex(PlayersTableModel.COL_ID);

        // Search by ID
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Long playerId = (Long) tableModel.getValueAt(i, idColIndex);
            if (player.getId().equals(playerId)) {
                return i;
            }
        }

        return -1; // Not found
    }

    public int getLastSelectedRow() {
        return lastSelectedRow;
    }

    public void setLastSelectedRow(int row) {
        this.lastSelectedRow = row;
    }

    /**
     * Forwards column index lookup to the table model
     */
    public int getColumnIndex(String columnName) {
        return getPlayersTableModel().getColumnIndex(columnName);
    }

    /**
     * Get the flash color for use by renderers
     */
    public Color getFlashColor() {
        return FLASH_COLOR;
    }

    /**
     * Get the flash duration for use by renderers
     */
    public int getFlashDurationMs() {
        return FLASH_DURATION_MS;
    }

    /**
     * Sorts the table by player name
     */
    public void sortTable() {
        TableRowSorter<PlayersTableModel> sorter = (TableRowSorter<PlayersTableModel>) getRowSorter();
        if (sorter != null) {
            int nameColumnIndex = getColumnIndex(PlayersTableModel.COL_NAME);
            List<SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new SortKey(nameColumnIndex, SortOrder.ASCENDING));
            sorter.setSortKeys(sortKeys);
            sorter.sort();
        }
    }
}