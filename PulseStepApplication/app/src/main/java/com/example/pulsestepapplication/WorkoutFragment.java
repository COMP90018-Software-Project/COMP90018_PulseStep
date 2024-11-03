package com.example.pulsestepapplication;

import static com.google.firebase.firestore.DocumentChange.Type.ADDED;
import static com.google.firebase.firestore.DocumentChange.Type.MODIFIED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.icu.text.SimpleDateFormat;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.pulsestepapplication.bean.MessageBean;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


public class WorkoutFragment extends Fragment {
    private int count = 0;
    // Permission request codes
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE_JUMP = 11;
    private static final int ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE = 2;
    private static final int ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE_JUMP = 22;
    private static final float DISTANCE_THRESHOLD_METERS = 30000f; // 10 miles in meters
    private static final int BACKGROUND_PERMISSION_REQUEST_CODE = 3;

    private Double lastLatitude = null;
    private Double lastLongitude = null;
    private static final String TAG = "WorkoutFragment";

    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mMap;
    private boolean isUsingAmap = false;
    private ProgressBar mapProgressBar;
    public boolean locationGranted = false;
    private String userId;
    private String userName;
    private int userAge;
    private double userWeight;

    private View rootView; // Store the root view
    private TextView greetingTextView;
    private Bundle savedInstanceState; // Store savedInstanceState if needed
    private ListenerRegistration userListenerRegistration; // Store Firestore listener
    private ListenerRegistration likeListener;
    private ImageView  myStar;
    private View notification_badge;

    private static final long MAP_LOADING_TIMEOUT = 5000;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable mapLoadingTimeoutRunnable;

    private AtomicBoolean isRunButtonClicked = new AtomicBoolean(false);
    private AtomicBoolean isJumpButtonClicked = new AtomicBoolean(false);
    private Button runButton;
    private Button jumpButton;
    private boolean useMap;


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

        // Store savedInstanceState
        this.savedInstanceState = savedInstanceState;

        // Inflate the layout for this fragment and store the root view
        rootView = inflater.inflate(R.layout.fragment_workout, container, false);
        mapProgressBar = rootView.findViewById(R.id.map_progress_bar);


        // Get the arguments passed from MainActivity
        Bundle args = getArguments();
        if (args != null) {
            userId = args.getString("userId");
            userName = args.getString("fullName");
//            userAge = args.getInt("age");
            userWeight = args.getDouble("weight");
            // Get location permission status
            locationGranted = args.getBoolean("locationGranted", false);
            //setupNotificationListener();
            // Initialize map based on location permission status
            if (locationGranted) {
                mapProgressBar.setVisibility(View.VISIBLE);
                View placeholder = rootView.findViewById(R.id.map_placeholder);
                if (placeholder != null) {
                    placeholder.setVisibility(View.GONE);
                }
                View mapContainer = rootView.findViewById(R.id.map_container);
                if (mapContainer != null) {
                    mapContainer.setVisibility(View.VISIBLE);
                }
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
                initializeMap(rootView, savedInstanceState);
            } else {
                mapProgressBar.setVisibility(View.GONE);
                View placeholder = rootView.findViewById(R.id.map_placeholder);
                if (placeholder != null) {
                    placeholder.setVisibility(View.VISIBLE);
                }
                View mapContainer = rootView.findViewById(R.id.map_container);
                if (mapContainer != null) {
                    mapContainer.setVisibility(View.GONE);
                }
                //showToast("Location permissions not granted");
            }

        }

        // greeting text rendered on workout page
        greetingTextView = rootView.findViewById(R.id.greeting_text);
        greetingTextView.setText("Hi, " + userName);

        startUserDataListener();

        starLikeDataListener();

        // date text rendered on workout page
        TextView dateTextView = rootView.findViewById((R.id.date_text));
        dateTextView.setText(getFormattedDate());

