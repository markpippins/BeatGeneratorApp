package com.angrysurfer.core.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sound.midi.MidiDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.midi.Instrument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstrumentEngine {

    static Logger logger = LoggerFactory.getLogger(InstrumentEngine.class.getCanonicalName());

    private boolean refresh = true;

    private List<Instrument> instrumentList;

    private List<MidiDevice> midiDevices;

    private List<String> devices;

    public InstrumentEngine(List<Instrument> instruments, List<MidiDevice> midiDevices) {
        this.instrumentList = new ArrayList<>(instruments);
        this.midiDevices = new ArrayList<>(midiDevices);
    }

    public List<Instrument> getInstrumentByChannel(int channel) {
        logger.info(String.format("getInstrumentByChannel(%s)", channel));
        return instrumentList.stream()
                .filter(i -> i.receivesOn(channel) && devices.contains(i.getDeviceName()))
                .collect(Collectors.toList());
    }

    public Instrument getInstrumentById(Long id) {
        logger.info(String.format("getInstrumentById(%s)", id));
        return instrumentList.stream().filter(i -> i.getId() == id).findFirst().orElseThrow();
    }

    public List<String> getInstrumentNames() {
        logger.info("getInstrumentNames()");
        return instrumentList.stream().map(i -> i.getName()).toList();
    }

}
