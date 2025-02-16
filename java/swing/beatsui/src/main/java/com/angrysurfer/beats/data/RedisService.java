package com.angrysurfer.beats.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.angrysurfer.beats.config.UserConfig;
import com.angrysurfer.core.proxy.ProxyCaption;
import com.angrysurfer.core.proxy.ProxyControlCode;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.proxy.ProxyPad;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisService {
    private static final Logger logger = Logger.getLogger(RedisService.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private static final String TICKER_KEY = "ticker";

    public RedisService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    // Keep the no-args constructor for non-test usage
    public RedisService() {
        this(RedisConfig.getJedisPool());
    }

    private String getKey(Class<?> cls, Long id) {
        String key = cls.getSimpleName().toLowerCase() + ":" + id;
        logger.fine("Generated key: " + key + " for class: " + cls.getSimpleName() + " and ID: " + id);
        return key;
    }

    private String getCollectionKey(Class<?> cls) {
        String key = cls.getSimpleName().toLowerCase();
        logger.fine("Generated collection key: " + key + " for class: " + cls.getSimpleName());
        return key;
    }

    public UserConfig loadConfigFromXml(String xmlFilePath) {
        try {
            // Clear existing data before loading config
            clearDatabase();

            // Create mapper configured for our needs
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // Read JSON config file
            String jsonPath = xmlFilePath.replace(".xml", ".json");
            File jsonFile = new File(jsonPath);
            JsonNode rootNode = mapper.readTree(jsonFile);
            JsonNode instrumentsNode = rootNode.get("instruments");

            // Clear existing data
            clearDatabase();

            List<ProxyInstrument> instruments = new ArrayList<>();

            // Process each instrument
            for (JsonNode instrumentNode : instrumentsNode) {
                ProxyInstrument instrument = new ProxyInstrument();

                // Set basic properties
                instrument.setName(instrumentNode.get("name").asText());
                instrument.setDeviceName(instrumentNode.get("deviceName").asText());
                instrument.setLowestNote(instrumentNode.get("lowestNote").asInt());
                instrument.setHighestNote(instrumentNode.get("highestNote").asInt());
                instrument.setAvailable(instrumentNode.get("available").asBoolean());
                instrument.setInitialized(instrumentNode.get("initialized").asBoolean());
                // instrument.setAssignmentCount(instrumentNode.get("assignmentCount").asInt());
                // instrument.setMultiTimbral(instrumentNode.get("multiTimbral").asBoolean());
                // instrument.setDefaultChannel(instrumentNode.get("defaultChannel").asInt());

                // Process channels
                JsonNode channelsNode = instrumentNode.get("channels");
                if (channelsNode != null && !channelsNode.isEmpty()) {
                    List<Integer> channels = new ArrayList<>();
                    for (JsonNode channel : channelsNode) {
                        channels.add(channel.asInt());
                    }
                    // instrument.setChannels((Integer[]) channels.toArray());
                }

                // Process control codes
                JsonNode controlCodesNode = instrumentNode.get("controlCodes");
                if (controlCodesNode != null && !controlCodesNode.isEmpty()) {
                    List<ProxyControlCode> controlCodes = new ArrayList<>();

                    for (JsonNode ccNode : controlCodesNode) {
                        ProxyControlCode cc = new ProxyControlCode();
                        cc.setName(ccNode.get("name").asText());
                        cc.setCode(ccNode.get("code").asInt());

                        if (ccNode.has("lowerBound") && !ccNode.get("lowerBound").isNull()) {
                            cc.setLowerBound(ccNode.get("lowerBound").asInt());
                        }
                        if (ccNode.has("upperBound") && !ccNode.get("upperBound").isNull()) {
                            cc.setUpperBound(ccNode.get("upperBound").asInt());
                        }

                        // Process captions
                        JsonNode captionsNode = ccNode.get("captions");
                        if (captionsNode != null && !captionsNode.isEmpty()) {
                            Set<ProxyCaption> captions = new HashSet<>();
                            for (JsonNode captionNode : captionsNode) {
                                ProxyCaption caption = new ProxyCaption();
                                caption.setCode(ccNode.get("code").longValue());
                                // caption.setCode(captionNode.has("code") ? captionNode.get("code").asInt() :
                                // 0);
                                caption.setDescription(captionNode.get("description").asText());
                                // Save caption and get new ID
                                captions.add(saveCaption(caption));
                            }
                            cc.setCaptions(captions);
                        }

                        // Save control code and get new ID
                        controlCodes.add(saveControlCode(cc));
                    }
                    instrument.setControlCodes(controlCodes);
                }

                // Save instrument with new ID
                saveInstrument(instrument);
                instruments.add(instrument);
            }

            // Create and return config
            UserConfig config = new UserConfig();
            config.setInstruments(instruments);
            return config;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration: " + e.getMessage(), e);
        }
    }

    public List<ProxyInstrument> loadInstrumentsFromXML(String xmlFilePath) {
        try {
            logger.info("Loading instruments from XML: " + xmlFilePath);

            // Create mapper configured for our needs
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // Read JSON config file
            String jsonPath = xmlFilePath.replace(".xml", ".json");
            File jsonFile = new File(jsonPath);
            JsonNode rootNode = mapper.readTree(jsonFile);
            JsonNode instrumentsNode = rootNode.get("instruments");

            List<ProxyInstrument> instruments = new ArrayList<>();

            // Process each instrument
            for (JsonNode instrumentNode : instrumentsNode) {
                ProxyInstrument instrument = new ProxyInstrument();

                // Set basic properties
                instrument.setName(instrumentNode.get("name").asText());
                instrument.setDeviceName(instrumentNode.get("deviceName").asText());
                instrument.setLowestNote(instrumentNode.get("lowestNote").asInt());
                instrument.setHighestNote(instrumentNode.get("highestNote").asInt());
                instrument.setAvailable(instrumentNode.get("available").asBoolean());
                instrument.setInitialized(instrumentNode.get("initialized").asBoolean());

                // Process control codes
                JsonNode controlCodesNode = instrumentNode.get("controlCodes");
                if (controlCodesNode != null && !controlCodesNode.isEmpty()) {
                    List<ProxyControlCode> controlCodes = new ArrayList<>();

                    for (JsonNode ccNode : controlCodesNode) {
                        ProxyControlCode cc = new ProxyControlCode();
                        cc.setName(ccNode.get("name").asText());
                        cc.setCode(ccNode.get("code").asInt());

                        if (ccNode.has("lowerBound") && !ccNode.get("lowerBound").isNull()) {
                            cc.setLowerBound(ccNode.get("lowerBound").asInt());
                        }
                        if (ccNode.has("upperBound") && !ccNode.get("upperBound").isNull()) {
                            cc.setUpperBound(ccNode.get("upperBound").asInt());
                        }

                        // Process captions
                        JsonNode captionsNode = ccNode.get("captions");
                        if (captionsNode != null && !captionsNode.isEmpty()) {
                            Set<ProxyCaption> captions = new HashSet<>();
                            for (JsonNode captionNode : captionsNode) {
                                ProxyCaption caption = new ProxyCaption();
                                caption.setCode(ccNode.get("code").longValue());
                                caption.setDescription(captionNode.get("description").asText());
                                captions.add(saveCaption(caption));
                            }
                            cc.setCaptions(captions);
                        }

                        controlCodes.add(saveControlCode(cc));
                    }
                    instrument.setControlCodes(controlCodes);
                }

                // Save instrument and add to list
                saveInstrument(instrument);
                instruments.add(instrument);
                logger.info("Loaded and saved instrument: " + instrument.getName());
            }

            return instruments;

        } catch (Exception e) {
            logger.severe("Failed to load instruments from XML: " + e.getMessage());
            throw new RuntimeException("Failed to load instruments from XML", e);
        }
    }

    public UserConfig getConfig() {
        try {
            logger.info("Getting current configuration from Redis");

            UserConfig config = new UserConfig();

            // Load all instruments with their relationships
            List<ProxyInstrument> instruments = findAllInstruments();
            logger.info("Found " + instruments.size() + " instruments");

            config.setInstruments(instruments);

            return config;
        } catch (Exception e) {
            logger.severe("Error getting configuration: " + e.getMessage());
            throw new RuntimeException("Error getting configuration", e);
        }
    }

    public void saveConfig(UserConfig config) {
        try {
            logger.info("Saving configuration to Redis");

            // Clear existing instruments
            Set<String> instrumentKeys = jedisPool.getResource().keys(getCollectionKey(ProxyInstrument.class) + ":*");
            if (instrumentKeys != null && !instrumentKeys.isEmpty()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    for (String key : instrumentKeys) {
                        jedis.del(key);
                    }
                }
            }

            // Save new instruments
            if (config.getInstruments() != null) {
                for (ProxyInstrument instrument : config.getInstruments()) {
                    saveInstrument(instrument);
                    logger.info("Saved instrument: " + instrument.getName());
                }
            }

            logger.info("Configuration saved successfully");
        } catch (Exception e) {
            logger.severe("Error saving configuration: " + e.getMessage());
            throw new RuntimeException("Error saving configuration", e);
        }
    }

    public ProxyCaption findCaptionById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(getKey(ProxyCaption.class, id));
            return json != null ? objectMapper.readValue(json, ProxyCaption.class) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error finding caption", e);
        }
    }

    public List<ProxyCaption> findAllCaptions() {
        try (Jedis jedis = jedisPool.getResource()) {
            List<ProxyCaption> captions = new ArrayList<>();
            Set<String> keys = jedis.keys(getCollectionKey(ProxyCaption.class) + ":*");
            for (String key : keys) {
                String json = jedis.get(key);
                if (json != null) {
                    captions.add(objectMapper.readValue(json, ProxyCaption.class));
                }
            }
            return captions;
        } catch (Exception e) {
            throw new RuntimeException("Error finding all captions", e);
        }
    }

    public ProxyCaption saveCaption(ProxyCaption caption) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (caption.getId() == null) {
                // Get next ID from sequence
                String seqKey = "seq:" + ProxyCaption.class.getSimpleName().toLowerCase();
                caption.setId(jedis.incr(seqKey));
            }
            String json = objectMapper.writeValueAsString(caption);
            String key = getKey(ProxyCaption.class, caption.getId());
            jedis.set(key, json);
            return caption;
        } catch (Exception e) {
            throw new RuntimeException("Error saving caption", e);
        }
    }

    public void deleteCaption(ProxyCaption caption) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(getKey(ProxyCaption.class, caption.getId()));
        }
    }

    public void clearDatabase() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }

    public ProxyControlCode findControlCodeById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(ProxyControlCode.class, id);
            String json = jedis.get(key);
            if (json == null)
                return null;

            ProxyControlCode controlCode = objectMapper.readValue(json, ProxyControlCode.class);

            // Load captions
            String captionsKey = key + ":captions";
            Set<String> captionIds = jedis.smembers(captionsKey);
            if (captionIds != null) {
                controlCode.setCaptions(new HashSet<ProxyCaption>());
                for (String captionId : captionIds) {
                    ProxyCaption caption = findCaptionById(Long.valueOf(captionId));
                    if (caption != null) {
                        controlCode.getCaptions().add(caption);
                    }
                }
            }

            return controlCode;
        } catch (Exception e) {
            throw new RuntimeException("Error finding control code", e);
        }
    }

    public List<ProxyControlCode> findAllControlCodes() {
        try (Jedis jedis = jedisPool.getResource()) {
            List<ProxyControlCode> controlCodes = new ArrayList<>();
            Set<String> keys = jedis.keys(getCollectionKey(ProxyControlCode.class) + ":*");
            
            // Filter out relationship keys
            Set<String> ccKeys = keys.stream()
                .filter(key -> !key.contains(":captions"))
                .collect(Collectors.toSet());
            
            logger.info("Found " + ccKeys.size() + " control code keys");
            
            for (String key : ccKeys) {
                String json = jedis.get(key);
                if (json != null) {
                    ProxyControlCode cc = objectMapper.readValue(json, ProxyControlCode.class);
                    
                    // Load captions
                    String captionsKey = key + ":captions";
                    Set<String> captionIds = jedis.smembers(captionsKey);
                    if (captionIds != null && !captionIds.isEmpty()) {
                        Set<ProxyCaption> captions = new HashSet<>();
                        for (String captionId : captionIds) {
                            ProxyCaption caption = findCaptionById(Long.valueOf(captionId));
                            if (caption != null) {
                                captions.add(caption);
                            }
                        }
                        cc.setCaptions(captions);
                    }
                    
                    controlCodes.add(cc);
                    logger.info("Loaded control code: " + cc.getName() + 
                              " with " + (cc.getCaptions() != null ? cc.getCaptions().size() : 0) + 
                              " captions");
                }
            }
            return controlCodes;
        } catch (Exception e) {
            logger.severe("Error finding all control codes: " + e.getMessage());
            throw new RuntimeException("Error finding all control codes", e);
        }
    }

    public ProxyControlCode saveControlCode(ProxyControlCode controlCode) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (controlCode.getId() == null) {
                String seqKey = "seq:" + ProxyControlCode.class.getSimpleName().toLowerCase();
                controlCode.setId(jedis.incr(seqKey));
                logger.info("Generated new control code ID: " + controlCode.getId());
            }

            // Save captions first and maintain relationships
            Set<ProxyCaption> savedCaptions = new HashSet<>();
            if (controlCode.getCaptions() != null) {
                for (ProxyCaption caption : controlCode.getCaptions()) {
                    ProxyCaption savedCaption = saveCaption(caption);
                    savedCaptions.add(savedCaption);
                }
            }
            controlCode.setCaptions(savedCaptions);

            // Save control code
            String json = objectMapper.writeValueAsString(controlCode);
            String key = getKey(ProxyControlCode.class, controlCode.getId());
            jedis.set(key, json);

            // Save caption relationships
            String captionsKey = key + ":captions";
            jedis.del(captionsKey); // Clear existing relationships
            if (!savedCaptions.isEmpty()) {
                for (ProxyCaption caption : savedCaptions) {
                    jedis.sadd(captionsKey, String.valueOf(caption.getId()));
                }
            }

            logger.info("Saved control code: " + controlCode.getName() + 
                       " with " + savedCaptions.size() + " captions");
            return controlCode;
        } catch (Exception e) {
            logger.severe("Error saving control code: " + e.getMessage());
            throw new RuntimeException("Error saving control code", e);
        }
    }

    public void deleteControlCode(ProxyControlCode controlCode) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(ProxyControlCode.class, controlCode.getId());
            jedis.del(key);
            jedis.del(key + ":captions");
        }
    }

    public ProxyInstrument findInstrumentById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(getKey(ProxyInstrument.class, id));
            if (json == null)
                return null;

            ProxyInstrument instrument = objectMapper.readValue(json, ProxyInstrument.class);

            // Load relationships separately
            String padsKey = getKey(ProxyInstrument.class, id) + ":pads";
            String controlCodesKey = getKey(ProxyInstrument.class, id) + ":controlcodes";

            // Load pads
            Set<String> padIds = jedis.smembers(padsKey);
            Set<ProxyPad> pads = new HashSet<>();
            for (String padId : padIds) {
                ProxyPad pad = findPadById(Long.valueOf(padId));
                if (pad != null)
                    pads.add(pad);
            }
            instrument.setPads(pads);

            // Load control codes
            Set<String> ccIds = jedis.smembers(controlCodesKey);
            List<ProxyControlCode> controlCodes = new ArrayList<>();
            for (String ccId : ccIds) {
                ProxyControlCode cc = findControlCodeById(Long.valueOf(ccId));
                if (cc != null)
                    controlCodes.add(cc);
            }
            instrument.setControlCodes(controlCodes);

            return instrument;
        } catch (Exception e) {
            throw new RuntimeException("Error finding instrument", e);
        }
    }

    public List<ProxyInstrument> findAllInstruments() {
        try (Jedis jedis = jedisPool.getResource()) {
            List<ProxyInstrument> instruments = new ArrayList<>();
            Set<String> keys = jedis.keys(getCollectionKey(ProxyInstrument.class) + ":*");
            
            // Filter out relationship keys
            Set<String> instrumentKeys = keys.stream()
                .filter(key -> !key.contains(":controlcodes") && !key.contains(":pads"))
                .collect(Collectors.toSet());
            
            logger.info("Found " + instrumentKeys.size() + " instrument keys");
            
            for (String key : instrumentKeys) {
                try {
                    String json = jedis.get(key);
                    if (json != null) {
                        ProxyInstrument instrument = objectMapper.readValue(json, ProxyInstrument.class);
                        
                        // Load control codes
                        String controlCodesKey = key + ":controlcodes";
                        Set<String> ccIds = jedis.smembers(controlCodesKey);
                        if (ccIds != null && !ccIds.isEmpty()) {
                            List<ProxyControlCode> controlCodes = new ArrayList<>();
                            for (String ccId : ccIds) {
                                ProxyControlCode cc = findControlCodeById(Long.valueOf(ccId));
                                if (cc != null) {
                                    controlCodes.add(cc);
                                }
                            }
                            instrument.setControlCodes(controlCodes);
                        }
                        
                        logger.info("Loaded instrument: " + instrument.getName() + 
                                  " with " + (instrument.getControlCodes() != null ? 
                                  instrument.getControlCodes().size() : 0) + " control codes");
                        instruments.add(instrument);
                    }
                } catch (Exception e) {
                    logger.warning("Error loading instrument from key " + key + ": " + e.getMessage());
                }
            }
            return instruments;
        } catch (Exception e) {
            logger.severe("Error finding all instruments: " + e.getMessage());
            throw new RuntimeException("Error finding all instruments", e);
        }
    }

    public void saveInstrument(ProxyInstrument instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (instrument.getId() == null) {
                String seqKey = "seq:" + ProxyInstrument.class.getSimpleName().toLowerCase();
                instrument.setId(jedis.incr(seqKey));
            }

            // Save control codes first
            if (instrument.getControlCodes() != null) {
                instrument.getControlCodes().forEach(this::saveControlCode);
            }

            // Save main instrument data
            String json = objectMapper.writeValueAsString(instrument);
            String key = getKey(ProxyInstrument.class, instrument.getId());
            jedis.set(key, json);

            // Save control code relationships using SET instead of LIST
            String controlCodesKey = key + ":controlcodes";
            jedis.del(controlCodesKey); // Clear existing
            if (instrument.getControlCodes() != null) {
                instrument.getControlCodes().forEach(cc -> 
                    jedis.sadd(controlCodesKey, String.valueOf(cc.getId()))
                );
            }

            logger.info("Saved instrument: " + instrument.getName() + 
                       " with " + (instrument.getControlCodes() != null ? 
                       instrument.getControlCodes().size() : 0) + " control codes");

        } catch (Exception e) {
            logger.severe("Error saving instrument: " + e.getMessage());
            throw new RuntimeException("Error saving instrument", e);
        }
    }

    public void deleteInstrument(ProxyInstrument instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(ProxyInstrument.class, instrument.getId());
            jedis.del(key);
            jedis.del(key + ":controlcodes");
            jedis.del(key + ":pads");
        }
    }

    public ProxyPad findPadById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(getKey(ProxyPad.class, id));
            return json != null ? objectMapper.readValue(json, ProxyPad.class) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error finding pad", e);
        }
    }

    public List<ProxyPad> findAllPads() {
        try (Jedis jedis = jedisPool.getResource()) {
            List<ProxyPad> pads = new ArrayList<>();
            Set<String> keys = jedis.keys(getCollectionKey(ProxyPad.class) + ":*");
            for (String key : keys) {
                String json = jedis.get(key);
                if (json != null) {
                    pads.add(objectMapper.readValue(json, ProxyPad.class));
                }
            }
            return pads;
        } catch (Exception e) {
            throw new RuntimeException("Error finding all pads", e);
        }
    }

    public ProxyPad savePad(ProxyPad pad) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(ProxyPad.class, pad.getId());
            String json = objectMapper.writeValueAsString(pad);
            jedis.set(key, json);

            // Store control codes list
            String controlCodesKey = key + ":controlcodes";
            jedis.del(controlCodesKey); // Clear existing
            if (pad.getControlCodes() != null) {
                pad.getControlCodes().forEach(cc -> jedis.rpush(controlCodesKey, cc.toString()));
            }

            return pad;
        } catch (Exception e) {
            throw new RuntimeException("Error saving pad", e);
        }
    }

    public void deletePad(ProxyPad pad) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(ProxyPad.class, pad.getId());
            jedis.del(key);
            jedis.del(key + ":controlcodes");
        }
    }

    public boolean isDatabaseEmpty() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("*");
            logger.info("All Redis keys: " + String.join(", ", keys));
            return keys.isEmpty();
        }
    }

    public List<ProxyRule> findAllRules() {
        try (Jedis jedis = jedisPool.getResource()) {
            List<ProxyRule> rules = new ArrayList<>();
            Set<String> keys = jedis.keys(getCollectionKey(ProxyRule.class) + ":*");
            
            // Filter out relationship keys
            Set<String> ruleKeys = keys.stream()
                .filter(key -> !key.contains(":player"))
                .collect(Collectors.toSet());
                
            for (String key : ruleKeys) {
                String json = jedis.get(key);
                if (json != null) {
                    rules.add(objectMapper.readValue(json, ProxyRule.class));
                }
            }
            logger.info("Found " + rules.size() + " total rules");
            return rules;
        } catch (Exception e) {
            throw new RuntimeException("Error finding all rules", e);
        }
    }

    public List<ProxyRule> findRulesByPlayer(ProxyStrike player) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (player == null || player.getId() == null) {
                logger.warning("Invalid player provided to findRulesByPlayer");
                return new ArrayList<>();
            }

            List<ProxyRule> rules = new ArrayList<>();
            String playerRulesKey = "player:" + player.getId() + ":rules";
            Set<String> ruleIds = jedis.smembers(playerRulesKey);

            logger.info("Finding rules for player: " + player.getName() + " (ID: " + player.getId() + ")");
            logger.info("Using player rules key: " + playerRulesKey);
            logger.info("Found rule IDs: " + String.join(", ", ruleIds));

            for (String ruleId : ruleIds) {
                String ruleKey = getKey(ProxyRule.class, Long.valueOf(ruleId));
                String json = jedis.get(ruleKey);
                if (json != null) {
                    ProxyRule rule = objectMapper.readValue(json, ProxyRule.class);
                    rule.setPlayerId(player.getId());
                    rules.add(rule);
                    logger.info("Loaded rule ID " + ruleId + " for player " + player.getName());
                }
            }
            
            logger.info("Total rules found for player " + player.getName() + ": " + rules.size());
            return rules;
        } catch (Exception e) {
            logger.severe("Error finding rules for player: " + e.getMessage());
            throw new RuntimeException("Error finding rules for player", e);
        }
    }

    public ProxyRule saveRule(ProxyRule rule, ProxyStrike player) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (player == null || player.getId() == null) {
                throw new IllegalArgumentException("Player cannot be null and must have an ID");
            }

            // Generate new ID if needed
            if (rule.getId() == null) {
                String seqKey = "seq:" + ProxyRule.class.getSimpleName().toLowerCase();
                Long newId = jedis.incr(seqKey);
                rule.setId(newId);
                logger.info("Generated new rule ID: " + newId);
            }

            rule.setPlayerId(player.getId());

            // Save the rule
            String ruleKey = getKey(ProxyRule.class, rule.getId());
            String json = objectMapper.writeValueAsString(rule);
            jedis.set(ruleKey, json);

            // Update player-rule relationship
            String playerRulesKey = "player:" + player.getId() + ":rules";
            jedis.sadd(playerRulesKey, rule.getId().toString());

            logger.info("Saved rule " + rule.getId() + " for player " + player.getName() + 
                       " (Key: " + ruleKey + ", Player Rules Key: " + playerRulesKey + ")");
            return rule;
        } catch (Exception e) {
            logger.severe("Error saving rule: " + e.getMessage());
            throw new RuntimeException("Error saving rule", e);
        }
    }

    public void deleteRule(ProxyRule rule, ProxyStrike player) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (rule == null || rule.getId() == null || player == null || player.getId() == null) {
                logger.warning("Cannot delete rule - missing rule or player ID");
                return;
            }

            // Remove rule data
            String ruleKey = getKey(ProxyRule.class, rule.getId());
            jedis.del(ruleKey);

            // Remove from player-rule relationship
            String playerRulesKey = "player:" + player.getId() + ":rules";
            jedis.srem(playerRulesKey, rule.getId().toString());

            logger.info("Deleted rule " + rule.getId() + " from player " + player.getName() + 
                       " (Key: " + ruleKey + ", Player Rules Key: " + playerRulesKey + ")");
        } catch (Exception e) {
            logger.severe("Error deleting rule: " + e.getMessage());
            throw new RuntimeException("Error deleting rule", e);
        }
    }

    public List<ProxyStrike> findAllStrikes() {
        try (Jedis jedis = jedisPool.getResource()) {
            List<ProxyStrike> strikes = new ArrayList<>();
            String pattern = getCollectionKey(ProxyStrike.class) + ":*";

            // Get all keys and filter only those that start with our pattern
            // This avoids issues with auxiliary keys (like name mappings)
            Set<String> allKeys = jedis.keys("*");
            Set<String> strikeKeys = allKeys.stream()
                    .filter(key -> key.matches(pattern + "\\d+"))
                    .collect(Collectors.toSet());

            logger.info("Strike pattern: " + pattern);
            logger.info("Found strike keys: " + String.join(", ", strikeKeys));

            for (String key : strikeKeys) {
                try {
                    String json = jedis.get(key);
                    if (json != null) {
                        ProxyStrike strike = objectMapper.readValue(json, ProxyStrike.class);
                        logger.info("Loaded strike: " + strike.getName() + " from key: " + key);
                        strikes.add(strike);
                    }
                } catch (Exception e) {
                    logger.warning("Error loading strike from key " + key + ": " + e.getMessage());
                    // Continue loading other strikes even if one fails
                }
            }

            return strikes;
        } catch (Exception e) {
            logger.severe("Error in findAllStrikes: " + e.getMessage());
            throw new RuntimeException("Error finding all strikes", e);
        }
    }

    public ProxyStrike saveStrike(ProxyStrike strike) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Always generate new ID for new strikes
            if (strike.getId() == null) {
                String seqKey = "seq:" + ProxyStrike.class.getSimpleName().toLowerCase();
                Long newId = jedis.incr(seqKey);
                strike.setId(newId);
                logger.info("Generated new ID for strike: " + newId + ", name: " + strike.getName());
            }

            // Save the strike data
            String json = objectMapper.writeValueAsString(strike);
            String key = getKey(ProxyStrike.class, strike.getId());
            jedis.set(key, json);

            // Save name to ID mapping for lookups
            String nameKey = "name_to_id:" + ProxyStrike.class.getSimpleName().toLowerCase() + ":" + strike.getName();
            jedis.set(nameKey, strike.getId().toString());

            logger.info("Saved strike - Key: " + key + ", ID: " + strike.getId() + ", Name: " + strike.getName());
            return strike;
        } catch (Exception e) {
            logger.severe("Error saving strike: " + e.getMessage());
            throw new RuntimeException("Error saving strike", e);
        }
    }

    public void deleteStrike(ProxyStrike strike) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(ProxyStrike.class, strike.getId());
            jedis.del(key);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting strike", e);
        }
    }

    public ProxyTicker saveTicker(ProxyTicker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(ticker);
            jedis.set(TICKER_KEY, json);
            return ticker;
        } catch (Exception e) {
            logger.severe("Error saving ticker: " + e.getMessage());
            throw new RuntimeException("Failed to save ticker", e);
        }
    }

    public ProxyTicker loadTicker() {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(TICKER_KEY);
            if (json != null) {
                return objectMapper.readValue(json, ProxyTicker.class);
            }
            // Return default ticker if none exists
            return createDefaultTicker();
        } catch (Exception e) {
            logger.severe("Error loading ticker: " + e.getMessage());
            throw new RuntimeException("Failed to load ticker", e);
        }
    }

    private ProxyTicker createDefaultTicker() {
        ProxyTicker ticker = new ProxyTicker();
        // Set default values
        ticker.setTempoInBPM(120.0f);
        ticker.setBars(4);
        ticker.setBeatsPerBar(4);
        ticker.setTicksPerBeat(24);
        ticker.setParts(1);
        ticker.setPartLength(4L);
        return saveTicker(ticker); // Save and return the default ticker
    }

    public void deleteTicker() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(TICKER_KEY);
        } catch (Exception e) {
            logger.severe("Error deleting ticker: " + e.getMessage());
            throw new RuntimeException("Failed to delete ticker", e);
        }
    }

    public void saveFrameState(FrameState state) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(state);
            jedis.set("frameState", json);
        } catch (Exception e) {
            logger.severe("Error saving frame state: " + e.getMessage());
        }
    }

    public FrameState loadFrameState() {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("frameState");
            if (json != null) {
                return objectMapper.readValue(json, FrameState.class);
            }
        } catch (Exception e) {
            logger.severe("Error loading frame state: " + e.getMessage());
        }
        return null;
    }
}