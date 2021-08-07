package com.ydd.zhichat.ui.me.redpacket;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.redpacket.SelectWindowModel;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.SelectBankPop;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;

import static com.ydd.zhichat.AppConstant.ZHI_FU_BAO;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_GONGSHANG_YINHANG;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_JIANSHE_YINHANG;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_JIAOTONG_YINHANG;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_NONGYE_YINHANG;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_YINHANG;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_YOUZHENG_YINHANG;

public class AddCardsActivity extends BaseActivity {

    private Button btn_bind;
    private TextView tv_bank;
    private EditText input_name,input_card_num,input_city;
    private LinearLayout select_bank;
    private SelectBankPop popBank;
    private ArrayList bankList = new ArrayList<SelectWindowModel>();
    private SelectWindowModel selectWindowModel = null;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_cards);

        initActionBar();
        initView();
        initData();
    }

    private void initData() {
        initBanks();
    }

    private void initView() {
        select_bank = findViewById(R.id.select_bank);
        btn_bind = findViewById(R.id.btn_bind);
        tv_bank = findViewById(R.id.tv_bank);
        input_name = findViewById(R.id.input_name);
        input_card_num = findViewById(R.id.input_card_num);
        input_city = findViewById(R.id.input_city);
        popBank = new SelectBankPop(this, bankList);

        select_bank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popBank.showLocation(btn_bind);
            }
        });

        popBank.setOnTypeSelectListaner(new SelectBankPop.OnTypeSelectListaner() {
            @Override
            public void typeSelect(SelectWindowModel item) {
                selectWindowModel = item;
                tv_bank.setText(item.name);
            }
        });

        btn_bind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bind();
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
        mTvTitle.setText("添加银行卡");
    }

    private void initBanks() {
        SelectWindowModel selectWindowModel1 = new SelectWindowModel();
        selectWindowModel1.name = "支付宝";
        selectWindowModel1.id = ZHI_FU_BAO;
        selectWindowModel1.icon = R.drawable.treasure;

        SelectWindowModel selectWindowModel2 = new SelectWindowModel();
        selectWindowModel2.name = "中国银行";
        selectWindowModel2.id = ZHONG_GUO_YINHANG;
        selectWindowModel2.icon = R.drawable.ic_card_boc;

        SelectWindowModel selectWindowModel3 = new SelectWindowModel();
        selectWindowModel3.name = "中国建设银行";
        selectWindowModel3.id = ZHONG_GUO_JIANSHE_YINHANG;
        selectWindowModel3.icon = R.drawable.ic_card_ccb;

        SelectWindowModel selectWindowModel4 = new SelectWindowModel();
        selectWindowModel4.name = "中国工商银行";
        selectWindowModel4.id = ZHONG_GUO_GONGSHANG_YINHANG;
        selectWindowModel4.icon = R.drawable.ic_card_icbc;

        SelectWindowModel selectWindowModel5 = new SelectWindowModel();
        selectWindowModel5.name = "中国农业银行";
        selectWindowModel5.id = ZHONG_GUO_NONGYE_YINHANG;
        selectWindowModel5.icon = R.drawable.ic_card_abc;

        SelectWindowModel selectWindowModel6 = new SelectWindowModel();
        selectWindowModel6.name = "中国交通银行";
        selectWindowModel6.id = ZHONG_GUO_JIAOTONG_YINHANG;
        selectWindowModel6.icon = R.drawable.ic_card_comm;

        SelectWindowModel selectWindowModel7 = new SelectWindowModel();
        selectWindowModel7.name = "中国邮政银行";
        selectWindowModel7.id = ZHONG_GUO_YOUZHENG_YINHANG;
        selectWindowModel7.icon = R.drawable.ic_card_psbc;

        bankList.add(selectWindowModel1);
        bankList.add(selectWindowModel2);
        bankList.add(selectWindowModel3);
        bankList.add(selectWindowModel4);
        bankList.add(selectWindowModel5);
        bankList.add(selectWindowModel6);
        bankList.add(selectWindowModel7);
    }

    private void bind(){

        String userName = input_name.getText().toString();
        String cardNum = input_card_num.getText().toString();
        String openBankAddr = input_city.getText().toString();

        if (userName == null || TextUtils.isEmpty(userName)) {
            ToastUtil.showLongToast(this,"持卡人不能为空");
        } else if (cardNum == null || TextUtils.isEmpty(cardNum) || cardNum.length() < 0) {
            ToastUtil.showLongToast(this,"卡号不能为空");
        } else if (cardNum.length() > 30) {
            ToastUtil.showLongToast(this,"卡号不能大于30位");
        } else if(selectWindowModel == null){
            ToastUtil.showLongToast(this,"请选择银行类型");
        }else {
            if (openBankAddr == null || TextUtils.isEmpty(openBankAddr)) {
                openBankAddr = "未填写";
            }
            int brandId = selectWindowModel.id;
            String bankName = selectWindowModel.name;
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("bankBrandId", brandId+"");
            params.put("brandName", bankName);
            params.put("cardName", "");
            params.put("cardNo", cardNum);
            params.put("cardType", "0");
            params.put("openBankAddr", openBankAddr);
            params.put("uid", coreManager.getSelf().getUserId());
            params.put("userName", userName);
            HttpUtils.post().url(coreManager.getConfig().BIND_CARD)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {
                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtil.showToast(AddCardsActivity.this,result.getResultMsg());
                                    finish();
                                }
                            });
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                            ToastUtil.showErrorNet(AddCardsActivity.this);
                        }
                    });
        }
    }
}
