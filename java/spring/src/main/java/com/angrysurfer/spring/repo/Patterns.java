package com.angrysurfer.spring.repo;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.core.model.Pattern;

public interface Patterns  extends JpaRepository<Pattern, Long> {

    Set<Pattern> findBySongId(Long id);
}
