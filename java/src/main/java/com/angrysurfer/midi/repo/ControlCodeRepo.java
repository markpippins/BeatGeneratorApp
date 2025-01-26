package com.angrysurfer.midi.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.midi.model.midi.ControlCode;

public interface ControlCodeRepo  extends JpaRepository<ControlCode, Long> {
}
