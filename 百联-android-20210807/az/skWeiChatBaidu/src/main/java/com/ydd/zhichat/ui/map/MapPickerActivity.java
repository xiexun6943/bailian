package com.ydd.zhichat.ui.map;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.BuildConfig;
import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.map.MapHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.mucfile.XfileUtils;
import com.ydd.zhichat.util.FileUtil;
import com.ydd.zhichat.util.ToastUtil;

import java.util.List;

/**
 * Created by Administrator on 2017/7/20.
 */

public class MapPickerActivity extends BaseActivity {
    private ImageView ivReturn;
    private RelativeLayout rlInfoView;
    private TextView tvName;
    private TextView tvDateils;
    private Button mSendLocation;

    private int infoViewHeight;
    private TranslateAnimation translateUp;
    private TranslateAnimation translateDown;
    private boolean isShow;

    private MapHelper mapHelper;
    private MapHelper.Picker picker;
    private MapHelper.LatLng beginLatLng;
    private MapHelper.LatLng currentLatLng;
    private Animation.AnimationListener animationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            rlInfoView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (isShow) {
                rlInfoView.setVisibility(View.VISIBLE);
            } else {
                rlInfoView.setVisibility(View.GONE);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            com.ydd.zhichat.util.LogUtils.log("onCreate");
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);
        initActionBar();
        initView();
        if (BuildConfig.DEBUG) {
            com.ydd.zhichat.util.LogUtils.log("after create");
        }
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
        tvTitle.setText(InternationalizationHelper.getString("JXUserInfoVC_Loation"));
    }

    public void initView() {
        // 跳回自己位置，
        ivReturn = findViewById(R.id.iv_location);
        // 地图没准备好，不能跳回去，
        ivReturn.setVisibility(View.GONE);
        rlInfoView = (RelativeLayout) findViewById(R.id.map_picker_info);
        tvName = (TextView) findViewById(R.id.map_name_tv);
        tvDateils = (TextView) findViewById(R.id.map_dateils_tv);
        mSendLocation = (Button) findViewById(R.id.map_send_data);

        ivReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLatLng = beginLatLng;
                picker.moveMap(beginLatLng);
            }
        });

        mSendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View mapView = picker.getMapView();
                int dw = mapView.getWidth();
                int dh = mapView.getHeight();
                // 截取宽度一半，
                int width = dw / 2;
                // 图片宽高比要和视图一样，
                int height = (int) (width * 1f / 672 * 221);
                // 以防万一，等比例缩小至全屏，
                float scale = Math.max(1, Math.min(width * 1.0f / dw, height * 1.0f / dh));
                final int rw = (int) (width / scale);
                final int rh = (int) (height / scale);

                int left = (dw - rw) / 2;
                int right = (dw + rw) / 2;
                int top = (dh - rh) / 2;
                int bottom = (dh + rh) / 2;
                Rect rect = new Rect(left, top, right, bottom);
                picker.snapshot(rect, new MapHelper.SnapshotReadyCallback() {
                    @Override
                    public void onSnapshotReady(Bitmap bitmap) {
                        // 部分截图保存本地，
                        String snapshot = FileUtil.saveBitmap(bitmap);
                        String address = tvDateils.getText().toString();
                        if (TextUtils.isEmpty(address)) {
                            address = MyApplication.getInstance().getBdLocationHelper().getAddress();
                        }
                        Intent intent = new Intent();
                        intent.putExtra(AppConstant.EXTRA_LATITUDE, currentLatLng.getLatitude());
                        intent.putExtra(AppConstant.EXTRA_LONGITUDE, currentLatLng.getLongitude());
                        intent.putExtra(AppConstant.EXTRA_ADDRESS, address);
                        intent.putExtra(AppConstant.EXTRA_SNAPSHOT, snapshot);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
            }
        });
        // 地图没准备好，不能发送，
        mSendLocation.setVisibility(View.GONE);
        initMap();
    }

    private void initAnim() {
        infoViewHeight = XfileUtils.measureViewHeight(rlInfoView);

        translateUp = new TranslateAnimation(0, 0, infoViewHeight, 0);
        translateUp.setFillAfter(true);
        translateUp.setDuration(400);
        translateUp.setAnimationListener(animationListener);

        translateDown = new TranslateAnimation(0, 0, 0, infoViewHeight);
        translateDown.setFillAfter(true);
        translateDown.setDuration(400);
        translateDown.setAnimationListener(animationListener);
    }

    private void initMap() {
        mapHelper = MapHelper.getInstance();
        picker = mapHelper.getPicker(this);
        getLifecycle().addObserver(picker);
        FrameLayout container = findViewById(R.id.map_view_container);
        picker.attack(container, new MapHelper.OnMapReadyListener() {
            @Override
            public void onMapReady() {
                // 初始化底部周边相关动画，
                initAnim();
                // 中心打上图标，
                picker.addCenterMarker(R.drawable.ic_position, "pos");
                mapHelper.requestLatLng(new MapHelper.OnSuccessListener<MapHelper.LatLng>() {
                    @Override
                    public void onSuccess(MapHelper.LatLng latLng) {
                        // 记录开始时定位的位置，用来点击按钮跳回来，
                        beginLatLng = latLng;
                        picker.moveMap(latLng);
                        // 加载周边位置信息，
                        // 记录当前位置也在这个方法里，
                        loadMapDatas(latLng);
                    }
                }, new MapHelper.OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        ToastUtil.showToast(MapPickerActivity.this, getString(R.string.tip_auto_location_failed) + t.getMessage());
                        // 总有个默认的经纬度，拿出来，
                        beginLatLng = picker.currentLatLng();
                        picker.moveMap(beginLatLng);
                        loadMapDatas(beginLatLng);

                    }
                });
            }
        });
        picker.setOnMapStatusChangeListener(new MapHelper.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapHelper.MapStatus mapStatus) {
                if (isShow) {
                    isShow = false;
                    rlInfoView.startAnimation(translateDown);
                }
            }

            @Override
            public void onMapStatusChange(MapHelper.MapStatus mapStatus) {
            }

            @Override
            public void onMapStatusChangeFinish(MapHelper.MapStatus mapStatus) {
                loadMapDatas(mapStatus.target);
            }
        });
    }

    private void loadMapDatas(MapHelper.LatLng latLng) {
        currentLatLng = latLng;
        // 到这里就是地图准备好了，可以发送了，
        ivReturn.setVisibility(View.VISIBLE);
        mSendLocation.setVisibility(View.VISIBLE);
        mapHelper.requestPlaceList(latLng, new MapHelper.OnSuccessListener<List<MapHelper.Place>>() {
            @Override
            public void onSuccess(List<MapHelper.Place> places) {
                MapHelper.Place place = places.get(0);
                tvName.setText(place.getName());
                tvDateils.setText(place.getAddress());
                isShow = true;
                rlInfoView.startAnimation(translateUp);
            }
        }, new MapHelper.OnErrorListener() {
            @Override
            public void onError(Throwable t) {
                ToastUtil.showToast(MapPickerActivity.this, getString(R.string.tip_places_around_failed) + t.getMessage());
            }
        });
    }
}
