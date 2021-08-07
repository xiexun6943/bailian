package com.ydd.zhichat.helper;

import android.text.InputType;
import android.widget.EditText;
import android.widget.ToggleButton;

/**
 * Created by phy on 2020/3/22
 */
public class PayPasswordHelper {
    private static final int INVISIBLE_TYPE = InputType.TYPE_CLASS_TEXT | InputType.TYPE_NUMBER_VARIATION_PASSWORD;
    private static final int VISIBLE_TYPE = InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER;

    private PayPasswordHelper() {
    }

    public static void bindPasswordEye(EditText et, ToggleButton eye) {
        // checked 为 true表示眼睛睁开可以看见密码的情况，
        et.setInputType(INVISIBLE_TYPE);
        eye.setChecked(false);
        eye.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                et.setInputType(VISIBLE_TYPE);
            } else {
                et.setInputType(INVISIBLE_TYPE);
            }
        });
    }
}
