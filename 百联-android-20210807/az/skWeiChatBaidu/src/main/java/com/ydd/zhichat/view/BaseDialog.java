package com.ydd.zhichat.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.ydd.zhichat.R;


/**
 * 弹框基类
 */

public abstract class BaseDialog extends Dialog {

    //region callback method

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContentViewId() <= 0) {
            throw new RuntimeException("layout resId undefind");
        }
        setContentView(getContentViewId());
        dm = context.getResources().getDisplayMetrics();
        init();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        isScale = setIsScale();
        if (isScale) {
            Window window = getWindow();
            widthScale = setWidthScale();
            if (widthScale == 0) {
                width = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                width = (int) (dm.widthPixels * widthScale);
            }

            heightScale = setHeightScale();
            if (heightScale == 0) {
                heightScale = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                height = (int) (dm.heightPixels * heightScale);
            }

            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = width;
            layoutParams.height = height;
            layoutParams.gravity = Gravity.CENTER;
            window.setAttributes(layoutParams);
        }

        enterAnim = setEnterAnim();
        if (enterAnim != null) {
            enterAnim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    superDismiss();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            enterAnim.start();
        }
    }

    @Override
    public void dismiss() {
        exitAnim = setExitAnim();
        if (exitAnim == null) {
            superDismiss();
            return;
        }
        exitAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                superDismiss();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                superDismiss();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        exitAnim.start();
    }

    //endregion

    //region other method'

    public BaseDialog(Context context) {
        super(context, R.style.DialogTransparent);
        this.context = context;
    }

    public void superDismiss() {
        super.dismiss();
    }

    //设置横向缩放比例
    protected abstract boolean setIsScale();

    //设置横向缩放比例
    protected abstract float setWidthScale();

    protected abstract float setHeightScale();

    //设置进入动画
    protected abstract AnimatorSet setEnterAnim();

    //设置退出动画
    protected abstract AnimatorSet setExitAnim();

    //初始化
    protected abstract void init(

    );

    //设置布局id
    protected abstract int getContentViewId();

    //endregion

    //region declare variable

    protected Context context;
    protected DisplayMetrics dm;
    protected float widthScale = 1;
    protected float heightScale = 0.6f;
    protected AnimatorSet enterAnim;
    protected AnimatorSet exitAnim;
    protected boolean isScale = true;
    protected int width;
    protected int height;

    //endregion

}
