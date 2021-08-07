package com.ydd.zhichat.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.db.InternationalizationHelper;

public class BasicInfoWindow extends PopupWindow {
    private TextView setName, addBlackList, removeBlackList, delete, reportTv;
    private View mMenuView;

    public BasicInfoWindow(FragmentActivity context, OnClickListener itemsOnClick, Friend friend) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMenuView = inflater.inflate(R.layout.popu_basicinfo, null);

        setName = (TextView) mMenuView.findViewById(R.id.set_remark_nameS);
        addBlackList = (TextView) mMenuView.findViewById(R.id.add_blacklist);
        delete = (TextView) mMenuView.findViewById(R.id.delete_tv);
        int status = Friend.STATUS_UNKNOW;
        if (friend != null) {
            status = friend.getStatus();
        }
        if (status != Friend.STATUS_FRIEND) {
            // 不是好友不能拉黑，比如公众号不能拉黑，
            addBlackList.setVisibility(View.GONE);
            mMenuView.findViewById(R.id.add_blacklist_v).setVisibility(View.GONE);
            // 不是好友不能删除好友，
            delete.setVisibility(View.GONE);
            mMenuView.findViewById(R.id.delete_tv_v).setVisibility(View.GONE);
        }
        removeBlackList = (TextView) mMenuView.findViewById(R.id.remove_blacklist);
        if (status != Friend.STATUS_BLACKLIST) {
            // 不是黑名单不能移出黑名单，
            removeBlackList.setVisibility(View.GONE);
            mMenuView.findViewById(R.id.remove_blacklist_v).setVisibility(View.GONE);
        }
        reportTv = (TextView) mMenuView.findViewById(R.id.report_tv);
        setName.setText(InternationalizationHelper.getString("JXUserInfoVC_SetName"));
        addBlackList.setText(InternationalizationHelper.getString("JXUserInfoVC_AddBlackList"));
        removeBlackList.setText(InternationalizationHelper.getString("REMOVE"));
        delete.setText(InternationalizationHelper.getString("JXUserInfoVC_DeleteFirend"));
        //设置按钮监听
        setName.setOnClickListener(itemsOnClick);
        addBlackList.setOnClickListener(itemsOnClick);
        removeBlackList.setOnClickListener(itemsOnClick);
        delete.setOnClickListener(itemsOnClick);
        reportTv.setOnClickListener(itemsOnClick);

        this.setContentView(mMenuView);
        this.setWidth(LayoutParams.WRAP_CONTENT);
        this.setHeight(LayoutParams.WRAP_CONTENT);
        this.setFocusable(true);

        this.setOutsideTouchable(true);

        this.setAnimationStyle(R.style.Buttom_Popwindow);
        ColorDrawable dw = new ColorDrawable(0000000000);
        this.setBackgroundDrawable(dw);
       /* mMenuView.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
                int bottom = mMenuView.findViewById(R.id.pop_layout).getBottom();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (y < height) {
                        dismiss();
                    } else if (y > bottom) {
                        dismiss();
                    }
                }
                return true;
            }
        });*/
    }
}
