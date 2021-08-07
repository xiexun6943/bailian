package com.ydd.zhichat.ui.me.redpacket;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.redpacket.ConsumeRecordItem;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.ui.base.BaseListActivity;
import com.ydd.zhichat.ui.mucfile.XfileUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;

/**
 * Created by wzw on 2016/9/26.
 */
public class MyConsumeRecord extends BaseListActivity<MyConsumeRecord.MyConsumeHolder> {
    private static final String TAG = "MyConsumeRecord";
    List<ConsumeRecordItem.PageDataEntity> datas = new ArrayList<>();

    @Override
    public void initView() {
        super.initView();
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
    }

    @Override
    public void initDatas(int pager) {
        if (pager == 0) {
            datas.clear();
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        // 如果是下拉刷新就重新加载第一页
        params.put("pageIndex", pager + "");
        params.put("pageSize", "30");
        HttpUtils.get().url(coreManager.getConfig().CONSUMERECORD_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<ConsumeRecordItem>(ConsumeRecordItem.class) {

                    @Override
                    public void onResponse(ObjectResult<ConsumeRecordItem> result) {
                        if (result.getData().getPageData() != null) {
                            for (ConsumeRecordItem.PageDataEntity data : result.getData().getPageData()) {
                                final double money = data.getOperationAmount();
                                boolean isZero = Double.toString(money).equals("0.0");
                                Log.d(TAG, "bool : " + isZero + " \t" + money);
                                if (!isZero) {
                                    datas.add(data);
                                }
                            }
                            if (result.getData().getPageData().size() != 30) {
                                more = false;
                            } else {
                                more = true;
                            }
                        } else {
                            more = false;
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update(datas);
                            }
                        });
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(MyConsumeRecord.this);
                    }
                });
    }

    @Override
    public MyConsumeHolder initHolder(ViewGroup parent) {
        View v = mInflater.inflate(R.layout.consumerecord_item, parent, false);
        MyConsumeHolder holder = new MyConsumeHolder(v);
        return holder;
    }

    @Override
    public void fillData(MyConsumeHolder holder, int position) {
        ConsumeRecordItem.PageDataEntity info = datas.get(position);
        long time = Long.valueOf(info.getTime());
        String StrTime = XfileUtils.fromatTime(time * 1000, "MM-dd HH:mm");
        holder.nameTv.setText(info.getDesc());
        holder.timeTv.setText(StrTime);
        holder.moneyTv.setText(XfileUtils.fromatFloat(info.getOperationAmount()) + InternationalizationHelper.getString("YUAN"));
    }

    public class MyConsumeHolder extends RecyclerView.ViewHolder {
        public TextView nameTv, timeTv, moneyTv;

        public MyConsumeHolder(View itemView) {
            super(itemView);
            nameTv = (TextView) itemView.findViewById(R.id.textview_name);
            timeTv = (TextView) itemView.findViewById(R.id.textview_time);
            moneyTv = (TextView) itemView.findViewById(R.id.textview_money);
        }
    }
}
