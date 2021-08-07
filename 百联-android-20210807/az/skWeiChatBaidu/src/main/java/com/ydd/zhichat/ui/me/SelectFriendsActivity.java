package com.ydd.zhichat.ui.me;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.bean.Label;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.db.dao.LabelDao;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.sortlist.BaseComparator;
import com.ydd.zhichat.sortlist.BaseSortModel;
import com.ydd.zhichat.sortlist.SideBar;
import com.ydd.zhichat.sortlist.SortHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.me.sendgroupmessage.ChatActivityForSendGroup;
import com.ydd.zhichat.util.AsyncUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.util.ViewHolder;
import com.ydd.zhichat.view.ClearEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/12/5 0005.
 * 只是对临时的数据(status)进行更改，来标记是否选中，并不需要去更新数据库 status 100 未选中 101选中
 */

public class SelectFriendsActivity extends BaseActivity {
    private ClearEditText mSearchEdit;
    private boolean isSearch;

    private SideBar mSideBar;
    private TextView mTextDialog;
    private ListView mListView;
    private SelectFriendAdapter mSelectAdapter;
    private List<Friend> mFriendList;
    private List<BaseSortModel<Friend>> mSortFriends;
    private List<BaseSortModel<Friend>> mSearchSortFriends;
    private BaseComparator<Friend> mBaseComparator;

    private TextView mNextTv;
    private List<String> userIdList;
    private List<String> userNameList;

