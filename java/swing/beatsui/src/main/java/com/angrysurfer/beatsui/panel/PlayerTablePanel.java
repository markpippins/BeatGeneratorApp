package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;

import com.angrysurfer.beatsui.Dialog;
import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.mock.Strike;
import com.angrysurfer.beatsui.Utils;
import com.angrysurfer.beatsui.App;
import com.angrysurfer.beatsui.api.Action;
import com.angrysurfer.beatsui.api.ActionBus;
import com.angrysurfer.beatsui.api.Commands;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerTablePanel extends JPanel {
    private static final Logger logger = Logger.getLogger(PlayerTablePanel.class.getName());
    private final JTable table;
    private final StatusConsumer status;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JMenuItem editMenuItem;
    private final JMenuItem deleteMenuItem;
    private final RuleTablePanel ruleTablePanel;
    private final ActionBus actionBus = ActionBus.getInstance();

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

    private Strike savePlayerToRedis(Strike player) {
        try {
            Strike savedPlayer = App.getRedisService().saveStrike(player);
            status.setStatus("Saved player: " + player.getName() + " (ID: " + savedPlayer.getId() + ")");
            return savedPlayer;
        } catch (Exception ex) {
            status.setStatus("Error saving player: " + ex.getMessage());
            ex.printStackTrace();
            return player;
        }
    }

    private void deletePlayerFromRedis(Strike player) {
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

        // Column setup
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(120);
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
                editButton.setEnabled(hasSelection);
                deleteButton.setEnabled(hasSelection);
                editMenuItem.setEnabled(hasSelection);
                deleteMenuItem.setEnabled(hasSelection);
                
                if (hasSelection) {
                    Strike selectedPlayer = getPlayerFromRow(table.getSelectedRow());
                    ruleTablePanel.setSelectedPlayer(selectedPlayer);
                    status.setStatus("Selected player: " + selectedPlayer.getName());
                } else {
                    ruleTablePanel.setSelectedPlayer(null);
                }
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    Strike player = getPlayerFromRow(selectedRow);
                    publishPlayerSelected(player);
                } else {
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

    private void showPlayerDialog(Strike player) {
        boolean isNewPlayer = (player == null);
        if (isNewPlayer) {
            player = new Strike();
            player.setName("New Strike");
        }

        PlayerEditorPanel editorPanel = new PlayerEditorPanel(player);
        Dialog<Strike> dialog = new Dialog<>(player, editorPanel);
        dialog.setTitle(isNewPlayer ? "Add Player" : "Edit Player: " + player.getName());

        if (dialog.showDialog()) {
            Strike updatedPlayer = editorPanel.getUpdatedPlayer();
            Strike savedPlayer = savePlayerToRedis(updatedPlayer);
            logger.info("Saved player with ID: " + savedPlayer.getId());
            updatePlayerTable(savedPlayer, table.getSelectedRow());
            
            // If this was a new player, select it
            if (isNewPlayer) {
                int lastRow = table.getModel().getRowCount() - 1;
                table.setRowSelectionInterval(lastRow, lastRow);
            }
        }
    }

    private void editSelectedPlayer() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            Strike player = getPlayerFromRow(row);
            showPlayerDialog(player);
        }
    }

    private void deleteSelectedPlayer() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            Strike player = getPlayerFromRow(row);
            deletePlayerFromRedis(player);
            ((DefaultTableModel) table.getModel()).removeRow(row);
        }
    }

    private Strike getPlayerFromRow(int row) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        Strike player = new Strike();
        
        // Get the ID from Redis based on the name
        String name = (String) model.getValueAt(row, 0);
        List<Strike> players = App.getRedisService().findAllStrikes();
        Strike existingPlayer = players.stream()
            .filter(p -> p.getName().equals(name))
            .findFirst()
            .orElse(null);
        
        if (existingPlayer != null) {
            player.setId(existingPlayer.getId());
        }
        
        player.setName(name);
        player.setChannel(((Number) model.getValueAt(row, 1)).intValue());
        player.setSwing(((Number) model.getValueAt(row, 2)).longValue());
        player.setLevel(((Number) model.getValueAt(row, 3)).longValue());
        player.setMinVelocity(((Number) model.getValueAt(row, 5)).longValue());
        player.setMaxVelocity(((Number) model.getValueAt(row, 6)).longValue());
        player.setPreset(((Number) model.getValueAt(row, 7)).longValue());
        player.setStickyPreset((Boolean) model.getValueAt(row, 8));
        player.setProbability(((Number) model.getValueAt(row, 9)).longValue());
        player.setRandomDegree(((Number) model.getValueAt(row, 10)).longValue());
        player.setRatchetCount(((Number) model.getValueAt(row, 11)).longValue());
        player.setRatchetInterval(((Number) model.getValueAt(row, 12)).longValue());
        player.setUseInternalBeats((Boolean) model.getValueAt(row, 13));
        player.setUseInternalBars((Boolean) model.getValueAt(row, 14));
        player.setPanPosition(((Number) model.getValueAt(row, 15)).longValue());
        player.setPreserveOnPurge((Boolean) model.getValueAt(row, 16));
        player.setSparse((Double) model.getValueAt(row, 17));

        return player;
    }

    private void updatePlayerTable(Strike player, int selectedRow) {
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
            List<Strike> players = App.getRedisService().findAllStrikes();
            logger.info("Found " + players.size() + " players in Redis");
            
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            for (Strike player : players) {
                Object[] rowData = player.toRow();
                model.addRow(rowData);
                logger.info("Added player to table: " + player.getName() + " (ID: " + player.getId() + ")");
            }
            
            status.setStatus("Loaded " + players.size() + " players from Redis");
        } catch (Exception e) {
            String error = "Error loading players: " + e.getMessage();
            logger.severe(error);
            status.setStatus(error);
            e.printStackTrace();
        }
    }

    private void publishPlayerSelected(Strike player) {
        Action action = new Action();
        action.setCommand(Commands.PLAYER_SELECTED);
        action.setData(player);
        action.setSender(this);
        actionBus.publish(action);
    }

    private void publishPlayerUnselected() {
        Action action = new Action();
        action.setCommand(Commands.PLAYER_UNSELECTED);
        action.setSender(this);
        actionBus.publish(action);
    }
}
