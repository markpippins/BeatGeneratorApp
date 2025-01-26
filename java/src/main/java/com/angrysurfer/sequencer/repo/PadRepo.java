package com.angrysurfer.sequencer.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.sequencer.model.Pad;

public interface PadRepo extends JpaRepository<Pad, Long> {
}