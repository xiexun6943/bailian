package com.ydd.zhichat.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.ui.MainActivity;
import com.ydd.zhichat.ui.base.CoreManager;

/**
 * Created by Administrator on 2016/7/14.
 * 未读消息更新
 */
public class UpdateUnReadReceiver extends BroadcastReceiver {
    private String action = null;
    private MainActivity main;

    public UpdateUnReadReceiver(MainActivity main) {
        this.main = main;
    }

    public void onReceive(Context context, Intent intent) {
        action = intent.getAction();
        if (action.equals(MsgBroadcast.ACTION_MSG_NUM_UPDATE)) {
            int operation = intent.getIntExtra(MsgBroadcast.EXTRA_NUM_OPERATION, MsgBroadcast.NUM_ADD);
            int count = intent.getIntExtra(MsgBroadcast.EXTRA_NUM_COUNT, 0);
            main.msg_num_update(operation, count);
        } else if (action.equals(MsgBroadcast.ACTION_MSG_NUM_UPDATE_NEW_FRIEND)) {// 刷新 新的朋友 消息数量
            Friend friend = FriendDao.getInstance().getFriend(CoreManager.requireSelf(context).getUserId(), Friend.ID_NEW_FRIEND_MESSAGE);
            main.updateNewFriendMsgNum(friend.getUnReadNum());
        } else if (action.equals(MsgBroadcast.ACTION_MSG_NUM_RESET)) {
            main.msg_num_reset();
        }
    }
}
