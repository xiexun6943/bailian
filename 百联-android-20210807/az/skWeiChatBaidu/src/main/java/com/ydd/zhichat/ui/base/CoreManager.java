package com.ydd.zhichat.ui.base;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ydd.zhichat.AppConfig;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.bean.ConfigBean;
import com.ydd.zhichat.bean.User;
import com.ydd.zhichat.bean.UserStatus;
import com.ydd.zhichat.bean.collection.Collectiion;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.bean.message.NewFriendMessage;
import com.ydd.zhichat.bean.message.XmppMessage;
import com.ydd.zhichat.bean.redpacket.Balance;
import com.ydd.zhichat.db.dao.UserDao;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.helper.LoginHelper;
import com.ydd.zhichat.sp.UserSp;
import com.ydd.zhichat.ui.UserCheckedActivity;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.xmpp.CoreService;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;

public class CoreManager {
    public static final String KEY_CONFIG_BEAN = "configBean";
    private static final String TAG = "CoreManager";
    private static AppConfig staticConfig = null;
    private static User staticSelf = null;
    private static UserStatus staticSelfStatus = null;
    private static Context mContext;
    private boolean loginRequired;
    private boolean configRequired;
    private User self = null;
    private UserStatus selfStatus = null;
    private Limit limit = new Limit(this);
    private AppConfig config = null;
    private BaseLoginActivity ctx;
    private CoreStatusListener coreStatusListener;
    // 绑定Service成功的回调，
    // TODO: 要改成登录成功才回调，
    private Runnable connectedCallback;
    private CoreService mService;
    private boolean isBind = false;
    // 当前绑定服务的连接，
    private ServiceConnection mCoreServiceConnection;

    CoreManager(BaseLoginActivity ctx, CoreStatusListener coreStatusListener) {
        this.ctx = ctx;
        this.coreStatusListener = coreStatusListener;
    }

    private static SharedPreferences getSharedPreferences(Context ctx) {
        return ctx.getSharedPreferences("core_manager", Context.MODE_PRIVATE);
    }

    @SuppressWarnings("WeakerAccess")
    public static AppConfig requireConfig(Context ctx) {
        mContext = ctx;
        if (staticConfig == null) {
            synchronized (CoreManager.class) {
                if (staticConfig == null) {
                    ConfigBean configBean = readConfigBean(ctx);
                    if (configBean == null) {
                        configBean = getDefaultConfig(ctx);
                    }
                    setStaticConfig(ctx, AppConfig.initConfig(configBean));
                }
            }
        }
        Log.d(TAG, "requireConfig() returned: " + staticConfig);
        return staticConfig;
    }

    private static ConfigBean readConfigBean(Context ctx) {
        String configBeanJson = getSharedPreferences(ctx)
                .getString(KEY_CONFIG_BEAN, null);
        if (TextUtils.isEmpty(configBeanJson)) {
            return null;
        }
        return JSON.parseObject(configBeanJson, ConfigBean.class);
    }

