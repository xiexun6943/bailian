package com.ydd.zhichat.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Created by 魏正旺 on 2016/9/9.
 * <p>
 * 用于监听红包和充值的输入字符，检测输入的
 * 数字只能是小数点后两位
 */
public class InputChangeListener implements TextWatcher {
    private EditText editRecharge;

    public InputChangeListener(EditText editRecharge) {
        this.editRecharge = editRecharge;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before,
                              int count) {
        if (s.toString().contains(".")) {
            if (s.length() - 1 - s.toString().indexOf(".") > 2) {
                s = s.toString().subSequence(0,
                        s.toString().indexOf(".") + 3);
                editRecharge.setText(s);
                editRecharge.setSelection(s.length());
            }
        }
        if (s.toString().trim().substring(0).equals(".")) {
            s = "0" + s;
            editRecharge.setText(s);
            editRecharge.setSelection(2);
        }

        if (s.toString().startsWith("0")
                && s.toString().trim().length() > 1) {
            if (!s.toString().substring(1, 2).equals(".")) {
                editRecharge.setText(s.subSequence(0, 1));
                editRecharge.setSelection(1);
                return;
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        // TODO Auto-generated method stub
    }
}
