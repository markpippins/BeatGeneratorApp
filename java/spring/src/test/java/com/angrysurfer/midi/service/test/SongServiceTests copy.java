// package com.angrysurfer.midi.service.test;

// import static org.junit.Assert.assertFalse;
// import static org.junit.Assert.assertSame;
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertTrue;

// import java.util.Objects;
// import java.util.Optional;
// import java.util.stream.IntStream;

// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.TestPropertySource;
// import org.springframework.test.context.junit4.SpringRunner;

// import com.angrysurfer.BeatGeneratorApplication;
// import com.angrysurfer.midi.model.Song;
// import com.angrysurfer.midi.model.Step;
// import com.angrysurfer.midi.repo.MidiInstrumentRepo;
// import com.angrysurfer.midi.repo.SongRepo;
// import com.angrysurfer.midi.repo.StepRepo;
// import com.angrysurfer.midi.service.MIDIService;
// import com.angrysurfer.midi.service.SongService;

// @RunWith(SpringRunner.class)
// @SpringBootTest(
//   webEnvironment = SpringBootTest.WebEnvironment.MOCK,
//   classes = BeatGeneratorApplication.class)
// @AutoConfigureMockMvc
// @TestPropertySource(
//   locations = "classpath:application-test.properties")
// public class SongServiceTests {
//     static Logger logger = LoggerFactory.getLogger(SongServiceTests.class.getCanonicalName());

//     @Autowired
//     SongService songService;

//     @Autowired
//     MidiInstrumentRepo instrumentRepo;

//     @Autowired
//     StepRepo stepRepo;

//     @Autowired
//     SongRepo songRepo;

//     @Before
//     public void setUp() {
//         // songRepo.deleteAll();
//     }
    
//     @After
//     public void tearDown() {
//         // songRepo.deleteAll();
//     }
    

//     @Test
//     public void whenSongRetrieved_thenItHasBeenSaved() {
//         assertNotNull(songService.getSong().getId());
//     }

//     @Test
//     public void whenNewSongCalled_thenGetSongWithNewId() {
//         songService.newSong();
//         Long id = songService.getSong().getId();
//         songService.newSong();
//         assertTrue(songService.getSong().getId() > id); 
//     }

//     @Test
//     public void whenLoadSongCalled_thenGetRequestedSong() {
//         songService.newSong();
//         Long id = songService.getSong().getId();
//         songService.newSong();
//         assertTrue(songService.getSong().getId() > id);
//         songService.newSong();
//         assertTrue(songService.getSong().getId() > id);
//         songService.newSong();
//         assertTrue(songService.getSong().getId() > id);
        
//         Song song = songService.loadSong(id);
//         assertTrue(Objects.nonNull(song));
//     }

    
//     @Test
//     public void whenNextSongRequestedWithNoSteps_thenSameSongReturned() {
//         songService.newSong();
//         Long id = songService.getSong().getId();
//         songService.getSong().getSteps().clear();
//         Song nextSong = songService.next(id);
//         assertSame(nextSong.getId(), id);
//     }

//     @Test
//     public void whenNextSongRequestedWithSteps_thenNewSongReturned() {
//         songService.newSong();
//         songService.addStep(0);
//         Long id = songService.getSong().getId();
//         Song nextSong = songService.next(id);
//         assertTrue(nextSong.getId() > id); 
//     }

//     @Test
//     public void whenNextSongRequestedWithSteps_thenNextSongCanBeRequested() {
//         songService.newSong();
//         IntStream.range(0, 5).forEach(i -> {
//             songService.addStep(0);
//             Long id = songService.getSong().getId();
//             Song nextSong = songService.next(id);
//             assertTrue(nextSong.getId() > id); 
//         });
//     }

//     @Test
//     public void whenPreviousSongRequestedWith_thenSongWithLowerIdReturned() {
//         songService.newSong();
//         songService.addStep(0);
//         Long id = songService.getSong().getId();
//         Song nextSong = songService.next(id);
//         assertTrue(nextSong.getId() > id); 

//         Song prevSong = songService.previous(nextSong.getId());
//         assertTrue(prevSong.getId() < nextSong.getId()); 
//     }

//     @Test
//     public void whenPreviousSongRequestedForTickWithSteps_thenStepsContainAddedStep() {
//         Long startingSongId = songService.getSong().getId();
//         Step step = songService.addStep(0);

//         // move to next song, add step and step
//         songService.next(startingSongId);
//         Long nextSongId = songService.getSong().getId();
//         songService.addStep(0);

//         // return to starting song
//         songService.previous(nextSongId);
//         Step step2 = songService.getSong().getSteps().stream().filter(s -> s.getId().equals(step.getId())).findAny().orElseThrow();
//         assertTrue(songService.getSong().getSteps().stream().anyMatch(r -> r.getPosition().equals(step2.getPosition()))); 
//     }

//     @Test
//     public void whenNextSongRequestedForSongWithSteps_thenStepsContainAddedStep() {
//         // songService.newSong();
//         // add data to current Song
//         Long startingSongId = songService.getSong().getId();
//         Step step = songService.addStep(0);
//         songService.addStep(0);

//         // move to next song, add step and step
//         Song song2 = songService.next(startingSongId);
//         Step step2 = songService.addStep(0);
//         // Step step = songService.addStep(0);

//         // return to starting song
//         Song previous = songService.previous(song2.getId());
//         Long prevId = previous.getId();

//         assertEquals(startingSongId, prevId);

//         // advance again
//         songService.next(songService.getSong().getId());

//         Step step3 = songService.getSong().getSteps().stream().filter(s -> s.getId().equals(step2.getId())).findAny().orElseThrow();
//         // Step step2 = step3.stream().filter(s -> s.getId().equals(step.getId())).findAny().orElseThrow();
//         assertTrue(step.getPosition().equals(step2.getPosition())); 
//     }

//     @Test
//     public void whenNextSongRequestedForSongWithSteps_thenStepDoesNotContainRemovedStep() {
//         // songService.newSong();
//         // add data to current Song
//         Long startingSongId = songService.getSong().getId();
//         songService.addStep(0);
//         songService.addStep(0);

//         // move to next song, add step and step
//         Song song2 = songService.next(startingSongId);
//         Step step = songService.addStep(0);
//         //remove step
//         songService.removeStep(step.getId());
//         // return to starting song
//         assertEquals(startingSongId, songService.previous(song2.getId()).getId());

//         // advance again
//         songService.next(songService.getSong().getId());

//         // Step step3 = songService.getSong().getSteps().stream().filter(s -> s.getId().equals(step2.getId()));
//         // Step step2 = step3.getStep(step.getId());
//         assertTrue(songService.getSong().getSteps().isEmpty()); 
//     }

// }