        myStar = rootView.findViewById(R.id.my_star);
        notification_badge = rootView.findViewById(R.id.notification_badge);
        if (myStar != null) {
            myStar.setOnClickListener(view -> jumpToStarActivity());
        }

        // Set up Run button to initiate permission and network checks
        runButton = rootView.findViewById(R.id.run_button);
        runButton.setOnClickListener(v -> {
            Log.d(TAG, "Run button clicked.");
            if (isRunButtonClicked.getAndSet(true)) {
                Log.d(TAG, "Run button is already clicked, ignoring additional clicks.");
                return;
            }
            v.setEnabled(false);
            Log.d(TAG, "Run button clicked. Disabling button and proceeding.");
            boolean networkConnected = isNetworkConnected();
            boolean gpsEnabled = isGPSEnabled();
            if (networkConnected && gpsEnabled && hasLocationPermissions()) {
                useMap = true;
                checkActivityRecognitionPermissionAndProceed();
            } else {
                useMap = false;
                checkActivityRecognitionPermissionAndProceedNoMap();
            }
            handler.postDelayed(() -> {
                isRunButtonClicked.set(false);
                if (runButton != null) {
                    runButton.setEnabled(true);
                    Log.d(TAG, "Run button re-enabled after timeout.");
                }
            }, 5000);
        });
        //Set up Jump button to navigate to JumpActivity
        jumpButton = rootView.findViewById(R.id.jump_button);
        jumpButton.setOnClickListener(v -> {
            Log.d(TAG, "Jump button clicked.");
            if (isJumpButtonClicked.getAndSet(true)) {
                Log.d(TAG, "Jump button is already clicked, ignoring additional clicks.");
                return;
            }
            v.setEnabled(false);
            Log.d(TAG, "Jump button clicked. Disabling button and proceeding.");
            boolean networkConnected = isNetworkConnected();
            boolean gpsEnabled = isGPSEnabled();
            if (networkConnected && gpsEnabled && hasLocationPermissions()) {
                useMap = true;
                checkActivityRecognitionPermissionAndProceedToJump();
            } else {
                useMap = false;
                checkActivityRecognitionPermissionAndProceedToNoMapJump();
            }
            handler.postDelayed(() -> {
                isJumpButtonClicked.set(false);
                if (jumpButton != null) {
                    jumpButton.setEnabled(true);
                    Log.d(TAG, "Jump button re-enabled after timeout.");
                }
            }, 5000); // 5秒
        });

