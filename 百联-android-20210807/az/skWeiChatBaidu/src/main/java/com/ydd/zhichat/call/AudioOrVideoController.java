package com.ydd.zhichat.call;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.bean.AgoraInfo;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.bean.message.XmppMessage;
import com.ydd.zhichat.broadcast.MsgBroadcast;
import com.ydd.zhichat.db.dao.ChatMessageDao;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.ui.base.CoreManager;
import com.ydd.zhichat.util.TimeUtils;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * Audio Video Call Meet Controller
 */
public class AudioOrVideoController {
    private Context mContext;
    private CoreManager coreManager;

    public AudioOrVideoController(Context context, CoreManager CoreService) {
        this.mContext = context;
        this.coreManager = CoreService;
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    // 我方取消、挂断通话后发送XMPP消息给对方
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventCancelOrHangUp event) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        ChatMessage message = new ChatMessage();
        if (event.type == XmppMessage.TYPE_NO_CONNECT_VOICE) {       // 取消 语音通话
            message.setType(XmppMessage.TYPE_NO_CONNECT_VOICE);
        } else if (event.type == XmppMessage.TYPE_END_CONNECT_VOICE) {// 挂断 语音通话
            message.setType(XmppMessage.TYPE_END_CONNECT_VOICE);
        } else if (event.type == XmppMessage.TYPE_NO_CONNECT_VIDEO) {// 取消 视频通话
            message.setType(XmppMessage.TYPE_NO_CONNECT_VIDEO);
        } else if (event.type == XmppMessage.TYPE_END_CONNECT_VIDEO) {// 挂断 视频通话
            message.setType(XmppMessage.TYPE_END_CONNECT_VIDEO);
        }
        message.setMySend(true);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(coreManager.getSelf().getNickName());
        message.setToUserId(event.toUserId);
        message.setContent(event.content);
        message.setTimeLen(event.callTimeLen);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));

        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, event.toUserId, message)) {
            // 更新朋友表
            FriendDao.getInstance().updateFriendContent(mLoginUserId, event.toUserId, event.content, event.type, TimeUtils.sk_time_current_time());
        }

        coreManager.sendChatMessage(event.toUserId, message);
        MsgBroadcast.broadcastMsgUiUpdate(mContext);   // 更新消息界面
        MsgBroadcast.broadcastMsgChatUpdate(mContext, message.getPacketId());// 更新聊天界面

        /*if (isSipback) {// 当app处于关闭状态收到来电，通话结束后终止程序
            ActivityStack.getInstance().exit();
            android.os.Process.killProcess(android.os.Process.myPid());
        }*/
    }

    // 单聊 通话 来电
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventSipEVent messsage) {
        if (messsage.number == XmppMessage.TYPE_IS_CONNECT_VOICE || messsage.number == XmppMessage.TYPE_IS_CONNECT_VIDEO) {
            if (!JitsistateMachine.isIncall) {
                if (messsage.number == XmppMessage.TYPE_IS_CONNECT_VOICE) {
                    call(CallManager.TYPE_CALL_AUDIO, coreManager.getSelf().getUserId(), messsage.touserid, coreManager.getSelf().getNickName(), messsage.message.getFromUserName(), CallManager.RECEIVE_CALL, messsage.message.getFilePath());
                } else {
                    call(CallManager.TYPE_CALL_VEDIO, coreManager.getSelf().getUserId(), messsage.touserid, coreManager.getSelf().getNickName(), messsage.message.getFromUserName(), CallManager.RECEIVE_CALL, messsage.message.getFilePath());
                }
            }
        } else if (messsage.number == XmppMessage.TYPE_NO_CONNECT_VOICE || messsage.number == XmppMessage.TYPE_NO_CONNECT_VIDEO) {
            Log.e("AVI", "收到对方取消协议");
            if (messsage.message.getTimeLen() == 0) {
                EventBus.getDefault().post(new MessageHangUpPhone(messsage.message));
            }
        }
    }

    // 群聊 会议 邀请
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventMeetingInvited event) {
        if (!JitsistateMachine.isIncall) {
            Intent intent = new Intent(mContext, JitsiIncomingcall.class);
            if (event.type == CallConstants.Audio_Meet) {
                intent.putExtra(CallConstants.AUDIO_OR_VIDEO_OR_MEET, CallConstants.Audio_Meet);
            } else {
                intent.putExtra(CallConstants.AUDIO_OR_VIDEO_OR_MEET, CallConstants.Video_Meet);
            }
            intent.putExtra("touserid", event.message.getFromUserId());
            intent.putExtra("fromuserid", event.message.getObjectId());
            intent.putExtra("name", event.message.getFromUserName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
    }

    public void release() {
        EventBus.getDefault().unregister(this);
    }

    public void call(int type, String formUid, String toUid, String myName, String friendName, int callOrReceive, String channel) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        if (callOrReceive == CallManager.RECEIVE_CALL) {
            params.put("channel", channel);
        }
        HttpUtils.get().url(coreManager.getConfig().CALL)
                .params(params)
                .build()
                .execute(new BaseCallback<AgoraInfo>(AgoraInfo.class) {

                    @Override
                    public void onResponse(ObjectResult<AgoraInfo> result) {
                        if (result.getData() != null) {
                            AgoraInfo agoraInfo = result.getData();
                            String ch = "";
                            if (callOrReceive == CallManager.CALL) {
                                ch = agoraInfo.getChannel();
                            } else {
                                ch = channel;
                            }
                            if (type == CallManager.TYPE_CALL_AUDIO) {
                                callAudio(ch, agoraInfo.getAppId(), agoraInfo.getOwnToken(), formUid, toUid, myName, friendName, callOrReceive);
                            } else {
                                callVideo(ch, agoraInfo.getAppId(), agoraInfo.getOwnToken(), formUid, toUid, myName, friendName, callOrReceive);
                            }
                        } else {
                            Toast.makeText(mContext, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });

    }

    private void callAudio(String channel, String appid, String token, String formUid, String toUid, String myName, String friendName, int callOrReceive) {
        ImVoiceCallActivity.Companion.start(mContext,
                formUid,
                toUid,
                myName,
                friendName,
                channel,
                appid,
                token,
                callOrReceive);
        CallManager.Companion.showCallNotification(MyApplication.getInstance(),
                formUid,
                toUid,
                myName,
                friendName,
                channel,
                appid,
                token,
                callOrReceive,
                "正在呼叫你",
                null,
                false);
    }

    private void callVideo(String channel, String appid, String token, String formUid, String toUid, String myName, String friendName, int callOrReceive) {
        ImVideoCallActivity.Companion.start(mContext,
                formUid,
                toUid,
                myName,
                friendName,
                channel,
                appid,
                token,
                callOrReceive);
        CallManager.Companion.showCallNotification(MyApplication.getInstance(),
                formUid,
                toUid,
                myName,
                friendName,
                channel,
                appid,
                token,
                callOrReceive,
                "正在呼叫你",
                null,
                true);
    }
}
