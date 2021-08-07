package com.ydd.zhichat.util;

import android.graphics.Color;
import android.text.TextUtils;


import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ChenGY on 2017/10/27.
 */
public class StrNumUtil {

    //region Str To Other

    /**
     * String转Int
     */
    public static int Str2Int(String str) {
        int i = 0;
        if (!TextUtils.isEmpty(str)) {
            try {
                i = (int) Str2Float(str);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return i;
    }

    /**
     * String转Float
     */
    public static float Str2Float(String str) {
        float f = 0f;
        if (!TextUtils.isEmpty(str)) {
            try {
                f = Float.parseFloat(str);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return f;
    }

    /**
     * String转Double
     */
    public static double Str2Double(String str) {
        double d = 0d;
        if (!TextUtils.isEmpty(str)) {
            try {
                d = Double.parseDouble(str);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return d;
    }

    /**
     * String转Long
     */
    public static long Str2Long(String str) {
        long l = 0;
        if (!TextUtils.isEmpty(str)) {
            try {
                l = Long.parseLong(str);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return l;
    }

    /**
     * String转Int
     */
    public static int DoubleStr2Int(String str) {
        int i = 0;
        if (!TextUtils.isEmpty(str)) {
            try {
                double d = Double.parseDouble(str);
                i = (int) d;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return i;
    }

    /**
     * String转Color Int
     */
    public static int colorStr2ColorInt(String str) {
        int i = 0xffffff;
        if (!TextUtils.isEmpty(str)) {
            try {
                i = Color.parseColor(str);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return i;
    }

    //endregion

    //region random

    /**
     * 获取随机Int的字符串
     */
    public static String getRandomIntStr(int rang) {
        return (int) (Math.random() * rang) + "";
    }

    /**
     * 获取随机Int值
     */
    public static int getRandomInt(int rang) {
        return (int) (Math.random() * rang);
    }

    /**
     * 获取随机boolean值
     */
    public static boolean getRandomBoolean() {
        return ((int) (Math.random() * 1000)) % 2 == 0;
    }

    /**
     * 获取随机Str值
     */
    public static String getRandomStr(String str1, String str2) {
        return getRandomBoolean() ? str1 : str2;
    }

    /**
     * 获取随机Str值
     */
    public static String getRandomStr(String[] strs) {
        if (strs != null && strs.length > 0) {
            int i = getRandomInt(1000) % strs.length;
            return strs[i];
        } else {
            return "";
        }
    }

    //endregion

    //region empty

    /**
     * 判空获取值
     */
    public static String getEmptyStr(Object obj) {
        String result = "";
        if (obj != null) {
            String str = obj.toString();
            if (!TextUtils.isEmpty(str)) {
                result = str;
            }
        }
        return result;
    }

    /**
     * 判空获取值（需要的）
     */
    public static String getEmptyStr(Object obj, String need) {
        String result = need;
        if (obj != null) {
            String str = obj.toString();
            if (!TextUtils.isEmpty(str)) {
                result = str;
            }
        }
        return result;
    }

    /**
     * 判空获取值
     */
    public static String getEmptyStr(String str) {
        String result = "";
        if (!TextUtils.isEmpty(str)) {
            result = str;
        }
        return result;
    }

    /**
     * 判空获取值（需要的）
     */
    public static String getEmptyStr(String str, String need) {
        String result = need;
        if (!TextUtils.isEmpty(str)) {
            result = str;
        }
        return result;
    }

    //endregion

    //region other

    /**
     * 比较两个long字符串的大小
     */
    public static long compareTwoStringLong(String str1, String str2) {
        return Str2Long(str1) - Str2Long(str2);
    }

    /**
     * 简单的隐藏一位小数.0
     */
    public static String formatMoney(String money) {
        String result = "0";
        if (!TextUtils.isEmpty(money)) {
            if (money.endsWith(".0")) {
                result = money.substring(0, money.length() - 2);
            }
        }
        return result;
    }

    /**
     * list判空
     */
    public static boolean notEmptyList(List list) {
        return list != null && list.size() > 0;
    }

    /**
     * 将数字转换成字母(0代表A)
     */
    public static String numToLetter(int num) {
        return (char) (num + 65) + "";
    }

    /**
     * 俩int数据相除得float
     */
    public static float twoIntdivideFloat(int a, int b) {
        return ((float) a) / ((float) b);
    }

    /**
     * 俩String 格式int数据相乘
     */
    public static String twoIntStrMutipyFloat(String a, String b) {
        return Str2Int(a) * Str2Int(b) + "";
    }

    /**
     * 俩double字符串数据相乘得int字符串
     */
    public static String twoDoubleStrMultipyInt(String str1, String str2) {
        return (int) (Str2Double(str1) * Str2Double(str2)) + "";
    }

    /**
     * 俩double字符串数据相乘,保留两位小数
     */
    public static String twoDoubleStrMultipy(String str1, String str2) {
        return keepTwoDecimal(Str2Double(str1) * Str2Double(str2));
    }

    /**
     * 俩double字符串数据相乘,保留两位小数（百分比）
     */
    public static String twoDoubleStrMultipy2(String str1, String str2) {
        return keepTwoDecimal(Str2Double(str1) * Str2Double(str2) / 100);
    }

    /**
     * 比较两个double字符串的大小
     */
    public static double compareTwoStringDouble(String str1, String str2) {
        return Str2Double(str1) - Str2Double(str2);
    }

    /**
     * 比较两个double字符串的大小2
     */
    public static double compareTwoStringDouble2(String str1, String str2) {
        return Str2Double(str1) - Str2Double(str2) * 10000;
    }

    //endregion

    //region money

    /**
     * 保留两位小数——四舍五入
     */
    public static float roundNormalTwoPlace(float res) {
        try {
            return new BigDecimal(res).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 取整——四舍五入
     */
    public static String roundNormalZeroPlace(String res) {
        try {
            return new BigDecimal(res).setScale(0, BigDecimal.ROUND_UP).toString();
        } catch (Exception e) {
            return "0";
        }
    }

    /**
     * 保留两位小数——进位
     */
    public static double roundUpTwoPlace(double res) {
        try {
            return new BigDecimal(res).setScale(2, BigDecimal.ROUND_UP).doubleValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 保留两位小数——进位
     */
    public static float roundUpTwoPlace(float res) {
        try {
            return new BigDecimal(res).setScale(2, BigDecimal.ROUND_UP).floatValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 保留两位小数——舍位
     */
    public static double roundDownTwoPlace(double res) {
        try {
            return new BigDecimal(res).setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 保留两位小数——舍位
     */
    public static float roundDownTwoPlace(float res) {
        try {
            return new BigDecimal(res).setScale(2, BigDecimal.ROUND_DOWN).floatValue();
        } catch (Exception e) {
            return 0;
        }
    }

    //endregion

    //region split

    /**
     * 分割字符串只要前一个
     */
    public static String splitFront(String res, String reg) {
        String result = "";
        if (!TextUtils.isEmpty(res)) {
            String[] strs = res.split(reg);
            if (strs.length == 0) {//有reg且reg是末尾，也就是res == reg
            } else {
                if (strs.length == 1) {//没有reg || 空串 || 末尾是reg
                    if (!res.contains(reg)) {//没有reg || 空串
                    } else {//末尾是reg
                    }
                } else {//有reg且reg不是末尾，reg前面可以没有值
                }
                result = strs[0];
            }
        }
        return result;
    }

    /**
     * 分割字符串只要后一个
     */
    public static String splitBehind(String res, String reg) {
        String result = "";
        if (!TextUtils.isEmpty(res)) {
            String[] strs = res.split(reg);
            if (strs.length == 0) {//有reg且reg是末尾，也就是res == reg
            } else {
                if (strs.length == 1) {//没有reg || 空串 || 末尾是reg
                    if (!res.contains(reg)) {//没有reg || 空串
                    } else {//末尾是reg
                    }
                } else {//有reg且reg不是末尾，reg前面可以没有值
                    result = strs[1];
                }
            }
        }
        return result;
    }

    /**
     * 截取值防报错
     */
    public static String getSubStr(String str, int start, int end) {
        String result = "";
        try {
            result = str.substring(start, end);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 截取值防报错
     */
    public static String getSubStr(String str, int start) {
        String result = "";
        try {
            result = str.substring(start);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 分割字符串只要从某个字段开始
     */
    public static String subStringFromReg(String res, String reg) {
        String result = "";
        if (!TextUtils.isEmpty(res)) {
            int start = res.indexOf(reg);
            if (start > -1) {
                result = getSubStr(res, start);
            }
        }
        return result;
    }

    //endregion

    //region 保留固定位数小数

    /**
     * 保留X位小数
     */
    public static String keepXDecimal(Double res, int place) {
        StringBuilder pattern = new StringBuilder("0");
        if (place > 0) {
            for (int i = 0; i < place; i++) {
                if (i == 0) {
                    pattern.append(".0");
                } else {
                    pattern.append("0");
                }
            }
        }
        return new DecimalFormat(pattern.toString()).format(res);
    }

    /**
     * 保留一位小数
     */
    public static String keepOneDecimal(String res) {
        return keepXDecimal(Str2Double(res), 1);
    }

    /**
     * 保留两位小数
     */
    public static String keepTwoDecimal(Double res) {
        return keepXDecimal(res, 2);
    }

    /**
     * 保留两位小数
     */
    public static String keepTwoDecimal(String res) {
        return keepTwoDecimal(Str2Double(res));
    }

    /**
     * 保留两位小数
     */
    public static String keepTwoDecimal(Float res) {
        return keepTwoDecimal(res.doubleValue());
    }

    /**
     * 保留八位小数
     */
    public static String keepEightDecimal(Double res) {
        return keepXDecimal(res, 8);
    }

    /**
     * 保留八位小数
     */
    public static String keepEightDecimal(String res) {
        return keepEightDecimal(Str2Double(res));
    }

    /**
     * 保留整数，并每隔3位一个分隔符
     */
    public static String keepZeroDecimalAddDouhao(String res) {
        return new DecimalFormat("###,###").format(Str2Int(res));
    }

    /**
     * 除以10000后，保留两位小数
     */
    public static String keepTwoDecimalDivideWan(Float res) {
        return keepTwoDecimal(res / 10000f);
    }

    /**
     * 除以10000后，保留两位小数
     */
    public static String keepTwoDecimalDivideWan(String res) {
        return keepTwoDecimalDivideWan(Str2Float(res));
    }

    /**
     * 除以1000保留整数(13位时间戳转10位)
     */
    public static String divideQian(String res) {
        return (int) (Str2Float(res) / 1000f) + "";
    }

    /**
     * 乘以10000
     *
     * @param res
     * @return
     */
    public static String multiWan(String res) {
        return String.valueOf(Str2Float(res) * 10000f);
    }

    //endregion

    //region other more


    public static ArrayList<String> toArrayList(List<String> list) {
        ArrayList<String> result = new ArrayList<>();
        if (notEmptyList(list)) {
            result.addAll(list);
        }
        return result;
    }

    //endregion
}