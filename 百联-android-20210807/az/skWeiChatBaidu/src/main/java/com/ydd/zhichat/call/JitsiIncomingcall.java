package com.ydd.zhichat.call;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import  com.ydd.zhichat.R;
import  com.ydd.zhichat.Reporter;
import  com.ydd.zhichat.bean.message.ChatMessage;
import  com.ydd.zhichat.bean.message.XmppMessage;
import  com.ydd.zhichat.broadcast.MsgBroadcast;
import  com.ydd.zhichat.db.InternationalizationHelper;
import  com.ydd.zhichat.db.dao.ChatMessageDao;
import  com.ydd.zhichat.db.dao.FriendDao;
import  com.ydd.zhichat.helper.AvatarHelper;
import  com.ydd.zhichat.ui.base.BaseActivity;
import  com.ydd.zhichat.util.TimeUtils;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

import static  com.ydd.zhichat.R.id.call_answer;
import static  com.ydd.zhichat.R.id.call_avatar;
import static  com.ydd.zhichat.R.id.call_hang_up;
import static  com.ydd.zhichat.R.id.call_invite_type;
import static  com.ydd.zhichat.R.id.call_name;

/**
 * 来电显示
 */
public class JitsiIncomingcall extends BaseActivity implements View.OnClickListener {
    Timer timer = new Timer();
    private String mLoginUserId;
    private String mLoginUserName;
    private int mCallType;
    private String call_fromUser;
    private String call_toUser;
    private String call_Name;
    private String meetUrl;

