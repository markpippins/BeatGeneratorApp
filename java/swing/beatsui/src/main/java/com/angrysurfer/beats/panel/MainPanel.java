package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.FlowLayout;
import java.io.File;
import java.net.URISyntaxException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.StatusBar;
import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;

public class MainPanel extends JPanel implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(MainPanel.class.getName());
    private JTabbedPane tabbedPane;

    public MainPanel(StatusBar statusBar) {
        super(new BorderLayout());
        setBorder(new EmptyBorder(2, 5, 2, 5));
        setupTabbedPane(statusBar);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupTabbedPane(StatusBar statusBar) {
        tabbedPane = new JTabbedPane();

        // Add tabs as before
        tabbedPane.addTab("X0X", new X0XPanel());
        tabbedPane.addTab("Players", new SessionPanel());
        tabbedPane.addTab("Instruments", new InstrumentsPanel());
        tabbedPane.addTab("Launch", new LaunchPanel());
        tabbedPane.addTab("System", new SystemsPanel());

        // Create a panel for the trailing component with proper vertical alignment
        JPanel tabToolbar = new JPanel();
        tabToolbar.setLayout(new BoxLayout(tabToolbar, BoxLayout.X_AXIS));
        tabToolbar.setOpaque(false);

        // Add vertical glue to center the buttons vertically
        tabToolbar.add(Box.createVerticalGlue());

        // Create a panel for the buttons with margins
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        // Add both buttons - notes off first, then metronome
        buttonPanel.add(createAllNotesOffButton());
        buttonPanel.add(createMetronomeToggleButton());

        // Add restart button
        buttonPanel.add(createRestartButton());

        tabToolbar.add(buttonPanel);
        tabToolbar.add(Box.createVerticalGlue());

        // Add the toolbar to the right side of the tabbed pane
        tabbedPane.putClientProperty("JTabbedPane.trailingComponent", tabToolbar);
    }

    private JToggleButton createMetronomeToggleButton() {
        // Create a FlatLaf toggle button
        JToggleButton metronomeButton = new JToggleButton();

        // Set metronome icon - use Unicode character for metronome
        metronomeButton.setText("⏱");
        metronomeButton.setBackground(ColorUtils.mutedRed);
        // Make it square with 28px size
        metronomeButton.setPreferredSize(new Dimension(28, 28));
        metronomeButton.setMinimumSize(new Dimension(28, 28));
        metronomeButton.setMaximumSize(new Dimension(28, 28));

        // Use FlatLaf styling - roundRect gives nice rounded corners
        metronomeButton.putClientProperty("JButton.buttonType", "roundRect");
        metronomeButton.putClientProperty("JButton.squareSize", true);

        // Set font to be proportional to the button size
        metronomeButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));

        // Center the text/icon
        metronomeButton.setHorizontalAlignment(SwingConstants.CENTER);
        metronomeButton.setVerticalAlignment(SwingConstants.CENTER);

        // Remove content margin to utilize full button space
        metronomeButton.setMargin(new Insets(0, 0, 0, 0));

        // Set tooltip
        metronomeButton.setToolTipText("Toggle Metronome");

        // Add action listener
        metronomeButton.addActionListener(e -> {
            boolean isSelected = metronomeButton.isSelected();
            logger.info("Metronome toggled: " + (isSelected ? "ON" : "OFF"));

            // Publish the command to toggle metronome
            CommandBus.getInstance().publish(isSelected ? Commands.METRONOME_START : Commands.METRONOME_STOP, this);

            // Optional: change text based on state for visual feedback
            metronomeButton.setText(isSelected ? "⏱" : "⏱");
            metronomeButton.setBackground(isSelected ? ColorUtils.mutedOlive : ColorUtils.mutedRed);
        });

        // Register for command bus events to sync button state
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                switch (action.getCommand()) {
                case Commands.METRONOME_STARTED:
                    SwingUtilities.invokeLater(() -> {
                        metronomeButton.setSelected(true);
                        metronomeButton.setBackground(Color.GREEN);
                        metronomeButton.invalidate();
                        metronomeButton.repaint();
                    });

                    break;

                case Commands.METRONOME_STOPPED:
                    SwingUtilities.invokeLater(() -> {
                        metronomeButton.setSelected(false);
                        metronomeButton.setBackground(Color.RED);
                        metronomeButton.invalidate();
                        metronomeButton.repaint();
                    });

                    break;

                }
            }
        });

        return metronomeButton;
    }

    private JButton createAllNotesOffButton() {
        // Create a regular button (not a toggle)
        JButton notesOffButton = new JButton();

        // Set a stop/mute symbol - using Unicode stop symbol
        notesOffButton.setText("■");

        // Make it square with 28px size to match metronome button
        notesOffButton.setPreferredSize(new Dimension(28, 28));
        notesOffButton.setMinimumSize(new Dimension(28, 28));
        notesOffButton.setMaximumSize(new Dimension(28, 28));

        // Use FlatLaf styling - roundRect gives nice rounded corners
        notesOffButton.putClientProperty("JButton.buttonType", "roundRect");
        notesOffButton.putClientProperty("JButton.squareSize", true);

        // Set font to be proportional to the button size
        notesOffButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));

        // Center the text/icon
        notesOffButton.setHorizontalAlignment(SwingConstants.CENTER);
        notesOffButton.setVerticalAlignment(SwingConstants.CENTER);

        // Remove content margin to utilize full button space
        notesOffButton.setMargin(new Insets(0, 0, 0, 0));

        // Set tooltip
        notesOffButton.setToolTipText("All Notes Off - Silence All Sounds");

        // Add action listener
        notesOffButton.addActionListener(e -> {
            logger.info("All Notes Off button pressed");

            // Publish the command to stop all notes
            CommandBus.getInstance().publish(Commands.ALL_NOTES_OFF, this);
        });

        return notesOffButton;
    }

    private JButton createRestartButton() {
        JButton restartButton = new JButton("Restart App");
        restartButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    null, 
                    "This will restart the application. Any unsaved changes will be lost.\nContinue?",
                    "Restart Application", 
                    JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                try {
                    // Close this app instance
                    System.exit(0);
                    
                    // Restart the application
                    String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                    File currentJar = new File(MainPanel.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    
                    // Build command: java -jar currentJar
                    if(currentJar.getName().endsWith(".jar")) {
                        ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", currentJar.getPath());
                        builder.start();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, 
                        "Error restarting: " + ex.getMessage(),
                        "Restart Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return restartButton;
    }

    public int getSelectedTab() {
        return tabbedPane.getSelectedIndex();
    }

    public void setSelectedTab(int index) {
        tabbedPane.setSelectedIndex(index);
    }

    public Component getSelectedComponent() {
        return tabbedPane.getSelectedComponent();
    }

    @Override
    public void close() throws Exception {
        if (tabbedPane != null) {
            for (Component comp : tabbedPane.getComponents()) {
                if (comp instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) comp).close();
                    } catch (Exception e) {
                        logger.error("Error closing component: " + e.getMessage());
                    }
                }
            }
        }
    }
}
