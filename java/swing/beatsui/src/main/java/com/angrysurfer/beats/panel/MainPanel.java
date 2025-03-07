package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.StatusBar;
import com.angrysurfer.beats.visualization.handler.SortingVisualizerPanel;

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

        tabbedPane.addTab("X0X", new X0XPanel());
        tabbedPane.addTab("Players", new SessionPanel(statusBar));
        tabbedPane.addTab("Instruments", new InstrumentsPanel());
        tabbedPane.addTab("Launch", new LaunchPanel());
        tabbedPane.addTab("Controls", new ControlsPanel());
        tabbedPane.addTab("System", new SystemsPanel(statusBar));
        tabbedPane.addTab("Step Sequencer", new TransportPanel());

        tabbedPane.addTab("Sorting", new SortingVisualizerPanel());
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
