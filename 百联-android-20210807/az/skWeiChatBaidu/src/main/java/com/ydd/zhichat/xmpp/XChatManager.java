package com.ydd.zhichat.xmpp;

import android.text.TextUtils;
import android.util.Log;

import com.ydd.zhichat.AppConfig;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.bean.message.NewFriendMessage;
import com.ydd.zhichat.bean.message.XmppMessage;
import com.ydd.zhichat.db.dao.login.MachineDao;
import com.ydd.zhichat.ui.base.CoreManager;
import com.ydd.zhichat.util.DES;
import com.ydd.zhichat.util.Md5Util;
import com.ydd.zhichat.util.ThreadManager;
import com.ydd.zhichat.xmpp.listener.ChatMessageListener;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.HashMap;
import java.util.Map;

/**
 * 功能：
 * 1、接收消息，包括单聊和群聊的接收消息的监听的绑定、具体处理已转移到 xChatlistener 和 xMucChatlistener
 * 2、发送消息，包括发送单聊和群聊的
 */
public class XChatManager {
    private CoreService mService;
    private XMPPConnection mConnection;

    private String mLoginUserId;
    private String mServerName;

    private ChatManager mChatManager;
    private XChatMessageListener mMessageListener;
    private Map<String, Chat> mChatMaps = new HashMap<>();
    // 引入org.jivesoftware.smack.chat.ChatManager对象 原因可至getChatByResource方法下查看
    private org.jivesoftware.smack.chat.ChatManager mMultiLoginChatManager;

    public XChatManager(CoreService coreService, XMPPConnection connection) {
        mService = coreService;
        mConnection = connection;

        mLoginUserId = CoreManager.requireSelf(coreService).getUserId();
        mServerName = CoreManager.requireConfig(MyApplication.getInstance()).XMPPDomain;

        initXChat();
    }

    private void initXChat() {
        mChatManager = ChatManager.getInstanceFor(mConnection);
        mChatManager.setXhmtlImEnabled(true);
        mMessageListener = new XChatMessageListener(mService);
        mChatManager.addIncomingListener(mMessageListener);

        mMultiLoginChatManager = org.jivesoftware.smack.chat.ChatManager.getInstanceFor(mConnection);
    }

    public void reset() { // 切换账号的操作
        mChatMaps.clear();
    }

