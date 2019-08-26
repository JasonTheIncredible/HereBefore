package co.clixel.herebefore;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        LocationListener {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private static final int Request_User_Location_Code = 99;
    boolean firstLoad = true;
    private Circle circle;
    private SeekBar circleSizeSeekBar;
    private String circleID;
    private LatLng latLng;
    private Double radius;
    private CameraPosition cameraPosition;
    private Button circleButton;
    private DatabaseReference firebaseCircles;
    private ValueEventListener eventListener;

    //TODO: Add discrete vertical seekBar (with images of circleButton) to change circle views and adjust max possible size of chatCircles.
    //TODO: Only load Firebase circles if they're within camera view (in onMapReady).
    //TODO: Prevent circle overlap.
    //TODO: Go to sign-in page only once user tries to add message to Firebase (in Chat.java).
    //TODO: Adjust what happens if no user is signed in upon clicking a chatCircle (in onMapReady).
    //TODO: Work on possible NullPointerExceptions.
    //TODO: Too much work on main thread.
    //TODO: Check updating in different states with another device.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.activity_maps);
        mapFragment.getMapAsync(this);

        circleButton = findViewById(R.id.circleButton);
        circleSizeSeekBar = findViewById(R.id.circleSizeSeekBar);

        // Get non-Firebase circle information after screen orientation change.
        if ( (savedInstanceState != null) && (savedInstanceState.getParcelable("circleCenter") != null) ) {

            latLng = savedInstanceState.getParcelable("circleCenter");
            radius = savedInstanceState.getDouble("circleRadius");
            firstLoad = false;
        }

        // Get camera position information after screen orientation change.
        if ( (savedInstanceState != null) && (savedInstanceState.getParcelable("cameraPosition") != null) ) {

            cameraPosition = savedInstanceState.getParcelable("cameraPosition");
            firstLoad = false;
        }

    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Start updating location.
        if (ContextCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            String provider = LocationManager.NETWORK_PROVIDER;
            locationManager.requestLocationUpdates(provider, 400, 1, this);
        } else{

            checkLocationPermission();
        }

        // Makes circle around user's current location on button press.
        circleButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                checkLocationPermission();

                FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(MapsActivity.this);

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(MapsActivity.this, new OnSuccessListener<Location>() {

                            @Override
                            public void onSuccess(Location location) {

                                // Get last known location. In some rare situations, this can be null.
                                if (location != null) {

                                    // Make circle the size set by the seekBar.
                                    int circleSize = circleSizeSeekBar.getProgress();

                                    // Make the circle a default size if the seekBar is set to 0.
                                    if (circleSize < 1){

                                        circleSize = 40;
                                    }

                                    // Logic to handle location object.
                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    CircleOptions circleOptions =
                                            new CircleOptions()
                                                    .center(latLng)
                                                    .radius(circleSize)
                                                    .clickable(true)
                                                    .strokeWidth(3f)
                                                    .strokeColor(Color.BLUE)
                                                    .fillColor(Color.argb(70, 50, 50, 100));

                                    if (circle != null){

                                        circle.remove();
                                    }

                                    circle = mMap.addCircle(circleOptions);
                                    circleSizeSeekBar.setProgress(circleSize);
                                }
                            }
                        });
            }
        });

        // Changes size of the circle using the seek bar at the bottom.
        circleSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (circle != null){

                    circle.setRadius(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {

            }
        });

        // Load Firebase circles.
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        firebaseCircles = rootRef.child("circles");
        eventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    if (dataSnapshot.getValue() != null) {

                        LatLng center = new LatLng((Double) ds.child("circle/center/latitude/").getValue(), (Double) ds.child("circle/center/longitude/").getValue());
                        boolean clickable = (boolean) ds.child("circle/clickable").getValue();
                        int fillColor = (int) (long) ds.child("circle/fillColor").getValue();
                        Object id = ds.child("circle/id").getValue();
                        long radius = (long) ds.child("circle/radius").getValue();
                        int strokeColor = (int) (long) ds.child("circle/strokeColor").getValue();
                        float strokeWidth = (float) (long) ds.child("circle/strokeWidth").getValue();
                        Circle circle = mMap.addCircle(
                                new CircleOptions()
                                        .center(center)
                                        .radius(radius)
                                        .clickable(clickable)
                                        .strokeWidth(strokeWidth)
                                        .strokeColor(strokeColor)
                                        .fillColor(fillColor)
                        );

                        // Set the Tag using the ID Firebase assigned. Sent to Chat.java in onMapReady() to identify the chatCircle.
                        circle.setTag(id);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        // Add the Firebase listener.
        firebaseCircles.addValueEventListener(eventListener);
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        // Clear map before adding new Firebase circles in onStart() to prevent overlap.
        if (mMap != null) {

            mMap.clear();
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.i(TAG, "onResume()");

        LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        // Check if GPS is enabled.
        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {

            buildAlertMessageNoGps();
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        Log.i(TAG, "onPause()");
    }

    @Override
    protected void onStop() {

        super.onStop();
        Log.i(TAG, "onStop()");


        // Stop updating location information.
        if (ContextCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(this);
        }

        // Stop the listener.
        if (circleButton != null) {

            circleButton.setOnClickListener(null);
        }

        // Stop the seekBar listener.
        if (circleSizeSeekBar != null) {

            circleSizeSeekBar.setOnSeekBarChangeListener(null);
        }

        // Stop the Firebase event listener.
        if (firebaseCircles != null){

            firebaseCircles.removeEventListener(eventListener);
        }

        // Stop the Firebase event listener.
        if (eventListener != null){

            eventListener = null;
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        Log.i(TAG, "onDestroy()");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        Log.i(TAG, "onSaveInstanceState()");

        // Save non-Firebase circle info upon screen orientation change.
        if (circle != null){

            outState.putParcelable("circleCenter", circle.getCenter());
            outState.putDouble("circleRadius", circle.getRadius());
        }

        // Save camera info upon screen orientation change.
        if (mMap != null) {

            outState.putParcelable("cameraPosition", mMap.getCameraPosition());
            super.onSaveInstanceState(outState);
        }
    }

    private void buildAlertMessageNoGps() {

        Log.i(TAG, "buildAlertMessageNoGps()");

        // If GPS is disabled, show an alert dialog and have the user turn GPS on.
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("GPS Disabled")
                .setMessage("Please enable your location services on the following screen.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int i) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .create()
                .show();
    }

    public void checkLocationPermission() {

        Log.i(TAG, "checkLocationPermission()");

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Device Location Required")
                        .setMessage("We need permission to use your location to find ChatCircles around you.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        Request_User_Location_Code);
                            }
                        })
                        .create()
                        .show();

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        Request_User_Location_Code);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.i(TAG, "onRequestPermissionsResult()");

        // If request is cancelled, the result arrays are empty.
        if (requestCode == Request_User_Location_Code) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Permission was granted, yay! Do the
                // location-related task you need to do.
                startLocationUpdates();

                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                    String provider = LocationManager.NETWORK_PROVIDER;
                    // Request location updates:
                    locationManager.requestLocationUpdates(provider, 400, 1, this);
                    mMap.setMyLocationEnabled(true);
                }
            }
        }
    }

    protected void startLocationUpdates(){

        Log.i(TAG, "startLocationUpdates()");

        // Create the location request to start receiving updates
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /* 10 secs */
        long UPDATE_INTERVAL = 10 * 1000;
        mLocationRequest.setInterval(UPDATE_INTERVAL);

        /* 2 sec */
        long FASTEST_INTERVAL = 2000;
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        checkLocationPermission();

        // New Google API SDK v11 uses getFusedLocationProviderClient(this)
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {

                    @Override
                    public void onLocationResult(LocationResult locationResult) {


                        onLocationChanged(locationResult.getLastLocation());
                    }
                },

                Looper.myLooper());
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.i(TAG, "onMapReady()");

        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
        }

        // Restore non-Firebase circle upon screen orientation change.
        if (latLng != null && radius != null) {

            CircleOptions circleOptions =
                    new CircleOptions()
                            .center(latLng)
                            .radius(radius)
                            .clickable(true)
                            .strokeWidth(3f)
                            .strokeColor(Color.BLUE)
                            .fillColor(Color.argb(70, 50, 50, 100));

            if (circle != null) {

                circle.remove();
            }

            int circleSize = (int) (double) radius;
            circle = mMap.addCircle(circleOptions);
            circleSizeSeekBar.setProgress(circleSize);
        }

        // Restore camera position upon screen orientation change.
        if (cameraPosition != null){

            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        // Go to chat when clicking on the circle.
        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {

            @Override
            public void onCircleClick(final Circle circle) {

                // Get the ID set by Firebase to identify which circle the user clicked on.
                circleID = (String) circle.getTag();

                // If the circle is new, it will not have a tag, as the tag is pulled from Firebase. Therefore, use the ID.
                if (circleID == null){

                    circleID = circle.getId();
                }

                // Check if the user is already signed in.
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                    // User is signed in.
                    // Connect to Firebase.
                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                    DatabaseReference firebaseCircles = rootRef.child("circles");
                    ValueEventListener eventListener = new ValueEventListener() {

                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            // If the circle the user clicked on doesn't already exist in Firebase, add it.
                            if( ( dataSnapshot.child("circle/id").getValue() == null ) || ! ( ( dataSnapshot.child("circle/id").getValue() ).equals(circleID) ) ) {

                                CircleInformation circleInformation = new CircleInformation();
                                circleInformation.setCircle(circle);
                                DatabaseReference newFirebaseCircle = FirebaseDatabase.getInstance().getReference().child("circles").push();
                                newFirebaseCircle.setValue(circleInformation);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    };

                    // Add Firebase listener.
                    firebaseCircles.addListenerForSingleValueEvent(eventListener);

                    // Go to Chat.java.
                    Intent Activity = new Intent(MapsActivity.this, Chat.class);
                    Activity.putExtra("circleID", circleID);
                    startActivity(Activity);
                } else {

                    // No user is signed in.
                    startActivity(new Intent(MapsActivity.this, signIn.class));
                }
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.i(TAG, "onLocationChanged()");

        // Zoom to user's location the first time the map loads.
        if ((location != null) && (firstLoad)) {

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                    .zoom(18)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera
                    .tilt(0)                   // Sets the tilt of the camera
                    .build();                   // Creates a CameraPosition from the builder
            //Can add a second parameter to this to change amount of time it takes for camera to zoom.
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), null);
            // Set Boolean to false to prevent unnecessary animation, as the camera should already be set on the user's location.
            firstLoad = false;
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

        Log.i(TAG, "onStatusChanged()");
    }

    @Override
    public void onProviderEnabled(String s) {

        Log.i(TAG, "onProviderEnabled()");
    }

    @Override
    public void onProviderDisabled(String s) {

        Log.i(TAG, "onProviderDisabled()");
    }
}