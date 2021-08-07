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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.luo.camfilter.GPUCamImgOperator;
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

public class PictrueFilterPreviewDialog extends Dialog {

    private final GPUCamImgOperator.GPUImgFilterType[] types = new GPUCamImgOperator.GPUImgFilterType[]{
            GPUCamImgOperator.GPUImgFilterType.NONE,
            GPUCamImgOperator.GPUImgFilterType.HEALTHY,
            GPUCamImgOperator.GPUImgFilterType.NOSTALGIA,
            GPUCamImgOperator.GPUImgFilterType.COOL,
            GPUCamImgOperator.GPUImgFilterType.EMERALD,
            GPUCamImgOperator.GPUImgFilterType.EVERGREEN,
            GPUCamImgOperator.GPUImgFilterType.CRAYON
    };
    protected XSeekBar mFaceSurgeryFaceShapeSeek;
    protected XSeekBar mFaceSurgeryBigEyeSeek;
    protected XSeekBar mSkinSmoothSeek;
    protected XSeekBar mSkinWihtenSeek;
    protected XSeekBar mRedFaceSeek;
    private Context mContext;
    private OnUpdateFilterListener mListener;
    private List<FilterInfo> mdatas;
    private RecyclerView mListView;
    private CommAvatarAdapter mAdapter;
    private int currt = 0;
    private SeekBarFilterListener seekBarFilterListener;

    private XSeekBar.OnChangeListener onSeekBarChangeListener = new XSeekBar.OnChangeListener() {
        @Override
        public void change(XSeekBar xSeekBar, int progress) {
            String seekb = (String) xSeekBar.getTag();
            if (seekb.equals("faceShapeValueBar")) {
                seekBarFilterListener.FaceShapeSeek(progress);

            } else if (seekb.equals("bigeyeValueBar")) {
                seekBarFilterListener.BigEyeSeek(progress);

            } else if (seekb.equals("skinSmoothValueBar")) {
                seekBarFilterListener.skinSmoothValueBar(progress);
            } else if (seekb.equals("skinWhitenValueBar")) {
                seekBarFilterListener.skinWhitenValueBar(progress);

            } else if (seekb.equals("redFaceValueBar")) {
                seekBarFilterListener.redFaceValueBar(progress);
            }
        }
    };
 /*   private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener1 = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            String seekb = (String) seekBar.getTag();
            if (seekb.equals("faceShapeValueBar")) {
                seekBarFilterListener.FaceShapeSeek(progress);

            } else if (seekb.equals("bigeyeValueBar")) {
                seekBarFilterListener.BigEyeSeek(progress);

            } else if (seekb.equals("skinSmoothValueBar")) {
                seekBarFilterListener.skinSmoothValueBar(progress);
            } else if (seekb.equals("skinWhitenValueBar")) {
                seekBarFilterListener.skinWhitenValueBar(progress);

            } else if (seekb.equals("redFaceValueBar")) {
                seekBarFilterListener.redFaceValueBar(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };*/

    public PictrueFilterPreviewDialog(Context context, OnUpdateFilterListener listener, SeekBarFilterListener seekBar) {
        super(context, R.style.TrillDialog);
        this.mContext = context;
        initDatas();
        mListener = listener;
        seekBarFilterListener = seekBar;
    }

