package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.Song;
import com.angrysurfer.midi.model.Step;
import com.angrysurfer.midi.service.SongService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class SongController {

    List<String> requestsToLog = new ArrayList<>();

    static Logger logger = LoggerFactory.getLogger(SongController.class.getCanonicalName());


    private final SongService service;

    public SongController(SongService service) {
        this.service = service;
    }

    
    @PostMapping(path = "/steps/update")
    public void addTrack(@RequestBody Step step) {
        service.updateStep(step);
    }
    
    @GetMapping(path = "/song/new")
    public Song newSong() {
        if (requestsToLog.contains("new"))
            logger.info("/song/new");
        return service.newSong();
    }

    @GetMapping(path = "/song/load")
    public Song load(@RequestParam long songId) {
        if (requestsToLog.contains("load"))
            logger.info("/song/load");
        return service.loadSong(songId);
    }

    // @GetMapping(path = "/song/start")
    // public void start() {
    //     if (requestsToLog.contains("start"))
    //         logger.info("/song/start");
    //     service.play();
    // }

    // @GetMapping(path = "/song/pause")
    // public void pause() {
    //     if (requestsToLog.contains("pause"))
    //        logger.info("/song/pause");
    //     service.pause();
    // }

    // @GetMapping(path = "/song/stop")
    // public SongInfo stop() {
    //     if (requestsToLog.contains("stop"))
    //        logger.info("/song/stop");
    //     return service.stop();
    // }

    // @GetMapping(path = "/song/next")
    // public SongInfo next(@RequestParam long currentSongId) {
    //     if (requestsToLog.contains("next"))
    //        logger.info("/song/next");
    //     return service.next(currentSongId);
    // }

    // @GetMapping(path = "/song/previous")
    // public SongInfo previous(@RequestParam long currentSongId) {
    //     if (requestsToLog.contains("previous"))
    //        logger.info("/song/next");
    //     return service.previous(currentSongId);
    // }

    // @GetMapping(path = "/song/status")
    // public @ResponseBody SongInfo getSongStatus() {
    //     if (requestsToLog.contains("status"))
    //        logger.info("/song/status");
    //     return service.getSongStatus();
    // }

    // @GetMapping(path = "/song/info")
    // public @ResponseBody SongInfo getSongInfo() {
    //     if (requestsToLog.contains("info"))
    //        logger.info("/song/info");
    //     return service.getSongInfo();
    // }

    // @GetMapping(path = "/song/log")
    // public void toggleShowSongInfo(@RequestParam String requestType) {
    //     if (requestsToLog.contains(requestType))
    //         requestsToLog.remove(requestType);
    //     else 
    //         requestsToLog.add(requestType);
    // }

    // @GetMapping(path = "/songs/info")
    // public @ResponseBody List<SongInfo> getAllSongInfo() {
    //     if (requestsToLog.contains("all-info"))
    //         logger.info("/songs/info");
    //     return service.getAllSongInfo();
    // }

    // @GetMapping("/song/update")
    // public @ResponseBody SongInfo updateSong(@RequestParam Long songId, @RequestParam int updateType, @RequestParam int updateValue) {
    //     if (requestsToLog.contains("update"))
    //         logger.info("/song/update");
    //     return service.updateSong(songId, updateType, updateValue);
    // }
}
