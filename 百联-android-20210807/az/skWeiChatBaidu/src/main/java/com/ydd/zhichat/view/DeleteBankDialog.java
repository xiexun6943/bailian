package com.ydd.zhichat.view;

import android.animation.AnimatorSet;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.ydd.zhichat.R;


public class DeleteBankDialog extends BaseDialog {
    private Context mContext;
    EditText et_transferNote;
    Button btnCancel;
    Button btn_sure;
    private int position;
    private OnClickListenner listenner;

    public DeleteBankDialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected boolean setIsScale() {
        return true;
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
        return 0.2f;
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

        this.setOnShowListener(new OnShowListener() {
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

    public void show(int position) {
        this.position = position;
        show();
    }

    @Override
    protected int getContentViewId() {
        return R.layout.dialog_delete_bank;
    }

    public interface OnClickListenner {
        void onSure(int position);
    }
}