    /**
     * 发送聊天的消息
     *
     * @param toUserId 要发送给的用户
     * @param oMessage 已经存到本地数据库的一条即将发送的消息
     */
    public void sendMessage(final String toUserId, final ChatMessage oMessage) {
        // 加密可能影响到消息对象复用，所以拷贝一份，
        ChatMessage chatMessage = oMessage.clone(false);
        /*
         * 先将自己定义的消息类型转(ChatMessage)换成 smack第三方定义的Message类型
         * 然后通过smack的Chat对象来发送一个msg
         */
        ThreadManager.getPool().execute(new Runnable() {
            Chat chat = getChat(toUserId);

            public void run() {
                try {
                    if (!mLoginUserId.equals(toUserId)) {
                        if (chatMessage.getIsEncrypt() == 1) {
                            try {
                                // 生成encryptKey
                                String encryptKey = Md5Util.toMD5(AppConfig.apiKey + chatMessage.getTimeSend() + chatMessage.getPacketId());
                                // 通过DES 对content进行加密
                               // Log.e("hm---加密前",chatMessage.getContent());
                                String x = DES.encryptDES(chatMessage.getContent(), encryptKey);
                               // Log.e("hm---加密后",x);
                                chatMessage.setContent(x);
                            } catch (Exception e) {
                                // 加密失败，将该字段置为不加密，以防接收方收到后去解密
                                chatMessage.setIsEncrypt(0);
                              //  Log.e("hm---加密后","加密失败");
                            }
                        }
                    } else {
                        // 给自己的消息不加密，
                        chatMessage.setIsEncrypt(0);
                    }
                    Message msg = new Message();
                    msg.setType(Message.Type.chat);
                    msg.setBody(chatMessage.toJsonString());
                    msg.setPacketID(chatMessage.getPacketId());
                    if (MyApplication.IS_OPEN_RECEIPT) {// 在发送消息之前发送回执请求
                        DeliveryReceiptManager.addDeliveryReceiptRequest(msg);
                    }

                    // 发送消息给其他人(一条resource不拼接的消息)
                    if (!mLoginUserId.equals(toUserId)) {// 发送转发消息 || 检测消息 || 给我的设备发消息，会直接往下走
                        try {
                            Log.e("MultiTest", "发送消息给其他人");
                            chat.send(msg);
                            // 调用消息发送状态监听，将消息发送状态改为发送中...
                            ListenerManager.getInstance().notifyMessageSendStateChange(
                                    mLoginUserId, toUserId, chatMessage.getPacketId(),
                                    ChatMessageListener.MESSAGE_SEND_ING);
                        } catch (InterruptedException e) {
                            // 调用消息发送状态监听，将消息发送状态改为发送中...
                            ListenerManager.getInstance().notifyMessageSendStateChange(
                                    mLoginUserId, toUserId, chatMessage.getPacketId(),
                                    ChatMessageListener.MESSAGE_SEND_FAILED);
                            e.printStackTrace();
                        }
                    }

                    // 给我的设备发消息，不转发，且需要重新获得Chat对象
                    if (!TextUtils.isEmpty(chatMessage.getFromUserId()) && !TextUtils.isEmpty(chatMessage.getToUserId())
                            && chatMessage.getFromUserId().equals(chatMessage.getToUserId())
                            && chatMessage.getType() != XmppMessage.TYPE_SEND_ONLINE_STATUS) {
                        try {
                            if (MyApplication.IsRingId.equals("Empty")) {
                                chat.send(msg);// 理论上不太可能
                            } else {
                                Log.e("MultiTest", toUserId + "--&&--" + MyApplication.IsRingId);
                                org.jivesoftware.smack.chat.Chat deviceChat = getChatByResource(toUserId, MyApplication.IsRingId);
                                deviceChat.sendMessage(msg);
                                Log.e("MultiTest", "消息发送成功");
                            }
                        } catch (InterruptedException e) {
                            ListenerManager.getInstance().notifyMessageSendStateChange(
                                    mLoginUserId, toUserId, chatMessage.getPacketId(),
                                    ChatMessageListener.MESSAGE_SEND_FAILED);
                        }
                        return;
                    }

                    if (MyApplication.IS_SUPPORT_MULTI_LOGIN) {// 发送转发消息 || 检测消息
                        Log.e("MultiTest", "发送转发消息 || 检测消息");
                        sendForwardMessage(msg);
                    }

                } catch (SmackException.NotConnectedException e) {
                    // 发送异常，调用消息发送状态监听，将消息发送状态改为发送失败
                    e.printStackTrace();
                    /*ListenerManager.getInstance().notifyMessageSendStateChange(
                            mLoginUserId, toUserId, chatMessage.getPacketId(),
                            ChatMessageListener.MESSAGE_SEND_FAILED);*/
                }
            }
        });
    }

