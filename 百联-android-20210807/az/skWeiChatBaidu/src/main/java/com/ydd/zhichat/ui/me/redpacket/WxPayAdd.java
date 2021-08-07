package com.ydd.zhichat.ui.me.redpacket;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.EventPaySuccess;
import com.ydd.zhichat.bean.redpacket.Balance;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.me.redpacket.alipay.AlipayHelper;
import com.ydd.zhichat.util.CommonAdapter;
import com.ydd.zhichat.util.CommonViewHolder;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.EventBusHelper;
import com.ydd.zhichat.util.ToastUtil;
import com.tencent.mm.opensdk.constants.Build;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 微信充值
 */
public class WxPayAdd extends BaseActivity {
    private IWXAPI api;

    private ListView mRechargeListView;
    private RechargeAdapter mRechargeAdapter;
    private List<String> mRechargeList = new ArrayList<>();

    private TextView mSelectMonryTv;
    private int mSelectedPosition = 0;// 默认选中0.01元

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wx_pay_add);

        api = WXAPIFactory.createWXAPI(this, Constants.VX_APP_ID, false);
        api.registerApp(Constants.VX_APP_ID);

        initActionBar();
        initData();
        initView();

        EventBusHelper.register(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventPaySuccess message) {
        finish();
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
        tvTitle.setText(getString(R.string.recharge));
    }

    private void initData() {
        mRechargeList.add(String.valueOf("0.01"));
        mRechargeList.add(String.valueOf("1"));
        mRechargeList.add(String.valueOf("10"));
        mRechargeList.add(String.valueOf("50"));
        mRechargeList.add(String.valueOf("100"));
        mRechargeList.add(String.valueOf("500"));
        mRechargeList.add(String.valueOf("1000"));
        mRechargeList.add(String.valueOf("5000"));
    }

    private void initView() {
        mRechargeListView = findViewById(R.id.recharge_lv);
        mRechargeAdapter = new RechargeAdapter(this, mRechargeList);
        mRechargeListView.setAdapter(mRechargeAdapter);

        mSelectMonryTv = findViewById(R.id.select_money_tv);

        mRechargeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedPosition = position;
                mSelectMonryTv.setText("￥" + mRechargeList.get(mSelectedPosition));
                mRechargeAdapter.notifyDataSetChanged();
            }
        });

        findViewById(R.id.chongzhi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (api.getWXAppSupportAPI() < Build.PAY_SUPPORTED_SDK_INT) {
                    Toast.makeText(getApplicationContext(), R.string.tip_no_wechat, Toast.LENGTH_SHORT).show();
                } else {
                    recharge(String.valueOf(mRechargeList.get(mSelectedPosition)));
                }
            }
        });
        findViewById(R.id.chongzhifubao).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlipayHelper.recharge(WxPayAdd.this, coreManager, mRechargeList.get(mSelectedPosition));
            }
        });
    }

    private void recharge(String money) {// 调用服务端接口，由服务端统一下单
        DialogHelper.showDefaulteMessageProgressDialog(this);

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("price", money);
        params.put("payType", "2");// 支付方式 1.支付宝 2.微信

        HttpUtils.get().url(coreManager.getConfig().VX_RECHARGE)
                .params(params)
                .build()
                .execute(new BaseCallback<Balance>(Balance.class) {

                    @Override
                    public void onResponse(ObjectResult<Balance> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            PayReq req = new PayReq();
                            req.appId = result.getData().getAppId();
                            req.partnerId = result.getData().getPartnerId();
                            req.prepayId = result.getData().getPrepayId();
                            req.packageValue = "Sign=WXPay";
                            req.nonceStr = result.getData().getNonceStr();
                            req.timeStamp = result.getData().getTimeStamp();
                            req.sign = result.getData().getSign();
                            api.sendReq(req);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(WxPayAdd.this);
                    }
                });
    }


    class RechargeAdapter extends CommonAdapter<String> {

        public RechargeAdapter(Context context, List<String> data) {
            super(context, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CommonViewHolder viewHolder = CommonViewHolder.get(mContext, convertView, parent,
                    R.layout.item_recharge, position);
            TextView mMoneyTv = viewHolder.getView(R.id.money);
            ImageView mCheckIv = viewHolder.getView(R.id.check);

            mMoneyTv.setText(mRechargeList.get(position) + "元");
            if (mSelectedPosition == position) {
                mCheckIv.setVisibility(View.VISIBLE);
            } else {
                mCheckIv.setVisibility(View.GONE);
            }

            return viewHolder.getConvertView();
        }
    }
}
