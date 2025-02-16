package com.angrysurfer.beats;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.panel.BackgroundPanel;
import com.angrysurfer.beats.panel.InstrumentsPanel;
import com.angrysurfer.beats.panel.LaunchPanel;
import com.angrysurfer.beats.panel.PlayerEditorPanel;
import com.angrysurfer.beats.panel.SystemsPanel;
import com.angrysurfer.beats.panel.TickerPanel;
import com.angrysurfer.beats.panel.WebPanel;
import com.angrysurfer.beats.panel.X0XPanel;
import com.angrysurfer.beats.panel.sorting.SortingVisualizerPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;

public class Frame extends JFrame implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(Frame.class.getName());

    private StatusBar statusBar = new StatusBar();
    private JTabbedPane tabbedPane;
    private final Map<Character, Integer> keyNoteMap;
    private BackgroundPanel backgroundPanel;

    public Frame() {
        super("Beats");
        this.keyNoteMap = setupKeyMap();
        
        // Create background panel first
        backgroundPanel = new BackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());
        setContentPane(backgroundPanel);
        
        setupFrame();
        setupMainContent();
        setupKeyboardManager();
    }

    private void setupFrame() {
        setPreferredSize(new Dimension(1200, 800));
        setMinimumSize(new Dimension(1200, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Make main content panel transparent
        backgroundPanel.setBackground(new Color(245, 245, 245, 200)); // Light background with some transparency
        
        setJMenuBar(new MenuBar(this, statusBar));
        add(new ToolBar(statusBar), BorderLayout.NORTH);
        add(statusBar, BorderLayout.SOUTH);
    }

    private Map<Character, Integer> setupKeyMap() {
        Map<Character, Integer> map = new HashMap<>();
        // White keys
        map.put('z', 60); // Middle C
        map.put('x', 62); // D
        map.put('c', 64); // E
        map.put('v', 65); // F
        map.put('b', 67); // G
        map.put('n', 69); // A
        map.put('m', 71); // B
        // Black keys
        map.put('s', 61); // C#
        map.put('d', 63); // D#
        map.put('g', 66); // F#
        map.put('h', 68); // G#
        map.put('j', 70); // A#
        return map;
    }

    private void setupKeyboardManager() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (tabbedPane.getSelectedComponent() instanceof TickerPanel) {
                    char keyChar = Character.toLowerCase(e.getKeyChar());

                    if (keyNoteMap.containsKey(keyChar)) {
                        int note = keyNoteMap.get(keyChar);
                        Command action = new Command();

                        switch (e.getID()) {
                            case KeyEvent.KEY_PRESSED -> {
                                // If shift is held, send KEY_HELD command
                                if (e.isShiftDown()) {
                                    action.setCommand(Commands.KEY_HELD);
                                } else {
                                    action.setCommand(Commands.KEY_PRESSED);
                                }
                                action.setData(note);
                                CommandBus.getInstance().publish(action);
                            }
                            case KeyEvent.KEY_RELEASED -> {
                                if (!e.isShiftDown()) {
                                    action.setCommand(Commands.KEY_RELEASED);
                                    action.setData(note);
                                    CommandBus.getInstance().publish(action);
                                }
                            }
                        }
                    }
                }
                return false; // Allow other key listeners to process the event
            }
        });
    }

    private void setupMainContent() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.setBorder(new EmptyBorder(2, 5, 2, 5));

        tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Players", new TickerPanel(statusBar));
        tabbedPane.addTab("Launch", new LaunchPanel());
        tabbedPane.addTab("X0X", new X0XPanel());
        tabbedPane.addTab("Instruments", new InstrumentsPanel());
        tabbedPane.addTab("System", new SystemsPanel(statusBar));

        tabbedPane.addTab("Params", new JPanel());
        tabbedPane.addTab("Controls", new JPanel());

        tabbedPane.addTab("Web", new WebPanel());
        tabbedPane.addTab("Sorting", new SortingVisualizerPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    public <T> Dialog<T> createDialog(T data, JPanel content) {
        return new Dialog<>(this, data, content);
    }

    public int getSelectedTab() {
        return tabbedPane.getSelectedIndex();
    }

    public void setSelectedTab(int index) {
        tabbedPane.setSelectedIndex(index);
    }

    // private void addPlayer() {
    //     ProxyStrike strike = new ProxyStrike();
    //     PlayerEditorPanel editor = new PlayerEditorPanel(strike, statusBar);
    //     Dialog<ProxyStrike> dialog = new Dialog<>(strike, editor);
    //     dialog.setTitle("Add Player");

    //     if (dialog.showDialog()) {
    //         try {
    //             ProxyStrike updatedStrike = editor.getUpdatedPlayer();
    //             ProxyStrike savedStrike = App.getRedisService().saveStrike(updatedStrike);

    //             Component selectedComponent = tabbedPane.getSelectedComponent();
    //             if (selectedComponent instanceof TickerPanel) {
    //                 TickerPanel tickerPanel = (TickerPanel) selectedComponent;
    //                 ProxyTicker currentTicker = tickerPanel.getTicker();
                    
    //                 currentTicker.getPlayers().add(savedStrike);
    //                 savedStrike.setTicker(currentTicker);
    //                 App.getRedisService().saveTicker(currentTicker);

    //                 // Use consistent command
    //                 Command cmd = new Command(Commands.PLAYER_ADDED, this, savedStrike);
    //                 CommandBus.getInstance().publish(cmd);
                    
    //                 logger.info("Added player: " + savedStrike.getName());
    //                 statusBar.setStatus("Added player: " + savedStrike.getName());
                    
    //             } else {
    //                 logger.warning("Cannot add player - not on ticker panel");
    //                 statusBar.setStatus("Please switch to Players tab to add players");
    //             }
    //         } catch (Exception e) {
    //             logger.severe("Error saving new player: " + e.getMessage());
    //             statusBar.setStatus("Error saving player: " + e.getMessage());
    //         }
    //     }
    // }

    @Override
    public void close() {
        // Clean up ActionBus subscriptions for child components
        if (tabbedPane != null) {
            for (Component comp : tabbedPane.getComponents()) {
                if (comp instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) comp).close();
                    } catch (Exception e) {
                        logger.warning("Error closing component: " + e.getMessage());
                    }
                }
            }
        }
        dispose();
    }
}
