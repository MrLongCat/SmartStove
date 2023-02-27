package com.bayu.iotstove;

public class Stove {

    private Integer relay;
    private Double maxD;
    private Double maxT;
    private String startTime;
    private boolean isHeating;

    public Integer getRelay() {
        return relay;
    }

    public void setRelay(Integer relay) {
        this.relay = relay;
    }

    public Double getMaxD() {
        return maxD;
    }

    public void setMaxD(Double maxD) {
        this.maxD = maxD;
    }

    public Double getMaxT() {
        return maxT;
    }

    public void setMaxT(Double maxT) {
        this.maxT = maxT;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public boolean isHeating() {
        return isHeating;
    }

    public void setHeating(boolean heating) {
        isHeating = heating;
    }
}
