package com.angrysurfer.core.engine;

import java.util.List;
import java.util.stream.Collectors;

import javax.sound.midi.MidiDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.util.db.FindAll;

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

    private FindAll<Instrument> instrumentsFindAll;

    public InstrumentEngine(FindAll<Instrument> instrumentsFindAll) {
        this.instrumentList = instrumentsFindAll.findAll();
        this.instrumentsFindAll = instrumentsFindAll;
    }

    public void refreshInstruments() {
        this.instrumentList = instrumentsFindAll.findAll();
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

    public Instrument findByName(String name) {
        logger.info(String.format("getInstrumentById(%s)", name));
        return instrumentList.stream().filter(i -> i.getName() == name).findFirst().orElseThrow();
    }

}
