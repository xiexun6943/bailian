package com.ydd.zhichat.ui.circle;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.ydd.zhichat.AppConfig;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.adapter.PublicMessageRecyclerAdapter;
import com.ydd.zhichat.bean.circle.Comment;
import com.ydd.zhichat.bean.circle.PublicMessage;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.db.dao.CircleMessageDao;
import com.ydd.zhichat.downloader.Downloader;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.helper.FileDataHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.circle.range.NewZanActivity;
import com.ydd.zhichat.ui.circle.range.SendAudioActivity;
import com.ydd.zhichat.ui.circle.range.SendFileActivity;
import com.ydd.zhichat.ui.circle.range.SendShuoshuoActivity;
import com.ydd.zhichat.ui.circle.range.SendVideoActivity;
import com.ydd.zhichat.ui.circle.util.RefreshListImp;
import com.ydd.zhichat.ui.other.BasicInfoActivity;
import com.ydd.zhichat.util.StringUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.PMsgBottomView;
import com.ydd.zhichat.volley.ArrayResult;
import com.ydd.zhichat.volley.StringJsonArrayRequest;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fm.jiecao.jcvideoplayer_lib.JCMediaManager;
import fm.jiecao.jcvideoplayer_lib.JVCideoPlayerStandardSecond;
import okhttp3.Call;

/**
 * 我的商务圈
 */
public class BusinessCircleActivity extends BaseActivity implements showCEView, RefreshListImp {
    private static final int REQUEST_CODE_SEND_MSG = 1;
    // 自定义的弹出框类
    SelectPicPopupWindow menuWindow;
    /**
     * 接口,调用外部类的方法,让应用不可见时停止播放声音
     */
    ListenerAudio listener;
    CommentReplyCache mCommentReplyCache = null;
    private int mType;
    /* mPageIndex仅用于商务圈情况下 */
    private int mPageIndex = 0;
    /* 封面视图 */
    private View mMyCoverView;   // 封面root view
    private ImageView mCoverImg; // 封面图片ImageView
    private ImageView mAvatarImg;// 用户头像
    private PMsgBottomView mPMsgBottomView;
    private List<PublicMessage> mMessages = new ArrayList<>();
    private SmartRefreshLayout mRefreshLayout;
    private SwipeRecyclerView mPullToRefreshListView;
    private PublicMessageRecyclerAdapter mAdapter;
    private String mLoginUserId;       // 当前登陆用户的UserId
    private String mLoginNickName;// 当前登陆用户的昵称
    private boolean isdongtai;
    private String cricleid;
    private String pinglun;
    private String dianzan;
    /* 当前选择的是哪个用户的个人空间,仅用于查看个人空间的情况下 */
    private String mUserId;
    private String mNickName;
    private ImageView mIvTitleLeft;
    private TextView mTvTitle;
    private ImageView mIvTitleRight;
    // 为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {

        public void onClick(View v) {
            menuWindow.dismiss();
            Intent intent = new Intent();
            switch (v.getId()) {
                case R.id.btn_send_picture:// 发表图文，
                    intent.setClass(getApplicationContext(), SendShuoshuoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_voice:  // 发表语音
                    intent.setClass(getApplicationContext(), SendAudioActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_video:  // 发表视频
                    intent.setClass(getApplicationContext(), SendVideoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_file:   // 发表文件
                    intent.setClass(getApplicationContext(), SendFileActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.new_comment:     // 最新评论
                    Intent intent2 = new Intent(getApplicationContext(), NewZanActivity.class);
                    intent2.putExtra("OpenALL", true);
                    startActivity(intent2);
                    break;
                default:
                    break;
            }
        }
    };
    private boolean more;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_circle);
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();

        if (getIntent() != null) {
            mType = getIntent().getIntExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_MY_BUSINESS);// 默认的为查看我的商务圈
            mUserId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
            mNickName = getIntent().getStringExtra(AppConstant.EXTRA_NICK_NAME);

            pinglun = getIntent().getStringExtra("pinglun");
            dianzan = getIntent().getStringExtra("dianzan");
            isdongtai = getIntent().getBooleanExtra("isdongtai", false);
            cricleid = getIntent().getStringExtra("messageid");
        }

        if (!isMyBusiness()) {//如果查看的是个人空间的话，那么mUserId必须要有意义
            if (TextUtils.isEmpty(mUserId)) {// 没有带userId参数，那么默认看的就是自己的空间
                mUserId = mLoginUserId;
                mNickName = mLoginNickName;
            }
        }

       /* if (mUserId != null && mUserId.equals(mLoginUserId)) {
            String mLastMessage = PreferenceUtils.getString(this, "BUSINESS_CIRCLE_DATA");
            if (!TextUtils.isEmpty(mLastMessage)) {
                mMessages = JSON.parseArray(mLastMessage, PublicMessage.class);
            }
        }*/

        initActionBar();
        Downloader.getInstance().init(MyApplication.getInstance().mAppDir + File.separator + coreManager.getSelf().getUserId()
                + File.separator + Environment.DIRECTORY_MOVIES);// 初始化视频下载目录
        initView();
    }

