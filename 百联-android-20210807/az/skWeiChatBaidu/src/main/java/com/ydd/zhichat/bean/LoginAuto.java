package com.ydd.zhichat.bean;

import java.util.List;

/**
 * 检查Token是否过期的Bean
 */
public class LoginAuto {

    private int tokenExists; // 1=令牌存在、0=令牌不存在
    private int serialStatus;// 1=没有设备号、2=设备号一致、3=设备号不一致
    private int payPassword; // 是否已经设置了支付密码，
    // 1=游客（用于后台浏览数据）；2=公众号 ；3=机器账号，由系统自动生成；4=客服账号;5=管理员；6=超级管理员；7=财务；
    private List<Integer> role; // 身份，
    private String myInviteCode;
    private Settings settings;

    public int getTokenExists() {
        return tokenExists;
    }

    public void setTokenExists(int tokenExists) {
        this.tokenExists = tokenExists;
    }

    public int getSerialStatus() {
        return serialStatus;
    }

    public void setSerialStatus(int serialStatus) {
        this.serialStatus = serialStatus;
    }

    public int getPayPassword() {
        return payPassword;
    }

    public void setPayPassword(int payPassword) {
        this.payPassword = payPassword;
    }

    public List<Integer> getRole() {
        return role;
    }

    public void setRole(List<Integer> role) {
        this.role = role;
    }

    public String getMyInviteCode() {
        return myInviteCode;
    }

    public void setMyInviteCode(String myInviteCode) {
        this.myInviteCode = myInviteCode;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    /**
     * User Private Settings
     */
    public static class Settings extends PrivacySetting {

    }

}
