package com.ydd.zhichat.util;

import android.content.Context;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.bean.message.XmppMessage;
import com.ydd.zhichat.db.InternationalizationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ydd.zhichat.bean.message.XmppMessage.TYPE_CHAT_HISTORY;
import static com.ydd.zhichat.bean.message.XmppMessage.TYPE_IMAGE_TEXT;
import static com.ydd.zhichat.bean.message.XmppMessage.TYPE_IMAGE_TEXT_MANY;

public class StringUtils {
    private final static Pattern email_pattern = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
    private static final String SEP1 = "#";
    private static final String SEP2 = "|";
    private static final String SEP3 = "=";
    static Pattern phoneNumberPat = Pattern.compile("^((13[0-9])|(147)|(15[0-3,5-9])|(17[0,6-8])|(18[0-9]))\\d{8}$");
    static Pattern nickNamePat = Pattern.compile("^[\u4e00-\u9fa5_a-zA-Z0-9_]{3,15}$");// 3-10个字符
    static Pattern searchNickNamePat = Pattern.compile("^[\u4e00-\u9fa5_a-zA-Z0-9_]*$");// 不限制长度，可以为空字符串
    static Pattern companyNamePat = Pattern.compile("^[\u4e00-\u9fa5_a-zA-Z0-9_]{3,50}$");// 3-50个字符

    /**
     * EditText显示Error
     *
     * @param context
     * @param resId
     * @return
     */
    public static CharSequence editTextHtmlErrorTip(Context context, int resId) {
        CharSequence html = Html.fromHtml("<font color='red'>" + context.getString(resId) + "</font>");
        return html;
    }

    public static CharSequence editTextHtmlErrorTip(Context context, String text) {
        CharSequence html = Html.fromHtml("<font color='red'>" + text + "</font>");
        return html;
    }

    /* 是否是手机号 */
    public static boolean isMobileNumber(String mobiles) {
        // 正则匹配不行，新号段和国外都不行，
        return true;
/*
        Matcher mat = phoneNumberPat.matcher(mobiles);
        return mat.matches();
*/
    }

    /* 检测之前，最好检测是否为空。检测是否是正确的昵称格式 */
    public static boolean isNickName(String nickName) {
        if (TextUtils.isEmpty(nickName)) {
            return false;
        }
        Matcher mat = nickNamePat.matcher(nickName);
        return mat.matches();
    }

    public static boolean isSearchNickName(String nickName) {
        if (nickName == null) {// 防止异常
            return false;
        }
        Matcher mat = searchNickNamePat.matcher(nickName);
        return mat.matches();
    }

    public static boolean isCompanyName(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        Matcher mat = companyNamePat.matcher(name);
        return mat.matches();
    }

