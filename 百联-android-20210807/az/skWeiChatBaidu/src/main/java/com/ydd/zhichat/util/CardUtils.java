package com.ydd.zhichat.util;

import com.ydd.zhichat.R;

import static com.ydd.zhichat.AppConstant.ZHI_FU_BAO;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_GONGSHANG_YINHANG;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_JIANSHE_YINHANG;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_JIAOTONG_YINHANG;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_NONGYE_YINHANG;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_YINHANG;
import static com.ydd.zhichat.AppConstant.ZHONG_GUO_YOUZHENG_YINHANG;

/**
 * Created by phy on 2020/1/6
 */
public class CardUtils {
    public static int getBankIconResId(int id) {
        int resId = R.drawable.ic_card_boc;
        switch (id) {
            case ZHI_FU_BAO:
                resId = R.drawable.treasure;
                break;
            case ZHONG_GUO_GONGSHANG_YINHANG:
                resId = R.drawable.ic_card_icbc;
                break;
            case ZHONG_GUO_JIANSHE_YINHANG:
                resId = R.drawable.ic_card_ccb;
                break;
            case ZHONG_GUO_JIAOTONG_YINHANG:
                resId = R.drawable.ic_card_comm;
                break;
            case ZHONG_GUO_NONGYE_YINHANG:
                resId = R.drawable.ic_card_abc;
                break;
            case ZHONG_GUO_YINHANG:
                resId = R.drawable.ic_card_boc;
                break;
            case ZHONG_GUO_YOUZHENG_YINHANG:
                resId = R.drawable.ic_card_psbc;
                break;
        }
        return resId;
    }
}
