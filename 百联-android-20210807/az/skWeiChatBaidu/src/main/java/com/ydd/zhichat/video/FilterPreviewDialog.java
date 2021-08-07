package com.ydd.zhichat.video;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.joe.camera2recorddemo.Entity.FilterInfo;
import com.joe.camera2recorddemo.OpenGL.Filter.ChooseFilter;
import com.ydd.zhichat.R;
import com.ydd.zhichat.ui.base.BaseRecViewHolder;
import com.ydd.zhichat.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.HORIZONTAL;

/**
 * 滤镜选择器
 * create by xuan
 * time :2018-11-26 17:47:08
 */

public class FilterPreviewDialog extends Dialog {

    private Context mContext;
    private OnUpdateFilterListener mListener;
    private List<FilterInfo> mdatas;

    private RecyclerView mListView;
    private CommAvatarAdapter mAdapter;
    private int currt = 0;

    public FilterPreviewDialog(Context context, OnUpdateFilterListener listener) {
        super(context, R.style.TrillDialog);
        this.mContext = context;
        initDatas();
        mListener = listener;
    }

    private void initDatas() {
        mdatas = new ArrayList<>();

        mdatas.add(new FilterInfo(ChooseFilter.FilterType.NORMAL, "默认", R.drawable.ic_filter_pre0));
        mdatas.add(new FilterInfo(ChooseFilter.FilterType.COOL, "寒冷", R.drawable.ic_filter_pre1));
        mdatas.add(new FilterInfo(ChooseFilter.FilterType.WARM, "温暖", R.drawable.ic_filter_pre2));
        mdatas.add(new FilterInfo(ChooseFilter.FilterType.GRAY, "灰度", R.drawable.ic_filter_pre3));
        mdatas.add(new FilterInfo(ChooseFilter.FilterType.CAMEO, "浮雕", R.drawable.ic_filter_pre4));
        mdatas.add(new FilterInfo(ChooseFilter.FilterType.INVERT, "底片", R.drawable.ic_filter_pre5));
        mdatas.add(new FilterInfo(ChooseFilter.FilterType.SEPIA, "旧照", R.drawable.ic_filter_pre6));
        mdatas.add(new FilterInfo(ChooseFilter.FilterType.TOON, "动画", R.drawable.ic_filter_pre7));
        mdatas.add(new FilterInfo(ChooseFilter.FilterType.CONVOLUTION, "卷积", R.drawable.ic_filter_pre8));
        mdatas.add(new FilterInfo(ChooseFilter.FilterType.SOBEL, "边缘", R.drawable.ic_filter_pre9));
        mdatas.add(new FilterInfo(ChooseFilter.FilterType.SKETCH, "素描", R.drawable.ic_filter_pre10));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_filter_list);
        setCanceledOnTouchOutside(true);
        initView();
        initListview();
    }

    private void initListview() {
        mListView = findViewById(R.id.rv_filter);
        mListView.setLayoutManager(new LinearLayoutManager(mContext, HORIZONTAL, false));
        mAdapter = new CommAvatarAdapter();
        mListView.setAdapter(mAdapter);
    }

    private void initView() {
        Window o = getWindow();
        WindowManager.LayoutParams lp = o.getAttributes();
        lp.width = ScreenUtil.getScreenWidth(getContext());
        o.setAttributes(lp);
        this.getWindow().setGravity(Gravity.BOTTOM);
        this.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
    }

    @Override
    public void show() {
        super.show();
        Log.e("xuan", "show: " + this.mdatas.size());
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (mListener != null) {
            mListener.dismiss();
        }
    }

    public interface OnUpdateFilterListener {
        void select(int type);

        void dismiss();
    }

    class CommAvatarAdapter extends RecyclerView.Adapter<FilterInfoHolder> {

        @NonNull
        @Override
        public FilterInfoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_filter, null);
            return new FilterInfoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FilterInfoHolder holder, int position) {
            holder.tvName.setText(mdatas.get(position).getName());
            holder.ivImage.setImageResource(mdatas.get(position).getRid());

            if (currt == position) {
                holder.iv_select_bg.setVisibility(View.VISIBLE);
                holder.ivSelect.setVisibility(View.VISIBLE);
            } else {
                holder.iv_select_bg.setVisibility(View.GONE);
                holder.ivSelect.setVisibility(View.GONE);
            }
            Log.e("xuan", "onBindViewHolder: " + mdatas.get(position).getRid());
        }

        @Override
        public int getItemCount() {
            return mdatas.size();
        }
    }

    public class FilterInfoHolder extends BaseRecViewHolder {
        public ImageView ivImage;
        public ImageView ivSelect;
        public TextView tvName;
        public FrameLayout mLlWrap;
        public ImageView iv_select_bg;

        public FilterInfoHolder(View rootView) {
            super(rootView);
            iv_select_bg = rootView.findViewById(R.id.iv_select_bg);
            tvName = rootView.findViewById(R.id.tv_name);
            ivImage = rootView.findViewById(R.id.iv_image);
            ivSelect = rootView.findViewById(R.id.iv_select);
            mLlWrap = rootView.findViewById(R.id.ll_wrap);
            mLlWrap.setOnClickListener(v -> {
                Log.e("xuan", "onReply: 选择了 " + getAdapterPosition());
                if (mListener != null) {
                    int type = mdatas.get(getAdapterPosition()).getType();
                    mListener.select(type);
                    mAdapter.notifyItemChanged(currt);
                    currt = getAdapterPosition();
                    mAdapter.notifyItemChanged(currt);
                }
            });
        }
    }
}
