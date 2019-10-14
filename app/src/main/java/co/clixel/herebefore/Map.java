package co.clixel.herebefore;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.SeekBar;

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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class Map extends FragmentActivity implements
        OnMapReadyCallback,
        LocationListener,
        PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "Map";
    private GoogleMap mMap;
    private static final int Request_User_Location_Code = 99;
    boolean firstLoad = true;
    private Circle circle;
    private DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference firebaseCircles = rootRef.child("circles");
    private ValueEventListener firebaseEventListener;
    private SeekBar circleSizeSeekBar;
    private SeekBar largerCircleSizeSeekBar;
    private String uuid;
    private Button pointButton, circleViewsButton, mapTypeButton;
    private PopupMenu popupMapType;
    private PopupMenu popupCircleViews;
    private PopupMenu popupCreateLargerCircles;
    private PopupMenu popupCreateSmallerCircles;
    private Boolean mapTypeMenuIsOpen = false;
    private Boolean circleViewsMenuIsOpen = false;
    private Boolean largerCirclesMenuIsOpen = false;
    private Boolean smallerCirclesMenuIsOpen = false;

    //TODO: Prevent circle overlap (also, clicking on circle creates a circle, may need to clear map).
    //TODO: Give user option to change color of the circles (or just change it outright).
    //TODO: Adjust circle location / size (make any shape possible) and get rid of circle always updating to be on user's location.
    //TODO: Make points easier to see somehow.
    //TODO: Have circles spread if they are too close when clicking.
    //TODO: Only load Firebase circles if they're within camera view (in onMapReady) (getMap().getProjection().getVisibleRegion().latLangBounds.)
    //TODO: Make sure Firebase listener is always updating map properly.
    //TODO: Optimize Firebase loading.
    //TODO: Too much work on main thread.
    //TODO: Make checkLocationPermission Async / create loading animations.
    //TODO: Send message without entering app.
    //TODO: Work on possible NullPointerExceptions (try/catch).
    //TODO: Check updating in different states with another device - make sure uuids never overlap.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.activity_maps);
        mapFragment.getMapAsync(this);

        mapTypeButton = findViewById(R.id.mapTypeButton);
        pointButton = findViewById(R.id.pointButton);
        circleSizeSeekBar = findViewById(R.id.circleSizeSeekBar);
        circleViewsButton = findViewById(R.id.circleViewsButton);
        largerCircleSizeSeekBar = findViewById(R.id.largerCircleSizeSeekBar);
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Start updating location.
        if (ContextCompat.checkSelfPermission(Map.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            String provider = LocationManager.NETWORK_PROVIDER;
            locationManager.requestLocationUpdates(provider, 1000, 0, this);
        } else{

            checkLocationPermission();
        }

        // Shows a menu to change the map type.
        mapTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                popupMapType = new PopupMenu(Map.this, mapTypeButton);
                popupMapType.setOnMenuItemClickListener(Map.this);
                popupMapType.inflate(R.menu.maptype_menu);
                popupMapType.show();
                mapTypeMenuIsOpen = true;

                // Changes boolean value (used in OnConfigurationChanged) to determine whether menu is currently open.
                popupMapType.setOnDismissListener(new PopupMenu.OnDismissListener(){
                    @Override
                    public void onDismiss(PopupMenu popupMenu) {

                        mapTypeMenuIsOpen = false;
                        popupMapType.setOnDismissListener(null);
                    }
                });
            }
        });

        // Shows a menu to filter circle views.
        circleViewsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                popupCircleViews = new PopupMenu(Map.this, circleViewsButton);
                popupCircleViews.setOnMenuItemClickListener(Map.this);
                popupCircleViews.inflate(R.menu.circleviews_menu);
                popupCircleViews.show();
                circleViewsMenuIsOpen = true;

                // Changes boolean value (used in OnConfigurationChanged) to determine whether menu is currently open.
                popupCircleViews.setOnDismissListener(new PopupMenu.OnDismissListener(){
                    @Override
                    public void onDismiss(PopupMenu popupMenu) {

                        circleViewsMenuIsOpen = false;
                        popupCircleViews.setOnDismissListener(null);
                    }
                });
            }
        });

        // Makes circle around user's current location on button press.
        pointButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                            @Override
                            public void onSuccess(Location location) {

                                // Get last known location. In some rare situations, this can be null.
                                if (location != null) {

                                    // Make circle the size set by the seekBar.
                                    int circleSize = 1;

                                    // Logic to handle location object.
                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    CircleOptions circleOptions =
                                            new CircleOptions()
                                                    .center(latLng)
                                                    .clickable(true)
                                                    .fillColor(Color.argb(100, 255, 255, 0))
                                                    .radius(circleSize)
                                                    .strokeColor(Color.YELLOW)
                                                    .strokeWidth(3f);

                                    // Remove any other "new" circle before adding the circle to Firebase.
                                    if (circle != null){

                                        circle.remove();
                                    }

                                    // Add circle to the map and go to chat.
                                    if (mMap != null) {

                                        circle = mMap.addCircle(circleOptions);

                                        uuid = UUID.randomUUID().toString();

                                        // Check if the user is already signed in.
                                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                                            // User is signed in.
                                            // Connect to Firebase.
                                            firebaseCircles.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                    // If the uuid already exists in Firebase, generate another uuid and try again.
                                                    if (!dataSnapshot.exists()) {

                                                        // Go to Chat.java with the uuid (used to connect the circle with the messageThread).
                                                        Intent Activity = new Intent(Map.this, Chat.class);
                                                        // Pass this boolean value (true) to Chat.java.
                                                        Activity.putExtra("newCircle", true);
                                                        // Pass this value to Chat.java to identify the circle.
                                                        Activity.putExtra("uuid", uuid);
                                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                                        Activity.putExtra("latitude", circle.getCenter().latitude);
                                                        Activity.putExtra("longitude", circle.getCenter().longitude);
                                                        Activity.putExtra("fillColor", Color.argb(100, 255, 255, 0));
                                                        Activity.putExtra("radius", circle.getRadius());
                                                        startActivity(Activity);
                                                    } else {

                                                        circle.remove();

                                                        pointButton.callOnClick();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                        } else {

                                            // No user is signed in.
                                            // Connect to Firebase.
                                            firebaseCircles.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {

                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                    // If the uuid doesn't already exist in Firebase, add the circle. Else, remove circle and add another until a unique uuid appears.
                                                    if (!dataSnapshot.exists()) {

                                                        // Go to Chat.java with the uuid (used to connect the circle with the messageThread).
                                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                                        // Pass this boolean value (true) to Chat.java.
                                                        Activity.putExtra("newCircle", true);
                                                        // Pass this value to Chat.java to identify the circle.
                                                        Activity.putExtra("uuid", uuid);
                                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                                        Activity.putExtra("latitude", circle.getCenter().latitude);
                                                        Activity.putExtra("longitude", circle.getCenter().longitude);
                                                        Activity.putExtra("fillColor", Color.argb(100, 255, 255, 0));
                                                        Activity.putExtra("radius", circle.getRadius());
                                                        startActivity(Activity);
                                                    } else {

                                                        circle.remove();

                                                        pointButton.callOnClick();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        });
            }
        });

        circleSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                // Changes size of the circle.
                if (circle != null) {

                    circle.setRadius(progress);
                }

                // Creates popup above seekBar that gives user option to make larger circle.
                if (circleSizeSeekBar.getProgress() == 100) {

                    // Set popup to show at end of seekBar if API >= 19, as this is when Gravity.END is supported.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                        popupCreateLargerCircles = new PopupMenu(Map.this, circleSizeSeekBar, Gravity.END);
                    } else {

                        popupCreateLargerCircles = new PopupMenu(Map.this, circleSizeSeekBar);
                    }
                    popupCreateLargerCircles.setOnMenuItemClickListener(Map.this);
                    popupCreateLargerCircles.inflate(R.menu.largercircle_seekbar_menu);
                    popupCreateLargerCircles.show();
                    largerCirclesMenuIsOpen = true;

                    // Changes boolean value (used in OnConfigurationChanged) to determine whether menu is currently open.
                    popupCreateLargerCircles.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu popupMenu) {

                            largerCirclesMenuIsOpen = false;
                            popupCreateLargerCircles.setOnDismissListener(null);
                        }
                    });
                }

                // Ensures the popup closes.
                if ( (popupCreateLargerCircles != null) && (circleSizeSeekBar.getProgress() < 100) ) {

                    popupCreateLargerCircles.dismiss();
                }
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {

                // Creates circle
                if (circle == null) {

                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                @Override
                                public void onSuccess(Location location) {

                                    // Get last known location. In some rare situations, this can be null.
                                    if (location != null) {

                                        // Make circle the size set by the seekBar.
                                        int circleSize = circleSizeSeekBar.getProgress();

                                        // Logic to handle location object.
                                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                        CircleOptions circleOptions =
                                                new CircleOptions()
                                                        .center(latLng)
                                                        .clickable(true)
                                                        .fillColor(Color.argb(70, 255, 215, 0))
                                                        .radius(circleSize)
                                                        .strokeColor(Color.YELLOW)
                                                        .strokeWidth(3f);

                                        if (circle != null) {

                                            circle.remove();
                                        }

                                        circle = mMap.addCircle(circleOptions);
                                        circleSizeSeekBar.setProgress(circleSize);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        // Clear map before adding new Firebase circles in onStart() to prevent overlap.
        // Set circle to null so changing circleSizeSeekBar in onStart() will create a circle.
        if (mMap != null) {

            mMap.clear();
            circleSizeSeekBar.setProgress(0);
            circle = null;
        }

        // If the largerCircleSizeSeekBar is visible, set it to View.GONE and make the original one visible (as mMap.clear() is called so no circles exist).
        if (largerCircleSizeSeekBar.getVisibility() != View.GONE) {

            largerCircleSizeSeekBar.setVisibility(View.GONE);
            circleSizeSeekBar.setVisibility(View.VISIBLE);
            circleSizeSeekBar.setEnabled(true);
            largerCircleSizeSeekBar.setOnSeekBarChangeListener(null);
        }

        // Load Firebase circles, as onMapReady() doesn't get called after onRestart().
        firebaseEventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    if (dataSnapshot.getValue() != null) {

                        LatLng center = new LatLng((Double) ds.child("circleOptions/center/latitude/").getValue(), (Double) ds.child("circleOptions/center/longitude/").getValue());
                        int fillColor = (int) (long) ds.child("circleOptions/fillColor").getValue();
                        double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                        Circle circle = mMap.addCircle(
                                new CircleOptions()
                                        .center(center)
                                        .clickable(true)
                                        .fillColor(fillColor)
                                        .radius(radius)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                        uuid = (String) ds.child("uuid").getValue();

                        circle.setTag(uuid);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        // Add the Firebase listener.
        firebaseCircles.addValueEventListener(firebaseEventListener);

        // Close any open menus
        if (popupCircleViews != null) {

            popupCircleViews.dismiss();
        }

        // Close any open menus
        if (popupCreateLargerCircles != null) {

            popupCreateLargerCircles.dismiss();
        }

        // Close any open menus
        if (popupCreateSmallerCircles != null) {

            popupCreateSmallerCircles.dismiss();
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

        Log.i(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {

        Log.i(TAG, "onStop()");

        // Remove updating location information.
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {

            if (ContextCompat.checkSelfPermission(Map.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                locationManager.removeUpdates(Map.this);
            }
        }

        //Remove the listener.
        if (mapTypeButton != null) {

            mapTypeButton.setOnClickListener(null);
        }

        // Remove the listener.
        if (circleViewsButton != null) {

            circleViewsButton.setOnClickListener(null);
        }

        // Remove the listener.
        if (popupCircleViews != null) {

            popupCircleViews.setOnDismissListener(null);
        }

        // Remove the listener.
        if (pointButton != null) {

            pointButton.setOnClickListener(null);
        }

        // Remove the seekBar listener.
        if (circleSizeSeekBar != null) {

            circleSizeSeekBar.setOnSeekBarChangeListener(null);
        }

        // Remove the seekBar listener.
        if (largerCircleSizeSeekBar != null) {

            largerCircleSizeSeekBar.setOnSeekBarChangeListener(null);
        }

        // Remove the Firebase event listener.
        if (firebaseCircles != null && firebaseEventListener != null) {

            firebaseCircles.removeEventListener(firebaseEventListener);
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        Log.i(TAG, "onDestroy()");
        super.onDestroy();
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

    private void checkLocationPermission() {

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
                                ActivityCompat.requestPermissions(Map.this,
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
                    if (locationManager != null) {

                        locationManager.requestLocationUpdates(provider, 1000, 0, this);
                        mMap.setMyLocationEnabled(true);
                        // Set firstLoad to true to guarantee the camera moves to the user's location.
                        firstLoad = true;
                    }
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

        /* 1 sec */
        long FASTEST_INTERVAL = 1000;
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
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
        }

        // Load Firebase circles.
        firebaseEventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    if (dataSnapshot.getValue() != null) {

                        LatLng center = new LatLng((Double) ds.child("circleOptions/center/latitude/").getValue(), (Double) ds.child("circleOptions/center/longitude/").getValue());
                        int fillColor = (int) (long) ds.child("circleOptions/fillColor").getValue();
                        double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                        Circle circle = mMap.addCircle(
                                new CircleOptions()
                                        .center(center)
                                        .clickable(true)
                                        .fillColor(fillColor)
                                        .radius(radius)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                        uuid = (String) ds.child("uuid").getValue();

                        circle.setTag(uuid);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        // Add the Firebase listener.
        firebaseCircles.addValueEventListener(firebaseEventListener);

        // Go to Chat.java when clicking on the circle.
        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {

            @Override
            public void onCircleClick(final Circle circle) {

                if (circle.getTag() != null) {

                    // Get the ID set by Firebase to identify which circle the user clicked on.
                    uuid = (String) circle.getTag();
                } else {

                    // If the circle is new, it will not have a tag, as the tag is pulled from Firebase. Therefore, generate a uuid.
                    uuid = UUID.randomUUID().toString();
                }

                // Check if the user is already signed in.
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                    // User is signed in.

                    // If circle.getTag() == null, the circle is new. Therefore, compare it to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                    if (circle.getTag() == null) {

                        firebaseCircles.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                // If the uuid already exists in Firebase, generate another uuid and try again.
                                if (dataSnapshot.exists()) {

                                    // Generate another UUID and try again.
                                    uuid = UUID.randomUUID().toString();

                                    // Go to Chat.java with the uuid (used to connect the circle with the messageThread).
                                    Intent Activity = new Intent(Map.this, Chat.class);
                                    // Pass this boolean value (true) to Chat.java.
                                    Activity.putExtra("newCircle", true);
                                    // Pass this value to Chat.java to identify the circle.
                                    Activity.putExtra("uuid", uuid);
                                    // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                    Activity.putExtra("latitude", circle.getCenter().latitude);
                                    Activity.putExtra("longitude", circle.getCenter().longitude);
                                    Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                    Activity.putExtra("radius", circle.getRadius());
                                    startActivity(Activity);
                                } else {

                                    // Go to Chat.java with the uuid (used to connect the circle with the messageThread).
                                    Intent Activity = new Intent(Map.this, Chat.class);
                                    // Pass this boolean value (true) to Chat.java.
                                    Activity.putExtra("newCircle", true);
                                    // Pass this value to Chat.java to identify the circle.
                                    Activity.putExtra("uuid", uuid);
                                    // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                    Activity.putExtra("latitude", circle.getCenter().latitude);
                                    Activity.putExtra("longitude", circle.getCenter().longitude);
                                    Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                    Activity.putExtra("radius", circle.getRadius());
                                    startActivity(Activity);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    } else {

                        // Go to Chat.java with the boolean value.
                        Intent Activity = new Intent(Map.this, Chat.class);
                        // Pass this boolean value (false) to Chat.java.
                        Activity.putExtra("newCircle", false);
                        // Pass this value to Chat.java to identify the circle.
                        Activity.putExtra("uuid", uuid);
                        startActivity(Activity);
                    }
                } else {

                    // No user is signed in.

                    // If circle.getTag() == null, the circle is new. Therefore, compare it to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                    if (circle.getTag() == null) {

                        firebaseCircles.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                // If the uuid already exists in Firebase, generate another uuid and try again.
                                if (dataSnapshot.exists()) {

                                    // Generate another UUID and try again.
                                    uuid = UUID.randomUUID().toString();

                                    // Go to Chat.java with the uuid (used to connect the circle with the messageThread).
                                    Intent Activity = new Intent(Map.this, SignIn.class);
                                    // Pass this boolean value (true) to Chat.java.
                                    Activity.putExtra("newCircle", true);
                                    // Pass this value to Chat.java to identify the circle.
                                    Activity.putExtra("uuid", uuid);
                                    // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                    Activity.putExtra("latitude", circle.getCenter().latitude);
                                    Activity.putExtra("longitude", circle.getCenter().longitude);
                                    Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                    Activity.putExtra("radius", circle.getRadius());
                                    startActivity(Activity);
                                } else {

                                    // Go to Chat.java with the uuid (used to connect the circle with the messageThread).
                                    Intent Activity = new Intent(Map.this, SignIn.class);
                                    // Pass this boolean value (true) to Chat.java.
                                    Activity.putExtra("newCircle", true);
                                    // Pass this value to Chat.java to identify the circle.
                                    Activity.putExtra("uuid", uuid);
                                    // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                    Activity.putExtra("latitude", circle.getCenter().latitude);
                                    Activity.putExtra("longitude", circle.getCenter().longitude);
                                    Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                    Activity.putExtra("radius", circle.getRadius());
                                    startActivity(Activity);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    } else {

                        // Go to Chat.java with the boolean value.
                        Intent Activity = new Intent(Map.this, SignIn.class);
                        // Pass this boolean value (false) to Chat.java.
                        Activity.putExtra("newCircle", false);
                        // Pass this value to Chat.java to identify the circle.
                        Activity.putExtra("uuid", uuid);
                        startActivity(Activity);
                    }
                }
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.i(TAG, "onLocationChanged()");

        // Zoom to user's location the first time the map loads.
        if ( (location != null) && (firstLoad) ) {

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                    .zoom(18)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera
                    .tilt(0)                   // Sets the tilt of the camera
                    .build();                   // Creates a CameraPosition from the builder
            // Can add a second parameter to this to change amount of time it takes for camera to zoom.
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), null);
            // Set Boolean to false to prevent unnecessary animation, as the camera should already be set on the user's location.
            firstLoad = false;
        }

        // Keep the circle's location on the user at all times.
        if (circle != null) {

            FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                        @Override
                        public void onSuccess(Location location) {

                            // Get last known location. In some rare situations, this can be null.
                            if (location != null) {

                                // Creates smaller circle if smaller seekBar is visible. Else, create larger circle.
                                if (circleSizeSeekBar.getVisibility() != View.GONE) {

                                    // Make circle the size set by the seekBar.
                                    int circleSize = circleSizeSeekBar.getProgress();

                                    // Logic to handle location object.
                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    CircleOptions circleOptions =
                                            new CircleOptions()
                                                    .center(latLng)
                                                    .clickable(true)
                                                    .fillColor(Color.argb(70, 255, 215, 0))
                                                    .radius(circleSize)
                                                    .strokeColor(Color.YELLOW)
                                                    .strokeWidth(3f);

                                    if (circle != null) {

                                        circle.remove();
                                    }

                                    circle = mMap.addCircle(circleOptions);
                                } else {

                                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                                    mFusedLocationClient.getLastLocation()
                                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                                @Override
                                                public void onSuccess(Location location) {

                                                    // Get last known location. In some rare situations, this can be null.
                                                    if (location != null) {

                                                        // Make circle the size set by the seekBar.
                                                        int circleSize = largerCircleSizeSeekBar.getProgress() + 100;

                                                        // Logic to handle location object.
                                                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                                        CircleOptions circleOptions =
                                                                new CircleOptions()
                                                                        .center(latLng)
                                                                        .clickable(true)
                                                                        .fillColor(Color.argb(70, 255, 215, 0))
                                                                        .radius(circleSize)
                                                                        .strokeColor(Color.YELLOW)
                                                                        .strokeWidth(3f);

                                                        if (circle != null) {

                                                            circle.remove();
                                                        }

                                                        circle = mMap.addCircle(circleOptions);
                                                    }
                                                }
                                            });
                                }
                            }
                        }
                    });
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {

        // Called when the orientation of the screen changes.
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged()");

        // Reloads the popup when the orientation changes to prevent viewing issues.
        if ( newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && mapTypeMenuIsOpen) {

            popupMapType.dismiss();
            popupMapType.show();
            mapTypeMenuIsOpen = true;
        } else if ( newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && circleViewsMenuIsOpen){

            popupMapType.dismiss();
            popupMapType.show();
            mapTypeMenuIsOpen = true;
        }

        // Reloads the popup when the orientation changes to prevent viewing issues.
        if ( newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && circleViewsMenuIsOpen) {

            popupCircleViews.dismiss();
            popupCircleViews.show();
            circleViewsMenuIsOpen = true;
        } else if ( newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && circleViewsMenuIsOpen){

            popupCircleViews.dismiss();
            popupCircleViews.show();
            circleViewsMenuIsOpen = true;
        }

        // Reloads the popup when the orientation changes to prevent viewing issues.
        if ( newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && largerCirclesMenuIsOpen) {

            popupCreateLargerCircles.dismiss();
            popupCreateLargerCircles.show();
            largerCirclesMenuIsOpen = true;
        } else if ( newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && largerCirclesMenuIsOpen) {

            popupCreateLargerCircles.dismiss();
            popupCreateLargerCircles.show();
            largerCirclesMenuIsOpen = true;
        }

        // Reloads the popup when the orientation changes to prevent viewing issues.
        if ( newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && smallerCirclesMenuIsOpen) {

            popupCreateSmallerCircles.dismiss();
            popupCreateSmallerCircles.show();
            smallerCirclesMenuIsOpen = true;
        } else if ( newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && smallerCirclesMenuIsOpen) {

            popupCreateSmallerCircles.dismiss();
            popupCreateSmallerCircles.show();
            smallerCirclesMenuIsOpen = true;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        Log.i(TAG, "onMenuItemClick()");
        // Sets the circleviews_menu actions.
        switch(menuItem.getItemId()) {

            // maptype_menu
            case R.id.roadmap:

                Log.i(TAG, "onMenuItemClick() -> road map");

                // Use the "road map" map type if the map is not null.
                if (mMap != null) {

                    // getMapType() returns 1 if the map type is set to "road map".
                    if (mMap.getMapType() != 1) {

                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    }
                }
                mapTypeMenuIsOpen = false;
                return true;

            // maptype_menu
            case R.id.satellite:

                Log.i(TAG, "onMenuItemClick() -> satellite");

                // Use the "satellite" map type if the map is not null.
                if (mMap != null) {

                    // getMapType() returns 2 if the map type is set to "satellite".
                    if (mMap.getMapType() != 2) {

                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    }
                }
                mapTypeMenuIsOpen = false;
                return true;

            // maptype_menu
            case R.id.hybrid:

                Log.i(TAG, "onMenuItemClick() -> hybrid");

                // Use the "hybrid" map type if the map is not null.
                if (mMap != null) {

                    // getMapType() returns 4 if the map type is set to "hybrid".
                    if (mMap.getMapType() != 4) {

                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    }
                }
                mapTypeMenuIsOpen = false;
                return true;

            // maptype_menu
            case R.id.terrain:

                Log.i(TAG, "onMenuItemClick() -> terrain");

                // Use the "terrain" map type if the map is not null.
                if (mMap != null) {

                    // getMapType() returns 3 if the map type is set to "terrain".
                    if (mMap.getMapType() != 3) {

                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    }
                }
                mapTypeMenuIsOpen = false;
                return true;

            // circleviews_menu
            case R.id.everything:

                Log.i(TAG, "onMenuItemClick() -> everything");

                // Remove previous Firebase listener.
                if (firebaseCircles != null && firebaseEventListener != null){

                    firebaseCircles.removeEventListener(firebaseEventListener);
                }
                mMap.clear();
                // Set circle to null so changing circleSizeSeekBar will create a circle.
                circle = null;
                // Set progress to 0, as no circle exists.
                circleSizeSeekBar.setProgress(0);
                // If the largerCircleSizeSeekBar is visible, set it to View.GONE and make the original one visible (as mMap.clear() is called so no circles exist).
                if (largerCircleSizeSeekBar.getVisibility() != View.GONE) {

                    largerCircleSizeSeekBar.setVisibility(View.GONE);
                    circleSizeSeekBar.setVisibility(View.VISIBLE);
                    circleSizeSeekBar.setEnabled(true);
                    largerCircleSizeSeekBar.setOnSeekBarChangeListener(null);
                }
                // Load all Firebase circles.
                firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot ds : dataSnapshot.getChildren()) {

                            if (dataSnapshot.getValue() != null) {

                                LatLng center = new LatLng((Double) ds.child("circleOptions/center/latitude/").getValue(), (Double) ds.child("circleOptions/center/longitude/").getValue());
                                int fillColor = (int) (long) ds.child("circleOptions/fillColor").getValue();
                                double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                Circle circle = mMap.addCircle(
                                        new CircleOptions()
                                                .center(center)
                                                .clickable(true)
                                                .fillColor(fillColor)
                                                .radius(radius)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                uuid = (String) ds.child("uuid").getValue();

                                circle.setTag(uuid);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                circleViewsMenuIsOpen = false;
                return true;

            case R.id.largeCircles:

                Log.i(TAG, "onMenuItemClick() -> largeCircles");

                // Remove previous Firebase listener.
                if (firebaseCircles != null && firebaseEventListener != null){

                    firebaseCircles.removeEventListener(firebaseEventListener);
                }
                mMap.clear();
                // Set circle to null so changing circleSizeSeekBar will create a circle.
                circle = null;
                // Set progress to 0, as no circle exists.
                circleSizeSeekBar.setProgress(0);
                // If the largerCircleSizeSeekBar is visible, set it to View.GONE and make the original one visible (as mMap.clear() is called so no circles exist).
                if (largerCircleSizeSeekBar.getVisibility() != View.GONE) {

                    largerCircleSizeSeekBar.setVisibility(View.GONE);
                    circleSizeSeekBar.setVisibility(View.VISIBLE);
                    circleSizeSeekBar.setEnabled(true);
                    largerCircleSizeSeekBar.setOnSeekBarChangeListener(null);
                }
                // Load Firebase circles.
                firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot ds : dataSnapshot.getChildren()) {

                            if (dataSnapshot.getValue() != null) {

                                // Only load large circles (radius > 100)
                                if ((double) (long)ds.child("circleOptions/radius").getValue() >  100) {

                                    LatLng center = new LatLng((Double) ds.child("circleOptions/center/latitude/").getValue(), (Double) ds.child("circleOptions/center/longitude/").getValue());
                                    int fillColor = (int) (long) ds.child("circleOptions/fillColor").getValue();
                                    double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                    Circle circle = mMap.addCircle(
                                            new CircleOptions()
                                                    .center(center)
                                                    .clickable(true)
                                                    .fillColor(fillColor)
                                                    .radius(radius)
                                                    .strokeColor(Color.YELLOW)
                                                    .strokeWidth(3f)
                                    );

                                    // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                    uuid = (String) ds.child("uuid").getValue();

                                    circle.setTag(uuid);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                circleViewsMenuIsOpen = false;
                return true;

            case R.id.mediumCircles:

                Log.i(TAG, "onMenuItemClick() -> mediumCircles");

                // Remove previous Firebase listener.
                if (firebaseCircles != null && firebaseEventListener != null){

                    firebaseCircles.removeEventListener(firebaseEventListener);
                }
                mMap.clear();
                // Set circle to null so changing circleSizeSeekBar will create a circle.
                circle = null;
                // Set progress to 0, as no circle exists.
                circleSizeSeekBar.setProgress(0);
                // If the largerCircleSizeSeekBar is visible, set it to View.GONE and make the original one visible (as mMap.clear() is called so no circles exist).
                if (largerCircleSizeSeekBar.getVisibility() != View.GONE) {

                    largerCircleSizeSeekBar.setVisibility(View.GONE);
                    circleSizeSeekBar.setVisibility(View.VISIBLE);
                    circleSizeSeekBar.setEnabled(true);
                    largerCircleSizeSeekBar.setOnSeekBarChangeListener(null);
                }
                // Load Firebase circles.
                firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot ds : dataSnapshot.getChildren()) {

                            if (dataSnapshot.getValue() != null) {

                                // Only load medium circles (50 < radius < 100)
                                if ((double) (long)ds.child("circleOptions/radius").getValue() > 50 && (double) (long)ds.child("circleOptions/radius").getValue() <= 100 ) {

                                    LatLng center = new LatLng((Double) ds.child("circleOptions/center/latitude/").getValue(), (Double) ds.child("circleOptions/center/longitude/").getValue());
                                    int fillColor = (int) (long) ds.child("circleOptions/fillColor").getValue();
                                    double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                    Circle circle = mMap.addCircle(
                                            new CircleOptions()
                                                    .center(center)
                                                    .clickable(true)
                                                    .fillColor(fillColor)
                                                    .radius(radius)
                                                    .strokeColor(Color.YELLOW)
                                                    .strokeWidth(3f)
                                    );

                                    // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                    uuid = (String) ds.child("uuid").getValue();

                                    circle.setTag(uuid);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                circleViewsMenuIsOpen = false;
                return true;

            case R.id.smallCircles:

                Log.i(TAG, "onMenuItemClick() -> smallCircles");

                // Remove previous Firebase listener.
                if (firebaseCircles != null && firebaseEventListener != null){

                    firebaseCircles.removeEventListener(firebaseEventListener);
                }
                mMap.clear();
                // Set circle to null so changing circleSizeSeekBar will create a circle.
                circle = null;
                // Set progress to 0, as no circle exists.
                circleSizeSeekBar.setProgress(0);
                // If the largerCircleSizeSeekBar is visible, set it to View.GONE and make the original one visible (as mMap.clear() is called so no circles exist).
                if (largerCircleSizeSeekBar.getVisibility() != View.GONE) {

                    largerCircleSizeSeekBar.setVisibility(View.GONE);
                    circleSizeSeekBar.setVisibility(View.VISIBLE);
                    circleSizeSeekBar.setEnabled(true);
                    largerCircleSizeSeekBar.setOnSeekBarChangeListener(null);
                }
                // Load Firebase circles.
                firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot ds : dataSnapshot.getChildren()) {

                            if (dataSnapshot.getValue() != null) {

                                // Only load small circles (1 < radius < 50)
                                if ((double) (long)ds.child("circleOptions/radius").getValue() > 1 && (double) (long)ds.child("circleOptions/radius").getValue() <= 50 ) {

                                    LatLng center = new LatLng((Double) ds.child("circleOptions/center/latitude/").getValue(), (Double) ds.child("circleOptions/center/longitude/").getValue());
                                    int fillColor = (int) (long) ds.child("circleOptions/fillColor").getValue();
                                    double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                    Circle circle = mMap.addCircle(
                                            new CircleOptions()
                                                    .center(center)
                                                    .clickable(true)
                                                    .fillColor(fillColor)
                                                    .radius(radius)
                                                    .strokeColor(Color.YELLOW)
                                                    .strokeWidth(3f)
                                    );

                                    // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                    uuid = (String) ds.child("uuid").getValue();

                                    circle.setTag(uuid);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                circleViewsMenuIsOpen = false;
                return true;

            case R.id.points:

                Log.i(TAG, "onMenuItemClick() -> points");

                // Clear previous Firebase listener.
                if (firebaseCircles != null && firebaseEventListener != null){

                    firebaseCircles.removeEventListener(firebaseEventListener);
                }
                mMap.clear();
                // Set circle to null so changing circleSizeSeekBar will create a circle.
                circle = null;
                // Set progress to 0, as no circle exists.
                circleSizeSeekBar.setProgress(0);
                // If the largerCircleSizeSeekBar is visible, set it to View.GONE and make the original one visible (as mMap.clear() is called so no circles exist).
                if (largerCircleSizeSeekBar.getVisibility() != View.GONE) {

                    largerCircleSizeSeekBar.setVisibility(View.GONE);
                    circleSizeSeekBar.setVisibility(View.VISIBLE);
                    circleSizeSeekBar.setEnabled(true);
                    largerCircleSizeSeekBar.setOnSeekBarChangeListener(null);
                }
                // Load Firebase circles.
                firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot ds : dataSnapshot.getChildren()) {

                            if (dataSnapshot.getValue() != null) {

                                // Only load "points" (radius == 1)
                                if ((double) (long)ds.child("circleOptions/radius").getValue() == 1) {

                                    LatLng center = new LatLng((Double) ds.child("circleOptions/center/latitude/").getValue(), (Double) ds.child("circleOptions/center/longitude/").getValue());
                                    int fillColor = (int) (long) ds.child("circleOptions/fillColor").getValue();
                                    double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                    Circle circle = mMap.addCircle(
                                            new CircleOptions()
                                                    .center(center)
                                                    .clickable(true)
                                                    .fillColor(fillColor)
                                                    .radius(radius)
                                                    .strokeColor(Color.YELLOW)
                                                    .strokeWidth(3f)
                                    );

                                    // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                    uuid = (String) ds.child("uuic").getValue();

                                    circle.setTag(uuid);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                circleViewsMenuIsOpen = false;
                return true;

            // largercircle_seekbar_menu
            case R.id.largerCircle:

                Log.i(TAG, "onMenuItemClick() -> largerCircle");

                largerCircleSeekBar();
                circleSizeSeekBar.setEnabled(false);
                largerCircleSizeSeekBar.setProgress(0);
                // Ensures the popup is closed when the progress is set to 0.
                if (popupCreateSmallerCircles != null) {

                    popupCreateSmallerCircles.dismiss();
                }
                largerCirclesMenuIsOpen = false;
                return true;

            // smallercircle_seekbar_menu
            case R.id.smallerCircle:

                Log.i(TAG, "onMenuItemClick() -> smallerCircle");

                largerCircleSizeSeekBar.setVisibility(View.GONE);
                circleSizeSeekBar.setVisibility(View.VISIBLE);
                circleSizeSeekBar.setEnabled(true);
                // Remove the larger seekBar listener.
                if (largerCircleSizeSeekBar != null) {

                    largerCircleSizeSeekBar.setOnSeekBarChangeListener(null);
                }
                smallerCirclesMenuIsOpen = false;
                return true;

            default:
                return false;
        }
    }

    protected void largerCircleSeekBar() {

        Log.i(TAG, "largerCircleSeekBar()");

        circleSizeSeekBar.setVisibility(View.GONE);
        largerCircleSizeSeekBar.setVisibility(View.VISIBLE);

        largerCircleSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                // Changes size of the circle.
                if (circle != null) {

                    circle.setRadius(progress + 100);
                }

                // Creates popup to change the largerCircleSizeSeekBar back to the circleSizeSeekBar.
                if (largerCircleSizeSeekBar.getProgress() == 0) {

                    popupCreateSmallerCircles = new PopupMenu(Map.this, largerCircleSizeSeekBar);
                    popupCreateSmallerCircles.setOnMenuItemClickListener(Map.this);
                    popupCreateSmallerCircles.inflate(R.menu.smallercircle_seekbar_menu);
                    popupCreateSmallerCircles.show();
                    smallerCirclesMenuIsOpen = true;

                    // Changes boolean value (used in OnConfigurationChanged) to determine whether menu is currently open.
                    popupCreateSmallerCircles.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu popupMenu) {

                            smallerCirclesMenuIsOpen = false;
                            popupCreateSmallerCircles.setOnDismissListener(null);
                        }
                    });
                }

                // Ensures the popup closes.
                if ( (popupCreateSmallerCircles != null) && (largerCircleSizeSeekBar.getProgress() != 0) ) {

                    popupCreateSmallerCircles.dismiss();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

                // Creates circle.
                if (circle == null) {

                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                @Override
                                public void onSuccess(Location location) {

                                    // Get last known location. In some rare situations, this can be null.
                                    if (location != null) {

                                        // Make circle the size set by the seekBar.
                                        int circleSize = 100;

                                        // Logic to handle location object.
                                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                        CircleOptions circleOptions =
                                                new CircleOptions()
                                                        .center(latLng)
                                                        .clickable(true)
                                                        .fillColor(Color.argb(70, 255, 215, 0))
                                                        .radius(circleSize)
                                                        .strokeColor(Color.YELLOW)
                                                        .strokeWidth(3f);

                                        if (circle != null) {

                                            circle.remove();
                                        }

                                        circle = mMap.addCircle(circleOptions);
                                        largerCircleSizeSeekBar.setProgress(0);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
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