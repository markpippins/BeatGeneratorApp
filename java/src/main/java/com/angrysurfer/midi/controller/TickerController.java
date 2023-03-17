package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.Ticker;
import com.angrysurfer.midi.service.PlayerService;
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


    private final PlayerService service;

    public TickerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping(path = "/ticker/new")
    public Ticker newTicker() {
        if (requestsToLog.contains("new"))
            logger.info("/ticker/new");
        return service.newTicker();
    }

    @GetMapping(path = "/ticker/load")
    public Ticker load(@RequestParam long tickerId) {
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
    public Ticker stop() {
        if (requestsToLog.contains("stop"))
           logger.info("/ticker/stop");
        return service.stop();
    }

    @GetMapping(path = "/ticker/next")
    public Ticker next(@RequestParam long currentTickerId) {
        if (requestsToLog.contains("next"))
           logger.info("/ticker/next");
        return service.next(currentTickerId);
    }

    @GetMapping(path = "/ticker/previous")
    public Ticker previous(@RequestParam long currentTickerId) {
        if (requestsToLog.contains("previous"))
           logger.info("/ticker/next");
        return service.previous(currentTickerId);
    }

    @GetMapping(path = "/ticker/status")
    public @ResponseBody Ticker getTickerStatus() {
        if (requestsToLog.contains("status"))
           logger.info("/ticker/status");
        return service.getTickerStatus();
    }

    @GetMapping(path = "/ticker/info")
    public @ResponseBody Ticker getTicker() {
        if (requestsToLog.contains("info"))
           logger.info("/ticker/info");
        return service.getTicker();
    }

    @GetMapping(path = "/ticker/log")
    public void toggleShowTicker(@RequestParam String requestType) {
        if (requestsToLog.contains(requestType))
            requestsToLog.remove(requestType);
        else 
            requestsToLog.add(requestType);
    }

    // @GetMapping(path = "/tickers/info")
    // public @ResponseBody List<Ticker> getAllTicker() {
    //     if (requestsToLog.contains("all-info"))
    //         logger.info("/tickers/info");
    //     return service.getAllTickerInfos();
    // }

    @GetMapping("/ticker/update")
    public @ResponseBody Ticker updateTicker(@RequestParam Long tickerId, @RequestParam int updateType, @RequestParam int updateValue) {
        if (requestsToLog.contains("update"))
            logger.info("/ticker/update");
        return service.updateTicker(tickerId, updateType, updateValue);
    }

    //  @MessageMapping("/tick")
    //  @SendTo("/topic/messages")
    //  public Ticker send() throws Exception {
    //      // String time = new SimpleDateFormat("HH:mm").format(new Date());
    //      return new Ticker();
    //  }
}

