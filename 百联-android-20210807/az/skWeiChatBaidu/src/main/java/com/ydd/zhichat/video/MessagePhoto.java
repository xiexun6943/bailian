package com.ydd.zhichat.video;

import android.graphics.Bitmap;

public class MessagePhoto {
    private Bitmap bitmap;
    private String path;

    public MessagePhoto(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
//
//    public String getPath() {
//        return path;
//    }
}
