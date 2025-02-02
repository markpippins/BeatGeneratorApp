package com.angrysurfer.sequencer.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.sequencer.model.midi.Instrument;
import com.angrysurfer.sequencer.repo.Instruments;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class InstrumentService {

    static Logger logger = LoggerFactory.getLogger(InstrumentService.class.getCanonicalName());

    private Instruments instruments;

    public InstrumentService(Instruments instruments) {
        this.instruments = instruments;
    }

    public List<Instrument> getAllInstruments() {
        List<Instrument> result = instruments.findAll().stream().filter(i -> i.getAvailable())
                .collect(Collectors.toList());
        return result;
    }

    public List<Instrument> getInstrumentByChannel(String deviceName, int channel) {
        logger.info(String.format("getInstrumentByChannel(%s)", channel));
        List<Instrument> result = getAllInstruments().stream()
                .filter(i -> i.getChannels().length == 1 && i.getChannels()[0] == channel)
                .collect(Collectors.toList());
        return result;
    }

    public Instrument getInstrumentById(Long id) {
        logger.info(String.format("getInstrumentById(%s)", id));
        return instruments.findById(id).orElseThrow();
    }

    public List<String> getInstrumentNames() {
        logger.info("getInstrumentNames()");
        return getAllInstruments().stream().filter(i -> i.getAvailable()).map(i -> i.getName()).toList();
    }
}
