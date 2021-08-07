package com.ydd.zhichat.ui.tool;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.signature.StringSignature;
import com.example.qrcode.utils.DecodeUtils;
import com.google.zxing.Result;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.db.dao.UserAvatarDao;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.message.HandleQRCodeScanUtil;
import com.ydd.zhichat.util.FileUtil;
import com.ydd.zhichat.view.SaveWindow;
import com.ydd.zhichat.view.ZoomImageView;
import com.ydd.zhichat.view.chatHolder.MessageEventClickFire;

import java.io.File;

import de.greenrobot.event.EventBus;
import me.kareluo.imaging.IMGEditActivity;
import pl.droidsonroids.gif.GifDrawable;

/**
 * 单张图片预览
 */
public class SingleImagePreviewActivity extends BaseActivity {
    public static final int REQUEST_IMAGE_EDIT = 1;

    private String mImageUri;
    private String mImagePath;
    private String mEditedPath;
    // 是否为阅后即焚类型   当前图片在消息内的位置
    private String delPackedId;

    private ZoomImageView mImageView;
    private Bitmap mBitmap;// 用于 识别图中二维码
    private SaveWindow mSaveWindow;
    private My_BroadcastReceiver my_broadcastReceiver = new My_BroadcastReceiver();

    @SuppressWarnings("unused")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_image_preview);

        if (getIntent() != null) {
            mImageUri = getIntent().getStringExtra(AppConstant.EXTRA_IMAGE_URI);
            mImagePath = getIntent().getStringExtra("image_path");
            boolean isReadDel = getIntent().getBooleanExtra("isReadDel", false);
            if (isReadDel) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            }

            // 这个是阅后即焚消息的 packedId
            delPackedId = getIntent().getStringExtra("DEL_PACKEDID");
        }

        initView();
        register();
    }

    public void doBack() {
        if (!TextUtils.isEmpty(delPackedId)) {
            // 发送广播去更新聊天界面，移除该message
            EventBus.getDefault().post(new MessageEventClickFire("delete", delPackedId));
        }
        finish();
        overridePendingTransition(0, 0);// 关闭过场动画
    }

    @Override
    public void onBackPressed() {
        doBack();
    }

    @Override
    protected boolean onHomeAsUp() {
        doBack();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (my_broadcastReceiver != null) {
            unregisterReceiver(my_broadcastReceiver);
        }
    }

    private void initView() {
        getSupportActionBar().hide();
        mImageView = findViewById(R.id.image_view);
        if (TextUtils.isEmpty(mImageUri)) {
            Toast.makeText(mContext, R.string.image_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        /*
        加载图片 || 头像
         */
        if (mImageUri.contains("http")) {// 图片 头像的mImageUri为UserId
            boolean isExists = false;
            if (!TextUtils.isEmpty(mImagePath)) {
                File file = new File(mImagePath);
                if (file.exists()) {
                    isExists = true;
                }
            }
            if (isExists) {// 本地加载
                if (mImageUri.endsWith(".gif")) {
                    try {
                        GifDrawable gifDrawable = new GifDrawable(new File(mImagePath));
                        mImageView.setImageDrawable(gifDrawable);
                    } catch (Exception e) {
                        mImageView.setImageResource(R.drawable.image_download_fail_icon);
                        e.printStackTrace();
                    }
                } else {
                    Glide.with(mContext)
                            .load(mImagePath)
                            .asBitmap()
                            .centerCrop()
                            .dontAnimate()
                            .error(R.drawable.image_download_fail_icon)
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    mBitmap = resource;
                                    mImageView.setImageBitmap(resource);
                                }

                                @Override
                                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                    mImageView.setImageResource(R.drawable.image_download_fail_icon);
                                }
                            });

                }
            } else {// 网络加载
                Glide.with(mContext)
                        .load(mImageUri)
                        .asBitmap()
                        .centerCrop()
                        .dontAnimate()
                        .error(R.drawable.image_download_fail_icon)
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                mBitmap = resource;
                                mImageView.setImageBitmap(resource);
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                mImageView.setImageResource(R.drawable.image_download_fail_icon);
                            }
                        });
            }
        } else {// 头像
            String time = UserAvatarDao.getInstance().getUpdateTime(mImageUri);
            mImageUri = AvatarHelper.getAvatarUrl(mImageUri, false);// 为头像重新赋值，用于保存功能
            if (TextUtils.isEmpty(mImageUri)) {
                mImageView.setImageResource(R.drawable.avatar_normal);
                return;
            }
            Glide.with(MyApplication.getContext())
                    .load(mImageUri)
                    .signature(new StringSignature(time))
                    .error(R.drawable.avatar_normal)
                    .into(mImageView);
            // AvatarHelper.getInstance().displayAvatar(mImageUri, mImageView, false);// 该方式加载头像不知道为什么头像不居中显示
        }
    }

    private void register() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(com.ydd.zhichat.broadcast.OtherBroadcast.singledown);
        filter.addAction(com.ydd.zhichat.broadcast.OtherBroadcast.longpress);
        registerReceiver(my_broadcastReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_EDIT:
                    mImagePath = mEditedPath;
                    mImageUri = new File(mEditedPath).toURI().toString();
                    Glide.with(mContext)
                            .load(mImagePath)
                            .asBitmap()
                            .centerCrop()
                            .dontAnimate()
                            .error(R.drawable.image_download_fail_icon)
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    mBitmap = resource;
                                    mImageView.setImageBitmap(resource);
                                }

                                @Override
                                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                    mImageView.setImageResource(R.drawable.image_download_fail_icon);
                                }
                            });
                    // 模拟那个长按，弹出菜单，
                    Intent intent = new Intent(com.ydd.zhichat.broadcast.OtherBroadcast.longpress);
                    sendBroadcast(intent);
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    class My_BroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(com.ydd.zhichat.broadcast.OtherBroadcast.singledown)) {
                // 轻触屏幕，退出预览
                doBack();
            } else if (intent.getAction().equals(com.ydd.zhichat.broadcast.OtherBroadcast.longpress)) {
                // 长按屏幕，弹出菜单
                mSaveWindow = new SaveWindow(SingleImagePreviewActivity.this, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSaveWindow.dismiss();
                        switch (v.getId()) {
                            case R.id.save_image:
                                if (!TextUtils.isEmpty(delPackedId)) {
                                    Toast.makeText(SingleImagePreviewActivity.this, R.string.tip_burn_image_cannot_save, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (mImageUri.toLowerCase().endsWith("gif")) {// 保存Gif
                                    FileUtil.downImageToGallery(SingleImagePreviewActivity.this, mImagePath);
                                } else {// 保存图片
                                    FileUtil.downImageToGallery(SingleImagePreviewActivity.this, mImageUri);
                                }
                                break;
                            case R.id.edit_image:
                                Glide.with(SingleImagePreviewActivity.this)
                                        .load(mImageUri)
                                        .downloadOnly(new SimpleTarget<File>() {
                                            @Override
                                            public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
                                                mEditedPath = FileUtil.createImageFileForEdit().getAbsolutePath();
                                                IMGEditActivity.startForResult(SingleImagePreviewActivity.this, Uri.fromFile(resource), mEditedPath, REQUEST_IMAGE_EDIT);
                                            }
                                        });
                                break;
                            case R.id.identification_qr_code:
                                // 识别图中二维码
                                if (mBitmap != null) {
                                    new Thread(() -> {
                                        final Result result = DecodeUtils.decodeFromPicture(mBitmap);
                                        mImageView.post(() -> {
                                            if (result != null && !TextUtils.isEmpty(result.getText())) {
                                                HandleQRCodeScanUtil.handleScanResult(mContext, result.getText());
                                            } else {
                                                Toast.makeText(SingleImagePreviewActivity.this, R.string.decode_failed, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }).start();
                                } else {
                                    Toast.makeText(SingleImagePreviewActivity.this, R.string.decode_failed, Toast.LENGTH_SHORT).show();
                                }
                                break;
                        }
                    }
                });
                mSaveWindow.show();
            }
        }
    }
}
