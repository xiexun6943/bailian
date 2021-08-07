package com.ydd.zhichat.ui.circle;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.signature.StringSignature;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.adapter.MessageEventHongdian;
import com.ydd.zhichat.adapter.PublicMessageRecyclerAdapter;
import com.ydd.zhichat.bean.EventAvatarUploadSuccess;
import com.ydd.zhichat.bean.MyZan;
import com.ydd.zhichat.bean.circle.Comment;
import com.ydd.zhichat.bean.circle.PublicMessage;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.db.dao.CircleMessageDao;
import com.ydd.zhichat.db.dao.MyZanDao;
import com.ydd.zhichat.db.dao.UserAvatarDao;
import com.ydd.zhichat.db.dao.UserDao;
import com.ydd.zhichat.downloader.Downloader;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.circle.range.NewZanActivity;
import com.ydd.zhichat.ui.circle.range.SendAudioActivity;
import com.ydd.zhichat.ui.circle.range.SendFileActivity;
import com.ydd.zhichat.ui.circle.range.SendShuoshuoActivity;
import com.ydd.zhichat.ui.circle.range.SendVideoActivity;
import com.ydd.zhichat.ui.mucfile.UploadingHelper;
import com.ydd.zhichat.ui.other.BasicInfoActivity;
import com.ydd.zhichat.util.AnimationUtil;
import com.ydd.zhichat.util.CameraUtil;
import com.ydd.zhichat.util.LogUtils;
import com.ydd.zhichat.util.StringUtils;
import com.ydd.zhichat.util.TimeUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.util.UiUtils;
import com.ydd.zhichat.view.FadingScrollView;
import com.ydd.zhichat.view.SkinImageView;
import com.ydd.zhichat.view.TrillCommentInputDialog;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import okhttp3.Call;

public class DiscoverActivity extends BaseActivity {
    private static final int REQUEST_CODE_SEND_MSG = 1;
    private static final int REQUEST_CODE_PICK_PHOTO = 2;
    private static int PAGER_SIZE = 10;
    private String mUserId;
    private String mUserName;
    //private TextView mTvTitle;
   // private ImageView mIvTitleRight;
   private ImageView iv_title_left;
   private  ImageView fabu;
    private SelectPicPopupWindow menuWindow;
    // 头部
   // private View mHeadView;
    private ImageView ivHeadBg, ivHead;
    // 通知...
    private LinearLayout mTipLl;
    private ImageView mTipIv;
    private TextView mTipTv;
    // 页面
    private SmartRefreshLayout mRefreshLayout;
    private SwipeRecyclerView mListView;
    private PublicMessageRecyclerAdapter mAdapter;
    private List<PublicMessage> mMessages = new ArrayList<>();
    private boolean more;
    private String messageId;
    private FadingScrollView nacRoot;
    View nacLayout;

    RelativeLayout publish;
    LinearLayout publish_ll;
    TextView image;
    TextView voice;
    TextView video;

    // 为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {

