package com.angrysurfer.core.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.model.midi.Instrument;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserConfig {
    private List<Instrument> instruments = new ArrayList<>();
    private List<Strike> players = new ArrayList<>();
    private Set<InstrumentConfig> configs = new HashSet<>();

    @Data
    public static class InstrumentConfig {
        private String port;
        private String device;
        private boolean available;
        private int channels;
        private int low;
        private int high;
    }
}
