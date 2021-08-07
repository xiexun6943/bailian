package com.ydd.zhichat.xmpp;

import android.util.Log;

import com.ydd.zhichat.AppConfig;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.bean.EventXMPPJoinGroupFailed;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.db.dao.ChatMessageDao;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.ui.base.CoreManager;
import com.ydd.zhichat.util.AsyncUtils;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.DES;
import com.ydd.zhichat.util.Md5Util;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.util.ThreadManager;
import com.ydd.zhichat.util.TimeUtils;
import com.ydd.zhichat.util.log.LogUtils;
import com.ydd.zhichat.xmpp.listener.ChatMessageListener;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.muc.MucEnterConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.greenrobot.event.EventBus;

public class XMucChatManager {
    private CoreService mService;
    private XMPPTCPConnection mConnection;

    private String mLoginUserId;
    private long mJoinTimeOut;

    private MultiUserChatManager mMultiUserChatManager;
    private XMuChatMessageListener mXMuChatMessageListener;
    private Map<String, MultiUserChat> mMucChatMap;

    public XMucChatManager(CoreService service, XMPPTCPConnection connection) {
        mService = service;
        mConnection = connection;

        mLoginUserId = CoreManager.requireSelf(mService).getUserId();
        mJoinTimeOut = 20000;

        mMucChatMap = new HashMap<String, MultiUserChat>();

        mMultiUserChatManager = MultiUserChatManager.getInstanceFor(connection);
        mXMuChatMessageListener = new XMuChatMessageListener(mService);
        // 自动加入到以前所有已经加入的房间
        joinExistRoom();
    }

    public static String getMucChatServiceName(XMPPConnection connection) {
        return "@muc." + connection.getXMPPServiceDomain();
    }

    public MultiUserChat getRoom(String roomJid) {
        return mMucChatMap.get(roomJid);
    }

