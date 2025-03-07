// package com.angrysurfer.midi.service.test;

// import static org.junit.Assert.assertSame;
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertTrue;

// import java.util.stream.IntStream;

// import org.junit.jupiter.api.Test;
// import org.junit.runner.RunWith;
// import org.slf4j.Logger;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.TestPropertySource;
// import org.springframework.test.context.junit4.SpringRunner;

// import com.angrysurfer.core.model.Rule;
// import com.angrysurfer.core.model.Session;
// import com.angrysurfer.core.model.player.AbstractPlayer;
// import com.angrysurfer.core.repo.Strikes;
// import com.angrysurfer.core.repo.Sessions;
// import com.angrysurfer.core.util.update.SessionUpdateType;
// import com.angrysurfer.spring.Application;
// import com.angrysurfer.spring.service.PlayerService;
// import com.angrysurfer.spring.service.SessionService;


// @RunWith(SpringRunner.class)
// @SpringBootTest(
//   webEnvironment = SpringBootTest.WebEnvironment.MOCK,
//   classes = Application.class)
// @AutoConfigureMockMvc
// @TestPropertySource(
//   locations = "classpath:application-test.properties")

// public class SessionServiceTests {
//     static Logger logger = org.slf4j.LoggerFactory.getLogger(SessionServiceTests.class.getCanonicalName());

//     @Autowired
//     Strikes playerRepo;

//     @Autowired
//     Sessions sessionRepo;

//     @Autowired
//     SessionService sessionService;

//     @Autowired
//     PlayerService playerService;
    
//     static String RAZ = "razzmatazz";
//     static String ZERO = "zero";

//     // @Before
//     // public void setUp() {
//     //     playerRepo.deleteAll();
//     //     sessionRepo.deleteAll();
//     // }
    
//     // @After
//     // public void tearDown() {
//     //     playerRepo.deleteAll();
//     //     sessionRepo.deleteAll();
//     // }
    
//     @Test
//     public void whenSessionRetrieved_thenItHasBeenSaved() {
//         assertNotNull(sessionService.getSession());
//     }

//     @Test
//     public void whenNewSessionCalled_thenGetSessionWithNewId() {
//         Long id = sessionService.getSession().getId();
//         sessionService.newSession();
//         assertTrue(sessionService.getSession().getId() > id); 
//     }

//     @Test
//     public void whenLoadSessionCalled_thenGetRequestedSession() {
//         Long id = sessionService.getSession().getId();
//         sessionService.newSession();
//         assertTrue(sessionService.getSession().getId() > id);
        
//         sessionService.loadSession(id);
//         assertTrue(sessionService.getSession().getId().equals(id));
//     }

    
//     @Test
//     public void whenNextSessionRequestedWithNoPlayers_thenSameSessionReturned() {

//         Long id = sessionService.getSession().getId();
//         sessionService.getSession().getPlayers().clear();
//         Session nextSession = sessionService.next(id);
//         assertSame(nextSession.getId(), id);
//     }

//     @Test
//     public void whenNextSessionRequestedWithPlayers_thenNewSessionReturned() {
//         playerService.addPlayer(RAZ, 36L);
//         Long id = sessionService.getSession().getId();
//         Session nextSession = sessionService.next(id);
//         assertTrue(nextSession.getId() > id); 
//     }

//     @Test
//     public void whenNextSessionRequestedWithPlayers_thenNextSessionCanBeRequested() {
//         IntStream.range(0, 99).forEach(i -> {
//             playerService.addPlayer(RAZ, 36L);
//             Long id = sessionService.getSession().getId();
//             Session nextSession = sessionService.next(id);
//             assertTrue(nextSession.getId() > id); 
//         });
//     }

//     @Test
//     public void whenPreviousSessionRequestedWith_thenSessionWithLowerIdReturned() {
//         playerService.addPlayer(RAZ, 36L);
//         Long id = sessionService.getSession().getId();
//         Session nextSession = sessionService.next(id);
//         assertTrue(nextSession.getId() > id); 

//         Session prevSession = sessionService.previous(nextSession.getId());
//         assertTrue(prevSession.getId() < nextSession.getId()); 
//     }

//     @Test
//     public void whenPreviousSessionRequestedForTickWithPlayers_thenPlayersContainAddedRule() {
//         Long startingSessionId = sessionService.getSession().getId();
//         AbstractPlayer player = playerService.addPlayer(RAZ, 36L);
//         Rule rule = playerService.addRule(player.getId());

