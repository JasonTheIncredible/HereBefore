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
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import androidx.core.content.ContextCompat;

import android.util.Log;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// https://googlemaps.github.io/android-maps-utils/javadoc/com/google/maps/android/PolyUtil.html
import com.google.maps.android.PolyUtil;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class Map extends FragmentActivity implements
        OnMapReadyCallback,
        LocationListener,
        PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "Map";
    private GoogleMap mMap;
    private static final int Request_User_Location_Code = 99;
    private Marker marker0, marker1, marker2, marker3, marker4, marker5, marker6, marker7;
    private Circle circle;
    private Polygon polygon;
    private DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference(), firebaseCircles = rootRef.child("circles"), firebasePolygons = rootRef.child("polygons");
    private SeekBar chatSizeSeekBar;
    private String uuid, marker0ID, marker1ID, marker2ID, marker3ID, marker4ID, marker5ID, marker6ID, marker7ID;
    private Button createChatButton, chatViewsButton, mapTypeButton;
    private PopupMenu popupMapType, popupChatViews, popupCreateChat;
    private Boolean mapTypeMenuIsOpen = false, chatViewsMenuIsOpen = false, createChatMenuIsOpen = false, usedSeekBar = false, userIsWithinShape, firstLoad = true, threeMarkers = false, fourMarkers = false, fiveMarkers = false, sixMarkers = false, sevenMarkers = false, eightMarkers = false;
    private LatLng marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position;
    private Double relativeAngle = 0.0;
    private Location mlocation;
    private List<LatLng> polygonPointsList;

    //TODO: Have chatViewsButton show polygons.
    //TODO Prevent map type reload with orientation change.
    //TODO: Prevent circle overlap.
    //TODO: Give user option to change color of the circles (or just change it outright).
    //TODO: Make points easier to see somehow.
    //TODO: Have circles spread if they are too close when clicking.
    //TODO: Only load Firebase circles if they're within camera view (in onMapReady) (getMap().getProjection().getVisibleRegion().latLangBounds). If this works, can possibly replace singleValueEventListener in onMapReady() and onRestart() with a valueEventListener.
    //TODO: Make sure Firebase listener is always updating map properly.
    //TODO: Optimize Firebase loading.
    //TODO: Too much work on main thread.
    //TODO: Change map type on different thread - check without wifi and maybe load map type in background or only if internet is good enough.
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
        createChatButton = findViewById(R.id.createChatButton);
        chatSizeSeekBar = findViewById(R.id.chatSizeSeekBar);
        chatViewsButton = findViewById(R.id.chatViewsButton);
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
        chatViewsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                popupChatViews = new PopupMenu(Map.this, chatViewsButton);
                popupChatViews.setOnMenuItemClickListener(Map.this);
                popupChatViews.inflate(R.menu.chatviews_menu);
                popupChatViews.show();
                chatViewsMenuIsOpen = true;

                // Changes boolean value (used in OnConfigurationChanged) to determine whether menu is currently open.
                popupChatViews.setOnDismissListener(new PopupMenu.OnDismissListener(){
                    @Override
                    public void onDismiss(PopupMenu popupMenu) {

                        chatViewsMenuIsOpen = false;
                        popupChatViews.setOnDismissListener(null);
                    }
                });
            }
        });

        // Shows a menu for creating chats.
        createChatButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                popupCreateChat = new PopupMenu(Map.this, createChatButton);
                popupCreateChat.setOnMenuItemClickListener(Map.this);
                popupCreateChat.inflate(R.menu.createchat_menu);
                // Check if the circle exists and adjust the menu items accordingly.
                if (circle != null || polygon != null) {

                    popupCreateChat.getMenu().findItem(R.id.createPolygon).setVisible(false);
                    popupCreateChat.getMenu().findItem(R.id.createCircle).setVisible(false);
                } else{

                    popupCreateChat.getMenu().findItem(R.id.removeShape).setVisible(false);
                }
                popupCreateChat.show();
                createChatMenuIsOpen = true;

                // Changes boolean value (used in OnConfigurationChanged) to determine whether menu is currently open.
                popupCreateChat.setOnDismissListener(new PopupMenu.OnDismissListener() {
                    @Override
                    public void onDismiss(PopupMenu popupMenu) {

                        createChatMenuIsOpen = false;
                        popupCreateChat.setOnDismissListener(null);
                    }
                });
            }
        });

        chatSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {

                // Global variable used to prevent conflicts when the user updates the circle's radius with the marker rather than the seekBar.
                usedSeekBar = true;

                // Creates circle with markers.
                if (circle == null && polygon == null) {

                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                @Override
                                public void onSuccess(Location location) {

                                    // Get last known location. In some rare situations, this can be null.
                                    if (location != null) {

                                        Log.i(TAG, "chatSizeSeekBar -> onStartTrackingTouch -> circle");

                                        // Make circle the size set by the seekBar.
                                        int circleSize = chatSizeSeekBar.getProgress();

                                        // Logic to handle location object.
                                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                        marker1Position = new LatLng(latLng.latitude + (circleSize / 6371000) * (180 / Math.PI), latLng.longitude + (circleSize / 6371000) * (180 / Math.PI) / cos(latLng.latitude * Math.PI / 180));
                                        CircleOptions circleOptions =
                                                new CircleOptions()
                                                        .center(latLng)
                                                        .clickable(true)
                                                        .fillColor(0)
                                                        .radius(circleSize)
                                                        .strokeColor(Color.YELLOW)
                                                        .strokeWidth(3f);

                                        // Create a marker in the center of the circle to allow for dragging.
                                        MarkerOptions markerOptionsCenter = new MarkerOptions()
                                                .position(latLng)
                                                .draggable(true);

                                        // Create marker at the edge of the circle to allow for changing of the circle's radius.
                                        MarkerOptions markerOptionsEdge = new MarkerOptions()
                                                .position(marker1Position)
                                                .draggable(true);

                                        marker0 = mMap.addMarker(markerOptionsCenter);
                                        marker1 = mMap.addMarker(markerOptionsEdge);

                                        // Update the global variable to compare with the marker the user clicks on during the dragging process.
                                        marker0ID = marker0.getId();
                                        marker1ID = marker1.getId();

                                        marker1.setVisible(false);

                                        circle = mMap.addCircle(circleOptions);
                                    }
                                }
                            });
                }
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                // Global variable used to prevent conflicts when the user updates the circle's radius with the marker rather than the seekBar.
                if (usedSeekBar) {

                    // Changes size of the circle, marker1 visibility, and fill color.
                    if (circle != null) {

                        Log.i(TAG, "chatSizeSeekBar -> onProgressChanged -> circle");

                        circle.setRadius(progress);
                        marker1.setVisible(false);
                        circle.setFillColor(0);
                    }

                    if (polygon != null) {

                        // Get last known location. In some rare situations, this can be null.
                        if (mlocation != null) {

                            // 3 markers.
                            if (chatSizeSeekBar.getProgress() <= 33 && !threeMarkers) {

                                Log.i(TAG, "chatSizeSeekBar -> onProgressChanged -> polygon -> 3 markers");

                                polygon.remove();
                                marker0.remove();
                                marker1.remove();
                                marker2.remove();
                                polygon = null;
                                marker0 = null;
                                marker1 = null;
                                marker2 = null;
                                marker0Position = null;
                                marker1Position = null;
                                marker2Position = null;
                                marker0ID = null;
                                marker1ID = null;
                                marker2ID = null;

                                if (marker3 != null) {

                                    marker3.remove();
                                    marker3 = null;
                                    marker3Position = null;
                                    marker3ID = null;
                                }

                                if (marker4 != null) {

                                    marker4.remove();
                                    marker4 = null;
                                    marker4Position = null;
                                    marker4ID = null;
                                }

                                if (marker5 != null) {

                                    marker5.remove();
                                    marker5 = null;
                                    marker5Position = null;
                                    marker5ID = null;
                                }

                                if (marker6 != null) {

                                    marker6.remove();
                                    marker6 = null;
                                    marker6Position = null;
                                    marker6ID = null;
                                }

                                if (marker7 != null) {

                                    marker7.remove();
                                    marker7 = null;
                                    marker7Position = null;
                                    marker7ID = null;
                                }

                                // Update any Boolean.
                                threeMarkers = true;
                                fourMarkers = false;
                                fiveMarkers = false;
                                sixMarkers = false;
                                sevenMarkers = false;
                                eightMarkers = false;

                                // Logic to handle location object.
                                marker0Position = new LatLng(mlocation.getLatitude() + 0.0001, mlocation.getLongitude());
                                marker2Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() + 0.0001);
                                marker1Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() - 0.0001);

                                PolygonOptions polygonOptions =
                                        new PolygonOptions()
                                                .add(marker0Position, marker1Position, marker2Position)
                                                .clickable(true)
                                                .fillColor(Color.argb(70, 255, 215, 0))
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f);

                                // Create markers when creating the polygon to allow for dragging of the center and vertices.
                                MarkerOptions markerOptions0 = new MarkerOptions()
                                        .position(marker0Position)
                                        .draggable(true);

                                MarkerOptions markerOptions1 = new MarkerOptions()
                                        .position(marker1Position)
                                        .draggable(true);

                                MarkerOptions markerOptions2 = new MarkerOptions()
                                        .position(marker2Position)
                                        .draggable(true);

                                marker0 = mMap.addMarker(markerOptions0);
                                marker1 = mMap.addMarker(markerOptions1);
                                marker2 = mMap.addMarker(markerOptions2);

                                // Update the global variable to compare with the marker the user clicks on during the dragging process.
                                marker0ID = marker0.getId();
                                marker1ID = marker1.getId();
                                marker2ID = marker2.getId();

                                polygon = mMap.addPolygon(polygonOptions);
                            }

                            // 4 markers.
                            if (chatSizeSeekBar.getProgress() > 33 && chatSizeSeekBar.getProgress() <= 66 && !fourMarkers) {

                                Log.i(TAG, "chatSizeSeekBar -> onProgressChanged -> polygon -> 4 markers");

                                polygon.remove();
                                marker0.remove();
                                marker1.remove();
                                marker2.remove();
                                polygon = null;
                                marker0 = null;
                                marker1 = null;
                                marker2 = null;
                                marker0Position = null;
                                marker1Position = null;
                                marker2Position = null;
                                marker0ID = null;
                                marker1ID = null;
                                marker2ID = null;

                                if (marker3 != null) {

                                    marker3.remove();
                                    marker3 = null;
                                    marker3Position = null;
                                    marker3ID = null;
                                }

                                if (marker4 != null) {

                                    marker4.remove();
                                    marker4 = null;
                                    marker4Position = null;
                                    marker4ID = null;
                                }

                                if (marker5 != null) {

                                    marker5.remove();
                                    marker5 = null;
                                    marker5Position = null;
                                    marker5ID = null;
                                }

                                if (marker6 != null) {

                                    marker6.remove();
                                    marker6 = null;
                                    marker6Position = null;
                                    marker6ID = null;
                                }

                                if (marker7 != null) {

                                    marker7.remove();
                                    marker7 = null;
                                    marker7Position = null;
                                    marker7ID = null;
                                }

                                // Update any Boolean.
                                fourMarkers = true;
                                threeMarkers = false;
                                fiveMarkers = false;
                                sixMarkers = false;
                                sevenMarkers = false;
                                eightMarkers = false;

                                // Logic to handle location object.
                                marker0Position = new LatLng(mlocation.getLatitude() + 0.0001, mlocation.getLongitude());
                                marker1Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() + 0.0001);
                                marker2Position = new LatLng(mlocation.getLatitude() - 0.0001, mlocation.getLongitude());
                                marker3Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() - 0.0001);

                                PolygonOptions polygonOptions =
                                        new PolygonOptions()
                                                .add(marker0Position, marker1Position, marker2Position, marker3Position)
                                                .clickable(true)
                                                .fillColor(Color.argb(70, 255, 215, 0))
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f);

                                // Create markers when creating the polygon to allow for dragging of the center and vertices.
                                MarkerOptions markerOptions0 = new MarkerOptions()
                                        .position(marker0Position)
                                        .draggable(true);

                                MarkerOptions markerOptions1 = new MarkerOptions()
                                        .position(marker1Position)
                                        .draggable(true);

                                MarkerOptions markerOptions2 = new MarkerOptions()
                                        .position(marker2Position)
                                        .draggable(true);

                                MarkerOptions markerOptions3 = new MarkerOptions()
                                        .position(marker3Position)
                                        .draggable(true);

                                marker0 = mMap.addMarker(markerOptions0);
                                marker1 = mMap.addMarker(markerOptions1);
                                marker2 = mMap.addMarker(markerOptions2);
                                marker3 = mMap.addMarker(markerOptions3);

                                // Update the global variable to compare with the marker the user clicks on during the dragging process.
                                marker0ID = marker0.getId();
                                marker1ID = marker1.getId();
                                marker2ID = marker2.getId();
                                marker3ID = marker3.getId();

                                polygon = mMap.addPolygon(polygonOptions);
                            }

                            // 5 markers.
                            if (chatSizeSeekBar.getProgress() > 66 && chatSizeSeekBar.getProgress() <= 99 && !fiveMarkers) {

                                Log.i(TAG, "chatSizeSeekBar -> onProgressChanged -> polygon -> 5 markers");

                                polygon.remove();
                                marker0.remove();
                                marker1.remove();
                                marker2.remove();
                                polygon = null;
                                marker0 = null;
                                marker1 = null;
                                marker2 = null;
                                marker0Position = null;
                                marker1Position = null;
                                marker2Position = null;
                                marker0ID = null;
                                marker1ID = null;
                                marker2ID = null;

                                if (marker3 != null) {

                                    marker3.remove();
                                    marker3 = null;
                                    marker3Position = null;
                                    marker3ID = null;
                                }

                                if (marker4 != null) {

                                    marker4.remove();
                                    marker4 = null;
                                    marker4Position = null;
                                    marker4ID = null;
                                }

                                if (marker5 != null) {

                                    marker5.remove();
                                    marker5 = null;
                                    marker5Position = null;
                                    marker5ID = null;
                                }

                                if (marker6 != null) {

                                    marker6.remove();
                                    marker6 = null;
                                    marker6Position = null;
                                    marker6ID = null;
                                }

                                if (marker7 != null) {

                                    marker7.remove();
                                    marker7 = null;
                                    marker7Position = null;
                                    marker7ID = null;
                                }

                                // Update any Boolean.
                                fiveMarkers = true;
                                fourMarkers = false;
                                threeMarkers = false;
                                sixMarkers = false;
                                sevenMarkers = false;
                                eightMarkers = false;

                                // Logic to handle location object.
                                marker0Position = new LatLng(mlocation.getLatitude() + 0.0001, mlocation.getLongitude());
                                marker1Position = new LatLng(mlocation.getLatitude() + 0.0001, mlocation.getLongitude() + 0.0001);
                                marker2Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() + 0.0001);
                                marker3Position = new LatLng(mlocation.getLatitude() - 0.0001, mlocation.getLongitude());
                                marker4Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() - 0.0001);

                                PolygonOptions polygonOptions =
                                        new PolygonOptions()
                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position)
                                                .clickable(true)
                                                .fillColor(Color.argb(70, 255, 215, 0))
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f);

                                // Create markers when creating the polygon to allow for dragging of the center and vertices.
                                MarkerOptions markerOptions0 = new MarkerOptions()
                                        .position(marker0Position)
                                        .draggable(true);

                                MarkerOptions markerOptions1 = new MarkerOptions()
                                        .position(marker1Position)
                                        .draggable(true);

                                MarkerOptions markerOptions2 = new MarkerOptions()
                                        .position(marker2Position)
                                        .draggable(true);

                                MarkerOptions markerOptions3 = new MarkerOptions()
                                        .position(marker3Position)
                                        .draggable(true);

                                MarkerOptions markerOptions4 = new MarkerOptions()
                                        .position(marker4Position)
                                        .draggable(true);

                                marker0 = mMap.addMarker(markerOptions0);
                                marker1 = mMap.addMarker(markerOptions1);
                                marker2 = mMap.addMarker(markerOptions2);
                                marker3 = mMap.addMarker(markerOptions3);
                                marker4 = mMap.addMarker(markerOptions4);

                                // Update the global variable to compare with the marker the user clicks on during the dragging process.
                                marker0ID = marker0.getId();
                                marker1ID = marker1.getId();
                                marker2ID = marker2.getId();
                                marker3ID = marker3.getId();
                                marker4ID = marker4.getId();

                                polygon = mMap.addPolygon(polygonOptions);
                            }

                            // 6 markers.
                            if (chatSizeSeekBar.getProgress() > 99 && chatSizeSeekBar.getProgress() <= 132 && !sixMarkers) {

                                Log.i(TAG, "chatSizeSeekBar -> onProgressChanged -> polygon -> 6 markers");

                                polygon.remove();
                                marker0.remove();
                                marker1.remove();
                                marker2.remove();
                                polygon = null;
                                marker0 = null;
                                marker1 = null;
                                marker2 = null;
                                marker0Position = null;
                                marker1Position = null;
                                marker2Position = null;
                                marker0ID = null;
                                marker1ID = null;
                                marker2ID = null;

                                if (marker3 != null) {

                                    marker3.remove();
                                    marker3 = null;
                                    marker3Position = null;
                                    marker3ID = null;
                                }

                                if (marker4 != null) {

                                    marker4.remove();
                                    marker4 = null;
                                    marker4Position = null;
                                    marker4ID = null;
                                }

                                if (marker5 != null) {

                                    marker5.remove();
                                    marker5 = null;
                                    marker5Position = null;
                                    marker5ID = null;
                                }

                                if (marker6 != null) {

                                    marker6.remove();
                                    marker6 = null;
                                    marker6Position = null;
                                    marker6ID = null;
                                }

                                if (marker7 != null) {

                                    marker7.remove();
                                    marker7 = null;
                                    marker7Position = null;
                                    marker7ID = null;
                                }

                                // Update any Boolean.
                                sixMarkers = true;
                                threeMarkers = false;
                                fourMarkers = false;
                                fiveMarkers = false;
                                sevenMarkers = false;
                                eightMarkers = false;

                                // Logic to handle location object.
                                marker0Position = new LatLng(mlocation.getLatitude() + 0.0001, mlocation.getLongitude());
                                marker1Position = new LatLng(mlocation.getLatitude() + 0.0001, mlocation.getLongitude() + 0.0001);
                                marker2Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() + 0.0001);
                                marker3Position = new LatLng(mlocation.getLatitude() - 0.0001, mlocation.getLongitude() + 0.0001);
                                marker4Position = new LatLng(mlocation.getLatitude() - 0.0001, mlocation.getLongitude());
                                marker5Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() - 0.0001);

                                PolygonOptions polygonOptions =
                                        new PolygonOptions()
                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position)
                                                .clickable(true)
                                                .fillColor(Color.argb(70, 255, 215, 0))
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f);

                                // Create markers when creating the polygon to allow for dragging of the center and vertices.
                                MarkerOptions markerOptions0 = new MarkerOptions()
                                        .position(marker0Position)
                                        .draggable(true);

                                MarkerOptions markerOptions1 = new MarkerOptions()
                                        .position(marker1Position)
                                        .draggable(true);

                                MarkerOptions markerOptions2 = new MarkerOptions()
                                        .position(marker2Position)
                                        .draggable(true);

                                MarkerOptions markerOptions3 = new MarkerOptions()
                                        .position(marker3Position)
                                        .draggable(true);

                                MarkerOptions markerOptions4 = new MarkerOptions()
                                        .position(marker4Position)
                                        .draggable(true);

                                MarkerOptions markerOptions5 = new MarkerOptions()
                                        .position(marker5Position)
                                        .draggable(true);

                                marker0 = mMap.addMarker(markerOptions0);
                                marker1 = mMap.addMarker(markerOptions1);
                                marker2 = mMap.addMarker(markerOptions2);
                                marker3 = mMap.addMarker(markerOptions3);
                                marker4 = mMap.addMarker(markerOptions4);
                                marker5 = mMap.addMarker(markerOptions5);

                                // Update the global variable to compare with the marker the user clicks on during the dragging process.
                                marker0ID = marker0.getId();
                                marker1ID = marker1.getId();
                                marker2ID = marker2.getId();
                                marker3ID = marker3.getId();
                                marker4ID = marker4.getId();
                                marker5ID = marker5.getId();

                                polygon = mMap.addPolygon(polygonOptions);
                            }

                            // 7 markers.
                            if (chatSizeSeekBar.getProgress() > 132 && chatSizeSeekBar.getProgress() <= 165 && !sevenMarkers) {

                                Log.i(TAG, "chatSizeSeekBar -> onProgressChanged -> polygon -> 7 markers");

                                polygon.remove();
                                marker0.remove();
                                marker1.remove();
                                marker2.remove();
                                polygon = null;
                                marker0 = null;
                                marker1 = null;
                                marker2 = null;
                                marker0Position = null;
                                marker1Position = null;
                                marker2Position = null;
                                marker0ID = null;
                                marker1ID = null;
                                marker2ID = null;

                                if (marker3 != null) {

                                    marker3.remove();
                                    marker3 = null;
                                    marker3Position = null;
                                    marker3ID = null;
                                }

                                if (marker4 != null) {

                                    marker4.remove();
                                    marker4 = null;
                                    marker4Position = null;
                                    marker4ID = null;
                                }

                                if (marker5 != null) {

                                    marker5.remove();
                                    marker5 = null;
                                    marker5Position = null;
                                    marker5ID = null;
                                }

                                if (marker6 != null) {

                                    marker6.remove();
                                    marker6 = null;
                                    marker6Position = null;
                                    marker6ID = null;
                                }

                                if (marker7 != null) {

                                    marker7.remove();
                                    marker7 = null;
                                    marker7Position = null;
                                    marker7ID = null;
                                }

                                // Update any Boolean.
                                sevenMarkers = true;
                                threeMarkers = false;
                                fourMarkers = false;
                                fiveMarkers = false;
                                sixMarkers = false;
                                eightMarkers = false;

                                // Logic to handle location object.
                                marker0Position = new LatLng(mlocation.getLatitude() + 0.0001, mlocation.getLongitude());
                                marker1Position = new LatLng(mlocation.getLatitude() + 0.0001, mlocation.getLongitude() + 0.0001);
                                marker2Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() + 0.0001);
                                marker3Position = new LatLng(mlocation.getLatitude() - 0.0001, mlocation.getLongitude() + 0.0001);
                                marker4Position = new LatLng(mlocation.getLatitude() - 0.0001, mlocation.getLongitude());
                                marker5Position = new LatLng(mlocation.getLatitude() - 0.0001, mlocation.getLongitude() - 0.0001);
                                marker6Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() - 0.0001);

                                PolygonOptions polygonOptions =
                                        new PolygonOptions()
                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position)
                                                .clickable(true)
                                                .fillColor(Color.argb(70, 255, 215, 0))
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f);

                                // Create markers when creating the polygon to allow for dragging of the center and vertices.
                                MarkerOptions markerOptions0 = new MarkerOptions()
                                        .position(marker0Position)
                                        .draggable(true);

                                MarkerOptions markerOptions1 = new MarkerOptions()
                                        .position(marker1Position)
                                        .draggable(true);

                                MarkerOptions markerOptions2 = new MarkerOptions()
                                        .position(marker2Position)
                                        .draggable(true);

                                MarkerOptions markerOptions3 = new MarkerOptions()
                                        .position(marker3Position)
                                        .draggable(true);

                                MarkerOptions markerOptions4 = new MarkerOptions()
                                        .position(marker4Position)
                                        .draggable(true);

                                MarkerOptions markerOptions5 = new MarkerOptions()
                                        .position(marker5Position)
                                        .draggable(true);

                                MarkerOptions markerOptions6 = new MarkerOptions()
                                        .position(marker6Position)
                                        .draggable(true);

                                marker0 = mMap.addMarker(markerOptions0);
                                marker1 = mMap.addMarker(markerOptions1);
                                marker2 = mMap.addMarker(markerOptions2);
                                marker3 = mMap.addMarker(markerOptions3);
                                marker4 = mMap.addMarker(markerOptions4);
                                marker5 = mMap.addMarker(markerOptions5);
                                marker6 = mMap.addMarker(markerOptions6);

                                // Update the global variable to compare with the marker the user clicks on during the dragging process.
                                marker0ID = marker0.getId();
                                marker1ID = marker1.getId();
                                marker2ID = marker2.getId();
                                marker3ID = marker3.getId();
                                marker4ID = marker4.getId();
                                marker5ID = marker5.getId();
                                marker6ID = marker6.getId();

                                polygon = mMap.addPolygon(polygonOptions);
                            }

                            // 8 markers.
                            if (chatSizeSeekBar.getProgress() > 165 && chatSizeSeekBar.getProgress() <= 200 && !eightMarkers) {

                                Log.i(TAG, "chatSizeSeekBar -> onProgressChanged -> polygon -> 8 markers");

                                polygon.remove();
                                marker0.remove();
                                marker1.remove();
                                marker2.remove();
                                polygon = null;
                                marker0 = null;
                                marker1 = null;
                                marker2 = null;
                                marker0Position = null;
                                marker1Position = null;
                                marker2Position = null;
                                marker0ID = null;
                                marker1ID = null;
                                marker2ID = null;

                                if (marker3 != null) {

                                    marker3.remove();
                                    marker3 = null;
                                    marker3Position = null;
                                    marker3ID = null;
                                }

                                if (marker4 != null) {

                                    marker4.remove();
                                    marker4 = null;
                                    marker4Position = null;
                                    marker4ID = null;
                                }

                                if (marker5 != null) {

                                    marker5.remove();
                                    marker5 = null;
                                    marker5Position = null;
                                    marker5ID = null;
                                }

                                if (marker6 != null) {

                                    marker6.remove();
                                    marker6 = null;
                                    marker6Position = null;
                                    marker6ID = null;
                                }

                                if (marker7 != null) {

                                    marker7.remove();
                                    marker7 = null;
                                    marker7Position = null;
                                    marker7ID = null;
                                }

                                // Update any Boolean.
                                eightMarkers = true;
                                threeMarkers = false;
                                fourMarkers = false;
                                fiveMarkers = false;
                                sixMarkers = false;
                                sevenMarkers = false;

                                // Logic to handle location object.
                                marker0Position = new LatLng(mlocation.getLatitude() + 0.0001, mlocation.getLongitude());
                                marker1Position = new LatLng(mlocation.getLatitude() + 0.0001, mlocation.getLongitude() + 0.0001);
                                marker2Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() + 0.0001);
                                marker3Position = new LatLng(mlocation.getLatitude() - 0.0001, mlocation.getLongitude() + 0.0001);
                                marker4Position = new LatLng(mlocation.getLatitude() - 0.0001, mlocation.getLongitude());
                                marker5Position = new LatLng(mlocation.getLatitude() - 0.0001, mlocation.getLongitude() - 0.0001);
                                marker6Position = new LatLng(mlocation.getLatitude(), mlocation.getLongitude() - 0.0001);
                                marker7Position = new LatLng(mlocation.getLatitude() + 0.0001, mlocation.getLongitude() - 0.0001);

                                PolygonOptions polygonOptions =
                                        new PolygonOptions()
                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position)
                                                .clickable(true)
                                                .fillColor(Color.argb(70, 255, 215, 0))
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f);

                                // Create markers when creating the polygon to allow for dragging of the center and vertices.
                                MarkerOptions markerOptions0 = new MarkerOptions()
                                        .position(marker0Position)
                                        .draggable(true);

                                MarkerOptions markerOptions1 = new MarkerOptions()
                                        .position(marker1Position)
                                        .draggable(true);

                                MarkerOptions markerOptions2 = new MarkerOptions()
                                        .position(marker2Position)
                                        .draggable(true);

                                MarkerOptions markerOptions3 = new MarkerOptions()
                                        .position(marker3Position)
                                        .draggable(true);

                                MarkerOptions markerOptions4 = new MarkerOptions()
                                        .position(marker4Position)
                                        .draggable(true);

                                MarkerOptions markerOptions5 = new MarkerOptions()
                                        .position(marker5Position)
                                        .draggable(true);

                                MarkerOptions markerOptions6 = new MarkerOptions()
                                        .position(marker6Position)
                                        .draggable(true);

                                MarkerOptions markerOptions7 = new MarkerOptions()
                                        .position(marker7Position)
                                        .draggable(true);

                                marker0 = mMap.addMarker(markerOptions0);
                                marker1 = mMap.addMarker(markerOptions1);
                                marker2 = mMap.addMarker(markerOptions2);
                                marker3 = mMap.addMarker(markerOptions3);
                                marker4 = mMap.addMarker(markerOptions4);
                                marker5 = mMap.addMarker(markerOptions5);
                                marker6 = mMap.addMarker(markerOptions6);
                                marker7 = mMap.addMarker(markerOptions7);

                                // Update the global variable to compare with the marker the user clicks on during the dragging process.
                                marker0ID = marker0.getId();
                                marker1ID = marker1.getId();
                                marker2ID = marker2.getId();
                                marker3ID = marker3.getId();
                                marker4ID = marker4.getId();
                                marker5ID = marker5.getId();
                                marker6ID = marker6.getId();
                                marker7ID = marker7.getId();

                                polygon = mMap.addPolygon(polygonOptions);
                            }
                        }
                    }
                }
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {

                // Global variable used to prevent conflicts when the user updates the circle's radius with the marker rather than the seekBar.
                usedSeekBar = false;

                // Sets fill color, sets marker1's position on the circle's edge relative to where the user last left it, and sets marker1's visibility.
                if (circle != null) {

                    Log.i(TAG, "chatSizeSeekBar -> onStopTrackingTouch -> circle");

                    circle.setFillColor(Color.argb(70, 255, 215, 0));

                    marker1.setPosition(latLngGivenDistance(circle.getCenter().latitude, circle.getCenter().longitude, chatSizeSeekBar.getProgress(), relativeAngle));

                    marker1.setVisible(true);
                }

                if (polygon != null) {

                    Log.i(TAG, "chatSizeSeekBar -> onStopTrackingTouch -> polygon");
                }
            }
        });
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        // Clear map before adding new Firebase circles in onStart() to prevent overlap.
        // Set shape to null so changing chatSizeSeekBar in onStart() will create a circle and createChatButton will reset itself.
        if (mMap != null) {

            // Remove the polygon and markers.
            if (polygon != null) {

                polygon.remove();
                marker0.remove();
                marker1.remove();
                marker2.remove();
                polygon = null;
                marker0 = null;
                marker1 = null;
                marker2 = null;
                marker0Position = null;
                marker1Position = null;
                marker2Position = null;
                marker0ID = null;
                marker1ID = null;
                marker2ID = null;

                if (marker3 != null) {

                    marker3.remove();
                    marker3 = null;
                    marker3Position = null;
                    marker3ID = null;
                }

                if (marker4 != null) {

                    marker4.remove();
                    marker4 = null;
                    marker4Position = null;
                    marker4ID = null;
                }

                if (marker5 != null) {

                    marker5.remove();
                    marker5 = null;
                    marker5Position = null;
                    marker5ID = null;
                }

                if (marker6 != null) {

                    marker6.remove();
                    marker6 = null;
                    marker6Position = null;
                    marker6ID = null;
                }

                if (marker7 != null) {

                    marker7.remove();
                    marker7 = null;
                    marker7Position = null;
                    marker7ID = null;
                }
            }

            // Remove the circle and markers.
            if (circle != null) {

                circle.remove();
                marker0.remove();
                marker1.remove();
                circle = null;
                marker0 = null;
                marker1 = null;
                marker0Position = null;
                marker1Position = null;
                marker0ID = null;
                marker1ID = null;
            }

            mMap.clear();
            chatSizeSeekBar.setProgress(0);
            relativeAngle = 0.0;
            usedSeekBar = false;
            mlocation = null;
            threeMarkers = false;
            fourMarkers = false;
            fiveMarkers = false;
            sixMarkers = false;
            sevenMarkers = false;
            eightMarkers = false;
            polygonPointsList = null;

            // Load Firebase circles, as onMapReady() doesn't get called after onRestart().
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

            // Go to Chat.java after clicking on a circle's middle marker.
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {

                    if (circle != null && marker.getId().equals(marker0ID)) {

                        // Since the circle is new, it will not have a tag, as the tag is pulled from Firebase. Therefore, generate a uuid.
                        uuid = UUID.randomUUID().toString();

                        // Check if the user is already signed in.
                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                            // User is signed in.

                            // Check if user is within the circle before going to the chat.
                            FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                            mFusedLocationClient.getLastLocation()
                                    .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {
                                        @Override
                                        public void onSuccess(Location location) {

                                            if (location != null) {

                                                float[] distance = new float[2];

                                                Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                                        circle.getCenter().latitude, circle.getCenter().longitude, distance);

                                                // Boolean; will be true if user is within the circle upon circle click.
                                                userIsWithinShape = !(distance[0] > circle.getRadius());
                                            }
                                        }
                                    });

                            // If circle.getTag() == null, the circle is new. Therefore, compare it to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                            if (circle.getTag() == null) {

                                firebaseCircles.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        // If the uuid already exists in Firebase, generate another uuid and try again.
                                        if (dataSnapshot.exists()) {

                                            // Generate another UUID and try again.
                                            uuid = UUID.randomUUID().toString();

                                            // Carry the extras all the way to Chat.java.
                                            Intent Activity = new Intent(Map.this, Chat.class);
                                            // Pass this boolean value (true) to Chat.java.
                                            Activity.putExtra("newShape", true);
                                            // Pass this value to Chat.java to identify the shape.
                                            Activity.putExtra("uuid", uuid);
                                            // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                            // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                            Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                            Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                            Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                            Activity.putExtra("radius", circle.getRadius());
                                            startActivity(Activity);
                                        } else {

                                            // Carry the extras all the way to Chat.java.
                                            Intent Activity = new Intent(Map.this, Chat.class);
                                            // Pass this boolean value (true) to Chat.java.
                                            Activity.putExtra("newShape", true);
                                            // Pass this value to Chat.java to identify the shape.
                                            Activity.putExtra("uuid", uuid);
                                            // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                            // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                            Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                            Activity.putExtra("circleLongitude", circle.getCenter().longitude);
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
                                Activity.putExtra("newShape", false);
                                // Pass this value to Chat.java to identify the shape.
                                Activity.putExtra("uuid", uuid);
                                // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
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

                                            // Carry the extras all the way to Chat.java.
                                            Intent Activity = new Intent(Map.this, SignIn.class);
                                            // Pass this boolean value (true) to Chat.java.
                                            Activity.putExtra("newShape", true);
                                            // Pass this value to Chat.java to identify the shape.
                                            Activity.putExtra("uuid", uuid);
                                            // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                            // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                            Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                            Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                            Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                            Activity.putExtra("radius", circle.getRadius());
                                            startActivity(Activity);
                                        } else {

                                            // Carry the extras all the way to Chat.java.
                                            Intent Activity = new Intent(Map.this, SignIn.class);
                                            // Pass this boolean value (true) to Chat.java.
                                            Activity.putExtra("newShape", true);
                                            // Pass this value to Chat.java to identify the shape.
                                            Activity.putExtra("uuid", uuid);
                                            // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                            // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                            Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                            Activity.putExtra("circleLongitude", circle.getCenter().longitude);
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
                                Activity.putExtra("newShape", false);
                                // Pass this value to Chat.java to identify the shape.
                                Activity.putExtra("uuid", uuid);
                                // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                startActivity(Activity);
                            }
                        }
                    }

                    return true;
                }
            });

            // Keep the marker on the shapes to allow for dragging.
            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {

                    LatLng markerPosition = marker.getPosition();

                    // If user holds the center marker, update the circle's position. Else, update the circle's radius.
                    if (circle != null) {

                        circle.setFillColor(0);

                        if (marker.getId().equals(marker0ID)) {

                            circle.setCenter(markerPosition);
                            marker1.setVisible(false);
                        }

                        if (marker.getId().equals(marker1ID)) {

                            // Limits the size of the circle.
                            if (distanceGivenLatLng(circle.getCenter().latitude, circle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude) < 200) {

                                circle.setRadius(distanceGivenLatLng(circle.getCenter().latitude, circle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude));
                            } else {

                                circle.setRadius(200);
                            }
                        }
                    }

                    // Update the polygon shape as the marker positions get updated.
                    if (polygon != null) {

                        if (marker.getId().equals(marker0ID)) {

                            if (threeMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fourMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fiveMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker1ID)) {

                            if (threeMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fourMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fiveMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker2ID)) {

                            if (threeMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fourMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fiveMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker3ID)) {

                            if (fourMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fiveMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker4ID)) {

                            if (fiveMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker5ID)) {

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker6ID)) {

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker7ID)) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        // Prevent the fill color from flashing by setting it to 0 and then re-adding it in onMarkerDragEnd().
                        polygon.setFillColor(0);
                    }
                }

                @Override
                public void onMarkerDrag(Marker marker) {

                    LatLng markerPosition = marker.getPosition();

                    // If user holds the center marker, update the circle's position. Else, update the circle's radius.
                    if (circle != null) {

                        if (marker.getId().equals(marker0ID)) {

                            circle.setCenter(markerPosition);
                        }

                        if (marker.getId().equals(marker1ID)) {

                            // Limits the size of the circle.
                            if (distanceGivenLatLng(markerPosition.latitude, markerPosition.longitude, circle.getCenter().latitude, circle.getCenter().longitude) < 200) {

                                circle.setRadius(distanceGivenLatLng(circle.getCenter().latitude, circle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude));
                            } else {

                                circle.setRadius(200);
                            }
                        }
                    }

                    // Update the polygon shape as the marker positions get updated.
                    if (polygon != null) {

                        if (marker.getId().equals(marker0ID)) {

                            if (threeMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fourMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fiveMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker1ID)) {

                            if (threeMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fourMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fiveMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker2ID)) {

                            if (threeMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fourMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fiveMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker3ID)) {

                            if (fourMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (fiveMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker4ID)) {

                            if (fiveMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker5ID)) {

                            if (sixMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker6ID)) {

                            if (sevenMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }

                            if (eightMarkers) {

                                LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition, marker7Position};
                                polygonPointsList = Arrays.asList(polygonPoints);
                                polygon.setPoints(polygonPointsList);
                            }
                        }

                        if (marker.getId().equals(marker7ID)) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }
                }

                @Override
                public void onMarkerDragEnd(Marker marker) {

                    LatLng markerPosition = marker.getPosition();

                    if (circle != null) {

                        circle.setFillColor(Color.argb(70, 255, 215, 0));

                        // Sets marker1's position on the circle's edge relative to where the user last left marker1.
                        if (marker.getId().equals(marker0ID)) {

                            marker1.setPosition(latLngGivenDistance(circle.getCenter().latitude, circle.getCenter().longitude, circle.getRadius(), relativeAngle));

                            marker1.setVisible(true);
                        }

                        if (marker.getId().equals(marker1ID)) {

                            // Update the global variable with the angle the user left the marker's position. This is used if the user drags the center marker.
                            relativeAngle = angleFromCoordinate(circle.getCenter().latitude, circle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude);

                            // Keep the seekBar's progress aligned with the marker.
                            if (circle.getRadius() < 200) {

                                chatSizeSeekBar.setProgress((int) distanceGivenLatLng(circle.getCenter().latitude, circle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude));
                            }

                            // Limits the size of the circle, keeps marker1's position on the circle's edge at the same angle relative to where the user last left it, and keeps the seekBar's progress aligned with the marker.
                            if (circle.getRadius() >= 200) {

                                marker.setPosition(latLngGivenDistance(circle.getCenter().latitude, circle.getCenter().longitude, 200, relativeAngle));

                                chatSizeSeekBar.setProgress(200);
                            }
                        }
                    }

                    // Updated the global variable with the marker's position.
                    if (polygon != null) {

                        if (marker.getId().equals(marker0ID)) {

                            marker0Position = marker.getPosition();
                        }

                        if (marker.getId().equals(marker1ID)) {

                            marker1Position = marker.getPosition();
                        }

                        if (marker.getId().equals(marker2ID)) {

                            marker2Position = marker.getPosition();
                        }

                        if (marker.getId().equals(marker3ID)) {

                            marker3Position = marker.getPosition();
                        }

                        if (marker.getId().equals(marker4ID)) {

                            marker4Position = marker.getPosition();
                        }

                        if (marker.getId().equals(marker5ID)) {

                            marker5Position = marker.getPosition();
                        }

                        if (marker.getId().equals(marker6ID)) {

                            marker6Position = marker.getPosition();
                        }

                        if (marker.getId().equals(marker7ID)) {

                            marker7Position = marker.getPosition();
                        }

                        polygon.setFillColor(Color.argb(70, 255, 215, 0));
                    }
                }
            });

            // Go to Chat.java when clicking on a polygon.
            mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
                @Override
                public void onPolygonClick(Polygon polygon) {

                    if (polygon.getTag() != null) {

                        // Get the ID set by Firebase to identify which polygon the user clicked on.
                        uuid = (String) circle.getTag();
                    } else {

                        // If the polygon is new, it will not have a tag, as the tag is pulled from Firebase. Therefore, generate a uuid.
                        uuid = UUID.randomUUID().toString();
                    }

                    // Check if the user is already signed in.
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                        // User is signed in.

                        // Check if user is within the polygon before going to the chat.
                        FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {

                                        if (location != null) {

                                            // Boolean; will be true if user is within the circle upon circle click.
                                            userIsWithinShape = PolyUtil.containsLocation(location.getLatitude(), location.getLongitude(), polygonPointsList, false);
                                        }
                                    }
                                });

                        // If polygon.getTag() == null, the polygon is new. Therefore, compare it to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                        if (polygon.getTag() == null) {

                            firebasePolygons.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    // If the uuid already exists in Firebase, generate another uuid and try again.
                                    if (dataSnapshot.exists()) {

                                        // uuid exists in Firebase. Generate another and try again.

                                        // Generate another UUID and try again.
                                        uuid = UUID.randomUUID().toString();

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, Chat.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                                        if (threeMarkers) {
                                            Activity.putExtra("threeMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        }

                                        if (fourMarkers) {
                                            Activity.putExtra("fourMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        }

                                        if (fiveMarkers) {
                                            Activity.putExtra("fiveMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        }

                                        if (sixMarkers) {
                                            Activity.putExtra("sixMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                        }

                                        if (sevenMarkers) {
                                            Activity.putExtra("sevenMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        if (eightMarkers) {
                                            Activity.putExtra("eightMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                        startActivity(Activity);
                                    } else {

                                        // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, Chat.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                                        if (threeMarkers) {
                                            Activity.putExtra("threeMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        }

                                        if (fourMarkers) {
                                            Activity.putExtra("fourMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        }

                                        if (fiveMarkers) {
                                            Activity.putExtra("fiveMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        }

                                        if (sixMarkers) {
                                            Activity.putExtra("sixMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                        }

                                        if (sevenMarkers) {
                                            Activity.putExtra("sevenMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        if (eightMarkers) {
                                            Activity.putExtra("eightMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                        startActivity(Activity);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                        } else {

                            // The polygon is not new, so go to Chat.java.

                            // Carry the extras all the way to Chat.java.
                            Intent Activity = new Intent(Map.this, Chat.class);
                            // Pass this boolean value (true) to Chat.java.
                            Activity.putExtra("newShape", false);
                            // Pass this value to Chat.java to identify the shape.
                            Activity.putExtra("uuid", uuid);
                            // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                            // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                            if (threeMarkers) {
                                Activity.putExtra("threeMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                            }

                            if (fourMarkers) {
                                Activity.putExtra("fourMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                            }

                            if (fiveMarkers) {
                                Activity.putExtra("fiveMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                            }

                            if (sixMarkers) {
                                Activity.putExtra("sixMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                Activity.putExtra("marker5Longitude", marker5Position.longitude);
                            }

                            if (sevenMarkers) {
                                Activity.putExtra("sevenMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                Activity.putExtra("marker6Longitude", marker6Position.longitude);
                            }

                            if (eightMarkers) {
                                Activity.putExtra("eightMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                Activity.putExtra("marker6Longitude", marker6Position.longitude);
                            }

                            Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                            startActivity(Activity);
                        }
                    } else {

                        // No user is signed in.

                        // If polygon.getTag() == null, the circle is new. Therefore, compare it to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                        if (circle.getTag() == null) {

                            firebasePolygons.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    // If the uuid already exists in Firebase, generate another uuid and try again.
                                    if (dataSnapshot.exists()) {

                                        // uuid exists in Firebase. Generate another and try again.

                                        // Generate another UUID and try again.
                                        uuid = UUID.randomUUID().toString();

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                                        if (threeMarkers) {
                                            Activity.putExtra("threeMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        }

                                        if (fourMarkers) {
                                            Activity.putExtra("fourMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        }

                                        if (fiveMarkers) {
                                            Activity.putExtra("fiveMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        }

                                        if (sixMarkers) {
                                            Activity.putExtra("sixMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                        }

                                        if (sevenMarkers) {
                                            Activity.putExtra("sevenMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        if (eightMarkers) {
                                            Activity.putExtra("eightMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                        startActivity(Activity);
                                    } else {

                                        // uuid does not already exist in Firebase. Go to SignIn.java with the uuid.

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                                        if (threeMarkers) {
                                            Activity.putExtra("threeMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        }

                                        if (fourMarkers) {
                                            Activity.putExtra("fourMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        }

                                        if (fiveMarkers) {
                                            Activity.putExtra("fiveMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        }

                                        if (sixMarkers) {
                                            Activity.putExtra("sixMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                        }

                                        if (sevenMarkers) {
                                            Activity.putExtra("sevenMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        if (eightMarkers) {
                                            Activity.putExtra("eightMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                        startActivity(Activity);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                        } else {

                            // The polygon is not new, so go to SignIn.java.

                            // Carry the extras all the way to Chat.java.
                            Intent Activity = new Intent(Map.this, SignIn.class);
                            // Pass this boolean value (true) to Chat.java.
                            Activity.putExtra("newShape", false);
                            // Pass this value to Chat.java to identify the shape.
                            Activity.putExtra("uuid", uuid);
                            // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                            // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                            if (threeMarkers) {
                                Activity.putExtra("threeMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                            }

                            if (fourMarkers) {
                                Activity.putExtra("fourMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                            }

                            if (fiveMarkers) {
                                Activity.putExtra("fiveMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                            }

                            if (sixMarkers) {
                                Activity.putExtra("sixMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                Activity.putExtra("marker5Longitude", marker5Position.longitude);
                            }

                            if (sevenMarkers) {
                                Activity.putExtra("sevenMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                Activity.putExtra("marker6Longitude", marker6Position.longitude);
                            }

                            if (eightMarkers) {
                                Activity.putExtra("eightMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                Activity.putExtra("marker6Longitude", marker6Position.longitude);
                            }

                            Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                            startActivity(Activity);
                        }
                    }
                }
            });

            // Go to Chat.java when clicking on a circle.
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

                        // Check if user is within the circle before going to the chat.
                        FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {

                                        if (location != null) {

                                            float[] distance = new float[2];

                                            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                                    circle.getCenter().latitude, circle.getCenter().longitude, distance);

                                            // Boolean; will be true if user is within the circle upon circle click.
                                            userIsWithinShape = !(distance[0] > circle.getRadius());
                                        }
                                    }
                                });

                        // If circle.getTag() == null, the circle is new. Therefore, compare it to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                        if (circle.getTag() == null) {

                            firebaseCircles.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    // If the uuid already exists in Firebase, generate another uuid and try again.
                                    if (dataSnapshot.exists()) {

                                        // uuid exists in Firebase. Generate another and try again.

                                        // Generate another UUID and try again.
                                        uuid = UUID.randomUUID().toString();

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, Chat.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                        Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                        Activity.putExtra("radius", circle.getRadius());
                                        startActivity(Activity);
                                    } else {

                                        // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, Chat.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
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

                            // The circle is not new, so go to Chat.java.

                            // Go to Chat.java with the boolean value.
                            Intent Activity = new Intent(Map.this, Chat.class);
                            // Pass this boolean value (false) to Chat.java.
                            Activity.putExtra("newShape", false);
                            // Pass this value to Chat.java to identify the shape.
                            Activity.putExtra("uuid", uuid);
                            // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
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

                                        // uuid exists in Firebase. Generate another and try again.

                                        // Generate another UUID and try again.
                                        uuid = UUID.randomUUID().toString();

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                        Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                        Activity.putExtra("radius", circle.getRadius());
                                        startActivity(Activity);
                                    } else {

                                        // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
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

                            // The polygon is not new, so go to SignIn.java.

                            // Go to Chat.java with the boolean value.
                            Intent Activity = new Intent(Map.this, SignIn.class);
                            // Pass this boolean value (false) to Chat.java.
                            Activity.putExtra("newShape", false);
                            // Pass this value to Chat.java to identify the shape.
                            Activity.putExtra("uuid", uuid);
                            // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                            startActivity(Activity);
                        }
                    }
                }
            });
        }

        // Close any open menus
        if (popupMapType != null) {

            popupMapType.dismiss();
            mapTypeMenuIsOpen = false;
        }

        // Close any open menus
        if (popupChatViews != null) {

            popupChatViews.dismiss();
            chatViewsMenuIsOpen = false;
        }

        // Close any open menus
        if (popupCreateChat != null) {

            popupCreateChat.dismiss();
            createChatMenuIsOpen = false;
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.i(TAG, "onResume()");

        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Check if GPS is enabled.
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

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

        // Remove the listener.
        if (mapTypeButton != null) {

            mapTypeButton.setOnClickListener(null);
        }

        // Remove the listener.
        if (chatViewsButton != null) {

            chatViewsButton.setOnClickListener(null);
        }

        // Remove the listener.
        if (popupChatViews != null) {

            popupChatViews.setOnDismissListener(null);
        }

        // Remove the listener.
        if (createChatButton != null) {

            createChatButton.setOnClickListener(null);
        }

        // Remove the seekBar listener.
        if (chatSizeSeekBar != null) {

            chatSizeSeekBar.setOnSeekBarChangeListener(null);
        }

        // Remove the listener.
        if (mMap != null) {

            mMap.setOnCircleClickListener(null);
            mMap.setOnMarkerDragListener(null);
        }

        super.onStop();
    }

    @Override
    public void onTrimMemory(int level) {

        Log.i(TAG, "onTrimMemory()");
        super.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {

        Log.i(TAG, "OnLowMemory()");
        super.onLowMemory();
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

        // NOTE: Anything done here should be done in onRestart() as well, as onMapReady() is not called again after the app restarts!

        Log.i(TAG, "onMapReady()");

        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
        }

        // Load Firebase circles.
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

        // Go to Chat.java after clicking on a circle's middle marker.
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                if (circle != null && marker.getId().equals(marker0ID)) {

                    // Since the circle is new, it will not have a tag, as the tag is pulled from Firebase. Therefore, generate a uuid.
                    uuid = UUID.randomUUID().toString();

                    // Check if the user is already signed in.
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                        // User is signed in.

                        // Check if user is within the circle before going to the chat.
                        FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {

                                        if (location != null) {

                                            float[] distance = new float[2];

                                            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                                    circle.getCenter().latitude, circle.getCenter().longitude, distance);

                                            // Boolean; will be true if user is within the circle upon circle click.
                                            userIsWithinShape = !(distance[0] > circle.getRadius());
                                        }
                                    }
                                });

                        // If circle.getTag() == null, the circle is new. Therefore, compare it to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                        if (circle.getTag() == null) {

                            firebaseCircles.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    // If the uuid already exists in Firebase, generate another uuid and try again.
                                    if (dataSnapshot.exists()) {

                                        // Generate another UUID and try again.
                                        uuid = UUID.randomUUID().toString();

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, Chat.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                        Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                        Activity.putExtra("radius", circle.getRadius());
                                        startActivity(Activity);
                                    } else {

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, Chat.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
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
                            Activity.putExtra("newShape", false);
                            // Pass this value to Chat.java to identify the shape.
                            Activity.putExtra("uuid", uuid);
                            // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
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

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                        Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                        Activity.putExtra("radius", circle.getRadius());
                                        startActivity(Activity);
                                    } else {

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
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
                            Activity.putExtra("newShape", false);
                            // Pass this value to Chat.java to identify the shape.
                            Activity.putExtra("uuid", uuid);
                            // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                            startActivity(Activity);
                        }
                    }
                }

                return true;
            }
        });

        // Keep the marker on the shapes to allow for dragging.
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

                LatLng markerPosition = marker.getPosition();

                // If user holds the center marker, update the circle's position. Else, update the circle's radius.
                if (circle != null) {

                    circle.setFillColor(0);

                    if (marker.getId().equals(marker0ID)) {

                        circle.setCenter(markerPosition);
                        marker1.setVisible(false);
                    }

                    if (marker.getId().equals(marker1ID)) {

                        // Limits the size of the circle.
                        if (distanceGivenLatLng(circle.getCenter().latitude, circle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude) < 200) {

                            circle.setRadius(distanceGivenLatLng(circle.getCenter().latitude, circle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude));
                        } else {

                            circle.setRadius(200);
                        }
                    }
                }

                // Update the polygon shape as the marker positions get updated.
                if (polygon != null) {

                    if (marker.getId().equals(marker0ID)) {

                        if (threeMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fourMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fiveMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker1ID)) {

                        if (threeMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fourMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fiveMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker2ID)) {

                        if (threeMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fourMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fiveMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker3ID)) {

                        if (fourMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fiveMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker4ID)) {

                        if (fiveMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker5ID)) {

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker6ID)) {

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker7ID)) {

                        LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        polygon.setPoints(polygonPointsList);
                    }

                    // Prevent the fill color from flashing by setting it to 0 and then re-adding it in onMarkerDragEnd().
                    polygon.setFillColor(0);
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {

                LatLng markerPosition = marker.getPosition();

                // If user holds the center marker, update the circle's position. Else, update the circle's radius.
                if (circle != null) {

                    if (marker.getId().equals(marker0ID)) {

                        circle.setCenter(markerPosition);
                    }

                    if (marker.getId().equals(marker1ID)) {

                        // Limits the size of the circle.
                        if (distanceGivenLatLng(markerPosition.latitude, markerPosition.longitude, circle.getCenter().latitude, circle.getCenter().longitude) < 200) {

                            circle.setRadius(distanceGivenLatLng(circle.getCenter().latitude, circle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude));
                        } else {

                            circle.setRadius(200);
                        }
                    }
                }

                // Update the polygon shape as the marker positions get updated.
                if (polygon != null) {

                    if (marker.getId().equals(marker0ID)) {

                        if (threeMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fourMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fiveMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker1ID)) {

                        if (threeMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fourMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fiveMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker2ID)) {

                        if (threeMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fourMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fiveMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker3ID)) {

                        if (fourMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (fiveMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker4ID)) {

                        if (fiveMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker5ID)) {

                        if (sixMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker6ID)) {

                        if (sevenMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }

                        if (eightMarkers) {

                            LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition, marker7Position};
                            polygonPointsList = Arrays.asList(polygonPoints);
                            polygon.setPoints(polygonPointsList);
                        }
                    }

                    if (marker.getId().equals(marker7ID)) {

                        LatLng[] polygonPoints = new LatLng[] {marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        polygon.setPoints(polygonPointsList);
                    }
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

                LatLng markerPosition = marker.getPosition();

                if (circle != null) {

                    circle.setFillColor(Color.argb(70, 255, 215, 0));

                    // Sets marker1's position on the circle's edge relative to where the user last left marker1.
                    if (marker.getId().equals(marker0ID)) {

                        marker1.setPosition(latLngGivenDistance(circle.getCenter().latitude, circle.getCenter().longitude, circle.getRadius(), relativeAngle));

                        marker1.setVisible(true);
                    }

                    if (marker.getId().equals(marker1ID)) {

                        // Update the global variable with the angle the user left the marker's position. This is used if the user drags the center marker.
                        relativeAngle = angleFromCoordinate(circle.getCenter().latitude, circle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude);

                        // Keep the seekBar's progress aligned with the marker.
                        if (circle.getRadius() < 200) {

                            chatSizeSeekBar.setProgress((int) distanceGivenLatLng(circle.getCenter().latitude, circle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude));
                        }

                        // Limits the size of the circle, keeps marker1's position on the circle's edge at the same angle relative to where the user last left it, and keeps the seekBar's progress aligned with the marker.
                        if (circle.getRadius() >= 200) {

                            marker.setPosition(latLngGivenDistance(circle.getCenter().latitude, circle.getCenter().longitude, 200, relativeAngle));

                            chatSizeSeekBar.setProgress(200);
                        }
                    }
                }

                // Updated the global variable with the marker's position.
                if (polygon != null) {

                    if (marker.getId().equals(marker0ID)) {

                        marker0Position = marker.getPosition();
                    }

                    if (marker.getId().equals(marker1ID)) {

                        marker1Position = marker.getPosition();
                    }

                    if (marker.getId().equals(marker2ID)) {

                        marker2Position = marker.getPosition();
                    }

                    if (marker.getId().equals(marker3ID)) {

                        marker3Position = marker.getPosition();
                    }

                    if (marker.getId().equals(marker4ID)) {

                        marker4Position = marker.getPosition();
                    }

                    if (marker.getId().equals(marker5ID)) {

                        marker5Position = marker.getPosition();
                    }

                    if (marker.getId().equals(marker6ID)) {

                        marker6Position = marker.getPosition();
                    }

                    if (marker.getId().equals(marker7ID)) {

                        marker7Position = marker.getPosition();
                    }

                    polygon.setFillColor(Color.argb(70, 255, 215, 0));
                }
            }
        });

        // Go to Chat.java when clicking on a polygon.
        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
            @Override
            public void onPolygonClick(Polygon polygon) {

                if (polygon.getTag() != null) {

                    // Get the ID set by Firebase to identify which polygon the user clicked on.
                    uuid = (String) circle.getTag();
                } else {

                    // If the polygon is new, it will not have a tag, as the tag is pulled from Firebase. Therefore, generate a uuid.
                    uuid = UUID.randomUUID().toString();
                }

                // Check if the user is already signed in.
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                    // User is signed in.

                    // Check if user is within the polygon before going to the chat.
                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {

                                    if (location != null) {

                                        // Boolean; will be true if user is within the circle upon circle click.
                                        userIsWithinShape = PolyUtil.containsLocation(location.getLatitude(), location.getLongitude(), polygonPointsList, false);
                                    }
                                }
                            });

                    // If polygon.getTag() == null, the polygon is new. Therefore, compare it to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                    if (polygon.getTag() == null) {

                        firebasePolygons.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                // If the uuid already exists in Firebase, generate another uuid and try again.
                                if (dataSnapshot.exists()) {

                                    // uuid exists in Firebase. Generate another and try again.

                                    // Generate another UUID and try again.
                                    uuid = UUID.randomUUID().toString();

                                    // Carry the extras all the way to Chat.java.
                                    Intent Activity = new Intent(Map.this, Chat.class);
                                    // Pass this boolean value (true) to Chat.java.
                                    Activity.putExtra("newShape", true);
                                    // Pass this value to Chat.java to identify the shape.
                                    Activity.putExtra("uuid", uuid);
                                    // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                    // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                                    if (threeMarkers) {
                                        Activity.putExtra("threeMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                    }

                                    if (fourMarkers) {
                                        Activity.putExtra("fourMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                        Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                    }

                                    if (fiveMarkers) {
                                        Activity.putExtra("fiveMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                        Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                        Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                    }

                                    if (sixMarkers) {
                                        Activity.putExtra("sixMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                        Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                        Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                        Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                    }

                                    if (sevenMarkers) {
                                        Activity.putExtra("sevenMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                        Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                        Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                        Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                        Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                        Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                    }

                                    if (eightMarkers) {
                                        Activity.putExtra("eightMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                        Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                        Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                        Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                        Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                        Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                    }

                                    Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                    startActivity(Activity);
                                } else {

                                    // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                    // Carry the extras all the way to Chat.java.
                                    Intent Activity = new Intent(Map.this, Chat.class);
                                    // Pass this boolean value (true) to Chat.java.
                                    Activity.putExtra("newShape", true);
                                    // Pass this value to Chat.java to identify the shape.
                                    Activity.putExtra("uuid", uuid);
                                    // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                    // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                                    if (threeMarkers) {
                                        Activity.putExtra("threeMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                    }

                                    if (fourMarkers) {
                                        Activity.putExtra("fourMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                        Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                    }

                                    if (fiveMarkers) {
                                        Activity.putExtra("fiveMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                        Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                        Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                    }

                                    if (sixMarkers) {
                                        Activity.putExtra("sixMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                        Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                        Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                        Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                    }

                                    if (sevenMarkers) {
                                        Activity.putExtra("sevenMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                        Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                        Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                        Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                        Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                        Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                    }

                                    if (eightMarkers) {
                                        Activity.putExtra("eightMarkers", true);
                                        Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                        Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                        Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                        Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                        Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                        Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                        Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                        Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                        Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                        Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                        Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                    }

                                    Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                    startActivity(Activity);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    } else {

                        // The polygon is not new, so go to Chat.java.

                        // Carry the extras all the way to Chat.java.
                        Intent Activity = new Intent(Map.this, Chat.class);
                        // Pass this boolean value (true) to Chat.java.
                        Activity.putExtra("newShape", false);
                        // Pass this value to Chat.java to identify the shape.
                        Activity.putExtra("uuid", uuid);
                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                        // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                        if (threeMarkers) {
                            Activity.putExtra("threeMarkers", true);
                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                        }

                        if (fourMarkers) {
                            Activity.putExtra("fourMarkers", true);
                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                        }

                        if (fiveMarkers) {
                            Activity.putExtra("fiveMarkers", true);
                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                        }

                        if (sixMarkers) {
                            Activity.putExtra("sixMarkers", true);
                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                        }

                        if (sevenMarkers) {
                            Activity.putExtra("sevenMarkers", true);
                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                        }

                        if (eightMarkers) {
                            Activity.putExtra("eightMarkers", true);
                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                        }

                        Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                        startActivity(Activity);
                    }
                } else {

                        // No user is signed in.

                        // If polygon.getTag() == null, the circle is new. Therefore, compare it to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                        if (circle.getTag() == null) {

                            firebasePolygons.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    // If the uuid already exists in Firebase, generate another uuid and try again.
                                    if (dataSnapshot.exists()) {

                                        // uuid exists in Firebase. Generate another and try again.

                                        // Generate another UUID and try again.
                                        uuid = UUID.randomUUID().toString();

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                                        if (threeMarkers) {
                                            Activity.putExtra("threeMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        }

                                        if (fourMarkers) {
                                            Activity.putExtra("fourMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        }

                                        if (fiveMarkers) {
                                            Activity.putExtra("fiveMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        }

                                        if (sixMarkers) {
                                            Activity.putExtra("sixMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                        }

                                        if (sevenMarkers) {
                                            Activity.putExtra("sevenMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        if (eightMarkers) {
                                            Activity.putExtra("eightMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                        startActivity(Activity);
                                    } else {

                                        // uuid does not already exist in Firebase. Go to SignIn.java with the uuid.

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        // Pass this boolean value (true) to Chat.java.
                                        Activity.putExtra("newShape", true);
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("uuid", uuid);
                                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                        // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                                        if (threeMarkers) {
                                            Activity.putExtra("threeMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                        }

                                        if (fourMarkers) {
                                            Activity.putExtra("fourMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                        }

                                        if (fiveMarkers) {
                                            Activity.putExtra("fiveMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                        }

                                        if (sixMarkers) {
                                            Activity.putExtra("sixMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                        }

                                        if (sevenMarkers) {
                                            Activity.putExtra("sevenMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        if (eightMarkers) {
                                            Activity.putExtra("eightMarkers", true);
                                            Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                            Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                            Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                            Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                            Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                            Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                            Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                            Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                            Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                            Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                            Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                            Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                            Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                            Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                        }

                                        Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                        startActivity(Activity);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                        } else {

                            // The polygon is not new, so go to SignIn.java.

                            // Carry the extras all the way to Chat.java.
                            Intent Activity = new Intent(Map.this, SignIn.class);
                            // Pass this boolean value (true) to Chat.java.
                            Activity.putExtra("newShape", false);
                            // Pass this value to Chat.java to identify the shape.
                            Activity.putExtra("uuid", uuid);
                            // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                            // Pass this information to Chat.java to create a new shape in Firebase after someone writes a message.
                            if (threeMarkers) {
                                Activity.putExtra("threeMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                            }

                            if (fourMarkers) {
                                Activity.putExtra("fourMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                            }

                            if (fiveMarkers) {
                                Activity.putExtra("fiveMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                            }

                            if (sixMarkers) {
                                Activity.putExtra("sixMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                Activity.putExtra("marker5Longitude", marker5Position.longitude);
                            }

                            if (sevenMarkers) {
                                Activity.putExtra("sevenMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                Activity.putExtra("marker6Longitude", marker6Position.longitude);
                            }

                            if (eightMarkers) {
                                Activity.putExtra("eightMarkers", true);
                                Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                Activity.putExtra("marker6Longitude", marker6Position.longitude);
                            }

                            Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                            startActivity(Activity);
                        }
                }
            }
        });

        // Go to Chat.java when clicking on a circle.
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

                    // Check if user is within the circle before going to the chat.
                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {

                                    if (location != null) {

                                        float[] distance = new float[2];

                                        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                                circle.getCenter().latitude, circle.getCenter().longitude, distance);

                                        // Boolean; will be true if user is within the circle upon circle click.
                                        userIsWithinShape = !(distance[0] > circle.getRadius());
                                    }
                                }
                            });

                    // If circle.getTag() == null, the circle is new. Therefore, compare it to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                    if (circle.getTag() == null) {

                        firebaseCircles.orderByChild("uuid").equalTo(uuid).addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                // If the uuid already exists in Firebase, generate another uuid and try again.
                                if (dataSnapshot.exists()) {

                                    // uuid exists in Firebase. Generate another and try again.

                                    // Generate another UUID and try again.
                                    uuid = UUID.randomUUID().toString();

                                    // Carry the extras all the way to Chat.java.
                                    Intent Activity = new Intent(Map.this, Chat.class);
                                    // Pass this boolean value (true) to Chat.java.
                                    Activity.putExtra("newShape", true);
                                    // Pass this value to Chat.java to identify the shape.
                                    Activity.putExtra("uuid", uuid);
                                    // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                    // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                    Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                    Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                    Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                    Activity.putExtra("radius", circle.getRadius());
                                    startActivity(Activity);
                                } else {

                                    // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                    // Carry the extras all the way to Chat.java.
                                    Intent Activity = new Intent(Map.this, Chat.class);
                                    // Pass this boolean value (true) to Chat.java.
                                    Activity.putExtra("newShape", true);
                                    // Pass this value to Chat.java to identify the shape.
                                    Activity.putExtra("uuid", uuid);
                                    // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                    // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                    Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                    Activity.putExtra("circleLongitude", circle.getCenter().longitude);
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

                        // The circle is not new, so go to Chat.java.

                        // Go to Chat.java with the boolean value.
                        Intent Activity = new Intent(Map.this, Chat.class);
                        // Pass this boolean value (false) to Chat.java.
                        Activity.putExtra("newShape", false);
                        // Pass this value to Chat.java to identify the shape.
                        Activity.putExtra("uuid", uuid);
                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
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

                                    // uuid exists in Firebase. Generate another and try again.

                                    // Generate another UUID and try again.
                                    uuid = UUID.randomUUID().toString();

                                    // Carry the extras all the way to Chat.java.
                                    Intent Activity = new Intent(Map.this, SignIn.class);
                                    // Pass this boolean value (true) to Chat.java.
                                    Activity.putExtra("newShape", true);
                                    // Pass this value to Chat.java to identify the shape.
                                    Activity.putExtra("uuid", uuid);
                                    // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                    // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                    Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                    Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                    Activity.putExtra("fillColor", Color.argb(70, 255, 215, 0));
                                    Activity.putExtra("radius", circle.getRadius());
                                    startActivity(Activity);
                                } else {

                                    // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                    // Carry the extras all the way to Chat.java.
                                    Intent Activity = new Intent(Map.this, SignIn.class);
                                    // Pass this boolean value (true) to Chat.java.
                                    Activity.putExtra("newShape", true);
                                    // Pass this value to Chat.java to identify the shape.
                                    Activity.putExtra("uuid", uuid);
                                    // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                    // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                    Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                    Activity.putExtra("circleLongitude", circle.getCenter().longitude);
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

                        // The polygon is not new, so go to SignIn.java.

                        // Go to Chat.java with the boolean value.
                        Intent Activity = new Intent(Map.this, SignIn.class);
                        // Pass this boolean value (false) to Chat.java.
                        Activity.putExtra("newShape", false);
                        // Pass this value to Chat.java to identify the shape.
                        Activity.putExtra("uuid", uuid);
                        // Pass this value to Chat.java to tell whether the user can leave a message in the chat.
                        Activity.putExtra("userIsWithinShape", userIsWithinShape);
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
            // Instantly move the camera to the user's location once the map is available.
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            // Change the map type to hybrid once the camera has adjusted for quicker initial loading.
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            // Set Boolean to false to prevent unnecessary animation, as the camera should already be set on the user's location.
            firstLoad = false;
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
        } else if ( newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && chatViewsMenuIsOpen){

            popupMapType.dismiss();
            popupMapType.show();
            mapTypeMenuIsOpen = true;
        }

        // Reloads the popup when the orientation changes to prevent viewing issues.
        if ( newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && chatViewsMenuIsOpen) {

            popupChatViews.dismiss();
            popupChatViews.show();
            chatViewsMenuIsOpen = true;
        } else if ( newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && chatViewsMenuIsOpen){

            popupChatViews.dismiss();
            popupChatViews.show();
            chatViewsMenuIsOpen = true;
        }

        // Reloads the popup when the orientation changes to prevent viewing issues.
        if ( newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && createChatMenuIsOpen) {

            popupCreateChat.dismiss();
            popupCreateChat.show();
            createChatMenuIsOpen = true;
        } else if ( newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && createChatMenuIsOpen){

            popupCreateChat.dismiss();
            popupCreateChat.show();
            createChatMenuIsOpen = true;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        Log.i(TAG, "onMenuItemClick()");
        // Sets the chatviews_menu actions.
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

            // chatviews_menu
            case R.id.showEverything:

                Log.i(TAG, "onMenuItemClick() -> everything");

                mMap.clear();

                // Set circle to null so changing chatSizeSeekBar will create a circle.
                circle = null;

                // Set progress to 0, as no circle exists.
                chatSizeSeekBar.setProgress(0);

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

                chatViewsMenuIsOpen = false;
                return true;

            // chatviews_menu
            case R.id.showLargeChats:

                Log.i(TAG, "onMenuItemClick() -> largeCircles");

                mMap.clear();

                // Set circle to null so changing chatSizeSeekBar will create a circle.
                circle = null;

                // Set progress to 0, as no circle exists.
                chatSizeSeekBar.setProgress(0);

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

                chatViewsMenuIsOpen = false;
                return true;

            // chatviews_menu
            case R.id.showMediumChats:

                Log.i(TAG, "onMenuItemClick() -> mediumCircles");

                mMap.clear();

                // Set circle to null so changing chatSizeSeekBar will create a circle.
                circle = null;

                // Set progress to 0, as no circle exists.
                chatSizeSeekBar.setProgress(0);

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

                chatViewsMenuIsOpen = false;
                return true;

            // chatviews_menu
            case R.id.showSmallChats:

                Log.i(TAG, "onMenuItemClick() -> smallCircles");

                mMap.clear();

                // Set circle to null so changing chatSizeSeekBar will create a circle.
                circle = null;

                // Set progress to 0, as no circle exists.
                chatSizeSeekBar.setProgress(0);

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

                chatViewsMenuIsOpen = false;
                return true;

            // chatviews_menu
            case R.id.showPoints:

                Log.i(TAG, "onMenuItemClick() -> points");

                mMap.clear();

                // Set circle to null so changing chatSizeSeekBar will create a circle.
                circle = null;

                // Set progress to 0, as no circle exists.
                chatSizeSeekBar.setProgress(0);

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

                chatViewsMenuIsOpen = false;
                return true;

            // createchat_menu
            case R.id.createPolygon:

                Log.i(TAG, "onMenuItemClick() -> createPolygon");

                // Remove the polygon and markers.
                if (polygon != null) {

                    polygon.remove();
                    marker0.remove();
                    marker1.remove();
                    marker2.remove();
                    polygon = null;
                    marker0 = null;
                    marker1 = null;
                    marker2 = null;
                    marker0Position = null;
                    marker1Position = null;
                    marker2Position = null;
                    marker0ID = null;
                    marker1ID = null;
                    marker2ID = null;

                    if (marker3 != null) {

                        marker3.remove();
                        marker3 = null;
                        marker3Position = null;
                        marker3ID = null;
                    }

                    if (marker4 != null) {

                        marker4.remove();
                        marker4 = null;
                        marker4Position = null;
                        marker4ID = null;
                    }

                    if (marker5 != null) {

                        marker5.remove();
                        marker5 = null;
                        marker5Position = null;
                        marker5ID = null;
                    }

                    if (marker6 != null) {

                        marker6.remove();
                        marker6 = null;
                        marker6Position = null;
                        marker6ID = null;
                    }

                    if (marker7 != null) {

                        marker7.remove();
                        marker7 = null;
                        marker7Position = null;
                        marker7ID = null;
                    }
                }

                // Remove the circle and markers.
                if (circle != null) {

                    circle.remove();
                    marker0.remove();
                    marker1.remove();
                    circle = null;
                    marker0 = null;
                    marker1 = null;
                    marker0Position = null;
                    marker1Position = null;
                    marker0ID = null;
                    marker1ID = null;
                }

                // Creates a polygon.
                FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                            @Override
                            public void onSuccess(Location location) {

                                // Get last known location. In some rare situations, this can be null.
                                if (location != null) {

                                    // Global variable used in chatSizeSeekBar's onProgressChanged().
                                    mlocation = location;

                                    // Update the global variable - used in chatSeekBarChangeListener.
                                    fourMarkers = true;

                                    // Set seekBar to be the same as the polygon's arbitrary size.
                                    chatSizeSeekBar.setProgress(50);

                                    // Logic to handle location object.
                                    marker0Position = new LatLng(location.getLatitude() - 0.0001, location.getLongitude());
                                    marker1Position = new LatLng(location.getLatitude(), location.getLongitude() - 0.0001);
                                    marker2Position = new LatLng(location.getLatitude() + 0.0001, location.getLongitude());
                                    marker3Position = new LatLng(location.getLatitude(), location.getLongitude() + 0.0001);
                                    PolygonOptions polygonOptions =
                                            new PolygonOptions()
                                                    .add(marker0Position, marker1Position, marker2Position, marker3Position)
                                                    .clickable(true)
                                                    .fillColor(Color.argb(70, 255, 215, 0))
                                                    .strokeColor(Color.YELLOW)
                                                    .strokeWidth(3f);

                                    // Create markers when creating the polygon to allow for dragging of the center and vertices.
                                    MarkerOptions markerOptions0 = new MarkerOptions()
                                            .position(marker0Position)
                                            .draggable(true);

                                    MarkerOptions markerOptions1 = new MarkerOptions()
                                            .position(marker1Position)
                                            .draggable(true);

                                    MarkerOptions markerOptions2 = new MarkerOptions()
                                            .position(marker2Position)
                                            .draggable(true);

                                    MarkerOptions markerOptions3 = new MarkerOptions()
                                            .position(marker3Position)
                                            .draggable(true);

                                    marker0 = mMap.addMarker(markerOptions0);
                                    marker1 = mMap.addMarker(markerOptions1);
                                    marker2 = mMap.addMarker(markerOptions2);
                                    marker3 = mMap.addMarker(markerOptions3);

                                    // Update the global variable to compare with the marker the user clicks on during the dragging process.
                                    marker0ID = marker0.getId();
                                    marker1ID = marker1.getId();
                                    marker2ID = marker2.getId();
                                    marker3ID = marker3.getId();

                                    polygon = mMap.addPolygon(polygonOptions);
                                }
                            }
                        });

                createChatMenuIsOpen = false;
                return true;

            // createchat_menu
            case R.id.createCircle:

                Log.i(TAG, "onMenuItemClick() -> createCircle");

                // Remove the polygon and markers.
                if (polygon != null) {

                    polygon.remove();
                    marker0.remove();
                    marker1.remove();
                    marker2.remove();
                    polygon = null;
                    marker0 = null;
                    marker1 = null;
                    marker2 = null;
                    marker0Position = null;
                    marker1Position = null;
                    marker2Position = null;
                    marker0ID = null;
                    marker1ID = null;
                    marker2ID = null;

                    if (marker3 != null) {

                        marker3.remove();
                        marker3 = null;
                        marker3Position = null;
                        marker3ID = null;
                    }

                    if (marker4 != null) {

                        marker4.remove();
                        marker4 = null;
                        marker4Position = null;
                        marker4ID = null;
                    }

                    if (marker5 != null) {

                        marker5.remove();
                        marker5 = null;
                        marker5Position = null;
                        marker5ID = null;
                    }

                    if (marker6 != null) {

                        marker6.remove();
                        marker6 = null;
                        marker6Position = null;
                        marker6ID = null;
                    }

                    if (marker7 != null) {

                        marker7.remove();
                        marker7 = null;
                        marker7Position = null;
                        marker7ID = null;
                    }
                }

                // Remove the circle and markers.
                if (circle != null) {

                    circle.remove();
                    marker0.remove();
                    marker1.remove();
                    circle = null;
                    marker0 = null;
                    marker1 = null;
                    marker0Position = null;
                    marker1Position = null;
                    marker0ID = null;
                    marker1ID = null;
                }

                // Creates circle.
                mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                            @Override
                            public void onSuccess(Location location) {
                                // Get last known location. In some rare situations, this can be null.
                                if (location != null) {

                                    // Set seekBar to be within 75 and 100, as there are 4 markers.
                                    chatSizeSeekBar.setProgress(88);

                                    // Logic to handle location object.
                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    marker1Position = new LatLng(location.getLatitude() + 0.0001, location.getLongitude());
                                    float circleRadius = distanceGivenLatLng(location.getLatitude(), location.getLongitude(), marker1Position.latitude, marker1Position.longitude);
                                    CircleOptions circleOptions =
                                            new CircleOptions()
                                                    .center(latLng)
                                                    .clickable(true)
                                                    .fillColor(Color.argb(70, 255, 215, 0))
                                                    .radius(circleRadius)
                                                    .strokeColor(Color.YELLOW)
                                                    .strokeWidth(3f);

                                    // Create a marker in the center of the circle to allow for dragging.
                                    MarkerOptions markerOptionsCenter = new MarkerOptions()
                                            .position(latLng)
                                            .draggable(true);

                                    MarkerOptions markerOptionsEdge = new MarkerOptions()
                                            .position(marker1Position)
                                            .draggable(true);

                                    marker0 = mMap.addMarker(markerOptionsCenter);
                                    marker1 = mMap.addMarker(markerOptionsEdge);

                                    // Update the global variable to compare with the marker the user clicks on during the dragging process.
                                    marker0ID = marker0.getId();
                                    marker1ID = marker1.getId();

                                    circle = mMap.addCircle(circleOptions);
                                    chatSizeSeekBar.setProgress((int) distanceGivenLatLng(location.getLatitude(), location.getLongitude(), marker1Position.latitude, marker1Position.longitude));
                                }
                            }
                        });

                createChatMenuIsOpen = false;
                return true;

            // createchat_menu
            case R.id.removeShape:

                Log.i(TAG, "onMenuItemClick() -> removeShape");

                // Remove the polygon and markers.
                if (polygon != null) {

                    polygon.remove();
                    marker0.remove();
                    marker1.remove();
                    marker2.remove();
                    polygon = null;
                    marker0 = null;
                    marker1 = null;
                    marker2 = null;
                    marker0Position = null;
                    marker1Position = null;
                    marker2Position = null;
                    marker0ID = null;
                    marker1ID = null;
                    marker2ID = null;

                    if (marker3 != null) {

                        marker3.remove();
                        marker3 = null;
                        marker3Position = null;
                        marker3ID = null;
                    }

                    if (marker4 != null) {

                        marker4.remove();
                        marker4 = null;
                        marker4Position = null;
                        marker4ID = null;
                    }

                    if (marker5 != null) {

                        marker5.remove();
                        marker5 = null;
                        marker5Position = null;
                        marker5ID = null;
                    }

                    if (marker6 != null) {

                        marker6.remove();
                        marker6 = null;
                        marker6Position = null;
                        marker6ID = null;
                    }

                    if (marker7 != null) {

                        marker7.remove();
                        marker7 = null;
                        marker7Position = null;
                        marker7ID = null;
                    }

                    mlocation = null;
                }

                // Remove the circle and markers.
                if (circle != null) {

                    circle.remove();
                    marker0.remove();
                    marker1.remove();
                    circle = null;
                    marker0 = null;
                    marker1 = null;
                    marker0Position = null;
                    marker1Position = null;
                    marker0ID = null;
                    marker1ID = null;
                }

                chatSizeSeekBar.setProgress(0);
                relativeAngle = 0.0;

                createChatMenuIsOpen = false;
                return true;

            // createchat_menu
            case R.id.createPoint:

                Log.i(TAG, "onMenuItemClick() -> createPoint");

                // Create a point and to to Chat.java or SignIn.java.
                mFusedLocationClient = getFusedLocationProviderClient(Map.this);

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

                                    // Remove any other shape before adding the circle to Firebase.
                                    if (circle != null) {

                                        circle.remove();
                                        circle = null;
                                    }

                                    if (polygon != null) {

                                        polygon.remove();
                                        polygon = null;
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
                                                    if (dataSnapshot.exists()) {

                                                        uuid = UUID.randomUUID().toString();

                                                        // Carry the extras all the way to Chat.java.
                                                        Intent Activity = new Intent(Map.this, Chat.class);
                                                        // Pass this boolean value (true) to Chat.java.
                                                        Activity.putExtra("newShape", true);
                                                        // Pass this value to Chat.java to identify the shape.
                                                        Activity.putExtra("uuid", uuid);
                                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                                        Activity.putExtra("fillColor", Color.argb(100, 255, 255, 0));
                                                        Activity.putExtra("radius", circle.getRadius());
                                                        startActivity(Activity);
                                                    } else {

                                                        // Carry the extras all the way to Chat.java.
                                                        Intent Activity = new Intent(Map.this, Chat.class);
                                                        // Pass this boolean value (true) to Chat.java.
                                                        Activity.putExtra("newShape", true);
                                                        // Pass this value to Chat.java to identify the shape.
                                                        Activity.putExtra("uuid", uuid);
                                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                                        Activity.putExtra("fillColor", Color.argb(100, 255, 255, 0));
                                                        Activity.putExtra("radius", circle.getRadius());
                                                        startActivity(Activity);
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

                                                    // If the uuid already exists in Firebase, generate another uuid and try again.
                                                    if (dataSnapshot.exists()) {

                                                        uuid = UUID.randomUUID().toString();

                                                        // Carry the extras all the way to Chat.java.
                                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                                        // Pass this boolean value (true) to Chat.java.
                                                        Activity.putExtra("newShape", true);
                                                        // Pass this value to Chat.java to identify the shape.
                                                        Activity.putExtra("uuid", uuid);
                                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                                        Activity.putExtra("fillColor", Color.argb(100, 255, 255, 0));
                                                        Activity.putExtra("radius", circle.getRadius());
                                                        startActivity(Activity);
                                                    } else {

                                                        // Carry the extras all the way to Chat.java.
                                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                                        // Pass this boolean value (true) to Chat.java.
                                                        Activity.putExtra("newShape", true);
                                                        // Pass this value to Chat.java to identify the shape.
                                                        Activity.putExtra("uuid", uuid);
                                                        // Pass this information to Chat.java to create a new circle in Firebase after someone writes a message.
                                                        Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                                        Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                                        Activity.putExtra("fillColor", Color.argb(100, 255, 255, 0));
                                                        Activity.putExtra("radius", circle.getRadius());
                                                        startActivity(Activity);
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

                createChatMenuIsOpen = false;
                return true;

            default:
                return false;
        }
    }

    // Returns the distance between 2 latitudes and longitudes in meters.
    private static float distanceGivenLatLng(double lat1, double lng1, double lat2, double lng2) {

        double earthRadius = 6371000; // Meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = sin(dLat/2) * sin(dLat/2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                        sin(dLng/2) * sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return (float) (earthRadius * c);
    }

    // Returns the angle between 2 latitudes and longitudes in degrees. If lat1, lng1 are circle's center, this will return 0 for 12 o'clock and 90 for 3 o'clock.
    private double angleFromCoordinate(double lat1, double lng1, double lat2, double lng2) {

        double dLon = (lng2 - lng1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double angle = Math.atan2(y, x);

        angle = Math.toDegrees(angle);
        angle = (angle + 360) % 360;
        //angle = 360 - angle; // count degrees counter-clockwise - remove to make clockwise

        return angle;
    }

    private LatLng latLngGivenDistance(double latitude, double longitude, double distanceInMetres, double bearing) {
        double brngRad = Math.toRadians(bearing);
        double latRad = Math.toRadians(latitude);
        double lonRad = Math.toRadians(longitude);
        int earthRadiusInMetres = 6371000;
        double distFrac = distanceInMetres / earthRadiusInMetres;

        double latitudeResult = Math.asin(sin(latRad) * cos(distFrac) + cos(latRad) * sin(distFrac) * cos(brngRad));
        double a = Math.atan2(sin(brngRad) * sin(distFrac) * cos(latRad), cos(distFrac) - sin(latRad) * sin(latitudeResult));
        double longitudeResult = (lonRad + a + 3 * Math.PI) % (2 * Math.PI) - Math.PI;


        return new LatLng (Math.toDegrees(latitudeResult), Math.toDegrees(longitudeResult));
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