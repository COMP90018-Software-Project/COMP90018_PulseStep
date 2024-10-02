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

    private static final String TAG = "WorkoutFragment";

    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mMap;

    public WorkoutFragment() {
        // Required empty public constructor
    }

    /**
     * Inflates the fragment layout, initializes location services, and sets up button listeners.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        // Initialize FusedLocationProviderClient for location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Initialize map if location permissions are granted
        if (hasLocationPermissions()) {
            initializeMap();
        } else {
            Log.d(TAG, "Location permissions not granted; map will not be displayed");
        }

        // Set up Run button to initiate permission and network checks
        Button runButton = view.findViewById(R.id.run_button);
        runButton.setOnClickListener(v -> checkPermissionsAndProceed());

        // Set up Jump button to navigate to JumpActivity
        Button jumpButton = view.findViewById(R.id.jump_button);
        jumpButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), JumpActivity.class);
            startActivity(intent);
        });

        return view;
    }

    /**
     * Initializes the Google Map by replacing the map container with a SupportMapFragment.
     */
    private void initializeMap() {
        SupportMapFragment mapFragment = new SupportMapFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);
    }

    /**
     * Called when the Google Map is ready. Configures map settings and retrieves the user's location.
     *
     * @param googleMap The GoogleMap object that is ready to be used.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Disable default location layer to hide the blue dot
        if (hasLocationPermissions()) {
            mMap.setMyLocationEnabled(false);
        }

        // Apply custom map style and center the map on the user's location
        applyCustomMapStyle();
        getUserLocationAndZoom();
    }

    /**
     * Retrieves the user's current location and moves the map camera to that position with zoom.
     */
    private void getUserLocationAndZoom() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)); // Zoom level 15
                        } else {
                            // If last known location is null, request a new location
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
     * Applies a custom style to the Google Map from a raw resource file.
     */
    private void applyCustomMapStyle() {
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.workout));
            if (!success) {
                Log.e(TAG, "Map style parsing failed.");
            } else {
                Log.d(TAG, "Map style applied successfully.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Map style resource not found", e);
        }
    }

    /**
     * Checks if necessary permissions are granted and proceeds to network checks.
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
            if (isActivityRecognitionPermissionRequired()) {
                if (!hasActivityRecognitionPermission()) {
                    // Request activity recognition permission if required and not granted
                    requestActivityRecognitionPermission();
                } else {
                    // Permissions are granted; proceed to network checks
                    checkNetworkAndProceed();
                }
            } else {
                // Activity recognition permission not required; proceed to network checks
                checkNetworkAndProceed();
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
     * Checks network connectivity and proceeds based on the network status.
     */
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

    /**
     * Checks location permissions and retrieves the user's location to start the appropriate map activity.
     */
    private void checkLocationAndStartMapActivity() {
        if (!hasLocationPermissions()) {
            showToast("Location permissions not granted");
            proceedToMapActivity(null, null);
            return;
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
                            requestNewLocation();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get location", e);
                        showToast("Unable to retrieve current location");
                        proceedToMapActivity(null, null);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            proceedToMapActivity(null, null);
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
                            Log.d(TAG, "New location obtained: " + latitude + ", " + longitude);
                            proceedToMapActivity(latitude, longitude);
                        } else {
                            showToast("Unable to retrieve current location");
                            proceedToMapActivity(null, null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to retrieve current location", e);
                        showToast("Unable to retrieve current location");
                        proceedToMapActivity(null, null);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            showToast("Location permissions were denied");
            proceedToMapActivity(null, null);
        }
    }

    /**
     * Starts the appropriate map activity based on the user's location and permissions.
     *
     * @param latitude  The latitude of the user's location, or null if unavailable.
     * @param longitude The longitude of the user's location, or null if unavailable.
     */
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

    /**
     * Determines if the given location is within China based on latitude and longitude.
     *
     * @param latitude  The latitude to check.
     * @param longitude The longitude to check.
     * @return True if the location is in China, false otherwise.
     */
    private boolean isInChina(double latitude, double longitude) {
        return latitude >= 18.0 && latitude <= 53.0 && longitude >= 73.0 && longitude <= 135.0;
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
            } else {
                showToast("Activity recognition permission denied");
            }
            checkNetworkAndProceed();
        }
    }
}
