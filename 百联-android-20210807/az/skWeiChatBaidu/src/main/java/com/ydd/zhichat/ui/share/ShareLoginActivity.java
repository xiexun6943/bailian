package com.ydd.zhichat.ui.share;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.LoginRegisterResult;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.helper.LoginHelper;
import com.ydd.zhichat.helper.PrivacySettingHelper;
import com.ydd.zhichat.ui.account.DataDownloadActivity;
import com.ydd.zhichat.ui.account.SelectPrefixActivity;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.DeviceInfoUtil;
import com.ydd.zhichat.util.Md5Util;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.volley.Result;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 分享 登录
 * 本地未登录需要先登录在分享或授权
 */
public class ShareLoginActivity extends BaseActivity implements View.OnClickListener {
    private EditText mPhoneNumberEdit;
    private TextView tv_prefix;
    private int mobilePrefix = 86;
    private EditText mPasswordEdit;

    public ShareLoginActivity() {
        noLoginRequired();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initActionBar();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!MyApplication.getInstance().getBdLocationHelper().isLocationUpdate()) {
            MyApplication.getInstance().getBdLocationHelper().requestLocation();
        }
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
        tvTitle.setText(getString(R.string.login));
    }

    private void initView() {
        mPhoneNumberEdit = (EditText) findViewById(R.id.phone_numer_edit);
        mPhoneNumberEdit.setHint(InternationalizationHelper.getString("JX_InputPhone"));

        tv_prefix = (TextView) findViewById(R.id.tv_prefix);
        tv_prefix.setOnClickListener(this);
        mobilePrefix = PreferenceUtils.getInt(this, Constants.AREA_CODE_KEY, mobilePrefix);
        tv_prefix.setText("+" + mobilePrefix);

        mPasswordEdit = (EditText) findViewById(R.id.psw_edit);

        Button loginBtn = (Button) findViewById(R.id.login_btn);
        loginBtn.setOnClickListener(this);
        loginBtn.setText(InternationalizationHelper.getString("JX_Login"));

        findViewById(R.id.forget_password_btn).setVisibility(View.GONE);
        findViewById(R.id.register_account_btn).setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_prefix:
                // 选择国家区号
                Intent intent = new Intent(this, SelectPrefixActivity.class);
                startActivityForResult(intent, SelectPrefixActivity.REQUEST_MOBILE_PREFIX_LOGIN);
                break;
            case R.id.login_btn:
                // 登陆
                login();
                break;
        }
    }

    private void login() {
        PreferenceUtils.putInt(this, Constants.AREA_CODE_KEY, mobilePrefix);
        final String phoneNumber = mPhoneNumberEdit.getText().toString().trim();
        String password = mPasswordEdit.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(mContext, InternationalizationHelper.getString("JX_InputPhone"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(mContext, InternationalizationHelper.getString("JX_InputPassWord"), Toast.LENGTH_SHORT).show();
            return;
        }
        // 加密之后的密码
        final String digestPwd = Md5Util.toMD5(password);
        // 加密后的手机号码
        String digestPhoneNumber = Md5Util.toMD5(phoneNumber);

        DialogHelper.showDefaulteMessageProgressDialog(this);

        Map<String, String> params = new HashMap<>();
        params.put("telephone", digestPhoneNumber);          // 账号登陆的时候需要MD5加密，服务器需求
        params.put("areaCode", String.valueOf(mobilePrefix));// 账号登陆的时候需要MD5加密，服务器需求
        params.put("password", digestPwd);
        params.put("xmppVersion", "1");
        // 附加信息+
        params.put("model", DeviceInfoUtil.getModel());
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        params.put("serial", DeviceInfoUtil.getDeviceId(mContext));
        // 地址信息
        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        if (latitude != 0)
            params.put("latitude", String.valueOf(latitude));
        if (longitude != 0)
            params.put("longitude", String.valueOf(longitude));

        if (MyApplication.IS_OPEN_CLUSTER) {// 服务端集群需要
            String area = PreferenceUtils.getString(this, AppConstant.EXTRA_CLUSTER_AREA);
            if (!TextUtils.isEmpty(area)) {
                params.put("area", area);
            }
        }

        HttpUtils.get().url(coreManager.getConfig().USER_LOGIN)
                .params(params)
                .build()
                .execute(new BaseCallback<LoginRegisterResult>(LoginRegisterResult.class) {

                    @Override
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ObjectResult<LoginRegisterResult> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result == null) {
                            ToastUtil.showErrorData(mContext);
                            return;
                        }
                        boolean success = false;
                        if (result.getResultCode() == Result.CODE_SUCCESS) {
                            success = LoginHelper.setLoginUser(mContext, coreManager, phoneNumber, digestPwd, result);// 设置登陆用户信息
                        }
                        if (success) {
                            LoginRegisterResult.Settings settings = result.getData().getSettings();
                            MyApplication.getInstance().initPayPassword(result.getData().getUserId(), result.getData().getPayPassword());
                            PrivacySettingHelper.setPrivacySettings(ShareLoginActivity.this, settings);
                            MyApplication.getInstance().initMulti();

                            // startActivity(new Intent(mContext, DataDownloadActivity.class));
                            DataDownloadActivity.start(mContext, result.getData().getIsupdate());
                            finish();
                        } else {
                            // 登录失败
                            String message = TextUtils.isEmpty(result.getResultMsg()) ? InternationalizationHelper.getString("JX_PasswordFiled") : result.getResultMsg();
                            ToastUtil.showToast(mContext, message);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != SelectPrefixActivity.RESULT_MOBILE_PREFIX_SUCCESS)
            return;
        mobilePrefix = data.getIntExtra(Constants.MOBILE_PREFIX, 86);
        tv_prefix.setText("+" + mobilePrefix);
    }
}
