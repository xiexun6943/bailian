package com.ydd.zhichat.push;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import cn.jpush.android.api.JPushInterface;

public class YanChatJpushReceive extends BroadcastReceiver {
    private NotificationManager manager;

    @Override
    public void onReceive(final Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        Log.e("YanChatJpushReceive","onReceive - " + intent.getAction()
                + "\n regId: " + bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID));

//        String regId = bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID);
//        String regId = JPushInterface.getRegistrationID(context);
//
//        Map<String, String> params = new HashMap<>();
//        params.put("access_token", CoreManager.requireSelfStatus(context).accessToken);
//        params.put("regId", regId);
//        params.put("deviceId", "3");
//        params.put("appId", CoreManager.requireSelfStatus(context).accessToken);
//        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).configJg)
//                .params(params)
//                .build()
//                .execute(new BaseCallback<Void>(Void.class) {
//
//                    @Override
//                    public void onResponse(ObjectResult<Void> result) {
//                        Log.e("push", "上传成功");
//
//                    }
//
//                    @Override
//                    public void onError(Call call, Exception e) {
//                        Log.e("push", "上传失败");
//                    }
//                });
    }
}
