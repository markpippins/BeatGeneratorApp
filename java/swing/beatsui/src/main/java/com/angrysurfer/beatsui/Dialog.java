package com.angrysurfer.beatsui;

import javax.swing.JDialog;

public class Dialog extends JDialog {

    public Dialog() {
        super();
        setup();
    }

    private void setup() {
        setTitle("Dialog");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setModal(true);
    }

    public void showDialog() {
        setVisible(true);
    }

    public void hideDialog() {
        setVisible(false);
    }

    public static void main(String[] args) {
        Dialog dialog = new Dialog();
        dialog.showDialog();
    }

}