        return rootView;
    }

    /**
     * Starts a real-time listener to monitor changes to the user's full name and weight in Firestore.
     */
    private void startUserDataListener() {
        if (userId == null) {
            Log.e(TAG, "User ID is null. Cannot set up listener.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);

        // Real-time listener for user data
        userListenerRegistration = userRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Failed to listen for user data changes", e);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                // Get the updated fields from Firestore
                String fullName = documentSnapshot.getString("fullName");
                String weight = documentSnapshot.getString("weight");

                if (fullName != null && weight != null) {
                    Log.d(TAG, "Real-time update: Full Name = " + fullName + ", Weight = " + weight);
                    // Update the UI with the new data
                    updateUserData(fullName, Double.parseDouble(weight));
                } else {
                    Log.e(TAG, "Missing fields in user document.");
                }
            } else {
                Log.e(TAG, "User document does not exist.");
            }
        });
    }

    private void starLikeDataListener() {
        SharedPreferences sharedPref = getActivity().getSharedPreferences("my_prefs", Context.MODE_PRIVATE);
        boolean isNotificationEnabled = sharedPref.getBoolean("notification_switch", true);
        long savedTimestamp = sharedPref.getLong("notification_time", 0);
        if(!isNotificationEnabled){
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        likeListener = db.collection("message")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Listen failed.", e);
                        return;
                    }
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == ADDED) {
                            MessageBean messageBean = dc.getDocument().toObject(MessageBean.class);
                            if (messageBean.getUpdateUserId().equals(userId) && messageBean.getIsRead().equals("0")) {
                                notification_badge.setVisibility(View.VISIBLE);
                            }
                        } else if (dc.getType() == MODIFIED) {
                            MessageBean messageBean = dc.getDocument().toObject(MessageBean.class);
                            if (messageBean.getUpdateUserId().equals(userId) && messageBean.getIsRead().equals("0")) {
                                notification_badge.setVisibility(View.GONE);
                            }
                        }
                    }
                });
    }

    /**
     * Updates the UI with the latest full name and weight.
     *
     * @param fullName The updated full name.
     * @param weight   The updated weight.
     */
    private void updateUserData(String fullName, Double weight) {
        this.userName = fullName;
        this.userWeight = weight;

        // Update the greeting text with the new full name
        greetingTextView.setText("Hi, " + fullName);
    }


    private void jumpToStarActivity() {
        Intent intent = new Intent(getActivity(), LikeActivity.class);
        intent.putExtra("USER_ID", userId); // Pass the UID to the next page)
        startActivity(intent);
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
    private void checkActivityRecognitionPermissionAndProceedNoMap() {
        if (isActivityRecognitionPermissionRequired() && !hasActivityRecognitionPermission()) {
            // Request activity recognition permission
            requestActivityRecognitionPermission();
        } else {
            // Activity recognition permission granted, continue to check location permissions
            proceedToNoMapActivity();
        }
    }
    private void checkActivityRecognitionPermissionAndProceedToJump() {
        if (isActivityRecognitionPermissionRequired() && !hasActivityRecognitionPermission()) {
            // Request activity recognition permission
            requestActivityRecognitionPermissionToJump();
        } else {
            // Activity recognition permission granted, continue to check location permissions
            checkLocationPermissionAndProceedToJump();
        }
    }
    private void checkActivityRecognitionPermissionAndProceedToNoMapJump() {
        if (isActivityRecognitionPermissionRequired() && !hasActivityRecognitionPermission()) {
            // Request activity recognition permission
            requestActivityRecognitionPermissionToJump();
        } else {
            // Activity recognition permission granted, continue to check location permissions
            proceedToJumpActivity(0.0,0.0);
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
            checkLocationAndStartMapActivity();
        }
    }

    private void checkLocationPermissionAndProceedToJump() {
        if (!hasLocationPermissions()) {
            // Request location permissions
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE_JUMP);
        } else {
            // Location permissions granted, check if background location permission is needed
            checkLocationAndStartJumpActivity();
        }
    }
    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        return true;
                    }
                }
            } else {
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }
    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return locationManager.isLocationEnabled();
            } else {
                try {
                    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Initializes the appropriate map based on user's location.
     */
    @SuppressLint("MissingPermission")
    private void initializeMap(View view, Bundle savedInstanceState) {
        if (!hasLocationPermissions()) {
            return;
        }
        mapProgressBar.setVisibility(View.VISIBLE);
        boolean networkConnected = isNetworkConnected();
        boolean gpsEnabled = isGPSEnabled();
        if (!networkConnected || !gpsEnabled) {
            mapProgressBar.setVisibility(View.GONE);
            String message = "No network or GPS, map disabled.";
            showToast(message);
            useMap = false;
            return;
        }

        if (!isNetworkConnected()) {
            Log.d(TAG, "No network connection. Skipping map initialization.");
            mapProgressBar.setVisibility(View.GONE);
            showToast("No network connection");
            return;
        }
        mapLoadingTimeoutRunnable = () -> {
            if (mapProgressBar != null && mapProgressBar.getVisibility() == View.VISIBLE) {
                mapProgressBar.setVisibility(View.GONE);
                showToast("Map loading timed out, check connection.");
            }
        };
        handler.postDelayed(mapLoadingTimeoutRunnable, MAP_LOADING_TIMEOUT);
        // Obtain user's location before initializing the map
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        lastLatitude = location.getLatitude();
                        lastLongitude = location.getLongitude();

                        initializeGoogleMapWithLocation(lastLatitude, lastLongitude);

                    } else {
                        // Request new location if last known location is null
                        requestNewLocationForMapInitialization(view, savedInstanceState);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve location", e);
                    if (mapProgressBar != null) {
                        mapProgressBar.setVisibility(View.GONE);
                    }
                    showToast("Unable get current location.");
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

                        // Initialize Google Map with user's location
                        isUsingAmap = false;
                        initializeGoogleMapWithLocation(lastLatitude, lastLongitude);

                    } else {
                        // Keep the placeholder image visible
                        showToast("Unable to retrieve current location");
                        mapProgressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve location", e);
                    mapProgressBar.setVisibility(View.GONE);
                    // Keep the placeholder image visible
                });
    }


    private void initializeGoogleMapWithLocation(Double latitude, Double longitude) {
        GoogleMapOptions options = new GoogleMapOptions();
        if (latitude != null && longitude != null) {
            LatLng lastLatLng = new LatLng(latitude, longitude);
            options.camera(CameraPosition.fromLatLngZoom(lastLatLng, 15f));
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
        if (hasLocationPermissions()) {
            mMap.setMyLocationEnabled(false);
        }

    }

    /**
     * Applies custom style to the map, depending on which map is in use.
     */
    private void applyCustomMapStyle() {

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


    /**
     * Called when the map is ready (either AMap or Google Map).
     */
    private void onMapReady() {
        if (mapProgressBar != null) {
            mapProgressBar.setVisibility(View.GONE);
        }
        if (mapLoadingTimeoutRunnable != null) {
            handler.removeCallbacks(mapLoadingTimeoutRunnable);
        }
        View placeholder = rootView.findViewById(R.id.map_placeholder);
        if (placeholder != null) {
            placeholder.setVisibility(View.GONE);
        }
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
                            LatLng currentLatLng = new LatLng(latitude, longitude);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));

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
                        mapProgressBar.setVisibility(View.GONE);
                        showToast("Unable to get current location");
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            mapProgressBar.setVisibility(View.GONE);
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
                            LatLng currentLatLng = new LatLng(latitude, longitude);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));

                            // Store the new location
                            lastLatitude = latitude;
                            lastLongitude = longitude;
                        } else {
                            showToast("Unable to retrieve current location");
                            mapProgressBar.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to retrieve current location", e);
                        showToast("Unable to retrieve current location");
                        mapProgressBar.setVisibility(View.GONE);
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

    private void requestActivityRecognitionPermissionToJump() {
        requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE_JUMP);
    }

    /**
     * Checks location permissions and retrieves the user's location to start the appropriate map activity.
     */
    private void checkLocationAndStartMapActivity() {
        if (!hasLocationPermissions()) {
            //showToast("Location permissions not granted 2");
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
                        mapProgressBar.setVisibility(View.GONE);
                        proceedToNoMapActivity();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            proceedToNoMapActivity();
        }
    }


    private void checkLocationAndStartJumpActivity() {
        if (!hasLocationPermissions()) {
            //showToast("Location permissions not granted 3");
            proceedToJumpActivity(0.0, 0.0);
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
                            proceedToJumpActivity(latitude, longitude);
                        } else {
                            Log.d(TAG, "Last known location is null; requesting new location");
                            requestNewLocationForActivityToJump();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get location", e);
                        showToast("Unable to retrieve current location");
                        mapProgressBar.setVisibility(View.GONE);
                        proceedToJumpActivity(0.0, 0.0);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            mapProgressBar.setVisibility(View.GONE);
            proceedToJumpActivity(0.0, 0.0);
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
                            mapProgressBar.setVisibility(View.GONE);
                            proceedToNoMapActivity();
                        }
                    })
                    .addOnFailureListener(e -> {
                        showToast("Unable to retrieve current location");
                        mapProgressBar.setVisibility(View.GONE);
                        proceedToNoMapActivity();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            proceedToNoMapActivity();
        }
    }

    private void requestNewLocationForActivityToJump() {
        try {
            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Log.d(TAG, "New location obtained: " + latitude + ", " + longitude);
                            proceedToJumpActivity(latitude, longitude);
                        } else {
                            showToast("Unable to retrieve current location");
                            mapProgressBar.setVisibility(View.GONE);
                            proceedToJumpActivity(0.0, 0.0);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to retrieve current location", e);
                        showToast("Unable to retrieve current location");
                        mapProgressBar.setVisibility(View.GONE);
                        proceedToJumpActivity(0.0, 0.0);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            mapProgressBar.setVisibility(View.GONE);
            proceedToJumpActivity(0.0, 0.0);
        }
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
        intent = new Intent(getActivity(), GoogleMapActivity.class);
        Log.d(TAG, "Launching GoogleMapActivity");

        intent.putExtra("LOCATION_GRANTED", locationGranted);
        intent.putExtra("ACTIVITY_RECOGNITION_GRANTED", activityRecognitionGranted);
        if (locationGranted && activityRecognitionGranted) {
            intent.putExtra("MAP_MODE", true);
        } else if (!locationGranted && activityRecognitionGranted) {
            intent.putExtra("MAP_MODE", false);
        }
        intent.putExtra("LATITUDE", latitude);
        intent.putExtra("LONGITUDE", longitude);

        intent.putExtra("name", userName);
        intent.putExtra("age", userAge);
        intent.putExtra("weight", userWeight);
        startActivityForResult(intent, 333);
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
        if (locationGranted && activityRecognitionGranted) {
            intent.putExtra("MAP_MODE", true);
        } else if (!locationGranted && activityRecognitionGranted) {
            intent.putExtra("MAP_MODE", false);
        }
        intent.putExtra("LATITUDE", (Double) null);
        intent.putExtra("LONGITUDE", (Double) null);

        intent.putExtra("name", userName);
        intent.putExtra("age", userAge);
        intent.putExtra("weight", userWeight);
        startActivityForResult(intent, 111);
    }

    /**
     * Starts the appropriate jump activity based on the user's info.
     */
    private void proceedToJumpActivity(Double latitude, Double longitude) {
        boolean locationGranted = hasLocationPermissions();
        boolean activityRecognitionGranted = hasActivityRecognitionPermission();

        Intent intent = new Intent(getActivity(), JumpActivity.class);
        intent.putExtra("LOCATION_GRANTED", locationGranted);
        intent.putExtra("ACTIVITY_RECOGNITION_GRANTED", activityRecognitionGranted);

        intent.putExtra("name", userName);
        intent.putExtra("age", userAge);
        intent.putExtra("weight", userWeight);

        intent.putExtra("LATITUDE", latitude);
        intent.putExtra("LONGITUDE", longitude);
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
        } else if (requestCode == ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE_JUMP) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Activity recognition permission granted");
                // Proceed to start the jump activity
                checkLocationAndStartJumpActivity();
            } else {
                Log.d(TAG, "Activity recognition permission denied");
                // Show a message to the user
                showToast("Activity recognition permission is required.");

            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE_JUMP) {
            if (grantResults.length > 0 &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                            grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                Log.d(TAG, "Location permissions granted");
                if (isActivityRecognitionPermissionRequired() && !hasActivityRecognitionPermission()) {
                    requestActivityRecognitionPermissionToJump();
                } else {

                    checkLocationAndStartJumpActivity();
                }
                // Re-initialize the map now that permissions are granted
                View view = getView();
                if (view != null) {
                    initializeMap(view, null);
                }
            } else {
                proceedToJumpActivity(0.0, 0.0);
            }
        }
    }


    public String getFormattedDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE - MMM d", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }

    // This method updates the fragment's data
    public void updateData(String fullName, Double weight, Boolean locationGranted) {
        this.userName = fullName;
        this.userWeight = weight;
        this.locationGranted = locationGranted;

        // Now update the UI or other components using this new data
        TextView greetingTextView = getView().findViewById(R.id.greeting_text);
        greetingTextView.setText("Hi, " + fullName);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (runButton != null) {
            runButton.setEnabled(true);
            isRunButtonClicked.set(false);
            Log.d(TAG, "Run button state reset.");
        }

        if (jumpButton != null) {
            jumpButton.setEnabled(true);
            isJumpButtonClicked.set(false);
            Log.d(TAG, "Jump button state reset.");
        }
        setupNotificationListener();
        boolean currentPermissionStatus = hasLocationPermissions();
        boolean networkConnected = isNetworkConnected();
        boolean gpsEnabled = isGPSEnabled();
        boolean shouldUseMap = currentPermissionStatus != locationGranted || currentPermissionStatus && networkConnected && gpsEnabled;

        if (shouldUseMap) {
            locationGranted = currentPermissionStatus;
            if (locationGranted) {
                mapProgressBar.setVisibility(View.VISIBLE);
                View placeholder = rootView.findViewById(R.id.map_placeholder);
                if (placeholder != null) {
                    placeholder.setVisibility(View.GONE);
                }
                View mapContainer = rootView.findViewById(R.id.map_container);
                if (mapContainer != null) {
                    mapContainer.setVisibility(View.VISIBLE);
                }
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
                initializeMap(rootView, savedInstanceState);
            } else {
                FragmentManager fragmentManager = getChildFragmentManager();
                Fragment mapFragment = fragmentManager.findFragmentById(R.id.map_container);
                if (mapFragment != null) {
                    fragmentManager.beginTransaction().remove(mapFragment).commit();
                }
                mapProgressBar.setVisibility(View.GONE);
                View placeholder = rootView.findViewById(R.id.map_placeholder);
                if (placeholder != null) {
                    placeholder.setVisibility(View.VISIBLE);
                }
                View mapContainer = rootView.findViewById(R.id.map_container);
                if (mapContainer != null) {
                    mapContainer.setVisibility(View.GONE);
                }
                //showToast("Location permissions not granted");
            }
        }
    }

    private void setupNotificationListener() {
        if (userId == null) {
            return;
        }
        // Remove existing listener if any
        if (likeListener != null) {
            likeListener.remove();
            likeListener = null;
        }
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("my_prefs", Context.MODE_PRIVATE);
        boolean isNotificationEnabled = sharedPref.getBoolean("notification_switch", true);

        if (!isNotificationEnabled) {
            myStar.setVisibility(View.GONE);
            notification_badge.setVisibility(View.GONE);
            return;
        } else {
            myStar.setVisibility(View.VISIBLE);
            Log.d(TAG, "Notification is enabled. Showing myStar.");
        }

        long notificationOnTime = sharedPref.getLong("notification_on_time", 0);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a query with composite conditions and order
        Query query = db.collection("message")
                .whereEqualTo("updateUserId", userId)
                .whereEqualTo("isRead", "0")
                .whereGreaterThan("timestamp", notificationOnTime)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        // Add a snapshot listener to the query
        likeListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                return;
            }

            if (snapshots == null) {
                Log.w(TAG, "No snapshots found.");
                requireActivity().runOnUiThread(() -> {
                    notification_badge.setVisibility(View.GONE);
                });
                return;
            }

            // Count the number of unread messages
            int unreadCount = snapshots.size();

            // Update the notification badge on the main thread
            requireActivity().runOnUiThread(() -> {
                if (unreadCount > 0 ) {
                    notification_badge.setVisibility(View.VISIBLE);
                } else {
                    notification_badge.setVisibility(View.GONE);
                }
            });
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        setupNotificationListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (likeListener != null) {
            likeListener.remove();
            likeListener = null;
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
        }

        if (likeListener != null) {
            likeListener.remove();
            likeListener = null;
        }
        if (mapLoadingTimeoutRunnable != null) {
            handler.removeCallbacks(mapLoadingTimeoutRunnable);
        }
        handler.removeCallbacksAndMessages(null);
    }
}
