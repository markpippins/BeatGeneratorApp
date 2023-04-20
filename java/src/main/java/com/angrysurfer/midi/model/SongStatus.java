package com.angrysurfer.midi.model;

import java.util.ArrayList;
import java.util.List;

public class SongStatus {
    List<PatternStatus> patternStatuses = new ArrayList<>();

    public static SongStatus from(Song song) {
        SongStatus  status = new SongStatus();
        song.getPatterns().forEach(p -> status.patternStatuses.add(PatternStatus.from(p)));
        return status;
    }
}
