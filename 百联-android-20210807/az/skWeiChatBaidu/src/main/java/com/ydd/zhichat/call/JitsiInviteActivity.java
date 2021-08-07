package com.ydd.zhichat.call;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import  com.ydd.zhichat.R;
import  com.ydd.zhichat.bean.message.MucRoom;
import  com.ydd.zhichat.bean.message.MucRoomMember;
import  com.ydd.zhichat.helper.AvatarHelper;
import  com.ydd.zhichat.helper.DialogHelper;
import  com.ydd.zhichat.ui.base.BaseActivity;
import  com.ydd.zhichat.util.Constants;
import  com.ydd.zhichat.util.ToastUtil;
import  com.ydd.zhichat.util.ViewHolder;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

public class JitsiInviteActivity extends BaseActivity {
    private String mLoginUserId;
    private String mLoginNickName;
    private boolean isAudio;
    private String voicejid;// 群发消息jid
    private String roomId;  // 查询群成员

    private EditText mEditText;
    private boolean isSearch = false;
    private ListView inviteList;
    private ListViewAdapter mAdapter;
    private List<SelectMuMembers> mMucMembers;
    private List<SelectMuMembers> mCurrentMucMembers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jitsi_invite);
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();
        isAudio = getIntent().getBooleanExtra(Constants.IS_AUDIO_CONFERENCE, false);
        voicejid = getIntent().getStringExtra("voicejid");
        roomId = getIntent().getStringExtra("roomid");

        initAction();
        initView();
        initData();
    }

    private void initAction() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.select_contacts));
        TextView tvRight = (TextView) findViewById(R.id.tv_title_right);
        tvRight.setText(getString(R.string.finish));
        tvRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> data = new ArrayList<>();
                for (int i = 0; i < mMucMembers.size(); i++) {
                    if (mMucMembers.get(i).isChecked) {
                        data.add(mMucMembers.get(i).getMember().getUserId());
                    }
                }
                if (isAudio) {
                    EventBus.getDefault().post(new MessageEventMeetingInvite(roomId, "", mLoginUserId, mLoginUserId, mLoginNickName, voicejid, data, true));
                } else {
                    EventBus.getDefault().post(new MessageEventMeetingInvite(roomId, "", mLoginUserId, mLoginUserId, mLoginNickName, voicejid, data, false));
                }

                // 进入会议
//                Intent intent = new Intent(JitsiInviteActivity.this, Jitsi_connecting_second.class);
//                if (isAudio) {
//                    intent.putExtra("type", 3);
//                } else {
//                    intent.putExtra("type", 4);
//                }
//                intent.putExtra("fromuserid", voicejid);
//                intent.putExtra("touserid", mLoginUserId);
//                startActivity(intent);
                finish();
            }
        });
    }

    private void initView() {
        mEditText = (EditText) findViewById(R.id.search_edit);
        mEditText.setHint(getString(R.string.search));
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mCurrentMucMembers.clear();
                String str = s.toString().trim();
                if (TextUtils.isEmpty(str)) {
                    isSearch = false;
                    mCurrentMucMembers.addAll(mMucMembers);
                } else {
                    isSearch = true;
                    for (int i = 0; i < mMucMembers.size(); i++) {
                        if (mMucMembers.get(i).getMember().getNickName().contains(str)) {// 符合条件
                            mCurrentMucMembers.add((mMucMembers.get(i)));
                        }
                    }
                }
                mAdapter.notifyDataSetChanged();
            }
        });
        inviteList = (ListView) findViewById(R.id.invitelist);
        mAdapter = new ListViewAdapter();

        inviteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (isSearch) {
                    mCurrentMucMembers.get(position).setChecked(mCurrentMucMembers.get(position).isChecked ? false : true);
                    for (int i = 0; i < mMucMembers.size(); i++) {
                        if (mMucMembers.get(i).getMember().getUserId().
                                equals(mCurrentMucMembers.get(position).getMember().getUserId())) {
                            mMucMembers.get(i).setChecked(mCurrentMucMembers.get(position).isChecked ? true : false);
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                } else {
                    mCurrentMucMembers.get(position).setChecked(mCurrentMucMembers.get(position).isChecked ? false : true);
                    mMucMembers.get(position).setChecked(mCurrentMucMembers.get(position).isChecked ? true : false);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void initData() {
        mMucMembers = new ArrayList<>();
        mCurrentMucMembers = new ArrayList<>();

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {
                    @Override
                    public void onResponse(ObjectResult<MucRoom> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            MucRoom mucRoom = result.getData();
                            List<MucRoomMember> mucRoomMembers = mucRoom.getMembers();
                            for (int i = 0; i < mucRoomMembers.size(); i++) {
                                if (!mucRoomMembers.get(i).getUserId().equals(mLoginUserId)) {// 移除自己
                                    SelectMuMembers selectMuMembers = new SelectMuMembers();
                                    selectMuMembers.setMember(mucRoom.getMembers().get(i));
                                    selectMuMembers.setChecked(false);
                                    mMucMembers.add(selectMuMembers);
                                    mCurrentMucMembers.add(selectMuMembers);
                                }
                            }
                            inviteList.setAdapter(mAdapter);
                        } else {
                            DialogHelper.dismissProgressDialog();
                            Toast.makeText(JitsiInviteActivity.this, "获取列表失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(getApplicationContext());
                    }
                });
    }

    class ListViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mCurrentMucMembers.size();
        }

        @Override
        public Object getItem(int position) {
            return mCurrentMucMembers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_select_contacts_jitsi, parent, false);
            }
            ImageView invite_avatar = ViewHolder.get(convertView, R.id.invite_avatar);
            TextView invite_name = ViewHolder.get(convertView, R.id.invite_name);
            CheckBox invite_ck = ViewHolder.get(convertView, R.id.invite_ck);
            Glide.with(mContext)
                    .load(AvatarHelper.getAvatarUrl(mCurrentMucMembers.get(position).member.getUserId(), true))
                    .asBitmap()
                    .placeholder(R.drawable.avatar_normal)
                    .error(R.drawable.avatar_normal)
                    .dontAnimate()
                    .into(invite_avatar);

            invite_name.setText(mCurrentMucMembers.get(position).getMember().getNickName());

            if (mCurrentMucMembers.get(position).isChecked) {
                invite_ck.setChecked(true);
            } else {
                invite_ck.setChecked(false);
            }
            return convertView;
        }
    }

    class SelectMuMembers {
        private MucRoomMember member;
        private boolean isChecked;

        public MucRoomMember getMember() {
            return member;
        }

        public void setMember(MucRoomMember member) {
            this.member = member;
        }

        public boolean isChecked() {
            return isChecked;
        }

        public void setChecked(boolean checked) {
            isChecked = checked;
        }
    }
}
