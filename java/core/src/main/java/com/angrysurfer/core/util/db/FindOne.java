package com.angrysurfer.core.util.db;

import java.util.Optional;

public interface FindOne<T> {

    Optional<T> find(Long id);
}
