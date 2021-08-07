package com.ydd.zhichat.util;

/**
 * 按钮点击事件过滤 - 防止连续点击打开两个相同的页面
 */
public class ClickFilter {
    private static final long INTERVAL = 800L; //防止连续点击的时间间隔
    private static long lastClickTime = 0L; //上一次点击的时间

    public static boolean isFastDoubleClick() {
        return isFastDoubleClick(INTERVAL);
    }

    public static boolean isFastDoubleClick(long interval) {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (0 < timeD && timeD < interval) {
            return true;
        } else {
            lastClickTime = time;
            return false;
        }
    }

}
