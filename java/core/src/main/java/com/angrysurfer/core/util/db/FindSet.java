package com.angrysurfer.core.util.db;

import java.util.Set;

public interface FindSet<T> {

    Set<T> find(Long id);
}
