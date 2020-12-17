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
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
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
import java.util.UUID;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class Map extends FragmentActivity implements
        OnMapReadyCallback,
        LocationListener,
        PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "Map";
    private GoogleMap mMap;
    private static final int Request_User_Location_Code = 99;
    private Circle circleTemp;
    private ChildEventListener childEventListenerDms, childEventListenerCircles;
    private String userEmailFirebase, shapeUUID, circleTempUUID;
    private Button circleButton, mapTypeButton, settingsButton;
    private PopupMenu popupMapType;
    private boolean locationProviderDisabled = false, firstLoadCamera = true, firstLoadShapes = true, firstLoadDms = true, dmExists = false, cameraMoved = false, waitingForBetterLocationAccuracy = false, badAccuracy = false, restarted = false, newCameraCoordinates = false, mapCleared = false;
    private final ArrayList<String> circleUUIDs = new ArrayList<>();
    private final ArrayList<LatLng> circleCenters = new ArrayList<>();
    private int dmCounter = 0, newNearLeftLat, newNearLeftLon;
    private Toast longToast;
    private View loadingIcon;
    private CounterFab dmButton;
    private LocationManager locationManager;
    private Query queryDms, queryCircles;
    private Pair<Integer, Integer> oldNearLeft, oldFarLeft, oldNearRight, oldFarRight, newNearLeft, newFarLeft, newNearRight, newFarRight;
    private final List<Pair<Integer, Integer>> loadedCoordinates = new ArrayList<>();

    // Switch from initChatAdapter() to notifyChatAdapter() to increase speed? Generally, make Chat load faster, especially if there are multiple ClickableSpans
    // Make scrollToPosition work in Chat after a restart. Also prevent reloading Chat and DMs every time app restarts.
    // Find a way to not clear and reload map every time user returns from clicking a shape. Same with DM notification.
    // Prevent data scraping (hide email addresses and other personal information).
    // Create timer that kicks people out of a new Chat if they haven't posted within an amount of time (or take the photo/video before entering Chat), or keep updating their location. Or have them take media before entering chat and have the media being sent to Firebase create the chat.
    // Find a way to add to existing snapshot - then send that snapshot to DirectMentions from Map. Also, prevent reloading everything after restart when user paginated (also save scroll position).
    // After clicking on a DM and going to that Chat, allow user to find that same shape on the map.
    // Make recyclerView load faster, possibly by adding layouts for all video/picture and then adding them when possible. Also, fix issue where images / videos are changing size with orientation change. Possible: Send image dimensions to Firebase and set a "null" image of that size.

    // Chat very laggy on emulator.
    // Require picture on creating a shape? Also, long press a shape to see a popup of that picture.
    // Increase point radius? Create a variable with the point's radius and use that instead of "1" to future-proof changes. Also, make "creating a point" more accurate to the user's location.
    // Deal with deprecated methods.
    // Allow users to get "likes".
    // Only be able to see things you've visited - Kenny.
    // Develop an Apple version.
    // Add ability to add both picture and video to firebase at the same time.
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
    // Increase speed of checking whether user is inside a circle when clicking the circleButton, as it currently cycles through all circles. Also, allow user to choose which circle they enter?
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
        circleButton = findViewById(R.id.circleButton);
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

                                if (!locationProviderDisabled) {

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
                            } else {

                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong("Enable your location provider and try again.");
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

                                if (!locationProviderDisabled) {

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
                            } else {

                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong("Enable your location provider and try again.");
                            }
                        });
            } else {

                checkLocationPermissions();
            }
        });

        // Create a point and enter chat.
        circleButton.setOnClickListener(view -> {

            Log.i(TAG, "circleButton");

            // Check location permissions.
            if (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                loadingIcon.setVisibility(View.VISIBLE);

                // Create a point and to to Chat.java or SignIn.java.
                FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(Map.this, location -> {

                            // Add circle to the map and go to Chat.
                            if (location != null) {

                                if (!locationProviderDisabled) {

                                    // This prevent the user from entering a circle before the map has adjusted to their location the first time.
                                    if (!mMap.isMyLocationEnabled()) {

                                        loadingIcon.setVisibility(View.GONE);
                                        return;
                                    }

                                    // If user is within a circle, enter it. Else, cycle through all circles that are 2 meters away and enter the closest one. Else, enter a new one.
                                    float[] oldDistance = new float[2];
                                    oldDistance[0] = 2f;
                                    LatLng latLng = null;
                                    String uuid = null;
                                    for (int i = 0; i < circleCenters.size(); i++) {

                                        float[] newDistance = new float[2];
                                        Location.distanceBetween(circleCenters.get(i).latitude, circleCenters.get(i).longitude, location.getLatitude(), location.getLongitude(), newDistance);

                                        if (newDistance[0] <= 1) {

                                            enterCircle(location, circleCenters.get(i), circleUUIDs.get(i), false, true);

                                            return;
                                        } else if (newDistance[0] <= oldDistance[0]) {

                                            oldDistance[0] = newDistance[0];
                                            latLng = circleCenters.get(i);
                                            uuid = circleUUIDs.get(i);
                                        }
                                    }

                                    // latLng and uuid will be null if it is a new circle, and newShape will be true.
                                    enterCircle(location, latLng, uuid, latLng == null, true);
                                } else {

                                    loadingIcon.setVisibility(View.GONE);
                                    toastMessageLong("Enable your location provider and try again.");
                                }
                            } else {

                                loadingIcon.setVisibility(View.GONE);
                                Log.e(TAG, "onStart() -> creatChatButton -> location == null");
                                toastMessageLong("An error occurred: your location is null.");
                            }
                        });
            } else {

                checkLocationPermissions();
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

        // Need to call clear because shapes will be loaded again in onStart(). This should change in the future to cut down on data usage.
        mMap.clear();

        restarted = true;
        firstLoadCamera = false;
        firstLoadShapes = true;
        firstLoadDms = true;
        dmCounter = 0;
        newCameraCoordinates = false;
        loadedCoordinates.clear();
        oldNearLeft = null;
        oldFarLeft = null;
        oldNearRight = null;
        oldFarRight = null;
        circleTemp = null;
        circleTempUUID = null;

        cancelToasts();

        if (dmButton != null) {

            dmButton.setCount(0);
        }

        // Clear map before adding new Firebase circles in onStart() to prevent overlap.
        // Set shape to null so changing chatSizeSeekBar in onStart() will create a circle and circleButton will reset itself.
        if (mMap != null) {

            mMap.getUiSettings().setScrollGesturesEnabled(true);

            // Cut down on code by using one method for the shared code from onMapReady() and onRestart().
            onMapReadyAndRestart();
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
        }

        if (locationManager != null) {

            locationManager.removeUpdates(this);
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

        if (queryCircles != null) {

            queryCircles.removeEventListener(childEventListenerCircles);
            queryCircles = null;
        }

        if (childEventListenerCircles != null) {

            childEventListenerCircles = null;
        }

        if (circleButton != null) {

            circleButton.setOnClickListener(null);
        }

        if (mMap != null) {

            mMap.setOnCircleClickListener(null);
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

            // If the user clicks on a circle that's already highlighted, enter that circle.
            if (circle.getFillColor() != 0) {

                // Check location permissions.
                if (ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    loadingIcon.setVisibility(View.VISIBLE);

                    // Create a point and to to Chat.java or SignIn.java.
                    FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(Map.this);

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, location -> {

                                // Add circle to the map and go to Chat.
                                if (location != null) {

                                    if (!locationProviderDisabled) {

                                        // Check distance to circleTemp and enter it.
                                        float[] distance = new float[2];
                                        Location.distanceBetween(circleTemp.getCenter().latitude, circleTemp.getCenter().longitude, location.getLatitude(), location.getLongitude(), distance);

                                        // If distance <= 2, enterCircle(location, circleTemp.getCenter(), false, true). Else, enterCircle(location, circleTemp.getCenter(), false, false).
                                        enterCircle(location, null, null, false, distance[0] <= 2);
                                    } else {

                                        loadingIcon.setVisibility(View.GONE);
                                        toastMessageLong("Enable your location provider and try again.");
                                    }
                                } else {

                                    loadingIcon.setVisibility(View.GONE);
                                    Log.e(TAG, "onStart() -> creatChatButton -> location == null");
                                    toastMessageLong("An error occurred: your location is null.");
                                }
                            });
                } else {

                    checkLocationPermissions();
                }

                return;
            }

            if (circleTemp != null) {

                circleTemp.remove();
                circleTemp = null;
            }

            if (mMap.getMapType() == 2 || mMap.getMapType() == 4) {

                circleTemp = mMap.addCircle(
                        new CircleOptions()
                                .center(circle.getCenter())
                                .clickable(true)
                                .fillColor(Color.argb(100, 255, 255, 0))
                                .radius(circle.getRadius())
                                .strokeColor(Color.rgb(255, 255, 0))
                                .strokeWidth(3f)
                                .zIndex(2)
                );
            } else {

                circleTemp = mMap.addCircle(
                        new CircleOptions()
                                .center(circle.getCenter())
                                .clickable(true)
                                .fillColor(Color.argb(100, 255, 0, 255))
                                .radius(circle.getRadius())
                                .strokeColor(Color.rgb(255, 0, 255))
                                .strokeWidth(3f)
                                .zIndex(2)
                );
            }

            circleTempUUID = circle.getTag().toString();
            loadingIcon.setVisibility(View.GONE);
        });

        mMap.setOnMapClickListener(latLng -> {

            Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> Replacing circles with z = -1 with z-index = 0");

            if (circleTemp != null) {

                circleTemp.remove();
                circleTemp = null;
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

    private void enterCircle(Location userLocation, LatLng circleLatLng, String circleUUID, boolean newShape, boolean userIsWithinShape) {

        Log.i(TAG, "enterCircle()");

        LatLng circleToEnterLatLng = null;
        String circleToEnterUUID = null;
        if (circleLatLng == null && newShape) {
            // New circle.
            circleToEnterLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
            circleToEnterUUID = UUID.randomUUID().toString();
        } else if (circleLatLng == null && !newShape) {
            // User is entering a circle by clicking on it, so circleTemp != null.
            circleToEnterLatLng = circleTemp.getCenter();
            circleToEnterUUID = circleTempUUID;
        } else if (circleLatLng != null && !newShape) {
            // User clicked on the circle button and is entering a circle close to their location.
            circleToEnterLatLng = circleLatLng;
            circleToEnterUUID = circleUUID;
        }

        Intent Activity;

        // Check if the user is already signed in.
        if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) != null) {

            // User signed in.

            Activity = new Intent(Map.this, Navigation.class);

        } else {

            // User NOT signed in.

            Activity = new Intent(Map.this, SignIn.class);
        }

        cancelToasts();

        // Pass this boolean value to Chat.java.
        Activity.putExtra("newShape", newShape);

        // Get a value with 1 decimal point and use it for Firebase.
        double nearLeftPrecisionLat = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double nearLeftLatTemp = (int) (nearLeftPrecisionLat * circleToEnterLatLng.latitude) / nearLeftPrecisionLat;
        nearLeftLatTemp *= 10;
        int shapeLat = (int) nearLeftLatTemp;

        double nearLeftPrecisionLon = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double nearLeftLonTemp = (int) (nearLeftPrecisionLon * circleToEnterLatLng.longitude) / nearLeftPrecisionLon;
        nearLeftLonTemp *= 10;
        int shapeLon = (int) nearLeftLonTemp;

        Activity.putExtra("shapeLat", shapeLat);
        Activity.putExtra("shapeLon", shapeLon);
        // UserLatitude and userLongitude are used in DirectMentions.
        Activity.putExtra("userLatitude", userLocation.getLatitude());
        Activity.putExtra("userLongitude", userLocation.getLongitude());
        // Pass this value to Chat.java to identify the shape.
        Activity.putExtra("shapeUUID", circleToEnterUUID);
        // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
        Activity.putExtra("userIsWithinShape", userIsWithinShape);
        Activity.putExtra("circleLatitude", circleToEnterLatLng.latitude);
        Activity.putExtra("circleLongitude", circleToEnterLatLng.longitude);
        Activity.putExtra("radius", 1.0);

        loadingIcon.setVisibility(View.GONE);

        startActivity(Activity);
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

            circleButton.setBackgroundResource(R.drawable.circle_button);

            settingsButton.setBackgroundResource(R.drawable.ic_more_vert_yellow_24dp);

            dmButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.yellow));

            for (int i = 0; i < circleCenters.size(); i++) {

                Circle circle = mMap.addCircle(
                        new CircleOptions()
                                .center(circleCenters.get(i))
                                .clickable(true)
                                .radius(1.0)
                                .strokeColor(Color.YELLOW)
                                .strokeWidth(3f)
                );

                circle.setTag(circleUUIDs.get(i));
            }
        } else {

            circleButton.setBackgroundResource(R.drawable.circle_button_purple);

            settingsButton.setBackgroundResource(R.drawable.ic_more_vert_purple_24dp);

            dmButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.purple));

            for (int i = 0; i < circleCenters.size(); i++) {

                Circle circle = mMap.addCircle(
                        new CircleOptions()
                                .center(circleCenters.get(i))
                                .clickable(true)
                                .radius(1.0)
                                .strokeColor(Color.rgb(255, 0, 255))
                                .strokeWidth(3f)
                );

                circle.setTag(circleUUIDs.get(i));
            }
        }

        loadingIcon.setVisibility(View.GONE);

        // Create a circleTemp if one already exists.
        if (circleTemp != null) {

            circleTemp.remove();

            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                circleTemp = mMap.addCircle(
                        new CircleOptions()
                                .center(circleTemp.getCenter())
                                .clickable(true)
                                .fillColor(Color.argb(100, 255, 255, 0))
                                .radius(1.0)
                                .strokeColor(Color.rgb(255, 255, 0))
                                .strokeWidth(3f)
                                .zIndex(2)
                );
            } else {

                circleTemp = mMap.addCircle(
                        new CircleOptions()
                                .center(circleTemp.getCenter())
                                .clickable(true)
                                .fillColor(Color.argb(100, 255, 0, 255))
                                .radius(1.0)
                                .strokeColor(Color.rgb(255, 0, 255))
                                .strokeWidth(3f)
                                .zIndex(2)
                );
            }
        }
    }

    private void loadShapes() {

        // Prevent resetting and reloading everything if this is already the state.
        if (firstLoadShapes || restarted || newCameraCoordinates) {

            Log.i(TAG, "loadShapes()");

            loadingIcon.setVisibility(View.VISIBLE);

            // Don't load more than 7 areas at a time.
            if (loadedCoordinates.size() == 7) {

                mMap.clear();
                loadedCoordinates.clear();
                mapCleared = true;
            }

            DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Points");

            mMap.clear();

            if (!loadedCoordinates.contains(newNearLeft) || mapCleared || restarted) {

                loadedCoordinates.add(0, newNearLeft);

                if (loadedCoordinates.size() > 7) {

                    loadedCoordinates.subList(7, loadedCoordinates.size()).clear();
                }

                firebasePoints.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        loadCirclesODC(snapshot);

                        addCirclesQuery();
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

                firebasePoints.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        loadCirclesODC(snapshot);

                        addCirclesQuery();
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

                firebasePoints.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        loadCirclesODC(snapshot);

                        addCirclesQuery();
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

                firebasePoints.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        loadCirclesODC(snapshot);

                        addCirclesQuery();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                        Log.e(TAG, "DatabaseError");
                        loadingIcon.setVisibility(View.GONE);
                        toastMessageLong(databaseError.getMessage());
                    }
                });
            }

            oldNearLeft = newNearLeft;
            oldFarLeft = newFarLeft;
            oldNearRight = newNearRight;
            oldFarRight = newFarRight;
            newCameraCoordinates = false;
        }
    }

    private void loadCirclesODC(DataSnapshot snapshot) {

        loadingIcon.setVisibility(View.VISIBLE);

        for (DataSnapshot ds : snapshot.getChildren()) {

            if (ds.child("circleOptions").exists()) {

                // Shape is a circle.

                LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());

                circleCenters.add(center);
                circleUUIDs.add((String) ds.child("shapeUUID").getValue());

                // Load different colored circles depending on the map type.
                if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                    // Yellow circle.

                    Circle circle = mMap.addCircle(
                            new CircleOptions()
                                    .center(center)
                                    .clickable(true)
                                    .radius(1.0)
                                    .strokeColor(Color.YELLOW)
                                    .strokeWidth(3f)
                    );

                    // Set the Tag using the UUID in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                    shapeUUID = (String) ds.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                } else {

                    // Purple circle.

                    Circle circle = mMap.addCircle(
                            new CircleOptions()
                                    .center(center)
                                    .clickable(true)
                                    .radius(1.0)
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
    private void addCirclesQuery() {

        queryCircles = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Points").limitToLast(1);

        childEventListenerCircles = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.i(TAG, "addCirclesQuery()");

                // If this is the first time calling this eventListener, prevent double posts Since all childEventListenerCircles use firstLoadShapes, set the other boolean to false.
                if (firstLoadShapes) {

                    return;
                }

                addCirclesQuery(snapshot);
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

        queryCircles.addChildEventListener(childEventListenerCircles);
    }

    private void addCirclesQuery(DataSnapshot snapshot) {

        if (snapshot.child("circleOptions").exists()) {

            // Shape is a circle.

            LatLng center = new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue());

            // Load different colored circles depending on the map type.
            if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                // Yellow circle.

                Circle circle = mMap.addCircle(
                        new CircleOptions()
                                .center(center)
                                .clickable(true)
                                .radius(1.0)
                                .strokeColor(Color.YELLOW)
                                .strokeWidth(3f)
                );

                // Set the Tag using the UUID in Firebase. Value is sent to Chat.java in onMapReady() to identify the chatCircle.
                shapeUUID = (String) snapshot.child("shapeUUID").getValue();

                circle.setTag(shapeUUID);
            } else {

                // Purple circle.

                Circle circle = mMap.addCircle(
                        new CircleOptions()
                                .center(center)
                                .clickable(true)
                                .radius(1.0)
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