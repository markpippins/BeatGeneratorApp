package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beats.service.TickerManager;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.proxy.IProxyPlayer;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;

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
    private static final int[] BOOLEAN_COLUMNS = {
        9,    // Sticky preset
        14,   // Internal Beats
        15,   // Internal Bars
        17,   // Preserve
        18    // Sparse
    };
    private static final int[] NUMERIC_COLUMNS = {
        2,  // Channel
        3,  // Swing
        4,  // Level
        5,  // Note
        6,  // Min Vel
        7,  // Max Vel
        8,  // Preset
        10, // Prob
        11, // Random
        12, // Ratchet #
        13, // Ratchet Int
        16  // Pan
    };
    private boolean hasActiveTicker = false;  // Add this field

    public PlayersPanel(StatusConsumer status, RulesPanel ruleTablePanel) {
        super(new BorderLayout());
        this.status = status;
        this.ruleTablePanel = ruleTablePanel;
        this.table = new JTable();
        this.buttonPanel = new ButtonPanel(
            Commands.PLAYER_ADD_REQUEST,
            Commands.PLAYER_EDIT_REQUEST,
            Commands.PLAYER_DELETE_REQUEST
        );
        this.contextMenu = new ContextMenuHelper(
            Commands.PLAYER_ADD_REQUEST,
            Commands.PLAYER_EDIT_REQUEST,
            Commands.PLAYER_DELETE_REQUEST
        );
        
        // Always enable Add functionality
        buttonPanel.setAddEnabled(true);
        contextMenu.setAddEnabled(true);
        
        setupTable();
        setupLayout();
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
        topPanel.add(buttonPanel, BorderLayout.NORTH);
        
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void setupTable() {
        String[] columnNames = {
            "Name", "Instrument", "Channel", "Swing", "Level", "Note", "Min Vel", "Max Vel",
            "Preset", "Sticky", "Prob", "Random", "Ratchet #", "Ratchet Int",
            "Int Beats", "Int Bars", "Pan", "Preserve", "Sparse"
        };

        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                // Return Boolean.class for boolean columns
                for (int booleanColumn : BOOLEAN_COLUMNS) {
                    if (column == booleanColumn) {
                        return Boolean.class;
                    }
                }
                return Object.class;  // Changed from super.getColumnClass(column)
            }
            
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };
        
        table.setModel(model);
        
        // Set minimum and preferred widths for Name and Instrument columns
        table.getColumnModel().getColumn(0).setMinWidth(100);   // Name column min
        table.getColumnModel().getColumn(1).setMinWidth(100);   // Instrument column min
        
        // Set relative widths for columns to control resize behavior
        table.getColumnModel().getColumn(0).setPreferredWidth(200);  // Name gets more space
        table.getColumnModel().getColumn(1).setPreferredWidth(150);  // Instrument gets less space
        
        // Set fixed widths for other columns to prevent them from growing
        for (int i = 2; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setMaxWidth(80);
            table.getColumnModel().getColumn(i).setPreferredWidth(60);
        }

        // Configure table appearance
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);  // Changed from default
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
                }
            );
        }
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
        
        // Only update Edit and Delete states
        buttonPanel.setEditEnabled(hasSelection);
        buttonPanel.setDeleteEnabled(hasSelection);
        contextMenu.setEditEnabled(hasSelection);
        contextMenu.setDeleteEnabled(hasSelection);
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

    // Helper record for player edit data
    public record PlayerEditData(ProxyTicker ticker, ProxyStrike player) {}

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
    // 0  - Name
    // 1  - Instrument (new column)
    // 2  - Channel
    // 3  - Swing
    // 4  - Level
    // 5  - Note
    // 6  - Min Vel
    // 7  - Max Vel
    // 8  - Preset
    // 9  - Sticky       (Boolean)
    // 10 - Prob
    // 11 - Random
    // 12 - Ratchet #
    // 13 - Ratchet Int
    // 14 - Int Beats    (Boolean)
    // 15 - Int Bars     (Boolean)
    // 16 - Pan
    // 17 - Preserve     (Boolean)
    // 18 - Sparse

    public void refreshPlayers(Set<IProxyPlayer> players) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        if (players != null) {
            List<IProxyPlayer> sortedPlayers = new ArrayList<>(players);
            Collections.sort(sortedPlayers, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            
            for (IProxyPlayer p : sortedPlayers) {
                ProxyStrike player = (ProxyStrike) p;
                Object[] rowData = player.toRow();
                
                // Insert instrument name after player name
                Object[] newRowData = new Object[rowData.length + 1];
                newRowData[0] = rowData[0]; // Name
                newRowData[1] = player.getInstrument() != null ? player.getInstrument().getName() : ""; // Instrument
                System.arraycopy(rowData, 1, newRowData, 2, rowData.length - 1);
                
                // Convert only specific columns to boolean
                for (int booleanColumn : BOOLEAN_COLUMNS) {
                    if (booleanColumn < newRowData.length) {
                        newRowData[booleanColumn] = Boolean.valueOf(String.valueOf(newRowData[booleanColumn]));
                    }
                }
                
                model.addRow(newRowData);
            }
        }
    }
}
