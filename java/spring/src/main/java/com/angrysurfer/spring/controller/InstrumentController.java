package com.angrysurfer.spring.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.angrysurfer.core.api.IInstrument;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.spring.service.InstrumentService;

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
    public @ResponseBody List<IInstrument> getInstrumentList() {
        logger.info("GET " + Constants.INSTRUMENT_LIST);
        List<IInstrument> instruments = instrumentService.getAllInstruments();
        return instruments;
    }

    @GetMapping(path = Constants.INSTRUMENT)
    public @ResponseBody IInstrument getInstrument(@RequestParam Long instrumentId) {
        logger.info("GET " + Constants.INSTRUMENT + " - instrumentId: {}", instrumentId);
        return instrumentService.getInstrumentById(instrumentId);
    }

    @GetMapping(path = Constants.INSTRUMENT_NAMES)
    public @ResponseBody List<String> getInstrumentNames() {
        logger.info("GET " + Constants.INSTRUMENT_NAMES);
        return instrumentService.getInstrumentNames();
    }

    @GetMapping(path = Constants.GET_INSTRUMENT_BY_CHANNEL)
    public @ResponseBody List<IInstrument> getInstrumentsByChannel(String deviceName, int channel) {
        logger.info("GET " + Constants.GET_INSTRUMENT_BY_CHANNEL + " - deviceName: {}, channel: {}", deviceName, channel);
        return instrumentService.getInstrumentByChannel(channel);
    }

    // @GetMapping(path = Constants.INSTRUMENT_LOOKUP)
    // public @ResponseBody List<LookupItem> getInstrumentLookupItems() {
    // return midiService.getInstrumentLookupItems();
    // }
}
