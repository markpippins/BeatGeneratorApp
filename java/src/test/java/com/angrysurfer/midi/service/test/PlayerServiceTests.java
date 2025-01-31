package com.angrysurfer.midi.service.test;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import com.angrysurfer.sequencer.model.Rule;
import com.angrysurfer.sequencer.model.player.AbstractPlayer;
import com.angrysurfer.sequencer.repo.*;
import com.angrysurfer.sequencer.service.MIDIService;
import com.angrysurfer.sequencer.service.PlayerService;
import com.angrysurfer.sequencer.service.TickerService;
import com.angrysurfer.sequencer.util.Comparison;
import com.angrysurfer.sequencer.util.update.PlayerUpdateType;
import com.angrysurfer.sequencer.util.update.RuleUpdateType;

@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = Application.class)
@AutoConfigureMockMvc
@TestPropertySource(
  locations = "classpath:application-test.properties")
public class PlayerServiceTests {

    static Logger logger = LoggerFactory.getLogger(PlayerServiceTests.class.getCanonicalName());

    @Autowired
    PlayerService playerService;

    @Autowired
    TickerService tickerService;

    @Autowired
    MIDIService midiService;

    @Autowired
    Strikes playerRepo;

    @Autowired
    Rules ruleRepo;

    @Autowired
    Tickers tickerRepo;

    @Autowired
    Instruments midiInstrumentRepo;

    @Autowired
    ControlCodes controlCodeRepo;

    @Autowired
    Pads padRepo;

    @Autowired
    Steps stepRepo;

    @Autowired
    Songs songRepo;

    static String RAZ = "razzmatazz";
    static String ZERO = "zero";

    Long razId;

    @Before
    public void setUp() {
        playerRepo.deleteAll();
    }
    
    @Before
    public void tearDown() {
        playerRepo.deleteAll();
    }

