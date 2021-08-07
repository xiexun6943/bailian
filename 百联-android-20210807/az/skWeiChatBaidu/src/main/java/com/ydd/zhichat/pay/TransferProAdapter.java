package com.ydd.zhichat.pay;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.TransferRecord;
import com.ydd.zhichat.ui.base.BaseRecViewHolder;
import com.ydd.zhichat.util.DateFormatUtil;
import com.ydd.zhichat.util.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// 增加头部的recyaler适配器
public class TransferProAdapter extends RecyclerView.Adapter {
    private static final int HEADER_TYPE = 1;
    private static final int NORMAL_TYPE = 0;

    private List<View> mHeaderViews;
    private Context mContext;
    private List<TransferRecord.DataBean.PageDataBean> mTransfers;

    public TransferProAdapter(Context context, List<TransferRecord.DataBean.PageDataBean> transfers) {
        this.mContext = context;
        this.mTransfers = transfers;
        mHeaderViews = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        if (viewType == HEADER_TYPE) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.transter_head, viewGroup, false);
            viewHolder = new HeadViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.transter_adapter, viewGroup, false);
            viewHolder = new TransferViewHolder(view);
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof HeadViewHolder) {
            ((HeadViewHolder) viewHolder).onBind(mTransfers, position);
        } else if (viewHolder instanceof TransferViewHolder) {
            ((TransferViewHolder) viewHolder).onBind(mTransfers, position - mHeaderViews.size());
        }
    }

    @Override
    public int getItemCount() {
        return mTransfers.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mTransfers.get(position).isTitle())
            return HEADER_TYPE;
        else
            return NORMAL_TYPE;
    }
}

/**
 * 增加转账头部记录总数
 */
class HeadViewHolder extends BaseRecViewHolder {
    TextView tv_in_sum, tv_to_sum, tv_month;

    public HeadViewHolder(View itemView) {
        super(itemView);
        tv_month = itemView.findViewById(R.id.tv_month);
        tv_to_sum = itemView.findViewById(R.id.tv_to_sum);
        tv_in_sum = itemView.findViewById(R.id.tv_in_sum);
    }

    public void onBind(List<TransferRecord.DataBean.PageDataBean> list, int position) {
        TransferRecord.DataBean.PageDataBean bean = list.get(position);
        int month = list.get(position).getMonth();
        if ((month - 1) == Calendar.getInstance().get(Calendar.MONTH)) {
            tv_month.setText("本月");
        } else {
            tv_month.setText("0" + month + "月");
        }

        String inP = StringUtils.getMoney(bean.getTotalInMoney());
        String toP = StringUtils.getMoney(bean.getTotalOutMoney());

        tv_in_sum.setText(inP);
        tv_to_sum.setText(toP);
    }
}

class TransferViewHolder extends BaseRecViewHolder {
    public TextView tv_date;
    public TextView tv_balance;
    public TextView tv_title_transfer;
    public TextView tv_receipt;

    public TransferViewHolder(View itemView) {
        super(itemView);
        tv_date = itemView.findViewById(R.id.tv_date);
        tv_balance = itemView.findViewById(R.id.tv_balance);
        tv_receipt = itemView.findViewById(R.id.tv_receipt);
        tv_title_transfer = itemView.findViewById(R.id.tv_title_transfer);
    }

    public void onBind(List<TransferRecord.DataBean.PageDataBean> list, int position) {
        tv_title_transfer.setText(list.get(position).getDesc());//类型
        tv_receipt.setText(list.get(position).getStatus() == 1 ? "交易成功" : "交易失败");//状态

        String data = DateFormatUtil.timedate(list.get(position).getTime());//时间
        tv_date.setText(data);
        switch (list.get(position).getType()) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 7:
            case 10:
            case 12:
                tv_balance.setText(String.valueOf("-" + list.get(position).getMoney()));//支出金额
                break;
            default:
                tv_balance.setText(String.valueOf("+" + list.get(position).getMoney()));//收入金额
                break;
        }
    }
}
