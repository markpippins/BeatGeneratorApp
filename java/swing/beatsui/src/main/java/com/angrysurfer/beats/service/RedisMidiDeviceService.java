package com.angrysurfer.beats.service;

import java.util.List;
import com.angrysurfer.beats.App;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.service.MidiDeviceService;

public class RedisMidiDeviceService implements MidiDeviceService {
    @Override
    public List<ProxyInstrument> findAllInstruments() {
        return App.getRedisService().findAllInstruments();
    }
}
