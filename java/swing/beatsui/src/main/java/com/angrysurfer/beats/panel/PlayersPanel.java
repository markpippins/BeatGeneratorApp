package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
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
import com.angrysurfer.core.data.RedisService;
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
    private static final int[] BOOLEAN_COLUMNS = {8, 15, 16}; // Sticky, Preserve, Sparse columns
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
            "Name", "Channel", "Swing", "Level", "Note", "Min Vel", "Max Vel",
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
                return super.getColumnClass(column);
            }
            
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };
        
        table.setModel(model);
        
        // Configure table appearance
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        
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
            if (e.getActionCommand().equals(Commands.PLAYER_ADD_REQUEST)) {
                // For add, we don't need a selected player
                CommandBus.getInstance().publish(Commands.PLAYER_ADD_REQUEST, this, null);
                return;
            }
            
            // For edit/delete, we need a selected player
            ProxyStrike selectedPlayer = getSelectedPlayer();
            if (selectedPlayer != null) {
                CommandBus.getInstance().publish(e.getActionCommand(), this, selectedPlayer);
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
                    case Commands.TICKER_SELECTED, Commands.TICKER_UPDATED, Commands.TICKER_LOADED -> {
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
                    case Commands.TICKER_UNSELECTED -> {
                        hasActiveTicker = false;
                        enableControls(false);
                        refreshPlayers(null);
                    }
                    case Commands.PLAYER_ADDED -> {
                        if (action.getData() instanceof TickerManager.PlayerAddedData data) {
                            // Update table with new player
                            refreshPlayers(data.ticker().getPlayers());
                            
                            // Select the new player
                            int rowIndex = findPlayerRowIndex(data.player());
                            if (rowIndex >= 0) {
                                table.setRowSelectionInterval(rowIndex, rowIndex);
                            }
                            
                            // Show edit dialog
                            CommandBus.getInstance().publish(Commands.PLAYER_EDIT_REQUEST, this, 
                                new PlayerEditData(data.ticker(), data.player()));
                        }
                    }

                    case Commands.PLAYER_EDIT_CANCELLED -> {
                        if (action.getData() instanceof PlayerEditData data) {
                            // Remove the player if this was a new player add that was cancelled
                            RedisService.getInstance().removePlayerFromTicker(data.ticker(), data.player());
                            RedisService.getInstance().deletePlayer(data.player());
                            
                            // Refresh the table
                            refreshPlayers(data.ticker().getPlayers());
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

    public void refreshPlayers(Set<IProxyPlayer> players) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        if (players != null) {
            List<IProxyPlayer> sortedPlayers = new ArrayList<>(players);
            Collections.sort(sortedPlayers, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            
            for (IProxyPlayer player : sortedPlayers) {
                Object[] rowData = player.toRow();
                // Get boolean values from row data strings
                rowData[8] = Boolean.valueOf(rowData[8].toString());   // Sticky
                rowData[15] = Boolean.valueOf(rowData[15].toString()); // Preserve
                rowData[16] = Boolean.valueOf(rowData[16].toString()); // Sparse
                model.addRow(rowData);
            }
        }
    }
}
