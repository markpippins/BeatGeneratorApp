package com.angrysurfer.sequencer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import com.angrysurfer.sequencer.model.midi.Instrument;
import com.angrysurfer.sequencer.service.MIDIService;
import com.angrysurfer.sequencer.service.InstrumentService;
import com.angrysurfer.sequencer.util.Constants;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class InstrumentController {
    static Logger logger = LoggerFactory.getLogger(InstrumentController.class.getCanonicalName());
    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping(path = Constants.INSTRUMENT_LIST)
    public @ResponseBody List<Instrument> getInstrumentList() {
        return instrumentService.getAllInstruments();
    }

    @GetMapping(path = Constants.INSTRUMENT)
    public @ResponseBody Instrument getInstrument(@RequestParam Long instrumentId) {
        return instrumentService.getInstrumentById(instrumentId);
    }

    @GetMapping(path = Constants.INSTRUMENT_NAMES)
    public @ResponseBody List<String> getInstrumentNames() {
        return instrumentService.getInstrumentNames();
    }

    @GetMapping(path = Constants.GET_INSTRUMENT_BY_CHANNEL)
    public @ResponseBody List<Instrument> getInstrumentsByChannel(String deviceName, int channel) {
        // logger.info("/instrument/info");
        return instrumentService.getInstrumentByChannel(deviceName, channel);
    }

    // @GetMapping(path = Constants.INSTRUMENT_LOOKUP)
    // public @ResponseBody List<LookupItem> getInstrumentLookupItems() {
    // return midiService.getInstrumentLookupItems();
    // }
}
