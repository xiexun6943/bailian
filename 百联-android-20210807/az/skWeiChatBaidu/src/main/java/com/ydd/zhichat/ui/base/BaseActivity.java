package com.ydd.zhichat.ui.base;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

public abstract class BaseActivity extends BaseLoginActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);// 竖屏
    }
}
