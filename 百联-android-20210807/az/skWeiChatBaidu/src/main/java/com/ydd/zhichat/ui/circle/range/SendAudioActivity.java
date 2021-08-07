package com.ydd.zhichat.ui.circle.range;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.ydd.im.audio.MessageEventVoice;
import com.ydd.im.audio.VoiceRecordActivity2;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.Area;
import com.ydd.zhichat.bean.UploadFileResult;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.helper.LoginHelper;
import com.ydd.zhichat.helper.UploadService;
import com.ydd.zhichat.ui.account.LoginHistoryActivity;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.map.MapPickerActivity;
import com.ydd.zhichat.util.DeviceInfoUtil;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.SelectionFrame;
import com.ydd.zhichat.view.TipDialog;
import com.ydd.zhichat.volley.Result;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 发布音频
 */
public class SendAudioActivity extends BaseActivity implements View.OnClickListener {
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
    // Voice Item
    private FrameLayout mFloatLayout;
    private ImageView mImageView;
    private ImageView mIconImageView;
    private TextView mVoiceTextTv;
    // 发布
    private Button mReleaseBtn;
    // data
    private String mAudioFilePath;
    private int mTimeLen;
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
    private String mRecordData;
    private String mImageData;
    private CheckBox checkBox;
    private boolean isBoolBan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_audio);
        initActionBar();
        initView();
        initEvent();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
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
        tvTitle.setText(InternationalizationHelper.getString("JX_SendVoice"));
    }

    private void initView() {
        checkBox = findViewById(R.id.cb_ban);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.e("zx", "onCheckedChanged: " + isChecked);
                isBoolBan = isChecked;
            }
        });
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
        mIconImageView.setBackgroundResource(R.drawable.add_voice);
        mVoiceTextTv = (TextView) findViewById(R.id.text_tv);
        mVoiceTextTv.setText(R.string.circle_add_voice);
        mReleaseBtn = (Button) findViewById(R.id.release_btn);
//        mReleaseBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        mReleaseBtn.setText(InternationalizationHelper.getString("JX_Publish"));
    }

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
                Intent intent = new Intent(SendAudioActivity.this, VoiceRecordActivity2.class);
                startActivity(intent);
            }
        });

        mReleaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mAudioFilePath) || mTimeLen <= 0) {
                    // Toast.makeText(SendAudioActivity.this, "需要上传录音", Toast.LENGTH_SHORT).show();
                    Toast.makeText(SendAudioActivity.this, InternationalizationHelper.getString("JX_AddFile"), Toast.LENGTH_SHORT).show();
                    return;
                }
                new UploadTask().execute();
            }
        });
    }

    // 默认经纬度、地址
    // private double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
    // private double longitude = MyApplication.getInstance().getBdLocationHelper().getLng();
    // private String address = MyApplication.getInstance().getBdLocationHelper().getAddress();

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
                    ToastUtil.showToast(SendAudioActivity.this, R.string.tip_private_cannot_use_this);
                } else {
                    Intent intent3 = new Intent(this, AtSeeCircleActivity.class);
                    intent3.putExtra("REMIND_TYPE", visible);
                    intent3.putExtra("REMIND_PERSON", lookPeople);
                    startActivityForResult(intent3, REQUEST_CODE_SELECT_REMIND);
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventVoice message) {
        mAudioFilePath = message.event;
        mTimeLen = (int) (message.timelen / 1000);
        // 获取音频时长
        if (!TextUtils.isEmpty(mAudioFilePath) && mTimeLen > 0) {
            mVoiceTextTv.setText(mTimeLen + "s");
        }
    }

    @Override
    public void onBackPressed() {
        isExitNoPublish();
    }

    private void isExitNoPublish() {
        if (!TextUtils.isEmpty(mAudioFilePath)) {
            mSelectionFrame = new SelectionFrame(SendAudioActivity.this);
            mSelectionFrame.setSomething(getString(R.string.app_name), getString(R.string.tip_has_voice_no_public), new SelectionFrame.OnSelectionFrameClickListener() {
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
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_LOCATE) {
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
        params.put("type", "3");
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
        if (!TextUtils.isEmpty(mImageData)) {
            // 图片
            params.put("images", mImageData);
        }
        params.put("text", mTextEdit.getText().toString());
        params.put("audios", mRecordData);
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

        params.put("isAllowComment", isBoolBan ? String.valueOf(1) : String.valueOf(0));


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

        DialogHelper.showDefaulteMessageProgressDialog(SendAudioActivity.this);

        HttpUtils.get().url(coreManager.getConfig().MSG_ADD_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {

                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        DialogHelper.dismissProgressDialog();
                        Intent intent = new Intent();
                        intent.putExtra(AppConstant.EXTRA_MSG_ID, result.getData());
                        setResult(RESULT_OK, intent);
                        finish();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(SendAudioActivity.this);
                    }
                });
    }

    private class UploadTask extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DialogHelper.showDefaulteMessageProgressDialog(SendAudioActivity.this);
        }

        /**
         * 上传的结果： <br/>
         * return 1 Token过期，请重新登陆 <br/>
         * return 2 语音为空，请重新录制 <br/>
         * return 3 上传出错<br/>
         * return 4 上传成功<br/>
         */
        @Override
        protected Integer doInBackground(Void... params) {
            if (!LoginHelper.isTokenValidation()) {
                return 1;
            }
            if (TextUtils.isEmpty(mAudioFilePath)) {
                return 2;
            }
            Map<String, String> mapParams = new HashMap<String, String>();
            mapParams.put("access_token", coreManager.getSelfStatus().accessToken);
            mapParams.put("userId", coreManager.getSelf().getUserId() + "");
            mapParams.put("validTime", "-1");// 文件有效期

            List<String> dataList = new ArrayList<String>();
            dataList.add(mAudioFilePath);

            String result = new UploadService().uploadFile(coreManager.getConfig().UPLOAD_AUDIO_URL, mapParams, dataList);
            if (TextUtils.isEmpty(result)) {
                return 3;
            }

            UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
            boolean success = Result.defaultParser(SendAudioActivity.this, recordResult, true);
            if (success) {
                if (recordResult.getSuccess() != recordResult.getTotal()) {// 上传丢失了某些文件
                    return 3;
                }
                if (recordResult.getData() != null) {
                    UploadFileResult.Data data = recordResult.getData();
                    if (data.getAudios() != null && data.getAudios().size() > 0) {
                        while (data.getAudios().size() > 1) {// 因为正确情况下只有一个语音，所以要保证只有一个语音
                            data.getAudios().remove(data.getAudios().size() - 1);
                        }
                        data.getAudios().get(0).setSize(new File(mAudioFilePath).length());
                        data.getAudios().get(0).setLength(mTimeLen);
                        mRecordData = JSON.toJSONString(data.getAudios(), UploadFileResult.sAudioVideosFilter);
                    } else {
                        return 3;
                    }

                    if (data.getImages() != null && data.getImages().size() > 0) {
                        mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
                    }
                    return 4;
                } else {
                    // 没有文件数据源，失败
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
                DialogHelper.dismissProgressDialog();
                startActivity(new Intent(SendAudioActivity.this, LoginHistoryActivity.class));
            } else if (result == 2) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(SendAudioActivity.this, InternationalizationHelper.getString("JXAlert_NotHaveFile"));
            } else if (result == 3) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(SendAudioActivity.this, R.string.upload_failed);
            } else {
                sendAudio();
            }
        }
    }
}
