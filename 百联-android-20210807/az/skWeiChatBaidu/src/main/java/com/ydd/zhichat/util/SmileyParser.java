/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ydd.zhichat.util;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

import com.ydd.zhichat.R;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A class for annotating a CharSequence with spans to convert textual emoticons to graphical ones.
 */
public class SmileyParser {
    private static SmileyParser sInstance;
    private final Context mContext;
    private final Pattern mPattern;
    private final Pattern mPatterns;
    private final Pattern mHtmlPattern;

    private SmileyParser(Context context) {
        mContext = context;
        mPattern = buildPattern();
        mPatterns = buildPatterns();
        mHtmlPattern = buildHtmlPattern();
    }

    public static SmileyParser getInstance(Context context) {
        if (sInstance == null) {
            synchronized (SmileyParser.class) {
                if (sInstance == null) {
                    sInstance = new SmileyParser(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * Builds the regular expression we use to find smileys in {@link #addSmileySpans}.
     */
    private Pattern buildPattern() {
        // Set the StringBuilder capacity with the assumption that the average
        // smiley is 3 characters long.
        StringBuilder patternString = new StringBuilder();

        // Build a regex that looks like (:-)|:-(|...), but escaping the smilies
        // properly so they will be interpreted literally by the regex matcher.
        patternString.append('(');
        for (int i = 0; i < Smilies.TEXTS.length; i++) {
            for (int j = 0; j < Smilies.TEXTS[i].length; j++) {
                patternString.append(Pattern.quote(Smilies.TEXTS[i][j]));
                patternString.append('|');
            }
        }

        // Replace the extra '|' with a ')'
        patternString.replace(patternString.length() - 1, patternString.length(), ")");

        return Pattern.compile(patternString.toString());
    }

    private Pattern buildPatterns() {
        // Set the StringBuilder capacity with the assumption that the average
        // smiley is 3 characters long.
        StringBuilder patternString = new StringBuilder();

        // Build a regex that looks like (:-)|:-(|...), but escaping the smilies
        // properly so they will be interpreted literally by the regex matcher.
        patternString.append('(');
        for (int i = 0; i < Smilies.TEXTSS.length; i++) {
            for (int j = 0; j < Smilies.TEXTSS[i].length; j++) {
                patternString.append(Pattern.quote(Smilies.TEXTSS[i][j]));
                patternString.append('|');
            }
        }

        // Replace the extra '|' with a ')'
        patternString.replace(patternString.length() - 1, patternString.length(), ")");

        return Pattern.compile(patternString.toString());
    }

    private Pattern buildHtmlPattern() {
        // Set the StringBuilder capacity with the assumption that the average
        // smiley is 3 characters long.
        // StringBuilder patternString = new StringBuilder();

        // Build a regex that looks like (:-)|:-(|...), but escaping the smilies
        // properly so they will be interpreted literally by the regex matcher.
        // patternString.append('(');
        // patternString.append(Pattern.quote("(<a)(\\w)+(?=</a>)"));
        // // Replace the extra '|' with a ')'
        // patternString.replace(patternString.length() - 1,
        // patternString.length(), ")");

        return Pattern.compile("(http://(\\S+?)(\\s))|(www.(\\S+?)(\\s))");
    }

    /**
     * Adds ImageSpans to a CharSequence that replace textual emoticons such as :-) with a graphical version.
     *
     * @param text A CharSequence possibly containing emoticons
     * @return A CharSequence annotated with ImageSpans covering any recognized emoticons.
     */
    public CharSequence addSmileySpans(CharSequence text, boolean canClick) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        Matcher matcher = mPattern.matcher(text);
        while (matcher.find()) {
            int resId = Smilies.textMapId(matcher.group());
            if (resId != -1) {
                builder.setSpan(new MyImageSpan(mContext, resId), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return builder;
    }

    public static class Smilies {
        private static final int[][] IDS = {
                {
                        R.drawable.e_01_smile, R.drawable.e_02_joy, R.drawable.e_03_heart_eyes,
                        R.drawable.e_04_sweat_smile, R.drawable.e_05_laughing, R.drawable.e_06_wink,
                        R.drawable.e_07_yum, R.drawable.e_24_blush, R.drawable.e_09_fearful,
                        R.drawable.e_10_ohyeah, R.drawable.e_11_cold_sweat, R.drawable.e_12_scream,
                        R.drawable.e_13_kissing_heart, R.drawable.e_14_smirk, R.drawable.e_15_angry,
                        R.drawable.e_16_sweat, R.drawable.e_17_stuck, R.drawable.e_18_rage,
                        R.drawable.e_19_etriumph, R.drawable.e_20_mask, R.drawable.e_27_flushed,
                        R.drawable.e_22_sunglasses, R.drawable.e_23_sob, R.drawable.e_del
                },
                {
                        R.drawable.e_08_relieved, R.drawable.e_26_doubt, R.drawable.e_21_confounded,
                        R.drawable.e_28_sleepy,
                        R.drawable.e_29_sleeping, R.drawable.e_30_disappointed_relieved,
                        R.drawable.e_31_tire, R.drawable.e_32_astonished,
                        R.drawable.e_33_buttonnose, R.drawable.e_34_frowning, R.drawable.e_35_shutup,
                        R.drawable.e_36_expressionless, R.drawable.e_37_confused, R.drawable.e_38_tired_face,
                        R.drawable.e_39_grin, R.drawable.e_40_unamused, R.drawable.e_41_persevere,
                        R.drawable.e_42_relaxed, R.drawable.e_43_pensive, R.drawable.e_44_no_mouth,
                        R.drawable.e_45_worried, R.drawable.e_46_cry, R.drawable.e_47_pill,
                        R.drawable.e_del,

                },
                {
                        R.drawable.e_48_celebrate,
                        R.drawable.e_49_gift, R.drawable.e_50_birthday,
                        R.drawable.e_51_pray, R.drawable.e_52_ok_hand, R.drawable.e_53_first,
                        R.drawable.e_54_v, R.drawable.e_55_punch, R.drawable.e_56_thumbsup,
                        R.drawable.e_57_thumbsdown, R.drawable.e_58_muscle, R.drawable.e_59_maleficeent,
                        R.drawable.e_60_broken_heart, R.drawable.e_61_heart, R.drawable.e_62_taxi,
                        R.drawable.e_63_eyes, R.drawable.e_64_rose,
                        R.drawable.e_65_ghost, R.drawable.e_66_lip, R.drawable.e_67_fireworks,
                        R.drawable.e_68_balloon, R.drawable.e_69_clasphands, R.drawable.e_70_bye,
                        R.drawable.e_del,
                },

        };
        private static final String[][] TEXTS = {
                {
                        "[smile]", "[joy]", "[heart-eyes]"
                        , "[sweat_smile]", "[laughing]", "[wink]"
                        , "[yum]", "[blush]", "[fearful]"
                        , "[ohYeah]", "[cold-sweat]", "[scream]"
                        , "[kissing_heart]", "[smirk]", "[angry]"
                        , "[sweat]", "[stuck]", "[rage]"
                        , "[etriumph]", "[mask]", "[flushed]"
                        , "[sunglasses]", "[sob]", "[del]"
                },
                {
                        "[relieved]", "[doubt]",
                        "[confounded]", "[sleepy]",
                        "[sleeping]", "[disappointed_relieved]"
                        , "[tire]", "[astonished]",
                        "[buttonNose]", "[frowning]", "[shutUp]",
                        "[expressionless]", "[confused]", "[tired_face]",
                        "[grin]", "[unamused]", "[persevere]",
                        "[relaxed]", "[pensive]", "[no_mouth]",
                        "[worried]", "[cry]", "[pill]",
                        "[del]"
                },
                {
                        "[celebrate]", "[gift]", "[birthday]",
                        "[paray]", "[ok_hand]", "[first]",
                        "[v]", "[punch]", "[thumbsup]",
                        "[thumbsdown]", "[muscle]", "[maleficeent]",
                        "[broken_heart]", "[heart]", "[taxi]",
                        "[eyes]", "[rose]",
                        "[ghost]", "[lip]", "[fireworks]",
                        "[balloon]", "[clasphands]", "[bye]",
                        "[del]"
                }

        };
        /**
         * @deprecated 这已经没有使用了，数据也对不上了，
         */
        @Deprecated
        private static final String[][] TEXTSS = {
                {
                        "[微笑]", "[快乐]", "[色咪咪]"
                        , "[汗]", "[大笑]", "[眨眼]"
                        , "[百胜]", "[脸红]", "[可怕]"
                        , "[欧耶]", "[冷汗]", "[尖叫]"
                        , "[亲亲]", "[得意]", "[害怕]"
                        , "[沮丧]", "[卡住]", "[愤怒]"
                        , "[生气]", "[面具]", "[激动]"
                        , "[太阳镜]", "[在]", "[放松]"
                },
                {
                        "[安静]", "[羞愧]"
                        , "[休息]",
                        "[睡着]", "[失望]", "[累]", "[惊讶]",
                        "[抠鼻]", "[皱眉头]", "[闭嘴]",
                        "[面无表情]", "[困惑]", "[厌倦]",
                        "[露齿而笑]", "[非娱乐]", "[坚持下去]",
                        "[傻笑]", "[沉思]", "[无嘴]",
                        "[担心]", "[哭]", "[药]",
                        "[庆祝]", "[礼物]",
                },
                {
                        "[生日]",
                        "[祈祷]", "[好]", "[冠军]",
                        "[耶]", "[拳头]", "[赞]",
                        "[垃圾]", "[肌肉]", "[鼓励]",
                        "[心碎]", "[心]", "[出租车]",
                        "[眼睛]", "[玫瑰]",
                        "[鬼]", "[嘴唇]", "[烟花]",
                        "[气球]", "[握手]", "[抱拳]"
                },
        };
        private static final Map<String, Integer> MAPS = new HashMap<String, Integer>();
        /**
         * 适配ios老版本中文
         */
        private static final Map<String, Integer> MAPSS = new HashMap<String, Integer>();

        static {
            // 取最小的长度，防止长度不一致出错
            int length = IDS.length > TEXTS.length ? TEXTS.length : IDS.length;
            for (int i = 0; i < length; i++) {
                int[] subIds = IDS[i];
                String[] subTexts = TEXTS[i];
                if (subIds == null || subTexts == null) {
                    continue;
                }
                int subLength = subIds.length > subTexts.length ? subTexts.length : subIds.length;
                for (int j = 0; j < subLength; j++) {
                    MAPS.put(TEXTS[i][j], IDS[i][j]);
                }
            }
        }

        static {
            // 取最小的长度，防止长度不一致出错
            int length = IDS.length > TEXTSS.length ? TEXTSS.length : IDS.length;
            for (int i = 0; i < length; i++) {
                int[] subIds = IDS[i];
                String[] subTexts = TEXTSS[i];
                if (subIds == null || subTexts == null) {
                    continue;
                }
                int subLength = subIds.length > subTexts.length ? subTexts.length : subIds.length;
                for (int j = 0; j < subLength; j++) {
                    MAPSS.put(TEXTSS[i][j], IDS[i][j]);
                }
            }
        }

        public static int[][] getIds() {
            return IDS;
        }

        public static String[][] getTexts() {
            return TEXTS;
        }

        public static int textMapId(String text) {
            if (MAPS.containsKey(text)) {
                return MAPS.get(text);
            } else if (MAPSS.containsKey(text)) {
                // 适配 ios老版本
                return MAPSS.get(text);
            } else {
                return -1;
            }
        }
    }

    public static class Gifs {
        private static final int[][] IDS = {
                {
                        R.drawable.gif_eight, R.drawable.gif_eighteen, R.drawable.gif_eleven, R.drawable.gif_fifity,
                        R.drawable.gif_fifity_four, R.drawable.gif_fifity_one, R.drawable.gif_fifity_three, R.drawable.gif_fifity_two
                        , R.drawable.gif_fifteen, R.drawable.gif_five
                },
                {
                        R.drawable.gif_forty, R.drawable.gif_forty_eight,
                        R.drawable.gif_forty_five, R.drawable.gif_forty_four, R.drawable.gif_forty_nine, R.drawable.gif_forty_one
                        , R.drawable.gif_forty_seven, R.drawable.gif_forty_three, R.drawable.gif_forty_two, R.drawable.gif_fourteen
                },
                {
                        R.drawable.gif_nine, R.drawable.gif_nineteen, R.drawable.gif_one, R.drawable.gif_seven,
                        R.drawable.gif_seventeen, R.drawable.gif_sixteen, R.drawable.gif_ten, R.drawable.gif_thirteen,
                        R.drawable.gif_thirty, R.drawable.gif_thirty_eight
                },
                {
                        R.drawable.gif_thirty_five, R.drawable.gif_thirty_four, R.drawable.gif_thirty_nine, R.drawable.gif_thirty_seven,
                        R.drawable.gif_thirty_six, R.drawable.gif_thirty_three, R.drawable.gif_thirty_two, R.drawable.gif_thirty_one,
                        R.drawable.gif_three, R.drawable.gif_twelve
                },
                {
                        R.drawable.gif_twenty, R.drawable.gif_twenty_eight, R.drawable.gif_twenty_five, R.drawable.gif_twenty_four,
                        R.drawable.gif_twenty_nine, R.drawable.gif_twenty_one, R.drawable.gif_twenty_seven, R.drawable.gif_twenty_six
                        , R.drawable.gif_twenty_three, R.drawable.gif_twenty_two
                }

        };
        private static final String[][] TEXTS = {
                {
                        "eight.gif", "eighteen.gif", "eleven.gif", "fifity.gif",
                        "fifity_four.gif", "fifity_one.gif", "fifity_three.gif", "fifity_two.gif"
                        , "fifteen.gif", "five.gif"
                },

                {
                        "forty.gif", "forty_eight.gif",
                        "forty_five.gif", "forty_four.gif", "forty_nine.gif", "forty_one.gif",
                        "forty_seven.gif", "forty_three.gif", "forty_two.gif", "fourteen.gif"
                },

                {

                        "nine.gif", "nineteen.gif", "one.gif", "seven.gif",
                        "seventeen.gif", "sixteen.gif", "ten.gif", "thirteen.gif",
                        "thirty.gif", "thirty_eight.gif",

                },

                {
                        "thirty_five.gif", "thirty_four.gif",
                        "thirty_nine.gif", "thirty_seven.gif", "thirty_six.gif", "thirty_three.gif",
                        "thirty_two.gif", "thirty-one.gif", "three.gif", "twelve.gif"
                },

                {
                        "twenty.gif", "twenty_eight.gif", "twenty_five.gif", "twenty_four.gif",
                        "twenty_nine.gif", "twenty_one.gif", "twenty_seven.gif", "twenty_six.gif"
                        , "twenty_three.gif", "twenty_two.gif"
                }

        };
        /**
         * gif
         */
        private static final int[][] PNGID = {
                {
                        R.drawable.gif_eight_png, R.drawable.gif_eighteen_png, R.drawable.gif_eleven_png, R.drawable.gif_fifity_png,
                        R.drawable.gif_fifity_four_png, R.drawable.gif_fifity_one_png, R.drawable.gif_fifity_three_png, R.drawable.gif_fifity_two_png
                        , R.drawable.gif_fifteen_png, R.drawable.gif_five_png
                },
                {
                        R.drawable.gif_forty_png, R.drawable.gif_forty_eight_png,
                        R.drawable.gif_forty_five_png, R.drawable.gif_forty_four_png, R.drawable.gif_forty_nine_png, R.drawable.gif_forty_one_png
                        , R.drawable.gif_forty_seven_png, R.drawable.gif_forty_three_png, R.drawable.gif_forty_two_png, R.drawable.gif_fourteen_png
                },
                {

                        R.drawable.gif_nine_png, R.drawable.gif_nineteen_png, R.drawable.gif_one_png, R.drawable.gif_seven_png,
                        R.drawable.gif_seventeen_png, R.drawable.gif_sixteen_png, R.drawable.gif_ten_png, R.drawable.gif_thirteen_png,
                        R.drawable.gif_thirty_png, R.drawable.gif_thirty_eight_png
                },
                {
                        R.drawable.gif_thirty_five_png, R.drawable.gif_thirty_four_png, R.drawable.gif_thirty_nine_png, R.drawable.gif_thirty_seven_png
                        , R.drawable.gif_thirty_six_png, R.drawable.gif_thirty_three_png, R.drawable.gif_thirty_two_png, R.drawable.gif_thirty_one_png
                        , R.drawable.gif_three_png, R.drawable.gif_twelve_png
                },
                {
                        R.drawable.gif_twenty_png, R.drawable.gif_twenty_eight_png, R.drawable.gif_twenty_five_png, R.drawable.gif_twenty_four_png,
                        R.drawable.gif_twenty_nine_png, R.drawable.gif_twenty_one_png, R.drawable.gif_twenty_seven_png, R.drawable.gif_twenty_six_png
                        , R.drawable.gif_twenty_three_png, R.drawable.gif_twenty_two_png
                }

        };
        private static final Map<String, Integer> MAPS = new HashMap<String, Integer>();

        static {
            // 取最小的长度，防止长度不一致出错
            int length = IDS.length > TEXTS.length ? TEXTS.length : IDS.length;
            for (int i = 0; i < length; i++) {
                int[] subIds = IDS[i];
                String[] subTexts = TEXTS[i];
                if (subIds == null || subTexts == null) {
                    continue;
                }
                int subLength = subIds.length > subTexts.length ? subTexts.length : subIds.length;
                for (int j = 0; j < subLength; j++) {
                    MAPS.put(TEXTS[i][j], IDS[i][j]);
                }
            }
        }

        public static int[][] getIds() {
            return IDS;
        }

        public static String[][] getTexts() {
            return TEXTS;
        }

        public static int textMapId(String text) {
            if (MAPS.containsKey(text)) {
                return MAPS.get(text);
            } else {
                return -1;
            }
        }

        public static int[][] getPngIds() {

            return PNGID;
        }
    }
}
