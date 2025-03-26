package com.angrysurfer.beats.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;

/**
 * Panel containing transport controls (play, stop, record, etc.)
 */
public class TransportPanel extends JPanel {
    private final CommandBus commandBus = CommandBus.getInstance();
    
    // Transport controls
    private JButton playButton;
    private JButton stopButton;
    private JButton recordButton;
    private JButton rewindButton;
    private JButton forwardButton;
    private JButton pauseButton;
    private boolean isRecording = false;

    public TransportPanel() {
        super(new FlowLayout(FlowLayout.CENTER));
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        setPreferredSize(new Dimension(getPreferredSize().width, 75));
        
        setupTransportButtons();
        setupCommandBusListener();
        
        // Set initial button states
        playButton.setEnabled(true);
        stopButton.setEnabled(false);
    }
    
    private void setupTransportButtons() {
        rewindButton = createToolbarButton(Commands.TRANSPORT_REWIND, "⏮", "Previous Session");
        pauseButton = createToolbarButton(Commands.TRANSPORT_PAUSE, "⏸", "Pause");
        
        recordButton = new JButton("⏺");
        recordButton.setToolTipText("Record");
        recordButton.setEnabled(true);
        recordButton.setActionCommand(Commands.TRANSPORT_RECORD);
        
        int size = 32;
        recordButton.setPreferredSize(new Dimension(size, size));
        recordButton.setMinimumSize(new Dimension(size, size));
        recordButton.setMaximumSize(new Dimension(size, size));
        recordButton.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        if (!recordButton.getFont().canDisplay('⏺')) {
            recordButton.setFont(new Font("Dialog", Font.PLAIN, 18));
        }
        recordButton.setMargin(new Insets(0, 0, 0, 0));
        recordButton.setFocusPainted(false);
        recordButton.setVerticalAlignment(SwingConstants.CENTER);
        recordButton.setBorderPainted(false);
        recordButton.setContentAreaFilled(true);
        recordButton.setOpaque(true);
        
        recordButton.addActionListener(e -> {
            toggleRecordingState();
            
            if (isRecording) {
                commandBus.publish(Commands.TRANSPORT_RECORD_START, this);
            } else {
                commandBus.publish(Commands.TRANSPORT_RECORD_STOP, this);
            }
        });
        
        stopButton = createToolbarButton(Commands.TRANSPORT_STOP, "⏹", "Stop");
        playButton = createToolbarButton(Commands.TRANSPORT_PLAY, "▶", "Play");
        forwardButton = createToolbarButton(Commands.TRANSPORT_FORWARD, "⏭", "Next Session");

        add(rewindButton);
        add(pauseButton);
        add(stopButton);
        add(recordButton);
        add(playButton);
        add(forwardButton);
        
        updateRecordButtonAppearance();
    }
    
    private JButton createToolbarButton(String command, String text, String tooltip) {
        JButton button = new JButton(text);
        
        button.setToolTipText(tooltip);
        button.setEnabled(true);
        button.setActionCommand(command);
        button.addActionListener(e -> commandBus.publish(command, this));
    
        // Styling
        int size = 32;
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));
        
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        
        if (!button.getFont().canDisplay('⏮')) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }
    
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setVerticalAlignment(SwingConstants.CENTER);
        
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(button.getBackground().brighter());
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(UIManager.getColor("Button.background"));
            }
        });
    
        return button;
    }

    private void toggleRecordingState() {
        isRecording = !isRecording;
        updateRecordButtonAppearance();
    }

    private void updateRecordButtonAppearance() {
        if (isRecording) {
            recordButton.setBackground(Color.RED);
            recordButton.setForeground(Color.WHITE);
        } else {
            recordButton.setBackground(UIManager.getColor("Button.background"));
            recordButton.setForeground(UIManager.getColor("Button.foreground"));
        }
    }
    
    /**
     * Update the enabled state of transport buttons based on session state
     */
    public void updateTransportState(Session session) {
        boolean hasActiveSession = Objects.nonNull(session);
        
        if (session == null) {
            rewindButton.setEnabled(false);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            recordButton.setEnabled(false);
            playButton.setEnabled(false);
            forwardButton.setEnabled(false);
            return;
        }
        
        // Update states for all buttons
        rewindButton.setEnabled(hasActiveSession && SessionManager.getInstance().canMoveBack());
        forwardButton.setEnabled(SessionManager.getInstance().canMoveForward());
        pauseButton.setEnabled(false); // Pause currently not implemented
        playButton.setEnabled(hasActiveSession && !session.isRunning());
        stopButton.setEnabled(hasActiveSession && session.isRunning());
        recordButton.setEnabled(hasActiveSession);
    }
    
    private void setupCommandBusListener() {
        commandBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                // Skip if this panel is the sender
                if (action.getSender() == TransportPanel.this) {
                    return;
                }

                if (Objects.nonNull(action.getCommand())) {
                    switch (action.getCommand()) {
                        case Commands.TRANSPORT_STATE_CHANGED:
                            if (action.getData() instanceof Boolean) {
                                Boolean isPlaying = (Boolean) action.getData();
                                SwingUtilities.invokeLater(() -> {
                                    playButton.setEnabled(!isPlaying);
                                    stopButton.setEnabled(isPlaying);
                                });
                            }
                            break;
                        case Commands.TRANSPORT_RECORD_START:
                            isRecording = true;
                            updateRecordButtonAppearance();
                            break;
                        case Commands.TRANSPORT_RECORD_STOP:
                            isRecording = false;
                            updateRecordButtonAppearance();
                            break;
                        case Commands.TRANSPORT_STOP:
                            // Also stop recording when transport is stopped
                            isRecording = false;
                            updateRecordButtonAppearance();
                            break;
                        case Commands.SESSION_CREATED:
                            if (action.getData() instanceof Session) {
                                Session session = (Session) action.getData();
                                updateTransportState(session);
                                // Force disable forward button for new session
                                forwardButton.setEnabled(false);
                            }
                            break;
                    }
                }
            }
        });
    }
}