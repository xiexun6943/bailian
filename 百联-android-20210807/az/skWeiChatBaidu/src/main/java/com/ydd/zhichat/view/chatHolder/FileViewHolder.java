package com.ydd.zhichat.view.chatHolder;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.helper.UploadEngine;
import com.ydd.zhichat.ui.mucfile.DownManager;
import com.ydd.zhichat.ui.mucfile.MucFileDetails;
import com.ydd.zhichat.ui.mucfile.XfileUtils;
import com.ydd.zhichat.ui.mucfile.bean.MucFileBean;
import com.ydd.zhichat.util.FileUtil;
import com.ydd.zhichat.view.FileProgressPar;
import com.ydd.zhichat.view.SelectionFrame;

class FileViewHolder extends AChatHolderInterface {

    ImageView ivCardImage;
    TextView tvPersonName;
    FileProgressPar progressPar;
    ImageView ivUploadCancel;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_card : R.layout.chat_to_item_card;
    }

    @Override
    public void initView(View view) {
        ivCardImage = view.findViewById(R.id.iv_card_head);
        tvPersonName = view.findViewById(R.id.person_name);
        progressPar = view.findViewById(R.id.chat_card_light);
        ivUploadCancel = view.findViewById(R.id.chat_upload_cancel_iv);
        TextView tvType = view.findViewById(R.id.person_title);
        tvType.setText(getString(R.string.chat_file));
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        String filePath = FileUtil.isExist(message.getFilePath()) ? message.getFilePath() : message.getContent();
        // 设置图标
        if (message.getTimeLen() > 0) { // 有文件类型
            if (message.getTimeLen() == 1) {
                Glide.with(mContext).load(filePath).override(100, 100).into(ivCardImage);
            } else {
                XfileUtils.setFileInco(message.getTimeLen(), ivCardImage);
            }
        } else {// 没有文件类型，取出后缀
            int pointIndex = filePath.lastIndexOf(".");
            if (pointIndex != -1) {
                String type = filePath.substring(pointIndex + 1).toLowerCase();
                if (type.equals("png") || type.equals("jpg") || type.equals("gif")) {
                    Glide.with(mContext).load(filePath).override(100, 100).into(ivCardImage);
                    message.setTimeLen(1);
                } else {
                    fillFileIcon(type, ivCardImage, message);
                }
            }
        }

        // 设置文件名称
        String fileName = TextUtils.isEmpty(message.getFilePath()) ? message.getContent() : message.getFilePath();
        int start = fileName.lastIndexOf("/");
        String name = fileName.substring(start + 1).toLowerCase();
        tvPersonName.setText(name);
        message.setObjectId(name);


        // 设置进度条显示 不是我发的，或者进度到了100，或者上传了，都不显示 ——zq
        boolean hide = !isMysend || message.getUploadSchedule() == 100 || message.isUpload();
        progressPar.visibleMode(!hide);
        if (isMysend) {
            // 没有上传或者 进度小于100
            boolean show = !message.isUpload() && message.getUploadSchedule() < 100;
            if (show) {
                if (ivUploadCancel != null) {
                    ivUploadCancel.setVisibility(View.VISIBLE);
                }
            } else {
                if (ivUploadCancel != null) {
                    ivUploadCancel.setVisibility(View.GONE);
                }
            }
        }

        progressPar.update(message.getUploadSchedule());
        mSendingBar.setVisibility(View.GONE);

        if (ivUploadCancel != null) {
            ivUploadCancel.setOnClickListener(v -> {
                SelectionFrame selectionFrame = new SelectionFrame(mContext);
                selectionFrame.setSomething(getString(R.string.cancel_upload), getString(R.string.sure_cancel_upload), new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        // 用户可能在弹窗弹起后停留很久，所以点击确认的时候还需要判断一下
                        if (!mdata.isUpload()) {
                            UploadEngine.cancel(mdata.getPacketId());
                        }
                    }
                });
                selectionFrame.show();
            });
        }
    }

    @Override
    protected void onRootClick(View v) {
        sendReadMessage(mdata);
        ivUnRead.setVisibility(View.GONE);

        MucFileBean data = new MucFileBean();
        String url = mdata.getContent();
        String filePath = mdata.getFilePath();
        if (TextUtils.isEmpty(filePath)) {
            filePath = url;
        }

        int size = mdata.getFileSize();
        // 取出文件名称
        int start = filePath.lastIndexOf("/");
        String name = filePath.substring(start + 1).toLowerCase();
        data.setNickname(name);
        data.setUrl(url);
        data.setName(name);
        data.setSize(size);
        data.setState(DownManager.STATE_UNDOWNLOAD);
        data.setType(mdata.getTimeLen());
        Intent intent = new Intent(mContext, MucFileDetails.class);
        intent.putExtra("data", data);
        mContext.startActivity(intent);
    }

    private void fillFileIcon(String type, ImageView v, ChatMessage chat) {
        if (type.equals("mp3")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_y);
            chat.setTimeLen(2);
        } else if (type.equals("mp4") || type.equals("avi")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_v);
            chat.setTimeLen(3);
        } else if (type.equals("xls")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_x);
            chat.setTimeLen(5);
        } else if (type.equals("doc")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_w);
            chat.setTimeLen(6);
        } else if (type.equals("ppt")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_p);
            chat.setTimeLen(4);
        } else if (type.equals("pdf")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_f);
            chat.setTimeLen(10);
        } else if (type.equals("apk")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_a);
            chat.setTimeLen(11);
        } else if (type.equals("txt")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_t);
            chat.setTimeLen(8);
        } else if (type.equals("rar") || type.equals("zip")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_z);
            chat.setTimeLen(7);
        } else {
            v.setImageResource(R.drawable.ic_muc_flie_type_what);
            chat.setTimeLen(9);
        }
    }

    @Override
    public boolean enableUnRead() {
        return true;
    }
}
