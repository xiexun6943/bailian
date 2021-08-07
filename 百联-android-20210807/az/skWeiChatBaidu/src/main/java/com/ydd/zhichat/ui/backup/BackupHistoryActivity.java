package com.ydd.zhichat.ui.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.EventSentChatHistory;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.EventBusHelper;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

public class BackupHistoryActivity extends BaseActivity {

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, BackupHistoryActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_history);

        initActionBar();

        findViewById(R.id.btnSelectChat).setOnClickListener((v) -> {
            SelectChatActivity.start(this);
        });
        EventBusHelper.register(this);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventSentChatHistory message) {
        finish();
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        findViewById(R.id.iv_title_left).setOnClickListener((v) -> {
            onBackPressed();
        });
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.backup_chat_history));
    }
}
