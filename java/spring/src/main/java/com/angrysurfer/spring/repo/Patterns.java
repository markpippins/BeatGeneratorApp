package com.angrysurfer.spring.repo;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.core.api.IPattern;

public interface Patterns  extends JpaRepository<IPattern, Long> {

    Set<IPattern> findBySongId(Long id);
}
