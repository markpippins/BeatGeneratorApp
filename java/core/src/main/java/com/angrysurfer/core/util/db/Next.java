package com.angrysurfer.core.util.db;

public interface Next<T> {

    T next(Long currentId);
}
