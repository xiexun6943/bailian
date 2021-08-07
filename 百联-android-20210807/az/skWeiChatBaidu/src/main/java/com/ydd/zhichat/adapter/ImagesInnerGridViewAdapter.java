package com.ydd.zhichat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.bumptech.glide.Glide;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.circle.PublicMessage.Resource;
import com.ydd.zhichat.view.SquareCenterImageView;

import java.util.List;

public class ImagesInnerGridViewAdapter extends BaseAdapter {

    private Context mContext;
    private List<Resource> mDatas;

    ImagesInnerGridViewAdapter(Context context, List<Resource> data) {
        mContext = context;
        mDatas = data;
    }

    @Override
    public int getCount() {
        if (mDatas.size() >= 9) {
            return 9;
        }
        return mDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.row_mu_normal, parent, false);
            holder.imageView = (SquareCenterImageView) convertView.findViewById(R.id.muc_normal);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Glide.with(mContext)
                .load(mDatas.get(position).getOriginalUrl())
                .override(150, 150)
                .placeholder(R.drawable.default_gray)
                .error(R.drawable.image_download_fail_icon)
                .into(holder.imageView);

        return convertView;
    }

    static class ViewHolder {
        SquareCenterImageView imageView;
    }
}