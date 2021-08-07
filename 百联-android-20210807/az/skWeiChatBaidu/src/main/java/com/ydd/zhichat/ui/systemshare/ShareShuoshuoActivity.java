package com.ydd.zhichat.ui.systemshare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.Area;
import com.ydd.zhichat.bean.UploadFileResult;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.helper.LoginHelper;
import com.ydd.zhichat.helper.UploadService;
import com.ydd.zhichat.ui.MainActivity;
import com.ydd.zhichat.ui.SplashActivity;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.circle.range.AtSeeCircleActivity;
import com.ydd.zhichat.ui.circle.range.SeeCircleActivity;
import com.ydd.zhichat.ui.map.MapPickerActivity;
import com.ydd.zhichat.ui.share.ShareBroadCast;
import com.ydd.zhichat.ui.tool.MultiImagePreviewActivity;
import com.ydd.zhichat.util.DeviceInfoUtil;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.video.EasyCameraActivity;
import com.ydd.zhichat.video.MessageEventGpu;
import com.ydd.zhichat.view.LoadFrame;
import com.ydd.zhichat.view.MyGridView;
import com.ydd.zhichat.view.SquareCenterImageView;
import com.ydd.zhichat.view.TipDialog;
import com.ydd.zhichat.view.photopicker.PhotoPickerActivity;
import com.ydd.zhichat.view.photopicker.SelectModel;
import com.ydd.zhichat.view.photopicker.intent.PhotoPickerIntent;
import com.ydd.zhichat.volley.Result;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

/**
 * 发布文字 || 图片
 */
public class ShareShuoshuoActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_CAPTURE_PHOTO = 1;  // 拍照
    private static final int REQUEST_CODE_PICK_PHOTO = 2;     // 图库
    private static final int REQUEST_CODE_SELECT_LOCATE = 3;  // 位置
    private static final int REQUEST_CODE_SELECT_TYPE = 4;    // 谁可以看
    private static final int REQUEST_CODE_SELECT_REMIND = 5;  // 提醒谁看
    private final int mType = 1;
    private EditText mTextEdit;
    // 所在位置
    private TextView mTVLocation;
    // 谁可以看
    private TextView mTVSee;
    // 提醒谁看
    private TextView mTVAt;
    // 发布图片类型说说可见
    private LinearLayout mSelectImgLayout;
    private MyGridView mGridView;
    // 发布
    private Button mReleaseBtn;
    private GridViewAdapter mAdapter;
    private ArrayList<String> mPhotoList;
    private String mImageData;
    // 部分可见 || 不给谁看 有值 用于恢复谁可以看的界面
    private String str1;
    private String str2;
    private String str3;
    // 拍照和图库，获得图片的Uri
    private Uri mNewPhotoUri;
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
    private LoadFrame mLoadFrame;

    public static void start(Context ctx, Intent intent) {
        intent.setClass(ctx, ShareShuoshuoActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_shuoshuo);

        mPhotoList = new ArrayList<>();
        mAdapter = new GridViewAdapter();
        initActionBar();
        initView();
        initEvent();
        EventBus.getDefault().register(this);

        String text = ShareUtil.parseText(getIntent());
        if (!TextUtils.isEmpty(text)) {
            mTextEdit.setText(text);
        }
        String image = ShareUtil.getFilePathFromStream(this, getIntent());
        if (!TextUtils.isEmpty(image) && new File(image).exists()) {
            ArrayList<String> list = new ArrayList<>(1);
            list.add(image);
            album(list, true);
        }
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
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.send_image_text);
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

        mSelectImgLayout = findViewById(R.id.select_img_layout);
        mGridView = (MyGridView) findViewById(R.id.grid_view);
        mGridView.setAdapter(mAdapter);
        if (mType == 1) {// 发布图片
            mSelectImgLayout.setVisibility(View.VISIBLE);
        }
        // 发布
        mReleaseBtn = (Button) findViewById(R.id.release_btn);
        mReleaseBtn.setText(InternationalizationHelper.getString("JX_Publish"));
