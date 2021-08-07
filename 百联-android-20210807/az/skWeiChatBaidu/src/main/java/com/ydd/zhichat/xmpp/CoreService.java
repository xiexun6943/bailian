package com.ydd.zhichat.xmpp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.ydd.zhichat.AppConfig;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.BuildConfig;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.audio.NoticeVoicePlayer;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.bean.MsgRoamTask;
import com.ydd.zhichat.bean.SyncBean;
import com.ydd.zhichat.bean.User;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.bean.message.LastChatHistoryList;
import com.ydd.zhichat.bean.message.NewFriendMessage;
import com.ydd.zhichat.bean.message.XmppMessage;
import com.ydd.zhichat.broadcast.MsgBroadcast;
import com.ydd.zhichat.db.dao.ChatMessageDao;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.db.dao.MsgRoamTaskDao;
import com.ydd.zhichat.db.dao.login.MachineDao;
import com.ydd.zhichat.db.dao.login.TimerListener;
import com.ydd.zhichat.helper.PrivacySettingHelper;
import com.ydd.zhichat.ui.MainActivity;
import com.ydd.zhichat.ui.base.CoreManager;
import com.ydd.zhichat.ui.message.ChatActivity;
import com.ydd.zhichat.ui.message.HandleSyncMoreLogin;
import com.ydd.zhichat.ui.message.MucChatActivity;
import com.ydd.zhichat.util.AppUtils;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.DES;
import com.ydd.zhichat.util.HttpUtil;
import com.ydd.zhichat.util.Md5Util;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.util.TimeUtils;
import com.ydd.zhichat.xmpp.ReceiptManager.SendType;
import com.ydd.zhichat.xmpp.listener.AuthStateListener;
import com.ydd.zhichat.xmpp.listener.ChatMessageListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;

import org.jivesoftware.smack.XMPPConnection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.leolin.shortcutbadger.ShortcutBadger;
import okhttp3.Call;

public class CoreService extends Service implements TimerListener {
    static final boolean DEBUG = true;
    static final String TAG = "XmppCoreService";

    private static final Intent SERVICE_INTENT = new Intent();
    private static final String EXTRA_LOGIN_USER_ID = "login_user_id";
    private static final String EXTRA_LOGIN_PASSWORD = "login_password";
    private static final String EXTRA_LOGIN_NICK_NAME = "login_nick_name";

    private static final String MESSAGE_CHANNEL_ID = "message";

    static {
        SERVICE_INTENT.setComponent(new ComponentName(BuildConfig.APPLICATION_ID, CoreService.class.getName()));
    }

    /*
    发送 已读 消息
     */
    ReadBroadcastReceiver receiver = new ReadBroadcastReceiver();
    private boolean isInit;
    private CoreServiceBinder mBinder;
    /* 当前登陆用户的基本属性 */
    private String mLoginUserId;
    @SuppressWarnings("unused")
    private String mLoginNickName;
    private String mLoginPassword;
    private XmppConnectionManager mConnectionManager;// 唯一
    private XChatManager mXChatManager;// 唯一
    private XMucChatManager mXMucChatManager;// 唯一
    private ReceiptManager mReceiptManager;// 唯一
    private ReceiptManagerNew mReceiptManagerNew;// 唯一
    private NotifyConnectionListener mNotifyConnectionListener = new NotifyConnectionListener() {
        @Override
        public void notifyConnecting() {
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_ING);
        }