//         // move to next session, add player and rule
//         sessionService.next(startingSessionId);
//         Long nextSessionId = sessionService.getSession().getId();
//         playerService.addRule(playerService.addPlayer(RAZ, 36L).getId());

//         // return to starting session
//         sessionService.previous(nextSessionId);
//         player = sessionService.getSession().getPlayer(player.getId());
//         assertTrue(player.getRules().stream().anyMatch(r -> r.isEqualTo(rule))); 
//     }

//     @Test
//     public void whenNextSessionRequestedForSessionWithPlayers_thenPlayersContainAddedRule() {
//         // sessionService.newSession();
//         // add data to current Session
//         Long startingSessionId = sessionService.getSession().getId();
//         AbstractPlayer player = playerService.addPlayer(RAZ, 36L);
//         playerService.addRule(player.getId());

//         // move to next session, add player and rule
//         Session session2 = sessionService.next(startingSessionId);
//         AbstractPlayer player2 = playerService.addPlayer(RAZ, 36L);
//         Rule rule = playerService.addRule(player2.getId());

//         // return to starting session
//         Session previous = sessionService.previous(session2.getId());
//         Long prevId = previous.getId();

//         assertEquals(startingSessionId, prevId);

//         // advance again
//         sessionService.next(sessionService.getSession().getId());

//         AbstractPlayer player3 = sessionService.getSession().getPlayer(player2.getId());
//         Rule rule2 = player3.getRule(rule.getId());
//         assertTrue(rule.isEqualTo(rule2)); 
//     }

//     @Test
//     public void whenNextSessionRequestedForSessionWithPlayers_thenPlayerDoesNotContainRemovedRule() {
//         // sessionService.newSession();
//         // add data to current Session
//         Long startingSessionId = sessionService.getSession().getId();
//         AbstractPlayer player = playerService.addPlayer(RAZ, 36L);
//         playerService.addRule(player.getId());

//         // move to next session, add player and rule
//         Session session2 = sessionService.next(startingSessionId);
//         AbstractPlayer player2 = playerService.addPlayer(RAZ, 36L);
//         Rule rule = playerService.addRule(player2.getId());

//         //remove rule
//         playerService.removeRule(player2.getId(), rule.getId());
//         // return to starting session
//         assertEquals(startingSessionId, sessionService.previous(session2.getId()).getId());

//         // advance again
//         sessionService.next(sessionService.getSession().getId());

//         AbstractPlayer player3 = sessionService.getSession().getPlayer(player2.getId());
//         // Rule rule2 = player3.getRule(rule.getId());
//         assertTrue(player3.getRules().size() == 0); 
//     }

//     @Test
//     public void whenSessionBeatsPerBarUpdated_thenChangeReflectedInSession() {
//         sessionService.updateSession(sessionService.getSession().getId(), SessionUpdateType.BEATS_PER_BAR, 16);
//         assertTrue(16 == sessionService.getSession().getBeatsPerBar()); 
//     }

//     @Test
//     public void whenSessionBPMUpdated_thenChangeReflectedInSession() {
//         sessionService.updateSession(sessionService.getSession().getId(), SessionUpdateType.BPM, 16);
//         assertTrue(16 == sessionService.getSession().getTempoInBPM()); 
//     }
//     @Test
//     public void whenSessionMaxTracksUpdated_thenChangeReflectedInSession() {
//         sessionService.updateSession(sessionService.getSession().getId(), SessionUpdateType.MAX_TRACKS, 16);
//         assertTrue(16 == sessionService.getSession().getMaxTracks()); 
//     }
//     @Test
//     public void whenTickePartLengthUpdated_thenChangeReflectedInSession() {
//         sessionService.updateSession(sessionService.getSession().getId(), SessionUpdateType.PART_LENGTH, 25);
//         assertTrue(25 == sessionService.getSession().getPartLength()); 
//     }
//     @Test
//     public void whenSessionPPQUpdated_thenChangeReflectedInSession() {
//         sessionService.updateSession(sessionService.getSession().getId(), SessionUpdateType.PPQ, 16);
//         assertTrue(16 == sessionService.getSession().getTicksPerBeat()); 
//     }

// }
