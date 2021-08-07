package com.ydd.zhichat.ui.circle.range;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.DisplayUtil;
import com.ydd.zhichat.util.ViewHolder;
import com.ydd.zhichat.view.CircleImageView;
import com.ydd.zhichat.view.ClearEditText;
import com.ydd.zhichat.view.HorizontalListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * 提醒谁看
 */
public class AtSeeCircleActivity extends BaseActivity {
    private ClearEditText mEditText;
    private boolean isSearch;
    private ListView mListView;
    private ListViewAdapter mAdapter;
    private HorizontalListView mHorizontalListView;
    private HorListViewAdapter mHorAdapter;
    private Button mOkBtn;
    private List<Friend> mFriendList;
    private List<Friend> mFriendSearch;
    private List<String> mSelectPositions;
    // 范围
    private int remindType;
    // 被标记为可看 || 不可看
    private List<String> mExistIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contacts);
        if (getIntent() != null) {
            remindType = getIntent().getIntExtra("REMIND_TYPE", 1);
            String remind = getIntent().getStringExtra("REMIND_PERSON");
            if (remind != null) {
                mExistIds = Arrays.asList(remind.split(","));
            }
        }
        mFriendList = new ArrayList<>();
        mFriendSearch = new ArrayList<>();
        mSelectPositions = new ArrayList<>();
        mAdapter = new ListViewAdapter();
        mHorAdapter = new HorListViewAdapter();
        initView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        List<Friend> friends = FriendDao.getInstance().getAllFriends(coreManager.getSelf().getUserId());
        if (friends != null) {
            mFriendList.clear();
            if (remindType == 1) {
                // 公开，都可被提醒
                for (int i = 0; i < friends.size(); i++) {
                    mFriendList.add(friends.get(i));
                }
            } else if (remindType == 3) {
                // 指定可看，可看需添加
                for (int i = 0; i < friends.size(); i++) {
                    String id = friends.get(i).getUserId();
                    for (int i1 = 0; i1 < mExistIds.size(); i1++) {
                        if (mExistIds.get(i1).equals(id)) {
                            mFriendList.add(friends.get(i));
                        }
                    }
                }
            } else if (remindType == 4) {
                // 指定不可看，不可看需移除
                for (int i = 0; i < friends.size(); i++) {
                    String id = friends.get(i).getUserId();
                    mFriendList.add(friends.get(i));
                    for (int i1 = 0; i1 < mExistIds.size(); i1++) {
                        if (mExistIds.get(i1).equals(id)) {
                            mFriendList.remove(friends.get(i));
                        }
                    }
                }
            }
            mAdapter.setData(mFriendList);
        }
    }

    private void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.remind_who_to_see));
        mListView = (ListView) findViewById(R.id.list_view);
        mHorizontalListView = (HorizontalListView) findViewById(R.id.horizontal_list_view);
        mOkBtn = (Button) findViewById(R.id.ok_btn);
        // mOkBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        mListView.setAdapter(mAdapter);
        mHorizontalListView.setAdapter(mHorAdapter);

        /**
         * 群内邀请好友搜索功能
         */
        mEditText = findViewById(R.id.search_et);
        mEditText.setHint(InternationalizationHelper.getString("JX_Seach"));
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                isSearch = true;
                mFriendSearch.clear();
                String mContent = mEditText.getText().toString();
                if (TextUtils.isEmpty(mContent)) {
                    isSearch = false;
                    mAdapter.setData(mFriendList);
                }
                for (int i = 0; i < mFriendList.size(); i++) {
                    String name = !TextUtils.isEmpty(mFriendList.get(i).getRemarkName()) ? mFriendList.get(i).getRemarkName() : mFriendList.get(i).getNickName();
                    if (name.contains(mContent)) {
                        // 符合搜索条件的好友
                        mFriendSearch.add((mFriendList.get(i)));
                    }
                }
                mAdapter.setData(mFriendSearch);
            }
        });

        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Friend friend;
                if (isSearch) {
                    friend = mFriendSearch.get(position);
                } else {
                    friend = mFriendList.get(position);
                }

                for (int i = 0; i < mFriendList.size(); i++) {
                    if (mFriendList.get(i).getUserId().equals(friend.getUserId())) {
                        if (friend.getStatus() != 100) {
                            friend.setStatus(100);
                            mFriendList.get(i).setStatus(100);
                            addSelect(friend.getUserId());
                        } else {
                            friend.setStatus(101);
                            mFriendList.get(i).setStatus(101);
                            removeSelect(friend.getUserId());
                        }

                        if (isSearch) {
                            mAdapter.setData(mFriendSearch);
                        } else {
                            mAdapter.setData(mFriendList);
                        }
                    }
                }
            }
        });

        mHorizontalListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                for (int i = 0; i < mFriendList.size(); i++) {
                    if (mFriendList.get(i).getUserId().equals(mSelectPositions.get(position))) {
                        mFriendList.get(i).setStatus(101);
                        mAdapter.setData(mFriendList);
                    }
                }
                mSelectPositions.remove(position);
                mHorAdapter.notifyDataSetInvalidated();
                mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
            }
        });

        mOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("THIS_CIRCLE_REMIND_PERSON", getSelected());
                intent.putExtra("THIS_CIRCLE_REMIND_PERSON_NAME", getSelectedName());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    // 得到'部分可见' || '不给谁看'的人的id,','号拼接
    private String getSelected() {
        String permissionsList = "";
        for (int i = 0; i < mSelectPositions.size(); i++) {
            Friend friend = new Friend();
            for (Friend friend1 : mFriendList) {
                if (friend1.getUserId().equals(mSelectPositions.get(i))) {
                    friend = friend1;
                }
            }
            if (i == mSelectPositions.size() - 1) {
                permissionsList += friend.getUserId();
            } else {
                permissionsList += friend.getUserId() + ",";
            }
        }
        return permissionsList;
    }

    // 得到'部分可见' || '不给谁看'的人的名字,'，'号拼接
    private String getSelectedName() {
        String permissionsListName = "";
        for (int i = 0; i < mSelectPositions.size(); i++) {
            Friend friend = new Friend();
            for (Friend friend1 : mFriendList) {
                if (friend1.getUserId().equals(mSelectPositions.get(i))) {
                    friend = friend1;
                }
            }
            String name = friend.getRemarkName();
            if (TextUtils.isEmpty(name)) {
                name = friend.getNickName();
            }
            if (i == mSelectPositions.size() - 1) {
                permissionsListName += name;
            } else {
                permissionsListName += name + "，";
            }
        }
        return permissionsListName;
    }

    private void addSelect(String userId) {
        mSelectPositions.add(userId);
        mHorAdapter.notifyDataSetInvalidated();
        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
    }

    private void removeSelect(String userId) {
        for (int i = 0; i < mSelectPositions.size(); i++) {
            if (mSelectPositions.get(i).equals(userId)) {
                mSelectPositions.remove(i);
            }
        }
        mHorAdapter.notifyDataSetInvalidated();
        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
    }

    class ListViewAdapter extends BaseAdapter {
        private List<Friend> mFriends;

        public ListViewAdapter() {
            mFriends = new ArrayList<>();
        }

        public void setData(List<Friend> mFriend) {
            mFriends = mFriend;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mFriends.size();
        }

        @Override
        public Object getItem(int position) {
            return mFriends.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_select_remind_see_circle, parent, false);
            }
            CheckBox checkBox = ViewHolder.get(convertView, R.id.see_check_box);
            checkBox.setChecked(false);
            CircleImageView avatarImg = ViewHolder.get(convertView, R.id.see_avatar);
            AvatarHelper.getInstance().displayAvatar(mFriends.get(position).getUserId(), avatarImg, true);
            TextView userNameTv = ViewHolder.get(convertView, R.id.see_name);
            if (TextUtils.isEmpty(mFriends.get(position).getRemarkName())) {
                userNameTv.setText(mFriends.get(position).getNickName());
            } else {
                userNameTv.setText(mFriends.get(position).getRemarkName());
            }

            if (mFriends.get(position).getStatus() == 100) {
                checkBox.setChecked(true);
            } else {
                checkBox.setChecked(false);
            }
            return convertView;
        }
    }

    class HorListViewAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mSelectPositions.size();
        }

        @Override
        public Object getItem(int position) {
            return mSelectPositions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new CircleImageView(mContext);
                int size = DisplayUtil.dip2px(mContext, 37);
                AbsListView.LayoutParams param = new AbsListView.LayoutParams(size, size);
                convertView.setLayoutParams(param);
            }
            CircleImageView imageView = (CircleImageView) convertView;
            String selectPosition = mSelectPositions.get(position);
            AvatarHelper.getInstance().displayAvatar(selectPosition, imageView, true);
            return convertView;
        }
    }
}
