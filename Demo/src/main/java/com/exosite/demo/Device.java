package com.exosite.demo;

/**
 * Device model.
 */
public class Device {

    public Device() {
        mTemperature = null;
        mSetpoint = null;
        mWriteInProgress = false;
        mError = "";
    }

    public Double getTemperature() {
        return mTemperature;
    }
    public void setTemperature(Double temperature) { mTemperature = temperature; }
    private Double mTemperature;

    public boolean getWriteInProgress() {
        return mWriteInProgress;
    }
    public void setWriteInProgress(boolean writeInProgress) {
        mWriteInProgress = writeInProgress;
    }
    private boolean mWriteInProgress;

    public String getError() { return mError; }
    public void setError(String error) { mError = error; }
    private String mError;

    final int SETPOINT_LOW = 50;
    final int SETPOINT_HIGH = 80;
    public Double getSetpoint() { return mSetpoint; }
    public Double getSetpointAsPercent() {
        // translate setpoint range range to 0-100
        Double val = ((mSetpoint - SETPOINT_LOW) / (SETPOINT_HIGH - SETPOINT_LOW)) * 100.0;
        return new Double(Math.round(val));
    }
    public void setSetpoint(Double setpoint) { mSetpoint = setpoint; }
    public void setSetpointFromPercent(int percent) {
        setSetpoint(new Double(Math.round((percent / 100.0) * (SETPOINT_HIGH - SETPOINT_LOW) + SETPOINT_LOW)));
    }
    private Double mSetpoint;

    public Integer getSwitch() {
        return mSwitch;
    }
    public void setSwitchFromCloud(Integer value) {
        if (!this.getWriteInProgress()) {
            mSwitch = value;
        }
    }
    public void setSwitchFromUI(Integer value) {
        mSwitch = value;
    }
    private Integer mSwitch;

}
