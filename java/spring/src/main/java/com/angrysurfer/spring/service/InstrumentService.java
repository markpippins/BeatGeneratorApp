package com.angrysurfer.spring.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.engine.InstrumentEngine;
import com.angrysurfer.core.engine.MIDIEngine;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.spring.repo.Instruments;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class InstrumentService {

    static Logger logger = LoggerFactory.getLogger(InstrumentService.class.getCanonicalName());

    private Instruments instrumentRepo;

    private InstrumentEngine instrumentEngine;

    public InstrumentService(Instruments instruments) {
        this.instrumentRepo = instruments;
        this.instrumentEngine = new InstrumentEngine(instrumentRepo.findAll(), MIDIEngine.getMidiOutDevices());
    }

    public Instrument save(Instrument instrument) {
        logger.info("save");
        return instrumentRepo.save(instrument);
    }

    public List<Instrument> getAllInstruments() {
        logger.info("getAllInstruments");
        return instrumentEngine.getInstrumentList();
    }

    public List<Instrument> getInstrumentByChannel(int channel) {
        logger.info(String.format("getInstrumentByChannel(%s)", channel));
        return instrumentEngine.getInstrumentByChannel(channel);
    }

    public Instrument getInstrumentById(Long id) {
        logger.info(String.format("getInstrumentById(%s)", id));
        return instrumentEngine.getInstrumentById(id);
    }

    public List<String> getInstrumentNames() {
        logger.info("getInstrumentNames()");
        return instrumentEngine.getInstrumentNames();
    }

}
