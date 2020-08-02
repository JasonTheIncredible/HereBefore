package co.clixel.herebefore;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.andremion.counterfab.CounterFab;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
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
    private Circle newCircle, circleTemp, mCircle = null;
    private Polygon newPolygon, polygonTemp, mPolygon = null;
    private DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference(), firebaseCircles = rootRef.child("Circles"), firebasePolygons = rootRef.child("Polygons"), databaseReferenceOne, databaseReferenceTwo;
    private ValueEventListener eventListenerOne, eventListenerTwo, eventListenerThree;
    private SeekBar chatSizeSeekBar, chatSelectorSeekBar;
    private String preferredMapType, shapeUUID, marker0ID, marker1ID, marker2ID, marker3ID, marker4ID, marker5ID, marker6ID, marker7ID, selectedOverlappingShapeUUID, email;
    private Button createChatButton, chatViewsButton, mapTypeButton, settingsButton;
    private PopupMenu popupMapType, popupChatViews, popupCreateChat;
    private boolean stillLoadingCircles = true, stillLoadingPolygons = true, stillUpdatingCamera = true;
    private Boolean firstLoad = true, cameraMoved = false, waitingForBetterLocationAccuracy = false, badAccuracy = false, showingEverything = true, showingLarge = false, showingMedium = false, showingSmall = false, showingPoints = false,
            waitingForClicksToProcess = false, waitingForShapeInformationToProcess = false, markerOutsidePolygon = false, usedSeekBar = false,
            userIsWithinShape, selectingShape = false, threeMarkers = false, fourMarkers = false, fiveMarkers = false, sixMarkers = false, sevenMarkers = false, eightMarkers = false;
    private LatLng markerPositionAtVertexOfPolygon, marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position, selectedOverlappingShapeCircleLocation;
    private Double relativeAngle = 0.0, selectedOverlappingShapeCircleRadius;
    private Location mlocation;
    private List<LatLng> polygonPointsList, selectedOverlappingShapePolygonVertices;
    private ArrayList<String> overlappingShapesUUID = new ArrayList<>(), overlappingShapesCircleUUID = new ArrayList<>(), overlappingShapesPolygonUUID = new ArrayList<>();
    private ArrayList<LatLng> overlappingShapesCircleLocation = new ArrayList<>();
    private ArrayList<Double> overlappingShapesCircleRadius = new ArrayList<>();
    private ArrayList<java.util.List<LatLng>> overlappingShapesPolygonVertices = new ArrayList<>();
    private float x, y;
    private int chatsSize, dmCounter = 0, dmCounter1 = 0, dmCounter2 = 0;
    private Toast longToast;
    private View loadingIcon;
    private FloatingActionButton randomButton;
    private CounterFab dmButton;
    private LocationManager locationManager;
    private Query query;

    // Check if you can get rid of the extras in Settings.
    // Return if any value in Chat is incorrect or missing before sending to Firebase.
    // The nested dataSnapshot.getChildren() in DirectMentions is newly getting called for every mention.
    // // Maybe use first dataSnapshot, get newest data, add it to old dataSnapshot, then make that the new dataSnapshot?
    // Only download shapes in Map when necessary to cut down on database usage.
    // AppIntro on Github.
    // Make sure the secret stuff is secret.
    // Decrease app size / Check on accumulation of size over time.
    // Make sure Firebase has enough bandwidth.
    // Check on feedback.
    // Make sure aboutLibraries is up to date.
    // Check warning messages.
    // More ads.
    // Update existing values in Firebase so new users can view old shapes.
    // Add preference for shape color.
    // Make recyclerView load faster, possibly by adding layouts for all video/picture and then adding them when possible. Also, fix issue where images / videos are changing size with orientation change. Possible: Send image dimensions to Firebase and set a "null" image of that size.
    // Leave messages in locations that users get notified of when they enter the area.
    // Add ability to filter recyclerView by type of content (recorded at the scene...).
    // Work on deprecated methods.
    // Add geo-fencing.
    // Load preferences after logging out and back in - looks like it will require saving info to database; is this worth it?
    // Add ability to add both picture and video to firebase at the same time.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        setContentView(R.layout.map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.activity_maps);
        if (mapFragment != null) {

            mapFragment.getMapAsync(this);
        } else {

            Log.e(TAG, "onCreate() -> mapFragment == null");
            toastMessageLong("An error occurred while loading the map");
        }

        mapTypeButton = findViewById(R.id.mapTypeButton);
        settingsButton = findViewById(R.id.settingsButton);
        chatViewsButton = findViewById(R.id.chatViewsButton);
        dmButton = findViewById(R.id.dmButton);
        randomButton = findViewById(R.id.randomButton);
        createChatButton = findViewById(R.id.createChatButton);
        chatSizeSeekBar = findViewById(R.id.chatSizeSeekBar);
        chatSelectorSeekBar = findViewById(R.id.chatSelectorSeekBar);
        loadingIcon = findViewById(R.id.loadingIcon);
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Check permissions if they have not been granted.
        if (!(ContextCompat.checkSelfPermission(Map.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)) {

            checkLocationPermissions();
        }

        // Used to set dmButton badge number.
        // If user has a Google account, get email one way. Else, get email another way.
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {

            email = acct.getEmail();
        } else {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            email = sharedPreferences.getString("userToken", "null");
        }

        // Check if the user is logged in. If true, make the settings button visible.
        boolean loggedIn = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsFragment.KEY_SIGN_OUT, false);

        if (loggedIn) {

            settingsButton.setVisibility(View.VISIBLE);
            dmButton.setVisibility(View.VISIBLE);
        } else {

            settingsButton.setVisibility(View.GONE);
            settingsButton.setVisibility(View.GONE);
        }

        // Shows a menu to change the map type.
        mapTypeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Log.i(TAG, "onStart() -> mapTypeButton -> onClick");

                popupMapType = new PopupMenu(Map.this, mapTypeButton);
                popupMapType.setOnMenuItemClickListener(Map.this);
                popupMapType.inflate(R.menu.maptype_menu);
                popupMapType.show();
            }
        });

        // Go to settings.
        settingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Log.i(TAG, "onStart() -> settingsButton -> onClick");

                // Check location permissions.
                if (ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    loadingIcon.setVisibility(View.VISIBLE);

                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                @Override
                                public void onSuccess(Location location) {

                                    if (location != null) {

                                        cancelToasts();

                                        Intent Activity = new Intent(Map.this, Navigation.class);

                                        Activity.putExtra("userLatitude", location.getLatitude());
                                        Activity.putExtra("userLongitude", location.getLongitude());

                                        Activity.putExtra("noChat", true);

                                        loadingIcon.setVisibility(View.GONE);

                                        startActivity(Activity);
                                    } else {

                                        loadingIcon.setVisibility(View.GONE);
                                        Log.e(TAG, "onStart() -> settingsButton -> location == null");
                                        toastMessageLong("An error occurred: your location is null");
                                    }
                                }
                            });
                } else {

                    checkLocationPermissions();
                }
            }
        });

        // If new DMs, update dmButton badge.
        if (email != null) {

            databaseReferenceOne = rootRef.child("MessageThreads");
            eventListenerOne = new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    for (final DataSnapshot ds : dataSnapshot.getChildren()) {

                        if (ds.child("removedMentionDuplicates").getValue() != null) {

                            for (final DataSnapshot mention : ds.child("removedMentionDuplicates").getChildren()) {

                                if (mention.getValue() != null) {

                                    dmCounter1++;
                                    databaseReferenceTwo = rootRef.child("MessageThreads");
                                    eventListenerTwo = new ValueEventListener() {

                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            for (DataSnapshot dss : dataSnapshot.getChildren()) {

                                                String userUUID = (String) dss.child("userUUID").getValue();
                                                if (mention.getValue().toString().equals(userUUID)) {

                                                    dmCounter2++;
                                                    String userEmail = (String) dss.child("email").getValue();
                                                    if (userEmail != null) {

                                                        if (userEmail.equals(email)) {

                                                            Boolean seenByUser = (Boolean) ds.child("seenByUser").getValue();
                                                            if (seenByUser != null) {

                                                                if (!seenByUser) {

                                                                    dmCounter++;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // If these counters equal, the DMs have all been received from Firebase.
                                            // Therefore, switch to .limitToLast(1) to cut down on data usage.
                                            if (dmCounter1 == dmCounter2) {

                                                // dmCounter is going to be increased by 1 during addQuery, so temporarily decrease it by 1 here.
                                                if (dmCounter != 0) {

                                                    dmCounter--;
                                                }
                                                addQuery();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                            toastMessageLong(databaseError.getMessage());
                                        }
                                    };

                                    // Add the second Firebase listener.
                                    databaseReferenceTwo.addListenerForSingleValueEvent(eventListenerTwo);
                                }
                            }
                        }
                    }
                }

                public void onCancelled(@NonNull DatabaseError databaseError) {

                    toastMessageLong(databaseError.getMessage());
                }
            };

            // Add the first Firebase listener.
            databaseReferenceOne.addValueEventListener(eventListenerOne);
        }

        // Go to DMs.
        dmButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Log.i(TAG, "onStart() -> dmButton -> onClick");

                // Check location permissions.
                if (ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    loadingIcon.setVisibility(View.VISIBLE);

                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                @Override
                                public void onSuccess(Location location) {

                                    if (location != null) {

                                        cancelToasts();

                                        Intent Activity = new Intent(Map.this, Navigation.class);

                                        Activity.putExtra("userLatitude", location.getLatitude());
                                        Activity.putExtra("userLongitude", location.getLongitude());

                                        Activity.putExtra("noChat", true);
                                        Activity.putExtra("fromDMs", true);

                                        loadingIcon.setVisibility(View.GONE);

                                        startActivity(Activity);
                                    } else {

                                        loadingIcon.setVisibility(View.GONE);
                                        Log.e(TAG, "onStart() -> dmButton -> location == null");
                                        toastMessageLong("An error occurred: your location is null");
                                    }
                                }
                            });
                } else {

                    checkLocationPermissions();
                }
            }
        });

        // Go to random shape.
        randomButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Log.i(TAG, "onStart() -> randomButton -> onClick");

                // RNG.
                long time = System.currentTimeMillis();
                if (time % 2 == 0) {

                    firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            // Get a random child from Firebase.
                            long children = dataSnapshot.getChildrenCount();
                            int count = (int) children;
                            int random = new Random().nextInt(count - 1);

                            int i = 0;
                            for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                if (i == random) {

                                    CameraPosition cameraPosition = new CameraPosition.Builder()
                                            .target(new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue()))      // Sets the center of the map to user's location
                                            .zoom(18)                   // Sets the zoom
                                            .bearing(0)                // Sets the orientation of the camera
                                            .tilt(0)                   // Sets the tilt of the camera
                                            .build();                   // Creates a CameraPosition from the builder

                                    // Move the camera to the user's location once the map is available.
                                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);

                                    // Adjust boolean values to prevent this logic from being called again.
                                    firstLoad = false;
                                    cameraMoved = true;

                                    // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                                    stillUpdatingCamera = false;
                                    break;
                                }

                                i++;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                            Log.e(TAG, "DatabaseError");
                            toastMessageLong(databaseError.getMessage());
                        }
                    });
                } else {

                    firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            // Get a random child from Firebase.
                            long children = dataSnapshot.getChildrenCount();
                            // If no children exist, try circles.
                            if (children == 0) {

                                firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        // Get a random child from Firebase.
                                        long children = dataSnapshot.getChildrenCount();
                                        int count = (int) children;
                                        int random = new Random().nextInt(count);

                                        int i = 0;
                                        for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                            if (i == random) {

                                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                                        .target(new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue()))      // Sets the center of the map to user's location
                                                        .zoom(18)                   // Sets the zoom
                                                        .bearing(0)                // Sets the orientation of the camera
                                                        .tilt(0)                   // Sets the tilt of the camera
                                                        .build();                   // Creates a CameraPosition from the builder

                                                // Move the camera to the user's location once the map is available.
                                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);

                                                // Adjust boolean values to prevent this logic from being called again.
                                                firstLoad = false;
                                                cameraMoved = true;

                                                // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                                                stillUpdatingCamera = false;
                                                break;
                                            }

                                            i++;
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                        Log.e(TAG, "DatabaseError");
                                        toastMessageLong(databaseError.getMessage());
                                    }
                                });

                                return;
                            }
                            int count = (int) children;
                            int random = new Random().nextInt(count - 1);

                            int i = 0;
                            for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                if (i == random) {

                                    LatLng marker3Position = null;
                                    LatLng marker4Position = null;
                                    LatLng marker5Position = null;
                                    LatLng marker6Position = null;

                                    LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                                    if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                        marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                                    }
                                    if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                        marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                                    }
                                    if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                        marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                                    }
                                    if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                        marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                                    }
                                    if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                        LatLng marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                .target(marker7Position)      // Sets the center of the map to user's location
                                                .zoom(18)                   // Sets the zoom
                                                .bearing(0)                // Sets the orientation of the camera
                                                .tilt(0)                   // Sets the tilt of the camera
                                                .build();                   // Creates a CameraPosition from the builder

                                        // Move the camera to the user's location once the map is available.
                                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);

                                        // Adjust boolean values to prevent this logic from being called again.
                                        firstLoad = false;
                                        cameraMoved = true;

                                        // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                                        stillUpdatingCamera = false;
                                        break;
                                    } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                .target(marker6Position)      // Sets the center of the map to user's location
                                                .zoom(18)                   // Sets the zoom
                                                .bearing(0)                // Sets the orientation of the camera
                                                .tilt(0)                   // Sets the tilt of the camera
                                                .build();                   // Creates a CameraPosition from the builder

                                        // Move the camera to the user's location once the map is available.
                                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);

                                        // Adjust boolean values to prevent this logic from being called again.
                                        firstLoad = false;
                                        cameraMoved = true;

                                        // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                                        stillUpdatingCamera = false;
                                        break;
                                    } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                .target(marker5Position)      // Sets the center of the map to user's location
                                                .zoom(18)                   // Sets the zoom
                                                .bearing(0)                // Sets the orientation of the camera
                                                .tilt(0)                   // Sets the tilt of the camera
                                                .build();                   // Creates a CameraPosition from the builder

                                        // Move the camera to the user's location once the map is available.
                                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);

                                        // Adjust boolean values to prevent this logic from being called again.
                                        firstLoad = false;
                                        cameraMoved = true;

                                        // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                                        stillUpdatingCamera = false;
                                        break;
                                    } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                .target(marker4Position)      // Sets the center of the map to user's location
                                                .zoom(18)                   // Sets the zoom
                                                .bearing(0)                // Sets the orientation of the camera
                                                .tilt(0)                   // Sets the tilt of the camera
                                                .build();                   // Creates a CameraPosition from the builder

                                        // Move the camera to the user's location once the map is available.
                                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);

                                        // Adjust boolean values to prevent this logic from being called again.
                                        firstLoad = false;
                                        cameraMoved = true;

                                        // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                                        stillUpdatingCamera = false;
                                        break;
                                    } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                .target(marker3Position)      // Sets the center of the map to user's location
                                                .zoom(18)                   // Sets the zoom
                                                .bearing(0)                // Sets the orientation of the camera
                                                .tilt(0)                   // Sets the tilt of the camera
                                                .build();                   // Creates a CameraPosition from the builder

                                        // Move the camera to the user's location once the map is available.
                                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);

                                        // Adjust boolean values to prevent this logic from being called again.
                                        firstLoad = false;
                                        cameraMoved = true;

                                        // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                                        stillUpdatingCamera = false;
                                        break;
                                    } else {
                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                .target(marker2Position)      // Sets the center of the map to user's location
                                                .zoom(18)                   // Sets the zoom
                                                .bearing(0)                // Sets the orientation of the camera
                                                .tilt(0)                   // Sets the tilt of the camera
                                                .build();                   // Creates a CameraPosition from the builder

                                        // Move the camera to the user's location once the map is available.
                                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);

                                        // Adjust boolean values to prevent this logic from being called again.
                                        firstLoad = false;
                                        cameraMoved = true;

                                        // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                                        stillUpdatingCamera = false;
                                        break;
                                    }
                                }

                                i++;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                            Log.e(TAG, "DatabaseError");
                            toastMessageLong(databaseError.getMessage());
                        }
                    });
                }
            }
        });

        // Shows a menu to filter circle views.
        chatViewsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Log.i(TAG, "onStart() -> chatViewsButton -> onClick");

                popupChatViews = new PopupMenu(Map.this, chatViewsButton);
                popupChatViews.setOnMenuItemClickListener(Map.this);
                popupChatViews.inflate(R.menu.chatviews_menu);
                popupChatViews.show();
            }
        });

        // Shows a menu for creating chats.
        createChatButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Log.i(TAG, "onStart() -> createChatButton -> onClick");

                popupCreateChat = new PopupMenu(Map.this, createChatButton);
                popupCreateChat.setOnMenuItemClickListener(Map.this);
                popupCreateChat.inflate(R.menu.createchat_menu);
                // Check if the circle exists and adjust the menu items accordingly.
                if (newCircle != null || newPolygon != null) {

                    popupCreateChat.getMenu().findItem(R.id.createPolygon).setVisible(false);
                    popupCreateChat.getMenu().findItem(R.id.createCircle).setVisible(false);
                } else {

                    popupCreateChat.getMenu().findItem(R.id.removeShape).setVisible(false);
                }

                popupCreateChat.show();
            }
        });

        chatSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {

                Log.i(TAG, "onStart() -> chatSizeSeekBar -> onStartTrackingTouch");

                // Global variable used to prevent conflicts when the user updates the circle's radius with the marker rather than the seekBar.
                usedSeekBar = true;

                // Creates circle with markers.
                if (newCircle == null && newPolygon == null) {

                    // Check location permissions.
                    if (ContextCompat.checkSelfPermission(getBaseContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                    @Override
                                    public void onSuccess(Location location) {

                                        // Get last known location. In some rare situations, this can be null.
                                        if (location != null) {

                                            // Change shape color depending on the map type.
                                            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                                                Log.i(TAG, "onStart() -> chatSizeSeekBar -> onStartTrackingTouch -> yellow circle");

                                                // Make circle the size set by the seekBar.
                                                float circleSize = chatSizeSeekBar.getProgress();

                                                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                                marker1Position = new LatLng(latLng.latitude + (circleSize / 6371000) * (180 / Math.PI), latLng.longitude + (circleSize / 6371000) * (180 / Math.PI) / cos(latLng.latitude * Math.PI / 180));
                                                CircleOptions circleOptions =
                                                        new CircleOptions()
                                                                .center(latLng)
                                                                .clickable(true)
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

                                                newCircle = mMap.addCircle(circleOptions);
                                            } else {

                                                Log.i(TAG, "onStart() -> chatSizeSeekBar -> onStartTrackingTouch -> purple circle");

                                                // Make circle the size set by the seekBar.
                                                float circleSize = chatSizeSeekBar.getProgress();

                                                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                                marker1Position = new LatLng(latLng.latitude + (circleSize / 6371000) * (180 / Math.PI), latLng.longitude + (circleSize / 6371000) * (180 / Math.PI) / cos(latLng.latitude * Math.PI / 180));
                                                CircleOptions circleOptions =
                                                        new CircleOptions()
                                                                .center(latLng)
                                                                .clickable(true)
                                                                .radius(circleSize)
                                                                .strokeColor(Color.rgb(255, 0, 255))
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

                                                newCircle = mMap.addCircle(circleOptions);
                                            }
                                        } else {

                                            Log.e(TAG, "onStart() -> chatSizeSeekBar -> location == null");
                                            toastMessageLong("An error occurred: your location is null");
                                        }
                                    }
                                });
                    } else {

                        checkLocationPermissions();
                    }
                }
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                // Global variable used to prevent conflicts when the user updates the circle's radius with the marker rather than the seekBar.
                if (usedSeekBar) {

                    // Changes size of the circle and marker1 visibility.
                    if (newCircle != null) {

                        Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> circle");

                        newCircle.setRadius(progress);
                        marker1.setVisible(false);
                    }

                    if (newPolygon != null) {

                        // Get last known location. In some rare situations, this can be null.
                        if (mlocation != null) {

                            // Change shape color depending on the map type.
                            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                                // 3 markers.
                                if (chatSizeSeekBar.getProgress() <= 33 && !threeMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> yellow polygon -> 3 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }

                                // 4 markers.
                                if (chatSizeSeekBar.getProgress() > 33 && chatSizeSeekBar.getProgress() <= 66 && !fourMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> yellow polygon -> 4 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }

                                // 5 markers.
                                if (chatSizeSeekBar.getProgress() > 66 && chatSizeSeekBar.getProgress() <= 99 && !fiveMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> yellow polygon -> 5 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }

                                // 6 markers.
                                if (chatSizeSeekBar.getProgress() > 99 && chatSizeSeekBar.getProgress() <= 132 && !sixMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> yellow polygon -> 6 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }

                                // 7 markers.
                                if (chatSizeSeekBar.getProgress() > 132 && chatSizeSeekBar.getProgress() <= 165 && !sevenMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> yellow polygon -> 7 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }

                                // 8 markers.
                                if (chatSizeSeekBar.getProgress() > 165 && chatSizeSeekBar.getProgress() <= 200 && !eightMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> yellow polygon -> 8 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }
                            } else {

                                // 3 markers.
                                if (chatSizeSeekBar.getProgress() <= 33 && !threeMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> purple polygon -> 3 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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
                                                    .strokeColor(Color.rgb(255, 0, 255))
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }

                                // 4 markers.
                                if (chatSizeSeekBar.getProgress() > 33 && chatSizeSeekBar.getProgress() <= 66 && !fourMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> purple polygon -> 4 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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
                                                    .strokeColor(Color.rgb(255, 0, 255))
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }

                                // 5 markers.
                                if (chatSizeSeekBar.getProgress() > 66 && chatSizeSeekBar.getProgress() <= 99 && !fiveMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> purple polygon -> 5 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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
                                                    .strokeColor(Color.rgb(255, 0, 255))
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }

                                // 6 markers.
                                if (chatSizeSeekBar.getProgress() > 99 && chatSizeSeekBar.getProgress() <= 132 && !sixMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> purple polygon -> 6 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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
                                                    .strokeColor(Color.rgb(255, 0, 255))
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }

                                // 7 markers.
                                if (chatSizeSeekBar.getProgress() > 132 && chatSizeSeekBar.getProgress() <= 165 && !sevenMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> purple polygon -> 7 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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
                                                    .strokeColor(Color.rgb(255, 0, 255))
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }

                                // 8 markers.
                                if (chatSizeSeekBar.getProgress() > 165 && chatSizeSeekBar.getProgress() <= 200 && !eightMarkers) {

                                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> purple polygon -> 8 markers");

                                    newPolygon.remove();
                                    marker0.remove();
                                    marker1.remove();
                                    marker2.remove();
                                    newPolygon = null;
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
                                                    .strokeColor(Color.rgb(255, 0, 255))
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

                                    newPolygon = mMap.addPolygon(polygonOptions);
                                }
                            }
                        } else {

                            Log.e(TAG, "onStart() -> chatSizeSeekBar -> onProgressChanged -> polygon -> mLocation == null");
                            toastMessageLong("An error occurred: your location is null");
                        }
                    }
                }
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {

                Log.i(TAG, "onStart() -> chatSizeSeekBar -> onStopTrackingTouch");

                // Global variable used to prevent conflicts when the user updates the circle's radius with the marker rather than the seekBar.
                usedSeekBar = false;

                // Sets marker1's position on the circle's edge relative to where the user last left it, and sets marker1's visibility.
                if (newCircle != null) {

                    Log.i(TAG, "onStart() -> chatSizeSeekBar -> onStopTrackingTouch -> circle");

                    marker1.setPosition(latLngGivenDistance(newCircle.getCenter().latitude, newCircle.getCenter().longitude, chatSizeSeekBar.getProgress(), relativeAngle));

                    marker1.setVisible(true);
                }
            }
        });

        chatSelectorSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (chatSelectorSeekBar.getVisibility() == View.VISIBLE) {

                    // Create an arrayList that combines a representative array from a circle and polygon to get a full list that represents the two.
                    ArrayList<Object> combinedList = new ArrayList<>();

                    // Add the smaller array fist for consistency with the rest of the logic.
                    if (overlappingShapesCircleLocation.size() > overlappingShapesPolygonVertices.size()) {

                        combinedList.addAll(overlappingShapesPolygonVertices);
                        combinedList.addAll(overlappingShapesCircleLocation);
                    } else {

                        combinedList.addAll(overlappingShapesCircleLocation);
                        combinedList.addAll(overlappingShapesPolygonVertices);
                    }

                    selectedOverlappingShapeUUID = overlappingShapesUUID.get(chatSelectorSeekBar.getProgress());

                    if (selectedOverlappingShapeUUID != null) {

                        if (circleTemp != null) {

                            circleTemp.remove();
                        }

                        if (polygonTemp != null) {

                            polygonTemp.remove();
                        }

                        if (combinedList.get(chatSelectorSeekBar.getProgress()) instanceof LatLng) {

                            if (overlappingShapesCircleLocation.size() > overlappingShapesPolygonVertices.size()) {

                                selectedOverlappingShapeCircleLocation = overlappingShapesCircleLocation.get(chatSelectorSeekBar.getProgress() - overlappingShapesPolygonVertices.size());
                                selectedOverlappingShapeCircleRadius = overlappingShapesCircleRadius.get(chatSelectorSeekBar.getProgress() - overlappingShapesPolygonVertices.size());
                            } else {

                                selectedOverlappingShapeCircleLocation = overlappingShapesCircleLocation.get(chatSelectorSeekBar.getProgress());
                                selectedOverlappingShapeCircleRadius = overlappingShapesCircleRadius.get(chatSelectorSeekBar.getProgress());
                            }

                            if (mMap.getMapType() == 2 || mMap.getMapType() == 4) {

                                circleTemp = mMap.addCircle(
                                        new CircleOptions()
                                                .center(selectedOverlappingShapeCircleLocation)
                                                .clickable(true)
                                                .fillColor(Color.argb(100, 255, 255, 0))
                                                .radius(selectedOverlappingShapeCircleRadius)
                                                .strokeColor(Color.rgb(255, 255, 0))
                                                .strokeWidth(3f)
                                                .zIndex(2)
                                );
                            } else {

                                circleTemp = mMap.addCircle(
                                        new CircleOptions()
                                                .center(selectedOverlappingShapeCircleLocation)
                                                .clickable(true)
                                                .fillColor(Color.argb(100, 255, 0, 255))
                                                .radius(selectedOverlappingShapeCircleRadius)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                                .zIndex(2)
                                );
                            }

                            // Used when getting rid of the shapes in onMapClick.
                            circleTemp.setTag(selectedOverlappingShapeUUID);
                            circleTemp.setCenter(selectedOverlappingShapeCircleLocation);
                            circleTemp.setRadius(selectedOverlappingShapeCircleRadius);
                        } else {

                            if (overlappingShapesPolygonVertices.size() > overlappingShapesCircleLocation.size() || overlappingShapesPolygonVertices.size() == overlappingShapesCircleLocation.size()) {

                                selectedOverlappingShapePolygonVertices = overlappingShapesPolygonVertices.get(chatSelectorSeekBar.getProgress() - overlappingShapesCircleLocation.size());
                            } else {

                                selectedOverlappingShapePolygonVertices = overlappingShapesPolygonVertices.get(chatSelectorSeekBar.getProgress());
                            }

                            if (mMap.getMapType() == 2 || mMap.getMapType() == 4) {

                                polygonTemp = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .fillColor(Color.argb(100, 255, 255, 0))
                                                .strokeColor(Color.rgb(255, 255, 0))
                                                .strokeWidth(3f)
                                                .addAll(selectedOverlappingShapePolygonVertices)
                                                .zIndex(2)
                                );
                            } else {

                                polygonTemp = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .fillColor(Color.argb(100, 255, 0, 255))
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                                .addAll(selectedOverlappingShapePolygonVertices)
                                                .zIndex(2)
                                );
                            }

                            // Used when getting rid of the shapes in onMapClick.
                            polygonTemp.setTag(selectedOverlappingShapeUUID);
                        }
                    } else {

                        Log.e(TAG, "onStart() -> chatSelectorSeekBar -> onProgressChanged -> selectedOverlappingShapeUUID == null");
                        toastMessageLong("An error occurred");
                    }

                    selectingShape = true;
                }
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
            }
        });

        // Clear the cache. This should clear the issue where Chat.java was creating files that were never deleted.
        deleteDirectory(this.getCacheDir());
    }

    // Change to .limitToLast(1) to cut down on data usage. Otherwise, EVERY child at this node will be downloaded every time the child is updated.
    private void addQuery() {

        Log.i(TAG, "addQuery()");

        databaseReferenceOne.removeEventListener(eventListenerOne);

        query = rootRef.child("MessageThreads").limitToLast(1);

        eventListenerThree = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (final DataSnapshot ds : dataSnapshot.getChildren()) {

                    if (ds.child("removedMentionDuplicates").getValue() != null) {

                        for (final DataSnapshot mention : ds.child("removedMentionDuplicates").getChildren()) {

                            if (mention.getValue() != null) {

                                databaseReferenceTwo = rootRef.child("MessageThreads");
                                eventListenerTwo = new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        for (DataSnapshot dss : dataSnapshot.getChildren()) {

                                            String userUUID = (String) dss.child("userUUID").getValue();
                                            if (mention.getValue().toString().equals(userUUID)) {

                                                String userEmail = (String) dss.child("email").getValue();
                                                if (userEmail != null) {

                                                    if (userEmail.equals(email)) {

                                                        Boolean seenByUser = (Boolean) ds.child("seenByUser").getValue();
                                                        if (seenByUser != null) {

                                                            if (!seenByUser) {

                                                                dmCounter++;
                                                                dmButton.setCount(dmCounter);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                        toastMessageLong(databaseError.getMessage());
                                    }
                                };

                                // Add the second Firebase listener.
                                databaseReferenceTwo.addListenerForSingleValueEvent(eventListenerTwo);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        query.addValueEventListener(eventListenerThree);
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        firstLoad = false;
        waitingForShapeInformationToProcess = false;
        waitingForClicksToProcess = false;
        selectingShape = false;
        chatsSize = 0;
        showingEverything = true;
        showingLarge = false;
        showingMedium = false;
        showingSmall = false;
        showingPoints = false;
        stillLoadingCircles = true;
        stillLoadingPolygons = true;
        // Update the following boolean to prevent the loading icon from appearing after the user restarts.
        stillUpdatingCamera = false;
        dmCounter = 0;
        dmCounter1 = 0;
        dmCounter2 = 0;

        if (dmButton != null) {

            dmButton.setCount(0);
        }

        // Clear map before adding new Firebase circles in onStart() to prevent overlap.
        // Set shape to null so changing chatSizeSeekBar in onStart() will create a circle and createChatButton will reset itself.
        if (mMap != null) {

            mMap.getUiSettings().setScrollGesturesEnabled(true);

            // Cut down on code by using one method for the shared code from onMapReady() and onRestart().
            onMapReadyAndRestart();
        }

        if (newPolygon != null) {

            newPolygon = null;
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

                marker3 = null;
                marker3Position = null;
                marker3ID = null;
            }

            if (marker4 != null) {

                marker4 = null;
                marker4Position = null;
                marker4ID = null;
            }

            if (marker5 != null) {

                marker5 = null;
                marker5Position = null;
                marker5ID = null;
            }

            if (marker6 != null) {

                marker6 = null;
                marker6Position = null;
                marker6ID = null;
            }

            if (marker7 != null) {

                marker7 = null;
                marker7Position = null;
                marker7ID = null;
            }
        }

        if (newCircle != null) {

            newCircle = null;
            marker0 = null;
            marker1 = null;
            marker0Position = null;
            marker1Position = null;
            marker0ID = null;
            marker1ID = null;
        }

        if (polygonTemp != null) {

            polygonTemp.remove();
        }

        if (circleTemp != null) {

            circleTemp.remove();
        }

        mCircle = null;
        mPolygon = null;
        chatSizeSeekBar.setProgress(0);
        relativeAngle = 0.0;
        usedSeekBar = false;
        selectingShape = false;
        mlocation = null;
        threeMarkers = false;
        fourMarkers = false;
        fiveMarkers = false;
        sixMarkers = false;
        sevenMarkers = false;
        eightMarkers = false;
        polygonPointsList = null;
        markerOutsidePolygon = false;
        userIsWithinShape = false;

        overlappingShapesUUID = new ArrayList<>();
        overlappingShapesCircleUUID = new ArrayList<>();
        overlappingShapesPolygonUUID = new ArrayList<>();
        overlappingShapesCircleLocation = new ArrayList<>();
        overlappingShapesCircleRadius = new ArrayList<>();
        overlappingShapesPolygonVertices = new ArrayList<>();

        // Get rid of the chatSelectorSeekBar.
        if (chatSelectorSeekBar.getVisibility() != View.INVISIBLE) {

            chatSelectorSeekBar.setVisibility(View.INVISIBLE);
            chatSizeSeekBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.i(TAG, "onResume()");

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Check if GPS is enabled.
        if (locationManager != null) {

            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                // If GPS is disabled, show an alert dialog and have the user turn GPS on.
                new buildAlertNoGPS(this).execute();
            }
        } else {

            Log.e(TAG, "onResume() -> manager == null");
            toastMessageLong("An error occurred while checking if GPS is enabled");
        }

        // Start updating location.
        if (ContextCompat.checkSelfPermission(Map.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            String provider = LocationManager.NETWORK_PROVIDER;
            if (locationManager != null) {

                locationManager.requestLocationUpdates(provider, 5000, 0, this);
                startLocationUpdates();
            } else {

                Log.e(TAG, "onResume() -> locationManager == null");
                toastMessageLong("Error retrieving your location");
            }
        } else {

            checkLocationPermissions();
        }
    }

    // If GPS is disabled, show an alert dialog and have the user turn GPS on.
    private static class buildAlertNoGPS extends AsyncTask<Void, Void, Void> {

        AlertDialog.Builder builder;
        private WeakReference<Map> activityWeakRef;

        buildAlertNoGPS(Map activity) {

            activityWeakRef = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            return null;
        }

        @Override
        protected void onPostExecute(Void results) {

            super.onPostExecute(results);

            Log.i(TAG, "buildAlertNoGPS -> onPostExecute()");

            if (activityWeakRef != null && activityWeakRef.get() != null) {

                builder = new AlertDialog.Builder(activityWeakRef.get());

                // If GPS is disabled, show an alert dialog and have the user turn GPS on.
                builder.setCancelable(false)
                        .setTitle("GPS Disabled")
                        .setMessage("Please enable your location services on the following screen.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int i) {

                                Activity activity = activityWeakRef.get();

                                activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        })
                        .create()
                        .show();
            }
        }
    }

    @Override
    protected void onPause() {

        Log.i(TAG, "onPause()");

        // Remove updating location information.
        if (locationManager != null) {

            if (ContextCompat.checkSelfPermission(Map.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                locationManager.removeUpdates(Map.this);
            }
        }

        if (locationManager != null) {

            locationManager = null;
        }

        super.onPause();
    }

    @Override
    protected void onStop() {

        Log.i(TAG, "onStop()");

        if (mapTypeButton != null) {

            mapTypeButton.setOnClickListener(null);
        }

        if (settingsButton != null) {

            settingsButton.setOnClickListener(null);
        }

        if (popupMapType != null) {

            popupMapType.setOnDismissListener(null);
        }

        if (chatViewsButton != null) {

            chatViewsButton.setOnClickListener(null);
        }

        if (popupChatViews != null) {

            popupChatViews.setOnDismissListener(null);
        }

        if (dmButton != null) {

            dmButton.setOnClickListener(null);
        }

        if (databaseReferenceOne != null) {

            if (eventListenerOne != null) {

                databaseReferenceOne.removeEventListener(eventListenerOne);
            }

            databaseReferenceOne = null;
        }

        if (databaseReferenceTwo != null) {

            databaseReferenceTwo.removeEventListener(eventListenerTwo);
            databaseReferenceTwo = null;
        }

        if (query != null) {

            query.removeEventListener(eventListenerThree);
            query = null;
        }

        if (eventListenerOne != null) {

            eventListenerOne = null;
        }

        if (eventListenerTwo != null) {

            eventListenerTwo = null;
        }

        if (eventListenerThree != null) {

            eventListenerThree = null;
        }

        if (createChatButton != null) {

            createChatButton.setOnClickListener(null);
        }

        if (popupCreateChat != null) {

            popupCreateChat.setOnDismissListener(null);
        }

        if (chatSizeSeekBar != null) {

            chatSizeSeekBar.setOnSeekBarChangeListener(null);
        }

        if (chatSelectorSeekBar != null) {

            chatSelectorSeekBar.setOnSeekBarChangeListener(null);
        }

        if (mMap != null) {

            mMap.setOnCircleClickListener(null);
            mMap.setOnPolygonClickListener(null);
            mMap.setOnMarkerDragListener(null);
            mMap.setOnMarkerClickListener(null);
            mMap.setOnMapClickListener(null);
            mMap.setOnCameraMoveListener(null);
        }

        cancelToasts();

        loadingIcon.setVisibility(View.GONE);

        super.onStop();
    }

    private void cancelToasts() {

        if (longToast != null) {

            longToast.cancel();
        }
    }

    private void checkLocationPermissions() {

        Log.i(TAG, "checkLocationPermissions()");

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Request_User_Location_Code);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            new locationPermissionAlertDialog(this).execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.i(TAG, "onRequestPermissionsResult()");

        // If request is cancelled, the result arrays are empty.
        if (requestCode == Request_User_Location_Code) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Permission was granted, yay! Do the location-related task you need to do.
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    startLocationUpdates();
                }
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new locationPermissionAlertDialog(this).execute();
            } else if (grantResults.length > 0
                    && grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                // User denied permission and checked "Don't ask again!"
                toastMessageLong("Location permission is required. Please enable it manually through the Android settings menu.");
            }
        }
    }

    // Show an explanation as to why location permission is necessary.
    private static class locationPermissionAlertDialog extends AsyncTask<Void, Void, Void> {

        AlertDialog.Builder builder;
        private WeakReference<Map> activityWeakRef;

        locationPermissionAlertDialog(Map activity) {

            activityWeakRef = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            super.onPostExecute(result);

            Log.i(TAG, "locationPermissionAlertDialog -> onPostExecute()");

            if (activityWeakRef != null && activityWeakRef.get() != null) {

                builder = new AlertDialog.Builder(activityWeakRef.get());

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                builder.setCancelable(false)
                        .setTitle("Device Location Required")
                        .setMessage("Here Before needs permission to use your location to find chat areas around you.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(activityWeakRef.get(),
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        Request_User_Location_Code);
                            }
                        })
                        .create()
                        .show();
            }
        }
    }

    protected void startLocationUpdates() {

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

        // New Google API SDK v11 uses getFusedLocationProviderClient(this).
        if (ContextCompat.checkSelfPermission(Map.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {

                        @Override
                        public void onLocationResult(LocationResult locationResult) {

                            onLocationChanged(locationResult.getLastLocation());
                        }
                    },

                    Looper.myLooper());
        } else {

            checkLocationPermissions();
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        if (mMap != null) {

            // If this is the first time loading the map and the user has NOT changed the camera position manually (from onMapReadyAndRestart() -> onCameraMoveListener) after the camera was changed programmatically with bad accuracy,
            // OR the camera position was changed by the user BEFORE the camera position was changed programmatically, this will get called to either change the camera position programmatically with good accuracy
            // or update it with bad accuracy and then wait to update it again with good accuracy assuming the user does not update it manually before it can be updated with good accuracy. This also gets called if the camera is zoomed
            // out after restart to get around bugs involving alert dialogs at start-up.
            // The following line allows the blue dot to appear on the map.

            // Check location permissions.
            if (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                mMap.setMyLocationEnabled(true);
                if (firstLoad && !cameraMoved && location.getAccuracy() < 60) {

                    Log.i(TAG, "updateLocation() -> Good accuracy");

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to user's location
                            .zoom(18)                   // Sets the zoom
                            .bearing(0)                // Sets the orientation of the camera
                            .tilt(0)                   // Sets the tilt of the camera
                            .build();                   // Creates a CameraPosition from the builder

                    // Move the camera to the user's location once the map is available.
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                    // Adjust boolean values to prevent this logic from being called again.
                    firstLoad = false;
                    cameraMoved = true;

                    // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                    stillUpdatingCamera = false;

                    if (!stillLoadingCircles && !stillLoadingPolygons) {

                        loadingIcon.setVisibility(View.GONE);
                    }
                } else if (firstLoad && !badAccuracy && location.getAccuracy() >= 60) {

                    Log.i(TAG, "updateLocation() -> Bad accuracy");

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to user's location
                            .zoom(18)                   // Sets the zoom
                            .bearing(0)                // Sets the orientation of the camera
                            .tilt(0)                   // Sets the tilt of the camera
                            .build();                   // Creates a CameraPosition from the builder

                    // Move the camera to the user's location once the map is available.
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                    // Adjust boolean values to prevent this logic from being called again.
                    badAccuracy = true;
                    waitingForBetterLocationAccuracy = true;

                    // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                    stillUpdatingCamera = false;

                    if (!stillLoadingCircles && !stillLoadingPolygons) {

                        loadingIcon.setVisibility(View.GONE);
                    }
                }
            } else {

                checkLocationPermissions();
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {

        Log.i(TAG, "onProviderDisabled()");
        toastMessageLong("Your location provider is disabled");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.i(TAG, "onMapReady()");

        mMap = googleMap;

        // Cut down on code by using one method for the shared code from onMapReady() and onRestart().
        onMapReadyAndRestart();
    }

    // Cut down on code by using one method for the shared code from onMapReady() and onRestart().
    private void onMapReadyAndRestart() {

        Log.i(TAG, "onMapReadyAndRestart()");

        // Update to the user's preferences.
        loadPreferences();
        updatePreferences();

        // Keep the marker on the shapes to allow for dragging.
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

            @Override
            public void onMarkerDragStart(Marker marker) {

                Log.i(TAG, "onMapReadyAndRestart() -> onMarkerDragStart");

                adjustShape(marker);
            }

            @Override
            public void onMarkerDrag(Marker marker) {

                Log.i(TAG, "onMapReadyAndRestart() -> onMarkerDrag");

                adjustShape(marker);
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

                Log.i(TAG, "onMapReadyAndRestart() -> onMarkerDragEnd");

                LatLng markerPosition = marker.getPosition();

                if (newCircle != null) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onMarkerDragEnd -> circle");

                    // Sets marker1's position on the circle's edge relative to where the user last left marker1.
                    if (marker.getId().equals(marker0ID)) {

                        marker1.setPosition(latLngGivenDistance(newCircle.getCenter().latitude, newCircle.getCenter().longitude, newCircle.getRadius(), relativeAngle));

                        marker1.setVisible(true);
                    }

                    if (marker.getId().equals(marker1ID)) {

                        // Update the global variable with the angle the user left the marker's position. This is used if the user drags the center marker.
                        relativeAngle = angleFromCoordinate(newCircle.getCenter().latitude, newCircle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude);

                        // Keep the seekBar's progress aligned with the marker.
                        if (newCircle.getRadius() < 200) {

                            chatSizeSeekBar.setProgress((int) distanceGivenLatLng(newCircle.getCenter().latitude, newCircle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude));
                        }

                        // Limits the size of the circle, keeps marker1's position on the circle's edge at the same angle relative to where the user last left it, and keeps the seekBar's progress aligned with the marker.
                        if (newCircle.getRadius() >= 200) {

                            marker.setPosition(latLngGivenDistance(newCircle.getCenter().latitude, newCircle.getCenter().longitude, 200, relativeAngle));

                            chatSizeSeekBar.setProgress(200);
                        }
                    }
                }

                // Update the global variable with the marker's position.
                if (newPolygon != null) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onMarkerDragEnd -> polygon");

                    // If the marker is dropped outside of the polygon, set it to the last known position where it was in the polygon.
                    if (!PolyUtil.containsLocation(markerPosition.latitude, markerPosition.longitude, newPolygon.getPoints(), false)) {

                        marker.setPosition(markerPositionAtVertexOfPolygon);
                    }

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
                }
            }
        });

        // Go to Chat.java after clicking on a circle's middle marker.
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {

                if (newCircle != null) {

                    if (marker.getId().equals(marker0ID)) {

                        Log.i(TAG, "onMapReadyAndRestart() -> onMarkerClick -> circle -> marker0");

                        // End this method if the method is already being processed from another shape clicking event.
                        if (waitingForShapeInformationToProcess) {

                            return false;
                        }

                        // Update boolean to prevent double clicking a shape.
                        waitingForShapeInformationToProcess = true;

                        // Inform the user is the circle is too small.
                        if (newCircle.getRadius() < 1) {

                            toastMessageLong("Please make the shape larger");

                            return false;
                        }

                        // Generate a uuid, as the shape is new.
                        shapeUUID = UUID.randomUUID().toString();

                        // Check if user is within the circle before going to the recyclerviewlayout.
                        FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                        // Check location permissions.
                        if (ContextCompat.checkSelfPermission(getBaseContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {

                            mFusedLocationClient.getLastLocation()
                                    .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {
                                        @Override
                                        public void onSuccess(Location location) {

                                            if (location != null) {

                                                float[] distance = new float[2];

                                                Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                                        newCircle.getCenter().latitude, newCircle.getCenter().longitude, distance);

                                                // Boolean; will be true if user is within the circle upon circle click.
                                                userIsWithinShape = !(distance[0] > newCircle.getRadius());
                                            } else {

                                                Log.e(TAG, "onMapReadyAndRestart() -> onMarkerClick -> location == null");
                                                mMap.getUiSettings().setScrollGesturesEnabled(true);
                                                loadingIcon.setVisibility(View.GONE);
                                                toastMessageLong("An error occurred: your location is null");
                                            }
                                        }
                                    });

                            // Check if the user is already signed in.
                            if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                // User is signed in.

                                // Compare the uuid to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                                firebaseCircles.orderByChild("shapeUUID").equalTo(shapeUUID).addListenerForSingleValueEvent(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        // If the uuid already exists in Firebase, generate another uuid and try again.
                                        if (dataSnapshot.exists()) {

                                            Log.i(TAG, "onMapReadyAndRestart() -> onMarkerClick -> user signed in -> circle -> marker0 -> shapeUUID exists");

                                            // uuid exists in Firebase. Generate another and try again.

                                            // Generate another uuid and try again.
                                            shapeUUID = UUID.randomUUID().toString();

                                            // Carry the extras all the way to Chat.java.
                                            Intent Activity = new Intent(Map.this, Navigation.class);
                                            goToNextActivityCircle(Activity, newCircle, true);
                                        } else {

                                            Log.i(TAG, "onMapReadyAndRestart() -> onMarkerClick -> user signed in -> circle -> marker0 -> shapeUUID does not exist");

                                            // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                            // Carry the extras all the way to Chat.java.
                                            Intent Activity = new Intent(Map.this, Navigation.class);
                                            goToNextActivityCircle(Activity, newCircle, true);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                        Log.e(TAG, "DatabaseError");
                                        mMap.getUiSettings().setScrollGesturesEnabled(true);
                                        loadingIcon.setVisibility(View.GONE);
                                        toastMessageLong(databaseError.getMessage());
                                    }
                                });
                            } else {

                                // No user is signed in.

                                // Compare the uuid to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                                firebaseCircles.orderByChild("shapeUUID").equalTo(shapeUUID).addListenerForSingleValueEvent(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        // If the uuid already exists in Firebase, generate another uuid and try again.
                                        if (dataSnapshot.exists()) {

                                            Log.i(TAG, "onMapReadyAndRestart() -> onMarkerClick -> no user signed in -> circle -> marker0 -> shapeUUID exist");

                                            // uuid exists in Firebase. Generate another and try again.

                                            // Generate another UUID and try again.
                                            shapeUUID = UUID.randomUUID().toString();

                                            // Carry the extras all the way to Chat.java.
                                            Intent Activity = new Intent(Map.this, SignIn.class);
                                            goToNextActivityCircle(Activity, newCircle, true);
                                        } else {

                                            Log.i(TAG, "onMapReadyAndRestart() -> onMarkerClick -> no user signed in -> circle -> marker0 -> shapeUUID does not exist");

                                            // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                            // Carry the extras all the way to Chat.java.
                                            Intent Activity = new Intent(Map.this, SignIn.class);
                                            goToNextActivityCircle(Activity, newCircle, true);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                        Log.e(TAG, "DatabaseError");
                                        mMap.getUiSettings().setScrollGesturesEnabled(true);
                                        loadingIcon.setVisibility(View.GONE);
                                        toastMessageLong(databaseError.getMessage());
                                    }
                                });
                            }
                        } else {

                            checkLocationPermissions();
                        }
                    }
                }

                return true;
            }
        });

        // Go to Chat.java when clicking on a polygon.
        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {

            @Override
            public void onPolygonClick(final Polygon polygon) {

                // If the user tries to click on a polygon that is not a polygonTemp while polygonTemp exists, return.
                if (chatSelectorSeekBar.getVisibility() == View.VISIBLE && (polygon.getTag() != selectedOverlappingShapeUUID)) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> Selected polygon is not a polygonTemp. Returning");
                    return;
                }

                // Change boolean value so the x and y values in touchAgain() from dispatchTouchEvent() do not change.
                waitingForClicksToProcess = true;

                // While clicking through the circles, if a circle does not have a tag, it is new. Therefore, go directly to the recyclerviewlayout, as this is probably the recyclerviewlayout the user wants to enter.
                if (polygon.getTag() == null) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> User clicked on a new polygon");

                    // End this method if the method is already being processed from another shape clicking event.
                    if (waitingForShapeInformationToProcess) {

                        return;
                    }

                    // Update boolean to prevent double clicking a shape.
                    waitingForShapeInformationToProcess = true;

                    // Inform the user is the circle is too small.
                    if (SphericalUtil.computeArea(polygonPointsList) < Math.PI) {

                        toastMessageLong("Please make the shape larger");

                        return;
                    }

                    // Generate a uuid, as the shape is new.
                    shapeUUID = UUID.randomUUID().toString();

                    // Check location permissions.
                    if (ContextCompat.checkSelfPermission(getBaseContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        // Check if user is within the circle before going to the recyclerviewlayout.
                        FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                    @Override
                                    public void onSuccess(Location location) {

                                        if (location != null) {

                                            // Boolean; will be true if user is within the shape upon shape click.
                                            userIsWithinShape = PolyUtil.containsLocation(location.getLatitude(), location.getLongitude(), polygon.getPoints(), false);

                                            // Check if the user is already signed in.
                                            if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                                // User is signed in.

                                                firebasePolygons.orderByChild("shapeUUID").equalTo(shapeUUID).addListenerForSingleValueEvent(new ValueEventListener() {

                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                        // If the uuid already exists in Firebase, generate another uuid and try again.
                                                        if (dataSnapshot.exists()) {

                                                            Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> New polygon -> User signed in -> shapeUUID exists");

                                                            // uuid exists in Firebase. Generate another and try again.

                                                            // Generate another uuid and try again.
                                                            shapeUUID = UUID.randomUUID().toString();

                                                            // Carry the extras all the way to Chat.java.
                                                            Intent Activity = new Intent(Map.this, Navigation.class);

                                                            // Pass this information to Chat.java to create a new shape in Firebase after someone writes a recyclerviewlayout.
                                                            goToNextActivityPolygon(Activity, true);
                                                        } else {

                                                            Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> New polygon -> User signed in -> shapeUUID does not exist");

                                                            // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                                            // Carry the extras all the way to Chat.java.
                                                            Intent Activity = new Intent(Map.this, Navigation.class);

                                                            // Pass this information to Chat.java to create a new shape in Firebase after someone writes a recyclerviewlayout.
                                                            goToNextActivityPolygon(Activity, true);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        Log.e(TAG, "DatabaseError");
                                                        mMap.getUiSettings().setScrollGesturesEnabled(true);
                                                        loadingIcon.setVisibility(View.GONE);
                                                        toastMessageLong(databaseError.getMessage());
                                                    }
                                                });
                                            } else {

                                                // No user is signed in.

                                                firebasePolygons.orderByChild("shapeUUID").equalTo(shapeUUID).addListenerForSingleValueEvent(new ValueEventListener() {

                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                        // If the uuid already exists in Firebase, generate another uuid and try again.
                                                        if (dataSnapshot.exists()) {

                                                            Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> New polygon -> No user signed in -> shapeUUID exists");

                                                            // uuid exists in Firebase. Generate another and try again.

                                                            // Generate another uuid and try again.
                                                            shapeUUID = UUID.randomUUID().toString();

                                                            // Carry the extras to SignIn.java
                                                            Intent Activity = new Intent(Map.this, SignIn.class);

                                                            // Pass this information to Chat.java to create a new shape in Firebase after someone writes a recyclerviewlayout.
                                                            goToNextActivityPolygon(Activity, true);
                                                        } else {

                                                            Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> New polygon -> No user signed in -> shapeUUID does not exist");

                                                            // uuid does not already exist in Firebase. Go to SignIn.java with the uuid.

                                                            // Carry the extras to SignIn.java.
                                                            Intent Activity = new Intent(Map.this, Navigation.class);

                                                            // Pass this information to Chat.java to create a new shape in Firebase after someone writes a recyclerviewlayout.
                                                            goToNextActivityPolygon(Activity, true);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        Log.e(TAG, "DatabaseError");
                                                        mMap.getUiSettings().setScrollGesturesEnabled(true);
                                                        loadingIcon.setVisibility(View.GONE);
                                                        toastMessageLong(databaseError.getMessage());
                                                    }
                                                });
                                            }
                                        } else {

                                            Log.e(TAG, "onMapReadyAndRestart() -> onPolygonClick -> polygon.getTag() == null -> location == null");
                                            mMap.getUiSettings().setScrollGesturesEnabled(true);
                                            loadingIcon.setVisibility(View.GONE);
                                            toastMessageLong("An error occurred: your location is null");
                                        }
                                    }
                                });
                    } else {

                        checkLocationPermissions();
                    }

                    // As getFusedLocationProviderClient is asynchronous, this return statement will prevent executing the rest of the code.
                    return;
                }

                // Click through all shapes, using the z-index to figure out which ones have not been cycled through. All the information to the arrayLists to be used by chatSelectorSeekBar.
                if (polygon.getZIndex() == 0 && polygon.getTag() != null) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> Lowering z-index of a polygon");

                    // Prevent the map from scrolling so the same spot will be clicked again in touchAgain().
                    if (mMap.getUiSettings().isScrollGesturesEnabled()) {

                        mMap.getUiSettings().setScrollGesturesEnabled(false);
                    }

                    if (loadingIcon.getVisibility() != View.VISIBLE) {

                        loadingIcon.setVisibility(View.VISIBLE);
                    }

                    // Drop the z-index to metaphorically check it off the "to click" list.
                    polygon.setZIndex(-1);

                    // Add the information to arrayLists to be used by chatSelectorSeekBar.
                    overlappingShapesUUID.add(polygon.getTag().toString());
                    overlappingShapesPolygonUUID.add(polygon.getTag().toString());
                    overlappingShapesPolygonVertices.add(polygon.getPoints());

                    // If the user zooms out and a shape is too small, touchAgain() will not touch the shape again (I'm not sure why).
                    // Therefore, save an instance of the shape so that if onMapClick is called, it will just go to the last shape.
                    mPolygon = polygon;

                    // Programmatically click the same spot again.
                    touchAgain();

                    return;
                }

                waitingForClicksToProcess = false;

                // This will get called after the last shape is programmatically clicked.
                chatsSize = overlappingShapesUUID.size();

                // If none of the clicked shapes are new, get rid of any new shapes.
                if (!overlappingShapesUUID.contains("new")) {

                    // Remove the polygon and markers.
                    if (newPolygon != null) {

                        newPolygon.remove();
                        marker0.remove();
                        marker1.remove();
                        marker2.remove();
                        newPolygon = null;
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
                    if (newCircle != null) {

                        newCircle.remove();

                        if (marker0 != null) {

                            marker0.remove();
                        }

                        if (marker1 != null) {
                            marker1.remove();
                        }

                        newCircle = null;
                        marker0 = null;
                        marker1 = null;
                        marker0Position = null;
                        marker1Position = null;
                        marker0ID = null;
                        marker1ID = null;
                    }
                }

                // If selectingShape, user has selected a highlighted shape. Similar logic applies to originally only clicking on one circle.
                if (selectingShape || (chatsSize == 1 && polygon.getTag() != null)) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> User selected a polygon");

                    // End this method if the method is already being processed from another shape clicking event.
                    if (waitingForShapeInformationToProcess) {

                        return;
                    }

                    // Update boolean to prevent double clicking a shape.
                    waitingForShapeInformationToProcess = true;

                    // "New" shapes are automatically clicked. Therefore, get the ID set by Firebase to identify which circle the user clicked on.
                    if (chatsSize == 1) {

                        shapeUUID = (String) polygon.getTag();
                    } else {

                        shapeUUID = selectedOverlappingShapeUUID;
                    }

                    // Check if user is within the shape before going to the recyclerviewlayout.
                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                @Override
                                public void onSuccess(Location location) {

                                    if (location != null) {

                                        // Boolean; will be true if user is within the shape upon shape click.
                                        userIsWithinShape = PolyUtil.containsLocation(location.getLatitude(), location.getLongitude(), polygon.getPoints(), false);

                                        // Check if the user is already signed in.
                                        if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                            Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> User selected a polygon -> User signed in");

                                            // User is signed in.

                                            Intent Activity = new Intent(Map.this, Navigation.class);
                                            goToNextActivityPolygon(Activity, false);
                                        } else {

                                            Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> User selected a polygon -> No user signed in");

                                            // No user is signed in.

                                            Intent Activity = new Intent(Map.this, SignIn.class);
                                            goToNextActivityPolygon(Activity, false);
                                        }
                                    } else {

                                        Log.e(TAG, "onMapReadyAndRestart() -> onPolygonClick -> User selected a polygon -> location == null");
                                        mMap.getUiSettings().setScrollGesturesEnabled(true);
                                        loadingIcon.setVisibility(View.GONE);
                                        toastMessageLong("An error occurred: your location is null");
                                    }
                                }
                            });

                    return;
                }

                selectingShape = true;

                // The following shows a shape consistent with the location of the seekBar.
                selectedOverlappingShapeUUID = overlappingShapesUUID.get(0);

                if (selectedOverlappingShapeUUID != null) {

                    if (circleTemp != null) {

                        circleTemp.remove();
                    }

                    if (polygonTemp != null) {

                        polygonTemp.remove();
                    }

                    if ((overlappingShapesPolygonVertices.size() > overlappingShapesCircleLocation.size() || overlappingShapesPolygonVertices.size() == overlappingShapesCircleLocation.size()) && overlappingShapesCircleLocation.size() > 0) {

                        selectedOverlappingShapeCircleLocation = overlappingShapesCircleLocation.get(0);
                        selectedOverlappingShapeCircleRadius = overlappingShapesCircleRadius.get(0);

                        if (mMap.getMapType() == 2 || mMap.getMapType() == 4) {

                            circleTemp = mMap.addCircle(
                                    new CircleOptions()
                                            .center(selectedOverlappingShapeCircleLocation)
                                            .clickable(true)
                                            .fillColor(Color.argb(100, 255, 255, 0))
                                            .radius(selectedOverlappingShapeCircleRadius)
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .zIndex(2)
                            );
                        } else {

                            circleTemp = mMap.addCircle(
                                    new CircleOptions()
                                            .center(selectedOverlappingShapeCircleLocation)
                                            .clickable(true)
                                            .fillColor(Color.argb(100, 255, 0, 255))
                                            .radius(selectedOverlappingShapeCircleRadius)
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .zIndex(2)
                            );
                        }

                        // Used when getting rid of the shapes in onMapClick.
                        circleTemp.setTag(selectedOverlappingShapeUUID);
                        circleTemp.setCenter(selectedOverlappingShapeCircleLocation);
                        circleTemp.setRadius(selectedOverlappingShapeCircleRadius);
                    } else {

                        selectedOverlappingShapePolygonVertices = overlappingShapesPolygonVertices.get(0);

                        if (mMap.getMapType() == 2 || mMap.getMapType() == 4) {

                            polygonTemp = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .fillColor(Color.argb(100, 255, 255, 0))
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .addAll(selectedOverlappingShapePolygonVertices)
                                            .zIndex(2)
                            );
                        } else {

                            polygonTemp = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .fillColor(Color.argb(100, 255, 0, 255))
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .addAll(selectedOverlappingShapePolygonVertices)
                                            .zIndex(2)
                            );
                        }

                        // Used when getting rid of the shapes in onMapClick.
                        polygonTemp.setTag(selectedOverlappingShapeUUID);
                    }
                } else {

                    Log.e(TAG, "onMapReadyAndRestart() -> onPolygonClick -> selectedOverlappingShapeUUID == null");
                    mMap.getUiSettings().setScrollGesturesEnabled(true);
                    loadingIcon.setVisibility(View.GONE);
                    toastMessageLong("An error occurred");
                }

                // At this point, chatsSize > 1 so set the chatSelectorSeekBar to VISIBLE.
                chatSelectorSeekBar.setMax(chatsSize - 1);
                chatSelectorSeekBar.setProgress(0);
                chatSizeSeekBar.setVisibility(View.GONE);
                chatSelectorSeekBar.setVisibility(View.VISIBLE);
                mMap.getUiSettings().setScrollGesturesEnabled(true);
                loadingIcon.setVisibility(View.GONE);
                toastMessageLong("Highlight and select a shape using the SeekBar below");
            }
        });

        // Go to Chat.java when clicking on a circle.
        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {

            @Override
            public void onCircleClick(final Circle circle) {

                // If the user tries to click on a circle that is not a circleTemp while circleTemp exists, return.
                if (chatSelectorSeekBar.getVisibility() == View.VISIBLE && (circle.getTag() != selectedOverlappingShapeUUID)) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> Selected circle is not a circleTemp. Resetting and returning");

                    if (circleTemp != null) {

                        circleTemp.remove();
                    }

                    if (polygonTemp != null) {

                        polygonTemp.remove();
                    }

                    selectingShape = false;

                    // Change the circle color depending on the map type.
                    if (mMap.getMapType() == 2 || mMap.getMapType() == 4) {

                        for (int i = 0; i < overlappingShapesCircleLocation.size(); i++) {

                            Circle circle0 = mMap.addCircle(
                                    new CircleOptions()
                                            .center(overlappingShapesCircleLocation.get(i))
                                            .clickable(true)
                                            .radius(overlappingShapesCircleRadius.get(i))
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            circle0.setTag(overlappingShapesCircleUUID.get(i));
                        }

                        for (int i = 0; i < overlappingShapesPolygonVertices.size(); i++) {

                            Polygon polygon0 = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .addAll(overlappingShapesPolygonVertices.get(i))
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            polygon0.setTag(overlappingShapesPolygonUUID.get(i));
                        }
                    } else {

                        for (int i = 0; i < overlappingShapesCircleLocation.size(); i++) {

                            Circle circle0 = mMap.addCircle(
                                    new CircleOptions()
                                            .center(overlappingShapesCircleLocation.get(i))
                                            .clickable(true)
                                            .radius(overlappingShapesCircleRadius.get(i))
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            circle0.setTag(overlappingShapesCircleUUID.get(i));
                        }

                        for (int i = 0; i < overlappingShapesPolygonVertices.size(); i++) {

                            Polygon polygon0 = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .addAll(overlappingShapesPolygonVertices.get(i))
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            polygon0.setTag(overlappingShapesPolygonUUID.get(i));
                        }
                    }

                    overlappingShapesUUID = new ArrayList<>();
                    overlappingShapesCircleUUID = new ArrayList<>();
                    overlappingShapesPolygonUUID = new ArrayList<>();
                    overlappingShapesCircleLocation = new ArrayList<>();
                    overlappingShapesCircleRadius = new ArrayList<>();
                    overlappingShapesPolygonVertices = new ArrayList<>();

                    chatSelectorSeekBar.setVisibility(View.INVISIBLE);
                    chatSizeSeekBar.setVisibility(View.VISIBLE);
                    chatSizeSeekBar.setProgress(0);
                    chatSelectorSeekBar.setProgress(0);

                    chatsSize = 0;

                    // Get rid of new shapes if the user clicks away from them.
                    if (newCircle != null) {

                        newCircle.remove();

                        if (marker0 != null) {

                            marker0.remove();
                        }

                        if (marker1 != null) {

                            marker1.remove();
                        }

                        newCircle = null;
                        marker0 = null;
                        marker1 = null;
                        marker0Position = null;
                        marker1Position = null;
                        marker0ID = null;
                        marker1ID = null;

                        chatSizeSeekBar.setProgress(0);
                        relativeAngle = 0.0;
                        usedSeekBar = false;
                    }

                    if (newPolygon != null) {

                        newPolygon.remove();
                        marker0.remove();
                        marker1.remove();
                        marker2.remove();
                        newPolygon = null;
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

                        chatSizeSeekBar.setProgress(0);
                        usedSeekBar = false;
                    }

                    return;
                }

                // Change boolean value so the x and y values in touchAgain() from dispatchTouchEvent() do not change.
                waitingForClicksToProcess = true;

                // While clicking through the circles, if a circle does not have a tag, it is new. Therefore, go directly to the recyclerviewlayout, as this is probably the recyclerviewlayout the user wants to enter.
                if (circle.getTag() == null) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> User clicked on a new circle");

                    // End this method if the method is already being processed from another shape clicking event.
                    if (waitingForShapeInformationToProcess) {

                        return;
                    }

                    // Update boolean to prevent double clicking a shape.
                    waitingForShapeInformationToProcess = true;

                    // Inform the user is the circle is too small.
                    if (circle.getRadius() < 1) {

                        toastMessageLong("Please make the shape larger");

                        return;
                    }

                    // Generate a uuid, as the shape is new.
                    shapeUUID = UUID.randomUUID().toString();

                    // Check location permissions.
                    if (ContextCompat.checkSelfPermission(getBaseContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        // Check if user is within the circle before going to the recyclerviewlayout.
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

                                            // Check if the user is already signed in.
                                            if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                                // User is signed in.

                                                // Compare the uuid to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                                                firebaseCircles.orderByChild("shapeUUID").equalTo(shapeUUID).addListenerForSingleValueEvent(new ValueEventListener() {

                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                        // If the uuid already exists in Firebase, generate another uuid and try again.
                                                        if (dataSnapshot.exists()) {

                                                            Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> New circle -> User signed in -> shapeUUID exists");

                                                            // uuid exists in Firebase. Generate another and try again.

                                                            // Generate another uuid and try again.
                                                            shapeUUID = UUID.randomUUID().toString();

                                                            // Carry the extras all the way to Chat.java.
                                                            Intent Activity = new Intent(Map.this, Navigation.class);

                                                            // Pass this information to Chat.java to create a new shape in Firebase after someone writes a recyclerviewlayout.
                                                            goToNextActivityCircle(Activity, circle, true);
                                                        } else {

                                                            Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> New circle -> User signed in -> shapeUUID does not exists");

                                                            // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                                            // Carry the extras all the way to Chat.java.
                                                            Intent Activity = new Intent(Map.this, Navigation.class);

                                                            // Pass this information to Chat.java to create a new shape in Firebase after someone writes a recyclerviewlayout.
                                                            goToNextActivityCircle(Activity, circle, true);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        Log.e(TAG, "DatabaseError");
                                                        mMap.getUiSettings().setScrollGesturesEnabled(true);
                                                        loadingIcon.setVisibility(View.GONE);
                                                        toastMessageLong(databaseError.getMessage());
                                                    }
                                                });
                                            } else {

                                                // No user is signed in.

                                                // Compare the uuid to the uuids in Firebase to prevent uuid overlap before adding it to Firebase.
                                                firebaseCircles.orderByChild("shapeUUID").equalTo(shapeUUID).addListenerForSingleValueEvent(new ValueEventListener() {

                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                        // If the uuid already exists in Firebase, generate another uuid and try again.
                                                        if (dataSnapshot.exists()) {

                                                            Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> New circle -> No user signed in -> shapeUUID exists");

                                                            // uuid exists in Firebase. Generate another and try again.

                                                            // Generate another uuid and try again.
                                                            shapeUUID = UUID.randomUUID().toString();

                                                            // Carry the extras to SignIn.java.
                                                            Intent Activity = new Intent(Map.this, SignIn.class);

                                                            // Pass this information to Chat.java to create a new shape in Firebase after someone writes a recyclerviewlayout.
                                                            goToNextActivityCircle(Activity, circle, true);
                                                        } else {

                                                            Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> New circle -> No user signed in -> shapeUUID does not exists");

                                                            // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                                            // Carry the extras to SignIn.java.
                                                            Intent Activity = new Intent(Map.this, SignIn.class);

                                                            // Pass this information to Chat.java to create a new shape in Firebase after someone writes a recyclerviewlayout.
                                                            goToNextActivityCircle(Activity, circle, true);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        Log.e(TAG, "DatabaseError");
                                                        mMap.getUiSettings().setScrollGesturesEnabled(true);
                                                        loadingIcon.setVisibility(View.GONE);
                                                        toastMessageLong(databaseError.getMessage());
                                                    }
                                                });
                                            }
                                        } else {

                                            Log.e(TAG, "onMapReadyAndRestart() -> onCircleClick -> circle.getTag() == null -> location == null");
                                            mMap.getUiSettings().setScrollGesturesEnabled(true);
                                            loadingIcon.setVisibility(View.GONE);
                                            toastMessageLong("An error occurred: your location is null");
                                        }
                                    }
                                });
                    } else {

                        checkLocationPermissions();
                    }

                    // As getFusedLocationProviderClient is asynchronous, this return statement will prevent executing the rest of the code.
                    return;
                }

                // Click through all shapes, using the z-index to figure out which ones have not been cycled through. All the information to the arrayLists to be used by chatSelectorSeekBar.
                if (circle.getZIndex() == 0 && circle.getTag() != null) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> Lowering z-index of a circle");

                    // Prevent the map from scrolling so the same spot will be clicked again in touchAgain().
                    if (mMap.getUiSettings().isScrollGesturesEnabled()) {

                        mMap.getUiSettings().setScrollGesturesEnabled(false);
                    }

                    if (loadingIcon.getVisibility() != View.VISIBLE) {

                        loadingIcon.setVisibility(View.VISIBLE);
                    }

                    // Drop the z-index to metaphorically check it off the "to click" list.
                    circle.setZIndex(-1);

                    // Add the information to arrayLists to be used by chatSelectorSeekBar.
                    overlappingShapesUUID.add(circle.getTag().toString());
                    overlappingShapesCircleUUID.add(circle.getTag().toString());
                    overlappingShapesCircleLocation.add(circle.getCenter());
                    overlappingShapesCircleRadius.add(circle.getRadius());

                    // If the user zooms out and a shape is too small, touchAgain() will not touch the shape again (I'm not sure why).
                    // Therefore, save an instance of the shape so that if onMapClick is called, it will just go to the last shape.
                    mCircle = circle;

                    // Programmatically click the same spot again.
                    touchAgain();

                    return;
                }

                waitingForClicksToProcess = false;

                // This will get called after the last shape is programmatically clicked.
                chatsSize = overlappingShapesUUID.size();

                // If none of the clicked shapes are new, get rid of any new shapes.
                if (!overlappingShapesUUID.contains("new")) {

                    // Remove the polygon and markers.
                    if (newPolygon != null) {

                        newPolygon.remove();
                        marker0.remove();
                        marker1.remove();
                        marker2.remove();
                        newPolygon = null;
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
                    if (newCircle != null) {

                        newCircle.remove();
                        if (marker0 != null) {

                            marker0.remove();
                        }

                        if (marker1 != null) {
                            marker1.remove();
                        }

                        newCircle = null;
                        marker0 = null;
                        marker1 = null;
                        marker0Position = null;
                        marker1Position = null;
                        marker0ID = null;
                        marker1ID = null;
                    }
                }

                // If selectingShape, user has selected a highlighted shape. Similar logic applies to originally only clicking on one circle.
                if (selectingShape || (chatsSize == 1 && circle.getTag() != null)) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> User selected a circle");

                    // End this method if the method is already being processed from another shape clicking event.
                    if (waitingForShapeInformationToProcess) {

                        return;
                    }

                    // Update boolean to prevent double clicking a shape.
                    waitingForShapeInformationToProcess = true;

                    // "New" shapes are automatically clicked. Therefore, get the ID set by Firebase to identify which circle the user clicked on.
                    if (chatsSize == 1) {

                        shapeUUID = (String) circle.getTag();
                    } else {

                        shapeUUID = selectedOverlappingShapeUUID;
                    }

                    // Check if user is within the circle before going to the recyclerviewlayout.
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

                                        // Check if the user is already signed in.
                                        if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                            Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> User selected a circle -> User signed in");

                                            // User is signed in.

                                            Intent Activity = new Intent(Map.this, Navigation.class);
                                            goToNextActivityCircle(Activity, circle, false);
                                        } else {

                                            Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> User selected a circle -> User not signed in");

                                            // No user is signed in.

                                            Intent Activity = new Intent(Map.this, SignIn.class);
                                            goToNextActivityCircle(Activity, circle, false);
                                        }
                                    } else {

                                        Log.e(TAG, "onMapReadyAndRestart() -> onCircleClick -> User selected a circle -> location == null");
                                        mMap.getUiSettings().setScrollGesturesEnabled(true);
                                        loadingIcon.setVisibility(View.GONE);
                                        toastMessageLong("An error occurred: your location is null");
                                    }
                                }
                            });

                    return;
                }

                selectingShape = true;

                // The following shows a shape consistent with the location of the seekBar.
                selectedOverlappingShapeUUID = overlappingShapesUUID.get(0);

                if (selectedOverlappingShapeUUID != null) {

                    if (circleTemp != null) {

                        circleTemp.remove();
                    }

                    if (polygonTemp != null) {

                        polygonTemp.remove();
                    }

                    if (overlappingShapesCircleLocation.size() > overlappingShapesPolygonVertices.size() && overlappingShapesPolygonVertices.size() > 0) {

                        selectedOverlappingShapePolygonVertices = overlappingShapesPolygonVertices.get(0);

                        if (mMap.getMapType() == 2 || mMap.getMapType() == 4) {

                            polygonTemp = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .fillColor(Color.argb(100, 255, 255, 0))
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .addAll(selectedOverlappingShapePolygonVertices)
                                            .zIndex(2)
                            );
                        } else {

                            polygonTemp = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .fillColor(Color.argb(100, 255, 0, 255))
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .addAll(selectedOverlappingShapePolygonVertices)
                                            .zIndex(2)
                            );
                        }

                        // Used when getting rid of the shapes in onMapClick.
                        polygonTemp.setTag(selectedOverlappingShapeUUID);
                    } else {

                        selectedOverlappingShapeCircleLocation = overlappingShapesCircleLocation.get(0);
                        selectedOverlappingShapeCircleRadius = overlappingShapesCircleRadius.get(0);

                        if (mMap.getMapType() == 2 || mMap.getMapType() == 4) {

                            circleTemp = mMap.addCircle(
                                    new CircleOptions()
                                            .center(selectedOverlappingShapeCircleLocation)
                                            .clickable(true)
                                            .fillColor(Color.argb(100, 255, 255, 0))
                                            .radius(selectedOverlappingShapeCircleRadius)
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .zIndex(2)
                            );
                        } else {

                            circleTemp = mMap.addCircle(
                                    new CircleOptions()
                                            .center(selectedOverlappingShapeCircleLocation)
                                            .clickable(true)
                                            .fillColor(Color.argb(100, 255, 0, 255))
                                            .radius(selectedOverlappingShapeCircleRadius)
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .zIndex(2)
                            );
                        }

                        // Used when getting rid of the shapes in onMapClick.
                        circleTemp.setTag(selectedOverlappingShapeUUID);
                        circleTemp.setCenter(selectedOverlappingShapeCircleLocation);
                        circleTemp.setRadius(selectedOverlappingShapeCircleRadius);
                    }
                } else {

                    Log.e(TAG, "onMapReadyAndRestart() -> onCircleClick -> selectedOverlappingShapeUUID == null");
                    mMap.getUiSettings().setScrollGesturesEnabled(true);
                    loadingIcon.setVisibility(View.GONE);
                    toastMessageLong("An error occurred");
                }

                // At this point, chatsSize > 1 so set the chatSelectorSeekBar to VISIBLE.
                chatSelectorSeekBar.setMax(chatsSize - 1);
                chatSelectorSeekBar.setProgress(0);
                chatSizeSeekBar.setVisibility(View.GONE);
                chatSelectorSeekBar.setVisibility(View.VISIBLE);
                mMap.getUiSettings().setScrollGesturesEnabled(true);
                loadingIcon.setVisibility(View.GONE);
                toastMessageLong("Highlight and select a shape using the SeekBar below");
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {

                // Make new shapes with z-index = 0.
                if (chatSelectorSeekBar.getVisibility() == View.VISIBLE) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> Replacing circles with z = -1 with z-index = 0");

                    if (circleTemp != null) {

                        circleTemp.remove();
                    }

                    if (polygonTemp != null) {

                        polygonTemp.remove();
                    }

                    selectingShape = false;

                    // Change the circle color depending on the map type.
                    if (mMap.getMapType() == 2 || mMap.getMapType() == 4) {

                        for (int i = 0; i < overlappingShapesCircleLocation.size(); i++) {

                            Circle circle0 = mMap.addCircle(
                                    new CircleOptions()
                                            .center(overlappingShapesCircleLocation.get(i))
                                            .clickable(true)
                                            .radius(overlappingShapesCircleRadius.get(i))
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            circle0.setTag(overlappingShapesCircleUUID.get(i));
                        }

                        for (int i = 0; i < overlappingShapesPolygonVertices.size(); i++) {

                            Polygon polygon0 = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .addAll(overlappingShapesPolygonVertices.get(i))
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            polygon0.setTag(overlappingShapesPolygonUUID.get(i));
                        }
                    } else {

                        for (int i = 0; i < overlappingShapesCircleLocation.size(); i++) {

                            Circle circle0 = mMap.addCircle(
                                    new CircleOptions()
                                            .center(overlappingShapesCircleLocation.get(i))
                                            .clickable(true)
                                            .radius(overlappingShapesCircleRadius.get(i))
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            circle0.setTag(overlappingShapesCircleUUID.get(i));
                        }

                        for (int i = 0; i < overlappingShapesPolygonVertices.size(); i++) {

                            Polygon polygon0 = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .addAll(overlappingShapesPolygonVertices.get(i))
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            polygon0.setTag(overlappingShapesPolygonUUID.get(i));
                        }
                    }

                    overlappingShapesUUID = new ArrayList<>();
                    overlappingShapesCircleUUID = new ArrayList<>();
                    overlappingShapesPolygonUUID = new ArrayList<>();
                    overlappingShapesCircleLocation = new ArrayList<>();
                    overlappingShapesCircleRadius = new ArrayList<>();
                    overlappingShapesPolygonVertices = new ArrayList<>();

                    chatSelectorSeekBar.setVisibility(View.INVISIBLE);
                    chatSizeSeekBar.setVisibility(View.VISIBLE);
                    chatSizeSeekBar.setProgress(0);
                    chatSelectorSeekBar.setProgress(0);

                    chatsSize = 0;
                }

                // If the user zooms out and a shape is too small, touchAgain() will not touch the shape again (I'm not sure why).
                // Therefore, just follow through with the on[Shape]Click code without getting ALL the shapes.
                if (loadingIcon.getVisibility() == View.VISIBLE && mCircle != null) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a circle");

                    chatsSize = overlappingShapesUUID.size();

                    // End this method if the method is already being processed from another shape clicking event.
                    if (waitingForShapeInformationToProcess) {

                        return;
                    }

                    // Update boolean to prevent double clicking a shape.
                    waitingForShapeInformationToProcess = true;

                    shapeUUID = (String) mCircle.getTag();

                    // Check location permissions.
                    if (ContextCompat.checkSelfPermission(getBaseContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        // Check if user is within the circle before going to the recyclerviewlayout.
                        FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                    @Override
                                    public void onSuccess(Location location) {

                                        if (location != null) {

                                            float[] distance = new float[2];

                                            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                                    mCircle.getCenter().latitude, mCircle.getCenter().longitude, distance);

                                            // Boolean; will be true if user is within the circle upon circle click.
                                            userIsWithinShape = !(distance[0] > mCircle.getRadius());

                                            // Check if the user is already signed in.
                                            if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                                Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a circle -> User signed in");

                                                // User is signed in.

                                                Intent Activity = new Intent(Map.this, Navigation.class);
                                                goToNextActivityCircle(Activity, mCircle, false);
                                            } else {

                                                Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a circle -> User not signed in");

                                                // No user is signed in.

                                                Intent Activity = new Intent(Map.this, SignIn.class);
                                                goToNextActivityCircle(Activity, mCircle, false);
                                            }
                                        } else {

                                            Log.e(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a circle -> location == null");
                                            mMap.getUiSettings().setScrollGesturesEnabled(true);
                                            loadingIcon.setVisibility(View.GONE);
                                            toastMessageLong("An error occurred: your location is null");
                                        }
                                    }
                                });
                    } else {

                        checkLocationPermissions();
                    }

                    return;
                }

                if (loadingIcon.getVisibility() == View.VISIBLE && mPolygon != null) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a polygon");

                    chatsSize = overlappingShapesUUID.size();

                    // End this method if the method is already being processed from another shape clicking event.
                    if (waitingForShapeInformationToProcess) {

                        return;
                    }

                    // Update boolean to prevent double clicking a shape.
                    waitingForShapeInformationToProcess = true;

                    shapeUUID = (String) mPolygon.getTag();

                    // Check location permissions.
                    if (ContextCompat.checkSelfPermission(getBaseContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        // Check if user is within the shape before going to the recyclerviewlayout.
                        FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                    @Override
                                    public void onSuccess(Location location) {

                                        if (location != null) {

                                            // Boolean; will be true if user is within the shape upon shape click.
                                            userIsWithinShape = PolyUtil.containsLocation(location.getLatitude(), location.getLongitude(), mPolygon.getPoints(), false);

                                            // Check if the user is already signed in.
                                            if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                                Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a polygon -> User signed in");

                                                // User is signed in.

                                                Intent Activity = new Intent(Map.this, Navigation.class);
                                                goToNextActivityPolygon(Activity, false);
                                            } else {

                                                Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a polygon -> No user signed in");

                                                // No user is signed in.

                                                Intent Activity = new Intent(Map.this, SignIn.class);
                                                goToNextActivityPolygon(Activity, false);
                                            }
                                        } else {

                                            Log.e(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a polygon -> location == null");
                                            mMap.getUiSettings().setScrollGesturesEnabled(true);
                                            loadingIcon.setVisibility(View.GONE);
                                            toastMessageLong("An error occurred: your location is null");
                                        }
                                    }
                                });
                    } else {

                        checkLocationPermissions();
                    }

                    return;
                }

                // Get rid of new shapes if the user clicks away from them.
                if (newCircle != null) {

                    newCircle.remove();
                    if (marker0 != null) {

                        marker0.remove();
                    }

                    if (marker1 != null) {

                        marker1.remove();
                    }

                    newCircle = null;
                    marker0 = null;
                    marker1 = null;
                    marker0Position = null;
                    marker1Position = null;
                    marker0ID = null;
                    marker1ID = null;

                    chatSizeSeekBar.setProgress(0);
                    relativeAngle = 0.0;
                    usedSeekBar = false;
                }

                if (newPolygon != null) {

                    newPolygon.remove();
                    marker0.remove();
                    marker1.remove();
                    marker2.remove();
                    newPolygon = null;
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

                    chatSizeSeekBar.setProgress(0);
                    usedSeekBar = false;
                }
            }
        });

        // Updates the boolean value for onLocationChanged() to prevent updating the camera position if the user has already changed it manually.
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {

            @Override
            public void onCameraMove() {

                if (waitingForBetterLocationAccuracy && !cameraMoved) {

                    Log.i(TAG, "onMapReadyAndRestart() -> onCameraMove");

                    cameraMoved = true;
                }
            }
        });
    }

    protected void loadPreferences() {

        Log.i(TAG, "loadPreferences()");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        preferredMapType = sharedPreferences.getString(SettingsFragment.KEY_MAP_TYPE, getResources().getString(R.string.hybrid_view));
    }

    protected void updatePreferences() {

        Log.i(TAG, "updatePreferences()");

        switch (preferredMapType) {

            case "Road map view":

                Log.i(TAG, "updatePreferences() -> Road map view");

                // Use the "road map" map type if the map is not null.
                if (mMap != null) {

                    Log.i(TAG, "updatePreferences() -> Road Map");

                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                    purpleAdjustmentsForMap();
                } else {

                    Log.e(TAG, "updatePreferences() -> Road Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map");
                }

                break;

            case "Satellite view":

                Log.i(TAG, "updatePreferences() -> Satellite view");

                // Use the "satellite" map type if the map is not null.
                if (mMap != null) {

                    Log.i(TAG, "updatePreferences() -> Satellite Map");

                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                    yellowAdjustmentsForMap();
                } else {

                    Log.e(TAG, "updatePreferences() -> Satellite Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map");
                }

                break;

            case "Hybrid view":

                Log.i(TAG, "updatePreferences() -> Hybrid view");

                // Use the "hybrid" map type if the map is not null.
                if (mMap != null) {

                    Log.i(TAG, "updatePreferences() -> Hybrid Map");

                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                    yellowAdjustmentsForMap();
                } else {

                    Log.e(TAG, "updatePreferences() -> Hybrid Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map");
                }

                break;

            case "Terrain view":

                Log.i(TAG, "updatePreferences() -> Terrain view");

                // Use the "terrain" map type if the map is not null.
                if (mMap != null) {

                    Log.i(TAG, "updatePreferences() -> Terrain Map");

                    mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

                    purpleAdjustmentsForMap();
                } else {

                    Log.e(TAG, "updatePreferences() -> Terrain Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map");
                }

                break;

            default:

                Log.i(TAG, "updatePreferences() -> default (something went wrong, error probably on next line)");
                Log.i(TAG, "updatePreferences(): preferredMapType: " + preferredMapType);

                // Use the "hybrid" map type if the map is not null.
                if (mMap != null) {

                    Log.i(TAG, "updatePreferences() -> Hybrid Map, default");

                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                    yellowAdjustmentsForMap();
                } else {

                    Log.e(TAG, "updatePreferences() -> default -> mMap == null");
                    toastMessageLong("An error occurred while loading the map");
                }

                break;
        }
    }

    // Used by onMapReadyAndRestart() -> onMarkerDragListener.
    private void adjustShape(Marker marker) {

        Log.i(TAG, "adjustShape()");

        LatLng markerPosition = marker.getPosition();

        // If user holds the center marker, update the circle's position. Else, update the circle's radius.
        if (newCircle != null) {

            if (marker.getId().equals(marker0ID)) {

                Log.i(TAG, "adjustShape() -> circle -> marker0");

                newCircle.setCenter(markerPosition);
                marker1.setVisible(false);
            }

            if (marker.getId().equals(marker1ID)) {

                Log.i(TAG, "adjustShape() -> circle -> marker1");

                // Limits the size of the circle.
                if (distanceGivenLatLng(markerPosition.latitude, markerPosition.longitude, newCircle.getCenter().latitude, newCircle.getCenter().longitude) < 200) {

                    newCircle.setRadius(distanceGivenLatLng(newCircle.getCenter().latitude, newCircle.getCenter().longitude, markerPosition.latitude, markerPosition.longitude));
                } else {

                    newCircle.setRadius(200);
                }
            }
        }

        // Update the polygon shape as the marker positions get updated.
        if (newPolygon != null) {

            // Limit polygon size. The low end will be handled in the onClickListener.
            if (SphericalUtil.computeArea(polygonPointsList) <= Math.PI * Math.pow(200, 2)) {

                markerPositionAtVertexOfPolygon = new LatLng(markerPosition.latitude, markerPosition.longitude);

                if (marker.getId().equals(marker0ID)) {

                    Log.i(TAG, "adjustShape() -> polygon -> marker0");

                    if (threeMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);

                    }

                    if (fourMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position, marker3Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fiveMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position, marker3Position, marker4Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker1ID)) {

                    Log.i(TAG, "adjustShape() -> polygon -> marker1");

                    if (threeMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fourMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position, marker3Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fiveMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position, marker3Position, marker4Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker2ID)) {

                    Log.i(TAG, "adjustShape() -> polygon -> marker2");

                    if (threeMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fourMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition, marker3Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fiveMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition, marker3Position, marker4Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker3ID)) {

                    Log.i(TAG, "adjustShape() -> polygon -> marker3");

                    if (fourMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fiveMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, markerPosition, marker4Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker4ID)) {

                    Log.i(TAG, "adjustShape() -> polygon -> marker4");

                    if (fiveMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker5ID)) {

                    Log.i(TAG, "adjustShape() -> polygon -> marker5");

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker6ID)) {

                    Log.i(TAG, "adjustShape() -> polygon -> marker6");

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker7ID)) {

                    Log.i(TAG, "adjustShape() -> polygon -> marker7");

                    LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, markerPosition};
                    polygonPointsList = Arrays.asList(polygonPoints);
                    newPolygon.setPoints(polygonPointsList);
                }
            } else {

                markerOutsidePolygon = true;
            }

            // If marker exits the polygon because it's too big and re-enters it, the following will update the shape with the marker so the previous code will work again.
            if (markerOutsidePolygon && PolyUtil.containsLocation(markerPosition.latitude, markerPosition.longitude, newPolygon.getPoints(), false)) {

                Log.i(TAG, "adjustShape() -> Marker exited polygon then re-entered polygon");

                markerPositionAtVertexOfPolygon = new LatLng(markerPosition.latitude, markerPosition.longitude);

                if (marker.getId().equals(marker0ID)) {

                    if (threeMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fourMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position, marker3Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fiveMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position, marker3Position, marker4Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{markerPosition, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker1ID)) {

                    if (threeMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fourMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position, marker3Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fiveMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position, marker3Position, marker4Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, markerPosition, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker2ID)) {

                    if (threeMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fourMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition, marker3Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fiveMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition, marker3Position, marker4Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, markerPosition, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker3ID)) {

                    if (fourMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (fiveMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, markerPosition, marker4Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, markerPosition, marker4Position, marker5Position, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker4ID)) {

                    if (fiveMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, markerPosition, marker5Position, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker5ID)) {

                    if (sixMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, markerPosition, marker6Position, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker6ID)) {

                    if (sevenMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }

                    if (eightMarkers) {

                        LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, markerPosition, marker7Position};
                        polygonPointsList = Arrays.asList(polygonPoints);
                        newPolygon.setPoints(polygonPointsList);
                    }
                }

                if (marker.getId().equals(marker7ID)) {

                    LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, markerPosition};
                    polygonPointsList = Arrays.asList(polygonPoints);
                    newPolygon.setPoints(polygonPointsList);
                }

                markerOutsidePolygon = false;
            }
        }
    }

    private void goToNextActivityPolygon(final Intent Activity, final Boolean newShape) {

        // Check location permissions.
        if (ContextCompat.checkSelfPermission(getBaseContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                        @Override
                        public void onSuccess(Location location) {

                            if (location != null) {

                                cancelToasts();

                                if (threeMarkers) {

                                    Log.i(TAG, "goToNextActivityPolygon() -> threeMarkers");

                                    // Calculate the area of the polygon and send it to Firebase - used for chatViewsMenu.
                                    Activity.putExtra("polygonArea", SphericalUtil.computeArea(polygonPointsList));
                                    Activity.putExtra("threeMarkers", true);
                                    Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                    Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                    Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                    Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                    Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                    Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                }

                                if (fourMarkers) {

                                    Log.i(TAG, "goToNextActivityPolygon() -> fourMarkers");

                                    // Calculate the area of the polygon and send it to Firebase - used for chatViewsMenu.
                                    Activity.putExtra("polygonArea", SphericalUtil.computeArea(polygonPointsList));
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

                                    Log.i(TAG, "goToNextActivityPolygon() -> fiveMarkers");

                                    // Calculate the area of the polygon and send it to Firebase - used for chatViewsMenu.
                                    Activity.putExtra("polygonArea", SphericalUtil.computeArea(polygonPointsList));
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

                                    Log.i(TAG, "goToNextActivityPolygon() -> sixMarkers");

                                    // Calculate the area of the polygon and send it to Firebase - used for chatViewsMenu.
                                    Activity.putExtra("polygonArea", SphericalUtil.computeArea(polygonPointsList));
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

                                    Log.i(TAG, "goToNextActivityPolygon() -> sevenMarkers");

                                    // Calculate the area of the polygon and send it to Firebase - used for chatViewsMenu.
                                    Activity.putExtra("polygonArea", SphericalUtil.computeArea(polygonPointsList));
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

                                    Log.i(TAG, "goToNextActivityPolygon() -> eightMarkers");

                                    // Calculate the area of the polygon and send it to Firebase - used for chatViewsMenu.
                                    Activity.putExtra("polygonArea", SphericalUtil.computeArea(polygonPointsList));
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
                                    Activity.putExtra("marker7Latitude", marker7Position.latitude);
                                    Activity.putExtra("marker7Longitude", marker7Position.longitude);
                                }

                                Activity.putExtra("userLatitude", location.getLatitude());
                                Activity.putExtra("userLongitude", location.getLongitude());
                                Activity.putExtra("shapeIsCircle", false);
                                // Pass this boolean value Chat.java.
                                Activity.putExtra("newShape", newShape);
                                // Pass this value to Chat.java to identify the shape.
                                Activity.putExtra("shapeUUID", shapeUUID);
                                // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
                                Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                loadingIcon.setVisibility(View.GONE);

                                startActivity(Activity);
                            } else {

                                loadingIcon.setVisibility(View.GONE);
                                Log.e(TAG, "goToNextActivityPolygon() -> location == null");
                                toastMessageLong("An error occurred: your location is null");
                            }
                        }
                    });
        } else {

            checkLocationPermissions();
        }
    }

    private void goToNextActivityCircle(final Intent Activity, final Circle circle, final Boolean newShape) {

        Log.i(TAG, "goToNextActivityCircle()");

        // Check location permissions.
        if (ContextCompat.checkSelfPermission(getBaseContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                        @Override
                        public void onSuccess(Location location) {

                            if (location != null) {

                                cancelToasts();

                                Activity.putExtra("userLatitude", location.getLatitude());
                                Activity.putExtra("userLongitude", location.getLongitude());
                                Activity.putExtra("shapeIsCircle", true);
                                // Pass this boolean value to Chat.java.
                                Activity.putExtra("newShape", newShape);
                                // Pass this value to Chat.java to identify the shape.
                                Activity.putExtra("shapeUUID", shapeUUID);
                                // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                // Pass this information to Chat.java to create a new circle in Firebase after someone writes a recyclerviewlayout.
                                if (newShape) {

                                    Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                    Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                    Activity.putExtra("radius", circle.getRadius());
                                }

                                loadingIcon.setVisibility(View.GONE);

                                startActivity(Activity);
                            } else {

                                loadingIcon.setVisibility(View.GONE);
                                Log.e(TAG, "goToNextActivityCircle() -> location == null");
                                toastMessageLong("An error occurred: your location is null");
                            }
                        }
                    });
        } else {

            checkLocationPermissions();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        switch (menuItem.getItemId()) {

            // maptype_menu
            case R.id.roadmap:

                // Use the "road map" map type if the map is not null.
                if (mMap != null) {

                    // getMapType() returns 1 if the map type is set to "road map".
                    if (mMap.getMapType() != 1) {

                        // Make adjustments to colors only if they're needed.
                        if (mMap.getMapType() == 3) {

                            Log.i(TAG, "onMenuItemClick -> Road Map");

                            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        } else {

                            Log.i(TAG, "onMenuItemClick -> Road Map");

                            purpleAdjustmentsForMap();

                            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        }
                    }
                } else {

                    Log.e(TAG, "onMenuItemClick -> Road Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map");
                }

                break;

            // maptype_menu
            case R.id.satellite:

                // Use the "satellite" map type if the map is not null.
                if (mMap != null) {

                    // getMapType() returns 2 if the map type is set to "satellite".
                    if (mMap.getMapType() != 2) {

                        // Make adjustments to colors only if they're needed.
                        if (mMap.getMapType() == 4) {

                            Log.i(TAG, "onMenuItemClick -> Satellite Map");

                            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        } else {

                            Log.i(TAG, "onMenuItemClick -> Satellite Map");

                            yellowAdjustmentsForMap();

                            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        }
                    }
                } else {

                    Log.e(TAG, "onMenuItemClick -> Satellite Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map");
                }

                break;

            // maptype_menu
            case R.id.hybrid:

                // Use the "hybrid" map type if the map is not null.
                if (mMap != null) {

                    // getMapType() returns 4 if the map type is set to "hybrid".
                    if (mMap.getMapType() != 4) {

                        // Make adjustments to colors only if they're needed.
                        if (mMap.getMapType() == 2) {

                            Log.i(TAG, "onMenuItemClick -> Hybrid Map");

                            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        } else {

                            Log.i(TAG, "onMenuItemClick -> Hybrid Map");

                            yellowAdjustmentsForMap();

                            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        }
                    }
                } else {

                    Log.e(TAG, "onMenuItemClick -> Hybrid Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map");
                }

                break;

            // maptype_menu
            case R.id.terrain:

                // Use the "terrain" map type if the map is not null.
                if (mMap != null) {

                    // getMapType() returns 3 if the map type is set to "terrain".
                    if (mMap.getMapType() != 3) {

                        // Make adjustments to colors only if they're needed.
                        if (mMap.getMapType() == 1) {

                            Log.i(TAG, "onMenuItemClick -> Terrain Map");

                            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        } else {

                            Log.i(TAG, "onMenuItemClick -> Terrain Map");

                            purpleAdjustmentsForMap();

                            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        }
                    }
                } else {

                    Log.e(TAG, "onMenuItemClick -> Terrain Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map");
                }

                break;

            // chatviews_menu
            case R.id.showEverything:

                Log.i(TAG, "onMenuItemClick() -> showEverything");

                // Prevent resetting and reloading everything if this is already the state.
                if (!showingEverything) {

                    mMap.clear();

                    // Remove the polygon and markers.
                    if (newPolygon != null) {

                        newPolygon = null;
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

                            marker3 = null;
                            marker3Position = null;
                            marker3ID = null;
                        }

                        if (marker4 != null) {

                            marker4 = null;
                            marker4Position = null;
                            marker4ID = null;
                        }

                        if (marker5 != null) {

                            marker5 = null;
                            marker5Position = null;
                            marker5ID = null;
                        }

                        if (marker6 != null) {

                            marker6 = null;
                            marker6Position = null;
                            marker6ID = null;
                        }

                        if (marker7 != null) {

                            marker7 = null;
                            marker7Position = null;
                            marker7ID = null;
                        }

                        mlocation = null;
                    }

                    // Remove the circle and markers.
                    if (newCircle != null) {

                        newCircle = null;
                        marker0 = null;
                        marker1 = null;
                        marker0Position = null;
                        marker1Position = null;
                        marker0ID = null;
                        marker1ID = null;
                    }

                    waitingForShapeInformationToProcess = false;
                    waitingForClicksToProcess = false;
                    selectingShape = false;
                    chatsSize = 0;

                    overlappingShapesUUID = new ArrayList<>();
                    overlappingShapesCircleUUID = new ArrayList<>();
                    overlappingShapesPolygonUUID = new ArrayList<>();
                    overlappingShapesCircleLocation = new ArrayList<>();
                    overlappingShapesCircleRadius = new ArrayList<>();
                    overlappingShapesPolygonVertices = new ArrayList<>();

                    // Get rid of the chatSelectorSeekBar.
                    if (chatSelectorSeekBar.getVisibility() != View.INVISIBLE) {

                        chatSelectorSeekBar.setVisibility(View.INVISIBLE);
                        chatSizeSeekBar.setProgress(0);
                        chatSizeSeekBar.setVisibility(View.VISIBLE);
                        chatSelectorSeekBar.setProgress(0);
                    }

                    // Load different colored shapes depending on the map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        yellowLoadFirebaseShapes();
                    } else {

                        purpleLoadFirebaseShapes();
                    }
                }

                showingEverything = true;
                showingLarge = false;
                showingMedium = false;
                showingSmall = false;
                showingPoints = false;

                break;

            // chatviews_menu
            case R.id.showLargeChats:

                Log.i(TAG, "onMenuItemClick() -> showLargeChats");

                // Prevent resetting and reloading everything if this is already the state.
                if (!showingLarge) {

                    mMap.clear();

                    // Remove the polygon and markers.
                    if (newPolygon != null) {

                        newPolygon = null;
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

                            marker3 = null;
                            marker3Position = null;
                            marker3ID = null;
                        }

                        if (marker4 != null) {

                            marker4 = null;
                            marker4Position = null;
                            marker4ID = null;
                        }

                        if (marker5 != null) {

                            marker5 = null;
                            marker5Position = null;
                            marker5ID = null;
                        }

                        if (marker6 != null) {

                            marker6 = null;
                            marker6Position = null;
                            marker6ID = null;
                        }

                        if (marker7 != null) {

                            marker7 = null;
                            marker7Position = null;
                            marker7ID = null;
                        }

                        mlocation = null;
                    }

                    // Remove the circle and markers.
                    if (newCircle != null) {

                        newCircle = null;
                        marker0 = null;
                        marker1 = null;
                        marker0Position = null;
                        marker1Position = null;
                        marker0ID = null;
                        marker1ID = null;
                    }

                    waitingForShapeInformationToProcess = false;
                    waitingForClicksToProcess = false;
                    selectingShape = false;
                    chatsSize = 0;

                    overlappingShapesUUID = new ArrayList<>();
                    overlappingShapesCircleUUID = new ArrayList<>();
                    overlappingShapesPolygonUUID = new ArrayList<>();
                    overlappingShapesCircleLocation = new ArrayList<>();
                    overlappingShapesCircleRadius = new ArrayList<>();
                    overlappingShapesPolygonVertices = new ArrayList<>();

                    // Get rid of the chatSelectorSeekBar.
                    if (chatSelectorSeekBar.getVisibility() != View.INVISIBLE) {

                        chatSelectorSeekBar.setVisibility(View.INVISIBLE);
                        chatSizeSeekBar.setProgress(0);
                        chatSizeSeekBar.setVisibility(View.VISIBLE);
                        chatSelectorSeekBar.setProgress(0);
                    }

                    // Load different colored shapes depending on the map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        loadingIcon.setVisibility(View.VISIBLE);

                        // Load Firebase circles.
                        firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load large circles (radius > 50)
                                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 50) {

                                            LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());
                                            double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                            Circle circle = mMap.addCircle(
                                                    new CircleOptions()
                                                            .center(center)
                                                            .clickable(true)
                                                            .radius(radius)
                                                            .strokeColor(Color.YELLOW)
                                                            .strokeWidth(3f)
                                            );

                                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                                            circle.setTag(shapeUUID);
                                        }
                                    }
                                }

                                stillLoadingCircles = false;

                                if (!stillLoadingPolygons) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });

                        // Load Firebase polygons.
                        firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load large polygons (polygonArea > pi(50^2))
                                        if ((double) ds.child("polygonArea").getValue() > Math.PI * (Math.pow(50, 2))) {

                                            LatLng marker3Position = null;
                                            LatLng marker4Position = null;
                                            LatLng marker5Position = null;
                                            LatLng marker6Position = null;
                                            Polygon polygon;

                                            LatLng marker0Position = new LatLng((double) ds.child("polygonOptions/points/0/latitude/").getValue(), (double) ds.child("polygonOptions/points/0/longitude/").getValue());
                                            LatLng marker1Position = new LatLng((double) ds.child("polygonOptions/points/1/latitude/").getValue(), (double) ds.child("polygonOptions/points/1/longitude/").getValue());
                                            LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                                            if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                                LatLng marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            }
                                        }
                                    }
                                }

                                stillLoadingPolygons = false;

                                if (!stillLoadingCircles) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });
                    } else {

                        loadingIcon.setVisibility(View.VISIBLE);

                        // Load Firebase circles.
                        firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load large circles (radius > 50)
                                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 50) {

                                            LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());
                                            double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                            Circle circle = mMap.addCircle(
                                                    new CircleOptions()
                                                            .center(center)
                                                            .clickable(true)
                                                            .radius(radius)
                                                            .strokeColor(Color.rgb(255, 0, 255))
                                                            .strokeWidth(3f)
                                            );

                                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                                            circle.setTag(shapeUUID);
                                        }
                                    }
                                }

                                stillLoadingCircles = false;

                                if (!stillLoadingPolygons) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });

                        // Load Firebase polygons.
                        firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load large polygons (polygonArea > pi(50^2))
                                        if ((double) ds.child("polygonArea").getValue() > Math.PI * (Math.pow(50, 2))) {

                                            LatLng marker3Position = null;
                                            LatLng marker4Position = null;
                                            LatLng marker5Position = null;
                                            LatLng marker6Position = null;
                                            Polygon polygon;

                                            LatLng marker0Position = new LatLng((double) ds.child("polygonOptions/points/0/latitude/").getValue(), (double) ds.child("polygonOptions/points/0/longitude/").getValue());
                                            LatLng marker1Position = new LatLng((double) ds.child("polygonOptions/points/1/latitude/").getValue(), (double) ds.child("polygonOptions/points/1/longitude/").getValue());
                                            LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                                            if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                                LatLng marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            }
                                        }
                                    }
                                }

                                stillLoadingPolygons = false;

                                if (!stillLoadingCircles) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });
                    }
                }

                showingLarge = true;
                showingEverything = false;
                showingMedium = false;
                showingSmall = false;
                showingPoints = false;

                break;

            // chatviews_menu
            case R.id.showMediumChats:

                Log.i(TAG, "onMenuItemClick() -> showMediumChats");

                // Prevent resetting and reloading everything if this is already the state.
                if (!showingMedium) {

                    mMap.clear();

                    // Remove the polygon and markers.
                    if (newPolygon != null) {

                        newPolygon = null;
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

                            marker3 = null;
                            marker3Position = null;
                            marker3ID = null;
                        }

                        if (marker4 != null) {

                            marker4 = null;
                            marker4Position = null;
                            marker4ID = null;
                        }

                        if (marker5 != null) {

                            marker5 = null;
                            marker5Position = null;
                            marker5ID = null;
                        }

                        if (marker6 != null) {

                            marker6 = null;
                            marker6Position = null;
                            marker6ID = null;
                        }

                        if (marker7 != null) {

                            marker7 = null;
                            marker7Position = null;
                            marker7ID = null;
                        }

                        mlocation = null;
                    }

                    // Remove the circle and markers.
                    if (newCircle != null) {

                        newCircle = null;
                        marker0 = null;
                        marker1 = null;
                        marker0Position = null;
                        marker1Position = null;
                        marker0ID = null;
                        marker1ID = null;
                    }

                    waitingForShapeInformationToProcess = false;
                    waitingForClicksToProcess = false;
                    selectingShape = false;
                    chatsSize = 0;

                    overlappingShapesUUID = new ArrayList<>();
                    overlappingShapesCircleUUID = new ArrayList<>();
                    overlappingShapesPolygonUUID = new ArrayList<>();
                    overlappingShapesCircleLocation = new ArrayList<>();
                    overlappingShapesCircleRadius = new ArrayList<>();
                    overlappingShapesPolygonVertices = new ArrayList<>();

                    // Get rid of the chatSelectorSeekBar.
                    if (chatSelectorSeekBar.getVisibility() != View.INVISIBLE) {

                        chatSelectorSeekBar.setVisibility(View.INVISIBLE);
                        chatSizeSeekBar.setProgress(0);
                        chatSizeSeekBar.setVisibility(View.VISIBLE);
                        chatSelectorSeekBar.setProgress(0);
                    }

                    // Load different colored shapes depending on the map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        loadingIcon.setVisibility(View.VISIBLE);

                        // Load Firebase circles.
                        firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load medium circles (10 < radius <= 50)
                                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 10 && (double) (long) ds.child("circleOptions/radius").getValue() <= 50) {

                                            LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());
                                            double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                            Circle circle = mMap.addCircle(
                                                    new CircleOptions()
                                                            .center(center)
                                                            .clickable(true)
                                                            .radius(radius)
                                                            .strokeColor(Color.YELLOW)
                                                            .strokeWidth(3f)
                                            );

                                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                                            circle.setTag(shapeUUID);
                                        }
                                    }
                                }

                                stillLoadingCircles = false;

                                if (!stillLoadingPolygons) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });

                        // Load Firebase polygons.
                        firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load medium polygons pi(10^2) < polygonArea <= pi(50^2))
                                        if ((double) ds.child("polygonArea").getValue() > Math.PI * (Math.pow(10, 2)) && (double) ds.child("polygonArea").getValue() <= Math.PI * (Math.pow(50, 2))) {

                                            LatLng marker3Position = null;
                                            LatLng marker4Position = null;
                                            LatLng marker5Position = null;
                                            LatLng marker6Position = null;
                                            Polygon polygon;

                                            LatLng marker0Position = new LatLng((double) ds.child("polygonOptions/points/0/latitude/").getValue(), (double) ds.child("polygonOptions/points/0/longitude/").getValue());
                                            LatLng marker1Position = new LatLng((double) ds.child("polygonOptions/points/1/latitude/").getValue(), (double) ds.child("polygonOptions/points/1/longitude/").getValue());
                                            LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                                            if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                                LatLng marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            }
                                        }
                                    }
                                }

                                stillLoadingPolygons = false;

                                if (!stillLoadingCircles) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });
                    } else {

                        loadingIcon.setVisibility(View.VISIBLE);

                        // Load Firebase circles.
                        firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load medium circles (10 < radius <= 50)
                                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 10 && (double) (long) ds.child("circleOptions/radius").getValue() <= 50) {

                                            LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());
                                            double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                            Circle circle = mMap.addCircle(
                                                    new CircleOptions()
                                                            .center(center)
                                                            .clickable(true)
                                                            .radius(radius)
                                                            .strokeColor(Color.rgb(255, 0, 255))
                                                            .strokeWidth(3f)
                                            );

                                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                                            circle.setTag(shapeUUID);
                                        }
                                    }
                                }

                                stillLoadingCircles = false;

                                if (!stillLoadingPolygons) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });

                        // Load Firebase polygons.
                        firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load medium polygons pi(10^2) < polygonArea <= pi(50^2))
                                        if ((double) ds.child("polygonArea").getValue() > Math.PI * (Math.pow(10, 2)) && (double) ds.child("polygonArea").getValue() <= Math.PI * (Math.pow(50, 2))) {

                                            LatLng marker3Position = null;
                                            LatLng marker4Position = null;
                                            LatLng marker5Position = null;
                                            LatLng marker6Position = null;
                                            Polygon polygon;

                                            LatLng marker0Position = new LatLng((double) ds.child("polygonOptions/points/0/latitude/").getValue(), (double) ds.child("polygonOptions/points/0/longitude/").getValue());
                                            LatLng marker1Position = new LatLng((double) ds.child("polygonOptions/points/1/latitude/").getValue(), (double) ds.child("polygonOptions/points/1/longitude/").getValue());
                                            LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                                            if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                                LatLng marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            }
                                        }
                                    }
                                }

                                stillLoadingPolygons = false;

                                if (!stillLoadingCircles) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });
                    }
                }

                showingMedium = true;
                showingEverything = false;
                showingLarge = false;
                showingSmall = false;
                showingPoints = false;

                break;

            // chatviews_menu
            case R.id.showSmallChats:

                Log.i(TAG, "onMenuItemClick() -> showSmallChats");

                // Prevent resetting and reloading everything if this is already the state.
                if (!showingSmall) {

                    mMap.clear();

                    // Remove the polygon and markers.
                    if (newPolygon != null) {

                        newPolygon = null;
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

                            marker3 = null;
                            marker3Position = null;
                            marker3ID = null;
                        }

                        if (marker4 != null) {

                            marker4 = null;
                            marker4Position = null;
                            marker4ID = null;
                        }

                        if (marker5 != null) {

                            marker5 = null;
                            marker5Position = null;
                            marker5ID = null;
                        }

                        if (marker6 != null) {

                            marker6 = null;
                            marker6Position = null;
                            marker6ID = null;
                        }

                        if (marker7 != null) {

                            marker7 = null;
                            marker7Position = null;
                            marker7ID = null;
                        }

                        mlocation = null;
                    }

                    // Remove the circle and markers.
                    if (newCircle != null) {

                        newCircle = null;
                        marker0 = null;
                        marker1 = null;
                        marker0Position = null;
                        marker1Position = null;
                        marker0ID = null;
                        marker1ID = null;
                    }

                    waitingForShapeInformationToProcess = false;
                    waitingForClicksToProcess = false;
                    selectingShape = false;
                    chatsSize = 0;

                    overlappingShapesUUID = new ArrayList<>();
                    overlappingShapesCircleUUID = new ArrayList<>();
                    overlappingShapesPolygonUUID = new ArrayList<>();
                    overlappingShapesCircleLocation = new ArrayList<>();
                    overlappingShapesCircleRadius = new ArrayList<>();
                    overlappingShapesPolygonVertices = new ArrayList<>();

                    // Get rid of the chatSelectorSeekBar.
                    if (chatSelectorSeekBar.getVisibility() != View.INVISIBLE) {

                        chatSelectorSeekBar.setVisibility(View.INVISIBLE);
                        chatSizeSeekBar.setProgress(0);
                        chatSizeSeekBar.setVisibility(View.VISIBLE);
                        chatSelectorSeekBar.setProgress(0);
                    }

                    // Load different colored shapes depending on the map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        loadingIcon.setVisibility(View.VISIBLE);

                        // Load Firebase circles.
                        firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load small circles (1 < radius <= 10)
                                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 1 && (double) (long) ds.child("circleOptions/radius").getValue() <= 10) {

                                            LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());
                                            double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                            Circle circle = mMap.addCircle(
                                                    new CircleOptions()
                                                            .center(center)
                                                            .clickable(true)
                                                            .radius(radius)
                                                            .strokeColor(Color.YELLOW)
                                                            .strokeWidth(3f)
                                            );

                                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                                            circle.setTag(shapeUUID);
                                        }
                                    }
                                }

                                stillLoadingCircles = false;

                                if (!stillLoadingPolygons) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });

                        // Load Firebase polygons.
                        firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load small polygons (pi < polygonArea <= pi(10^2))
                                        if ((double) ds.child("polygonArea").getValue() > (Math.PI) && (double) ds.child("polygonArea").getValue() <= Math.PI * (Math.pow(10, 2))) {

                                            LatLng marker3Position = null;
                                            LatLng marker4Position = null;
                                            LatLng marker5Position = null;
                                            LatLng marker6Position = null;
                                            Polygon polygon;

                                            LatLng marker0Position = new LatLng((double) ds.child("polygonOptions/points/0/latitude/").getValue(), (double) ds.child("polygonOptions/points/0/longitude/").getValue());
                                            LatLng marker1Position = new LatLng((double) ds.child("polygonOptions/points/1/latitude/").getValue(), (double) ds.child("polygonOptions/points/1/longitude/").getValue());
                                            LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                                            if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                                LatLng marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position)
                                                                .strokeColor(Color.YELLOW)
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            }
                                        }
                                    }
                                }

                                stillLoadingPolygons = false;

                                if (!stillLoadingCircles) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });
                    } else {

                        loadingIcon.setVisibility(View.VISIBLE);

                        // Load Firebase circles.
                        firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load small circles (1 < radius <= 10)
                                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 1 && (double) (long) ds.child("circleOptions/radius").getValue() <= 10) {

                                            LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());
                                            double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                            Circle circle = mMap.addCircle(
                                                    new CircleOptions()
                                                            .center(center)
                                                            .clickable(true)
                                                            .radius(radius)
                                                            .strokeColor(Color.rgb(255, 0, 255))
                                                            .strokeWidth(3f)
                                            );

                                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                                            circle.setTag(shapeUUID);
                                        }
                                    }
                                }

                                stillLoadingCircles = false;

                                if (!stillLoadingPolygons) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });

                        // Load Firebase polygons.
                        firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load small polygons (pi < polygonArea <= pi(10^2))
                                        if ((double) ds.child("polygonArea").getValue() > (Math.PI) && (double) ds.child("polygonArea").getValue() <= Math.PI * (Math.pow(10, 2))) {

                                            LatLng marker3Position = null;
                                            LatLng marker4Position = null;
                                            LatLng marker5Position = null;
                                            LatLng marker6Position = null;
                                            Polygon polygon;

                                            LatLng marker0Position = new LatLng((double) ds.child("polygonOptions/points/0/latitude/").getValue(), (double) ds.child("polygonOptions/points/0/longitude/").getValue());
                                            LatLng marker1Position = new LatLng((double) ds.child("polygonOptions/points/1/latitude/").getValue(), (double) ds.child("polygonOptions/points/1/longitude/").getValue());
                                            LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                                            if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                                            }
                                            if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                                LatLng marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position, marker3Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            } else {
                                                polygon = mMap.addPolygon(
                                                        new PolygonOptions()
                                                                .clickable(true)
                                                                .add(marker0Position, marker1Position, marker2Position)
                                                                .strokeColor(Color.rgb(255, 0, 255))
                                                                .strokeWidth(3f)
                                                );

                                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                polygon.setTag(shapeUUID);
                                            }
                                        }
                                    }
                                }

                                stillLoadingPolygons = false;

                                if (!stillLoadingCircles) {

                                    loadingIcon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });
                    }
                }

                showingSmall = true;
                showingEverything = false;
                showingLarge = false;
                showingMedium = false;
                showingPoints = false;

                break;

            // chatviews_menu
            case R.id.showPoints:

                Log.i(TAG, "onMenuItemClick() -> showPoints");

                // Prevent resetting and reloading everything if this is already the state.
                if (!showingPoints) {

                    mMap.clear();

                    // Remove the polygon and markers.
                    if (newPolygon != null) {

                        newPolygon = null;
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

                            marker3 = null;
                            marker3Position = null;
                            marker3ID = null;
                        }

                        if (marker4 != null) {

                            marker4 = null;
                            marker4Position = null;
                            marker4ID = null;
                        }

                        if (marker5 != null) {

                            marker5 = null;
                            marker5Position = null;
                            marker5ID = null;
                        }

                        if (marker6 != null) {

                            marker6 = null;
                            marker6Position = null;
                            marker6ID = null;
                        }

                        if (marker7 != null) {

                            marker7 = null;
                            marker7Position = null;
                            marker7ID = null;
                        }

                        mlocation = null;
                    }

                    // Remove the circle and markers.
                    if (newCircle != null) {

                        newCircle = null;
                        marker0 = null;
                        marker1 = null;
                        marker0Position = null;
                        marker1Position = null;
                        marker0ID = null;
                        marker1ID = null;
                    }

                    waitingForShapeInformationToProcess = false;
                    waitingForClicksToProcess = false;
                    selectingShape = false;
                    chatsSize = 0;

                    overlappingShapesUUID = new ArrayList<>();
                    overlappingShapesCircleUUID = new ArrayList<>();
                    overlappingShapesPolygonUUID = new ArrayList<>();
                    overlappingShapesCircleLocation = new ArrayList<>();
                    overlappingShapesCircleRadius = new ArrayList<>();
                    overlappingShapesPolygonVertices = new ArrayList<>();

                    // Get rid of the chatSelectorSeekBar.
                    if (chatSelectorSeekBar.getVisibility() != View.INVISIBLE) {

                        chatSelectorSeekBar.setVisibility(View.INVISIBLE);
                        chatSizeSeekBar.setProgress(0);
                        chatSizeSeekBar.setVisibility(View.VISIBLE);
                        chatSelectorSeekBar.setProgress(0);
                    }

                    // Load different colored points depending on map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        loadingIcon.setVisibility(View.VISIBLE);

                        // Load Firebase points.
                        firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load points (radius = 1)
                                        if ((double) (long) ds.child("circleOptions/radius").getValue() == 1) {

                                            LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());
                                            double radius = (double) (long) ds.child("circleOptions/radius").getValue();
                                            Circle circle = mMap.addCircle(
                                                    new CircleOptions()
                                                            .center(center)
                                                            .clickable(true)
                                                            .radius(radius)
                                                            .strokeColor(Color.YELLOW)
                                                            .strokeWidth(3f)
                                            );

                                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                                            circle.setTag(shapeUUID);
                                        }
                                    }
                                }

                                loadingIcon.setVisibility(View.GONE);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });
                    } else {

                        loadingIcon.setVisibility(View.VISIBLE);

                        // Load Firebase points.
                        firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (dataSnapshot.getValue() != null) {

                                        // Only load "points" (radius == 1)
                                        if ((double) (long) ds.child("circleOptions/radius").getValue() == 1) {

                                            LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());
                                            Circle circle = mMap.addCircle(
                                                    new CircleOptions()
                                                            .center(center)
                                                            .clickable(true)
                                                            .radius(1)
                                                            .strokeColor(Color.rgb(255, 0, 255))
                                                            .strokeWidth(3f)
                                            );

                                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                                            circle.setTag(shapeUUID);
                                        }
                                    }
                                }

                                loadingIcon.setVisibility(View.GONE);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Log.e(TAG, "DatabaseError");
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(databaseError.getMessage());
                            }
                        });
                    }
                }

                showingPoints = true;
                showingEverything = false;
                showingLarge = false;
                showingMedium = false;
                showingSmall = false;

                break;

            // createchat_menu
            case R.id.createPolygon:

                Log.i(TAG, "onMenuItemClick() -> createPolygon");

                // Gets rid of the shapeTemps if the user wants to make a new shape.
                if (selectingShape) {

                    if (circleTemp != null) {

                        circleTemp.remove();
                    }

                    if (polygonTemp != null) {

                        polygonTemp.remove();
                    }

                    selectingShape = false;

                    // Change the circle color depending on the map type.
                    if (mMap.getMapType() == 2 || mMap.getMapType() == 4) {

                        for (int i = 0; i < overlappingShapesCircleLocation.size(); i++) {

                            Circle circle0 = mMap.addCircle(
                                    new CircleOptions()
                                            .center(overlappingShapesCircleLocation.get(i))
                                            .clickable(true)
                                            .radius(overlappingShapesCircleRadius.get(i))
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            circle0.setTag(overlappingShapesCircleUUID.get(i));
                        }

                        for (int i = 0; i < overlappingShapesPolygonVertices.size(); i++) {

                            Polygon polygon0 = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .addAll(overlappingShapesPolygonVertices.get(i))
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            polygon0.setTag(overlappingShapesPolygonUUID.get(i));
                        }
                    } else {

                        for (int i = 0; i < overlappingShapesUUID.size(); i++) {

                            Circle circle0 = mMap.addCircle(
                                    new CircleOptions()
                                            .center(overlappingShapesCircleLocation.get(i))
                                            .clickable(true)
                                            .radius(overlappingShapesCircleRadius.get(i))
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            circle0.setTag(overlappingShapesCircleUUID.get(i));
                        }

                        for (int i = 0; i < overlappingShapesPolygonVertices.size(); i++) {

                            Polygon polygon0 = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .addAll(overlappingShapesPolygonVertices.get(i))
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            polygon0.setTag(overlappingShapesPolygonUUID.get(i));
                        }
                    }

                    overlappingShapesUUID = new ArrayList<>();
                    overlappingShapesCircleUUID = new ArrayList<>();
                    overlappingShapesPolygonUUID = new ArrayList<>();
                    overlappingShapesCircleLocation = new ArrayList<>();
                    overlappingShapesCircleRadius = new ArrayList<>();
                    overlappingShapesPolygonVertices = new ArrayList<>();

                    chatSelectorSeekBar.setVisibility(View.INVISIBLE);
                    chatSizeSeekBar.setVisibility(View.VISIBLE);
                    chatSizeSeekBar.setProgress(0);
                    chatSelectorSeekBar.setProgress(0);

                    chatsSize = 0;
                }

                // Remove the polygon and markers.
                if (newPolygon != null) {

                    newPolygon.remove();
                    marker0.remove();
                    marker1.remove();
                    marker2.remove();
                    newPolygon = null;
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
                if (newCircle != null) {

                    newCircle.remove();
                    marker0.remove();
                    marker1.remove();
                    newCircle = null;
                    marker0 = null;
                    marker1 = null;
                    marker0Position = null;
                    marker1Position = null;
                    marker0ID = null;
                    marker1ID = null;
                }

                // Check location permissions.
                if (ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    // Creates a polygon.
                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                @Override
                                public void onSuccess(Location location) {

                                    // Get last known location. In some rare situations, this can be null.
                                    if (location != null) {

                                        // Load different colored shapes depending on the map type.
                                        if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

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

                                            // Update the global variable for use when a user clicks on the polygon to go to recyclerviewlayout without updating the marker locations.
                                            LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position};
                                            polygonPointsList = Arrays.asList(polygonPoints);

                                            newPolygon = mMap.addPolygon(polygonOptions);
                                        } else {

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
                                                            .strokeColor(Color.rgb(255, 0, 255))
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

                                            // Update the global variable for use when a user clicks on the polygon to go to recyclerviewlayout without updating the marker locations.
                                            LatLng[] polygonPoints = new LatLng[]{marker0Position, marker1Position, marker2Position};
                                            polygonPointsList = Arrays.asList(polygonPoints);

                                            newPolygon = mMap.addPolygon(polygonOptions);
                                        }
                                    } else {

                                        Log.e(TAG, "createPolygon -> location == null");
                                        toastMessageLong("An error occurred: your location is null");
                                    }
                                }
                            });
                } else {

                    checkLocationPermissions();
                }

                break;

            // createchat_menu
            case R.id.createCircle:

                Log.i(TAG, "onMenuItemClick() -> createCircle");

                // Gets rid of the shapeTemps if the user wants to make a new shape.
                if (selectingShape) {

                    if (circleTemp != null) {

                        circleTemp.remove();
                    }

                    if (polygonTemp != null) {

                        polygonTemp.remove();
                    }

                    selectingShape = false;

                    // Change the circle color depending on the map type.
                    if (mMap.getMapType() == 2 || mMap.getMapType() == 4) {

                        for (int i = 0; i < overlappingShapesCircleLocation.size(); i++) {

                            Circle circle0 = mMap.addCircle(
                                    new CircleOptions()
                                            .center(overlappingShapesCircleLocation.get(i))
                                            .clickable(true)
                                            .radius(overlappingShapesCircleRadius.get(i))
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            circle0.setTag(overlappingShapesCircleUUID.get(i));
                        }

                        for (int i = 0; i < overlappingShapesPolygonVertices.size(); i++) {

                            Polygon polygon0 = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .addAll(overlappingShapesPolygonVertices.get(i))
                                            .strokeColor(Color.rgb(255, 255, 0))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            polygon0.setTag(overlappingShapesPolygonUUID.get(i));
                        }
                    } else {

                        for (int i = 0; i < overlappingShapesUUID.size(); i++) {

                            Circle circle0 = mMap.addCircle(
                                    new CircleOptions()
                                            .center(overlappingShapesCircleLocation.get(i))
                                            .clickable(true)
                                            .radius(overlappingShapesCircleRadius.get(i))
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            circle0.setTag(overlappingShapesCircleUUID.get(i));
                        }

                        for (int i = 0; i < overlappingShapesPolygonVertices.size(); i++) {

                            Polygon polygon0 = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .addAll(overlappingShapesPolygonVertices.get(i))
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                                            .zIndex(0)
                            );

                            polygon0.setTag(overlappingShapesPolygonUUID.get(i));
                        }
                    }

                    overlappingShapesUUID = new ArrayList<>();
                    overlappingShapesCircleUUID = new ArrayList<>();
                    overlappingShapesPolygonUUID = new ArrayList<>();
                    overlappingShapesCircleLocation = new ArrayList<>();
                    overlappingShapesCircleRadius = new ArrayList<>();
                    overlappingShapesPolygonVertices = new ArrayList<>();

                    chatSelectorSeekBar.setVisibility(View.INVISIBLE);
                    chatSizeSeekBar.setVisibility(View.VISIBLE);
                    chatSizeSeekBar.setProgress(0);
                    chatSelectorSeekBar.setProgress(0);

                    chatsSize = 0;
                }

                // Remove the polygon and markers.
                if (newPolygon != null) {

                    newPolygon.remove();
                    marker0.remove();
                    marker1.remove();
                    marker2.remove();
                    newPolygon = null;
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
                if (newCircle != null) {

                    newCircle.remove();
                    marker0.remove();
                    marker1.remove();
                    newCircle = null;
                    marker0 = null;
                    marker1 = null;
                    marker0Position = null;
                    marker1Position = null;
                    marker0ID = null;
                    marker1ID = null;
                }

                // Check location permissions.
                if (ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    // Creates circle.
                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                @Override
                                public void onSuccess(Location location) {
                                    // Get last known location. In some rare situations, this can be null.
                                    if (location != null) {

                                        // Load different colored shapes depending on the map type.
                                        if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                                            chatSizeSeekBar.setProgress(88);

                                            // Logic to handle location object.
                                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                            marker1Position = new LatLng(location.getLatitude() + 0.0001, location.getLongitude());
                                            float circleRadius = distanceGivenLatLng(location.getLatitude(), location.getLongitude(), marker1Position.latitude, marker1Position.longitude);
                                            CircleOptions circleOptions =
                                                    new CircleOptions()
                                                            .center(latLng)
                                                            .clickable(true)
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

                                            newCircle = mMap.addCircle(circleOptions);
                                            chatSizeSeekBar.setProgress((int) distanceGivenLatLng(location.getLatitude(), location.getLongitude(), marker1Position.latitude, marker1Position.longitude));
                                        } else {

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
                                                            .radius(circleRadius)
                                                            .strokeColor(Color.rgb(255, 0, 255))
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

                                            newCircle = mMap.addCircle(circleOptions);
                                            chatSizeSeekBar.setProgress((int) distanceGivenLatLng(location.getLatitude(), location.getLongitude(), marker1Position.latitude, marker1Position.longitude));
                                        }
                                    } else {

                                        Log.e(TAG, "createCircle -> location == null");
                                        toastMessageLong("An error occurred: your location is null");
                                    }
                                }
                            });
                } else {

                    checkLocationPermissions();
                }

                break;

            // createchat_menu
            case R.id.removeShape:

                Log.i(TAG, "onMenuItemClick() -> removeShape");

                // Remove the polygon and markers.
                if (newPolygon != null) {

                    newPolygon.remove();
                    marker0.remove();
                    marker1.remove();
                    marker2.remove();
                    newPolygon = null;
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
                if (newCircle != null) {

                    newCircle.remove();
                    marker0.remove();
                    marker1.remove();
                    newCircle = null;
                    marker0 = null;
                    marker1 = null;
                    marker0Position = null;
                    marker1Position = null;
                    marker0ID = null;
                    marker1ID = null;
                }

                chatSizeSeekBar.setProgress(0);
                relativeAngle = 0.0;
                usedSeekBar = false;

                break;

            // createchat_menu
            case R.id.createPoint:

                Log.i(TAG, "onMenuItemClick() -> createPoint");

                // Check location permissions.
                if (ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    // Create a point and to to Chat.java or SignIn.java.
                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, new OnSuccessListener<Location>() {

                                @Override
                                public void onSuccess(final Location location) {

                                    // Get last known location. In some rare situations, this can be null.
                                    if (location != null) {

                                        // Remove any other shape before adding the circle to Firebase.
                                        if (newCircle != null) {

                                            newCircle.remove();
                                            newCircle = null;
                                        }

                                        if (newPolygon != null) {

                                            newPolygon.remove();
                                            newPolygon = null;
                                        }

                                        // Add circle to the map and go to recyclerviewlayout.
                                        if (mMap != null) {

                                            shapeUUID = UUID.randomUUID().toString();

                                            // Check if the user is already signed in.
                                            if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                                // User is signed in.
                                                // Connect to Firebase.
                                                firebaseCircles.orderByChild("shapeUUID").equalTo(shapeUUID).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                        // If the uuid already exists in Firebase, generate another uuid and try again.
                                                        if (dataSnapshot.exists()) {

                                                            shapeUUID = UUID.randomUUID().toString();

                                                            // Carry the extras all the way to Chat.java.
                                                            Intent Activity = new Intent(Map.this, Navigation.class);
                                                            Activity.putExtra("shapeIsCircle", true);
                                                            // Pass this boolean value to Chat.java.
                                                            Activity.putExtra("newShape", true);
                                                            // Pass this value to Chat.java to identify the shape.
                                                            Activity.putExtra("shapeUUID", shapeUUID);
                                                            // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
                                                            Activity.putExtra("userIsWithinShape", true);
                                                            // Pass this information to Chat.java to create a new circle in Firebase after someone writes a recyclerviewlayout.
                                                            Activity.putExtra("circleLatitude", location.getLatitude());
                                                            Activity.putExtra("circleLongitude", location.getLongitude());
                                                            Activity.putExtra("radius", 1.0);
                                                            startActivity(Activity);
                                                        } else {

                                                            // Carry the extras all the way to Chat.java.
                                                            Intent Activity = new Intent(Map.this, Navigation.class);
                                                            Activity.putExtra("shapeIsCircle", true);
                                                            // Pass this boolean value to Chat.java.
                                                            Activity.putExtra("newShape", true);
                                                            // Pass this value to Chat.java to identify the shape.
                                                            Activity.putExtra("shapeUUID", shapeUUID);
                                                            // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
                                                            Activity.putExtra("userIsWithinShape", true);
                                                            // Pass this information to Chat.java to create a new circle in Firebase after someone writes a recyclerviewlayout.
                                                            Activity.putExtra("circleLatitude", location.getLatitude());
                                                            Activity.putExtra("circleLongitude", location.getLongitude());
                                                            Activity.putExtra("radius", 1.0);
                                                            startActivity(Activity);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        Log.e(TAG, "DatabaseError");
                                                        loadingIcon.setVisibility(View.GONE);
                                                        toastMessageLong(databaseError.getMessage());
                                                    }
                                                });
                                            } else {

                                                // No user is signed in.
                                                // Connect to Firebase.
                                                firebaseCircles.orderByChild("shapeUUID").equalTo(shapeUUID).addListenerForSingleValueEvent(new ValueEventListener() {

                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                        // If the uuid already exists in Firebase, generate another uuid and try again.
                                                        if (dataSnapshot.exists()) {

                                                            shapeUUID = UUID.randomUUID().toString();

                                                            // Carry the extras all the way to Chat.java.
                                                            Intent Activity = new Intent(Map.this, SignIn.class);
                                                            Activity.putExtra("shapeIsCircle", true);
                                                            // Pass this boolean value to Chat.java.
                                                            Activity.putExtra("newShape", true);
                                                            // Pass this value to Chat.java to identify the shape.
                                                            Activity.putExtra("shapeUUID", shapeUUID);
                                                            // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
                                                            Activity.putExtra("userIsWithinShape", true);
                                                            // Pass this information to Chat.java to create a new circle in Firebase after someone writes a recyclerviewlayout.
                                                            Activity.putExtra("circleLatitude", location.getLatitude());
                                                            Activity.putExtra("circleLongitude", location.getLongitude());
                                                            Activity.putExtra("radius", 1.0);
                                                            startActivity(Activity);
                                                        } else {

                                                            // Carry the extras all the way to Chat.java.
                                                            Intent Activity = new Intent(Map.this, SignIn.class);
                                                            Activity.putExtra("shapeIsCircle", true);
                                                            // Pass this boolean value to Chat.java.
                                                            Activity.putExtra("newShape", true);
                                                            // Pass this value to Chat.java to identify the shape.
                                                            Activity.putExtra("shapeUUID", shapeUUID);
                                                            // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
                                                            Activity.putExtra("userIsWithinShape", true);
                                                            // Pass this information to Chat.java to create a new circle in Firebase after someone writes a recyclerviewlayout.
                                                            Activity.putExtra("circleLatitude", location.getLatitude());
                                                            Activity.putExtra("circleLongitude", location.getLongitude());
                                                            Activity.putExtra("radius", 1.0);
                                                            startActivity(Activity);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        Log.e(TAG, "DatabaseError");
                                                        loadingIcon.setVisibility(View.GONE);
                                                        toastMessageLong(databaseError.getMessage());
                                                    }
                                                });
                                            }
                                        }
                                    }
                                }
                            });
                } else {

                    checkLocationPermissions();
                }

                break;

            default:

                break;
        }

        return false;
    }

    private void purpleAdjustmentsForMap() {

        Log.i(TAG, "purpleAdjustmentsForMap()");

        // Load shapes from Firebase and make them purple.
        purpleLoadFirebaseShapes();

        // Change button color depending on map type.
        chatViewsButton.setBackgroundResource(R.drawable.chatviews_button_purple);

        createChatButton.setBackgroundResource(R.drawable.createchat_button_purple);

        settingsButton.setBackgroundResource(R.drawable.ic_more_vert_purple_24dp);

        dmButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.purple));

        randomButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.purple));

        // Remove the polygon and markers.
        if (newPolygon != null) {

            newPolygon = null;
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

                marker3 = null;
                marker3Position = null;
                marker3ID = null;
            }

            if (marker4 != null) {

                marker4 = null;
                marker4Position = null;
                marker4ID = null;
            }

            if (marker5 != null) {

                marker5 = null;
                marker5Position = null;
                marker5ID = null;
            }

            if (marker6 != null) {

                marker6 = null;
                marker6Position = null;
                marker6ID = null;
            }

            if (marker7 != null) {

                marker7 = null;
                marker7Position = null;
                marker7ID = null;
            }
        }

        // Remove the circle and markers.
        if (newCircle != null) {

            newCircle = null;
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
        showingEverything = true;
        showingLarge = false;
        showingMedium = false;
        showingSmall = false;
        showingPoints = false;

        // Create a circleTemp or polygonTemp if one already exists.
        if (chatSelectorSeekBar.getVisibility() == View.VISIBLE) {

            // Create an arrayList that combines a representative array from a circle and polygon to get a full list that represents the two.
            ArrayList<Object> combinedList = new ArrayList<>();

            // Add the smaller array fist for consistency with the rest of the logic.
            if (overlappingShapesCircleLocation.size() > overlappingShapesPolygonVertices.size()) {

                combinedList.addAll(overlappingShapesPolygonVertices);
                combinedList.addAll(overlappingShapesCircleLocation);
            } else {

                combinedList.addAll(overlappingShapesCircleLocation);
                combinedList.addAll(overlappingShapesPolygonVertices);
            }

            selectedOverlappingShapeUUID = overlappingShapesUUID.get(chatSelectorSeekBar.getProgress());

            if (selectedOverlappingShapeUUID != null) {

                if (circleTemp != null) {

                    circleTemp.remove();
                }

                if (polygonTemp != null) {

                    polygonTemp.remove();
                }

                // Change the shape color depending on the map type.
                if (combinedList.get(chatSelectorSeekBar.getProgress()) instanceof LatLng) {

                    circleTemp = mMap.addCircle(
                            new CircleOptions()
                                    .center(selectedOverlappingShapeCircleLocation)
                                    .clickable(true)
                                    .fillColor(Color.argb(100, 255, 0, 255))
                                    .radius(selectedOverlappingShapeCircleRadius)
                                    .strokeColor(Color.rgb(255, 0, 255))
                                    .strokeWidth(3f)
                                    .zIndex(2)
                    );

                    // Used when getting rid of the shapes in onMapClick.
                    circleTemp.setTag(selectedOverlappingShapeUUID);
                    circleTemp.setCenter(selectedOverlappingShapeCircleLocation);
                    circleTemp.setRadius(selectedOverlappingShapeCircleRadius);
                } else {

                    polygonTemp = mMap.addPolygon(
                            new PolygonOptions()
                                    .clickable(true)
                                    .fillColor(Color.argb(100, 255, 0, 255))
                                    .strokeColor(Color.rgb(255, 0, 255))
                                    .strokeWidth(3f)
                                    .addAll(selectedOverlappingShapePolygonVertices)
                                    .zIndex(2)
                    );

                    // Used when getting rid of the shapes in onMapClick.
                    polygonTemp.setTag(selectedOverlappingShapeUUID);
                }
            }
        }
    }

    private void purpleLoadFirebaseShapes() {

        Log.i(TAG, "purpleLoadFirebaseShapes()");

        loadingIcon.setVisibility(View.VISIBLE);

        showingEverything = true;

        // Load Firebase points and circles.
        firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    if (dataSnapshot.getValue() != null) {

                        double radius = 0;

                        LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());
                        if ((ds.child("circleOptions/radius").getValue()) != null) {

                            radius = ((Number) (Objects.requireNonNull(ds.child("circleOptions/radius").getValue()))).doubleValue();
                        }
                        Circle circle = mMap.addCircle(
                                new CircleOptions()
                                        .center(center)
                                        .clickable(true)
                                        .radius(radius)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                        shapeUUID = (String) ds.child("shapeUUID").getValue();

                        circle.setTag(shapeUUID);
                    }
                }

                // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                stillLoadingCircles = false;

                if (!stillLoadingPolygons && !stillUpdatingCamera) {

                    loadingIcon.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                Log.e(TAG, "DatabaseError");
                loadingIcon.setVisibility(View.GONE);
                toastMessageLong(databaseError.getMessage());
            }
        });

        // Load Firebase polygons.
        firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    if (dataSnapshot.getValue() != null) {

                        LatLng marker3Position = null;
                        LatLng marker4Position = null;
                        LatLng marker5Position = null;
                        LatLng marker6Position = null;
                        Polygon polygon;

                        LatLng marker0Position = new LatLng((double) ds.child("polygonOptions/points/0/latitude/").getValue(), (double) ds.child("polygonOptions/points/0/longitude/").getValue());
                        LatLng marker1Position = new LatLng((double) ds.child("polygonOptions/points/1/latitude/").getValue(), (double) ds.child("polygonOptions/points/1/longitude/").getValue());
                        LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                        if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                            marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                        }
                        if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                            marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                        }
                        if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                            marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                        }
                        if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                            marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                        }
                        if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                            LatLng marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position)
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position)
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position)
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position)
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position, marker3Position)
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        } else {
                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position)
                                            .strokeColor(Color.rgb(255, 0, 255))
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        }
                    }
                }

                // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                stillLoadingPolygons = false;

                if (!stillLoadingCircles && !stillUpdatingCamera) {

                    loadingIcon.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                Log.e(TAG, "DatabaseError");
                loadingIcon.setVisibility(View.GONE);
                toastMessageLong(databaseError.getMessage());
            }
        });
    }

    private void yellowAdjustmentsForMap() {

        Log.i(TAG, "yellowAdjustmentsForMap()");

        // Load shapes from Firebase and make them yellow.
        yellowLoadFirebaseShapes();

        // Change button color depending on map type.
        chatViewsButton.setBackgroundResource(R.drawable.chatviews_button);

        createChatButton.setBackgroundResource(R.drawable.createchat_button);

        settingsButton.setBackgroundResource(R.drawable.ic_more_vert_yellow_24dp);

        dmButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.yellow));

        randomButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.yellow));

        // Remove the polygon and markers.
        if (newPolygon != null) {

            newPolygon = null;
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

                marker3 = null;
                marker3Position = null;
                marker3ID = null;
            }

            if (marker4 != null) {

                marker4 = null;
                marker4Position = null;
                marker4ID = null;
            }

            if (marker5 != null) {

                marker5 = null;
                marker5Position = null;
                marker5ID = null;
            }

            if (marker6 != null) {

                marker6 = null;
                marker6Position = null;
                marker6ID = null;
            }

            if (marker7 != null) {

                marker7 = null;
                marker7Position = null;
                marker7ID = null;
            }
        }

        // Remove the circle and markers.
        if (newCircle != null) {

            newCircle = null;
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
        showingEverything = true;
        showingLarge = false;
        showingMedium = false;
        showingSmall = false;
        showingPoints = false;

        // Create a circleTemp or polygonTemp if one already exists.
        if (chatSelectorSeekBar.getVisibility() == View.VISIBLE) {

            // Create an arrayList that combines a representative array from a circle and polygon to get a full list that represents the two.
            ArrayList<Object> combinedList = new ArrayList<>();

            // Add the smaller array fist for consistency with the rest of the logic.
            if (overlappingShapesCircleLocation.size() > overlappingShapesPolygonVertices.size()) {

                combinedList.addAll(overlappingShapesPolygonVertices);
                combinedList.addAll(overlappingShapesCircleLocation);
            } else {

                combinedList.addAll(overlappingShapesCircleLocation);
                combinedList.addAll(overlappingShapesPolygonVertices);
            }

            selectedOverlappingShapeUUID = overlappingShapesUUID.get(chatSelectorSeekBar.getProgress());

            if (selectedOverlappingShapeUUID != null) {

                if (circleTemp != null) {

                    circleTemp.remove();
                }

                if (polygonTemp != null) {

                    polygonTemp.remove();
                }

                // Change the shape color depending on the map type.
                if (combinedList.get(chatSelectorSeekBar.getProgress()) instanceof LatLng) {

                    circleTemp = mMap.addCircle(
                            new CircleOptions()
                                    .center(selectedOverlappingShapeCircleLocation)
                                    .clickable(true)
                                    .fillColor(Color.argb(100, 255, 255, 0))
                                    .radius(selectedOverlappingShapeCircleRadius)
                                    .strokeColor(Color.rgb(255, 255, 0))
                                    .strokeWidth(3f)
                                    .zIndex(2)
                    );

                    // Used when getting rid of the shapes in onMapClick.
                    circleTemp.setTag(selectedOverlappingShapeUUID);
                    circleTemp.setCenter(selectedOverlappingShapeCircleLocation);
                    circleTemp.setRadius(selectedOverlappingShapeCircleRadius);
                } else {

                    polygonTemp = mMap.addPolygon(
                            new PolygonOptions()
                                    .clickable(true)
                                    .fillColor(Color.argb(100, 255, 255, 0))
                                    .strokeColor(Color.rgb(255, 255, 0))
                                    .strokeWidth(3f)
                                    .addAll(selectedOverlappingShapePolygonVertices)
                                    .zIndex(2)
                    );

                    // Used when getting rid of the shapes in onMapClick.
                    polygonTemp.setTag(selectedOverlappingShapeUUID);
                }
            }
        }
    }

    private void yellowLoadFirebaseShapes() {

        Log.i(TAG, "yellowLoadFirebaseShapes()");

        loadingIcon.setVisibility(View.VISIBLE);

        showingEverything = true;

        // Load Firebase points and circles.
        firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    if (dataSnapshot.getValue() != null) {

                        double radius = 0;

                        LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());
                        if ((ds.child("circleOptions/radius").getValue()) != null) {

                            radius = ((Number) (Objects.requireNonNull(ds.child("circleOptions/radius").getValue()))).doubleValue();
                        }
                        Circle circle = mMap.addCircle(
                                new CircleOptions()
                                        .center(center)
                                        .clickable(true)
                                        .radius(radius)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                        shapeUUID = (String) ds.child("shapeUUID").getValue();

                        circle.setTag(shapeUUID);
                    }
                }

                // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                stillLoadingCircles = false;

                if (!stillLoadingPolygons && !stillUpdatingCamera) {

                    loadingIcon.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                Log.e(TAG, "DatabaseError");
                loadingIcon.setVisibility(View.GONE);
                toastMessageLong(databaseError.getMessage());
            }
        });

        // Load Firebase polygons.
        firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    if (dataSnapshot.getValue() != null) {

                        LatLng marker3Position = null;
                        LatLng marker4Position = null;
                        LatLng marker5Position = null;
                        LatLng marker6Position = null;
                        Polygon polygon;

                        LatLng marker0Position = new LatLng((double) ds.child("polygonOptions/points/0/latitude/").getValue(), (double) ds.child("polygonOptions/points/0/longitude/").getValue());
                        LatLng marker1Position = new LatLng((double) ds.child("polygonOptions/points/1/latitude/").getValue(), (double) ds.child("polygonOptions/points/1/longitude/").getValue());
                        LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                        if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                            marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                        }
                        if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                            marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                        }
                        if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                            marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                        }
                        if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                            marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                        }
                        if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                            LatLng marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position)
                                            .strokeColor(Color.YELLOW)
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position)
                                            .strokeColor(Color.YELLOW)
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position)
                                            .strokeColor(Color.YELLOW)
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position, marker3Position, marker4Position)
                                            .strokeColor(Color.YELLOW)
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position, marker3Position)
                                            .strokeColor(Color.YELLOW)
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        } else {
                            polygon = mMap.addPolygon(
                                    new PolygonOptions()
                                            .clickable(true)
                                            .add(marker0Position, marker1Position, marker2Position)
                                            .strokeColor(Color.YELLOW)
                                            .strokeWidth(3f)
                            );

                            // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                            shapeUUID = (String) ds.child("shapeUUID").getValue();

                            polygon.setTag(shapeUUID);
                        }
                    }
                }

                // The following 3 boolean will determine when the loading icon will disappear. It should only disappear once the camera is adjusted and all shapes are loaded.
                stillLoadingPolygons = false;

                if (!stillLoadingCircles && !stillUpdatingCamera) {

                    loadingIcon.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                Log.e(TAG, "DatabaseError");
                loadingIcon.setVisibility(View.GONE);
                toastMessageLong(databaseError.getMessage());
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        // If user already clicked on a circle, don't change the x and y values.
        if (waitingForClicksToProcess) {

            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            x = event.getX();
            y = event.getY();
        }

        return super.dispatchTouchEvent(event);
    }

    private void touchAgain() {

        Log.i(TAG, "touchAgain()");

        // If x and y are the default value, return.
        if (x == 0.0f || y == 0.0f) {

            return;
        }

        // Obtain MotionEvent object
        int downTime = 1;
        int eventTime = 1;
        // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
        int metaState = 0;

        MotionEvent motionEventDown = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                metaState
        );

        View v = findViewById(R.id.activity_maps);

        v.dispatchTouchEvent(motionEventDown);

        motionEventDown.recycle();

        MotionEvent motionEventUp = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                metaState
        );

        v.dispatchTouchEvent(motionEventUp);

        motionEventUp.recycle();
    }

    // Returns the distance between 2 latitudes and longitudes in meters.
    private static float distanceGivenLatLng(double lat1, double lng1, double lat2, double lng2) {

        Log.i(TAG, "distanceGivenLatLng");

        double earthRadius = 6371000; // Meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                        sin(dLng / 2) * sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (float) (earthRadius * c);
    }

    // Returns the angle between 2 latitudes and longitudes in degrees. If lat1, lng1 are circle's center, this will return 0 for 12 o'clock and 90 for 3 o'clock.
    private double angleFromCoordinate(double lat1, double lng1, double lat2, double lng2) {

        Log.i(TAG, "angleFromCoordinate");

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

        Log.i(TAG, "latLngGivenDistance()");

        double brngRad = Math.toRadians(bearing);
        double latRad = Math.toRadians(latitude);
        double lonRad = Math.toRadians(longitude);
        int earthRadiusInMetres = 6371000;
        double distFrac = distanceInMetres / earthRadiusInMetres;

        double latitudeResult = Math.asin(sin(latRad) * cos(distFrac) + cos(latRad) * sin(distFrac) * cos(brngRad));
        double a = Math.atan2(sin(brngRad) * sin(distFrac) * cos(latRad), cos(distFrac) - sin(latRad) * sin(latitudeResult));
        double longitudeResult = (lonRad + a + 3 * Math.PI) % (2 * Math.PI) - Math.PI;

        return new LatLng(Math.toDegrees(latitudeResult), Math.toDegrees(longitudeResult));
    }

    private void deleteDirectory(File file) {

        Log.i(TAG, "deleteDirectory()");

        if (file.exists()) {

            if (file.isDirectory()) {

                File[] files = file.listFiles();

                if (files != null) {

                    for (File value : files) {

                        if (value.isDirectory()) {

                            deleteDirectory(value);
                        } else {

                            if (value.delete()) {
                            } else {
                            }
                        }
                    }
                }
            }

            if (file.delete()) {
            } else {
            }
        }
    }

    private void toastMessageLong(String message) {

        longToast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG);
        longToast.setGravity(Gravity.CENTER, 0, 0);
        longToast.show();
    }
}