    private boolean isMyBusiness() {
        return mType == AppConstant.CIRCLE_TYPE_MY_BUSINESS;
    }

    private boolean isMySpace() {
        return mLoginUserId.equals(mUserId);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(mNickName);
        mIvTitleRight = (ImageView) findViewById(R.id.iv_title_right);
        if (mUserId.equals(mLoginUserId)) {// 查看自己的空间才有发布按钮
            mIvTitleRight.setImageResource(R.drawable.ic_app_add);
            mIvTitleRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    menuWindow = new SelectPicPopupWindow(BusinessCircleActivity.this, itemsOnClick);
                    // 在获取宽高之前需要先测量，否则得不到宽高
                    menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    // +x右,-x左,+y下,-y上
                    // pop向左偏移显示
                    menuWindow.showAsDropDown(v,
                            -(menuWindow.getContentView().getMeasuredWidth() - v.getWidth() / 2 - 40),
                            0);
                }
            });
        }
    }

    private void initView() {
        mPullToRefreshListView = findViewById(R.id.recyclerView);
        mPullToRefreshListView.setLayoutManager(new LinearLayoutManager(this));
        initCoverView();
        mRefreshLayout = findViewById(R.id.refreshLayout);
        mPMsgBottomView = (PMsgBottomView) findViewById(R.id.bottom_view);
       /* mResizeLayout.setOnResizeListener(new ResizeLayout.OnResizeListener() {
            @Override
            public void OnResize(int w, int h, int oldw, int oldh) {
                if (oldh < h) {// 键盘被隐藏
                    mCommentReplyCache = null;
                    mPMsgBottomView.setHintText("");
                    mPMsgBottomView.reset();
                }
            }
        });*/

        mPMsgBottomView.setPMsgBottomListener(new PMsgBottomView.PMsgBottomListener() {
            @Override
            public void sendText(String text) {
                if (mCommentReplyCache != null) {
                    mCommentReplyCache.text = text;
                    addComment(mCommentReplyCache);
                    mPMsgBottomView.hide();
                }
            }
        });

        if (isdongtai) {
            // 如果是动态，不添加HeadView
        } else {
            mPullToRefreshListView.addHeaderView(mMyCoverView);
        }

        mAdapter = new PublicMessageRecyclerAdapter(this, coreManager, mMessages);
        setListenerAudio(mAdapter);
        mPullToRefreshListView.setAdapter(mAdapter);

        if (isdongtai) {
            mRefreshLayout.setEnableRefresh(false);
            mRefreshLayout.setEnableLoadMore(false);
        }
        mRefreshLayout.setOnRefreshListener(refreshLayout -> {
            requestData(true);
        });
        mRefreshLayout.setOnLoadMoreListener(refreshLayout -> {
            requestData(false);
        });

        mPullToRefreshListView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {

                    @Override
                    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int scrollState) {
                        if (mPMsgBottomView.getVisibility() != View.GONE) {
                            mPMsgBottomView.hide();
                        }
                    }

                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    }
                });

        if (isMyBusiness()) {
            readFromLocal();
        } else {
            requestData(true);
        }
    }

    private void initCoverView() {
        mMyCoverView = LayoutInflater.from(this).inflate(R.layout.space_cover_view, mPullToRefreshListView, false);
        mMyCoverView.findViewById(R.id.ll_btn_send).setVisibility(View.GONE);
        mCoverImg = (ImageView) mMyCoverView.findViewById(R.id.cover_img);
        mAvatarImg = (ImageView) mMyCoverView.findViewById(R.id.avatar_img);
        // 头像
        if (isMyBusiness() || isMySpace()) {
            AvatarHelper.getInstance().displayAvatar(mLoginNickName, mLoginUserId, mAvatarImg, true);
            // 优先加载user信息中的背景图片，失败就加载头像，
            String bg = coreManager.getSelf().getMsgBackGroundUrl();
            if (!TextUtils.isEmpty(bg)) {
                Glide.with(this)
                        .load(bg)
                        .placeholder(R.drawable.avatar_normal)
                        .dontAnimate()
                        .into(new SimpleTarget<GlideDrawable>() {
                            @Override
                            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                                mCoverImg.setImageDrawable(resource);
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                AvatarHelper.getInstance().displayRoundAvatar(mLoginNickName, mLoginUserId, mCoverImg, false);
                            }
                        });
            } else {
                AvatarHelper.getInstance().displayRoundAvatar(mLoginNickName, mLoginUserId, mCoverImg, false);
            }
        } else {
            AvatarHelper.getInstance().displayAvatar(mNickName, mUserId, mAvatarImg, true);
            AvatarHelper.getInstance().displayRoundAvatar(mNickName, mUserId, mCoverImg, false);
        }
        mAvatarImg.setOnClickListener(v -> {// 进入个人资料页
            Intent intent = new Intent(getApplicationContext(), BasicInfoActivity.class);
            if (isMyBusiness() || isMySpace()) {
                intent.putExtra(AppConstant.EXTRA_USER_ID, mLoginUserId);
            } else {
                intent.putExtra(AppConstant.EXTRA_USER_ID, mUserId);
            }
            startActivity(intent);
        });
    }

    private void readFromLocal() {
        FileDataHelper.readArrayData(getApplicationContext(), mLoginUserId, FileDataHelper.FILE_BUSINESS_CIRCLE, new StringJsonArrayRequest.Listener<PublicMessage>() {
            @Override
            public void onResponse(ArrayResult<PublicMessage> result) {
                if (result != null && result.getData() != null) {
                    mMessages.clear();
                    mMessages.addAll(result.getData());
                    mAdapter.notifyDataSetChanged();
                }
                requestData(true);
            }
        }, PublicMessage.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (mPMsgBottomView != null && mPMsgBottomView.getVisibility() == View.VISIBLE) {
            mPMsgBottomView.hide();
        } else {
            // 点返回键退出全屏视频，
            // 如果PublicMessageAdapter用在其他activity, 也要加上，
            if (JVCideoPlayerStandardSecond.backPress()) {
                JCMediaManager.instance().recoverMediaPlayer();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listener != null) {
            listener.ideChange();
        }
        listener = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       /* if (mUserId.equals(mLoginUserId)) {
            if (mMessages != null && mMessages.size() > 0) {
                PreferenceUtils.putString(this, "BUSINESS_CIRCLE_DATA", JSON.toJSONString(mMessages));
            }
        }*/
    }

    public void setListenerAudio(ListenerAudio listener) {
        this.listener = listener;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SEND_MSG) {
            if (resultCode == Activity.RESULT_OK) {// 发说说成功
                String messageId = data.getStringExtra(AppConstant.EXTRA_MSG_ID);
                CircleMessageDao.getInstance().addMessage(mLoginUserId, messageId);
                requestData(true);
                removeNullTV();
            }
        }
    }

    /********** 公共消息的数据请求部分 *********/

    /**
     * 请求公共消息
     *
     * @param isPullDwonToRefersh 是下拉刷新，还是上拉加载
     */
    private void requestData(boolean isPullDwonToRefersh) {
        if (isMyBusiness()) {
            requestMyBusiness(isPullDwonToRefersh);
        } else {
            if (isdongtai) {
                if (isPullDwonToRefersh) {
                    more = true;
                }
                if (!more) {
                    // ToastUtil.showToast(getContext(), getString(R.string.tip_last_item));
                    mRefreshLayout.setNoMoreData(true);
                    refreshComplete();
                } else {
                    requestSpacedongtai(isPullDwonToRefersh);
                }
            } else {
                requestSpace(isPullDwonToRefersh);
            }
        }
    }

    /**
     * 停止刷新动画
     */
    private void refreshComplete() {
        mPullToRefreshListView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.finishRefresh();
                mRefreshLayout.finishLoadMore();
            }
        }, 200);
    }

    private void requestMyBusiness(final boolean isPullDwonToRefersh) {
        if (isPullDwonToRefersh) {
            mPageIndex = 0;
        }
        List<String> msgIds = CircleMessageDao.getInstance().getCircleMessageIds(mLoginUserId, mPageIndex, AppConfig.PAGE_SIZE);

        if (msgIds == null || msgIds.size() <= 0) {
            refreshComplete();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("ids", JSON.toJSONString(msgIds));

        HttpUtils.get().url(coreManager.getConfig().MSG_GETS)
                .params(params)
                .build()
                .execute(new ListCallback<PublicMessage>(PublicMessage.class) {
                    @Override
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ArrayResult<PublicMessage> result) {
                        List<PublicMessage> data = result.getData();
                        if (isPullDwonToRefersh) {
                            mMessages.clear();
                        }
                        if (data != null && data.size() > 0) {// 没有更多数据
                            mPageIndex++;
                            if (isPullDwonToRefersh) {
                                FileDataHelper.writeFileData(getApplicationContext(), mLoginUserId, FileDataHelper.FILE_BUSINESS_CIRCLE, result);
                            }
                            mMessages.addAll(data);
                        }
                        mAdapter.notifyDataSetChanged();

                        refreshComplete();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getApplicationContext());
                        refreshComplete();
                    }
                });
    }

    private void requestSpace(final boolean isPullDwonToRefersh) {
        String messageId = null;
        if (!isPullDwonToRefersh && mMessages.size() > 0) {
            messageId = mMessages.get(mMessages.size() - 1).getMessageId();
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mUserId);
        params.put("flag", PublicMessage.FLAG_NORMAL + "");

        if (!TextUtils.isEmpty(messageId)) {
            if (isdongtai) {
                params.put("messageId", cricleid);
            } else {
                params.put("messageId", messageId);
            }
        }
        params.put("pageSize", String.valueOf(AppConfig.PAGE_SIZE));

        HttpUtils.get().url(coreManager.getConfig().MSG_USER_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<PublicMessage>(PublicMessage.class) {
                    @Override
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ArrayResult<PublicMessage> result) {
                        List<PublicMessage> data = result.getData();
                        if (isPullDwonToRefersh) {
                            mMessages.clear();
                        }
                        if (data != null && data.size() > 0) {
                            mMessages.addAll(data);
                        }
                        more = !(data == null || data.size() < AppConfig.PAGE_SIZE);
                        mAdapter.notifyDataSetChanged();

                        if (more) {
                            mRefreshLayout.resetNoMoreData();
                        } else {
                            mRefreshLayout.setNoMoreData(true);
                        }
                        refreshComplete();
                        if (mAdapter.getItemCount() == 0)
                            addNullTV2LV();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getApplicationContext());
                        refreshComplete();
                    }
                });
    }

    // 最近评论&赞进入
    private void requestSpacedongtai(final boolean isPullDwonToRefersh) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", cricleid);

        HttpUtils.get().url(coreManager.getConfig().MSG_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<PublicMessage>(PublicMessage.class) {
                    @Override
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ObjectResult<PublicMessage> result) {

                        PublicMessage datas = result.getData();
                        List<PublicMessage> datass = new ArrayList<>();
                        datass.add(datas);
                        if (isPullDwonToRefersh) {
                            mMessages.clear();
                        }
                        mMessages.addAll(datass);
                        mAdapter.notifyDataSetChanged();

                        refreshComplete();
                        if (mAdapter.getItemCount() == 0)
                            addNullTV2LV();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getApplicationContext());
                        refreshComplete();
                    }
                });
    }

    public void showCommentEnterView(int messagePosition, String toUserId, String toNickname, String toShowName) {
        mCommentReplyCache = new CommentReplyCache();
        mCommentReplyCache.messagePosition = messagePosition;
        mCommentReplyCache.toUserId = toUserId;
        mCommentReplyCache.toNickname = toNickname;
        if (TextUtils.isEmpty(toUserId) || TextUtils.isEmpty(toNickname) || TextUtils.isEmpty(toShowName)) {
            mPMsgBottomView.setHintText("");
        } else {
            mPMsgBottomView.setHintText(getString(R.string.replay_text, toShowName));
        }
        mPMsgBottomView.show();
    }

    private void addComment(CommentReplyCache cache) {
        Comment comment = new Comment();
        comment.setUserId(mLoginUserId);
        comment.setNickName(mLoginNickName);
        comment.setToUserId(cache.toUserId);
        comment.setToNickname(cache.toNickname);
        comment.setBody(cache.text);
        addComment(cache.messagePosition, comment);
    }

    private void addComment(final int position, final Comment comment) {
        final PublicMessage message = mMessages.get(position);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", message.getMessageId());
        if (!TextUtils.isEmpty(comment.getToUserId())) {
            params.put("toUserId", comment.getToUserId());
        }
        if (!TextUtils.isEmpty(comment.getToNickname())) {
            params.put("toNickname", comment.getToNickname());
        }
        params.put("body", comment.getBody());

        HttpUtils.get().url(coreManager.getConfig().MSG_COMMENT_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ObjectResult<String> result) {
                        List<Comment> comments = message.getComments();
                        if (comments == null) {
                            comments = new ArrayList<>();
                            message.setComments(comments);
                        }
                        comment.setCommentId(result.getData());
                        comments.add(comment);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getApplicationContext());
                    }
                });
    }

    @Override
    public void showView(int messagePosition, String toUserId, String toNickname, String toShowName) {
        showCommentEnterView(messagePosition, toUserId, toNickname, toShowName);
    }

    @Override
    public void refreshAfterOperation(PublicMessage message) {
        int size = mMessages.size();
        for (int i = 0; i < size; i++) {
            if (StringUtils.strEquals(mMessages.get(i).getMessageId(), message.getMessageId())) {
                mMessages.set(i, message);
                mAdapter.setData(mMessages);
            }
        }
    }

    public void addNullTV2LV() {
        TextView nullTextView = new TextView(this);
        nullTextView.setTag("NullTV");
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int paddingSize = getResources().getDimensionPixelSize(R.dimen.NormalPadding);
        nullTextView.setPadding(0, paddingSize, 0, paddingSize);
        nullTextView.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        nullTextView.setGravity(Gravity.CENTER);

        nullTextView.setLayoutParams(lp);
        nullTextView.setText(InternationalizationHelper.getString("JX_NoData"));
        mPullToRefreshListView.addFooterView(nullTextView);
        mRefreshLayout.setEnableRefresh(false);
    }

    public void removeNullTV() {
        mPullToRefreshListView.removeFooterView(mPullToRefreshListView.findViewWithTag("NullTV"));
        mRefreshLayout.setEnableRefresh(true);
    }

    public interface ListenerAudio {
        void ideChange();
    }

    class CommentReplyCache {
        int messagePosition;// 消息的Position
        String toUserId;
        String toNickname;
        String text;
    }
}
