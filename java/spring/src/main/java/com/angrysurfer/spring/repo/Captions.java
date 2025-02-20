package com.angrysurfer.spring.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.angrysurfer.core.model.Caption;

@Repository
public interface Captions extends JpaRepository<Caption, Long> {
    Iterable<Caption> findByCode(Long code);
}
