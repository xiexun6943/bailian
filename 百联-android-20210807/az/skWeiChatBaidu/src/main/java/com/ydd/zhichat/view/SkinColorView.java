package com.ydd.zhichat.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

import com.ydd.zhichat.util.SkinUtils;

public class SkinColorView extends View {

    public SkinColorView(Context context) {
        super(context);
        init();
    }

    public SkinColorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SkinColorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SkinColorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        SkinUtils.Skin skin = SkinUtils.getSkin(getContext());
        setBackgroundColor(skin.getAccentColor());
    }
}
