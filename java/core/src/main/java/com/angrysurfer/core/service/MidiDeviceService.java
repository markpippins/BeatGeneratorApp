package com.angrysurfer.core.service;

import java.util.List;
import com.angrysurfer.core.proxy.ProxyInstrument;

public interface MidiDeviceService {
    List<ProxyInstrument> findAllInstruments();
}
