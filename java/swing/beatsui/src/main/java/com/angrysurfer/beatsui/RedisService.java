package com.angrysurfer.beatsui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.angrysurfer.beatsui.config.BeatsUIConfig;
import com.angrysurfer.beatsui.config.RedisConfig;
import com.angrysurfer.core.api.db.Delete;
import com.angrysurfer.core.api.db.FindAll;
import com.angrysurfer.core.api.db.FindOne;
import com.angrysurfer.core.api.db.FindSet;
import com.angrysurfer.core.api.db.Max;
import com.angrysurfer.core.api.db.Min;
import com.angrysurfer.core.api.db.Next;
import com.angrysurfer.core.api.db.Prior;
import com.angrysurfer.core.api.db.Save;
import com.angrysurfer.core.proxy.IProxyPlayer;
import com.angrysurfer.core.proxy.ProxyCaption;
import com.angrysurfer.core.proxy.ProxyControlCode;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.proxy.ProxyPad;
import com.angrysurfer.core.proxy.ProxyPattern;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxySong;
import com.angrysurfer.core.proxy.ProxyStep;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisService { // implements Database {
    private static final Logger logger = Logger.getLogger(RedisService.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    private static final String TICKER_KEY = "ticker";

    public RedisService() {
        this.jedisPool = RedisConfig.getJedisPool();
        this.objectMapper = new ObjectMapper();
        // Configure ObjectMapper for handling bidirectional references
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
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

    public BeatsUIConfig loadConfigFromXml(String xmlFilePath) {
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
            BeatsUIConfig config = new BeatsUIConfig();
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

    public BeatsUIConfig getConfig() {
        try {
            logger.info("Getting current configuration from Redis");

            BeatsUIConfig config = new BeatsUIConfig();

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

    public void saveConfig(BeatsUIConfig config) {
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

    // public static void main(String[] args) {
    // RedisService service = new RedisService();

    // try {
    // // Clear database for fresh start
    // service.clearDatabase();
    // System.out.println("Database cleared");

    // // Load configuration from file into Redis
    // String configPath =
    // "swing\\beatsui\\src\\main\\java\\com\\angrysurfer\\beatsui\\config\\beats-config.json";
    // System.out.println("Loading configuration from " + configPath);

    // // BeatsUIConfig config = BeatsUIConfig.loadDefaults(configPath, service);

    // // // Verify database content
    // // System.out.println("\n=== Database Content Verification ===");
    // // List<Instrument> instruments = service.findAllInstruments();
    // // System.out.println("Instruments in database: " + instruments.size());

    // // instruments.forEach(instrument -> {
    // // System.out.println("\nInstrument: " + instrument.getName());
    // // System.out.println("Control codes: " +
    // instrument.getControlCodes().size());
    // // System.out.println("Pads: " + instrument.getPads().size());
    // // System.out.println(instrument.toString());
    // // });

    // // Clear database and load from XML
    // String xmlPath =
    // "C:/Users/MarkP/dev/BeatGeneratorApp/java/swing/beatsui/src/main/java/com/angrysurfer/beatsui/config/beats-config.xml";
    // BeatsUIConfig configFromXml = service.loadConfigFromXml(xmlPath);

    // // Verify the loaded configuration
    // System.out.println("\n=== Loaded Configuration ===");
    // configFromXml.getInstruments().forEach(instrument -> {
    // System.out.println("\nInstrument: " + instrument.getName() + " (ID: " +
    // instrument.getId() + ")");
    // System.out.println("Control codes: " + instrument.getControlCodes().size());
    // System.out.println("Pads: " + (instrument.getPads() != null ?
    // instrument.getPads().size() : 0));
    // });

    // } catch (Exception e) {
    // System.err.println("Error during testing: " + e.getMessage());
    // e.printStackTrace();
    // }
    // }

    // @Override
    public ProxyCaption findCaptionById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(getKey(ProxyCaption.class, id));
            return json != null ? objectMapper.readValue(json, ProxyCaption.class) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error finding caption", e);
        }
    }

    // @Override
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

    // @Override
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

    // @Override
    public void deleteCaption(ProxyCaption caption) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(getKey(ProxyCaption.class, caption.getId()));
        }
    }

    // @Override
    public void clearDatabase() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }

    // @Override
    public FindAll<ProxyCaption> getCaptionFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCaptionFindAll'");
    }

    // @Override
    public FindOne<ProxyCaption> getCaptionFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCaptionFindOne'");
    }

    // @Override
    public Save<ProxyCaption> getCaptionSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCaptionSaver'");
    }

    // @Override
    public Delete<ProxyCaption> getCaptionDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCaptionDeleter'");
    }

    // @Override
    public FindAll<ProxyControlCode> getControlCodeFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getControlCodeFindAll'");
    }

    // @Override
    public FindOne<ProxyControlCode> getControlCodeFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getControlCodeFindOne'");
    }

    // @Override
    public Save<ProxyControlCode> getControlCodeSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getControlCodeSaver'");
    }

    // @Override
    public Delete<ProxyControlCode> getControlCodeDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getControlCodeDeleter'");
    }

    // @Override
    public FindAll<ProxyInstrument> getInstrumentFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInstrumentFindAll'");
    }

    // @Override
    public FindOne<ProxyInstrument> getInstrumentFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInstrumentFindOne'");
    }

    // @Override
    public Save<ProxyInstrument> getInstrumentSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInstrumentSaver'");
    }

    // @Override
    public Delete<ProxyInstrument> getInstrumentDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInstrumentDeleter'");
    }

    // @Override
    public FindAll<ProxyPad> getPadFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPadFindAll'");
    }

    // @Override
    public FindOne<ProxyPad> getPadFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPadFindOne'");
    }

    // @Override
    public Save<ProxyPad> getPadSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPadSaver'");
    }

    // @Override
    public Delete<ProxyPad> getPadDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPadDeleter'");
    }

    // @Override
    public FindAll<ProxyPattern> getPatternFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPatternFindAll'");
    }

    // @Override
    public FindOne<ProxyPattern> getPatternFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPatternFindOne'");
    }

    // @Override
    public FindSet<ProxyPattern> getSongPatternFinder() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongPatternFinder'");
    }

    // @Override
    public Save<ProxyPattern> getPatternSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPatternSaver'");
    }

    // @Override
    public Delete<ProxyPattern> getPatternDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPatternDeleter'");
    }

    // @Override
    public FindAll<ProxyRule> getRuleFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRuleFindAll'");
    }

    // @Override
    public FindOne<ProxyRule> getRuleFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRuleFindOne'");
    }

    // @Override
    public FindSet<ProxyRule> getPlayerRuleFindSet() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerRuleFindSet'");
    }

    // @Override
    public Save<ProxyRule> getRuleSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRuleSaver'");
    }

    // @Override
    public Delete<ProxyRule> getRuleDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRuleDeleter'");
    }

    // @Override
    public FindAll<ProxySong> getSongFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongFindAll'");
    }

    // @Override
    public FindOne<ProxySong> getSongFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongFindOne'");
    }

    // @Override
    public Save<ProxySong> getSongSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongSaver'");
    }

    // @Override
    public Delete<ProxySong> getSongDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongDeleter'");
    }

    // @Override
    public Next<ProxySong> getSongForward() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongForward'");
    }

    // @Override
    public Prior<ProxySong> getSongBack() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongBack'");
    }

    // @Override
    public Max<ProxySong> getSongMax() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongMax'");
    }

    // @Override
    public Min<ProxySong> getSongMin() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongMin'");
    }

    // @Override
    public FindAll<ProxyStep> getStepFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStepFindAll'");
    }

    // @Override
    public FindOne<ProxyStep> getStepFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStepFindOne'");
    }

    // @Override
    public FindSet<ProxyStep> getPatternStepFinder() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPatternStepFinder'");
    }

    // @Override
    public Save<ProxyStep> getStepSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStepSaver'");
    }

    // @Override
    public Delete<ProxyStep> getStepDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStepDeleter'");
    }

    // @Override
    public FindAll<ProxyStrike> getStrikeFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStrikeFindAll'");
    }

    // @Override
    public FindOne<ProxyStrike> getStrikeFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStrikeFindOne'");
    }

    // @Override
    public FindSet<ProxyStrike> getTickerStrikeFinder() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerStrikeFinder'");
    }

    // @Override
    public Save<IProxyPlayer> getStrikeSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStrikeSaver'");
    }

    // @Override
    public Delete<IProxyPlayer> getStrikeDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStrikeDeleter'");
    }

    // @Override
    public FindAll<ProxyTicker> getTickerFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerFindAll'");
    }

    // @Override
    public FindOne<ProxyTicker> getTickerFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerFindOne'");
    }

    // @Override
    public Save<ProxyTicker> getTickerSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerSaver'");
    }

    // @Override
    public Delete<ProxyTicker> getTickerDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerDeleter'");
    }

    // @Override
    public Next<ProxyTicker> getTickerForward() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerForward'");
    }

    // @Override
    public Prior<ProxyTicker> getTickerBack() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerBack'");
    }

    // @Override
    public Max<ProxyTicker> getTickerMax() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerMax'");
    }

    // @Override
    public Min<ProxyTicker> getTickerMin() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerMin'");
    }

    // @Override
    public void setCaptionFindAll(FindAll<ProxyCaption> captionFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCaptionFindAll'");
    }

    // @Override
    public void setCaptionFindOne(FindOne<ProxyCaption> captionFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCaptionFindOne'");
    }

    // @Override
    public void setCaptionSaver(Save<ProxyCaption> captionSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCaptionSaver'");
    }

    // @Override
    public void setCaptionDeleter(Delete<ProxyCaption> captionDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCaptionDeleter'");
    }

    // @Override
    public void setControlCodeFindAll(FindAll<ProxyControlCode> controlCodeFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setControlCodeFindAll'");
    }

    // @Override
    public void setControlCodeFindOne(FindOne<ProxyControlCode> controlCodeFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setControlCodeFindOne'");
    }

    // @Override
    public void setControlCodeSaver(Save<ProxyControlCode> controlCodeSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setControlCodeSaver'");
    }

    // @Override
    public void setControlCodeDeleter(Delete<ProxyControlCode> controlCodeDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setControlCodeDeleter'");
    }

    // @Override
    public void setInstrumentFindAll(FindAll<ProxyInstrument> instrumentFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInstrumentFindAll'");
    }

    // @Override
    public void setInstrumentFindOne(FindOne<ProxyInstrument> instrumentFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInstrumentFindOne'");
    }

    // @Override
    public void setInstrumentSaver(Save<ProxyInstrument> instrumentSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInstrumentSaver'");
    }

    // @Override
    public void setInstrumentDeleter(Delete<ProxyInstrument> instrumentDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInstrumentDeleter'");
    }

    // @Override
    public void setPadFindAll(FindAll<ProxyPad> padFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPadFindAll'");
    }

    // @Override
    public void setPadFindOne(FindOne<ProxyPad> padFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPadFindOne'");
    }

    // @Override
    public void setPadSaver(Save<ProxyPad> padSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPadSaver'");
    }

    // @Override
    public void setPadDeleter(Delete<ProxyPad> padDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPadDeleter'");
    }

    // @Override
    public void setPatternFindAll(FindAll<ProxyPattern> patternFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPatternFindAll'");
    }

    // @Override
    public void setPatternFindOne(FindOne<ProxyPattern> patternFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPatternFindOne'");
    }

    // @Override
    public void setSongPatternFinder(FindSet<ProxyPattern> songPatternFinder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongPatternFinder'");
    }

    // @Override
    public void setPatternSaver(Save<ProxyPattern> patternSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPatternSaver'");
    }

    // @Override
    public void setPatternDeleter(Delete<ProxyPattern> patternDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPatternDeleter'");
    }

    // @Override
    public void setRuleFindAll(FindAll<ProxyRule> ruleFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRuleFindAll'");
    }

    // @Override
    public void setRuleFindOne(FindOne<ProxyRule> ruleFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRuleFindOne'");
    }

    // @Override
    public void setPlayerRuleFindSet(FindSet<ProxyRule> playerRuleFindSet) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerRuleFindSet'");
    }

    // @Override
    public void setRuleSaver(Save<ProxyRule> ruleSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRuleSaver'");
    }

    // @Override
    public void setRuleDeleter(Delete<ProxyRule> ruleDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRuleDeleter'");
    }

    // @Override
    public void setSongFindAll(FindAll<ProxySong> songFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongFindAll'");
    }

    // @Override
    public void setSongFindOne(FindOne<ProxySong> songFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongFindOne'");
    }

    // @Override
    public void setSongSaver(Save<ProxySong> songSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongSaver'");
    }

    // @Override
    public void setSongDeleter(Delete<ProxySong> songDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongDeleter'");
    }

    // @Override
    public void setSongForward(Next<ProxySong> songForward) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongForward'");
    }

    // @Override
    public void setSongBack(Prior<ProxySong> songBack) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongBack'");
    }

    // @Override
    public void setSongMax(Max<ProxySong> songMax) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongMax'");
    }

    // @Override
    public void setSongMin(Min<ProxySong> songMin) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongMin'");
    }

    // @Override
    public void setStepFindAll(FindAll<ProxyStep> stepFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStepFindAll'");
    }

    // @Override
    public void setStepFindOne(FindOne<ProxyStep> stepFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStepFindOne'");
    }

    // @Override
    public void setPatternStepFinder(FindSet<ProxyStep> patternStepFinder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPatternStepFinder'");
    }

    // @Override
    public void setStepSaver(Save<ProxyStep> stepSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStepSaver'");
    }

    // @Override
    public void setStepDeleter(Delete<ProxyStep> stepDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStepDeleter'");
    }

    // @Override
    public void setStrikeFindAll(FindAll<ProxyStrike> strikeFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStrikeFindAll'");
    }

    // @Override
    public void setStrikeFindOne(FindOne<ProxyStrike> strikeFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStrikeFindOne'");
    }

    // @Override
    public void setTickerStrikeFinder(FindSet<ProxyStrike> tickerStrikeFinder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerStrikeFinder'");
    }

    // @Override
    public void setStrikeSaver(Save<IProxyPlayer> strikeSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStrikeSaver'");
    }

    // @Override
    public void setStrikeDeleter(Delete<IProxyPlayer> strikeDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStrikeDeleter'");
    }

    // @Override
    public void setTickerFindAll(FindAll<ProxyTicker> tickerFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerFindAll'");
    }

    // @Override
    public void setTickerFindOne(FindOne<ProxyTicker> tickerFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerFindOne'");
    }

    // @Override
    public void setTickerSaver(Save<ProxyTicker> tickerSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerSaver'");
    }

    // @Override
    public void setTickerDeleter(Delete<ProxyTicker> tickerDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerDeleter'");
    }

    // @Override
    public void setTickerForward(Next<ProxyTicker> tickerForward) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerForward'");
    }

    // @Override
    public void setTickerBack(Prior<ProxyTicker> tickerBack) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerBack'");
    }

    // @Override
    public void setTickerMax(Max<ProxyTicker> tickerMax) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerMax'");
    }

    // @Override
    public void setTickerMin(Min<ProxyTicker> tickerMin) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerMin'");
    }

    // @Override
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

    // @Override
    public void deleteControlCode(ProxyControlCode controlCode) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(ProxyControlCode.class, controlCode.getId());
            jedis.del(key);
            jedis.del(key + ":captions");
        }
    }

    // @Override
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

    // @Override
    public void deleteInstrument(ProxyInstrument instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(ProxyInstrument.class, instrument.getId());
            jedis.del(key);
            jedis.del(key + ":controlcodes");
            jedis.del(key + ":pads");
        }
    }

    // @Override
    public ProxyPad findPadById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(getKey(ProxyPad.class, id));
            return json != null ? objectMapper.readValue(json, ProxyPad.class) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error finding pad", e);
        }
    }

    // @Override
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

    // @Override
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

    // @Override
    public void deletePad(ProxyPad pad) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(ProxyPad.class, pad.getId());
            jedis.del(key);
            jedis.del(key + ":controlcodes");
        }
    }

    // @Override
    public ProxyPattern findPatternById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findPatternById'");
    }

    // @Override
    public Set<ProxyPattern> findPatternBySongId(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findPatternBySongId'");
    }

    // @Override
    public List<ProxyPattern> findAllPatterns() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAllPatterns'");
    }

    // @Override
    public ProxyPattern savePattern(ProxyPattern pattern) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'savePattern'");
    }

    // @Override
    public void deletePattern(ProxyPattern pattern) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deletePattern'");
    }

    // @Override
    public ProxyRule findRuleById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findRuleById'");
    }

    // @Override
    public Set<ProxyRule> findRulesByPlayerId(Long playerId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findRulesByPlayerId'");
    }

    // @Override
    public ProxyRule saveRule(ProxyRule rule, ProxyStrike player) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Generate new ID if needed
            if (rule.getId() == null) {
                String seqKey = "seq:" + ProxyRule.class.getSimpleName().toLowerCase();
                Long newId = jedis.incr(seqKey);
                rule.setId(newId);
                logger.info("Generated new rule ID: " + newId);
            }

            rule.setPlayerId(player.getId()); // Ensure player ID is set

            // Save the rule
            String json = objectMapper.writeValueAsString(rule);
            String ruleKey = getKey(ProxyRule.class, rule.getId());
            jedis.set(ruleKey, json);

            // Link rule to player
            String playerRulesKey = getKey(ProxyStrike.class, player.getId()) + ":rules";
            jedis.sadd(playerRulesKey, rule.getId().toString());

            logger.info("Saved rule " + rule.getId() + " for player " + player.getName());
            return rule;
        } catch (Exception e) {
            logger.severe("Error saving rule: " + e.getMessage());
            throw new RuntimeException("Error saving rule", e);
        }
    }

    // @Override
    public void deleteRule(ProxyRule rule) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteRule'");
    }

    // @Override
    public ProxySong findSongById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findSongById'");
    }

    // @Override
    public List<ProxySong> findAllSongs() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAllSongs'");
    }

    // @Override
    public ProxySong saveSong(ProxySong song) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveSong'");
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
            for (String key : keys) {
                String json = jedis.get(key);
                if (json != null) {
                    rules.add(objectMapper.readValue(json, ProxyRule.class));
                }
            }
            return rules;
        } catch (Exception e) {
            throw new RuntimeException("Error finding all rules", e);
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

    public List<ProxyRule> findRulesByPlayer(ProxyStrike player) {
        try (Jedis jedis = jedisPool.getResource()) {
            List<ProxyRule> rules = new ArrayList<>();
            String playerRulesKey = getKey(ProxyStrike.class, player.getId()) + ":rules";
            Set<String> ruleIds = jedis.smembers(playerRulesKey);

            logger.info("Finding rules for player: " + player.getName() + " (ID: " + player.getId() + "), key: "
                    + playerRulesKey);
            logger.info("Found rule IDs: " + String.join(", ", ruleIds));

            for (String ruleId : ruleIds) {
                String key = getKey(ProxyRule.class, Long.valueOf(ruleId));
                String json = jedis.get(key);
                if (json != null) {
                    ProxyRule rule = objectMapper.readValue(json, ProxyRule.class);
                    rule.setPlayerId(player.getId()); // Ensure playerId is set
                    rules.add(rule);
                    logger.info("Loaded rule: " + json);
                }
            }
            return rules;
        } catch (Exception e) {
            logger.severe("Error finding rules for player: " + e.getMessage());
            throw new RuntimeException("Error finding rules for player", e);
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

            // Remove link to player
            String playerRulesKey = getKey(ProxyStrike.class, player.getId()) + ":rules";
            jedis.srem(playerRulesKey, rule.getId().toString());

            logger.info("Deleted rule " + rule.getId() + " from player " + player.getName());
        } catch (Exception e) {
            logger.severe("Error deleting rule: " + e.getMessage());
            throw new RuntimeException("Error deleting rule", e);
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