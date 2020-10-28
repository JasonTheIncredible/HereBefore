package co.clixel.herebefore;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
    private ChildEventListener childEventListenerDMs, childEventListenerShapesLarge, childEventListenerShapesMedium, childEventListenerShapesSmall, childEventListenerShapesPoints;
    private SeekBar chatSizeSeekBar, chatSelectorSeekBar;
    private String userEmailFirebase, shapeUUID, marker0ID, marker1ID, marker2ID, marker3ID, marker4ID, marker5ID, marker6ID, marker7ID, selectedOverlappingShapeUUID;
    private Button createChatButton, chatViewsButton, mapTypeButton, settingsButton;
    private PopupMenu popupMapType, popupChatViews, popupCreateChat;
    private boolean locationProviderDisabled = false, firstLoadCamera = true, firstLoadShapes = true, firstLoadDMs = true, dmExists = false, largeExists = false, mediumExists = false, smallExists = false, pointsExist = false, mapChanged, cameraMoved = false, waitingForBetterLocationAccuracy = false, badAccuracy = false,
            waitingForClicksToProcess = false, waitingForShapeInformationToProcess = false, markerOutsidePolygon = false, usedSeekBar = false,
            userIsWithinShape, selectingShape = false, threeMarkers = false, fourMarkers = false, fiveMarkers = false, sixMarkers = false, sevenMarkers = false, eightMarkers = false,
            restarted = false, newCameraCoordinates = false, showEverything = true, showLarge = false, showMedium = false, showSmall = false, showPoints = false, mapCleared = false;
    private Boolean showingEverything, showingLarge, showingMedium, showingSmall, showingPoints;
    private LatLng markerPositionAtVertexOfPolygon, marker0Position, marker1Position, marker2Position, marker3Position, marker4Position, marker5Position, marker6Position, marker7Position, selectedOverlappingShapeCircleLocation;
    private Double relativeAngle = 0.0, selectedOverlappingShapeCircleRadius;
    private Location mlocation;
    private List<LatLng> polygonPointsList, selectedOverlappingShapePolygonVertices;
    private ArrayList<String> overlappingShapesUUID = new ArrayList<>(), overlappingShapesCircleUUID = new ArrayList<>(), overlappingShapesPolygonUUID = new ArrayList<>();
    private final ArrayList<String> circleUUIDListForMapChange = new ArrayList<>(), polygonUUIDListForMapChange = new ArrayList<>();
    private ArrayList<LatLng> overlappingShapesCircleLocation = new ArrayList<>();
    private final ArrayList<LatLng> circleCenterListForMapChange = new ArrayList<>();
    private ArrayList<Double> overlappingShapesCircleRadius = new ArrayList<>();
    private final ArrayList<Double> circleRadiusArrayList = new ArrayList<>();
    private ArrayList<java.util.List<LatLng>> overlappingShapesPolygonVertices = new ArrayList<>();
    private final ArrayList<java.util.List<LatLng>> polygonPointsListForMapChange = new ArrayList<>();
    private float x, y;
    private int chatsSize, dmCounter = 0, newNearLeftLat, newNearLeftLon;
    private Toast longToast;
    private View loadingIcon;
    private CounterFab dmButton;
    private LocationManager locationManager;
    private Query queryDMs, queryShapesLarge, queryShapesMedium, queryShapesSmall, queryShapesPoints;
    private Pair<Integer, Integer> oldNearLeft, oldFarLeft, oldNearRight, oldFarRight, newNearLeft, newFarLeft, newNearRight, newFarRight;
    private final List<Pair<Integer, Integer>> loadedCoordinates = new ArrayList<>();

    // Don't use the compressed video if it is bigger than the original video.
    // Allow user to click on a mention in Chat and scroll to that mention for context.
    // Uploading a picture takes a long time.
    // Make a better loading icon, with a progress bar.
    // Loading icon for Glide images in Chat and DirectMentions, and cut down on loading time.
    // If user is on a point, prevent creating a new one. Deal with overlapping shapes in general. Maybe a warning message?
    // Require picture on creating a shape? Also, long press a shape to see a popup of that picture.
    // Make situations where Firebase circles are added to the map and then polygons are added (like in chatViews) async? Also, do that for changing map and therefore changing shape colors.
    // Panoramic view?
    // Show direction camera was facing when taking photo?
    // Get rid of "larger" shapes and only allow points? (Or make allowable shapes smaller?)
    // Remember the AC: Origins inspiration. Also, airdrop - create items in the world.
    // Prevent data scraping (hide email addresses and other personal information).
    // Create timer that kicks people out of a new Chat if they haven't posted within an amount of time, or keep updating their location.
    // Allow private posts or sharing with specific people.
    // Prevent spamming messages.
    // Let users allow specific other users to see their name.
    // Think of way to make "creating a point" more accurate to the user's location.
    // Make scrollToPosition work in Chat after a restart. Also prevent reloading Chat and DMs every time app restarts.
    // Find a way to not clear and reload map every time user returns from clicking a shape. Same with DM notification.
    // When sending DMs back and forth, verify that a user was included in the last DM (as any anonymous UUID could pretend to be the last person).
    // Create "my locations" or "my photos" and see friends' locations / follow friends?
    // Leave messages in locations that users get notified of when they enter the area by adding geo-fencing..
    // Find a way to add to existing snapshot - then send that snapshot to DirectMentions from Map. Also, prevent reloading everything after restart when user paginated (also save scroll position).
    // Update general look of app.
    // After clicking on a DM and going to that Chat, allow user to find that same shape on the map.
    // Create a "general chat" where everyone can chat anonymously, maybe with more specific location rooms too? Delete general chat after x amount of time or # of items.
    // Add some version of the random button, or allow users to click on a circle in a far away area while zoomed out on map.
    // Make recyclerView load faster, possibly by adding layouts for all video/picture and then adding them when possible. Also, fix issue where images / videos are changing size with orientation change. Possible: Send image dimensions to Firebase and set a "null" image of that size.
    // Add preference for shape color.
    //// Add ability to add images and video to general chat and Chat from gallery. Distinguish them from media added from location. Github 8/29.
    // Add ability to add both picture and video to firebase at the same time.
    // Increase viral potential - make it easier to share?
    // Add ability to filter recyclerView by type of content (recorded at the scene...).
    // Load preferences after logging out and back in - looks like it will require saving info to database; is this worth it?

    // Decrease app size (compress repeating code into methods) / Check on accumulation of size over time.
    // Work on deprecated methods.
    // Check warning messages.
    // Finish setting up Google ads, then add more ads.
    // Adjust AppIntro.
    // Make sure Firebase has enough bandwidth.
    // Make sure aboutLibraries includes all libraries, and make sure all licenses are fair use (NOT GPL).
    // Make sure the secret stuff is secret.
    // Switch existing values in Firebase.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        // Show the intro if the user has not yet seen it.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean showIntro = sharedPreferences.getBoolean(SettingsFragment.KEY_SHOW_INTRO, true);

        if (showIntro) {

            Intent Activity = new Intent(this, MyAppIntro.class);
            startActivity(Activity);
            finish();
        }

        setContentView(R.layout.map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.activity_maps);
        if (mapFragment != null) {

            mapFragment.getMapAsync(this);
        } else {

            Log.e(TAG, "onCreate() -> mapFragment == null");
            toastMessageLong("An error occurred while loading the map.");
        }

        mapTypeButton = findViewById(R.id.mapTypeButton);
        settingsButton = findViewById(R.id.settingsButton);
        chatViewsButton = findViewById(R.id.chatViewsButton);
        dmButton = findViewById(R.id.dmButton);
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

        // Check if the user is logged in. If true, make the settings button visible.
        boolean loggedIn = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsFragment.KEY_SIGN_OUT, false);

        if (loggedIn) {

            settingsButton.setVisibility(View.VISIBLE);
            dmButton.setVisibility(View.VISIBLE);
        } else {

            settingsButton.setVisibility(View.GONE);
            dmButton.setVisibility(View.GONE);
        }

        // Shows a menu to change the map type.
        mapTypeButton.setOnClickListener(view -> {

            Log.i(TAG, "onStart() -> mapTypeButton -> onClick");

            popupMapType = new PopupMenu(Map.this, mapTypeButton);
            popupMapType.setOnMenuItemClickListener(Map.this);
            popupMapType.inflate(R.menu.maptype_menu);
            popupMapType.show();
        });

        // Go to settings.
        settingsButton.setOnClickListener(v -> {

            Log.i(TAG, "onStart() -> settingsButton -> onClick");

            // Check location permissions.
            if (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                loadingIcon.setVisibility(View.VISIBLE);

                FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(Map.this, location -> {

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
                                toastMessageLong("An error occurred: your location is null.");
                            }
                        });
            } else {

                checkLocationPermissions();
            }
        });

        // Used to set dmButton badge number.
        // If user has a Google account, get email one way. Else, get email another way.
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        String email;
        if (acct != null) {

            email = acct.getEmail();
        } else {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            email = sharedPreferences.getString("userToken", "null");
        }

        // If new DMs, update dmButton badge.
        if (email != null) {

            // Firebase does not allow ".", so replace them with ",".
            userEmailFirebase = email.replace(".", ",");

            DatabaseReference DMs = FirebaseDatabase.getInstance().getReference().child("Users").child(userEmailFirebase).child("ReceivedDMs");
            DMs.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    for (DataSnapshot ds : snapshot.getChildren()) {

                        // Used in addDMsQuery to allow the child to be called the first time if no child exists and prevent double posts if a child exists.
                        dmExists = true;

                        if (!(Boolean) ds.child("seenByUser").getValue()) {

                            dmCounter++;
                        }
                    }

                    dmButton.setCount(dmCounter);
                    addDMsQuery();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    // Don't put error toastMessage here, as it will create a "Permissions Denied" message when no user is signed in.
                }
            });
        }

        // Shows a menu to filter circle views.
        chatViewsButton.setOnClickListener(view -> {

            Log.i(TAG, "onStart() -> chatViewsButton -> onClick");

            popupChatViews = new PopupMenu(Map.this, chatViewsButton);
            popupChatViews.setOnMenuItemClickListener(Map.this);
            popupChatViews.inflate(R.menu.chatviews_menu);
            popupChatViews.show();
        });

        // Go to DMs.
        dmButton.setOnClickListener(v -> {

            Log.i(TAG, "onStart() -> dmButton -> onClick");

            // Check location permissions.
            if (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                loadingIcon.setVisibility(View.VISIBLE);

                FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(Map.this, location -> {

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
                                toastMessageLong("An error occurred: your location is null.");
                            }
                        });
            } else {

                checkLocationPermissions();
            }
        });

        // Shows a menu for creating chats.
        createChatButton.setOnClickListener(view -> {

            Log.i(TAG, "onStart() -> createChatButton -> onClick");

            popupCreateChat = new PopupMenu(Map.this, createChatButton);
            popupCreateChat.setOnMenuItemClickListener(Map.this);
            popupCreateChat.inflate(R.menu.createchat_menu);
            // Check if the circle exists and adjust the menu items accordingly.
            if (newCircle != null || newPolygon != null) {

                popupCreateChat.getMenu().findItem(R.id.createPolygon).setVisible(false);
            } else {

                popupCreateChat.getMenu().findItem(R.id.removeShape).setVisible(false);
            }

            popupCreateChat.show();
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
                                .addOnSuccessListener(Map.this, location -> {

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
                                        toastMessageLong("An error occurred: your location is null.");
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
                                    threeMarkers = false;
                                    fourMarkers = false;
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
                            toastMessageLong("An error occurred: your location is null.");
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

                    // Create arrayLists that hold shape information in a useful order.
                    ArrayList<Object> combinedListShapes = new ArrayList<>();
                    ArrayList<String> combinedListUUID = new ArrayList<>();

                    // Add the smaller array fist for consistency with the rest of the logic.
                    if (overlappingShapesCircleLocation.size() <= overlappingShapesPolygonVertices.size()) {

                        combinedListShapes.addAll(overlappingShapesPolygonVertices);
                        combinedListShapes.addAll(overlappingShapesCircleLocation);
                        combinedListUUID.addAll(overlappingShapesPolygonUUID);
                        combinedListUUID.addAll(overlappingShapesCircleUUID);
                    } else {

                        combinedListShapes.addAll(overlappingShapesCircleLocation);
                        combinedListShapes.addAll(overlappingShapesPolygonVertices);
                        combinedListUUID.addAll(overlappingShapesCircleUUID);
                        combinedListUUID.addAll(overlappingShapesPolygonUUID);
                    }

                    selectedOverlappingShapeUUID = combinedListUUID.get(chatSelectorSeekBar.getProgress());

                    if (selectedOverlappingShapeUUID != null) {

                        if (circleTemp != null) {

                            circleTemp.remove();
                        }

                        if (polygonTemp != null) {

                            polygonTemp.remove();
                        }

                        if (combinedListShapes.get(chatSelectorSeekBar.getProgress()) instanceof LatLng) {

                            if (overlappingShapesCircleLocation.size() > overlappingShapesPolygonVertices.size()) {

                                selectedOverlappingShapeCircleLocation = overlappingShapesCircleLocation.get(chatSelectorSeekBar.getProgress());
                                selectedOverlappingShapeCircleRadius = overlappingShapesCircleRadius.get(chatSelectorSeekBar.getProgress());
                            } else {

                                selectedOverlappingShapeCircleLocation = overlappingShapesCircleLocation.get(chatSelectorSeekBar.getProgress() - overlappingShapesPolygonVertices.size());
                                selectedOverlappingShapeCircleRadius = overlappingShapesCircleRadius.get(chatSelectorSeekBar.getProgress() - overlappingShapesPolygonVertices.size());
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

                            if (overlappingShapesPolygonVertices.size() >= overlappingShapesCircleLocation.size()) {

                                selectedOverlappingShapePolygonVertices = overlappingShapesPolygonVertices.get(chatSelectorSeekBar.getProgress());
                            } else {

                                selectedOverlappingShapePolygonVertices = overlappingShapesPolygonVertices.get(chatSelectorSeekBar.getProgress() - overlappingShapesCircleLocation.size());
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
                        toastMessageLong("An error occurred.");
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
    private void addDMsQuery() {

        queryDMs = FirebaseDatabase.getInstance().getReference().child("Users").child(userEmailFirebase).child("ReceivedDMs").limitToLast(1);
        childEventListenerDMs = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.i(TAG, "addDMQuery()");

                // If this is the first time calling this eventListener, prevent double posts (as onStart() already added the last item).
                if (firstLoadDMs && dmExists) {

                    firstLoadDMs = false;
                    return;
                }

                dmCounter++;
                dmButton.setCount(dmCounter);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                toastMessageLong(error.getMessage());
            }
        };

        queryDMs.addChildEventListener(childEventListenerDMs);
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        restarted = true;
        firstLoadCamera = false;
        firstLoadShapes = true;
        firstLoadDMs = true;
        waitingForShapeInformationToProcess = false;
        waitingForClicksToProcess = false;
        selectingShape = false;
        chatsSize = 0;
        dmCounter = 0;
        newCameraCoordinates = false;
        loadedCoordinates.clear();
        oldNearLeft = null;
        oldFarLeft = null;
        oldNearRight = null;
        oldFarRight = null;

        polygonPointsListForMapChange.clear();
        polygonUUIDListForMapChange.clear();
        circleCenterListForMapChange.clear();
        circleUUIDListForMapChange.clear();
        cancelToasts();

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

                buildAlertNoGPS();
            }
        } else {

            Log.e(TAG, "onResume() -> manager == null");
            toastMessageLong("An error occurred while checking if GPS is enabled.");
        }

        // Start updating location.
        if (ContextCompat.checkSelfPermission(Map.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Location seems to work without the following line, but it is set to false in onPause so for symmetry's sake, I'm setting it to true here.
            if (mMap != null) {

                mMap.setMyLocationEnabled(true);
            }

            String provider = LocationManager.NETWORK_PROVIDER;
            if (locationManager != null) {

                locationManager.requestLocationUpdates(provider, 5000, 0, this);
                startLocationUpdates();
            } else {

                Log.e(TAG, "onResume() -> locationManager == null");
                toastMessageLong("Error retrieving your location.");
            }
        } else {

            checkLocationPermissions();
        }
    }

    private void buildAlertNoGPS() {

        AlertDialog.Builder alert;
        alert = new AlertDialog.Builder(this);

        HandlerThread GPSHandlerThread = new HandlerThread("GPSHandlerThread");
        GPSHandlerThread.start();
        Handler mHandler = new Handler(GPSHandlerThread.getLooper());
        Runnable runnable = () ->

                alert.setCancelable(false)
                .setTitle("GPS Disabled")
                .setMessage("Please enable your location services on the following screen.")
                .setPositiveButton("OK", (dialog, i) -> {

                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                })
                .create()
                .show();

        mHandler.post(runnable);
    }

    @Override
    protected void onPause() {

        Log.i(TAG, "onPause()");

        // Remove updating location information.
        if (ContextCompat.checkSelfPermission(Map.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            if (mMap != null) {

                mMap.setMyLocationEnabled(false);
            }

            if (locationManager != null) {

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

        if (queryDMs != null) {

            queryDMs.removeEventListener(childEventListenerDMs);
            queryDMs = null;
        }

        if (childEventListenerDMs != null) {

            childEventListenerDMs = null;
        }

        if (queryShapesLarge != null) {

            queryShapesLarge.removeEventListener(childEventListenerShapesLarge);
            queryShapesLarge = null;
        }

        if (queryShapesMedium != null) {

            queryShapesMedium.removeEventListener(childEventListenerShapesMedium);
            queryShapesMedium = null;
        }

        if (queryShapesSmall != null) {

            queryShapesSmall.removeEventListener(childEventListenerShapesSmall);
            queryShapesSmall = null;
        }

        if (queryShapesPoints != null) {

            queryShapesPoints.removeEventListener(childEventListenerShapesPoints);
            queryShapesPoints = null;
        }

        if (childEventListenerShapesLarge != null) {

            childEventListenerShapesLarge = null;
        }

        if (childEventListenerShapesMedium != null) {

            childEventListenerShapesMedium = null;
        }

        if (childEventListenerShapesSmall != null) {

            childEventListenerShapesSmall = null;
        }

        if (childEventListenerShapesPoints != null) {

            childEventListenerShapesPoints = null;
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
            mMap.setOnCameraIdleListener(null);
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

            locationPermissionAlertAsync();
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

                locationPermissionAlertAsync();
            } else if (grantResults.length > 0
                    && grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                // User denied permission and checked "Don't ask again!"
                toastMessageLong("Location permission is required. Please enable it manually through the Android settings menu.");
            }
        }
    }

    private void locationPermissionAlertAsync() {

        AlertDialog.Builder alert;
        alert = new AlertDialog.Builder(this);

        HandlerThread locationPermissionHandlerThread = new HandlerThread("locationPermissionHandlerThread");
        locationPermissionHandlerThread.start();
        Handler mHandler = new Handler(locationPermissionHandlerThread.getLooper());
        Runnable runnable = () ->

                alert.setCancelable(false)
                .setTitle("Device Location Required")
                .setMessage("Here Before needs permission to use your location to find chat areas around you.")
                .setPositiveButton("OK", (dialogInterface, i) -> {

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            Request_User_Location_Code);
                })
                .create()
                .show();

        mHandler.post(runnable);
    }

    protected void startLocationUpdates() {

        Log.i(TAG, "startLocationUpdates()");

        // Create the location request to start receiving updates
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /* 1000 = 1 sec */
        long UPDATE_INTERVAL = 0;
        mLocationRequest.setInterval(UPDATE_INTERVAL);

        /* 1000 = 1 sec */
        long FASTEST_INTERVAL = 0;
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
    public void onLocationChanged(@NonNull final Location location) {

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
                if (firstLoadCamera && !cameraMoved && location.getAccuracy() < 60) {

                    Log.i(TAG, "updateLocation() -> Good accuracy");

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to user's location
                            .zoom(19)                   // Sets the zoom
                            .bearing(0)                // Sets the orientation of the camera
                            .tilt(0)                   // Sets the tilt of the camera
                            .build();                   // Creates a CameraPosition from the builder

                    // Move the camera to the user's location once the map is available.
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                    // Adjust boolean values to prevent this logic from being called again.
                    firstLoadCamera = false;
                    cameraMoved = true;
                } else if (firstLoadCamera && !badAccuracy && location.getAccuracy() >= 60) {

                    Log.i(TAG, "updateLocation() -> Bad accuracy");

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to user's location
                            .zoom(19)                   // Sets the zoom
                            .bearing(0)                // Sets the orientation of the camera
                            .tilt(0)                   // Sets the tilt of the camera
                            .build();                   // Creates a CameraPosition from the builder

                    // Move the camera to the user's location once the map is available.
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                    // Adjust boolean values to prevent this logic from being called again.
                    badAccuracy = true;
                    waitingForBetterLocationAccuracy = true;
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
    public void onProviderEnabled(@NonNull String provider) {

        Log.i(TAG, "onProviderEnabled()");
        locationProviderDisabled = false;
        toastMessageLong("Your location provider is enabled.");
        loadingIcon.setVisibility(View.GONE);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

        Log.i(TAG, "onProviderDisabled()");
        locationProviderDisabled = true;
        toastMessageLong("Your location provider is disabled.");
        loadingIcon.setVisibility(View.GONE);
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

        if (!restarted) {

            updatePreferences();
        }

        if (restarted) {

            cameraIdle();
        }

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
        mMap.setOnMarkerClickListener(marker -> {

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

                        toastMessageLong("Please make the shape larger.");
                        waitingForClicksToProcess = false;
                        waitingForShapeInformationToProcess = false;
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
                                .addOnSuccessListener(Map.this, location -> {

                                    if (location != null) {

                                        float[] distance = new float[2];

                                        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                                newCircle.getCenter().latitude, newCircle.getCenter().longitude, distance);

                                        // Boolean; will be true if user is within the circle upon circle click.
                                        userIsWithinShape = !(distance[0] > newCircle.getRadius());

                                        if (!userIsWithinShape) {

                                            toastMessageLong("You must be inside the shape to create a new shape.");
                                            waitingForClicksToProcess = false;
                                            waitingForShapeInformationToProcess = false;
                                            return;
                                        }

                                        // Check if the user is already signed in.
                                        if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) != null) {

                                            // User signed in.

                                            Log.i(TAG, "onMapReadyAndRestart() -> onMarkerClick -> user signed in -> circle -> marker0");

                                            // uuid does not already exist in Firebase. Go to Chat.java with the uuid.

                                            // Carry the extras all the way to Chat.java.
                                            Intent Activity = new Intent(Map.this, Navigation.class);
                                            goToNextActivityCircle(Activity, newCircle, true);
                                        } else {

                                            // User NOT signed in.

                                            Log.i(TAG, "onMapReadyAndRestart() -> onMarkerClick -> user NOT signed in -> circle -> marker0");

                                            // Carry the extras all the way to Chat.java.
                                            Intent Activity = new Intent(Map.this, SignIn.class);
                                            goToNextActivityCircle(Activity, newCircle, true);
                                        }
                                    } else {

                                        Log.e(TAG, "onMapReadyAndRestart() -> onMarkerClick -> location == null");
                                        mMap.getUiSettings().setScrollGesturesEnabled(true);
                                        loadingIcon.setVisibility(View.GONE);
                                        toastMessageLong("An error occurred: your location is null.");
                                    }
                                });
                    } else {

                        checkLocationPermissions();
                    }
                }
            }

            return true;
        });

        // Go to Chat.java when clicking on a polygon.
        mMap.setOnPolygonClickListener(polygon -> {

            // If the user tries to click on a polygon that is not a polygonTemp while polygonTemp exists, return.
            if (chatSelectorSeekBar.getVisibility() == View.VISIBLE && (polygon.getTag() != selectedOverlappingShapeUUID)) {

                Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> Selected polygon is not a polygonTemp. Returning");

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
            if (polygon.getTag() == null) {

                Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> User clicked on a new polygon");

                // End this method if the method is already being processed from another shape clicking event.
                if (waitingForShapeInformationToProcess) {

                    return;
                }

                // Update boolean to prevent double clicking a shape.
                waitingForShapeInformationToProcess = true;

                // Inform the user is the circle is too small.
                if (SphericalUtil.computeArea(polygon.getPoints()) < Math.PI) {

                    toastMessageLong("Please make the shape larger.");
                    waitingForClicksToProcess = false;
                    waitingForShapeInformationToProcess = false;
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
                            .addOnSuccessListener(Map.this, location -> {

                                if (location != null) {

                                    // Boolean; will be true if user is within the shape upon shape click.
                                    userIsWithinShape = PolyUtil.containsLocation(location.getLatitude(), location.getLongitude(), polygon.getPoints(), false);

                                    if (!userIsWithinShape) {

                                        toastMessageLong("You must be inside the shape to create a new shape.");
                                        waitingForClicksToProcess = false;
                                        waitingForShapeInformationToProcess = false;
                                        return;
                                    }

                                    // Check if the user is already signed in.
                                    if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) != null) {

                                        // User signed in.

                                        Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> New polygon -> User signed in");

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, Navigation.class);
                                        goToNextActivityPolygon(Activity, polygon, true);
                                    } else {

                                        // User NOT signed in.

                                        Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> New polygon -> User NOT signed in");

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        goToNextActivityPolygon(Activity, polygon, true);
                                    }
                                } else {

                                    Log.e(TAG, "onMapReadyAndRestart() -> onPolygonClick -> polygon.getTag() == null -> location == null");
                                    mMap.getUiSettings().setScrollGesturesEnabled(true);
                                    loadingIcon.setVisibility(View.GONE);
                                    toastMessageLong("An error occurred: your location is null.");
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

                loadingIcon.setVisibility(View.VISIBLE);

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
                        .addOnSuccessListener(Map.this, location -> {

                            if (location != null) {

                                // Boolean; will be true if user is within the shape upon shape click.
                                userIsWithinShape = PolyUtil.containsLocation(location.getLatitude(), location.getLongitude(), polygon.getPoints(), false);

                                // Check if the user is already signed in.
                                if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                    Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> User selected a polygon -> User signed in");

                                    // User is signed in.

                                    Intent Activity = new Intent(Map.this, Navigation.class);
                                    goToNextActivityPolygon(Activity, polygon, false);
                                } else {

                                    Log.i(TAG, "onMapReadyAndRestart() -> onPolygonClick -> User selected a polygon -> No user signed in");

                                    // No user is signed in.

                                    Intent Activity = new Intent(Map.this, SignIn.class);
                                    goToNextActivityPolygon(Activity, polygon, false);
                                }
                            } else {

                                Log.e(TAG, "onMapReadyAndRestart() -> onPolygonClick -> User selected a polygon -> location == null");
                                mMap.getUiSettings().setScrollGesturesEnabled(true);
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong("An error occurred: your location is null.");
                            }
                        });

                return;
            }

            selectingShape = true;

            // Create arrayLists that hold shape information in a useful order.
            ArrayList<String> combinedListUUID = new ArrayList<>();

            // Add the smaller array fist for consistency with the rest of the logic.
            if (overlappingShapesCircleLocation.size() <= overlappingShapesPolygonVertices.size()) {

                combinedListUUID.addAll(overlappingShapesPolygonUUID);
                combinedListUUID.addAll(overlappingShapesCircleUUID);
            } else {

                combinedListUUID.addAll(overlappingShapesCircleUUID);
                combinedListUUID.addAll(overlappingShapesPolygonUUID);
            }

            selectedOverlappingShapeUUID = combinedListUUID.get(chatSelectorSeekBar.getProgress());

            if (selectedOverlappingShapeUUID != null) {

                if (circleTemp != null) {

                    circleTemp.remove();
                }

                if (polygonTemp != null) {

                    polygonTemp.remove();
                }

                if (overlappingShapesPolygonVertices.size() < overlappingShapesCircleLocation.size() && overlappingShapesPolygonVertices.size() > 0) {

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
                toastMessageLong("An error occurred.");
            }

            // At this point, chatsSize > 1 so set the chatSelectorSeekBar to VISIBLE.
            chatSelectorSeekBar.setMax(chatsSize - 1);
            chatSelectorSeekBar.setProgress(0);
            chatSizeSeekBar.setVisibility(View.GONE);
            chatSelectorSeekBar.setVisibility(View.VISIBLE);
            mMap.getUiSettings().setScrollGesturesEnabled(true);
            loadingIcon.setVisibility(View.GONE);

            longToast = Toast.makeText(getBaseContext(), "Select a shape using the SeekBar below", Toast.LENGTH_LONG);
            longToast.setGravity(Gravity.BOTTOM, 0, 250);
            longToast.show();
        });

        // Go to Chat.java when clicking on a circle.
        mMap.setOnCircleClickListener(circle -> {

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

                    toastMessageLong("Please make the shape larger.");
                    waitingForClicksToProcess = false;
                    waitingForShapeInformationToProcess = false;
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
                            .addOnSuccessListener(Map.this, location -> {

                                if (location != null) {

                                    float[] distance = new float[2];

                                    Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                            circle.getCenter().latitude, circle.getCenter().longitude, distance);

                                    // Boolean; will be true if user is within the circle upon circle click.
                                    userIsWithinShape = !(distance[0] > circle.getRadius());

                                    if (!userIsWithinShape) {

                                        toastMessageLong("You must be inside the shape to create a new shape.");
                                        waitingForClicksToProcess = false;
                                        waitingForShapeInformationToProcess = false;
                                        return;
                                    }

                                    // Check if the user is already signed in.
                                    if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) != null) {

                                        // User signed in.

                                        Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> New circle -> User signed in");

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, Navigation.class);
                                        goToNextActivityCircle(Activity, circle, true);
                                    } else {

                                        // User NOT signed in.

                                        Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> New circle -> User NOT signed in");

                                        // Carry the extras all the way to Chat.java.
                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        goToNextActivityCircle(Activity, circle, true);
                                    }
                                } else {

                                    Log.e(TAG, "onMapReadyAndRestart() -> onCircleClick -> circle.getTag() == null -> location == null");
                                    mMap.getUiSettings().setScrollGesturesEnabled(true);
                                    loadingIcon.setVisibility(View.GONE);
                                    toastMessageLong("An error occurred: your location is null.");
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

                loadingIcon.setVisibility(View.VISIBLE);

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
                        .addOnSuccessListener(Map.this, location -> {

                            if (location != null) {

                                float[] distance = new float[2];

                                Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                        circle.getCenter().latitude, circle.getCenter().longitude, distance);

                                // Boolean; will be true if user is within the circle upon circle click.
                                userIsWithinShape = !(distance[0] > circle.getRadius());

                                // Check if the user is already signed in.
                                if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                    // User signed in.

                                    Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> User selected a circle -> User signed in");

                                    Intent Activity = new Intent(Map.this, Navigation.class);
                                    goToNextActivityCircle(Activity, circle, false);
                                } else {

                                    // User NOT signed in.

                                    Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> User selected a circle -> User NOT signed in");

                                    Intent Activity = new Intent(Map.this, SignIn.class);
                                    goToNextActivityCircle(Activity, circle, false);
                                }
                            } else {

                                Log.e(TAG, "onMapReadyAndRestart() -> onCircleClick -> User selected a circle -> location == null");
                                mMap.getUiSettings().setScrollGesturesEnabled(true);
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong("An error occurred: your location is null.");
                            }
                        });

                return;
            }

            selectingShape = true;

            // Create arrayLists that hold shape information in a useful order.
            ArrayList<String> combinedListUUID = new ArrayList<>();

            // Add the smaller array fist for consistency with the rest of the logic.
            if (overlappingShapesCircleLocation.size() <= overlappingShapesPolygonVertices.size()) {

                combinedListUUID.addAll(overlappingShapesPolygonUUID);
                combinedListUUID.addAll(overlappingShapesCircleUUID);
            } else {

                combinedListUUID.addAll(overlappingShapesCircleUUID);
                combinedListUUID.addAll(overlappingShapesPolygonUUID);
            }

            selectedOverlappingShapeUUID = combinedListUUID.get(chatSelectorSeekBar.getProgress());

            if (selectedOverlappingShapeUUID != null) {

                if (circleTemp != null) {

                    circleTemp.remove();
                }

                if (polygonTemp != null) {

                    polygonTemp.remove();
                }

                if (overlappingShapesCircleLocation.size() < overlappingShapesPolygonVertices.size() && overlappingShapesCircleLocation.size() > 0) {

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
                toastMessageLong("An error occurred.");
            }

            // At this point, chatsSize > 1 so set the chatSelectorSeekBar to VISIBLE.
            chatSelectorSeekBar.setMax(chatsSize - 1);
            chatSelectorSeekBar.setProgress(0);
            chatSizeSeekBar.setVisibility(View.GONE);
            chatSelectorSeekBar.setVisibility(View.VISIBLE);
            mMap.getUiSettings().setScrollGesturesEnabled(true);
            loadingIcon.setVisibility(View.GONE);

            longToast = Toast.makeText(getBaseContext(), "Select a shape using the SeekBar below", Toast.LENGTH_LONG);
            longToast.setGravity(Gravity.BOTTOM, 0, 250);
            longToast.show();
        });

        mMap.setOnMapClickListener(latLng -> {

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

                // !mapChanged prevents duplicate shapes when a user is selecting a shape and then changes map types.
                if (!mapChanged) {

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
                            .addOnSuccessListener(Map.this, location -> {

                                if (location != null) {

                                    float[] distance = new float[2];

                                    Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                            mCircle.getCenter().latitude, mCircle.getCenter().longitude, distance);

                                    // Boolean; will be true if user is within the circle upon circle click.
                                    userIsWithinShape = !(distance[0] > mCircle.getRadius());

                                    // Check if the user is already signed in.
                                    if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                        // User signed in.

                                        Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a circle -> User signed in");

                                        Intent Activity = new Intent(Map.this, Navigation.class);
                                        goToNextActivityCircle(Activity, mCircle, false);
                                    } else {

                                        // User NOT signed in.

                                        Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a circle -> User NOT signed in");

                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        goToNextActivityCircle(Activity, mCircle, false);
                                    }
                                } else {

                                    Log.e(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a circle -> location == null");
                                    mMap.getUiSettings().setScrollGesturesEnabled(true);
                                    loadingIcon.setVisibility(View.GONE);
                                    toastMessageLong("An error occurred: your location is null.");
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
                            .addOnSuccessListener(Map.this, location -> {

                                if (location != null) {

                                    // Boolean; will be true if user is within the shape upon shape click.
                                    userIsWithinShape = PolyUtil.containsLocation(location.getLatitude(), location.getLongitude(), mPolygon.getPoints(), false);

                                    // Check if the user is already signed in.
                                    if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) instanceof GoogleSignInAccount) {

                                        Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a polygon -> User signed in");

                                        // User is signed in.

                                        Intent Activity = new Intent(Map.this, Navigation.class);
                                        goToNextActivityPolygon(Activity, mPolygon, false);
                                    } else {

                                        Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a polygon -> No user signed in");

                                        // No user is signed in.

                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        goToNextActivityPolygon(Activity, mPolygon, false);
                                    }
                                } else {

                                    Log.e(TAG, "onMapReadyAndRestart() -> onMapClick -> User selected a polygon -> location == null");
                                    mMap.getUiSettings().setScrollGesturesEnabled(true);
                                    loadingIcon.setVisibility(View.GONE);
                                    toastMessageLong("An error occurred: your location is null.");
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
        });

        // Updates the boolean value for onLocationChanged() to prevent updating the camera position if the user has already changed it manually.
        mMap.setOnCameraMoveListener(() -> {

            if (waitingForBetterLocationAccuracy && !cameraMoved) {

                Log.i(TAG, "onMapReadyAndRestart() -> onCameraMove");

                cameraMoved = true;
            }
        });

        // Once camera stops moving, load the shapes in that region.
        mMap.setOnCameraIdleListener(this::cameraIdle);
    }

    protected void updatePreferences() {

        Log.i(TAG, "updatePreferences()");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String preferredMapType = sharedPreferences.getString(SettingsFragment.KEY_MAP_TYPE, getResources().getString(R.string.hybrid_view));

        switch (preferredMapType) {

            case "Road map view":

                Log.i(TAG, "updatePreferences() -> Road map view");

                // Use the "road map" map type if the map is not null.
                if (mMap != null) {

                    Log.i(TAG, "updatePreferences() -> Road Map");

                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                    adjustMapColors();
                } else {

                    Log.e(TAG, "updatePreferences() -> Road Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map.");
                }

                break;

            case "Satellite view":

                Log.i(TAG, "updatePreferences() -> Satellite view");

                // Use the "satellite" map type if the map is not null.
                if (mMap != null) {

                    Log.i(TAG, "updatePreferences() -> Satellite Map");

                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                    adjustMapColors();
                } else {

                    Log.e(TAG, "updatePreferences() -> Satellite Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map.");
                }

                break;

            case "Hybrid view":

                Log.i(TAG, "updatePreferences() -> Hybrid view");

                // Use the "hybrid" map type if the map is not null.
                if (mMap != null) {

                    Log.i(TAG, "updatePreferences() -> Hybrid Map");

                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                    adjustMapColors();
                } else {

                    Log.e(TAG, "updatePreferences() -> Hybrid Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map.");
                }

                break;

            case "Terrain view":

                Log.i(TAG, "updatePreferences() -> Terrain view");

                // Use the "terrain" map type if the map is not null.
                if (mMap != null) {

                    Log.i(TAG, "updatePreferences() -> Terrain Map");

                    mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

                    adjustMapColors();
                } else {

                    Log.e(TAG, "updatePreferences() -> Terrain Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map.");
                }

                break;

            default:

                Log.i(TAG, "updatePreferences() -> default (something went wrong, error probably on next line)");
                Log.i(TAG, "updatePreferences(): preferredMapType: " + preferredMapType);

                // Use the "hybrid" map type if the map is not null.
                if (mMap != null) {

                    Log.i(TAG, "updatePreferences() -> Hybrid Map, default");

                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                    adjustMapColors();
                } else {

                    Log.e(TAG, "updatePreferences() -> default -> mMap == null");
                    toastMessageLong("An error occurred while loading the map.");
                }

                break;
        }
    }

    // Used in the onCameraIdle listener and after restart (as the listener is not called after restart).
    private void cameraIdle() {

        Log.i(TAG, "cameraIdle()");

        // If the camera is zoomed too far out, don't load any shapes, as too many loaded shapes will affect performance (and user wouldn't be able to see the shapes anyway).
        if (mMap.getCameraPosition().zoom < 12) {

            return;
        }

        newCameraCoordinates = true;

        Projection projection = mMap.getProjection();
        VisibleRegion visibleRegion = projection.getVisibleRegion();
        LatLng nearLeft = visibleRegion.nearLeft;
        LatLng farLeft = visibleRegion.farLeft;
        LatLng nearRight = visibleRegion.nearRight;
        LatLng farRight = visibleRegion.farRight;

        // Get a value with 1 decimal point and use it for Firebase.
        double nearLeftPrecisionLat = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double nearLeftLatTemp = (int) (nearLeftPrecisionLat * nearLeft.latitude) / nearLeftPrecisionLat;
        nearLeftLatTemp *= 10;
        newNearLeftLat = (int) nearLeftLatTemp;

        double nearLeftPrecisionLon = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double nearLeftLonTemp = (int) (nearLeftPrecisionLon * nearLeft.longitude) / nearLeftPrecisionLon;
        nearLeftLonTemp *= 10;
        newNearLeftLon = (int) nearLeftLonTemp;

        // Get a value with 1 decimal point and use it for Firebase.
        double farLeftPrecisionLat = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double farLeftLatTemp = (int) (farLeftPrecisionLat * farLeft.latitude) / farLeftPrecisionLat;
        farLeftLatTemp *= 10;
        int newFarLeftLat = (int) farLeftLatTemp;

        double farLeftPrecisionLon = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double farLeftLonTemp = (int) (farLeftPrecisionLon * farLeft.longitude) / farLeftPrecisionLon;
        farLeftLonTemp *= 10;
        int newFarLeftLon = (int) farLeftLonTemp;

        // Get a value with 1 decimal point and use it for Firebase.
        double nearRightPrecisionLat = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double nearRightLatTemp = (int) (nearRightPrecisionLat * nearRight.latitude) / nearRightPrecisionLat;
        nearRightLatTemp *= 10;
        int newNearRightLat = (int) nearRightLatTemp;

        double nearRightPrecisionLon = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double nearRightLonTemp = (int) (nearRightPrecisionLon * nearRight.longitude) / nearRightPrecisionLon;
        nearRightLonTemp *= 10;
        int newNearRightLon = (int) nearRightLonTemp;

        // Get a value with 1 decimal point and use it for Firebase.
        double farRightPrecisionLat = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double farRightLatTemp = (int) (farRightPrecisionLat * farRight.latitude) / farRightPrecisionLat;
        farRightLatTemp *= 10;
        int newFarRightLat = (int) farRightLatTemp;

        double farRightPrecisionLon = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double farRightLonTemp = (int) (farRightPrecisionLon * farRight.longitude) / farRightPrecisionLon;
        farRightLonTemp *= 10;
        int newFarRightLon = (int) farRightLonTemp;

        // Do not load the map if the user can see more than 7 loadable areas (e.g. they are on a tablet with a big screen).
        if (Math.abs((newNearRightLat - newFarLeftLat) + (newNearRightLon - newFarLeftLon)) >= 4 ||
                Math.abs((newFarRightLat - newNearLeftLat) - (newFarRightLon - newNearLeftLon)) >= 4) {

            return;
        }

        newNearLeft = new Pair<>(newNearLeftLat, newNearLeftLon);
        newFarLeft = new Pair<>(newFarLeftLat, newFarLeftLon);
        newNearRight = new Pair<>(newNearRightLat, newNearRightLon);
        newFarRight = new Pair<>(newFarRightLat, newFarRightLon);

        // No need to continue if none of the new coordinates need to be loaded.
        if (loadedCoordinates.contains(newNearLeft) && loadedCoordinates.contains(newFarLeft) && loadedCoordinates.contains(newNearRight) && loadedCoordinates.contains(newFarRight)) {

            return;
        }

        // If the camera view has not entered a new section of the map, there's no need to load new shapes. Need to account for a 90 degree turn, so check all values against all old values.
        if ((newNearLeft.equals(oldNearLeft) || newNearLeft.equals(oldFarLeft) || newNearLeft.equals(oldNearRight) || newNearLeft.equals(oldFarRight)) && (newFarLeft.equals(oldNearLeft) || newFarLeft.equals(oldFarLeft) || newFarLeft.equals(oldNearRight) || newFarLeft.equals(oldFarRight))
                && (newNearRight.equals(oldNearLeft) || newNearRight.equals(oldFarLeft) || newNearRight.equals(oldNearRight) || newNearRight.equals(oldFarRight)) && (newFarRight.equals(oldNearLeft) || newFarRight.equals(oldFarLeft) || newFarRight.equals(oldNearRight) || newFarRight.equals(oldFarRight))) {

            return;
        }

        loadShapes();
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

    private void goToNextActivityCircle(final Intent Activity, final Circle circle, final Boolean newShape) {

        Log.i(TAG, "goToNextActivityCircle()");

        // Check location permissions.
        if (ContextCompat.checkSelfPermission(getBaseContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(Map.this, location -> {

                        if (location != null) {

                            if (!locationProviderDisabled) {

                                cancelToasts();

                                // Pass this boolean value to Chat.java.
                                Activity.putExtra("newShape", newShape);

                                // Get a value with 1 decimal point and use it for Firebase.
                                double nearLeftPrecisionLat = Math.pow(10, 1);
                                // Can't create a firebase path with '.', so get rid of decimal.
                                double nearLeftLatTemp = (int) (nearLeftPrecisionLat * circle.getCenter().latitude) / nearLeftPrecisionLat;
                                nearLeftLatTemp *= 10;
                                int shapeLat = (int) nearLeftLatTemp;

                                double nearLeftPrecisionLon = Math.pow(10, 1);
                                // Can't create a firebase path with '.', so get rid of decimal.
                                double nearLeftLonTemp = (int) (nearLeftPrecisionLon * circle.getCenter().longitude) / nearLeftPrecisionLon;
                                nearLeftLonTemp *= 10;
                                int shapeLon = (int) nearLeftLonTemp;

                                Activity.putExtra("shapeLat", shapeLat);
                                Activity.putExtra("shapeLon", shapeLon);
                                // UserLatitude and userLongitude are used in DirectMentions.
                                Activity.putExtra("userLatitude", location.getLatitude());
                                Activity.putExtra("userLongitude", location.getLongitude());
                                // Pass this value to Chat.java to identify the shape.
                                Activity.putExtra("shapeUUID", shapeUUID);
                                // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                Activity.putExtra("circleLatitude", circle.getCenter().latitude);
                                Activity.putExtra("circleLongitude", circle.getCenter().longitude);
                                Activity.putExtra("radius", circle.getRadius());

                                clearMap();

                                loadingIcon.setVisibility(View.GONE);

                                startActivity(Activity);
                            } else {

                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong("Enable the location provider and try again.");
                            }
                        } else {

                            loadingIcon.setVisibility(View.GONE);
                            Log.e(TAG, "goToNextActivityCircle() -> location == null");
                            toastMessageLong("An error occurred: your location is null.");
                        }
                    });
        } else {

            checkLocationPermissions();
        }
    }

    private void goToNextActivityPolygon(final Intent Activity, final Polygon polygon, final Boolean newShape) {

        // Check location permissions.
        if (ContextCompat.checkSelfPermission(getBaseContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(Map.this, location -> {

                        if (location != null) {

                            if (!locationProviderDisabled) {

                                cancelToasts();

                                // Pass this boolean value Chat.java.
                                Activity.putExtra("newShape", newShape);

                                // Get a value with 1 decimal point and use it for Firebase.
                                double nearLeftPrecisionLat = Math.pow(10, 1);
                                // Can't create a firebase path with '.', so get rid of decimal.
                                double nearLeftLatTemp = (int) (nearLeftPrecisionLat * polygon.getPoints().get(0).latitude) / nearLeftPrecisionLat;
                                nearLeftLatTemp *= 10;
                                int shapeLat = (int) nearLeftLatTemp;

                                double nearLeftPrecisionLon = Math.pow(10, 1);
                                // Can't create a firebase path with '.', so get rid of decimal.
                                double nearLeftLonTemp = (int) (nearLeftPrecisionLon * polygon.getPoints().get(0).longitude) / nearLeftPrecisionLon;
                                nearLeftLonTemp *= 10;
                                int shapeLon = (int) nearLeftLonTemp;

                                Activity.putExtra("shapeLat", shapeLat);
                                Activity.putExtra("shapeLon", shapeLon);
                                // UserLatitude and userLongitude are used in DirectMentions.
                                Activity.putExtra("userLatitude", location.getLatitude());
                                Activity.putExtra("userLongitude", location.getLongitude());
                                // Pass this value to Chat.java to identify the shape.
                                Activity.putExtra("shapeUUID", shapeUUID);
                                // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                // Calculate the area of the polygon and send it to Firebase - used for chatViewsMenu.
                                Activity.putExtra("polygonArea", SphericalUtil.computeArea(polygon.getPoints()));
                                Activity.putExtra("threeMarkers", threeMarkers);
                                Activity.putExtra("fourMarkers", fourMarkers);
                                Activity.putExtra("fiveMarkers", fiveMarkers);
                                Activity.putExtra("sixMarkers", sixMarkers);
                                Activity.putExtra("sevenMarkers", sevenMarkers);
                                Activity.putExtra("eightMarkers", eightMarkers);
                                if (marker0Position != null) {
                                    Activity.putExtra("marker0Latitude", marker0Position.latitude);
                                    Activity.putExtra("marker0Longitude", marker0Position.longitude);
                                }
                                if (marker1Position != null) {
                                    Activity.putExtra("marker1Latitude", marker1Position.latitude);
                                    Activity.putExtra("marker1Longitude", marker1Position.longitude);
                                }
                                if (marker2Position != null) {
                                    Activity.putExtra("marker2Latitude", marker2Position.latitude);
                                    Activity.putExtra("marker2Longitude", marker2Position.longitude);
                                }
                                if (marker3Position != null) {
                                    Activity.putExtra("marker3Latitude", marker3Position.latitude);
                                    Activity.putExtra("marker3Longitude", marker3Position.longitude);
                                }
                                if (marker4Position != null) {
                                    Activity.putExtra("marker4Latitude", marker4Position.latitude);
                                    Activity.putExtra("marker4Longitude", marker4Position.longitude);
                                }
                                if (marker5Position != null) {
                                    Activity.putExtra("marker5Latitude", marker5Position.latitude);
                                    Activity.putExtra("marker5Longitude", marker5Position.longitude);
                                }
                                if (marker6Position != null) {
                                    Activity.putExtra("marker6Latitude", marker6Position.latitude);
                                    Activity.putExtra("marker6Longitude", marker6Position.longitude);
                                }
                                if (marker7Position != null) {
                                    Activity.putExtra("marker7Latitude", marker7Position.latitude);
                                    Activity.putExtra("marker7Longitude", marker7Position.longitude);
                                }

                                clearMap();

                                loadingIcon.setVisibility(View.GONE);

                                startActivity(Activity);
                            } else {

                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong("Enable the location provider and try again.");
                            }
                        } else {

                            loadingIcon.setVisibility(View.GONE);
                            Log.e(TAG, "goToNextActivityPolygon() -> location == null");
                            toastMessageLong("An error occurred: your location is null.");
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

                            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                            mapChanged = true;

                            adjustMapColors();
                        }
                    }
                } else {

                    Log.e(TAG, "onMenuItemClick -> Road Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map.");
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

                            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                            mapChanged = true;

                            adjustMapColors();
                        }
                    }
                } else {

                    Log.e(TAG, "onMenuItemClick -> Satellite Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map.");
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

                            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                            mapChanged = true;

                            adjustMapColors();
                        }
                    }
                } else {

                    Log.e(TAG, "onMenuItemClick -> Hybrid Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map.");
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

                            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

                            mapChanged = true;

                            adjustMapColors();
                        }
                    }
                } else {

                    Log.e(TAG, "onMenuItemClick -> Terrain Map -> mMap == null");
                    toastMessageLong("An error occurred while loading the map.");
                }

                break;

            // chatviews_menu
            case R.id.showEverything:

                Log.i(TAG, "onMenuItemClick() -> showEverything");

                // Tells loadShapes which shapes to load.
                showEverything = true;
                showLarge = false;
                showMedium = false;
                showSmall = false;
                showPoints = false;

                // Prevent multiple listeners.
                if (queryShapesLarge != null && childEventListenerShapesLarge != null) {
                    queryShapesLarge.removeEventListener(childEventListenerShapesLarge);
                }
                if (queryShapesMedium != null && childEventListenerShapesMedium != null) {
                    queryShapesMedium.removeEventListener(childEventListenerShapesMedium);
                }
                if (queryShapesSmall != null && childEventListenerShapesSmall != null) {
                    queryShapesSmall.removeEventListener(childEventListenerShapesSmall);
                }
                if (queryShapesPoints != null && childEventListenerShapesPoints != null) {
                    queryShapesPoints.removeEventListener(childEventListenerShapesPoints);
                }

                // Prevents doing a bunch of unnecessary work.
                if (!showingEverything) {

                    loadShapes();
                }

                break;

            // chatviews_menu
            case R.id.showLargeChats:

                Log.i(TAG, "onMenuItemClick() -> showLargeChats");

                // Tells loadShapes which shapes to load.
                showLarge = true;
                showEverything = false;
                showMedium = false;
                showSmall = false;
                showPoints = false;

                // Prevent multiple listeners.
                if (queryShapesLarge != null && childEventListenerShapesLarge != null) {
                    queryShapesLarge.removeEventListener(childEventListenerShapesLarge);
                }
                if (queryShapesMedium != null && childEventListenerShapesMedium != null) {
                    queryShapesMedium.removeEventListener(childEventListenerShapesMedium);
                }
                if (queryShapesSmall != null && childEventListenerShapesSmall != null) {
                    queryShapesSmall.removeEventListener(childEventListenerShapesSmall);
                }
                if (queryShapesPoints != null && childEventListenerShapesPoints != null) {
                    queryShapesPoints.removeEventListener(childEventListenerShapesPoints);
                }

                // Prevents doing a bunch of unnecessary work.
                if (!showingLarge) {

                    loadShapes();
                }

                break;

            // chatviews_menu
            case R.id.showMediumChats:

                Log.i(TAG, "onMenuItemClick() -> showMediumChats");

                // Tells loadShapes which shapes to load.
                showMedium = true;
                showEverything = false;
                showLarge = false;
                showSmall = false;
                showPoints = false;

                // Prevent multiple listeners.
                if (queryShapesLarge != null && childEventListenerShapesLarge != null) {
                    queryShapesLarge.removeEventListener(childEventListenerShapesLarge);
                }
                if (queryShapesMedium != null && childEventListenerShapesMedium != null) {
                    queryShapesMedium.removeEventListener(childEventListenerShapesMedium);
                }
                if (queryShapesSmall != null && childEventListenerShapesSmall != null) {
                    queryShapesSmall.removeEventListener(childEventListenerShapesSmall);
                }
                if (queryShapesPoints != null && childEventListenerShapesPoints != null) {
                    queryShapesPoints.removeEventListener(childEventListenerShapesPoints);
                }

                // Prevents doing a bunch of unnecessary work.
                if (!showingMedium) {

                    loadShapes();
                }

                break;

            // chatviews_menu
            case R.id.showSmallChats:

                Log.i(TAG, "onMenuItemClick() -> showSmallChats");

                // Tells loadShapes which shapes to load.
                showSmall = true;
                showEverything = false;
                showLarge = false;
                showMedium = false;
                showPoints = false;

                // Prevent multiple listeners.
                if (queryShapesLarge != null && childEventListenerShapesLarge != null) {
                    queryShapesLarge.removeEventListener(childEventListenerShapesLarge);
                }
                if (queryShapesMedium != null && childEventListenerShapesMedium != null) {
                    queryShapesMedium.removeEventListener(childEventListenerShapesMedium);
                }
                if (queryShapesSmall != null && childEventListenerShapesSmall != null) {
                    queryShapesSmall.removeEventListener(childEventListenerShapesSmall);
                }
                if (queryShapesPoints != null && childEventListenerShapesPoints != null) {
                    queryShapesPoints.removeEventListener(childEventListenerShapesPoints);
                }

                // Prevents doing a bunch of unnecessary work.
                if (!showingSmall) {

                    loadShapes();
                }

                break;

            // chatviews_menu
            case R.id.showPoints:

                Log.i(TAG, "onMenuItemClick() -> showPoints");

                // Tells loadShapes which shapes to load.
                showPoints = true;
                showEverything = false;
                showLarge = false;
                showMedium = false;
                showSmall = false;

                // Prevent multiple listeners.
                if (queryShapesLarge != null && childEventListenerShapesLarge != null) {
                    queryShapesLarge.removeEventListener(childEventListenerShapesLarge);
                }
                if (queryShapesMedium != null && childEventListenerShapesMedium != null) {
                    queryShapesMedium.removeEventListener(childEventListenerShapesMedium);
                }
                if (queryShapesSmall != null && childEventListenerShapesSmall != null) {
                    queryShapesSmall.removeEventListener(childEventListenerShapesSmall);
                }
                if (queryShapesPoints != null && childEventListenerShapesPoints != null) {
                    queryShapesPoints.removeEventListener(childEventListenerShapesPoints);
                }

                // Prevents doing a bunch of unnecessary work.
                if (!showingPoints) {

                    loadShapes();
                }

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
                            .addOnSuccessListener(Map.this, location -> {

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
                                    toastMessageLong("An error occurred: your location is null.");
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

                    loadingIcon.setVisibility(View.VISIBLE);

                    // Create a point and to to Chat.java or SignIn.java.
                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, location -> {

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

                                        // User signed in.

                                        Log.i(TAG, "onMenuItemClick() -> create point and enter chat -> user signed in");

                                        cancelToasts();

                                        Intent Activity = new Intent(Map.this, Navigation.class);
                                        // Pass this boolean value to Chat.java.
                                        Activity.putExtra("newShape", true);

                                        // Get a value with 1 decimal point and use it for Firebase.
                                        double nearLeftPrecisionLat = Math.pow(10, 1);
                                        // Can't create a firebase path with '.', so get rid of decimal.
                                        double nearLeftLatTemp = (int) (nearLeftPrecisionLat * location.getLatitude()) / nearLeftPrecisionLat;
                                        nearLeftLatTemp *= 10;
                                        int shapeLat = (int) nearLeftLatTemp;

                                        double nearLeftPrecisionLon = Math.pow(10, 1);
                                        // Can't create a firebase path with '.', so get rid of decimal.
                                        double nearLeftLonTemp = (int) (nearLeftPrecisionLon * location.getLongitude()) / nearLeftPrecisionLon;
                                        nearLeftLonTemp *= 10;
                                        int shapeLon = (int) nearLeftLonTemp;

                                        Activity.putExtra("shapeLat", shapeLat);
                                        Activity.putExtra("shapeLon", shapeLon);
                                        // UserLatitude and userLongitude are used in DirectMentions.
                                        Activity.putExtra("userLatitude", location.getLatitude());
                                        Activity.putExtra("userLongitude", location.getLongitude());
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("shapeUUID", shapeUUID);
                                        // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
                                        Activity.putExtra("userIsWithinShape", true);
                                        Activity.putExtra("circleLatitude", location.getLatitude());
                                        Activity.putExtra("circleLongitude", location.getLongitude());
                                        Activity.putExtra("radius", 1.0);

                                        clearMap();

                                        loadingIcon.setVisibility(View.GONE);

                                        startActivity(Activity);
                                    } else {

                                        // User NOT signed in.

                                        Log.i(TAG, "onMenuItemClick() -> create point and enter chat -> user NOT signed in");

                                        cancelToasts();

                                        Intent Activity = new Intent(Map.this, SignIn.class);
                                        // Pass this boolean value to Chat.java.
                                        Activity.putExtra("newShape", true);

                                        // Get a value with 1 decimal point and use it for Firebase.
                                        double nearLeftPrecisionLat = Math.pow(10, 1);
                                        // Can't create a firebase path with '.', so get rid of decimal.
                                        double nearLeftLatTemp = (int) (nearLeftPrecisionLat * location.getLatitude()) / nearLeftPrecisionLat;
                                        nearLeftLatTemp *= 10;
                                        int shapeLat = (int) nearLeftLatTemp;

                                        double nearLeftPrecisionLon = Math.pow(10, 1);
                                        // Can't create a firebase path with '.', so get rid of decimal.
                                        double nearLeftLonTemp = (int) (nearLeftPrecisionLon * location.getLongitude()) / nearLeftPrecisionLon;
                                        nearLeftLonTemp *= 10;
                                        int shapeLon = (int) nearLeftLonTemp;

                                        Activity.putExtra("shapeLat", shapeLat);
                                        Activity.putExtra("shapeLon", shapeLon);
                                        // UserLatitude and userLongitude are used in DirectMentions.
                                        Activity.putExtra("userLatitude", location.getLatitude());
                                        Activity.putExtra("userLongitude", location.getLongitude());
                                        // Pass this value to Chat.java to identify the shape.
                                        Activity.putExtra("shapeUUID", shapeUUID);
                                        // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
                                        Activity.putExtra("userIsWithinShape", true);
                                        Activity.putExtra("circleLatitude", location.getLatitude());
                                        Activity.putExtra("circleLongitude", location.getLongitude());
                                        Activity.putExtra("radius", 1.0);

                                        clearMap();

                                        loadingIcon.setVisibility(View.GONE);

                                        startActivity(Activity);
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

    private void adjustMapColors() {

        Log.i(TAG, "adjustMapColors()");

        loadingIcon.setVisibility(View.VISIBLE);

        mMap.clear();

        // Change button color depending on map type.
        if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

            chatViewsButton.setBackgroundResource(R.drawable.chatviews_button);

            createChatButton.setBackgroundResource(R.drawable.createchat_button);

            settingsButton.setBackgroundResource(R.drawable.ic_more_vert_yellow_24dp);

            dmButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.yellow));

            for (int i = 0; i < circleCenterListForMapChange.size(); i++) {

                Circle circle = mMap.addCircle(
                        new CircleOptions()
                                .center(circleCenterListForMapChange.get(i))
                                .clickable(true)
                                .radius(circleRadiusArrayList.get(i))
                                .strokeColor(Color.YELLOW)
                                .strokeWidth(3f)
                );

                circle.setTag(circleUUIDListForMapChange.get(i));
            }

            for (int i = 0; i < polygonPointsListForMapChange.size(); i++) {

                Polygon polygon = mMap.addPolygon(
                        new PolygonOptions()
                                .clickable(true)
                                .addAll(polygonPointsListForMapChange.get(i))
                                .strokeColor(Color.YELLOW)
                                .strokeWidth(3f)
                );

                polygon.setTag(polygonUUIDListForMapChange.get(i));
            }
        } else {

            chatViewsButton.setBackgroundResource(R.drawable.chatviews_button_purple);

            createChatButton.setBackgroundResource(R.drawable.createchat_button_purple);

            settingsButton.setBackgroundResource(R.drawable.ic_more_vert_purple_24dp);

            dmButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.purple));

            for (int i = 0; i < circleCenterListForMapChange.size(); i++) {

                Circle circle = mMap.addCircle(
                        new CircleOptions()
                                .center(circleCenterListForMapChange.get(i))
                                .clickable(true)
                                .radius(circleRadiusArrayList.get(i))
                                .strokeColor(Color.rgb(255, 0, 255))
                                .strokeWidth(3f)
                );

                circle.setTag(circleUUIDListForMapChange.get(i));
            }

            for (int i = 0; i < polygonPointsListForMapChange.size(); i++) {

                Polygon polygon = mMap.addPolygon(
                        new PolygonOptions()
                                .clickable(true)
                                .addAll(polygonPointsListForMapChange.get(i))
                                .strokeColor(Color.rgb(255, 0, 255))
                                .strokeWidth(3f)
                );

                polygon.setTag(polygonUUIDListForMapChange.get(i));
            }
        }

        loadingIcon.setVisibility(View.GONE);

        newCircle = null;
        newPolygon = null;
        chatSizeSeekBar.setProgress(0);

        // Create a circleTemp or polygonTemp if one already exists.
        if (chatSelectorSeekBar.getVisibility() == View.VISIBLE) {

            // Create arrayLists that hold shape information in a useful order.
            ArrayList<Object> combinedListShapes = new ArrayList<>();
            ArrayList<String> combinedListUUID = new ArrayList<>();

            // Add the smaller array fist for consistency with the rest of the logic.
            if (overlappingShapesCircleLocation.size() <= overlappingShapesPolygonVertices.size()) {

                combinedListShapes.addAll(overlappingShapesPolygonVertices);
                combinedListShapes.addAll(overlappingShapesCircleLocation);
                combinedListUUID.addAll(overlappingShapesPolygonUUID);
                combinedListUUID.addAll(overlappingShapesCircleUUID);
            } else {

                combinedListShapes.addAll(overlappingShapesCircleLocation);
                combinedListShapes.addAll(overlappingShapesPolygonVertices);
                combinedListUUID.addAll(overlappingShapesCircleUUID);
                combinedListUUID.addAll(overlappingShapesPolygonUUID);
            }

            selectedOverlappingShapeUUID = combinedListUUID.get(chatSelectorSeekBar.getProgress());

            if (selectedOverlappingShapeUUID != null) {

                if (circleTemp != null) {

                    circleTemp.remove();
                }

                if (polygonTemp != null) {

                    polygonTemp.remove();
                }

                if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                    // Create a yellow highlighted shape.
                    if (combinedListShapes.get(chatSelectorSeekBar.getProgress()) instanceof LatLng) {

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
                } else {

                    // Create a purple highlighted shape.
                    if (combinedListShapes.get(chatSelectorSeekBar.getProgress()) instanceof LatLng) {

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
    }

    private void loadShapes() {

        // Prevent resetting and reloading everything if this is already the state.
        if (firstLoadShapes || restarted || newCameraCoordinates || mapChanged || showEverything || showLarge || showMedium || showSmall || showPoints) {

            Log.i(TAG, "loadShapes()");

            // Don't load more than 7 areas at a time.
            if (loadedCoordinates.size() == 7) {

                mMap.clear();
                loadedCoordinates.clear();
                mapCleared = true;
            }

            DatabaseReference firebaseShapes;
            if (showEverything) {

                firebaseShapes = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")");
            } else if (showLarge) {

                firebaseShapes = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Large");
            } else if (showMedium) {

                firebaseShapes = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Medium");
            } else if (showSmall) {

                firebaseShapes = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Small");
            } else if (showPoints) {

                firebaseShapes = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Points");
            } else {

                toastMessageLong("Oops! Something went wrong!");
                loadingIcon.setVisibility(View.GONE);
                return;
            }

            clearMap();

            if (!loadedCoordinates.contains(newNearLeft) || mapCleared || restarted || showEverything || showLarge || showMedium || showSmall || showPoints) {

                loadedCoordinates.add(0, newNearLeft);

                if (loadedCoordinates.size() > 7) {

                    loadedCoordinates.subList(7, loadedCoordinates.size()).clear();
                }

                firebaseShapes.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        loadShapesODC(snapshot);

                        addShapesQuery();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                        Log.e(TAG, "DatabaseError");
                        loadingIcon.setVisibility(View.GONE);
                        toastMessageLong(databaseError.getMessage());
                    }
                });
            }

            // Without the final "&&" statements, every one of these statement groups will be called after restart, even when newNearLeft == newFarLeft == newNearRight == newFarRight.
            if (!loadedCoordinates.contains(newFarLeft) || mapCleared || (!loadedCoordinates.contains(newFarLeft) && restarted) || (!loadedCoordinates.contains(newFarLeft) && showEverything) ||
                    (!loadedCoordinates.contains(newFarLeft) && showLarge) || (!loadedCoordinates.contains(newFarLeft) && showMedium) ||
                    (!loadedCoordinates.contains(newFarLeft) && showSmall) || (!loadedCoordinates.contains(newFarLeft) && showPoints)) {

                loadedCoordinates.add(0, newFarLeft);

                if (loadedCoordinates.size() > 7) {

                    loadedCoordinates.subList(7, loadedCoordinates.size()).clear();
                }

                firebaseShapes.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        loadShapesODC(snapshot);

                        addShapesQuery();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                        Log.e(TAG, "DatabaseError");
                        loadingIcon.setVisibility(View.GONE);
                        toastMessageLong(databaseError.getMessage());
                    }
                });
            }

            // Without the final "&&" statements, every one of these statement groups will be called after restart, even when newNearLeft == newFarLeft == newNearRight == newFarRight.
            if (!loadedCoordinates.contains(newNearRight) || mapCleared || (!loadedCoordinates.contains(newNearRight) && restarted) || (!loadedCoordinates.contains(newNearRight) && showEverything) ||
                    (!loadedCoordinates.contains(newNearRight) && showLarge) || (!loadedCoordinates.contains(newNearRight) && showMedium) ||
                    (!loadedCoordinates.contains(newNearRight) && showSmall) || (!loadedCoordinates.contains(newNearRight) && showPoints)) {

                loadedCoordinates.add(0, newNearRight);

                if (loadedCoordinates.size() > 7) {

                    loadedCoordinates.subList(7, loadedCoordinates.size()).clear();
                }

                firebaseShapes.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        loadShapesODC(snapshot);

                        addShapesQuery();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                        Log.e(TAG, "DatabaseError");
                        loadingIcon.setVisibility(View.GONE);
                        toastMessageLong(databaseError.getMessage());
                    }
                });
            }

            // Without the final "&&" statements, every one of these statement groups will be called after restart, even when newNearLeft == newFarLeft == newNearRight == newFarRight.
            if (!loadedCoordinates.contains(newFarRight) || mapCleared || (!loadedCoordinates.contains(newFarRight) && restarted) || (!loadedCoordinates.contains(newFarRight) && showEverything) ||
                    (!loadedCoordinates.contains(newFarRight) && showLarge) || (!loadedCoordinates.contains(newFarRight) && showMedium) ||
                    (!loadedCoordinates.contains(newFarRight) && showSmall) || (!loadedCoordinates.contains(newFarRight) && showPoints)) {

                loadedCoordinates.add(0, newFarRight);

                if (loadedCoordinates.size() > 7) {

                    loadedCoordinates.subList(7, loadedCoordinates.size()).clear();
                }

                firebaseShapes.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        loadShapesODC(snapshot);

                        addShapesQuery();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                        Log.e(TAG, "DatabaseError");
                        loadingIcon.setVisibility(View.GONE);
                        toastMessageLong(databaseError.getMessage());
                    }
                });
            }

            if (showEverything) {

                showingEverything = true;
                showingLarge = false;
                showingMedium = false;
                showingSmall = false;
                showingPoints = false;
            } else if (showLarge) {

                showingLarge = true;
                showingEverything = false;
                showingMedium = false;
                showingSmall = false;
                showingPoints = false;
            } else if (showMedium) {

                showingMedium = true;
                showingEverything = false;
                showingLarge = false;
                showingSmall = false;
                showingPoints = false;
            } else if (showSmall) {

                showingSmall = true;
                showingEverything = false;
                showingLarge = false;
                showingMedium = false;
                showingPoints = false;
            } else if (showPoints) {

                showingPoints = true;
                showingEverything = false;
                showingLarge = false;
                showingMedium = false;
                showingSmall = false;
            }

            mapChanged = false;

            oldNearLeft = newNearLeft;
            oldFarLeft = newFarLeft;
            oldNearRight = newNearRight;
            oldFarRight = newFarRight;
            newCameraCoordinates = false;
        }
    }

    private void loadShapesODC(DataSnapshot snapshot) {

        if (showEverything) {

            loadingIcon.setVisibility(View.VISIBLE);

            for (DataSnapshot ds : snapshot.getChildren()) {

                // Used in addShapesQuery to allow the child to be called the first time if no child exists and prevent double posts if a child exists.
                if (ds.getKey().equals("Large")) {

                    largeExists = true;
                }

                if (ds.getKey().equals("Medium")) {

                    mediumExists = true;
                }

                if (ds.getKey().equals("Small")) {

                    smallExists = true;
                }

                if (ds.getKey().equals("Points")) {

                    pointsExist = true;
                }

                for (DataSnapshot dss : ds.getChildren()) {

                    if (dss.child("circleOptions").exists()) {

                        // Shape is a circle.

                        circleCenterListForMapChange.add(new LatLng((double) dss.child("circleOptions/center/latitude/").getValue(), (double) dss.child("circleOptions/center/longitude/").getValue()));
                        circleRadiusArrayList.add(((Number) (Objects.requireNonNull(dss.child("circleOptions/radius").getValue()))).doubleValue());
                        circleUUIDListForMapChange.add((String) dss.child("shapeUUID").getValue());

                        // Load different colored shapes depending on the map type.
                        if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                            // Yellow circle.

                            double radius = 0;

                            LatLng center = new LatLng((double) dss.child("circleOptions/center/latitude/").getValue(), (double) dss.child("circleOptions/center/longitude/").getValue());
                            if ((dss.child("circleOptions/radius").getValue()) != null) {

                                radius = ((Number) (Objects.requireNonNull(dss.child("circleOptions/radius").getValue()))).doubleValue();
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
                            shapeUUID = (String) dss.child("shapeUUID").getValue();

                            circle.setTag(shapeUUID);
                        } else {

                            // Purple circle.

                            double radius = 0;

                            LatLng center = new LatLng((double) dss.child("circleOptions/center/latitude/").getValue(), (double) dss.child("circleOptions/center/longitude/").getValue());
                            if ((dss.child("circleOptions/radius").getValue()) != null) {

                                radius = ((Number) (Objects.requireNonNull(dss.child("circleOptions/radius").getValue()))).doubleValue();
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
                            shapeUUID = (String) dss.child("shapeUUID").getValue();

                            circle.setTag(shapeUUID);
                        }
                    } else {

                        // Shape is a polygon.

                        // Load different colored shapes depending on the map type.
                        if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                            // Yellow polygon.

                            LatLng marker3Position = null;
                            LatLng marker4Position = null;
                            LatLng marker5Position = null;
                            LatLng marker6Position = null;
                            Polygon polygon;

                            LatLng marker0Position = new LatLng((double) dss.child("polygonOptions/points/0/latitude/").getValue(), (double) dss.child("polygonOptions/points/0/longitude/").getValue());
                            LatLng marker1Position = new LatLng((double) dss.child("polygonOptions/points/1/latitude/").getValue(), (double) dss.child("polygonOptions/points/1/longitude/").getValue());
                            LatLng marker2Position = new LatLng((double) dss.child("polygonOptions/points/2/latitude/").getValue(), (double) dss.child("polygonOptions/points/2/longitude/").getValue());
                            if (dss.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                marker3Position = new LatLng((double) dss.child("polygonOptions/points/3/latitude/").getValue(), (double) dss.child("polygonOptions/points/3/longitude/").getValue());
                            }
                            if (dss.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                marker4Position = new LatLng((double) dss.child("polygonOptions/points/4/latitude/").getValue(), (double) dss.child("polygonOptions/points/4/longitude/").getValue());
                            }
                            if (dss.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                marker5Position = new LatLng((double) dss.child("polygonOptions/points/5/latitude/").getValue(), (double) dss.child("polygonOptions/points/5/longitude/").getValue());
                            }
                            if (dss.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                marker6Position = new LatLng((double) dss.child("polygonOptions/points/6/latitude/").getValue(), (double) dss.child("polygonOptions/points/6/longitude/").getValue());
                            }
                            if (dss.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                LatLng marker7Position = new LatLng((double) dss.child("polygonOptions/points/7/latitude/").getValue(), (double) dss.child("polygonOptions/points/7/longitude/").getValue());

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                points.add(marker7Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) dss.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (dss.child("polygonOptions/points/6/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) dss.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (dss.child("polygonOptions/points/5/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) dss.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (dss.child("polygonOptions/points/4/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) dss.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (dss.child("polygonOptions/points/3/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) dss.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) dss.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            }
                        } else {

                            // Purple polygon.

                            LatLng marker3Position = null;
                            LatLng marker4Position = null;
                            LatLng marker5Position = null;
                            LatLng marker6Position = null;
                            Polygon polygon;

                            LatLng marker0Position = new LatLng((double) dss.child("polygonOptions/points/0/latitude/").getValue(), (double) dss.child("polygonOptions/points/0/longitude/").getValue());
                            LatLng marker1Position = new LatLng((double) dss.child("polygonOptions/points/1/latitude/").getValue(), (double) dss.child("polygonOptions/points/1/longitude/").getValue());
                            LatLng marker2Position = new LatLng((double) dss.child("polygonOptions/points/2/latitude/").getValue(), (double) dss.child("polygonOptions/points/2/longitude/").getValue());
                            if (dss.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                marker3Position = new LatLng((double) dss.child("polygonOptions/points/3/latitude/").getValue(), (double) dss.child("polygonOptions/points/3/longitude/").getValue());
                            }
                            if (dss.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                marker4Position = new LatLng((double) dss.child("polygonOptions/points/4/latitude/").getValue(), (double) dss.child("polygonOptions/points/4/longitude/").getValue());
                            }
                            if (dss.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                marker5Position = new LatLng((double) dss.child("polygonOptions/points/5/latitude/").getValue(), (double) dss.child("polygonOptions/points/5/longitude/").getValue());
                            }
                            if (dss.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                marker6Position = new LatLng((double) dss.child("polygonOptions/points/6/latitude/").getValue(), (double) dss.child("polygonOptions/points/6/longitude/").getValue());
                            }
                            if (dss.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                LatLng marker7Position = new LatLng((double) dss.child("polygonOptions/points/7/latitude/").getValue(), (double) dss.child("polygonOptions/points/7/longitude/").getValue());

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                points.add(marker7Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (dss.child("polygonOptions/points/6/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (dss.child("polygonOptions/points/5/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (dss.child("polygonOptions/points/4/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (dss.child("polygonOptions/points/3/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) dss.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            }
                        }
                    }
                }
            }

            loadingIcon.setVisibility(View.GONE);
        } else if (showLarge) {

            loadingIcon.setVisibility(View.VISIBLE);

            for (DataSnapshot ds : snapshot.getChildren()) {

                // Used in addShapesQuery to allow the child to be called the first time if no child exists and prevent double posts if a child exists.
                largeExists = true;

                if (ds.child("circleOptions").exists()) {

                    // Shape is a circle.

                    circleCenterListForMapChange.add(new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue()));
                    circleRadiusArrayList.add(((Number) (Objects.requireNonNull(ds.child("circleOptions/radius").getValue()))).doubleValue());
                    circleUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                    // Load different colored shapes depending on the map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        // Only load large circles (radius > 50)
                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 50) {

                            // Yellow circle.

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
                    } else {

                        // Purple circle.

                        // Only load large circles (radius > 50)
                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 50) {

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
                } else {

                    // Shape is a polygon.

                    // Load different colored shapes depending on the map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        // Yellow polygon.

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

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                points.add(marker7Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            }
                        }
                    } else {

                        // Purple polygon.

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

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                points.add(marker7Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
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
            }

            loadingIcon.setVisibility(View.GONE);
        } else if (showMedium) {

            loadingIcon.setVisibility(View.VISIBLE);

            for (DataSnapshot ds : snapshot.getChildren()) {

                // Used in addShapesQuery to allow the child to be called the first time if no child exists and prevent double posts if a child exists.
                mediumExists = true;

                if (ds.child("circleOptions").exists()) {

                    // Shape is a circle.

                    circleCenterListForMapChange.add(new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue()));
                    circleRadiusArrayList.add(((Number) (Objects.requireNonNull(ds.child("circleOptions/radius").getValue()))).doubleValue());
                    circleUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                    // Load different colored shapes depending on the map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        // Yellow circle.

                        // Only load medium circles (10 < radius <= 50)
                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 10 && (double) (long) ds.child("circleOptions/radius").getValue() <= 50) {

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
                    } else {

                        // Purple circle.

                        // Only load medium circles (10 < radius <= 50)
                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 10 && (double) (long) ds.child("circleOptions/radius").getValue() <= 50) {

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
                } else {

                    // Shape is a polygon.

                    // Load different colored shapes depending on the map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        // Only load medium polygons pi(10^2) < polygonArea <= pi(50^2))
                        if ((double) ds.child("polygonArea").getValue() > Math.PI * (Math.pow(10, 2)) && (double) ds.child("polygonArea").getValue() <= Math.PI * (Math.pow(50, 2))) {

                            // Yellow polygon.

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

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                points.add(marker7Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            }
                        }
                    } else {

                        // Purple polygon.

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

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                points.add(marker7Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
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
            }

            loadingIcon.setVisibility(View.GONE);
        } else if (showSmall) {

            loadingIcon.setVisibility(View.VISIBLE);

            for (DataSnapshot ds : snapshot.getChildren()) {

                // Used in addShapesQuery to allow the child to be called the first time if no child exists and prevent double posts if a child exists.
                smallExists = true;

                if (ds.child("circleOptions").exists()) {

                    // Shape is a circle.

                    circleCenterListForMapChange.add(new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue()));
                    circleRadiusArrayList.add(((Number) (Objects.requireNonNull(ds.child("circleOptions/radius").getValue()))).doubleValue());
                    circleUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                    // Load different colored shapes depending on the map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        // Yellow circle.

                        // Only load small circles (1 < radius <= 10)
                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 1 && (double) (long) ds.child("circleOptions/radius").getValue() <= 10) {

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
                    } else {

                        // Purple circle.

                        // Only load small circles (1 < radius <= 10)
                        if ((double) (long) ds.child("circleOptions/radius").getValue() > 1 && (double) (long) ds.child("circleOptions/radius").getValue() <= 10) {

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
                } else {

                    // Shape is a polygon.

                    // Load different colored shapes depending on the map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        // Only load small polygons pi < polygonArea <= pi(10^2))
                        if ((double) ds.child("polygonArea").getValue() > Math.PI && (double) ds.child("polygonArea").getValue() <= Math.PI * (Math.pow(10, 2))) {

                            // Yellow polygon.

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

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                points.add(marker7Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.YELLOW)
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            }
                        }
                    } else {

                        // Purple polygon.

                        // Only load small polygons pi < polygonArea <= pi(10^2))
                        if ((double) ds.child("polygonArea").getValue() > Math.PI && (double) ds.child("polygonArea").getValue() <= Math.PI * (Math.pow(10, 2))) {

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

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                points.add(marker7Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                points.add(marker6Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                points.add(marker5Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                points.add(marker4Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                points.add(marker3Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
                                                .strokeColor(Color.rgb(255, 0, 255))
                                                .strokeWidth(3f)
                                );

                                // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                                shapeUUID = (String) ds.child("shapeUUID").getValue();

                                polygon.setTag(shapeUUID);
                            } else {

                                List<LatLng> points = new ArrayList<>();
                                points.add(marker0Position);
                                points.add(marker1Position);
                                points.add(marker2Position);
                                polygonPointsListForMapChange.add(points);
                                polygonUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                                polygon = mMap.addPolygon(
                                        new PolygonOptions()
                                                .clickable(true)
                                                .addAll(points)
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
            }

            loadingIcon.setVisibility(View.GONE);
        } else if (showPoints) {

            loadingIcon.setVisibility(View.VISIBLE);

            for (DataSnapshot ds : snapshot.getChildren()) {

                // Used in addShapesQuery to allow the child to be called the first time if no child exists and prevent double posts if a child exists.
                showPoints = true;

                if (ds.child("circleOptions").exists()) {

                    // Shape is a circle.

                    circleCenterListForMapChange.add(new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue()));
                    circleRadiusArrayList.add(((Number) (Objects.requireNonNull(ds.child("circleOptions/radius").getValue()))).doubleValue());
                    circleUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                    // Load different colored shapes depending on the map type.
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        // Yellow circle.

                        // Only load points (radius == 1)
                        if ((double) (long) ds.child("circleOptions/radius").getValue() == 1) {

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
                    } else {

                        // Purple circle.

                        // Only load points (radius == 1)
                        if ((double) (long) ds.child("circleOptions/radius").getValue() == 1) {

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
                }
            }

            loadingIcon.setVisibility(View.GONE);
        }
    }

    // Change to .limitToLast(1) to cut down on data usage. Otherwise, EVERY child at this node will be downloaded every time the child is updated.
    private void addShapesQuery() {

        queryShapesLarge = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Large").limitToLast(1);
        queryShapesMedium = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Medium").limitToLast(1);
        queryShapesSmall = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Small").limitToLast(1);
        queryShapesPoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Points").limitToLast(1);

        childEventListenerShapesLarge = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.i(TAG, "addShapesQueryLarge()");

                // If this is the first time calling this eventListener, prevent double posts Since all childEventListenerShapes use firstLoadShapes, set the other boolean to false.
                if (firstLoadShapes && largeExists) {

                    largeExists = false;
                    return;
                }

                addShapesQueryLarge(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                toastMessageLong(error.getMessage());
            }
        };

        childEventListenerShapesMedium = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.i(TAG, "addShapesQueryMedium()");

                // If this is the first time calling this eventListener, prevent double posts Since all childEventListenerShapes use firstLoadShapes, set the other boolean to false.
                if (firstLoadShapes && mediumExists) {

                    mediumExists = false;
                    return;
                }

                addShapesQueryMedium(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                toastMessageLong(error.getMessage());
            }
        };

        childEventListenerShapesSmall = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.i(TAG, "addShapesQuerySmall()");

                // If this is the first time calling this eventListener, prevent double posts Since all childEventListenerShapes use firstLoadShapes, set the other boolean to false.
                if (firstLoadShapes && smallExists) {

                    smallExists = false;
                    return;
                }

                addShapesQuerySmall(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                toastMessageLong(error.getMessage());
            }
        };

        childEventListenerShapesPoints = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.i(TAG, "addShapesQueryPoints()");

                // If this is the first time calling this eventListener, prevent double posts Since all childEventListenerShapes use firstLoadShapes, set the other boolean to false.
                if (firstLoadShapes && pointsExist) {

                    pointsExist = false;
                    return;
                }

                addShapesQueryPoints(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                toastMessageLong(error.getMessage());
            }
        };

        // Other childEventListeners are removed when user clicks on the corresponding menu item.
        if (showEverything) {

            queryShapesLarge.addChildEventListener(childEventListenerShapesLarge);
            queryShapesMedium.addChildEventListener(childEventListenerShapesMedium);
            queryShapesSmall.addChildEventListener(childEventListenerShapesSmall);
            queryShapesPoints.addChildEventListener(childEventListenerShapesPoints);
        } else if (showLarge) {

            queryShapesLarge.addChildEventListener(childEventListenerShapesLarge);
        } else if (showMedium) {

            queryShapesMedium.addChildEventListener(childEventListenerShapesMedium);
        } else if (showSmall) {

            queryShapesSmall.addChildEventListener(childEventListenerShapesSmall);
        } else if (showPoints) {

            queryShapesPoints.addChildEventListener(childEventListenerShapesPoints);
        }
    }

    private void addShapesQueryLarge(DataSnapshot snapshot) {

        loadingIcon.setVisibility(View.VISIBLE);

        if (snapshot.child("circleOptions").exists()) {

            // Shape is a circle.

            circleCenterListForMapChange.add(new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue()));
            circleRadiusArrayList.add(((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue());
            circleUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

            // Load different colored shapes depending on the map type.
            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                // Only load large circles (radius > 50)
                if ((double) (long) snapshot.child("circleOptions/radius").getValue() > 50) {

                    // Yellow circle.

                    double radius = 0;

                    LatLng center = new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue());
                    if ((snapshot.child("circleOptions/radius").getValue()) != null) {

                        radius = ((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue();
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
                    shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                }
            } else {

                // Purple circle.

                // Only load large circles (radius > 50)
                if ((double) (long) snapshot.child("circleOptions/radius").getValue() > 50) {

                    double radius = 0;

                    LatLng center = new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue());
                    if ((snapshot.child("circleOptions/radius").getValue()) != null) {

                        radius = ((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue();
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
                    shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                }
            }
        } else {

            // Shape is a polygon.

            // Load different colored shapes depending on the map type.
            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                // Yellow polygon.

                // Only load large polygons (polygonArea > pi(50^2))
                if ((double) snapshot.child("polygonArea").getValue() > Math.PI * (Math.pow(50, 2))) {

                    LatLng marker3Position = null;
                    LatLng marker4Position = null;
                    LatLng marker5Position = null;
                    LatLng marker6Position = null;
                    Polygon polygon;

                    LatLng marker0Position = new LatLng((double) snapshot.child("polygonOptions/points/0/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/0/longitude/").getValue());
                    LatLng marker1Position = new LatLng((double) snapshot.child("polygonOptions/points/1/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/1/longitude/").getValue());
                    LatLng marker2Position = new LatLng((double) snapshot.child("polygonOptions/points/2/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/2/longitude/").getValue());
                    if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {
                        marker3Position = new LatLng((double) snapshot.child("polygonOptions/points/3/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/3/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {
                        marker4Position = new LatLng((double) snapshot.child("polygonOptions/points/4/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/4/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {
                        marker5Position = new LatLng((double) snapshot.child("polygonOptions/points/5/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/5/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {
                        marker6Position = new LatLng((double) snapshot.child("polygonOptions/points/6/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/6/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/7/latitude/").getValue() != null) {
                        LatLng marker7Position = new LatLng((double) snapshot.child("polygonOptions/points/7/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/7/longitude/").getValue());

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        points.add(marker7Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    }
                }
            } else {

                // Purple polygon.

                // Only load large polygons (polygonArea > pi(50^2))
                if ((double) snapshot.child("polygonArea").getValue() > Math.PI * (Math.pow(50, 2))) {

                    LatLng marker3Position = null;
                    LatLng marker4Position = null;
                    LatLng marker5Position = null;
                    LatLng marker6Position = null;
                    Polygon polygon;

                    LatLng marker0Position = new LatLng((double) snapshot.child("polygonOptions/points/0/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/0/longitude/").getValue());
                    LatLng marker1Position = new LatLng((double) snapshot.child("polygonOptions/points/1/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/1/longitude/").getValue());
                    LatLng marker2Position = new LatLng((double) snapshot.child("polygonOptions/points/2/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/2/longitude/").getValue());
                    if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {
                        marker3Position = new LatLng((double) snapshot.child("polygonOptions/points/3/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/3/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {
                        marker4Position = new LatLng((double) snapshot.child("polygonOptions/points/4/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/4/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {
                        marker5Position = new LatLng((double) snapshot.child("polygonOptions/points/5/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/5/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {
                        marker6Position = new LatLng((double) snapshot.child("polygonOptions/points/6/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/6/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/7/latitude/").getValue() != null) {
                        LatLng marker7Position = new LatLng((double) snapshot.child("polygonOptions/points/7/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/7/longitude/").getValue());

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        points.add(marker7Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    }
                }
            }
        }

        loadingIcon.setVisibility(View.GONE);
    }

    private void addShapesQueryMedium(DataSnapshot snapshot) {

        loadingIcon.setVisibility(View.VISIBLE);

        if (snapshot.child("circleOptions").exists()) {

            // Shape is a circle.

            circleCenterListForMapChange.add(new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue()));
            circleRadiusArrayList.add(((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue());
            circleUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

            // Load different colored shapes depending on the map type.
            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                // Yellow circle.

                // Only load medium circles (10 < radius <= 50)
                if ((double) (long) snapshot.child("circleOptions/radius").getValue() > 10 && (double) (long) snapshot.child("circleOptions/radius").getValue() <= 50) {

                    double radius = 0;

                    LatLng center = new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue());
                    if ((snapshot.child("circleOptions/radius").getValue()) != null) {

                        radius = ((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue();
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
                    shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                }
            } else {

                // Purple circle.

                // Only load medium circles (10 < radius <= 50)
                if ((double) (long) snapshot.child("circleOptions/radius").getValue() > 10 && (double) (long) snapshot.child("circleOptions/radius").getValue() <= 50) {

                    double radius = 0;

                    LatLng center = new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue());
                    if ((snapshot.child("circleOptions/radius").getValue()) != null) {

                        radius = ((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue();
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
                    shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                }
            }
        } else {

            // Shape is a polygon.

            // Load different colored shapes depending on the map type.
            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                // Only load medium polygons pi(10^2) < polygonArea <= pi(50^2))
                if ((double) snapshot.child("polygonArea").getValue() > Math.PI * (Math.pow(10, 2)) && (double) snapshot.child("polygonArea").getValue() <= Math.PI * (Math.pow(50, 2))) {

                    // Yellow polygon.

                    LatLng marker3Position = null;
                    LatLng marker4Position = null;
                    LatLng marker5Position = null;
                    LatLng marker6Position = null;
                    Polygon polygon;

                    LatLng marker0Position = new LatLng((double) snapshot.child("polygonOptions/points/0/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/0/longitude/").getValue());
                    LatLng marker1Position = new LatLng((double) snapshot.child("polygonOptions/points/1/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/1/longitude/").getValue());
                    LatLng marker2Position = new LatLng((double) snapshot.child("polygonOptions/points/2/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/2/longitude/").getValue());
                    if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {
                        marker3Position = new LatLng((double) snapshot.child("polygonOptions/points/3/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/3/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {
                        marker4Position = new LatLng((double) snapshot.child("polygonOptions/points/4/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/4/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {
                        marker5Position = new LatLng((double) snapshot.child("polygonOptions/points/5/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/5/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {
                        marker6Position = new LatLng((double) snapshot.child("polygonOptions/points/6/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/6/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/7/latitude/").getValue() != null) {
                        LatLng marker7Position = new LatLng((double) snapshot.child("polygonOptions/points/7/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/7/longitude/").getValue());

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        points.add(marker7Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    }
                }
            } else {

                // Purple polygon.

                // Only load medium polygons pi(10^2) < polygonArea <= pi(50^2))
                if ((double) snapshot.child("polygonArea").getValue() > Math.PI * (Math.pow(10, 2)) && (double) snapshot.child("polygonArea").getValue() <= Math.PI * (Math.pow(50, 2))) {

                    LatLng marker3Position = null;
                    LatLng marker4Position = null;
                    LatLng marker5Position = null;
                    LatLng marker6Position = null;
                    Polygon polygon;

                    LatLng marker0Position = new LatLng((double) snapshot.child("polygonOptions/points/0/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/0/longitude/").getValue());
                    LatLng marker1Position = new LatLng((double) snapshot.child("polygonOptions/points/1/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/1/longitude/").getValue());
                    LatLng marker2Position = new LatLng((double) snapshot.child("polygonOptions/points/2/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/2/longitude/").getValue());
                    if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {
                        marker3Position = new LatLng((double) snapshot.child("polygonOptions/points/3/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/3/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {
                        marker4Position = new LatLng((double) snapshot.child("polygonOptions/points/4/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/4/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {
                        marker5Position = new LatLng((double) snapshot.child("polygonOptions/points/5/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/5/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {
                        marker6Position = new LatLng((double) snapshot.child("polygonOptions/points/6/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/6/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/7/latitude/").getValue() != null) {
                        LatLng marker7Position = new LatLng((double) snapshot.child("polygonOptions/points/7/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/7/longitude/").getValue());

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        points.add(marker7Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    }
                }
            }
        }

        loadingIcon.setVisibility(View.GONE);
    }

    private void addShapesQuerySmall(DataSnapshot snapshot) {

        loadingIcon.setVisibility(View.VISIBLE);

        if (snapshot.child("circleOptions").exists()) {

            // Shape is a circle.

            circleCenterListForMapChange.add(new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue()));
            circleRadiusArrayList.add(((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue());
            circleUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

            // Load different colored shapes depending on the map type.
            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                // Yellow circle.

                // Only load small circles (1 < radius <= 10)
                if ((double) (long) snapshot.child("circleOptions/radius").getValue() > 1 && (double) (long) snapshot.child("circleOptions/radius").getValue() <= 10) {

                    double radius = 0;

                    LatLng center = new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue());
                    if ((snapshot.child("circleOptions/radius").getValue()) != null) {

                        radius = ((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue();
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
                    shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                }
            } else {

                // Purple circle.

                // Only load small circles (1 < radius <= 10)
                if ((double) (long) snapshot.child("circleOptions/radius").getValue() > 1 && (double) (long) snapshot.child("circleOptions/radius").getValue() <= 10) {

                    double radius = 0;

                    LatLng center = new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue());
                    if ((snapshot.child("circleOptions/radius").getValue()) != null) {

                        radius = ((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue();
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
                    shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                }
            }
        } else {

            // Shape is a polygon.

            // Load different colored shapes depending on the map type.
            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                // Only load small polygons pi < polygonArea <= pi(10^2))
                if ((double) snapshot.child("polygonArea").getValue() > Math.PI && (double) snapshot.child("polygonArea").getValue() <= Math.PI * (Math.pow(10, 2))) {

                    // Yellow polygon.

                    LatLng marker3Position = null;
                    LatLng marker4Position = null;
                    LatLng marker5Position = null;
                    LatLng marker6Position = null;
                    Polygon polygon;

                    LatLng marker0Position = new LatLng((double) snapshot.child("polygonOptions/points/0/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/0/longitude/").getValue());
                    LatLng marker1Position = new LatLng((double) snapshot.child("polygonOptions/points/1/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/1/longitude/").getValue());
                    LatLng marker2Position = new LatLng((double) snapshot.child("polygonOptions/points/2/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/2/longitude/").getValue());
                    if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {
                        marker3Position = new LatLng((double) snapshot.child("polygonOptions/points/3/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/3/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {
                        marker4Position = new LatLng((double) snapshot.child("polygonOptions/points/4/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/4/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {
                        marker5Position = new LatLng((double) snapshot.child("polygonOptions/points/5/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/5/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {
                        marker6Position = new LatLng((double) snapshot.child("polygonOptions/points/6/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/6/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/7/latitude/").getValue() != null) {
                        LatLng marker7Position = new LatLng((double) snapshot.child("polygonOptions/points/7/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/7/longitude/").getValue());

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        points.add(marker7Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    }
                }
            } else {

                // Purple polygon.

                // Only load small polygons pi < polygonArea <= pi(10^2))
                if ((double) snapshot.child("polygonArea").getValue() > Math.PI && (double) snapshot.child("polygonArea").getValue() <= Math.PI * (Math.pow(10, 2))) {

                    LatLng marker3Position = null;
                    LatLng marker4Position = null;
                    LatLng marker5Position = null;
                    LatLng marker6Position = null;
                    Polygon polygon;

                    LatLng marker0Position = new LatLng((double) snapshot.child("polygonOptions/points/0/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/0/longitude/").getValue());
                    LatLng marker1Position = new LatLng((double) snapshot.child("polygonOptions/points/1/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/1/longitude/").getValue());
                    LatLng marker2Position = new LatLng((double) snapshot.child("polygonOptions/points/2/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/2/longitude/").getValue());
                    if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {
                        marker3Position = new LatLng((double) snapshot.child("polygonOptions/points/3/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/3/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {
                        marker4Position = new LatLng((double) snapshot.child("polygonOptions/points/4/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/4/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {
                        marker5Position = new LatLng((double) snapshot.child("polygonOptions/points/5/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/5/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {
                        marker6Position = new LatLng((double) snapshot.child("polygonOptions/points/6/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/6/longitude/").getValue());
                    }
                    if (snapshot.child("polygonOptions/points/7/latitude/").getValue() != null) {
                        LatLng marker7Position = new LatLng((double) snapshot.child("polygonOptions/points/7/latitude/").getValue(), (double) snapshot.child("polygonOptions/points/7/longitude/").getValue());

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        points.add(marker7Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/6/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        points.add(marker6Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/5/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        points.add(marker5Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/4/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        points.add(marker4Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else if (snapshot.child("polygonOptions/points/3/latitude/").getValue() != null) {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        points.add(marker3Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    } else {

                        List<LatLng> points = new ArrayList<>();
                        points.add(marker0Position);
                        points.add(marker1Position);
                        points.add(marker2Position);
                        polygonPointsListForMapChange.add(points);
                        polygonUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

                        polygon = mMap.addPolygon(
                                new PolygonOptions()
                                        .clickable(true)
                                        .addAll(points)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );

                        // Set the Tag using the uuid in Firebase. Value is sent to Chat.java in onMapReady() to identify the shape.
                        shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                        polygon.setTag(shapeUUID);
                    }
                }
            }
        }

        loadingIcon.setVisibility(View.GONE);
    }

    private void addShapesQueryPoints(DataSnapshot snapshot) {

        loadingIcon.setVisibility(View.VISIBLE);

        if (snapshot.child("circleOptions").exists()) {

            // Shape is a circle.

            circleCenterListForMapChange.add(new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue()));
            circleRadiusArrayList.add(((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue());
            circleUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

            // Load different colored shapes depending on the map type.
            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                // Yellow circle.

                // Only load points (radius == 1)
                if ((double) (long) snapshot.child("circleOptions/radius").getValue() == 1) {

                    double radius = 0;

                    LatLng center = new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue());
                    if ((snapshot.child("circleOptions/radius").getValue()) != null) {

                        radius = ((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue();
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
                    shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                }
            } else {

                // Purple circle.

                // Only load points (radius == 1)
                if ((double) (long) snapshot.child("circleOptions/radius").getValue() == 1) {

                    double radius = 0;

                    LatLng center = new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue());
                    if ((snapshot.child("circleOptions/radius").getValue()) != null) {

                        radius = ((Number) (Objects.requireNonNull(snapshot.child("circleOptions/radius").getValue()))).doubleValue();
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
                    shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                }
            }
        }

        loadingIcon.setVisibility(View.GONE);
    }

    private void clearMap() {

        Log.i(TAG, "clearMap()");

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
            chatSizeSeekBar.setVisibility(View.VISIBLE);
        }

        chatSizeSeekBar.setProgress(0);
        chatSelectorSeekBar.setProgress(0);
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
    private static float distanceGivenLatLng(double lat1, double lng1, double lat2,
                                             double lng2) {

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

    private LatLng latLngGivenDistance(double latitude, double longitude,
                                       double distanceInMetres, double bearing) {

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

        cancelToasts();
        longToast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG);
        longToast.setGravity(Gravity.CENTER, 0, 0);
        longToast.show();
    }
}