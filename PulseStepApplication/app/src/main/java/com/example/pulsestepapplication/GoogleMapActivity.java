package com.example.pulsestepapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.gms.location.LocationCallback;

import com.google.android.gms.location.LocationRequest;

import com.google.android.gms.location.LocationServices;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;

import com.google.android.gms.maps.CameraUpdateFactory;

import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.CameraPosition;

import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.MapStyleOptions;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.Locale;

public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    // 常量
    private static final String TAG = "GoogleMapActivity";
    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 1002;
    private static final int BACKGROUND_LOCATION_REQUEST_CODE = 1003; // 唯一请求码
    private static final float MOVE_ZOOM_LEVEL = 17f;
    private static final float DEFAULT_ZOOM_LEVEL = 15f;
    private static final float MAX_ZOOM_LEVEL = 19f;
    private static final float DISTANCE_THRESHOLD_METERS = 1.0f; // 距离阈值，单位米

    // UI组件
    private ImageButton btnPauseResume;
    private TextView timerTextView, stepTextView, avgPaceTextView;
    private ImageButton btnShow;
    private ImageView backButton;
    private ImageView mapImageView;
    private TextView cTextView;
    private ImageView waitView;
    private TextView waitTextView;

    // 地图和位置
    private GoogleMap googleMap;
    private double initialLatitude;
    private double initialLongitude;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // 追踪变量
    private final List<Polyline> polyLines = new ArrayList<>();
    private final List<LatLng> pathPoints = new ArrayList<>();
    private final List<LatLng> trajectory = new ArrayList<>();
    private boolean isTracking = false;
    private boolean isPaused = false;
    private boolean isFirstStart = true;
    private boolean isLocationReady = false;
    private float totalDistance = 0.0f;
    private int currentStepCount = 0;
    private static final Double realDistance = 0.05;
    private static final double metValue = 8.0;
    private static final int LOCATION_TIMEOUT = 10000; // 定位超时时间，毫秒

    // 定时器变量
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

            // 在无地图模式下更新平均配速
            if (!isMapMode) {
                updateAvgPaceNoMapMode();
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    // 定位超时处理
    private final Handler locationTimeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable locationTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isLocationReady) {
                isLocationReady = true;
                waitView.clearAnimation();
                waitView.setVisibility(View.GONE);
                waitTextView.setVisibility(View.GONE);
                btnPauseResume.setClickable(true);
                if (ActivityCompat.checkSelfPermission(GoogleMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(GoogleMapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                googleMap.setMyLocationEnabled(true);
                Toast.makeText(GoogleMapActivity.this, "定位超时，信号较弱！", Toast.LENGTH_LONG).show();
            }
        }
    };

    // 模式标志：地图模式为true，无地图模式为false
    private boolean isMapMode;
    private boolean isServiceRunning = false;

    private String userName;
    private int userAge;
    private double userWeight;
    private Geocoder geocoder;

    // SharedPreferences相关
    private static final String PREFS_NAME = "LocationPrefs";
    private static final String KEY_HAS_DENIED_BACKGROUND_PERMISSION = "hasDeniedBackgroundPermission";
    private SharedPreferences sharedPreferences;

    // Flags to track permission states
    private boolean hasRequestedBackgroundPermission = false;
    private boolean hasDeniedBackgroundPermission = false;
    private ImageButton btnGrantPermissions;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map);

        // 获取模式
        Intent intent = getIntent();
        isMapMode = intent.getBooleanExtra("MAP_MODE", true); // 默认地图模式
        userName = intent.getStringExtra("name");
        userAge = intent.getIntExtra("age", 25);
        userWeight = intent.getDoubleExtra("weight", 70.0);

        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        hasDeniedBackgroundPermission = sharedPreferences.getBoolean(KEY_HAS_DENIED_BACKGROUND_PERMISSION, false);

        // 初始化UI组件
        initializeUIComponents();
        
        // 初始化“授予权限”按钮
        btnGrantPermissions = findViewById(R.id.btn_grant_permissions);
        setupGrantPermissionsButton();
        // 检查权限
        checkPermissions();
    }

    /**
     * 初始化UI组件，通过ID查找
     */
    private void initializeUIComponents() {
        btnPauseResume = findViewById(R.id.btn_stop);
        btnShow = findViewById(R.id.btn_show);
        timerTextView = findViewById(R.id.timer_text_view);
        stepTextView = findViewById(R.id.step_text_view);
        avgPaceTextView = findViewById(R.id.avg_text_view);
        cTextView = findViewById(R.id.calories_text_view);
        backButton = findViewById(R.id.back_button_running_page);
        mapImageView = findViewById(R.id.default_image_view);
        waitView = findViewById(R.id.wait);
        waitTextView = findViewById(R.id.waitText);
        // 初始时隐藏地图
        View mapFragment = findViewById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.setVisibility(View.GONE);  // 初始隐藏地图
        }
        // 设置返回按钮点击监听
        backButton.setOnClickListener(v -> navigateToMainActivity());

        if (isMapMode) {
            // 开始旋转动画
            Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
            waitView.startAnimation(rotateAnimation);
            waitView.setVisibility(View.VISIBLE);
            waitTextView.setVisibility(View.VISIBLE);
            btnPauseResume.setClickable(false);
        } else {
            waitView.setVisibility(View.GONE);
            waitTextView.setVisibility(View.GONE);
            // 显示默认地图
            if (mapFragment != null) {
                mapFragment.setVisibility(View.VISIBLE);  // 当位置准备好时显示地图
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
            }
            btnPauseResume.setClickable(true);
        }
    }
    /**
     * 设置“授予权限”按钮的点击监听器
     */
    private void setupGrantPermissionsButton() {
        btnGrantPermissions.setOnClickListener(v -> {
            // 打开应用设置页面
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }
    /**
     * 根据权限状态显示或隐藏“授予权限”按钮
     */
    private void updateGrantPermissionsButton() {
        if (isMapMode && hasDeniedBackgroundPermission) {
            btnGrantPermissions.setVisibility(View.VISIBLE);
        } else {
            btnGrantPermissions.setVisibility(View.GONE);
        }
    }

    /**
     * 在权限状态变化后，更新“授予权限”按钮的可见性
     */
    private void handlePermissionChanges() {
        if (isMapMode) {
            // 检查定位权限是否被撤销
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // 权限被撤销，切换到无地图模式或其他处理
                isMapMode = false;
                if (googleMap != null) {
                    googleMap.clear();
                }
                showDefaultMap();
            }
        }
        // 检查活动识别权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                // 权限被撤销，导航回MainActivity
                Toast.makeText(this, "活动识别权限是必需的", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        }

        // 更新“授予权限”按钮的可见性
        updateGrantPermissionsButton();
    }

    /**
     * 导航回MainActivity
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(GoogleMapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 检查并请求必要的权限
     */
    private void checkPermissions() {
        // 检查活动识别权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                // 请求活动识别权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_REQUEST_CODE);
                return;
            }
        }

        if (isMapMode) {
            // 检查定位权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // 请求定位权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                return;
            }
        }

        // 权限已授予，继续设置
        setupActivity();
    }

    /**
     * 设置活动，根据选择的模式
     */
    private void setupActivity() {
        // 如果是地图模式，初始化地图
        if (isMapMode) {
            setupMapFragment();
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            setupLocationCallback();
        } else {
            showDefaultMap();
        }

        // 设置按钮监听
        setupButtonListeners();

        // 启动追踪服务
        startTrackingService();
    }

    /**
     * 设置SupportMapFragment并异步初始化地图
     */
    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "地图片段为空");
        }
    }

    /**
     * 设置按钮的点击监听
     */
    private void setupButtonListeners() {
        btnPauseResume.setOnClickListener(v -> handleStartStopButtonClick());
        btnShow.setOnClickListener(v -> showLastTrack());
    }

    /**
     * 处理开始/暂停按钮的点击事件
     */
    private void handleStartStopButtonClick() {
        if (isTracking) {
            pauseTracking();
        } else {
            if (isMapMode && !isLocationReady) {
                Toast.makeText(this, "正在获取位置，请稍候...", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isMapMode) {
                if (isBackgroundLocationPermissionGranted()) {
                    // 已授予后台定位权限，恢复追踪
                    resumeTracking();
                } else {
                    if (!hasRequestedBackgroundPermission && !hasDeniedBackgroundPermission) {
                        // 尚未请求过后台定位权限，进行请求
                        checkAndRequestBackgroundLocationPermission();
                    } else if (hasDeniedBackgroundPermission) {
                        // 用户已拒绝后台定位权限，显示引导对话框
                       // showPermissionDeniedDialog();
                    }
                    // 即使未授予后台定位权限，仍允许前台追踪
                    resumeTracking();
                }
            } else {
                // 无地图模式，正常恢复追踪
                resumeTracking();
            }
        }
    }

    /**
     * 检查并请求后台定位权限
     */
    private void checkAndRequestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 检查后台定位权限是否已授予
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // 检查是否应展示权限请求说明
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    // 显示权限请求说明
                    showBackgroundPermissionRationale();
                } else {
                    // 直接请求权限
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_REQUEST_CODE);
                }
            }
        }
    }

    /**
     * 显示后台定位权限的解释对话框
     */
    private void showBackgroundPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("后台定位权限")
                .setMessage("为了在应用程序后台运行时继续跟踪您的位置，需要授予后台定位权限。")
                .setPositiveButton("允许", (dialog, which) -> {
                    // 请求后台定位权限
                    ActivityCompat.requestPermissions(GoogleMapActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_REQUEST_CODE);
                })
                .setNegativeButton("拒绝", (dialog, which) -> {
                    // 用户拒绝权限请求，设置标志位
                    hasDeniedBackgroundPermission = true;
                    sharedPreferences.edit().putBoolean(KEY_HAS_DENIED_BACKGROUND_PERMISSION, true).apply();
                    Toast.makeText(GoogleMapActivity.this, "后台定位权限被拒绝，应用将在后台停止跟踪。", Toast.LENGTH_LONG).show();
                })
                .create()
                .show();
    }

    /**
     * 显示权限被拒绝后的对话框，引导用户前往设置手动授予权限
     */
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("后台定位权限被拒绝")
                .setMessage("若要启用后台跟踪，请在应用设置中允许后台定位权限。")
                .setPositiveButton("打开设置", (dialog, which) -> {
                    // 打开应用设置页面
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    Toast.makeText(this, "后台定位权限被拒绝，应用将在后台停止跟踪。", Toast.LENGTH_LONG).show();
                })
                .create()
                .show();
    }
    /**
     * Starts the tracking process.
     */
    @SuppressLint({"MissingPermission", "UseCompatLoadingForDrawables"})
    private void resumeTracking() {
        // 如果已经在追踪且未暂停，则不执行任何操作
        if (isTracking && !isPaused) {
            Log.d(TAG, "resumeTracking() 已经在追踪且未暂停，跳过执行。");
            return;
        }

        isTracking = true;
        isPaused = false;

        if (isFirstStart) {
            pathPoints.clear();
            totalDistance = 0.0f;
            startTime = SystemClock.elapsedRealtime();
            timerHandler.postDelayed(timerRunnable, 0);
            isFirstStart = false;

            if (isMapMode) {
                // 获取最后已知位置并移动相机
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, MOVE_ZOOM_LEVEL));
                    }
                });
            }
        } else {
            // 仅在从暂停状态恢复时调整 startTime
            long pauseDuration = SystemClock.elapsedRealtime() - pauseTime;
            startTime += pauseDuration;
            timerHandler.postDelayed(timerRunnable, 0);
        }

        btnPauseResume.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pause));
        btnShow.setVisibility(View.GONE);

        // 发送广播给服务，恢复步数计数
        Intent resumeIntent = new Intent(LocationTrackingService.ACTION_RESUME_STEP_COUNTING);
        // 传递是否授予后台定位权限
        resumeIntent.putExtra("background_permission_granted", isBackgroundLocationPermissionGranted());
        LocalBroadcastManager.getInstance(this).sendBroadcast(resumeIntent);
    }

    /**
     * 暂停追踪
     */
    private void pauseTracking() {
        isTracking = false;
        isPaused = true;
        pauseTime = SystemClock.elapsedRealtime();
        timerHandler.removeCallbacks(timerRunnable);
        btnPauseResume.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.start));
        btnShow.setVisibility(View.VISIBLE);
        if (isMapMode) {
            drawCurrentPolyline();
        }
        trajectory.add(null);

        // 发送广播给服务，暂停步数计数
        Intent pauseIntent = new Intent(LocationTrackingService.ACTION_PAUSE_STEP_COUNTING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(pauseIntent);
    }

    /**
     * 启动追踪服务
     */
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        isServiceRunning = true;
    }

    /**
     * 停止追踪服务
     */
    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);
        isServiceRunning = false;
    }

    /**
     * 显示默认地图（无地图模式）
     */
    private void showDefaultMap() {
        // 显示默认图片
        mapImageView.setImageResource(R.drawable.bg_workout);
        mapImageView.setVisibility(View.VISIBLE);
        // 隐藏地图片段
        View mapFragment = findViewById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.setVisibility(View.GONE);
        }
    }

    /**
     * 当地图准备好时调用，配置地图设置
     *
     * @param map 准备好的GoogleMap对象
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        applyCustomMapStyle();
        googleMap.setMaxZoomPreference(MAX_ZOOM_LEVEL);
        // 设置初始相机位置为用户的最后已知位置
        Intent intent = getIntent();
        initialLatitude = intent.getDoubleExtra("LATITUDE", 0.0);
        initialLongitude = intent.getDoubleExtra("LONGITUDE", 0.0);
        if (initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(initialLatLng)   // 设置地图中心
                    .zoom(DEFAULT_ZOOM_LEVEL) // 设置缩放级别
                    .tilt(0)                // 设置倾斜角度为0，确保二维视图
                    .build();

            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        if (ActivityCompat.checkSelfPermission(GoogleMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(false);
        googleMap.setBuildingsEnabled(false);

        // 开始请求位置更新
        onLocationPermissionGranted();
    }

    /**
     * 应用自定义地图样式
     */
    private void applyCustomMapStyle() {
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                Log.e(TAG, "地图样式解析失败。");
            } else {
                Log.d(TAG, "地图样式应用成功。");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "地图样式资源未找到", e);
        }
    }

    /**
     * 设置位置回调
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // 无论是否在追踪状态，都更新位置
                for (Location location : locationResult.getLocations()) {
                    if (location.hasAccuracy() && location.getAccuracy() < 50.0) {
                        isLocationReady = true;
                        waitView.clearAnimation();
                        waitView.setVisibility(View.GONE);
                        waitTextView.setVisibility(View.GONE);
                        // 一旦位置准备好，显示地图
                        View mapFragment = findViewById(R.id.google_map);
                        if (mapFragment != null) {
                            mapFragment.setVisibility(View.VISIBLE);  // 位置准备好后显示地图
                        }
                        btnPauseResume.setClickable(true);
                        if (ActivityCompat.checkSelfPermission(GoogleMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(GoogleMapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        googleMap.setMyLocationEnabled(true);
                        locationTimeoutHandler.removeCallbacks(locationTimeoutRunnable);
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        if (isFirstStart) {
                            // 初始时将相机移动到当前位置
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM_LEVEL));
                        }
                    } else {
                        if (!isLocationReady) {
                            Log.d(TAG, "位置精度不足，继续尝试...");
                        }
                    }

                    if (isTracking && !isPaused && isLocationReady) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        updatePath(currentLatLng);
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(currentLatLng)
                                .zoom(MOVE_ZOOM_LEVEL)
                                .tilt(0)
                                .bearing(0)
                                .build();
                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 200, null);
                    }
                }
            }
        };
    }

    /**
     * 开始请求位置更新
     */
    @SuppressLint("MissingPermission")
    private void onLocationPermissionGranted() {
        if (googleMap != null) {
            googleMap.setMyLocationEnabled(false);
            googleMap.setBuildingsEnabled(false);

            // 开始请求位置更新
            requestLocationUpdates();
            // 开始定位超时处理
            locationTimeoutHandler.postDelayed(locationTimeoutRunnable, LOCATION_TIMEOUT);
        }
    }

    /**
     * 请求位置更新
     */
    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(5000)
                .setMinUpdateIntervalMillis(2000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(2)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * 根据新位置更新路径并计算总距离
     *
     * @param latLng 新的位置坐标
     */
    @SuppressLint("DefaultLocale")
    private void updatePath(LatLng latLng) {
        if (!isTracking || isPaused) {
            return;
        }

        // 如果pathPoints为空，表示处理第一个位置点
        if (pathPoints.isEmpty()) {
            pathPoints.add(latLng); // 直接添加第一个点，但暂不绘制
            trajectory.add(latLng);
            return; // 跳过绘制，等待下一个点
        }

        // 计算当前点与最后一个添加点之间的距离
        LatLng lastLatLng = pathPoints.get(pathPoints.size() - 1);
        float[] results = new float[1];
        Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, latLng.latitude, latLng.longitude, results);

        // 检查两点之间的距离是否显著（> 1米）
        if (results[0] > DISTANCE_THRESHOLD_METERS) {
            // 仅当用户移动超过阈值时更新总距离
            totalDistance += results[0];
            double totalDistanceKm = totalDistance / 1000.0;
            double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);

            // 确保距离和时间都有效后计算配速
            if (totalDistanceKm > realDistance && totalTimeMinutes > 0) {
                double avgPace = totalTimeMinutes / totalDistanceKm;
                double elapsedTimeInMinutes = elapsedTime / 60000.0;
                double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, metValue);
                runOnUiThread(() -> cTextView.setText(String.format("%d", Math.round(caloriesBurned))));
                // 检查配速是否在合理范围内
                if (avgPace >= 1.0 && avgPace <= 30.0) {
                    runOnUiThread(() -> avgPaceTextView.setText(String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60))));
                } else {
                    runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
                }
            } else {
                runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
            }

            // 仅在满足条件时添加当前点到pathPoints并绘制线条
            pathPoints.add(latLng);
            trajectory.add(latLng);
            drawCurrentPolyline();
        }
    }

    /**
     * 绘制当前的折线
     */
    private void drawCurrentPolyline() {
        if (!pathPoints.isEmpty() && googleMap != null) {
            PolylineOptions polylineOptions = new PolylineOptions().addAll(pathPoints).color(getResources().getColor(R.color.like_orange)).width(10);
            if (polyLines.isEmpty() || isPaused) {
                Polyline polyline = googleMap.addPolyline(polylineOptions);
                polyLines.add(polyline);
            } else {
                polyLines.get(polyLines.size() - 1).remove();
                Polyline polyline = googleMap.addPolyline(polylineOptions);
                polyLines.set(polyLines.size() - 1, polyline);
            }
        }
    }

    /**
     * 在无地图模式下，根据步数和时间更新平均配速
     */
    @SuppressLint("DefaultLocale")
    private void updateAvgPaceNoMapMode() {
        // 假设平均步长为0.75米
        float averageStepLength = 0.75f;
        float distance = currentStepCount * averageStepLength; // 单位米
        double distanceKm = distance / 1000.0;
        double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);
        if (distanceKm > realDistance && totalTimeMinutes > 0) {
            double avgPace = totalTimeMinutes / distanceKm;
            double elapsedTimeInMinutes = elapsedTime / 60000.0;
            double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, metValue);
            runOnUiThread(() -> cTextView.setText(String.format("%d", Math.round(caloriesBurned))));
            // 更新avgPaceTextView
            runOnUiThread(() -> avgPaceTextView.setText(String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60))));
        } else {
            runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
        }
    }

    /**
     * 根据体重、持续时间和MET值计算燃烧的卡路里
     *
     * @param weight            用户体重，单位千克
     * @param durationInMinutes 活动持续时间，单位分钟
     * @param metValue          活动的MET值
     * @return 燃烧的卡路里
     */
    private double calculateCalories(double weight, double durationInMinutes, double metValue) {
        double durationInHours = durationInMinutes / 60.0;
        return metValue * weight * durationInHours;
    }

    /**
     * 将LatLng点转换为可读的地址字符串
     *
     * @param latLng 位置坐标
     * @return 国家和城市的字符串，或“未知位置”
     */
    private String getAddressFromLatLng(LatLng latLng) {
        String address = "未知位置";

        // 确保Geocoder已初始化
        if (geocoder == null) {
            if (Geocoder.isPresent()) {
                geocoder = new Geocoder(this, Locale.getDefault());
            } else {
                Log.e(TAG, "Geocoder不可用。");
                return address;
            }
        }

        try {
            // 从纬度和经度获取地址
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                String country = addr.getCountryName(); // 国家
                String city = addr.getLocality();       // 城市

                if (country != null && city != null) {
                    address = country + ", " + city;
                } else if (country != null) {
                    address = country;
                } else if (city != null) {
                    address = city;
                }
            } else {
                Log.e(TAG, "未找到位置的地址。");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "无效的纬度或经度值。");
            e.printStackTrace();
        }

        return address;
    }

    /**
     * 显示最后的追踪路径，包括起点和终点标记
     */
    private void showLastTrack() {
        // 计算距离
        float distanceInKm;
        if (isMapMode) {
            distanceInKm = totalDistance / 1000.0f;
        } else {
            // 无地图模式，根据步数计算距离
            float averageStepLength = 0.75f;
            float distance = currentStepCount * averageStepLength; // 单位米
            distanceInKm = distance / 1000.0f;
        }

        String timeElapsed = timerTextView.getText().toString();
        int stepCount = currentStepCount;

        String avg = avgPaceTextView.getText().toString();
        // 获取最后位置的地址
        String address = "未知位置";
        if (isMapMode && initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            address = getAddressFromLatLng(initialLatLng);
        }
        // 创建跳转到RunSummaryActivity的Intent
        Intent intent = new Intent(GoogleMapActivity.this, RunSummaryActivity.class);
        intent.putExtra("distance", distanceInKm);
        intent.putExtra("avgPace", avg);
        intent.putExtra("time", timeElapsed);
        intent.putExtra("address", address);
        intent.putExtra("stepCount", stepCount);
        intent.putExtra("calories", cTextView.getText().toString());
        intent.putExtra("MODE", isMapMode ? "MAP" : "NO_MAP");

        if (isMapMode) {
            // 收集轨迹点
            ArrayList<LatLng> trajectoryList = new ArrayList<>(trajectory);
            intent.putParcelableArrayListExtra("trajectory", trajectoryList);
        }

        startActivity(intent);
        finish();
    }


    /**
     * 导航回MainActivity
     */
    private void navigateToWorkoutPage() {
        Intent intent = new Intent(GoogleMapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 在活动恢复时调用
     */
    @Override
    protected void onResume() {
        super.onResume();

        // 处理权限变化
        handlePermissionChanges();

        // 注册BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.pulsestepapplication.LOCATION_UPDATE");
        filter.addAction("com.example.pulsestepapplication.STEP_UPDATE");
        LocalBroadcastManager.getInstance(this).registerReceiver(trackingReceiver, filter);

        if (isTracking && !isPaused) {
            timerHandler.postDelayed(timerRunnable, 0);
            btnShow.setVisibility(View.GONE);
        } else {
            btnShow.setVisibility(View.VISIBLE);
        }

        // 检查用户是否已从设置页面授予后台定位权限
        if (isMapMode && hasDeniedBackgroundPermission) {
            // 重新检查后台定位权限
            if (isBackgroundLocationPermissionGranted()) {
                hasDeniedBackgroundPermission = false;
                sharedPreferences.edit().putBoolean(KEY_HAS_DENIED_BACKGROUND_PERMISSION, false).apply();
                // 更新按钮可见性
                updateGrantPermissionsButton();
                // 恢复追踪
                resumeTracking();
            }
        }
    }

    /**
     * 在活动暂停时调用
     */
    @Override
    protected void onPause() {
        super.onPause();
        // 取消注册BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trackingReceiver);
    }

    /**
     * 在活动停止时调用
     */
    @Override
    protected void onStop() {
        super.onStop();
        // 移除定时器回调
        if (isTracking) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        // 检查应用是否进入后台
        if (isTracking && isMapMode && !isBackgroundLocationPermissionGranted()) {
            pauseTracking();
            Toast.makeText(this, "由于缺少后台定位权限，跟踪已暂停。", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 检查是否授予了后台定位权限
     *
     * @return 如果授予，返回true；否则返回false
     */
    private boolean isBackgroundLocationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        // Android Q以下不需要后台定位权限
        return true;
    }

    /**
     * 处理权限请求结果
     *
     * @param requestCode  请求码
     * @param permissions  请求的权限
     * @param grantResults 权限结果
     */
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BACKGROUND_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 后台定位权限被授予
                hasDeniedBackgroundPermission = false;
                sharedPreferences.edit().putBoolean(KEY_HAS_DENIED_BACKGROUND_PERMISSION, false).apply();
                //resumeTracking();
                updateGrantPermissionsButton();
            } else {
                // 后台定位权限被拒绝
                hasDeniedBackgroundPermission = true;
                sharedPreferences.edit().putBoolean(KEY_HAS_DENIED_BACKGROUND_PERMISSION, true).apply();
                Toast.makeText(this, "后台定位权限被拒绝，应用将在后台停止跟踪。", Toast.LENGTH_LONG).show();
                // 显示引导对话框，引导用户前往设置手动授予权限
                showPermissionDeniedDialog();
                updateGrantPermissionsButton();

            }
        } else if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 活动识别权限被授予
                checkPermissions(); // 检查其他权限
            } else {
                // 权限被拒绝，导航回Workout页面
                Toast.makeText(this, "活动识别权限是必需的", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 定位权限被授予
                checkPermissions(); // 检查其他权限
            } else {
                // 权限被拒绝，导航回Workout页面
                Toast.makeText(this, "地图模式需要定位权限", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        }
    }

    /**
     * BroadcastReceiver，用于接收来自服务的位置信息和步数更新
     */
    private BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.pulsestepapplication.LOCATION_UPDATE".equals(intent.getAction())) {
                double lat = intent.getDoubleExtra("lat", 0.0);
                double lng = intent.getDoubleExtra("lng", 0.0);
                // 在地图上更新路径
                updatePath(new LatLng(lat, lng));
            } else if ("com.example.pulsestepapplication.STEP_UPDATE".equals(intent.getAction())) {
                int stepCount = intent.getIntExtra("stepCount", 0);
                Log.d(TAG, "收到步数更新: " + stepCount);
                // 更新步数显示
                updateStepCount(stepCount);
            }
        }
    };

    /**
     * 更新UI上的步数
     *
     * @param stepCount 当前步数
     */
    private void updateStepCount(int stepCount) {
        runOnUiThread(() -> {
            if (stepCount < 5) {
                stepTextView.setText("--");
            } else {
                stepTextView.setText(String.valueOf(stepCount));
            }
            currentStepCount = stepCount;
        });
    }

    /**
     * 按下返回键时调用
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /**
     * 当活动销毁时调用，停止服务和移除位置更新
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTrackingService();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        timerHandler.removeCallbacks(timerRunnable);
        locationTimeoutHandler.removeCallbacks(locationTimeoutRunnable);
    }
}
