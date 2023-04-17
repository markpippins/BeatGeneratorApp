package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.ControlCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlCodeRepo  extends JpaRepository<ControlCode, Long> {
}
