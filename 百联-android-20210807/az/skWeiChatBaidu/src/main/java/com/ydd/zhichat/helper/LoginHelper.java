package com.ydd.zhichat.helper;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import com.ydd.zhichat.AppConfig;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.bean.LoginAuto;
import com.ydd.zhichat.bean.LoginRegisterResult;
import com.ydd.zhichat.bean.User;
import com.ydd.zhichat.bean.UserStatus;
import com.ydd.zhichat.db.dao.UserDao;
import com.ydd.zhichat.sp.UserSp;
import com.ydd.zhichat.ui.MainActivity;
import com.ydd.zhichat.ui.base.CoreManager;
import com.ydd.zhichat.util.Constants;
import com.ydd.zhichat.util.DeviceInfoUtil;
import com.ydd.zhichat.util.LogUtils;
import com.ydd.zhichat.util.PreferenceUtils;
import com.ydd.zhichat.volley.Result;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 当前登陆用户的帮助类
 */
public class LoginHelper {
    public static final String ACTION_LOGIN = AppConfig.sPackageName + ".action.login";  // 登陆
    public static final String ACTION_LOGOUT = AppConfig.sPackageName + ".action.logout";// 用户手动注销登出
    public static final String ACTION_CONFLICT = AppConfig.sPackageName + ".action.conflict";          // 登陆冲突（另外一个设备登陆了）
    // 用户需要重新登陆，更新本地数据（可能是STATUS_USER_TOKEN_OVERDUE，STATUS_USER_NO_UPDATE，STATUS_USER_TOKEN_CHANGE三种状态之一）
    public static final String ACTION_NEED_UPDATE = AppConfig.sPackageName + ".action.need_update";
    public static final String ACTION_LOGIN_GIVE_UP = AppConfig.sPackageName + ".action.login_give_up";// 在下载资料的时候，没下载完就放弃登陆了
    /* 信息完整程度由低到高，从第2级别开始，MyApplication中的mLoginUser是有值得 */
    /* 没有用户，即游客（不需要进行其他操作） */
    public static final int STATUS_NO_USER = 0;
    /* 有用户，但是不完整，只有手机号，可能是之前注销过（不需要进行其他操作） */
    public static final int STATUS_USER_SIMPLE_TELPHONE = 1;
    /* 有用户，但是本地Token已经过期了（需要弹出对话框提示：本地Token已经过期，重新登陆） */
    public static final int STATUS_USER_TOKEN_OVERDUE = 2;
    /*
     * 有用户，本地Token未过期，但是可能信息不是最新的，即在上次登陆之后，没有更新完数据就退出了app （需要检测Token是否变更，变更即提示登陆，未变更则提示更新资料）
     */
    public static final int STATUS_USER_NO_UPDATE = 3;
    /*
     * 用户资料全部完整，但是Token已经变更了，提示重新登陆
     */
    public static final int STATUS_USER_TOKEN_CHANGE = 4;
    /*
     * 用户资料全部完整，但是还要检测Token是否变更 （需要检测Token是否变更，变更即提示登陆） 此状态比较特殊，因为有可能Token没有变更，不需要在进行登陆操作。<br/> 在检测Token接口调用失败的情况下，默认为一个不需要重新登陆的用户。
     * 在检测Token接口调用成功的情况下，此状态会立即过度到STATUS_USER_VALIDATION状态。
     */
    public static final int STATUS_USER_FULL = 5;
    /*
     * 用户资料全部完整，并且已经检测Token没有变更，或者新登录更新完成 （不需要进行其他操作）
     */
    public static final int STATUS_USER_VALIDATION = 6;//
    private static final String TAG = "LoginHelper";

