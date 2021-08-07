package com.ydd.zhichat.ui.account;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.bean.AttentionUser;
import com.ydd.zhichat.bean.Contact;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.bean.Label;
import com.ydd.zhichat.bean.User;
import com.ydd.zhichat.bean.message.MucRoom;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.db.dao.ContactDao;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.db.dao.LabelDao;
import com.ydd.zhichat.db.dao.OnCompleteListener2;
import com.ydd.zhichat.db.dao.UserDao;
import com.ydd.zhichat.helper.LoginHelper;
import com.ydd.zhichat.sp.UserSp;
import com.ydd.zhichat.ui.MainActivity;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.share.AuthorizationActivity;
import com.ydd.zhichat.ui.share.ShareConstant;
import com.ydd.zhichat.ui.share.ShareNearChatFriend;
import com.ydd.zhichat.util.AsyncUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.DataLoadView;
import com.ydd.zhichat.view.TipDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.List;

import okhttp3.Call;


/**
 * 数据下载页面
 */
public class DataDownloadActivity extends BaseActivity {
    private final int STATUS_NO_RESULT = 0;// 请求中，尚未返回
    private final int STATUS_FAILED = 1;// 已经返回，失败了
    private final int STATUS_SUCCESS = 2;// 已经返回，成功了
    private DataLoadView mDataLoadView;
    // 好友列表保存本地数据库的进度条，
    private NumberProgressBar mNumberProgressBar;
    private NumberProgressBar mNumberProgressBarRoom;
    private String mLoginUserId;
    private Handler mHandler;
    private int user_info_download_status = STATUS_NO_RESULT;// 个人基本资料下载
    private int user_contact_download_status = STATUS_NO_RESULT;// 我的联系人下载
    private int user_friend_download_status = STATUS_NO_RESULT;// 我的好友下载
    private int user_label_download_status = STATUS_NO_RESULT;// 我的标签下载
    private int user_room_download_status = STATUS_NO_RESULT;// 我的群组下载

    private int isupdate;
    // 用于确保只更新一百次，
    private int lastRate = -1;
    private int lastRateRoom = -1;

    public static void start(Context ctx, int isupdate) {
        Intent intent = new Intent(ctx, DataDownloadActivity.class);
        intent.putExtra("isupdate", isupdate);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_download);

        // 进入下载资料界面，就将该值赋值false
        UserSp.getInstance(DataDownloadActivity.this).setUpdate(false);
        mLoginUserId = coreManager.getSelf().getUserId();
        mHandler = new Handler();

        isupdate = getIntent().getIntExtra("isupdate", 1);
        List<Friend> friendList = FriendDao.getInstance().getAllFriends(mLoginUserId);
        if (isupdate == 0 && friendList.size() > 0) {
            // 之前没有好友操作 && 本地好友数量大于0，将user_friend_download_status置为STATUS_SUCCESS(即不去服务器获取好友列表)
            findViewById(R.id.ll1).setVisibility(View.GONE);
            user_friend_download_status = STATUS_SUCCESS;
        }

