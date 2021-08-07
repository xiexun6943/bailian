package com.ydd.zhichat.ui.mucfile;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.daimajia.numberprogressbar.NumberProgressBar;
import com.ydd.zhichat.R;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.ui.base.BaseListActivity;
import com.ydd.zhichat.ui.mucfile.bean.DownBean;
import com.ydd.zhichat.ui.mucfile.bean.MucFileBean;
import com.ydd.zhichat.util.TimeUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.LoadingDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;

import static com.ydd.zhichat.ui.mucfile.DownManager.STATE_DOWNLOADED;
import static com.ydd.zhichat.ui.mucfile.DownManager.STATE_DOWNLOADING;
import static com.ydd.zhichat.ui.mucfile.DownManager.STATE_PAUSEDOWNLOAD;
import static com.ydd.zhichat.ui.mucfile.DownManager.STATE_UNDOWNLOAD;

public class MucFileListActivity extends BaseListActivity<MucFileListActivity.MucFileHolder> implements DownManager.DownLoadObserver {
    private static final int REQUEST_CODE_SELECT_FILE = 10086;
    public int mRole;
    List<MucFileBean> mdatas = new ArrayList<>();
    boolean cancel = false;
    AlertDialog mAlertDialog;
    LoadingDialog mProDialog;
    private String mRoomId;
    private String mUserId;
    private int mAllowUploadFile;

