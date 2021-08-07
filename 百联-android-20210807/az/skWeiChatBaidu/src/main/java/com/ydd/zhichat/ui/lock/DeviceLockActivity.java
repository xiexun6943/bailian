package com.ydd.zhichat.ui.lock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.jungly.gridpasswordview.GridPasswordView;
import com.ydd.zhichat.R;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.ui.base.BaseActivity;

public class DeviceLockActivity extends BaseActivity {
    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, DeviceLockActivity.class);
        ctx.startActivity(intent);
    }

    public static void verify(Activity ctx, int requestCode) {
        Intent intent = new Intent(ctx, DeviceLockActivity.class);
        ctx.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_lock);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setVisibility(View.GONE);

        GridPasswordView gpvPassword = findViewById(R.id.gpvPassword);
        gpvPassword.setOnPasswordChangedListener(new GridPasswordView.OnPasswordChangedListener() {
            @Override
            public void onTextChanged(String psw) {

            }

            @Override
            public void onInputFinish(String psw) {
                if (DeviceLockHelper.checkPassword(psw)) {
                    setResult(Activity.RESULT_OK);
                    DeviceLockHelper.unlock();
                    finish();
                } else {
                    gpvPassword.clearPassword();
                    DialogHelper.tip(DeviceLockActivity.this, getString(R.string.tip_device_lock_password_incorrect));
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
