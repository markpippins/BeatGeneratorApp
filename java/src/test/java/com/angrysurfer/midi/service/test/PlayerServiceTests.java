package com.angrysurfer.midi.service.test;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;
import java.util.stream.IntStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import com.angrysurfer.midi.repo.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import com.angrysurfer.midi.util.Comparison;
import com.angrysurfer.midi.util.PlayerUpdateType;
import com.angrysurfer.midi.util.TickerUpdateType;

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

    static String RAZ = "razzmatazz";
    static String ZERO = "zero";

    Long razId;

    @Before
    public void setUp() {
        playerRepository.deleteAll();
    }
    
    @Before
    public void tearDown() {
        playerRepository.deleteAll();
    }
    
    @Test
    public void whenInstrumentRequestedDeviceIsInitialized() {
        MidiInstrument raz = midiService.getInstrumentByChannel(15);
        try {
            raz.noteOn(60, 0);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
    public void whenMultipleRulesAdded_thenPlayerShouldContainThem() {
        Player player = playerService.addPlayer(RAZ);
        Rule r1 = playerService.addRule(player.getId());
        r1.setComparisonId(Comparison.EQUALS);

        Rule r2 = playerService.addRule(player.getId());
        r2.setComparisonId(Comparison.GREATER_THAN);
        
        Rule r3 = playerService.addRule(player.getId());
        r3.setComparisonId(Comparison.LESS_THAN);
        
        Player tickerPlayer = playerService.getTicker().getPlayer(player.getId());
        assertEquals(3, tickerPlayer.getRules().size());
    }

    @Test
    public void whenRuleAddTried_thenEnsureThatExistingRulesHaveBeenInitialized() {
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());
        playerService.addRule(player.getId());
        playerService.addRule(player.getId());
        playerService.addRule(player.getId());
        playerService.addRule(player.getId());

        assertEquals(2, player.getRules().size());
    }

    @Test
    public void whenRuleAdded_thenPlayerShouldContainIt() {
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());
        assertEquals(1, player.getRules().size());
    }

    @Test
    public void whenRuleRemoved_thenPlayerShouldNotContainIt() {
        Player player = playerService.addPlayer(RAZ);
        Rule rule = playerService.addRule(player.getId());
        assertEquals(1, player.getRules().size());

        playerService.removeRule(player.getId(), rule.getId());
        assertEquals(0, player.getRules().size());
    }

    @Test
    public void whenRuleValueUpdated_thenRuleShouldReflectIt() {
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        assertTrue(player.getRules().size() > 0);
        Rule rule = player.getRules().stream().toList().get(0);
        double value = rule.getValue() + 1;

        playerService.updateRule(player.getId(), rule.getId(), rule.getOperatorId(),
                rule.getComparisonId(), value, rule.getPart());

        assertEquals(player.getRules().stream().toList().get(0).getValue(), value, 0.0); 
    }

    @Test
    public void whenRuleOperatorUpdated_thenRuleShouldReflectIt() {
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        assertTrue(player.getRules().size() > 0);
        Rule rule = player.getRules().stream().toList().get(0);
        int operatorId = rule.getOperatorId() + 1;

        playerService.updateRule(player.getId(), rule.getId(), operatorId,
                rule.getComparisonId(), rule.getValue(), rule.getPart());

        assertEquals(player.getRules().stream().toList().get(0).getOperatorId(), operatorId);
    }

    @Test
    public void whenRulePartUpdated_thenRuleShouldReflectIt() {
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        assertTrue(player.getRules().size() > 0);
        Rule rule = player.getRules().stream().toList().get(0);
        int part = rule.getPart() + 1;

        playerService.updateRule(player.getId(), rule.getId(), rule.getOperatorId(),
                part, rule.getValue(), part);

        assertEquals(player.getRule(rule.getId()).getPart(), part);
    }

    @Test
    public void whenRuleComparisonUpdated_thenRuleShouldReflectIt() {
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        assertTrue(player.getRules().size() > 0);
        Rule rule = player.getRules().stream().toList().get(0);
        int comparisonId = rule.getComparisonId() + 1;

        playerService.updateRule(player.getId(), rule.getId(), rule.getOperatorId(),
                comparisonId, rule.getValue(), rule.getPart());

        assertEquals(player.getRules().stream().toList().get(0).getComparisonId(), comparisonId);
    }

    @Test
    public void whenNextTickerRequestedWithNoPlayers_thenSameTickerReturned() {

        Long id = playerService.getTicker().getId();
        playerService.getTicker().getPlayers().clear();
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
    public void whenNextTickerRequestedWithPlayers_thenNextTickerCanBeRequested() {
        IntStream.range(0, 99).forEach(i -> {
            playerService.addPlayer(RAZ);
            Long id = playerService.getTicker().getId();
            Ticker nextTicker = playerService.next(id);
            assertTrue(nextTicker.getId() > id); 
        });
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
        playerService.next(startingTickerId);
        Long nextTickerId = playerService.getTicker().getId();
        playerService.addRule(playerService.addPlayer(RAZ).getId());

        // return to starting ticker
        playerService.previous(nextTickerId);
        player = playerService.getTicker().getPlayer(player.getId());
        assertTrue(player.getRules().stream().anyMatch(r -> r.isEqualTo(rule))); 
    }

    @Test
    public void whenNextTickerRequestedForTickerWithPlayers_thenPlayersContainAddedRule() {
        // playerService.newTicker();
        // add data to current Ticker
        Long startingTickerId = playerService.getTicker().getId();
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        // move to next ticker, add player and rule
        Ticker ticker2 = playerService.next(startingTickerId);
        Player player2 = playerService.addPlayer(RAZ);
        Rule rule = playerService.addRule(player2.getId());

        // return to starting ticker
        Ticker previous = playerService.previous(ticker2.getId());
        Long prevId = previous.getId();

        assertEquals(startingTickerId, prevId);

        // advance again
        playerService.next(playerService.getTicker().getId());

        Player player3 = playerService.getTicker().getPlayer(player2.getId());
        Rule rule2 = player3.getRule(rule.getId());
        assertTrue(rule.isEqualTo(rule2)); 
    }

    @Test
    public void whenNextTickerRequestedForTickerWithPlayers_thenPlayerDoesNotContainRemovedRule() {
        // playerService.newTicker();
        // add data to current Ticker
        Long startingTickerId = playerService.getTicker().getId();
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        // move to next ticker, add player and rule
        Ticker ticker2 = playerService.next(startingTickerId);
        Player player2 = playerService.addPlayer(RAZ);
        Rule rule = playerService.addRule(player2.getId());

        //remove rule
        playerService.removeRule(player2.getId(), rule.getId());
        // return to starting ticker
        assertEquals(startingTickerId, playerService.previous(ticker2.getId()).getId());

        // advance again
        playerService.next(playerService.getTicker().getId());

        Player player3 = playerService.getTicker().getPlayer(player2.getId());
        // Rule rule2 = player3.getRule(rule.getId());
        assertTrue(player3.getRules().isEmpty()); 
    }

    @Test
    public void whenPlayerMuted_thenMutedPlayer() {
        Player player = playerService.addPlayer(RAZ);
        boolean muted = player.isMuted();
        playerService.mutePlayer(player.getId());
        assertTrue(player.isMuted() != muted); 
    }

    @Test
    public void whenPlayerMinVelUpdated_thenPlayerUpdatedWithNewValues() {
        Player player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.MIN_VELOCITY, 50);
        assertTrue(player.getMinVelocity() == 50); 
    }

    @Test
    public void whenPlayerMaxVelUpdated_thenPlayerUpdatedWithNewValues() {
        Player player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.MAX_VELOCITY, 50);
        assertTrue(player.getMaxVelocity() == 50); 
    }

    @Test
    public void whenPlayerPresetUpdated_thenPlayerUpdatedWithNewValues() {
        Player player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.PRESET, 50);
        assertTrue(player.getPreset() == 50); 
    }

    @Test
    public void whenPlayerNoteUpdated_thenPlayerUpdatedWithNewValues() {
        Player player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.NOTE, 50);
        assertTrue(player.getNote() == 50); 
    }

    @Test
    public void whenPlayerMuted_thenPlayerUpdatedWithNewValues() {
        Player player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.MUTE, 1);
        assertTrue(player.isMuted()); 
    }
    
    @Test
    public void whenPlayerUnmuted_thenPlayerUpdatedWithNewValues() {
        Player player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.MUTE, 0);
        assertFalse(player.isMuted()); 
    }
    

    @Test
    public void whenPlayerProbabilityUpdated_thenPlayerUpdatedWithNewValues() {
        Player player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.PROBABILITY, 50);
        assertTrue(player.getProbability() == 50); 
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
    public void whenNewTickerCalled_thenGetTickerWithNewId() {
        Long id = playerService.getTicker().getId();
        playerService.newTicker();
        assertTrue(playerService.getTicker().getId() > id); 
    }

    @Test
    public void whenLoadTickerCalled_thenGetRequestedTicker() {
        Long id = playerService.getTicker().getId();
        playerService.newTicker();
        assertTrue(playerService.getTicker().getId() > id);
        
        playerService.loadTicker(id);
        assertTrue(playerService.getTicker().getId().equals(id));
    }

    @Test
    public void whenTickerBeatsPerBarUpdated_thenChangeReflectedInTicker() {
        playerService.updateTicker(playerService.getTicker().getId(), TickerUpdateType.BEATS_PER_BAR, 16);
        assertTrue(16 == playerService.getTicker().getBeatsPerBar()); 
    }

    @Test
    public void whenTickerBPMUpdated_thenChangeReflectedInTicker() {
        playerService.updateTicker(playerService.getTicker().getId(), TickerUpdateType.BPM, 16);
        assertTrue(16 == playerService.getTicker().getTempoInBPM()); 
    }
    @Test
    public void whenTickerMaxTracksUpdated_thenChangeReflectedInTicker() {
        playerService.updateTicker(playerService.getTicker().getId(), TickerUpdateType.MAX_TRACKS, 16);
        assertTrue(16 == playerService.getTicker().getMaxTracks()); 
    }
    @Test
    public void whenTickePartLengthUpdated_thenChangeReflectedInTicker() {
        playerService.updateTicker(playerService.getTicker().getId(), TickerUpdateType.PART_LENGTH, 25);
        assertTrue(25 == playerService.getTicker().getPartLength()); 
    }
    @Test
    public void whenTickerPPQUpdated_thenChangeReflectedInTicker() {
        playerService.updateTicker(playerService.getTicker().getId(), TickerUpdateType.PPQ, 16);
        assertTrue(16 == playerService.getTicker().getTicksPerBeat()); 
    }

}
