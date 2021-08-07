package com.ydd.zhichat.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.util.SkinUtils;

public class TabView implements View.OnClickListener {
    private Context mContext;
    private View mView;
    private TextView attention_each_tv;
    private TextView attention_single_tv;
    private boolean isfriend;

    public TextView getAttention_each_tv() {
        return attention_each_tv;
    }

    public TextView getAttention_single_tv() {
        return attention_single_tv;
    }

    public TabView(Context context) {
        mContext = context;
        initView();
    }

    public TabView(Context context, boolean isnotgroup) {
        mContext = context;
        isfriend = isnotgroup;
        initView();
    }

    private void initView() {
        mView = LayoutInflater.from(mContext).inflate(R.layout.actionbar_tag_second, null);
        View view1 = findviewbyid(R.id.tag1);
        view1.setBackgroundColor(SkinUtils.getSkin(mContext).getAccentColor());
        View view2 = findviewbyid(R.id.tag2);
        view2.setBackgroundColor(SkinUtils.getSkin(mContext).getAccentColor());
        attention_each_tv = findviewbyid(R.id.attention_each_tv);
        attention_single_tv = findviewbyid(R.id.attention_single_tv);
        if (isfriend) {
            attention_each_tv.setText(InternationalizationHelper.getString("PSMyViewController_MyFirend"));
            attention_single_tv.setText(InternationalizationHelper.getString("JX_BlackList"));
        } else {
            attention_each_tv.setText(InternationalizationHelper.getString("MY_GROUP"));
            attention_single_tv.setText(InternationalizationHelper.getString("ALL_GROUP"));
        }
        attention_each_tv.setOnClickListener(this);
        attention_single_tv.setOnClickListener(this);
        hideIndexView();
        showViewByTag(0);
    }

    public View getView() {
        return mView;
    }

    private int index = 0;

    @Override
    public void onClick(View v) {
        int beforeIndex = index;
        switch (v.getId()) {
            case R.id.attention_each_tv:
                index = 0;
                break;
            case R.id.attention_single_tv:
                index = 1;
                break;
        }
        if (beforeIndex == index)
            return;
        hideIndexView();
        showViewByTag(index);
        onTabSelectedLisenter.onSelected(index);
    }

    public void callOnSelect(int index) {
        View view;
        switch (index) {
            case 0:
                view = findviewbyid(R.id.attention_each_tv);
                break;
            case 1:
                view = findviewbyid(R.id.attention_single_tv);
                break;
            default:
                view = findviewbyid(R.id.attention_each_tv);
        }
        onClick(view);
    }

    private OnTabSelectedLisenter onTabSelectedLisenter;

    public void setOnTabSelectedLisenter(OnTabSelectedLisenter onTabSelectedLisenter) {
        this.onTabSelectedLisenter = onTabSelectedLisenter;
    }

    public interface OnTabSelectedLisenter {
        void onSelected(int index);
    }

    public <T> T findviewbyid(int id) {
        return (T) mView.findViewById(id);
    }

    public String getString(int rid) {
        return mContext.getResources().getString(rid);
    }

    public void hideIndexView() {
        hideViewByTag(0);
        hideViewByTag(1);
    }

    public void hideViewByTag(int tag) {
        mView.findViewWithTag(tag + "").setVisibility(View.INVISIBLE);
    }

    public void showViewByTag(int tag) {
        mView.findViewWithTag(tag + "").setVisibility(View.VISIBLE);
    }
}
