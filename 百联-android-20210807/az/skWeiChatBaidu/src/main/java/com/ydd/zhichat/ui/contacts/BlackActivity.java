package com.ydd.zhichat.ui.contacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.adapter.FriendSortAdapter1;
import com.ydd.zhichat.bean.AttentionUser;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.broadcast.CardcastUiUpdateUtil;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.sortlist.BaseComparator;
import com.ydd.zhichat.sortlist.BaseSortModel;
import com.ydd.zhichat.sortlist.SideBar;
import com.ydd.zhichat.sortlist.SortHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.base.CoreManager;
import com.ydd.zhichat.ui.other.BasicInfoActivity;
import com.ydd.zhichat.util.AsyncUtils;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 黑名单列表
 */
public class BlackActivity extends BaseActivity {
    private ListView mPullToRefreshListView;
    private FriendSortAdapter1 mAdapter;
    private List<BaseSortModel<Friend>> mSortFriends;
    private BaseComparator<Friend> mBaseComparator;
    private SideBar mSideBar;
    private TextView mTextDialog;
    private String mLoginUserId;
    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CardcastUiUpdateUtil.ACTION_UPDATE_UI)) {
                loadData();
                mAdapter.setData(mSortFriends);
            }
        }
    };

    public BlackActivity() {
        mSortFriends = new ArrayList<BaseSortModel<Friend>>();
        mBaseComparator = new BaseComparator<Friend>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_black);
        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUpdateReceiver);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(R.string.black_list);
    }

    private void initView() {
        mPullToRefreshListView = (ListView) findViewById(R.id.pull_refresh_list);
        mPullToRefreshListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Friend friend = mSortFriends.get(position).getBean();
                if (friend != null) {
                    Intent intent = new Intent(BlackActivity.this, BasicInfoActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
                    startActivity(intent);
                }
            }
        });

        mSideBar = (SideBar) findViewById(R.id.sidebar);
        mTextDialog = (TextView) findViewById(R.id.text_dialog);
        mSideBar.setTextView(mTextDialog);
        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                // 该字母首次出现的位置
                int position = mAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mPullToRefreshListView.setSelection(position);
                }
            }
        });

        mAdapter = new FriendSortAdapter1(this, mSortFriends);
        mPullToRefreshListView.setAdapter(mAdapter);
        getBlackList();

        registerReceiver(mUpdateReceiver, CardcastUiUpdateUtil.getUpdateActionFilter());
    }

    private void loadData() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        AsyncUtils.doAsync(this, e -> {
            Reporter.post("加载数据失败，", e);
            AsyncUtils.runOnUiThread(this, ctx -> {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(ctx, R.string.data_exception);
            });
        }, c -> {
            final List<Friend> friends = FriendDao.getInstance().getAllBlacklists(mLoginUserId);
            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, Friend::getShowName);
            c.uiThread(r -> {
                DialogHelper.dismissProgressDialog();
                mSideBar.setExistMap(existMap);
                mSortFriends = sortedList;
                mAdapter.setData(sortedList);
                if (friends.size() == 0) {
                    findViewById(R.id.fl_empty).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.fl_empty).setVisibility(View.GONE);
                }
            });
        });
    }

    /**
     * 获取黑名单列表
     */
    private void getBlackList() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACK_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<AttentionUser>(AttentionUser.class) {
                    @Override
                    public void onResponse(ArrayResult<AttentionUser> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            List<AttentionUser> attentionUsers = result.getData();
                            if (attentionUsers != null && attentionUsers.size() > 0) {
                                for (int i = 0; i < attentionUsers.size(); i++) {
                                    AttentionUser attentionUser = attentionUsers.get(i);
                                    if (attentionUser == null) {
                                        continue;
                                    }
                                    String userId = attentionUser.getToUserId();// 好友的Id
                                    Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, userId);
                                    if (friend == null) {
                                        friend = new Friend();
                                        friend.setOwnerId(attentionUser.getUserId());
                                        friend.setUserId(attentionUser.getToUserId());
                                        friend.setNickName(attentionUser.getToNickName());
                                        friend.setRemarkName(attentionUser.getRemarkName());
                                        friend.setTimeCreate(attentionUser.getCreateTime());
                                        friend.setStatus(Friend.STATUS_BLACKLIST);

                                        friend.setOfflineNoPushMsg(attentionUser.getOfflineNoPushMsg());
                                        friend.setTopTime(attentionUser.getOpenTopChatTime());
                                        PreferenceUtils.putInt(MyApplication.getContext(), Constants.MESSAGE_READ_FIRE + attentionUser.getUserId() + CoreManager.requireSelf(MyApplication.getContext()).getUserId(),
                                                attentionUser.getIsOpenSnapchat());
                                        friend.setChatRecordTimeOut(attentionUser.getChatRecordTimeOut());// 消息保存天数 -1/0 永久

                                        friend.setCompanyId(attentionUser.getCompanyId());
                                        friend.setRoomFlag(0);
                                        FriendDao.getInstance().createOrUpdateFriend(friend);
                                    } else {
                                        FriendDao.getInstance().updateFriendStatus(mLoginUserId, userId, Friend.STATUS_BLACKLIST);
                                    }
                                }
                            }
                            loadData();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    //////////////其他操作///////////////////
   /* private CardcastActivity mActivity;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (CardcastActivity) getActivity();
    }*/

   /* private BaseSortModel<Friend> mmsortFriend = null;
    private NewFriendMessage mmessage = null;
    private String deletepacketid = null;

    private void showLongClickOperationDialog(final BaseSortModel<Friend> sortFriend) {
        Friend friend = sortFriend.getBean();
        if (friend.getStatus() != Friend.STATUS_BLACKLIST && friend.getStatus() == Friend.STATUS_ATTENTION
                && friend.getStatus() == Friend.STATUS_FRIEND) {
            return;
        }
        CharSequence[] items = new CharSequence[3];
        items[0] = getString(R.string.set_remark_name);// 设置备注名
        if (friend.getStatus() == Friend.STATUS_BLACKLIST) {// 在黑名单中,显示“设置备注名”、“移除黑名单”,"取消关注"，“彻底删除”
            items[1] = getString(R.string.remove_blacklist);
        } else {
            // items[1] = getString(R.string.add_blacklist);
        }
        // items[2] = getString(R.string.cancel_attention);
        items[2] = getString(R.string.delete_all);

        new AlertDialog.Builder(getActivity()).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:// 设置备注名
                        showRemarkDialog(sortFriend);
                        break;
                    case 1:// 加入黑名单，或者移除黑名单
                        showBlacklistDialog(sortFriend);
                        break;
                    case 2:// 解除关注关系或者解除好友关系
                        showDeleteAllDialog(sortFriend);
                        break;
                }
            }
        }).setCancelable(true).create().show();
    }

    *//**
     * 设置备注名称
     *//*
    private void showRemarkDialog(final BaseSortModel<Friend> sortFriend) {
        DialogHelper.showSingleInputDialog(getActivity(), getString(R.string.set_remark_name), sortFriend.getBean().getShowName(), 2, 2, new InputFilter[]{new InputFilter.LengthFilter(20)}, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = ((EditText) v).getText().toString().trim();
                if (input.equals(sortFriend.getBean().getShowName())) {// 备注名没变
                    return;
                }
                *//*if (!StringUtils.isNickName(input)) {// 不符合昵称
                    if (input.length() != 0) {
                        ToastUtil.showToast(getActivity(), R.string.remark_name_format_error);
                        return;
                    } else {// 不符合昵称，因为长度为0，但是可以做备注名操作，操作就是清除备注名
                        // 判断之前有没有备注名
                        if (TextUtils.isEmpty(sortFriend.getBean().getRemarkName())) {// 如果没有备注名，就不需要清除
                            return;
                        }
                    }
                }*//*
                remarkFriend(sortFriend, input);
            }
        });
    }

    private void remarkFriend(final BaseSortModel<Friend> sortFriend, final String remarkName) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", sortFriend.getBean().getUserId());
        params.put("remarkName", remarkName);
        DialogHelper.showDefaulteMessageProgressDialog(getActivity());

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_REMARK)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        String firstLetter = sortFriend.getFirstLetter();
                        mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
                        sortFriend.getBean().setRemarkName(remarkName);// 修改备注名称
                        setSortCondition(sortFriend);
                        Collections.sort(mSortFriends, mBaseComparator);
                        mAdapter.notifyDataSetChanged();
                        // 更新到数据库
                        FriendDao.getInstance().setRemarkName(mLoginUserId, sortFriend.getBean().getUserId(), remarkName);
                        // 更新消息界面（因为昵称变了，所有要更新）
                        MsgBroadcast.broadcastMsgUiUpdate(getActivity());
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    *//**
     * 移除黑名单
     *//*
    private void showBlacklistDialog(final BaseSortModel<Friend> sortFriend) {
        removeBlacklist(sortFriend);
    }

    private void removeBlacklist(final BaseSortModel<Friend> sortFriend) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", sortFriend.getBean().getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(getActivity());

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(getActivity(), "移出黑名单成功");
                        FriendDao.getInstance().updateFriendStatus(sortFriend.getBean().getOwnerId(), sortFriend.getBean().getUserId(), Friend.STATUS_FRIEND);

                        ChatMessage removeChatMessage = new ChatMessage();
                        removeChatMessage.setContent(coreManager.getSelf().getNickName() + InternationalizationHelper.getString("REMOVE"));
                        removeChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                        FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, removeChatMessage);

                        mSortFriends.remove(sortFriend);
                        String firstLetter = sortFriend.getFirstLetter();
                        mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
                        mAdapter.notifyDataSetInvalidated();

                        CardcastUiUpdateUtil.broadcastUpdateUi(getActivity());
                        MsgBroadcast.broadcastMsgUiUpdate(getActivity());
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    *//**
     * 彻底删除
     *//*
    private void showDeleteAllDialog(final BaseSortModel<Friend> sortFriend) {
        if (sortFriend.getBean().getStatus() == Friend.STATUS_UNKNOW) {
            return;
        }
        deleteFriend(sortFriend, 1);
    }

    private void deleteFriend(final BaseSortModel<Friend> sortFriend, final int type) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", sortFriend.getBean().getUserId());
        String url = mActivity.coreManager.getConfig().FRIENDS_DELETE;
        DialogHelper.showDefaulteMessageProgressDialog(getActivity());

        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(getActivity(), R.string.delete_all_succ);
                        NewFriendMessage message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(),
                                XmppMessage.TYPE_DELALL, null, sortFriend.getBean());

                        mActivity.sendNewFriendMessage(sortFriend.getBean().getUserId(), message);
                        deletepacketid = message.getPacketId();
                        mmessage = message;
                        mmsortFriend = sortFriend;
                        FriendHelper.removeAttentionOrFriend(mLoginUserId, mmsortFriend.getBean().getUserId());

                        mSortFriends.remove(mmsortFriend);
                        String firstLetter = mmsortFriend.getFirstLetter();
                        // 移除之前设置的首字母
                        mSideBar.removeExist(firstLetter);
                        mAdapter.notifyDataSetInvalidated();

                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setContent(InternationalizationHelper.getString("JXAlert_DeleteFirend") + " " + mmsortFriend.getBean().getNickName());
                        chatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                        FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, chatMessage);

                        NewFriendDao.getInstance().createOrUpdateNewFriend(message);
                        NewFriendDao.getInstance().changeNewFriendState(mmsortFriend.getBean().getUserId(), Friend.STATUS_16);
                        ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mmessage, true);

                        MsgBroadcast.broadcastMsgUiUpdate(getActivity());
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }*/
}
