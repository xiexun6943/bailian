package com.ydd.zhichat.pay;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.qrcode.utils.CommonUtils;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.DisplayUtil;
import com.ydd.zhichat.util.ScreenUtil;

import java.util.Random;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

/**
 * 付款码
 */
public class PaymentActivity extends BaseActivity {

    private ImageView mPayQrCodeIv;
    private ImageView mPayBarCodeIv;
    // 每间隔一分钟刷新一次付款码
    private CountDownTimer mCodeRefreshCountDownTimer = new CountDownTimer(60000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            refreshPaymentCode();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initActionBar();
        initView();
        initEvent();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        TextView titleTv = findViewById(R.id.tv_title_center);
        titleTv.setText(getString(R.string.receipt_payment));
    }

    private void initView() {
        mPayQrCodeIv = findViewById(R.id.pm_qr_code_iv);
        mPayBarCodeIv = findViewById(R.id.pm_bar_code_iv);
        refreshPaymentCode();
    }

    private void initEvent() {
/*
        mPayQrCodeIv.setOnClickListener(v -> {// 刷新付款码
            refreshPaymentCode();
        });
*/

        findViewById(R.id.go_receipt_ll).setOnClickListener(v -> startActivity(new Intent(mContext, ReceiptActivity.class)));
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventPaymentSuccess message) {
        DialogHelper.tip(mContext, getString(R.string.receipted, message.getReceiptName()));
        refreshPaymentCode();
    }

    private void refreshPaymentCode() {
        mCodeRefreshCountDownTimer.cancel();
        mCodeRefreshCountDownTimer.start();

        String code = generateReceiptCode();

        Bitmap bitmap1 = CommonUtils.createQRCode(code, DisplayUtil.dip2px(MyApplication.getContext(), 160),
                DisplayUtil.dip2px(MyApplication.getContext(), 160));
        Bitmap bitmap2 = CommonUtils.createBarCode(code, ScreenUtil.getScreenWidth(MyApplication.getContext()) - DisplayUtil.dip2px(MyApplication.getContext(), 40),
                DisplayUtil.dip2px(MyApplication.getContext(), 80));
        mPayQrCodeIv.setImageBitmap(bitmap1);
        mPayBarCodeIv.setImageBitmap(bitmap2);
    }

    private String generateReceiptCode() {
        /**
         *  规则
         *  (userId+n+opt)的长度+(userId+n+opt)+opt+(time/opt)
         */
        String barCode;
        int type = 1;// 支付类型  1：账户余额    2：银行卡1  3：银行卡2  4：银行卡3  ....

        int n = 9;
        int userId = Integer.valueOf(coreManager.getSelf().getUserId());
        String accessToken = coreManager.getSelfStatus().accessToken;
        long time = System.currentTimeMillis() / 1000;

        // byte[] sha = DigestUtils.sha(accessToken + time + AppConfig.apiKey);
        // int opt = Math.abs(sha[0]);
        Random random = new Random();
        int opt = random.nextInt(100) + 100;

        String userCode = String.valueOf(userId + n + opt);
        int userCodeLen = userCode.length();
        barCode = String.valueOf(userCodeLen) + userCode + String.valueOf(opt);

        long timeCode = (time / opt);
        if (String.valueOf(timeCode).length() < 8) {
            timeCode = (time / (opt - 100));
        }
        barCode += String.valueOf(timeCode);
        Log.e("Payment", "opt-->" + opt);
        Log.e("Payment", "userId-->" + userId);
        Log.e("Payment", "time-->" + time);
        Log.e("Payment", barCode);
        Log.e("Payment", "Len-->" + barCode.length());
        return barCode;
    }
}
