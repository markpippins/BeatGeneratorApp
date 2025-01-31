package com.angrysurfer.midi.service.test;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.angrysurfer.sequencer.Application;
import com.angrysurfer.sequencer.model.Pattern;
import com.angrysurfer.sequencer.model.Song;
import com.angrysurfer.sequencer.repo.Instruments;
import com.angrysurfer.sequencer.repo.Songs;
import com.angrysurfer.sequencer.repo.Steps;
import com.angrysurfer.sequencer.service.SongService;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = Application.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class SongServiceTests {
    static Logger logger = LoggerFactory.getLogger(SongServiceTests.class.getCanonicalName());

    @Autowired
    SongService songService;

    @Autowired
    Instruments midiInstrumentRepository;

    @Autowired
    Steps stepRepository;

    @Autowired
    Songs songRepository;

    @Before
    public void setUp() {
        // songRepository.deleteAll();
    }

    @After
    public void tearDown() {
        // songRepository.deleteAll();
    }

    @Test
    public void whenSongRetrieved_thenItHasBeenSaved() {
        assertNotNull(songService.getSong().getId());
    }

    @Test
    public void whenNewSongCalled_thenGetSongWithNewId() {
        songService.newSong();
        Long id = songService.getSong().getId();
        songService.newSong();
        assertTrue(songService.getSong().getId() > id);
    }

    // @Test
    // public void whenLoadSongCalled_thenGetRequestedSong() {
    //     songService.newSong();
    //     Long id = songService.getSong().getId();
    //     songService.newSong();
    //     assertTrue(songService.getSong().getId() > id);
    //     songService.newSong();
    //     assertTrue(songService.getSong().getId() > id);
    //     songService.newSong();
    //     assertTrue(songService.getSong().getId() > id);

    //     Song song = songService.loadSong(id);
    //     assertTrue(Objects.nonNull(song));
    // }

    @Test
    public void whenNextSongRequestedWithNoPatterns_thenSameSongReturned() {
        songService.newSong();
        Long id = songService.getSong().getId();
        songService.getSong().getPatterns().clear();
        Song nextSong = songService.next(id);
        assertSame(nextSong.getId(), id);
    }

    @Test
    public void whenNextSongRequestedWithPatterns_thenNewSongReturned() {
        songService.newSong();
        songService.addPattern();
        Long id = songService.getSong().getId();
        Song nextSong = songService.next(id);
        assertTrue(nextSong.getId() > id);
    }

    @Test
    public void whenNextSongRequestedWithPatterns_thenNextSongCanBeRequested() {
        songService.newSong();
        IntStream.range(0, 5).forEach(i -> {
            songService.addPattern();
            Long id = songService.getSong().getId();
            Song nextSong = songService.next(id);
            assertTrue(nextSong.getId() > id);
        });
    }

    @Test
    public void whenPreviousSongRequestedWith_thenSongWithLowerIdReturned() {
        songService.newSong();
        songService.addPattern();
        Long id = songService.getSong().getId();
        Song nextSong = songService.next(id);
        assertTrue(nextSong.getId() > id);

        Song prevSong = songService.previous(nextSong.getId());
        assertTrue(prevSong.getId() < nextSong.getId());
    }

    @Test
    public void whenPreviousSongRequestedForTickWithPatterns_thenPatternsContainAddedPattern() {
        Long startingSongId = songService.getSong().getId();
        Pattern pattern = songService.addPattern();

        // move to next song, add step and step
        songService.next(startingSongId);
        Long nextSongId = songService.getSong().getId();
        songService.addPattern();

        // return to starting song
        songService.previous(nextSongId);
        Pattern pattern2 = songService.getSong().getPatterns().stream().filter(s -> s.getId().equals(pattern.getId()))
                .findAny().orElseThrow();
        assertTrue(songService.getSong().getPatterns().stream()
                .anyMatch(r -> r.getPosition().equals(pattern2.getPosition())));
    }

    @Test
    public void whenNextSongRequestedForSongWithPatterns_thenPatternsContainAddedPatter() {
        // songService.newSong();
        // add data to current Song
        Long startingSongId = songService.getSong().getId();
        Pattern pattern = songService.addPattern();
        songService.addPattern();

        // move to next song, add step and step
        Song song2 = songService.next(startingSongId);
        Pattern pattern2 = songService.addPattern();
        // Step step = songService.addStep(0);

        // return to starting song
        Song previous = songService.previous(song2.getId());
        Long prevId = previous.getId();

        assertEquals(startingSongId, prevId);

        // advance again
        songService.next(songService.getSong().getId());

        pattern = songService.getSong().getPatterns().stream().filter(s -> s.getId().equals(pattern2.getId()))
                .findAny().orElseThrow();

        assertTrue(pattern.getPosition().equals(pattern2.getPosition()));
    }

    // @Test
    // public void whenNextSongRequestedForSongWithPatterns_thenSongDoesNotContainRemovedPattern() {
    //     // songService.newSong();
    //     // add data to current Song
    //     Long startingSongId = songService.getSong().getId();
    //     songService.addPattern();
    //     songService.addPattern();

    //     // move to next song, add step and step
    //     Song song2 = songService.next(startingSongId);
    //     Pattern pattern = songService.addPattern();
    //     // remove pattern
    //     songService.removePattern(pattern.getId());
    //     // return to starting song
    //     assertEquals(startingSongId, songService.previous(song2.getId()).getId());

    //     // advance again
    //     songService.next(songService.getSong().getId());

    //     // Step step3 = songService.getSong().getSteps().stream().filter(s ->
    //     // s.getId().equals(step2.getId()));
    //     // Step step2 = step3.getStep(step.getId());
    //     assertTrue(songService.getSong().getPatterns().isEmpty());
    // }

}
