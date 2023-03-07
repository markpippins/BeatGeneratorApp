package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LookupItem {
    Long id;
    String desc;
    Long data;
    public LookupItem(Long id, String desc, Long data) {
        setId(id);
        setDesc(desc);
        setData(data);
    }
}
