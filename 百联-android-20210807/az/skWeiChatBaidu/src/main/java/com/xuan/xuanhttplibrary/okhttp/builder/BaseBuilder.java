package com.xuan.xuanhttplibrary.okhttp.builder;


import android.text.TextUtils;
import android.util.Log;

import com.ydd.zhichat.AppConfig;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.bean.UserStatus;
import com.ydd.zhichat.ui.base.CoreManager;
import com.ydd.zhichat.util.Md5Util;
import com.ydd.zhichat.util.TimeUtils;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;

import java.util.Locale;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * @author Administrator
 * @time 2017/3/30 0:14
 * @des ${TODO}
 */

public abstract class BaseBuilder {

    protected String url;
    protected Object tag;
    protected Map<String, String> headers;
    protected Map<String, String> params;
    protected Request build;

    public abstract BaseBuilder url(String url);

    public abstract BaseBuilder tag(Object tag);

    public abstract BaseCall build();

    public abstract BaseBuilder params(String k, String v);

    /**
     * @return 返回true表示accessToken正常，
     */
    private boolean checkAccessToken(UserStatus status) {
        String mAccessToken;
        // 如果没有accessToken就不添加time和secret,
        if (status == null) {
            return false;
        } else {
            mAccessToken = status.accessToken;
            if (TextUtils.isEmpty(mAccessToken)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 给所有接口调添加secret,
     */
    public BaseBuilder addSecret() {
        if (url.contains("config")// 获取配置的接口，不验证
                || url.contains("getCurrentTime") // 获取服务器时间接口，不验证，
                ) {
            return this;
        }
        // 上面两个接口调用时可能还没有获取到config,
        AppConfig mConfig = CoreManager.requireConfig(MyApplication.getInstance());
        if (url.equals(mConfig.SDK_OPEN_AUTH_INTERFACE)) {
            return this;
        }

        // 所有接口都需要time与secret参数
        String time = String.valueOf(TimeUtils.sk_time_current_time());
        String secret;
        UserStatus status = CoreManager.getSelfStatus(MyApplication.getContext());
        if (url.equals(mConfig.VX_RECHARGE)
                || url.equals(mConfig.REDPACKET_OPEN)
                || url.equals(mConfig.SKTRANSFER_RECEIVE_TRANSFER)) {
            // 微信支付 领红包 领取转账 调用的接口
            if (!checkAccessToken(status)) {
                return this;
            }
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            String step1 = Md5Util.toMD5(AppConfig.apiKey + time);
            secret = Md5Util.toMD5(step1 + mLoginUserId + status.accessToken);
        } else if (url.equals(mConfig.USER_LOGIN)
                || url.equals(mConfig.USER_REGISTER)
                || url.equals(mConfig.USER_PASSWORD_RESET)
                || url.equals(mConfig.VERIFY_TELEPHONE)
                || url.equals(mConfig.USER_GETCODE_IMAGE)
                // 未登录之前 && 微信登录相关 调用的接口
                || url.equals(mConfig.VX_GET_OPEN_ID) || url.equals(mConfig.USER_THIRD_LOGIN) || url.equals(mConfig.USER_THIRD_BIND) || url.equals(mConfig.USER_THIRD_REGISTER) || url.equals(mConfig.SEND_AUTH_CODE)) {
            secret = Md5Util.toMD5(AppConfig.apiKey + time);
        } else {
            // 其他接口
            if (!checkAccessToken(status)) {
                return this;
            }
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            secret = Md5Util.toMD5(AppConfig.apiKey + time + mLoginUserId + status.accessToken);
        }

        params("time", time);
        params("secret", secret);

        return this;
    }

    /**
     * 给需要支付密码的接口调添加secret,
     *
     * @param payPassword 支付密码
     */
    public BaseBuilder addSecret(String payPassword, String money) {
        AppConfig mConfig = CoreManager.requireConfig(MyApplication.getInstance());

        // 所有接口都需要time与secret参数
        String time = String.valueOf(TimeUtils.sk_time_current_time());
        String secret;
        String mAccessToken = CoreManager.requireSelfStatus(MyApplication.getInstance()).accessToken;
        if (url.equals(mConfig.REDPACKET_SEND)
                || url.equals(mConfig.SKTRANSFER_SEND_TRANSFER)) {
            // 发红包 || 转账 调用的接口
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            String step1 = Md5Util.toMD5(AppConfig.apiKey + time + money);
            String step2 = Md5Util.toMD5(payPassword);
            secret = Md5Util.toMD5(step1 + mLoginUserId + mAccessToken + step2);
            Log.d(HttpUtils.TAG, String.format(Locale.CHINA, "addSecret: md5(md5(%s+%s+%s)+%s+%s+md5(%s)) = %s", AppConfig.apiKey, time, money, mLoginUserId, mAccessToken, payPassword, secret));
        } else {
            // 不走这里，
            Reporter.unreachable();
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            secret = Md5Util.toMD5(AppConfig.apiKey + time + mLoginUserId + mAccessToken);
        }
        /*
        提现接口的secret计算在外面，
        com.client.yanchat.wxapi.WXEntryActivity.transfer
         */
        params("time", time);
        params("secret", secret);
        return this;
    }

    /**
     * 给收付款接口进行加密,
     *
     * @param payStr
     */
    public BaseBuilder addSecret2(String payStr, String money) {
        AppConfig mConfig = CoreManager.requireConfig(MyApplication.getInstance());

        // 所有接口都需要time与secret参数
        String time = String.valueOf(TimeUtils.sk_time_current_time());
        String secret;
        String mAccessToken = CoreManager.requireSelfStatus(MyApplication.getInstance()).accessToken;
        if (url.equals(mConfig.PAY_CODE_PAYMENT)) {
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            String step1 = Md5Util.toMD5(AppConfig.apiKey + time + money + payStr);
            secret = Md5Util.toMD5(step1 + mLoginUserId + mAccessToken);
        } else if (url.equals(mConfig.PAY_CODE_RECEIPT)) {
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            String step1 = Md5Util.toMD5(AppConfig.apiKey + time + money + Md5Util.toMD5(payStr));
            secret = Md5Util.toMD5(step1 + mLoginUserId + mAccessToken);
        } else if (url.equals(mConfig.PAY_PASSWORD_PAYMENT)) {
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            String step1 = Md5Util.toMD5(AppConfig.apiKey + time + Md5Util.toMD5(payStr));
            secret = Md5Util.toMD5(mLoginUserId + mAccessToken + step1);
        } else {
            // 不走这里，
            Reporter.unreachable();
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            secret = Md5Util.toMD5(AppConfig.apiKey + time + mLoginUserId + mAccessToken);
        }
        /*
        提现接口的secret计算在外面，
        com.client.yanchat.wxapi.WXEntryActivity.transfer
         */
        params("time", time);
        params("secret", secret);
        return this;
    }

    public class BaseCall {
        public void execute(Callback callback) {
            OkHttpClient mOkHttpClient = HttpUtils.getInstance().getOkHttpClient();
            mOkHttpClient.newCall(build).enqueue(callback);
        }
    }
}
