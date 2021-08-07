package com.ydd.zhichat.ui.base;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.ui.SplashActivity;
import com.ydd.zhichat.util.PreferenceUtils;

import java.util.List;
import java.util.Stack;

public class ActivityStack {
    private static ActivityStack instance;
    public boolean isBoot;
    private Stack<Activity> stack;

    private ActivityStack() {
        stack = new Stack<Activity>();
    }

    public static ActivityStack getInstance() {
        if (instance == null) {
            synchronized (ActivityStack.class) {
                if (instance == null) {
                    instance = new ActivityStack();
                }
            }
        }
        return instance;
    }

    //判断某一个类是否存在任务栈里面
    public boolean isExistMainActivity(Activity activity) {
        boolean flag = false;
        isBoot = PreferenceUtils.getBoolean(activity, "isBoot");
        if (isBoot && activity.equals(SplashActivity.class)) {
            Intent intent = new Intent(MyApplication.getContext(), activity.getClass());
            ComponentName cmpName = intent.resolveActivity(MyApplication.getContext().getPackageManager());
            if (cmpName != null) { // 说明系统中存在这个activity
                ActivityManager am = (ActivityManager) MyApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> taskInfoList = am.getRunningTasks(10); //获取从栈顶开始往下查找的10个activity
                for (ActivityManager.RunningTaskInfo taskInfo : taskInfoList) {
                    if (taskInfo.baseActivity.equals(cmpName)) { // 说明它已经启动了
                        flag = true;
                        break; //跳出循环，优化效率
                    }
                }
            }

        }
        return flag;
    }

    public void push(Activity activity) {

        stack.add(activity);

    }

    public void pop(Activity activity) {
        if (activity != null) {
            stack.remove(activity);
        }
    }

    public boolean has() {
        return stack.size() > 0;
    }

    /*
      靠不住，activity栈里的activity不一定是stop状态，可能存在destroy状态，
      比如 A -> B -> C, C崩溃，安卓自动恢复activity栈，出现A -> B，然后初始化B，而A就保持destroy,
      模拟可以使用开发者选项里的“不保留活动”，
      或者kill命令杀进程模拟崩溃，
     */
    @Deprecated
    public void exit() {
        for (int i = 0; i < stack.size(); i++) {
            Activity activity = stack.get(i);
            stack.remove(i);
            i--;
            if (activity != null) {
                activity.finish();
                activity = null;
            }
        }
    }

    public Activity getActivity(int position) {
        return stack.get(position);
    }

    public int size() {
        return stack.size();
    }

    /**
     * 获取上一个activity
     *
     * @return
     */
    public Activity getPreActivity() {
        return this.stack.get(this.stack.size() - 2);
    }
}
