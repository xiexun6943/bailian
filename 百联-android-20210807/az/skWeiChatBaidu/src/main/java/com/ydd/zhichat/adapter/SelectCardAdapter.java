package com.ydd.zhichat.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.redpacket.SelectWindowModel;

import java.util.ArrayList;

import static com.ydd.zhichat.AppConstant.ZHI_FU_BAO;

public class SelectCardAdapter extends RecyclerView.Adapter<SelectCardAdapter.ThisViewHolder> {


    private Context context;

    public int selectedPosition;

    private ArrayList<SelectWindowModel> list;

    private SelectCardAdapter.OnItemClickListener mOnItemClickListener;

    public SelectCardAdapter(ArrayList<SelectWindowModel> data) {
        this.list = data;
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }

    @NonNull
    @Override
    public ThisViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_withdrawal_layout, parent, false);
        SelectCardAdapter.ThisViewHolder holder = new SelectCardAdapter.ThisViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ThisViewHolder holder, int position) {
        holder.setData(list.get(position), position);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface OnItemClickListener {
        void onItemBack(SelectWindowModel item, int position);
    }


    public void setOnItemClickListener(SelectCardAdapter.OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    protected class ThisViewHolder extends RecyclerView.ViewHolder {

        private View itemView;
        private ImageView icon;
        private TextView name;
        private TextView tv_number;
        private TextView tip;
        private ImageView isSelect;

        public ThisViewHolder(View view) {
            super(view);
            itemView = view;
            icon = view.findViewById(R.id.icon);
            name = view.findViewById(R.id.name);
            tv_number = view.findViewById(R.id.tv_number);
            tip = view.findViewById(R.id.tip);
            isSelect = view.findViewById(R.id.isSelect);
        }


        public void setData(final SelectWindowModel object, final int position) {
//            Glide.with(context).load(object.icon).into(icon);
            icon.setImageResource(object.icon);
            name.setText(object.name);
//            tv_number.setText(StringUtils.hideCardNo(object.cardNum));
            tv_number.setText(object.cardNum);
            isSelect.setVisibility(position == selectedPosition ? View.VISIBLE : View.INVISIBLE);

            if(object.id == ZHI_FU_BAO){
                tip.setText("实时到账");
            }else {
                tip.setText("2小时内到账");
            }
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemBack(object, position);
                    }
                }
            });

        }
    }
}

