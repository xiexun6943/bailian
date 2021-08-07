package com.ydd.zhichat.pay.sk;

import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.db.dao.ChatMessageDao;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.TimeUtils;
import com.ydd.zhichat.xmpp.ListenerManager;
import com.ydd.zhichat.xmpp.listener.ChatMessageListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SKPayActivity extends BaseActivity implements ChatMessageListener {

    private RecyclerView mSKPayRcy;
    private SKPayAdapter mSKPayAdapter;
    private List<ChatMessage> mChatMessageSource = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sk_pay);
        initActionBar();
        initView();
        ListenerManager.getInstance().addChatMessageListener(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        TextView titleTv = findViewById(R.id.tv_title_center);
        titleTv.setText(getString(R.string.sk_pay));
    }

    private void initView() {
        mChatMessageSource = new ArrayList<>();
        List<ChatMessage> messages = ChatMessageDao.getInstance().getSingleChatMessages(coreManager.getSelf().getUserId(),
                Friend.ID_SK_PAY, TimeUtils.sk_time_current_time(), 100);
        Collections.reverse(messages);// 将集合倒序
        mChatMessageSource.addAll(messages);

        mSKPayRcy = findViewById(R.id.sk_pay_rcy);
        mSKPayAdapter = new SKPayAdapter(mChatMessageSource);
        mSKPayRcy.setLayoutManager(new LinearLayoutManager(this));
        mSKPayRcy.setAdapter(mSKPayAdapter);
        mSKPayRcy.setItemAnimator(new DefaultItemAnimator());

        mSKPayRcy.scrollToPosition(mSKPayAdapter.getItemCount() - 1);
    }

    @Override
    public void onMessageSendStateChange(int messageState, String msgId) {

    }

    @Override
    public boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg) {
        if (fromUserId.equals(Friend.ID_SK_PAY)) {
            mChatMessageSource.add(message);
            mSKPayAdapter.notifyItemInserted(mChatMessageSource.size());
            mSKPayRcy.scrollToPosition(mSKPayAdapter.getItemCount() - 1);
        }
        return false;
    }
}
