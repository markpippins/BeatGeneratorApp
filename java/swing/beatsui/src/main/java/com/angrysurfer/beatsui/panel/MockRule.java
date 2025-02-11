package com.angrysurfer.beatsui.panel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MockRule {
    public static final String[] OPERATORS = {
            "Tick", "Beat", "Bar", "Part",
            "Ticks", "Beats", "Bars", "Parts"
    };

    public static final String[] COMPARISONS = {
            "=", "!=", "<", ">", "%"
    };

    private Integer operator;
    private Integer comparison;
    private Double value;
    private Integer part;

    public MockRule() {
        this.operator = 0;
        this.comparison = 0;
        this.value = 0.0;
        this.part = 0;
    }

    public Object[] toRow() {
        return new Object[] {
                OPERATORS[operator],
                COMPARISONS[comparison],
                value,
                part
        };
    }
}