        initActionBar();
        initView();
        startDownload();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doBack();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.data_update);
    }

    private void initView() {
        mDataLoadView = (DataLoadView) findViewById(R.id.data_load_view);
        mDataLoadView.setLoadingEvent(new DataLoadView.LoadingEvent() {
            @Override
            public void load() {
                startDownload();
            }
        });
        mNumberProgressBar = (NumberProgressBar) findViewById(R.id.number_progress_bar);
        mNumberProgressBarRoom = (NumberProgressBar) findViewById(R.id.number_progress_bar_room);
    }

    private void startDownload() {
        findViewById(R.id.ll).setVisibility(View.VISIBLE);
        mDataLoadView.setVisibility(View.GONE);
        mDataLoadView.showLoading();
        if (user_info_download_status != STATUS_SUCCESS) {
            downloadUserInfo();
        }

        if (user_contact_download_status != STATUS_SUCCESS) {
            downloadUserAddressBook();
        }

        if (user_friend_download_status != STATUS_SUCCESS) {
            downloadUserFriend();
        }

        if (user_label_download_status != STATUS_SUCCESS) {
            downloadUserLabel();
        }

        if (user_room_download_status != STATUS_SUCCESS) {
            downloadRoom();
        }
    }

    private void endDownload() {
        // 只要有一个下载没返回，那么就继续等待
        if (user_info_download_status == STATUS_NO_RESULT || user_contact_download_status == STATUS_NO_RESULT
                || user_friend_download_status == STATUS_NO_RESULT || user_label_download_status == STATUS_NO_RESULT
                || user_room_download_status == STATUS_NO_RESULT) {
            return;
        }

        // 只要有一个下载失败，那么显示更新失败，继续下载
        if (user_contact_download_status == STATUS_FAILED || user_friend_download_status == STATUS_FAILED
                || user_info_download_status == STATUS_FAILED || user_label_download_status == STATUS_FAILED
                || user_room_download_status == STATUS_FAILED) {
            // 失败时用mDataLoadView显示重试，
            mDataLoadView.showFailed();
            mDataLoadView.setVisibility(View.VISIBLE);
            findViewById(R.id.ll).setVisibility(View.GONE);
            return;
        }

        // 所有数据加载完毕,跳转回用户操作界面
        if (this.isDestroyed()) {// 之前发现返回到登录界面还会跳转，坐下判断
            return;
        }
        UserSp.getInstance(DataDownloadActivity.this).setUpdate(true);
        Intent intent;
        if (ShareConstant.IS_SHARE_S_COME) {
            intent = new Intent(DataDownloadActivity.this, ShareNearChatFriend.class);
        } else if (ShareConstant.IS_SHARE_L_COME) {
            intent = new Intent(DataDownloadActivity.this, AuthorizationActivity.class);
        } else if (ShareConstant.IS_SHARE_QL_COME) {
            intent = new Intent(DataDownloadActivity.this, QuickLoginAuthority.class);
        } else if (ShareConstant.IS_SHARE_QP_COME) {
            intent = new Intent(DataDownloadActivity.this, QuickPay.class);
        } else {
            LoginHelper.broadcastLogin(mContext);
            intent = new Intent(DataDownloadActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
        finish();
    }

    /**
     * 下载个人基本资料
     */
    private void downloadUserInfo() {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {

                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        boolean updateSuccess = false;
                        if (result.getResultCode() == 1) {
                            User user = result.getData();
                            updateSuccess = UserDao.getInstance().updateByUser(user);
                            // 设置登陆用户信息
                            if (updateSuccess) {
                                // 如果成功，保存User变量，
                                coreManager.setSelf(user);
                            }
                        }
                        if (updateSuccess) {
                            user_info_download_status = STATUS_SUCCESS;// 成功
                        } else {
                            user_info_download_status = STATUS_FAILED;    // 失败
                        }
                        endDownload();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                        user_info_download_status = STATUS_FAILED;// 失败
                        endDownload();
                    }
                });
    }

    /**
     * 下载我的联系人
     */
    private void downloadUserAddressBook() {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("telephone", coreManager.getSelf().getTelephone());

        HttpUtils.get().url(coreManager.getConfig().ADDRESSBOOK_GETALL)
                .params(params)
                .build()
                .execute(new ListCallback<Contact>(Contact.class) {
                    @Override
                    public void onResponse(ArrayResult<Contact> result) {
                        if (result.getResultCode() == 1) {
                            ContactDao.getInstance().refreshContact(mLoginUserId, result.getData());
                            user_contact_download_status = STATUS_SUCCESS;// 成功
                        } else {
                            user_contact_download_status = STATUS_FAILED; // 失败
                        }
                        endDownload();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                        user_contact_download_status = STATUS_FAILED;// 失败
                        endDownload();
                    }
                });
    }

    /**
     * 下载我的好友
     */
    private void downloadUserFriend() {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_ATTENTION_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<AttentionUser>(AttentionUser.class) {
                    @Override
                    public void onResponse(ArrayResult<AttentionUser> result) {
                        if (result.getResultCode() == 1) {
                            AsyncUtils.doAsync(DataDownloadActivity.this, e -> {
                                Reporter.post("保存好友失败，", e);
                                AsyncUtils.runOnUiThread(DataDownloadActivity.this, ctx -> {
                                    ToastUtil.showToast(ctx, R.string.data_exception);
                                });
                            }, c -> {
                                FriendDao.getInstance().addAttentionUsers(coreManager.getSelf().getUserId(), result.getData(), new OnCompleteListener2() {

                                    @Override
                                    public void onLoading(int progressRate, int sum) {
                                        int rate = (int) ((float) progressRate / sum * 100);
                                        if (rate != lastRate) {
                                            c.uiThread(r -> {
                                                mNumberProgressBar.setProgress(rate);
                                            });
                                            lastRate = rate;
                                        }
                                    }

                                    @Override
                                    public void onCompleted() {
                                        c.uiThread(r -> {
                                            user_friend_download_status = STATUS_SUCCESS;// 成功
                                            endDownload();
                                        });
                                    }
                                });
                            });
                        } else {
                            user_friend_download_status = STATUS_FAILED;// 失败
                            endDownload();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                        user_friend_download_status = STATUS_FAILED;// 失败
                        endDownload();
                    }
                });
    }

    /**
     * 下载我的标签
     */
    private void downloadUserLabel() {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().FRIENDGROUP_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<Label>(Label.class) {
                    @Override
                    public void onResponse(ArrayResult<Label> result) {
                        if (result.getResultCode() == 1) {
                            LabelDao.getInstance().refreshLabel(mLoginUserId, result.getData());
                            user_label_download_status = STATUS_SUCCESS;// 成功
                            endDownload();
                        } else {
                            user_label_download_status = STATUS_FAILED;// 失败
                            endDownload();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                        user_label_download_status = STATUS_FAILED;// 失败
                        endDownload();
                    }
                });

    }

    /**
     * 下载我的群组
     */
    private void downloadRoom() {
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
                            AsyncUtils.doAsync(DataDownloadActivity.this, e -> {
                                Reporter.post("保存群组失败，", e);
                                AsyncUtils.runOnUiThread(DataDownloadActivity.this, ctx -> {
                                    ToastUtil.showToast(ctx, R.string.data_exception);
                                });
                            }, c -> {
                                FriendDao.getInstance().addRooms(mHandler, mLoginUserId, result.getData(), new OnCompleteListener2() {

                                    @Override
                                    public void onLoading(int progressRate, int sum) {
                                        int rate = (int) ((float) progressRate / sum * 100);
                                        if (rate != lastRateRoom) {
                                            c.uiThread(r -> {
                                                mNumberProgressBarRoom.setProgress(rate);
                                            });
                                            lastRateRoom = rate;
                                        }
                                    }

                                    @Override
                                    public void onCompleted() {
                                        c.uiThread(r -> {
                                            user_room_download_status = STATUS_SUCCESS;// 成功
                                            endDownload();
                                        });
                                    }
                                });
                            });
                        } else {
                            user_room_download_status = STATUS_FAILED;// 失败
                            endDownload();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                        user_room_download_status = STATUS_FAILED;// 失败
                        endDownload();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        doBack();
    }

    @Override
    protected boolean onHomeAsUp() {
        doBack();
        return true;
    }

    private void doBack() {
        TipDialog tipDialog = new TipDialog(this);
        tipDialog.setmConfirmOnClickListener(InternationalizationHelper.getString("UPDATE_YET"), new TipDialog.ConfirmOnClickListener() {
            @Override
            public void confirm() {
                LoginHelper.broadcastLoginGiveUp(DataDownloadActivity.this);
                finish();
            }
        });
        tipDialog.show();
    }
}
