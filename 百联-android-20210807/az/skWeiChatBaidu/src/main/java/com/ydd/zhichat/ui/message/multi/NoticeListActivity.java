package com.ydd.zhichat.ui.message.multi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.roamer.slidelistview.SlideBaseAdapter;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.message.MucRoom;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.util.TimeUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.util.ViewHolder;
import com.ydd.zhichat.view.PullToRefreshSlideListView;
import com.ydd.zhichat.view.TipDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

import static com.ydd.zhichat.AppConstant.NOTICE_ID;
import static com.ydd.zhichat.AppConstant.PROCLAMATION;

/**
 * 群公告列表
 */
public class NoticeListActivity extends BaseActivity {
    private PullToRefreshSlideListView mListView;
    private NoticeAdapter mNoticeAdapter;
    private List<MucRoom.Notice> mNoticeList;
    private int mRole;
    private String mRoomId;

    private boolean isNeedUpdate;// 回到群组信息界面是否需要刷新ui

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_list);
        List<String> mNoticeIdList = JSON.parseArray(getIntent().getStringExtra("mNoticeIdList"), String.class);
        List<String> mNoticeUserIdList = JSON.parseArray(getIntent().getStringExtra("mNoticeUserIdList"), String.class);
        List<String> mNoticeNickNameIdList = JSON.parseArray(getIntent().getStringExtra("mNoticeNickNameIdList"), String.class);
        List<Long> mNoticeTimeList = JSON.parseArray(getIntent().getStringExtra("mNoticeTimeList"), Long.class);
        List<String> mNoticeTextList = JSON.parseArray(getIntent().getStringExtra("mNoticeTextList"), String.class);

        mRole = getIntent().getIntExtra("mRole", 3);
        mRoomId = getIntent().getStringExtra("mRoomId");
        mNoticeList = new ArrayList<>();
        for (int i = mNoticeNickNameIdList.size() - 1; i >= 0; i--) {
            MucRoom.Notice mNotice = new MucRoom.Notice();
            mNotice.setId(mNoticeIdList.get(i));
            mNotice.setUserId(mNoticeUserIdList.get(i));
            mNotice.setNickname(mNoticeNickNameIdList.get(i));
            mNotice.setTime(mNoticeTimeList.get(i));
            mNotice.setText(mNoticeTextList.get(i));
            mNoticeList.add(mNotice);
        }
        initActionBar();
        initView();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("isNeedUpdate", isNeedUpdate);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("isNeedUpdate", isNeedUpdate);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(R.string.group_bulletin);
        TextView mTvTitleRight = (TextView) findViewById(R.id.tv_title_right);
        mTvTitleRight.setText(R.string.btn_public);
        mTvTitleRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRole == 1 || mRole == 2) {
                    /*VerifyDialog verifyDialog = new VerifyDialog(NoticeListActivity.this);
                    verifyDialog.setVerifyClickListener(getString(R.string.btn_public_bulletin), new VerifyDialog.VerifyClickListener() {
                        @Override
                        public void cancel() {

                        }

                        @Override
                        public void send(String str) {
                            updateNotice(str);
                        }
                    });
                    verifyDialog.show();*/
                    startActivityForResult(new Intent(NoticeListActivity.this, ProclamationActivity.class), PROCLAMATION);
                } else {
                    ToastUtil.showToast(NoticeListActivity.this, R.string.tip_cannot_public_bulletin);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PROCLAMATION) {
            switch (resultCode) {
                case RESULT_OK:
                    String str = data.getStringExtra("proclamation");
                    updateNotice(str);
                    break;
                case NOTICE_ID:
                    String id = data.getStringExtra("noticeId");
                    String content = data.getStringExtra("proclamation");
                    String oldContent = PreferenceUtils.getString(NoticeListActivity.this, id);
                    if (!content.equals(oldContent))
                        editNotice(id, content);
                    break;
            }
        }
    }

    private void initView() {
        mListView = (PullToRefreshSlideListView) findViewById(R.id.pull_refresh_list);
        mNoticeAdapter = new NoticeAdapter(this);
        mListView.setAdapter(mNoticeAdapter);
        if (mNoticeList.size() == 0) {
            mListView.setVisibility(View.GONE);
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
        }
    }

    private void updateNotice(final String text) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);
        params.put("notice", text);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            isNeedUpdate = true;
                            MucRoom.Notice notice = new MucRoom.Notice();
                            notice.setUserId(coreManager.getSelf().getUserId());
                            notice.setNickname(coreManager.getSelf().getNickName());
                            notice.setTime(TimeUtils.sk_time_current_time());
                            notice.setText(text);
                            if (!TextUtils.isEmpty(result.getResultMsg())) {
                                // 按理说公告发布成功后，服务端需要返回data过来，因为删除公告需要公告id，但服务端比较懒，直接将公告id返回到了resultMsg字段内，我们就这样取值吧
                                notice.setId(result.getResultMsg());
                                PreferenceUtils.putString(NoticeListActivity.this, notice.getId(), text);
                            }
                            mNoticeList.add(notice);
                            mNoticeAdapter.notifyDataSetChanged();
                            findViewById(R.id.empty).setVisibility(View.GONE);
                            mListView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void editNotice(String noticeId, String NewNotice) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);
        params.put("noticeId", noticeId);
        params.put("noticeContent", NewNotice);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_EDIT_NOTICE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Log.e("zx", "onResponse:  result");
                            isNeedUpdate = true;
                            for (int i = 0; i < mNoticeList.size(); i++) {
                                if (mNoticeList.get(i).getId().equals(noticeId)) {
                                    Log.e("zx", "onResponse: " + mNoticeList.get(i));
                                    mNoticeList.get(i).setText(NewNotice);
                                }
                            }
                            mNoticeAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void deleteNotice(final MucRoom.Notice notice) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);
        params.put("noticeId", notice.getId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_DELETE_NOTICE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            isNeedUpdate = true;
                            mNoticeList.remove(notice);
                            mNoticeAdapter.notifyDataSetChanged();

                            if (mNoticeList.size() <= 0) {
                                findViewById(R.id.empty).setVisibility(View.GONE);
                                mListView.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }


    class NoticeAdapter extends SlideBaseAdapter {

        public NoticeAdapter(Context context) {
            super(context);
        }

        @Override
        public int getFrontViewId(int position) {
            return R.layout.row_notice;
        }

        @Override
        public int getLeftBackViewId(int position) {
            return 0;
        }

        @Override
        public int getRightBackViewId(int position) {
            return R.layout.row_item_delete_style;
        }

        @Override
        public int getCount() {
            if (mNoticeList != null) {
                return mNoticeList.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mNoticeList != null) {
                return mNoticeList.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createConvertView(position);
            }
            ImageView mAvatar = ViewHolder.get(convertView, R.id.avatar_iv);
            TextView mName = ViewHolder.get(convertView, R.id.name_tv);
            TextView mTime = ViewHolder.get(convertView, R.id.time_tv);
            TextView mContent = ViewHolder.get(convertView, R.id.content_tv);
            LinearLayout mEdit = ViewHolder.get(convertView, R.id.item_edit_style1);

            LinearLayout mDeleteStyle1 = ViewHolder.get(convertView, R.id.item_delete_style1);
            if (mNoticeList.size() > 0) {
                final MucRoom.Notice mNotice = mNoticeList.get(mNoticeList.size() - 1 - position);// 根据时间倒序显示
                if (mNotice != null) {
                    AvatarHelper.getInstance().displayAvatar(mNotice.getUserId(), mAvatar);
                    mName.setText(mNotice.getNickname());
                    mTime.setText(TimeUtils.getFriendlyTimeDesc(NoticeListActivity.this, mNotice.getTime()));
                    mContent.setText(mNotice.getText());
                    mEdit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mRole == 1 || mRole == 2) {
                                Intent intent = new Intent(NoticeListActivity.this, ProclamationActivity.class);
                                intent.putExtra("noticeId", mNotice.getId());
                                startActivityForResult(intent, PROCLAMATION);
                            } else {
                                TipDialog tipDialog = new TipDialog(NoticeListActivity.this);
                                tipDialog.setTip(getString(R.string.tip_cannot_edit_bulletin));
                                tipDialog.show();
                            }
                        }
                    });
                    mDeleteStyle1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mRole == 1 || mRole == 2) {
                                deleteNotice(mNotice);
                            } else {
                                TipDialog tipDialog = new TipDialog(NoticeListActivity.this);
                                tipDialog.setTip(getString(R.string.tip_cannot_remove_bulletin));
                                tipDialog.show();
                            }
                        }
                    });
                }
            }
            return convertView;
        }
    }
}
