package com.angrysurfer.midi.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.midi.model.MidiInstrument;

public interface MidiInstrumentRepository extends JpaRepository<MidiInstrument, Long> {

    Optional<MidiInstrument> findByChannel(int channel);

}