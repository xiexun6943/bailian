package com.ydd.zhichat.ui.me;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ydd.zhichat.AppConfig;
import com.ydd.zhichat.R;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.DeviceInfoUtil;
import com.ydd.zhichat.view.SharePopupWindow;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JXAboutVC_AboutUS"));
        ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.share);
        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharePopupWindow mSharePopupWindow = new SharePopupWindow(AboutActivity.this);
                mSharePopupWindow.showAtLocation(findViewById(R.id.about), Gravity.BOTTOM, 0, 0);
            }
        });

        TextView versionTv = (TextView) findViewById(R.id.version_tv);
        versionTv.setText(getString(R.string.app_name) + " " + DeviceInfoUtil.getVersionName(mContext));

        TextView tvCompany = findViewById(R.id.company_tv);
        TextView tvCopyright = findViewById(R.id.copy_right_tv);

        tvCompany.setText(coreManager.getConfig().companyName);
        tvCopyright.setText(coreManager.getConfig().copyright);

        if (!AppConfig.isShiku()) {
            tvCompany.setVisibility(View.GONE);
            tvCopyright.setVisibility(View.GONE);
            ivRight.setVisibility(View.GONE);
        }
    }
}