    public static SpannableString matcherSearchTitle(int color, String text, String keyword) {
        String str = text.toLowerCase();
        String key = "";
        if (!TextUtils.isEmpty(keyword)) {
            key = keyword.toLowerCase();
        }
        Pattern pattern = Pattern.compile(key);
        Matcher matcher = pattern.matcher(str);
        SpannableString spannableString = new SpannableString(text);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            spannableString.setSpan(new ForegroundColorSpan(color), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }

    /**
     * 判断是不是一个合法的电子邮件地址
     *
     * @param email
     * @return
     */
    public static boolean isEmail(String email) {
        if (email == null || email.trim().length() == 0)
            return false;
        return email_pattern.matcher(email).matches();
    }

    public static boolean strEquals(String s1, String s2) {
        if (s1 == s2) {// 引用相等直接返回true
            return true;
        }
        boolean emptyS1 = s1 == null || s1.trim().length() == 0;
        boolean emptyS2 = s2 == null || s2.trim().length() == 0;
        if (emptyS1 && emptyS2) {// 都为空，认为相等
            return true;
        }
        if (s1 != null) {
            return s1.equals(s2);
        }
        if (s2 != null) {
            return s2.equals(s1);
        }
        return false;
    }

    /**
     * 去掉特殊字符
     */
    public static String replaceSpecialChar(String str) {
        if (str != null && str.length() > 0) {
            /*return str.replaceAll("&#39;", "’").replaceAll("&#039;", "’").replaceAll("&nbsp;", " ")
                    .replaceAll("\r\n", "\n").replaceAll("\n", "\r\n");*/

            return str.replaceAll("\r\n", "\n").replaceAll("\n", "\r\n");
        }
        return "";
    }

    public static String ListToString(List<?> list) {
        StringBuffer sb = new StringBuffer();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == null || list.get(i) == "") {
                    continue;
                }
                // 如果值是list类型则调用自己
                if (list.get(i) instanceof List) {
                    sb.append(ListToString((List<?>) list.get(i)));
                    sb.append(SEP1);
                } else if (list.get(i) instanceof Map) {
                    sb.append(MapToString((Map<?, ?>) list.get(i)));
                    sb.append(SEP1);
                } else {
                    sb.append(list.get(i));
                    sb.append(SEP1);
                }
            }
        }
        return "L" + sb.toString();
    }

    public static String MapToString(Map<?, ?> map) {
        StringBuffer sb = new StringBuffer();
        // 遍历map
        for (Object obj : map.keySet()) {
            if (obj == null) {
                continue;
            }
            Object key = obj;
            Object value = map.get(key);
            if (value instanceof List<?>) {
                sb.append(key.toString() + SEP1 + ListToString((List<?>) value));
                sb.append(SEP2);
            } else if (value instanceof Map<?, ?>) {
                sb.append(key.toString() + SEP1
                        + MapToString((Map<?, ?>) value));
                sb.append(SEP2);
            } else {
                sb.append(key.toString() + SEP3 + value.toString());
                sb.append(SEP2);
            }
        }
        return "M" + sb.toString();
    }

    public static List<Object> StringToList(String listText) {
        if (listText == null || listText.equals("")) {
            return null;
        }
        listText = listText.substring(1);

        listText = listText;

        List<Object> list = new ArrayList<Object>();
        String[] text = listText.split(SEP1);
        for (String str : text) {
            if (str.charAt(0) == 'M') {
                Map<?, ?> map = StringToMap(str);
                list.add(map);
            } else if (str.charAt(0) == 'L') {
                List<?> lists = StringToList(str);
                list.add(lists);
            } else {
                list.add(str);
            }
        }
        return list;
    }

    public static Map<String, Object> StringToMap(String mapText) {

        if (mapText == null || mapText.equals("")) {
            return null;
        }
        mapText = mapText.substring(1);

        mapText = mapText;

        Map<String, Object> map = new HashMap<String, Object>();
        String[] text = mapText.split("\\" + SEP2); // 转换为数组
        for (String str : text) {
            String[] keyText = str.split(SEP3); // 转换key与value的数组
            if (keyText.length < 1) {
                continue;
            }
            String key = keyText[0]; // key
            String value = keyText[1]; // value
            if (value.charAt(0) == 'M') {
                Map<?, ?> map1 = StringToMap(value);
                map.put(key, map1);
            } else if (value.charAt(0) == 'L') {
                List<?> list = StringToList(value);
                map.put(key, list);
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    // 多选，根据消息类型返回文字
    public static String getMsgContent(ChatMessage chatMessage) {
        int type = chatMessage.getType();
        String text = "";
        if (type == XmppMessage.TYPE_TEXT) {
            text = chatMessage.getContent();
        } else if (type == XmppMessage.TYPE_VOICE) {
            text = "[" + InternationalizationHelper.getString("JX_Voice") + "]";
        } else if (type == XmppMessage.TYPE_GIF) {
            text = "[" + InternationalizationHelper.getString("emojiVC_Anma") + "]";
        } else if (type == XmppMessage.TYPE_IMAGE) {
            text = "[" + InternationalizationHelper.getString("JX_Image") + "]";
        } else if (type == XmppMessage.TYPE_VIDEO) {
            text = "[" + InternationalizationHelper.getString("JX_Video") + "]";
        } else if (type >= XmppMessage.TYPE_IS_CONNECT_VOICE
                && type <= XmppMessage.TYPE_EXIT_VOICE) {
            text = MyApplication.getContext().getString(R.string.msg_video_voice);
        } else if (type == XmppMessage.TYPE_FILE) {
            text = "[" + InternationalizationHelper.getString("JX_File") + "]";
        } else if (type == XmppMessage.TYPE_LOCATION) {
            text = "[" + InternationalizationHelper.getString("JX_Location") + "]";
        } else if (type == XmppMessage.TYPE_CARD) {
            text = "[" + InternationalizationHelper.getString("JX_Card") + "]";
        } else if (type == XmppMessage.TYPE_RED) {
            text = "[" + InternationalizationHelper.getString("JX_RED") + "]";
        } else if (type == XmppMessage.TYPE_SHAKE) {
            text = MyApplication.getContext().getString(R.string.msg_shake);
        } else if (type == XmppMessage.TYPE_LINK || type == XmppMessage.TYPE_SHARE_LINK) {
            text = "[" + InternationalizationHelper.getString("JXLink") + "]";
        } else if (type == TYPE_IMAGE_TEXT || type == TYPE_IMAGE_TEXT_MANY) {
            text = "[" + InternationalizationHelper.getString("JXGraphic") + InternationalizationHelper.getString("JXMainViewController_Message") + "]";
        } else if (type == TYPE_CHAT_HISTORY) {
            text = MyApplication.getContext().getString(R.string.msg_chat_history);
        }
        return chatMessage.getFromUserName() + "：" + text;
    }


    /**
     *      * 获取double数据小数点后两位不进行四舍五入
     *      * @param value
     *      * @return -1 , double
     *     
     */
    public static String getMoney(double value) {
        String cutString = "" + value;
        String[] strings = cutString.split("\\.");
        String point = strings[1].length() > 1 ? strings[1].substring(0, 2) : strings[1].substring(0, 1);
        return strings[0] + "." + point;
    }

    /**
     * 方法描述 隐藏银行卡号中间的字符串（使用*号），显示前四后四
     *
     * @param cardNo
     * @return
     * @author yaomy
     * @date 2018年4月3日 上午10:37:00
     */
    public static String hideCardNo(String cardNo) {
        if (TextUtils.isEmpty(cardNo)) {
            return cardNo;
        }

        int length = cardNo.length();
        int beforeLength = 4;
        int afterLength = 4;
// 替换字符串，当前使用“”
        String replaceSymbol = "*";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            if (i < beforeLength || i >= (length - afterLength)) {
                sb.append(cardNo.charAt(i));
            } else {
                sb.append(replaceSymbol);
            }
        }

        return sb.toString();
    }

    /**
     * 方法描述 隐藏手机号中间位置字符，显示前三后三个字符
     *
     * @param phoneNo
     * @return
     * @author yaomy
     * @date 2018年4月3日 上午10:38:51
     */
    public static String hidePhoneNo(String phoneNo) {
        if (TextUtils.isEmpty(phoneNo)) {
            return phoneNo;
        }

        int length = phoneNo.length();
        int beforeLength = 3;
        int afterLength = 3;
        // 替换字符串，当前使用“”
        String replaceSymbol = "*";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            if (i < beforeLength || i >= (length - afterLength)) {
                sb.append(phoneNo.charAt(i));
            } else {
                sb.append(replaceSymbol);
            }
        }

        return sb.toString();
    }
}