    @Override
    public void initView() {
        mRoomId = getIntent().getStringExtra("roomId");
        mRole = getIntent().getIntExtra("role", 3);
        mAllowUploadFile = getIntent().getIntExtra("allowUploadFile", 1);
        mUserId = coreManager.getSelf().getUserId();
        initActionBar();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JXRoomMemberVC_ShareFile"));
        ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.ic_app_add);
        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAllowUploadFile == 1 || mRole == 1 || mRole == 2) {
                    Intent intent = new Intent(MucFileListActivity.this, AddMucFileActivity.class);
                    intent.putExtra("roomId", mRoomId);
                    startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
                } else {
                    ToastUtil.showToast(MucFileListActivity.this, R.string.tip_cannot_upload);
                }
            }
        });
    }

    @Override
    public void initFristDatas() {

    }

    @Override
    public void initDatas(int pager) {
        if (pager == 0) {
            mdatas.clear();
        }
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);
        params.put("pageSize", "10");
        params.put("pageIndex", "" + pager);
        HttpUtils.get().url(coreManager.getConfig().UPLOAD_MUC_FILE_FIND_ALL)
                .params(params)
                .build()
                .execute(new ListCallback<MucFileBean>(MucFileBean.class) {
                    @Override
                    public void onResponse(ArrayResult<MucFileBean> result) {
                        List<MucFileBean> datas = result.getData();
                        if (datas != null && datas.size() > 0) {
                            mdatas.addAll(datas);
                            update(mdatas);
                            if (datas.size() != 10) {
                                more = false;
                            }
                        } else {
                            update(null);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        update(null);
                    }
                });
    }

    @Override
    public MucFileHolder initHolder(ViewGroup parent) {
        View v = mInflater.inflate(R.layout.activity_muc_file, parent, false);
        MucFileHolder holder = new MucFileHolder(v);
        return holder;
    }

    @Override
    public void fillData(MucFileHolder holder, int position) {
        MucFileBean bean = mdatas.get(position);
        if (TextUtils.isEmpty(bean.getName())) {
            if (!TextUtils.isEmpty(bean.getUrl())) {
                String sp[] = bean.getUrl().split("/");
                if (sp.length > 1) {
                    bean.setName(sp[sp.length - 1]);
                } else {
                    bean.setName(bean.getUrl());
                }
            } else {
                bean.setName("Not acquired");
            }
        }

        holder.tvName.setText(bean.getName());
        holder.tvFrom.setText(bean.getNickname());
        holder.tvTime.setText(TimeUtils.s_long_2_str(bean.getTime() * 1000));
        holder.tvSize.setText(XfileUtils.fromatSize(bean.getSize()) + " " + InternationalizationHelper.getString("JXFile_from"));
        // 获取本地下载状态
        DownBean info = DownManager.instance().getDownloadState(bean);
        if (info.state == STATE_UNDOWNLOAD) {
            holder.progressBar.setProgressTextVisibility(NumberProgressBar.ProgressTextVisibility.Invisible);
            holder.ivOk.setVisibility(View.GONE);
            holder.rbCase.setVisibility(View.GONE);
            holder.tvTime.setVisibility(View.VISIBLE);
        } else if (info.state == STATE_DOWNLOADING) {  // 下载中
            holder.rbCase.setChecked(false);
            holder.rbCase.setVisibility(View.VISIBLE);
            holder.tvTime.setVisibility(View.GONE);
            holder.ivOk.setVisibility(View.GONE);
            holder.progressBar.setProgressTextVisibility(NumberProgressBar.ProgressTextVisibility.Visible);
        } else if (info.state == STATE_PAUSEDOWNLOAD) { // 暂停下载
            holder.rbCase.setVisibility(View.VISIBLE);
            holder.rbCase.setChecked(true);
            holder.tvTime.setVisibility(View.GONE);
            holder.ivOk.setVisibility(View.GONE);
            holder.progressBar.setProgressTextVisibility(NumberProgressBar.ProgressTextVisibility.Visible);
        } else if (info.state == STATE_DOWNLOADED) {    // 下载完成
            holder.ivOk.setVisibility(View.VISIBLE);
            holder.rbCase.setVisibility(View.GONE);
            holder.tvTime.setVisibility(View.VISIBLE);
            holder.progressBar.setProgressTextVisibility(NumberProgressBar.ProgressTextVisibility.Visible);
        } else {
            holder.progressBar.setProgressTextVisibility(NumberProgressBar.ProgressTextVisibility.Visible);
            holder.ivOk.setVisibility(View.GONE);
            holder.rbCase.setVisibility(View.GONE);
            holder.tvTime.setVisibility(View.VISIBLE);
        }
        holder.progressBar.setProgress(XfileUtils.getProgress(info.cur, info.max));
        if (bean.getType() == 1) {
            // 图片直接显示
            Glide.with(MucFileListActivity.this).load(bean.getUrl()).override(100, 100).centerCrop().into(holder.ivInco);
        } else {
            // 加载本地
            XfileUtils.setFileInco(bean.getType(), holder.ivInco);
        }
    }

    /**
     * 长按删除
     */
    private void onItemLongClick(final int layoutPosition) {
        cancel = false;
        MucFileBean bena = mdatas.get(layoutPosition);
        AlertDialog.Builder b = new AlertDialog.Builder(mContext);
        View v = mInflater.inflate(R.layout.activity_user_checked, null);
        TextView tvTitle = (TextView) v.findViewById(R.id.title_tv);
        TextView tvContent = (TextView) v.findViewById(R.id.des_tv);
        Button tvNetBtn = (Button) v.findViewById(R.id.left_btn);
        Button tvlocalBtn = (Button) v.findViewById(R.id.right_btn);
       /* String str = "删除群文件将从群文件中移除";
        tvlocalBtn.setText("删除本地");
        tvTitle.setText("删除文件");
        tvNetBtn.setText("刪除群文件");*/
        String str = InternationalizationHelper.getString("REMOVE_BY_GROUP");
        tvTitle.setText(InternationalizationHelper.getString("DELETE_FILE"));
        tvNetBtn.setText(InternationalizationHelper.getString("DELETE_GROUP"));
        tvlocalBtn.setText(InternationalizationHelper.getString("DELETE_LOCAL"));

        if (mRole > 2) {
            // 普通人
            if (bena.getUserId().equals(mUserId)) {
                if (bena.getState() == STATE_DOWNLOADED) {
                    // str = str + "\n\n删除本地将会删除本地文件";
                    str = str + "\n\n" + InternationalizationHelper.getString("REMOVE_BY_LOVAL");
                } else {
                    tvlocalBtn.setVisibility(View.GONE);
                }
            } else {
                if (bena.getState() == STATE_DOWNLOADED) {
                    // str = "删除本地将会删除本地文件";
                    str = InternationalizationHelper.getString("REMOVE_BY_LOVAL");
                    tvNetBtn.setVisibility(View.GONE);

                } else {
                    cancel = true;
                    str = getString(R.string.tip_cannot_remove_file);
                    // tvlocalBtn.setText("取消");
                    tvlocalBtn.setText(InternationalizationHelper.getString("JX_Cencal"));
                    tvNetBtn.setVisibility(View.GONE);
                }
            }
        } else {
            if (bena.getState() != STATE_DOWNLOADED) {
                tvlocalBtn.setVisibility(View.GONE);
                v.findViewById(R.id.v_dark).setVisibility(View.GONE);
            } else {
                str = str + "\n\n" + InternationalizationHelper.getString("REMOVE_BY_LOVAL");
            }
        }

        tvContent.setText(str);
        tvNetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                }
                mProDialog = new LoadingDialog(mContext);
                mProDialog.show();
                delNetFile(mdatas.get(layoutPosition));
            }
        });

        tvlocalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!cancel) {
                    DownManager.instance().detele(mdatas.get(layoutPosition));
                    notifyItemData(0);
                }

                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                }
            }
        });

        b.setView(v);
        mAlertDialog = b.create();
        mAlertDialog.show();
    }

    private void delNetFile(final MucFileBean data) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);
        params.put("userId", mUserId);
        params.put("shareId", data.getShareId());

        HttpUtils.get().url(coreManager.getConfig().UPLOAD_MUC_FILE_DEL)
                .params(params)
                .build()
                .execute(new ListCallback<MucFileBean>(MucFileBean.class) {
                    @Override
                    public void onResponse(ArrayResult<MucFileBean> result) {
                        mProDialog.dismiss();
                        mdatas.remove(data);
                        notifyItemData(0);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        mProDialog.dismiss();
                    }
                });
    }


    private void onItemClick(int position) {
        MucFileBean bean = mdatas.get(position);
        if (bean.getState() == STATE_DOWNLOADED && bean.getType() == 1) {
            // 去打开
            Intent intent = new Intent(MucFileListActivity.this, MucFilePreviewActivity.class);
            MucFileBean data = mdatas.get(position);
            intent.putExtra("data", data);
            startActivity(intent);

        } else {
            // 去下载
            Intent intent = new Intent(MucFileListActivity.this, MucFileDetails.class);
            MucFileBean data = mdatas.get(position);
            intent.putExtra("data", data);
            startActivity(intent);
        }
    }

    private void onItemCase(int position, boolean isChecked) {
        MucFileBean bean = mdatas.get(position);
        if (isChecked) {
            DownManager.instance().download(bean);
        } else {
            DownManager.instance().pause(bean);
        }
    }

    @Override
    public void onDownLoadInfoChange(DownBean info) {
        // 实现单条刷新
        if (mAdapter != null) {
            for (int i = 0; i < mdatas.size(); i++) {
                MucFileBean data = mdatas.get(i);
                if (data.getUrl().equals(info.url)) {
                    data.setState(info.state);
                    int progress = (int) (info.cur / (float) info.max * 100);
                    data.setProgress(progress);

                    notifyItemData(i);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DownManager.instance().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DownManager.instance().deleteObserver(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getIntExtra("code", 0) == 200) {
            initDatas(0);
        }
    }

    public class MucFileHolder extends RecyclerView.ViewHolder {
        public ImageView ivInco;// 图标
        public ImageView ivOk;  // 图标
        public TextView tvName; // 名称
        public TextView tvSize; // 大小 +_来自
        public TextView tvFrom; // 属于
        public TextView tvTime; // 时间
        public CheckBox rbCase;
        NumberProgressBar progressBar;

        public MucFileHolder(View itemView) {
            super(itemView);
            ivOk = (ImageView) itemView.findViewById(R.id.item_file_ok);
            ivInco = (ImageView) itemView.findViewById(R.id.item_file_inco);
            tvName = (TextView) itemView.findViewById(R.id.item_file_name);
            tvFrom = (TextView) itemView.findViewById(R.id.item_file_from);
            tvSize = (TextView) itemView.findViewById(R.id.item_file_size);
            tvTime = (TextView) itemView.findViewById(R.id.item_file_time);
            rbCase = (CheckBox) itemView.findViewById(R.id.item_file_case);
            progressBar = (NumberProgressBar) itemView.findViewById(R.id.number_progress_bar);
            itemView.findViewById(R.id.ll_item_file).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(getLayoutPosition());
                }
            });

            itemView.findViewById(R.id.ll_item_file).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onItemLongClick(getLayoutPosition());
                    return true;
                }
            });

            rbCase.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rbCase.setChecked(!rbCase.isChecked());
                    onItemCase(getLayoutPosition(), rbCase.isChecked());
                }
            });
        }
    }
}
