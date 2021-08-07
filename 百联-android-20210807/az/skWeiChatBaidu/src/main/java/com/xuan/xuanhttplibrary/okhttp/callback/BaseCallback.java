package com.xuan.xuanhttplibrary.okhttp.callback;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.helper.LoginHelper;
import com.ydd.zhichat.util.LogUtils;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * @author Administrator
 * @time 2017/3/30 0:45
 * @des ${TODO}
 */

public abstract class BaseCallback<T> implements Callback {
    // private Gson mGson;
    private Class<T> mClazz;
    private Handler mDelivery;

    public BaseCallback(Class<T> clazz) {
       /* mType = getSuperclassTypeParameter(getClass());
        mGson = new Gson();*/
        mClazz = clazz;
        mDelivery = new Handler(Looper.getMainLooper());
    }

    public abstract void onResponse(ObjectResult<T> result);

    public abstract void onError(Call call, Exception e);

    @Override
    public void onFailure(Call call, IOException e) {
        Log.i(HttpUtils.TAG, "服务器请求失败", e);
        if (e instanceof ConnectException) {
            Log.i(HttpUtils.TAG, "ConnectException", e);
        }
        if (e instanceof SocketTimeoutException) {
            Log.i(HttpUtils.TAG, "SocketTimeoutException", e);
        }
        errorData(call, e);
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        if (response.code() == 200) {
            try {
                ObjectResult<T> result = new ObjectResult<T>();
                String body = response.body().string();
                Log.i(HttpUtils.TAG, "服务器数据包：" + body);
                JSONObject jsonObject = JSON.parseObject(body);

                result.setResultCode(jsonObject.getIntValue(Result.RESULT_CODE));
                result.setResultMsg(jsonObject.getString(Result.RESULT_MSG));
                result.setCurrentTime(jsonObject.getLongValue(Result.RESULT_CURRENT_TIME));

                if (!mClazz.equals(Void.class)) {
                    String data = jsonObject.getString(Result.DATA);
                    if (!TextUtils.isEmpty(data)) {
                        if (mClazz.equals(String.class) || mClazz.getSuperclass().equals(Number.class)) {// String
                            // 类型或者基本数据类型（Integer）
                            result.setData(castValue(mClazz, data));
                        } else {
                            result.setData(JSON.parseObject(data, mClazz));
                        }
                    }
                }
                successData(result);
            } catch (Exception e) {
                LogUtils.log(response);
                Reporter.post("json解析失败, ", e);
                Log.i(HttpUtils.TAG, "数据解析异常:" + e.getMessage());
                errorData(call, new Exception("数据解析异常"));
            }
        } else {
            Log.i(HttpUtils.TAG, "服务器请求异常");
            errorData(call, new Exception("服务器请求异常"));
        }
    }

    protected void successData(final ObjectResult<T> data) {
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                if (data.getResultCode() == 1030101 || data.getResultCode() == 1030102) {
                    // 缺少访问令牌 || 访问令牌过期或无效
                    MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_OVERDUE;
                    LoginHelper.broadcastLogout(MyApplication.getContext());
                    return;
                }
                onResponse(data);
            }
        });
    }

    protected void errorData(final Call call, final Exception e) {
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                onError(call, e);
            }
        });
    }

    private T castValue(Class<T> clazz, String data) {
        try {
            Constructor<T> constructor = clazz.getConstructor(String.class);
            return constructor.newInstance(data);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
