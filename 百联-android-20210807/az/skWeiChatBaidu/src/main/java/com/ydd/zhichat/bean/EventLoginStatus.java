package com.ydd.zhichat.bean;

public class EventLoginStatus {

    private final String device;
    private final boolean onLine;

    public EventLoginStatus(String device, boolean onLine) {
        this.device = device;
        this.onLine = onLine;
    }

    public String getDevice() {
        return device;
    }

    public boolean isOnLine() {
        return onLine;
    }
}
