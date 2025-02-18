package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
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

    public PlayersPanel(StatusConsumer status, RulesPanel ruleTablePanel) {
        super(new BorderLayout());
        this.status = status;
        this.ruleTablePanel = ruleTablePanel;
        this.table = new JTable();
        setupTable();
        setupLayout();
        setupCommandBusListener();
        
        // Request initial ticker state through CommandBus
        SwingUtilities.invokeLater(() -> {
            CommandBus.getInstance().publish(Commands.TICKER_REQUEST, this);
        });
    }

    private void setupLayout() {
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void setupTable() {
        String[] columnNames = {
            "Name", "Channel", "Swing", "Level", "Note", "Min Vel", "Max Vel",
            "Preset", "Sticky", "Prob", "Random", "Ratchet #", "Ratchet Int",
            "Int Beats", "Int Bars", "Pan", "Preserve", "Sparse"
        };

        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        table.setModel(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
    }

    private void setupCommandBusListener() {
        CommandBus.getInstance().register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                if (action.getData() instanceof ProxyTicker ticker) {
                    switch (action.getCommand()) {
                        case Commands.TICKER_SELECTED, Commands.TICKER_UPDATED, Commands.TICKER_LOADED -> {
                            refreshPlayers(ticker.getPlayers());
                            if (!ticker.getPlayers().isEmpty()) {
                                // Select first player by default
                                table.setRowSelectionInterval(0, 0);
                                // Notify about player selection
                                ProxyStrike selectedPlayer = (ProxyStrike) ticker.getPlayers().iterator().next();
                                CommandBus.getInstance().publish(Commands.PLAYER_SELECTED, this, selectedPlayer);
                            }
                        }
                    }
                }
            }
        });

        // Add selection listener to notify when user selects a player
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                String playerName = (String) model.getValueAt(modelRow, 0);
                // Find player by name and notify
                ProxyTicker currentTicker = TickerManager.getInstance().getActiveTicker();
                if (currentTicker != null) {
                    currentTicker.getPlayers().stream()
                        .filter(p -> p.getName().equals(playerName))
                        .findFirst()
                        .ifPresent(player -> 
                            CommandBus.getInstance().publish(Commands.PLAYER_SELECTED, this, player));
                }
            }
        });
    }

    public void refreshPlayers(Set<IProxyPlayer> players) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        if (players != null) {
            List<IProxyPlayer> sortedPlayers = new ArrayList<>(players);
            Collections.sort(sortedPlayers, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            
            for (IProxyPlayer player : sortedPlayers) {
                model.addRow(player.toRow());
            }
        }
    }
}
