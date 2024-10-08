package com.example.pulsestepapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.MapView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

public class WorkoutFragment extends Fragment {

    // Permission request codes
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE = 2;
    private static final float DISTANCE_THRESHOLD_METERS = 16093.4f; // 10 miles in meters
    private static final int BACKGROUND_PERMISSION_REQUEST_CODE = 3;

    private Double lastLatitude = null;
    private Double lastLongitude = null;
    private static final String TAG = "WorkoutFragment";

    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mMap;
    private MapView amapView;
    private AMap aMap;
    private boolean isUsingAmap = false;
    private ProgressBar mapProgressBar;

    private String userName;
    private int userAge;
    private double userWeight;
    public WorkoutFragment() {
        // Required empty public constructor
    }

    /**
     * Inflates the fragment layout, initializes location services, and sets up button listeners.
     */
    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_workout, container, false);
        mapProgressBar = view.findViewById(R.id.map_progress_bar);
        // Get the arguments passed from MainActivity
        Bundle args = getArguments();
        if (args != null) {
            userName = args.getString("name");
            userAge = args.getInt("age");
            userWeight = args.getDouble("weight");
            // Get location permission status
            boolean locationGranted = args.getBoolean("locationGranted", false);
            // Initialize map based on location permission status
            if (locationGranted) {
                mapProgressBar.setVisibility(View.VISIBLE);
                // Initialize FusedLocationProviderClient for location services
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

                // Initialize the map
                initializeMap(view, savedInstanceState);
            } else {
                // Show placeholder or notify the user that location permission is not granted
                View placeholder = view.findViewById(R.id.map_placeholder);
                if (placeholder != null) {
                    placeholder.setVisibility(View.VISIBLE);
                }
                showToast("Location permissions not granted");
                //proceedToNoMapActivity();
            }
        }

        //greeting_text
        TextView greetingText = view.findViewById(R.id.greeting_text);
        greetingText.setText("Hi, " + userName);

        // Set up Run button to initiate permission and network checks
        Button runButton = view.findViewById(R.id.run_button);
        runButton.setOnClickListener(v -> {
            // When the user clicks the Run button, check and request activity recognition permission, then start the map activity
            checkActivityRecognitionPermissionAndProceed();
        });

        // Set up Jump button to navigate to JumpActivity
        Button jumpButton = view.findViewById(R.id.jump_button);
        jumpButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), JumpActivity.class);
            startActivity(intent);
        });

        return view;
    }
    private void checkActivityRecognitionPermissionAndProceed() {
        if (isActivityRecognitionPermissionRequired() && !hasActivityRecognitionPermission()) {
            // Request activity recognition permission
            requestActivityRecognitionPermission();
        } else {
            // Activity recognition permission granted, continue to check location permissions
            checkLocationPermissionAndProceed();
        }
    }

    private void checkLocationPermissionAndProceed() {
        if (!hasLocationPermissions()) {
            // Request location permissions
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Location permissions granted, check if background location permission is needed
            checkBackgroundLocationPermission();
        }
    }

    private void checkBackgroundLocationPermission() {
        if (!hasBackgroundLocationPermission()) {
            // Request background location permission
            requestBackgroundPermission();
        } else {
            // No need for background permission or already granted, enter map mode
            checkLocationAndStartMapActivity();
        }
    }

    private boolean hasBackgroundLocationPermission() {
        // Check if background permission is granted
        return ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBackgroundPermission() {
        // Request background location permission
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_PERMISSION_REQUEST_CODE);
    }

    /**
     * Initializes the appropriate map based on user's location.
     */
    @SuppressLint("MissingPermission")
    private void initializeMap(View view, Bundle savedInstanceState) {
        // Obtain user's location before initializing the map
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        lastLatitude = location.getLatitude();
                        lastLongitude = location.getLongitude();

                        if (isInChina(lastLatitude, lastLongitude)) {
                            // Initialize AMap with user's location
                            isUsingAmap = true;
                            initializeAMapWithLocation(view, savedInstanceState, lastLatitude, lastLongitude);
                        } else {
                            // Initialize Google Map with user's location
                            isUsingAmap = false;
                            initializeGoogleMapWithLocation(lastLatitude, lastLongitude);
                        }
                    } else {
                        // Request new location if last known location is null
                        requestNewLocationForMapInitialization(view, savedInstanceState);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve location", e);
                    // Keep the placeholder image visible
                });
    }

    private void requestNewLocationForMapInitialization(View view, Bundle savedInstanceState) {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        lastLatitude = location.getLatitude();
                        lastLongitude = location.getLongitude();

                        if (isInChina(lastLatitude, lastLongitude)) {
                            // Initialize AMap with user's location
                            isUsingAmap = true;
                            initializeAMapWithLocation(view, savedInstanceState, lastLatitude, lastLongitude);
                        } else {
                            // Initialize Google Map with user's location
                            isUsingAmap = false;
                            initializeGoogleMapWithLocation(lastLatitude, lastLongitude);
                        }
                    } else {
                        // Keep the placeholder image visible
                        showToast("Unable to retrieve current location");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve location", e);
                    // Keep the placeholder image visible
                });
    }

    private void initializeAMapWithLocation(View view, Bundle savedInstanceState, Double latitude, Double longitude) {
        AMapOptions aOptions = new AMapOptions();
        if (latitude != null && longitude != null) {
            com.amap.api.maps.model.CameraPosition cp = new com.amap.api.maps.model.CameraPosition(
                    new com.amap.api.maps.model.LatLng(latitude, longitude), 18f, 0, 0);
            aOptions.camera(cp);
        }
        amapView = new MapView(requireContext(), aOptions);
        ViewGroup mapContainer = view.findViewById(R.id.map_container);
        // Add the map view to the container
        mapContainer.addView(amapView);
        amapView.onCreate(savedInstanceState);
        aMap = amapView.getMap();

        configureMap();
        onMapReady();
    }

    private void initializeGoogleMapWithLocation(Double latitude, Double longitude) {
        GoogleMapOptions options = new GoogleMapOptions();
        if (latitude != null && longitude != null) {
            LatLng lastLatLng = new LatLng(latitude, longitude);
            options.camera(CameraPosition.fromLatLngZoom(lastLatLng, 18f));
        }
        SupportMapFragment mapFragment = SupportMapFragment.newInstance(options);
        // Replace the placeholder with the map fragment
        getChildFragmentManager().beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit();
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            configureMap();
            onMapReady();
        });
    }

    /**
     * Configures map settings based on which map is being used.
     */
    @SuppressLint("MissingPermission")
    private void configureMap() {
        // Apply custom map style
        applyCustomMapStyle();
        if (isUsingAmap) {
            aMap.showBuildings(false);
            aMap.showMapText(false);
        } else {
            // Configure Google Map settings
            if (hasLocationPermissions()) {
                mMap.setMyLocationEnabled(false);
            }
        }
    }

    /**
     * Applies custom style to the map, depending on which map is in use.
     */
    private void applyCustomMapStyle() {
        if (isUsingAmap) {
            // Apply custom style to AMap
            try {
                com.amap.api.maps.model.CustomMapStyleOptions customMapStyleOptions =
                        new com.amap.api.maps.model.CustomMapStyleOptions();

                // Set style data path (located in assets directory)
                customMapStyleOptions.setStyleDataPath(getAssetsPath("style/style.data"));

                // If there are extra texture files, set the texture file path
                customMapStyleOptions.setStyleExtraPath(getAssetsPath("style/style_extra.data"));

                // Apply custom style options to the map
                aMap.setCustomMapStyle(customMapStyleOptions);

                // Enable custom map style
                aMap.setMapCustomEnable(true);

                Log.d(TAG, "Custom map style applied successfully to AMap.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to apply custom map style to AMap", e);
            }
        } else {
            // Apply custom style to Google Map
            try {
                boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.workout));
                if (!success) {
                    Log.e(TAG, "Map style parsing failed.");
                } else {
                    Log.d(TAG, "Custom map style applied successfully to Google Map.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Map style resource not found", e);
            }
        }
    }

    /**
     * Gets the full path of a file in the assets directory.
     */
    private String getAssetsPath(String fileName) {
        return "file:///android_asset/" + fileName;
    }

    /**
     * Called when the map is ready (either AMap or Google Map).
     */
    private void onMapReady() {
        // Hide the placeholder image
        if (mapProgressBar != null) {
            mapProgressBar.setVisibility(View.GONE);
        }
        // Proceed with map setup
        getUserLocationAndZoom();
    }

    /**
     * Retrieves the user's current location and moves the map camera to that position with zoom.
     */
    private void getUserLocationAndZoom() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null && shouldUpdateMap(location)) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            if (isUsingAmap) {
                                com.amap.api.maps.model.LatLng currentLatLng =
                                        new com.amap.api.maps.model.LatLng(latitude, longitude);
                                aMap.moveCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f));
                            } else {
                                LatLng currentLatLng = new LatLng(latitude, longitude);
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f));
                            }
                            // Store the new location
                            lastLatitude = latitude;
                            lastLongitude = longitude;
                        } else {
                            // If last known location is null or doesn't need update, request a new location
                            requestNewLocation();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to retrieve location", e);
                        showToast("Unable to get current location");
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            showToast("Location permission denied");
        }
    }

    /**
     * Requests a new high-accuracy location from the location provider.
     */
    private void requestNewLocation() {
        try {
            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            if (isUsingAmap) {
                                com.amap.api.maps.model.LatLng currentLatLng =
                                        new com.amap.api.maps.model.LatLng(latitude, longitude);
                                aMap.moveCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f));
                            } else {
                                LatLng currentLatLng = new LatLng(latitude, longitude);
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f));
                            }
                            // Store the new location
                            lastLatitude = latitude;
                            lastLongitude = longitude;
                        } else {
                            showToast("Unable to retrieve current location");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to retrieve current location", e);
                        showToast("Unable to retrieve current location");
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            showToast("Location permission denied");
        }
    }

    /**
     * Determines whether the map should be updated based on the distance from the last known location.
     *
     * @param newLocation The new location to compare.
     * @return True if the map should be updated, false otherwise.
     */
    private boolean shouldUpdateMap(Location newLocation) {
        if (lastLatitude == null || lastLongitude == null) {
            // No previous location, so we should update the map
            return true;
        }

        // Create a Location object for the last known location
        Location lastLocation = new Location("lastLocation");
        lastLocation.setLatitude(lastLatitude);
        lastLocation.setLongitude(lastLongitude);

        // Calculate the distance between the last location and the new location
        float distanceInMeters = lastLocation.distanceTo(newLocation);

        // Return true if the distance is greater than the threshold
        return distanceInMeters > DISTANCE_THRESHOLD_METERS;
    }

    /**
     * Checks if necessary permissions are granted and proceeds to location checks.
     * If permissions are not granted, requests them.
     */
    private void checkPermissionsAndProceed() {
        if (!hasLocationPermissions()) {
            // Request location permissions if not granted
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Location permissions are granted; check activity recognition permissions
            if (isActivityRecognitionPermissionRequired() && !hasActivityRecognitionPermission()) {
                // Request activity recognition permission if required and not granted
                requestActivityRecognitionPermission();
            } else {
                // Permissions are granted; proceed to start map activity
                checkLocationAndStartMapActivity();
            }
        }
    }

    /**
     * Determines if activity recognition permission is required based on Android version.
     *
     * @return True if permission is required, false otherwise.
     */
    private boolean isActivityRecognitionPermissionRequired() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    /**
     * Checks if location permissions are granted.
     *
     * @return True if granted, false otherwise.
     */
    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if activity recognition permission is granted.
     *
     * @return True if granted, false otherwise.
     */
    private boolean hasActivityRecognitionPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests activity recognition permission.
     */
    private void requestActivityRecognitionPermission() {
        requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE);
    }

    /**
     * Checks location permissions and retrieves the user's location to start the appropriate map activity.
     */
    private void checkLocationAndStartMapActivity() {
        if (!hasLocationPermissions()) {
            showToast("Location permissions not granted");
            proceedToNoMapActivity();
            return;
        }
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        }
        Log.d(TAG, "Attempting to get last known location");

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Log.d(TAG, "Location obtained: " + latitude + ", " + longitude);
                            proceedToMapActivity(latitude, longitude);
                        } else {
                            Log.d(TAG, "Last known location is null; requesting new location");
                            requestNewLocationForActivity();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get location", e);
                        showToast("Unable to retrieve current location");
                        proceedToNoMapActivity();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            proceedToNoMapActivity();
        }
    }

    /**
     * Requests a new high-accuracy location from the location provider for starting map activity.
     */
    private void requestNewLocationForActivity() {
        try {
            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Log.d(TAG, "New location obtained: " + latitude + ", " + longitude);
                            proceedToMapActivity(latitude, longitude);
                        } else {
                            showToast("Unable to retrieve current location");
                            proceedToNoMapActivity();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to retrieve current location", e);
                        showToast("Unable to retrieve current location");
                        proceedToNoMapActivity();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            proceedToNoMapActivity();
        }
    }

    /**
     * Determines if the given latitude and longitude are within China's boundaries.
     *
     * @param latitude  The latitude to check.
     * @param longitude The longitude to check.
     * @return True if within China, false otherwise.
     */
    private boolean isInChina(double latitude, double longitude) {
        // China's approximate geographical boundaries
        return latitude >= 18.0 && latitude <= 54.0 && longitude >= 73.0 && longitude <= 135.0;
    }

    /**
     * Starts the appropriate map activity based on the user's location and permissions.
     *
     * @param latitude  The latitude of the user's location, or null if unavailable.
     * @param longitude The longitude of the user's location, or null if unavailable.
     */
    private void proceedToMapActivity(Double latitude, Double longitude) {
        boolean locationGranted = hasLocationPermissions();
        boolean activityRecognitionGranted = hasActivityRecognitionPermission();

        Intent intent;
        if (latitude != null && longitude != null && isInChina(latitude, longitude)) {
            intent = new Intent(getActivity(), GoogleMapActivity.class);
            Log.d(TAG, "Launching AmapActivity");
        } else {
            intent = new Intent(getActivity(), GoogleMapActivity.class);
            Log.d(TAG, "Launching GoogleMapActivity");
        }

        intent.putExtra("LOCATION_GRANTED", locationGranted);
        intent.putExtra("ACTIVITY_RECOGNITION_GRANTED", activityRecognitionGranted);
        if (locationGranted && activityRecognitionGranted){
            intent.putExtra("MAP_MODE", true);
        }else if (!locationGranted && activityRecognitionGranted){
            intent.putExtra("MAP_MODE", false);
        }
        intent.putExtra("LATITUDE", latitude);
        intent.putExtra("LONGITUDE", longitude);

        intent.putExtra("name", userName);
        intent.putExtra("age", userAge);
        intent.putExtra("weight", userWeight);
        startActivity(intent);
    }
    /**
     * Starts the appropriate map activity based on the user's location and permissions.
     */
    private void proceedToNoMapActivity() {
        boolean locationGranted = hasLocationPermissions();
        boolean activityRecognitionGranted = hasActivityRecognitionPermission();

        Intent intent;
        intent = new Intent(getActivity(), NoMapActivity.class);
        Log.d(TAG, "Launching NoMapActivity");


        intent.putExtra("LOCATION_GRANTED", locationGranted);
        intent.putExtra("ACTIVITY_RECOGNITION_GRANTED", activityRecognitionGranted);
        if (locationGranted && activityRecognitionGranted){
            intent.putExtra("MAP_MODE", true);
        }else if (!locationGranted && activityRecognitionGranted){
            intent.putExtra("MAP_MODE", false);
        }
        intent.putExtra("LATITUDE", (Double) null);
        intent.putExtra("LONGITUDE", (Double) null);

        intent.putExtra("name", userName);
        intent.putExtra("age", userAge);
        intent.putExtra("weight", userWeight);
        startActivity(intent);
    }
    /**
     * Displays a short toast message to the user.
     *
     * @param message The message to display.
     */
    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Handles the result of permission requests and proceeds accordingly.
     *
     * @param requestCode  The request code passed in requestPermissions().
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                            grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                Log.d(TAG, "Location permissions granted");
                if (isActivityRecognitionPermissionRequired() && !hasActivityRecognitionPermission()) {
                    requestActivityRecognitionPermission();
                } else {
                    checkLocationAndStartMapActivity();
                }
                // Re-initialize the map now that permissions are granted
                View view = getView();
                if (view != null) {
                    initializeMap(view, null);
                }
            } else {
                proceedToNoMapActivity();
            }
        } else if (requestCode == ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Activity recognition permission granted");
                // Proceed to start the map activity
                checkLocationAndStartMapActivity();
            } else {
                Log.d(TAG, "Activity recognition permission denied");
                // Show a message to the user
                showToast("Activity recognition permission is required.");

            }
        }
    }

    /**
     * Lifecycle methods to manage MapView's state.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (isUsingAmap && amapView != null) {
            amapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isUsingAmap && amapView != null) {
            amapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isUsingAmap && amapView != null) {
            amapView.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isUsingAmap && amapView != null) {
            amapView.onSaveInstanceState(outState);
        }
    }
}
