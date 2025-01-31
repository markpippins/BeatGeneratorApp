package com.angrysurfer.sequencer.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.sequencer.model.midi.ControlCode;

public interface ControlCodes  extends JpaRepository<ControlCode, Long> {
}
