package com.ydd.zhichat.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.helper.ShareSdkHelper;
import com.ydd.zhichat.ui.base.BaseActivity;

public class SharePopupWindow extends PopupWindow implements OnClickListener {
    private BaseActivity mContent;

    public SharePopupWindow(BaseActivity context) {
        this.mContent = context;
        initView();
    }

    private void initView() {
        LayoutInflater inflater = (LayoutInflater) mContent.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View mMenuView = inflater.inflate(R.layout.view_share, null);
        setContentView(mMenuView);
        setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);

        setFocusable(true);
        setAnimationStyle(R.style.Buttom_Popwindow);

        // 因为某些机型是虚拟按键的,所以要加上以下设置防止挡住按键.
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mMenuView.findViewById(R.id.platformshare_wechat).setOnClickListener(this);
        mMenuView.findViewById(R.id.platformshare_moment).setOnClickListener(this);
        mMenuView.findViewById(R.id.cancel).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        dismiss();
        switch (v.getId()) {
            case R.id.platformshare_wechat:
                ShareSdkHelper.shareWechat(mContent, MyApplication.getContext().getString(R.string.app_name) + mContent.getString(R.string.suffix_share_content),
                        MyApplication.getContext().getString(R.string.app_name) + mContent.getString(R.string.suffix_share_content),
                        mContent.coreManager.getConfig().website);
                break;
            case R.id.platformshare_moment:
                ShareSdkHelper.shareWechatMoments(mContent, MyApplication.getContext().getString(R.string.app_name) + mContent.getString(R.string.suffix_share_content),
                        MyApplication.getContext().getString(R.string.app_name) + mContent.getString(R.string.suffix_share_content),
                        mContent.coreManager.getConfig().website);
                break;
            case R.id.cancel:
                break;
        }
    }
}
