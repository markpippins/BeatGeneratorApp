package com.angrysurfer.spring.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.redis.InstrumentHelper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.InstrumentManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class InstrumentService {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentService.class);

    private final InstrumentHelper instrumentHelper;
    public InstrumentService(RedisService redisService) {
        this.instrumentHelper = redisService.getInstrumentHelper();
    }

    public List<InstrumentWrapper> getAllInstruments() {
        return InstrumentManager.getInstance().getCachedInstruments();
    }

    public InstrumentWrapper getInstrumentById(Long id) {
        return InstrumentManager.getInstance().getInstrumentById(id);
    }

    public List<String> getInstrumentNames() {
        return InstrumentManager.getInstance().getInstrumentNames();
    }

    public List<InstrumentWrapper> getInstrumentByChannel(int channel) {
        return InstrumentManager.getInstance().getInstrumentByChannel(channel);
    }

    public void saveInstrument(InstrumentWrapper instrument) {
        instrumentHelper.saveInstrument(instrument);
        InstrumentManager.getInstance().setNeedsRefresh(true);
    }

    public List<InstrumentWrapper> getInstrumentList() {
        logger.debug("Getting instrument list");
        return InstrumentManager.getInstance().getCachedInstruments();
    }

    public InstrumentWrapper findByName(String name) {
        logger.debug("Finding instrument by name: {}", name);
        return InstrumentManager.getInstance().findByName(name);
    }
}
