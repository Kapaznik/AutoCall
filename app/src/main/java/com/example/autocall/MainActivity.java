package com.example.autocall;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PHONE_CALL = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private static final double TARGET_LATITUDE = 32.108918; // Replace with your target latitude
    private static final double TARGET_LONGITUDE = 34.796233; // Replace with your target longitude
    private static final double MAX_DISTANCE = 100.0; // Define the maximum distance in meters for proximity

    private boolean called = false;

    private EditText coordinatesEditText;
    private EditText phoneNumberEditText;
    private Button checkButton;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_COORDINATES = "coordinates";

    private LocationManager locationManager;
    private LocationListener locationListener;

    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        coordinatesEditText = findViewById(R.id.coordinatesEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        checkButton = findViewById(R.id.checkButton);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                called = false;
                checkLocationPermission();
                saveCoordinates();
                minimizeApp();
            }
        });

        // Check if coordinates are already saved
        String savedCoordinates = sharedPreferences.getString(KEY_COORDINATES, "");
        if (!savedCoordinates.isEmpty()) {
            coordinatesEditText.setText(savedCoordinates);
        }

        // Start the background thread
        startBackgroundThread();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkLocationEnabled();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_LOCATION_PERMISSION);
        }
    }

    private void checkLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isGpsEnabled || isNetworkEnabled) {
            // Location services are enabled
        } else {
            Toast.makeText(this, "Location services are disabled", Toast.LENGTH_SHORT).show();
            openLocationSettings();
        }
    }

    private void openLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveCoordinates() {
        String coordinates = coordinatesEditText.getText().toString().trim();
        sharedPreferences.edit().putString(KEY_COORDINATES, coordinates).apply();
    }

    private void minimizeApp() {
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        stopBackgroundThread();
    }

    private void stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void startBackgroundThread() {
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                checkProximityInBackground();
                handler.postDelayed(this, 5000); // Adjust the interval as needed (e.g., 1000 milliseconds = 1 second)
            }
        };
        handler.postDelayed(runnable, 0);
    }

    private void stopBackgroundThread() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    private void checkProximityInBackground() {
        String coordinates = coordinatesEditText.getText().toString();
        String[] parts = coordinates.split(",");

        if (parts.length == 2) {
            double targetLatitude = Double.parseDouble(parts[0].trim());
            double targetLongitude = Double.parseDouble(parts[1].trim());

            Location targetLocation = new Location("");
            targetLocation.setLatitude(targetLatitude);
            targetLocation.setLongitude(targetLongitude);

            Log.i("UserTargetLocation",String.valueOf(targetLocation));

            Location userLocation = getLocation();

            Log.i("UserRealLocation", String.valueOf(userLocation));
            if (String.valueOf(userLocation)!=null) {
                Toast.makeText(this, String.valueOf(userLocation), Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "null", Toast.LENGTH_SHORT).show();
            }
            if (userLocation != null) {
                float distance = userLocation.distanceTo(targetLocation);
                if (distance <= MAX_DISTANCE && !called) {
                    makePhoneCallInBackground();
                }
            }
        }
    }

    private void makePhoneCallInBackground() {
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_PHONE_CALL);
        } else {
            startActivity(intent);
            called = true;
        }
    }

    private Location userLocation; // Declare userLocation as an instance variable

    private Location getLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    // Update the userLocation with the latest location
                    userLocation = location;
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            };

            // Request location updates from both GPS and network providers
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

            // Retrieve the last known location from GPS and network providers
            Location locationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            // Choose the best location from GPS and network providers
            if (locationGps != null && locationNetwork != null) {
                if (isBetterLocation(locationGps, locationNetwork)) {
                    userLocation = locationGps; // Update userLocation with the best location
                } else {
                    userLocation = locationNetwork; // Update userLocation with the best location
                }
            } else if (locationGps != null) {
                userLocation = locationGps; // Update userLocation with the available location
            } else if (locationNetwork != null) {
                userLocation = locationNetwork; // Update userLocation with the available location
            }
        }
        return userLocation; // Return the updated userLocation
    }




    private boolean isBetterLocation(Location location1, Location location2) {
        // Check which location has better accuracy and freshness
        final int TWO_MINUTES = 1000 * 60 * 2;
        boolean isNewer = location1.getTime() > location2.getTime();
        boolean isMoreAccurate = location1.getAccuracy() < location2.getAccuracy();
        boolean isWithinTwoMinutes = (location1.getTime() - location2.getTime()) < TWO_MINUTES;

        if (isNewer && isMoreAccurate) {
            return true;
        } else if (isNewer && !isMoreAccurate && isWithinTwoMinutes) {
            return true;
        }
        return false;
    }
}
