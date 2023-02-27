package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository  extends JpaRepository<Rule, Long> {
}
