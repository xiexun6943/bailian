package com.ydd.zhichat.bean;

/**
 * 用户登录状态，
 */
public class UserStatus {
    public String accessToken;
    public int userStatus;
    public boolean userStatusChecked = false;

    @Override
    public String toString() {
        return "UserStatus{" +
                "accessToken='" + accessToken + '\'' +
                ", userStatus=" + userStatus +
                ", userStatusChecked=" + userStatusChecked +
                '}';
    }
}
