package com.ydd.zhichat.ui.backup;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.view.HeadView;

import java.util.ArrayList;
import java.util.List;

class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private static final String TAG = "ChatAdapter";
    private List<Item> data = new ArrayList<>();
    private OnItemSelectedChangeListener listener;
    private String userId;

    ChatAdapter(OnItemSelectedChangeListener listener, String userId) {
        this.listener = listener;
        this.userId = userId;
    }

    void setData(List<Item> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    void cancelAll() {
        selectAll(false);
    }

    void selectAll() {
        selectAll(true);
    }

    private void selectAll(boolean isSelected) {
        for (Item item : data) {
            if (item.selected != isSelected) {
                item.selected = isSelected;
                if (listener != null) {
                    listener.onItemSelectedChange(item, isSelected);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_select_chat, parent, false);
        return new ViewHolder(view, listener, userId);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Item item = data.get(i);
        viewHolder.apply(item);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    interface OnItemSelectedChangeListener {
        void onItemSelectedChange(Item item, boolean isSelected);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        HeadView hvHead = itemView.findViewById(R.id.hvHead);
        TextView tvNickName = itemView.findViewById(R.id.tvNickName);
        CheckBox cbSelect = itemView.findViewById(R.id.cbSelect);
        private OnItemSelectedChangeListener listener;
        private String userId;

        ViewHolder(@NonNull View itemView, OnItemSelectedChangeListener listener, String userId) {
            super(itemView);
            this.listener = listener;
            this.userId = userId;
        }

        void apply(Item item) {
            AvatarHelper.getInstance().displayAvatar(userId, item.friend, hvHead);
            tvNickName.setText(item.getNickName());

            cbSelect.setOnCheckedChangeListener(null);
            cbSelect.setChecked(item.selected);
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.selected = isChecked;
                if (listener != null) {
                    listener.onItemSelectedChange(item, isChecked);
                }
            });
        }
    }

    static class Item {
        Friend friend;
        boolean selected;

        String getNickName() {
            return friend.getShowName();
        }

        String getUserId() {
            return friend.getUserId();
        }

        @Override
        public String toString() {
            return "Item{" +
                    "friend=" + JSON.toJSONString(friend) +
                    '}';
        }

        public static Item fromFriend(Friend friend) {
            Item item = new Item();
            item.friend = friend;
            return item;
        }

    }
}
