package com.ydd.zhichat.ui.me;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.lock.ChangeDeviceLockPasswordActivity;
import com.ydd.zhichat.ui.lock.DeviceLockActivity;
import com.ydd.zhichat.ui.lock.DeviceLockHelper;
import com.suke.widget.SwitchButton;

public class SecureSettingActivity extends BaseActivity {

    public static final int REQUEST_DISABLE_LOCK = 1;
    private SwitchButton sbDeviceLock;
    private SwitchButton sbDeviceLockFree;
    private View llDeviceLockDetail;
    private View rlChangeDeviceLockPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secure_setting);
        initActionBar();
        initView();

        sbDeviceLock.setOnCheckedChangeListener((view, isChecked) -> {
            if (!isChecked) {
                DeviceLockActivity.verify(this, REQUEST_DISABLE_LOCK);
                return;
            }
            rlChangeDeviceLockPassword.setVisibility(View.VISIBLE);
            ChangeDeviceLockPasswordActivity.start(this);
        });
        rlChangeDeviceLockPassword.setOnClickListener(v -> {
            ChangeDeviceLockPasswordActivity.start(this);
        });
        sbDeviceLockFree.setChecked(DeviceLockHelper.isAutoLock());
        sbDeviceLockFree.setOnCheckedChangeListener((view, isChecked) -> {
            DeviceLockHelper.setAutoLock(isChecked);
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDeviceLockSettings();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        switch (requestCode) {
            case REQUEST_DISABLE_LOCK:
                DeviceLockHelper.clearPassword();
                updateDeviceLockSettings();
                break;
        }

    }

    private void updateDeviceLockSettings() {
        boolean enabled = DeviceLockHelper.isEnabled();
        sbDeviceLock.setChecked(enabled);
        if (enabled) {
            llDeviceLockDetail.setVisibility(View.VISIBLE);
        } else {
            llDeviceLockDetail.setVisibility(View.GONE);
        }
        boolean autoLock = DeviceLockHelper.isAutoLock();
        sbDeviceLockFree.setChecked(autoLock);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.secure_settings);
    }

    private void initView() {
        sbDeviceLock = findViewById(R.id.sbDeviceLock);
        sbDeviceLockFree = findViewById(R.id.sbDeviceLockFree);
        llDeviceLockDetail = findViewById(R.id.llDeviceLockDetail);
        rlChangeDeviceLockPassword = findViewById(R.id.rlChangeDeviceLockPassword);
    }
}
