package com.angrysurfer.sequencer.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.sequencer.model.midi.MidiInstrument;
import com.angrysurfer.sequencer.repo.MidiInstrumentRepo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class InstrumentService {

    static Logger logger = LoggerFactory.getLogger(InstrumentService.class.getCanonicalName());

    private MidiInstrumentRepo instrumentRepo;

    public InstrumentService(MidiInstrumentRepo instrumentRepo) {
        this.instrumentRepo = instrumentRepo;
    }

    public List<MidiInstrument> getAllInstruments() {
        return instrumentRepo.findAll().stream().filter(i -> i.getAvailable()).collect(Collectors.toList());
    }

    public List<MidiInstrument> getInstrumentByChannel(int channel) {
        logger.info(String.format("getInstrumentByChannel(%s)", channel));
        return getAllInstruments().stream().filter(i -> i.getChannel() == channel)
                .collect(Collectors.toList());
    }

    public MidiInstrument getInstrumentById(Long id) {
        logger.info(String.format("getInstrumentById(%s)", id));
        return instrumentRepo.findById(id).orElseThrow();
    }

    public List<String> getInstrumentNames() {
        logger.info("getInstrumentNames()");
        return getAllInstruments().stream().filter(i -> i.getAvailable()).map(i -> i.getName()).toList();
    }
}
