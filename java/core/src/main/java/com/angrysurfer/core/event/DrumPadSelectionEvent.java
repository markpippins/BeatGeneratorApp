package com.angrysurfer.core.event;

/**
 * Event data for drum pad selection changes
 */
public record DrumPadSelectionEvent(int oldSelection, int newSelection) {
}