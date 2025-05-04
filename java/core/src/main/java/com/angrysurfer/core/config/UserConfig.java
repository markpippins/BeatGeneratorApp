package com.angrysurfer.core.config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Strike;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserConfig {
    private List<InstrumentWrapper> instruments = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private int configVersion = 2;
    private Date lastUpdated;
    private String name;
}


