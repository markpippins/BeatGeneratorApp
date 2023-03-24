package com.angrysurfer.midi.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.midi.model.MidiInstrument;

public interface MidiInstrumentRepository extends JpaRepository<MidiInstrument, Long> {

    public List<MidiInstrument> findByChannel(int channel);

    public Optional<MidiInstrument> findByName(String instrument);

}