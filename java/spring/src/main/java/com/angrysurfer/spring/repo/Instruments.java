package com.angrysurfer.spring.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.core.model.Instrument;

public interface Instruments extends JpaRepository<Instrument, Long> {

    @SuppressWarnings("null")
    public Optional<Instrument> findById(Long id);

    public Optional<Instrument> findByName(String instrument);

}