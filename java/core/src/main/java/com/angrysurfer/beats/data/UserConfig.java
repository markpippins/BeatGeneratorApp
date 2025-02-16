package com.angrysurfer.beats.data;

import java.util.List;
import java.util.Set;

import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserConfig {
    private List<ProxyInstrument> instruments;
    private List<ProxyStrike> players;
    private Set<InstrumentConfig> configs;

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
