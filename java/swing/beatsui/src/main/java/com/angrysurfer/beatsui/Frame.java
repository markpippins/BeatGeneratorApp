package com.angrysurfer.beatsui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beatsui.panel.LaunchPanel;
import com.angrysurfer.beatsui.panel.InstrumentsPanel;
import com.angrysurfer.beatsui.panel.PlayerPanel;
import com.angrysurfer.beatsui.panel.SystemsPanel;
import com.angrysurfer.beatsui.panel.X0XPanel;
import com.angrysurfer.beatsui.widget.StatusBar;
import com.angrysurfer.beatsui.api.Action;
import com.angrysurfer.beatsui.api.ActionBus;
import com.angrysurfer.beatsui.api.Commands;

public class Frame extends JFrame {

    private StatusBar statusBar = new StatusBar();
    private JTabbedPane tabbedPane;
    private final Map<Character, Integer> keyNoteMap;

    public Frame() {
        super("Beats");
        this.keyNoteMap = setupKeyMap();
        setupFrame();
        setupMainContent();
        setupKeyboardManager();
    }

    private void setupFrame() {
        setPreferredSize(new Dimension(1200, 800));
        setMinimumSize(new Dimension(1200, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setJMenuBar(new MenuBar(this, statusBar)); // We're passing statusBar correctly here
        add(new ToolBar(), BorderLayout.NORTH);
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
                if (tabbedPane.getSelectedComponent() instanceof PlayerPanel) {
                    char keyChar = Character.toLowerCase(e.getKeyChar());

                    if (keyNoteMap.containsKey(keyChar)) {
                        int note = keyNoteMap.get(keyChar);
                        Action action = new Action();

                        switch (e.getID()) {
                            case KeyEvent.KEY_PRESSED -> {
                                // If shift is held, send KEY_HELD command
                                if (e.isShiftDown()) {
                                    action.setCommand(Commands.KEY_HELD);
                                } else {
                                    action.setCommand(Commands.KEY_PRESSED);
                                }
                                action.setData(note);
                                ActionBus.getInstance().publish(action);
                            }
                            case KeyEvent.KEY_RELEASED -> {
                                if (!e.isShiftDown()) {
                                    action.setCommand(Commands.KEY_RELEASED);
                                    action.setData(note);
                                    ActionBus.getInstance().publish(action);
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

        tabbedPane.addTab("Players", new PlayerPanel(statusBar));
        tabbedPane.addTab("Launch", new LaunchPanel());
        tabbedPane.addTab("X0X", new X0XPanel());

        tabbedPane.addTab("Params", new JPanel());
        tabbedPane.addTab("Controls", new JPanel());

        tabbedPane.addTab("Instruments", new InstrumentsPanel());
        tabbedPane.addTab("System", new SystemsPanel(statusBar));

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

}
