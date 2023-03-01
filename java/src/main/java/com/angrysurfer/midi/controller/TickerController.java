package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.config.TickerInfo;
import com.angrysurfer.midi.service.BeatGeneratorService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class TickerController {

    private final BeatGeneratorService service;

    public TickerController(BeatGeneratorService service) {
        this.service = service;
    }

    @GetMapping(path = "/ticker/load")
    public void load(@RequestParam long tickerId) {
        service.loadTicker(tickerId);
    }
    @GetMapping(path = "/ticker/start")
    public void play() {
        service.play();
    }

    @GetMapping(path = "/ticker/pause")
    public void pause() {
        service.pause();
    }

    @GetMapping(path = "/ticker/stop")
    public void stop() {
        service.stop();
    }

    @GetMapping(path = "/ticker/next")
    public void next() {
        service.next();
    }

    @GetMapping(path = "/ticker/info")
    public @ResponseBody TickerInfo getTickerInfo() {
        return service.getTickerInfo();
    }
}