    // 获取登陆和登出的action filter
    public static IntentFilter getLogInOutActionFilter() {
        Log.d(TAG, "getLogInOutActionFilter() called");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_LOGIN);
        intentFilter.addAction(ACTION_LOGOUT);
        intentFilter.addAction(ACTION_CONFLICT);
        intentFilter.addAction(ACTION_NEED_UPDATE);
        intentFilter.addAction(ACTION_LOGIN_GIVE_UP);
        return intentFilter;
    }

    // 登陆广播,且登陆的用户为MyApplication.getInstance().mLoginUser
    public static void broadcastLogin(Context context) {
        Log.d(TAG, "broadcastLogin() called with: context = [" + context + "]");
        if (context == null) {// 防止传入Context对象的界面被销毁导致context为空
            context = MyApplication.getContext();
        }
        Intent intent = new Intent(ACTION_LOGIN);
        context.sendBroadcast(intent);
    }

    // 登出广播
    public static void broadcastLogout(Context context) {
        Log.d(TAG, "broadcastLogout() called with: context = [" + context + "]");
        if (context == null) {// 防止传入Context对象的界面被销毁导致context为空
            context = MyApplication.getContext();
        }
        Intent intent = new Intent(ACTION_LOGOUT);
        context.sendBroadcast(intent);
    }

    // 放弃登陆
    public static void broadcastLoginGiveUp(Context context) {
        Log.d(TAG, "broadcastLoginGiveUp() called with: context = [" + context + "]");
        if (context == null) {// 防止传入Context对象的界面被销毁导致context为空
            context = MyApplication.getContext();
        }
        Intent intent = new Intent(ACTION_LOGIN_GIVE_UP);
        context.sendBroadcast(intent);
    }

    // 登陆冲突（另外一个设备登陆了）
    public static void broadcastConflict(Context context) {
        Log.d(TAG, "broadcastConflict() called with: context = [" + context + "]");
        if (context == null) {// 防止传入Context对象的界面被销毁导致context为空
            context = MyApplication.getContext();
        }
        Intent intent = new Intent(ACTION_CONFLICT);
        context.sendBroadcast(intent);
    }

    public static void broadcastNeedUpdate(Context context) {
        Log.d(TAG, "broadcastNeedUpdate() called with: context = [" + context + "]");
        if (context == null) {// 防止传入Context对象的界面被销毁导致context为空
            context = MyApplication.getContext();
        }
        Intent intent = new Intent(ACTION_NEED_UPDATE);
        context.sendBroadcast(intent);
    }

    /* 进入MainActivity，判断当前是游客，还是之前已经登陆过的用户 */
    public static int prepareUser(Context context, CoreManager coreManager) {
        Log.d(TAG, "prepareUser() called with: context = [" + context + "]");
        boolean idIsEmpty = TextUtils.isEmpty(UserSp.getInstance(context).getUserId(""));
        boolean telephoneIsEmpty = TextUtils.isEmpty(UserSp.getInstance(context).getTelephone(null));

        UserStatus userInfo = coreManager.getSelfStatus();
        if (userInfo == null) {
            userInfo = new UserStatus();
            userInfo.accessToken = UserSp.getInstance(context).getAccessToken(null);
            userInfo.userStatus = STATUS_NO_USER;
        }
        if (!idIsEmpty && !telephoneIsEmpty) {// 用户标识都不为空，那么就能代表一个完整的用户
            // 进入之前，加载本地已经存在的数据
            User user = coreManager.getSelf();
            if (user == null) {
                String userId = UserSp.getInstance(context).getUserId("");
                user = UserDao.getInstance().getUserByUserId(userId);
            }

            if (!LoginHelper.isUserValidation(user)) {// 用户数据错误,那么就认为是一个游客
                userInfo.userStatus = STATUS_NO_USER;
            } else {
                coreManager.setSelf(user);

                if (LoginHelper.isTokenValidation()) {// Token未过期
                    boolean isUpdate = UserSp.getInstance(context).isUpdate(true);
                    if (isUpdate) {
                        userInfo.userStatus = STATUS_USER_FULL;
                    } else {
                        userInfo.userStatus = STATUS_USER_NO_UPDATE;
                    }
                } else {// Token过期
                    userInfo.userStatus = STATUS_USER_TOKEN_OVERDUE;
                }
            }
        } else if (!idIsEmpty) {// （适用于切换账号之后的操作）手机号不为空
            userInfo.userStatus = STATUS_USER_SIMPLE_TELPHONE;
        } else {
            userInfo.userStatus = STATUS_NO_USER;
        }
        MyApplication.getInstance().mUserStatus = userInfo.userStatus;
        coreManager.setSelfStatus(userInfo);
        Log.d(TAG, "prepareUser() returned: " + userInfo.userStatus);
        return userInfo.userStatus;
    }

    // User数据是否能代表一个有效的用户
    public static boolean isUserValidation(User user) {
        if (user == null) {
            return false;
        }
        if (TextUtils.isEmpty(user.getUserId())) {
            return false;
        }
        if (TextUtils.isEmpty(user.getTelephone())) {
            return false;
        }
        if (TextUtils.isEmpty(user.getPassword())) {
            return false;
        }
        if (TextUtils.isEmpty(user.getNickName())) {
            return false;
        }
        return true;
    }

    /**
     * AccessToken 是否是有效的
     *
     * @return
     */
    public static boolean isTokenValidation() {
        Log.d(TAG, "isTokenValidation() called");
        if (TextUtils.isEmpty(CoreManager.requireSelfStatus(MyApplication.getInstance()).accessToken)) {
            return false;
        }
       /* if (MyApplication.getInstance().mExpiresIn < System.currentTimeMillis()) {
            return false;
        }*/
        return true;
    }

    public static boolean setLoginUser(Context context, CoreManager coreManager, String telephone, String password, com.xuan.xuanhttplibrary.okhttp.result.ObjectResult<LoginRegisterResult> result) {
        Log.d(TAG, "setLoginUser() called with: context = [" + context + "], telephone = [" + telephone + "], password = [" + password + "], result = [" + result + "]");
        if (result == null) {
            return false;
        }
        if (result.getResultCode() != Result.CODE_SUCCESS) {
            return false;
        }
        if (result.getData() == null) {
            return false;
        }
        String sAreaCode = result.getData().getAreaCode();
        try {
            int areaCode;
            areaCode = Integer.valueOf(result.getData().getAreaCode());
            PreferenceUtils.putInt(context, Constants.AREA_CODE_KEY, areaCode);
        } catch (Exception e) {
            LogUtils.log("没有areaCode");
        }
        try {
            String rPassword = result.getData().getPassword();
            if (!TextUtils.isEmpty(rPassword)) {
                password = rPassword;
            }
        } catch (Exception e) {
            LogUtils.log("没有password");
        }
        try {
            String rTelephone = result.getData().getTelephone();
            if (!TextUtils.isEmpty(rTelephone)) {
                telephone = rTelephone;
                if (!TextUtils.isEmpty(sAreaCode) && telephone.startsWith(sAreaCode)) {
                    telephone = telephone.substring(sAreaCode.length());
                }
            }
        } catch (Exception e) {
            LogUtils.log("没有telephone");
        }

        // 保存当前登陆的用户信息和Token信息作为全局变量，方便调用
        User user = coreManager.getSelf();
        if (user == null) {
            user = new User();
        }
        user.setPhone(telephone);
        user.setTelephone(telephone);
        user.setPassword(password);
        user.setUserId(result.getData().getUserId());
        user.setNickName(result.getData().getNickName());
        user.setRole(result.getData().getRole());
        user.setAccount(result.getData().getAccount());
        user.setSetAccountCount(result.getData().getSetAccountCount());
        user.setMyInviteCode(result.getData().getMyInviteCode());
        if (!LoginHelper.isUserValidation(user)) {
            // 请求下来的用户数据不完整有错误
            return false;
        }
        UserStatus userStatus = new UserStatus();
        userStatus.accessToken = result.getData().getAccess_token();
        long expires_in = result.getData().getExpires_in() * 1000L + System.currentTimeMillis();
        if (result.getData().getLogin() != null) {
            // 保存该用户上次离线的时间，通过当前时间与离线时间的时间差，获取该时间段内群组内的离线消息
            Constants.OFFLINE_TIME_IS_FROM_SERVICE = true;
            CoreManager.saveOfflineTime(context, result.getData().getUserId(), result.getData().getLogin().getOfflineTime());
        }
        // 保存基本信息到数据库
        boolean saveAble = UserDao.getInstance().saveUserLogin(user);
        if (!saveAble) {
            return false;
        }
        // 保存最后一次登录的用户信息到Sp，用于免登陆
        UserSp.getInstance(MyApplication.getInstance()).setUserId(result.getData().getUserId());
        UserSp.getInstance(MyApplication.getInstance()).setTelephone(telephone);
        UserSp.getInstance(MyApplication.getInstance()).setAccessToken(result.getData().getAccess_token());
        UserSp.getInstance(MyApplication.getInstance()).setExpiresIn(expires_in);
        MyApplication.getInstance().mUserStatusChecked = true;
        MyApplication.getInstance().mUserStatus = STATUS_USER_VALIDATION;
        userStatus.userStatusChecked = true;
        userStatus.userStatus = STATUS_USER_VALIDATION;
        coreManager.setSelf(user);
        coreManager.setSelfStatus(userStatus);
        return true;
    }

    /**
     * 检测Status，是否要弹出更新用户状态的对话框
     *
     * @param activity 是否弹出对话框有本方法内部逻辑决定。<br/>
     *                 是否继续循环检测（检测未成功的情况下），<br/>
     *                 由 MyApplication.getInstance().mUserStatusChecked值决定，（ 因为方法内部有异步操作，不能直接返回值来判断） 在MainActivity中进行重复检测的判定
     */
    public static void checkStatusForUpdate(final MainActivity activity, CoreManager coreManager, final OnCheckedFailedListener listener) {
        Log.d(TAG, "checkStatusForUpdate() called with: activity = [" + activity + "], listener = [" + listener + "]");
        if (MyApplication.getInstance().mUserStatusChecked) {
            return;
        }
        final int status = MyApplication.getInstance().mUserStatus;
        if (status == STATUS_NO_USER || status == STATUS_USER_SIMPLE_TELPHONE) {
            MyApplication.getInstance().mUserStatusChecked = true;
            return;
        }

        final User user = coreManager.getSelf();
        if (!isUserValidation(user)) {
            // 如果是用户数据不完整，那么就可能是数据错误，将状态变为STATUS_NO_USER和STATUS_USER_SIMPLE_TELPHONE
            boolean telephoneIsEmpty = TextUtils.isEmpty(UserSp.getInstance(activity).getTelephone(null));
            if (telephoneIsEmpty) {
                MyApplication.getInstance().mUserStatus = STATUS_NO_USER;
            } else {
                MyApplication.getInstance().mUserStatus = STATUS_USER_SIMPLE_TELPHONE;
            }
            return;
        }

        if (status == STATUS_USER_VALIDATION) {
            MyApplication.getInstance().mUserStatusChecked = true;
            broadcastLogin(activity);
            return;
        }

        if (status == STATUS_USER_TOKEN_CHANGE) {
            // Token已经变更，直接提示，不需要再检测Token是否变更
            MyApplication.getInstance().mUserStatusChecked = true;
            // broadcastNeedUpdate(activity);
            return;
        }

        /**
         * 能往下执行的只有三种状态 <br/>
         * public static final int STATUS_USER_TOKEN_OVERDUE = 2;<br/>
         * public static final int STATUS_USER_NO_UPDATE = 3;<br/>
         * public static final int STATUS_USER_FULL = 5;<br/>
         */
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", user.getUserId());
        params.put("serial", DeviceInfoUtil.getDeviceId(activity));

        // 地址信息
        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        if (latitude != 0)
            params.put("latitude", String.valueOf(latitude));
        if (longitude != 0)
            params.put("longitude", String.valueOf(longitude));

        if (MyApplication.IS_OPEN_CLUSTER) {// 服务端集群需要
            String area = PreferenceUtils.getString(MyApplication.getContext(), AppConstant.EXTRA_CLUSTER_AREA);
            if (!TextUtils.isEmpty(area)) {
                params.put("area", area);
            }
        }

        HttpUtils.get().url(coreManager.getConfig().USER_LOGIN_AUTO)
                .params(params)
                .build()
                .execute(new BaseCallback<LoginAuto>(LoginAuto.class) {
                    @Override
                    public void onResponse(ObjectResult<LoginAuto> result) {
                        Log.d(TAG, "onResponse() called with: result = [" + result + "]");
                        boolean success = Result.defaultParser(activity, result, false);
                        if (success && result.getData() != null) {
                            // 保存身份改变，
                            user.setRole(result.getData().getRole());
                            user.setMyInviteCode(result.getData().getMyInviteCode());
                            UserDao.getInstance().saveUserLogin(user);
                            MyApplication.getInstance().initPayPassword(user.getUserId(), result.getData().getPayPassword());
                            PrivacySettingHelper.setPrivacySettings(MyApplication.getContext(), result.getData().getSettings());
                            MyApplication.getInstance().initMulti();

                            MyApplication.getInstance().mUserStatusChecked = true;// 检测Token成功
                            int tokenExists = result.getData().getTokenExists();  // 1=令牌存在、0=令牌不存在
                            int serialStatus = result.getData().getSerialStatus();// 1=没有设备号、2=设备号一致、3=设备号不一致
                            if (serialStatus == 2) {// 设备号一致，说明没有切换过设备
                                if (tokenExists == 1) {// Token也存在，说明不用登陆了
                                    if (status == STATUS_USER_FULL) {// 本地数据完整，那么就免登陆使用
                                        MyApplication.getInstance().mUserStatus = STATUS_USER_VALIDATION;
                                    } else {
                                        // do no thing 依然保持其他的状态
                                    }
                                } else {// Token 不存在
                                    if (status == STATUS_USER_FULL) {// 数据也完整，那么就是Token过期
                                        MyApplication.getInstance().mUserStatus = STATUS_USER_TOKEN_OVERDUE;
                                    } else {
                                        // do no thing 依然保持其他的状态
                                    }
                                }
                            } else if (serialStatus == 3) {// 设备号不一致，那么就是切换过手机
                                MyApplication.getInstance().mUserStatus = STATUS_USER_TOKEN_CHANGE;
                            }
                            if (MyApplication.getInstance().mUserStatus == STATUS_USER_VALIDATION) {
                                Log.e("LoginAuto", "STATUS_USER_VALIDATION");
                                broadcastLogin(activity);
                            } else if (MyApplication.getInstance().mUserStatus == STATUS_USER_TOKEN_CHANGE) {
                                Log.e("LoginAuto", "STATUS_USER_TOKEN_CHANGE");
                                // broadcastConflict(activity);
                                broadcastLogin(activity);
                            } else if (MyApplication.getInstance().mUserStatus == STATUS_USER_NO_UPDATE) {
                                Log.e("LoginAuto", "STATUS_USER_NO_UPDATE");
                                broadcastNeedUpdate(activity);
                            } else {
                                Log.e("LoginAuto", MyApplication.getInstance().mUserStatus + "");
                            }
                        } else {
                            if (listener != null) {
                                listener.onCheckFailed();
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.d(TAG, "onError() called with: call = [" + call + "], e = [" + e + "]");
                    }
                });

    }

    public static interface OnCheckedFailedListener {// 检查Token失败才回调，然后外部在继续下一次检测

        void onCheckFailed();
    }
}
