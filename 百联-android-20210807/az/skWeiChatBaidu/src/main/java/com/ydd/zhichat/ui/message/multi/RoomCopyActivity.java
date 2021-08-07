package com.ydd.zhichat.ui.message.multi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.bean.RoomMember;
import com.ydd.zhichat.bean.RoomMessage;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.bean.message.XmppMessage;
import com.ydd.zhichat.broadcast.MsgBroadcast;
import com.ydd.zhichat.broadcast.MucgroupUpdateUtil;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.db.dao.ChatMessageDao;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.db.dao.RoomMemberDao;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.message.MucChatActivity;
import com.ydd.zhichat.util.TimeUtils;
import com.ydd.zhichat.view.HeadView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.JsonCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class RoomCopyActivity extends BaseActivity {

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if (msg.obj != null) {
                        RoomMessage roomMessage = (RoomMessage) msg.obj;
                        createRoomSuccess(roomMessage.getData().getId(),
                                roomMessage.getData().getJid(),
                                roomMessage.getData().getName(),
                                roomMessage.getData().getDesc());
                        Intent intent = new Intent(RoomCopyActivity.this, MucChatActivity.class);
                        intent.putExtra(AppConstant.EXTRA_USER_ID, roomMessage.getData().getJid());
                        intent.putExtra(AppConstant.EXTRA_NICK_NAME, roomMessage.getData().getNickname());
                        intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
                        startActivity(intent);
                        finish();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_room_copy);
        initView();
    }

    private void initView() {
        String rId = getIntent().getStringExtra("roomId");
        TextView tv_people_num = findViewById(R.id.tv_people_num);
        TextView tv_people = (TextView) findViewById(R.id.tv_people);
        List<RoomMember> roomMembers = RoomMemberDao.getInstance().getRoomMember(rId);
        int num = roomMembers.size();
        if (num > 3) {
            StringBuffer stringBufferD = null;
            for (int i = 0; i < 3; i++) {
                stringBufferD = new StringBuffer();
                stringBufferD.append(roomMembers.get(i).getCardName() + ",");
            }
            tv_people.setText(stringBufferD + coreManager.getSelf().getNickName());

        } else {
            StringBuffer stringBuffer = null;

            for (int i = 0; i < num; i++) {
                stringBuffer = new StringBuffer();
                stringBuffer.append(roomMembers.get(i).getCardName() + ",");
            }
            tv_people.setText(stringBuffer + coreManager.getSelf().getNickName());

        }
        tv_people_num.setText(num + "人");

        HeadView headView = findViewById(R.id.avatar_imgS);
        Friend friend = FriendDao.getInstance().getMucFriendByRoomId(coreManager.getSelf().getUserId(), rId);
        AvatarHelper.getInstance().displayAvatar(coreManager.getSelf().getUserId(), friend, headView);


        Button bt_copy_room = findViewById(R.id.bt_copy_room);
//        bt_copy_room.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        bt_copy_room.setText(getResources().getString(R.string.copy_sure));
        bt_copy_room.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        copyRoom(rId);


                    }
                });
            }
        });
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // 创建成功的时候将会调用此方法，将房间也存为好友
    private void createRoomSuccess(String roomId,
                                   String roomJid,
                                   String roomName,
                                   String roomDesc) {
        Friend friend = new Friend();
        friend.setOwnerId(coreManager.getSelf().getUserId());
        friend.setUserId(roomJid);
        friend.setNickName(roomName);
        friend.setDescription(roomDesc);
        friend.setRoomFlag(1);
        friend.setRoomId(roomId);
        friend.setRoomCreateUserId(coreManager.getSelf().getUserId());
        // timeSend作为取群聊离线消息的标志，所以要在这里设置一个初始值
        friend.setTimeSend(TimeUtils.sk_time_current_time());
        friend.setStatus(Friend.STATUS_FRIEND);
        FriendDao.getInstance().createOrUpdateFriend(friend);

        // 更新群组
        MucgroupUpdateUtil.broadcastUpdateUi(this);

        // 本地发送一条消息至该群 否则未邀请其他人时在消息列表不会显示
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_TIP);
        chatMessage.setFromUserId(coreManager.getSelf().getUserId());
        chatMessage.setFromUserName(coreManager.getSelf().getNickName());
        chatMessage.setToUserId(roomJid);
        chatMessage.setContent(InternationalizationHelper.getString("NEW_FRIEND_CHAT"));
        chatMessage.setPacketId(coreManager.getSelf().getNickName());
        chatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(coreManager.getSelf().getUserId(), roomJid, chatMessage)) {
            // 更新聊天界面
            MsgBroadcast.broadcastMsgUiUpdate(RoomCopyActivity.this);
        }
    }

    private void copyRoom(String mRoomId) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);

        HttpUtils.get().url(coreManager.getConfig().ROOM_COPY)
                .params(params)
                .build()
                .execute(new JsonCallback() {
                    @Override
                    public void onResponse(String result) {
                        Log.e("zx", "onResponse: " + result);
                        RoomMessage roomMessage = JSON.parseObject(result, RoomMessage.class);
                        if (1 == roomMessage.getResultCode()) {
                            Message message = new Message();
                            message.what = 1;
                            message.obj = roomMessage;
                            handler.sendMessage(message);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }
}
