package com.angrysurfer.midi.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.midi.model.Pad;

public interface PadRepository extends JpaRepository<Pad, Long> {
}