    private AssetFileDescriptor mAssetFileDescriptor;
    private MediaPlayer mediaPlayer;
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    abort();
                    if (mCallType == 1 || mCallType == 2) {//  来电界面显示 三十秒内 不主动响应  发送挂断消息给对方
                        sendHangUpMessage();
                    }
                    JitsistateMachine.isIncall = false;
                    finish();
                }
            });
        }
    };
    private ImageView mInviteAvatar;
    private TextView mInviteName;
    private TextView mInviteInfo;
    private ImageButton mAnswer; // 接听
    private ImageButton mHangUp; // 挂断
    private boolean isAllowBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);// 锁屏也可显示 | Activity启动时点亮屏幕
        setContentView(R.layout.view_call_trying);
        JitsistateMachine.isIncall = true;
        initData();
        initView();
        timer.schedule(timerTask, 30000, 30000);
        EventBus.getDefault().register(this);
    }

    private void initData() {
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginUserName = coreManager.getSelf().getNickName();

        mCallType = getIntent().getIntExtra(CallConstants.AUDIO_OR_VIDEO_OR_MEET, 0);
        call_fromUser = getIntent().getStringExtra("fromuserid");
        call_toUser = getIntent().getStringExtra("touserid");
        call_Name = getIntent().getStringExtra("name");
        meetUrl = getIntent().getStringExtra("meetUrl");

        bell();
    }

    private void initView() {
        mInviteAvatar = (ImageView) findViewById(call_avatar);
        mInviteName = (TextView) findViewById(call_name);
        mInviteInfo = (TextView) findViewById(call_invite_type);
        mAnswer = (ImageButton) findViewById(call_answer);
        mHangUp = (ImageButton) findViewById(call_hang_up);
        AvatarHelper.getInstance().displayAvatar(call_toUser, mInviteAvatar, true);
        mInviteName.setText(call_Name);
        if (mCallType == 1) {
            mInviteInfo.setText(getString(R.string.suffix_invite_you_voice));
        } else if (mCallType == 2) {
            mInviteInfo.setText(getString(R.string.suffix_invite_you_video));
        } else if (mCallType == 3) {
            mInviteInfo.setText(getString(R.string.tip_invite_voice_meeting));
        } else if (mCallType == 4) {
            mInviteInfo.setText(getString(R.string.tip_invite_video_meeting));
        }
        mAnswer.setOnClickListener(this);
        mHangUp.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.call_answer:// 接听
                abort();
                if (coreManager.isLogin()) {
                    if (mCallType == 1 || mCallType == 2) {// 通话 接听 告诉 对方，会议不需要
                        sendAnswerMessage();
                    }
                    finish();
                }
                break;
            case R.id.call_hang_up:// 拒绝
                abort();
                if (coreManager.isLogin()) {
                    if (mCallType == 1 || mCallType == 2) {// 通话 拒绝 告诉 对方，会议不需要
                        sendHangUpMessage();
                    }
                }
                JitsistateMachine.isIncall = false;
                finish();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageHangUpPhone message) {// 对方取消了 || 其他端 (接听 || 取消)了
        if (message.chatMessage.getFromUserId().equals(call_toUser)
                || message.chatMessage.getFromUserId().equals(mLoginUserId)) {
            abort();
            JitsistateMachine.isIncall = false;
            finish();
            /*if (isSipback) {// 当app处于关闭状态收到来电，通话结束后终止程序
                ActivityStack.getInstance().exit();
                android.os.Process.killProcess(android.os.Process.myPid());
            }*/
        }
    }

    private void sendAnswerMessage() {
        ChatMessage message = new ChatMessage();
        if (mCallType == 1) {
            message.setType(XmppMessage.TYPE_CONNECT_VOICE);
        } else if (mCallType == 2) {
            message.setType(XmppMessage.TYPE_CONNECT_VIDEO);
        }
        message.setContent("");
        message.setFromUserId(mLoginUserId);
        message.setToUserId(call_toUser);
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setFromUserName(mLoginUserName);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        coreManager.sendChatMessage(call_toUser, message);
    }

    private void sendHangUpMessage() {
        ChatMessage message = new ChatMessage();
        if (mCallType == 1) {
            message.setType(XmppMessage.TYPE_NO_CONNECT_VOICE);
        } else if (mCallType == 2) {
            message.setType(XmppMessage.TYPE_NO_CONNECT_VIDEO);
        }
        message.setMySend(true);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginUserName);
        message.setToUserId(call_toUser);
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setTimeSend(TimeUtils.sk_time_current_time());
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, call_toUser, message)) {
            // 更新聊天界面
            MsgBroadcast.broadcastMsgChatUpdate(this, message.getPacketId());
            // 更新朋友表
            if (mCallType == 1) {
                FriendDao.getInstance().updateFriendContent(mLoginUserId, call_toUser,
                        InternationalizationHelper.getString("JXSip_Canceled") + " " + InternationalizationHelper.getString("JX_VoiceChat"),
                        XmppMessage.TYPE_NO_CONNECT_VOICE, TimeUtils.sk_time_current_time());
            } else if (mCallType == 2) {
                FriendDao.getInstance().updateFriendContent(mLoginUserId, call_toUser,
                        InternationalizationHelper.getString("JXSip_Canceled") + " " + InternationalizationHelper.getString("JX_VideoChat"),
                        XmppMessage.TYPE_NO_CONNECT_VIDEO, TimeUtils.sk_time_current_time());
            }
        }

        coreManager.sendChatMessage(call_toUser, message);
        MsgBroadcast.broadcastMsgUiUpdate(this);  // 更新消息界面
    }

    private void bell() {
        try {
            mAssetFileDescriptor = getAssets().openFd("dial.mp3");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(mAssetFileDescriptor.getFileDescriptor(), mAssetFileDescriptor.getStartOffset(), mAssetFileDescriptor.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer arg0) {
                    mediaPlayer.start();
                    mediaPlayer.setLooping(true);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abort() {
        if (timer != null) {
            timer.cancel();
        }
        try {
            mediaPlayer.stop();
        } catch (Exception e) {
            // 在华为手机上疯狂点击挂断按钮会出现崩溃的情况
            Reporter.post("停止铃声出异常，", e);
        }
        mediaPlayer.release();
    }

    @Override
    public void onBackPressed() {
        if (isAllowBack) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mAssetFileDescriptor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        EventBus.getDefault().unregister(this);
    }
}
