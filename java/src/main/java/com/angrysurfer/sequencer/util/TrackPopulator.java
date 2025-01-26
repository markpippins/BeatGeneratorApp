package com.angrysurfer.sequencer.util;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;
import java.util.Map;

public class TrackPopulator {

    public void populateTrack(Track track, Map<Long, MidiEvent> events) {
        for (Map.Entry<Long, MidiEvent> entry : events.entrySet()) {
            long tick = entry.getKey();
            MidiEvent event = entry.getValue();
            track.add(new MidiEvent(event.getMessage(), tick));
        }
    }

    public static void main(String[] args) {
        // Example usage
        // Create a Track and a Map<Long, MidiEvent> and call populateTrack
    }
}
