package com.angrysurfer.core.api.db;

public interface Next<T> {

    T next(Long currentId);
}
