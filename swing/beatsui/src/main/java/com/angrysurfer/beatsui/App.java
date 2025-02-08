package com.angrysurfer.beatsui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Hello world!
 *
 */
public class App {
    private AppFrame frame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                // UIManager.setLookAndFeel(
                // UIManager.getSystemLookAndFeelClassName());
                UIManager.setLookAndFeel(
                UIManager.getCrossPlatformLookAndFeelClassName());
                // UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
                // UIManager.setLookAndFeel("com.sun.java.swing.plaf.metal.MetalLookAndFeel");
                // UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                // UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
                // UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                
            } catch (Exception e) {
                e.printStackTrace();
            }

            App app = new App();
            app.frame = new AppFrame();
            app.frame.setLocationRelativeTo(null); // Center on screen
            app.frame.setVisible(true);
        });
    }
}
