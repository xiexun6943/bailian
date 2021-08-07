package com.ydd.zhichat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Observable;
import android.net.ConnectivityManager;
import android.util.Log;

import com.ydd.zhichat.NetWorkObservable.NetWorkObserver;
import com.ydd.zhichat.util.HttpUtil;

/**
 * 网络改变通知并回调的类
 */
public class NetWorkObservable extends Observable<NetWorkObserver> {
    public interface NetWorkObserver {
        void onNetWorkStatusChange(boolean connected);
    }

    private Context mContext;
    private ConnectivityManager mConnectivityManager;
    private boolean mIsNetWorkActive;// 当前网络是否连接上
    private boolean mRegisted;// 是否注册了广播

    NetWorkObservable(Context context) {
        mContext = context;
        // 获取程序启动时的网络状态
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mIsNetWorkActive = HttpUtil.isGprsOrWifiConnected(mContext);
        if (AppConfig.DEBUG) {
            Log.d(AppConfig.TAG, "mIsNetWorkActive:" + mIsNetWorkActive);
        }
        // 注册网络监听广播
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mNetWorkChangeReceiver, intentFilter);
        mRegisted = true;
    }

    /**
     * 监听网络状态改变的广播
     */
    private BroadcastReceiver mNetWorkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                return;
            }
            // 网络发生改变，执行回调
            mIsNetWorkActive = HttpUtil.isGprsOrWifiConnected(mContext);
            notifyChanged(mIsNetWorkActive);
        }
    };

    /**
     * 执行网络改变的回调（通知注册了的类）
     *
     * @param connected
     */
    public void notifyChanged(boolean connected) {
        synchronized (mObservers) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onNetWorkStatusChange(connected);
            }
        }
    }

    public void release() {
        if (mRegisted && mContext != null) {
            mContext.unregisterReceiver(mNetWorkChangeReceiver);
            mRegisted = false;
        }
        unregisterAll();
    }

    public boolean isNetworkActive() {
        return mIsNetWorkActive;
    }
}