    private void initDatas() {
        mdatas = new ArrayList<>();
        mdatas.add(new FilterInfo(types[0], "原图", R.drawable.filter_thumb_original));
        mdatas.add(new FilterInfo(types[1], "健康", R.drawable.filter_thumb_healthy));
        mdatas.add(new FilterInfo(types[2], "怀旧", R.drawable.filter_thumb_nostalgia));
        mdatas.add(new FilterInfo(types[3], "冰冷", R.drawable.filter_thumb_cool));
        mdatas.add(new FilterInfo(types[4], "祖母绿", R.drawable.filter_thumb_emerald));
        mdatas.add(new FilterInfo(types[5], "常青", R.drawable.filter_thumb_evergreen));
        mdatas.add(new FilterInfo(types[6], "蜡笔", R.drawable.filter_thumb_original));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_filter_list_p);
        setCanceledOnTouchOutside(true);
        initView();
        initListview();
    }

    private void initListview() {
        TextView tv_filter = findViewById(R.id.tv_filter);
        TextView tv_beauty = findViewById(R.id.tv_beauty);
        View layout_facesurgery = findViewById(R.id.layout_facesurgery);
        LinearLayout ll_seekbar_beauty = findViewById(R.id.ll_seekbar_beauty);
        RelativeLayout rl_picture = findViewById(R.id.rl_picture);

        mFaceSurgeryFaceShapeSeek = (XSeekBar) findViewById(R.id.faceShapeValueBar);
        mFaceSurgeryFaceShapeSeek.changeSelect(20);
        mFaceSurgeryFaceShapeSeek.setTag("faceShapeValueBar");


        mFaceSurgeryBigEyeSeek = (XSeekBar) findViewById(R.id.bigeyeValueBar);
        mFaceSurgeryBigEyeSeek.changeSelect(50);
        mFaceSurgeryBigEyeSeek.setTag("bigeyeValueBar");

        mSkinSmoothSeek = (XSeekBar) findViewById(R.id.skinSmoothValueBar);
        mSkinSmoothSeek.changeSelect(100);
        mSkinSmoothSeek.setTag("skinSmoothValueBar");

        mSkinWihtenSeek = (XSeekBar) findViewById(R.id.skinWhitenValueBar);
        mSkinWihtenSeek.changeSelect(20);
        mSkinWihtenSeek.setTag("skinWhitenValueBar");

        mRedFaceSeek = (XSeekBar) findViewById(R.id.redFaceValueBar);
        mRedFaceSeek.changeSelect(80);
        mRedFaceSeek.setTag("redFaceValueBar");

        mFaceSurgeryFaceShapeSeek.addOnChangeListener(onSeekBarChangeListener);
        mFaceSurgeryBigEyeSeek.addOnChangeListener(onSeekBarChangeListener);
        mSkinSmoothSeek.addOnChangeListener(onSeekBarChangeListener);
        mSkinWihtenSeek.addOnChangeListener(onSeekBarChangeListener);
        mRedFaceSeek.addOnChangeListener(onSeekBarChangeListener);

        mListView = findViewById(R.id.rv_filter);
        mListView.setLayoutManager(new LinearLayoutManager(mContext, HORIZONTAL, false));
        mAdapter = new CommAvatarAdapter();
        mListView.setAdapter(mAdapter);
//        LinearLayout ll_filter_adapter = findViewById(R.id.ll_filter_adapter);
        tv_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup.LayoutParams params = rl_picture.getLayoutParams();
                params.height = 540;
                rl_picture.setLayoutParams(params);
                layout_facesurgery.setVisibility(View.GONE);
                ll_seekbar_beauty.setVisibility(View.GONE);
                mListView.setVisibility(View.VISIBLE);
//                ll_filter_adapter.setVisibility(View.VISIBLE);
            }
        });
        tv_beauty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup.LayoutParams params = rl_picture.getLayoutParams();
                params.height = 960;
                rl_picture.setLayoutParams(params);
                mListView.setVisibility(View.GONE);
                ll_seekbar_beauty.setVisibility(View.VISIBLE);
                layout_facesurgery.setVisibility(View.VISIBLE);
            }
        });

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
        void select(GPUCamImgOperator.GPUImgFilterType type);

        void dismiss();
    }

    public interface SeekBarFilterListener {
        void FaceShapeSeek(int i);

        void BigEyeSeek(int i);

        void skinSmoothValueBar(int i);

        void skinWhitenValueBar(int i);

        void redFaceValueBar(int i);
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
                    GPUCamImgOperator.GPUImgFilterType type = mdatas.get(getAdapterPosition()).getType();
                    mListener.select(type);
                    mAdapter.notifyItemChanged(currt);
                    currt = getAdapterPosition();
                    mAdapter.notifyItemChanged(currt);
                }
            });
        }
    }
}
