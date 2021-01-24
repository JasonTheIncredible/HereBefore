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
import android.net.Uri;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class Map extends FragmentActivity implements
        OnMapReadyCallback,
        LocationListener,
        PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "Map";
    private static final int Request_User_Location_Code = 42, Request_ID_Take_Photo = 69, Request_ID_Record_Video = 420;
    private GoogleMap mMap;
    private Circle circleTemp;
    private ChildEventListener childEventListenerNearLeft, childEventListenerFarLeft, childEventListenerNearRight, childEventListenerFarRight;
    private String circleTempUUID, circleTempUUIDTemp;
    protected static String imageFile, videoFile;
    private Button circleButton, mapTypeButton, settingsButton;
    private ImageButton dmButton;
    private PopupMenu popupMapType;
    private boolean firstLoadCamera = true, cameraMoved = false, waitingForBetterLocationAccuracy = false, badAccuracy = false, restarted = false, mapCleared = false, checkPermissionsPicture;
    private final ArrayList<String> circleUUIDsAL = new ArrayList<>();
    private final ArrayList<LatLng> circleCentersAL = new ArrayList<>();
    private int newNearLeftLat, newNearLeftLon, newFarLeftLat, newFarLeftLon, newNearRightLat, newNearRightLon, newFarRightLat, newFarRightLon;
    private Toast longToast;
    private View loadingIcon;
    private Query queryNearLeft, queryFarLeft, queryNearRight, queryFarRight;
    private Pair<Integer, Integer> newNearLeft, newFarLeft, newNearRight, newFarRight;
    private List<Pair<Integer, Integer>> loadedCoordinates;
    private LocationManager locationManager;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    // Create a more consistent visual to let the user know their location with a circle has changed.
    // Make creating the original circle more accurate.
    // Deal with user sending new message at previous circle's location - send circle location AL to Chat and check for new circles when sending message?
    // Use the user's most up-to-date location information in DirectMentions.
    // Fix bug where after sending a message and restarting Chat, date in datesAL will not match the value in Firebase as date in Firebase gets updated soon after getting the initial value. Will probably need to switch from date to something else.
    // Show picture upon opening circle or show picture at the top at all times.
    // Prevent data scraping (hide email addresses and other personal information).

    // Long press a shape to see a popup of that picture.
    // Allow users to get "likes". Allow more likes per second with a higher "level" like DS.
    // If user is outside of circle, show how far outside - requires checking location every time before posting new message.
    // After clicking on a DM and going to that Chat, allow user to find that same shape on the map.
    // Create a variable with the point's radius and use that instead of "1" to future-proof changes. Also, make "creating a point" more accurate to the user's location.
    // Deal with deprecated methods.
    // After clicking on a UUID, keep it expanded after updating recyclerView.
    // Create a "New messages" toast if user is scrolled up (or maybe just if user is scrolled up and restarts?)
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
    // Add a function for deleting / decreasing position of DMs after deleting / updating messageThreads - problem: need to store the name of the person being DM'ed, but that's a lot of useless information and possibly increases security risk.
    // Load user-specific shared preferences - looks like it might require saving info to database; is this worth it?
    // Increase viral potential - make it easier to share?
    // Update general look of app.
    // Panoramic view, like gMaps.

    // Update to Node.js 10.
    // Deal with leaks.
    // Test on multiple devices.
    // Remember the AC: Origins inspiration. Also, airdrop - create items in the world. Also, gMaps drag and drop. Also, DS virtual items in the world.
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
    // Form LLC?

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        // Clear the cache.
        deleteDirectory(this.getCacheDir());

        // This will be called if user opens app using a notification.
        onNewIntent(getIntent());

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
                            }
                        });
            } else {

                checkLocationPermissions();
            }
        });

        // Go to DMs.
        dmButton.setOnClickListener(v -> {

            Log.i(TAG, "onStart() -> dmButton -> onClick");

            // Check location permissions.
            if (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                loadingIcon.setVisibility(View.VISIBLE);

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
                            }
                        });
            } else {

                checkLocationPermissions();
            }
        });

        // Take a photo. Create chat if one doesn't exist.
        circleButton.setOnClickListener(view -> {

            Log.i(TAG, "onStart() -> circleButton -> onClick");

            if (checkPermissionsPicture()) {

                cancelToasts();

                startActivityTakePhoto();
            }
        });

        // Record a video. Create chat if one doesn't exist.
        circleButton.setOnLongClickListener(view -> {

            Log.i(TAG, "onStart() -> circleButton -> onLongClick");

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
        int permissionWriteExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {

            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (permissionWriteExternalStorage != PackageManager.PERMISSION_GRANTED) {

            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
        int permissionWriteExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionRecordAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {

            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (permissionWriteExternalStorage != PackageManager.PERMISSION_GRANTED) {

            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
                toastMessageLong(ex.getMessage());
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
                toastMessageLong(ex.getMessage());
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
                        .setTitle("Camera Permission Required")
                        .setMessage("Here Before needs permission to use your camera to take pictures and video.")
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

    private void writeExternalStoragePermissionAlertAsync(Boolean checkPermissionsPicture) {

        AlertDialog.Builder alert;
        alert = new AlertDialog.Builder(this);

        HandlerThread writeExternalStoragePermissionHandlerThread = new HandlerThread("writeExternalStorageHandlerThread");
        writeExternalStoragePermissionHandlerThread.start();
        Handler mHandler = new Handler(writeExternalStoragePermissionHandlerThread.getLooper());
        Runnable runnable = () ->

                alert.setCancelable(false)
                        .setTitle("Storage Permission Required")
                        .setMessage("Here Before needs permission to use your storage to save photos and videos.")
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
                        .setTitle("Audio Permission Required")
                        .setMessage("Here Before needs permission to record audio during video recording.")
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

            // Disable buttons while next activity is opened.
            circleButton.setEnabled(false);
            dmButton.setEnabled(false);
            settingsButton.setEnabled(false);
        } else {

            imageFile = null;
            videoFile = null;
            return;
        }

        // Check location permissions.
        if (ContextCompat.checkSelfPermission(getBaseContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Create a point and go to Chat.java or SignIn.java.
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(Map.this, location -> {

                        if (location != null) {

                            // If user is within a circle, enter it. Else, cycle through all circles that are 2 meters away and enter the closest one. Else, enter a new one.
                            float[] oldDistance = new float[2];
                            oldDistance[0] = 2f;
                            LatLng latLng = null;
                            String uuid = null;
                            for (int i = 0; i < circleCentersAL.size(); i++) {

                                float[] newDistance = new float[2];
                                Location.distanceBetween(circleCentersAL.get(i).latitude, circleCentersAL.get(i).longitude, location.getLatitude(), location.getLongitude(), newDistance);

                                if (newDistance[0] <= 1) {

                                    if (requestCode == 3) {

                                        Log.i(TAG, "onActivityResult() -> Camera");

                                        enterCircle(location, circleCentersAL.get(i), circleUUIDsAL.get(i), false, true);
                                        return;
                                    } else if (requestCode == 4) {

                                        Log.i(TAG, "onActivityResult() -> Video");

                                        enterCircle(location, circleCentersAL.get(i), circleUUIDsAL.get(i), false, true);
                                        return;
                                    }
                                } else if (newDistance[0] <= oldDistance[0]) {

                                    oldDistance[0] = newDistance[0];
                                    latLng = circleCentersAL.get(i);
                                    uuid = circleUUIDsAL.get(i);
                                }
                            }

                            if (requestCode == 3) {

                                Log.i(TAG, "onActivityResult() -> Camera");

                                // latLng and uuid will be null if it is a new circle, and newShape will be true.
                                enterCircle(location, latLng, uuid, latLng == null, true);
                            } else if (requestCode == 4) {

                                Log.i(TAG, "onActivityResult() -> Video");

                                // latLng and uuid will be null if it is a new circle, and newShape will be true.
                                enterCircle(location, latLng, uuid, latLng == null, true);
                            }
                        } else {

                            loadingIcon.setVisibility(View.GONE);
                        }
                    });
        }
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        restarted = true;

        // Clear map before adding new Firebase circles in onStart() to prevent overlap.
        // Set shape to null so changing chatSizeSeekBar in onStart() will create a circle and circleButton will reset itself.
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

        circleButton.setEnabled(true);
        dmButton.setEnabled(true);
        settingsButton.setEnabled(true);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Start updating location.
        if (ContextCompat.checkSelfPermission(Map.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Location seems to work without the following line, but it is set to false in onPause so for symmetry's sake, I'm setting it to true here.
            if (mMap != null) {

                // Needs to be re-enabled or circleButton won't work.
                mMap.setMyLocationEnabled(true);
            }

            String provider = LocationManager.NETWORK_PROVIDER;
            if (locationManager != null) {

                locationManager.requestLocationUpdates(provider, 0, 0, this);
                startLocationUpdates();
            } else {

                Log.e(TAG, "onResume() -> locationManager == null");
                toastMessageLong("Error retrieving your location.");
            }
        } else {

            checkLocationPermissions();
        }
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
        }

        if (mFusedLocationClient != null) {

            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
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

        if (circleButton != null) {

            circleButton.setOnClickListener(null);
            circleButton.setOnLongClickListener(null);
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

            // Check location permissions.
            if (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(Map.this, location -> {

                            if (location != null) {

                                String circleUUID = extras.getString("shapeUUID");

                                // If the user clicks on a notification while the app is running, lat !=0 && lon != 0, so the if statement will run.
                                double lat = extras.getDouble("lat");
                                double lon = extras.getDouble("lon");
                                if (lat != 0 && lon != 0 && circleUUID != null) {

                                    // Check distance to circleTemp and enter it.
                                    float[] distance = new float[2];
                                    Location.distanceBetween(lat, lon, location.getLatitude(), location.getLongitude(), distance);

                                    // If distance <= 2, enterCircle(location, circleTemp.getCenter(), false, true). Else, enterCircle(location, circleTemp.getCenter(), false, false).
                                    enterCircle(location, new LatLng(lat, lon), circleUUID, false, distance[0] <= 2);
                                    return;
                                }

                                // If the code got to this point, the user clicked on a notification while the app was closed.
                                double latDouble = Double.parseDouble(extras.getString("lat"));
                                double lonDouble = Double.parseDouble(extras.getString("lon"));
                                if (circleUUID != null) {

                                    // Check distance to circleTemp and enter it.
                                    float[] distance = new float[2];
                                    Location.distanceBetween(latDouble, lonDouble, location.getLatitude(), location.getLongitude(), distance);

                                    // If distance <= 2, enterCircle(location, circleTemp.getCenter(), false, true). Else, enterCircle(location, circleTemp.getCenter(), false, false).
                                    enterCircle(location, new LatLng(latDouble, lonDouble), circleUUID, false, distance[0] <= 2);
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

                                    // Check distance to circleTemp and enter it.
                                    float[] distance = new float[2];
                                    Location.distanceBetween(circleTemp.getCenter().latitude, circleTemp.getCenter().longitude, location.getLatitude(), location.getLongitude(), distance);

                                    // If distance <= 2, enterCircle(location, circleTemp.getCenter(), false, true). Else, enterCircle(location, circleTemp.getCenter(), false, false).
                                    enterCircle(location, null, null, false, distance[0] <= 2);
                                } else {

                                    loadingIcon.setVisibility(View.GONE);
                                    Log.e(TAG, "onStart() -> onCircleClick -> location == null");
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

                        toastMessageLong("Location permission is required. Please enable it manually through the Android settings menu.");
                    }
                }

                break;
            }

            case Request_ID_Take_Photo: {

                HashMap<String, Integer> perms = new HashMap<>();
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);

                if (grantResults.length > 0) {

                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);

                    Integer cameraPermissions = perms.get(Manifest.permission.CAMERA);
                    Integer externalStoragePermissions = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    if (cameraPermissions != null && externalStoragePermissions != null) {

                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Camera permissions were not granted. Ask again.");

                            cameraPermissionAlertAsync(checkPermissionsPicture);
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Storage permissions were not granted. Ask again.");

                            writeExternalStoragePermissionAlertAsync(checkPermissionsPicture);
                        } else if (cameraPermissions == PackageManager.PERMISSION_GRANTED
                                && externalStoragePermissions == PackageManager.PERMISSION_GRANTED) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Camera and Write External Storage permission granted.");
                            // Process the normal workflow.
                            startActivityTakePhoto();
                        } else if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                            toastMessageLong("Camera and External Storage permissions are required. Please enable them manually through the Android settings menu.");
                        }
                    }
                }

                break;
            }

            case Request_ID_Record_Video: {

                HashMap<String, Integer> perms = new HashMap<>();
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);

                if (grantResults.length > 0) {

                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);

                    Integer cameraPermissions = perms.get(Manifest.permission.CAMERA);
                    Integer externalStoragePermissions = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    Integer audioPermissions = perms.get(Manifest.permission.RECORD_AUDIO);

                    if (cameraPermissions != null && externalStoragePermissions != null && audioPermissions != null) {

                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Camera permissions were not granted. Ask again.");

                            cameraPermissionAlertAsync(checkPermissionsPicture);
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Storage permissions were not granted. Ask again.");

                            writeExternalStoragePermissionAlertAsync(checkPermissionsPicture);
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                            Log.d(TAG, "Request_ID_Record_Video -> Audio permissions were not granted. Ask again.");

                            audioPermissionAlertAsync(checkPermissionsPicture);
                        } else if (cameraPermissions == PackageManager.PERMISSION_GRANTED
                                && externalStoragePermissions == PackageManager.PERMISSION_GRANTED
                                && audioPermissions == PackageManager.PERMISSION_GRANTED) {

                            Log.d(TAG, "Request_ID_Record_Video -> Camera, Write External Storage, and Record Audio permission granted.");
                            // Process the normal workflow.
                            startActivityRecordVideo();
                        } else if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                            toastMessageLong("Camera, External Storage, and Audio permissions are required. Please enable them manually through the Android settings menu.");
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
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /* 1000 = 1 sec */
        long UPDATE_INTERVAL = 0;
        locationRequest.setInterval(UPDATE_INTERVAL);

        /* 1000 = 1 sec */
        long FASTEST_INTERVAL = 0;
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

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
                public void onLocationResult(LocationResult locationResult) {

                    if (locationResult != null) {

                        onLocationChanged(locationResult.getLastLocation());
                    }
                }
            };

            getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
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
                        .setTitle("GPS Disabled")
                        .setMessage("Please enable your location services on the following screen.")
                        .setPositiveButton("OK", (dialog, i) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                        .create()
                        .show();

        mHandler.post(runnable);
    }

    private void enterCircle(Location userLocation, LatLng circleLatLng, String circleUUID, boolean newShape, boolean userIsWithinShape) {

        Log.i(TAG, "enterCircle()");

        LatLng circleToEnterLatLng = null;
        String circleToEnterUUID;
        if (circleLatLng == null && newShape) {
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

        if (!newShape) {

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
            // Pass this value to Chat.java to tell where the user can leave a message in the recyclerView.
            Activity.putExtra("userIsWithinShape", userIsWithinShape);
            // UserLatitude and userLongitude are used in DirectMentions.
            Activity.putExtra("userLatitude", userLocation.getLatitude());
            Activity.putExtra("userLongitude", userLocation.getLongitude());
            Activity.putExtra("circleLatitude", circleToEnterLatLng.latitude);
            Activity.putExtra("circleLongitude", circleToEnterLatLng.longitude);
        }

        // Pass this value to Chat.java to identify the shape.
        Activity.putExtra("shapeUUID", circleToEnterUUID);
        Activity.putExtra("imageFile", imageFile);
        Activity.putExtra("videoFile", videoFile);

        // Prevent previous activities from being in the back stack.
        Activity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // If getIntent().getExtras != null, user is entering a circle from a notification and seenByUser needs to be set to true. Else, enter circle like normal.
        if (getIntent().getExtras() != null) {

            String userUUID = getIntent().getExtras().getString("userUUID");
            Activity.putExtra("UUIDToHighlight", userUUID);

            // If user has a Google account, get email one way. Else, get email another way.
            GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
            String email;
            if (acct != null) {

                email = acct.getEmail();
            } else {

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                email = sharedPreferences.getString("userToken", "null");
            }
            // Firebase does not allow ".", so replace them with ",".
            String userEmailFirebase = email.replace(".", ",");

            // Set "seenByUser" to true so it is not highlighted in the future.
            DatabaseReference Dms = FirebaseDatabase.getInstance().getReference().child("Users").child(userEmailFirebase).child("ReceivedDms");
            Dms.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    for (DataSnapshot ds : snapshot.getChildren()) {

                        String mUserUUID = (String) ds.child("userUUID").getValue();

                        if (mUserUUID != null) {

                            if (mUserUUID.equals(userUUID)) {

                                if (!(Boolean) ds.child("seenByUser").getValue()) {

                                    ds.child("seenByUser").getRef().setValue(true);

                                    loadingIcon.setVisibility(View.GONE);

                                    startActivity(Activity);

                                    return;
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    toastMessageLong(error.getMessage());
                }
            });
        } else {

            circleButton.setEnabled(true);
            dmButton.setEnabled(true);
            settingsButton.setEnabled(true);

            loadingIcon.setVisibility(View.GONE);

            startActivity(Activity);
        }
    }

    protected void updatePreferences() {

        Log.i(TAG, "updatePreferences()");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String preferredMapType = sharedPreferences.getString(SettingsFragment.KEY_MAP_TYPE, getResources().getString(R.string.hybrid_view));

        if (preferredMapType != null) {

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

            dmButton.setBackgroundResource(R.drawable.dm_button);

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

            circleButton.setBackgroundResource(R.drawable.circle_button_purple);

            settingsButton.setBackgroundResource(R.drawable.ic_more_vert_purple_24dp);

            dmButton.setBackgroundResource(R.drawable.dm_button_purple);

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

                    addCirclesQuery(firebasePoints, newNearLeft);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                    Log.e(TAG, "DatabaseError");
                    loadingIcon.setVisibility(View.GONE);
                    toastMessageLong(databaseError.getMessage());
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

                    addCirclesQuery(firebasePoints, newFarLeft);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                    Log.e(TAG, "DatabaseError");
                    loadingIcon.setVisibility(View.GONE);
                    toastMessageLong(databaseError.getMessage());
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

                    addCirclesQuery(firebasePoints, newNearRight);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                    Log.e(TAG, "DatabaseError");
                    loadingIcon.setVisibility(View.GONE);
                    toastMessageLong(databaseError.getMessage());
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

                    addCirclesQuery(firebasePoints, newFarRight);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                    Log.e(TAG, "DatabaseError");
                    loadingIcon.setVisibility(View.GONE);
                    toastMessageLong(databaseError.getMessage());
                }
            });
        }

        if (!mapCleared || restarted) {

            loadingIcon.setVisibility(View.VISIBLE);

            // Check if the latest value in Firebase equals the saved value. If not, load the new shapes.
            for (Pair<Integer, Integer> coordinates : removedDuplicatesCoordinatesNotJustLoadedTo) {

                Query query = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + coordinates.first + ", " + coordinates.second + ")").child("Points").limitToLast(1);

                query.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        // The user restarted, but no shape exists or was added.
                        if (snapshot.getChildrenCount() == 0) {

                            DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + coordinates.first + ", " + coordinates.second + ")").child("Points");
                            addCirclesQuery(firebasePoints, coordinates);
                            return;
                        }

                        for (DataSnapshot ds : snapshot.getChildren()) {

                            String shapeUUID = (String) ds.child("shapeUUID").getValue();

                            if (shapeUUID != null) {

                                // If new circles exist, add them to the map. Else, add the query to add new shapes in the future.
                                if (!circleUUIDsAL.contains(shapeUUID)) {

                                    Query query = FirebaseDatabase.getInstance().getReference()
                                            .child("Shapes").child("(" + coordinates.first + ", " + coordinates.second + ")").child("Points")
                                            .orderByKey()
                                            .startAt(ds.getKey());

                                    query.addListenerForSingleValueEvent(new ValueEventListener() {

                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                                            loadCirclesODC(snapshot);

                                            DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + coordinates.first + ", " + coordinates.second + ")").child("Points");
                                            addCirclesQuery(firebasePoints, coordinates);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    });
                                } else {

                                    DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + coordinates.first + ", " + coordinates.second + ")").child("Points");
                                    addCirclesQuery(firebasePoints, coordinates);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
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

                LatLng center = new LatLng((double) ds.child("circleOptions/center/latitude/").getValue(), (double) ds.child("circleOptions/center/longitude/").getValue());

                circleCentersAL.add(center);
                circleUUIDsAL.add((String) ds.child("shapeUUID").getValue());

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

        loadingIcon.setVisibility(View.GONE);
    }

    // Change to .limitToLast(1) to cut down on data usage. Otherwise, EVERY child at this node will be downloaded every time the child is updated.
    private void addCirclesQuery(DatabaseReference databaseReference, Pair<Integer, Integer> cornerReference) {

        if (newNearLeft.equals(cornerReference)) {

            queryNearLeft = databaseReference.limitToLast(1);
            childEventListenerNearLeft = new ChildEventListener() {

                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

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

                        LatLng center = new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue());

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

            queryNearLeft.addChildEventListener(childEventListenerNearLeft);
        } else if (newFarLeft.equals(cornerReference)) {

            queryFarLeft = databaseReference.limitToLast(1);
            childEventListenerFarLeft = new ChildEventListener() {

                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

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

                        if (snapshot.child("circleOptions").exists()) {

                            LatLng center = new LatLng((double) snapshot.child("circleOptions/center/latitude/").getValue(), (double) snapshot.child("circleOptions/center/longitude/").getValue());

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

            queryFarLeft.addChildEventListener(childEventListenerFarLeft);
        } else if (newNearRight.equals(cornerReference)) {

            queryNearRight = databaseReference.limitToLast(1);
            childEventListenerNearRight = new ChildEventListener() {

                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

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
                    }
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

            queryNearRight.addChildEventListener(childEventListenerNearRight);
        } else if (newFarRight.equals(cornerReference)) {

            queryFarRight = databaseReference.limitToLast(1);
            childEventListenerFarRight = new ChildEventListener() {

                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

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
                    }
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

            queryFarRight.addChildEventListener(childEventListenerFarRight);
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