    // 初始化本地自定义表情
    public static void initLocalCollectionEmoji() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", staticSelfStatus.accessToken);
        params.put("userId", CoreManager.requireSelf(MyApplication.getInstance()).getUserId());

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).Collection_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<Collectiion>(Collectiion.class) {
                    @Override
                    public void onResponse(ArrayResult<Collectiion> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            MyApplication.mCollection = result.getData();
                            //添加一个管理表情item
                            Collectiion collection = new Collectiion();
                            collection.setType(7);
                            MyApplication.mCollection.add(0, collection);
                        }

                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 刷新用户余额
    // 单聊、群聊的onCreate方法就不调用updateMyBalance了，放到主界面的onCreate方法内调用
    public static void updateMyBalance() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", staticSelfStatus.accessToken);
        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).RECHARGE_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<Balance>(Balance.class) {

                    @Override
                    public void onResponse(ObjectResult<Balance> result) {
                        Balance b = result.getData();
                        if (b != null) {
                            staticSelf.setBalance(b.getBalance());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    public static void appBackstage(Context context, boolean isBack) {
        if (staticSelf == null || staticSelfStatus == null) {
            return;
        }
        String str;
        User self = staticSelf;
        if (isBack) {
            str = "程序已到后台";
            Log.e("appBackstage", str + "，开始--》将离线时间本存至本地");
            long time = System.currentTimeMillis() / 1000;
            CoreManager.saveOfflineTime(context, self.getUserId(), time);
            UserDao.getInstance().updateUnLineTime(self.getUserId(), time);
            Log.e("appBackstage", str + "，结束--》将离线时间本存至本地");
        } else {
            str = "XMPP连接关闭 || 异常断开";
        }

        Log.e("appBackstage", str + "，开始--》调用outTime接口");
        Map<String, String> params = new HashMap<>();
        params.put("access_token", staticSelfStatus.accessToken);
        params.put("userId", self.getUserId());

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).USER_OUTTIME)
                .params(params)
                .build().execute(new BaseCallback<Void>(Void.class) {
            @Override
            public void onResponse(ObjectResult<Void> result) {

            }

            @Override
            public void onError(Call call, Exception e) {

            }
        });
/*
        StringJsonObjectRequest<User> request = new StringJsonObjectRequest<>(CoreManager.requireConfig(MyApplication.getInstance()).USER_OUTTIME,
                null, null, null, params);
        MyApplication.getInstance().getFastVolley().addShortRequest(TAG + "@", request);
*/
    }

    public static void saveOfflineTime(Context context, String userId, long outTime) {
        if (Constants.OFFLINE_TIME_IS_FROM_SERVICE) {
            Log.e("appBackstage", "服务端获取到的离线时间--》" + outTime);
        } else {
            Log.e("appBackstage", "本地生成的离线时间--》" + outTime);
        }
        PreferenceUtils.putLong(context, Constants.OFFLINE_TIME + userId, outTime);
        if (staticSelf != null) {
            staticSelf.setOfflineTime(outTime);
        }
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static User requireSelf(Context ctx) {
        if (staticSelf == null) {
            synchronized (CoreManager.class) {
                if (staticSelf == null) {
                    String userId = UserSp.getInstance(ctx).getUserId("");
                    User user = UserDao.getInstance().getUserByUserId(userId);
                    if (user == null) {
                        Reporter.post("登录的User为空，");
                        // 无论如何没有登录信息就跳到重新登录，
                        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_CHANGE;
                        // 弹出对话框
                        UserCheckedActivity.start(ctx);
                        // 就算没有登录也不能返回null, 直接把本地过期的返回以便正常初始化，然后finish页面，
                        user = new User();
                    }
                    setStaticSelf(user);
                }
            }
        }
        Log.d(TAG, "requireSelfUser() returned: " + staticSelf);
        return staticSelf;
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public static User getSelf(Context ctx) {
        if (staticSelf == null) {
            synchronized (CoreManager.class) {
                if (staticSelf == null) {
                    String userId = UserSp.getInstance(ctx).getUserId("");
                    setStaticSelf(UserDao.getInstance().getUserByUserId(userId));
                }
            }
        }
        Log.d(TAG, "requireSelfUser() returned: " + staticSelf);
        return staticSelf;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static UserStatus requireSelfStatus(Context ctx) {
        if (staticSelfStatus == null) {
            synchronized (CoreManager.class) {
                if (staticSelfStatus == null) {
                    UserStatus info = new UserStatus();
                    info.accessToken = UserSp.getInstance(ctx).getAccessToken(null);

                    if (TextUtils.isEmpty(info.accessToken)) {
                        Reporter.post("登录的accessToken为空，");
                        // 无论如何没有登录信息就跳到重新登录，
                        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_CHANGE;
                        // 弹出对话框
                        UserCheckedActivity.start(ctx);
                        // 就算没有登录也不能返回null, 直接把本地过期的返回以便正常初始化，然后finish页面，
                    }
                    setStaticSelfStatus(info);
                }
            }
        }
        return staticSelfStatus;
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public static UserStatus getSelfStatus(Context ctx) {
        if (staticSelfStatus == null) {
            synchronized (CoreManager.class) {
                if (staticSelfStatus == null) {
                    UserStatus info = new UserStatus();
                    info.accessToken = UserSp.getInstance(ctx).getAccessToken(null);
                    if (!TextUtils.isEmpty(info.accessToken)) {
                        setStaticSelfStatus(info);
                    }
                }
            }
        }
        return staticSelfStatus;
    }

    private static void setStaticConfig(Context ctx, AppConfig config) {
        staticConfig = config;
        Reporter.putUserData("configUrl", AppConfig.readConfigUrl(ctx));
        if (config != null) {
            Reporter.putUserData("apiUrl", config.apiUrl);
        }
    }

    /**
     * 登录和读取本地用户信息时会调用，
     * 顺便设置到bugly上，
     */
    private static void setStaticSelf(User self) {
        staticSelf = self;
        if (self != null) {
            Reporter.setUserId(self.getTelephone());
            Reporter.putUserData("userId", self.getUserId());
            Reporter.putUserData("telephone", self.getTelephone());
            // Reporter.putUserData("password", self.getPassword());
            Reporter.putUserData("nickName", self.getNickName());
        }
    }

    /**
     * 登录和读取本地用户信息时会调用，
     * 顺便设置到bugly上，
     */
    private static void setStaticSelfStatus(UserStatus selfStatus) {
        staticSelfStatus = selfStatus;
        if (selfStatus != null) {
            Reporter.putUserData("accessToken", selfStatus.accessToken);
        }
    }

    /**
     * 可能是获取服务器config失败，也可能是没获取config时就被拉起了其他页面，
     */
    @NonNull
    public static ConfigBean getDefaultConfig(Context ctx) {
        try {
            // 手动下载一份config接口返回值放在assets里作为默认config,
            ObjectResult<ConfigBean> result = JSON.parseObject(ctx.getAssets().open("default_config"), new TypeReference<ObjectResult<ConfigBean>>() {
            }.getType());
            if (result == null || result.getData() == null) {
                // 就算有个万一，也不要返回null,
                ConfigBean configBean = new ConfigBean();
                if(configBean.getApiUrl()==null || "".equals(configBean.getApiUrl())){
                    configBean.setApiUrl(AppConfig.OFFLINE_URL);
                }
                return configBean;
            }
            return result.getData();
        } catch (IOException e) {
            // 不可到达，本地assets一定要提供默认config,
            Reporter.unreachable();
            return new ConfigBean();
        }
    }

    // 创建一个服务连接，重连时要创建新的，
    private ServiceConnection createCoreServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                // 可能后台Service断掉，然后回到前台时不会自动重新绑定，
                Log.d(TAG, "onServiceDisconnected() called with: name = [" + name + "]");
                mService = null;
                mCoreServiceConnection = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onServiceConnected() called with: name = [" + name + "], service = [" + service + "]");
                mService = ((CoreService.CoreServiceBinder) service).getService();
                mCoreServiceConnection = this;
                // 改成传进CoreService里面回调，
                if (connectedCallback != null) {
                    connectedCallback.run();
                }
            }
        };
    }

    public ConfigBean readConfigBean() {
        return readConfigBean(ctx);
    }

    public void saveConfigBean(ConfigBean configBean) {
        getSharedPreferences(ctx)
                .edit()
                .putString(KEY_CONFIG_BEAN, JSON.toJSONString(configBean))
                .apply();
        config = AppConfig.initConfig(configBean);
        setStaticConfig(ctx, config);
    }

    public AppConfig getConfig() {
        if (config == null) {
            config = requireConfig(ctx);
        }
        return config;
    }

    public User getSelf() {
        return self;
    }

    /**
     * 登录时会调用该方法保存用户信息，
     */
    public void setSelf(User self) {
        this.self = self;
        setStaticSelf(self);
    }

    public UserStatus getSelfStatus() {
        return selfStatus;
    }

    /**
     * 登录时会调用该方法保存用户信息，
     */
    public void setSelfStatus(UserStatus selfStatus) {
        this.selfStatus = selfStatus;
        setStaticSelfStatus(selfStatus);
    }

    public Limit getLimit() {
        return limit;
    }

    void init(boolean loginRequired, boolean configRequired) {
        Log.d(TAG, "init() called");
        this.loginRequired = loginRequired;
        this.configRequired = configRequired;
        if (loginRequired) {
            this.self = requireSelf(ctx);
            this.selfStatus = requireSelfStatus(ctx);
        } else {
            this.self = getSelf(ctx);
            this.selfStatus = getSelfStatus(ctx);
        }
        if (configRequired) {
            this.config = requireConfig(ctx);
        }
        // TODO: 判断一下User状态，是否需要跳到登录，
        if (loginRequired && !isServiceReady()) {
            ContextCompat.startForegroundService(ctx, CoreService.getIntent(ctx, self.getUserId(), self.getPassword(), self.getNickName()));
            connectedCallback = () -> {
                coreStatusListener.onCoreReady();
                // onCoreReady回调只调用一次，用于第一次初始化，
                // 重新登录不再次调用，
                connectedCallback = null;
            };
            isBind = ctx.bindService(CoreService.getIntent(), createCoreServiceConnection(), Context.BIND_AUTO_CREATE);
        }
    }

    void destroy() {
        if (isBind && mCoreServiceConnection != null) {
            try {
                ctx.unbindService(mCoreServiceConnection);
            } catch (Exception e) {
                // 以防万一，比如没绑定时会抛IllegalArgumentException，
                Reporter.unreachable(e);
            }
        }
    }

    public void logout() {
        if (isServiceReady()) {
            try {
                mService.logout();
                Log.e("zq", "logout成功");
            } catch (Exception e) {
                // 以防万一，
                Reporter.unreachable(e);
                Log.e("zq", "logout失败1");
            }
        } else {
            Log.e("zq", "logout失败2");
        }
    }

    public boolean isServiceReady() {
        return isBind && mService != null;
    }

    private void requireXmpp() {
        if (!isServiceReady()) {
            throw new IllegalStateException("xmpp服务没启动");
        }
    }

    private boolean requireXmppOrReport() {
        boolean ret = isServiceReady();
        if (!ret) {
            Reporter.post("xmpp服务没启动");
        }
        return ret;
    }

    public void disconnect() {
        Log.d("zx", "disconnect() called");
        if (isServiceReady() && mService.getmConnectionManager() != null
                && mService.getmConnectionManager().getConnection() != null) {
            mService.getmConnectionManager().getConnection().disconnect();
        }
    }

    /**
     * 比如后台回来时的重新登录，
     */
    public void relogin() {
        Log.d(TAG, "relogin() called");
        if (!isLogin()) {
            ContextCompat.startForegroundService(ctx, CoreService.getIntent(ctx, self.getUserId(), self.getPassword(), self.getNickName()));
            connectedCallback = null;
            isBind = ctx.bindService(CoreService.getIntent(), createCoreServiceConnection(), Context.BIND_AUTO_CREATE);
        }
    }

    public void login() {
        Log.d(TAG, "login() called");
        requireXmpp();
        mService.login(getSelf().getUserId(), getSelf().getPassword());
    }

    public void joinExistGroup() {
        Log.d(TAG, "joinExistGroup() called");
        requireXmpp();
        mService.joinExistGroup();
    }

    public String createMucRoom(String roomName) {
        Log.d(TAG, "createMucRoom() called with: roomName = [" + roomName + "]");
        requireXmpp();
        return mService.createMucRoom(roomName);
    }

    public void joinMucChat(String mRoomJid, long lastSeconds) {
        Log.d(TAG, "joinMucChat() called with: mRoomJid = [" + mRoomJid + "], l = [" + lastSeconds + "]");
        requireXmpp();
        mService.joinMucChat(mRoomJid, lastSeconds);
    }

    public void exitMucChat(String mRoomJid) {
        Log.d(TAG, "exitMucChat() called with: mRoomJid = [" + mRoomJid + "]");
        if (!requireXmppOrReport()) {
            return;
        }
        mService.exitMucChat(mRoomJid);
    }

    private void requirePackageId(XmppMessage message) {
        if (TextUtils.isEmpty(message.getPacketId())) {
            message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        }
    }

    public void sendChatMessage(String toUserId, ChatMessage message) {
        Log.d(TAG, "sendChatMessage() called with: call_toUser = [" + toUserId + "], message = [" + message + "]");
        requireXmpp();
        requirePackageId(message);
        mService.sendChatMessage(toUserId, message);
    }

    public void sendMucChatMessage(String mRoomJid, ChatMessage message) {
        Log.d(TAG, "sendMucChatMessage() called with: mRoomJid = [" + mRoomJid + "], message = [" + message + "]");
        requireXmpp();
        requirePackageId(message);
        mService.sendMucChatMessage(mRoomJid, message);
    }

    public void sendNewFriendMessage(String userId, NewFriendMessage message) {
        Log.d(TAG, "sendNewFriendMessage() called with: userId = [" + userId + "], message = [" + message + "]");
        requireXmpp();
        requirePackageId(message);
        mService.sendNewFriendMessage(userId, message);
    }

    public boolean isLogin() {
        Log.d(TAG, "isLogin() called");
        return isServiceReady() && mService.isAuthenticated();
    }

    /**
     * 自动重连
     */
    public void autoReconnect(Activity activity) {
        logout();
        User user = getSelf();
        if (user != null) {
            Log.e("zq", "自动重连--->重新启动服务");
            ContextCompat.startForegroundService(ctx, CoreService.getIntent(ctx, self.getUserId(), self.getPassword(), self.getNickName()));
        } else {
            Log.e("zq", "自动重连--->本地用户数据空了");
            Toast.makeText(activity, "本地用户数据空了", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 自动重连 弹出等待窗
     */
    public void autoReconnectShowProgress(Activity activity) {
        DialogHelper.showMessageProgressDialogAddCancel(activity, ctx.getString(R.string.keep_reconnection), dialog -> dialog.dismiss());

        logout();
        User user = getSelf();
        if (user != null) {
            Log.e("zq", "自动重连--->重新启动服务");
            ContextCompat.startForegroundService(ctx, CoreService.getIntent(ctx, self.getUserId(), self.getPassword(), self.getNickName()));
        } else {
            DialogHelper.dismissProgressDialog();
            Log.e("zq", "自动重连--->本地用户数据空了");
            Toast.makeText(activity, "本地用户数据空了", Toast.LENGTH_SHORT).show();
        }
    }
}
