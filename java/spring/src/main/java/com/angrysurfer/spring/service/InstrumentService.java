package com.angrysurfer.spring.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.engine.InstrumentEngine;
import com.angrysurfer.core.engine.MIDIEngine;
import com.angrysurfer.core.model.midi.Instrument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class InstrumentService {

    static Logger logger = LoggerFactory.getLogger(InstrumentService.class.getCanonicalName());

    private DBService dbUtils;

    private InstrumentEngine instrumentEngine;

    public InstrumentService(DBService dbUtils) {
        this.dbUtils = dbUtils;
        this.instrumentEngine = new InstrumentEngine(dbUtils.getInstrumentFindAll());
    }

    public Instrument save(Instrument instrument) {
        logger.info("save");
        return dbUtils.getInstrumentSaver().save(instrument);
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

    public Instrument findByName(String instrumentName) {
        return instrumentEngine.findByName(instrumentName);
    }

}
