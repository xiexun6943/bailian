package com.ydd.zhichat.ui.me.redpacket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.redpacket.RedPacket;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.message.ChatActivity;
import com.ydd.zhichat.ui.message.MucChatActivity;
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
 * Created by 魏正旺 on 2016/9/8.
 */
public class MucSendRedPacketActivity extends BaseActivity implements View.OnClickListener {
    LayoutInflater inflater;
    private SmartTabLayout smartTabLayout;
    private ViewPager viewPager;
    private List<View> views;
    private List<String> mTitleList;
    private RelativeLayout rel_red1;
    private RelativeLayout rel_red2;
    private RelativeLayout rel_red3;
    private EditText edit_count_pt;
    private EditText edit_money_pt;
    private EditText edit_words_pt;

    private EditText edit_count_psq;
    private EditText edit_money_psq;
    private EditText edit_words_psq;

    private EditText edit_count_kl;
    private EditText edit_money_kl;
    private EditText edit_words_kl;

    private TextView mAmtCountPt;
    private TextView mAmtCountKl;
    private TextView mAmtCountSq;
    private MergerStatus mergerStatus;

    private TextView hbgs, ge, zje, yuan, xhb;
    private Button sq;
    private String groupId;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_muc_redpacket);
        inflater = LayoutInflater.from(this);
        groupId = getIntent().getStringExtra("groupId");
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

        smartTabLayout = (SmartTabLayout) findViewById(R.id.muc_smarttablayout_redpacket);
        viewPager = (ViewPager) findViewById(R.id.muc_viewpagert_redpacket);
        views = new ArrayList<View>();
        mTitleList = new ArrayList<String>();

        mTitleList.add(InternationalizationHelper.getString("JX_LuckGift"));
        mTitleList.add(InternationalizationHelper.getString("JX_UsualGift"));
        mTitleList.add(InternationalizationHelper.getString("JX_MesGift"));

        views.add(inflater.inflate(R.layout.muc_redpacket_pager_pt, null));
        views.add(inflater.inflate(R.layout.muc_redpacket_pager_sq, null));
        views.add(inflater.inflate(R.layout.muc_redpacket_pager_kl, null));

        View temp_view = views.get(0);
        rel_red1 = temp_view.findViewById(R.id.rel_red);
        edit_count_pt = (EditText) temp_view.findViewById(R.id.edit_redcount);
        edit_count_pt.addTextChangedListener(new RemoveZeroTextWatcher(edit_count_pt));
        edit_money_pt = (EditText) temp_view.findViewById(R.id.edit_money);
        edit_words_pt = (EditText) temp_view.findViewById(R.id.edit_blessing);
        mAmtCountPt = temp_view.findViewById(R.id.mAmtCountly);
        hbgs = (TextView) temp_view.findViewById(R.id.hbgs);
        ge = (TextView) temp_view.findViewById(R.id.ge);
        zje = (TextView) temp_view.findViewById(R.id.zje);
        yuan = (TextView) temp_view.findViewById(R.id.yuan);
        xhb = (TextView) temp_view.findViewById(R.id.textviewtishi);
        sq = (Button) temp_view.findViewById(R.id.btn_sendRed);
        hbgs.setText(InternationalizationHelper.getString("NUMBER_OF_ENVELOPES"));
        ge.setText(InternationalizationHelper.getString("INDIVIDUAL"));
        zje.setText(InternationalizationHelper.getString("TOTAL_AMOUNT"));
        edit_money_pt.setHint(InternationalizationHelper.getString("INPUT_AMOUNT"));
        yuan.setText(InternationalizationHelper.getString("YUAN"));
        xhb.setText(InternationalizationHelper.getString("RONDOM_AMOUNT"));
        edit_words_pt.setHint(InternationalizationHelper.getString("JX_GiftText"));
        sq.setOnClickListener(this);

        temp_view = views.get(1);
        rel_red2 = temp_view.findViewById(R.id.rel_red);
        edit_count_psq = (EditText) temp_view.findViewById(R.id.edit_redcount);
        edit_count_psq.addTextChangedListener(new RemoveZeroTextWatcher(edit_count_psq));
        edit_money_psq = (EditText) temp_view.findViewById(R.id.edit_money);
        edit_words_psq = (EditText) temp_view.findViewById(R.id.edit_blessing);
        mAmtCountSq = temp_view.findViewById(R.id.mAmtCountSq);
        mergerStatus = findViewById(R.id.mergerStatus);
        mergerStatus.setBackgroundResource(R.color.redpacket_bg);

        hbgs = (TextView) temp_view.findViewById(R.id.hbgs);
        ge = (TextView) temp_view.findViewById(R.id.ge);
        zje = (TextView) temp_view.findViewById(R.id.zje);
        yuan = (TextView) temp_view.findViewById(R.id.yuan);
        xhb = (TextView) temp_view.findViewById(R.id.textviewtishi);
        sq = (Button) temp_view.findViewById(R.id.btn_sendRed);
        hbgs.setText(InternationalizationHelper.getString("NUMBER_OF_ENVELOPES"));
        ge.setText(InternationalizationHelper.getString("INDIVIDUAL"));
        zje.setText(InternationalizationHelper.getString("TOTAL_AMOUNT"));
        edit_money_psq.setHint(InternationalizationHelper.getString("INPUT_AMOUNT"));
        yuan.setText(InternationalizationHelper.getString("YUAN"));
        xhb.setText(InternationalizationHelper.getString("SAME_AMOUNT"));
        edit_words_psq.setHint(InternationalizationHelper.getString("JX_GiftText"));
        sq.setOnClickListener(this);

        temp_view = views.get(2);
        rel_red3 = temp_view.findViewById(R.id.rel_red);
        edit_count_kl = (EditText) temp_view.findViewById(R.id.edit_redcount);
        edit_count_kl.addTextChangedListener(new RemoveZeroTextWatcher(edit_count_kl));
        edit_money_kl = (EditText) temp_view.findViewById(R.id.edit_money);
        edit_words_kl = (EditText) temp_view.findViewById(R.id.edit_password);
        EditText edit_compatible = (EditText) temp_view.findViewById(R.id.edit_compatible);
        mAmtCountKl = temp_view.findViewById(R.id.mAmtCountKl);
        edit_compatible.requestFocus();

        hbgs = (TextView) temp_view.findViewById(R.id.hbgs);
        ge = (TextView) temp_view.findViewById(R.id.ge);
        zje = (TextView) temp_view.findViewById(R.id.zje);
        yuan = (TextView) temp_view.findViewById(R.id.yuan);
        xhb = (TextView) temp_view.findViewById(R.id.textviewtishi);
        sq = (Button) temp_view.findViewById(R.id.btn_sendRed);
        TextView kl = (TextView) temp_view.findViewById(R.id.kl);
        kl.setText(InternationalizationHelper.getString("JX_Message"));
        hbgs.setText(InternationalizationHelper.getString("NUMBER_OF_ENVELOPES"));
        ge.setText(InternationalizationHelper.getString("INDIVIDUAL"));
        zje.setText(InternationalizationHelper.getString("TOTAL_AMOUNT"));
        edit_money_kl.setHint(InternationalizationHelper.getString("INPUT_AMOUNT"));
        yuan.setText(InternationalizationHelper.getString("YUAN"));
        xhb.setText(InternationalizationHelper.getString("REPLY_GRAB"));
        edit_words_kl.setHint(InternationalizationHelper.getString("BIG_ENVELOPE"));
        sq.setOnClickListener(this);

