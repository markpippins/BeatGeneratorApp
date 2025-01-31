package com.angrysurfer.sequencer.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.sequencer.model.Pad;

public interface Pads extends JpaRepository<Pad, Long> {
}