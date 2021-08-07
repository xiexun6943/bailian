package com.xuan.xuanhttplibrary.okhttp.builder;

import android.text.TextUtils;
import android.util.Log;

import com.xuan.xuanhttplibrary.okhttp.HttpUtils;

import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.Request;

/**
 * @author Administrator
 * @time 2017/3/30 0:11
 * @des ${TODO}
 */

public class GetBuilder extends BaseBuilder {

    @Override
    public GetBuilder url(String url) {
        if (!TextUtils.isEmpty(url)) {
            this.url = url;
        }
        addSecret();
        return this;
    }

    @Override
    public GetBuilder tag(Object tag) {
        return this;
    }

    public GetCall build() {
        url = appenParams();
        build = new Request.Builder().url(url).build();
        Log.i(HttpUtils.TAG, "网络请求参数：" + url);
        return new GetCall();
    }

    private String appenParams() {
        StringBuffer sb = new StringBuffer();
        sb.append(url);

        if (params != null && !params.isEmpty()) {
            sb.append("?");
            for (String key : params.keySet()) {
                sb.append(key).append("=").append(params.get(key)).append("&");
            }
            sb = sb.deleteCharAt(sb.length() - 1); // 去掉后面的&
        }

        return sb.toString();
    }

    @Override
    public GetBuilder params(String k, String v) {
        try {
            // url安全，部分字符不能直接放进url, 要改成百分号开头%的，
            v = URLEncoder.encode(v, "UTF-8");
        } catch (Exception e) {
            // 不可到达，UTF-8不可能不支持，
            e.printStackTrace();
        }
        // this.url = this.url+k+"="+v;
        if (params == null) {
            params = new LinkedHashMap<>();
        }
        params.put(k, v);
        return this;
    }

    public GetBuilder params(Map<String, String> params) {
        for (String key : params.keySet()) {
            params(key, params.get(key));
        }

        return this;
    }

    public class GetCall extends BaseCall {
        @Override
        public void execute(Callback callback) {
            super.execute(callback);
        }
    }
}
