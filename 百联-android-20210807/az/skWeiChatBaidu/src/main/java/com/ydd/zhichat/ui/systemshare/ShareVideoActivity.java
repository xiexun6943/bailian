package com.ydd.zhichat.ui.systemshare;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.bean.Area;
import com.ydd.zhichat.bean.UploadFileResult;
import com.ydd.zhichat.bean.VideoFile;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.helper.LoginHelper;
import com.ydd.zhichat.helper.UploadService;
import com.ydd.zhichat.ui.MainActivity;
import com.ydd.zhichat.ui.SplashActivity;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.circle.range.AtSeeCircleActivity;
import com.ydd.zhichat.ui.circle.range.SeeCircleActivity;
import com.ydd.zhichat.ui.map.MapPickerActivity;
import com.ydd.zhichat.ui.me.LocalVideoActivity;
import com.ydd.zhichat.ui.share.ShareBroadCast;
import com.ydd.zhichat.util.BitmapUtil;
import com.ydd.zhichat.util.CameraUtil;
import com.ydd.zhichat.util.DeviceInfoUtil;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.util.UploadCacheUtils;
import com.ydd.zhichat.view.LoadFrame;
import com.ydd.zhichat.view.SelectionFrame;
import com.ydd.zhichat.view.TipDialog;
import com.ydd.zhichat.volley.Result;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 发布视频
 */
