package com.angrysurfer.core.service;

import java.util.List;
import com.angrysurfer.core.proxy.ProxyInstrument;

public interface MidiDeviceManager {
    List<ProxyInstrument> findAllInstruments();
}
