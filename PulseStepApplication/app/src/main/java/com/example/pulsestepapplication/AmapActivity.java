package com.example.pulsestepapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnMyLocationChangeListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;

import com.amap.api.maps.model.CustomMapStyleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AmapActivity extends AppCompatActivity {

    // 常量
    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 1002;
    private static final float MOVE_ZOOM_LEVEL = 16f;
    private static final float DEFAULT_ZOOM_LEVEL = 15f;
    private static final String TAG = "AmapActivity";

    // UI组件
    private ImageButton btnPauseResume;
    private TextView timerTextView, distanceTextView, stepTextView, avgPaceTextView;
    private ImageButton btnShow;
    private ImageView backButton;
    private ImageView mapImageView;

    // 地图和位置
    private MapView mMapView;
    private AMap aMap;
    private MyLocationStyle myLocationStyle;

    private double initialLatitude;
    private double initialLongitude;

    // 追踪变量
    private final List<Polyline> polyLines = new ArrayList<>();
    private final List<LatLng> pathPoints = new ArrayList<>();
    private boolean isTracking = false;
    private boolean isPaused = false;
    private boolean isFirstStart = true;
    private boolean isLocationReady = false;
    private float totalDistance = 0.0f;
    private int currentStepCount = 0;

    // 计步器
    private StepCounter stepCounter;

    // 计时器变量
    private long startTime = 0L;
    private long pauseTime = 0L;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long elapsedTime;
    private final Runnable timerRunnable = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            long millis = SystemClock.elapsedRealtime() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds %= 60;
            timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            elapsedTime = millis;
            timerHandler.postDelayed(this, 1000);
        }
    };

    private TextView cTextView;
    private ImageView waitView;
    private TextView waitTextView;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAmapPrivacy();
        setContentView(R.layout.activity_amap);

        // 初始化UI组件
        initializeUIComponents();

        // 获取传递的初始经纬度
        Intent intent = getIntent();
        initialLatitude = intent.getDoubleExtra("LATITUDE", 0.0);
        initialLongitude = intent.getDoubleExtra("LONGITUDE", 0.0);

        // 检查和请求定位权限
        checkAndRequestLocationPermission();

        // 设置按钮监听器
        setupButtonListeners();

        // 如果支持，初始化计步器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            initStepCounter();
        }

        // 处理传入的权限
        handleIncomingPermissions();
    }

    /**
     * 初始化UI组件
     */
    private void initializeUIComponents() {
        btnPauseResume = findViewById(R.id.btn_stop);
        btnShow = findViewById(R.id.btn_show);
        timerTextView = findViewById(R.id.timer_text_view);
        stepTextView = findViewById(R.id.step_text_view);
        avgPaceTextView = findViewById(R.id.avg_text_view);
        cTextView = findViewById(R.id.calories_text_view);
        waitView = findViewById(R.id.wait);
        waitTextView = findViewById(R.id.waitText);
        backButton = findViewById(R.id.back_button_running_page);
        mapImageView = findViewById(R.id.default_image_view);
        mMapView = findViewById(R.id.amap_view);

        // 返回按钮点击事件
        backButton.setOnClickListener(v -> navigateToMainActivity());
        btnPauseResume.setClickable(false);
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        waitView.startAnimation(rotateAnimation);
        waitTextView.setVisibility(View.VISIBLE);
    }

    /**
     * 导航回主界面
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(AmapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 初始化计步器并设置监听器
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initStepCounter() {
        stepCounter = new StepCounter(this);
        stepCounter.setStepCounterListener(stepCount -> {
            runOnUiThread(() -> {
                if (stepCount < 10) {
                    stepTextView.setText("--");
                } else {
                    stepTextView.setText(String.valueOf(stepCount));
                }
                currentStepCount = stepCount;
            });
        });
    }

    /**
     * 初始化地图视图并配置地图
     */
    private void initializeMapView() {
        mMapView.onCreate(null);
        aMap = mMapView.getMap();
        configureMap();
    }

    /**
     * 配置地图设置
     */
    private void configureMap() {
        if (aMap == null) return;
        aMap.setMyLocationEnabled(true);
        // 设置地图类型
        aMap.setMapType(AMap.MAP_TYPE_NAVI);
        aMap.setMaxZoomLevel(19.0f);

        // 设置定位样式
        myLocationStyle = new MyLocationStyle();
        myLocationStyle.interval(2000);

        // 设置自定义定位图标
        /*Bitmap originalIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img);
        if (originalIconBitmap != null) {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalIconBitmap, 80, 80, false);
            myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
        }

         */

        // 设置精度圈颜色
        myLocationStyle.strokeColor(0x00000000);
        myLocationStyle.radiusFillColor(0x30000000);

        aMap.setMyLocationStyle(myLocationStyle);

        // 隐藏一些地图特征
        aMap.showBuildings(false);
        aMap.showMapText(false);

        // 应用自定义地图样式
        applyCustomMapStyle();

        // 设置初始相机位置
        setInitialCameraPosition();

        // 设置定位变化监听器
        aMap.setOnMyLocationChangeListener(location -> {
            if (location != null && location.hasAccuracy() && location.getAccuracy() < 20.0) {
                isLocationReady = true;
                waitView.clearAnimation();
                waitView.setVisibility(View.GONE);
                waitTextView.setVisibility(View.GONE);
                btnPauseResume.setClickable(true);
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                aMap.setMyLocationEnabled(true);

                if (isTracking && !isPaused && isLocationReady) {
                    updatePath(currentLatLng);
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(currentLatLng)
                            .zoom(MOVE_ZOOM_LEVEL)
                            .tilt(0)
                            .bearing(0)
                            .build();
                    aMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 200, null);
                }
            }
        });
    }

    /**
     * 设置初始相机位置
     */
    private void setInitialCameraPosition() {
        if (initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(initialLatLng)
                    .zoom(DEFAULT_ZOOM_LEVEL)
                    .tilt(0)
                    .build();
            aMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            // 获取当前位置并设置相机位置
            Location myLocation = aMap.getMyLocation();
            if (myLocation != null) {
                LatLng currentLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM_LEVEL));
            }
        }
    }

    /**
     * 应用自定义地图样式
     */
    private void applyCustomMapStyle() {
        try {
            // 创建 CustomMapStyleOptions 对象
            CustomMapStyleOptions customMapStyleOptions = new CustomMapStyleOptions();

            // 设置样式数据文件路径（位于 assets 目录下）
            customMapStyleOptions.setStyleDataPath(getAssetsPath("style/style.data"));

            // 如果有额外的纹理文件，设置纹理文件路径
            customMapStyleOptions.setStyleExtraPath(getAssetsPath("style/style_extra.data"));

            // 应用自定义样式选项到地图
            aMap.setCustomMapStyle(customMapStyleOptions);

            // 启用自定义地图样式
            aMap.setMapCustomEnable(true);

            Log.d(TAG, "自定义地图样式已成功应用。");
        } catch (Exception e) {
            Log.e(TAG, "应用自定义地图样式失败", e);
        }
    }

    /**
     * 获取 assets 目录下文件的完整路径
     */
    private String getAssetsPath(String fileName) {
        return "file:///android_asset/" + fileName;
    }

    /**
     * 设置按钮监听器
     */
    private void setupButtonListeners() {
        btnPauseResume.setOnClickListener(v -> handlePauseResumeButtonClick());
        btnShow.setOnClickListener(v -> showLastTrack());
    }

    /**
     * 处理暂停/继续按钮点击事件
     */
    private void handlePauseResumeButtonClick() {
        if (!isLocationReady) {
            Toast.makeText(this, "定位中...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFirstStart) {
            if (checkPermissionsForTracking()) {
                startTracking();
                isFirstStart = false;
            }
        } else if (isPaused) {
            resumeTracking();
        } else {
            pauseTracking();
        }
    }

    /**
     * 检查追踪所需的权限
     *
     * @return 如果所有必要的权限都被授予，则返回true
     */
    private boolean checkPermissionsForTracking() {
        boolean locationGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean stepCounterGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounterGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !stepCounterGranted) {
            Toast.makeText(this, "请授予计步器权限以启用步数追踪", Toast.LENGTH_SHORT).show();
        }
        if (!locationGranted) {
            Toast.makeText(this, "请授予定位权限以启用地图功能", Toast.LENGTH_SHORT).show();
            checkAndRequestLocationPermission(); // 请求定位权限
        }
        return stepCounterGranted && locationGranted;
    }

    /**
     * 检查并请求定位权限
     */
    private void checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 请求定位权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            // 已经拥有定位权限，初始化地图
            initializeMapView();
        }
    }

    /**
     * 开始追踪，包括位置更新和计步
     */
    @SuppressLint({"MissingPermission", "UseCompatLoadingForDrawables"})
    private void startTracking() {
        if (aMap != null) {
            aMap.setMyLocationEnabled(true);
        }
        isTracking = true;
        isPaused = false;
        pathPoints.clear();
        totalDistance = 0.0f;
        startTime = SystemClock.elapsedRealtime();
        timerHandler.postDelayed(timerRunnable, 0);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        btnShow.setVisibility(View.GONE);

        // 开始计步
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.startStepTracking();
        }

        // 聚焦到当前位置
        Location myLocation = aMap.getMyLocation();
        if (myLocation != null) {
            LatLng currentLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, MOVE_ZOOM_LEVEL));
        }
    }

    /**
     * 恢复追踪
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void resumeTracking() {
        isPaused = false;
        startTime += (SystemClock.elapsedRealtime() - pauseTime);
        timerHandler.postDelayed(timerRunnable, 0);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        btnShow.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.startStepTracking();
        }
        pathPoints.clear();
    }

    /**
     * 暂停追踪
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void pauseTracking() {
        isPaused = true;
        pauseTime = SystemClock.elapsedRealtime();
        timerHandler.removeCallbacks(timerRunnable);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.start));
        btnShow.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.stopStepTracking();
        }
        drawCurrentPolyline();
    }

    /**
     * 更新路径和计算总距离
     *
     * @param latLng 新的位置坐标
     */
    @SuppressLint("DefaultLocale")
    private void updatePath(LatLng latLng) {
        if (pathPoints.isEmpty()) {
            pathPoints.add(latLng);
            return;
        }

        LatLng lastLatLng = pathPoints.get(pathPoints.size() - 1);
        float[] results = new float[1];
        Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, latLng.latitude, latLng.longitude, results);

        if (results[0] > 1.0 || currentStepCount > 0) {
            totalDistance += results[0];
            double totalDistanceKm = totalDistance / 1000.0;
            double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);

            if (totalDistanceKm > 0 && totalTimeMinutes > 0) {
                double avgPace = totalTimeMinutes / totalDistanceKm;
                if (avgPace >= 1.0 && avgPace <= 30.0) {
                    avgPaceTextView.setText(String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60)));
                } else {
                    avgPaceTextView.setText("--'--\"");
                }
            } else {
                avgPaceTextView.setText("--'--\"");
            }

            pathPoints.add(latLng);
            drawCurrentPolyline();
        }
    }

    /**
     * 绘制当前的折线
     */
    private void drawCurrentPolyline() {
        if (!pathPoints.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(pathPoints)
                    .color(getResources().getColor(R.color.like_orange))
                    .width(30);
            if (polyLines.isEmpty() || isPaused) {
                Polyline polyline = aMap.addPolyline(polylineOptions);
                polyLines.add(polyline);
            } else {
                polyLines.get(polyLines.size() - 1).remove();
                Polyline polyline = aMap.addPolyline(polylineOptions);
                polyLines.set(polyLines.size() - 1, polyline);
            }
        }
    }

    /**
     * 显示最后的轨迹并跳转到总结页面
     */
    private void showLastTrack() {
        float distanceInKm = totalDistance / 1000;
        String timeElapsed = timerTextView.getText().toString();
        int stepCount = currentStepCount;

        String avg = "--'--\"";
        if (distanceInKm > 0) {
            double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);
            double avgPace = totalTimeMinutes / distanceInKm;

            if (avgPace >= 1.0 && avgPace <= 30.0) {
                avg = String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60));
            } else {
                Log.d("DEBUG", "Abnormal pace detected: " + avgPace + " min/km, ignoring this point.");
            }
        } else {
            Log.d("DEBUG", "Invalid distance detected, setting average pace to default '--'");
        }

        // 收集轨迹点
        ArrayList<LatLng> trajectory = new ArrayList<>();
        for (Polyline polyline : polyLines) {
            if (!trajectory.isEmpty()) {
                trajectory.add(null);
            }
            trajectory.addAll(polyline.getPoints());
        }

        // 跳转到总结页面
        Intent intent = new Intent(AmapActivity.this, AmapRunSummaryActivity.class);
        intent.putExtra("distance", distanceInKm);
        intent.putExtra("avgPace", avg);
        intent.putExtra("time", timeElapsed);
        intent.putExtra("stepCount", stepCount);
        intent.putParcelableArrayListExtra("trajectory", trajectory);

        startActivity(intent);
        finish();
    }

    /**
     * 处理传入的权限
     */
    private void handleIncomingPermissions() {
        boolean activityRecognitionGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityRecognitionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !activityRecognitionGranted) {
            checkAndRequestStepCounterPermission();
        }
    }

    /**
     * 检查并请求计步器权限
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void checkAndRequestStepCounterPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_REQUEST_CODE);
        } else {
            onStepCounterPermissionGranted();
        }
    }

    /**
     * 计步器权限被授予后的操作
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void onStepCounterPermissionGranted() {
        if (stepCounter != null) {
            stepCounter.startStepTracking();
        }
    }

    /**
     * 显示默认地图
     */
    private void showDefaultMap() {
        Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_workout);
        if (defaultBitmap != null) {
            mapImageView.setImageBitmap(defaultBitmap);
            mapImageView.setVisibility(View.VISIBLE);
        }
        stepTextView.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.VISIBLE);
    }

    /**
     * 设置高德地图隐私
     */
    private void setupAmapPrivacy() {
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
    }

    /**
     * 保存实例状态
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null) {
            mMapView.onSaveInstanceState(outState);
        }
        outState.putBoolean("isTracking", isTracking);
        outState.putBoolean("isPaused", isPaused);
        outState.putLong("startTime", startTime);
        outState.putLong("pauseTime", pauseTime);
    }

    /**
     * 恢复实例状态
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mMapView != null) {
            mMapView.onCreate(savedInstanceState);
        }
        isTracking = savedInstanceState.getBoolean("isTracking", false);
        isPaused = savedInstanceState.getBoolean("isPaused", false);
        startTime = savedInstanceState.getLong("startTime", 0L);
        pauseTime = savedInstanceState.getLong("pauseTime", 0L);

        if (isTracking && !isPaused) {
            startTracking();
        }
    }

    /**
     * 在活动开始时恢复计步器监听器
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (isTracking && !isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
            stepCounter.registerListener();
        }
    }

    /**
     * 处理活动恢复时的追踪状态
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
        }

        if (isTracking && !isPaused) {
            startTracking();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
                stepCounter.registerListener();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
                stepCounter.unregisterListener();
            }
        }

        btnShow.setVisibility(View.VISIBLE);
    }

    /**
     * 在活动停止时移除位置更新并注销计步器
     */
    @Override
    protected void onStop() {
        super.onStop();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
            stepCounter.unregisterListener();
        }

        if (isTracking) {
            pauseTracking();
        }
    }

    /**
     * 暂停地图视图
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mMapView != null) {
            mMapView.onPause();
        }
    }

    /**
     * 销毁地图视图并清理资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        timerHandler.removeCallbacks(timerRunnable);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
            stepCounter.unregisterListener();
        }
    }

    /**
     * 处理权限请求结果
     */
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStepCounterPermissionGranted();
            } else {
                Log.e(TAG, "计步器权限被拒绝");
            }
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 定位权限已授予，初始化地图
                initializeMapView();
            } else {
                Log.e(TAG, "定位权限被拒绝");
                Toast.makeText(this, "定位权限被拒绝，无法使用地图功能", Toast.LENGTH_SHORT).show();
                showDefaultMap();
            }
        }
    }

    /**
     * 处理返回按钮按下事件
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
