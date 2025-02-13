package com.angrysurfer.spring.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.core.api.IInstrument;

public interface Instruments extends JpaRepository<IInstrument, Long> {

    @SuppressWarnings("null")
    public Optional<IInstrument> findById(Long id);

    public Optional<IInstrument> findByName(String instrument);

}