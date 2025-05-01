package com.angrysurfer.core.redis;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.sequencer.DrumSequenceData;
import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RedisServiceTest {

    @Mock
    private JedisPool jedisPool;
    
    @Mock
    private Jedis jedis;
    
    @Mock
    private CommandBus commandBus;
    
    // Helper mocks
    @Mock
    private SessionHelper sessionHelper;
    
    @Mock
    private PlayerHelper playerHelper;
    
    @Mock
    private DrumSequenceHelper drumSequenceHelper;
    
    @Mock
    private MelodicSequencerHelper melodicSequencerHelper;
    
    // Class to test - we need a custom subclass to inject mocks
    private TestableRedisService redisService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Configure jedisPool to return our mock jedis
        when(jedisPool.getResource()).thenReturn(jedis);
        
        // Create testable version with injected mocks
        redisService = new TestableRedisService(jedisPool);
        redisService.setSessionHelper(sessionHelper);
        redisService.setPlayerHelper(playerHelper);
        redisService.setDrumSequenceHelper(drumSequenceHelper);
        redisService.setMelodicSequencerHelper(melodicSequencerHelper);
    }
    
    @Test
    void testDatabaseEmptyCheck() {
        // Arrange
        when(jedis.keys("*")).thenReturn(Collections.emptySet());
        
        // Act
        boolean isEmpty = redisService.isDatabaseEmpty();
        
        // Assert
        assertTrue(isEmpty);
        verify(jedis).keys("*");
    }
    
    @Test
    void testClearDatabase() {
        // Act
        redisService.clearDatabase();
        
        // Assert
        verify(jedis).flushDB();
    }
    
    @Test
    void testLoadTableState_WhenExists() throws Exception {
        // Arrange
        String jsonState = "{\"columnOrder\":[\"col1\",\"col2\"],\"sortColumn\":\"col1\",\"sortAscending\":true}";
        when(jedis.get("tablestate-testTable")).thenReturn(jsonState);
        
        // Act
        TableState state = redisService.loadTableState("testTable");
        
        // Assert
        assertNotNull(state);
        assertEquals(2, state.getColumnOrder().size());
        assertEquals("col1", state.getColumnOrder().get(0));
        assertEquals("col2", state.getColumnOrder().get(1));
    }
    
    @Test
    void testLoadFrameState_WhenExists() throws Exception {
        // Arrange
        String jsonState = "{\"x\":100,\"y\":200,\"width\":800,\"height\":600,\"columnOrder\":[\"col1\",\"col2\"]}";
        when(jedis.get("framestate-testWindow")).thenReturn(jsonState);
        
        // Act
        FrameState state = redisService.loadFrameState("testWindow");
        
        // Assert
        assertNotNull(state);
        assertEquals(100, state.getX());
        assertEquals(200, state.getY());
        assertEquals(800, state.getWidth());
        assertEquals(600, state.getHeight());
    }
    
    // A testable subclass that allows us to inject mocks
    private static class TestableRedisService extends RedisService {
        private SessionHelper sessionHelper;
        private PlayerHelper playerHelper;
        private DrumSequenceHelper drumSequenceHelper;
        private MelodicSequencerHelper melodicSequencerHelper;
        
        public TestableRedisService(JedisPool jedisPool) {
            super(jedisPool, new ObjectMapper());
        }
        
        public void setSessionHelper(SessionHelper sessionHelper) {
            this.sessionHelper = sessionHelper;
        }
        
        public void setPlayerHelper(PlayerHelper playerHelper) {
            this.playerHelper = playerHelper;
        }
        
        public void setDrumSequenceHelper(DrumSequenceHelper helper) {
            this.drumSequenceHelper = helper;
        }
        
        public void setMelodicSequencerHelper(MelodicSequencerHelper helper) {
            this.melodicSequencerHelper = helper;
        }
        
        // Override methods to use our mocks
        @Override
        public SessionHelper getSessionHelper() {
            return sessionHelper;
        }
        
        @Override
        public PlayerHelper getPlayerHelper() {
            return playerHelper;
        }
        
        @Override
        public DrumSequenceHelper getDrumSequenceHelper() {
            return drumSequenceHelper;
        }
        
        @Override
        public MelodicSequencerHelper getMelodicSequencerHelper() {
            return melodicSequencerHelper;
        }
    }
}