package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.TickerInfo;
import com.angrysurfer.midi.service.BeatGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class TickerController {

    List<String> requestsToLog = new ArrayList<>();

    static Logger logger = LoggerFactory.getLogger(TickerController.class.getCanonicalName());


    private final BeatGeneratorService service;

    public TickerController(BeatGeneratorService service) {
        this.service = service;
    }

    @GetMapping(path = "/ticker/new")
    public TickerInfo newTicker() {
        if (requestsToLog.contains("new"))
            logger.info("/ticker/new");
        return service.newTicker();
    }

    @GetMapping(path = "/ticker/load")
    public TickerInfo load(@RequestParam long tickerId) {
        if (requestsToLog.contains("load"))
            logger.info("/ticker/load");
        return service.loadTicker(tickerId);
    }

    @GetMapping(path = "/ticker/start")
    public void start() {
        if (requestsToLog.contains("start"))
            logger.info("/ticker/start");
        service.play();
    }

    @GetMapping(path = "/ticker/pause")
    public void pause() {
        if (requestsToLog.contains("pause"))
           logger.info("/ticker/pause");
        service.pause();
    }

    @GetMapping(path = "/ticker/stop")
    public TickerInfo stop() {
        if (requestsToLog.contains("stop"))
           logger.info("/ticker/stop");
        return service.stop();
    }

    @GetMapping(path = "/ticker/next")
    public TickerInfo next(@RequestParam long currentTickerId) {
        if (requestsToLog.contains("next"))
           logger.info("/ticker/next");
        return service.next(currentTickerId);
    }

    @GetMapping(path = "/ticker/previous")
    public TickerInfo previous(@RequestParam long currentTickerId) {
        if (requestsToLog.contains("previous"))
           logger.info("/ticker/next");
        return service.previous(currentTickerId);
    }

    @GetMapping(path = "/ticker/status")
    public @ResponseBody TickerInfo getTickerStatus() {
        if (requestsToLog.contains("status"))
           logger.info("/ticker/status");
        return service.getTickerStatus();
    }

    @GetMapping(path = "/ticker/info")
    public @ResponseBody TickerInfo getTickerInfo() {
        if (requestsToLog.contains("info"))
           logger.info("/ticker/info");
        return service.getTickerInfo();
    }

    @GetMapping(path = "/ticker/log")
    public void toggleShowTickerInfo(@RequestParam String requestType) {
        if (requestsToLog.contains(requestType))
            requestsToLog.remove(requestType);
        else 
            requestsToLog.add(requestType);
    }

    @GetMapping(path = "/tickers/info")
    public @ResponseBody List<TickerInfo> getAllTickerInfo() {
        if (requestsToLog.contains("all-info"))
            logger.info("/tickers/info");
        return service.getAllTickerInfo();
    }

    @GetMapping("/ticker/update")
    public @ResponseBody TickerInfo updateTicker(@RequestParam Long tickerId, @RequestParam int updateType, @RequestParam int updateValue) {
        if (requestsToLog.contains("update"))
            logger.info("/ticker/update");
        return service.updateTicker(tickerId, updateType, updateValue);
    }


}

