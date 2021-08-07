package com.ydd.zhichat.adapter

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.widget.RelativeLayout
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.ydd.zhichat.R
import com.ydd.zhichat.bean.redpacket.CardBean
import com.ydd.zhichat.AppConstant.ZHI_FU_BAO
import com.ydd.zhichat.AppConstant.ZHONG_GUO_YINHANG
import com.ydd.zhichat.AppConstant.ZHONG_GUO_JIANSHE_YINHANG
import com.ydd.zhichat.AppConstant.ZHONG_GUO_GONGSHANG_YINHANG
import com.ydd.zhichat.AppConstant.ZHONG_GUO_NONGYE_YINHANG
import com.ydd.zhichat.AppConstant.ZHONG_GUO_JIAOTONG_YINHANG
import com.ydd.zhichat.AppConstant.ZHONG_GUO_YOUZHENG_YINHANG

/**
 * Created by 蒲弘宇的本地账户 on 2018/5/8.
 */
class CardsAdapter(layoutResId: Int, val context: Context, movies: List<CardBean>) : BaseQuickAdapter<CardBean, BaseViewHolder>(layoutResId, movies) {


    @RequiresApi(Build.VERSION_CODES.M)
    override fun convert(helper: BaseViewHolder?, item: CardBean?) {
        var bankId = item?.bankBrandId
        var bankName = item?.bankBrandName
//        var bankNum = StringUtils.hideCardNo(item?.cardNo)
        var bankNum = item?.cardNo

        var rel_card = helper?.getView<RelativeLayout>(R.id.rel_card)
        var tv_number = helper?.getView<TextView>(R.id.tv_number)
        var positions = helper?.adapterPosition

        when(bankId){
            ZHI_FU_BAO -> {
                tv_number?.setText(bankNum)
                rel_card?.setBackgroundResource(R.drawable.play_treasure)
            }
            ZHONG_GUO_YINHANG -> {
                tv_number?.setText(bankNum)
                rel_card?.setBackgroundResource(R.drawable.ic_cardbg_boc)
            }
            ZHONG_GUO_JIANSHE_YINHANG -> {
                tv_number?.setText(bankNum)
                rel_card?.setBackgroundResource(R.drawable.ic_cardbg_ccb)
            }
            ZHONG_GUO_GONGSHANG_YINHANG -> {
                tv_number?.setText(bankNum)
                rel_card?.setBackgroundResource(R.drawable.ic_cardbg_icbc)
            }
            ZHONG_GUO_NONGYE_YINHANG -> {
                tv_number?.setText(bankNum)
                rel_card?.setBackgroundResource(R.drawable.ic_cardbg_abc)
            }
            ZHONG_GUO_JIAOTONG_YINHANG -> {
                tv_number?.setText(bankNum)
                rel_card?.setBackgroundResource(R.drawable.ic_cardbg_comm)
            }
            ZHONG_GUO_YOUZHENG_YINHANG -> {
                tv_number?.setText(bankNum)
                rel_card?.setBackgroundResource(R.drawable.ic_cardbg_psbc)
            }


        }
//        //可链式调用赋值
//        helper?.setText(R.id.tv_payType, billDesc)
//                ?.setText(R.id.tv_tradeTime, tradeTime)
    }

}