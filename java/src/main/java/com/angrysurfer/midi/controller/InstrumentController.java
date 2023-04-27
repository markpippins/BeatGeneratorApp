package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.MidiInstrument;
import com.angrysurfer.midi.service.MIDIService;
import com.angrysurfer.midi.service.MidiInstrumentService;
import com.angrysurfer.midi.util.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class InstrumentController {
    static Logger logger = LoggerFactory.getLogger(InstrumentController.class.getCanonicalName());
    private final MIDIService midiService;
    private final MidiInstrumentService instrumentService;

    public InstrumentController(MIDIService midiService, MidiInstrumentService instrumentService) {
        this.midiService = midiService;
        this.instrumentService = instrumentService;
    }

    @GetMapping(path = Constants.INSTRUMENT_LIST)
    public @ResponseBody List<MidiInstrument> getInstrumentList() {
        return instrumentService.getAllInstruments(false);
    }

    @GetMapping(path = Constants.INSTRUMENT)
    public @ResponseBody MidiInstrument getInstrument(@RequestParam Long instrumentId) {
        return instrumentService.getInstrumentById(instrumentId);
    }

    @GetMapping(path = Constants.INSTRUMENT_NAMES)
    public @ResponseBody List<String> getInstrumentNames() {
        return instrumentService.getInstrumentNames(false);
    }
    
    @GetMapping(path = Constants.GET_INSTRUMENT_BY_CHANNEL)
    public @ResponseBody List<MidiInstrument> getInstrumentsByChannel(int channel) {
        // logger.info("/instrument/info");
        return instrumentService.getInstrumentByChannel(channel, false);
    }

    // @GetMapping(path = Constants.INSTRUMENT_LOOKUP)
    // public @ResponseBody List<LookupItem> getInstrumentLookupItems() {
    //     return midiService.getInstrumentLookupItems();
    // }
}

