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
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.common.api.GoogleApiClient;
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

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private static final int Request_User_Location_Code = 99;
    boolean firstLoad = true;
    //private static SeekBar seekBar;
    private Circle circle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Button circleButton = findViewById(R.id.circleButton);
        checkLocationPermission();

        circleButton.setOnClickListener(new View.OnClickListener() {

            // Makes circle around current location on button press
            public void onClick(View v) {

                //TODO: Add onSeekBarListener to change the size of circle when a user creates a circle.
                //TODO: Add background to seekBar to see it better.
                //TODO: Change the look of the circleButton.
                //TODO: Add circleInformation to firebase after changing size and entering the chatCircle.
                //TODO: Make sure the circle button shows the previously created circle after entering and exiting the chatCircle rather than deleting it and creating a new chatCircle.

                checkLocationPermission();

                FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(MapsActivity.this);

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(MapsActivity.this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {

                                // Got last known location. In some rare situations, this can be null.
                                if (location != null) {

                                    // Logic to handle location object
                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    CircleOptions circleOptions =
                                            new CircleOptions()
                                                    .center(latLng)
                                                    .radius(40)
                                                    .clickable(true)
                                                    .strokeWidth(3f)
                                                    .strokeColor(Color.BLUE)
                                                    .fillColor(Color.argb(70, 50, 50, 100));

                                    if (circle != null){
                                        circle.remove();
                                    }
                                    circle = mMap.addCircle(circleOptions);
                                    //CircleInformation circleInformation = new CircleInformation();
                                    //circleInformation.setCircle(circle);
                                    //DatabaseReference newFirebaseCircle = FirebaseDatabase.getInstance().getReference().child("circles").push();
                                    //newFirebaseCircle.setValue(circleInformation);
                                }
                            }
                        });
            }
        });


        // Check if GPS is enabled.
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }

        startLocationUpdates();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.activity_maps);
        mapFragment.getMapAsync(this);

        //circleButton.setOnClickListener(this);

        /**
         * This method will be invoked any time the data on the database changes.
         * Additionally, it will be invoked as soon as we connect the listener, so that we can get an initial snapshot of the data on the database.
         * @param dataSnapshot
         */
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference();
        databaseReference.child("circles").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                // get all of the children at this level
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();

                // shake hands with all of them.
                for (DataSnapshot ds : children) {

                    LatLng center = new LatLng ((Double) ds.child("circle/center/latitude/").getValue(),(Double) ds.child("circle/center/longitude/").getValue());
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
                    ); circle.setTag(id);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void buildAlertMessageNoGps() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled and GPS is required. Do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        ActivityCompat.finishAffinity(MapsActivity.this);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void checkLocationPermission() {

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

        // If request is cancelled, the result arrays are empty.
        if (requestCode == Request_User_Location_Code) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay! Do the
                // location-related task you need to do.
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                    String provider = LocationManager.NETWORK_PROVIDER;
                    //Request location updates:
                    locationManager.requestLocationUpdates(provider, 400, 1, this);
                }

            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                ActivityCompat.finishAffinity(this);

            }
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (ContextCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            String provider = LocationManager.NETWORK_PROVIDER;
            locationManager.requestLocationUpdates(provider, 400, 1, this);
        }
        else{

            checkLocationPermission();
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        if (ContextCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(this);
        }
    }

    protected void startLocationUpdates(){

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

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {

                        // do work here
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

        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
        }
        //TODO: extract all (visible) circle data and rebuild them when the map loads.

        // Does something when clicking on the circle
        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {

            @Override
            public void onCircleClick(Circle circle) {

                    // Checks if user is already signed in.
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                        // User is signed in.
                        String circleID = (String) circle.getTag();
                        Intent Activity = new Intent(MapsActivity.this, Chat.class);
                        Activity.putExtra("circleID", circleID);
                        startActivity(Activity);
                    } else {

                        // No user is signed in.
                        //TODO: Do the above here.
                        startActivity(new Intent(MapsActivity.this, signIn.class));
                    }
                }
        });
    }

    @Override
    public void onLocationChanged(Location location) {

        // Zoom to the user's location the first time the map is loaded.
        if ((location != null) && (firstLoad)) {

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                    .zoom(18)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera
                    .tilt(0)                   // Sets the tilt of the camera
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 4000, null);
            firstLoad = false;
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

