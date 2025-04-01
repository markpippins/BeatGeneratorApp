package com.angrysurfer.core.api;

/**
 * Immutable record for bundling status bar updates.
 * Any field can be null to indicate no change is needed for that field.
 */
public record StatusUpdate(String site, String status, String message) {
    /**
     * Constructor for updating site and status only
     */
    public StatusUpdate(String site, String status) {
        this(site, status, null);
    }
    
    /**
     * Constructor for updating just the status
     */
    public StatusUpdate(String status) {
        this(null, status, null);
    }
}