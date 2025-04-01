package com.angrysurfer.spring.controller;

import java.util.List;
import java.util.Objects;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.spring.service.InstrumentService;

@CrossOrigin("*")
@RequestMapping(path = "/api")
@Controller
@RestController
public class MidiController {

    private static final Logger logger = LoggerFactory.getLogger(MidiController.class);

    InstrumentService instrumentService;

    public MidiController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping(path = Constants.DEVICES_INFO)
    public @ResponseBody List<MidiDevice.Info> getDeviceInfo() {
        logger.info("GET " + Constants.DEVICES_INFO);
        return DeviceManager.getMidiDeviceInfos();
    }

    @GetMapping(path = Constants.DEVICE_NAMES)
    public @ResponseBody List<String> getDeviceNames() {
        logger.info("GET " + Constants.DEVICE_NAMES);
        return DeviceManager.getMidiOutDevices().stream().map(d -> d.getDeviceInfo().getName()).toList();
    }

    @PostMapping(path = Constants.SERVICE_RESET)
    public void reset() {
        logger.info("POST " + Constants.SERVICE_RESET);
        DeviceManager.reset();
    }

    @PostMapping(path = Constants.SERVICE_SELECT)
    public @ResponseBody boolean select(String name) {
        logger.info("POST " + Constants.SERVICE_SELECT + " - name: {}", name);
        try {
            return DeviceManager.select(name);
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
        logger.info(
                "GET " + Constants.SEND_MESSAGE
                        + " - instrumentId: {}, channel: {}, messageType: {}, data1: {}, data2: {}",
                instrumentId, channel, messageType, data1, data2);
        InstrumentWrapper instrument = instrumentService.getInstrumentById((long) instrumentId);
        if (Objects.nonNull(instrument)) {
            instrument.setDevice(DeviceManager.getMidiDevice(instrument.getDeviceName()));
            if (Objects.nonNull(instrument.getDevice())) {
                // instrument.setChannel(channel);
                DeviceManager.sendMessage(instrument, channel, messageType, data1, data2);
            }
        }

    }
}
