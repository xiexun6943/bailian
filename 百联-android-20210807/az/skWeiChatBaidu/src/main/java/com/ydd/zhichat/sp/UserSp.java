package com.ydd.zhichat.sp;

import android.content.Context;

/**
 * 保存当前登录用户的一些简单快捷使用的基本信息 userId ,userName,userPassword进入了MainActivity一定是正常的，可以随意取
 */
public class UserSp extends CommonSp {
    private static final String SP_NAME = "login_user_info";// FILE_NAME
    private static UserSp instance;

    /* known key */
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_TELEPHONE = "telephone";// 用户手机号码，相当于用户名
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_EXPIRES_IN = "expires_in";// 过期时间，服务器返回的是7天这样的时间，本地转换为当前时间+过期时间段的1个时间戳
    private static final String KEY_UPDATE = "update";// 本地资料有没有更新，防止切换手机出现数据断层

    public static final UserSp getInstance(Context context) {
        if (instance == null) {
            synchronized (UserSp.class) {
                if (instance == null) {
                    instance = new UserSp(context);
                }
            }
        }
        return instance;
    }

    private UserSp(Context context) {
        super(context, SP_NAME);
    }

    // access_token
    public String getAccessToken(String defaultValue) {
        return getValue(KEY_ACCESS_TOKEN, defaultValue);
    }

    public void setAccessToken(String value) {
        setValue(KEY_ACCESS_TOKEN, value);
    }

    // access_token
    public long getExpiresIn(long defaultValue) {
        return getValue(KEY_EXPIRES_IN, defaultValue);
    }

    public void setExpiresIn(long value) {
        setValue(KEY_EXPIRES_IN, value);
    }

    // user_id
    public String getUserId(String defaultValue) {
        return getValue(KEY_USER_ID, defaultValue);
    }

    public void setUserId(String value) {
        setValue(KEY_USER_ID, value);
    }

    // telephone
    public String getTelephone(String defaultValue) {
        return getValue(KEY_TELEPHONE, defaultValue);
    }

    public void setTelephone(String value) {
        setValue(KEY_TELEPHONE, value);
    }

    // update
    public boolean isUpdate(boolean defaultValue) {
        return getValue(KEY_UPDATE, defaultValue);
    }

    public void setUpdate(boolean value) {
        setValue(KEY_UPDATE, value);
    }

    /* 注销登录时，将其他数据清空，只保留UserId这一个 */
    public void clearUserInfo() {
        // setValue(KEY_USER_ID, "");
        setTelephone("");
        setAccessToken("");
        // setUserId("");
        setExpiresIn(0);
        setUpdate(true);
    }
}
