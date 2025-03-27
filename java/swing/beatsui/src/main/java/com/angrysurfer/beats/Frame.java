package com.angrysurfer.beats;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.PlayersTable;
import com.angrysurfer.beats.widget.panel.MainPanel;
import com.angrysurfer.beats.widget.panel.PlayersPanel;
import com.angrysurfer.beats.widget.panel.SessionPanel;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.util.Constants;

public class Frame extends JFrame implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Frame.class.getName());

    private StatusBar statusBar = new StatusBar();

    private final Map<Character, Integer> keyNoteMap;
    // private BackgroundPanel backgroundPanel;
    private MainPanel mainPanel;

    public Frame() {
        super("Beats");
        this.keyNoteMap = setupKeyMap();

        // Create background panel first
        // backgroundPanel = new BackgroundPanel();
        // backgroundPanel.setLayout(new BorderLayout());
        // setContentPane(backgroundPanel);

        setupFrame();
        setupMainContent();
        setupKeyboardManager();

        // Initialize DialogService with this frame
        DialogManager.initialize(this);
    }

    public void loadFrameState() {
        logger.info("Loading frame state for window");
        FrameState state = RedisService.getInstance().loadFrameState(Constants.APPLICATION_FRAME);
        logger.info("Frame state loaded: " + (state != null));

        if (state != null) {
            setSize(state.getFrameSizeX(), state.getFrameSizeY());
            setLocation(state.getFramePosX(), state.getFramePosY());
            setSelectedTab(state.getSelectedTab());

            // Restore window state
            if (state.isMaximized()) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            } else if (state.isMinimized()) {
                setExtendedState(JFrame.ICONIFIED);
            } else {
                setExtendedState(JFrame.NORMAL);
            }

            logger.info("Applied frame state: " +
                    "size=" + state.getFrameSizeX() + "x" + state.getFrameSizeY() +
                    ", pos=" + state.getFramePosX() + "," + state.getFramePosY() +
                    ", tab=" + state.getSelectedTab() +
                    ", maximized=" + state.isMaximized() +
                    ", minimized=" + state.isMinimized());
        }

        // Add window listener for saving state on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveFrameState();
            }
        });
    }

    public void saveFrameState() {
        try {
            FrameState currentState = new FrameState();
            currentState.setSelectedTab(getSelectedTab());

            // Save normal window bounds even when maximized
            if (getExtendedState() == JFrame.MAXIMIZED_BOTH || getExtendedState() == JFrame.ICONIFIED) {
                currentState.setFrameSizeX((int) getPreferredSize().getWidth());
                currentState.setFrameSizeY((int) getPreferredSize().getHeight());
                // currentState.setFramePosX(getX());
                // currentState.setFramePosY(getY());
            } else {
                currentState.setFrameSizeX(getWidth());
                currentState.setFrameSizeY(getHeight());
                currentState.setFramePosX(getX());
                currentState.setFramePosY(getY());
            }

            // Save window state
            currentState.setMaximized(getExtendedState() == JFrame.MAXIMIZED_BOTH);
            currentState.setMinimized(getExtendedState() == JFrame.ICONIFIED);
            currentState.setLookAndFeelClassName(UIManager.getLookAndFeel().getClass().getName());

            logger.info("Saving frame state: " +
                    "size=" + getWidth() + "x" + getHeight() +
                    ", pos=" + getX() + "," + getY() +
                    ", tab=" + getSelectedTab() +
                    ", maximized=" + currentState.isMaximized() +
                    ", minimized=" + currentState.isMinimized());

            RedisService.getInstance().saveFrameState(currentState, Constants.APPLICATION_FRAME);
        } catch (Exception e) {
            logger.error("Error saving frame state: " + e.getMessage());
        }
    }

    private void setupFrame() {
        setPreferredSize(new Dimension(1200, 800));
        setMinimumSize(new Dimension(1200, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Add component listener for resize and move events
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (isShowing()) { // Only handle if window is visible
                    CommandBus.getInstance().publish(Commands.WINDOW_RESIZED, this);
                    saveFrameState();
                }
            }

            public void componentMoved(java.awt.event.ComponentEvent e) {
                if (isShowing() && getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                    // Don't save position when maximized
                    saveFrameState();
                }
            }
        });

        // Make main content panel transparent
        // backgroundPanel.setBackground(new Color(245, 245, 245, 200)); // Light
        // background with some transparency

        setJMenuBar(new MenuBar(this, statusBar));
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
                // Only process KEY_PRESSED events to avoid duplicate handling
                if (e.getID() != KeyEvent.KEY_PRESSED) {
                    return false;
                }
                
                // Handle MIDI key mapping for piano
                if (mainPanel != null && mainPanel.getSelectedComponent() instanceof SessionPanel) {
                    char keyChar = Character.toLowerCase(e.getKeyChar());

                    // Special case for 'a' key - play active player's current note
                    if (keyChar == 'a') {
                        Player activePlayer = PlayerManager.getInstance().getActivePlayer();
                        if (activePlayer != null && activePlayer.getNote() != null) {
                            int playerNote = activePlayer.getNote().intValue();
                            logger.info("A key pressed - Playing active player's note: " + playerNote);
                            
                            // Determine command based on shift key
                            String command = e.isShiftDown() ? Commands.KEY_HELD : Commands.KEY_PRESSED;
                            CommandBus.getInstance().publish(command, this, playerNote);
                            
                            // Consume the event
                            e.consume();
                            return true;
                        }
                    }
                    
                    // Regular MIDI piano key handling (existing code)
                    else if (keyNoteMap.containsKey(keyChar)) {
                        // Get base note (for octave 5)
                        int baseNote = keyNoteMap.get(keyChar);
                        
                        // Adjust for active player's octave
                        Player activePlayer = PlayerManager.getInstance().getActivePlayer();
                        int noteToPlay = baseNote;
                        
                        if (activePlayer != null && activePlayer.getNote() != null) {
                            int playerOctave = activePlayer.getNote().intValue() / 12;
                            int baseOctave = 5; // Default keyboard mapping is in octave 5
                            
                            // Adjust the note by the octave difference
                            noteToPlay = baseNote + ((playerOctave - baseOctave) * 12);
                            
                            // Ensure within valid MIDI range
                            noteToPlay = Math.max(0, Math.min(127, noteToPlay));
                            logger.info("Key " + keyChar + " mapped to note " + noteToPlay + 
                                      " (player octave: " + playerOctave + ")");
                        }

                        String command = e.isShiftDown() ? Commands.KEY_HELD : Commands.KEY_PRESSED;
                        CommandBus.getInstance().publish(command, this, noteToPlay);
                        
                        // Consume the event
                        e.consume();
                        return true;
                    }
                    
                    // Handle arrow key navigation for PlayersTable
                    int keyCode = e.getKeyCode();
                    if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
                        // Find the session panel
                        SessionPanel sessionPanel = (SessionPanel) mainPanel.getSelectedComponent();
                        PlayersPanel playersPanel = sessionPanel.getPlayerTablePanel();
                        PlayersTable playersTable = playersPanel.getTable();
                        
                        // Don't intercept if any of these conditions are true:
                        // 1. Dialog is active (check if any dialog is showing)
                        // 2. Rules table has focus
                        // 3. Text field or similar component has focus
                        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                        boolean isRulesTableFocused = focusOwner != null && 
                                                     (focusOwner == sessionPanel.getRuleTablePanel().getTable() || 
                                                      focusOwner.getParent() == sessionPanel.getRuleTablePanel().getTable());
                        boolean isTextComponentFocused = focusOwner instanceof javax.swing.text.JTextComponent;
                        
                        // If none of these conditions are true, handle arrow key navigation
                        if (!isModalDialogShowing() && !isRulesTableFocused && !isTextComponentFocused) {
                            int currentRow = playersTable.getSelectedRow();
                            int rowCount = playersTable.getRowCount();
                            
                            if (rowCount > 0) {
                                if (currentRow < 0) {
                                    // No row selected, select first row
                                    playersTable.setRowSelectionInterval(0, 0);
                                    playersTable.handlePlayerSelection(0);
                                    playersTable.scrollRectToVisible(playersTable.getCellRect(0, 0, true));
                                } else {
                                    // Calculate new row based on key
                                    int newRow = (keyCode == KeyEvent.VK_UP) ? 
                                        Math.max(0, currentRow - 1) : 
                                        Math.min(rowCount - 1, currentRow + 1);
                                    
                                    if (newRow != currentRow) {
                                        playersTable.setRowSelectionInterval(newRow, newRow);
                                        playersTable.handlePlayerSelection(newRow);
                                        playersTable.scrollRectToVisible(playersTable.getCellRect(newRow, 0, true));
                                    }
                                }
                                
                                // Consume the event so it isn't processed elsewhere
                                e.consume();
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        });
    }

    // Helper method to check if any modal dialog is showing
    private boolean isModalDialogShowing() {
        // Check if any modal dialogs are active
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window instanceof Dialog && ((Dialog) window).isModal()) {
                return true;
            }
        }
        return false;
    }

    private void setupMainContent() {
        mainPanel = new MainPanel(statusBar);
        add(mainPanel, BorderLayout.CENTER);

        // Replace direct access with command
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                CommandBus.getInstance().publish(Commands.WINDOW_CLOSING, this);
                saveFrameState();
            }
        });
    }

    public <T> Dialog<T> createDialog(T data, JPanel content) {
        logger.info("Creating dialog with content: " + content.getClass().getSimpleName());
        Dialog<T> dialog = new Dialog<>(this, data, content);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        logger.info("Dialog created and positioned");
        return dialog;
    }

    public int getSelectedTab() {
        return mainPanel.getSelectedTab();
    }

    public void setSelectedTab(int index) {
        mainPanel.setSelectedTab(index);
    }

    @Override
    public void close() {
        // Clean up ActionBus subscriptions for child components
        if (mainPanel != null) {
            try {
                mainPanel.close();
            } catch (Exception e) {
                logger.error("Error closing main panel: " + e.getMessage());
            }
        }
        dispose();
    }
}
