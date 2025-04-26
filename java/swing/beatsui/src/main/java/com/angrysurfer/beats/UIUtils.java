package com.angrysurfer.beats;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DoubleDial;

public class UIUtils {

    // Greys & Dark Blues
    public static Color charcoalGray = new Color(40, 40, 40); // Deep console casing
    public static Color slateGray = new Color(70, 80, 90); // Cool metallic panel
    public static Color deepNavy = new Color(20, 50, 90); // Darker Neve-style blue

    public static Color mutedOlive = new Color(85, 110, 60); // Vintage military-style green
    public static Color fadedLime = new Color(140, 160, 80); // Aged LED green

    // Yellows & Oranges
    public static Color dustyAmber = new Color(200, 140, 60); // Classic VU meter glow
    public static Color warmMustard = new Color(180, 140, 50); // Retro knob indicator
    public static Color deepOrange = new Color(190, 90, 40); // Vintage warning light

    // Accents
    public static Color agedOffWhite = new Color(225, 215, 190); // Worn plastic knobs
    public static Color deepTeal = new Color(30, 80, 90); // Tascam-inspired accent

    public static Color darkGray = new Color(50, 50, 50); // Deep charcoal (console casing)
    public static Color warmGray = new Color(120, 120, 120); // Aged metal panel
    public static Color mutedRed = new Color(180, 60, 60); // Classic button color
    public static Color fadedOrange = new Color(210, 120, 50); // Vintage indicator light
    public static Color coolBlue = new Color(50, 130, 200); // Neve-style trim
    public static Color warmOffWhite = new Color(230, 220, 200);// Aged plastic knobs

    public static Color getDialColor(String name) {

        switch (name) {
            case "swing":
                return charcoalGray;
            case "velocity":
                return slateGray;
            case "probability":
            case "reverb":
                return deepNavy;
            case "random":
                return mutedOlive;
            case "pan":
            case "sparse":
                return fadedLime;
            case "chorus":
                return dustyAmber;
            case "gate":
            case "decay":
                return warmMustard;
            case "nudge":
                return deepOrange;
            case "tilt":
            case "delay":
                return mutedRed;
            case "tune":
            case "drive":
                return fadedOrange;
            case "bright":
            case "tone":
                return darkGray;
            default:
                return coolBlue; // Default to white if not found
        }
    }

    public static Color getButtonColor() {
        return new Color(150, 150, 150); // Aged metal panel
    }

    public static Color getBackgroundColor() {
        return new Color(40, 40, 40); // Deep console casing
    }

    public static Color getTextColor() {
        return new Color(255, 255, 255); // Bright white text for contrast
    }

    public static Color getAccentColor() {
        return new Color(30, 80, 90); // Tascam-inspired accent color
    }

    public static Color[] getColors() {
        return new Color[] { coolBlue, darkGray, warmGray, charcoalGray, slateGray, deepNavy, dustyAmber, warmMustard,
                deepOrange, mutedRed, fadedOrange, mutedOlive, fadedLime };
    }

    public static Color[] getAccentColors() {
        return new Color[] { agedOffWhite, deepTeal, warmOffWhite };
    }

    /**
     * Safely adds a component to a container with error handling
     * @param container The container to add the component to
     * @param component The component to add
     * @param constraints Layout constraints (if applicable)
     * @return true if addition was successful, false otherwise
     */
    public static boolean addSafely(Container container, Component component, Object constraints) {
        if (component == null) {
            Logger.getLogger(UIUtils.class.getName()).warning(
                "Attempted to add null component to " + 
                (container != null ? container.getClass().getSimpleName() : "null container"));
            return false;
        }
        
        if (container == null) {
            Logger.getLogger(UIUtils.class.getName()).warning(
                "Attempted to add component to null container");
            return false;
        }
        
        try {
            container.add(component, constraints);
            return true;
        } catch (Exception e) {
            Logger.getLogger(UIUtils.class.getName()).warning(
                "Error adding component: " + e.getMessage());
            e.printStackTrace(); // More detailed stack trace for debugging
            return false;
        }
    }

    /**
     * Overloaded version without constraints for simpler layouts
     */
    public static boolean addSafely(Container container, Component component) {
        return addSafely(container, component, null);
    }
}
