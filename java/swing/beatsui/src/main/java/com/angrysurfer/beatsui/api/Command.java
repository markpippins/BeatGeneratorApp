package com.angrysurfer.beatsui.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Command {

    private String command;
    private Object sender;
    private Object data;
}
