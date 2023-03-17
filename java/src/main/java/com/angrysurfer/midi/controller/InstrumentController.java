package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.LookupItem;
import com.angrysurfer.midi.model.MidiInstrument;
import com.angrysurfer.midi.service.MIDIService;
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

    public InstrumentController(MIDIService midiService) {
        this.midiService = midiService;
    }

    @GetMapping(path = MIDIService.INSTRUMENT_LIST)
    public @ResponseBody List<MidiInstrument> getInstrumentList() {
        return midiService.getInstrumentList();
    }

    @GetMapping(path = MIDIService.INSTRUMENT)
    public @ResponseBody MidiInstrument getInstrument(@RequestParam Long instrumentId) {
        return midiService.getInstrumentById(instrumentId);
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