        @Override
        public void notifyConnected(XMPPConnection arg0) {
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_ING);
        }

        @Override
        public void notifyAuthenticated(XMPPConnection arg0) {
            onAuthenticated();
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_SUCCESS);// 通知登陆成功
            authenticatedOperating();
        }

        @Override
        public void notifyConnectionClosedOnError(Exception arg0) {
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_NOT);
        }

        @Override
        public void notifyConnectionClosed() {
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_NOT);
        }
    };
    /**
     * 本地 发送 通知 至 通知栏
     */
    private int notifyId = 1003020303;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    public static Intent getIntent() {
        return SERVICE_INTENT;
    }

    // 要用ContextCompat.startForegroundService启动，否则安卓8.0以上可能崩溃，而且是不一定复现的那种，
    public static Intent getIntent(Context context, String userId, String password, String nickName) {
        Intent intent = new Intent(context, CoreService.class);
        intent.putExtra(EXTRA_LOGIN_USER_ID, userId);
        intent.putExtra(EXTRA_LOGIN_PASSWORD, password);
        intent.putExtra(EXTRA_LOGIN_NICK_NAME, nickName);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new CoreServiceBinder();
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService OnCreate :" + android.os.Process.myPid());
        }
        register(); // 注册发送已读消息的广播监听
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 绑定服务只是为了提供一些外部调用的方法
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService onBind");
        }
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService onDestroy");
        }
        release();

        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService onStartCommand");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationBuilder();
            startForeground(1, mBuilder.build());
            stopForeground(true);
        }

        init();

        // return START_NOT_STICKY;
        return START_STICKY;
    }

    private void init() {
        if (isInit) {
            Log.e("zq", "isInit==true,直接登录");
            login(mLoginUserId, mLoginPassword);
            return;
        }
        isInit = true;
        User self = CoreManager.requireSelf(this);
        mLoginUserId = self.getUserId();
        mLoginPassword = self.getPassword();
        mLoginNickName = self.getNickName();

        if (Constants.IS_CLOSED_ON_ERROR_END_DOCUMENT && mConnectionManager != null) {
            Log.e("zq", "CLOSED_ON_ERROR_END_DOCUMENT--->调用release方法");
            Constants.IS_CLOSED_ON_ERROR_END_DOCUMENT = false;
            release();
        }

        if (mConnectionManager == null) {
            initConnection();
        }
    }

    public void login(String userId, String password) {
        if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(password)) {
            mConnectionManager.login(userId, password);
        }
    }

    public void initConnection() {
        mConnectionManager = new XmppConnectionManager(this, mNotifyConnectionListener);
    }

    private void release() {
        if (mConnectionManager != null) {
            mConnectionManager.release();
            mConnectionManager = null;
        }

        mReceiptManager = null;
        mXChatManager = null;
        mXMucChatManager = null;
    }

    private void onAuthenticated() {
        if (!isAuthenticated()) {
            return;
        }

        /* 消息回执管理 */
        if (mReceiptManager == null) {
            mReceiptManager = new ReceiptManager(this, mConnectionManager.getConnection());
        } else {
            mReceiptManager.reset();
        }
        if (mReceiptManagerNew != null) {
            mReceiptManagerNew.release();
            mReceiptManagerNew = null;
        }
        mReceiptManagerNew = new ReceiptManagerNew(this, mConnectionManager.getConnection());

        // 初始化消息处理
        if (mXChatManager == null) {
            mXChatManager = new XChatManager(this, mConnectionManager.getConnection());
        } else {
            mXChatManager.reset();
        }

        if (mXMucChatManager == null) {
            mXMucChatManager = new XMucChatManager(this, mConnectionManager.getConnection());
        } else {
            mXMucChatManager.reset();
        }

        /*  获取离线消息 */
        mConnectionManager.sendOnLineMessage();
    }

    /**
     * 获得XmppConnectionManager对象
     */
    public XmppConnectionManager getmConnectionManager() {
        return mConnectionManager;
    }

    public boolean isAuthenticated() {
        if (mConnectionManager != null && mConnectionManager.isAuthenticated()) {
            return true;
        }
        return false;
    }

    public void logout() {
        isInit = false;
        if (CoreService.DEBUG)
            Log.e(CoreService.TAG, "Xmpp登出");
        if (mConnectionManager != null) {
            mConnectionManager.logout();
        }
        stopSelf();
    }

    public void logoutWithOutStopSelf() {
        if (CoreService.DEBUG)
            Log.e(CoreService.TAG, "Xmpp登出但不销毁服务");
        if (mConnectionManager != null) {
            mConnectionManager.logout();
        }
    }

    public void sendReceipt(String messageId) {
        if (TextUtils.isEmpty(messageId)) {
            return;
        }
        if (mReceiptManagerNew != null) {
            mReceiptManagerNew.sendReceipt(messageId);
        } else {
            Reporter.post("初始化异常，回执管理器为空");
        }
    }

    /**
     * 发送聊天消息
     */
    public void sendChatMessage(String toUserId, ChatMessage chatMessage) {
        if (mXChatManager == null) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "mXChatManager==null");
        }

        if (mReceiptManager == null) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "mReceiptManager==null");
        }

        if (!isAuthenticated()) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "isAuthenticated==false");
        }

        if (mXChatManager == null || mReceiptManager == null
                || (!isAuthenticated() && !HttpUtil.isGprsOrWifiConnected(MyApplication.getContext()))) {
            // 现在!isAuthenticated()不能直接标记发送失败，还需要判断网络是否连接
            ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.getPacketId(),
                    ChatMessageListener.MESSAGE_SEND_FAILED);// 保存自己发送的消息 先给一个默认值
        } else {
            /**
             * 先添加一个等待接收回执的消息
             * 然后再发送这条消息
             */
            mReceiptManager.addWillSendMessage(toUserId, chatMessage, SendType.NORMAL, chatMessage.getContent());
            mXChatManager.sendMessage(toUserId, chatMessage);
        }
    }

    /**
     * 发送新的朋友消息
     */
    public void sendNewFriendMessage(String toUserId, NewFriendMessage message) {
        if (mXChatManager == null || mReceiptManager == null || !isAuthenticated()) {
            ListenerManager.getInstance().notifyNewFriendSendStateChange(toUserId, message, ChatMessageListener.MESSAGE_SEND_FAILED);
        } else {
            Log.e(CoreService.TAG, "CoreService：" + toUserId);
            mReceiptManager.addWillSendMessage(toUserId, message, SendType.PUSH_NEW_FRIEND, message.getContent());
            mXChatManager.sendMessage(toUserId, message);
        }
    }

    public void sendMucChatMessage(String toUserId, ChatMessage chatMessage) {
        if (mXMucChatManager == null) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "mXMucChatManager==null");
        }

        if (mReceiptManager == null) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "mReceiptManager==null");
        }

        if (!isAuthenticated()) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "isAuthenticated==false");
        }

        if (mXMucChatManager == null || mReceiptManager == null
                || (!isAuthenticated() && !HttpUtil.isGprsOrWifiConnected(MyApplication.getContext()))) {
            // 现在!isAuthenticated()不能直接标记发送失败，还需要判断网络是否连接
            ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.getPacketId(),
                    ChatMessageListener.MESSAGE_SEND_FAILED);
        } else {
            mReceiptManager.addWillSendMessage(toUserId, chatMessage, SendType.NORMAL, chatMessage.getContent());
            mXMucChatManager.sendMessage(toUserId, chatMessage);
        }
    }

    /* 群聊的外部接口 */
    public boolean isMucEnable() {
        return isAuthenticated() && mXMucChatManager != null;
    }

    public void joinExistGroup() {
        if (isAuthenticated()) {
            if (mXMucChatManager == null) {
                mXMucChatManager = new XMucChatManager(this, mConnectionManager.getConnection());
                mXMucChatManager.joinExistGroup();
            } else {
                mXMucChatManager.joinExistGroup();
            }
        }
    }

    /* 创建群聊 */
    public String createMucRoom(String roomName) {
        if (isMucEnable()) {
            return mXMucChatManager.createMucRoom(roomName);
        }
        return null;
    }

    /* 加入群聊 */
    public void joinMucChat(String toUserId, long lastSeconds) {
        if (isMucEnable()) {
            mXMucChatManager.joinMucChat(toUserId, lastSeconds);
        }
    }

    /* 退出群聊 */
    public void exitMucChat(String toUserId) {
        if (isMucEnable()) {
            mXMucChatManager.exitMucChat(toUserId);
        }
    }

    /********************
     *  其他操作
     *********************/
    /*
    XMPP认证后需要做的操作
    */
    public void authenticatedOperating() {
        Log.e("zq", "认证之后需要调用的操作");

        if (MyApplication.IS_SUPPORT_MULTI_LOGIN) {
            Log.e("TAG", "我已上线，发送Type 200 协议");
            loadMachineList();
        }

        new Thread(() -> {
            // 删除本地已过期的消息
            List<Friend> nearlyFriendMsg = FriendDao.getInstance().getNearlyFriendMsg(mLoginUserId);
            for (int i = 0; i < nearlyFriendMsg.size(); i++) {
                if (nearlyFriendMsg.get(i).getRoomFlag() == 0) {// 单聊可删除
                    ChatMessageDao.getInstance().deleteOutTimeChatMessage(mLoginUserId, nearlyFriendMsg.get(i).getUserId());
                } else {// 群聊修改字段
                    ChatMessageDao.getInstance().updateExpiredStatus(mLoginUserId, nearlyFriendMsg.get(i).getUserId());
                }
            }
        }).start();

        // 从服务端获取与其它好友 || 群组内最后一条聊天消息列表(单聊：我在其他端的产生的聊天记录 群聊：离线消息大于100条时，之前的数据)
        getLastChatHistory();
        getInterfaceTransferInOfflineTime();
    }

    public void getInterfaceTransferInOfflineTime() {
        long syncTimeLen = PreferenceUtils.getLong(MyApplication.getContext(), Constants.OFFLINE_TIME + mLoginUserId, 0);

        Map<String, String> params = new HashMap();
        params.put("access_token", CoreManager.requireSelfStatus(this).accessToken);
        params.put("offlineTime", String.valueOf(syncTimeLen));

        HttpUtils.get().url(CoreManager.requireConfig(this).USER_OFFLINE_OPERATION)
                .params(params)
                .build()
                .execute(new ListCallback<SyncBean>(SyncBean.class) {
                    @Override
                    public void onResponse(ArrayResult<SyncBean> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            List<SyncBean> syncBeans = result.getData();
                            for (int i = 0; i < syncBeans.size(); i++) {
                                HandleSyncMoreLogin.distributionService(syncBeans.get(i), CoreService.this);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    public void getLastChatHistory() {
        Map<String, String> params = new HashMap();
        params.put("access_token", CoreManager.requireSelfStatus(this).accessToken);

        long syncTimeLen;
        if (Constants.OFFLINE_TIME_IS_FROM_SERVICE) {// 离线时间为服务端获取 取出消息漫游时长
            Constants.OFFLINE_TIME_IS_FROM_SERVICE = false;
            String chatSyncTimeLen = String.valueOf(PrivacySettingHelper.getPrivacySettings(this).getChatSyncTimeLen());
            Double realSyncTime = Double.parseDouble(chatSyncTimeLen);
            if (realSyncTime == -2) {// 不同步
                joinExistGroup();
                return;
            } else if (realSyncTime == -1 || realSyncTime == 0) {// 同步 永久 syncTime == 0
                syncTimeLen = 0;
            } else {
                syncTimeLen = (long) (realSyncTime * 24 * 60 * 60);// 得到消息同步时长
            }
        } else {// syncTime为上一次本地保存的离线时间
            syncTimeLen = PreferenceUtils.getLong(MyApplication.getContext(), Constants.OFFLINE_TIME + mLoginUserId, 0);
        }
        params.put("startTime", String.valueOf(syncTimeLen));

        HttpUtils.get().url(CoreManager.requireConfig(this).GET_LAST_CHAT_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<LastChatHistoryList>(LastChatHistoryList.class) {
                    @Override
                    public void onResponse(ArrayResult<LastChatHistoryList> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            final List<LastChatHistoryList> data = result.getData();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = 0; i < data.size(); i++) {
                                        LastChatHistoryList mLastChatHistoryList = data.get(i);

                                        if (mLastChatHistoryList.getIsRoom() == 1) {// 群组消息
                                            // 取出该群组最后一条消息
                                            ChatMessage mLocalLastMessage = ChatMessageDao.getInstance().getLastChatMessage(mLoginUserId, mLastChatHistoryList.getJid());
                                            if (mLocalLastMessage == null
                                                    || mLocalLastMessage.getPacketId().equals(mLastChatHistoryList.getMessageId())) {
                                                // 最后一条消息为空(代表本地为空)
                                                // || 最后一条消息的msgId==服务端记录的该群组最后一条消息msgId(代表离线期间无消息产生 理论上服务端这个情况服务端是不会返回的) 不需要生成任务
                                            } else {
                                                // 生成一条群组漫游任务，存入任务表
                                                MsgRoamTask mMsgRoamTask = new MsgRoamTask();
                                                mMsgRoamTask.setTaskId(System.currentTimeMillis());
                                                mMsgRoamTask.setOwnerId(mLoginUserId);
                                                mMsgRoamTask.setUserId(mLastChatHistoryList.getJid());
                                                mMsgRoamTask.setStartTime(mLocalLastMessage.getTimeSend());
                                                mMsgRoamTask.setStartMsgId(mLocalLastMessage.getPacketId());
                                                MsgRoamTaskDao.getInstance().createMsgRoamTask(mMsgRoamTask);
                                            }
                                        }
                                        // 更新朋友表部分字段，用于显示
                                        String str = "";
                                        if (mLastChatHistoryList.getIsEncrypt() == 1) {// 需要解密
                                            if (!TextUtils.isEmpty(mLastChatHistoryList.getContent())) {
                                                String content = mLastChatHistoryList.getContent().replaceAll("\n", "");
                                                String decryptKey = Md5Util.toMD5(AppConfig.apiKey + mLastChatHistoryList.getTimeSend() + mLastChatHistoryList.getMessageId());
                                                try {
                                                    str = DES.decryptDES(content, decryptKey);
                                                } catch (Exception e) {
                                                    str = mLastChatHistoryList.getContent();
                                                    e.printStackTrace();
                                                }
                                            }
                                        } else {
                                            str = mLastChatHistoryList.getContent();
                                        }

                                        FriendDao.getInstance().updateApartDownloadTime(mLastChatHistoryList.getUserId(), mLastChatHistoryList.getJid(),
                                                str, mLastChatHistoryList.getType(), mLastChatHistoryList.getTimeSend(),
                                                mLastChatHistoryList.getIsRoom(), mLastChatHistoryList.getFrom(), mLastChatHistoryList.getFromUserName(),
                                                mLastChatHistoryList.getToUserName());
                                    }
                                    MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getContext());
                                    // 以上任务生成之后，在通知XMPP加入群组 获取群组离线消息
                                    joinExistGroup();
                                }
                            }).start();
                        } else {// 数据异常，也需要调用XMPP加入群组
                            joinExistGroup();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        // 同上
                        joinExistGroup();
                    }
                });
    }

    /*
    发送本地通知
     */
    // 这个方法有被异步线程调用，
    @WorkerThread
    public void notificationMessage(ChatMessage chatMessage, boolean isGroupChat) {
        boolean isAppForeground = AppUtils.isAppForeground(this);
        Log.e(TAG, "notificationMessage() called with: chatMessage = [" + chatMessage.getContent() + "], isGroupChat = [" + isGroupChat + "], isAppForeground = [" + isAppForeground + "]");

        if (isAppForeground) {// 在前台 不通知
            return;
        }

        int messageType = chatMessage.getType();
        String title;
        String content;
        boolean isSpecialMsg = false;// 特殊消息 跳转至主界面 而非聊天界面

        switch (messageType) {
            case XmppMessage.TYPE_REPLAY:
            case XmppMessage.TYPE_TEXT:
                if (chatMessage.getIsReadDel()) {
                    content = getString(R.string.tip_click_to_read);
                } else {
                    content = chatMessage.getContent();
                }
                break;
            case XmppMessage.TYPE_VOICE:
                content = getString(R.string.msg_voice);
                break;
            case XmppMessage.TYPE_GIF:
                content = getString(R.string.msg_animation);
                break;
            case XmppMessage.TYPE_IMAGE:
                content = getString(R.string.msg_picture);
                break;
            case XmppMessage.TYPE_VIDEO:
                content = getString(R.string.msg_video);
                break;
            case XmppMessage.TYPE_RED:
                content = getString(R.string.msg_red_packet);
                break;
            case XmppMessage.TYPE_LOCATION:
                content = getString(R.string.msg_location);
                break;
            case XmppMessage.TYPE_CARD:
                content = getString(R.string.msg_card);
                break;
            case XmppMessage.TYPE_FILE:
                content = getString(R.string.msg_file);
                break;
            case XmppMessage.TYPE_TIP:
                content = getString(R.string.msg_system);
                break;
            case XmppMessage.TYPE_IMAGE_TEXT:
            case XmppMessage.TYPE_IMAGE_TEXT_MANY:
                content = getString(R.string.msg_image_text);
                break;
            case XmppMessage.TYPE_LINK:
            case XmppMessage.TYPE_SHARE_LINK:
                content = getString(R.string.msg_link);
                break;
            case XmppMessage.TYPE_SHAKE:
                content = getString(R.string.msg_shake);
                break;
            case XmppMessage.TYPE_CHAT_HISTORY:
                content = getString(R.string.msg_chat_history);
                break;
            case XmppMessage.TYPE_TRANSFER:
                content = getString(R.string.tip_transfer_money);
                break;
            case XmppMessage.TYPE_TRANSFER_RECEIVE:
                content = getString(R.string.tip_transfer_money) + getString(R.string.transfer_friend_sure_save);
                break;
            case XmppMessage.TYPE_TRANSFER_BACK:
                content = getString(R.string.transfer_back);
                break;
            case XmppMessage.TYPE_PAY_CERTIFICATE:
                content = getString(R.string.pay_certificate);
                break;

            case XmppMessage.TYPE_IS_CONNECT_VOICE:
                content = getString(R.string.suffix_invite_you_voice);
                break;
            case XmppMessage.TYPE_IS_CONNECT_VIDEO:
                content = getString(R.string.suffix_invite_you_video);
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_VOICE:
                content = getString(R.string.suffix_invite_you_voice_meeting);
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_VIDEO:
                content = getString(R.string.suffix_invite_you_video_meeting);
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_TALK:
                content = getString(R.string.suffix_invite_you_talk);
                break;

            case XmppMessage.TYPE_SAYHELLO:// 打招呼
                isSpecialMsg = true;
                content = getString(R.string.apply_to_add_me_as_a_friend);
                break;
            case XmppMessage.TYPE_PASS:    // 同意加好友
                isSpecialMsg = true;
                content = getString(R.string.agree_with_my_plus_friend_request);
                break;
            case XmppMessage.TYPE_FRIEND:  // 直接成为好友
                isSpecialMsg = true;
                content = getString(R.string.added_me_as_a_friend);
                break;

            case XmppMessage.DIANZAN:// 朋友圈点赞
                isSpecialMsg = true;
                content = getString(R.string.notification_praise_me_life_circle);
                break;
            case XmppMessage.PINGLUN:    // 朋友圈评论
                isSpecialMsg = true;
                content = getString(R.string.notification_comment_me_life_circle);
                break;
            case XmppMessage.ATMESEE:  // 朋友圈提醒我看
                isSpecialMsg = true;
                content = getString(R.string.notification_at_me_life_circle);
                break;

            default:// 其他消息类型不通知
                return;
        }

        createNotificationBuilder();

        String id;
        PendingIntent pendingIntent;
        if (isSpecialMsg) {
            title = chatMessage.getFromUserName();
            content = chatMessage.getFromUserName() + content;
            pendingIntent = pendingIntentForSpecial();
        } else {
            if (isGroupChat) {
                id = chatMessage.getToUserId();
                content = chatMessage.getFromUserName() + "：" + content;// 群组消息通知需要带上消息发送方的名字
            } else {
                id = chatMessage.getFromUserId();
            }

            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, id);
            if (friend != null) {
                title = TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName();
            } else {
                title = chatMessage.getFromUserName();
            }

            if (isGroupChat) {
                pendingIntent = pendingIntentForMuc(friend);
            } else {
                pendingIntent = pendingIntentForSingle(friend);
            }

        }
        if (pendingIntent == null)
            return;

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setContentTitle(title) // 通知标题
                .setContentText(content)  // 通知内容
                .setTicker(getString(R.string.tip_new_message))
                .setWhen(System.currentTimeMillis()) // 通知时间
                .setPriority(Notification.PRIORITY_HIGH) // 通知优先级
                .setAutoCancel(true)// 当用户单击面板就可以让通知自动取消
                .setOngoing(false)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setSmallIcon(R.mipmap.icon); // 通知icon
        Notification n = mBuilder.build();
        int numMessage = FriendDao.getInstance().getMsgUnReadNumTotal(mLoginUserId);
        // 先通知后保存的数据库，所以数据库里读出来的未读消息数要加1，
        ShortcutBadger.applyNotification(getApplicationContext(), n, numMessage + 1);
        mNotificationManager.notify(chatMessage.getFromUserId(), notifyId, n);
        if (isSpecialMsg) {// 特殊消息响铃通知
            NoticeVoicePlayer.getInstance().start();
        }
    }

    private void createNotificationBuilder() {
        // 同步锁防止线程冲突，大量消息通知时可能需要，
        if (mNotificationManager == null) {
            synchronized (this) {
                if (mNotificationManager == null) {
                    mNotificationManager = (NotificationManager) getApplicationContext()
                            .getSystemService(NOTIFICATION_SERVICE);
                }
            }
        }
        if (mBuilder == null) {
            synchronized (this) {
                if (mBuilder == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(
                                MESSAGE_CHANNEL_ID,
                                getString(R.string.message_channel_name),
                                NotificationManager.IMPORTANCE_DEFAULT);
                        // 关闭通知铃声，我们有自己播放，
                        channel.setSound(null, null);
                        mNotificationManager.createNotificationChannel(channel);
                        mBuilder = new NotificationCompat.Builder(this, channel.getId());
                    } else {
                        //noinspection deprecation
                        mBuilder = new NotificationCompat.Builder(this);
                    }
                }
            }
        }
    }

    /**
     * <跳到单人聊天界面>
     */
    public PendingIntent pendingIntentForSingle(Friend friend) {
        Intent intent;
        if (friend != null) {
            intent = new Intent(getApplicationContext(), ChatActivity.class);
            intent.putExtra(AppConstant.EXTRA_FRIEND, friend);
        } else {
            intent = new Intent(getApplicationContext(), MainActivity.class);
        }
        intent.putExtra(Constants.IS_NOTIFICATION_BAR_COMING, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    /**
     * <跳到群组聊天界面>
     */
    public PendingIntent pendingIntentForMuc(Friend friend) {
        Intent intent;
        if (friend != null) {
            intent = new Intent(getApplicationContext(), MucChatActivity.class);
            intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
            intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
        } else {
            intent = new Intent(getApplicationContext(), MainActivity.class);
        }
        intent.putExtra(Constants.IS_NOTIFICATION_BAR_COMING, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    /**
     * <跳到主界面>
     */
    public PendingIntent pendingIntentForSpecial() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public void register() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(com.ydd.zhichat.broadcast.OtherBroadcast.Read);
        registerReceiver(receiver, intentFilter);
    }

    /*
    多点登录
     */
    // 加载设备表
    public void loadMachineList() {
        MachineDao.getInstance().loadMachineList(this);

        MyApplication.IS_SEND_MSG_EVERYONE = true;
        sendOnLineMessage();
    }

    @Override
    public void onFinish(String machineName) {
        Log.e(TAG, machineName + "计时完成，开始检测" + machineName + "的在线状态 ");
        if (MachineDao.getInstance().getMachineSendReceiptStatus(machineName)) {
            sendOnLineMessage();
            // 发送检测消息后，将是否发送回执的状态更新为false
            MachineDao.getInstance().updateMachineSendReceiptStatus(machineName, false);
        } else {// 当前machine对于我上次发给他的转发 || 检测消息，并未给回执给我，所以我们判断他离线了，将他的状态置为false
            Log.e(TAG, "发送回执的状态为false，判断" + machineName + "为离线 ");
            MachineDao.getInstance().updateMachineOnLineStatus(machineName, false);
        }
    }

    // 发送上线、检测消息
    public void sendOnLineMessage() {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_SEND_ONLINE_STATUS);

        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(mLoginNickName);
        chatMessage.setToUserId(mLoginUserId);
        chatMessage.setContent("1");// 0 离线 1 在线

        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        sendChatMessage(mLoginUserId, chatMessage);
    }

    // 发送下线消息
    public void sendOffLineMessage() {
        MyApplication.IS_SEND_MSG_EVERYONE = true;
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_SEND_ONLINE_STATUS);

        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(mLoginNickName);
        chatMessage.setToUserId(mLoginUserId);
        chatMessage.setContent("0");// 0 离线 1在线

        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        sendChatMessage(mLoginUserId, chatMessage);
    }

    // 发送忙线消息
    public void sendBusyMessage(String toUserId) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_IS_BUSY);

        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(mLoginNickName);
        chatMessage.setToUserId(toUserId);

        chatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        sendChatMessage(toUserId, chatMessage);
    }

    // Binder
    public class CoreServiceBinder extends Binder {
        public CoreService getService() {
            return CoreService.this;
        }
    }

    /*
    发送已读消息
     */
    public class ReadBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(com.ydd.zhichat.broadcast.OtherBroadcast.Read)) {
                Bundle bundle = intent.getExtras();
                String packetId = bundle.getString("packetId");
                boolean isGroup = bundle.getBoolean("isGroup");
                String friendId = bundle.getString("friendId");
                String friendName = bundle.getString("fromUserName");

                ChatMessage msg = new ChatMessage();
                msg.setType(XmppMessage.TYPE_READ);
                msg.setFromUserId(mLoginUserId);
                msg.setFromUserName(friendName);
                msg.setToUserId(friendId);
                msg.setContent(packetId);
                // 发送已读消息 本地置为已读
                msg.setSendRead(true);
                msg.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                msg.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                if (isGroup) {
                    sendMucChatMessage(friendId, msg);
                } else {
                    sendChatMessage(friendId, msg);
                }
            }
        }
    }
}
