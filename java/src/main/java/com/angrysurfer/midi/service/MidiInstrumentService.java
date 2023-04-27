package com.angrysurfer.midi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.midi.model.MidiInstrument;
import com.angrysurfer.midi.repo.MidiInstrumentRepo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class MidiInstrumentService {

    static Logger logger = LoggerFactory.getLogger(MidiInstrumentService.class.getCanonicalName());

    private MidiInstrumentRepo instrumentRepo;

    private List<MidiInstrument> instruments = new ArrayList<>();

    public MidiInstrumentService(MidiInstrumentRepo instrumentRepo) {
        this.instrumentRepo = instrumentRepo;
    }

    public List<MidiInstrument> getAllInstruments(boolean clearCache) {
        logger.info("getAllInstruments()");
        if (clearCache || instruments.isEmpty())
            setInstruments(instrumentRepo.findAll());

        // results.forEach(i -> i.setDevice(findMidiOutDevice(i.getDeviceName())));

        return instruments;
    }

    public List<MidiInstrument> getInstrumentByChannel(int channel, boolean clearCache) {
        logger.info(String.format("getInstrumentByChannel(%s)", channel));
        return getAllInstruments(clearCache).stream().filter(i -> i.getChannel() == channel)
                .collect(Collectors.toList());
    }

    public MidiInstrument getInstrumentById(Long id) {
        logger.info(String.format("getInstrumentById(%s)", id));
        return instrumentRepo.findById(id).orElseThrow();
    }

    public List<String> getInstrumentNames(boolean clearCache) {
        logger.info("getInstrumentNames()");
        return getAllInstruments(clearCache).stream().map(i -> i.getName()).toList();
    }
}
