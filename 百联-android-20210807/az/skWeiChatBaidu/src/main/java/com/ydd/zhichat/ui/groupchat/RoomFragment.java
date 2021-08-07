package com.ydd.zhichat.ui.groupchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ListView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.adapter.FriendSortAdapter;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.bean.message.MucRoom;
import com.ydd.zhichat.broadcast.MsgBroadcast;
import com.ydd.zhichat.broadcast.MucgroupUpdateUtil;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.db.dao.OnCompleteListener2;
import com.ydd.zhichat.sortlist.BaseComparator;
import com.ydd.zhichat.sortlist.BaseSortModel;
import com.ydd.zhichat.sortlist.SideBar;
import com.ydd.zhichat.sortlist.SortHelper;
import com.ydd.zhichat.ui.base.EasyFragment;
import com.ydd.zhichat.ui.message.MucChatActivity;
import com.ydd.zhichat.util.AsyncUtils;
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
 * 我的群组
 */
public class RoomFragment extends EasyFragment {
    private PullToRefreshListView mPullToRefreshListView;
    private FriendSortAdapter mAdapter;
    private List<BaseSortModel<Friend>> mSortFriends;
    private BaseComparator<Friend> mBaseComparator;
    private SideBar mSideBar;
    private TextView mTextDialog;

