package com.angrysurfer.sequencer.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.sequencer.model.midi.MidiInstrument;

public interface MidiInstrumentRepo extends JpaRepository<MidiInstrument, Long> {

    public List<MidiInstrument> findByChannel(int channel);

    public Optional<MidiInstrument> findByName(String instrument);

}