package com.ydd.zhichat.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ydd.zhichat.volley.ArrayResult;
import com.ydd.zhichat.volley.ObjectResult;
import com.ydd.zhichat.volley.Result;
import com.ydd.zhichat.volley.StringJsonArrayRequest.Listener;
import com.ydd.zhichat.volley.StringJsonObjectRequest;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 保存数据到文件<br/>
 * 1、公共消息最近的一次刷新出来的数据
 */
public class FileDataHelper {

    // 个人数据文件 文件前缀名
    public static final String FILE_BUSINESS_CIRCLE = "business_circle";// 商务圈

    public static Map<String, Boolean> isWritingMaps = new HashMap<String, Boolean>();

    public static void writeFileData(final Context context, final String fileName, final com.xuan.xuanhttplibrary.okhttp.result.Result result) {
        if (result == null) {// 数据错误
            return;
        }

        if (result.getResultCode() < Result.CODE_SUCCESS) {
            return;
        }

        Boolean isWriting = isWritingMaps.get(fileName);
        if (isWriting != null && isWriting) {// 正在写，防止很短的时间内重复写入数据
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                isWritingMaps.put(fileName, true);
                FileOutputStream os = null;
                try {
                    os = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (os == null) {
                    isWritingMaps.put(fileName, false);
                    return;
                }
                String body = JSON.toJSONString(result);
                byte[] buffer = body.getBytes();
                try {
                    os.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    isWritingMaps.put(fileName, false);
                }
            }
        }).start();
    }

    /**
     * @param context
     * @param ownerId
     * @param fileName
     * @param result
     */
    public static void writeFileData(final Context context, String ownerId, String fileName, final com.xuan.xuanhttplibrary.okhttp.result.Result result) {
        String ownerFileName = fileName + ownerId;
        writeFileData(context, ownerFileName, result);
    }

    // //////////////////////////////////////////////// Read部分

    public static <T> void readArrayData(Context context, String ownerId, String fileName, Listener<T> listener, Class<T> clazz) {
        String ownerFileName = fileName + ownerId;
        readArrayData(context, ownerFileName, listener, clazz);
    }

    /**
     * @param context
     * @param fileName
     */
    public static <T> void readArrayData(final Context context, final String fileName, final Listener<T> listener, final Class<T> clazz) {
        if (listener == null) {// 如果监听为null，那么读取数据就没意义，直接返回
            return;
        }

        final Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(context.openFileInput(fileName)));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (reader == null) {
                    post(handler, listener, null);
                    return;
                }
                String data = "";
                String temp = null;
                try {
                    while ((temp = reader.readLine()) != null) {
                        data += temp;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (TextUtils.isEmpty(data)) {
                    post(handler, listener, null);
                } else {
                    try {
                        if (TextUtils.isEmpty(data)) {
                            post(handler, listener, null);
                            return;
                        }
                        ArrayResult<T> result = new ArrayResult<T>();
                        JSONObject jsonObject = JSON.parseObject(data);
                        result.setResultCode(jsonObject.getIntValue(Result.RESULT_CODE));
                        result.setResultMsg(jsonObject.getString(Result.RESULT_MSG));

                        String dataObject = jsonObject.getString(Result.DATA);
                        if (!TextUtils.isEmpty(data)) {
                            result.setData(JSON.parseArray(dataObject, clazz));
                        }
                        post(handler, listener, result);
                    } catch (Exception e) {
                        e.printStackTrace();
                        post(handler, listener, null);
                    }
                }
            }
        }).start();
    }

    private static <T> void post(Handler handler, final Listener<T> listener, final ArrayResult<T> result) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onResponse(result);
            }
        });
    }

    private static <T> void post(Handler handler, final StringJsonObjectRequest.Listener<T> listener, final ObjectResult<T> result) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onResponse(result);
            }
        });
    }

    public static <T> void readObjectData(Context context, String ownerId, String fileName, StringJsonObjectRequest.Listener<T> listener,
                                          Class<T> clazz) {
        String ownerFileName = fileName + ownerId;
        readObjectData(context, ownerFileName, listener, clazz);
    }

    /**
     * @param context
     * @param fileName
     */
    public static <T> void readObjectData(final Context context, final String fileName,
                                          final StringJsonObjectRequest.Listener<T> listener, final Class<T> clazz) {
        if (listener == null) {// 如果监听为null，那么读取数据就没意义，直接返回
            return;
        }
        final Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(context.openFileInput(fileName)));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (reader == null) {
                    post(handler, listener, null);
                    return;
                }
                String data = "";
                String temp = null;
                try {
                    while ((temp = reader.readLine()) != null) {
                        data += temp;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (TextUtils.isEmpty(data)) {
                    post(handler, listener, null);
                } else {
                    try {
                        if (TextUtils.isEmpty(data)) {
                            post(handler, listener, null);
                            return;
                        }
                        ObjectResult<T> result = new ObjectResult<T>();
                        JSONObject jsonObject = JSON.parseObject(data);
                        result.setResultCode(jsonObject.getIntValue(Result.RESULT_CODE));
                        result.setResultMsg(jsonObject.getString(Result.RESULT_MSG));

                        String dataObject = jsonObject.getString(Result.DATA);
                        if (!TextUtils.isEmpty(data)) {
                            result.setData(JSON.parseObject(dataObject, clazz));
                        }
                        post(handler, listener, result);
                    } catch (Exception e) {
                        e.printStackTrace();
                        post(handler, listener, null);
                    }
                }
            }
        }).start();
    }

    /**
     * @param context
     * @param fileName
     */
    public static <T> ObjectResult<T> readObjectDataSync(final Context context, final String fileName, final Class<T> clazz) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(context.openFileInput(fileName)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (reader == null) {
            return null;
        }
        String data = "";
        String temp = null;
        try {
            while ((temp = reader.readLine()) != null) {
                data += temp;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (TextUtils.isEmpty(data)) {
            return null;
        } else {
            try {
                if (TextUtils.isEmpty(data)) {
                    return null;
                }
                ObjectResult<T> result = new ObjectResult<T>();
                JSONObject jsonObject = JSON.parseObject(data);
                result.setResultCode(jsonObject.getIntValue(Result.RESULT_CODE));
                result.setResultMsg(jsonObject.getString(Result.RESULT_MSG));

                String dataObject = jsonObject.getString(Result.DATA);
                if (!TextUtils.isEmpty(data)) {
                    result.setData(JSON.parseObject(dataObject, clazz));
                }
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * 返回图片的高宽
     *
     * @return
     */
    public static int[] getImageParamByIntsFile(String filePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        int[] param = new int[]{bitmap.getWidth(), bitmap.getHeight()};
        bitmap.recycle();
        return param;
    }
}
