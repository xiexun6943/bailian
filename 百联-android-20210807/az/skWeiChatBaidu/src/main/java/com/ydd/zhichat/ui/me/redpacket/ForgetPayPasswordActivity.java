package com.ydd.zhichat.ui.me.redpacket;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.Code;
import com.ydd.zhichat.bean.User;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.db.dao.UserDao;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.sp.UserSp;
import com.ydd.zhichat.ui.account.SelectPrefixActivity;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.Md5Util;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.util.StringUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.util.ViewPiexlUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.JsonCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;

public class ForgetPayPasswordActivity extends BaseActivity implements View.OnClickListener {
    private Button btn_getCode, btn_change;
    private EditText mPhoneNumberEdit;
    private EditText mPasswordEdit, mConfigPasswordEdit, mAuthCodeEdit;
    private TextView tv_prefix;
    private LinearLayout lin_select;
    private int mobilePrefix = 86;
    private String mobileCountry = "中国";
    // 驗證碼
    private String randcode;
    // 图形验证码
    private EditText mImageCodeEdit;
    private ImageView mImageCodeIv;
    private ImageView mRefreshIv;
    private int reckonTime = 60;
    private Handler mReckonHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0x1) {
                btn_getCode.setText("(" + reckonTime + ")");
                reckonTime--;
                if (reckonTime < 0) {
                    mReckonHandler.sendEmptyMessage(0x2);
                } else {
                    mReckonHandler.sendEmptyMessageDelayed(0x1, 1000);
                }
            } else if (msg.what == 0x2) {
                // 60秒结束
                btn_getCode.setText(InternationalizationHelper.getString("JX_Send"));
                btn_getCode.setEnabled(true);
                reckonTime = 60;
            }
        }
    };

    public ForgetPayPasswordActivity() {
        noLoginRequired();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_pay_password);
        initView();
    }

    private void initView() {
        mobileCountry = PreferenceUtils.getString(this, Constants.COUNTRY_NANE, mobileCountry);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);

        tv_prefix = (TextView) findViewById(R.id.tv_prefix);
        lin_select = findViewById(R.id.lin_select);
        tv_prefix.setOnClickListener(this);
        lin_select.setOnClickListener(this);
        mobilePrefix = PreferenceUtils.getInt(this, Constants.AREA_CODE_KEY, mobilePrefix);
        tv_prefix.setText(mobileCountry+"(+" + mobilePrefix+")");

        btn_getCode = (Button) findViewById(R.id.send_again_btn);
//        btn_getCode.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        btn_getCode.setOnClickListener(this);
        btn_change = (Button) findViewById(R.id.login_btn);
//        btn_change.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        btn_change.setOnClickListener(this);

        mPhoneNumberEdit = (EditText) findViewById(R.id.phone_numer_edit);
        tvTitle.setText(InternationalizationHelper.getString("JX_ForgetPassWord"));
        if (coreManager.getSelf() != null && !TextUtils.isEmpty(coreManager.getSelf().getTelephone())) {
            String telephone = coreManager.getSelf().getTelephone();
            String prefix = String.valueOf(mobilePrefix);
            if (telephone.startsWith(prefix)) {
                telephone = telephone.substring(prefix.length());
            }
            mPhoneNumberEdit.setText(telephone);
        } else {
            String userId = UserSp.getInstance(this).getUserId("");
            if (!TextUtils.isEmpty(userId)) {
                User mLastLoginUser = UserDao.getInstance().getUserByUserId(userId);
                if (mLastLoginUser != null) {
                    String phoneNumber = mLastLoginUser.getTelephone();
                    int mobilePrefix = PreferenceUtils.getInt(ForgetPayPasswordActivity.this, Constants.AREA_CODE_KEY, -1);
                    String sPrefix = String.valueOf(mobilePrefix);
                    // 删除开头的区号，
                    if (phoneNumber.startsWith(sPrefix)) {
                        phoneNumber = phoneNumber.substring(sPrefix.length());
                    }
                    mPhoneNumberEdit.setText(phoneNumber);
                }
            }
        }

        mPasswordEdit = (EditText) findViewById(R.id.psw_edit);
//        PayPasswordHelper.bindPasswordEye(mPasswordEdit, findViewById(R.id.tbEye));
        mConfigPasswordEdit = (EditText) findViewById(R.id.confirm_psw_edit);
