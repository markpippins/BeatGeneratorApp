package com.angrysurfer.core.sequencer;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class MelodicSequenceModifier {

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceModifier.class);

    /**
     * Generate a random pattern
     *
     * @param octaveRange The number of octaves to use (1-4)
     * @param density     The note density (0-100)
     * @return true if pattern was generated successfully
     */
    public static boolean generatePattern(MelodicSequencer sequencer, int octaveRange, int density) {

        MelodicSequenceData sequenceData = sequencer.getSequenceData();
        try {
            if (sequenceData == null) {
                logger.error("Cannot generate pattern - sequence data is null");
                return false;
            }

            // Validate parameters
            if (octaveRange < 1) octaveRange = 1;
            if (octaveRange > 4) octaveRange = 4;
            if (density < 0) density = 0;
            if (density > 100) density = 100;

            logger.info("Generating pattern with octave range: {}, density: {}", octaveRange, density);

            // Clear current pattern
            for (int step = 0; step < sequenceData.getMaxSteps(); step++) {
                sequenceData.setStepActive(step, false);
                sequenceData.setNoteValue(step, 60); // Middle C default
                sequenceData.setVelocityValue(step, 64); // Medium velocity
                sequenceData.setGateValue(step, 75); // Medium gate
                sequenceData.setProbabilityValue(step, 100); // Full probability
            }

            // Calculate note range based on octave selection
            int baseNote = 60 - (12 * (octaveRange / 2)); // Center around middle C
            int noteRange = 12 * octaveRange;

            // Determine active steps based on density
            int stepsToActivate = (int) Math.round(sequenceData.getMaxSteps() * (density / 100.0));

            // Ensure we have at least one step if density > 0
            if (density > 0 && stepsToActivate == 0) {
                stepsToActivate = 1;
            }

            logger.debug("Will activate {} steps out of {}", stepsToActivate, sequenceData.getMaxSteps());

            // Generate steps
            java.util.Random random = new java.util.Random();
            for (int i = 0; i < stepsToActivate; i++) {
                // Choose a random step that's not already active
                int step;
                int attempts = 0;
                do {
                    step = random.nextInt(sequenceData.getMaxSteps());
                    attempts++;
                    // Prevent infinite loops
                    if (attempts > 100) break;
                } while (sequenceData.isStepActive(step) && attempts < 100);

                // Activate the step
                sequenceData.setStepActive(step, true);

                // Generate a random note in the selected range
                int noteOffset = random.nextInt(noteRange);
                int note = baseNote + noteOffset;

                // Quantize to scale if enabled
                if (sequenceData.isQuantizeEnabled() && sequencer.getQuantizer() != null) {
                    note = sequencer.quantizeNote(note);
                }

                // Set the note
                sequenceData.setNoteValue(step, note);

                // Random velocity between 70-100
                int velocity = 70 + random.nextInt(31);
                sequenceData.setVelocityValue(step, velocity);

                // Random gate between 50-100
                int gate = 50 + random.nextInt(51);
                sequenceData.setGateValue(step, gate);

                // Sometimes add randomized probability
                if (random.nextDouble() < 0.3) { // 30% chance of partial probability
                    int probability = 50 + random.nextInt(51); // 50-100
                    sequenceData.setProbabilityValue(step, probability);
                }
            }

            // Notify that pattern was updated
            CommandBus.getInstance().publish(
                    Commands.MELODIC_PATTERN_UPDATED,
                    sequencer,
                    sequenceData);

            logger.info("Successfully generated new pattern with {} active steps", stepsToActivate);
            return true;
        } catch (Exception e) {
            logger.error("Error generating pattern: {}", e.getMessage(), e);
            return false;
        }
    }

    public static void updateInstrumentSettingsInSequenceData(MelodicSequencer sequencer) {

        Player player = sequencer.getPlayer();
        MelodicSequenceData sequenceData = sequencer.getSequenceData();

        if (player == null || player.getInstrument() == null || sequenceData == null) {
            logger.warn("Cannot update sequence data - missing player, instrument or sequenceData");
            return;
        }

        InstrumentWrapper instrument = player.getInstrument();

        // Save previous values for logging
        Integer prevBankIndex = sequenceData.getBankIndex();
        Integer prevPreset = sequenceData.getPreset();
        String prevSoundbankName = sequenceData.getSoundbankName();

        // Update with current instrument data
        sequenceData.setSoundbankName(instrument.getSoundbankName());
        sequenceData.setBankIndex(instrument.getBankIndex());
        sequenceData.setPreset(instrument.getPreset());
        sequenceData.setDeviceName(instrument.getDeviceName());
        sequenceData.setInstrumentId(instrument.getId());
        sequenceData.setInstrumentName(instrument.getName());

        // Log only if there were actual changes
        if (!Objects.equals(prevBankIndex, instrument.getBankIndex()) ||
                !Objects.equals(prevPreset, instrument.getPreset()) ||
                !Objects.equals(prevSoundbankName, instrument.getSoundbankName())) {

            logger.info("Updated sequence data for {} with instrument settings changed from {}/{}/{} to {}/{}/{}",
                    player.getName(),
                    prevBankIndex, prevPreset, prevSoundbankName,
                    instrument.getBankIndex(), instrument.getPreset(), instrument.getSoundbankName());
        }
    }
}
