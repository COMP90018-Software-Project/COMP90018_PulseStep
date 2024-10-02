package com.example.pulsestepapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class WorkoutFragment extends Fragment implements OnMapReadyCallback {
    // Permission request codes
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE = 2;

    private static final String TAG = "MyFragment";

    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mMap;
    public WorkoutFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment (replace 'fragment_workout' with your layout file)
        View view = inflater.inflate(R.layout.fragment_workout, container, false);


        // 初始化 FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        if (hasLocationPermissions()) {

            initializeMap();
        } else {
            Log.d(TAG, "定位权限未授予，地图未显示");
        }
        // Find the run button and set up an OnClickListener
        Button runButton = view.findViewById(R.id.run_button);
        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the permission and network checking process when the button is clicked
                checkPermissionsAndProceed();
            }
        });


        // Find the Jump button by its ID
        Button jumpButton = view.findViewById(R.id.jump_button);

        // Set OnClickListener for the Jump button
        jumpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start RunningActivity
                Intent intent = new Intent(getActivity(), JumpActivity.class);
                startActivity(intent);
            }
        });


        return view;
    }

    /**
     * 初始化地图，将其加载到 map_container 中
     */
    private void initializeMap() {
        SupportMapFragment mapFragment = new SupportMapFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // 禁用定位图层以隐藏蓝色的定位点
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(false); // 设置为 false 以隐藏定位点
        }
        applyCustomMapStyle();
        // 获取并设置用户当前位置
        getUserLocationAndZoom();
    }

    /**
     * 获取用户当前位置并将地图中心定位到该位置，同时放大地图
     */
    private void getUserLocationAndZoom() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)); // 放大级别为15
                        } else {
                            // 如果最后已知位置为空，尝试获取新位置
                            requestNewLocation();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "获取位置失败", e);
                        showToast("无法获取当前位置");
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "权限错误", e);
            showToast("定位权限被拒绝");
        }
    }

    // Attempt to apply custom map style
    private void applyCustomMapStyle() {
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.workout));
            if (!success) {
                Log.e("GoogleMapActivity", "Style parsing failed.");
            } else {
                Log.d("GoogleMapActivity", "Map style applied successfully.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("GoogleMapActivity", "Can't find style. Error: ", e);
        }
    }
    /**
     * Checks if the necessary permissions are granted.
     * If not, requests them. Proceeds to network checking if permissions are granted.
     */
    private void checkPermissionsAndProceed() {
        if (!hasLocationPermissions()) {
            // Request location permissions if not granted
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Location permissions are granted, now check activity recognition permissions
            if (isActivityRecognitionPermissionRequired()) {
                if (!hasActivityRecognitionPermission()) {
                    // Request activity recognition permission if required and not granted
                    requestActivityRecognitionPermission();
                } else {
                    // Activity recognition permission is granted, proceed to network checking
                    checkNetworkAndProceed();
                }
            } else {
                // Activity recognition permission not required (below Android 10), proceed to network checking
                checkNetworkAndProceed();
            }
        }
    }

    private boolean isActivityRecognitionPermissionRequired() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasActivityRecognitionPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestActivityRecognitionPermission() {
        requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE);
    }

    private void checkNetworkAndProceed() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            showToast("Unable to retrieve network status");
            proceedToMapActivity(null, null);
            return;
        }

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "Network is available");
                requireActivity().runOnUiThread(() -> checkLocationAndStartMapActivity());
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "Network lost");
                requireActivity().runOnUiThread(() -> showToast("Network connection unavailable"));
            }
        });

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            checkLocationAndStartMapActivity();
        } else {
            showToast("Waiting for network connection...");
        }
    }

    private void checkLocationAndStartMapActivity() {
        if (!hasLocationPermissions()) {
            showToast("Location permissions not granted");
            proceedToMapActivity(null, null);
            return;
        }

        Log.d(TAG, "Attempting to get last known location");

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            proceedToMapActivity(null, null);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Log.d(TAG, "Location obtained: " + latitude + ", " + longitude);
                            proceedToMapActivity(latitude, longitude);
                        } else {
                            Log.d(TAG, "Last known location is null, attempting to get a new location");
                            requestNewLocation();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to get location", e);
                        showToast("Unable to retrieve current location");
                        proceedToMapActivity(null, null);
                    }
                });
    }

    private void requestNewLocation() {
        try {
            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                Log.d(TAG, "New location obtained: " + latitude + ", " + longitude);
                                proceedToMapActivity(latitude, longitude);
                            } else {
                                showToast("Unable to retrieve current location");
                                proceedToMapActivity(null, null);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Failed to retrieve current location", e);
                            showToast("Unable to retrieve current location");
                            proceedToMapActivity(null, null);
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
            showToast("Location permissions were denied");
            proceedToMapActivity(null, null);
        }
    }

    private void proceedToMapActivity(Double latitude, Double longitude) {
        Intent intent;
        if (latitude != null && longitude != null) {
            if (isInChina(latitude, longitude)) {
                intent = new Intent(getActivity(), AmapActivity.class);
            } else {
                intent = new Intent(getActivity(), GoogleMapActivity.class);
            }
        } else {
            intent = new Intent(getActivity(), GoogleMapActivity.class);
        }

        boolean locationGranted = hasLocationPermissions();
        boolean activityRecognitionGranted = hasActivityRecognitionPermission();
        intent.putExtra("LOCATION_GRANTED", locationGranted);
        intent.putExtra("ACTIVITY_RECOGNITION_GRANTED", activityRecognitionGranted);

        startActivity(intent);
    }

    private boolean isInChina(double latitude, double longitude) {
        return latitude >= 18.0 && latitude <= 53.0 && longitude >= 73.0 && longitude <= 135.0;
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                Log.d(TAG, "Location permissions granted");
                if (isActivityRecognitionPermissionRequired()) {
                    if (!hasActivityRecognitionPermission()) {
                        requestActivityRecognitionPermission();
                    } else {
                        checkNetworkAndProceed();
                    }
                } else {
                    checkNetworkAndProceed();
                }
            } else {
                showToast("Location permissions denied");
                proceedToMapActivity(null, null);
            }
        } else if (requestCode == ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Activity recognition permission granted");
                checkNetworkAndProceed();
            } else {
                showToast("Activity recognition permission denied");
                checkNetworkAndProceed();
            }
        }
    }

}
