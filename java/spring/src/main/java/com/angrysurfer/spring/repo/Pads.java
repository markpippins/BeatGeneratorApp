package com.angrysurfer.spring.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.core.model.Pad;

public interface Pads extends JpaRepository<Pad, Long> {
}