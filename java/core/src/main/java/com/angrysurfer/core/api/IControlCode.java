package com.angrysurfer.core.api;

import java.util.Set;

public interface IControlCode {

    Long getId();

    String getName();

    Integer getCode();

    Integer getLowerBound();

    Integer getUpperBound();

    Integer getPad();

    Boolean getBinary();

    Set<ICaption> getCaptions();

    void setId(Long id);

    void setName(String name);

    void setCode(Integer code);

    void setLowerBound(Integer lowerBound);

    void setUpperBound(Integer upperBound);

    void setPad(Integer pad);

    void setBinary(Boolean binary);

    void setCaptions(Set<ICaption> captions);

}