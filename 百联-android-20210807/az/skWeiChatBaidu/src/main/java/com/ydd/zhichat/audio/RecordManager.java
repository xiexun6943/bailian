package com.ydd.zhichat.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.media.MediaRecorder.OutputFormat;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.util.FileUtil;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class RecordManager {
    private static final String TAG = "RecordManager";

    private static final int MSG_VOICE_CHANGE = 1;
    private static RecordManager instance;

    private RecordStateListener listener;
    private MediaRecorder mr;
    private String name;
    // private Thread voiceVolumeListener;
    // private static ExecutorService pool;
    private Handler handler = new Handler(new Handler.Callback() {
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_VOICE_CHANGE) {
                if (listener != null) {
                    listener.onRecordVolumeChange((Integer) msg.obj);
                }
            }
            return false;
        }
    });
    private long startTime = System.currentTimeMillis();
    private Timer timer = new Timer();
    private boolean running = false;
    private boolean mAudioFocus;
    private AudioManager mAudioManager;
    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.i(TAG, "AudioFocusChange AUDIOFOCUS_GAIN");
                    mAudioFocus = true;
                    requestAudioFocus();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                    Log.i(TAG, "AudioFocusChange AUDIOFOCUS_GAIN_TRANSIENT");
                    mAudioFocus = true;
                    requestAudioFocus();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                    Log.i(TAG, "AudioFocusChange AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK");
                    mAudioFocus = true;
                    requestAudioFocus();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    Log.i(TAG, "AudioFocusChange AUDIOFOCUS_LOSS");
                    mAudioFocus = false;
                    abandonAudioFocus();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Log.i(TAG, "AudioFocusChange AUDIOFOCUS_LOSS_TRANSIENT");
                    mAudioFocus = false;
                    abandonAudioFocus();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    Log.i(TAG, "AudioFocusChange AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    mAudioFocus = false;
                    abandonAudioFocus();
                    break;
                default:
                    Log.i(TAG, "AudioFocusChange focus = " + focusChange);
                    break;
            }
        }
    };

    private RecordManager() {
    }

    public static RecordManager getInstance() {
        if (instance == null) {
            instance = new RecordManager();
        }
        return instance;
    }

    private void notifyStartLoading() {
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onRecordStarting();
                }
            });
        }
    }

    private void notifyTooShoot() {
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onRecordTooShoot();
                }
            });
        }
    }

    private void notifyStart() {
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onRecordStart();
                }
            });
        }
    }

    private void notifyFinish(final String file) {
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onRecordFinish(file);
                }
            });
        }
    }

    private void notifyCancal() {
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onRecordCancel();
                }
            });
        }
    }

    private void notifyVoiceChange(int v) {
        Message message = new Message();
        message.what = MSG_VOICE_CHANGE;
        message.obj = v;
        handler.sendMessage(message);
    }

    public boolean isRunning() {
        return running;
    }

    @SuppressWarnings("deprecation")
    public void startRecord() {
        // Thread recordThread = new Thread(new Runnable() {

        // @Override
        // public void run() {
        try {
            requestAudioFocus();
            notifyStartLoading();
            mr = new MediaRecorder();
            mr.setAudioSource(AudioSource.MIC);
            // 设置音源,这里是来自麦克风,虽然有VOICE_CALL,但经真机测试,不行
            mr.setOutputFormat(OutputFormat.RAW_AMR);
            // 输出格式
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            name = FileUtil.getRandomAudioAmrFilePath();

            if (TextUtils.isEmpty(name)) {
                notifyError();
                return;
            }
            // 编码
            mr.setOutputFile(name);
            mr.prepare();
            notifyStart();
            // 做些准备工作
            mr.start();
            startTime = System.currentTimeMillis();
            running = true;
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    int i = mr.getMaxAmplitude();
                    if (listener != null) {
                        int seconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
                        notifyVoiceSecondsChange(seconds);
                        notifyVoiceChange(i);
                    }
                }
            }, 0, 100);
        } catch (Exception e) {
            e.printStackTrace();
            notifyError();
        }
    }

    private void notifyError() {
        handler.post(new Runnable() {
            public void run() {
                listener.onRecordError();
            }
        });
    }

    private void notifyVoiceSecondsChange(final int seconds) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onRecordTimeChange(seconds);
            }
        });
    }

    private void stopVolumeListener() {
        if (timer != null) {
            timer.cancel();
        }
    }

    public void waitRunning() {
        /*
         * while (!running) { try { Thread.sleep(10); } catch
         * (InterruptedException e) { e.printStackTrace(); } }
         */
    }

    public synchronized String stop() {
        stopVolumeListener();
        if (mr != null) {
            try {
                // 这里native崩溃无法catch, 所以调用stop时一定要确保mr没有释放，
                mr.stop();
                mr.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            long delay = System.currentTimeMillis() - startTime;

            if (delay <= 500) {
                notifyTooShoot();
            } else {
                notifyFinish(name);
            }
        } else {
            notifyCancal();
        }
        running = false;
        abandonAudioFocus();
        return name;
    }

    public synchronized void cancel() {
        stopVolumeListener();
        if (mr != null) {
            try {
                mr.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            File file = new File(name);
            file.deleteOnExit();
            notifyCancal();
        }
        running = false;
        abandonAudioFocus();
    }

    public void setVoiceVolumeListener(RecordStateListener listener) {
        this.listener = listener;
    }

    private void requestAudioFocus() {
        Log.v(TAG, "requestAudioFocus mAudioFocus = " + mAudioFocus);
        if (!mAudioFocus) {
            int result = getAudioManager().requestAudioFocus(afChangeListener,
                    AudioManager.STREAM_MUSIC, // Use the music stream.
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = true;
            } else {
                Log.e(TAG, "AudioManager request Audio Focus result = " + result);
            }
        }
    }

    private void abandonAudioFocus() {
        Log.v(TAG, "abandonAudioFocus mAudioFocus = " + mAudioFocus);
        if (mAudioFocus) {
            getAudioManager().abandonAudioFocus(afChangeListener);
            mAudioFocus = false;
        }
    }

    private AudioManager getAudioManager() {
        if (mAudioManager == null) {
            synchronized (this) {
                if (mAudioManager == null) {
                    mAudioManager = (AudioManager) MyApplication.getContext()
                            .getSystemService(Context.AUDIO_SERVICE);
                }
            }
        }
        return mAudioManager;
    }

}
