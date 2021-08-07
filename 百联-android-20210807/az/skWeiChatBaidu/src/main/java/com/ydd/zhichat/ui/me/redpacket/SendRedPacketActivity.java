package com.ydd.zhichat.ui.me.redpacket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.redpacket.RedPacket;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.message.ChatActivity;
import com.ydd.zhichat.ui.smarttab.SmartTabLayout;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.MergerStatus;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import org.jivesoftware.smack.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * Created by 魏正旺 on 2016/9/9.
 */
public class SendRedPacketActivity extends BaseActivity implements View.OnClickListener {
    LayoutInflater inflater;
    private SmartTabLayout smartTabLayout;
    private ViewPager viewPager;
    private List<View> views;
    private List<String> mTitleList;
    private RelativeLayout rel_red1,rel_red2;
    private EditText editTextPt;  // 普通红包的金额输入框
    private EditText editTextKl;  // 口令红包的金额输入框
    private EditText editTextPwd; // 口令输入框
    private EditText editTextGre; // 祝福语输入框
    private TextView mAmtCountKl;//
    private TextView mAmtCount;//
    private MergerStatus mergerStatus;//
    private String friendId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redpacket);
        inflater = LayoutInflater.from(this);
        friendId = getIntent().getStringExtra("friendId");
        initView();

        checkHasPayPassword();
    }

    private void checkHasPayPassword() {
        boolean hasPayPassword = PreferenceUtils.getBoolean(this, Constants.IS_PAY_PASSWORD_SET + coreManager.getSelf().getUserId(), true);
        if (!hasPayPassword) {
            ToastUtil.showToast(this, R.string.tip_no_pay_password);
            Intent intent = new Intent(this, ChangePayPasswordActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.tv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JX_SendGift"));

        viewPager = (ViewPager) findViewById(R.id.viewpagert_redpacket);
        smartTabLayout = (SmartTabLayout) findViewById(R.id.smarttablayout_redpacket);
        mergerStatus = findViewById(R.id.mergerStatus);
        mergerStatus.setBackgroundResource(R.color.redpacket_bg);
        views = new ArrayList<View>();
        mTitleList = new ArrayList<String>();
        mTitleList.add(InternationalizationHelper.getString("JX_UsualGift"));
        mTitleList.add(InternationalizationHelper.getString("JX_MesGift"));
        View v1, v2;
        v1 = inflater.inflate(R.layout.redpacket_pager_pt, null);
        v2 = inflater.inflate(R.layout.redpacket_pager_kl, null);
        views.add(v1);
        views.add(v2);

        rel_red1 =  v1.findViewById(R.id.rel_red);
        rel_red2 =  v2.findViewById(R.id.rel_red);
        // 获取EditText

        editTextPt = (EditText) v1.findViewById(R.id.edit_money);
        editTextGre = (EditText) v1.findViewById(R.id.edit_blessing);
        editTextKl = (EditText) v2.findViewById(R.id.edit_money);
        editTextPwd = (EditText) v2.findViewById(R.id.edit_password);
        mAmtCount = v1.findViewById(R.id.mAmtCount);
        mAmtCountKl = v2.findViewById(R.id.mAmtCountKl);


        TextView jineTv, tipTv, sumjineTv, yuan1, yuan2;
        jineTv = (TextView) v1.findViewById(R.id.JinETv);
        tipTv = (TextView) v2.findViewById(R.id.textviewtishi);
        sumjineTv = (TextView) v2.findViewById(R.id.sumMoneyTv);
        yuan1 = (TextView) v1.findViewById(R.id.yuanTv);
        yuan2 = (TextView) v2.findViewById(R.id.yuanTv);
        jineTv.setText(InternationalizationHelper.getString("AMOUNT_OF_MONEY"));
        tipTv.setText(InternationalizationHelper.getString("SMALL_PARTNERS"));
        sumjineTv.setText(InternationalizationHelper.getString("TOTAL_AMOUNT"));
        yuan1.setText(InternationalizationHelper.getString("YUAN"));
        yuan2.setText(InternationalizationHelper.getString("YUAN"));

        editTextPt.setHint(InternationalizationHelper.getString("JX_InputGiftCount"));
        editTextGre.setHint(InternationalizationHelper.getString("JX_GiftText"));

        editTextKl.setHint(InternationalizationHelper.getString("JX_InputGiftCount"));
        editTextPwd.setHint(InternationalizationHelper.getString("JX_WantOpenGift"));

        TextView koulinTv;
        koulinTv = (TextView) v2.findViewById(R.id.setKouLinTv);
        koulinTv.setText(InternationalizationHelper.getString("JX_Message"));

        Button b1 = (Button) v1.findViewById(R.id.btn_sendRed);
        b1.setOnClickListener(this);
        Button b2 = (Button) v2.findViewById(R.id.btn_sendRed);
        b2.setOnClickListener(this);

        b1.requestFocus();
        b1.setClickable(true);
        b2.requestFocus();
        b2.setClickable(true);

//        InputChangeListener inputChangeListenerPt = new InputChangeListener(editTextPt);
//        InputChangeListener inputChangeListenerKl = new InputChangeListener(editTextKl);
//
//        editTextPt.addTextChangedListener(inputChangeListenerPt);
//        editTextKl.addTextChangedListener(inputChangeListenerKl);

        //设置值允许输入数字和小数点
        editTextPt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editTextKl.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        viewPager.setAdapter(new PagerAdapter());
        inflater = LayoutInflater.from(this);
        smartTabLayout.setViewPager(viewPager);

        /**
         * 为了实现点击Tab栏切换的时候不出现动画
         * 为每个Tab重新设置点击事件
         */
        for (int i = 0; i < mTitleList.size(); i++) {
            View view = smartTabLayout.getTabAt(i);
            view.setTag(i + "");
            view.setOnClickListener(this);
        }

        rel_red1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm == null) return;
                View view = getCurrentFocus();
                if (view == null) view = new View(SendRedPacketActivity.this);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        rel_red2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm == null) return;
                View view = getCurrentFocus();
                if (view == null) view = new View(SendRedPacketActivity.this);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        editTextPt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().contains(".")) {
                    if (s.length() - 1 - s.toString().indexOf(".") > 2) {
                        s = s.toString().subSequence(0,
                                s.toString().indexOf(".") + 3);
                        editTextPt.setText(s);
                        editTextPt.setSelection(s.length());
                    }
                }
                if (s.toString().trim().substring(0).equals(".")) {
                    s = "0" + s;
                    editTextPt.setText(s);
                    editTextPt.setSelection(2);
                }

                if (s.toString().startsWith("0")
                        && s.toString().trim().length() > 1) {
                    if (!s.toString().substring(1, 2).equals(".")) {
                        editTextPt.setText(s.subSequence(0, 1));
                        editTextPt.setSelection(1);
                        return;
                    }
                }


            }

            @Override
            public void afterTextChanged(Editable editable) {
                mAmtCount.setText("￥"+editTextPt.getText().toString());
            }
        });

        editTextKl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().contains(".")) {
                    if (s.length() - 1 - s.toString().indexOf(".") > 2) {
                        s = s.toString().subSequence(0,
                                s.toString().indexOf(".") + 3);
                        editTextKl.setText(s);
                        editTextKl.setSelection(s.length());
                    }
                }
                if (s.toString().trim().substring(0).equals(".")) {
                    s = "0" + s;
                    editTextKl.setText(s);
                    editTextKl.setSelection(2);
                }

                if (s.toString().startsWith("0")
                        && s.toString().trim().length() > 1) {
                    if (!s.toString().substring(1, 2).equals(".")) {
                        editTextKl.setText(s.subSequence(0, 1));
                        editTextKl.setSelection(1);
                        return;
                    }
                }


            }

            @Override
            public void afterTextChanged(Editable editable) {
                mAmtCountKl.setText("￥"+editTextKl.getText().toString());
            }
        });

    }

    @Override
    public void onClick(View v) {
        /*if (v.getId() == R.id.btn_sendRed) {
            final Bundle bundle = new Bundle();
            final Intent data = new Intent(this, ChatActivity.class);
            String money = null, words = null;

            //根据Tab的Item来判断当前发送的是那种红包
            final int item = viewPager.getCurrentItem();

            //获取金额和文字信息(口令或者祝福语)
            if (item == 0) {
                money = editTextPt.getText().toString();
                words = editTextGre.getText().toString();
                if (StringUtils.isNullOrEmpty(words)) {
                    words = editTextGre.getHint().toString();
                }
            } else if (item == 1) {
                money = editTextKl.getText().toString();
                words = editTextPwd.getText().toString();
                if (StringUtils.isNullOrEmpty(words)) {
                    words = editTextPwd.getHint().toString();
                    words = words.substring(1, words.length());
                }
            }
            if (StringUtils.isNullOrEmpty(money)) {
                ToastUtil.showToast(mContext, InternationalizationHelper.getString("JX_InputGiftCount"));
            } else if (Double.parseDouble(money) > 500 || Double.parseDouble(money) <= 0) {
                ToastUtil.showToast(mContext, InternationalizationHelper.getString("JXRechargeVC_MoneyCount"));
            } else if (Double.parseDouble(money) > coreManager.getSelf().getBalance()) {
                ToastUtil.showToast(mContext, InternationalizationHelper.getString("JX_NotEnough"));
            } else {
                PayPasswordVerifyDialog dialog = new PayPasswordVerifyDialog(this);
                dialog.setAction(getString(R.string.chat_redpacket));
                dialog.setMoney(money);
                final String finalMoney = money;
                final String finalWords = words;
                dialog.setOnInputFinishListener(new PayPasswordVerifyDialog.OnInputFinishListener() {
                    @Override
                    public void onInputFinish(final String password) {
                        sendRed(data,bundle,item == 0 ? "1" : "3",  finalMoney,  "1", finalWords, password);
                    }
                });
                dialog.show();
            }
        } else {
            // 根据Tab按钮传递的Tag来判断是那个页面，设置到相应的界面并且去掉动画
            int index = Integer.parseInt(v.getTag().toString());
            viewPager.setCurrentItem(index, false);
        }*/

        if (v.getId() == R.id.btn_sendRed) {
            final Bundle bundle = new Bundle();
            final Intent data = new Intent(this, ChatActivity.class);
            String money = null, words = null;

            //根据Tab的Item来判断当前发送的是那种红包
            final int item = viewPager.getCurrentItem();

            //获取金额和文字信息(口令或者祝福语)
            if (item == 0) {
                money = editTextPt.getText().toString();
                words = editTextGre.getText().toString();
                if (StringUtils.isNullOrEmpty(words)) {
                    words = editTextGre.getHint().toString();
                }
            } else if (item == 1) {
                money = editTextKl.getText().toString();
                words = editTextPwd.getText().toString();
                if (StringUtils.isNullOrEmpty(words)) {
                    words = editTextPwd.getHint().toString();
                    words = words.substring(1, words.length());
                }
            }
            if (StringUtils.isNullOrEmpty(money)) {
                ToastUtil.showToast(mContext, InternationalizationHelper.getString("JX_InputGiftCount"));
            } else if (Double.parseDouble(money) > 500 || Double.parseDouble(money) <= 0) {
                ToastUtil.showToast(mContext, InternationalizationHelper.getString("JXRechargeVC_MoneyCount"));
            } else if (Double.parseDouble(money) > coreManager.getSelf().getBalance()) {
                ToastUtil.showToast(mContext, InternationalizationHelper.getString("JX_NotEnough"));
            } else {
                PayPasswordVerifyDialog dialog = new PayPasswordVerifyDialog(this);
                dialog.setAction(getString(R.string.chat_redpacket));
                dialog.setMoney(money);
                final String finalMoney = money;
                final String finalWords = words;
                dialog.setOnInputFinishListener(new PayPasswordVerifyDialog.OnInputFinishListener() {
                    @Override
                    public void onInputFinish(final String password) {
                        // 回传信息
                        bundle.putString("money", finalMoney);
                        bundle.putString(item == 0 ? "greetings" : "password", finalWords);
                        bundle.putString("type", item == 0 ? "1" : "3"); // 类型
                        bundle.putString("count", "1"); // 因为是单聊，所以个数必须是一
                        bundle.putString("payPassword", password);
                        data.putExtras(bundle);
                        setResult(item == 0 ? ChatActivity.REQUEST_CODE_SEND_RED_PT : ChatActivity.REQUEST_CODE_SEND_RED_KL, data);
                        finish();
                    }
                });
                dialog.show();
            }
        } else {
            // 根据Tab按钮传递的Tag来判断是那个页面，设置到相应的界面并且去掉动画
            int index = Integer.parseInt(v.getTag().toString());
            viewPager.setCurrentItem(index, false);
        }
    }

    public void sendRed(Intent data,Bundle bundle,final String type, String money, String count, final String words, String payPassword) {
        Map<String, String> params = new HashMap();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", type);
        params.put("moneyStr", money);
        params.put("count", count);
        params.put("greetings", words);
        params.put("toUserId", friendId);

        HttpUtils.get().url(coreManager.getConfig().REDPACKET_SEND)
                .params(params)
                .addSecret(payPassword, money)
                .build()
                .execute(new BaseCallback<RedPacket>(RedPacket.class) {

                    @Override
                    public void onResponse(ObjectResult<RedPacket> result) {
                        RedPacket redPacket = result.getData();
                        if (result.getResultCode() != 1) {
                            // 发送红包失败，
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            bundle.putSerializable("redPacket",redPacket);
                            data.putExtras(bundle);
                            setResult(viewPager.getCurrentItem() == 0 ? ChatActivity.REQUEST_CODE_SEND_RED_PT : ChatActivity.REQUEST_CODE_SEND_RED_KL, data);
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    private class PagerAdapter extends android.support.v4.view.PagerAdapter {

        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(View container, int position) {
            ((ViewGroup) container).addView(views.get(position));
            return views.get(position);
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            return mTitleList.get(position);
        }
    }
}
