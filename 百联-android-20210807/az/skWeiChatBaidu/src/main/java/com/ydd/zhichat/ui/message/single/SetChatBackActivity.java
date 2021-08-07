package com.ydd.zhichat.ui.message.single;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.UploadFileResult;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.helper.UploadService;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.photopicker.PhotoPickerActivity;
import com.ydd.zhichat.view.photopicker.SelectModel;
import com.ydd.zhichat.view.photopicker.intent.PhotoPickerIntent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by Administrator on 2017/12/5 0005.
 * 聊天背景
 */

public class SetChatBackActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_PICK_PHOTO = 1;
    private ImageView mChaIv;
    private String mFriendId;
    private String mLoginUserId;
    private String mChatBackgroundPath;
    private String mChatBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_chang_chatbg);
        mFriendId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        initView();
        goSelect();
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
        tvTitle.setText(getString(R.string.set_chat_bg));
        TextView tvRight = (TextView) findViewById(R.id.tv_title_right);
        tvRight.setText(getString(R.string.cover_chat_bg));
        tvRight.setOnClickListener(this);
    }

    private void initView() {
        mChaIv = (ImageView) findViewById(R.id.chat_bg);
        TextView iv1 = (TextView) findViewById(R.id.sure);
        TextView iv2 = (TextView) findViewById(R.id.over);
        iv1.setOnClickListener(this);
        iv2.setOnClickListener(this);

        String mChatBgPath = PreferenceUtils.getString(this, Constants.SET_CHAT_BACKGROUND_PATH
                + mFriendId + mLoginUserId, "reset");

        String mChatBg = PreferenceUtils.getString(this, Constants.SET_CHAT_BACKGROUND
                + mFriendId + mLoginUserId, "reset");

        if (mChatBgPath.equals("reset") || mChatBg.equals("reset")) {// 该用户之前未设置聊天背景
            return;
        }

        File file = new File(mChatBgPath);
        if (file.exists()) { // 加载本地
            if (mChatBgPath.toLowerCase().endsWith("gif")) {
                try {
                    GifDrawable gifDrawable = new GifDrawable(file);
                    mChaIv.setImageDrawable(gifDrawable);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Glide.with(SetChatBackActivity.this)
                        .load(file)
                        .error(R.drawable.fez)
                        .into(mChaIv);
            }
        } else { // 加载网络
            Glide.with(this)
                    .load(mChatBg)
                    .error(R.drawable.fez)
                    .into(mChaIv);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_title_right:// 还原
                over(0);
                break;
            case R.id.sure:// 确定 将背景路径与URL存入共享参数内
                over(1);
                break;
            case R.id.over:// 重选
                goSelect();
                break;
        }
    }

    private void over(int type) {
        if (type == 1) {
            PreferenceUtils.putString(SetChatBackActivity.this, Constants.SET_CHAT_BACKGROUND_PATH
                    + mFriendId + mLoginUserId, mChatBackgroundPath);

            PreferenceUtils.putString(SetChatBackActivity.this, Constants.SET_CHAT_BACKGROUND
                    + mFriendId + mLoginUserId, mChatBackground);
        } else {// 还原
            PreferenceUtils.putString(SetChatBackActivity.this, Constants.SET_CHAT_BACKGROUND_PATH
                    + mFriendId + mLoginUserId, "reset");

            PreferenceUtils.putString(SetChatBackActivity.this, Constants.SET_CHAT_BACKGROUND
                    + mFriendId + mLoginUserId, "reset");
        }
        Intent intent = new Intent();
        intent.putExtra("Operation_Code", 1);
        intent.setAction(com.ydd.zhichat.broadcast.OtherBroadcast.QC_FINISH);
        sendBroadcast(intent); // 还原 || 更换聊天背景成功，发送广播更新单聊界面
        finish();
    }

    private void goSelect() {
        ArrayList<String> imagePaths = new ArrayList<>();
        PhotoPickerIntent intent = new PhotoPickerIntent(SetChatBackActivity.this);
        intent.setSelectModel(SelectModel.MULTI);
        intent.setShowCarema(false);
        intent.setMaxTotal(1);
        intent.setSelectedPaths(imagePaths);
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_PHOTO && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                DialogHelper.showDefaulteMessageProgressDialog(SetChatBackActivity.this);
                UploadUrl uploadUrl = new UploadUrl();
                ArrayList<String> stringArrayListExtra = data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT);
                uploadUrl.execute(stringArrayListExtra.get(0));
                mChatBackgroundPath = stringArrayListExtra.get(0);
            } else {
                ToastUtil.showToast(this, R.string.c_photo_album_failed);
            }
        }
    }

    class UploadUrl extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            Map<String, String> params = new HashMap<>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("userId", coreManager.getSelf().getUserId());
            params.put("validTime", "-1");// 文件有效期

            List<String> filePathList = new ArrayList<>();
            filePathList.add(strings[0]);
            return new UploadService().uploadFile(coreManager.getConfig().UPLOAD_URL, params, filePathList);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            DialogHelper.dismissProgressDialog();
            UploadFileResult recordResult = JSON.parseObject(s, UploadFileResult.class);
            List<UploadFileResult.Sources> images = recordResult.getData().getImages();
            mChatBackground = images.get(0).getOriginalUrl();

            // 预览
            File file = new File(mChatBackgroundPath);
            if (file.exists()) { // 加载本地
                if (mChatBackgroundPath.toLowerCase().endsWith("gif")) {
                    try {
                        GifDrawable gifDrawable = new GifDrawable(file);
                        mChaIv.setImageDrawable(gifDrawable);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Glide.with(SetChatBackActivity.this)
                            .load(file)
                            .error(R.drawable.fez)
                            .into(mChaIv);
                }
            } else { // 加载网络
                Glide.with(SetChatBackActivity.this)
                        .load(mChatBackground)
                        .error(R.drawable.fez)
                        .into(mChaIv);
            }
        }
    }
}
