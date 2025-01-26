package com.angrysurfer.midi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import com.angrysurfer.midi.model.Pattern;
import com.angrysurfer.midi.model.Song;
import com.angrysurfer.midi.model.Step;
import com.angrysurfer.midi.service.SongService;
import com.angrysurfer.midi.util.Constants;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class SongController {
    // double numberOfTicksToWait = getBeatFraction() *
    // (getTicker().getTicksPerBeat() / getSubDivisions());
    List<String> requestsToLog = new ArrayList<>();

    static Logger logger = LoggerFactory.getLogger(SongController.class.getCanonicalName());

    private final SongService service;

    public SongController(SongService service) {
        this.service = service;
    }

    @GetMapping(path = "/steps/update")
    public Step updateStep(@RequestParam long stepId, @RequestParam int updateType, @RequestParam int updateValue) {
        return service.updateStep(stepId, updateType, updateValue);
    }

    @GetMapping(path = "/patterns/update")
    public Pattern updatePatter(@RequestParam long patternId, @RequestParam int updateType,
            @RequestParam int updateValue) {
        return service.updatePattern(patternId, updateType, updateValue);
    }

    @GetMapping(path = Constants.SONG_INFO)
    public Song songInfo() {
        return service.getSongInfo();
    }

    @GetMapping(path = "/song/new")
    public Song newSong() {
        if (requestsToLog.contains("new"))
            logger.info("/song/new");
        return service.newSong();
    }

    // @GetMapping(path = "/song/load")
    // public Song load(@RequestParam long songId) {
    // if (requestsToLog.contains("load"))
    // logger.info("/song/load");
    // return service.loadSong(songId);
    // }

    @GetMapping(path = Constants.NEXT_SONG)
    public Song next(@RequestParam long currentSongId) {
        if (requestsToLog.contains("next"))
            logger.info(Constants.NEXT_SONG);
        return service.next(currentSongId);
    }

    @GetMapping(path = Constants.PREV_SONG)
    public Song previous(@RequestParam long currentSongId) {
        if (requestsToLog.contains("previous"))
            logger.info(Constants.PREV_SONG);
        return service.previous(currentSongId);
    }


}

// @GetMapping(path = "/song/start")
// public void start() {
// if (requestsToLog.contains("start"))
// logger.info("/song/start");
// service.play();
// }

// @GetMapping(path = "/song/pause")
// public void pause() {
// if (requestsToLog.contains("pause"))
// logger.info("/song/pause");
// service.pause();
// }

// @GetMapping(path = "/song/stop")
// public SongInfo stop() {
// if (requestsToLog.contains("stop"))
// logger.info("/song/stop");
// return service.stop();
// }

// @GetMapping(path = "/song/next")
// public SongInfo next(@RequestParam long currentSongId) {
// if (requestsToLog.contains("next"))
// logger.info("/song/next");
// return service.next(currentSongId);
// }

// @GetMapping(path = "/song/previous")
// public SongInfo previous(@RequestParam long currentSongId) {
// if (requestsToLog.contains("previous"))
// logger.info("/song/next");
// return service.previous(currentSongId);
// }

// @GetMapping(path = "/xox")
// public @ResponseBody SongInfo getSongStatus() {
// if (requestsToLog.contains("status"))
// logger.info("/xox");
// return service.getSongStatus();
// }

// @GetMapping(path = "/song/info")
// public @ResponseBody SongInfo getSongInfo() {
// if (requestsToLog.contains("info"))
// logger.info("/song/info");
// return service.getSongInfo();
// }

// @GetMapping(path = "/song/log")
// public void toggleShowSongInfo(@RequestParam String requestType) {
// if (requestsToLog.contains(requestType))
// requestsToLog.remove(requestType);
// else
// requestsToLog.add(requestType);
// }

// @GetMapping(path = "/songs/info")
// public @ResponseBody List<SongInfo> getAllSongInfo() {
// if (requestsToLog.contains("all-info"))
// logger.info("/songs/info");
// return service.getAllSongInfo();
// }

// @GetMapping("/song/update")
// public @ResponseBody SongInfo updateSong(@RequestParam Long songId,
// @RequestParam int updateType, @RequestParam int updateValue) {
// if (requestsToLog.contains("update"))
// logger.info("/song/update");
// return service.updateSong(songId, updateType, updateValue);
// }
