package com.ydd.zhichat.ui.me.redpacket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.PreferenceUtils;

public class PasswordManagerActivity extends BaseActivity {

    RelativeLayout rl_change_pw,rl_forget_pw;
    TextView tv_change_pw;
    private boolean needOldPassword = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_manager);

        initActionBar();
        initView();
        initData();
    }
    private void initData() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        String userId = coreManager.getSelf().getUserId();
        // 如果没有设置过支付密码，就不需要输入旧密码，
        needOldPassword = PreferenceUtils.getBoolean(this, Constants.IS_PAY_PASSWORD_SET + userId, true);
        if(!needOldPassword){
            rl_forget_pw.setVisibility(View.GONE);
            tv_change_pw.setText("设置支付密码");
        }else {
            rl_forget_pw.setVisibility(View.VISIBLE);
        }
    }

    private void initView() {
        rl_change_pw = findViewById(R.id.rl_change_pw);
        rl_forget_pw = findViewById(R.id.rl_forget_pw);
        tv_change_pw = findViewById(R.id.tv_change_pw);

        rl_change_pw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PasswordManagerActivity.this, ChangePayPasswordActivity.class);
                startActivity(intent);
            }
        });
        rl_forget_pw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 忘记密码
                Intent intentToFind = new Intent(mContext, ForgetPayPasswordActivity.class);
                startActivity(intentToFind);
            }
        });
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText("支付密码设置");
    }
}
