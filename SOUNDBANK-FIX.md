# Soundbank Fix Implementation Plan

## Current Issues

1. **SoundParametersPanel soundbank changes not applying to instrument**: 
   - UI updates correctly but the soundbank isn't applied to the instrument
   - Likely caused by timing issues or race conditions
   - May be related to insufficient synchronization between UI and MIDI system

2. **Multiple mousewheel handlers with duplicated code**:
   - ScalePanel, MelodicSequencerGeneratorPanel, and DrumParamsSequencerPanel have similar but separate implementations
   - Code duplication increases maintenance burden
   - Inconsistent behavior across different panels

## Immediate Fixes

### 1. Improve SoundParametersPanel Soundbank Application

- Enhance `applySoundbankChanges()` with better error handling and recovery
- Add a retry mechanism when soundbank application fails
- Ensure UI changes happen after soundbank loading using `SwingUtilities.invokeLater`
- Add test note verification to confirm sound configuration is working

### 2. Create Common Mousewheel Handler

- Extract common mousewheel handling code into a utility class
- Implement specialized handlers for different component types
- Create a unified API that can be used across all panels

## Implementation Details

### SoundParametersPanel Fix

```java
private void applySoundbankChanges() {
    Player currentPlayer = getPlayer();
    if (currentPlayer != null && currentPlayer.getInstrument() != null) {
        // Get selected soundbank
        SoundbankItem item = (SoundbankItem) soundbankCombo.getSelectedItem();
        if (item == null) return;
        
        String soundbankName = item.getName();
        logger.info("Applying soundbank: {}", soundbankName);
        
        // Update instrument property
        InstrumentWrapper instrument = currentPlayer.getInstrument();
        instrument.setSoundbankName(soundbankName);
        
        // Try to apply soundbank with retry mechanism
        boolean applied = false;
        for (int attempt = 1; attempt <= 2; attempt++) {
            applied = SoundbankManager.getInstance().applySoundbank(instrument, soundbankName);
            if (applied) break;
            
            // If failed on first attempt, try to reinitialize synthesizer
            if (attempt == 1) {
                logger.warn("Failed to apply soundbank on first try, reinitializing synthesizer...");
                InternalSynthManager.getInstance().initializeSynthesizer();
            }
        }
        
        // Update bank and preset UI
        updateBankCombo();
        
        // Ensure changes are fully applied using SwingUtilities.invokeLater
        SwingUtilities.invokeLater(() -> {
            // Apply preset through both managers to ensure consistency
            PlayerManager.getInstance().applyInstrumentPreset(currentPlayer);
            
            // Play test note to verify configuration
            SoundbankManager.getInstance().playPreviewNote(currentPlayer, 100);
        });
    }
}
```

### Common Mousewheel Handler

```java
public class MouseWheelUtil {
    // Handle combo box mousewheel events
    public static void handleComboBoxWheel(JComboBox<?> comboBox, int scrollDirection) {
        int currentIndex = comboBox.getSelectedIndex();
        int newIndex = currentIndex + scrollDirection;
        newIndex = Math.max(0, Math.min(newIndex, comboBox.getItemCount() - 1));
        
        if (newIndex != currentIndex) {
            comboBox.setSelectedIndex(newIndex);
        }
    }
    
    // Handle spinner mousewheel events
    public static void handleSpinnerWheel(JSpinner spinner, int scrollDirection) {
        int currentValue = (Integer) spinner.getValue();
        int newValue = currentValue + scrollDirection;
        
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
        int min = (Integer) model.getMinimum();
        int max = (Integer) model.getMaximum();
        
        newValue = Math.max(min, Math.min(newValue, max));
        
        if (newValue != currentValue) {
            spinner.setValue(newValue);
        }
    }
    
    // Handle toggle button mousewheel events
    public static void handleToggleButtonWheel(JToggleButton toggle) {
        toggle.doClick();
    }
    
    // Handle dial mousewheel events
    public static void handleDialWheel(Dial dial, int scrollDirection) {
        int currentValue = dial.getValue();
        int newValue = currentValue + scrollDirection;
        
        newValue = Math.max(dial.getMinimum(), Math.min(newValue, dial.getMaximum()));
        
        if (newValue != currentValue) {
            dial.setValue(newValue);
        }
    }
}
```

## Testing Plan

1. **Soundbank Testing**:
   - Test changing soundbanks on different players
   - Verify correct application of soundbank changes
   - Test recovery from failure scenarios
   - Verify proper sound output after changes

2. **Mousewheel Testing**:
   - Test mousewheel functionality on all panels
   - Verify correct component behavior based on focus
   - Test boundary conditions (min/max values)
   - Verify consistent behavior across different panels
