package com.ydd.zhichat.ui.other;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.signature.StringSignature;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.AddAttentionResult;
import com.ydd.zhichat.bean.Area;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.bean.Label;
import com.ydd.zhichat.bean.Report;
import com.ydd.zhichat.bean.User;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.bean.message.NewFriendMessage;
import com.ydd.zhichat.bean.message.XmppMessage;
import com.ydd.zhichat.broadcast.CardcastUiUpdateUtil;
import com.ydd.zhichat.broadcast.MsgBroadcast;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.db.dao.ChatMessageDao;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.db.dao.LabelDao;
import com.ydd.zhichat.db.dao.NewFriendDao;
import com.ydd.zhichat.db.dao.UserAvatarDao;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.helper.FriendHelper;
import com.ydd.zhichat.helper.UsernameHelper;
import com.ydd.zhichat.ui.MainActivity;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.circle.BusinessCircleActivity;
import com.ydd.zhichat.ui.map.MapActivity;
import com.ydd.zhichat.ui.message.ChatActivity;
import com.ydd.zhichat.ui.message.single.SetRemarkActivity;
import com.ydd.zhichat.ui.tool.SingleImagePreviewActivity;
import com.ydd.zhichat.util.TimeUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.BasicInfoWindow;
import com.ydd.zhichat.view.NoDoubleClickListener;
import com.ydd.zhichat.view.ReportDialog;
import com.ydd.zhichat.view.SelectionFrame;
import com.ydd.zhichat.xmpp.ListenerManager;
import com.ydd.zhichat.xmpp.listener.ChatMessageListener;
import com.ydd.zhichat.xmpp.listener.NewFriendListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;

/**
 * 基本信息next_step_btn
 */
public class BasicInfoActivity extends BaseActivity implements NewFriendListener {
    public static final String KEY_FROM_ADD_TYPE = "KEY_FROM_ADD_TYPE";
    public static final int FROM_ADD_TYPE_QRCODE = 1;
    public static final int FROM_ADD_TYPE_CARD = 2;
    public static final int FROM_ADD_TYPE_GROUP = 3;
    public static final int FROM_ADD_TYPE_PHONE = 4;
    public static final int FROM_ADD_TYPE_NAME = 5;
    public static final int FROM_ADD_TYPE_OTHER = 6;
    private static final int REQUEST_CODE_SET_REMARK = 475;
    private String fromAddType;

    private String mUserId;
    private String mLoginUserId;
    private boolean isMyInfo = false;
    private User mUser;
    private Friend mFriend;

    private ImageView ivRight;
    private BasicInfoWindow menuWindow;

    private ImageView mAvatarImg;
    private TextView tv_remarks;
    private ImageView iv_remarks;
    private LinearLayout ll_nickname;
    private TextView tv_name_basic;
    private TextView tv_communication;
    private TextView tv_number;
    private LinearLayout ll_place;
    private TextView tv_place;
    private TextView photo_tv;
    private RelativeLayout photo_rl;

    private RelativeLayout mRemarkLayout;
    private TextView tv_setting_name;
    private TextView tv_lable_basic;
    private RelativeLayout rl_describe;
    private TextView tv_describe_basic;
    private TextView birthday_tv;
    private TextView online_tv;
    private RelativeLayout online_rl;
    private RelativeLayout look_location_rl;
    private RelativeLayout erweima;

    private Button mNextStepBtn;

    /**
     * Todo All NewFriendMessage packetId
     */
    private String addhaoyouid = null;
    private String addblackid = null;
    private String removeblack = null;
    private String deletehaoyou = null;

    private int isyanzheng = 0;// 该好友是否需要验证

