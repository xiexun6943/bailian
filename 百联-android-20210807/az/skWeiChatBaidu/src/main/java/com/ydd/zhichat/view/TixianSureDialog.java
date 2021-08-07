package com.ydd.zhichat.view;

import android.animation.AnimatorSet;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.redpacket.SelectWindowModel;
import com.ydd.zhichat.util.StrNumUtil;

/**
 * Created by phy on 2020/1/6
 */
public class TixianSureDialog extends BaseDialog{

    private Context mContext;
    private TextView tv_tip1, tv_tip2, tv_tip3, tv_bankName, tv_cardNo,tv_tixian,tv_fee,tv_tixianBoth;
    private Button btnCancel;
    private Button btnSure;
    private ImageView img_bank;
    private OnClickListenner listenner;

    private SelectWindowModel mSelectWindowModel = null;
    public  String mBoth;// 提现金额 单位:分
    public  String mAmount;// 提现金额 单位:分
    public  String mFee;// 手续费 单位:分


    public TixianSureDialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected boolean setIsScale() {
        return false;
    }

    public void setListenner(OnClickListenner listenner) {
        this.listenner = listenner;
    }

    @Override
    protected float setWidthScale() {
        return 0.8f;
    }

    @Override
    protected float setHeightScale() {
        return 0.6f;
    }

    @Override
    protected AnimatorSet setEnterAnim() {
        return null;
    }

    @Override
    protected AnimatorSet setExitAnim() {
        return null;
    }

    @Override
    protected void init() {
        this.setCanceledOnTouchOutside(true);
        tv_tip1 = findViewById(R.id.tv_tip1);
        tv_tip2 = findViewById(R.id.tv_tip2);
        tv_bankName = findViewById(R.id.tv_bankName);
        tv_cardNo = findViewById(R.id.tv_cardNo);
        tv_tixianBoth = findViewById(R.id.tv_tixianBoth);
        tv_tixian = findViewById(R.id.tv_tixian);
        tv_fee = findViewById(R.id.tv_fee);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSure = findViewById(R.id.btn_sure);
        img_bank = findViewById(R.id.img_bank);

        setCancelable(false);
        setCanceledOnTouchOutside(false);
        setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK)
                    return false;
                return false;
            }
        });

        img_bank.setImageResource(mSelectWindowModel.icon);
        tv_bankName.setText(mSelectWindowModel.name);
//        tv_cardNo.setText(StringUtils.hideCardNo(mSelectWindowModel.cardNum));
        tv_cardNo.setText(mSelectWindowModel.cardNum);
        tv_tixian.setText("￥"+ StrNumUtil.keepTwoDecimal(mAmount));
        tv_fee.setText("￥"+ StrNumUtil.keepTwoDecimal(mFee));
        tv_tixianBoth.setText("￥"+ StrNumUtil.keepTwoDecimal(mBoth));

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listenner.onCancel();
            }
        });
        btnSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listenner.onSure();
            }
        });
    }

    public void show(SelectWindowModel selectWindowModel,String both,String amount,String fee) {
        mSelectWindowModel = selectWindowModel;
        mAmount = amount;
        mFee = fee;
        mBoth = both;

        show();
    }

    @Override
    protected int getContentViewId() {
        return R.layout.dialog_tixian;
    }

    public interface OnClickListenner {
        void onSure();
        void onCancel();
    }
}
