package com.angrysurfer.midi.service.test;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;


import com.angrysurfer.midi.repo.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.angrysurfer.BeatGeneratorApplication;
import com.angrysurfer.midi.model.MidiInstrument;
import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.Ticker;
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
    StrikeRepository playerRepository;

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
        playerRepository.deleteAll();

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
    public void whenRuleRemoved_thenTickerCopyOfPlayerShouldNotContainIt() {
        Player player = playerService.addPlayer(RAZ);
        Rule rule = playerService.addRule(player.getId());
        Player tickerPlayer = playerService.getTicker().getPlayer(player.getId());
        assertEquals(1, tickerPlayer.getRules().size());

        playerService.removeRule(tickerPlayer.getId(), rule.getId());
        assertEquals(0, tickerPlayer.getRules().size());
    }

    @Test
    public void whenRuleUpdated_thenPlayerCopyOfRuleShouldReflectIt() {
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());
        Player tickerPlayer = playerService.getTicker().getPlayer(player.getId());

        assertTrue(tickerPlayer.getRules().size() > 0);
        Rule rule = tickerPlayer.getRules().stream().toList().get(0);
        int operatorId = rule.getOperatorId() + 1;
        int comparisonId = rule.getComparisonId() + 1;
        double value = rule.getValue() + 1;

        playerService.updateRule(tickerPlayer.getId(), rule.getId(), operatorId,
                comparisonId, value);

        assertEquals(tickerPlayer.getRules().stream().toList().get(0).getOperatorId(), operatorId);
        assertEquals(tickerPlayer.getRules().stream().toList().get(0).getComparisonId(), comparisonId);
        assertEquals(tickerPlayer.getRules().stream().toList().get(0).getValue(), value, 0.0); 
    }

    @Test
    public void whenNextTickerRequestedWithNoPlayers_thenSameTickerReturned() {
        // add player to current ticker to ensure we get new record when calling next()
        playerService.addPlayer(RAZ);
        Long id = playerService.getTicker().getId();
        Ticker nextTicker = playerService.next(id);
        assertTrue(nextTicker.getId() > id);

        Ticker nextNextTicker = playerService.next(nextTicker.getId());
        assertSame(nextTicker.getId(), nextNextTicker.getId());
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

    @Test
    public void whenPreviousTickerRequestedForTickWithPlayers_thenPlayersContainAddedRule() {
        Long startingTickerId = playerService.getTicker().getId();
        Player player = playerService.addPlayer(RAZ);
        Rule rule = playerService.addRule(player.getId());

        // move to next ticker, add player and rule
        Ticker ticker2 = playerService.next(startingTickerId);
        playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        // return to starting ticker
        playerService.previous(ticker2.getId());

        player = playerService.getTicker().getPlayer(player.getId());
        boolean[] found = { false };
        player.getRules().forEach(r -> {
            if (r.getId().equals(rule.getId()))
                found[0] = true;
        });

        assertTrue(found[0]); 
    }

    @Test
    public void whenNextTickerRequestedForTickWithPlayers_thenPlayersContainAddedRule() {
        
        Long startingTickerId = playerService.getTicker().getId();
        Player player = playerService.addPlayer(RAZ);
        Rule rule = playerService.addRule(player.getId());

        // move to next ticker, add player and rule
        Ticker ticker2 = playerService.next(startingTickerId);
        Player player2 = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        // return to starting ticker
        Ticker orig = playerService.previous(ticker2.getId());
        assertTrue(startingTickerId == orig.getId());

        // advance again
        Ticker ticker3 = playerService.next(orig.getId());



        player2 = ticker3.getPlayer(player2.getId());
        boolean[] found = { false };
        player2.getRules().forEach(r -> {
            if (r.getId().equals(rule.getId()))
                found[0] = true;
        });

        assertTrue(found[0]); 
    }

    @Test
    public void whenPlayerMuted_thenTickerReturnsMutedPlayer() {
        Player player = playerService.addPlayer(RAZ);
        boolean muted = player.isMuted();
        playerService.mutePlayer(player.getId());
        Player tickerPlayer = playerService.getTicker().getPlayer(player.getId());
        assertTrue(tickerPlayer.isMuted() != muted); 
    }

    @Test
    public void whenPlayerAdded_thenTickerContainsPlayer() {
        Player player = playerService.addPlayer(RAZ);
        assertTrue(playerService.getTicker().getPlayers().contains(player)); 
    }

    @Test
    public void whenPlayerRemoved_thenTickerNoLongerContainsPlayer() {
        Player player = playerService.addPlayer(RAZ);
        playerService.removePlayer(player.getId());
        assertTrue(!playerService.getTicker().getPlayers().contains(player)); 
    }

    @Test
    public void whenPlayersAdded_thenTickerContainsPlayers() {
        playerService.getTicker().getPlayers().clear();
        playerService.addPlayer(RAZ);
        playerService.addPlayer(RAZ);
        playerService.addPlayer(RAZ);
        assertTrue(playerService.getTicker().getPlayers().size() > 0); 
    }

    @Test
    public void whenPlayersCleared_thenTickerNoLongerContainsPlayers() {
        playerService.addPlayer(RAZ);
        playerService.addPlayer(RAZ);
        playerService.addPlayer(RAZ);
        playerService.clearPlayers();
        assertTrue(playerService.getTicker().getPlayers().size() == 0); 
    }

    @Test
    public void whenNewTickerCalled_thenGetTickerReturnsTickerWithNewId() {
        Long id = playerService.getTicker().getId();
        playerService.newTicker();
        assertTrue(playerService.getTicker().getId() > id); 
    }

    @Test
    public void whenLoadTickerCalled_thenGetTickerReturnsRequestedTicker() {
        Long id = playerService.getTicker().getId();
        playerService.newTicker();
        assertTrue(playerService.getTicker().getId() > id);
        
        playerService.loadTicker(id);
        assertTrue(playerService.getTicker().getId() == id);
    }
}