//        mReleaseBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());

        mLoadFrame = new LoadFrame(this);
        mLoadFrame.setSomething(getString(R.string.back_last_page), getString(R.string.open_im), new LoadFrame.OnLoadFrameClickListener() {
            @Override
            public void cancelClick() {
                ShareBroadCast.broadcastFinishActivity(ShareShuoshuoActivity.this);
                finish();
            }

            @Override
            public void confirmClick() {
                ShareBroadCast.broadcastFinishActivity(ShareShuoshuoActivity.this);
                startActivity(new Intent(ShareShuoshuoActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void initEvent() {
        if (coreManager.getConfig().disableLocationServer) {
            findViewById(R.id.rl_location).setVisibility(View.GONE);
        } else {
            findViewById(R.id.rl_location).setOnClickListener(this);
        }
        findViewById(R.id.rl_see).setOnClickListener(this);
        findViewById(R.id.rl_at).setOnClickListener(this);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int viewType = mAdapter.getItemViewType(position);
                if (viewType == 1) {
                    showSelectPictureDialog();
                } else {
                    showPictureActionDialog(position);
                }
            }
        });

        mReleaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotoList.size() <= 0 && TextUtils.isEmpty(mTextEdit.getText().toString())) {
                    return;
                }
                if (mPhotoList.size() <= 0) {
                    // 发布文字
                    mLoadFrame.show();
                    sendShuoshuo();
                } else {
                    // 发布图片+文字
                    mLoadFrame.show();
                    new UploadPhoto().execute();
                }
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
                    ToastUtil.showToast(ShareShuoshuoActivity.this, R.string.tip_private_cannot_use_this);
//                    final TipDialog tipDialog = new TipDialog(this);
//                    tipDialog.setmConfirmOnClickListener(getString(R.string.tip_private_cannot_use_this), new TipDialog.ConfirmOnClickListener() {
//                        @Override
//                        public void confirm() {
//                            tipDialog.dismiss();
//                        }
//                    });
//                    tipDialog.show();
                } else {
                    Intent intent3 = new Intent(this, AtSeeCircleActivity.class);
                    intent3.putExtra("REMIND_TYPE", visible);
                    intent3.putExtra("REMIND_PERSON", lookPeople);
                    startActivityForResult(intent3, REQUEST_CODE_SELECT_REMIND);
                }
                break;
        }
    }

    private void showSelectPictureDialog() {
        String[] items = new String[]{InternationalizationHelper.getString("PHOTOGRAPH"), InternationalizationHelper.getString("ALBUM")};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setSingleChoiceItems(items, 0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            takePhoto();
                        } else {
                            selectPhoto();
                        }
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void showPictureActionDialog(final int position) {
        String[] items = new String[]{getString(R.string.look_over), InternationalizationHelper.getString("JX_Delete")};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(InternationalizationHelper.getString("JX_Image"))
                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // 查看
                            Intent intent = new Intent(ShareShuoshuoActivity.this, MultiImagePreviewActivity.class);
                            intent.putExtra(AppConstant.EXTRA_IMAGES, mPhotoList);
                            intent.putExtra(AppConstant.EXTRA_POSITION, position);
                            intent.putExtra(AppConstant.EXTRA_CHANGE_SELECTED, false);
                            startActivity(intent);
                        } else {
                            // 删除
                            deletePhoto(position);
                        }
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void deletePhoto(final int position) {
        mPhotoList.remove(position);
        mAdapter.notifyDataSetInvalidated();
    }

    // 拍照
    private void takePhoto() {
      /*  mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
        CameraUtil.captureImage(this, mNewPhotoUri, REQUEST_CODE_CAPTURE_PHOTO);*/
        Intent intent = new Intent(this, EasyCameraActivity.class);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventGpu message) {
        photograph(new File(message.event));
    }

    /**
     * 相册
     * 可以多选的图片选择器
     */
    private void selectPhoto() {
        ArrayList<String> imagePaths = new ArrayList<>();
        PhotoPickerIntent intent = new PhotoPickerIntent(ShareShuoshuoActivity.this);
        intent.setSelectModel(SelectModel.MULTI);
        // 是否显示拍照， 默认false
        intent.setShowCarema(false);
        // 最多选择照片数量，默认为9
        intent.setMaxTotal(9 - mPhotoList.size());
        // 已选中的照片地址， 用于回显选中状态
        intent.setSelectedPaths(imagePaths);
        // intent.setImageConfig(config);
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
    }

    // 默认经纬度、地址
    // private double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
    // private double longitude = MyApplication.getInstance().getBdLocationHelper().getLng();
    // private String address = MyApplication.getInstance().getBdLocationHelper().getAddress();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {
            // 拍照返回 Todo 已更换拍照方式
            if (resultCode == Activity.RESULT_OK) {
                if (mNewPhotoUri != null) {
                    photograph(new File(mNewPhotoUri.getPath()));
                } else {
                    ToastUtil.showToast(this, R.string.c_take_picture_failed);
                }
            }
        } else if (requestCode == REQUEST_CODE_PICK_PHOTO) {
            // 选择图片返回
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    boolean isOriginal = data.getBooleanExtra(PhotoPickerActivity.EXTRA_RESULT_ORIGINAL, false);
                    album(data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT), isOriginal);
                } else {
                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
                }
            }
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
                mTVSee.setText(R.string.publics);
            } else if (visible == 2) {
                mTVSee.setText(R.string.privates);
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

    // 单张图片压缩 拍照
    private void photograph(final File file) {
        Log.e("zq", "压缩前图片路径:" + file.getPath() + "压缩前图片大小:" + file.length() / 1024 + "KB");
        // 拍照出来的图片Luban一定支持，
        Luban.with(this)
                .load(file)
                .ignoreBy(100)     // 原图小于100kb 不压缩
                // .putGear(2)     // 设定压缩档次，默认三挡
                // .setTargetDir() // 指定压缩后的图片路径
                .setCompressListener(new OnCompressListener() { //设置回调
                    @Override
                    public void onStart() {
                        Log.e("zq", "开始压缩");
                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.e("zq", "压缩成功，压缩后图片位置:" + file.getPath() + "压缩后图片大小:" + file.length() / 1024 + "KB");
                        mPhotoList.add(file.getPath());
                        mAdapter.notifyDataSetInvalidated();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("zq", "压缩失败,原图上传");
                        mPhotoList.add(file.getPath());
                        mAdapter.notifyDataSetInvalidated();
                    }
                }).launch();// 启动压缩
    }

    // 多张图片压缩 相册
    private void album(ArrayList<String> stringArrayListExtra, boolean isOriginal) {
        if (isOriginal) {// 原图发送，不压缩
            Log.e("zq", "原图上传，不压缩，选择原文件路径");
            for (int i = 0; i < stringArrayListExtra.size(); i++) {
                mPhotoList.add(stringArrayListExtra.get(i));
                mAdapter.notifyDataSetInvalidated();
            }
            return;
        }

        List<File> fileList = new ArrayList<>();
        for (int i = 0; i < stringArrayListExtra.size(); i++) {
            // Luban只处理特定后缀的图片，不满足的不处理也不走回调，
            // 只能挑出来不压缩，
            List<String> lubanSupportFormatList = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");
            boolean support = false;
            for (int j = 0; j < lubanSupportFormatList.size(); j++) {
                if (stringArrayListExtra.get(i).endsWith(lubanSupportFormatList.get(j))) {
                    support = true;
                    break;
                }
            }
            if (!support) {
                fileList.add(new File(stringArrayListExtra.get(i)));
                stringArrayListExtra.remove(i);
            }
        }

        if (fileList.size() > 0) {
            for (File file : fileList) {// 不压缩的部分，直接发送
                mPhotoList.add(file.getPath());
                mAdapter.notifyDataSetInvalidated();
            }
        }

        Luban.with(this)
                .load(stringArrayListExtra)
                .ignoreBy(100)// 原图小于100kb 不压缩
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        mPhotoList.add(file.getPath());
                        mAdapter.notifyDataSetInvalidated();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();// 启动压缩
    }

    // 发布一条说说
    public void sendShuoshuo() {

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        // 消息类型：1=文字消息；2=图文消息；3=语音消息；4=视频消息；
        if (TextUtils.isEmpty(mImageData)) {
            params.put("type", "1");
        } else {
            params.put("type", "2");
        }
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
            // 城市id
            params.put("cityId", String.valueOf(area.getId()));
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
                            Toast.makeText(ShareShuoshuoActivity.this, getString(R.string.share_failed), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        mLoadFrame.dismiss();
                        ToastUtil.showErrorNet(ShareShuoshuoActivity.this);
                    }
                });
    }

    private class UploadPhoto extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * 上传的结果： <br/>
         * return 1 Token过期，请重新登陆 <br/>
         * return 2 上传出错<br/>
         * return 3 上传成功<br/>
         */
        @Override
        protected Integer doInBackground(Void... params) {
            if (!LoginHelper.isTokenValidation()) {
                return 1;
            }
            Map<String, String> mapParams = new HashMap<>();
            mapParams.put("access_token", coreManager.getSelfStatus().accessToken);
            mapParams.put("userId", coreManager.getSelf().getUserId() + "");
            mapParams.put("validTime", "-1");// 文件有效期

            String result = new UploadService().uploadFile(coreManager.getConfig().UPLOAD_URL, mapParams, mPhotoList);
            if (TextUtils.isEmpty(result)) {
                return 2;
            }

            UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
            boolean success = Result.defaultParser(ShareShuoshuoActivity.this, recordResult, true);
            if (success) {
                if (recordResult.getSuccess() != recordResult.getTotal()) {
                    // 上传丢失了某些文件
                    return 2;
                }
                if (recordResult.getData() != null) {
                    UploadFileResult.Data data = recordResult.getData();
                    if (data.getImages() != null && data.getImages().size() > 0) {
                        mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
                    }
                    return 3;
                } else {
                    // 没有文件数据源，失败
                    return 2;
                }
            } else {
                return 2;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result == 1) {
                mLoadFrame.dismiss();
                startActivity(new Intent(ShareShuoshuoActivity.this, SplashActivity.class));
            } else if (result == 2) {
                mLoadFrame.dismiss();
                ToastUtil.showToast(ShareShuoshuoActivity.this, getString(R.string.upload_failed));
            } else {
                sendShuoshuo();
            }
        }
    }

    private class GridViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (mPhotoList.size() >= 9) {
                return 9;
            }
            return mPhotoList.size() + 1;
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
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (mPhotoList.size() == 0) {
                // View Type 1代表添加更多的视图
                return 1;
            } else if (mPhotoList.size() < 9) {
                if (position < mPhotoList.size()) {
                    // View Type 0代表普通的ImageView视图
                    return 0;
                } else {
                    return 1;
                }
            } else {
                return 0;
            }
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(ShareShuoshuoActivity.this)
                    .inflate(R.layout.layout_circle_add_more_item_temp, parent, false);
            SquareCenterImageView iv = (SquareCenterImageView) view.findViewById(R.id.iv);
            if (getItemViewType(position) == 0) { // 普通的视图
                Glide.with(ShareShuoshuoActivity.this)
                        .load(mPhotoList.get(position))
                        .override(150, 150)
                        .error(R.drawable.pic_error)
                        .into(iv);
            } else {
                iv.setImageResource(R.drawable.circle_image);
            }
            return view;
        }
    }
}
