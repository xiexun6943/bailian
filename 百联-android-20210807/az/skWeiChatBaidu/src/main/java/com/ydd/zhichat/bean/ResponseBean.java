package com.ydd.zhichat.bean;

/**
 * Created by phy on 2020/1/4
 */
public class ResponseBean {

    /**
     * currentTime : 1578061856658
     * resultCode : 1
     * resultMsg : 申请成功，待管理员审核
     */

    private long currentTime;
    private int resultCode;
    private String resultMsg;

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMsg() {
        return resultMsg;
    }

    public void setResultMsg(String resultMsg) {
        this.resultMsg = resultMsg;
    }
}
