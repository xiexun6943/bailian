package com.ydd.zhichat.ui.mucfile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.daimajia.numberprogressbar.NumberProgressBar;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.bean.message.XmppMessage;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.db.dao.ChatMessageDao;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.message.InstantMessageActivity;
import com.ydd.zhichat.ui.mucfile.bean.DownBean;
import com.ydd.zhichat.ui.mucfile.bean.MucFileBean;
import com.ydd.zhichat.util.TimeUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.video.ChatVideoPreviewActivity;

import java.io.File;
import java.util.UUID;

import static com.ydd.zhichat.ui.mucfile.DownManager.STATE_DOWNLOADED;
import static com.ydd.zhichat.ui.mucfile.DownManager.STATE_DOWNLOADFAILED;
import static com.ydd.zhichat.ui.mucfile.DownManager.STATE_UNDOWNLOAD;
import static com.ydd.zhichat.ui.mucfile.DownManager.STATE_WAITINGDOWNLOAD;

/**
 * Created by Administrator on 2017/7/4.
 */
public class MucFileDetails extends BaseActivity implements DownManager.DownLoadObserver, View.OnClickListener {
    private ImageView ivInco;
    private TextView tvName;
    private Button btnStart;
    private TextView tvSize;
    private NumberProgressBar progressPar;
    private MucFileBean data;
    private RelativeLayout rlProgress;
    private TextView tvType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_muc_dateils);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());

        ImageView ivRight = findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.ic_share_right);
        ivRight.setOnClickListener(v -> {
            shareFile();
        });

        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JX_Detail"));
        data = (MucFileBean) getIntent().getSerializableExtra("data");
        Log.e(TAG, "onCreate: " + data);
        initView();
        initDatas();
    }

    /**
     * 发送给朋友
     */
    private void shareFile() {

        String mLoginUserId = coreManager.getSelf().getUserId();

        ChatMessage message = new ChatMessage();
        message.setType(fileType2XmppType(data.getType()));
        message.setContent(data.getUrl());
        message.setFileSize((int) data.getSize());
        message.setFilePath(data.getName());
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());

        // Todo 将封装好的消息存入10010 号的msg 表内，在跳转至转发->聊天界面(跳转传值均为10010号与msgId)，之后在聊天界面内通过这两个值查询到对用消息，发送
        String mNewUserId = "10010";
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mNewUserId, message)) {
            Intent intent = new Intent(MucFileDetails.this, InstantMessageActivity.class);
            intent.putExtra("fromUserId", mNewUserId);
            intent.putExtra("messageId", message.getPacketId());
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(mContext, "消息封装失败", Toast.LENGTH_SHORT).show();
        }
    }

    private int fileType2XmppType(int type) {
        if (type == 1) {
            return XmppMessage.TYPE_IMAGE;
        } else if (type == 3) {
            return XmppMessage.TYPE_VIDEO;
        }
        return XmppMessage.TYPE_FILE;
    }

    @Override
    protected void onResume() {
        super.onResume();
        DownManager.instance().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DownManager.instance().deleteObserver(MucFileDetails.this);
    }

    public void initView() {
        Log.e(TAG, "initview: ");
        ivInco = (ImageView) findViewById(R.id.item_file_inco);
        tvName = (TextView) findViewById(R.id.item_file_name);
        tvType = (TextView) findViewById(R.id.item_file_type);
        tvSize = (TextView) findViewById(R.id.muc_dateils_size);

        btnStart = (Button) findViewById(R.id.btn_muc_down);
//        btnStart.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        progressPar = (NumberProgressBar) findViewById(R.id.number_progress_bar);
        rlProgress = (RelativeLayout) findViewById(R.id.muc_dateils_rl_pro);
        btnStart.setOnClickListener(this);
        findViewById(R.id.muc_dateils_stop).setOnClickListener(this);
        findViewById(R.id.muc_dateils_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });

        tvType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (data.getState() == DownManager.STATE_DOWNLOADING) {
                    DownManager.instance().pause(data);
                }

                if (data.getType() == 2) { // 音乐在线播放
                    Intent intent = new Intent(mContext, ChatVideoPreviewActivity.class);
                    intent.putExtra(AppConstant.EXTRA_VIDEO_FILE_PATH, data.getUrl());
                    startActivity(intent);
                } else if (data.getType() == 3) {  // 视频在线播放
                    Intent intent = new Intent(mContext, ChatVideoPreviewActivity.class);
                    intent.putExtra(AppConstant.EXTRA_VIDEO_FILE_PATH, data.getUrl());
                    startActivity(intent);
                } else if (data.getType() == 4 || data.getType() == 5 || data.getType() == 6 || data.getType() == 10) {
                    // 在线预览文档，需要解析，上面为微软的，下面为谷歌的(google无法访问)
                   /* String url = "https://view.officeapps.live.com/op/view.aspx?src=" + data.getUrl();
                    //String url = "https://docs.google.com/viewer?url=" + data.getUrl();
                    Intent intent = new Intent(MucFileDetails.this, WebViewActivity.class);
                    intent.putExtra(EXTRA_URL, url);
                    startActivity(intent);*/
                    ToastUtil.showToast(MucFileDetails.this, "暂不支持该类型文件预览");
                } else {
                    Intent intent = new Intent(MucFileDetails.this, MucFilePreviewActivity.class);
                    intent.putExtra("data", data);
                    startActivity(intent);
                }
            }
        });
    }

    protected void initDatas() {
        Log.e(TAG, "initDatas: ");
        if (data != null) {
            updateUI();
        }
    }

    private void updateUI() {
        Log.e(TAG, "updateUI: ");
        if (data.getType() == 1) {
            // 图片直接显示
            Glide.with(MucFileDetails.this).load(data.getUrl()).override(100, 100).centerCrop().into(ivInco);
        } else {
            // 加载本地
            XfileUtils.setFileInco(data.getType(), ivInco);
        }
        tvName.setText(data.getName());

        if (data.getType() == 9) {
            tvType.setText(InternationalizationHelper.getString("NOT_SUPPORT_PREVIEW"));
        } else if (data.getType() == 4 || data.getType() == 5 || data.getType() == 6 || data.getType() == 10) {
            // tvType.setText("该文件暂不支持在线预览");
            tvType.setText(InternationalizationHelper.getString("NOT_SUPPORT_PREVIEW"));
        } else {

            SpannableString type = XfileUtils.matcherSearchTitle(Color.parseColor("#6699FF"),
                    InternationalizationHelper.getString("PREVIEW_ONLINE"), InternationalizationHelper.getString("JXFile_online"));
            tvType.setText(type);

        }
        tvName.setText(data.getName());
        DownBean info = DownManager.instance().getDownloadState(data);
        onDownLoadInfoChange(info);
    }

    @Override
    public void onClick(View v) {
        if (checkNet()) {
            // 检查网络
            switch (data.getState()) {
                case STATE_DOWNLOADED:
                    open();
                    break;
                case STATE_UNDOWNLOAD:
                    down();
                    break;
                case DownManager.STATE_DOWNLOADING:
                    stop();
                    break;
                case DownManager.STATE_PAUSEDOWNLOAD:
                    start();
                    break;
                case STATE_WAITINGDOWNLOAD:
                    cancelDown();
                    break;
                case STATE_DOWNLOADFAILED:
                    down();
                    break;
            }
        }
    }

    private void open() {
        File file = new File(DownManager.instance().getFileDir(), data.getName());
        FileOpenWays openWays = new FileOpenWays(mContext);
        openWays.openFiles(file.getAbsolutePath());
    }

    private void del() {
        DownManager.instance().detele(data);
    }

    private void cancelDown() {
        DownManager.instance().cancel(data);
    }

    private void stop() {
        DownManager.instance().pause(data);
    }

    private void down() {
        DownManager.instance().download(data);
    }

    private void start() {
        DownManager.instance().download(data);
    }

    private boolean checkNet() {
        return true;
    }

    @Override
    public void onDownLoadInfoChange(final DownBean info) {
        data.setState(info.state);

        int progress = (int) (info.cur / (float) info.max * 100);
        progressPar.setProgress(progress);

        rlProgress.setVisibility(View.VISIBLE);

        switch (info.state) {
            case STATE_DOWNLOADED:
                //                tvType.setText("下载完成");
                //                btnStart.setText("打开");
                tvType.setText(InternationalizationHelper.getString("DOWNLOAD_COMPLETE"));
                btnStart.setText(InternationalizationHelper.getString("JX_Open"));
                rlProgress.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
                break;
            case STATE_UNDOWNLOAD:
                //                tvSize.setText("未下载");
                //                btnStart.setText("下载(" + XfileUtils.fromatSize(info.max) + ")");
                tvSize.setText(InternationalizationHelper.getString("NOT_DOWNLOADED"));
                btnStart.setText(InternationalizationHelper.getString("JX_Download") + "(" + XfileUtils.fromatSize(info.max) + ")");
                rlProgress.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
                break;
            case DownManager.STATE_DOWNLOADING:
                //                tvSize.setText("下载中…(" + XfileUtils.fromatSize(info.cur) + "/" + XfileUtils.fromatSize(info.max) + ")");
                tvSize.setText(InternationalizationHelper.getString("JX_Downloading") + "…(" + XfileUtils.fromatSize(info.cur) + "/" + XfileUtils.fromatSize(info.max) + ")");
                btnStart.setVisibility(View.GONE);
                rlProgress.setVisibility(View.VISIBLE);
                break;
            case DownManager.STATE_PAUSEDOWNLOAD:
                rlProgress.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
                //                tvSize.setText("暂停中…(" + XfileUtils.fromatSize(info.cur) + "/" + XfileUtils.fromatSize(info.max) + ")");
                //                btnStart.setText("继续下载(" + XfileUtils.fromatSize((info.max - info.cur)) + ")");
                tvSize.setText(InternationalizationHelper.getString("IN_PAUSE") + "…(" + XfileUtils.fromatSize(info.cur) + "/" + XfileUtils.fromatSize(info.max) + ")");
                btnStart.setText(InternationalizationHelper.getString("CONTINUE_DOWNLOADING") + "(" + XfileUtils.fromatSize((info.max - info.cur)) + ")");
                break;
            case STATE_WAITINGDOWNLOAD:
                //                tvSize.setText("等待下载");
                //                btnStart.setText("取消下载");
                break;
            case STATE_DOWNLOADFAILED:
                //                tvType.setText("下载失败");
                tvType.setText(InternationalizationHelper.getString("DOWNLOAD_FAILED"));
                rlProgress.setVisibility(View.GONE);
                //                tvSize.setText("重新下载");
                tvSize.setText(InternationalizationHelper.getString("JX_ReDownload"));
                btnStart.setVisibility(View.VISIBLE);
                break;
        }
    }
}
