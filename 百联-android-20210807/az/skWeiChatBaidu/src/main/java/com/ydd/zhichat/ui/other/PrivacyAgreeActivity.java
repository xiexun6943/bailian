package com.ydd.zhichat.ui.other;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.adapter.MessageLogin;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.EventBusHelper;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.view.SelectionFrame;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

import static com.ydd.zhichat.AppConfig.XIEYI_URL;

/**
 * 请求同意隐私政策的页面，
 */
public class PrivacyAgreeActivity extends BaseActivity {
    private WebView mWebView;
    private TextView mTitleTv;
    private ImageView mTitleLeftIv;

    public PrivacyAgreeActivity() {
        noConfigRequired();
        noLoginRequired();
    }

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, PrivacyAgreeActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_agree);
        initActionBar();
        EventBusHelper.register(this);
        mWebView = (WebView) findViewById(R.id.mWebView);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }
        });
//        String prefix = coreManager.getConfig().privacyPolicyPrefix;
//        if (TextUtils.isEmpty(prefix)) {
//            PreferenceUtils.putBoolean(mContext, Constants.PRIVACY_AGREE_STATUS, true);
//            finish();
//            return;
//        }
//        String language = Locale.getDefault().getLanguage();
//        if (language.startsWith("zh")) {
//            language = "zh";
//        } else {
//            language = "en";
//        }
//        String url = prefix + language + ".html";
        String url = XIEYI_URL;
        mWebView.loadUrl(url);

        findViewById(R.id.btnAgree).setOnClickListener((v) -> {
            PreferenceUtils.putBoolean(mContext, Constants.PRIVACY_AGREE_STATUS, true);
            finish();
        });

        findViewById(R.id.btnDisagree).setOnClickListener((v) -> {
            disagree();
        });

    }

    private void disagree() {
        SelectionFrame selectionFrame = new SelectionFrame(mContext);
        selectionFrame.setSomething(null, getString(R.string.tip_privacy_can_not_disagree),
                getString(R.string.btn_privacy_disagree), getString(R.string.btn_privacy_re_reading),
                new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {
                        // 关闭Splash页面和当前页面，
                        EventBus.getDefault().post(new MessageLogin());
                    }

                    @Override
                    public void confirmClick() {
                    }
                });
        selectionFrame.show();
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disagree();
            }
        });
        mTitleTv = findViewById(R.id.tv_title_center);
        mTitleTv.setText(R.string.title_privacy_policy);
        mTitleLeftIv = findViewById(R.id.iv_title_left);
        mTitleLeftIv.setImageResource(R.drawable.icon_close);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }
}
