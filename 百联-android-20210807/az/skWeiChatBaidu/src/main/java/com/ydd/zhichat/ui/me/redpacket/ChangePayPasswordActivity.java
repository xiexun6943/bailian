package com.ydd.zhichat.ui.me.redpacket;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.jungly.gridpasswordview.GridPasswordView;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.Md5Util;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import okhttp3.Call;

public class ChangePayPasswordActivity extends BaseActivity {

    private boolean needOldPassword = true;
    private boolean needTwice = true;

    private String oldPayPassword;
    private String newPayPassword;

    private TextView tvTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pay_password);

        initActionBar();
        initView();
        initData();
    }

    private void initData() {
        String userId = coreManager.getSelf().getUserId();
        if (TextUtils.isEmpty(userId)) {
            ToastUtil.showToast(this, R.string.tip_no_user_id);
            finish();
            return;
        }
        // 如果没有设置过支付密码，就不需要输入旧密码，
        needOldPassword = PreferenceUtils.getBoolean(this, Constants.IS_PAY_PASSWORD_SET + userId, true);
        Log.d(TAG, "initData: needOldPassword = " + needOldPassword);
        TextView tvTitle = findViewById(R.id.tv_title_center);
        TextView tvAction = findViewById(R.id.tvAction);
        ((TextView) findViewById(R.id.tv_title_center)).setText(getString(R.string.change_password));
        ((TextView) findViewById(R.id.tvAction)).setText(getString(R.string.btn_set_pay_password));
        if (!needOldPassword) {
            // 如果不需要旧密码，直接传空字符串，
            oldPayPassword = "";
            tvTip.setText(R.string.tip_change_pay_password_input_new);
            tvTitle.setText(R.string.btn_set_pay_password);
            tvAction.setText(R.string.btn_set_pay_password);
        } else {
            tvTitle.setText(R.string.btn_change_pay_password);
            tvAction.setText(R.string.btn_change_pay_password);
        }
    }

    private void initView() {
        tvTip = findViewById(R.id.tvTip);
        final TextView tvFinish = findViewById(R.id.tvFinish);
        tvFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.showDefaulteMessageProgressDialog(ChangePayPasswordActivity.this);
                HttpUtils.get().url(coreManager.getConfig().UPDATE_PAY_PASSWORD)
                        .params("access_token", coreManager.getSelfStatus().accessToken)
                        .params("oldPayPassword", Md5Util.toMD5(oldPayPassword))
                        .params("payPassword", Md5Util.toMD5(newPayPassword))
                        .build()
                        .execute(new BaseCallback<Void>(Void.class) {
                            @Override
                            public void onResponse(ObjectResult<Void> result) {
                                DialogHelper.dismissProgressDialog();
                                if (Result.checkSuccess(ChangePayPasswordActivity.this, result)) {
                                    // 成功，
                                    ToastUtil.showToast(ChangePayPasswordActivity.this, R.string.tip_change_pay_password_success);
                                    // 记录下支付密码已经设置，
                                    MyApplication.getInstance().initPayPassword(coreManager.getSelf().getUserId(), 1);
                                }
                                finish();
                            }

                            @Override
                            public void onError(Call call, Exception e) {
                                Reporter.post("修改支付密码接口调用失败，", e);
                                DialogHelper.dismissProgressDialog();
                                String reason = e.getMessage();
                                if (TextUtils.isEmpty(reason)) {
                                    // 提示网络异常，
                                    reason = getString(R.string.net_exception);
                                }
                                ToastUtil.showToast(ChangePayPasswordActivity.this, reason);
                                finish();
                            }
                        });
            }
        });
        final GridPasswordView gpvPassword = findViewById(R.id.gpvPassword);
        gpvPassword.setOnPasswordChangedListener(new GridPasswordView.OnPasswordChangedListener() {
            @Override
            public void onTextChanged(String psw) {
                tvFinish.setVisibility(View.GONE);
            }

            @Override
            public void onInputFinish(String psw) {
                if (needOldPassword) {
                    oldPayPassword = psw;
                    DialogHelper.showDefaulteMessageProgressDialog(ChangePayPasswordActivity.this);
                    HttpUtils.get().url(coreManager.getConfig().UPDATE_PAY_PASSWORD)
                            .params("access_token", coreManager.getSelfStatus().accessToken)
                            .params("oldPayPassword", Md5Util.toMD5(oldPayPassword))
                            .build()
                            .execute(new BaseCallback<Void>(Void.class) {
                                @Override
                                public void onResponse(ObjectResult<Void> result) {
                                    DialogHelper.dismissProgressDialog();
                                    gpvPassword.clearPassword();
                                    if (Result.checkSuccess(ChangePayPasswordActivity.this, result)) {
                                        needOldPassword = false;
                                        tvTip.setText(R.string.tip_change_pay_password_input_new);
                                    }
                                }

                                @Override
                                public void onError(Call call, Exception e) {
                                    Reporter.post("修改支付密码接口调用失败，", e);
                                    DialogHelper.dismissProgressDialog();
                                    String reason = e.getMessage();
                                    if (TextUtils.isEmpty(reason)) {
                                        // 提示网络异常，
                                        reason = getString(R.string.net_exception);
                                    }
                                    ToastUtil.showToast(ChangePayPasswordActivity.this, reason);
                                    finish();
                                }
                            });
                } else if (needTwice) {
                    needTwice = false;
                    newPayPassword = psw;
                    gpvPassword.clearPassword();
                    tvTip.setText(R.string.tip_change_pay_password_input_twice);
                } else if (psw.equals(newPayPassword)) {
                    // 二次确认成功，
                    tvFinish.setVisibility(View.VISIBLE);
                } else {
                    // 二次确认失败，重新输入新密码，
                    gpvPassword.clearPassword();
                    needTwice = true;
                    tvTip.setText(R.string.tip_change_pay_password_input_incorrect);
                    tvFinish.setVisibility(View.GONE);
                }
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
        mTvTitle.setText(getString(R.string.change_password));
    }
}
