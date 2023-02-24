package com.angrysurfer.midi.swing;

import javax.swing.*;
import java.awt.*;

public class Console extends JFrame {

    public Console() {

        setAlwaysOnTop(true);
        setVisible(true);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setupUI();
        setMinimumSize(new Dimension(800, 800));
        setContentPane(getContentPane());
    }

    public static void main(String[] args) {
        new Console();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        add(createTopPanel(), BorderLayout.NORTH);
        add(createRightPanel(), BorderLayout.SOUTH);
        add(new DrumPadPanel(), BorderLayout.WEST);
        add(createLeftPanel(), BorderLayout.CENTER);
        add(new SliderPanel(), BorderLayout.EAST);
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel();
        bottom.add(new JLabel("bottom"));
        return bottom;
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel();
        right.add(new JLabel("right"));
        return right;
    }

    private JPanel createLeftPanel() {
        JPanel left = new JPanel();

        return left;
    }

    private JPanel createTopPanel() {
        JPanel top = new JPanel();
        top.add(new JLabel("top"));
        return top;
    }
}

//        HttpRequest request = null;
//        try {
//            request = HttpRequest.newBuilder()
//                    .uri(new URI("https://postman-echo.com/get"))
//                    .headers("key1", "value1", "key2", "value2")
//                    .GET()
//                    .build();
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
//
//        try {
//            HttpRequest request2 = HttpRequest.newBuilder()
//                    .uri(new URI("https://postman-echo.com/get"))
//                    .header("key1", "value1")
//                    .header("key2", "value2")
//                    .GET()
//                    .build();
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
//        HttpRequest.newBuilder(request, (name, value) -> !name.equalsIgnoreCase("Foo-Bar"));
