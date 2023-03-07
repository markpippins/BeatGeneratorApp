package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.TickerInfo;
import com.angrysurfer.midi.service.BeatGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class TickerController {

    static Logger logger = LoggerFactory.getLogger(TickerController.class.getCanonicalName());


    private final BeatGeneratorService service;

    public TickerController(BeatGeneratorService service) {
        this.service = service;
    }

    @GetMapping(path = "/ticker/new")
    public TickerInfo newTicker() {
        logger.info("/ticker/new");
        return service.newTicker();
    }

    @GetMapping(path = "/ticker/load")
    public TickerInfo load(@RequestParam long tickerId) {
        logger.info("/ticker/load");
        return service.loadTicker(tickerId);
    }

    @GetMapping(path = "/ticker/start")
    public void play() {
        logger.info("/ticker/start");
        service.play();
    }

    @GetMapping(path = "/ticker/pause")
    public void pause() {
        logger.info("/ticker/pause");
        service.pause();
    }

    @GetMapping(path = "/ticker/stop")
    public TickerInfo stop() {
        logger.info("/ticker/stop");
        return service.stop();
    }

    @GetMapping(path = "/ticker/next")
    public TickerInfo next(@RequestParam long currentTickerId) {
        logger.info("/ticker/next");
        return service.next(currentTickerId);
    }

    @GetMapping(path = "/ticker/previous")
    public TickerInfo previous(@RequestParam long currentTickerId) {
        logger.info("/ticker/next");
        return service.previous(currentTickerId);
    }

    @GetMapping(path = "/ticker/status")
    public @ResponseBody TickerInfo getTickerStatus() {
//        logger.info("/ticker/info");
        return service.getTickerStatus();
    }

    @GetMapping(path = "/ticker/info")
    public @ResponseBody TickerInfo getTickerInfo() {
//        logger.info("/ticker/info");
        return service.getTickerInfo();
    }

    @GetMapping(path = "/tickers/info")
    public @ResponseBody List<TickerInfo> getAllTickerInfo() {
//        logger.info("/ticker/info");
        return service.getAllTickerInfo();
    }

}