    /**
     * @param toUserId    要发送消息的房间Id
     * @param chatMessage 已经存到本地数据库的一条即将发送的消息
     */
    public void sendMessage(final String toUserId, final ChatMessage chatMessage) {
        ThreadManager.getPool().execute(new Runnable() {
            public void run() {
                String roomJid = toUserId + getMucChatServiceName(mConnection);
                MultiUserChat chat = getRoom(roomJid);
                if (chat == null || !chat.isJoined()) {
                    if (chat != null) {
                        Log.e("zq", "是否加入了该群组:" + chat.isJoined());
                    } else {
                        Log.e("zq", "该群组的MultiUserChat对象为空");
                    }
                    EventBus.getDefault().post(new EventXMPPJoinGroupFailed(toUserId));// 通知聊天界面xmpp加入群组失败

                    ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.getPacketId(),
                            ChatMessageListener.MESSAGE_SEND_FAILED);
                    return;
                }

                if (chatMessage.getIsEncrypt() == 1) {
                    try {
                        // 生成encryptKey
                        String encryptKey = Md5Util.toMD5(AppConfig.apiKey + chatMessage.getTimeSend() + chatMessage.getPacketId());
                        String x = DES.encryptDES(chatMessage.getContent(), encryptKey);
                        chatMessage.setContent(x);
                    } catch (Exception e) {
                        // 加密失败，将该字段置为不加密，以防接收方收到后去解密
                        chatMessage.setIsEncrypt(0);
                    }
                }
                Message msg = new Message();
                msg.setType(Message.Type.groupchat);
                msg.setBody(chatMessage.toJsonString());
                msg.setPacketID(chatMessage.getPacketId());
                msg.setTo(roomJid);
                if (MyApplication.IS_OPEN_RECEIPT) {// 添加回执请求
                    DeliveryReceiptManager.addDeliveryReceiptRequest(msg);
                }
                // int sendStatus = ChatMessageListener.MESSAGE_SEND_FAILED;
                int sendStatus = ChatMessageListener.MESSAGE_SEND_ING;
                // 发送消息
                try {
                    chat.sendMessage(msg);
                    // sendStatus = ChatMessageListener.MESSAGE_SEND_ING;
                } catch (NotConnectedException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.getPacketId(),
                        sendStatus);
            }
        });
    }

    public String createMucRoom(String roomName) {
        try {
            /**
             * randomUUID
             */
            String roomId = UUID.randomUUID().toString().replaceAll("-", "");
            String roomJid = roomId + getMucChatServiceName(mConnection);
            // 创建聊天室
            EntityBareJid mEntityBareJid = null;
            try {
                mEntityBareJid = JidCreate.entityBareFrom(roomJid);
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }

            MultiUserChat multiUserChat = mMultiUserChatManager.getMultiUserChat(mEntityBareJid);
            Resourcepart resourcepart = Resourcepart.fromOrThrowUnchecked(mLoginUserId);
            try {
                multiUserChat.create(resourcepart);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            multiUserChat.addMessageListener(mXMuChatMessageListener);// 添加消息监听
            // 获得聊天室的配置表单
            Form form = null;
            try {
                form = multiUserChat.getConfigurationForm();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 根据原始表单创建一个要提交的新表单。
            Form submitForm = form.createAnswerForm();
            // 向要提交的表单添加默认答复
            List<FormField> fields = form.getFields();
            for (int i = 0; i < fields.size(); i++) {
                FormField field = (FormField) fields.get(i);
/*
                if (!FormField.TYPE_HIDDEN.equals(field.getType()) && field.getVariable() != null) {
                    // 设置默认值作为答复
                    submitForm.setDefaultAnswer(field.getVariable());
                }
*/
                submitForm.setDefaultAnswer(field.getVariable());
            }
            // 设置聊天室的新拥有者
            // List owners = new ArrayList();
            // owners.add("liaonaibo2\\40slook.cc");
            // owners.add("liaonaibo1\\40slook.cc");
            // submitForm.setAnswer("muc#roomconfig_roomowners", owners);
            // 设置聊天室的名字
            submitForm.setAnswer("muc#roomconfig_roomname", roomName);
            // 登录房间对话
            submitForm.setAnswer("muc#roomconfig_enablelogging", true);
            // 设置聊天室是持久聊天室，即将要被保存下来
            submitForm.setAnswer("muc#roomconfig_persistentroom", true);
            // 设置聊天室描述
            // if (!TextUtils.isEmpty(roomDesc)) {
            // submitForm.setAnswer("muc#roomconfig_roomdesc", roomDesc);
            // }
            // 允许修改主题
            // submitForm.setAnswer("muc#roomconfig_changesubject", true);
            // 允许占有者邀请其他人
            // submitForm.setAnswer("muc#roomconfig_allowinvites", true);
            // 最大人数
            // List<String> maxusers = new ArrayList<String>();
            // maxusers.add("50");
            // submitForm.setAnswer("muc#roomconfig_maxusers", maxusers);
            // 公开的，允许被搜索到
            // submitForm.setAnswer("muc#roomconfig_publicroom", true);
            // 是否主持腾出空间(加了这个默认游客进去不能发言)
            // submitForm.setAnswer("muc#roomconfig_moderatedroom", true);
            // 房间仅对成员开放
            // submitForm.setAnswer("muc#roomconfig_membersonly", true);
            // 不需要密码
            // submitForm.setAnswer("muc#roomconfig_passwordprotectedroom",false);
            // 房间密码
            // submitForm.setAnswer("muc#roomconfig_roomsecret", "111");
            // 允许主持 能够发现真实 JID
            // List<String> whois = new ArrayList<String>();
            // whois.add("anyone");
            // submitForm.setAnswer("muc#roomconfig_whois", whois);

            // 管理员
            // <field var='muc#roomconfig_roomadmins'>
            // <value>wiccarocks@shakespeare.lit</value>
            // <value>hecate@shakespeare.lit</value>
            // </field>

            // 仅允许注册的昵称登录
            // submitForm.setAnswer("x-muc#roomconfig_reservednick", true);
            // 允许使用者修改昵称
            // submitForm.setAnswer("x-muc#roomconfig_canchangenick", false);
            // 允许用户注册房间
            // submitForm.setAnswer("x-muc#roomconfig_registration", false);

            // 发送已完成的表单（有默认值）到服务器来配置聊天室
            try {
                multiUserChat.sendConfigurationForm(submitForm);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // muc.changeSubject(roomSubject);
            // mMucChatMap.put(roomJid, muc);
            mMucChatMap.put(roomJid, multiUserChat);
            return roomId;
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (NoResponseException e) {
            e.printStackTrace();
        } catch (SmackException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void joinMucChat(final String toUserId, long lastSeconds) {
        String roomJid = toUserId + getMucChatServiceName(mConnection);
        if (mMucChatMap.containsKey(roomJid)) {
            MultiUserChat mucChat = mMucChatMap.get(roomJid);
            if (mucChat != null && mucChat.isJoined()) {
                Log.e("zq", "已加入，Return");
                return;
            }
        }
        Log.e("zq", "未加入，去加入");

        // 创建聊天室
        EntityBareJid mEntityBareJid = null;
        try {
            mEntityBareJid = JidCreate.entityBareFrom(roomJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        MultiUserChat mucChat = mMultiUserChatManager.getMultiUserChat(mEntityBareJid);
        mucChat.addMessageListener(mXMuChatMessageListener);
        Resourcepart resourcepart = Resourcepart.fromOrThrowUnchecked(mLoginUserId);

        Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, toUserId);
        if (friend != null) {
            // Log.e("zq", "friend.getGroupStatus:" + friend.getGroupStatus());
            if (friend.getGroupStatus() != 0) {
                // 我已被踢出该群 || 该群已解散 || 该群已被后台锁定，不加入该群
                Log.e("zq", " 我已被踢出该群 || 该群已解散 || 该群已被后台锁定，Return");
                return;
            }
        }

        try {
            // 将mucChat存入Map内
            mMucChatMap.put(roomJid, mucChat);

            // 以前的aSmack.jar加群方式
           /* DiscussionHistory history = new DiscussionHistory();
            if (lastSeconds > 0) {
                history.setSeconds((int) (lastSeconds));// 减去1秒，防止最后一条消息重复（当然有可能导致在这个时间点的其他消息丢失，不过概率极小）
            } else {
                // request no history 不请求历史记录
                history.setSeconds(0);
            }
            boolean isShieldGroupMsg = PreferenceUtils.getBoolean(MyApplication.getContext(),
                    Constants.SHIELD_GROUP_MSG + roomJid + mLoginUserId, false);
            if (isShieldGroupMsg) {// 屏蔽了该群组消息
                history.setSeconds(0);
            }

            try {
                mucChat.join(resourcepart, null, history, mJoinTimeOut);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (MultiUserChatException.NotAMucServiceException e) {
                e.printStackTrace();
            }*/

            boolean isShieldGroupMsg = PreferenceUtils.getBoolean(MyApplication.getContext(),
                    Constants.SHIELD_GROUP_MSG + roomJid + mLoginUserId, false);
            if (isShieldGroupMsg) {// 屏蔽了该群组消息
                lastSeconds = 0;
            }
            // http://download.igniterealtime.org/smack/docs/latest/javadoc/org/jivesoftware/smackx/muc/MucEnterConfiguration.Builder.html
            MucEnterConfiguration.Builder mucChatEnterConfigurationBuilder = mucChat.getEnterConfigurationBuilder(resourcepart);
            mucChatEnterConfigurationBuilder.requestHistorySince((int) lastSeconds);
            MucEnterConfiguration mucEnterConfiguration = mucChatEnterConfigurationBuilder.build();
            mucChat.join(mucEnterConfiguration);
            Log.e("zq", "加入成功");
        } catch (XMPPException e) {// 如果加入前是将对方挤下线的，那么对方可能还在这个房间内，会导致加入房间失败(概率偏小)
            Log.e("zq", "加入失败");
            /**
             * if an error occurs joining the room. In particular, a 401 error can occur if no password was provided and one is required; or a 403 error can occur if the user is banned;
             * or a 404 error can occur if the room does not exist or is locked;
             * or a 407 error can occur if user is not on the member list; or a 409 error can occur if someone is already in the group chat with the same nickname.
             *
             * 如果没有提供密码并且需要一个密码，则会发生401错误。 或者如果用户被禁止，则可能发生403错误;
             * 或者如果房间不存在或被锁定，则可能发生404错误; 或者如果用户不在成员列表中，则可能发生407错误;
             * 或者如果某人已经在使用相同昵称的组聊天中，则可能会发生409错误。
             *
             * e.getMessage() : feature-not-implemented Changing nickname is not supported yet.// 功能未实现更改昵称尚不支持。
             */
        } catch (NoResponseException e) {
            e.printStackTrace();
        } catch (NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MultiUserChatException.NotAMucServiceException e) {
            e.printStackTrace();
        }
    }

    public void exitMucChat(String toUserId) {
        String roomJid = toUserId + getMucChatServiceName(mConnection);
        if (mMucChatMap.containsKey(roomJid)) {
            MultiUserChat mucChat = mMucChatMap.get(roomJid);
            if (mucChat != null && mucChat.isJoined()) {
                try {
                    try {
                        mucChat.leave();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (NotConnectedException e) {
                    e.printStackTrace();
                }
                mMucChatMap.remove(roomJid);
            }
        }
    }

    public void reset() {
        String userId = CoreManager.requireSelf(mService).getUserId();
        mMucChatMap.clear();
        if (!mLoginUserId.equals(userId)) {
            mLoginUserId = userId;
        }
        joinExistRoom();
    }

    // 自动加入到以前所有已经加入的房间
    private void joinExistRoom() {
/*
        new Thread(new Runnable() {
            @Override
            public void run() {
                long offlineTime = PreferenceUtils.getLong(mService, Constants.OFFLINE_TIME, 0);
                Log.e("zq", offlineTime + "");
                int lastSeconds = (int) (TimeUtils.sk_time_current_time() - offlineTime);
                if (offlineTime == 0) {
                    lastSeconds = 0;
                }
                List<Friend> friends = FriendDao.getInstance().getAllRooms(mLoginUserId);
                if (friends != null && friends.size() > 0) {
                    for (int i = 0; i < friends.size(); i++) {
                        String roomJid = friends.get(i).getUserId();
                        joinMucChat(roomJid, lastSeconds);
                    }
                }
            }
        }).start();
*/
    }

    // 现在加入了群组分页漫游，群组的离线消息不能立即获取，必须要等到'tigase/getLastChatList'接口调用完毕后在加入群组，获取离线消息记录
    public void joinExistGroup() {
        // 先获取全局的离线-->上线 这个时间段的时间
        int lastSeconds;
        long offlineTime = PreferenceUtils.getLong(MyApplication.getContext(), Constants.OFFLINE_TIME + mLoginUserId, 0);
        if (offlineTime == 0) {
            lastSeconds = 0;
        } else {
            lastSeconds = (int) (TimeUtils.sk_time_current_time() - offlineTime);
            LogUtils.e("debug_suddenly_get_msg", TimeUtils.sk_time_current_time() + "，" + offlineTime + "，" + lastSeconds + "，");
        }

        ExecutorService executorService = Executors.newFixedThreadPool(2 * Runtime.getRuntime().availableProcessors());

        List<Friend> friends = FriendDao.getInstance().getAllRooms(mLoginUserId);// 获取本地所有群组
        if (friends != null && friends.size() > 0) {
            for (int i = 0; i < friends.size(); i++) {
                Friend friend = friends.get(i);
                AsyncUtils.doAsync(this, e -> {
                    Reporter.post("加入群组出异常，", e);
                }, executorService, c -> {
                    ChatMessage mLastChatMessage = ChatMessageDao.getInstance().getLastChatMessage(mLoginUserId, friend.getUserId());
                    if (mLastChatMessage != null) {// 如果该群组的最后一条消息不为空，将该条消息的timeSend作为当前群组的离线时间，这样比上面全局的离线时间更加准确
                        int lastMessageTimeSend = (int) (TimeUtils.sk_time_current_time() - mLastChatMessage.getTimeSend());
                        joinMucChat(friend.getUserId(), lastMessageTimeSend + 30);
                    } else {// 该群组本地无消息记录，取全局的离线时间
                        joinMucChat(friend.getUserId(), lastSeconds);
                    }
                });
            }
        }
    }
}