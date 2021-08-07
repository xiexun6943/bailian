package com.ydd.zhichat.view;

import android.content.Context;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.ydd.zhichat.R;
import com.ydd.zhichat.util.SkinUtils;

/**
 * 抽象出标题栏上的控件以实现根据皮肤切换深色浅色，
 */
public class SkinImageView extends AppCompatImageView {
    public SkinImageView(Context context) {
        super(context);
        init();
    }

    public SkinImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SkinImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        SkinUtils.Skin skin = SkinUtils.getSkin(getContext());
        if (skin.isLight()) {
            ImageViewCompat.setImageTintList(this, getContext().getResources().getColorStateList(R.color.black));
        }
    }

    public void setLeftBackImageColor(int ResId){
        ImageViewCompat.setImageTintList(this, getContext().getResources().getColorStateList(ResId));
    }
}
