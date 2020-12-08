package co.clixel.herebefore;

import android.Manifest;
import android.annotation.SuppressLint;
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
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class Map extends FragmentActivity implements
        OnMapReadyCallback,
        LocationListener,
        PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "Map";
    private GoogleMap mMap;
    private static final int Request_User_Location_Code = 99;
    private Circle newCircle, circleTemp, mCircle = null;
    private ChildEventListener childEventListenerDms, childEventListenerShapesPoints;
    private SeekBar chatSelectorSeekBar;
    private String userEmailFirebase, shapeUUID, selectedOverlappingShapeUUID;
    private Button createChatButton, mapTypeButton, settingsButton;
    private PopupMenu popupMapType;
    private boolean locationProviderDisabled = false, firstLoadCamera = true, firstLoadShapes = true, firstLoadDms = true, dmExists = false, pointsExist = false, mapChanged, cameraMoved = false, waitingForBetterLocationAccuracy = false, badAccuracy = false,
            waitingForClicksToProcess = false, waitingForShapeInformationToProcess = false, userIsWithinShape, selectingShape = false, restarted = false, newCameraCoordinates = false, mapCleared = false;
    private LatLng selectedOverlappingShapeCircleLocation;
    private Double selectedOverlappingShapeCircleRadius;
    private ArrayList<String> overlappingShapesUUID = new ArrayList<>(), overlappingShapesCircleUUID = new ArrayList<>();
    private final ArrayList<String> circleUUIDListForMapChange = new ArrayList<>();
    private ArrayList<LatLng> overlappingShapesCircleLocation = new ArrayList<>();
    private final ArrayList<LatLng> circleCenterListForMapChange = new ArrayList<>();
    private ArrayList<Double> overlappingShapesCircleRadius = new ArrayList<>();
    private final ArrayList<Double> circleRadiusArrayList = new ArrayList<>();
    private float x, y;
    private int chatsSize, dmCounter = 0, newNearLeftLat, newNearLeftLon;
    private Toast longToast;
    private View loadingIcon;
    private CounterFab dmButton;
    private LocationManager locationManager;
    private Query queryDms, queryShapesPoints;
    private Pair<Integer, Integer> oldNearLeft, oldFarLeft, oldNearRight, oldFarRight, newNearLeft, newFarLeft, newNearRight, newFarRight;
    private final List<Pair<Integer, Integer>> loadedCoordinates = new ArrayList<>();

    // Deleting messageThread no longer deletes shape.
    // Change look of createChatButton - maybe make a circle around outside that's mostly transparent in the middle?
    // If user is on a point, prevent creating a new one. Deal with overlapping shapes in general.
    // Deal with deprecated methods.
    // Get rid of mPosition, because there is no way to guarantee a position when multiple people are adding messages simultaneously.
    // Add Firebase functions to adjust spannable string when changing a messageThread.
    // Switch from initChatAdapter() to notifyChatAdapter() to increase speed? Generally, make Chat load faster, especially if there are multiple ClickableSpans
    // Make scrollToPosition work in Chat after a restart. Also prevent reloading Chat and DMs every time app restarts.
    // Find a way to not clear and reload map every time user returns from clicking a shape. Same with DM notification.
    // Prevent data scraping (hide email addresses and other personal information).
    // Create timer that kicks people out of a new Chat if they haven't posted within an amount of time (or take the photo/video before entering Chat), or keep updating their location.
    // Find a way to add to existing snapshot - then send that snapshot to DirectMentions from Map. Also, prevent reloading everything after restart when user paginated (also save scroll position).
    // After clicking on a DM and going to that Chat, allow user to find that same shape on the map.
    // Make recyclerView load faster, possibly by adding layouts for all video/picture and then adding them when possible. Also, fix issue where images / videos are changing size with orientation change. Possible: Send image dimensions to Firebase and set a "null" image of that size.
    // Add ability to add both picture and video to firebase at the same time.

    // Chat very laggy on emulator.
    // Increase point radius? Also, make "creating a point" more accurate to the user's location.
    // Allow users to get "likes".
    // Only be able to see things you've visited - Kenny.
    // Develop an Apple version.
    // Require picture on creating a shape? Also, long press a shape to see a popup of that picture.
    // Add ability to filter recyclerView by type of content (recorded at the scene...).
    // Allow private posts or sharing with specific people.
    // Let users allow specific other users to see their name.
    // Send the shape creator notifications about all comments from a shape?
    // Create "my locations" or "my photos" and see friends' locations / follow friends?
    // Track where user is while taking the original video or picture and make the shape that big?
    // Add some version of the random button, or allow users to click on a circle in a far away area while zoomed out on map.
    // Create a "general chat" where everyone can chat anonymously, maybe with more specific location rooms too? Delete general chat after x amount of time or # of items.
    //// Add ability to add images and video to general chat and Chat from gallery. Distinguish them from media added from location. Github 8/29.
    // Leave messages in locations that users get notified of when they enter the area by adding geo-fencing.
    // Prevent spamming messages.
    // Create widget for faster picture / creating point.
    // Truncate mention in editText to look like userUUID in Chat.
    // Add Firebase rules to mentionPositionPairs for more than 1 child. A "forEach" rule would be best.
    // Add a function for deleting / decreasing position of DMs after deleting / updating messageThreads - problem: need to store the name of the person being DM'ed, but that's a lot of useless information and possibly increases security risk.
    // Load user-specific shared preferences - looks like it might require saving info to database; is this worth it?
    // Increase viral potential - make it easier to share?
    // Update general look of app.
    // Panoramic view, like gMaps.

    // Test on multiple devices.
    // Remember the AC: Origins inspiration. Also, airdrop - create items in the world. Also, gMaps drag and drop.
    // Unit testing.
    // Decrease app size (compress repeating code into methods) / Check on accumulation of size over time.
    // Work on deprecated methods.
    // Check warning messages.
    // Finish setting up Google ads, then add more ads. Then get rid of testID in Chat.
    // Adjust AppIntro.
    // Make sure Firebase has enough bandwidth.
    // Make sure aboutLibraries includes all libraries, and make sure all licenses are fair use (NOT GPL).
    // Make sure the secret stuff is secret.
    // Switch existing values in Firebase (including storage).

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
        dmButton = findViewById(R.id.dmButton);
        createChatButton = findViewById(R.id.createChatButton);
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

            DatabaseReference Dms = FirebaseDatabase.getInstance().getReference().child("Users").child(userEmailFirebase).child("ReceivedDms");
            Dms.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    for (DataSnapshot ds : snapshot.getChildren()) {

                        // Used in addDmsQuery to allow the child to be called the first time if no child exists and prevent double posts if a child exists.
                        dmExists = true;

                        if (!(Boolean) ds.child("seenByUser").getValue()) {

                            dmCounter++;
                        }
                    }

                    dmButton.setCount(dmCounter);
                    addDmsQuery();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    // Don't put error toastMessage here, as it will create a "Permissions Denied" message when no user is signed in.
                }
            });
        }

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
                                Activity.putExtra("fromDms", true);

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

        // Create a point and enter chat.
        createChatButton.setOnClickListener(view -> {

            Log.i(TAG, "createChatButton");

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

                            // Add circle to the map and go to recyclerviewlayout.
                            if (mMap != null) {

                                shapeUUID = UUID.randomUUID().toString();

                                // Check if the user is already signed in.
                                if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) != null) {

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
        });

        chatSelectorSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (chatSelectorSeekBar.getVisibility() == View.VISIBLE) {

                    // Create arrayLists that hold shape information in a useful order.
                    ArrayList<Object> combinedListShapes = new ArrayList<>(overlappingShapesCircleLocation);
                    ArrayList<String> combinedListUUID = new ArrayList<>(overlappingShapesCircleUUID);

                    selectedOverlappingShapeUUID = combinedListUUID.get(chatSelectorSeekBar.getProgress());

                    if (selectedOverlappingShapeUUID != null) {

                        if (circleTemp != null) {

                            circleTemp.remove();
                        }


                        if (combinedListShapes.get(chatSelectorSeekBar.getProgress()) instanceof LatLng) {

                            selectedOverlappingShapeCircleLocation = overlappingShapesCircleLocation.get(chatSelectorSeekBar.getProgress());
                            selectedOverlappingShapeCircleRadius = overlappingShapesCircleRadius.get(chatSelectorSeekBar.getProgress());

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
    private void addDmsQuery() {

        queryDms = FirebaseDatabase.getInstance().getReference().child("Users").child(userEmailFirebase).child("ReceivedDms").limitToLast(1);
        childEventListenerDms = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.i(TAG, "addDmQuery()");

                // If this is the first time calling this eventListener, prevent double posts (as onStart() already added the last item).
                if (firstLoadDms && dmExists) {

                    firstLoadDms = false;
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

        queryDms.addChildEventListener(childEventListenerDms);
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        restarted = true;
        firstLoadCamera = false;
        firstLoadShapes = true;
        firstLoadDms = true;
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

        if (newCircle != null) {

            newCircle = null;
        }

        if (circleTemp != null) {

            circleTemp.remove();
        }

        mCircle = null;
        selectingShape = false;
        userIsWithinShape = false;

        overlappingShapesUUID = new ArrayList<>();
        overlappingShapesCircleUUID = new ArrayList<>();
        overlappingShapesCircleLocation = new ArrayList<>();
        overlappingShapesCircleRadius = new ArrayList<>();

        // Get rid of the chatSelectorSeekBar.
        if (chatSelectorSeekBar.getVisibility() != View.GONE) {

            chatSelectorSeekBar.setVisibility(View.GONE);
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
                        .setPositiveButton("OK", (dialog, i) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
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

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    protected void onStop() {

        Log.i(TAG, "onStop()");

        if (mapTypeButton != null) {

            mapTypeButton.setOnClickListener(null);
        }

        if (settingsButton != null) {

            settingsButton.setOnClickListener(null);
        }

        if (dmButton != null) {

            dmButton.setOnClickListener(null);
        }

        if (queryDms != null) {

            queryDms.removeEventListener(childEventListenerDms);
            queryDms = null;
        }

        if (childEventListenerDms != null) {

            childEventListenerDms = null;
        }

        if (queryShapesPoints != null) {

            queryShapesPoints.removeEventListener(childEventListenerShapesPoints);
            queryShapesPoints = null;
        }

        if (childEventListenerShapesPoints != null) {

            childEventListenerShapesPoints = null;
        }

        if (createChatButton != null) {

            createChatButton.setOnClickListener(null);
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
                        .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                Request_User_Location_Code))
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

        // Go to Chat.java when clicking on a circle.
        mMap.setOnCircleClickListener(circle -> {

            // If the user tries to click on a circle that is not a circleTemp while circleTemp exists, return.
            if (chatSelectorSeekBar.getVisibility() == View.VISIBLE && (circle.getTag() != selectedOverlappingShapeUUID)) {

                Log.i(TAG, "onMapReadyAndRestart() -> onCircleClick -> Selected circle is not a circleTemp. Resetting and returning");

                if (circleTemp != null) {

                    circleTemp.remove();
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
                }

                overlappingShapesUUID = new ArrayList<>();
                overlappingShapesCircleUUID = new ArrayList<>();
                overlappingShapesCircleLocation = new ArrayList<>();
                overlappingShapesCircleRadius = new ArrayList<>();

                chatSelectorSeekBar.setVisibility(View.GONE);
                chatSelectorSeekBar.setProgress(0);

                chatsSize = 0;

                // Get rid of new shapes if the user clicks away from them.
                if (newCircle != null) {

                    newCircle.remove();

                    newCircle = null;
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

                // Generate a UUID, as the shape is new.
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

                // Remove the circle and markers.
                if (newCircle != null) {

                    newCircle.remove();

                    newCircle = null;
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
                                if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) != null) {

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

            ArrayList<String> combinedListUUID = new ArrayList<>(overlappingShapesCircleUUID);

            selectedOverlappingShapeUUID = combinedListUUID.get(chatSelectorSeekBar.getProgress());

            if (selectedOverlappingShapeUUID != null) {

                if (circleTemp != null) {

                    circleTemp.remove();
                }

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

                Log.e(TAG, "onMapReadyAndRestart() -> onCircleClick -> selectedOverlappingShapeUUID == null");
                mMap.getUiSettings().setScrollGesturesEnabled(true);
                loadingIcon.setVisibility(View.GONE);
                toastMessageLong("An error occurred.");
            }

            // At this point, chatsSize > 1 so set the chatSelectorSeekBar to VISIBLE.
            chatSelectorSeekBar.setMax(chatsSize - 1);
            chatSelectorSeekBar.setProgress(0);
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
                    }
                }

                overlappingShapesUUID = new ArrayList<>();
                overlappingShapesCircleUUID = new ArrayList<>();
                overlappingShapesCircleLocation = new ArrayList<>();
                overlappingShapesCircleRadius = new ArrayList<>();

                chatSelectorSeekBar.setVisibility(View.GONE);
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
                                    if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) != null) {

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

            // Get rid of new shapes if the user clicks away from them.
            if (newCircle != null) {

                newCircle.remove();

                newCircle = null;
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

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        int id = menuItem.getItemId();

        if (id == R.id.roadmap) {

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
        } else if (id == R.id.satellite) {

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
        } else if (id == R.id.hybrid) {

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
        } else if (id == R.id.terrain) {

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
        }

        return false;
    }

    private void adjustMapColors() {

        Log.i(TAG, "adjustMapColors()");

        loadingIcon.setVisibility(View.VISIBLE);

        mMap.clear();

        // Change button color depending on map type.
        if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

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
        } else {

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
        }

        loadingIcon.setVisibility(View.GONE);

        newCircle = null;

        // Create a circleTemp or polygonTemp if one already exists.
        if (chatSelectorSeekBar.getVisibility() == View.VISIBLE) {

            // Create arrayLists that hold shape information in a useful order.
            ArrayList<Object> combinedListShapes = new ArrayList<>();
            ArrayList<String> combinedListUUID = new ArrayList<>();

            selectedOverlappingShapeUUID = combinedListUUID.get(chatSelectorSeekBar.getProgress());

            if (selectedOverlappingShapeUUID != null) {

                if (circleTemp != null) {

                    circleTemp.remove();
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
                    }
                }
            }
        }
    }

    private void loadShapes() {

        // Prevent resetting and reloading everything if this is already the state.
        if (firstLoadShapes || restarted || newCameraCoordinates || mapChanged) {

            Log.i(TAG, "loadShapes()");

            loadingIcon.setVisibility(View.VISIBLE);

            // Don't load more than 7 areas at a time.
            if (loadedCoordinates.size() == 7) {

                mMap.clear();
                loadedCoordinates.clear();
                mapCleared = true;
            }

            DatabaseReference firebaseShapes = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Points");

            clearMap();

            if (!loadedCoordinates.contains(newNearLeft) || mapCleared || restarted) {

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

            if (!loadedCoordinates.contains(newFarLeft) || mapCleared || (!loadedCoordinates.contains(newFarLeft) && restarted)) {

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

            if (!loadedCoordinates.contains(newNearRight) || mapCleared || (!loadedCoordinates.contains(newNearRight) && restarted)) {

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

            if (!loadedCoordinates.contains(newFarRight) || mapCleared || (!loadedCoordinates.contains(newFarRight) && restarted)) {

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

            mapChanged = false;

            oldNearLeft = newNearLeft;
            oldFarLeft = newFarLeft;
            oldNearRight = newNearRight;
            oldFarRight = newFarRight;
            newCameraCoordinates = false;
        }
    }

    private void loadShapesODC(DataSnapshot snapshot) {

        loadingIcon.setVisibility(View.VISIBLE);

        for (DataSnapshot ds : snapshot.getChildren()) {

            if (ds.child("circleOptions").exists()) {

                // Shape is a circle.

                circleCenterListForMapChange.add(new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue()));
                circleRadiusArrayList.add(((Number) (Objects.requireNonNull(ds.child("circleOptions/radius").getValue()))).doubleValue());
                circleUUIDListForMapChange.add((String) ds.child("shapeUUID").getValue());

                // Load different colored shapes depending on the map type.
                if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

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

                    // Set the Tag using the UUID in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                    shapeUUID = (String) ds.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                } else {

                    // Purple circle.

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

                    // Set the Tag using the UUID in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                    shapeUUID = (String) ds.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                }
            }
        }

        loadingIcon.setVisibility(View.GONE);
    }

    // Change to .limitToLast(1) to cut down on data usage. Otherwise, EVERY child at this node will be downloaded every time the child is updated.
    private void addShapesQuery() {

        queryShapesPoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Points").limitToLast(1);

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

        queryShapesPoints.addChildEventListener(childEventListenerShapesPoints);
    }

    private void addShapesQueryPoints(DataSnapshot snapshot) {

        if (snapshot.child("circleOptions").exists()) {

            // Shape is a circle.

            circleCenterListForMapChange.add(new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue()));
            circleRadiusArrayList.add(((Number) (snapshot.child("circleOptions/radius").getValue())).doubleValue());
            circleUUIDListForMapChange.add((String) snapshot.child("shapeUUID").getValue());

            // Load different colored shapes depending on the map type.
            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

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

                // Set the Tag using the UUID in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                circle.setTag(shapeUUID);
            } else {

                // Purple circle.

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

                // Set the Tag using the UUID in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                circle.setTag(shapeUUID);
            }
        }

        loadingIcon.setVisibility(View.GONE);
    }

    private void clearMap() {

        Log.i(TAG, "clearMap()");

        mMap.clear();

        // Remove the circle and markers.
        if (newCircle != null) {

            newCircle = null;
        }

        waitingForShapeInformationToProcess = false;
        waitingForClicksToProcess = false;
        selectingShape = false;
        chatsSize = 0;

        overlappingShapesUUID = new ArrayList<>();
        overlappingShapesCircleUUID = new ArrayList<>();
        overlappingShapesCircleLocation = new ArrayList<>();
        overlappingShapesCircleRadius = new ArrayList<>();

        // Get rid of the chatSelectorSeekBar.
        if (chatSelectorSeekBar.getVisibility() != View.GONE) {

            chatSelectorSeekBar.setVisibility(View.GONE);
        }

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