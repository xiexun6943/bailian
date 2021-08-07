package com.ydd.zhichat.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.ui.tool.WebViewActivity;
import com.ydd.zhichat.util.ScreenUtil;

import java.util.Arrays;
import java.util.List;

import okhttp3.HttpUrl;

public class WebMoreDialog extends Dialog {
    private Context mContent;
    private String mUrl;

    private TextView mBrowserProvideTv;
    private RecyclerView mBrowserRecycleView;
    private BrowserActionAdapter mBrowserActionAdapter;
    private List<Item> mData;

    private BrowserActionClickListener mBrowserActionClickListener;

    public WebMoreDialog(Context context, String url, BrowserActionClickListener browserActionClickListener) {
        super(context, R.style.BottomDialog);
        this.mContent = context;
        this.mUrl = url;
        this.mBrowserActionClickListener = browserActionClickListener;
        mData = getData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_browser);

        mBrowserProvideTv = findViewById(R.id.browser_provide_tv);
        HttpUrl httpUrl = HttpUrl.parse(mUrl);
        if (httpUrl != null) {
            mBrowserProvideTv.setText("网页由 " + httpUrl.host() + " 提供");
        } else {
            mBrowserProvideTv.setText("网页由 " + mUrl + " 提供");
        }
        mBrowserRecycleView = findViewById(R.id.browser_ry);
        mBrowserRecycleView.setLayoutManager(new GridLayoutManager(mContent, 4));
        mBrowserActionAdapter = new BrowserActionAdapter();
        mBrowserRecycleView.setAdapter(mBrowserActionAdapter);

        setCanceledOnTouchOutside(true);

        Window o = getWindow();
        WindowManager.LayoutParams lp = o.getAttributes();
        lp.width = ScreenUtil.getScreenWidth(getContext());
        lp.height = ScreenUtil.getScreenHeight(getContext()) / 2;
        o.setAttributes(lp);
        this.getWindow().setGravity(Gravity.BOTTOM);
        this.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
    }

    private List<Item> getData() {
        return Arrays.asList(
                new Item(!WebViewActivity.IS_FLOATING ? R.drawable.ic_wb_xf : R.drawable.ic_wb_xf_cancel,
                        !WebViewActivity.IS_FLOATING ? R.string.floating_window : R.string.floating_window_cancel, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.floatingWindow();
                    }
                }),
                new Item(R.drawable.ic_wb_fx, R.string.send_to_friend, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.sendToFriend();
                    }
                }),
                new Item(R.drawable.ic_wb_fx_moment, R.string.share_to_life_circle, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.shareToLifeCircle();
                    }
                }),
                new Item(R.drawable.ic_wb_sc, R.string.collection, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.collection();
                    }
                }),
                new Item(R.drawable.ic_wb_ss, R.string.search_paper_content, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.searchContent();
                    }
                }),
                new Item(R.drawable.ic_wb_copy, R.string.copy_link, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.copyLink();
                    }
                }),
                new Item(R.drawable.ic_wb_ldk, R.string.open_outside, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.openOutSide();
                    }
                }),
                new Item(R.drawable.ic_wb_font_size, R.string.modify_font_size, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.modifyFontSize();
                    }
                }),
                new Item(R.drawable.ic_wb_refresh, R.string.refresh, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.refresh();
                    }
                }),
                new Item(R.drawable.ic_wb_ts, R.string.complaint, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.complaint();
                    }
                }),
                new Item(R.drawable.share_wx, R.string.share_wechat, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.shareWechat();
                    }
                }),
                new Item(R.drawable.share_circle, R.string.share_moments, () -> {
                    if (mBrowserActionClickListener != null) {
                        dismiss();
                        mBrowserActionClickListener.shareWechatMoments();
                    }
                })
        );
    }

    public interface BrowserActionClickListener {
        void floatingWindow();

        void sendToFriend();

        void shareToLifeCircle();

        void collection();

        void searchContent();

        void copyLink();

        void openOutSide();

        void modifyFontSize();

        void refresh();

        void complaint();

        void shareWechat();

        void shareWechatMoments();
    }

    class BrowserActionAdapter extends RecyclerView.Adapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.item_browser, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final Item item = mData.get(position);
            holder.ivActionImage.setImageResource(item.icon);
            holder.ivActionImage.setOnClickListener(v -> item.runnable.run());
            holder.tvActionName.setText(item.text);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvActionName;
        private final ImageView ivActionImage;

        public ViewHolder(View itemView) {
            super(itemView);
            tvActionName = itemView.findViewById(R.id.tvActionName);
            ivActionImage = itemView.findViewById(R.id.ivActionImage);
        }
    }

    class Item {
        @StringRes
        int text;
        @DrawableRes
        int icon;
        Runnable runnable;

        public Item(int icon, int text, Runnable runnable) {
            this.icon = icon;
            this.text = text;
            this.runnable = runnable;
        }
    }
}
