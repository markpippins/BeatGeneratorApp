package com.angrysurfer.spring.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.angrysurfer.core.api.ICaption;

@Repository
public interface Captions extends JpaRepository<ICaption, Long> {
    Iterable<ICaption> findByCode(Long code);
}
