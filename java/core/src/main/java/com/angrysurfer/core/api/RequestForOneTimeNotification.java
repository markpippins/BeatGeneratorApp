package com.angrysurfer.core.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestForOneTimeNotification {
    private final String eventToWaitFor;
    private final Runnable callback;

    public RequestForOneTimeNotification(String eventToWaitFor, Runnable callback) {
        this.eventToWaitFor = eventToWaitFor;
        this.callback = callback;
    }

    public String getEventToWaitFor() {
        return eventToWaitFor;
    }

    public void execute() {
        if (callback != null) {
            callback.run();
        }
    }
}