package com.angrysurfer.beats.service;

import java.util.List;

import com.angrysurfer.core.data.RedisService;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.service.MidiDeviceService;

public class RedisMidiDeviceService implements MidiDeviceService {
    @Override
    public List<ProxyInstrument> findAllInstruments() {
        return RedisService.getInstance().findAllInstruments();
    }
}
