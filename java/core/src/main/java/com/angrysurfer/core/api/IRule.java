package com.angrysurfer.core.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface IRule {

    Long getId();

    Long getPlayerId();

    Integer getOperator();

    Integer getComparison();

    Double getValue();

    Integer getPart();

    Long getStart();

    Long getEnd();

    boolean isUnsaved();

    IPlayer getPlayer();

    void setId(Long id);

    void setPlayerId(Long playerId);

    void setOperator(Integer operator);

    void setComparison(Integer comparison);

    void setValue(Double value);

    void setPart(Integer part);

    void setStart(Long start);

    void setEnd(Long end);

    @JsonIgnore
    void setUnsaved(boolean unsaved);

    void setPlayer(IPlayer player);

    boolean isEqualTo(IRule rule);

}