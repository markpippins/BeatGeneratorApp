// package com.angrysurfer.core.redis;

// import com.angrysurfer.core.model.InstrumentWrapper;
// import com.angrysurfer.core.model.Player;
// import com.angrysurfer.core.model.Session;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.util.HashSet;
// import java.util.Set;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// public class RedisServicePlayerTest extends RedisServiceTest {

//     @Mock
//     private Player mockPlayer;
    
//     @Mock
//     private Session mockSession;
    
//     @Mock
//     private InstrumentWrapper mockInstrument;
    
//     @BeforeEach
//     void setUpPlayerTests() {
//         // Additional setup for player tests
//         when(mockPlayer.getId()).thenReturn(1L);
//         when(mockPlayer.getName()).thenReturn("Test Player");
//     }
    
//     @Test
//     void testFindPlayerById() {
//         // Arrange
//         Long playerId = 1L;
//         when(getPlayerHelper().findPlayerById(eq(playerId), anyString())).thenReturn(mockPlayer);
        
//         // Act
//         Player result = getRedisService().findPlayerById(playerId);
        
//         // Assert
//         assertNotNull(result);
//         assertEquals(playerId, result.getId());
//     }
    
//     @Test
//     void testSavePlayer() {
//         // Arrange
//         when(mockPlayer.getInstrument()).thenReturn(mockInstrument);
//         when(mockInstrument.getId()).thenReturn(1L);
        
//         // Act
//         getRedisService().savePlayer(mockPlayer);
        
//         // Assert
//         verify(getInstrumentHelper()).saveInstrument(mockInstrument);
//         verify(getPlayerHelper()).savePlayer(mockPlayer);
//     }
    
//     @Test
//     void testSavePlayerWithSession() {
//         // Arrange
//         when(mockPlayer.getInstrument()).thenReturn(mockInstrument);
//         when(mockInstrument.getId()).thenReturn(1L);
        
//         Set<Player> players = new HashSet<>();
//         players.add(mockPlayer);
        
//         when(mockSession.getPlayers()).thenReturn(players);
//         when(getSessionHelper().findSessionForPlayer(mockPlayer)).thenReturn(mockSession);
        
//         // Act
//         getRedisService().savePlayer(mockPlayer);
        
//         // Assert
//         verify(getInstrumentHelper()).saveInstrument(mockInstrument);
//         verify(getPlayerHelper()).savePlayer(mockPlayer);
//         verify(getSessionHelper()).saveSession(mockSession);
//     }
    
//     @Test
//     void testFindPlayersForSession() {
//         // Arrange
//         Long sessionId = 1L;
//         Set<Player> expectedPlayers = new HashSet<>();
//         expectedPlayers.add(mockPlayer);
        
//         when(getPlayerHelper().findPlayersForSession(eq(sessionId), anyString())).thenReturn(expectedPlayers);
        
//         // Act
//         Set<Player> result = getRedisService().findPlayersForSession(sessionId);
        
//         // Assert
//         assertEquals(expectedPlayers, result);
//     }
    
//     @Test
//     void testDeletePlayer() {
//         // Act
//         getRedisService().deletePlayer(mockPlayer);
        
//         // Assert
//         verify(getPlayerHelper()).deletePlayer(mockPlayer);
//     }
// }