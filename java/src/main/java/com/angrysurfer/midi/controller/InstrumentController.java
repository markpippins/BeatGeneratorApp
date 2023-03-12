package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.LookupItem;
import com.angrysurfer.midi.model.MidiInstrument;
import com.angrysurfer.midi.service.BeatGeneratorService;
import com.angrysurfer.midi.service.MIDIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class InstrumentController {
    static Logger logger = LoggerFactory.getLogger(InstrumentController.class.getCanonicalName());
    private final BeatGeneratorService beatGeneratorService;
    private final MIDIService midiService;

    public InstrumentController(BeatGeneratorService service, MIDIService midiService) {
        this.beatGeneratorService = service;
        this.midiService = midiService;
    }

    // @GetMapping(path = "/instruments/info")
    // public @ResponseBody Map<String, MidiInstrument> getInstruments() {
    //     logger.info("/instruments/info");
    //     return beatGeneratorService.getInstrumentMap();
    // }

    @GetMapping(path = "/instrument/info")
    public @ResponseBody MidiInstrument getInstrument(int channel) {
        logger.info("/instrument/info");
        return beatGeneratorService.getInstrument(channel);
    }

    @GetMapping(path = MIDIService.INSTRUMENT_LIST)
    public @ResponseBody List<MidiInstrument> getInstrumentList() {
        return midiService.getInstrumentList();
    }

    @GetMapping(path = MIDIService.INSTRUMENT_NAMES)
    public @ResponseBody List<String> getInstrumentNames() {
        return midiService.getInstrumentNames();
    }

    @GetMapping(path = MIDIService.INSTRUMENT_LOOKUP)
    public @ResponseBody List<LookupItem> getInstrumentLookupItems() {
        return midiService.getInstrumentLookupItems();
    }
}

