package com.angrysurfer.beatsui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beatsui.widgets.LaunchPanel;
import com.angrysurfer.beatsui.widgets.OptionsPanel;
import com.angrysurfer.beatsui.widgets.PlayerPanel;
import com.angrysurfer.beatsui.widgets.X0XPanel;

public class AppFrame extends JFrame {

    private JLabel statusBar;

    public AppFrame() {
        super("Beats");
        setupFrame();
        setupMenuBar();
        setupToolBar();
        setupMainContent();
        setupStatusBar();
    }

    private void setupToolBar() {
        add(new AppToolBar(), BorderLayout.NORTH);
    }

    private void setupFrame() {
        setMinimumSize(new Dimension(1200, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(new JMenuItem("New"));
        fileMenu.add(new JMenuItem("Open"));
        fileMenu.add(new JMenuItem("Save"));
        fileMenu.add(new JMenuItem("Save As..."));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem("Exit"));

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(new JMenuItem("Undo"));
        editMenu.add(new JMenuItem("Redo"));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem("Cut"));
        editMenu.add(new JMenuItem("Copy"));
        editMenu.add(new JMenuItem("Paste"));

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.add(new JMenuItem("About"));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void setupMainContent() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.setBorder(new EmptyBorder(2, 2, 2,2));

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Players", new PlayerPanel());
        tabbedPane.addTab("Launch", new LaunchPanel());
        tabbedPane.addTab("X0X", new X0XPanel());

        tabbedPane.addTab("Params", new JPanel());
        tabbedPane.addTab("Controls", new JPanel());
        tabbedPane.addTab("Options", new OptionsPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void setupStatusBar() {
        statusBar = new JLabel(" Ready");
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        add(statusBar, BorderLayout.SOUTH);
    }

    public void setStatus(String message) {
        statusBar.setText(" " + message);
    }
}