    private String mLoginUserId;
    private Handler mHandler = new Handler();
    private boolean mNeedUpdate = true;
    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MucgroupUpdateUtil.ACTION_UPDATE)) {
                update();
            }
        }
    };

    public RoomFragment() {
        mSortFriends = new ArrayList<BaseSortModel<Friend>>();
        mBaseComparator = new BaseComparator<Friend>();
    }

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_room;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        mLoginUserId = coreManager.getSelf().getUserId();
        if (createView) {
            initView();
        }
    }

    public void update() {
        if (isResumed()) {
            loadData();
        } else {
            mNeedUpdate = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNeedUpdate) {
            loadData();
            mNeedUpdate = false;
        }
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mUpdateReceiver);
        super.onDestroy();
    }

    private void initView() {
        mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
        mAdapter = new FriendSortAdapter(getActivity(), mSortFriends);
        mPullToRefreshListView.setMode(Mode.PULL_FROM_START);
        mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);

        mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                updateRoom();
            }
        });

        mPullToRefreshListView.setOnItemClickListener((parent, view, position, id) -> {
            Friend friend = mSortFriends.get((int) id).getBean();
            Intent intent = new Intent(getActivity(), MucChatActivity.class);
            intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
            intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
            intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
            startActivity(intent);
            if (friend.getUnReadNum() > 0) {// 如该群组未读消息数量大于1, 刷新MessageFragment
                MsgBroadcast.broadcastMsgNumReset(getActivity());
                MsgBroadcast.broadcastMsgUiUpdate(getActivity());
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
                    mPullToRefreshListView.getRefreshableView().setSelection(position);
                }
            }
        });
       /* mPullToRefreshListView.getRefreshableView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                BaseSortModel<Friend> sortFriend = mSortFriends.get((int) id);
                if (sortFriend == null || sortFriend.getBean() == null) {
                    return false;
                }
                showLongClickOperationDialog(sortFriend);
                return true;
            }
        });*/

        getActivity().registerReceiver(mUpdateReceiver, MucgroupUpdateUtil.getUpdateActionFilter());
    }

    private void loadData() {
        AsyncUtils.doAsync(this, e -> {
            Reporter.post("加载数据失败，", e);
            AsyncUtils.runOnUiThread(requireContext(), ctx -> {
                ToastUtil.showToast(ctx, R.string.data_exception);
            });
        }, c -> {
            long startTime = System.currentTimeMillis();
            final List<Friend> friends = FriendDao.getInstance().getAllRooms(mLoginUserId);
            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, Friend::getShowName);

            long delayTime = 200 - (startTime - System.currentTimeMillis());// 保证至少200ms的刷新过程
            if (delayTime < 0) {
                delayTime = 0;
            }
            c.postDelayed(r -> {
                mSideBar.setExistMap(existMap);
                mSortFriends = sortedList;
                mAdapter.setData(sortedList);
                mPullToRefreshListView.onRefreshComplete();
            }, delayTime);
        });
    }

    /**
     * 下载我的群组
     */
    private void updateRoom() {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", "0");
        params.put("pageIndex", "0");
        params.put("pageSize", "1000");// 给一个尽量大的值

        HttpUtils.get().url(coreManager.getConfig().ROOM_LIST_HIS)
                .params(params)
                .build()
                .execute(new ListCallback<MucRoom>(MucRoom.class) {
                    @Override
                    public void onResponse(ArrayResult<MucRoom> result) {
                        if (result.getResultCode() == 1) {
                            FriendDao.getInstance().addRooms(mHandler, mLoginUserId, result.getData(),
                                    new OnCompleteListener2() {
                                        @Override
                                        public void onLoading(int progressRate, int sum) {

                                        }

                                        @Override
                                        public void onCompleted() {// 下载完成
                                            if (coreManager.isLogin()) {
                                                // 1.调用smack内join方法加入群组
                                                List<Friend> mFriends = FriendDao.getInstance().getAllRooms(mLoginUserId);
                                                for (int i = 0; i < mFriends.size(); i++) {// 已加入的群组不会重复加入，方法内已去重
                                                    coreManager.joinMucChat(mFriends.get(i).getUserId(),
                                                            mFriends.get(i).getTimeSend());
                                                }
                                                // 2.更新我的群组列表
                                                loadData();
                                            }
                                        }
                                    });
                        } else {
                            mPullToRefreshListView.onRefreshComplete();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(getActivity());
                        mPullToRefreshListView.onRefreshComplete();
                    }
                });
    }

    /////////////其他操作///////////////////
   /* private void showLongClickOperationDialog(final BaseSortModel<Friend> sortFriend) {
        Friend friend = sortFriend.getBean();
        if (friend.getStatus() != Friend.STATUS_BLACKLIST && friend.getStatus() == Friend.STATUS_ATTENTION
                && friend.getStatus() == Friend.STATUS_FRIEND) {
            return;
        }
        CharSequence[] items = new CharSequence[1];
        // items[0] = getString(R.string.set_remark_name);// 设置备注名
        // if (friend.getStatus() == Friend.STATUS_BLACKLIST) {// 在黑名单中,显示“设置备注名”、“移除黑名单”,"取消关注"，“彻底删除”
        // items[1] = getString(R.string.remove_blacklist);
        // } else {
        // items[1] = getString(R.string.add_blacklist);
        // }
        // items[2] = getString(R.string.cancel_attention);
        items[0] = getString(R.string.delete_all);

        new AlertDialog.Builder(getActivity()).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:// 彻底删除
                        deleteRoom(sortFriend);
                        break;
                }
                // switch (which) {
                // case 0:// 设置备注名
                // showRemarkDialog(sortFriend);
                // break;
                // case 1:// 加入黑名单，或者移除黑名单
                // showBlacklistDialog(sortFriend);
                // break;
                // case 2:// 取消关注
                // showCancelAttentionDialog(sortFriend);
                // break;
                // case 3:// 解除关注关系或者解除好友关系
                // showDeleteAllDialog(sortFriend);
                // break;
                // }
            }
        }).setCancelable(true).create().show();
    }

    private void deleteRoom(final BaseSortModel<Friend> sortFriend) {
        MainActivity activity = (MainActivity) getActivity();
        boolean deleteRoom = false;
        if (mLoginUserId.equals(sortFriend.getBean().getRoomCreateUserId())) {
            deleteRoom = true;
        }
        String url = null;
        if (deleteRoom) {
            url = activity.coreManager.getConfig().ROOM_DELETE;
        } else {
            url = activity.coreManager.getConfig().ROOM_MEMBER_DELETE;
        }
        DialogHelper.showDefaulteMessageProgressDialog(getActivity());
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", sortFriend.getBean().getRoomId());
        if (!deleteRoom) {
            params.put("userId", mLoginUserId);
        }

        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        deleteFriend(sortFriend);
                        // 删除该张表
                        RoomMemberDao.getInstance().deleteRoomMemberTable(sortFriend.getBean().getRoomId());
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    private void deleteFriend(final BaseSortModel<Friend> sortFriend) {
        mSortFriends.remove(sortFriend);
        String firstLetter = sortFriend.getFirstLetter();
        mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
        mAdapter.notifyDataSetChanged();

        Friend friend = sortFriend.getBean();
        // 删除这个房间
        FriendDao.getInstance().deleteFriend(mLoginUserId, friend.getUserId());
        // 消息表中删除
        ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, friend.getUserId());

        // 更新消息界面
        MsgBroadcast.broadcastMsgNumReset(getActivity());
        MsgBroadcast.broadcastMsgUiUpdate(getActivity());

        MainActivity activity = (MainActivity) getActivity();
        activity.exitMucChat(friend.getUserId());
    }*/
}