//        PayPasswordHelper.bindPasswordEye(mConfigPasswordEdit, findViewById(R.id.tbEyeConfirm));
        mImageCodeEdit = (EditText) findViewById(R.id.image_tv);
        mAuthCodeEdit = (EditText) findViewById(R.id.auth_code_edit);
        List<EditText> mEditList = new ArrayList<>();
        mEditList.add(mPasswordEdit);
        mEditList.add(mConfigPasswordEdit);
        mEditList.add(mImageCodeEdit);
        mEditList.add(mAuthCodeEdit);
        setBound(mEditList);

        mImageCodeIv = (ImageView) findViewById(R.id.image_iv);
        mRefreshIv = (ImageView) findViewById(R.id.image_iv_refresh);
        mRefreshIv.setOnClickListener(this);

        mPhoneNumberEdit.setHint(InternationalizationHelper.getString("JX_InputPhone"));
        mAuthCodeEdit.setHint(InternationalizationHelper.getString("ENTER_VERIFICATION_CODE"));
        mPasswordEdit.setHint(InternationalizationHelper.getString("JX_InputNewPassWord"));
        mConfigPasswordEdit.setHint(InternationalizationHelper.getString("JX_ConfirmNewPassWord"));
        btn_change.setText(InternationalizationHelper.getString("JX_UpdatePassWord"));

        // 请求图形验证码
        if (!TextUtils.isEmpty(mPhoneNumberEdit.getText().toString())) {
            requestImageCode();
        }
    }

    public void setBound(List<EditText> mEditList) {// 为Edit内的drawableLeft设置大小
        for (int i = 0; i < mEditList.size(); i++) {
            Drawable[] compoundDrawable = mEditList.get(i).getCompoundDrawables();
            Drawable drawable = compoundDrawable[0];
            if (drawable != null) {
                drawable.setBounds(0, 0, ViewPiexlUtil.dp2px(this, 20), ViewPiexlUtil.dp2px(this, 20));
                mEditList.get(i).setCompoundDrawables(drawable, null, null, null);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.lin_select:
                // 选择国家区号
                Intent intent = new Intent(this, SelectPrefixActivity.class);
                startActivityForResult(intent, SelectPrefixActivity.REQUEST_MOBILE_PREFIX_LOGIN);
                break;
            case R.id.tv_prefix:
                // 选择国家区号
//                Intent intent = new Intent(this, SelectPrefixActivity.class);
//                startActivityForResult(intent, SelectPrefixActivity.REQUEST_MOBILE_PREFIX_LOGIN);
                break;
            case R.id.image_iv_refresh:
                if (TextUtils.isEmpty(mPhoneNumberEdit.getText().toString())) {
                    ToastUtil.showToast(this, getString(R.string.tip_phone_number_empty_request_verification_code));
                } else {
                    requestImageCode();
                }
                break;
            case R.id.send_again_btn:
                // 获取验证码
                String phoneNumber = mPhoneNumberEdit.getText().toString().trim();
                String imagecode = mImageCodeEdit.getText().toString().trim();
                if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(imagecode)) {
                    ToastUtil.showToast(mContext, getString(R.string.tip_phone_number_verification_code_empty));
                    return;
                }
                verifyTelephone(phoneNumber, imagecode);
                break;
            case R.id.login_btn:
                // 获取验证码
                phoneNumber = mPhoneNumberEdit.getText().toString().trim();
                imagecode = mImageCodeEdit.getText().toString().trim();
                if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(imagecode)) {
                    ToastUtil.showToast(mContext, getString(R.string.tip_phone_number_verification_code_empty));
                    return;
                }
                if (!configPassword()) {// 两次密码是否一致
                    return;
                }

                // 判断新密码是否与老密码一致
                String mNewPassword = mPasswordEdit.getText().toString().trim();
                String oldPassword = null;
                if (coreManager.getSelf() != null) {
                    oldPassword = coreManager.getSelf().getPassword();
                }
                if (Md5Util.toMD5(mNewPassword).equals(oldPassword)) {
                    ToastUtil.showToast(mContext, getString(R.string.tip_password_no_change));
                    return;
                }
                // 确认修改
                if (nextStep()) {
                    // 如果验证码正确，则可以重置密码
                    resetPassword();
                }
                break;
        }
    }

    /**
     * 修改密码
     */
    private void resetPassword() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        final String phoneNumber = mPhoneNumberEdit.getText().toString().trim();
        final String password = mPasswordEdit.getText().toString().trim();
        String authCode = mAuthCodeEdit.getText().toString().trim();
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("telephone", phoneNumber);
        params.put("randcode", authCode);
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("newPassword", Md5Util.toMD5(password));

        HttpUtils.get().url(coreManager.getConfig().USER_PAY_PASSWORD_RESET)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(ForgetPayPasswordActivity.this, result)) {
                            Toast.makeText(ForgetPayPasswordActivity.this, InternationalizationHelper.getString("JXAlert_UpdateOK"), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(ForgetPayPasswordActivity.this, InternationalizationHelper.getString("JXServer_ErrorNetwork"), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 请求图形验证码
     */
    private void requestImageCode() {
        Map<String, String> params = new HashMap<>();
        params.put("telephone", mobilePrefix + mPhoneNumberEdit.getText().toString().trim());
        HttpUtils.get().url(coreManager.getConfig().USER_GETCODE_IMAGE)
                .params(params)
                .build().execute(new JsonCallback() {
            @Override
            public void onResponse(String result) {
                String url = coreManager.getConfig().USER_GETCODE_IMAGE + "?telephone=" + mobilePrefix + mPhoneNumberEdit.getText().toString().trim();
                Glide.with(mContext).load(url)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                                mImageCodeIv.setImageBitmap(bitmap);
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                Toast.makeText(ForgetPayPasswordActivity.this, R.string.tip_verification_code_load_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onError(Call call, Exception e) {

            }
        });
    }

    /**
     * 请求验证码
     */
    private void verifyTelephone(String phoneNumber, String imageCode) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        String language = Locale.getDefault().getLanguage();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("language", language);
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone", phoneNumber);
        params.put("imgCode", imageCode);
        params.put("isRegister", String.valueOf(0));
        params.put("version", "1");

        /**
         * 只判断中国手机号格式
         */
        if (!StringUtils.isMobileNumber(phoneNumber) && mobilePrefix == 86) {
            // Toast.makeText(this, "手机格式错误", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, InternationalizationHelper.getString("JX_Input11phoneNumber"), Toast.LENGTH_SHORT).show();
            return;
        }

        HttpUtils.get().url(coreManager.getConfig().SEND_PAY_AUTH_CODE)
                .params(params)
                .build()
                .execute(new BaseCallback<Code>(Code.class) {
                    @Override
                    public void onResponse(ObjectResult<Code> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            btn_getCode.setEnabled(false);
                            // 开始计时
                            mReckonHandler.sendEmptyMessage(0x1);
                            // 得到验证码
                            randcode = result.getData().getCode();
                            Toast.makeText(ForgetPayPasswordActivity.this, R.string.verification_code_send_success, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ForgetPayPasswordActivity.this, R.string.verification_code_send_failed, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(ForgetPayPasswordActivity.this, InternationalizationHelper.getString("JXServer_ErrorNetwork"), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 确认两次输入的密码是否一致
     */
    private boolean configPassword() {
        String password = mPasswordEdit.getText().toString().trim();
        String confirmPassword = mConfigPasswordEdit.getText().toString().trim();
        if (TextUtils.isEmpty(password) || password.length() != 6) {
            mPasswordEdit.requestFocus();
            mPasswordEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.pay_password_empty_error));
            return false;
        }
        if (TextUtils.isEmpty(confirmPassword) || confirmPassword.length() != 6) {
            mConfigPasswordEdit.requestFocus();
            mConfigPasswordEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.pay_confirm_password_empty_error));
            return false;
        }
        if (confirmPassword.equals(password)) {
            return true;
        } else {
            mConfigPasswordEdit.requestFocus();
            mConfigPasswordEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.password_confirm_password_not_match));
            return false;
        }
    }

    /**
     * 验证验证码
     */
    private boolean nextStep() {
        final String phoneNumber = mPhoneNumberEdit.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            //            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, InternationalizationHelper.getString("JX_InputPhone"), Toast.LENGTH_SHORT).show();
            return false;
        }
        /**
         * 只判断中国手机号格式
         */
        if (!StringUtils.isMobileNumber(phoneNumber) && mobilePrefix == 86) {
            //            Toast.makeText(this, "手机号码格式错误", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, InternationalizationHelper.getString("JX_Input11phoneNumber"), Toast.LENGTH_SHORT).show();
            return false;
        }
        String authCode = mAuthCodeEdit.getText().toString().trim();
        if (TextUtils.isEmpty(authCode)) {
            //            Toast.makeText(this, "请填写验证码", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, InternationalizationHelper.getString("JX_InputMessageCode"), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (authCode.equals(randcode)) {
            // 验证码正确
            return true;
        } else {
            // 验证码错误
            //            Toast.makeText(this, "验证码错误", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, InternationalizationHelper.getString("inputPhoneVC_MsgCodeNotOK"), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != SelectPrefixActivity.RESULT_MOBILE_PREFIX_SUCCESS)
            return;
        mobilePrefix = data.getIntExtra(Constants.MOBILE_PREFIX, 86);
        mobileCountry = data.getStringExtra(Constants.MOBILE_COUNTRY);
        tv_prefix.setText(mobileCountry+"(+" + mobilePrefix+")");
        // 图形验证码可能因区号失效，
        // 请求图形验证码
        if (!TextUtils.isEmpty(mPhoneNumberEdit.getText().toString())) {
            requestImageCode();
        }
    }
}

