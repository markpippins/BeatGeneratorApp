package com.angrysurfer.core.event;

import lombok.Getter;

/**
 * Updated StepUpdateEvent to include drum index
 */
public record DrumStepUpdateEvent(int drumIndex, int oldStep, int newStep) {

}