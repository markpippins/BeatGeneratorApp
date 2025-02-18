package com.angrysurfer.core.api;

public interface StatusConsumer {

    public void setSite(String status);

    public void clearSite();

    public void setStatus(String status);

    public void clearStatus();

    public void setMessage(String status);

    public void clearMessage();

    // public void setSender(String status);

    // public void clearSender();

}