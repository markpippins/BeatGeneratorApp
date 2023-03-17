package com.angrysurfer;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.angrysurfer.midi.model.MidiInstrument;
import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.Ticker;
import com.angrysurfer.midi.repo.ControlCodeRepository;
import com.angrysurfer.midi.repo.MidiInstrumentRepository;
import com.angrysurfer.midi.repo.PadRepository;
import com.angrysurfer.midi.repo.PlayerRepository;
import com.angrysurfer.midi.repo.RuleRepository;
import com.angrysurfer.midi.repo.SongRepository;
import com.angrysurfer.midi.repo.StepRepository;
import com.angrysurfer.midi.repo.TickerRepo;
import com.angrysurfer.midi.service.MIDIService;
import com.angrysurfer.midi.service.PlayerService;

@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = BeatGeneratorApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
  locations = "classpath:application.properties")
public class PlayerServiceTests {

    @Autowired
    PlayerService playerService;

    @Autowired
    MIDIService midiService;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    RuleRepository ruleRepository;

    @Autowired
    TickerRepo tickerRepo;

    @Autowired
    MidiInstrumentRepository midiInstrumentRepository;

    @Autowired
    ControlCodeRepository controlCodeRepository;

    @Autowired
    PadRepository padRepository;

    @Autowired
    StepRepository stepRepository;

    @Autowired
    SongRepository songRepository;

    static String RAZ = "raz";
    static String ZERO = "zero";

    @Before
    public void setUp() {
//        tickerRepo.deleteAll();

        if (!midiInstrumentRepository.findByName(RAZ).isPresent()) {
            MidiInstrument raz = new MidiInstrument();
            raz.setName(RAZ);
            raz.setDeviceName("MRCC 880");
            raz.setChannel(9);
            midiInstrumentRepository.save(raz);
        }
    }

    @Test
    public void whenTickerRetrieved_thenItHasBeenSaved() {
        assertNotNull(playerService.getTicker());
    }

    @Test
    public void whenPlayerAdded_thenTickerShouldNotBeNull() {
        Player player = playerService.addPlayer(RAZ);
        assertNotNull(player.getTicker());
    }
    
    @Test
    public void whenRuleAdded_thenTickerCopyOfPlayerShouldContainIt() {
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());
        Player tickerPlayer = playerService.getTicker().getPlayer(player.getId());
        assertEquals(1, tickerPlayer.getRules().size());
    }

    @Test
    public void whenNextTickerRequestedWithNoPlayers_thenSameTickerReturned() {
        Long id = playerService.getTicker().getId();
        Ticker nextTicker = playerService.next(id);
        assertSame(nextTicker.getId(), id);
    }

    @Test
    public void whenNextTickerRequestedWithPlayers_thenNewTickerReturned() {
        playerService.addPlayer(RAZ);
        Long id = playerService.getTicker().getId();
        Ticker nextTicker = playerService.next(id);
        assertTrue(nextTicker.getId() > id); 
    }

    @Test
    public void whenPreviousTickerRequestedWith_thenTickerWithLowerIdReturned() {
        playerService.addPlayer(RAZ);
        Long id = playerService.getTicker().getId();
        Ticker nextTicker = playerService.next(id);
        assertTrue(nextTicker.getId() > id); 

        Ticker prevTicker = playerService.previous(nextTicker.getId());
        assertTrue(prevTicker.getId() < nextTicker.getId()); 
    }
}
