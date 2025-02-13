package com.angrysurfer.spring.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.core.api.IControlCode;

public interface ControlCodes extends JpaRepository<IControlCode, Long> {
}
