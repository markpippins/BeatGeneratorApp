package com.angrysurfer.core.engine;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sound.midi.MidiDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.IInstrument;
import com.angrysurfer.core.api.db.FindAll;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstrumentEngine {

    static Logger logger = LoggerFactory.getLogger(InstrumentEngine.class.getCanonicalName());

    private boolean refresh = true;

    private List<IInstrument> instrumentList;

    private List<MidiDevice> midiDevices;

    private List<String> devices;

    private FindAll<IInstrument> instrumentsFindAll;

    public InstrumentEngine(FindAll<IInstrument> instrumentsFindAll) {
        this.instrumentList = instrumentsFindAll.findAll();
        this.instrumentsFindAll = instrumentsFindAll;
    }

    public void refreshInstruments() {
        this.instrumentList = instrumentsFindAll.findAll();
        refresh = false;
    }

    public List<IInstrument> getInstrumentByChannel(int channel) {
        logger.info(String.format("getInstrumentByChannel(%s)", channel));
        if (refresh)
            refreshInstruments();
        return instrumentList.stream()
                .filter(i -> i.receivesOn(channel) && devices.contains(i.getDeviceName()))
                .collect(Collectors.toList());
    }

    public IInstrument getInstrumentById(Long id) {
        logger.info(String.format("getInstrumentById(%s)", id));
        if (refresh)
            refreshInstruments();
        return instrumentList.stream().filter(i -> i.getId() == id).findFirst().orElseThrow();
    }

    public List<String> getInstrumentNames() {
        logger.info("getInstrumentNames()");
        if (refresh)
            refreshInstruments();
        return instrumentList.stream().map(i -> i.getName()).toList();
    }

    public IInstrument findByName(String name) {
        logger.info(String.format("findByName(%s)", name));
        IInstrument result = null;

        if (refresh)
            refreshInstruments();

        if (getInstrumentNames().contains(name)) {
            Optional<IInstrument> opt = instrumentList.stream()
                    .filter(i -> i.getName().toLowerCase().equals(name.toLowerCase())).findFirst();
            if (opt.isPresent())
                result = opt.get();
        }

        return result;
    }

}
