package com.ydd.zhichat.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.util.FileUtil;
import com.ydd.zhichat.view.ZoomImageView;

import java.io.File;
import java.util.List;

public class ChatOverviewAdapter extends PagerAdapter {
    private Context mContext;
    private List<ChatMessage> mChatMessages;
    private SparseArray<View> mViews = new SparseArray<>();

    public ChatOverviewAdapter(Context context, List<ChatMessage> chatMessages) {
        mContext = context;
        mChatMessages = chatMessages;
    }

    public void refreshItem(String url, int index) {
        AvatarHelper.getInstance().displayUrl(url, (ZoomImageView) mViews.get(index));
    }

    @Override
    public int getCount() {
        return mChatMessages.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mViews.get(position);
        if (view == null) {
            view = new ZoomImageView(mContext);
            mViews.put(position, view);
        }

        ChatMessage chatMessage = mChatMessages.get(position);
        if (!TextUtils.isEmpty(chatMessage.getFilePath()) && FileUtil.isExist(chatMessage.getFilePath())) {
            Glide.with(mContext).load(new File(chatMessage.getFilePath())).into((ZoomImageView) view);
        } else {
            Glide.with(mContext).load(chatMessage.getContent()).into((ZoomImageView) view);
        }
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = mViews.get(position);
        if (view == null) {
            super.destroyItem(container, position, object);
        } else {
            container.removeView(view);
        }

    }
}
