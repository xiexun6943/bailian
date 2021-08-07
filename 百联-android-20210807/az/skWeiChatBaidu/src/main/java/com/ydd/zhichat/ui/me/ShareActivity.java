package com.ydd.zhichat.ui.me;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ydd.zhichat.R;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.view.MergerStatus;
import com.ydd.zhichat.view.SkinImageView;
import com.ydd.zhichat.view.SkinTextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ShareActivity extends BaseActivity {


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

    @BindView(R.id.save)
    TextView save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        tvTitleCenter.setText("分享好友");
    }

    @OnClick({R.id.iv_title_left, R.id.save})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_title_left:
                this.finish();
                break;
            case R.id.save:
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.new_sharecare, null);
                saveImageToGallery(this, bitmap);
                Toast.makeText(this, "保存图片到本地", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void saveImageToGallery(Context context, Bitmap bmp) {
        // 首先保存图片
        File appDir = new File(Environment.getExternalStorageDirectory(), "Boohee");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(file.getAbsolutePath())));
        Toast.makeText(ShareActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
    }

}
