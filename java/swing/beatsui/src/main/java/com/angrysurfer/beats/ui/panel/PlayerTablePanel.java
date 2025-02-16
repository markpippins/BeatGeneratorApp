package com.angrysurfer.beats.ui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beats.App;
import com.angrysurfer.beats.api.Command;
import com.angrysurfer.beats.api.CommandBus;
import com.angrysurfer.beats.api.CommandListener;
import com.angrysurfer.beats.api.Commands;
import com.angrysurfer.beats.api.StatusConsumer;
import com.angrysurfer.beats.ui.Utils;
import com.angrysurfer.beats.ui.widget.Dialog;
import com.angrysurfer.core.model.player.Strike;
import com.angrysurfer.core.proxy.IProxyPlayer;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerTablePanel extends JPanel implements CommandListener {
    private static final Logger logger = Logger.getLogger(PlayerTablePanel.class.getName());
    private final JTable table;
    private final StatusConsumer status;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JMenuItem editMenuItem;
    private final JMenuItem deleteMenuItem;
    private final RuleTablePanel ruleTablePanel;
    private final CommandBus actionBus = CommandBus.getInstance();

    public PlayerTablePanel(StatusConsumer status, RuleTablePanel ruleTablePanel) {
        super(new BorderLayout());
        this.status = status;
        this.table = new JTable();
        this.addButton = new JButton("Add");
        this.editButton = new JButton("Edit");
        this.deleteButton = new JButton("Delete");
        this.editMenuItem = new JMenuItem("Edit...");
        this.deleteMenuItem = new JMenuItem("Delete");
        this.ruleTablePanel = ruleTablePanel;
        
        setupTable();
        setupButtons();
        setupLayout();
        setupPopupMenu();
    }

    private void setupLayout() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        add(buttonPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void setupButtons() {
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        addButton.addActionListener(e -> showPlayerDialog(null));
        editButton.addActionListener(e -> editSelectedPlayer());
        deleteButton.addActionListener(e -> deleteSelectedPlayer());
    }

    private ProxyStrike savePlayerToRedis(ProxyStrike player) {
        try {
            ProxyStrike savedPlayer = App.getRedisService().saveStrike(player);
            status.setStatus("Saved player: " + player.getName() + " (ID: " + savedPlayer.getId() + ")");
            return savedPlayer;
        } catch (Exception ex) {
            status.setStatus("Error saving player: " + ex.getMessage());
            ex.printStackTrace();
            return player;
        }
    }

    private void deletePlayerFromRedis(ProxyStrike player) {
        try {
            App.getRedisService().deleteStrike(player);
            status.setStatus("Deleted player: " + player.getName());
        } catch (Exception ex) {
            status.setStatus("Error deleting player: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void setupPopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem addMenuItem = new JMenuItem("Add...");
        
        addMenuItem.addActionListener(e -> showPlayerDialog(null));
        editMenuItem.addActionListener(e -> editSelectedPlayer());
        deleteMenuItem.addActionListener(e -> deleteSelectedPlayer());

        editMenuItem.setEnabled(false);
        deleteMenuItem.setEnabled(false);

        popup.add(addMenuItem);
        popup.add(editMenuItem);
        popup.addSeparator();
        popup.add(deleteMenuItem);

        table.setComponentPopupMenu(popup);
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
                switch (column) {
                    case 0: return String.class;  // Name
                    case 8:
                    case 13:
                    case 14:
                    case 16: return Boolean.class;  // Sticky, IntBeats, IntBars, Preserve
                    case 17: return Double.class;  // Sparse
                    default: return Long.class;  // All other numeric fields
                }
            }
        };

        table.setModel(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        // Column setup - keeping existing setup
        table.getColumnModel().getColumn(0).setPreferredWidth(100); // Name
        
        // Set fixed widths for Channel and Preset columns
        table.getColumnModel().getColumn(1).setMaxWidth(60);  // Channel
        table.getColumnModel().getColumn(1).setPreferredWidth(60);
        table.getColumnModel().getColumn(7).setMaxWidth(60);  // Preset
        table.getColumnModel().getColumn(7).setPreferredWidth(60);

        // Continue with existing column setups
        for (int i = 2; i < table.getColumnCount(); i++) {
            if (i != 7) { // Skip Preset column as it's already set
                table.getColumnModel().getColumn(i).setPreferredWidth(120);
            }
        }

        // Set up custom editors
        Utils.setupColumnEditor(table, "Channel", 1, 16);
        Utils.setupColumnEditor(table, "Swing", 0, 100);
        Utils.setupColumnEditor(table, "Level", 1, 127);
        Utils.setupColumnEditor(table, "Note", 1, 127);
        Utils.setupColumnEditor(table, "Min Vel", 1, 127);
        Utils.setupColumnEditor(table, "Max Vel", 1, 127);
        Utils.setupColumnEditor(table, "Preset", 1, 127);
        Utils.setupColumnEditor(table, "Prob", 0, 100);
        Utils.setupColumnEditor(table, "Random", 0, 100);
        Utils.setupColumnEditor(table, "Ratchet #", 0, 6);
        Utils.setupColumnEditor(table, "Pan", 1, 127);

        // Center alignment for numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 1; i < table.getColumnCount(); i++) {
            if (!isBooleanColumn(table.getColumnName(i))) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        // Add selection listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() >= 0;
                updateButtonStates(hasSelection);
                
                if (hasSelection) {
                    IProxyPlayer player = getPlayerFromRow(table.getSelectedRow());
                    if (player != null) {
                        logger.info("Publishing PLAYER_SELECTED event for: " + player.getName());
                        publishPlayerSelected(player);
                    }
                } else {
                    logger.info("Publishing PLAYER_UNSELECTED event");
                    publishPlayerUnselected();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedPlayer();
                }
            }
        });

        // Remove sample data and load from Redis instead
        loadPlayersFromRedis();
    }

    private void updateButtonStates(boolean hasSelection) {
        editButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
        editMenuItem.setEnabled(hasSelection);
        deleteMenuItem.setEnabled(hasSelection);
    }

    private void showPlayerDialog(ProxyStrike player) {
        boolean isNewPlayer = (player == null);
        if (isNewPlayer) {
            player = new ProxyStrike();
            player.setName("New Strike");
        }

        PlayerEditorPanel editorPanel = new PlayerEditorPanel(player);
        Dialog<ProxyStrike> dialog = new Dialog<>(player, editorPanel);
        dialog.setTitle(isNewPlayer ? "Add Player" : "Edit Player: " + player.getName());

        if (dialog.showDialog()) {
            ProxyStrike updatedPlayer = editorPanel.getUpdatedPlayer();
            ProxyStrike savedPlayer = savePlayerToRedis(updatedPlayer);
            logger.info("Saved player with ID: " + savedPlayer.getId());
            
            // Refresh the entire table instead of just updating one row
            loadPlayersFromRedis();
            
            // Select the newly added/edited player
            selectPlayerByName(savedPlayer.getName());
        }
    }

    // Add new helper method to select a player by name
    private void selectPlayerByName(String name) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (name.equals(model.getValueAt(i, 0))) {
                table.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private void editSelectedPlayer() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            ProxyStrike player = (ProxyStrike) getPlayerFromRow(row);
            showPlayerDialog(player);
        }
    }

    private void deleteSelectedPlayer() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            ProxyStrike player = (ProxyStrike) getPlayerFromRow(row);
            deletePlayerFromRedis(player);
            ((DefaultTableModel) table.getModel()).removeRow(row);
        }
    }

    private IProxyPlayer getPlayerFromRow(int row) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        String name = (String) model.getValueAt(row, 0);
        List<ProxyStrike> players = App.getRedisService().findAllStrikes();
        ProxyStrike existingPlayer = players.stream()
            .filter(p -> p.getName().equals(name))
            .findFirst()
            .orElse(null);

        if (existingPlayer != null) {
            return existingPlayer;
        }

        // If not found, create new ProxyStrike with values from the row
        ProxyStrike player = new ProxyStrike();
        player.setName(name);
        player.setChannel((int)((Long) model.getValueAt(row, 1)).longValue());
        player.setSwing((Long) model.getValueAt(row, 2));
        player.setLevel((Long) model.getValueAt(row, 3));
        player.setNote((Long) model.getValueAt(row, 4));
        player.setMinVelocity((Long) model.getValueAt(row, 5));
        player.setMaxVelocity((Long) model.getValueAt(row, 6));
        player.setPreset((Long) model.getValueAt(row, 7));
        player.setStickyPreset((Boolean) model.getValueAt(row, 8));
        player.setProbability((Long) model.getValueAt(row, 9));
        player.setRandomDegree((Long) model.getValueAt(row, 10));
        player.setRatchetCount((Long) model.getValueAt(row, 11));
        player.setRatchetInterval((Long) model.getValueAt(row, 12));
        player.setUseInternalBeats((Boolean) model.getValueAt(row, 13));
        player.setUseInternalBars((Boolean) model.getValueAt(row, 14));
        player.setPanPosition((Long) model.getValueAt(row, 15));
        player.setPreserveOnPurge((Boolean) model.getValueAt(row, 16));
        player.setSparse((Double) model.getValueAt(row, 17));

        return player;
    }

    private void updatePlayerTable(ProxyStrike player, int selectedRow) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        Object[] rowData = player.toRow();

        if (selectedRow >= 0) {
            for (int i = 0; i < rowData.length; i++) {
                model.setValueAt(rowData[i], selectedRow, i);
            }
        } else {
            model.addRow(rowData);
        }
    }

    private boolean isBooleanColumn(String columnName) {
        return columnName.equals("Sticky") ||
               columnName.equals("Int Beats") ||
               columnName.equals("Int Bars") ||
               columnName.equals("Preserve");
    }

    private void loadPlayersFromRedis() {
        try {
            logger.info("Starting to load players from Redis");
            List<ProxyStrike> players = App.getRedisService().findAllStrikes();
            logger.info("Found " + players.size() + " players in Redis");
            
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            for (ProxyStrike player : players) {
                Object[] rowData = player.toRow();
                model.addRow(rowData);
                logger.info("Added player to table: " + player.getName() + " (ID: " + player.getId() + ")");
            }
            
            // Select the first row if there are any players
            if (model.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
                
                // Enable edit/delete buttons
                editButton.setEnabled(true);
                deleteButton.setEnabled(true);
                editMenuItem.setEnabled(true);
                deleteMenuItem.setEnabled(true);
                
                // Get and publish the first player
                ProxyStrike firstPlayer = (ProxyStrike) getPlayerFromRow(0);
                publishPlayerSelected(firstPlayer);
            }
            
            status.setStatus("Loaded " + players.size() + " players from Redis");
        } catch (Exception e) {
            String error = "Error loading players: " + e.getMessage();
            logger.severe(error);
            status.setStatus(error);
            e.printStackTrace();
        }
    }

    private void publishPlayerSelected(IProxyPlayer player) {
        Command action = new Command();
        action.setCommand(Commands.PLAYER_SELECTED);
        action.setData(player);
        action.setSender(this);
        actionBus.publish(action);
    }

    private void publishPlayerUnselected() {
        Command action = new Command();
        action.setCommand(Commands.PLAYER_UNSELECTED);
        action.setSender(this);
        actionBus.publish(action);
    }

    @Override
    public void onAction(Command action) {
        switch (action.getCommand()) {
            case Commands.TICKER_LOADED:
            case Commands.TICKER_CHANGED:
                ProxyTicker ticker = (ProxyTicker) action.getData();
                refreshPlayerList(ticker);
                break;
            
            case Commands.PLAYER_ADDED_TO_TICKER:
            case Commands.PLAYER_REMOVED_FROM_TICKER:
                ProxyStrike player = (ProxyStrike) action.getData();
                refreshPlayerList(App.getTickerManager().getActiveTicker());
                break;
        }
    }

    private void refreshPlayerList(ProxyTicker ticker) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        if (ticker != null && ticker.getPlayers() != null) {
            for (IProxyPlayer player : ticker.getPlayers()) {
                model.addRow(player.toRow());
            }
        }
    }
}
