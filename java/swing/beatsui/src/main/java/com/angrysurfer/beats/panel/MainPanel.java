package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.StatusBar;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.BusListener;
import com.angrysurfer.core.api.Commands;

public class MainPanel extends JPanel implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(MainPanel.class.getName());
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
        tabbedPane.addTab("Players", new SessionPanel(statusBar));
        tabbedPane.addTab("Instruments", new InstrumentsPanel());
        tabbedPane.addTab("Launch", new LaunchPanel());
        tabbedPane.addTab("System", new SystemsPanel(statusBar));
        // tabbedPane.addTab("Sorting", new SortingVisualizerPanel());
        
        // Create a panel for the trailing component with proper vertical alignment
        JPanel tabToolbar = new JPanel();
        tabToolbar.setLayout(new BoxLayout(tabToolbar, BoxLayout.X_AXIS));
        tabToolbar.setOpaque(false);
        
        // Add vertical glue to center the button vertically
        tabToolbar.add(Box.createVerticalGlue());
        
        // Create a panel for the button with margins
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        buttonPanel.add(createMetronomeToggleButton(), BorderLayout.CENTER);
        
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
        
        // Make it square with 38px size
        metronomeButton.setPreferredSize(new Dimension(38, 38));
        metronomeButton.setMinimumSize(new Dimension(38, 38));
        metronomeButton.setMaximumSize(new Dimension(38, 38));
        
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
            CommandBus.getInstance().publish(
                isSelected ? Commands.METRONOME_START : Commands.METRONOME_STOP, 
                this
            );
            
            // Optional: change text based on state for visual feedback
            metronomeButton.setText(isSelected ? "⏱" : "⏱");
        });
        
        // Register for command bus events to sync button state
        CommandBus.getInstance().register(new BusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) return;
                
                switch (action.getCommand()) {
                    case Commands.METRONOME_STARTED:
                        SwingUtilities.invokeLater(() -> metronomeButton.setSelected(true));
                        break;
                    case Commands.METRONOME_STOPPED:
                        SwingUtilities.invokeLater(() -> metronomeButton.setSelected(false));
                        break;
                }
            }
        });
        
        return metronomeButton;
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
                        logger.warning("Error closing component: " + e.getMessage());
                    }
                }
            }
        }
    }
}
