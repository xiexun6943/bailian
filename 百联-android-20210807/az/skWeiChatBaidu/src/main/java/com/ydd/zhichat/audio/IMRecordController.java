package com.ydd.zhichat.audio;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.ydd.zhichat.R;

public class IMRecordController implements View.OnTouchListener, RecordStateListener {
    private final int UP_MOVE_CHECK_NUM = 80;
    private Context mContext;
    private RecordPopWindow mRecordPopWindow;
    private long mLastTouchUpTime = System.currentTimeMillis();
    private RecordManager mRecordManager;
    private int mLastY = 0;
    private int timeLen;
    private boolean isRun = true;
    private RecordListener mRecordListener;

    public IMRecordController(Context context) {
        mContext = context;
        mRecordPopWindow = new RecordPopWindow(mContext);
        mRecordManager = RecordManager.getInstance();
        mRecordManager.setVoiceVolumeListener(this);
    }

    private boolean canVoice() {
        long now = System.currentTimeMillis();
        return now - mLastTouchUpTime > 100;
    }

    public void setRecordListener(RecordListener listener) {
        mRecordListener = listener;
    }

    /**
     * 判断是否在上滑
     *
     * @param y
     * @return
     */
    private boolean upMove(int y) {
        if ((mLastY - y) > UP_MOVE_CHECK_NUM) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isRun) {
                mLastY = (int) event.getY();
                if (canVoice() && !mRecordManager.isRunning()) {
                    if (mRecordListener != null) {
                        mRecordListener.onRecordStart();
                    }
                    mRecordPopWindow.startRecord();
                    mRecordManager.startRecord();
                    final MotionEvent ev = event;
                    ev.setAction(MotionEvent.ACTION_MOVE);
                    v.dispatchTouchEvent(ev);
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (isRun) {
                if (mRecordManager.isRunning()) {
                    if (!mRecordPopWindow.isRubishVoiceImgShow()) {
                        if (upMove((int) event.getY())) {
                            mRecordPopWindow.setRubishTip();
                        }
                    } else {
                        if (!upMove((int) event.getY())) {
                            mRecordPopWindow.hideRubishTip();
                        }
                    }
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            mLastY = 0;
            if (isRun) {
                if (mRecordManager.isRunning()) {
                    mLastTouchUpTime = System.currentTimeMillis();
                }
                if (mRecordPopWindow.isRubishVoiceImgShow()) {
                    mRecordManager.cancel();
                } else {
                    // ACTION_CANCEL 可能情况是这个录音已经stop, 再次调用stop会导致native崩溃，
                    if (mRecordManager.isRunning()) {
                        mRecordManager.stop();
                    }
                }
            }
            isRun = true;
        }
        return true;
    }

    @Override
    public void onRecordStarting() {
        mRecordPopWindow.show();

    }

    @Override
    public void onRecordStart() {

    }

    @Override
    public void onRecordFinish(String file) {
        mRecordPopWindow.dismiss();
        if (mRecordListener != null) {
            mRecordListener.onRecordSuccess(file, timeLen);
        }
    }

    @Override
    public void onRecordCancel() {
        mRecordPopWindow.dismiss();
        if (mRecordListener != null) {
            mRecordListener.onRecordCancel();
        }
    }

    @Override
    public void onRecordVolumeChange(int v) {
        Log.d("roamer", "v:" + v);
/*
        int level = v / 1300;

        if (level < 0) {
            level = 0;
        } else if (level > 15) {
            level = 15;
        }
        mRecordPopWindow.setVoicePercent(level);
*/
        int level = v / 1000;
        Log.d("roamer", "level1:" + level);
        if (level < 1) {
            level = 1;
            Log.d("roamer", "level2:" + level);
        } else if (level > 7) {
            level = 7;
            Log.d("roamer", "level2:" + level);
        }
        mRecordPopWindow.setVoicePercent(level);
    }

    @Override
    public void onRecordTimeChange(int seconds) {
        mRecordPopWindow.setVoiceSecond(seconds);
        if (seconds >= 60) {//在录音到60s时让其自动发送
            timeLen = 60;
            isRun = false;//设置为false,让触摸动作不可用,防止程序的OOM
            if (mRecordManager.isRunning()) {
                mLastTouchUpTime = System.currentTimeMillis();
            }
            if (mRecordPopWindow.isRubishVoiceImgShow()) {
                mRecordManager.cancel();
            } else {
                mRecordManager.stop();
            }
        } else {
            timeLen = seconds;
        }
    }

    @Override
    public void onRecordTooShoot() {
        mRecordPopWindow.dismiss();
        if (mRecordListener != null) {
            mRecordListener.onRecordCancel();
        }
        Toast.makeText(mContext, R.string.tip_record_time_too_short, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordError() {
        mRecordPopWindow.dismiss();
        if (mRecordListener != null) {
            mRecordListener.onRecordCancel();
        }
        Toast.makeText(mContext, R.string.tip_voice_record_error, Toast.LENGTH_SHORT).show();
    }

    public void cancel() {
        if (mRecordPopWindow != null) {
            mRecordPopWindow.dismiss();
        }
        if (mRecordManager != null) {
            mRecordManager.cancel();
        }
    }
}
