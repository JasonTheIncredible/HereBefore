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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class Map extends FragmentActivity implements
        OnMapReadyCallback,
        LocationListener,
        PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "Map";
    private static final int Request_User_Location_Code = 42, Request_ID_Take_Photo = 69, Request_ID_Record_Video = 420, Update_Interval = 0, Fastest_Interval = 0;
    private GoogleMap mMap;
    private Circle circleTemp;
    private ChildEventListener childEventListenerNearLeft, childEventListenerFarLeft, childEventListenerNearRight, childEventListenerFarRight;
    private String circleTempUUID, circleTempUUIDTemp, lastKnownKey;
    protected static String imageFile, videoFile;
    private Button recordButton, mapTypeButton;
    private ImageButton settingsButton, dmsButton;
    private PopupMenu popupMapType;
    private boolean firstLoadCamera = true, cameraMoved = false, waitingForBetterLocationAccuracy = false, badAccuracy = false, restarted = false, mapCleared = false, checkPermissionsPicture;
    private final ArrayList<String> circleUUIDsAL = new ArrayList<>();
    private final ArrayList<LatLng> circleCentersAL = new ArrayList<>();
    private int newNearLeftLat, newNearLeftLon, newFarLeftLat, newFarLeftLon, newNearRightLat, newNearRightLon, newFarRightLat, newFarRightLon;
    private Toast longToast;
    private View loadingIcon, rootView;
    private Query queryNearLeft, queryFarLeft, queryNearRight, queryFarRight;
    private Pair<Integer, Integer> newNearLeft, newFarLeft, newNearRight, newFarRight;
    private List<Pair<Integer, Integer>> loadedCoordinates;
    private LocationManager locationManager;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        // Clear the cache.
        deleteDirectory(this.getCacheDir());

        // This will be called if user opens app using a notification.
        onNewIntent(getIntent());

        // Show the intro if the user has not yet seen it.
        boolean showIntro = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.prefShowIntro), true);

        if (showIntro) {

            Intent Activity = new Intent(this, MyAppIntro.class);
            startActivity(Activity);
            finish();
        }

        setContentView(R.layout.map);

        rootView = findViewById(R.id.rootViewMap);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.activity_maps);
        if (mapFragment != null) {

            mapFragment.getMapAsync(this);
        } else {

            Log.e(TAG, "onCreate() -> mapFragment == null");
            showMessageLong("An error occurred while loading the map.");
        }

        mapTypeButton = findViewById(R.id.mapTypeButton);
        settingsButton = findViewById(R.id.settingsButton);
        dmsButton = findViewById(R.id.dmsButton);
        recordButton = findViewById(R.id.recordButton);
        loadingIcon = findViewById(R.id.loadingIcon);

        loadedCoordinates = new ArrayList<>();
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
        } else {

            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            String provider = LocationManager.NETWORK_PROVIDER;
            if (locationManager != null) {

                locationManager.requestLocationUpdates(provider, Fastest_Interval, 0, this);
                startLocationUpdates();
            } else {

                Log.e(TAG, "onResume() -> locationManager == null");
                showMessageLong("Error retrieving your location.");
            }
        }

        // Check if the user is logged in. If true, make the settings button visible.
        boolean loggedIn = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.prefSignOut), false);

        if (loggedIn) {

            settingsButton.setVisibility(View.VISIBLE);
            dmsButton.setVisibility(View.VISIBLE);
        } else {

            settingsButton.setVisibility(View.GONE);
            dmsButton.setVisibility(View.GONE);
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

            goToNextActivity(null, null, false, "settings");
        });

        // Go to DMs.
        dmsButton.setOnClickListener(v -> {

            Log.i(TAG, "onStart() -> dmButton -> onClick");

            goToNextActivity(null, null, false, "dms");
        });

        // Take a photo. Create chat if one doesn't exist.
        recordButton.setOnClickListener(view -> {

            Log.i(TAG, "onStart() -> recordButton -> onClick");

            if (checkPermissionsPicture()) {

                cancelToasts();

                startActivityTakePhoto();
            }
        });

        // Record a video. Create chat if one doesn't exist.
        recordButton.setOnLongClickListener(view -> {

            Log.i(TAG, "onStart() -> recordButton -> onLongClick");

            if (checkPermissionsVideo()) {

                cancelToasts();

                startActivityRecordVideo();
            }

            return false;
        });
    }

    private boolean checkPermissionsPicture() {

        Log.i(TAG, "checkPermissionsPicture()");

        // boolean to loop back to this point and start activity after xPermissionAlertDialog.
        checkPermissionsPicture = true;

        int permissionCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {

            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (!listPermissionsNeeded.isEmpty()) {

            requestPermissions(listPermissionsNeeded.toArray(new String[0]), Request_ID_Take_Photo);
            return false;
        }

        return true;
    }

    private boolean checkPermissionsVideo() {

        Log.i(TAG, "checkPermissionsVideo()");

        // boolean to loop back to this point and start activity after xPermissionAlertDialog.
        checkPermissionsPicture = false;

        int permissionCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int permissionRecordAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {

            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (permissionRecordAudio != PackageManager.PERMISSION_GRANTED) {

            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!listPermissionsNeeded.isEmpty()) {

            requestPermissions(listPermissionsNeeded.toArray(new String[0]), Request_ID_Record_Video);
            return false;
        }

        return true;
    }

    private void startActivityTakePhoto() {

        Log.i(TAG, "startActivityTakePhoto()");

        // Permission was granted, yay! Do the task you need to do.
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(this.getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {

                photoFile = createImageFile();
            } catch (IOException ex) {

                // Error occurred while creating the File
                showMessageLong(ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                Uri imageURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
                startActivityForResult(cameraIntent, 3);
            }
        }
    }

    private void startActivityRecordVideo() {

        Log.i(TAG, "startActivityRecordVideo()");

        // Permission was granted, yay! Do the task you need to do.
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (videoIntent.resolveActivity(this.getPackageManager()) != null) {

            // Create the File where the video should go
            File videoFile = null;
            try {

                videoFile = createVideoFile();
            } catch (IOException ex) {

                // Error occurred while creating the File
                showMessageLong(ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (videoFile != null) {

                Uri videoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        videoFile);
                videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI);
                // Limit the amount of time a video can be recorded (in seconds).
                videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
                startActivityForResult(videoIntent, 4);
            }
        }
    }

    private void cameraPermissionAlertAsync(Boolean checkPermissionsPicture) {

        AlertDialog.Builder alert;
        alert = new AlertDialog.Builder(this);

        HandlerThread cameraHandlerThread = new HandlerThread("CameraHandlerThread");
        cameraHandlerThread.start();
        Handler mHandler = new Handler(cameraHandlerThread.getLooper());
        Runnable runnable = () ->

                alert.setCancelable(false)
                        .setTitle(R.string.camera_permission_required)
                        .setMessage(R.string.camera_permission_explanation)
                        .setPositiveButton("OK", (dialogInterface, i) -> {

                            if (checkPermissionsPicture) {

                                checkPermissionsPicture();
                            } else {

                                checkPermissionsVideo();
                            }
                        })
                        .create()
                        .show();

        mHandler.post(runnable);
    }

    private void audioPermissionAlertAsync(Boolean checkPermissionsPicture) {

        AlertDialog.Builder alert;
        alert = new AlertDialog.Builder(this);

        HandlerThread audioPermissionHandlerThread = new HandlerThread("audioHandlerThread");
        audioPermissionHandlerThread.start();
        Handler mHandler = new Handler(audioPermissionHandlerThread.getLooper());
        Runnable runnable = () ->

                alert.setCancelable(false)
                        .setTitle(R.string.audio_permission_required)
                        .setMessage(R.string.audio_permission_explanation)
                        .setPositiveButton("OK", (dialogInterface, i) -> {

                            if (checkPermissionsPicture) {

                                checkPermissionsPicture();
                            } else {

                                checkPermissionsVideo();
                            }
                        })
                        .create()
                        .show();

        mHandler.post(runnable);
    }

    private File createImageFile() throws IOException {

        Log.i(TAG, "createImageFile()");

        // Create an image file name
        String fileName = "HereBefore_" + System.currentTimeMillis();
        File storageDir = this.getCacheDir();

        File image = File.createTempFile(
                fileName,  /* prefix */
                ".jpeg",         /* suffix */
                storageDir      /* directory */
        );

        // Pass this info to Chat.
        imageFile = image.toString();

        return image;
    }

    private File createVideoFile() throws IOException {

        Log.i(TAG, "createVideoFile()");

        // Create a video file name
        String fileName = "HereBefore_" + System.currentTimeMillis();
        File storageDir = this.getCacheDir();

        File video = File.createTempFile(
                fileName,  /* prefix */
                ".mp4",         /* suffix */
                storageDir      /* directory */
        );

        // Pass this info to Chat.
        videoFile = video.toString();

        return video;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            loadingIcon.setVisibility(View.VISIBLE);
        } else {

            imageFile = null;
            videoFile = null;
            return;
        }

        goToNextActivity(null, null, true, "chat");
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        restarted = true;

        // Clear map before adding new Firebase circles in onStart() to prevent overlap.
        // Set shape to null so changing chatSizeSeekBar in onStart() will create a circle and recordButton will reset itself.
        if (mMap != null) {

            // Cut down on code by using one method for the shared code from onMapReady and onRestart.
            onMapReadyAndRestart();
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.i(TAG, "onResume()");

        // If the user backs out of Chat too quickly, circleTemp will not be cleared as onStop and onStart are not called.
        if (circleTemp != null) {

            circleTemp.remove();
        }

        circleTempUUID = null;
        circleTemp = null;

        // Enable buttons, unless the user took a photo or video and is entering the next activity.
        if ((getIntent().getExtras() != null && getIntent().getExtras().getString("senderUserUUID") == null) || (getIntent().getExtras() == null && imageFile == null && videoFile == null)) {

            recordButton.setEnabled(true);
            dmsButton.setEnabled(true);
            settingsButton.setEnabled(true);
        } else {

            loadingIcon.setVisibility(View.VISIBLE);
            recordButton.setEnabled(false);
            dmsButton.setEnabled(false);
            settingsButton.setEnabled(false);
        }
    }

    @Override
    protected void onStop() {

        Log.i(TAG, "onStop()");

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
        }

        if (mFusedLocationClient != null) {

            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

        if (mapTypeButton != null) {

            mapTypeButton.setOnClickListener(null);
        }

        if (settingsButton != null) {

            settingsButton.setOnClickListener(null);
        }

        if (dmsButton != null) {

            dmsButton.setOnClickListener(null);
        }

        if (queryNearLeft != null) {

            queryNearLeft.removeEventListener(childEventListenerNearLeft);
            queryNearLeft = null;
        }

        if (queryFarLeft != null) {

            queryFarLeft.removeEventListener(childEventListenerFarLeft);
            queryFarLeft = null;
        }

        if (queryNearRight != null) {

            queryNearRight.removeEventListener(childEventListenerNearRight);
            queryNearRight = null;
        }

        if (queryFarRight != null) {

            queryFarRight.removeEventListener(childEventListenerFarRight);
            queryFarRight = null;
        }

        if (recordButton != null) {

            recordButton.setOnClickListener(null);
            recordButton.setOnLongClickListener(null);
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

        // Cut down on code by using one method for the shared code from onMapReady and onRestart.
        onMapReadyAndRestart();
    }

    // If the user clicks on a notification, this should trigger.
    @Override
    public void onNewIntent(Intent intent) {

        Log.i(TAG, "onNewIntent()");

        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();
        if (extras != null) {

            if (loadingIcon != null) {

                loadingIcon.setVisibility(View.VISIBLE);
            }

            if (recordButton != null) {

                recordButton.setEnabled(false);
            }

            if (dmsButton != null) {

                dmsButton.setEnabled(false);
            }

            if (settingsButton != null) {

                settingsButton.setEnabled(false);
            }

            // Check location permissions.
            if (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                if (mFusedLocationClient == null) {

                    mFusedLocationClient = getFusedLocationProviderClient(Map.this);
                }

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(Map.this, location -> {

                            if (location != null) {

                                String circleUUID = extras.getString("shapeUUID");

                                // If the user clicks on a notification while the app is running, lat !=0 && lon != 0, so the if statement will run.
                                double lat = extras.getDouble("lat");
                                double lon = extras.getDouble("lon");
                                if (lat != 0 && lon != 0 && circleUUID != null) {

                                    goToNextActivity(new LatLng(lat, lon), circleUUID, false, "chat");
                                    return;
                                }

                                if (extras.getString("lat") != null && extras.getString("lon") != null) {

                                    // If the code got to this point, the user clicked on a notification while the app was closed.
                                    double latDouble = Double.parseDouble(Objects.requireNonNull(extras.getString("lat")));
                                    double lonDouble = Double.parseDouble(Objects.requireNonNull(extras.getString("lon")));
                                    if (circleUUID != null) {

                                        goToNextActivity(new LatLng(latDouble, lonDouble), circleUUID, false, "chat");
                                    }
                                }
                            }
                        });
            } else {

                checkLocationPermissions();
            }
        }
    }

    // Cut down on code by using one method for the shared code from onMapReady and onRestart.
    private void onMapReadyAndRestart() {

        Log.i(TAG, "onMapReadyAndRestart()");

        if (restarted) {

            cameraIdle();
        }

        updatePreferences();

        // Go to Chat.java when clicking on a circle.
        mMap.setOnCircleClickListener(circle -> {

            // If the user clicks on a circle that's already highlighted, enter that circle.
            if (circle.getFillColor() != 0) {

                circleTempUUID = circleTempUUIDTemp;

                // Check location permissions.
                if (ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    loadingIcon.setVisibility(View.VISIBLE);

                    // Create a point and go to Chat.java or SignIn.java.
                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(Map.this, location -> {

                                // Add circle to the map and go to Chat.
                                if (location != null) {

                                    goToNextActivity(circleTemp.getCenter(), circleTempUUID, false, "chat");
                                } else {

                                    loadingIcon.setVisibility(View.GONE);
                                    Log.e(TAG, "onStart() -> onCircleClick -> location == null");
                                    showMessageLong("An error occurred: your location is null.");
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

            if (circle.getTag() == null) {

                showMessageLong("An error occurred. Please try again later.");
                return;
            }

            // Can't set circleTempUUID here, as user might highlight a circle, then click on a notification and accidentally enter the highlighted circle from the notification.
            circleTempUUIDTemp = circle.getTag().toString();
            loadingIcon.setVisibility(View.GONE);
        });

        mMap.setOnMapClickListener(latLng -> {

            Log.i(TAG, "onMapReadyAndRestart() -> onMapClick -> Replacing circles with z = -1 with z-index = 0");

            if (circleTemp != null) {

                circleTemp.remove();
                circleTemp = null;
            }
        });

        // Updates the boolean value for onLocationChanged to prevent updating the camera position if the user has already changed it manually.
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

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Request_User_Location_Code);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.i(TAG, "onRequestPermissionsResult()");

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case Request_User_Location_Code: {

                if (grantResults.length > 0) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                        locationPermissionAlertAsync();
                    } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        // Permission was granted, yay! Do the location-related task you need to do.
                        if (ContextCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {

                            startLocationUpdates();
                        }
                    } else {

                        showMessageLong("Location permission is required. You may need to enable it manually through the Android settings menu.");
                    }
                }

                break;
            }

            case Request_ID_Take_Photo: {

                HashMap<String, Integer> perms = new HashMap<>();
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);

                if (grantResults.length > 0) {

                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);

                    Integer cameraPermissions = perms.get(Manifest.permission.CAMERA);

                    if (cameraPermissions != null) {

                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Camera permissions were not granted. Ask again.");

                            cameraPermissionAlertAsync(checkPermissionsPicture);
                        } else if (cameraPermissions == PackageManager.PERMISSION_GRANTED) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Camera permission granted.");
                            // Process the normal workflow.
                            startActivityTakePhoto();
                        } else if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                            showMessageLong("Camera permission is required. You may need to enable it manually through the Android settings menu.");
                        }
                    }
                }

                break;
            }

            case Request_ID_Record_Video: {

                HashMap<String, Integer> perms = new HashMap<>();
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);

                if (grantResults.length > 0) {

                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);

                    Integer cameraPermissions = perms.get(Manifest.permission.CAMERA);
                    Integer audioPermissions = perms.get(Manifest.permission.RECORD_AUDIO);

                    if (cameraPermissions != null && audioPermissions != null) {

                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Camera permissions were not granted. Ask again.");

                            cameraPermissionAlertAsync(checkPermissionsPicture);
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                            Log.d(TAG, "Request_ID_Record_Video -> Audio permissions were not granted. Ask again.");

                            audioPermissionAlertAsync(checkPermissionsPicture);
                        } else if (cameraPermissions == PackageManager.PERMISSION_GRANTED
                                && audioPermissions == PackageManager.PERMISSION_GRANTED) {

                            Log.d(TAG, "Request_ID_Record_Video -> Camera and Record Audio permission granted.");
                            // Process the normal workflow.
                            startActivityRecordVideo();
                        } else if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                            showMessageLong("Camera and Audio permissions are required. You may need to enable them manually through the Android settings menu.");
                        }
                    }
                }

                break;
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
                        .setTitle(R.string.location_permission_required)
                        .setMessage(R.string.location_permission_required_explanation)
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
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /* 1000 = 1 sec */
        locationRequest.setInterval(Update_Interval);

        /* 1000 = 1 sec */
        locationRequest.setFastestInterval(Fastest_Interval);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // New Google API SDK v11 uses getFusedLocationProviderClient(this).
        if (ContextCompat.checkSelfPermission(Map.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mFusedLocationClient = getFusedLocationProviderClient(Map.this);

            mLocationCallback = new LocationCallback() {

                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {

                    onLocationChanged(locationResult.getLastLocation());
                }
            };

            getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, mLocationCallback, Objects.requireNonNull(Looper.myLooper()));
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
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

        Log.i(TAG, "onProviderDisabled()");
        buildAlertNoGPS();
    }

    private void buildAlertNoGPS() {

        AlertDialog.Builder alert;
        alert = new AlertDialog.Builder(this);

        HandlerThread GPSHandlerThread = new HandlerThread("GPSHandlerThread");
        GPSHandlerThread.start();
        Handler mHandler = new Handler(GPSHandlerThread.getLooper());
        Runnable runnable = () ->

                alert.setCancelable(false)
                        .setTitle(R.string.gps_disabled)
                        .setMessage(R.string.gps_disabled_manually)
                        .setPositiveButton("OK", (dialog, i) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                        .create()
                        .show();

        mHandler.post(runnable);
    }

    private void goToNextActivity(LatLng circleLatLng, String circleUUID, boolean newShape, String activity) {

        Log.i(TAG, "goToNextActivity()");

        LatLng circleToEnterLatLng = null;
        String circleToEnterUUID;
        if (!activity.equals("chat")) {

            circleToEnterUUID = null;
        } else if (circleLatLng == null && newShape) {

            // New circle.
            circleToEnterUUID = UUID.randomUUID().toString();
        } else if (circleLatLng == null) {

            // User is entering a circle by clicking on it, so circleTemp != null.
            circleToEnterLatLng = circleTemp.getCenter();
            circleToEnterUUID = circleTempUUID;
        } else {

            // User clicked on the circle button and is entering a circle close to their location, OR user clicked on a notification.
            circleToEnterLatLng = circleLatLng;
            circleToEnterUUID = circleUUID;
        }

        cancelToasts();

        Intent Activity = null;

        // Check if the user is already signed in.
        if (activity.equals("chat")) {

            if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(Map.this) != null) {

                // User signed in.
                Activity = new Intent(Map.this, Navigation.class);
            } else {

                // User NOT signed in.
                Activity = new Intent(Map.this, SignIn.class);
            }

            // Pass this boolean value to Chat.java.
            Activity.putExtra("newShape", newShape);
            if (!newShape) {

                Activity.putExtra("shapeLat", circleToEnterLatLng.latitude);
                Activity.putExtra("shapeLon", circleToEnterLatLng.longitude);
            } else {

                Activity.putExtra("circleCentersAL", circleCentersAL);
                Activity.putExtra("circleUUIDsAL", circleUUIDsAL);
            }

            // Pass this value to Chat.java to identify the shape.
            Activity.putExtra("shapeUUID", circleToEnterUUID);
            Activity.putExtra("imageFile", imageFile);
            Activity.putExtra("videoFile", videoFile);
            Activity.putExtra("lastKnownKey", lastKnownKey);
        } else if (activity.equals("settings") || activity.equals("dms")) {

            Activity = new Intent(Map.this, Navigation.class);
            Activity.putExtra("noChat", true);

            if (activity.equals("dms")) {

                Activity.putExtra("fromDms", true);
            }
        }

        if (Activity != null) {

            // Prevent previous activities from being in the back stack.
            Activity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // If getIntent().getExtras != null, user is entering a circle from a notification and seenByUser needs to be set to true. Else, enter circle like normal.
            if (getIntent().getExtras() != null && getIntent().getExtras().getString("senderUserUUID") != null) {

                String senderUserUUID = getIntent().getExtras().getString("senderUserUUID");
                Activity.putExtra("UUIDToHighlight", senderUserUUID);

                String firebaseUid = FirebaseAuth.getInstance().getUid();

                if (firebaseUid == null) {

                    return;
                }

                // Set "seenByUser" to true so it is not highlighted in the future.
                DatabaseReference Dms = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("ReceivedDms");
                Intent finalActivity = Activity;
                Query DmsQuery = Dms.orderByChild("senderUserUUID").equalTo(senderUserUUID);
                DmsQuery.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot ds : snapshot.getChildren()) {

                            Boolean seenByUser = (Boolean) ds.child("seenByUser").getValue();
                            if (seenByUser != null && !seenByUser) {

                                ds.child("seenByUser").getRef().setValue(true);
                            }

                            // .removeExtra prevents issues when user clicks on a mention, backs into Map, then tries to go to a new circle.
                            getIntent().removeExtra("senderUserUUID");
                            loadingIcon.setVisibility(View.GONE);
                            startActivity(finalActivity);
                            // "return" is not strictly necessary (as there should only be one child), but it keeps the data usage and processing to a minimum in the event of strange behavior.
                            return;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        showMessageLong(error.getMessage());
                    }
                });
            } else {

                recordButton.setEnabled(true);
                dmsButton.setEnabled(true);
                settingsButton.setEnabled(true);

                loadingIcon.setVisibility(View.GONE);

                startActivity(Activity);
            }
        }
    }

    protected void updatePreferences() {

        Log.i(TAG, "updatePreferences()");

        String preferredMapType = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.prefMapType), getResources().getString(R.string.use_hybrid_view));

        if (preferredMapType != null) {

            switch (preferredMapType) {

                case "Use road map view":

                    Log.i(TAG, "updatePreferences() -> Road map view");

                    // Use the "road map" map type if the map is not null.
                    if (mMap != null) {

                        Log.i(TAG, "updatePreferences() -> Road Map");

                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                        adjustMapColors();
                    } else {

                        Log.e(TAG, "updatePreferences() -> Road Map -> mMap == null");
                        showMessageLong("An error occurred while loading the map.");
                    }

                    break;

                case "Use satellite view":

                    Log.i(TAG, "updatePreferences() -> Satellite view");

                    // Use the "satellite" map type if the map is not null.
                    if (mMap != null) {

                        Log.i(TAG, "updatePreferences() -> Satellite Map");

                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                        adjustMapColors();
                    } else {

                        Log.e(TAG, "updatePreferences() -> Satellite Map -> mMap == null");
                        showMessageLong("An error occurred while loading the map.");
                    }

                    break;

                case "Use hybrid view":

                    Log.i(TAG, "updatePreferences() -> Hybrid view");

                    // Use the "hybrid" map type if the map is not null.
                    if (mMap != null) {

                        Log.i(TAG, "updatePreferences() -> Hybrid Map");

                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                        adjustMapColors();
                    } else {

                        Log.e(TAG, "updatePreferences() -> Hybrid Map -> mMap == null");
                        showMessageLong("An error occurred while loading the map.");
                    }

                    break;

                case "Use terrain view":

                    Log.i(TAG, "updatePreferences() -> Terrain view");

                    // Use the "terrain" map type if the map is not null.
                    if (mMap != null) {

                        Log.i(TAG, "updatePreferences() -> Terrain Map");

                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

                        adjustMapColors();
                    } else {

                        Log.e(TAG, "updatePreferences() -> Terrain Map -> mMap == null");
                        showMessageLong("An error occurred while loading the map.");
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
                        showMessageLong("An error occurred while loading the map.");
                    }

                    break;
            }
        } else {

            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

            adjustMapColors();
        }
    }

    // Used in the onCameraIdle listener and after restart (as the listener is not called after restart).
    private void cameraIdle() {

        Log.i(TAG, "cameraIdle()");

        // If the camera is zoomed too far out, don't load any shapes, as too many loaded shapes will affect performance (and user wouldn't be able to see the shapes anyway).
        if (mMap.getCameraPosition().zoom < 12) {

            return;
        }

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
        newFarLeftLat = (int) farLeftLatTemp;

        double farLeftPrecisionLon = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double farLeftLonTemp = (int) (farLeftPrecisionLon * farLeft.longitude) / farLeftPrecisionLon;
        farLeftLonTemp *= 10;
        newFarLeftLon = (int) farLeftLonTemp;

        // Get a value with 1 decimal point and use it for Firebase.
        double nearRightPrecisionLat = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double nearRightLatTemp = (int) (nearRightPrecisionLat * nearRight.latitude) / nearRightPrecisionLat;
        nearRightLatTemp *= 10;
        newNearRightLat = (int) nearRightLatTemp;

        double nearRightPrecisionLon = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double nearRightLonTemp = (int) (nearRightPrecisionLon * nearRight.longitude) / nearRightPrecisionLon;
        nearRightLonTemp *= 10;
        newNearRightLon = (int) nearRightLonTemp;

        // Get a value with 1 decimal point and use it for Firebase.
        double farRightPrecisionLat = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double farRightLatTemp = (int) (farRightPrecisionLat * farRight.latitude) / farRightPrecisionLat;
        farRightLatTemp *= 10;
        newFarRightLat = (int) farRightLatTemp;

        double farRightPrecisionLon = Math.pow(10, 1);
        // Can't create a firebase path with '.', so get rid of decimal.
        double farRightLonTemp = (int) (farRightPrecisionLon * farRight.longitude) / farRightPrecisionLon;
        farRightLonTemp *= 10;
        newFarRightLon = (int) farRightLonTemp;

        // Do not load the map if the user can see more than 7 loadable areas (e.g. they are on a tablet with a big screen).
        if (Math.abs((newNearRightLat - newFarLeftLat) + (newNearRightLon - newFarLeftLon)) >= 4 ||
                Math.abs((newFarRightLat - newNearLeftLat) - (newFarRightLon - newNearLeftLon)) >= 4) {

            return;
        }

        newNearLeft = new Pair<>(newNearLeftLat, newNearLeftLon);
        newFarLeft = new Pair<>(newFarLeftLat, newFarLeftLon);
        newNearRight = new Pair<>(newNearRightLat, newNearRightLon);
        newFarRight = new Pair<>(newFarRightLat, newFarRightLon);

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

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.prefMapType), getString(R.string.use_road_map_view));
                editor.apply();
            } else {

                Log.e(TAG, "onMenuItemClick -> Road Map -> mMap == null");
                showMessageLong("An error occurred while loading the map.");
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

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.prefMapType), getString(R.string.use_satellite_view));
                editor.apply();
            } else {

                Log.e(TAG, "onMenuItemClick -> Satellite Map -> mMap == null");
                showMessageLong("An error occurred while loading the map.");
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

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.prefMapType), getString(R.string.use_hybrid_view));
                editor.apply();
            } else {

                Log.e(TAG, "onMenuItemClick -> Hybrid Map -> mMap == null");
                showMessageLong("An error occurred while loading the map.");
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

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.prefMapType), getString(R.string.use_terrain_view));
                editor.apply();
            } else {

                Log.e(TAG, "onMenuItemClick -> Terrain Map -> mMap == null");
                showMessageLong("An error occurred while loading the map.");
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

            for (int i = 0; i < circleCentersAL.size(); i++) {

                Circle circle = mMap.addCircle(
                        new CircleOptions()
                                .center(circleCentersAL.get(i))
                                .clickable(true)
                                .radius(1.0)
                                .strokeColor(Color.YELLOW)
                                .strokeWidth(3f)
                );

                circle.setTag(circleUUIDsAL.get(i));
            }
        } else {

            for (int i = 0; i < circleCentersAL.size(); i++) {

                Circle circle = mMap.addCircle(
                        new CircleOptions()
                                .center(circleCentersAL.get(i))
                                .clickable(true)
                                .radius(1.0)
                                .strokeColor(Color.rgb(255, 0, 255))
                                .strokeWidth(3f)
                );

                circle.setTag(circleUUIDsAL.get(i));
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

        Log.i(TAG, "loadShapes()");

        // No need to load shapes when user is entering the next activity after taking a picture / video.
        if (imageFile != null || videoFile != null) {

            return;
        }

        // Don't load more than 7 areas at a time.
        if (loadedCoordinates.size() == 7) {

            mMap.clear();
            loadedCoordinates.clear();
            mapCleared = true;
        }

        // Used to query Firebase.
        ArrayList<Pair<Integer, Integer>> coordinatesNotJustLoadedTo = new ArrayList<>();
        coordinatesNotJustLoadedTo.add(newNearLeft);
        coordinatesNotJustLoadedTo.add(newFarLeft);
        coordinatesNotJustLoadedTo.add(newNearRight);
        coordinatesNotJustLoadedTo.add(newFarRight);
        Set<Pair<Integer, Integer>> removedDuplicatesCoordinatesNotJustLoadedTo = new HashSet<>(coordinatesNotJustLoadedTo);

        if (!loadedCoordinates.contains(newNearLeft) || mapCleared) {

            loadingIcon.setVisibility(View.VISIBLE);

            loadedCoordinates.add(0, newNearLeft);
            removedDuplicatesCoordinatesNotJustLoadedTo.remove(newNearLeft);

            if (loadedCoordinates.size() > 7) {

                loadedCoordinates.subList(7, loadedCoordinates.size()).clear();
            }

            DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearLeftLat + ", " + newNearLeftLon + ")").child("Points");
            firebasePoints.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    loadCirclesODC(snapshot);

                    addQuery(firebasePoints, newNearLeft);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    Log.e(TAG, "DatabaseError");
                    loadingIcon.setVisibility(View.GONE);
                    showMessageLong(error.getMessage());
                }
            });
        }

        if (!loadedCoordinates.contains(newFarLeft) && newFarLeft != newNearLeft || mapCleared) {

            loadingIcon.setVisibility(View.VISIBLE);

            loadedCoordinates.add(0, newFarLeft);
            removedDuplicatesCoordinatesNotJustLoadedTo.remove(newFarLeft);

            if (loadedCoordinates.size() > 7) {

                loadedCoordinates.subList(7, loadedCoordinates.size()).clear();
            }

            DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newFarLeftLat + ", " + newFarLeftLon + ")").child("Points");
            firebasePoints.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    loadCirclesODC(snapshot);

                    addQuery(firebasePoints, newFarLeft);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    Log.e(TAG, "DatabaseError");
                    loadingIcon.setVisibility(View.GONE);
                    showMessageLong(error.getMessage());
                }
            });
        }

        if (!loadedCoordinates.contains(newNearRight) && newNearRight != newNearLeft && newNearRight != newFarLeft || mapCleared) {

            loadingIcon.setVisibility(View.VISIBLE);

            loadedCoordinates.add(0, newNearRight);
            removedDuplicatesCoordinatesNotJustLoadedTo.remove(newNearRight);

            if (loadedCoordinates.size() > 7) {

                loadedCoordinates.subList(7, loadedCoordinates.size()).clear();
            }

            DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newNearRightLat + ", " + newNearRightLon + ")").child("Points");
            firebasePoints.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    loadCirclesODC(snapshot);

                    addQuery(firebasePoints, newNearRight);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    Log.e(TAG, "DatabaseError");
                    loadingIcon.setVisibility(View.GONE);
                    showMessageLong(error.getMessage());
                }
            });
        }

        if (!loadedCoordinates.contains(newFarRight) && newFarRight != newNearLeft && newFarRight != newFarLeft && newFarRight != newNearRight || mapCleared) {

            loadingIcon.setVisibility(View.VISIBLE);

            loadedCoordinates.add(0, newFarRight);
            removedDuplicatesCoordinatesNotJustLoadedTo.remove(newFarRight);

            if (loadedCoordinates.size() > 7) {

                loadedCoordinates.subList(7, loadedCoordinates.size()).clear();
            }

            DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + newFarRightLat + ", " + newFarRightLon + ")").child("Points");
            firebasePoints.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    loadCirclesODC(snapshot);

                    addQuery(firebasePoints, newFarRight);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    Log.e(TAG, "DatabaseError");
                    loadingIcon.setVisibility(View.GONE);
                    showMessageLong(error.getMessage());
                }
            });
        }

        // Check if the latest value in Firebase equals the saved value. If not, load the new shapes.
        if (!mapCleared || restarted) {

            loadingIcon.setVisibility(View.VISIBLE);

            for (Pair<Integer, Integer> coordinates : removedDuplicatesCoordinatesNotJustLoadedTo) {

                Query query = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + coordinates.first + ", " + coordinates.second + ")").child("Points").limitToLast(1);
                query.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        // The user restarted, but no shape exists or was added.
                        if (snapshot.getChildrenCount() == 0) {

                            DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + coordinates.first + ", " + coordinates.second + ")").child("Points");
                            addQuery(firebasePoints, coordinates);
                            return;
                        }

                        for (DataSnapshot ds : snapshot.getChildren()) {

                            String shapeUUID = (String) ds.child("shapeUUID").getValue();

                            if (shapeUUID != null) {

                                // If new circles exist, add them to the map. Else, add the query to add new shapes in the future.
                                if (!circleUUIDsAL.contains(shapeUUID)) {

                                    Log.i(TAG, "loadShapes() -> new shapes since app restarted");

                                    // If lastKnownKey is null, no shapes existed before user put app into the background and all shapes need to be retrieved.
                                    Query query;
                                    if (lastKnownKey == null) {

                                        query = FirebaseDatabase.getInstance().getReference()
                                                .child("Shapes").child("(" + coordinates.first + ", " + coordinates.second + ")").child("Points");
                                    } else {

                                        query = FirebaseDatabase.getInstance().getReference()
                                                .child("Shapes").child("(" + coordinates.first + ", " + coordinates.second + ")").child("Points")
                                                .orderByKey()
                                                .startAt(lastKnownKey);
                                    }

                                    query.addListenerForSingleValueEvent(new ValueEventListener() {

                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                                            loadCirclesODC(snapshot);

                                            DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + coordinates.first + ", " + coordinates.second + ")").child("Points");
                                            addQuery(firebasePoints, coordinates);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                            showMessageLong(error.getMessage());
                                        }
                                    });
                                } else {

                                    Log.i(TAG, "loadShapes() -> no new shapes");

                                    DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + coordinates.first + ", " + coordinates.second + ")").child("Points");
                                    addQuery(firebasePoints, coordinates);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        showMessageLong(error.getMessage());
                    }
                });
            }
        }

        mapCleared = false;
        restarted = false;
    }

    private void loadCirclesODC(DataSnapshot snapshot) {

        for (DataSnapshot ds : snapshot.getChildren()) {

            if (ds.child("circleOptions").exists()) {

                Double lat = (Double) ds.child("circleOptions/center/latitude/").getValue();
                Double lon = (Double) ds.child("circleOptions/center/longitude/").getValue();
                if (lat != null && lon != null) {

                    LatLng center = new LatLng(lat, lon);

                    // Prevents duplicates.
                    if (circleCentersAL.contains(center)) {

                        continue;
                    }

                    circleCentersAL.add(center);
                    circleUUIDsAL.add((String) ds.child("shapeUUID").getValue());
                    lastKnownKey = ds.getKey();

                    // Load different colored circles depending on the map type.
                    Circle circle;
                    if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                        // Yellow circle.
                        circle = mMap.addCircle(
                                new CircleOptions()
                                        .center(center)
                                        .clickable(true)
                                        .radius(1.0)
                                        .strokeColor(Color.YELLOW)
                                        .strokeWidth(3f)
                        );
                    } else {

                        // Purple circle.
                        circle = mMap.addCircle(
                                new CircleOptions()
                                        .center(center)
                                        .clickable(true)
                                        .radius(1.0)
                                        .strokeColor(Color.rgb(255, 0, 255))
                                        .strokeWidth(3f)
                        );
                    }

                    // Set the Tag using the UUID in Firebase. Value is sent to Chat.java in onMapReady to identify the chatCircle.
                    String shapeUUID = (String) ds.child("shapeUUID").getValue();

                    circle.setTag(shapeUUID);
                }
            }
        }

        loadingIcon.setVisibility(View.GONE);
    }

    // Change to .limitToLast(1) to cut down on data usage. Otherwise, EVERY child at this node will be downloaded every time the child is updated.
    private void addQuery(DatabaseReference databaseReference, Pair<Integer, Integer> cornerReference) {

        if (newNearLeft.equals(cornerReference)) {

            queryNearLeft = databaseReference.limitToLast(1);
            childEventListenerNearLeft = new ChildEventListener() {

                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    addQueryOCA(snapshot);
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

                    showMessageLong(error.getMessage());
                }
            };

            queryNearLeft.addChildEventListener(childEventListenerNearLeft);
        } else if (newFarLeft.equals(cornerReference)) {

            queryFarLeft = databaseReference.limitToLast(1);
            childEventListenerFarLeft = new ChildEventListener() {

                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    addQueryOCA(snapshot);
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

                    showMessageLong(error.getMessage());
                }
            };

            queryFarLeft.addChildEventListener(childEventListenerFarLeft);
        } else if (newNearRight.equals(cornerReference)) {

            queryNearRight = databaseReference.limitToLast(1);
            childEventListenerNearRight = new ChildEventListener() {

                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    addQueryOCA(snapshot);
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

                    showMessageLong(error.getMessage());
                }
            };

            queryNearRight.addChildEventListener(childEventListenerNearRight);
        } else if (newFarRight.equals(cornerReference)) {

            queryFarRight = databaseReference.limitToLast(1);
            childEventListenerFarRight = new ChildEventListener() {

                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    addQueryOCA(snapshot);
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

                    showMessageLong(error.getMessage());
                }
            };

            queryFarRight.addChildEventListener(childEventListenerFarRight);
        }

        // Prevent getting rid of the loading icon if the user takes a picture or video, gets the notification about GPS being turned off, then turns it on and returns.
        if (imageFile == null && videoFile == null) {

            loadingIcon.setVisibility(View.GONE);
        }
    }

    private void addQueryOCA(DataSnapshot snapshot) {

        if (snapshot.child("circleOptions").exists()) {

            // Set the Tag using the UUID in Firebase. Value is sent to Chat.java in onMapReady to identify the chatCircle.
            String shapeUUID = (String) snapshot.child("shapeUUID").getValue();

            // Prevent duplicates.
            if (circleUUIDsAL.contains(shapeUUID)) {

                return;
            }

            if (shapeUUID != null && circleUUIDsAL.contains(shapeUUID)) {

                return;
            }

            Double lat = (Double) snapshot.child("circleOptions/center/latitude/").getValue();
            Double lon = (Double) snapshot.child("circleOptions/center/longitude/").getValue();
            if (lat != null && lon != null) {

                LatLng center = new LatLng(lat, lon);

                circleCentersAL.add(center);
                circleUUIDsAL.add((String) snapshot.child("shapeUUID").getValue());
                lastKnownKey = snapshot.getKey();

                // Load different colored circles depending on the map type.
                Circle circle;
                if (mMap.getMapType() != 1 && mMap.getMapType() != 3) {

                    // Yellow circle.
                    circle = mMap.addCircle(
                            new CircleOptions()
                                    .center(center)
                                    .clickable(true)
                                    .radius(1.0)
                                    .strokeColor(Color.YELLOW)
                                    .strokeWidth(3f)
                    );
                } else {

                    // Purple circle.
                    circle = mMap.addCircle(
                            new CircleOptions()
                                    .center(center)
                                    .clickable(true)
                                    .radius(1.0)
                                    .strokeColor(Color.rgb(255, 0, 255))
                                    .strokeWidth(3f)
                    );
                }

                circle.setTag(shapeUUID);
            }
        }
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

    private void showMessageLong(String message) {

        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

            Snackbar snackBar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
            snackBar.setAnchorView(recordButton);
            View snackBarView = snackBar.getView();
            TextView snackTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
            snackTextView.setMaxLines(10);
            snackBar.show();
        } else {

            cancelToasts();
            longToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
            longToast.setGravity(Gravity.CENTER, 0, 0);
            longToast.show();
        }
    }
}