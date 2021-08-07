package com.ydd.zhichat.ui.nearby;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.helper.UsernameHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.tool.SelectConstantActivity;
import com.ydd.zhichat.ui.tool.SelectDateActivity;

/**
 * 添加好友
 */
public class UserSearchActivity extends BaseActivity implements View.OnClickListener {
    private int mSex;
    private int mMinAge;
    private int mMaxAge;
    private int mShowTime;
    private EditText mKeyWordEdit;
    private TextView mSexTv;
    private EditText mMinAgeEdit;
    private EditText mMaxAgeEdit;
    private TextView mShowTimeTv;
    private TextView mKeyWordText, mmSex, mMinText, mMaxText, mmShowTime;
    private Button mSearchBtn;
    private boolean isPublicNumber;

    public static void start(Context ctx, boolean isPublicNumber) {
        Intent intent = new Intent(ctx, UserSearchActivity.class);
        intent.putExtra("isPublicNumber", isPublicNumber);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);
        isPublicNumber = getIntent().getBooleanExtra("isPublicNumber", false);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        if (isPublicNumber) {
            tvTitle.setText(R.string.search_public_number);
        } else {
            tvTitle.setText(InternationalizationHelper.getString("JXNearVC_AddFriends"));
        }
        initView();
    }

    private void initView() {
        mKeyWordEdit = (EditText) findViewById(R.id.keyword_edit);
        // 获取焦点，键盘弹出
        mKeyWordEdit.requestFocus();
        mSexTv = (TextView) findViewById(R.id.sex_tv);
        mMinAgeEdit = (EditText) findViewById(R.id.min_age_edit);
        mMaxAgeEdit = (EditText) findViewById(R.id.max_age_edit);
        mShowTimeTv = (TextView) findViewById(R.id.show_time_tv);

        mKeyWordText = (TextView) findViewById(R.id.keyword_text);
        mmSex = (TextView) findViewById(R.id.sex_text);
        mMinText = (TextView) findViewById(R.id.min_age_text);
        mMaxText = (TextView) findViewById(R.id.max_age_text);
        mmShowTime = (TextView) findViewById(R.id.show_time_text);
        mSearchBtn = (Button) findViewById(R.id.search_btn);
//        mSearchBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        if (isPublicNumber) {
            mKeyWordText.setText(R.string.tip_search_public_number);
            mKeyWordEdit.setHint(R.string.hint_search_public_number);
        } else {
            UsernameHelper.initSearchLabel(mKeyWordText, coreManager.getConfig());
            UsernameHelper.initSearchEdit(mKeyWordEdit, coreManager.getConfig());
        }
        mmSex.setText(InternationalizationHelper.getString("JX_Sex"));
        mMinText.setText(InternationalizationHelper.getString("JXSearchUserVC_MinAge"));
        mMaxText.setText(InternationalizationHelper.getString("JXSearchUserVC_MaxAge"));
        mmShowTime.setText(InternationalizationHelper.getString("JXSearchUserVC_AppearTime"));
        mSearchBtn.setText(InternationalizationHelper.getString("JX_Seach"));

        findViewById(R.id.sex_rl).setOnClickListener(this);
        findViewById(R.id.show_time_rl).setOnClickListener(this);
        mSearchBtn.setOnClickListener(this);
        reset();
    }

    private void reset() {
        mSex = 0;
        mMinAge = 0;
        mMaxAge = 200;
        mShowTime = 0;
        mKeyWordEdit.setText(null);
        //mSexTv.setText(R.string.all);
        mSexTv.setText(InternationalizationHelper.getString("JXSearchUserVC_All"));
        mMinAgeEdit.setText(String.valueOf(mMinAge));
        mMaxAgeEdit.setText(String.valueOf(mMaxAge));
        //mShowTimeTv.setText(R.string.all_date);
        mShowTimeTv.setText(InternationalizationHelper.getString("JXSearchUserVC_AllDate"));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sex_rl:// 点击性别
                showSelectSexDialog();
                break;
            case R.id.show_time_rl:
                startActivityForResult(new Intent(mContext, SelectDateActivity.class), 1);
                break;
            case R.id.search_btn: {
                if (TextUtils.isEmpty(mKeyWordEdit.getText().toString())) {
                    return;
                }
                mSex = 0;
                mMinAge = 0;
                mMaxAge = 200;
                Intent intent = new Intent(mContext, UserListGatherActivity.class);
                intent.putExtra("key_word", mKeyWordEdit.getText().toString());
                intent.putExtra("sex", mSex);
                intent.putExtra("min_age", mMinAge);
                intent.putExtra("max_age", mMaxAge);
                intent.putExtra("show_time", mShowTime);
                startActivity(intent);
            }
            break;
        }
    }

    private void showSelectSexDialog() {
        // 1是男，0是女，2是全部
        String[] sexs = new String[]{InternationalizationHelper.getString("JXSearchUserVC_All"), InternationalizationHelper.getString("JX_Man"), InternationalizationHelper.getString("JX_Wuman")};
        int checkItem = 0;
        if (mSex == 2) {
            checkItem = 0;
        } else if (mSex == 1) {
            mSex = 1;
        } else if (mSex == 0) {
            mSex = 2;
        }
        new AlertDialog.Builder(this).setTitle(InternationalizationHelper.getString("GENDER_SELECTION"))
                .setSingleChoiceItems(sexs, checkItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            mSex = 2;
                            mSexTv.setText(InternationalizationHelper.getString("JXSearchUserVC_All"));
                        } else if (which == 1) {
                            mSex = 1;
                            mSexTv.setText(InternationalizationHelper.getString("JX_Man"));
                        } else {
                            mSex = 0;
                            mSexTv.setText(InternationalizationHelper.getString("JX_Wuman"));
                        }
                        dialog.dismiss();
                    }
                }).setCancelable(true).create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // 日期选择
            if (resultCode == RESULT_OK && data != null) {
                int id = data.getIntExtra(SelectConstantActivity.EXTRA_CONSTANT_ID, 0);
                String name = data.getStringExtra(SelectConstantActivity.EXTRA_CONSTANT_NAME);
                mShowTime = id;
                mShowTimeTv.setText(name);
            }
        }
    }
}
