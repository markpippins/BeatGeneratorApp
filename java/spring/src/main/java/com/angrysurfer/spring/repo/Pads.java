package com.angrysurfer.spring.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.core.api.IPad;

public interface Pads extends JpaRepository<IPad, Long> {
}