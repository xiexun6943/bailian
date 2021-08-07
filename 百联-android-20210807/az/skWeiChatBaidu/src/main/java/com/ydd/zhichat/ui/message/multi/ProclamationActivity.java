package com.ydd.zhichat.ui.message.multi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.ui.base.BaseActivity;

import static com.ydd.zhichat.AppConstant.NOTICE_ID;

public class ProclamationActivity extends BaseActivity {

    private EditText et_proclamation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_proclamation);
        initActionBar();
        initView();
    }

    private void initView() {
        et_proclamation = (EditText) findViewById(R.id.et_proclamation);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(R.string.group_bulletin);
        TextView mTvTitleRight = (TextView) findViewById(R.id.tv_title_right);
        mTvTitleRight.setText(R.string.btn_public);
        mTvTitleRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                String id = getIntent().getStringExtra("noticeId");
                if (!TextUtils.isEmpty(id)) {
                    intent.putExtra("proclamation", et_proclamation.getText().toString());
                    intent.putExtra("noticeId", id);
                    setResult(NOTICE_ID, intent);
                } else {
                    intent.putExtra("proclamation", et_proclamation.getText().toString());
                    setResult(RESULT_OK, intent);
                }
                finish();
            }
        });
    }
}
