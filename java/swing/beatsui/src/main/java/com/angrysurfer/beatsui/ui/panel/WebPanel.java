package com.angrysurfer.beatsui.ui.panel;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;

public class WebPanel extends JPanel {
    private final JEditorPane webView;

    public WebPanel() {
        setLayout(new BorderLayout());
        webView = new JEditorPane();
        webView.setEditable(false);
        
        try {
            webView.setPage("https://www.google.com");
        } catch (IOException e) {
            webView.setContentType("text/html");
            webView.setText("<html>Could not load https://www.google.com</html>");
        }

        webView.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    webView.setPage(e.getURL());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        add(new JScrollPane(webView), BorderLayout.CENTER);
    }
}
