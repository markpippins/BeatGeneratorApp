package com.angrysurfer.core.event;

/**
 * Event class for step updates
 */
public record StepUpdateEvent(int oldStep, int newStep) {
}