    @Test
    public void whenPlayerAdded_thenTickerShouldNotBeNull() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        assertNotNull(player.getTicker());
    }
    
    @Test
    public void whenMultipleRulesAdded_thenPlayerShouldContainThem() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        Rule r1 = playerService.addRule(player.getId());
        r1.setComparison(Comparison.EQUALS);

        Rule r2 = playerService.addRule(player.getId());
        r2.setComparison(Comparison.GREATER_THAN);
        
        Rule r3 = playerService.addRule(player.getId());
        r3.setComparison(Comparison.LESS_THAN);
        
        AbstractPlayer tickerPlayer = tickerService.getTicker().getPlayer(player.getId());
        assertEquals(3, tickerPlayer.getRules().size());
    }

    @Test
    public void whenRuleAddTried_thenEnsureThatExistingRulesHaveBeenInitialized() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());
        playerService.addRule(player.getId());
        playerService.addRule(player.getId());
        playerService.addRule(player.getId());
        playerService.addRule(player.getId());

        assertEquals(2, player.getRules().size());
    }

    @Test
    public void whenRuleAdded_thenPlayerShouldContainIt() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());
        assertEquals(1, player.getRules().size());
    }

    @Test
    public void whenRuleRemoved_thenPlayerShouldNotContainIt() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        Rule rule = playerService.addRule(player.getId());
        assertEquals(1, player.getRules().size());

        playerService.removeRule(player.getId(), rule.getId());
        assertEquals(0, player.getRules().size());
    }

    @Test
    public void whenRuleValueUpdated_thenRuleShouldReflectIt() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        assertTrue(player.getRules().size() > 0);
        Rule rule = player.getRules().stream().toList().get(0);
        double value = rule.getValue() + 1;

        playerService.updateRule(rule.getId(), RuleUpdateType.VALUE, (long) value);

        assertEquals(player.getRules().stream().toList().get(0).getValue(), value, 0.0); 
    }

    @Test
    public void whenRuleOperatorUpdated_thenRuleShouldReflectIt() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        assertTrue(player.getRules().size() > 0);
        Rule rule = player.getRules().stream().toList().get(0);
        int value = rule.getOperator() + 1;

        playerService.updateRule(rule.getId(), RuleUpdateType.OPERATOR, value);

        assertTrue(player.getRules().stream().toList().get(0).getOperator().equals(value));
    }

    @Test
    public void whenRulePartUpdated_thenRuleShouldReflectIt() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        assertTrue(player.getRules().size() > 0);
        Rule rule = player.getRules().stream().toList().get(0);
        int value = rule.getPart() + 1;

        playerService.updateRule(rule.getId(), RuleUpdateType.PART, value);
        assertTrue(player.getRule(rule.getId()).getPart().equals(value));
    }

    @Test
    public void whenRuleComparisonUpdated_thenRuleShouldReflectIt() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        assertTrue(player.getRules().size() > 0);
        Rule rule = player.getRules().stream().toList().get(0);
        int value = rule.getComparison() + 1;

        playerService.updateRule(rule.getId(), RuleUpdateType.COMPARISON, value);
        assertTrue(player.getRules().stream().toList().get(0).getComparison().equals(value));
    }

    @Test
    public void whenPlayerMuted_thenMutedPlayer() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        boolean muted = player.isMuted();
        playerService.mutePlayer(player.getId());
        assertTrue(player.isMuted() != muted); 
    }

    @Test
    public void whenPlayerMinVelUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.MIN_VELOCITY, 50);
        assertTrue(player.getMinVelocity() == 50); 
    }

    @Test
    public void whenPlayerMaxVelUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.MAX_VELOCITY, 50);
        assertTrue(player.getMaxVelocity() == 50); 
    }

    @Test
    public void whenPlayerPresetUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.PRESET, 50);
        assertTrue(player.getPreset() == 50); 
    }

    @Test
    public void whenPlayerFadeInUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.FADE_IN, 50);
        assertTrue(player.getFadeIn() == 50); 
    }

    @Test
    public void whenPlayerFadeOutUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.FADE_OUT, 50);
        assertTrue(player.getFadeOut() == 50); 
    }

    @Test
    public void whenPlayerBeatFractionUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.BEAT_FRACTION, 50);
        assertTrue(player.getBeatFraction() == 50); 
    }

    @Test
    public void whenRandomDegreeUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.RANDOM_DEGREE, 50);
        assertTrue(player.getRandomDegree() == 50); 
    }

    @Test
    public void whenPlayerRatchetCountUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.RATCHET_COUNT, 50);
        assertTrue(player.getRatchetCount() == 50); 
    }

    @Test
    public void whenPlayerRatchetIntervalUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.RATCHET_INTERVAL, 50);
        assertTrue(player.getRatchetInterval() == 50); 
    }

    @Test
    public void whenPlayerNoteUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.NOTE, 50L);
        assertTrue(50L == player.getNote()); 
    }

    @Test
    public void whenPlayerMuted_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.MUTE, 1);
        assertTrue(player.isMuted()); 
    }
    
    @Test
    public void whenPlayerUnmuted_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.MUTE, 0);
        assertFalse(player.isMuted()); 
    }

    @Test
    public void whenPlayerProbabilityUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.PROBABILITY, 50);
        assertTrue(player.getProbability() == 50); 
    }

    @Test
    public void whenPlayerLevelUpdated_thenPlayerUpdatedWithNewValues() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.updatePlayer(player.getId(), PlayerUpdateType.LEVEL, 50);
        assertTrue(player.getLevel() == 50); 
    }

    @Test
    public void whenPlayerAdded_thenTickerContainsPlayer() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        assertTrue(tickerService.getTicker().getPlayers().contains(player)); 
    }

    @Test
    public void whenPlayerRemoved_thenTickerNoLongerContainsPlayer() {
        AbstractPlayer player = playerService.addPlayer(RAZ);
        playerService.removePlayer(player.getId());
        assertTrue(!tickerService.getTicker().getPlayers().contains(player)); 
    }

    @Test
    public void whenPlayersAdded_thenTickerContainsPlayers() {
        tickerService.getTicker().getPlayers().clear();
        playerService.addPlayer(RAZ);
        playerService.addPlayer(RAZ);
        playerService.addPlayer(RAZ);
        assertTrue(tickerService.getTicker().getPlayers().size() > 0); 
    }

    // @Test
    // public void whenPlayersCleared_thenTickerNoLongerContainsPlayers() {
    //     playerService.addPlayer(RAZ);
    //     playerService.addPlayer(RAZ);
    //     playerService.addPlayer(RAZ);
    //     playerService.clearPlayers();
    //     assertTrue(tickerService.getTicker().getPlayers().size() == 0); 
    // }

}
