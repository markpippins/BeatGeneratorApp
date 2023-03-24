package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.Ticker;
import com.angrysurfer.midi.service.PlayerService;
import com.angrysurfer.midi.service.TickerService;
import com.angrysurfer.midi.util.Constants;

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

    private final TickerService tickerService;

    public TickerController(PlayerService service, TickerService tickerService) {
        this.service = service;
        this.tickerService = tickerService;
    }

    // @GetMapping(path = Constants.ADD_TICKER)
    // public Ticker newTicker() {
    //     if (requestsToLog.contains("new"))
    //         logger.info(Constants.ADD_TICKER);
    //     return service.newTicker();
    // }

    @GetMapping(path = Constants.LOAD_TICKER)
    public Ticker load(@RequestParam long tickerId) {
        if (requestsToLog.contains("load"))
            logger.info(Constants.LOAD_TICKER);
        return tickerService.loadTicker(tickerId);
    }

    @GetMapping(path = Constants.START_TICKER)
    public void start() {
        if (requestsToLog.contains("start"))
            logger.info(Constants.START_TICKER);
            tickerService.play();
    }

    @GetMapping(path = Constants.PAUSE_TICKER)
    public void pause() {
        if (requestsToLog.contains("pause"))
           logger.info(Constants.PAUSE_TICKER);
           tickerService.pause();
    }

    @GetMapping(path = Constants.STOP_TICKER)
    public Ticker stop() {
        if (requestsToLog.contains("stop"))
           logger.info(Constants.STOP_TICKER);
        return tickerService.stop();
    }

    @GetMapping(path = Constants.NEXT_TICKER)
    public Ticker next(@RequestParam long currentTickerId) {
        if (requestsToLog.contains("next"))
           logger.info(Constants.NEXT_TICKER);
        return tickerService.next(currentTickerId);
    }

    @GetMapping(path = Constants.PREV_TICKER)
    public Ticker previous(@RequestParam long currentTickerId) {
        if (requestsToLog.contains("previous"))
           logger.info(Constants.PREV_TICKER);
        return tickerService.previous(currentTickerId);
    }

    @GetMapping(path = Constants.TICKER_STATUS)
    public @ResponseBody Ticker getTickerStatus() {
        if (requestsToLog.contains("status"))
           logger.info(Constants.TICKER_STATUS);
        return tickerService.getTickerStatus();
    }

    @GetMapping(path = Constants.TICKER_INFO)
    public @ResponseBody Ticker getTicker() {
        if (requestsToLog.contains("info"))
           logger.info(Constants.TICKER_INFO);
        return tickerService.getTicker();
    }

    @GetMapping(path = Constants.TICKER_LOG)
    public void toggleShowTicker(@RequestParam String requestType) {
        if (requestsToLog.contains(requestType))
            requestsToLog.remove(requestType);
        else 
            requestsToLog.add(requestType);
    }

    @GetMapping(Constants.UPDATE_TICKER)
    public @ResponseBody Ticker updateTicker(@RequestParam Long tickerId, @RequestParam int updateType, @RequestParam int updateValue) {
        if (requestsToLog.contains("update"))
            logger.info(Constants.UPDATE_TICKER);
        return tickerService.updateTicker(tickerId, updateType, updateValue);
    }

}

    // @GetMapping(path = "/tickers/info")
    // public @ResponseBody List<Ticker> getAllTicker() {
    //     if (requestsToLog.contains("all-info"))
    //         logger.info("/tickers/info");
    //     return service.getAllTickerInfos();
    // }

    //  @MessageMapping("/tick")
    //  @SendTo("/topic/messages")
    //  public Ticker send() throws Exception {
    //      // String time = new SimpleDateFormat("HH:mm").format(new Date());
    //      return new Ticker();
    //  }


