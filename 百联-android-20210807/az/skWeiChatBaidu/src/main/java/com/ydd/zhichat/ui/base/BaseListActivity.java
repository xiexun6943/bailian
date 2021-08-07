package com.ydd.zhichat.ui.base;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.ydd.zhichat.R;

import java.util.List;

public abstract class BaseListActivity<VH extends RecyclerView.ViewHolder> extends BaseActivity {
    public LayoutInflater mInflater;
    RecyclerView mRecyclerView;
    public SwipeRefreshLayout mSSRlayout;
    FrameLayout mFlNoDatas;
    public PreviewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_list);
        mRecyclerView = (RecyclerView) findViewById(R.id.fragment_list_recyview);
        mSSRlayout = (SwipeRefreshLayout) findViewById(R.id.fragment_list_swip);
        mFlNoDatas = (FrameLayout) findViewById(R.id.fl_empty);
        mInflater = LayoutInflater.from(this);

        mSSRlayout.setRefreshing(true);
        initView();
        initFristDatas();
        initBaseView();
    }

    public void initView() {

    }

    public void initFristDatas() {

    }

    protected void initBaseView() {
        mSSRlayout.setColorSchemeResources(R.color.orange, R.color.purple,
                R.color.btn_live_2);
        mSSRlayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initDatas(0);
                pager = 0;
                loading = false;
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new PreviewAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener(layoutManager));
        more = true;
        initDatas(0);
        pager = 0;
    }

    class PreviewAdapter extends RecyclerView.Adapter<VH> {
        private List<?> data;

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return initHolder(parent);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            fillData(holder, position);
        }

        @Override
        public int getItemCount() {
            if (data != null) {
                return data.size();
            }
            return 0;
        }

        public void setData(List<?> data) {
            if (data != null) {
                this.data = data;
                notifyDataSetChanged();

            }
        }
    }

    private int pager;
    private boolean loading = true;
    public boolean more = false;

    public class EndlessRecyclerOnScrollListener extends
            RecyclerView.OnScrollListener {

        private int previousTotal = 0;
        int firstVisibleItem, visibleItemCount, totalItemCount;
        private LinearLayoutManager mLinearLayoutManager;

        public EndlessRecyclerOnScrollListener(
                LinearLayoutManager linearLayoutManager) {
            this.mLinearLayoutManager = linearLayoutManager;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (!more) {
                // 外界不让加载数据了
                return;
            }
            visibleItemCount = recyclerView.getChildCount();
            totalItemCount = mLinearLayoutManager.getItemCount();
            firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition();

            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount) <= firstVisibleItem) {
                pager++;
                initDatas(pager);
                loading = true;
            }
        }
    }

    /**
     * 数据层面
     */
    public abstract void initDatas(int pager);

    /* 视图层面 */
    public abstract VH initHolder(ViewGroup parent);

    public abstract void fillData(VH holder, int position);

    /**
     * 通知更新
     */
    public void update(List<?> data) {
        if (data != null && data.size() > 0) {
            mFlNoDatas.setVisibility(View.GONE);
            if (mSSRlayout.isRefreshing()) {
                mSSRlayout.setRefreshing(false);
            }
            mAdapter.setData(data);
        } else {
            if (mSSRlayout.isRefreshing()) {
                mSSRlayout.setRefreshing(false);
            }
            mFlNoDatas.setVisibility(View.VISIBLE);
            more = false;
        }
    }

    /*
     * 单条刷新
     * */
    public void notifyItemData(int position) {
        mAdapter.notifyDataSetChanged();
    }
}
