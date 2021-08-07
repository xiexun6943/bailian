package com.ydd.zhichat.ui.me.redpacket;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.redpacket.Balance;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.MergerStatus;
import com.ydd.zhichat.view.SkinImageView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.text.DecimalFormat;
import java.util.HashMap;

import okhttp3.Call;

public class WxPayBlance extends BaseActivity {

    public static final String RSA_PRIVATE = "";
    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_AUTH_FLAG = 2;
    private TextView mBalanceTv;
    private RelativeLayout mRechargeTv;
    private TextView mWithdrawTv;
    private RelativeLayout rel_bill;
    private RelativeLayout rel_myCrad;
    private MergerStatus mergerStatus;//
//    private LinearLayout lin_bill;
//    private LinearLayout lin_myCrad;
    private LinearLayout lin_forgetPassword;
//    private LinearLayout lin_chongzhi;
//    private LinearLayout lin_tixian;
    RelativeLayout hongbao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_wx_pay_blance);
        setContentView(R.layout.activity_wx_wallte);
      //  initActionBar();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        SkinImageView iv_title_left = findViewById(R.id.iv_title_left);
        iv_title_left.setLeftBackImageColor(Color.BLACK);
        mergerStatus = findViewById(R.id.mergerStatus);
        mTvTitle.setText(getString(R.string.my_purse));
        mTvTitle.setTextColor(Color.BLACK);
//        TextView mTvTitleRight = (TextView) findViewById(R.id.tv_title_right);
//        mTvTitleRight.setVisibility(View.GONE);
//        mTvTitleRight.setText(getString(R.string.expenses_record));
       // mergerStatus.setBackgroundResource(R.color.main_color);

    }

    private void initView() {
//        mBalanceTv = (TextView) findViewById(R.id.myblance);
//        mRechargeTv = (TextView) findViewById(R.id.chongzhi);
//        mWithdrawTv = (TextView) findViewById(R.id.quxian);
        hongbao=findViewById(R.id.hongbao);
        mBalanceTv = (TextView) findViewById(R.id.tv_balance);
        mRechargeTv = findViewById(R.id.tv_recharge);
        mWithdrawTv = (TextView) findViewById(R.id.tv_withdrawal);
        rel_bill =  findViewById(R.id.rel_bill);
        rel_myCrad =  findViewById(R.id.rel_myCrad);
//        lin_bill =  findViewById(R.id.lin_bill);
//        lin_myCrad =  findViewById(R.id.lin_myCrad);
        lin_forgetPassword =  findViewById(R.id.lin_forgetPassword);
//        lin_chongzhi =  findViewById(R.id.lin_chongzhi);
//        lin_tixian =  findViewById(R.id.lin_tixian);
        SkinImageView iv_title_left = findViewById(R.id.iv_title_left);
        iv_title_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        hongbao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WxPayBlance.this, MyConsumeRecord.class);
                startActivity(intent);
            }
        });

        mRechargeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(WxPayBlance.this, WxPayAdd.class);
                Intent intent = new Intent(WxPayBlance.this, RechargeActivity.class);
                startActivity(intent);
            }
        });

        mWithdrawTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WxPayBlance.this, QuXianActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.rel_changePassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WxPayBlance.this, PasswordManagerActivity.class);
                startActivity(intent);
            }
        });

        rel_bill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 访问接口 获取记录
                Intent intent = new Intent(WxPayBlance.this, MyConsumeRecord.class);
                startActivity(intent);
            }
        });

        rel_myCrad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 访问接口 获取记录
                Intent intent = new Intent(WxPayBlance.this, MyCardsActivity.class);
                startActivity(intent);
            }
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(getString(R.string.my_purse));
        mTvTitle.setTextColor(Color.BLACK);
    }

    private void initData() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().RECHARGE_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<Balance>(Balance.class) {

                    @Override
                    public void onResponse(ObjectResult<Balance> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            DecimalFormat decimalFormat = new DecimalFormat("0.00");
                            Balance balance = result.getData();
                            coreManager.getSelf().setBalance(Double.parseDouble(decimalFormat.format(balance.getBalance())));
                            mBalanceTv.setText("￥" + decimalFormat.format(Double.parseDouble(decimalFormat.format(balance.getBalance()))));
                        } else {
                            ToastUtil.showErrorData(WxPayBlance.this);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(WxPayBlance.this);
                    }
                });
    }

}
