package com.ydd.zhichat.ui.circle.range;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.bean.circle.Praise;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.other.BasicInfoActivity;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.HeadView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;

interface OnLoadMoreListener {
    void onLoadMore(int currentPage);
}

/**
 * 点赞列表单独一页做分页加载，
 */
public class PraiseListActivity extends BaseActivity implements OnLoadMoreListener {
    private String messageId;
    private RecyclerView rvContent;
    private MyAdapter adapter;
    private MyOnScrollListener onScrollListener;

    public static void start(Context ctx, String messageId) {
        Intent intent = new Intent(ctx, PraiseListActivity.class);
        intent.putExtra("messageId", messageId);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_praise_list);

        messageId = getIntent().getStringExtra("messageId");

        rvContent = findViewById(R.id.rvContent);
        rvContent.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyAdapter(this);
        rvContent.setAdapter(adapter);

        initData();
    }

    private void initData() {
        onScrollListener = new MyOnScrollListener(this);
        rvContent.addOnScrollListener(onScrollListener);
        loadData(0);
    }

    private void loadData(int index) {
        int pageSize = 20;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("pageIndex", String.valueOf(index));
        params.put("pageSize", String.valueOf(pageSize));
        params.put("messageId", messageId);

        String url = coreManager.getConfig().MSG_PRAISE_LIST;
        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new ListCallback<Praise>(Praise.class) {
                    @Override
                    public void onResponse(ArrayResult<Praise> result) {
                        List<Praise> data = result.getData();
                        if (data.size() > 0) {
                            adapter.addAll(data);
                        }
                        onScrollListener.setNoMore(data.size() < pageSize);
                        if (onScrollListener.isNoMore()) {
                            ToastUtil.showToast(mContext, R.string.tip_no_more);
                        }
                        onScrollListener.setLoaded();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Reporter.post("点赞分页加载失败，", e);
                        ToastUtil.showToast(getApplicationContext(), getString(R.string.tip_praise_load_error));
                        onScrollListener.setNoMore(true);
                        onScrollListener.setLoaded();
                    }
                });
    }

    @Override
    public void onLoadMore(int currentPage) {
        loadData(currentPage);
    }

    static class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private final LayoutInflater layoutInflater;
        private List<Praise> data = new ArrayList<>();

        MyAdapter(Context ctx) {
            layoutInflater = LayoutInflater.from(ctx);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(layoutInflater.inflate(R.layout.item_praise, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Praise item = data.get(position);
            AvatarHelper.getInstance().displayAvatar(item.getNickName(), item.getUserId(), holder.hvHead.getHeadImage(), false);
            holder.tvName.setText(item.getNickName());
            holder.tvTime.setText(DateUtils.getRelativeTimeSpanString(
                    item.getTime() * TimeUnit.SECONDS.toMillis(1),
                    System.currentTimeMillis(),
                    TimeUnit.SECONDS.toMillis(1))
            );
            holder.itemView.setOnClickListener(v -> {
                BasicInfoActivity.start(v.getContext(), item.getUserId());
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public void setData(List<Praise> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        public void addAll(List<Praise> data) {
            this.data.addAll(data);
            notifyItemRangeChanged(
                    this.data.size() - data.size(),
                    data.size()
            );
        }
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private HeadView hvHead = itemView.findViewById(R.id.hvHead);
        private TextView tvName = itemView.findViewById(R.id.tvName);
        private TextView tvTime = itemView.findViewById(R.id.tvTime);

        MyViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class MyOnScrollListener extends RecyclerView.OnScrollListener {

        //当前页，从0开始
        private int currentPage = 0;

        //已经加载出来的Item的数量
        private int totalItemCount;

        //主要用来存储上一个totalItemCount
        private int previousTotal = 0;

        //在屏幕上可见的item数量
        private int visibleItemCount;

        //在屏幕可见的Item中的第一个
        private int firstVisibleItem;

        //是否正在上拉数据
        private boolean loading = true;
        private OnLoadMoreListener listener;
        private boolean noMore = true;

        MyOnScrollListener(OnLoadMoreListener listener) {
            this.listener = listener;
        }

        public boolean isNoMore() {
            return noMore;
        }

        void setNoMore(boolean noMore) {
            this.noMore = noMore;
        }

        void setLoaded() {
            loading = false;
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (noMore) {
                return;
            }
            visibleItemCount = recyclerView.getChildCount();
            totalItemCount = recyclerView.getLayoutManager().getItemCount();
            firstVisibleItem = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
            if (loading) {

                if (totalItemCount > previousTotal) {
                    //说明数据已经加载结束
                    loading = false;
                    previousTotal = totalItemCount;
                }
            }
            if (!loading && totalItemCount - visibleItemCount <= firstVisibleItem) {
                currentPage++;
                listener.onLoadMore(currentPage);
                loading = true;
            }
        }
    }
}
