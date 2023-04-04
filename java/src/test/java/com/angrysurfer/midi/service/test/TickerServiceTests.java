package com.angrysurfer.midi.service.test;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.angrysurfer.BeatGeneratorApplication;
import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.Ticker;
import com.angrysurfer.midi.repo.StrikeRepository;
import com.angrysurfer.midi.repo.TickerRepo;
import com.angrysurfer.midi.service.PlayerService;
import com.angrysurfer.midi.service.TickerService;
import com.angrysurfer.midi.util.TickerUpdateType;


@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = BeatGeneratorApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
  locations = "classpath:application-test.properties")

public class TickerServiceTests {
    static Logger logger = org.slf4j.LoggerFactory.getLogger(TickerServiceTests.class.getCanonicalName());

    @Autowired
    StrikeRepository playerRepository;

    @Autowired
    TickerRepo tickerRepo;

    @Autowired
    TickerService tickerService;

    @Autowired
    PlayerService playerService;
    
    static String RAZ = "razzmatazz";
    static String ZERO = "zero";

    // @Before
    // public void setUp() {
    //     playerRepository.deleteAll();
    //     tickerRepo.deleteAll();
    // }
    
    // @After
    // public void tearDown() {
    //     playerRepository.deleteAll();
    //     tickerRepo.deleteAll();
    // }
    
    @Test
    public void whenTickerRetrieved_thenItHasBeenSaved() {
        assertNotNull(tickerService.getTicker());
    }

    @Test
    public void whenNewTickerCalled_thenGetTickerWithNewId() {
        Long id = tickerService.getTicker().getId();
        tickerService.newTicker();
        assertTrue(tickerService.getTicker().getId() > id); 
    }

    @Test
    public void whenLoadTickerCalled_thenGetRequestedTicker() {
        Long id = tickerService.getTicker().getId();
        tickerService.newTicker();
        assertTrue(tickerService.getTicker().getId() > id);
        
        tickerService.loadTicker(id);
        assertTrue(tickerService.getTicker().getId().equals(id));
    }

    
    @Test
    public void whenNextTickerRequestedWithNoPlayers_thenSameTickerReturned() {

        Long id = tickerService.getTicker().getId();
        tickerService.getTicker().getPlayers().clear();
        Ticker nextTicker = tickerService.next(id);
        assertSame(nextTicker.getId(), id);
    }

    @Test
    public void whenNextTickerRequestedWithPlayers_thenNewTickerReturned() {
        playerService.addPlayer(RAZ);
        Long id = tickerService.getTicker().getId();
        Ticker nextTicker = tickerService.next(id);
        assertTrue(nextTicker.getId() > id); 
    }

    @Test
    public void whenNextTickerRequestedWithPlayers_thenNextTickerCanBeRequested() {
        IntStream.range(0, 99).forEach(i -> {
            playerService.addPlayer(RAZ);
            Long id = tickerService.getTicker().getId();
            Ticker nextTicker = tickerService.next(id);
            assertTrue(nextTicker.getId() > id); 
        });
    }

    @Test
    public void whenPreviousTickerRequestedWith_thenTickerWithLowerIdReturned() {
        playerService.addPlayer(RAZ);
        Long id = tickerService.getTicker().getId();
        Ticker nextTicker = tickerService.next(id);
        assertTrue(nextTicker.getId() > id); 

        Ticker prevTicker = tickerService.previous(nextTicker.getId());
        assertTrue(prevTicker.getId() < nextTicker.getId()); 
    }

    @Test
    public void whenPreviousTickerRequestedForTickWithPlayers_thenPlayersContainAddedRule() {
        Long startingTickerId = tickerService.getTicker().getId();
        Player player = playerService.addPlayer(RAZ);
        Rule rule = playerService.addRule(player.getId());

        // move to next ticker, add player and rule
        tickerService.next(startingTickerId);
        Long nextTickerId = tickerService.getTicker().getId();
        playerService.addRule(playerService.addPlayer(RAZ).getId());

        // return to starting ticker
        tickerService.previous(nextTickerId);
        player = tickerService.getTicker().getPlayer(player.getId());
        assertTrue(player.getRules().stream().anyMatch(r -> r.isEqualTo(rule))); 
    }

    @Test
    public void whenNextTickerRequestedForTickerWithPlayers_thenPlayersContainAddedRule() {
        // tickerService.newTicker();
        // add data to current Ticker
        Long startingTickerId = tickerService.getTicker().getId();
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        // move to next ticker, add player and rule
        Ticker ticker2 = tickerService.next(startingTickerId);
        Player player2 = playerService.addPlayer(RAZ);
        Rule rule = playerService.addRule(player2.getId());

        // return to starting ticker
        Ticker previous = tickerService.previous(ticker2.getId());
        Long prevId = previous.getId();

        assertEquals(startingTickerId, prevId);

        // advance again
        tickerService.next(tickerService.getTicker().getId());

        Player player3 = tickerService.getTicker().getPlayer(player2.getId());
        Rule rule2 = player3.getRule(rule.getId());
        assertTrue(rule.isEqualTo(rule2)); 
    }

    @Test
    public void whenNextTickerRequestedForTickerWithPlayers_thenPlayerDoesNotContainRemovedRule() {
        // tickerService.newTicker();
        // add data to current Ticker
        Long startingTickerId = tickerService.getTicker().getId();
        Player player = playerService.addPlayer(RAZ);
        playerService.addRule(player.getId());

        // move to next ticker, add player and rule
        Ticker ticker2 = tickerService.next(startingTickerId);
        Player player2 = playerService.addPlayer(RAZ);
        Rule rule = playerService.addRule(player2.getId());

        //remove rule
        playerService.removeRule(player2.getId(), rule.getId());
        // return to starting ticker
        assertEquals(startingTickerId, tickerService.previous(ticker2.getId()).getId());

        // advance again
        tickerService.next(tickerService.getTicker().getId());

        Player player3 = tickerService.getTicker().getPlayer(player2.getId());
        // Rule rule2 = player3.getRule(rule.getId());
        assertTrue(player3.getRules().size() == 0); 
    }

    @Test
    public void whenTickerBeatsPerBarUpdated_thenChangeReflectedInTicker() {
        tickerService.updateTicker(tickerService.getTicker().getId(), TickerUpdateType.BEATS_PER_BAR, 16);
        assertTrue(16 == tickerService.getTicker().getBeatsPerBar()); 
    }

    @Test
    public void whenTickerBPMUpdated_thenChangeReflectedInTicker() {
        tickerService.updateTicker(tickerService.getTicker().getId(), TickerUpdateType.BPM, 16);
        assertTrue(16 == tickerService.getTicker().getTempoInBPM()); 
    }
    @Test
    public void whenTickerMaxTracksUpdated_thenChangeReflectedInTicker() {
        tickerService.updateTicker(tickerService.getTicker().getId(), TickerUpdateType.MAX_TRACKS, 16);
        assertTrue(16 == tickerService.getTicker().getMaxTracks()); 
    }
    @Test
    public void whenTickePartLengthUpdated_thenChangeReflectedInTicker() {
        tickerService.updateTicker(tickerService.getTicker().getId(), TickerUpdateType.PART_LENGTH, 25);
        assertTrue(25 == tickerService.getTicker().getPartLength()); 
    }
    @Test
    public void whenTickerPPQUpdated_thenChangeReflectedInTicker() {
        tickerService.updateTicker(tickerService.getTicker().getId(), TickerUpdateType.PPQ, 16);
        assertTrue(16 == tickerService.getTicker().getTicksPerBeat()); 
    }

}
