package com.angrysurfer.sequencer.controller;

import java.util.List;
import java.util.Objects;

import javax.sound.midi.MidiUnavailableException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.angrysurfer.sequencer.model.midi.Instrument;
import com.angrysurfer.sequencer.model.midi.Device;
import com.angrysurfer.sequencer.service.InstrumentService;
import com.angrysurfer.sequencer.service.MIDIService;
import com.angrysurfer.sequencer.util.Constants;

@CrossOrigin("*")
@RequestMapping(path = "/api")
@Controller
@RestController
public class MidiController {

    InstrumentService instrumentService;

    MIDIService midiService;

    public MidiController(InstrumentService instrumentService, MIDIService midiService) {
        this.instrumentService = instrumentService;
        this.midiService = midiService;
    }

    @GetMapping(path = Constants.DEVICES_INFO)
    public @ResponseBody List<Device> getDeviceInfo() {
        return MIDIService.getMidiDeviceInfos();
    }

    @GetMapping(path = Constants.DEVICE_NAMES)
    public @ResponseBody List<String> getDeviceNames() {
        return MIDIService.getMidiOutDevices().stream().map(d -> d.getDeviceInfo().getName()).toList();
    }

    @PostMapping(path = Constants.SERVICE_RESET)
    public void reset() {
        MIDIService.reset();
    }

    @PostMapping(path = Constants.SERVICE_SELECT)
    public @ResponseBody boolean select(String name) {
        try {
            return MIDIService.select(name);
        } catch (MidiUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    @GetMapping(Constants.SEND_MESSAGE)
    public void sendMessage(@RequestParam int instrumentId, @RequestParam int channel, @RequestParam int messageType,
            @RequestParam int data1,
            @RequestParam int data2) {

        Instrument instrument = instrumentService.getInstrumentById((long) instrumentId);
        if (Objects.nonNull(instrument)) {
            instrument.setDevice(MIDIService.getMidiDevice(instrument.getDeviceName()));
            if (Objects.nonNull(instrument.getDevice())) {
                // instrument.setChannel(channel);
                midiService.sendMessage(instrument, messageType, data1, data2);
            }
        }

    }
}
