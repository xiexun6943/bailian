package com.ydd.zhichat.ui.other;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.bean.message.XmppMessage;
import com.ydd.zhichat.db.dao.ChatMessageDao;
import com.ydd.zhichat.helper.FileDataHelper;
import com.ydd.zhichat.helper.UploadEngine;
import com.ydd.zhichat.ui.message.InstantMessageActivity;
import com.ydd.zhichat.util.TimeUtils;
import com.example.qrcode.utils.CommonUtils;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.FileUtil;
import com.ydd.zhichat.view.MessageAvatar;

import java.io.File;
import java.util.UUID;

/**
 * Created by Administrator on 2017/9/14 0014.
 * 二维码类
 */
public class QRcodeActivity extends BaseActivity {
    private RelativeLayout rel_content;
    private RelativeLayout rel_qrc;
    private ImageView qrcode;
    private ImageView mPAva;
    private ImageView avatar_img_top;
    private TextView tv_save;
    private TextView tv_share;
    private TextView tv_name;
    private MessageAvatar mGAva;
    private MessageAvatar avatar_imgS_top;
    private boolean isgroup;
    private String userId;
    private String userAvatar;
    private String userName;
    private String roomJid;
    private String roomName;
    private String action;
    private String str;

    private Bitmap bitmap;
    private ChatMessage message;
    private String mLoginUserId;
    private String mLoginNickName;
    private String mNewUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_im_code_image);
        if (getIntent() != null) {
            isgroup = getIntent().getBooleanExtra("isgroup", false);
            userId = getIntent().getStringExtra("userid");
            userAvatar = getIntent().getStringExtra("userAvatar");
            userName = getIntent().getStringExtra("userName");
            if (isgroup) {
                roomJid = getIntent().getStringExtra("roomJid");
                roomName = getIntent().getStringExtra("roomName");
            }
        }
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JXQR_QRImage"));
        ImageView mIvTitleRight = (ImageView) findViewById(R.id.iv_title_right);
        mIvTitleRight.setImageResource(R.drawable.save_local);
        mIvTitleRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileUtil.saveImageToGallery2(mContext, getBitmap(QRcodeActivity.this.getWindow().getDecorView()));
            }
        });
        mIvTitleRight.setVisibility(View.GONE);
    }

    private void initView() {
        rel_content =  findViewById(R.id.rel_content);
        rel_qrc =  findViewById(R.id.rel_qrc);
        qrcode = (ImageView) findViewById(R.id.qrcode);
        mPAva = (ImageView) findViewById(R.id.avatar_img);
        avatar_img_top = (ImageView) findViewById(R.id.avatar_img_top);
        mGAva = (MessageAvatar) findViewById(R.id.avatar_imgS);
        avatar_imgS_top = (MessageAvatar) findViewById(R.id.avatar_imgS_top);
        tv_save = findViewById(R.id.tv_save);
        tv_name = findViewById(R.id.tv_name);
        tv_share = findViewById(R.id.tv_share);
        if (isgroup) {
            action = "group";
            mGAva.setVisibility(View.VISIBLE);
            avatar_imgS_top.setVisibility(View.VISIBLE);
        } else {
            action = "user";
            mPAva.setVisibility(View.VISIBLE);
            avatar_img_top.setVisibility(View.VISIBLE);
        }
        str = coreManager.getConfig().website + "?action=" + action + "&shikuId=" + userId;
        Log.e("zq", "二维码链接：" + str);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        // 生成二维码
        bitmap = CommonUtils.createQRCode(str, screenWidth - 200, screenWidth - 200);

        // 显示 二维码 与 头像
        if (isgroup) {// 群组头像
            Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), roomJid);
            if (friend != null) {
                mGAva.fillData(friend);
                avatar_imgS_top.fillData(friend);
                tv_name.setText(friend.getNickName());
            }
        } else {// 用户头像
           /* Glide.with(mContext)
                    .load(AvatarHelper.getInstance().getAvatarUrl(userId, false))
                    .asBitmap()
                    .signature(new StringSignature(UserAvatarDao.getInstance().getUpdateTime(userId)))
                    .dontAnimate()
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            bitmap = EncodingUtils.createQRCode(str, screenWidth - 200, screenWidth - 200,
                                    resource);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);
                            bitmap = EncodingUtils.createQRCode(str, screenWidth - 200, screenWidth - 200,
                                    BitmapFactory.decodeResource(getResources(), R.drawable.avatar_normal));// 默认头像
                        }
                    });*/
            AvatarHelper.getInstance().displayAvatar(userAvatar, mPAva);
            AvatarHelper.getInstance().displayAvatar(userAvatar, avatar_img_top);
            tv_name.setText(userName);
        }
        qrcode.setImageBitmap(bitmap);

        tv_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileUtil.saveImageToGallery2(mContext, getBitmap(QRcodeActivity.this.getWindow().getDecorView()));
                Toast.makeText(QRcodeActivity.this, R.string.tip_saved_qr_code, Toast.LENGTH_SHORT).show();
            }
        });
        tv_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                File file = FileUtil.saveImageToGallery2(mContext, getBitmap(QRcodeActivity.this.getWindow().getDecorView()));
                File file = FileUtil.saveImageToGallery2(mContext, getBitmap(rel_qrc));
                initChatByContent(file);
            }
        });
    }

    private void initChatByContent(File file) {
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();
        // Todo 将封装好的消息存入10010 号的msg 表内，在跳转至转发->聊天界面(跳转传值均为10010号与msgId)，之后在聊天界面内通过这两个值查询到对用消息，发送
        mNewUserId = "10010";
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent("");
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        int[] imageParam = FileDataHelper.getImageParamByIntsFile(filePath);
        message.setLocation_x(String.valueOf(imageParam[0]));
        message.setLocation_y(String.valueOf(imageParam[1]));
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        UploadEngine.uploadImFile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), mNewUserId, message, mUploadResponse);

    }

    private UploadEngine.ImFileUploadResponse mUploadResponse = new UploadEngine.ImFileUploadResponse() {

        @Override
        public void onSuccess(String toUserId, ChatMessage message) {
            message.setUpload(true);
            message.setUploadSchedule(100);
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mNewUserId, message)) {
                Intent intent = new Intent(QRcodeActivity.this, InstantMessageActivity.class);
                intent.putExtra("fromUserId", mNewUserId);
                intent.putExtra("messageId", message.getPacketId());
                intent.putExtra("isShare", true);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(mContext, "消息封装失败", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(String toUserId, ChatMessage message) {
            Toast.makeText(mContext, getString(R.string.upload_failed), Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 获取这个view的缓存bitmap,
     */
    private Bitmap getBitmap(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap result = Bitmap.createBitmap(view.getDrawingCache());
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(false);
        return result;
    }
}