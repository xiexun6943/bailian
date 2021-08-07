package com.xuan.xuanhttplibrary.okhttp.builder;

import android.text.TextUtils;
import android.util.Log;

import com.xuan.xuanhttplibrary.okhttp.HttpUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;

/**
 * @author Administrator
 * @time 2017/3/30 0:11
 * @des ${TODO}
 */

public class PostBuilder extends BaseBuilder {

    @Override
    public PostBuilder url(String url) {
        if (!TextUtils.isEmpty(url)) {
            this.url = url;
        }
        addSecret();
        return this;
    }

    @Override
    public PostBuilder tag(Object tag) {
        return this;
    }

    @Override
    public PostCall build() {
        FormBody.Builder builder = appenParams(new FormBody.Builder());

        build = new Request.Builder()
                .url(url).post(builder.build())
                .build();

        return new PostCall();
    }

    private FormBody.Builder appenParams(FormBody.Builder builder) {
        StringBuffer sb = new StringBuffer();
        sb.append(url);
        sb.append("?");
        if (params != null) {
            for (String key : params.keySet()) {
                builder.add(key, params.get(key));
                sb.append(key).append("=").append(params.get(key)).append("&");
            }
        }
        sb = sb.deleteCharAt(sb.length() - 1); // 去掉后面的&

        Log.i(HttpUtils.TAG, "网络请求参数：" + sb.toString());
        // Log.d(HttpUtils.TAG, "网络请求参数：" + url + "?" + sb.toString());
        return builder;
    }

    @Override
    public PostBuilder params(String k, String v) {
        // this.url = this.url+k+"="+v;
        if (params == null) {
            params = new LinkedHashMap<>();
        }
        params.put(k, v);
        return this;
    }

    public PostBuilder params(Map<String, String> params) {
        // 所有接口都需要time与secret参数
        if (params == null) {
            params = new LinkedHashMap<>();
        }
        this.params.putAll(params);
        return this;
    }

    public class PostCall extends BaseCall {
        @Override
        public void execute(Callback callback) {
            super.execute(callback);
        }
    }
}
