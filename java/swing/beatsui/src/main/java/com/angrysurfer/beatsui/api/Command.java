package com.angrysurfer.beatsui.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Command {

    private String command;
    private Object sender;
    private Object data;

    public Command(String command, Object sender, Object data) {
        this.command = command;
        this.sender = sender;
        this.data = data;
    }

    public Command() {
        //TODO Auto-generated constructor stub
    }
}
