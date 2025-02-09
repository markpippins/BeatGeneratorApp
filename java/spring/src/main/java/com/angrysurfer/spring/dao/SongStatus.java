package com.angrysurfer.spring.dao;

import java.util.ArrayList;
import java.util.List;

import com.angrysurfer.core.model.Song;

public class SongStatus {
    List<PatternStatus> patternStatuses = new ArrayList<>();

    public static SongStatus from(Song song) {
        SongStatus  status = new SongStatus();
        song.getPatterns().forEach(p -> status.patternStatuses.add(PatternStatus.from(p)));
        return status;
    }
}