    /**
     * 发送新朋友消息
     */
    public void sendMessage(final String toUserId, final NewFriendMessage newFriendMessage) {
        ThreadManager.getPool().execute(new Runnable() {
            public void run() {
                Chat chat = getChat(toUserId);
                Log.e("SendNewFriendMessage：", "toUserId:" + toUserId);
                try {
                    Message msg = new Message();
                    msg.setType(Message.Type.chat);
                    msg.setBody(newFriendMessage.toJsonString());// 新朋友推送消息
                    msg.setPacketID(newFriendMessage.getPacketId());
                    if (MyApplication.IS_OPEN_RECEIPT) {
                        DeliveryReceiptManager.addDeliveryReceiptRequest(msg);
                    }
                    try {
                        chat.send(msg);// 发送消息
                        ListenerManager.getInstance().notifyNewFriendSendStateChange(toUserId, newFriendMessage, ChatMessageListener.MESSAGE_SEND_ING);
                    } catch (InterruptedException e) {
                        ListenerManager.getInstance().notifyNewFriendSendStateChange(toUserId, newFriendMessage, ChatMessageListener.MESSAGE_SEND_FAILED);
                        e.printStackTrace();
                    }

                    // 转发给自己
                    if (MyApplication.IS_SUPPORT_MULTI_LOGIN) {// 多点登录下需要转发
                        sendForwardMessage(msg);
                    }

                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    ListenerManager.getInstance().notifyNewFriendSendStateChange(toUserId, newFriendMessage, ChatMessageListener.MESSAGE_SEND_FAILED);
                }
            }
        });
    }

    private Chat getChat(String toUserId) {
        String to = toUserId + "@" + mServerName;

        Chat chat = mChatMaps.get(toUserId);
        if (chat != null) {
            return chat;
        }
        EntityBareJid mEntityBareJid = null;
        try {
            mEntityBareJid = JidCreate.entityBareFrom(to);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        chat = mChatManager.chatWith(mEntityBareJid);
        return chat;
    }

    /**
     * bug：给“我的设备”发送消息，本地查看日志，消息明明已经发送出去了，但其他端就是没有收到该条消息
     * 原因：Smack 有过一次重大升级  之前发消息的Chat对象全部变为了Chat2对象，但查看源码发现Chat2对象内有一个lockedResource对象，
     * 该对象导致了toJid只能to到与自己登录时设置的Resource一致
     * 解决方法：‘发消息给“我的设备”，转发消息给其他端，通过Chat对象来发送而非Chat2对象
     */
    private org.jivesoftware.smack.chat.Chat getChatByResource(String toUserId, String resource) {
        String s = toUserId + "@" + mServerName + "/" + resource;
        EntityJid entityJid = null;
        try {
            entityJid = JidCreate.entityFrom(s);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        org.jivesoftware.smack.chat.Chat chat = mMultiLoginChatManager.createChat(entityJid);
        return chat;
    }

    // 发送上、下线，检测(type==200)，转发消息
    private void sendForwardMessage(Message msg) {
        if (MyApplication.IS_SEND_MSG_EVERYONE) {
            Log.e("msg", "sendMessageToEvery");
            /*
            第一次发送type==200的消息，因为本地其他端的状态都为离线，
            因此不能调用sendMessageToSome去发消息，直接发一条200的消息出去，
            无条件请求回执
             */
            if (!MyApplication.IS_OPEN_RECEIPT) {// 为true的话上面已经请求过回执了，不在重复请求
                DeliveryReceiptManager.addDeliveryReceiptRequest(msg);
            }
            sendMessageToEvery(msg);
        } else {
            sendMessageToSome(msg);
        }
    }

    private void sendMessageToEvery(Message msg) {
        Chat chat = getChat(mLoginUserId);
        try {
            chat.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        MyApplication.IS_SEND_MSG_EVERYONE = false;
    }

    private void sendMessageToSome(Message msg) {
        for (String s : MyApplication.machine) {
            if (MachineDao.getInstance().getMachineOnLineStatus(s)) {
                Log.e("msg", "转发给" + s + "设备");
                org.jivesoftware.smack.chat.Chat chat = getChatByResource(mLoginUserId, s);
                try {
                    Message message = new Message();// 需要重新创建一个Msg，如果引用之前的Msg对象，当第一个Msg或前面的Msg还未发送出去时，可能会出问题
                    message.setType(Message.Type.chat);
                    message.setBody(msg.getBody());
                    message.setPacketID(msg.getPacketID());
                    chat.sendMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
