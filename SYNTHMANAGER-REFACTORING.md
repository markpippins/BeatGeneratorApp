# Synthesizer Management Refactoring Plan

## Current Issues

1. **Overlapping responsibilities**:
   - `InternalSynthManager` and `SoundbankManager` both manage synthesizer resources
   - Both maintain separate collections for soundbanks and preset information
   - Code duplication in synthesizer and soundbank operations

2. **Improper dependency direction**:
   - `SoundbankManager` calls `InternalSynthManager` to get the synthesizer
   - This creates an awkward coupling where soundbank manager depends on synth manager

3. **Resource management inefficiencies**:
   - Duplicate collections for similar data
   - Potentially inefficient loading of soundbanks
   - Unclear ownership of synthesizer resources

## Refactoring Plan

### Step 1: Define Clear Responsibilities

- **InternalSynthManager**: Core synthesizer operations and MIDI channel management
  - Synthesizer initialization and management
  - Channel management and control
  - Instrument parameter control
  - Access to synthesizer API
  
- **SoundbankManager**: Soundbank catalog and discovery
  - Soundbank file handling (loading, saving)
  - Soundbank metadata
  - Interface for soundbank selection
  - Organization of soundbank catalog

### Step 2: Centralize Synthesizer Operations in InternalSynthManager

1. Move all direct synthesizer interaction code to InternalSynthManager:
   - Add methods in InternalSynthManager:
     - `loadSoundbank(File file)` 
     - `applySoundbank(InstrumentWrapper instrument, String soundbankName)`
     - `unloadSoundbank(String name)`
   
2. Make SoundbankManager delegate to InternalSynthManager:
   - Update SoundbankManager methods to call InternalSynthManager
   - Keep the catalog features in SoundbankManager
   - Make SoundbankManager focus on organization, not low-level operations

### Step 3: Fix Soundbank Application Flow

Current flow:
```
SoundParametersPanel -> SoundbankManager.applySoundbank() -> InternalSynthManager.getSynthesizer()
```

New flow:
```
SoundParametersPanel -> SoundbankManager.applySoundbank() -> InternalSynthManager.applySoundbank()
```

### Step 4: Implement Changes

1. **Enhance InternalSynthManager**:
   - Add soundbank loading & application methods
   - Move direct synth interaction code from SoundbankManager
   - Create proper error handling and recovery

2. **Refactor SoundbankManager**:
   - Remove duplicated functionality
   - Focus on soundbank catalog management
   - Delegate operations to InternalSynthManager

3. **Update SoundParametersPanel**:
   - Make applySoundbankChanges use the new centralized methods
   - Add better error handling and recovery

### Expected Benefits

1. Clear separation of concerns
2. Reduced code duplication
3. Better error handling
4. More consistent behavior
5. Simplified maintenance
6. More efficient resource usage
