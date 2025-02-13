package com.angrysurfer.beatsui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beatsui.panel.LaunchPanel;
import com.angrysurfer.beatsui.panel.OptionsPanel;
import com.angrysurfer.beatsui.panel.PlayerPanel;
import com.angrysurfer.beatsui.panel.SystemsPanel;
import com.angrysurfer.beatsui.panel.X0XPanel;
import com.angrysurfer.beatsui.widget.StatusBar;

public class Frame extends JFrame {

    private StatusBar statusBar = new StatusBar();

    public Frame() {
        super("Beats");
        setupFrame();
        setupMainContent();
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

    private void setupMainContent() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.setBorder(new EmptyBorder(2, 5, 2, 5));

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Players", new PlayerPanel(statusBar));
        tabbedPane.addTab("Launch", new LaunchPanel());
        tabbedPane.addTab("X0X", new X0XPanel());

        tabbedPane.addTab("Params", new JPanel());
        tabbedPane.addTab("Controls", new JPanel());

        tabbedPane.addTab("Instruments", new OptionsPanel());
        tabbedPane.addTab("System", new SystemsPanel(statusBar));

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    public <T> Dialog<T> createDialog(T data, JPanel content) {
        return new Dialog<>(this, data, content);
    }

}
