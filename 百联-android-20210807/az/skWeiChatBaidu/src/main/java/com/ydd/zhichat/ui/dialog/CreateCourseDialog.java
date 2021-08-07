package com.ydd.zhichat.ui.dialog;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.ui.dialog.base.BaseDialog;
import com.ydd.zhichat.util.ToastUtil;

/**
 * Created by Administrator on 2016/4/21.
 * 创建课程的提示框
 */
public class CreateCourseDialog extends BaseDialog {
    private TextView mTitleTv;
    private EditText mContentEt;
    private Button mCommitBtn;

    private CoureseDialogConfirmListener mOnClickListener;

    {
        RID = R.layout.dialog_single_input;
    }

    public CreateCourseDialog(Activity activity, CoureseDialogConfirmListener listener) {
        mActivity = activity;
        initView();
        mOnClickListener = listener;
    }

    protected void initView() {
        super.initView();
        mTitleTv = (TextView) mView.findViewById(R.id.title);
        mContentEt = mView.findViewById(R.id.content);
        mView.findViewById(R.id.public_rl).setVisibility(View.GONE);

        mCommitBtn = (Button) mView.findViewById(R.id.sure_btn);
//        mCommitBtn.setBackgroundColor(SkinUtils.getSkin(mActivity).getAccentColor());
        mCommitBtn.setText(InternationalizationHelper.getString("JX_Confirm"));


        mCommitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnClickListener != null) {
                    String content = mContentEt.getText().toString().trim();
                    if (TextUtils.isEmpty(content)) {
                        ToastUtil.showToast(mActivity, mActivity.getString(R.string.name_course_error));
                    } else {
                        CreateCourseDialog.this.mDialog.dismiss();
                        mOnClickListener.onClick(content);
                    }
                }
            }
        });

        mTitleTv.setText(InternationalizationHelper.getString("JX_CourseName"));
        mContentEt.setHint(InternationalizationHelper.getString("JX_InputCourseName"));
    }

    @Override
    public BaseDialog show() {
        mContentEt.setFocusable(true);
        mContentEt.setFocusableInTouchMode(true);
        return super.show();
    }

    public void setTitle(String title) {
        mTitleTv.setText(title);
    }

    public void setHint(String hint) {
        mContentEt.setHint(hint);
    }

    public void setMaxLines(int maxLines) {
        mContentEt.setMaxLines(maxLines);
    }

    public String getContent() {
        return mContentEt.getText().toString();
    }

    // 外面需要对两个EditText做操作，给获取方法
    public EditText getE1() {
        return mContentEt;
    }

    public interface CoureseDialogConfirmListener {
        void onClick(String content);
    }
}