    // 为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            menuWindow.dismiss();
            if (mFriend == null) {
                mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
            }
            switch (v.getId()) {
                case R.id.set_remark_nameS:
                    start();
                    break;
                case R.id.add_blacklist:
                    // 加入黑名单
                    showBlacklistDialog(mFriend);
                    break;
                case R.id.remove_blacklist:
                    // 移除黑名单
                    removeBlacklist(mFriend);
                    break;
                case R.id.delete_tv:
                    // 彻底删除
                    showDeleteAllDialog(mFriend);
                    break;
                case R.id.report_tv:
                    ReportDialog mReportDialog = new ReportDialog(BasicInfoActivity.this, false, new ReportDialog.OnReportListItemClickListener() {
                        @Override
                        public void onReportItemClick(Report report) {
                            report(mUserId, report);
                        }
                    });
                    mReportDialog.show();
                    break;
            }
        }
    };


    public static void start(Context ctx, String userId) {
        Intent intent = new Intent(ctx, BasicInfoActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        ctx.startActivity(intent);
    }

    public static void start(Context ctx, String userId, int fromAddType) {
        Intent intent = new Intent(ctx, BasicInfoActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        intent.putExtra(KEY_FROM_ADD_TYPE, String.valueOf(fromAddType));
        ctx.startActivity(intent);
    }

    private void start() {
        String name = "";
        String desc = "";
        if (mUser != null && mUser.getFriends() != null) {
            name = mUser.getFriends().getRemarkName();
            desc = mUser.getFriends().getDescribe();
        }
        SetRemarkActivity.startForResult(BasicInfoActivity.this, mUserId, name, desc, REQUEST_CODE_SET_REMARK);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_info_new);
        if (getIntent() != null) {
            mUserId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
            fromAddType = getIntent().getStringExtra(KEY_FROM_ADD_TYPE);
        }

        mLoginUserId = coreManager.getSelf().getUserId();
        if (TextUtils.isEmpty(mUserId)) {
            mUserId = mLoginUserId;
        }
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
        initActionBar();
        initView();

        if (mUserId.equals(Friend.ID_SYSTEM_MESSAGE)) {// 客服公众号
            findViewById(R.id.part_1).setVisibility(View.VISIBLE);
            findViewById(R.id.part_2).setVisibility(View.GONE);
            findViewById(R.id.go_publish_tv).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mFriend != null) {
                        Intent intent = new Intent(BasicInfoActivity.this, ChatActivity.class);
                        intent.putExtra(ChatActivity.FRIEND, mFriend);
                        startActivity(intent);
                    } else {
                        Toast.makeText(BasicInfoActivity.this, R.string.tip_not_like_public_number, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return;
        }
        initEvent();

        if (mLoginUserId.equals(mUserId)) { // 显示自己资料
            isMyInfo = true;
            loadMyInfoFromDb();
        } else { // 显示其他用户的资料
            isMyInfo = false;
            loadOthersInfoFromNet();
        }

        ListenerManager.getInstance().addNewFriendListener(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JX_BaseInfo"));

        ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.title_moress);
    }

    private void initView() {
        UsernameHelper.initTextView(findViewById(R.id.photo_text), coreManager.getConfig().registerUsername);
        mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
        tv_remarks = (TextView) findViewById(R.id.tv_remarks);
        iv_remarks = findViewById(R.id.iv_remarks);
        ll_nickname = findViewById(R.id.ll_nickname);
        tv_name_basic = (TextView) findViewById(R.id.tv_name_basic);
        tv_communication = (TextView) findViewById(R.id.tv_communication);
        tv_number = (TextView) findViewById(R.id.tv_number);
        ll_place = findViewById(R.id.ll_place);
        tv_place = (TextView) findViewById(R.id.tv_place);

        mRemarkLayout = findViewById(R.id.rn_rl);
        tv_setting_name = findViewById(R.id.tv_setting_name);
        tv_lable_basic = findViewById(R.id.tv_lable_basic);
        rl_describe = findViewById(R.id.rl_describe);
        tv_describe_basic = findViewById(R.id.tv_describe_basic);
        birthday_tv = (TextView) findViewById(R.id.birthday_tv);
        online_rl = (RelativeLayout) findViewById(R.id.online_rl);
        online_tv = (TextView) findViewById(R.id.online_tv);
        erweima = (RelativeLayout) findViewById(R.id.erweima);
        look_location_rl = (RelativeLayout) findViewById(R.id.look_location_rl);
        photo_tv = findViewById(R.id.photo_tv);
        photo_rl = (RelativeLayout) findViewById(R.id.photo_rl);

        mNextStepBtn = (Button) findViewById(R.id.next_step_btn);
//        mNextStepBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));

        if (coreManager.getConfig().disableLocationServer) {
            ll_place.setVisibility(View.GONE);
        }
    }

    private void initEvent() {
        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuWindow = new BasicInfoWindow(BasicInfoActivity.this, itemsOnClick, mFriend);
                // 显示窗口
                menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                // +x右,-x左,+y下,-y上
                // pop向左偏移显示
                menuWindow.showAsDropDown(view,
                        -(menuWindow.getContentView().getMeasuredWidth() - view.getWidth() / 2 - 40),
                        0);
            }
        });

        mAvatarImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
                intent.putExtra(AppConstant.EXTRA_IMAGE_URI, mUserId);
                startActivity(intent);
            }
        });

        mRemarkLayout.setOnClickListener(v -> {
            start();
        });

        rl_describe.setOnClickListener(v -> {
            start();
        });

        findViewById(R.id.look_bussic_cicle_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUser != null) {
                    Intent intent = new Intent(BasicInfoActivity.this, BusinessCircleActivity.class);
                    intent.putExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_PERSONAL_SPACE);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, mUserId);
                    intent.putExtra(AppConstant.EXTRA_NICK_NAME, mUser.getNickName());
                    startActivity(intent);
                }
            }
        });

        //TODO 这个item已经隐藏掉了  因为现在的用户基本消息不是很多 如果再增加的话  可以打开此item 另开一个activity
        findViewById(R.id.rl_more_basic).setOnClickListener(v -> {
            Intent intent = new Intent(mContext, MoreInfoActivity.class);
            intent.putExtra("user", mUser);
            startActivity(intent);
        });

        erweima.setOnClickListener(v -> {
            if (mUser != null) {
                Intent intent = new Intent(BasicInfoActivity.this, QRcodeActivity.class);
                intent.putExtra("isgroup", false);
                if (!TextUtils.isEmpty(mUser.getAccount())) {
                    intent.putExtra("userid", mUser.getAccount());
                } else {
                    intent.putExtra("userid", mUser.getUserId());
                }
                intent.putExtra("userAvatar", mUser.getUserId());
                intent.putExtra("userName", mUser.getNickName());
                startActivity(intent);
            }
        });

        look_location_rl.setOnClickListener(view -> {
            double latitude = 0;
            double longitude = 0;
            if (mUser != null && mUser.getLoc() != null) {
                latitude = mUser.getLoc().getLat();
                longitude = mUser.getLoc().getLng();
            }
            if (latitude == 0 || longitude == 0) {
                ToastUtil.showToast(mContext, getString(R.string.this_friend_not_open_position));
                return;
            }
            Intent intent = new Intent(mContext, MapActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("address", mUser.getNickName());
            startActivity(intent);
        });
    }

    // 加载自己的信息
    private void loadMyInfoFromDb() {
        mUser = coreManager.getSelf();
        updateUI();
    }

    // 加载好友的信息
    private void loadOthersInfoFromNet() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mUserId);

        HttpUtils.get().url(coreManager.getConfig().USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {

                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (Result.checkSuccess(BasicInfoActivity.this, result)) {
                            mUser = result.getData();
                            if (mUser.getUserType() != 2) {// 公众号不做该操作 否则会将公众号的status变为好友
                                // 服务器的状态 与本地状态对比
                                if (FriendHelper.updateFriendRelationship(mLoginUserId, mUser)) {
                                    CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                                }
                            }
                            updateUI();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private void updateFriendName(User user) {
        if (user != null) {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
            if (friend != null) {
                FriendDao.getInstance().updateNickName(mLoginUserId, mUserId, user.getNickName());
            }
        }
    }

    private void updateUI() {
        if (mUser == null) {
            return;
        }
        if (isFinishing()) {
            return;
        }

        if (mFriend != null) {// 该人为我的好友，先赋值，接口成功后在更新ui
            List<Label> friendLabelList = LabelDao.getInstance().getFriendLabelList(mLoginUserId, mUserId);
            String labelNames = "";
            if (friendLabelList != null && friendLabelList.size() > 0) {
                for (int i = 0; i < friendLabelList.size(); i++) {
                    if (i == friendLabelList.size() - 1) {
                        labelNames += friendLabelList.get(i).getGroupName();
                    } else {
                        labelNames += friendLabelList.get(i).getGroupName() + "，";
                    }
                }
                tv_lable_basic.setText(labelNames);  //标签有则显示
                tv_setting_name.setText(getResources().getString(R.string.tag));
            } else {
                // 没有则判断描述 描述也为空则显示标签一栏
                if (TextUtils.isEmpty(mFriend.getDescribe())) {
                    tv_setting_name.setText(getResources().getString(R.string.setting_nickname));
                    tv_lable_basic.setText("");
                } else {
                    // 否则不出现这一栏
                    findViewById(R.id.rn_rl).setVisibility(View.GONE);
                }
            }
            // 描述不为空则显示出来  否则隐藏
            if (!TextUtils.isEmpty(mFriend.getDescribe())) {
                rl_describe.setVisibility(View.VISIBLE);
                tv_describe_basic.setText(mFriend.getDescribe());
            } else rl_describe.setVisibility(View.GONE);

            // 备注为空只显示昵称
            if (TextUtils.isEmpty(mFriend.getRemarkName())) {
                tv_remarks.setText(mFriend.getNickName());
                ll_nickname.setVisibility(View.GONE);
            } else {  // 备注不为空  显示备注  同时也显示昵称
                tv_remarks.setText(mFriend.getRemarkName());
                ll_nickname.setVisibility(View.VISIBLE);
                tv_name_basic.setText(mFriend.getNickName());
            }
        } else {
            tv_remarks.setText(mUser.getNickName());
            ll_nickname.setVisibility(View.GONE);
        }

        if (mUser.getShowLastLoginTime() > 0) {
            online_rl.setVisibility(View.VISIBLE);
            online_tv.setText(TimeUtils.getFriendlyTimeDesc(this, mUser.getShowLastLoginTime()));
        } else {
            online_rl.setVisibility(View.GONE);
        }
        if (TextUtils.isEmpty(mUser.getAccount())) {  //如果通讯号为空则不显示
            findViewById(R.id.ll_communication).setVisibility(View.GONE);
        } else {
            tv_number.setText(mUser.getAccount());  //否则显示通讯号
        }

        if (!TextUtils.isEmpty(mUser.getPhone())) {
            photo_tv.setText(mUser.getPhone());  //手机号不为空显示手机号
            photo_rl.setVisibility(View.VISIBLE);
        } else photo_rl.setVisibility(View.GONE);

        // 更新用户的头像
        AvatarHelper.updateAvatar(mUser.getUserId());
        displayAvatar(mUser.getUserId());

        updateFriendName(mUser);

        tv_remarks.setText(mUser.getNickName());
        tv_name_basic.setText(mUser.getNickName());
        if (mUser.getFriends() != null) {
            if (!TextUtils.isEmpty(mUser.getFriends().getRemarkName())) {
                tv_remarks.setText(mUser.getFriends().getRemarkName());
                ll_nickname.setVisibility(View.VISIBLE);// 有备注名，原昵称也显示
                tv_setting_name.setText(getString(R.string.tag));// 有备注 || 标签 显示“标签”
            } else {
                ll_nickname.setVisibility(View.GONE);
            }

            if (mFriend != null) {
                FriendDao.getInstance().updateFriendPartStatus(mFriend.getUserId(), mUser);

                if (!TextUtils.equals(mFriend.getRemarkName(), mUser.getFriends().getRemarkName())
                        || !TextUtils.equals(mFriend.getDescribe(), mUser.getFriends().getDescribe())) {
                    // 本地备注或者描述与服务器数据不匹配时更新数据库，
                    // mUser是接口数据，
                    mFriend.setRemarkName(mUser.getFriends().getRemarkName());
                    mFriend.setDescribe(mUser.getFriends().getDescribe());
                    FriendDao.getInstance().updateRemarkNameAndDescribe(coreManager.getSelf().getUserId(),
                            mUserId, mUser.getFriends().getRemarkName(),
                            mUser.getFriends().getDescribe());
                    // 刷新消息、通讯录、聊天页面
                    MsgBroadcast.broadcastMsgUiUpdate(mContext);
                    CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                    sendBroadcast(new Intent(com.ydd.zhichat.broadcast.OtherBroadcast.NAME_CHANGE));
                }
            }
        } else {
            ll_nickname.setVisibility(View.GONE);
        }
        iv_remarks.setImageResource(mUser.getSex() == 0 ? R.mipmap.basic_famale : R.mipmap.basic_male);

        if (TextUtils.isEmpty(mUser.getAccount())) {
            findViewById(R.id.ll_communication).setVisibility(View.GONE);
        } else {
            findViewById(R.id.ll_communication).setVisibility(View.VISIBLE);
            tv_number.setText(mUser.getAccount());
        }

        String place = Area.getProvinceCityString(mUser.getProvinceId(), mUser.getCityId());
        if (!TextUtils.isEmpty(place)) {
            ll_place.setVisibility(View.VISIBLE);
            tv_place.setText(place);
        } else {
//            ll_place.setVisibility(View.GONE);
        }

        List<Label> friendLabelList = LabelDao.getInstance().getFriendLabelList(mLoginUserId, mUserId);
        String labelNames = "";
        if (friendLabelList != null && friendLabelList.size() > 0) {
            for (int i = 0; i < friendLabelList.size(); i++) {
                if (i == friendLabelList.size() - 1) {
                    labelNames += friendLabelList.get(i).getGroupName();
                } else {
                    labelNames += friendLabelList.get(i).getGroupName() + "，";
                }
            }
            tv_setting_name.setText(getString(R.string.tag));// 有备注 || 标签 显示“标签”
            tv_lable_basic.setText(labelNames);
        }

        if (mUser.getFriends() != null && !TextUtils.isEmpty(mUser.getFriends().getDescribe())) {
            rl_describe.setVisibility(View.VISIBLE);
            tv_describe_basic.setText(mUser.getFriends().getDescribe());
        } else {
            rl_describe.setVisibility(View.GONE);
        }
        birthday_tv.setText(TimeUtils.sk_time_s_long_2_str(mUser.getBirthday()));
        if (mUser.getShowLastLoginTime() > 0) {
            online_rl.setVisibility(View.VISIBLE);
            online_tv.setText(TimeUtils.getFriendlyTimeDesc(this, mUser.getShowLastLoginTime()));
        } else {
            online_rl.setVisibility(View.GONE);
        }

        if (isMyInfo) {
            mNextStepBtn.setVisibility(View.GONE);
            findViewById(R.id.rn_rl).setVisibility(View.GONE);
            rl_describe.setVisibility(View.GONE);
        } else {
            mNextStepBtn.setVisibility(View.VISIBLE);
            if (mUser.getFriends() == null) {// 陌生人
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JX_AddFriend"));
                mNextStepBtn.setOnClickListener(new AddAttentionListener());
            } else if (mUser.getFriends().getBlacklist() == 1) {  //  需显示移除黑名单
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("REMOVE"));
                mNextStepBtn.setOnClickListener(new RemoveBlacklistListener());
            } else if (mUser.getFriends().getIsBeenBlack() == 1) {//  需显示加入黑名单
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("TO_BLACKLIST"));
            } else if (mUser.getFriends().getStatus() == 2 || mUser.getFriends().getStatus() == 4) {// 好友
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.VISIBLE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));
                mNextStepBtn.setOnClickListener(new SendMsgListener());
            } else {
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JX_AddFriend"));
                mNextStepBtn.setOnClickListener(new AddAttentionListener());
            }
        }
    }

    public void displayAvatar(final String userId) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        final String mOriginalUrl = AvatarHelper.getAvatarUrl(userId, false);
        if (!TextUtils.isEmpty(mOriginalUrl)) {
            String time = UserAvatarDao.getInstance().getUpdateTime(userId);

            Glide.with(MyApplication.getContext())
                    .load(mOriginalUrl)
                    .placeholder(R.drawable.avatar_normal)
                    .signature(new StringSignature(time))
                    .dontAnimate()
                    .error(R.drawable.avatar_normal)
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            DialogHelper.dismissProgressDialog();
                            mAvatarImg.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            DialogHelper.dismissProgressDialog();
                            Log.e("zq", "加载原图失败：" + mOriginalUrl);// 该用户未设置头像，网页访问该URL也是404
                            if (mUser.getFriends() != null && !TextUtils.isEmpty(mUser.getFriends().getRemarkName())) {
                                AvatarHelper.getInstance().displayAvatar(mUser.getFriends().getRemarkName(), mUser.getUserId(), mAvatarImg, true);
                            } else {
                                AvatarHelper.getInstance().displayAvatar(mUser.getNickName(), mUser.getUserId(), mAvatarImg, true);
                            }

                        }
                    });
        } else {
            DialogHelper.dismissProgressDialog();
            Log.e("zq", "未获取到原图地址");// 基本上不会走这里
        }
    }

    @Override
    public void onNewFriendSendStateChange(String toUserId, NewFriendMessage message, int messageState) {
        if (messageState == ChatMessageListener.MESSAGE_SEND_SUCCESS) {
            msgSendSuccess(message, message.getPacketId());
        } else if (messageState == ChatMessageListener.MESSAGE_SEND_FAILED) {
            msgSendFailed(message.getPacketId());
        }
    }

    @Override
    public boolean onNewFriend(NewFriendMessage message) {
        if (!TextUtils.equals(mUserId, mLoginUserId)
                && TextUtils.equals(message.getUserId(), mUserId)) {// 当前对象正在对我进行操作
            loadOthersInfoFromNet();
            return false;
        }
        if (message.getType() == XmppMessage.TYPE_PASS) {// 对方同意好友请求 更新当前界面
            loadOthersInfoFromNet();
        }
        return false;
    }

    // xmpp消息发送成功最终回调到这，
    // 在这里调整ui,
    // 还有存本地数据库，
    public void msgSendSuccess(NewFriendMessage message, String packet) {
        if (addhaoyouid != null && addhaoyouid.equals(packet)) {
            if (isyanzheng == 0) {// 需要验证
                Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("JXAlert_SayHiOK"), Toast.LENGTH_SHORT).show();
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);

                ChatMessage sayChatMessage = new ChatMessage();
                sayChatMessage.setContent(InternationalizationHelper.getString("JXFriendObject_WaitPass"));
                sayChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                // 各种Dao都是本地数据库的操作，
                // 这个是朋友表里更新最后一条消息，
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, sayChatMessage);

                // 这个是新的朋友界面用的，等验证什么的都在新的朋友页面，
                NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_10);// 朋友状态
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);
            } else if (isyanzheng == 1) {
                Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("JX_AddSuccess"), Toast.LENGTH_SHORT).show();
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.VISIBLE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));
                mNextStepBtn.setOnClickListener(new SendMsgListener());

                NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_FRIEND);
                FriendHelper.addFriendExtraOperation(mLoginUserId, mUser.getUserId());// 加好友

                ChatMessage addChatMessage = new ChatMessage();
                addChatMessage.setContent(InternationalizationHelper.getString("JXNearVC_AddFriends") + ":" + mUser.getNickName());
                addChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, addChatMessage);

                NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_22);//添加了xxx
                FriendDao.getInstance().updateFriendContent(mLoginUserId, mUser.getUserId(), InternationalizationHelper.getString("JXMessageObject_BeFriendAndChat"), XmppMessage.TYPE_TEXT, TimeUtils.sk_time_current_time());
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

                loadOthersInfoFromNet();
                CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
            }
            // 已经是好友了，mFriend不能为空，
            mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
        } else if (addblackid != null && addblackid.equals(packet)) {
            Toast.makeText(getApplicationContext(), getString(R.string.add_blacklist_succ), Toast.LENGTH_SHORT).show();
            findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
            mNextStepBtn.setText(InternationalizationHelper.getString("REMOVE"));
            mNextStepBtn.setOnClickListener(new RemoveBlacklistListener());

            // 更新当前持有的Friend对象，
            mFriend.setStatus(Friend.STATUS_BLACKLIST);
            FriendDao.getInstance().updateFriendStatus(message.getOwnerId(), message.getUserId(), mFriend.getStatus());
            FriendHelper.addBlacklistExtraOperation(message.getOwnerId(), message.getUserId());

            ChatMessage addBlackChatMessage = new ChatMessage();
            addBlackChatMessage.setContent(InternationalizationHelper.getString("JXFriendObject_AddedBlackList") + " " + mUser.getNickName());
            addBlackChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, addBlackChatMessage);

            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_18);
            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (removeblack != null && removeblack.equals(packet)) {
            Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("REMOVE_BLACKLIST"), Toast.LENGTH_SHORT).show();
            findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.VISIBLE);
            mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));
            mNextStepBtn.setOnClickListener(new SendMsgListener());

            // 更新当前持有的Friend对象，
            if (mFriend != null) {
                mFriend.setStatus(Friend.STATUS_FRIEND);
            }
            NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_FRIEND);
            FriendHelper.beAddFriendExtraOperation(message.getOwnerId(), message.getUserId());

            ChatMessage removeChatMessage = new ChatMessage();
            removeChatMessage.setContent(coreManager.getSelf().getNickName() + InternationalizationHelper.getString("REMOVE"));
            removeChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, removeChatMessage);

            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(message.getUserId(), Friend.STATUS_24);
            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            loadOthersInfoFromNet();
        } else if (deletehaoyou != null && deletehaoyou.equals(packet)) {
            Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("JXAlert_DeleteOK"), Toast.LENGTH_SHORT).show();

            FriendHelper.removeAttentionOrFriend(mLoginUserId, message.getUserId());

            ChatMessage deleteChatMessage = new ChatMessage();
            deleteChatMessage.setContent(InternationalizationHelper.getString("JXAlert_DeleteFirend") + " " + mUser.getNickName());
            deleteChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, deleteChatMessage);

            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_16);
            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void msgSendFailed(String packet) {
        DialogHelper.dismissProgressDialog();
        if (packet.equals(addhaoyouid)) {
            Toast.makeText(this, R.string.tip_hello_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(addblackid)) {
            Toast.makeText(this, R.string.tip_put_black_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(removeblack)) {
            Toast.makeText(this, R.string.tip_remove_black_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(deletehaoyou)) {
            Toast.makeText(this, R.string.tip_remove_friend_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Todo NextStep && ivRight Operating
     */

    // 点击加好友就调用这个方法，
    private void doAddAttention() {
        if (mUser == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mUser.getUserId());
        if (TextUtils.isEmpty(fromAddType)) {
            // 默认就是其他方式，
            fromAddType = String.valueOf(FROM_ADD_TYPE_OTHER);
        }
        params.put("fromAddType", fromAddType);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        // 首先是调接口，
        HttpUtils.get().url(coreManager.getConfig().FRIENDS_ATTENTION_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<AddAttentionResult>(AddAttentionResult.class) {

                    @Override
                    public void onResponse(ObjectResult<AddAttentionResult> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            if (result.getData().getType() == 1 || result.getData().getType() == 3) {
                                isyanzheng = 0;// 需要验证
                                // 需要验证就发送打招呼的消息，
                                doSayHello(InternationalizationHelper.getString("JXUserInfoVC_Hello"));
                            } else if (result.getData().getType() == 2 || result.getData().getType() == 4) {// 已经是好友了
                                isyanzheng = 1;// 不需要验证
                                NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                        coreManager.getSelf(), XmppMessage.TYPE_FRIEND, null, mUser);
                                NewFriendDao.getInstance().createOrUpdateNewFriend(message);
                                // 不需要验证的话直接加上，就发个xmpp消息，
                                // 这里最终调用smack的方法发送xmpp消息，
                                coreManager.sendNewFriendMessage(mUser.getUserId(), message);

                                addhaoyouid = message.getPacketId();
                            } else if (result.getData().getType() == 5) {
                                ToastUtil.showToast(mContext, R.string.add_attention_failed);
                            }
                        } else {
                            Toast.makeText(BasicInfoActivity.this, result.getResultMsg() + "", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_hello_failed, Toast.LENGTH_SHORT).show();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    // 打招呼
    private void doSayHello(String text) {
        if (TextUtils.isEmpty(text)) {
            text = InternationalizationHelper.getString("HEY-HELLO");
        }
        NewFriendMessage message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(),
                XmppMessage.TYPE_SAYHELLO, text, mUser);
        NewFriendDao.getInstance().createOrUpdateNewFriend(message);
        // 这里最终调用smack的发送消息，
        coreManager.sendNewFriendMessage(mUser.getUserId(), message);

        addhaoyouid = message.getPacketId();

        // 发送打招呼的消息
        ChatMessage sayMessage = new ChatMessage();
        sayMessage.setFromUserId(coreManager.getSelf().getUserId());
        sayMessage.setFromUserName(coreManager.getSelf().getNickName());
        sayMessage.setContent(InternationalizationHelper.getString("HEY-HELLO"));
        sayMessage.setType(XmppMessage.TYPE_TEXT); //文本类型
        sayMessage.setMySend(true);
        sayMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
        sayMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        sayMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        ChatMessageDao.getInstance().saveNewSingleChatMessage(message.getOwnerId(), message.getUserId(), sayMessage);
    }

    // 加入、移除黑名单
    private void showBlacklistDialog(final Friend friend) {
/*
        if (friend.getStatus() == Friend.STATUS_BLACKLIST) {
            removeBlacklist(friend);
        } else if (friend.getStatus() == Friend.STATUS_ATTENTION || friend.getStatus() == Friend.STATUS_FRIEND) {
            addBlacklist(friend);
        }
*/
        SelectionFrame mSF = new SelectionFrame(this);
        mSF.setSomething(getString(R.string.add_black_list), getString(R.string.sure_add_friend_blacklist), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                addBlacklist(friend);
            }
        });
        mSF.show();
    }

    private void addBlacklist(final Friend friend) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            if (friend.getStatus() == Friend.STATUS_FRIEND) {
                                NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                        coreManager.getSelf(), XmppMessage.TYPE_BLACK, null, friend);
                                coreManager.sendNewFriendMessage(friend.getUserId(), message);// 加入黑名单

                                // 记录加入黑名单消息packet，如收到消息回执，在做后续操作
                                addblackid = message.getPacketId();
                            }
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, getString(R.string.add_blacklist_fail), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeBlacklist(final Friend friend) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mUser.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                    coreManager.getSelf(), XmppMessage.TYPE_REFUSED, null, friend);
                            coreManager.sendNewFriendMessage(friend.getUserId(), message);// 移除黑名单

                            // 记录移除黑名单消息packet，如收到消息回执，在做后续操作
                            removeblack = message.getPacketId();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_remove_black_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 删除好友
    private void showDeleteAllDialog(final Friend friend) {
        if (friend.getStatus() == Friend.STATUS_UNKNOW) {// 陌生人
            return;
        }
        SelectionFrame mSF = new SelectionFrame(this);
        mSF.setSomething(getString(R.string.delete_friend), getString(R.string.sure_delete_friend), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                deleteFriend(friend, 1);
            }
        });
        mSF.show();
    }

    private void deleteFriend(final Friend friend, final int type) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                    coreManager.getSelf(), XmppMessage.TYPE_DELALL, null, friend);
                            coreManager.sendNewFriendMessage(mUser.getUserId(), message); // 删除好友

                            // 记录删除好友消息packet，如收到消息回执，在做后续操作
                            deletehaoyou = message.getPacketId();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_remove_friend_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void report(String userId, Report report) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", userId);
        params.put("reason", String.valueOf(report.getReportId()));
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_REPORT)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            ToastUtil.showToast(BasicInfoActivity.this, R.string.report_success);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    protected void onDestroy() {
        super.onDestroy();
        ListenerManager.getInstance().removeNewFriendListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SET_REMARK) {
            loadOthersInfoFromNet();
        }
    }

    // 加关注
    private class AddAttentionListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            doAddAttention();
        }
    }

    // 移除黑名单  直接变为好友  不需要验证
    private class RemoveBlacklistListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUser.getUserId());// 更新好友的状态
            removeBlacklist(friend);
        }
    }

    // 发消息
    private class SendMsgListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUser.getUserId());
            MsgBroadcast.broadcastMsgUiUpdate(BasicInfoActivity.this);
            MsgBroadcast.broadcastMsgNumReset(BasicInfoActivity.this);

            Intent intent = new Intent(mContext, ChatActivity.class);
            intent.putExtra(ChatActivity.FRIEND, friend);
            startActivity(intent);
        }
    }
}