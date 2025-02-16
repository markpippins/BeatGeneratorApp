// package com.angrysurfer.beatsui;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// import java.util.HashSet;
// import java.util.List;
// import java.util.Set;

// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import com.angrysurfer.beats.config.UserConfig;
// import com.angrysurfer.beats.data.RedisService;
// import com.angrysurfer.core.proxy.ProxyCaption;
// import com.angrysurfer.core.proxy.ProxyControlCode;
// import com.angrysurfer.core.proxy.ProxyInstrument;
// import com.angrysurfer.core.proxy.ProxyRule;
// import com.angrysurfer.core.proxy.ProxyStrike;
// import com.angrysurfer.core.proxy.ProxyTicker;

// import redis.clients.jedis.Jedis;
// import redis.clients.jedis.JedisPool;

// @ExtendWith(MockitoExtension.class)
// class RedisServiceTest {

//     private RedisService redisService;
    
//     @Mock
//     private JedisPool jedisPool;
    
//     @Mock
//     private Jedis jedis;

//     @BeforeEach
//     void setUp() {
//         // Setup the mock behavior
//         when(jedisPool.getResource()).thenReturn(jedis);
//         redisService = new RedisService(jedisPool);
//     }

//     @AfterEach
//     void tearDown() {
//         redisService.clearDatabase();
//     }

//     @Nested
//     @DisplayName("Configuration Tests")
//     class ConfigurationTests {
        
//         @BeforeEach
//         void setUp() {
//             // Add stubbing only in tests that need it
//             when(jedisPool.getResource()).thenReturn(jedis);
//         }

//         @Test
//         @DisplayName("Should load config from XML successfully")
//         void loadConfigFromXml() {
//             // Add necessary mock behavior
//             when(jedis.get(anyString())).thenReturn(null);
            
//             String xmlPath = "src/test/resources/test-config.xml";
//             UserConfig config = redisService.loadConfigFromXml(xmlPath);
//             assertNotNull(config);
//             assertNotNull(config.getInstruments());
//             assertFalse(config.getInstruments().isEmpty());
//         }

//         @Test
//         @DisplayName("Should save and load config")
//         void saveAndLoadConfig() {
//             UserConfig config = new UserConfig();
//             // Setup test data
//             ProxyInstrument instrument = new ProxyInstrument();
//             instrument.setName("Test Instrument");
//             config.setInstruments(List.of(instrument));

//             redisService.saveConfig(config);
//             UserConfig loadedConfig = redisService.getConfig();

//             assertNotNull(loadedConfig);
//             assertEquals(1, loadedConfig.getInstruments().size());
//             assertEquals("Test Instrument", loadedConfig.getInstruments().get(0).getName());
//         }
//     }

//     @Nested
//     @DisplayName("Instrument Tests")
//     class InstrumentTests {
        
//         @Test
//         @DisplayName("Should save and find instrument")
//         void saveAndFindInstrument() {
//             ProxyInstrument instrument = new ProxyInstrument();
//             instrument.setName("Test Instrument");
            
//             redisService.saveInstrument(instrument);
            
//             List<ProxyInstrument> instruments = redisService.findAllInstruments();
//             assertFalse(instruments.isEmpty());
//             assertEquals("Test Instrument", instruments.get(0).getName());
//         }
//     }

//     @Nested
//     @DisplayName("Control Code Tests")
//     class ControlCodeTests {
        
//         @Test
//         @DisplayName("Should save and find control code with captions")
//         void saveAndFindControlCode() {
//             ProxyControlCode cc = new ProxyControlCode();
//             cc.setName("Test CC");
//             cc.setCode(1);
            
//             ProxyCaption caption = new ProxyCaption();
//             caption.setDescription("Test Caption");
//             cc.setCaptions(Set.of(caption));
            
//             redisService.saveControlCode(cc);
            
//             List<ProxyControlCode> controlCodes = redisService.findAllControlCodes();
//             assertFalse(controlCodes.isEmpty());
//             assertEquals("Test CC", controlCodes.get(0).getName());
//             assertFalse(controlCodes.get(0).getCaptions().isEmpty());
//         }
//     }

//     @Nested
//     @DisplayName("Rule Tests")
//     class RuleTests {
        
//         @Test
//         @DisplayName("Should save and find rules for player")
//         void saveAndFindRulesForPlayer() {
//             ProxyStrike player = new ProxyStrike();
//             player.setName("Test Player");
//             player = redisService.saveStrike(player);
            
//             ProxyRule rule = new ProxyRule();
//             rule.setPlayerId(player.getId());
            
//             redisService.saveRule(rule, player);
            
//             List<ProxyRule> rules = redisService.findRulesByPlayer(player);
//             assertFalse(rules.isEmpty());
//             assertEquals(player.getId(), rules.get(0).getPlayerId());
//         }
//     }

//     @Nested
//     @DisplayName("Ticker Tests")
//     class TickerTests {
        
//         @Test
//         @DisplayName("Should save and load ticker")
//         void saveAndLoadTicker() {
//             // Arrange
//             ProxyTicker ticker = new ProxyTicker();
//             ticker.setTempoInBPM(120.0f);
            
//             // Mock behavior for save
//             when(jedis.set(anyString(), anyString())).thenReturn("OK");
            
//             // Mock behavior for load
//             when(jedis.get("ticker")).thenReturn("{\"tempoInBPM\":120.0}");
            
//             // Act
//             ProxyTicker savedTicker = redisService.saveTicker(ticker);
//             ProxyTicker loadedTicker = redisService.loadTicker();
            
//             // Assert
//             assertNotNull(savedTicker);
//             assertNotNull(loadedTicker);
//             assertEquals(120.0f, loadedTicker.getTempoInBPM());
            
//             // Verify interactions
//             verify(jedis).set(eq("ticker"), anyString());
//             verify(jedis).get("ticker");
//         }
//     }

//     @Nested
//     @DisplayName("Database Management Tests")
//     class DatabaseManagementTests {
        
//         @Test
//         @DisplayName("Should clear database")
//         void clearDatabase() {
//             // Act
//             redisService.clearDatabase();
            
//             // Verify
//             verify(jedis).flushAll();
//         }

//         @Test
//         @DisplayName("Should check if database is empty")
//         void isDatabaseEmpty() {
//             // Arrange
//             when(jedis.keys("*")).thenReturn(new HashSet<>());
            
//             // Act & Assert
//             assertTrue(redisService.isDatabaseEmpty());
//             verify(jedis).keys("*");
//         }
//     }
// }