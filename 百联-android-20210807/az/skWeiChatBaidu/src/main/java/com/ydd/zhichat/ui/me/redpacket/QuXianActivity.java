package com.ydd.zhichat.ui.me.redpacket;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.redpacket.CardBean;
import com.ydd.zhichat.bean.redpacket.SelectWindowModel;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.me.redpacket.alipay.AlipayHelper;
import com.ydd.zhichat.util.CardUtils;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.SelectCardPop;
import com.ydd.zhichat.view.TixianSureDialog;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;

public class QuXianActivity extends BaseActivity {
    public static String amount;// 提现金额 单位:分
    private IWXAPI api;
    private EditText mMentionMoneyEdit;
    private TextView mBalanceTv;
    private TextView mAllMentionTv;
    private TextView mSureMentionTv;//微信提现
    private TextView withdraw_defult;//默认提现
    private TextView tvAlipay;//支付宝提现
    private TextView tv_bankName;//提现方式
    private TextView tv_tipRate;//费率提示
    private RelativeLayout rel_selectCard;
    private DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private ArrayList<CardBean> mCards = new ArrayList();
    private ArrayList<SelectWindowModel> cardList = new ArrayList();
    private SelectWindowModel mSelectWindowModel = null;
    private SelectCardPop popCard;
    private TixianSureDialog mTixianSureDialog;
    private double serviceChargeRate = 0.01;
    private double balance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qu_xian);

        api = WXAPIFactory.createWXAPI(QuXianActivity.this, Constants.VX_APP_ID, false);
        api.registerApp(Constants.VX_APP_ID);
        serviceChargeRate = coreManager.getConfig().drawRate;
        initActionbar();
        initView();
        intEvent();

        checkHasPayPassword();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initCards();
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

    private void initActionbar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(getString(R.string.withdraw));
