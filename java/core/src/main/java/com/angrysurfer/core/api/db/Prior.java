package com.angrysurfer.core.api.db;

public interface Prior<T> {

    T prior(Long currentId);
}
