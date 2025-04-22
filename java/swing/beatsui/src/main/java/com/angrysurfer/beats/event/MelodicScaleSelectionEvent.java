// Create this class in the appropriate package
package com.angrysurfer.beats.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MelodicScaleSelectionEvent {
    private Integer sequencerId;
    private String scale;
}