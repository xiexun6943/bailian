package com.ydd.zhichat.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Message

class NetworkChangeReceiver : BroadcastReceiver() {
    var handler: Handler? = null
    override fun onReceive(context: Context?, intent: Intent?) {
        val connectionManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectionManager.activeNetworkInfo
        val msg = Message()
        if (networkInfo != null && networkInfo.isAvailable) {
        } else {
            msg.what = 1
            handler?.sendMessage(msg)
        }
    }
}