package com.ydd.zhichat.view;

import android.animation.AnimatorSet;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.redpacket.CardBean;

/**
 * Created by phy on 2020/1/13
 */
public class EditBankDialog extends BaseDialog {
    private Context mContext;
    EditText et_transferNote;
    Button btnCancel;
    Button btn_sure;
    TextView tv_name;
    TextView tv_bank;
    TextView tv_no;
    private int position;
    private CardBean cardBean;
    private EditBankDialog.OnClickListenner listenner;

    public EditBankDialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected boolean setIsScale() {
        return true;
    }

    public void setListenner(EditBankDialog.OnClickListenner listenner) {
        this.listenner = listenner;
    }

    @Override
    protected float setWidthScale() {
        return 0.8f;
    }

    @Override
    protected float setHeightScale() {
        return 0.4f;
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
        btnCancel = findViewById(R.id.btn_cancel);
        btn_sure = findViewById(R.id.btn_change);
        tv_name = findViewById(R.id.tv_name);
        tv_bank = findViewById(R.id.tv_bank);
        tv_no = findViewById(R.id.tv_no);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        btn_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listenner.onSure(position);
            }
        });

        this.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                showKeyboard();
            }
        });
    }

    private void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(et_transferNote, 0);
    }

    public void show(int position,CardBean cardBean) {
        this.position = position;
        show();
        tv_name.setText(cardBean.getUserName());
        tv_bank.setText(cardBean.getBankBrandName());
        tv_no.setText(cardBean.getCardNo());
    }


    @Override
    protected int getContentViewId() {
        return R.layout.dialog_edit_bank;
    }

    public interface OnClickListenner {
        void onSure(int position);
    }
}


