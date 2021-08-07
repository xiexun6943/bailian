package com.ydd.zhichat.ui.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.bean.EventSentChatHistory;
import com.ydd.zhichat.bean.Friend;
import com.ydd.zhichat.db.dao.FriendDao;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.AsyncUtils;
import com.ydd.zhichat.util.EventBusHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

public class SelectChatActivity extends BaseActivity implements ChatAdapter.OnItemSelectedChangeListener {

    private RecyclerView rvChatList;
    private ChatAdapter chatAdapter;
    private TextView tvSelectedCount;
    private Set<String> selectedUserIdList = new HashSet<>();
    private View llSelectedCount;
    private TextView btnSelectAll;
    private View btnSelectFinish;

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, SelectChatActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_chat);

        initActionBar();
        initView();

        initData();
        EventBusHelper.register(this);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventSentChatHistory message) {
        finish();
    }

    private void initData() {
        AsyncUtils.doAsync(this, r -> {
            Reporter.post("查询存在聊天记录的好友失败", r);
        }, c -> {
            List<Friend> chatFriendList = FriendDao.getInstance().getChatFriendList(coreManager.getSelf().getUserId());
            List<ChatAdapter.Item> data = new ArrayList<>(chatFriendList.size());
            for (Friend friend : chatFriendList) {
                data.add(ChatAdapter.Item.fromFriend(friend));
            }
            c.uiThread(r -> {
                chatAdapter.setData(data);
                btnSelectAll.setEnabled(true);
            });
        });
    }

    private void initView() {
        btnSelectFinish = findViewById(R.id.btnSelectFinish);
        btnSelectFinish.setOnClickListener((v) -> {
            SendChatHistoryActivity.start(this, selectedUserIdList);
        });
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnSelectAll.setOnClickListener((v) -> {
            if (selectedUserIdList.size() == chatAdapter.getItemCount()) {
                chatAdapter.cancelAll();
            } else {
                chatAdapter.selectAll();
            }
        });
        llSelectedCount = findViewById(R.id.llSelectedCount);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        rvChatList = findViewById(R.id.rvChatList);
        rvChatList.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(this, coreManager.getSelf().getUserId());
        rvChatList.setAdapter(chatAdapter);
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        findViewById(R.id.iv_title_left).setOnClickListener((v) -> {
            onBackPressed();
        });
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.select_chat_history));
    }

    @Override
    public void onItemSelectedChange(ChatAdapter.Item item, boolean isSelected) {
        Log.i(TAG, "checked change " + isSelected + ", " + item);
        if (item.selected) {
            selectedUserIdList.add(item.getUserId());
        } else {
            selectedUserIdList.remove(item.getUserId());
        }
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        if (selectedUserIdList.isEmpty()) {
            btnSelectFinish.setEnabled(false);
            llSelectedCount.setVisibility(View.GONE);
        } else {
            btnSelectFinish.setEnabled(true);
            llSelectedCount.setVisibility(View.VISIBLE);
            tvSelectedCount.setText(getString(R.string.migrate_chat_count_place_holder, selectedUserIdList.size()));
        }
        if (selectedUserIdList.size() == chatAdapter.getItemCount()) {
            btnSelectAll.setText(R.string.cancel_all);
        } else {
            btnSelectAll.setText(R.string.select_all);
        }
    }
}
