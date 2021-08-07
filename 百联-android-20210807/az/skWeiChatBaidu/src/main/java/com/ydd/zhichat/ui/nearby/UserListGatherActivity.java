package com.ydd.zhichat.ui.nearby;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.MyFragmentManager;

public class UserListGatherActivity extends BaseActivity {
    private MyFragmentManager mMyFragmentManager;
    private UserListGatherFragment userListGatherFragment;
    private TextView noDataTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list_gather);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        noDataTV = (TextView) findViewById(R.id.noDataTV);
        tvTitle.setText(InternationalizationHelper.getString("JX_Seach"));
        mMyFragmentManager = new MyFragmentManager(this, R.id.fl_fragments);
        userListGatherFragment = new UserListGatherFragment();
        mMyFragmentManager.add(userListGatherFragment);
        mMyFragmentManager.show(0);
        userListGatherFragment.setActivity(this);
    }

    public void showNoData(boolean b){
        if(b) {
            noDataTV.setVisibility(View.VISIBLE);
        }else {
            noDataTV.setVisibility(View.GONE);
        }
    }
}