//        InputChangeListener inputChangeListenerPt = new InputChangeListener(edit_money_pt);
//        InputChangeListener inputChangeListenerPsq = new InputChangeListener(edit_money_psq);
//        InputChangeListener inputChangeListenerKl = new InputChangeListener(edit_money_kl);

        // 添加输入监听
//        edit_money_pt.addTextChangedListener(inputChangeListenerPt);
//        edit_money_psq.addTextChangedListener(inputChangeListenerPsq);
//        edit_money_kl.addTextChangedListener(inputChangeListenerKl);

        rel_red1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftInput();
            }
        });
        rel_red2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftInput();
            }
        });
        rel_red3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftInput();
            }
        });

        edit_money_pt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().contains(".")) {
                    if (s.length() - 1 - s.toString().indexOf(".") > 2) {
                        s = s.toString().subSequence(0,
                                s.toString().indexOf(".") + 3);
                        edit_money_pt.setText(s);
                        edit_money_pt.setSelection(s.length());
                    }
                }
                if (s.toString().trim().substring(0).equals(".")) {
                    s = "0" + s;
                    edit_money_pt.setText(s);
                    edit_money_pt.setSelection(2);
                }

                if (s.toString().startsWith("0")
                        && s.toString().trim().length() > 1) {
                    if (!s.toString().substring(1, 2).equals(".")) {
                        edit_money_pt.setText(s.subSequence(0, 1));
                        edit_money_pt.setSelection(1);
                        return;
                    }
                }


            }

            @Override
            public void afterTextChanged(Editable editable) {
                mAmtCountPt.setText("￥"+edit_money_pt.getText().toString());
            }
        });

        edit_money_kl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().contains(".")) {
                    if (s.length() - 1 - s.toString().indexOf(".") > 2) {
                        s = s.toString().subSequence(0,
                                s.toString().indexOf(".") + 3);
                        edit_money_kl.setText(s);
                        edit_money_kl.setSelection(s.length());
                    }
                }
                if (s.toString().trim().substring(0).equals(".")) {
                    s = "0" + s;
                    edit_money_kl.setText(s);
                    edit_money_kl.setSelection(2);
                }

                if (s.toString().startsWith("0")
                        && s.toString().trim().length() > 1) {
                    if (!s.toString().substring(1, 2).equals(".")) {
                        edit_money_kl.setText(s.subSequence(0, 1));
                        edit_money_kl.setSelection(1);
                        return;
                    }
                }


            }

            @Override
            public void afterTextChanged(Editable editable) {
                mAmtCountKl.setText("￥"+edit_money_kl.getText().toString());
            }
        });
        edit_money_psq.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().contains(".")) {
                    if (s.length() - 1 - s.toString().indexOf(".") > 2) {
                        s = s.toString().subSequence(0,
                                s.toString().indexOf(".") + 3);
                        edit_money_psq.setText(s);
                        edit_money_psq.setSelection(s.length());
                    }
                }
                if (s.toString().trim().substring(0).equals(".")) {
                    s = "0" + s;
                    edit_money_psq.setText(s);
                    edit_money_psq.setSelection(2);
                }

                if (s.toString().startsWith("0")
                        && s.toString().trim().length() > 1) {
                    if (!s.toString().substring(1, 2).equals(".")) {
                        edit_money_psq.setText(s.subSequence(0, 1));
                        edit_money_psq.setSelection(1);
                        return;
                    }
                }


            }

            @Override
            public void afterTextChanged(Editable editable) {
                mAmtCountSq.setText("￥"+edit_money_psq.getText().toString());
            }
        });
        // 只允许输入小数点和数字
        edit_money_pt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit_money_psq.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit_money_kl.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        viewPager.setAdapter(new PagerAdapter());
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

        viewPager.setCurrentItem(0);
    }

    private void hideSoftInput() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        View view = getCurrentFocus();
        if (view == null) view = new View(MucSendRedPacketActivity.this);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onClick(View v) {
        /*if (v.getId() == R.id.btn_sendRed) {
            final int item = viewPager.getCurrentItem();
            final Bundle bundle = new Bundle();
            final Intent intent = new Intent(this, MucChatActivity.class);
            String money = null, words = null, count = null;
            int resultCode = 0;
            switch (item) {
                case 0: {
                    money = edit_money_pt.getText().toString();
                    words = StringUtils.isNullOrEmpty(edit_words_pt.getText().toString()) ?
                            edit_words_pt.getHint().toString() : edit_words_pt.getText().toString();
                    count = edit_count_pt.getText().toString();
                    // 拼手气与普通红包位置对调  修改resultCode
                    resultCode = ChatActivity.REQUEST_CODE_SEND_RED_PSQ;
                }
                break;

                case 1: {
                    money = edit_money_psq.getText().toString();
                    words = StringUtils.isNullOrEmpty(edit_words_psq.getText().toString()) ?
                            edit_words_psq.getHint().toString() : edit_words_psq.getText().toString();
                    count = edit_count_psq.getText().toString();
                    resultCode = ChatActivity.REQUEST_CODE_SEND_RED_PT;
                }
                break;

                case 2: {
                    money = edit_money_kl.getText().toString();
                    words = StringUtils.isNullOrEmpty(edit_words_kl.getText().toString()) ?
                            edit_words_kl.getHint().toString() : edit_words_kl.getText().toString();
                    count = edit_count_kl.getText().toString();
                    resultCode = ChatActivity.REQUEST_CODE_SEND_RED_KL;
                }
                break;
            }

            if (!TextUtils.isEmpty(count) && Integer.parseInt(count) == 0) {
                Toast.makeText(this, R.string.tip_red_packet_too_slow, Toast.LENGTH_SHORT).show();
                return;
            }

            // 当金额过小，红包个数过多的情况下会出现不够分的情况
            if (!TextUtils.isEmpty(count) && Integer.parseInt(count) > 100) {
                Toast.makeText(this, R.string.tip_red_packet_too_much, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!TextUtils.isEmpty(money) &&
                    !TextUtils.isEmpty(count) &&
                    Double.parseDouble(money) / Integer.parseInt(count) < 0.01) {
                Toast.makeText(this, R.string.tip_money_too_less, Toast.LENGTH_SHORT).show();
                return;
            }

            if (eqData(money, count, words)) {
                PayPasswordVerifyDialog dialog = new PayPasswordVerifyDialog(this);
                dialog.setAction(getString(R.string.chat_redpacket));
                dialog.setMoney(money);
                final String finalMoney = money;
                final String finalWords = words;
                final String finalCount = count;
                dialog.setOnInputFinishListener(new PayPasswordVerifyDialog.OnInputFinishListener() {
                    @Override
                    public void onInputFinish(final String password) {
                        // 拼手气与普通红包位置对调，修改type
                        // bundle.putString("type", (item + 1) + "");
                        String type;
                        if (item == 0) {
                            type = 2 + "";
                        } else if (item == 1) {
                            type = 1 + "";
                        } else {
                            type = item + 1 + "";
                        }
                        sendRed(intent,bundle,type, finalMoney, finalCount, finalWords,password);
                    }
                });
                dialog.show();
            }
        } else {
            int index = Integer.parseInt(v.getTag().toString());
            viewPager.setCurrentItem(index, false);
        }*/
        if (v.getId() == R.id.btn_sendRed) {
            final int item = viewPager.getCurrentItem();
            final Bundle bundle = new Bundle();
            final Intent intent = new Intent(this, MucChatActivity.class);
            String money = null, words = null, count = null;
            int resultCode = 0;
            switch (item) {
                case 0: {
                    money = edit_money_pt.getText().toString();
                    words = StringUtils.isNullOrEmpty(edit_words_pt.getText().toString()) ?
                            edit_words_pt.getHint().toString() : edit_words_pt.getText().toString();
                    count = edit_count_pt.getText().toString();
                    // 拼手气与普通红包位置对调  修改resultCode
                    resultCode = ChatActivity.REQUEST_CODE_SEND_RED_PSQ;
                }
                break;

                case 1: {
                    money = edit_money_psq.getText().toString();
                    words = StringUtils.isNullOrEmpty(edit_words_psq.getText().toString()) ?
                            edit_words_psq.getHint().toString() : edit_words_psq.getText().toString();
                    count = edit_count_psq.getText().toString();
                    resultCode = ChatActivity.REQUEST_CODE_SEND_RED_PT;
                }
                break;

                case 2: {
                    money = edit_money_kl.getText().toString();
                    words = StringUtils.isNullOrEmpty(edit_words_kl.getText().toString()) ?
                            edit_words_kl.getHint().toString() : edit_words_kl.getText().toString();
                    count = edit_count_kl.getText().toString();
                    resultCode = ChatActivity.REQUEST_CODE_SEND_RED_KL;
                }
                break;
            }

            if (!TextUtils.isEmpty(count) && Integer.parseInt(count) == 0) {
                Toast.makeText(this, R.string.tip_red_packet_too_slow, Toast.LENGTH_SHORT).show();
                return;
            }

            // 当金额过小，红包个数过多的情况下会出现不够分的情况
            if (!TextUtils.isEmpty(count) && Integer.parseInt(count) > 100) {
                Toast.makeText(this, R.string.tip_red_packet_too_much, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!TextUtils.isEmpty(money) &&
                    !TextUtils.isEmpty(count) &&
                    Double.parseDouble(money) / Integer.parseInt(count) < 0.01) {
                Toast.makeText(this, R.string.tip_money_too_less, Toast.LENGTH_SHORT).show();
                return;
            }

            if (eqData(money, count, words)) {
                PayPasswordVerifyDialog dialog = new PayPasswordVerifyDialog(this);
                dialog.setAction(getString(R.string.chat_redpacket));
                dialog.setMoney(money);
                final String finalMoney = money;
                final String finalWords = words;
                final String finalCount = count;
                dialog.setOnInputFinishListener(new PayPasswordVerifyDialog.OnInputFinishListener() {
                    @Override
                    public void onInputFinish(final String password) {
                        // 回传信息
                        bundle.putString("money", finalMoney);
                        bundle.putString("count", finalCount);
                        bundle.putString("words", finalWords);
                        // 拼手气与普通红包位置对调，修改type
                        // bundle.putString("type", (item + 1) + "");
                        if (item == 0) {
                            bundle.putString("type", 2 + "");
                        } else if (item == 1) {
                            bundle.putString("type", 1 + "");
                        } else {
                            bundle.putString("type", (item + 1) + "");
                        }
                        bundle.putString("payPassword", password);
                        intent.putExtras(bundle);
                        setResult(item == 0 ? ChatActivity.REQUEST_CODE_SEND_RED_PSQ : ChatActivity.REQUEST_CODE_SEND_RED_KL, intent);
                        finish();
                    }
                });
                dialog.show();
            }
        } else {
            int index = Integer.parseInt(v.getTag().toString());
            viewPager.setCurrentItem(index, false);
        }
    }

    private boolean eqData(String money, String count, String words) {
        if (StringUtils.isNullOrEmpty(money)) {
            ToastUtil.showToast(mContext, getString(R.string.need_input_money));
            return false;
        } else if (Double.parseDouble(money) > 500 || Double.parseDouble(money) <= 0) {
            ToastUtil.showToast(mContext, getString(R.string.red_packet_range));
            return false;
        } else if (Double.parseDouble(money) > coreManager.getSelf().getBalance()) {
            ToastUtil.showToast(mContext, getString(R.string.balance_not_enough));
            return false;
        } else if (StringUtils.isNullOrEmpty(count)) {
            ToastUtil.showToast(mContext, getString(R.string.need_red_packet_count));
            return false;
        } else if (StringUtils.isNullOrEmpty(words)) {
            return false;
        }
        return true;
    }

    private static class RemoveZeroTextWatcher implements TextWatcher {
        private EditText editText;

        RemoveZeroTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // 删除开头的0，
            int end = 0;
            for (int i = 0; i < editable.length(); i++) {
                char ch = editable.charAt(i);
                if (ch == '0') {
                    end = i + 1;
                } else {
                    break;
                }
            }
            if (end > 0) {
                editable.delete(0, end);
                editText.setText(editable);
            }
        }
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
        public void destroyItem(View container, int position, Object object) {
            ((ViewPager) container).removeView(views.get(position));
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

    /**
     * 发送红包方法
     *
     * @param type  类型(口令、普通、拼手气)
     * @param money 金额
     * @param count 数量
     * @param words 祝福语(或者口令)
     */
    public void sendRed(Intent intent,Bundle bundle,String type, String money, String count, String words, String payPassword) {
        /**
         * 步骤
         * 1.调发红包的接口，发送一个红包
         * 2.吧消息发送出去
         */
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", type);
        params.put("moneyStr", money);
        params.put("count", count);
        params.put("greetings", words);
        params.put("roomJid",groupId);

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
                            intent.putExtras(bundle);
                            setResult(viewPager.getCurrentItem() == 0 ? ChatActivity.REQUEST_CODE_SEND_RED_PSQ : ChatActivity.REQUEST_CODE_SEND_RED_KL, intent);
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }
}
