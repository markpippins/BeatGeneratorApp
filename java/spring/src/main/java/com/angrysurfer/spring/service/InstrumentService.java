package com.angrysurfer.spring.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.redis.RedisInstrumentHelper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.InstrumentManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class InstrumentService {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentService.class);

    private final RedisInstrumentHelper instrumentHelper;
    private final InstrumentManager instrumentEngine;

    public InstrumentService(RedisService redisService) {
        this.instrumentHelper = redisService.getInstrumentHelper();
        this.instrumentEngine = new InstrumentManager(); // SessionM anager.getInstance().getInstrumentEngine();
    }

    public List<Instrument> getAllInstruments() {
        return instrumentEngine.getCachedInstruments();
    }

    public Instrument getInstrumentById(Long id) {
        return instrumentEngine.getInstrumentById(id);
    }

    public List<String> getInstrumentNames() {
        return instrumentEngine.getInstrumentNames();
    }

    public List<Instrument> getInstrumentByChannel(int channel) {
        return instrumentEngine.getInstrumentByChannel(channel);
    }

    public void saveInstrument(Instrument instrument) {
        instrumentHelper.saveInstrument(instrument);
        instrumentEngine.setNeedsRefresh(true);
    }

    public List<Instrument> getInstrumentList() {
        logger.debug("Getting instrument list");
        return instrumentEngine.getCachedInstruments();
    }

    public Instrument findByName(String name) {
        logger.debug("Finding instrument by name: {}", name);
        return instrumentEngine.findByName(name);
    }
}
