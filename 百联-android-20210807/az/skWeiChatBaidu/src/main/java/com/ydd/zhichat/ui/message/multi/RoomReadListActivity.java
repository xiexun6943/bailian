package com.ydd.zhichat.ui.message.multi;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.db.dao.ChatMessageDao;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.ui.base.BaseListActivity;
import com.ydd.zhichat.util.TimeUtils;
import com.ydd.zhichat.view.HeadView;

import java.util.List;

/**
 * Created by Administrator on 2017/6/28 0028.
 * 群已读人数
 */
public class RoomReadListActivity extends BaseListActivity<RoomReadListActivity.ReadViewHolder> {
    String packetId;
    private String loginUserId;
    private String roomId;
    private List<ChatMessage> mdata;

    @Override
    public void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.read_list);

        packetId = getIntent().getStringExtra("packetId");
        roomId = getIntent().getStringExtra("roomId");
        loginUserId = coreManager.getSelf().getUserId();
    }

    @Override
    public void initDatas(int pager) {
        mdata = ChatMessageDao.getInstance().queryFriendsByReadList(loginUserId, roomId, packetId, pager);
        update(mdata);
    }

    @Override
    public ReadViewHolder initHolder(ViewGroup parent) {
        View view = mInflater.inflate(R.layout.row_nearly_message, parent, false);
        ReadViewHolder holder = new ReadViewHolder(view);
        return holder;
    }

    @Override
    public void fillData(ReadViewHolder holder, int position) {
        ChatMessage chat = mdata.get(position);
        //        String url = AvatarHelper.getInstance().getAvatarUrl(chat.getFromUserId(), true);
        //        Glide.with(mContext).load(url).into(holder.ivInco.getHeadImage()).er;
        AvatarHelper.getInstance().displayAvatar(chat.getFromUserId(), holder.ivInco);
        holder.tvName.setText(chat.getFromUserName());
        String time = TimeUtils.f_long_2_str(chat.getTimeSend() * 1000);
        holder.tvTime.setText(getString(R.string.prefix_read_time) + time);
        holder.tvTime.setText(getString(R.string.prefix_read_time) + time);
        holder.tvTimeDuring.setText(TimeUtils.getFriendlyTimeDesc(this, chat.getTimeSend()));
    }

    public class ReadViewHolder extends RecyclerView.ViewHolder {
        public HeadView ivInco;
        public TextView tvName;
        public TextView tvTime;
        public TextView tvTimeDuring;

        public ReadViewHolder(View itemView) {
            super(itemView);
            itemView.findViewById(R.id.num_tv).setVisibility(View.GONE);
            itemView.findViewById(R.id.not_push_iv).setVisibility(View.GONE);
            itemView.findViewById(R.id.replay_iv).setVisibility(View.GONE);

            ivInco = (HeadView) itemView.findViewById(R.id.avatar_imgS);
            tvName = (TextView) itemView.findViewById(R.id.nick_name_tv);
            tvTime = (TextView) itemView.findViewById(R.id.content_tv);
            tvTimeDuring = (TextView) itemView.findViewById(R.id.time_tv);
            ivInco.setVisibility(View.VISIBLE);
        }
    }
}
