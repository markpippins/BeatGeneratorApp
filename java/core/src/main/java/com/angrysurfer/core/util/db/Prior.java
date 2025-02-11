package com.angrysurfer.core.util.db;

public interface Prior<T> {

    T prior(Long currentId);
}
