package com.angrysurfer.sequencer.service;

import java.util.ArrayList;
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

    private Instruments instrumentRepo;

    private boolean refresh = true;

    private List<Instrument> instrumentCache;

    private List<String> devices;

    public InstrumentService(Instruments instruments) {
        this.instrumentRepo = instruments;
        this.instrumentCache = new ArrayList<>();
        this.devices = new ArrayList<>();
        MIDIService.getMidiOutDevices().forEach(device -> devices.add(device.getDeviceInfo().getName()));
    }

    public List<Instrument> getAllInstruments() {
        logger.info("getAllInstruments");

        if (refresh == true) {
            this.instrumentCache.clear();
            this.instrumentCache = instrumentRepo.findAll().stream()
                    .filter(i -> i.getAvailable())
                    .collect(Collectors.toList());
            refresh = false;
        }

        return instrumentCache;
    }

    public List<Instrument> getInstrumentByChannel(int channel) {
        logger.info(String.format("getInstrumentByChannel(%s)", channel));

        return getAllInstruments().stream()
                .filter(i -> i.receivesOn(channel) && devices.contains(i.getDeviceName()))
                .collect(Collectors.toList());
    }

    public Instrument getInstrumentById(Long id) {
        logger.info(String.format("getInstrumentById(%s)", id));
        return instrumentCache.stream().filter(i -> i.getId() == id).findFirst().orElseThrow();
    }

    public List<String> getInstrumentNames() {
        logger.info("getInstrumentNames()");
        return getAllInstruments().stream().map(i -> i.getName()).toList();
    }

    public Instrument save(Instrument instrument) {
        return instrumentRepo.save(instrument);
    }
}
