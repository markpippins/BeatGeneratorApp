package com.angrysurfer.sequencer.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.sequencer.model.midi.Instrument;

public interface Instruments extends JpaRepository<Instrument, Long> {

    @SuppressWarnings("null")
    public Optional<Instrument> findById(Long id);

    public Optional<Instrument> findByName(String instrument);

}