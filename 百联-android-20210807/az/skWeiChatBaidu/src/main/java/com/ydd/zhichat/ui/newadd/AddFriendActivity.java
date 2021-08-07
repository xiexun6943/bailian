package com.ydd.zhichat.ui.newadd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.User;
import com.ydd.zhichat.pay.PaymentActivity;
import com.ydd.zhichat.ui.MainActivity;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.base.CoreManager;
import com.ydd.zhichat.ui.contacts.BlackActivity;
import com.ydd.zhichat.ui.groupchat.FaceToFaceGroup;
import com.ydd.zhichat.ui.groupchat.SelectContactsActivity;
import com.ydd.zhichat.ui.nearby.UserSearchActivity;
import com.ydd.zhichat.ui.other.QRcodeActivity;
import com.ydd.zhichat.view.MergerStatus;
import com.ydd.zhichat.view.SkinImageView;
import com.ydd.zhichat.view.SkinTextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AddFriendActivity extends BaseActivity {

    @BindView(R.id.iv_title_left)
    SkinImageView ivTitleLeft;
    @BindView(R.id.tv_title_left)
    SkinTextView tvTitleLeft;
    @BindView(R.id.tv_title_center)
    SkinTextView tvTitleCenter;
    @BindView(R.id.pb_title_center)
    ProgressBar pbTitleCenter;
    @BindView(R.id.iv_title_right)
    SkinImageView ivTitleRight;
    @BindView(R.id.iv_title_right_right)
    SkinImageView ivTitleRightRight;
    @BindView(R.id.tv_title_right)
    SkinTextView tvTitleRight;
    @BindView(R.id.rel_top)
    RelativeLayout relTop;
    @BindView(R.id.mergerStatus)
    MergerStatus mergerStatus;
    @BindView(R.id.search)
    TextView search;
    @BindView(R.id.my_id)
    TextView myId;
    @BindView(R.id.create_group)
    LinearLayout createGroup;
    @BindView(R.id.receipt_payment)
    LinearLayout receiptPayment;
    @BindView(R.id.scanning)
    LinearLayout scanning;
    // 依赖activity里的coreManager，不单独bind服务，防止多次unbind,
    User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
         mUser = coreManager.getSelf();
        tvTitleCenter.setText("添加朋友");
        myId.setText("讯聊号:"+mUser.getUserId()+"");
    }

    @OnClick({R.id.iv_title_left, R.id.search, R.id.my_id, R.id.create_group, R.id.receipt_payment, R.id.scanning,R.id.qcode})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_title_left:
                this.finish();
                break;
            case R.id.search:
                startActivity(new Intent(this, UserSearchActivity.class));
                break;
            case R.id.qcode:

                Intent intent2 = new Intent(this, QRcodeActivity.class);
                intent2.putExtra("isgroup", false);
                if (!TextUtils.isEmpty(mUser.getAccount())) {
                    intent2.putExtra("userid", mUser.getAccount());
                } else {
                    intent2.putExtra("userid", mUser.getUserId());
                }
                intent2.putExtra("userAvatar", mUser.getUserId());
                intent2.putExtra("userName", mUser.getNickName());
                startActivity(intent2);
                break;
            case R.id.create_group:
                startActivity(new Intent(this, FaceToFaceGroup.class));
                break;
            case R.id.receipt_payment:
                Intent intentBlack1 = new Intent(this, BlackActivity.class);
                startActivity(intentBlack1);
                break;
            case R.id.scanning:
                MainActivity.requestQrCodeScan(this);
                break;
        }
    }
}
