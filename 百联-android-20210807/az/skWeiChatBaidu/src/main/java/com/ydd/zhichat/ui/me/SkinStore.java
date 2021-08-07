package com.ydd.zhichat.ui.me;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ydd.zhichat.R;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.ui.MainActivity;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.CommonAdapter;
import com.ydd.zhichat.util.CommonViewHolder;
import com.ydd.zhichat.util.SkinUtils;

import java.util.List;

/**
 * Created by zq on 2017/8/26 0026.
 * <p>
 * 更换皮肤
 */
public class SkinStore extends BaseActivity {
    private ListView mListView;
    private SkinAdapter skinAdapter;
    private List<SkinUtils.Skin> skins;
    private SkinUtils.Skin currentSkin;

    private boolean isClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_switch_language);
        isClick = false;
        initView();

    }

    protected void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JXTheme_switch"));

        // 当前皮肤
        currentSkin = SkinUtils.getSkin(this);
        // 初始化皮肤
        skins = SkinUtils.defaultSkins;
        initUI();
    }

    void initUI() {
        mListView = (ListView) findViewById(R.id.lg_lv);
        skinAdapter = new SkinAdapter(this, skins);
        mListView.setAdapter(skinAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isClick) {
                    return;
                }

                isClick = true;
                // 这里重复点击会有问题
                currentSkin = skins.get(position);
                SkinUtils.setSkin(SkinStore.this, currentSkin);
                skinAdapter.notifyDataSetInvalidated();

                Toast.makeText(SkinStore.this, getString(R.string.tip_change_skin_success), Toast.LENGTH_SHORT).show();
                MainActivity.isInitView = true;
                Intent intent = new Intent(SkinStore.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    class SkinAdapter extends CommonAdapter<SkinUtils.Skin> {
        SkinAdapter(Context context, List<SkinUtils.Skin> data) {
            super(context, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CommonViewHolder viewHolder = CommonViewHolder.get(mContext, convertView, parent,
                    R.layout.item_switch_skin, position);
            AppCompatTextView skinName = viewHolder.getView(R.id.skin_name);
            AppCompatImageView skinIv = viewHolder.getView(R.id.skin_iv);
            ImageView skinCheck = viewHolder.getView(R.id.check);
            SkinUtils.Skin skin = skins.get(position);
            skinName.setText(skin.getColorName());
            ViewCompat.setBackgroundTintList(skinName, ColorStateList.valueOf(skin.getPrimaryColor()));
            if (skin.isLight()) {
                skinName.setTextColor(skinName.getContext().getResources().getColor(R.color.black));
            }
            ImageViewCompat.setImageTintList(skinIv, ColorStateList.valueOf(skin.getAccentColor()));
            if (currentSkin == skin) {
                skinCheck.setVisibility(View.VISIBLE);
            } else {
                skinCheck.setVisibility(View.GONE);
            }
            return viewHolder.getConvertView();
        }
    }
}
