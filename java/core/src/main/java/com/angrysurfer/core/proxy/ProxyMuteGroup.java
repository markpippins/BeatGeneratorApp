package com.angrysurfer.core.proxy;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProxyMuteGroup { // should extend event
    private String name;
    private Set<IProxyPlayer> players = new HashSet<>();
}
