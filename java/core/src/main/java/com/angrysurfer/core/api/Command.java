package com.angrysurfer.core.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Command {
    private String command;
    private Object sender;
    private Object data;

    Command(String command, Object sender, Object data) {
        this.command = command;
        this.sender = sender;
        this.data = data;
    }
}
