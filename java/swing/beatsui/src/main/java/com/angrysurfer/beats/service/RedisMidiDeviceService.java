package com.angrysurfer.beats.service;

import java.util.List;

import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.service.MidiDeviceManager;
import com.angrysurfer.core.service.RedisService;

public class RedisMidiDeviceService implements MidiDeviceManager {
    @Override
    public List<ProxyInstrument> findAllInstruments() {
        return RedisService.getInstance().findAllInstruments();
    }
}
