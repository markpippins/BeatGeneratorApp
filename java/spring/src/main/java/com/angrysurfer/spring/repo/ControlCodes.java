package com.angrysurfer.spring.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.core.model.midi.ControlCode;

public interface ControlCodes  extends JpaRepository<ControlCode, Long> {
}
