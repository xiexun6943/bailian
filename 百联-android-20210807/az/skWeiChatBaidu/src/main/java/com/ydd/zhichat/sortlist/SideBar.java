package com.ydd.zhichat.sortlist;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.ydd.zhichat.R;

import java.util.HashMap;
import java.util.Map;

public class SideBar extends View {
    // 触摸事件
    private OnTouchingLetterChangedListener onTouchingLetterChangedListener;
    // 26个字母和#,首字母不是英文字母的放到#分类
    public static String[] b = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W",
            "X", "Y", "Z", "#"};
    private int choose = -1;// 选中
    private Paint paint = new Paint();

    private TextView mTextDialog;

    private Map<String, Integer> isExistMap;

    public void setExistMap(Map<String, Integer> existMap) {
        isExistMap = existMap;
        invalidate();
    }

    public void addExist(String alphaet) {// 存在的Count+1
        int count = 0;
        if (isExistMap.containsKey(alphaet)) {
            count = isExistMap.get(alphaet);
        }
        count++;
        isExistMap.put(alphaet, count);
    }

    public void removeExist(String alphaet) {// 存在的Count-1,存在才减1，不存在则移除
        int count = 0;
        if (isExistMap.containsKey(alphaet)) {
            count = isExistMap.get(alphaet);
        }
        if (count > 0) {
            count--;
        }
        if (count > 0) {
            isExistMap.put(alphaet, count);
        } else {
            isExistMap.remove(alphaet);
        }
    }

    public void clearExist() {
        isExistMap.clear();
    }

    public void setTextView(TextView mTextDialog) {
        this.mTextDialog = mTextDialog;
    }

    public SideBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        isExistMap = new HashMap<String, Integer>();
    }

    public SideBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        isExistMap = new HashMap<String, Integer>();
    }

    public SideBar(Context context) {
        super(context);
        isExistMap = new HashMap<String, Integer>();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 获取焦点改变背景颜色.
        int height = getHeight();// 获取对应高度
        int width = getWidth(); // 获取对应宽度
        int singleHeight = height / b.length;// 获取每一个字母的高度

        for (int i = 0; i < b.length; i++) {
            paint.setColor(Color.parseColor("#555555"));
            paint.setTypeface(Typeface.DEFAULT);
            paint.setAntiAlias(true);
            paint.setTextSize(28);
            // 选中的状态
            if (i == choose) {
                paint.setColor(Color.parseColor("#4FC557"));
                paint.setFakeBoldText(true);
            }
            // x坐标等于中间-字符串宽度的一半.
            float xPos = width / 2 - paint.measureText(b[i]) / 2;
            float yPos = singleHeight * i + singleHeight;
            canvas.drawText(b[i], xPos, yPos, paint);
            paint.reset();// 重置画笔
        }

    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final float y = event.getY();// 点击y坐标
        final int oldChoose = choose;
        final int c = (int) (y / getHeight() * b.length);// 点击y坐标所占总高度的比例*b数组的长度就等于点击b中的个数.

        switch (action) {
            case MotionEvent.ACTION_UP:
                // setBackgroundDrawable(new ColorDrawable(0x00000000));
                setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.app_white)));
                choose = -1;//
                invalidate();
                if (mTextDialog != null) {
                    mTextDialog.setVisibility(View.INVISIBLE);
                }
                break;
            default:
                setBackgroundResource(R.drawable.sidebar_background);
                if (oldChoose != c) {
                    if (c >= 0 && c < b.length) {
                        if (onTouchingLetterChangedListener != null) {
                            int count = 0;
                            if (isExistMap.containsKey(b[c])) {
                                count = isExistMap.get(b[c]);
                            }
                            if (count > 0) {
                                onTouchingLetterChangedListener.onTouchingLetterChanged(b[c]);
                                if (mTextDialog != null) {
                                    mTextDialog.setText(b[c]);
                                    mTextDialog.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                        choose = c;
                        invalidate();
                    }
                }

                break;
        }
        return true;
    }

    /**
     * 向外公开的方法
     *
     * @param onTouchingLetterChangedListener
     */
    public void setOnTouchingLetterChangedListener(OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
        this.onTouchingLetterChangedListener = onTouchingLetterChangedListener;
    }

    /**
     * 接口
     *
     * @author coder
     */
    public interface OnTouchingLetterChangedListener {
        public void onTouchingLetterChanged(String s);
    }

}