        public void onClick(View v) {
            if (menuWindow != null) {
                // 顶部一排按钮复用这个listener, 没有menuWindow,
                menuWindow.dismiss();

            }

            Intent intent = new Intent();
            switch (v.getId()) {
                case R.id.image:
                case R.id.btn_send_picture:
                    // 发表图文，
                    intent.setClass(DiscoverActivity.this, SendShuoshuoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    disPubish();
                    break;
                case R.id.voice:
                case R.id.btn_send_voice:
                    // 发表语音
                    intent.setClass(DiscoverActivity.this, SendAudioActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    disPubish();
                    break;

                case R.id.video:
                case R.id.btn_send_video:
                    // 发表视频
                    intent.setClass(DiscoverActivity.this, SendVideoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    disPubish();
                    break;
                case R.id.btn_send_file:
                    // 发表文件
                    intent.setClass(DiscoverActivity.this, SendFileActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.new_comment:
                    // 最新评论&赞
                    Intent intent2 = new Intent(DiscoverActivity.this, NewZanActivity.class);
                    intent2.putExtra("OpenALL", true);
                    startActivity(intent2);
                    mTipLl.setVisibility(View.GONE);
                    EventBus.getDefault().post(new MessageEventHongdian(0));
                    break;
                default:
                    break;
            }
        }
    };

    public static void start(Activity activity){
        Intent intent = new Intent(activity,DiscoverActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);
        initActionBar();
        Downloader.getInstance().init(MyApplication.getInstance().mAppDir + File.separator + coreManager.getSelf().getUserId()
                + File.separator + Environment.DIRECTORY_MOVIES);// 初始化视频下载目录
        initViews();
        initData();

    }

    public void onDestroy() {
        super.onDestroy();
        // 退出页面时关闭视频和语音，
        JCVideoPlayer.releaseAllVideos();
        if (mAdapter != null) {
            mAdapter.stopVoice();
        }
        EventBus.getDefault().unregister(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        if (coreManager.getConfig().newUi) {
            findViewById(R.id.iv_title_left_i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DiscoverActivity.this.finish();
                }
            });
        } else {
           // findViewById(R.id.iv_title_left_i).setVisibility(View.GONE);
        }
       // mTvTitle = ((TextView) findViewById(R.id.tv_title_center));
       // mTvTitle.setText(getString(R.string.life_circle));
        //mIvTitleRight = (ImageView) findViewById(R.id.iv_title_right);
        iv_title_left =  findViewById(R.id.iv_title_left_i);
        //mIvTitleRight.setImageResource(R.mipmap.xiangji);

        findViewById(R.id.fabu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            /*    menuWindow = new SelectPicPopupWindow(DiscoverActivity.this, itemsOnClick);
                menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                menuWindow.showAsDropDown(v, -(menuWindow.getContentView().getMeasuredWidth() - v.getWidth() / 2 - 40), 0);*/
                publish.setVisibility(View.VISIBLE);
                publish_ll.setVisibility(View.VISIBLE);
                publish_ll.setAnimation(AnimationUtil.moveToViewLocation());
            }
        });
        //iv_title_left.setVisibility(View.VISIBLE);
        iv_title_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void initViews() {
        more = true;
        mUserId = coreManager.getSelf().getUserId();
        mUserName = coreManager.getSelf().getNickName();
        nacRoot=findViewById(R.id.nac_root);
        nacLayout=findViewById(R.id.nac_layout);
        fabu=findViewById(R.id.fabu);
        publish=findViewById(R.id.publish);

        publish_ll=findViewById(R.id.publish_ll);
        image=findViewById(R.id.image);
        voice=findViewById(R.id.voice);
        video=findViewById(R.id.video);
        image.setOnClickListener(itemsOnClick);
        voice.setOnClickListener(itemsOnClick);
        video.setOnClickListener(itemsOnClick);
        nacLayout.setAlpha(0);
        nacRoot.setFadingView(nacLayout);
        nacRoot.setFadingHeightView(findViewById(R.id.cover_img));
        // ---------------------------初始化头部-----------------------
        LayoutInflater inflater = LayoutInflater.from(DiscoverActivity.this);
        mListView = findViewById(R.id.recyclerView);
        mListView.setLayoutManager(new LinearLayoutManager(DiscoverActivity.this));
       // mHeadView = inflater.inflate(R.layout.space_cover_view, mListView, false);
        ivHeadBg = (ImageView) findViewById(R.id.cover_img);
        ivHeadBg.setOnClickListener(v -> {
            if (UiUtils.isNormalClick(v)) {
                changeBackgroundImage();
            }
        });
        ivHead = (ImageView)findViewById(R.id.avatar_img);
        ivHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UiUtils.isNormalClick(v)) {
                    Intent intent = new Intent(DiscoverActivity.this, BasicInfoActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, mUserId);
                    startActivity(intent);
                }
            }
        });
        publish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                disPubish();
            }
        });
        displayAvatar();

        mTipLl = (LinearLayout) findViewById(R.id.tip_ll);
        mTipIv = (ImageView) findViewById(R.id.tip_avatar);
        mTipTv = (TextView) findViewById(R.id.tip_content);
        mTipLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTipLl.setVisibility(View.GONE);
                EventBus.getDefault().post(new MessageEventHongdian(0));

                Intent intent = new Intent(DiscoverActivity.this, NewZanActivity.class);
                intent.putExtra("OpenALL", false); // 是否展示全部还是单条
                startActivity(intent);
            }
        });

        // ---------------------------初始化主视图-----------------------
        mRefreshLayout = findViewById(R.id.refreshLayout);
       // mListView.addHeaderView(mHeadView);

        mRefreshLayout.setOnRefreshListener(refreshLayout -> {
            requestData(true);
        });
        mRefreshLayout.setOnLoadMoreListener(refreshLayout -> {
            requestData(false);
        });

        EventBus.getDefault().register(this);

        findViewById(R.id.btn_send_picture).setOnClickListener(itemsOnClick);
        findViewById(R.id.btn_send_voice).setOnClickListener(itemsOnClick);
        findViewById(R.id.btn_send_video).setOnClickListener(itemsOnClick);
        findViewById(R.id.btn_send_file).setOnClickListener(itemsOnClick);
        findViewById(R.id.new_comment).setOnClickListener(itemsOnClick);
    }

    private void changeBackgroundImage() {
        CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_PHOTO);
    }

    private void updateBackgroundImage(String path) {
        File bg = new File(path);
        if (!bg.exists()) {
            LogUtils.log(path);
            Reporter.unreachable();
            ToastUtil.showToast(DiscoverActivity.this, R.string.image_not_found);
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(this);
        UploadingHelper.upfile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), new File(path), new UploadingHelper.OnUpFileListener() {
            @Override
            public void onSuccess(String url, String filePath) {
                Map<String, String> params = new HashMap<>();
                params.put("access_token", coreManager.getSelfStatus().accessToken);
                params.put("msgBackGroundUrl", url);

                HttpUtils.get().url(coreManager.getConfig().USER_UPDATE)
                        .params(params)
                        .build()
                        .execute(new BaseCallback<Void>(Void.class) {

                            @Override
                            public void onResponse(ObjectResult<Void> result) {
                                DialogHelper.dismissProgressDialog();
                                coreManager.getSelf().setMsgBackGroundUrl(url);
                                UserDao.getInstance().updateMsgBackGroundUrl(coreManager.getSelf().getUserId(), url);
                                if (DiscoverActivity.this == null) {
                                    return;
                                }
                                displayAvatar();
                            }

                            @Override
                            public void onError(Call call, Exception e) {
                                DialogHelper.dismissProgressDialog();
                                if (DiscoverActivity.this == null) {
                                    return;
                                }
                                ToastUtil.showErrorNet(DiscoverActivity.this);
                            }
                        });
            }

            @Override
            public void onFailure(String err, String filePath) {
                DialogHelper.dismissProgressDialog();
                if (DiscoverActivity.this == null) {
                    return;
                }
                ToastUtil.showErrorNet(DiscoverActivity.this);
            }
        });

    }

    private  void  disPubish(){
        publish_ll.setVisibility(View.GONE);
        publish_ll.setAnimation(AnimationUtil.moveToViewBottom());
        publish.setVisibility(View.GONE);
    }
    public void initData() {
        mAdapter = new PublicMessageRecyclerAdapter(DiscoverActivity.this, coreManager, mMessages);
        mListView.setAdapter(mAdapter);
        requestData(true);
    }

    private void requestData(boolean isPullDownToRefresh) {
        if (isPullDownToRefresh) {// 上拉刷新
            updateTip();
            messageId = null;
            more = true;
        }

        if (!more) {
            // ToastUtil.showToast(DiscoverActivity.this, getString(R.string.tip_last_item));
            mRefreshLayout.setNoMoreData(true);
            refreshComplete();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("pageSize", String.valueOf(PAGER_SIZE));
        if (messageId != null) {
            params.put("messageId", messageId);
        }

        HttpUtils.get().url(coreManager.getConfig().MSG_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<PublicMessage>(PublicMessage.class) {
                    @Override
                    public void onResponse(ArrayResult<PublicMessage> result) {
                        if (DiscoverActivity.this != null && Result.checkSuccess(DiscoverActivity.this, result)) {
                            List<PublicMessage> data = result.getData();
                            if (isPullDownToRefresh) {
                                mMessages.clear();
                            }
                            if (data != null && data.size() > 0) {
                                mMessages.addAll(data);
                                // 记录最后一条说说的id
                                messageId = data.get(data.size() - 1).getMessageId();
                                if (data.size() == PAGER_SIZE) {
                                    more = true;
                                    mRefreshLayout.resetNoMoreData();
                                } else {
                                    // 服务器返回未满10条，下拉不在去请求
                                    more = false;
                                }
                            } else {
                                // 服务器未返回数据，下拉不再去请求
                                more = false;
                            }
                            mAdapter.notifyDataSetChanged();
                            refreshComplete();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        if (DiscoverActivity.this != null) {
                            ToastUtil.showNetError(DiscoverActivity.this);
                            refreshComplete();
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_SEND_MSG) {
            // 发布说说成功,刷新Fragment
            String messageId = data.getStringExtra(AppConstant.EXTRA_MSG_ID);
            CircleMessageDao.getInstance().addMessage(mUserId, messageId);
            requestData(true);
        } else if (requestCode == REQUEST_CODE_PICK_PHOTO) {
            if (data != null && data.getData() != null) {
                String path = CameraUtil.getImagePathFromUri(DiscoverActivity.this, data.getData());
                updateBackgroundImage(path);
            } else {
                ToastUtil.showToast(DiscoverActivity.this, R.string.c_photo_album_failed);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventAvatarUploadSuccess message) {
        if (message.event) {// 头像更新了，但该界面没有被销毁，不会去重新加载头像，所以这里更新一下
            displayAvatar();
        }
    }

    public void displayAvatar() {
        // 加载小头像，
        AvatarHelper.getInstance().displayAvatar(mUserId, ivHead, true);
        // 优先加载user信息中的背景图片，失败就加载头像，
        String bg = coreManager.getSelf().getMsgBackGroundUrl();
        if (TextUtils.isEmpty(bg)) {
            realDisplayAvatar();
        }
        Glide.with(DiscoverActivity.this.getApplicationContext())
                .load(bg)
                .placeholder(R.drawable.avatar_normal)
                .dontAnimate()
                .into(new SimpleTarget<GlideDrawable>() {
                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                        ivHeadBg.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        realDisplayAvatar();
                    }
                });

    }

    private void realDisplayAvatar() {
        final String mOriginalUrl = AvatarHelper.getAvatarUrl(mUserId, false);
        if (!TextUtils.isEmpty(mOriginalUrl)) {
            String time = UserAvatarDao.getInstance().getUpdateTime(mUserId);

            Glide.with(MyApplication.getContext())
                    .load(mOriginalUrl)
                    .placeholder(R.drawable.avatar_normal)
                    .signature(new StringSignature(time))
                    .dontAnimate()
                    .error(R.drawable.avatar_normal)
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            ivHeadBg.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            Log.e("zq", "加载原图失败：" + mOriginalUrl);
                        }
                    });
        } else {
            Log.e("zq", "未获取到原图地址");// 基本上不会走这里
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEvent message) {
        if (message.message.equals("prepare")) {// 准备播放视频，关闭语音播放
            mAdapter.stopVoice();
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageEventNotifyDynamic message) {
        // 收到赞 || 评论 || 提醒我看 协议 刷新页面
        requestData(true);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventComment message) {

        Log.e("zx", "helloEventBus: " + message.pbmessage.getIsAllowComment());
        if (message.event.equals("Comment") && message.pbmessage.getIsAllowComment() == 0) {
            TrillCommentInputDialog trillCommentInputDialog = new TrillCommentInputDialog(DiscoverActivity.this, InternationalizationHelper.getString("ENTER_PINLUNT"),
                    str -> {
                        Comment mComment = new Comment();
                        Comment comment = mComment.clone();
                        if (comment == null)
                            comment = new Comment();
                        comment.setBody(str);
                        comment.setUserId(mUserId);
                        comment.setNickName(mUserName);
                        comment.setTime(TimeUtils.sk_time_current_time());
                        addComment(message, comment);
                    });
            Window window = trillCommentInputDialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
                trillCommentInputDialog.show();
            }
        } else {
            Toast.makeText(DiscoverActivity.this, "禁止评论", Toast.LENGTH_SHORT).show();
        }

    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventReply message) {
        if (message.event.equals("Reply")) {
            TrillCommentInputDialog trillCommentInputDialog = new TrillCommentInputDialog(DiscoverActivity.this, InternationalizationHelper.getString("JX_Reply") + "：" + message.comment.getNickName(),
                    str -> {
                        Comment mComment = new Comment();
                        Comment comment = mComment.clone();
                        if (comment == null)
                            comment = new Comment();
                        comment.setToUserId(message.comment.getUserId());
                        comment.setToNickname(message.comment.getNickName());
                        comment.setToBody(message.comment.getToBody());
                        comment.setBody(str);
                        comment.setUserId(mUserId);
                        comment.setNickName(mUserId);
                        comment.setTime(TimeUtils.sk_time_current_time());
                        Reply(message, comment);
                    });
            Window window = trillCommentInputDialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
                trillCommentInputDialog.show();
            }
        }
    }

    /**
     * 停止刷新动画
     */
    private void refreshComplete() {
        mListView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.finishRefresh();
                mRefreshLayout.finishLoadMore();
            }
        }, 200);
    }

    public void updateTip() {
        int tipCount = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());
        if (tipCount == 0) {
            mTipLl.setVisibility(View.GONE);
            EventBus.getDefault().post(new MessageEventHongdian(0));
        } else {
            List<MyZan> zanList = MyZanDao.getInstance().queryZan(coreManager.getSelf().getUserId());
            if (zanList == null || zanList.size() == 0) {
                return;
            }
            MyZan zan = zanList.get(zanList.size() - 1);
            AvatarHelper.getInstance().displayAvatar(zan.getFromUserId(), mTipIv, true);
            mTipTv.setText(tipCount + InternationalizationHelper.getString("JX_PieceNewMessage"));
            mTipLl.setVisibility(View.VISIBLE);
            EventBus.getDefault().post(new MessageEventHongdian(tipCount));
        }
    }

    private void addComment(MessageEventComment message, final Comment comment) {
        String messageId = message.id;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", messageId);
        if (comment.isReplaySomeBody()) {
            params.put("toUserId", comment.getToUserId() + "");
            params.put("toNickname", comment.getToNickname());
            params.put("toBody", comment.getToBody());
        }
        params.put("body", comment.getBody());

        HttpUtils.get().url(coreManager.getConfig().MSG_COMMENT_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        // 评论成功
                        message.pbmessage.setCommnet(message.pbmessage.getCommnet() + 1);
                        if (message.view.getTag() == message.pbmessage) {
                            PublicMessageRecyclerAdapter.CommentAdapter adapter = (PublicMessageRecyclerAdapter.CommentAdapter) message.view.getAdapter();
                            adapter.addComment(comment);
                            mAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(DiscoverActivity.this);
                    }
                });
    }

    /**
     * 回复
     */
    private void Reply(MessageEventReply event, final Comment comment) {
        final int position = event.id;
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
                    public void onResponse(ObjectResult<String> result) {
                        // 评论成功
                        message.setCommnet(message.getCommnet() + 1);
                        PublicMessageRecyclerAdapter.CommentAdapter adapter = (PublicMessageRecyclerAdapter.CommentAdapter) event.view.getAdapter();
                        adapter.addComment(comment);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(DiscoverActivity.this);
                    }
                });
    }

    /**
     * 定位到评论位置
     */
    public void showToCurrent(String mCommentId) {
        int pos = -1;
        for (int i = 0; i < mMessages.size(); i++) {
            if (StringUtils.strEquals(mCommentId, mMessages.get(i).getMessageId())) {
                pos = i + 2;
                break;
            }
        }
        // 如果找到就定位到这条说说
        if (pos != -1) {
            mListView.scrollToPosition(pos);
        }
    }
}
