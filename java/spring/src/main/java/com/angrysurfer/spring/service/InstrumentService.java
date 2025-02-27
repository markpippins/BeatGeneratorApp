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
    private final InstrumentManager instrumentManager;

    public InstrumentService(RedisService redisService) {
        this.instrumentHelper = redisService.getInstrumentHelper();
        this.instrumentManager = InstrumentManager.getInstance(instrumentHelper);
    }

    public List<Instrument> getAllInstruments() {
        return instrumentManager.getCachedInstruments();
    }

    public Instrument getInstrumentById(Long id) {
        return instrumentManager.getInstrumentById(id);
    }

    public List<String> getInstrumentNames() {
        return instrumentManager.getInstrumentNames();
    }

    public List<Instrument> getInstrumentByChannel(int channel) {
        return instrumentManager.getInstrumentByChannel(channel);
    }

    public void saveInstrument(Instrument instrument) {
        instrumentHelper.saveInstrument(instrument);
        instrumentManager.setNeedsRefresh(true);
    }

    public List<Instrument> getInstrumentList() {
        logger.debug("Getting instrument list");
        return instrumentManager.getCachedInstruments();
    }

    public Instrument findByName(String name) {
        logger.debug("Finding instrument by name: {}", name);
        return instrumentManager.findByName(name);
    }
}
