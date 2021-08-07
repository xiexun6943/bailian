package com.ydd.zhichat;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ydd.zhichat.ui.SplashActivity;
import com.ydd.zhichat.ui.base.ActivityStack;

public class RestartService extends Service {
    private boolean IS_SERVICE;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                Log.d("zxzxzx", "handleMessage: ");
                Intent intent = new Intent(RestartService.this, SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                ActivityStack.getInstance().exit();
                MyApplication.getInstance().destoryRestart();
                stopSelf();
            }

        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message message = new Message();
        message.what = 1;
        Log.d("zxzxzx", "onStartCommand: ");

        handler.sendMessageDelayed(message, 100);
        return super.onStartCommand(intent, flags, startId);
    }

}
