package com.angrysurfer.sequencer.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.sequencer.model.midi.Instrument;

public interface Instruments extends JpaRepository<Instrument, Long> {

    // public List<Instrument> findByChannel(int channel);

    public Optional<Instrument> findByName(String instrument);

}