package com.ydd.zhichat.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;


import com.ydd.zhichat.R;
import com.ydd.zhichat.adapter.SelectCardAdapter;
import com.ydd.zhichat.bean.redpacket.SelectWindowModel;

import java.util.ArrayList;

public class SelectCardPop extends PopupWindow {
    private View popView;
    private Activity activity;
    private ArrayList<SelectWindowModel> list = new ArrayList<>();
    private RecyclerView rv_bank;
    private SelectCardAdapter adapter;
    private SelectWindowModel mSelectItem;
    private OnTypeSelectListaner onTypeSelectListaner;

    public SelectCardPop(final Activity activity, ArrayList<SelectWindowModel> list) {
        super(activity);
        this.activity = activity;
        this.list = list;
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        popView = inflater.inflate(R.layout.pop_select_bank, null);// 加载菜单布局文件
        this.setContentView(popView);// 把布局文件添加到popupwindow中
        this.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);// 设置菜单的宽度（需要和菜单于右边距的距离搭配，可以自己调到合适的位置）
        this.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        this.setFocusable(true);// 获取焦点
        this.setTouchable(true); // 设置PopupWindow可触摸
        this.setOutsideTouchable(true); // 设置非PopupWindow区域可触摸
        ColorDrawable dw = new ColorDrawable(0x00000000);
        this.setBackgroundDrawable(dw);
        this.setAnimationStyle(R.style.popwindow_bottom_anim_style); // 设置动画
        this.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                setBackgroundAlpha(activity, 1f);
            }
        });
        adapter = new SelectCardAdapter(list);
        rv_bank = popView.findViewById(R.id.rv_bank);
        LinearLayoutManager layOutMannager = new LinearLayoutManager(activity);
        rv_bank.setLayoutManager(layOutMannager);
        rv_bank.setAdapter(adapter);

        initListener();

    }

    public void setOnTypeSelectListaner(OnTypeSelectListaner onTypeSelectListaner) {
        this.onTypeSelectListaner = onTypeSelectListaner;
    }

    public void initListener() {
        adapter.setOnItemClickListener(new SelectCardAdapter.OnItemClickListener() {
            @Override
            public void onItemBack(SelectWindowModel item, int position) {
                mSelectItem = item;
                adapter.setSelectedPosition(position);
                adapter.notifyDataSetChanged();
                onTypeSelectListaner.typeSelect(item);
                dismiss();
            }
        });
    }

    /**
     * 设置背景色
     */
    private void setBackgroundAlpha(Activity context, float bgAlpha) {
        WindowManager.LayoutParams lp = context.getWindow().getAttributes();
        lp.alpha = bgAlpha;
        context.getWindow().setAttributes(lp);
    }

    /**
     * pop设置显示的位置
     */
    public void showLocation(View anchorView) {
        this.showAtLocation(anchorView, Gravity.BOTTOM, 0, 0);
        setBackgroundAlpha(activity, 0.7f);
    }

    public interface OnTypeSelectListaner {
        void typeSelect(SelectWindowModel item);
    }

}

