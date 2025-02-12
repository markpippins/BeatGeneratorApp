package com.angrysurfer.beatsui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.angrysurfer.beatsui.config.BeatsUIConfig;
import com.angrysurfer.beatsui.config.RedisConfig;
import com.angrysurfer.beatsui.mock.Caption;
import com.angrysurfer.beatsui.mock.ControlCode;
import com.angrysurfer.beatsui.mock.Instrument;
import com.angrysurfer.beatsui.mock.Pad;
import com.angrysurfer.core.api.db.Delete;
import com.angrysurfer.core.api.db.FindAll;
import com.angrysurfer.core.api.db.FindOne;
import com.angrysurfer.core.api.db.FindSet;
import com.angrysurfer.core.api.db.Max;
import com.angrysurfer.core.api.db.Min;
import com.angrysurfer.core.api.db.Next;
import com.angrysurfer.core.api.db.Prior;
import com.angrysurfer.core.api.db.Save;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.player.IPlayer;
import com.angrysurfer.core.model.player.Strike;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisService { // implements Database {
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

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
        return cls.getSimpleName().toLowerCase() + ":" + id;
    }

    private String getCollectionKey(Class<?> cls) {
        return cls.getSimpleName().toLowerCase() + "s";
    }

    public static void main(String[] args) {
        RedisService service = new RedisService();

        try {
            // Clear database for fresh start
            service.clearDatabase();
            System.out.println("Database cleared");

            // Load configuration from file into Redis
            String configPath = "swing\\beatsui\\src\\main\\java\\com\\angrysurfer\\beatsui\\config\\beats-config.json";
            System.out.println("Loading configuration from " + configPath);

            // BeatsUIConfig config = BeatsUIConfig.loadDefaults(configPath, service);

            // // Verify database content
            // System.out.println("\n=== Database Content Verification ===");
            // List<Instrument> instruments = service.findAllInstruments();
            // System.out.println("Instruments in database: " + instruments.size());

            // instruments.forEach(instrument -> {
            // System.out.println("\nInstrument: " + instrument.getName());
            // System.out.println("Control codes: " + instrument.getControlCodes().size());
            // System.out.println("Pads: " + instrument.getPads().size());
            // System.out.println(instrument.toString());
            // });

            // Clear database and load from XML
            String xmlPath = "C:/Users/MarkP/dev/BeatGeneratorApp/java/swing/beatsui/src/main/java/com/angrysurfer/beatsui/config/beats-config.xml";
            BeatsUIConfig configFromXml = service.loadConfigFromXml(xmlPath);

            // Verify the loaded configuration
            System.out.println("\n=== Loaded Configuration ===");
            configFromXml.getInstruments().forEach(instrument -> {
                System.out.println("\nInstrument: " + instrument.getName() + " (ID: " + instrument.getId() + ")");
                System.out.println("Control codes: " + instrument.getControlCodes().size());
                System.out.println("Pads: " + (instrument.getPads() != null ? instrument.getPads().size() : 0));
            });

        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // @Override
    public Caption findCaptionById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(getKey(Caption.class, id));
            return json != null ? objectMapper.readValue(json, Caption.class) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error finding caption", e);
        }
    }

    // @Override
    public List<Caption> findAllCaptions() {
        try (Jedis jedis = jedisPool.getResource()) {
            List<Caption> captions = new ArrayList<>();
            Set<String> keys = jedis.keys(getCollectionKey(Caption.class) + ":*");
            for (String key : keys) {
                String json = jedis.get(key);
                if (json != null) {
                    captions.add(objectMapper.readValue(json, Caption.class));
                }
            }
            return captions;
        } catch (Exception e) {
            throw new RuntimeException("Error finding all captions", e);
        }
    }

    // @Override
    public Caption saveCaption(Caption caption) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (caption.getId() == null) {
                // Get next ID from sequence
                String seqKey = "seq:" + Caption.class.getSimpleName().toLowerCase();
                caption.setId(jedis.incr(seqKey));
            }
            String json = objectMapper.writeValueAsString(caption);
            String key = getKey(Caption.class, caption.getId());
            jedis.set(key, json);
            return caption;
        } catch (Exception e) {
            throw new RuntimeException("Error saving caption", e);
        }
    }

    // @Override
    public void deleteCaption(Caption caption) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(getKey(Caption.class, caption.getId()));
        }
    }

    // @Override
    public void clearDatabase() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }

    // @Override
    public FindAll<com.angrysurfer.core.model.ui.Caption> getCaptionFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCaptionFindAll'");
    }

    // @Override
    public FindOne<com.angrysurfer.core.model.ui.Caption> getCaptionFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCaptionFindOne'");
    }

    // @Override
    public Save<com.angrysurfer.core.model.ui.Caption> getCaptionSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCaptionSaver'");
    }

    // @Override
    public Delete<com.angrysurfer.core.model.ui.Caption> getCaptionDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCaptionDeleter'");
    }

    // @Override
    public FindAll<ControlCode> getControlCodeFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getControlCodeFindAll'");
    }

    // @Override
    public FindOne<ControlCode> getControlCodeFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getControlCodeFindOne'");
    }

    // @Override
    public Save<ControlCode> getControlCodeSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getControlCodeSaver'");
    }

    // @Override
    public Delete<ControlCode> getControlCodeDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getControlCodeDeleter'");
    }

    // @Override
    public FindAll<Instrument> getInstrumentFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInstrumentFindAll'");
    }

    // @Override
    public FindOne<Instrument> getInstrumentFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInstrumentFindOne'");
    }

    // @Override
    public Save<Instrument> getInstrumentSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInstrumentSaver'");
    }

    // @Override
    public Delete<Instrument> getInstrumentDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInstrumentDeleter'");
    }

    // @Override
    public FindAll<Pad> getPadFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPadFindAll'");
    }

    // @Override
    public FindOne<Pad> getPadFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPadFindOne'");
    }

    // @Override
    public Save<Pad> getPadSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPadSaver'");
    }

    // @Override
    public Delete<Pad> getPadDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPadDeleter'");
    }

    // @Override
    public FindAll<Pattern> getPatternFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPatternFindAll'");
    }

    // @Override
    public FindOne<Pattern> getPatternFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPatternFindOne'");
    }

    // @Override
    public FindSet<Pattern> getSongPatternFinder() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongPatternFinder'");
    }

    // @Override
    public Save<Pattern> getPatternSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPatternSaver'");
    }

    // @Override
    public Delete<Pattern> getPatternDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPatternDeleter'");
    }

    // @Override
    public FindAll<Rule> getRuleFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRuleFindAll'");
    }

    // @Override
    public FindOne<Rule> getRuleFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRuleFindOne'");
    }

    // @Override
    public FindSet<Rule> getPlayerRuleFindSet() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerRuleFindSet'");
    }

    // @Override
    public Save<Rule> getRuleSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRuleSaver'");
    }

    // @Override
    public Delete<Rule> getRuleDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRuleDeleter'");
    }

    // @Override
    public FindAll<Song> getSongFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongFindAll'");
    }

    // @Override
    public FindOne<Song> getSongFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongFindOne'");
    }

    // @Override
    public Save<Song> getSongSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongSaver'");
    }

    // @Override
    public Delete<Song> getSongDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongDeleter'");
    }

    // @Override
    public Next<Song> getSongForward() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongForward'");
    }

    // @Override
    public Prior<Song> getSongBack() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongBack'");
    }

    // @Override
    public Max<Song> getSongMax() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongMax'");
    }

    // @Override
    public Min<Song> getSongMin() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSongMin'");
    }

    // @Override
    public FindAll<Step> getStepFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStepFindAll'");
    }

    // @Override
    public FindOne<Step> getStepFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStepFindOne'");
    }

    // @Override
    public FindSet<Step> getPatternStepFinder() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPatternStepFinder'");
    }

    // @Override
    public Save<Step> getStepSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStepSaver'");
    }

    // @Override
    public Delete<Step> getStepDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStepDeleter'");
    }

    // @Override
    public FindAll<Strike> getStrikeFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStrikeFindAll'");
    }

    // @Override
    public FindOne<Strike> getStrikeFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStrikeFindOne'");
    }

    // @Override
    public FindSet<Strike> getTickerStrikeFinder() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerStrikeFinder'");
    }

    // @Override
    public Save<IPlayer> getStrikeSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStrikeSaver'");
    }

    // @Override
    public Delete<IPlayer> getStrikeDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStrikeDeleter'");
    }

    // @Override
    public FindAll<Ticker> getTickerFindAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerFindAll'");
    }

    // @Override
    public FindOne<Ticker> getTickerFindOne() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerFindOne'");
    }

    // @Override
    public Save<Ticker> getTickerSaver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerSaver'");
    }

    // @Override
    public Delete<Ticker> getTickerDeleter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerDeleter'");
    }

    // @Override
    public Next<Ticker> getTickerForward() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerForward'");
    }

    // @Override
    public Prior<Ticker> getTickerBack() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerBack'");
    }

    // @Override
    public Max<Ticker> getTickerMax() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerMax'");
    }

    // @Override
    public Min<Ticker> getTickerMin() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTickerMin'");
    }

    // @Override
    public void setCaptionFindAll(FindAll<com.angrysurfer.core.model.ui.Caption> captionFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCaptionFindAll'");
    }

    // @Override
    public void setCaptionFindOne(FindOne<com.angrysurfer.core.model.ui.Caption> captionFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCaptionFindOne'");
    }

    // @Override
    public void setCaptionSaver(Save<com.angrysurfer.core.model.ui.Caption> captionSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCaptionSaver'");
    }

    // @Override
    public void setCaptionDeleter(Delete<com.angrysurfer.core.model.ui.Caption> captionDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCaptionDeleter'");
    }

    // @Override
    public void setControlCodeFindAll(FindAll<ControlCode> controlCodeFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setControlCodeFindAll'");
    }

    // @Override
    public void setControlCodeFindOne(FindOne<ControlCode> controlCodeFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setControlCodeFindOne'");
    }

    // @Override
    public void setControlCodeSaver(Save<ControlCode> controlCodeSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setControlCodeSaver'");
    }

    // @Override
    public void setControlCodeDeleter(Delete<ControlCode> controlCodeDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setControlCodeDeleter'");
    }

    // @Override
    public void setInstrumentFindAll(FindAll<Instrument> instrumentFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInstrumentFindAll'");
    }

    // @Override
    public void setInstrumentFindOne(FindOne<Instrument> instrumentFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInstrumentFindOne'");
    }

    // @Override
    public void setInstrumentSaver(Save<Instrument> instrumentSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInstrumentSaver'");
    }

    // @Override
    public void setInstrumentDeleter(Delete<Instrument> instrumentDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInstrumentDeleter'");
    }

    // @Override
    public void setPadFindAll(FindAll<Pad> padFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPadFindAll'");
    }

    // @Override
    public void setPadFindOne(FindOne<Pad> padFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPadFindOne'");
    }

    // @Override
    public void setPadSaver(Save<Pad> padSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPadSaver'");
    }

    // @Override
    public void setPadDeleter(Delete<Pad> padDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPadDeleter'");
    }

    // @Override
    public void setPatternFindAll(FindAll<Pattern> patternFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPatternFindAll'");
    }

    // @Override
    public void setPatternFindOne(FindOne<Pattern> patternFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPatternFindOne'");
    }

    // @Override
    public void setSongPatternFinder(FindSet<Pattern> songPatternFinder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongPatternFinder'");
    }

    // @Override
    public void setPatternSaver(Save<Pattern> patternSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPatternSaver'");
    }

    // @Override
    public void setPatternDeleter(Delete<Pattern> patternDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPatternDeleter'");
    }

    // @Override
    public void setRuleFindAll(FindAll<Rule> ruleFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRuleFindAll'");
    }

    // @Override
    public void setRuleFindOne(FindOne<Rule> ruleFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRuleFindOne'");
    }

    // @Override
    public void setPlayerRuleFindSet(FindSet<Rule> playerRuleFindSet) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerRuleFindSet'");
    }

    // @Override
    public void setRuleSaver(Save<Rule> ruleSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRuleSaver'");
    }

    // @Override
    public void setRuleDeleter(Delete<Rule> ruleDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRuleDeleter'");
    }

    // @Override
    public void setSongFindAll(FindAll<Song> songFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongFindAll'");
    }

    // @Override
    public void setSongFindOne(FindOne<Song> songFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongFindOne'");
    }

    // @Override
    public void setSongSaver(Save<Song> songSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongSaver'");
    }

    // @Override
    public void setSongDeleter(Delete<Song> songDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongDeleter'");
    }

    // @Override
    public void setSongForward(Next<Song> songForward) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongForward'");
    }

    // @Override
    public void setSongBack(Prior<Song> songBack) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongBack'");
    }

    // @Override
    public void setSongMax(Max<Song> songMax) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongMax'");
    }

    // @Override
    public void setSongMin(Min<Song> songMin) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSongMin'");
    }

    // @Override
    public void setStepFindAll(FindAll<Step> stepFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStepFindAll'");
    }

    // @Override
    public void setStepFindOne(FindOne<Step> stepFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStepFindOne'");
    }

    // @Override
    public void setPatternStepFinder(FindSet<Step> patternStepFinder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPatternStepFinder'");
    }

    // @Override
    public void setStepSaver(Save<Step> stepSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStepSaver'");
    }

    // @Override
    public void setStepDeleter(Delete<Step> stepDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStepDeleter'");
    }

    // @Override
    public void setStrikeFindAll(FindAll<Strike> strikeFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStrikeFindAll'");
    }

    // @Override
    public void setStrikeFindOne(FindOne<Strike> strikeFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStrikeFindOne'");
    }

    // @Override
    public void setTickerStrikeFinder(FindSet<Strike> tickerStrikeFinder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerStrikeFinder'");
    }

    // @Override
    public void setStrikeSaver(Save<IPlayer> strikeSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStrikeSaver'");
    }

    // @Override
    public void setStrikeDeleter(Delete<IPlayer> strikeDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStrikeDeleter'");
    }

    // @Override
    public void setTickerFindAll(FindAll<Ticker> tickerFindAll) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerFindAll'");
    }

    // @Override
    public void setTickerFindOne(FindOne<Ticker> tickerFindOne) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerFindOne'");
    }

    // @Override
    public void setTickerSaver(Save<Ticker> tickerSaver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerSaver'");
    }

    // @Override
    public void setTickerDeleter(Delete<Ticker> tickerDeleter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerDeleter'");
    }

    // @Override
    public void setTickerForward(Next<Ticker> tickerForward) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerForward'");
    }

    // @Override
    public void setTickerBack(Prior<Ticker> tickerBack) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerBack'");
    }

    // @Override
    public void setTickerMax(Max<Ticker> tickerMax) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerMax'");
    }

    // @Override
    public void setTickerMin(Min<Ticker> tickerMin) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTickerMin'");
    }

    // @Override
    public com.angrysurfer.core.model.ui.Caption saveCaption(com.angrysurfer.core.model.ui.Caption caption) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveCaption'");
    }

    // @Override
    public void deleteCaption(com.angrysurfer.core.model.ui.Caption caption) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteCaption'");
    }

    // @Override
    public ControlCode findControlCodeById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(ControlCode.class, id);
            String json = jedis.get(key);
            if (json == null)
                return null;

            ControlCode controlCode = objectMapper.readValue(json, ControlCode.class);

            // Load captions
            String captionsKey = key + ":captions";
            Set<String> captionIds = jedis.smembers(captionsKey);
            if (captionIds != null) {
                controlCode.setCaptions(new HashSet<Caption>());
                for (String captionId : captionIds) {
                    Caption caption = findCaptionById(Long.valueOf(captionId));
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

    // @Override
    public List<ControlCode> findAllControlCodes() {
        try (Jedis jedis = jedisPool.getResource()) {
            List<ControlCode> controlCodes = new ArrayList<>();
            Set<String> keys = jedis.keys(getCollectionKey(ControlCode.class) + ":*");
            for (String key : keys) {
                String json = jedis.get(key);
                if (json != null) {
                    controlCodes.add(objectMapper.readValue(json, ControlCode.class));
                }
            }
            return controlCodes;
        } catch (Exception e) {
            throw new RuntimeException("Error finding all control codes", e);
        }
    }

    // @Override
    public ControlCode saveControlCode(ControlCode controlCode) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (controlCode.getId() == null) {
                // Get next ID from sequence
                String seqKey = "seq:" + ControlCode.class.getSimpleName().toLowerCase();
                controlCode.setId(jedis.incr(seqKey));
            }

            // Save captions first
            if (controlCode.getCaptions() != null) {
                controlCode.getCaptions().forEach(this::saveCaption);
            }

            String json = objectMapper.writeValueAsString(controlCode);
            String key = getKey(ControlCode.class, controlCode.getId());
            jedis.set(key, json);

            // Save caption relationships
            String captionsKey = key + ":captions";
            jedis.del(captionsKey);
            if (controlCode.getCaptions() != null) {
                controlCode.getCaptions().forEach(caption -> jedis.sadd(captionsKey, String.valueOf(caption.getId())));
            }

            return controlCode;
        } catch (Exception e) {
            throw new RuntimeException("Error saving control code", e);
        }
    }

    // @Override
    public void deleteControlCode(ControlCode controlCode) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(ControlCode.class, controlCode.getId());
            jedis.del(key);
            jedis.del(key + ":captions");
        }
    }

    // @Override
    public Instrument findInstrumentById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(getKey(Instrument.class, id));
            if (json == null)
                return null;

            Instrument instrument = objectMapper.readValue(json, Instrument.class);

            // Load relationships separately
            String padsKey = getKey(Instrument.class, id) + ":pads";
            String controlCodesKey = getKey(Instrument.class, id) + ":controlcodes";

            // Load pads
            Set<String> padIds = jedis.smembers(padsKey);
            Set<Pad> pads = new HashSet<>();
            for (String padId : padIds) {
                Pad pad = findPadById(Long.valueOf(padId));
                if (pad != null)
                    pads.add(pad);
            }
            instrument.setPads(pads);

            // Load control codes
            Set<String> ccIds = jedis.smembers(controlCodesKey);
            List<ControlCode> controlCodes = new ArrayList<>();
            for (String ccId : ccIds) {
                ControlCode cc = findControlCodeById(Long.valueOf(ccId));
                if (cc != null)
                    controlCodes.add(cc);
            }
            instrument.setControlCodes(controlCodes);

            return instrument;
        } catch (Exception e) {
            throw new RuntimeException("Error finding instrument", e);
        }
    }

    // @Override
    public Instrument saveInstrument(Instrument instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (instrument.getId() == null) {
                // Get next ID from sequence
                String seqKey = "seq:" + Instrument.class.getSimpleName().toLowerCase();
                instrument.setId(jedis.incr(seqKey));
            }

            // Save control codes first
            if (instrument.getControlCodes() != null) {
                instrument.getControlCodes().forEach(this::saveControlCode);
            }

            // Save main instrument data
            String json = objectMapper.writeValueAsString(instrument);
            String key = getKey(Instrument.class, instrument.getId());
            jedis.set(key, json);

            // Save control code relationships
            String controlCodesKey = key + ":controlcodes";
            jedis.del(controlCodesKey);
            if (instrument.getControlCodes() != null) {
                instrument.getControlCodes().forEach(cc -> jedis.sadd(controlCodesKey, String.valueOf(cc.getId())));
            }

            return instrument;
        } catch (Exception e) {
            throw new RuntimeException("Error saving instrument", e);
        }
    }

    // @Override
    public void deleteInstrument(Instrument instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(Instrument.class, instrument.getId());
            jedis.del(key);
            jedis.del(key + ":controlcodes");
            jedis.del(key + ":pads");
        }
    }

    // @Override
    public Pad findPadById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(getKey(Pad.class, id));
            return json != null ? objectMapper.readValue(json, Pad.class) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error finding pad", e);
        }
    }

    // @Override
    public List<Pad> findAllPads() {
        try (Jedis jedis = jedisPool.getResource()) {
            List<Pad> pads = new ArrayList<>();
            Set<String> keys = jedis.keys(getCollectionKey(Pad.class) + ":*");
            for (String key : keys) {
                String json = jedis.get(key);
                if (json != null) {
                    pads.add(objectMapper.readValue(json, Pad.class));
                }
            }
            return pads;
        } catch (Exception e) {
            throw new RuntimeException("Error finding all pads", e);
        }
    }

    // @Override
    public Pad savePad(Pad pad) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(Pad.class, pad.getId());
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
    public void deletePad(Pad pad) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(Pad.class, pad.getId());
            jedis.del(key);
            jedis.del(key + ":controlcodes");
        }
    }

    // @Override
    public Pattern findPatternById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findPatternById'");
    }

    // @Override
    public Set<Pattern> findPatternBySongId(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findPatternBySongId'");
    }

    // @Override
    public List<Pattern> findAllPatterns() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAllPatterns'");
    }

    // @Override
    public Pattern savePattern(Pattern pattern) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'savePattern'");
    }

    // @Override
    public void deletePattern(Pattern pattern) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deletePattern'");
    }

    // @Override
    public Rule findRuleById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findRuleById'");
    }

    // @Override
    public Set<Rule> findRulesByPlayerId(Long playerId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findRulesByPlayerId'");
    }

    // @Override
    public Rule saveRule(Rule rule) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveRule'");
    }

    // @Override
    public void deleteRule(Rule rule) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteRule'");
    }

    // @Override
    public Song findSongById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findSongById'");
    }

    // @Override
    public List<Song> findAllSongs() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAllSongs'");
    }

    // @Override
    public Song saveSong(Song song) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveSong'");
    }

    public List<Instrument> findAllInstruments() {
        try (Jedis jedis = jedisPool.getResource()) {
            List<Instrument> instruments = new ArrayList<>();
            Set<String> keys = jedis.keys(getCollectionKey(Instrument.class) + ":*");

            for (String key : keys) {
                // Extract ID from key (instrument:1 -> 1)
                Long id = Long.valueOf(key.split(":")[1]);
                // Use findInstrumentById to properly load relationships
                Instrument instrument = findInstrumentById(id);
                if (instrument != null) {
                    instruments.add(instrument);
                }
            }
            return instruments;
        } catch (Exception e) {
            throw new RuntimeException("Error finding all instruments", e);
        }
    }

    public BeatsUIConfig loadConfigFromXml(String xmlFilePath) {
        try {
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

            List<Instrument> instruments = new ArrayList<>();

            // Process each instrument
            for (JsonNode instrumentNode : instrumentsNode) {
                Instrument instrument = new Instrument();

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
                    List<ControlCode> controlCodes = new ArrayList<>();

                    for (JsonNode ccNode : controlCodesNode) {
                        ControlCode cc = new ControlCode();
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
                            Set<Caption> captions = new HashSet<>();
                            for (JsonNode captionNode : captionsNode) {
                                Caption caption = new Caption();
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
                instruments.add(saveInstrument(instrument));
            }

            // Create and return config
            BeatsUIConfig config = new BeatsUIConfig();
            config.setInstruments(instruments);
            return config;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration: " + e.getMessage(), e);
        }
    }
}