package com.ydd.zhichat.xmpp.helloDemon;

import android.content.Context;
import android.util.Log;

import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.ui.base.CoreManager;
import com.ydd.zhichat.ui.notification.NotificationProxyActivity;
import com.ydd.zhichat.util.LogUtils;
import com.vivo.push.PushClient;
import com.vivo.push.model.UPSNotificationMessage;
import com.vivo.push.sdk.OpenClientPushMessageReceiver;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class VivoPushMessageReceiver extends OpenClientPushMessageReceiver {
    /**
     * TAG to Log
     */
    public static final String TAG = VivoPushMessageReceiver.class.getSimpleName();

    public static void init(Context ctx) {
        PushClient.getInstance(ctx).initialize();
        PushClient.getInstance(ctx).turnOnPush(state -> {
            if (state != 0) {
                Log.e(TAG, "vivo push: 打开push异常[" + state + "]");
            } else {
                String regId = PushClient.getInstance(ctx).getRegId();
                // 第一次打开推送时可能重复调用上传regId，但是无所谓，
                putRegId(CoreManager.requireSelfStatus(ctx).accessToken, regId);
            }
        });
    }

    public static void putRegId(String accessToken, String regId) {
        Log.d(TAG, "putRegId() called with: accessToken = [" + accessToken + "], regId = [" + regId + "]");
        String at = accessToken;
        if (at == null) {
            Reporter.post("access token is null");
        } else {
            HttpUtils.post()
                    .url(CoreManager.requireConfig(MyApplication.getInstance()).configVi)
                    .params("pushId", regId)
                    .params("access_token", at)
                    // devicesId后端没有用上，但是沿用旧接口的参数列表带上这个，实际没用，
                    .params("deviceId", "5")
                    .build()
                    .execute(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Reporter.post("上传vivo regId失败，", e);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            Log.i(TAG, "上传vivo regId，onResponse: status = " + response.code());
                        }
                    });
        }
    }

    @Override
    public void onNotificationMessageClicked(Context context, UPSNotificationMessage msg) {
        String customContentString = msg.getSkipContent();
        String notifyString = "通知点击 msgId " + msg.getMsgId() + " ;customContent=" + customContentString;
        Log.d(TAG, notifyString);
        LogUtils.log(msg);
        NotificationProxyActivity.start(context, msg.getParams());
    }

    @Override
    public void onReceiveRegId(Context context, String regId) {
        String responseString = "push vivo regId = " + regId;
        Log.e(TAG, responseString);

        putRegId(CoreManager.requireSelfStatus(context).accessToken, regId);
    }
}