/*
        TextView mTvTitleRight = (TextView) findViewById(R.id.tv_title_right);
        mTvTitleRight.setText(getString(R.string.withdrawal_instructions));
*/
    }

    private void initView() {
        mMentionMoneyEdit = (EditText) findViewById(R.id.tixianmoney);
        mBalanceTv = (TextView) findViewById(R.id.blance_weixin);
        mBalanceTv.setText("￥" + decimalFormat.format(coreManager.getSelf().getBalance()));
        mAllMentionTv = (TextView) findViewById(R.id.tixianall);
        mSureMentionTv = (TextView) findViewById(R.id.tixian);
        withdraw_defult = (TextView) findViewById(R.id.withdraw_defult);
        tvAlipay = (TextView) findViewById(R.id.withdraw_alipay);
        rel_selectCard = findViewById(R.id.rel_selectCard);
        tv_bankName = findViewById(R.id.tv_bankName);
        tv_tipRate = findViewById(R.id.tv_tipRate);
        tv_tipRate.setText(String.format("提现手续费为%s%%,最低提现手续费为1元", serviceChargeRate * 100));

        balance = coreManager.getSelf().getBalance();
    }

    private void intEvent() {

        mMentionMoneyEdit.addTextChangedListener(new TextWatcher() {
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
                    mMentionMoneyEdit.setText(editable);
                }
                String moneyStr = mMentionMoneyEdit.getText().toString();
                if (TextUtils.isEmpty(moneyStr)) {
                    mSureMentionTv.setBackgroundResource(R.drawable.weixin_text_yuanjiao_no);
                    tvAlipay.setBackgroundResource(R.drawable.weixin_text_yuanjiao_no);
                    withdraw_defult.setBackgroundResource(R.drawable.weixin_text_yuanjiao_no);
                } else {
                    mSureMentionTv.setBackgroundResource(R.drawable.weixin_text_yuanjiao);
                    tvAlipay.setBackgroundResource(R.drawable.weixin_text_yuanjiao);
                    withdraw_defult.setBackgroundResource(R.drawable.weixin_text_yuanjiao);
                }
            }
        });

        mAllMentionTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMentionMoneyEdit.setText(balance + "");
            }
        });

        withdraw_defult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String moneyStr = mMentionMoneyEdit.getText().toString();
                if (checkMoney(moneyStr)) {
                    amount = moneyStr;
                    double money = Double.parseDouble(moneyStr);//提现金额

                    Double serviceCharge = money * serviceChargeRate;
                    Double both = money + serviceCharge;
                    if (money < 100) {
                        serviceCharge = 1.0;
                    }
                    Double subMoney = BigDecimal.valueOf(balance).subtract(BigDecimal.valueOf(both)).doubleValue();
                    if (subMoney < 0) {
                        money = BigDecimal.valueOf(balance - serviceCharge).setScale(2,BigDecimal.ROUND_DOWN).doubleValue();
                        serviceCharge = BigDecimal.valueOf(money * serviceChargeRate).setScale(2,BigDecimal.ROUND_UP).doubleValue();
                        amount = money+"";
                    }
                    serviceCharge = BigDecimal.valueOf(serviceCharge).setScale(2,BigDecimal.ROUND_UP).doubleValue();
                    money = BigDecimal.valueOf(money).setScale(2,BigDecimal.ROUND_UP).doubleValue();
                    if (mSelectWindowModel != null) {
//                        Double.parseDouble(StrNumUtil.keepTwoDecimal(money));
                        mTixianSureDialog = new TixianSureDialog(QuXianActivity.this);
                        mTixianSureDialog.show(mSelectWindowModel, money+serviceCharge + "", money + "", serviceCharge + "");
                        mTixianSureDialog.setListenner(new TixianSureDialog.OnClickListenner() {
                            @Override
                            public void onSure() {
                                tixian(mSelectWindowModel.cardId, amount);
                            }

                            @Override
                            public void onCancel() {
                                mTixianSureDialog.dismiss();
                                mTixianSureDialog = null;
                            }
                        });
                    } else {
                        ToastUtil.showToast(QuXianActivity.this, "请选择提现方式");
                    }
                }
            }
        });

        mSureMentionTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String moneyStr = mMentionMoneyEdit.getText().toString();
                if (checkMoney(moneyStr)) {
                    amount = String.valueOf(Integer.valueOf(moneyStr) * 100);

                    SendAuth.Req req = new SendAuth.Req();
                    req.scope = "snsapi_userinfo";
                    req.state = "wechat_sdk_demo_test";
                    api.sendReq(req);

                    finish();
                }
            }
        });

        tvAlipay.setOnClickListener(v -> {
            String moneyStr = mMentionMoneyEdit.getText().toString();
            if (checkMoney(moneyStr)) {
                amount = moneyStr;
                AlipayHelper.auth(this, coreManager, userId -> {
                    AlipayHelper.withdraw(this, coreManager, amount, userId);
                });
            }
        });

        rel_selectCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectCardPop();
            }
        });

    }

    private boolean checkMoney(String moneyStr) {
        if (TextUtils.isEmpty(moneyStr)) {
            DialogHelper.tip(QuXianActivity.this, getString(R.string.tip_withdraw_empty));
        } else {
            if (Double.valueOf(moneyStr) < 1) {
                DialogHelper.tip(QuXianActivity.this, getString(R.string.tip_withdraw_too_little));
            } else if (Double.valueOf(moneyStr) > balance) {
                DialogHelper.tip(QuXianActivity.this, getString(R.string.tip_balance_not_enough));
            } else {// 获取用户code
                return true;
            }
        }
        return false;
    }

    private void tixian(String cardId, String money) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("cardId", cardId);
        params.put("money", money);
        HttpUtils.post().url(coreManager.getConfig().WITHDRAWAL)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DialogHelper.dismissProgressDialog();
                                if (result.getResultCode() == 1) {
                                    finish();
                                }
                                ToastUtil.showToast(QuXianActivity.this, result.getResultMsg());
                            }
                        });
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(QuXianActivity.this);
                    }
                });
    }

    private void initCards() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        HttpUtils.get().url(coreManager.getConfig().CARDS)
                .params(params)
                .build()
                .execute(new ListCallback<CardBean>(CardBean.class) {
                    @Override
                    public void onResponse(ArrayResult<CardBean> result) {
                        DialogHelper.dismissProgressDialog();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mCards.clear();
                                cardList.clear();
                                if (result.getResultCode() == 1 && result.getData() != null) {
                                    mCards.addAll(result.getData());
                                }
                                for (int i = 0; i < mCards.size(); i++) {
                                    SelectWindowModel selectWindowModel = new SelectWindowModel();
                                    selectWindowModel.icon = CardUtils.getBankIconResId(mCards.get(i).getBankBrandId());
                                    selectWindowModel.cardNum = mCards.get(i).getCardNo();
                                    selectWindowModel.cardId = mCards.get(i).getId();
                                    selectWindowModel.id = mCards.get(i).getBankBrandId();
                                    selectWindowModel.name = mCards.get(i).getBankBrandName();
                                    cardList.add(selectWindowModel);
                                }
                                if (cardList.size() > 0) {
                                    mSelectWindowModel = cardList.get(0);
                                    tv_bankName.setText(mSelectWindowModel.name);
                                    popCard = new SelectCardPop(QuXianActivity.this, cardList);
                                    popCard.setOnTypeSelectListaner(new SelectCardPop.OnTypeSelectListaner() {
                                        @Override
                                        public void typeSelect(SelectWindowModel item) {
                                            mSelectWindowModel = item;
                                            tv_bankName.setText(item.name);
                                        }
                                    });
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void showSelectCardPop() {
        if (cardList.size() > 0) {

            popCard.showLocation(rel_selectCard);

        } else {
            ToastUtil.showToast(QuXianActivity.this, "你还未添加任何提现方式");
            // 访问接口 获取记录
            Intent intent = new Intent(QuXianActivity.this, AddCardsActivity.class);
            startActivity(intent);
        }

    }

    private double getMaxMoney(double money, double balance) {
        Double serviceCharge = money * 0.01;
        Double operationAmount = money + serviceCharge;
        Double validMoney = 0.0;

        if (money < 100) {
            serviceCharge = 1.0;
            operationAmount = money + serviceCharge;
        }
        Double subMoney = BigDecimal.valueOf(balance).subtract(BigDecimal.valueOf(operationAmount)).doubleValue();
        if (subMoney < 0) {
            validMoney = BigDecimal.valueOf(balance - serviceCharge).setScale(BigDecimal.ROUND_DOWN, 2).doubleValue();
        }
        return validMoney;
    }

}
