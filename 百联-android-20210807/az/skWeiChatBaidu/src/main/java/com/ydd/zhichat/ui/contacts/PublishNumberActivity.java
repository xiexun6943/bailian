package com.ydd.zhichat.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.makeramen.roundedimageview.RoundedImageView;
import com.roamer.slidelistview.SlideBaseAdapter;
import com.roamer.slidelistview.SlideListView;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.bean.message.NewFriendMessage;
import com.ydd.zhichat.bean.message.XmppMessage;
import com.ydd.zhichat.broadcast.CardcastUiUpdateUtil;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.db.dao.NewFriendDao;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.helper.FriendHelper;
import com.ydd.zhichat.pay.sk.SKPayActivity;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.message.ChatActivity;
import com.ydd.zhichat.ui.nearby.PublicNumberSearchActivity;
import com.ydd.zhichat.util.TimeUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.util.ViewHolder;
import com.ydd.zhichat.view.HeadView;
import com.ydd.zhichat.view.SelectionFrame;
import com.ydd.zhichat.xmpp.ListenerManager;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class PublishNumberActivity extends BaseActivity {

    private SlideListView mNoticeAccountList;
    private NoticeAdapter mNoticeAdapter;
    private List<Friend> mNoticeFriendList;
    private long mOldTime;
    private long mDelayMilliseconds = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);
        initActionBar();
        initView();
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
        tvTitle.setText(R.string.public_number);
        ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.search_near);
        ivRight.setOnClickListener(v -> {
            PublicNumberSearchActivity.start(this);
        });
    }

    private void initView() {

        mNoticeFriendList = FriendDao.getInstance().getAllSystems(coreManager.getSelf().getUserId());
        if (mNoticeFriendList == null) {
            mNoticeFriendList = new ArrayList<>();
        }
        mNoticeAccountList = findViewById(R.id.notice_account_lv);
        mNoticeAdapter = new NoticeAdapter(this);
        mNoticeAccountList.setAdapter(mNoticeAdapter);

        mNoticeAccountList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                long nowTime = SystemClock.elapsedRealtime();
                long intervalTime = nowTime - mOldTime;
                if (mOldTime == 0 || intervalTime >= mDelayMilliseconds) {
                    mOldTime = nowTime;

                    Friend mFriend = mNoticeFriendList.get(position);
                    if (mFriend != null) {
                        if (mFriend.getUserId().equals(Friend.ID_SK_PAY)) {
                            startActivity(new Intent(mContext, SKPayActivity.class));
                        } else {
                            Intent intent = new Intent(mContext, ChatActivity.class);
                            intent.putExtra(ChatActivity.FRIEND, mFriend);
                            startActivity(intent);
                        }
                    }

                }

            }
        });
    }

    // 删除公众号，
    private void showDeleteAllDialog(final int position) {
        Friend friend = mNoticeFriendList.get(position);
        if (friend.getStatus() == Friend.STATUS_UNKNOW) {// 陌生人
            return;
        }
        if (friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE)
                || friend.getUserId().equals(Friend.ID_SK_PAY)) {// 10000 与1100 号不能删除，
            Toast.makeText(mContext, getString(R.string.tip_not_allow_delete), Toast.LENGTH_SHORT).show();
            return;
        }
        SelectionFrame mSF = new SelectionFrame(this);
        mSF.setSomething(getString(R.string.delete_public_number), getString(R.string.ask_delete_public_number), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                deleteFriend(position, 1);
            }
        });
        mSF.show();
    }

    private void deleteFriend(final int position, final int type) {
        Friend friend = mNoticeFriendList.get(position);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_ATTENTION_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                    coreManager.getSelf(), XmppMessage.TYPE_DELALL, null, friend);
                            coreManager.sendNewFriendMessage(coreManager.getSelf().getUserId(), message); // 删除好友
                            // 老大说不要提示，行消失就可以，
                            /*
                            ToastUtil.showToast(getApplicationContext(), R.string.tip_delete_public_number_success);
                             */

                            FriendHelper.removeAttentionOrFriend(coreManager.getSelf().getUserId(), message.getUserId());

                            ChatMessage deleteChatMessage = new ChatMessage();
                            deleteChatMessage.setContent(getString(R.string.has_delete_public_number_place_holder, coreManager.getSelf().getNickName()));
                            deleteChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                            FriendDao.getInstance().updateLastChatMessage(coreManager.getSelf().getUserId(), Friend.ID_NEW_FRIEND_MESSAGE, deleteChatMessage);

                            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
                            NewFriendDao.getInstance().changeNewFriendState(coreManager.getSelf().getUserId(), Friend.STATUS_16);
                            ListenerManager.getInstance().notifyNewFriend(coreManager.getSelf().getUserId(), message, true);

                            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                            mNoticeFriendList.remove(position);
                            mNoticeAdapter.notifyDataSetChanged();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(getApplicationContext());
                    }
                });
    }

    class NoticeAdapter extends SlideBaseAdapter {
        NoticeAdapter(Context context) {
            super(context);
        }

        @Override
        public int getFrontViewId(int position) {
            return R.layout.item_notice_account;
        }

        @Override
        public int getLeftBackViewId(int position) {
            return 0;
        }

        @Override
        public int getRightBackViewId(int position) {
            return R.layout.item_notice_right;
        }

        @Override
        public int getCount() {
            return mNoticeFriendList.size();
        }

        @Override
        public Friend getItem(int position) {
            return mNoticeFriendList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public  int dip2px(Context context, float dpValue) {
            final float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dpValue * scale + 0.5f);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createConvertView(position);
            }
            HeadView mNoticeAccountIv = ViewHolder.get(convertView, R.id.notice_iv);
            //动态设置图片宽高 我加的
            //动态设置图片宽高
            RoundedImageView roundedImageView=mNoticeAccountIv.findViewById(R.id.ivHead);
            LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(dip2px(mContext,46),dip2px(mContext,46));
            layoutParams.setMargins(0,dip2px(mContext,4),0,dip2px(mContext,5));
            roundedImageView.setLayoutParams(layoutParams);
            TextView mNoticeAccountTv = ViewHolder.get(convertView, R.id.notice_tv);
            Friend friend = getItem(position);
            if (friend != null) {
                AvatarHelper.getInstance().displayAvatar(friend.getUserId(), mNoticeAccountIv);
                mNoticeAccountTv.setText(!TextUtils.isEmpty(friend.getRemarkName()) ? friend.getRemarkName() : friend.getNickName());
            }

            TextView delete_tv = ViewHolder.get(convertView, R.id.delete_tv);

            delete_tv.setOnClickListener(v -> {
                showDeleteAllDialog(position);
            });
            return convertView;
        }
    }
}
