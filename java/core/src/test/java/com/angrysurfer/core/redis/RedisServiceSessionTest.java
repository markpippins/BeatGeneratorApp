// package com.angrysurfer.core.redis;

// import com.angrysurfer.core.model.Session;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.util.Arrays;
// import java.util.List;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// public class RedisServiceSessionTest extends RedisServiceTest {

//     @Mock
//     private Session mockSession;
    
//     @BeforeEach
//     void setUpSessionTests() {
//         // Additional setup for session tests
//         when(mockSession.getId()).thenReturn(1L);
//     }
    
//     @Test
//     void testFindSessionById() {
//         // Arrange
//         Long sessionId = 1L;
//         when(getSessionHelper().findSessionById(sessionId)).thenReturn(mockSession);
        
//         // Act
//         Session result = getRedisService().findSessionById(sessionId);
        
//         // Assert
//         assertNotNull(result);
//         assertEquals(sessionId, result.getId());
//         verify(getSessionHelper()).findSessionById(sessionId);
//     }
    
//     @Test
//     void testSaveSession() {
//         // Act
//         getRedisService().saveSession(mockSession);
        
//         // Assert
//         verify(getSessionHelper()).saveSession(mockSession);
//     }
    
//     @Test
//     void testGetAllSessionIds() {
//         // Arrange
//         List<Long> expectedIds = Arrays.asList(1L, 2L, 3L);
//         when(getSessionHelper().getAllSessionIds()).thenReturn(expectedIds);
        
//         // Act
//         List<Long> result = getRedisService().getAllSessionIds();
        
//         // Assert
//         assertEquals(expectedIds, result);
//         verify(getSessionHelper()).getAllSessionIds();
//     }
    
//     @Test
//     void testGetNextSessionId() {
//         // Arrange
//         Long currentId = 1L;
//         Long expectedNextId = 2L;
//         List<Long> allIds = Arrays.asList(1L, 2L, 3L);
        
//         when(mockSession.getId()).thenReturn(currentId);
//         when(getSessionHelper().getAllSessionIds()).thenReturn(allIds);
        
//         // Act
//         Long result = getRedisService().getNextSessionId(mockSession);
        
//         // Assert
//         assertEquals(expectedNextId, result);
//     }
    
//     @Test
//     void testGetPreviousSessionId() {
//         // Arrange
//         Long currentId = 2L;
//         Long expectedPrevId = 1L;
//         List<Long> allIds = Arrays.asList(1L, 2L, 3L);
        
//         when(mockSession.getId()).thenReturn(currentId);
//         when(getSessionHelper().getAllSessionIds()).thenReturn(allIds);
        
//         // Act
//         Long result = getRedisService().getPreviousSessionId(mockSession);
        
//         // Assert
//         assertEquals(expectedPrevId, result);
//     }
    
//     @Test
//     void testNewSession() {
//         // Arrange
//         when(getSessionHelper().newSession()).thenReturn(mockSession);
        
//         // Act
//         Session result = getRedisService().newSession();
        
//         // Assert
//         assertNotNull(result);
//         verify(getSessionHelper()).newSession();
//     }
// }