    private TextView mLabelNameTv;
    private List<String> mLabelIds;
    private List<String> mLabelNames;
    private TextView tvRight;
    private boolean isAllOrCancel;
    private My_BroadcastReceiver mMyBroadcastReceiver = new My_BroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friends);
        initActionBar();
        initData();
        initView();
        initEvent();

        registerReceiver(mMyBroadcastReceiver, new IntentFilter(com.ydd.zhichat.broadcast.OtherBroadcast.SEND_MULTI_NOTIFY));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMyBroadcastReceiver != null) {
            unregisterReceiver(mMyBroadcastReceiver);
        }
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
        tvTitle.setText(getString(R.string.select_recipient));
        tvRight = (TextView) findViewById(R.id.tv_title_right);
        tvRight.setText(getString(R.string.select_all));
    }

    private void initData() {
        mFriendList = new ArrayList<>();
        mSortFriends = new ArrayList<>();
        mSearchSortFriends = new ArrayList<>();
        mBaseComparator = new BaseComparator<>();

        userIdList = new ArrayList<>();
        userNameList = new ArrayList<>();

        mLabelIds = new ArrayList<>();
        mLabelNames = new ArrayList<>();
    }

    private void initView() {
        mSearchEdit =  findViewById(R.id.search_et);
        mLabelNameTv = (TextView) findViewById(R.id.label_name);

        mListView = (ListView) findViewById(R.id.select_lv);
        mSelectAdapter = new SelectFriendAdapter(this);
        mListView.setAdapter(mSelectAdapter);

        mNextTv = (TextView) findViewById(R.id.next_tv);

        mSideBar = (SideBar) findViewById(R.id.sidebar);
        mSideBar.setVisibility(View.VISIBLE);
        mTextDialog = (TextView) findViewById(R.id.text_dialog);
        mSideBar.setTextView(mTextDialog);
        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                // 该字母首次出现的位置
                int position = mSelectAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mListView.setSelection(position);
                }
            }
        });

        loadData();
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
            final List<Friend> friends = FriendDao.getInstance().getAllFriends(coreManager.getSelf().getUserId());
            for (Friend friend : friends) {
                // 只是对临时的数据(status)进行更改，来标记是否选中，并不需要去更新数据库 status 100 未选中 101选中
                // TODO: 不如改成存个稀疏数组表示选中，最好是另外封装个实体，包括friend和选中状态，
                friend.setStatus(100);
            }
            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, Friend::getShowName);
            c.uiThread(r -> {
                DialogHelper.dismissProgressDialog();
                mSideBar.setExistMap(existMap);
                mFriendList = friends;
                mSortFriends = sortedList;
                mSelectAdapter.setData(sortedList);
            });
        });
    }

    private void initEvent() {
        tvRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFriendList == null || mFriendList.size() <= 0
                        || mSortFriends == null || mSortFriends.size() <= 0)
                    return;

                isAllOrCancel = !isAllOrCancel;
                if (isAllOrCancel) {
                    for (int i = 0; i < mFriendList.size(); i++) {
                        mFriendList.get(i).setStatus(101);
                        mSortFriends.get(i).getBean().setStatus(101);
                    }
                    tvRight.setText(getString(R.string.cancel));
                    mNextTv.setText(getString(R.string.next_step) + "(" + mFriendList.size() + ")");
                } else {
                    for (int i = 0; i < mFriendList.size(); i++) {
                        mFriendList.get(i).setStatus(100);
                        mSortFriends.get(i).getBean().setStatus(100);
                    }
                    tvRight.setText(getString(R.string.select_all));
                    mNextTv.setText(getString(R.string.next_step));
                }
                mSelectAdapter.setData(mSortFriends);
            }
        });

        mSearchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = mSearchEdit.getText().toString();
                mSearchSortFriends.clear();
                if (TextUtils.isEmpty(str)) {
                    isSearch = false;
                    mSelectAdapter.setData(mSortFriends);
                    tvRight.setVisibility(View.VISIBLE);
                } else {
                    isSearch = true;
                    for (int i = 0; i < mSortFriends.size(); i++) {
                        Friend friend = mSortFriends.get(i).getBean();
                        String matchX = !TextUtils.isEmpty(friend.getRemarkName()) ? friend.getRemarkName() : friend.getNickName();
                        if (matchX.contains(str)) {
                            mSearchSortFriends.add(mSortFriends.get(i));
                        }
                    }
                    mSelectAdapter.setData(mSearchSortFriends);
                    tvRight.setVisibility(View.GONE);
                }
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Friend friend;
                if (!isSearch) {
                    friend = mSortFriends.get(position).getBean();
                } else {
                    friend = mSearchSortFriends.get(position).getBean();
                }
                if (friend.getStatus() == 101) {
                    friend.setStatus(100);
                } else {
                    friend.setStatus(101);
                }
                if (!isSearch) {
                    mSortFriends.get(position).getBean().setStatus(friend.getStatus());
                    mSelectAdapter.setData(mSortFriends);
                } else {
                    mSearchSortFriends.get(position).getBean().setStatus(friend.getStatus());
                    mSelectAdapter.setData(mSearchSortFriends);
                }
                // 同时需要更新总数据
                for (int i = 0; i < mFriendList.size(); i++) {
                    if (mFriendList.get(i).getUserId().equals(friend.getUserId())) {
                        mFriendList.get(i).setStatus(friend.getStatus());
                    }
                }

                int count = 0;
                // 计算被选中人数
                for (int i = 0; i < mFriendList.size(); i++) {
                    if (mFriendList.get(i).getStatus() == 101) {
                        count = count + 1;
                    }
                }

                if (count == 0) {
                    mNextTv.setText(getString(R.string.next_step));
                } else {
                    mNextTv.setText(getString(R.string.next_step) + "(" + count + ")");
                }
            }
        });

        mNextTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Friend friend : mFriendList) {
                    if (friend.getStatus() == 101) {
                        userIdList.add(friend.getUserId());
                        userNameList.add(!TextUtils.isEmpty(friend.getRemarkName()) ? friend.getRemarkName() : friend.getNickName());
                    }
                }
                // Todo 因为添加了标签，所以逻辑需要改变
                List<String> ids = new ArrayList<>();
                List<String> names = new ArrayList<>();
                ids.addAll(userIdList);
                names.addAll(userNameList);
                // 取出标签内的用户
                for (int i = 0; i < mLabelIds.size(); i++) {
                    Label label = LabelDao.getInstance().getLabel(coreManager.getSelf().getUserId(), mLabelIds.get(i));
                    if (label != null) {
                        String idList = label.getUserIdList();
                        List<String> list = JSON.parseArray(idList, String.class);
                        if (list != null && list.size() > 0) {
                            for (int i1 = 0; i1 < list.size(); i1++) {
                                ids.add(list.get(i1));
                                Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), list.get(i1));
                                if (friend != null) {
                                    names.add(TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName());
                                }
                            }
                        }
                    }
                }

                // 去重
                List<String> x = new ArrayList<>(new HashSet<>(ids));
                List<String> y = new ArrayList<>(new HashSet<>(names));

                if (x.size() > 0) {
                    Intent intent = new Intent(SelectFriendsActivity.this, ChatActivityForSendGroup.class);
                    intent.putExtra("USERIDS", JSON.toJSONString(x));
                    intent.putExtra("USERNAMES", JSON.toJSONString(y));

                    userIdList.clear();
                    userNameList.clear();
                    startActivity(intent);
                } else {
                    ToastUtil.showToast(mContext, getString(R.string.alert_select_one));
                }
            }
        });

        findViewById(R.id.ll_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectFriendsActivity.this, SelectLabelActivity.class);
                intent.putExtra("SELECTED_LABEL", JSON.toJSONString(mLabelIds));
                startActivityForResult(intent, 0x01);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0x01 && resultCode == RESULT_OK) {
            mLabelIds.clear();
            mLabelNames.clear();

            String mSelectedLabelIds = data.getStringExtra("SELECTED_LABEL_IDS");
            String mSelectedLabelNames = data.getStringExtra("SELECTED_LABEL_NAMES");
            mLabelIds = JSON.parseArray(mSelectedLabelIds, String.class);
            mLabelNames = JSON.parseArray(mSelectedLabelNames, String.class);

            if (mLabelIds.size() > 0) {
                mLabelNameTv.setText(mSelectedLabelNames);
                mLabelNameTv.setVisibility(View.VISIBLE);
            } else {
                mLabelNameTv.setText("");
                mLabelNameTv.setVisibility(View.GONE);
            }
        }
    }

    private class My_BroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(com.ydd.zhichat.broadcast.OtherBroadcast.SEND_MULTI_NOTIFY)) {
                finish();
            }
        }
    }

    class SelectFriendAdapter extends BaseAdapter implements SectionIndexer {

        List<BaseSortModel<Friend>> mSortFriends;
        private Context mContext;

        public SelectFriendAdapter(Context context) {
            this.mContext = context;
            this.mSortFriends = new ArrayList<>();
        }

        public void setData(List<BaseSortModel<Friend>> sortFriends) {
            this.mSortFriends = sortFriends;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mSortFriends.size();
        }

        @Override
        public Object getItem(int position) {
            return mSortFriends.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_select_friend, parent, false);
            }

            CheckBox cb = ViewHolder.get(convertView, R.id.select_cb);
            ImageView iv = ViewHolder.get(convertView, R.id.select_iv);
            TextView tv = ViewHolder.get(convertView, R.id.select_tv);

            Friend friend = mSortFriends.get(position).getBean();
            if (friend != null) {
                AvatarHelper.getInstance().displayAvatar(TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName(),
                        friend.getUserId(), iv, true);
                tv.setText(!TextUtils.isEmpty(friend.getRemarkName()) ? friend.getRemarkName() : friend.getNickName());
                cb.setChecked(friend.getStatus() == 101);
            }
            return convertView;
        }

        @Override
        public Object[] getSections() {
            return null;
        }

        @Override
        public int getPositionForSection(int section) {
            for (int i = 0; i < getCount(); i++) {
                String sortStr = mSortFriends.get(i).getFirstLetter();
                char firstChar = sortStr.toUpperCase().charAt(0);
                if (firstChar == section) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mSortFriends.get(position).getFirstLetter().charAt(0);
        }
    }
}
