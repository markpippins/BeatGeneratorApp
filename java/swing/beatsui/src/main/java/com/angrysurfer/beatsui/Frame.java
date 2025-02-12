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
import com.angrysurfer.beatsui.panel.X0XPanel;
import com.angrysurfer.beatsui.widget.StatusBar;

public class Frame extends JFrame {

    private StatusBar statusBar;

    public Frame() {
        super("Beats");
        setupFrame();
        setJMenuBar(new MenuBar(this));
        add(new ToolBar(), BorderLayout.NORTH);
        setUpStatusBar();
        setupMainContent();
    }

    private void setUpStatusBar() {
        this.statusBar = new StatusBar();
        add(statusBar, BorderLayout.SOUTH);

    }

    private void setupFrame() {
        setPreferredSize(new Dimension(1200, 800));
        setMinimumSize(new Dimension(1200, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
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

        tabbedPane.addTab("Options", new OptionsPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

}