public class ShareVideoActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_SELECT_LOCATE = 3;  // 位置
    private static final int REQUEST_CODE_SELECT_TYPE = 4;    // 谁可以看
    private static final int REQUEST_CODE_SELECT_REMIND = 5;  // 提醒谁看
    private EditText mTextEdit;
    // 所在位置
    private TextView mTVLocation;
    // 谁可以看
    private TextView mTVSee;
    // 提醒谁看
    private TextView mTVAt;
    // Video Item
    private FrameLayout mFloatLayout;
    private ImageView mImageView;
    private ImageView mIconImageView;
    private TextView mVideoTextTv;
    // 发布
    private Button mReleaseBtn;
    // data
    private int mSelectedId;
    private String mVideoFilePath;
    private Bitmap mThumbBmp;
    private long mTimeLen;
    private SelectionFrame mSelectionFrame;
    // 部分可见 || 不给谁看 有值 用于恢复谁可以看的界面
    private String str1;
    private String str2;
    private String str3;
    // 默认为公开
    private int visible = 1;
    // 谁可以看 || 不给谁看
    private String lookPeople;
    // 提醒谁看
    private String atlookPeople;
    // 默认不发位置
    private double latitude;
    private double longitude;
    private String address;
    private String mVideoData;
    private String mImageData;
    private LoadFrame mLoadFrame;

    public static void start(Context ctx, Intent intent) {
        intent.setClass(ctx, ShareVideoActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_video);

        initActionBar();
        initView();
        initEvent();

        String text = ShareUtil.parseText(getIntent());
        if (!TextUtils.isEmpty(text)) {
            mTextEdit.setText(text);
        }
        String filePath = ShareUtil.getFilePathFromStream(this, getIntent());
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File fVideo = new File(filePath);
        if (fVideo.exists()) {
            VideoFile videoFile = parseVideo(fVideo);
            if (videoFile == null) {
                DialogHelper.tip(this, getString(R.string.tip_file_cache_failed));
                return;
            }
            mVideoTextTv.setText("");
            mIconImageView.setBackground(null);

            mVideoFilePath = filePath;
            mThumbBmp = AvatarHelper.getInstance().displayVideoThumb(filePath, mImageView);
            mTimeLen = videoFile.getFileLength();
            // id好像没有用，
            mSelectedId = videoFile.get_id();
        }
    }

    private VideoFile parseVideo(File fVideo) {
        if (!fVideo.exists()) {
            return null;
        }
        VideoFile ret = new VideoFile();
        ret.setFilePath(fVideo.getAbsolutePath());
        ret.setFileSize(fVideo.length());
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, Uri.fromFile(fVideo));
            mediaPlayer.prepare();
            ret.setFileLength(mediaPlayer.getDuration() / 1000);
        } catch (IOException e) {
            Reporter.post("视频时长解析失败", e);
            return null;
        }
        return ret;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageView.setImageBitmap(null);
        mThumbBmp = null;
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isExitNoPublish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JX_SendVideo"));
    }

    private void initView() {
        mTextEdit = (EditText) findViewById(R.id.text_edit);
        mTextEdit.setHint(InternationalizationHelper.getString("addMsgVC_Mind"));
        // 所在位置
        mTVLocation = (TextView) findViewById(R.id.tv_location);
        // 谁可以看
        mTVSee = (TextView) findViewById(R.id.tv_see);
        // 提醒谁看
        mTVAt = (TextView) findViewById(R.id.tv_at);

        mFloatLayout = findViewById(R.id.float_layout);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mIconImageView = (ImageView) findViewById(R.id.icon_image_view);
        mIconImageView.setBackgroundResource(R.drawable.add_video);
        mVideoTextTv = (TextView) findViewById(R.id.text_tv);
        mVideoTextTv.setText(R.string.circle_add_video);
        mReleaseBtn = (Button) findViewById(R.id.release_btn);
//        mReleaseBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        mReleaseBtn.setText(InternationalizationHelper.getString("JX_Publish"));

        mLoadFrame = new LoadFrame(this);
        mLoadFrame.setSomething(getString(R.string.back_last_page), getString(R.string.open_im), new LoadFrame.OnLoadFrameClickListener() {
            @Override
            public void cancelClick() {
                com.ydd.zhichat.ui.share.ShareBroadCast.broadcastFinishActivity(ShareVideoActivity.this);
                finish();
            }

            @Override
            public void confirmClick() {
                ShareBroadCast.broadcastFinishActivity(ShareVideoActivity.this);
                startActivity(new Intent(ShareVideoActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    // 默认经纬度、地址
    // private double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
    // private double longitude = MyApplication.getInstance().getBdLocationHelper().getLng();
    // private String address = MyApplication.getInstance().getBdLocationHelper().getAddress();

    private void initEvent() {
        if (coreManager.getConfig().disableLocationServer) {
            findViewById(R.id.rl_location).setVisibility(View.GONE);
        } else {
            findViewById(R.id.rl_location).setOnClickListener(this);
        }
        findViewById(R.id.rl_see).setOnClickListener(this);
        findViewById(R.id.rl_at).setOnClickListener(this);

        mFloatLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ShareVideoActivity.this, LocalVideoActivity.class);
                intent.putExtra(AppConstant.EXTRA_ACTION, AppConstant.ACTION_SELECT);
                // 这里选择视频不支持多选，
                intent.putExtra(AppConstant.EXTRA_MULTI_SELECT, false);
                if (mSelectedId != 0) {
                    intent.putExtra(AppConstant.EXTRA_SELECT_ID, mSelectedId);
                }
                startActivityForResult(intent, 1);
            }
        });

        mReleaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mVideoFilePath) || mTimeLen <= 0) {
                    Toast.makeText(ShareVideoActivity.this, InternationalizationHelper.getString("JX_AddFile"), Toast.LENGTH_SHORT).show();
                    return;
                }
                new UploadTask().execute();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_location:
                // 所在位置
                Intent intent1 = new Intent(this, MapPickerActivity.class);
                startActivityForResult(intent1, REQUEST_CODE_SELECT_LOCATE);
                break;
            case R.id.rl_see:
                // 谁可以看
                Intent intent2 = new Intent(this, SeeCircleActivity.class);
                intent2.putExtra("THIS_CIRCLE_TYPE", visible - 1);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER1", str1);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER2", str2);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER3", str3);
                startActivityForResult(intent2, REQUEST_CODE_SELECT_TYPE);
                break;
            case R.id.rl_at:
                // 提醒谁看
                if (visible == 2) {

                    ToastUtil.showToast(ShareVideoActivity.this, R.string.tip_private_cannot_use_this);
//                    final TipDialog tipDialog = new TipDialog(this);
////                    tipDialog.setmConfirmOnClickListener(getString(R.string.tip_private_cannot_use_this), new TipDialog.ConfirmOnClickListener() {
////                        @Override
////                        public void confirm() {
////                            tipDialog.dismiss();
////                        }
////                    });
////                    tipDialog.show();
                } else {
                    Intent intent3 = new Intent(this, AtSeeCircleActivity.class);
                    intent3.putExtra("REMIND_TYPE", visible);
                    intent3.putExtra("REMIND_PERSON", lookPeople);
                    startActivityForResult(intent3, REQUEST_CODE_SELECT_REMIND);
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        isExitNoPublish();
    }

    private void isExitNoPublish() {
        if (!TextUtils.isEmpty(mVideoFilePath)) {
            mSelectionFrame = new SelectionFrame(ShareVideoActivity.this);
            mSelectionFrame.setSomething(getString(R.string.app_name), getString(R.string.tip_has_video_no_public), new SelectionFrame.OnSelectionFrameClickListener() {
                @Override
                public void cancelClick() {

                }

                @Override
                public void confirmClick() {
                    finish();
                }
            });
            mSelectionFrame.show();
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // 选择视频的返回
            String json = data.getStringExtra(AppConstant.EXTRA_VIDEO_LIST);
            List<VideoFile> fileList = JSON.parseArray(json, VideoFile.class);
            if (fileList == null || fileList.size() == 0) {
                // 不可到达，列表里有做判断，
                Reporter.unreachable();
                return;
            }
            VideoFile videoFile = fileList.get(0);

            String filePath = videoFile.getFilePath();
            if (TextUtils.isEmpty(filePath)) {
                ToastUtil.showToast(this, R.string.select_failed);

                mVideoTextTv.setText(InternationalizationHelper.getString("addMsgVC_AddVideo"));
                mIconImageView.setBackgroundResource(R.drawable.add_video);
                return;
            }
            File file = new File(filePath);
            if (!file.exists()) {
                ToastUtil.showToast(this, R.string.select_failed);

                mVideoTextTv.setText(InternationalizationHelper.getString("addMsgVC_AddVideo"));
                mIconImageView.setBackgroundResource(R.drawable.add_video);
                return;
            }
            // 返回成功，隐藏录制图标
            mVideoTextTv.setText("");
            mIconImageView.setBackground(null);

            mVideoFilePath = filePath;
            mThumbBmp = AvatarHelper.getInstance().displayVideoThumb(filePath, mImageView);
            mTimeLen = videoFile.getFileLength();
            mSelectedId = videoFile.get_id();
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_LOCATE) {
            // 选择位置返回
            latitude = data.getDoubleExtra(AppConstant.EXTRA_LATITUDE, 0);
            longitude = data.getDoubleExtra(AppConstant.EXTRA_LONGITUDE, 0);
            address = data.getStringExtra(AppConstant.EXTRA_ADDRESS);
            if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)) {
                Log.e("zq", "纬度:" + latitude + "   经度：" + longitude + "   位置：" + address);
                mTVLocation.setText(address);
            } else {
                ToastUtil.showToast(mContext, InternationalizationHelper.getString("JXLoc_StartLocNotice"));
            }
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_TYPE) {
            // 谁可以看返回
            visible = data.getIntExtra("THIS_CIRCLE_TYPE", 1);
            if (visible == 1) {
                mTVSee.setText(getString(R.string.publics));
            } else if (visible == 2) {
                mTVSee.setText(getString(R.string.privates));
                if (!TextUtils.isEmpty(atlookPeople)) {
                    final TipDialog tipDialog = new TipDialog(this);
                    tipDialog.setmConfirmOnClickListener(getString(R.string.tip_private_cannot_notify), new TipDialog.ConfirmOnClickListener() {
                        @Override
                        public void confirm() {
                            tipDialog.dismiss();
                            // 清空提醒谁看列表
                            atlookPeople = "";
                            mTVAt.setText("");
                        }
                    });
                    tipDialog.show();
                }
            } else if (visible == 3) {
                lookPeople = data.getStringExtra("THIS_CIRCLE_PERSON");
                String looKenName = data.getStringExtra("THIS_CIRCLE_PERSON_NAME");
                mTVSee.setText(looKenName);
            } else if (visible == 4) {
                lookPeople = data.getStringExtra("THIS_CIRCLE_PERSON");
                String lookName = data.getStringExtra("THIS_CIRCLE_PERSON_NAME");
                mTVSee.setText("除去 " + lookName);
            }
            str1 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER1");
            str2 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER2");
            str3 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER3");
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_REMIND) {
            // 提醒谁看返回
            atlookPeople = data.getStringExtra("THIS_CIRCLE_REMIND_PERSON");
            String atLookPeopleName = data.getStringExtra("THIS_CIRCLE_REMIND_PERSON_NAME");
            mTVAt.setText(atLookPeopleName);
        }
    }

    public void sendAudio() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        // 消息类型：1=文字消息；2=图文消息；3=语音消息；4=视频消息；
        params.put("type", "4");
        // 消息标记：1：求职消息；2：招聘消息；3：普通消息；
        params.put("flag", "3");

        // 消息隐私范围：1=公开；2=私密；3=部分选中好友可见；4=不给谁看
        params.put("visible", String.valueOf(visible));
        if (visible == 3) {
            // 谁可以看
            params.put("userLook", lookPeople);
        } else if (visible == 4) {
            // 不给谁看
            params.put("userNotLook", lookPeople);
        }
        // 提醒谁看
        if (!TextUtils.isEmpty(atlookPeople)) {
            params.put("userRemindLook", atlookPeople);
        }

        // 消息内容
        params.put("text", mTextEdit.getText().toString());
        params.put("videos", mVideoData);
        if (!TextUtils.isEmpty(mImageData) && !mImageData.equals("{}") && !mImageData.equals("[{}]")) {
            params.put("images", mImageData);
        }

        /**
         * 所在位置
         */
        if (!TextUtils.isEmpty(address)) {
            // 纬度
            params.put("latitude", String.valueOf(latitude));
            // 经度
            params.put("longitude", String.valueOf(longitude));
            // 位置
            params.put("location", address);
        }

        // 必传，之前删除该字段，发布说说，服务器返回接口内部异常
        Area area = Area.getDefaultCity();
        if (area != null) {
            params.put("cityId", String.valueOf(area.getId()));// 城市Id
        } else {
            params.put("cityId", "0");
        }

        /**
         * 附加信息
         */
        // 手机型号
        params.put("model", DeviceInfoUtil.getModel());
        // 手机操作系统版本号
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        if (!TextUtils.isEmpty(DeviceInfoUtil.getDeviceId(mContext))) {
            // 设备序列号
            params.put("serialNumber", DeviceInfoUtil.getDeviceId(mContext));
        }

        HttpUtils.get().url(coreManager.getConfig().MSG_ADD_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {

                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        if (result.getResultCode() == 1) {
                            mLoadFrame.change();
                        } else {
                            mLoadFrame.dismiss();
                            Toast.makeText(ShareVideoActivity.this, getString(R.string.share_failed), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        mLoadFrame.dismiss();
                        ToastUtil.showErrorNet(ShareVideoActivity.this);
                    }
                });
    }

    private class UploadTask extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadFrame.show();
        }

        /**
         * 上传的结果： <br/>
         * return 1 Token过期，请重新登陆 <br/>
         * return 2 视频为空，请重新录制 <br/>
         * return 3 上传出错<br/>
         * return 4 上传成功<br/>
         */
        @Override
        protected Integer doInBackground(Void... params) {
            if (!LoginHelper.isTokenValidation()) {
                return 1;
            }
            if (TextUtils.isEmpty(mVideoFilePath)) {
                return 2;
            }

            // 保存视频缩略图至sd卡
            String imageSavePsth = CameraUtil.getOutputMediaFileUri(ShareVideoActivity.this, CameraUtil.MEDIA_TYPE_IMAGE).getPath();
            if (!BitmapUtil.saveBitmapToSDCard(mThumbBmp, imageSavePsth)) {// 保存缩略图失败
                return 3;
            }

            Map<String, String> mapParams = new HashMap<String, String>();
            mapParams.put("access_token", coreManager.getSelfStatus().accessToken);
            mapParams.put("userId", coreManager.getSelf().getUserId() + "");
            mapParams.put("validTime", "-1");// 文件有效期

            List<String> dataList = new ArrayList<String>();
            dataList.add(mVideoFilePath);
            if (!TextUtils.isEmpty(imageSavePsth)) {
                dataList.add(imageSavePsth);
            }
            String result = new UploadService().uploadFile(coreManager.getConfig().UPLOAD_URL, mapParams, dataList);
            if (TextUtils.isEmpty(result)) {
                return 3;
            }

            UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
            boolean success = Result.defaultParser(ShareVideoActivity.this, recordResult, true);
            if (success) {
                if (recordResult.getSuccess() != recordResult.getTotal()) {// 上传丢失了某些文件
                    return 3;
                }
                if (recordResult.getData() != null) {
                    UploadFileResult.Data data = recordResult.getData();
                    if (data.getVideos() != null && data.getVideos().size() > 0) {
                        while (data.getVideos().size() > 1) {// 因为正确情况下只有一个视频，所以要保证只有一个视频
                            data.getVideos().remove(data.getVideos().size() - 1);
                        }
                        data.getVideos().get(0).setSize(new File(mVideoFilePath).length());
                        data.getVideos().get(0).setLength(mTimeLen);
                        // 记录本机上传，用于快速读取，
                        UploadCacheUtils.save(ShareVideoActivity.this, data.getVideos().get(0).getOriginalUrl(), mVideoFilePath);
                        mVideoData = JSON.toJSONString(data.getVideos(), UploadFileResult.sAudioVideosFilter);
                    } else {
                        return 3;
                    }
                    if (data.getImages() != null && data.getImages().size() > 0) {
                        mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
                    }
                    return 4;
                } else {// 没有文件数据源，失败
                    return 3;
                }
            } else {
                return 3;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result == 1) {
                mLoadFrame.dismiss();
                startActivity(new Intent(ShareVideoActivity.this, SplashActivity.class));
            } else if (result == 2) {
                mLoadFrame.dismiss();
                ToastUtil.showToast(ShareVideoActivity.this, InternationalizationHelper.getString("JXAlert_NotHaveFile"));
            } else if (result == 3) {
                mLoadFrame.dismiss();
                ToastUtil.showToast(ShareVideoActivity.this, R.string.upload_failed);
            } else {
                sendAudio();
            }
        }
    }
}
