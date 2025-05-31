package com.angrysurfer.core.event;

/**
 * Event class for pattern switching notifications
 */
public record PatternSwitchEvent(Long previousPatternId, Long newPatternId) {

}