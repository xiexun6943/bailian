package com.ydd.zhichat.volley;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 通过字符串参数集，请求json参数，并序列号为JsonModel对象
 *
 * @param <T>
 * @author dty
 */
public class StringJsonArrayRequest<T> extends Request<String> {

    public static interface Listener<T> {
        void onResponse(ArrayResult<T> result);
    }

    private Listener<T> mListener;
    private Class<T> mClazz;
    private Map<String, String> mParams;

    private boolean mGzipEnable = false;

    /**
     * 请求方式post
     *
     * @param url      url地址
     * @param listener
     */
    public StringJsonArrayRequest(String url, ErrorListener errorListener, Listener<T> listener, Class<T> clazz, Map<String, String> params) {
        this(Method.POST, url, errorListener, listener, clazz, params);
    }

    /**
     * @param method   请求方式，post或者get
     * @param url      url地址
     * @param listener
     */
    public StringJsonArrayRequest(int method, String url, ErrorListener errorListener, Listener<T> listener, Class<T> clazz,
                                  Map<String, String> params) {
        super(method, url, errorListener);
        mListener = listener;
        mClazz = clazz;
        mParams = params;
        if (method == Method.GET) {
            spliceGetUrl();
        }
    }

    public void setGzipEnable(boolean eanble) {
        mGzipEnable = eanble;
    }

    /* Post 参数设置 */
    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        if (getMethod() != Method.POST && getMethod() != Method.PUT) {
            return null;
        }

        if (FastVolley.DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append(getUrl());
            if (mParams != null) {
                sb.append("?");
                for (String key : mParams.keySet()) {
                    sb.append(key);
                    sb.append("=");
                    sb.append(mParams.get(key));
                    sb.append("&");
                }
                sb.deleteCharAt(sb.length() - 1);
            }
            Log.i(FastVolley.TAG, "requst:" + sb.toString());
        }

        return mParams;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        if (mGzipEnable) {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Charset", "UTF-8");
            headers.put("Content-Type", "application/x-javascript");
            headers.put("Accept-Encoding", "gzip,deflate");
            return headers;
        } else {
            return super.getHeaders();
        }
    }

    /* Get 参数拼接 */
    private void spliceGetUrl() {
        if (mParams != null && mParams.size() > 0) {
            String url = getUrl();
            if (TextUtils.isEmpty(url)) {
                return;
            }
            if (url != null && !url.contains("?")) {
                url += "?";
            }
            String param = "";
            for (String key : mParams.keySet()) {
                param += (key + "=" + mParams.get(key) + "&");
            }
            param = param.substring(0, param.length() - 1);// 去掉最后一个&


            setUrl(url + param);
        }
    }

    @Override
    protected void deliverResponse(String arg0) {
        if (mListener == null) {
            return;
        }
        if (FastVolley.DEBUG) {
            Log.i(FastVolley.TAG, "response:" + arg0);
        }

        if (TextUtils.isEmpty(arg0)) {
            deliverError(new VolleyError(new NetworkError()));
            return;
        }

        ArrayResult<T> result = new ArrayResult<T>();
        try {
            JSONObject jsonObject = JSON.parseObject(arg0);
            result.setResultCode(jsonObject.getIntValue(Result.RESULT_CODE));
            result.setResultMsg(jsonObject.getString(Result.RESULT_MSG));

            String data = jsonObject.getString(Result.DATA);
            if (!TextUtils.isEmpty(data)) {
                result.setData(JSON.parseArray(data, mClazz));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mListener.onResponse(result);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            if (mGzipEnable) {
                parsed = getRealString(response.data);
            } else {
                parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            }
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }

    private int getShort(byte[] data) {
        return (int) ((data[0] << 8) | data[1] & 0xFF);
    }

    /**
     * GZip解压缩
     */
    private String getRealString(byte[] data) {
        byte[] h = new byte[2];
        h[0] = (data)[0];
        h[1] = (data)[1];
        int head = getShort(h);
        boolean t = head == 0x1f8b;
        InputStream in;
        StringBuilder sb = new StringBuilder();
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            if (t) {
                in = new GZIPInputStream(bis);
            } else {
                in = bis;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                sb.append(line);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
