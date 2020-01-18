package co.clixel.herebefore;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static java.text.DateFormat.getDateTimeInstance;

public class Chat extends AppCompatActivity implements
        PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "Chat";
    private static final int Request_User_Camera_Code = 1800;
    private static final int Request_User_Write_External_Storage_Code = 1900;
    private EditText mInput;
    private ArrayList<String> mUser = new ArrayList<>();
    private ArrayList<String> mTime = new ArrayList<>();
    private ArrayList<String> mText = new ArrayList<>();
    private ArrayList<String> mImage = new ArrayList<>();
    private RecyclerView recyclerView;
    private static int index = -1;
    private static int top = -1;
    private DatabaseReference databaseReference;
    private ValueEventListener eventListener;
    private FloatingActionButton sendButton, mediaButton;
    private boolean reachedEndOfRecyclerView = false;
    private boolean recyclerViewHasScrolled = false;
    private boolean messageSent = false;
    private boolean mediaButtonMenuIsOpen, newShape, threeMarkers, fourMarkers, fiveMarkers, sixMarkers, sevenMarkers, eightMarkers, shapeIsCircle;
    private Boolean userIsWithinShape;
    private View.OnLayoutChangeListener onLayoutChangeListener;
    private String uuid, currentPhotoPath;
    private Double polygonArea, circleLatitude, circleLongitude, radius, marker0Latitude, marker0Longitude, marker1Latitude, marker1Longitude, marker2Latitude, marker2Longitude, marker3Latitude, marker3Longitude, marker4Latitude, marker4Longitude, marker5Latitude, marker5Longitude, marker6Latitude, marker6Longitude, marker7Latitude, marker7Longitude;
    private int fillColor;
    private PopupMenu mediaButtonMenu;
    private ImageView imageView;
    public Uri imgURI;
    private StorageTask uploadTask;

    //TODO: Move recyclerView down when new message is added.
    //TODO: Firebase caching.
    //TODO: Compress image before upload to Firebase.
    //TODO: Add ability to add taken pictures and video to RecyclerView.
    //TODO: Look up videos about texting apps to change design of + button.
    //TODO: Add a username (in recyclerviewlayout).
    //TODO: Keep checking user's location while user is in recyclerviewlayout to see if they can keep messaging, add a recyclerviewlayout at the top notifying user of this. Add differentiation between messaging within area vs not.
    //TODO: Too much work on main thread.
    //TODO: Check updating in different states with another device.
    //TODO: Change popupMenu color.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        setContentView(R.layout.chat);

        mediaButton = findViewById(R.id.mediaButton);
        imageView = findViewById(R.id.imageView);
        mInput = findViewById(R.id.input);
        sendButton = findViewById(R.id.sendButton);
        recyclerView = findViewById(R.id.messageList);
        recyclerView.hasFixedSize();

        // Set to dark mode.
        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES);

        // Get info from Map.java
        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            newShape = extras.getBoolean("newShape");
            uuid = extras.getString("uuid");
            userIsWithinShape = extras.getBoolean("userIsWithinShape");
            // fillColor will be null if the shape is not a point.
            fillColor = extras.getInt("fillColor");
            // circleLatitude, circleLongitude, and radius will be null if the circle is not new (as a new circle is not being created).
            circleLatitude = extras.getDouble("circleLatitude");
            circleLongitude = extras.getDouble("circleLongitude");
            radius = extras.getDouble("radius");
            // Most of these will be null if the polygon does not have eight markers, or if the polygon is not new.
            shapeIsCircle = extras.getBoolean("shapeIsCircle");
            polygonArea = extras.getDouble("polygonArea");
            threeMarkers = extras.getBoolean("threeMarkers");
            fourMarkers = extras.getBoolean("fourMarkers");
            fiveMarkers = extras.getBoolean("fiveMarkers");
            sixMarkers = extras.getBoolean("sixMarkers");
            sevenMarkers = extras.getBoolean("sevenMarkers");
            eightMarkers = extras.getBoolean("eightMarkers");
            marker0Latitude = extras.getDouble("marker0Latitude");
            marker0Longitude = extras.getDouble("marker0Longitude");
            marker1Latitude = extras.getDouble("marker1Latitude");
            marker1Longitude = extras.getDouble("marker1Longitude");
            marker2Latitude = extras.getDouble("marker2Latitude");
            marker2Longitude = extras.getDouble("marker2Longitude");
            marker3Latitude = extras.getDouble("marker3Latitude");
            marker3Longitude = extras.getDouble("marker3Longitude");
            marker4Latitude = extras.getDouble("marker4Latitude");
            marker4Longitude = extras.getDouble("marker4Longitude");
            marker5Latitude = extras.getDouble("marker5Latitude");
            marker5Longitude = extras.getDouble("marker5Longitude");
            marker6Latitude = extras.getDouble("marker6Latitude");
            marker6Longitude = extras.getDouble("marker6Longitude");
            marker7Latitude = extras.getDouble("marker7Latitude");
            marker7Longitude = extras.getDouble("marker7Longitude");
        } else {

            Log.e(TAG, "onStart() -> extras == null");
            Crashlytics.logException(new RuntimeException("onStart() -> extras == null"));
        }
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Connect to Firebase.
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        databaseReference = rootRef.child("messageThreads");
        eventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                // Clear the RecyclerView before adding new entries to prevent duplicates.
                if (recyclerView.getLayoutManager() != null) {

                    mUser.clear();
                    mTime.clear();
                    mImage.clear();
                    mText.clear();
                }

                // If the value from Map.java is false, check Firebase and load any existing messages.
                if (!newShape) {

                    for (DataSnapshot ds : dataSnapshot.getChildren()) {

                        // If the uuid brought from Map.java equals the uuid attached to the recyclerviewlayout in Firebase, load it into the RecyclerView.
                        String firebaseUUID = (String) ds.child("uuid").getValue();
                        if (firebaseUUID != null) {

                            if (firebaseUUID.equals(uuid)) {

                                Long serverDate = (Long) ds.child("date").getValue();
                                String messageURL = (String) ds.child("imageURL").getValue();
                                String messageText = (String) ds.child("message").getValue();
                                DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                                // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                if (serverDate != null) {

                                    Date netDate = (new Date(serverDate));
                                    String messageTime = dateFormat.format(netDate);
                                    mTime.add(messageTime);
                                } else {

                                    Log.e(TAG, "onStart() -> serverDate == null");
                                    Crashlytics.logException(new RuntimeException("onStart() -> serverDate == null"));
                                }
                                mImage.add(messageURL);
                                mText.add(messageText);
                            }
                        }
                    }
                }

                // Read RecyclerView scroll position (for use in initRecyclerView).
                if (recyclerView.getLayoutManager() != null) {

                    index = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                    View v = recyclerView.getChildAt(0);
                    top = (v == null) ? 0 : (v.getTop() - recyclerView.getPaddingTop());
                }

                initRecyclerView();

                // Check RecyclerView scroll state (to allow the layout to move up when keyboard appears).
                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

                    @Override
                    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {

                        super.onScrollStateChanged(recyclerView, newState);

                        // If RecyclerView can't be scrolled down, reachedEndOfRecyclerView = true.
                        reachedEndOfRecyclerView = !recyclerView.canScrollVertically(1);

                        // Used to detect if user has just entered the recyclerviewlayout (so layout needs to move up when keyboard appears).
                        recyclerViewHasScrolled = true;
                    }
                });

                // If RecyclerView is scrolled to the bottom, move the layout up when the keyboard appears.
                recyclerView.addOnLayoutChangeListener(onLayoutChangeListener = new View.OnLayoutChangeListener() {

                    @Override
                    public void onLayoutChange(View v,
                                               int left, int top, int right, int bottom,
                                               int oldLeft, int oldTop, int oldRight, int oldBottom) {

                        if (reachedEndOfRecyclerView || !recyclerViewHasScrolled) {

                            if (bottom < oldBottom) {

                                if (recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) {

                                    recyclerView.postDelayed(new Runnable() {

                                        @Override
                                        public void run() {

                                            recyclerView.smoothScrollToPosition(

                                                    recyclerView.getAdapter().getItemCount() - 1);
                                        }
                                    }, 100);
                                }
                            }
                        }
                    }
                });
            }

            public void onCancelled(@NonNull DatabaseError databaseError) {

                Toast.makeText(Chat.this, databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        // Add the Firebase listener.
        databaseReference.addValueEventListener(eventListener);

        // Hide the imageView if user presses the delete button.
        mInput.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_DEL && imageView.getVisibility() == View.VISIBLE &&
                        (mInput.getText().toString().trim().length() == 0 || mInput.getSelectionStart() == 0)) {

                    imageView.setVisibility(View.GONE);
                }

                if (keyCode == KeyEvent.KEYCODE_BACK && getCurrentFocus() == mInput) {

                    mInput.clearFocus();
                }

                return true;
            }
        });

        mediaButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Log.i(TAG, "onStart() -> mediaButton -> onClick");

                mediaButtonMenu = new PopupMenu(Chat.this, mediaButton);
                mediaButtonMenu.setOnMenuItemClickListener(Chat.this);
                mediaButtonMenu.inflate(R.menu.mediabutton_menu);
                mediaButtonMenu.show();
                mediaButtonMenuIsOpen = true;

                // Changes boolean value (used in OnConfigurationChanged) to determine whether menu is currently open.
                mediaButtonMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {

                    @Override
                    public void onDismiss(PopupMenu popupMenu) {

                        Log.i(TAG, "onStart() -> mediaButton -> onDismiss");

                        mediaButtonMenuIsOpen = false;
                        mediaButtonMenu.setOnDismissListener(null);
                    }
                });
            }
        });

        // onClickListener for sending recyclerviewlayout to Firebase.
        sendButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Log.i(TAG, "onStart() -> sendButton -> onClick");

                final String input = mInput.getText().toString();
                final Bundle extras = getIntent().getExtras();

                // Send recyclerviewlayout to Firebase.
                if (!input.equals("") || imageView.getVisibility() != View.GONE) {

                    // Check Boolean value from onStart();
                    if (newShape) {

                        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

                        if (shapeIsCircle) {

                            DatabaseReference firebaseCircles = rootRef.child("circles");
                            firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    if (imageView.getVisibility() != View.GONE) {

                                        // Upload the image to Firebase if it exists and is not already in the process of sending an image.
                                        if (uploadTask != null && uploadTask.isInProgress()) {

                                            Toast.makeText(Chat.this, "Image upload in progress", Toast.LENGTH_SHORT).show();
                                        } else {

                                            uploadImage();
                                        }
                                    } else {

                                        // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                                        messageSent = true;
                                        // Since the uuid doesn't already exist in Firebase, add the circle.
                                        CircleOptions circleOptions = new CircleOptions()
                                                .center(new LatLng(circleLatitude, circleLongitude))
                                                .clickable(true)
                                                .fillColor(fillColor)
                                                .radius(radius);
                                        CircleInformation circleInformation = new CircleInformation();
                                        circleInformation.setCircleOptions(circleOptions);
                                        circleInformation.setUUID(uuid);
                                        DatabaseReference newFirebaseCircle = FirebaseDatabase.getInstance().getReference().child("circles").push();
                                        newFirebaseCircle.setValue(circleInformation);

                                        MessageInformation messageInformation = new MessageInformation();
                                        messageInformation.setMessage(input);
                                        // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                        // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                        Object date = ServerValue.TIMESTAMP;
                                        messageInformation.setDate(date);
                                        if (extras != null) {

                                            String uuid = extras.getString("uuid");
                                            messageInformation.setUUID(uuid);
                                        }
                                        DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("messageThreads").push();
                                        newMessage.setValue(messageInformation);
                                        mInput.getText().clear();

                                        newShape = false;
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                    Toast.makeText(Chat.this, databaseError.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {

                            // Shape is not a circle.

                            DatabaseReference firebasePolygons = rootRef.child("polygons");
                            firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    if (imageView.getVisibility() != View.GONE) {

                                        // Upload the image to Firebase if it exists and is not already in the process of sending an image.
                                        if (uploadTask != null && uploadTask.isInProgress()) {

                                            Toast.makeText(Chat.this, "Image upload in progress", Toast.LENGTH_SHORT).show();
                                        } else {

                                            uploadImage();
                                        }
                                    } else {

                                        // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                                        messageSent = true;

                                        // Since the uuid doesn't already exist in Firebase, add the circle.
                                        if (threeMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }

                                        if (fourMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }

                                        if (fiveMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }

                                        if (sixMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }

                                        if (sevenMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }

                                        if (eightMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude), new LatLng(marker7Latitude, marker7Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }

                                        MessageInformation messageInformation = new MessageInformation();
                                        messageInformation.setMessage(input);
                                        // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                        // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                        Object date = ServerValue.TIMESTAMP;
                                        messageInformation.setDate(date);
                                        if (extras != null) {

                                            String uuid = extras.getString("uuid");
                                            messageInformation.setUUID(uuid);
                                        }
                                        DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("messageThreads").push();
                                        newMessage.setValue(messageInformation);
                                        mInput.getText().clear();
                                        newShape = false;
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                    Toast.makeText(Chat.this, databaseError.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } else {

                        // Shape is not new.

                        if (imageView.getVisibility() != View.GONE) {

                            // Upload the image to Firebase if it exists and is not already in the process of sending an image.
                            if (uploadTask != null && uploadTask.isInProgress()) {

                                Toast.makeText(Chat.this, "Image upload in progress", Toast.LENGTH_SHORT).show();
                            } else {

                                uploadImage();
                            }
                        } else {

                            // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                            messageSent = true;
                            MessageInformation messageInformation = new MessageInformation();
                            messageInformation.setMessage(input);
                            // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                            // This will cause onDataChange to fire twice; optimizations could be made in the future.
                            Object date = ServerValue.TIMESTAMP;
                            messageInformation.setDate(date);
                            if (extras != null) {

                                String uuid = extras.getString("uuid");
                                messageInformation.setUUID(uuid);
                            }
                            DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("messageThreads").push();
                            newMessage.setValue(messageInformation);
                            mInput.getText().clear();
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        // Prevent keyboard from opening.
        if (mInput != null) {

            mInput.clearFocus();
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.i(TAG, "onResume()");
    }

    @Override
    protected void onPause() {

        Log.i(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {

        Log.i(TAG, "onStop()");

        if (databaseReference != null) {

            databaseReference.removeEventListener(eventListener);
        }

        if (recyclerView != null) {

            recyclerView.clearOnScrollListeners();
            recyclerView.removeOnLayoutChangeListener(onLayoutChangeListener);
        }

        if (eventListener != null) {

            eventListener = null;
        }

        if (sendButton != null) {

            sendButton.setOnClickListener(null);
        }

        if (mInput != null) {

            mInput.setOnKeyListener(null);
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

    private void initRecyclerView() {

        // Initialize the RecyclerView
        Log.i(TAG, "initRecyclerView()");

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mUser, mTime, mImage, mText);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (index == -1 || messageSent) {

            // Scroll to bottom of recyclerviewlayout after first initialization and after sending a recyclerviewlayout.
            recyclerView.scrollToPosition(mText.size() - 1);
            messageSent = false;
        } else {

            // Set RecyclerView scroll position to prevent movement when Firebase gets updated and after screen orientation change.
            if (recyclerView.getLayoutManager() != null) {

                ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(index, top);
            } else {

                Log.e(TAG, "initRecyclerView -> recyclerView.getLayoutManager() == null");
                Crashlytics.logException(new RuntimeException("initRecyclerView -> recyclerView.getLayoutManager() == null"));
            }
        }

        // Close keyboard after sending a recyclerviewlayout.
        if (Chat.this.getCurrentFocus() != null) {

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {

                imm.hideSoftInputFromWindow(Chat.this.getCurrentFocus().getWindowToken(), 0);
            } else {

                Log.e(TAG, "initRecyclerView -> imm == null");
                Crashlytics.logException(new RuntimeException("initRecyclerView -> imm == null"));
            }
            if (mInput != null) {

                mInput.clearFocus();
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        switch (menuItem.getItemId()) {

            case R.id.selectImage:

                Log.i(TAG, "onMenuItemClick() -> selectImage");

                chooseImage();
                mediaButtonMenuIsOpen = false;
                return true;

            case R.id.takePhoto:

                Log.i(TAG, "onMenuItemClick() -> takePhoto");

                if (ContextCompat.checkSelfPermission(Chat.this,
                        Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Chat.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {

                    startActivityTakePhoto();
                } else {

                    checkPermissions();
                }
                mediaButtonMenuIsOpen = false;
                return true;

            default:
                return false;
        }
    }

    private void chooseImage() {

        Log.i(TAG, "chooseImage");

        Intent intent = new Intent();
        intent.setType("image/");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 1);
    }

    private void checkPermissions() {

        Log.i(TAG, "checkPermissions()");

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    Request_User_Camera_Code);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {

            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            new cameraPermissionAlertDialog(this).execute();
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Request_User_Write_External_Storage_Code);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            new writeExternalStoragePermissionAlertDialog(this).execute();
        } else {

            startActivityTakePhoto();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Request_User_Camera_Code) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {

                startActivityTakePhoto();
            } else if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                checkPermissions();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new cameraPermissionAlertDialog(this).execute();
            } else {

                // User denied permission and checked "Don't ask again!"
                Toast toast = Toast.makeText(Chat.this, "Camera permission is required. Please enable it manually through the Android settings menu.", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }

        if (requestCode == Request_User_Write_External_Storage_Code) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {

                startActivityTakePhoto();
            } else if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                checkPermissions();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new writeExternalStoragePermissionAlertDialog(this).execute();
            } else {

                // User denied permission and checked "Don't ask again!"
                Toast toast = Toast.makeText(Chat.this, "Camera permission is required. Please enable it manually through the Android settings menu.", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
    }

    private void startActivityTakePhoto() {

        Log.i(TAG, "startActivityTakePhoto()");

        // Permission was granted, yay! Do the location-related task you need to do.
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {

                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                imgURI = FileProvider.getUriForFile(Chat.this,
                        "com.example.android.fileprovider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imgURI);
                startActivityForResult(cameraIntent, Request_User_Camera_Code);
            }
        }
    }

    // Show an explanation as to why location permission is necessary.
    private static class cameraPermissionAlertDialog extends AsyncTask<Void, Void, Void> {

        AlertDialog.Builder builder;
        private WeakReference<Activity> activityWeakRef;

        cameraPermissionAlertDialog(Activity activity) {

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

            Log.i(TAG, "cameraPermissionAlertDialog -> onPostExecute()");

            if (activityWeakRef != null && activityWeakRef.get() != null) {

                builder = new AlertDialog.Builder(activityWeakRef.get());

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                builder.setCancelable(false)
                        .setTitle("Camera Permission Required")
                        .setMessage("HereBefore needs permission to use your camera to take pictures.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(activityWeakRef.get(),
                                        new String[]{Manifest.permission.CAMERA},
                                        Request_User_Camera_Code);
                            }
                        })
                        .create()
                        .show();
            }
        }
    }

    // Show an explanation as to why location permission is necessary.
    private static class writeExternalStoragePermissionAlertDialog extends AsyncTask<Void, Void, Void> {

        AlertDialog.Builder builder;
        private WeakReference<Activity> activityWeakRef;

        writeExternalStoragePermissionAlertDialog(Activity activity) {

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

            Log.i(TAG, "writeExternalStoragePermissionAlertDialog -> onPostExecute()");

            if (activityWeakRef != null && activityWeakRef.get() != null) {

                builder = new AlertDialog.Builder(activityWeakRef.get());

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                builder.setCancelable(false)
                        .setTitle("Storage Permission Required")
                        .setMessage("HereBefore needs permission to use your storage to save photos.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(activityWeakRef.get(),
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        Request_User_Write_External_Storage_Code);
                            }
                        })
                        .create()
                        .show();
            }
        }
    }

    private File createImageFile() throws IOException {

        Log.i(TAG, "createImageFile()");

        // Create an image file name
        String timeStamp = getDateTimeInstance().format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Log.i(TAG, "onActivityResult() -> Gallery");

            imgURI = data.getData();
            imageView.setImageURI(imgURI);
            imageView.setVisibility(View.VISIBLE);
        }

        if (requestCode == Request_User_Camera_Code && resultCode == RESULT_OK) {

            Log.i(TAG, "onActivityResult() -> Camera");

            Bitmap myBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            try {

                ExifInterface exif = new ExifInterface(currentPhotoPath);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                Log.d("EXIF", "Exif: " + orientation);
                Matrix matrix = new Matrix();
                if (orientation == 6) {

                    matrix.postRotate(90);
                } else if (orientation == 3) {

                    matrix.postRotate(180);
                } else if (orientation == 8) {

                    matrix.postRotate(270);
                }

                myBitmap = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(), myBitmap.getHeight(), matrix, true); // rotating bitmap
                String path = MediaStore.Images.Media.insertImage(Chat.this.getContentResolver(), myBitmap, "HereBefore_" + System.currentTimeMillis() + ".png", null);
                imgURI = Uri.parse(path);
                galleryAddPic();
                imageView.setImageBitmap(myBitmap);
                imageView.setVisibility(View.VISIBLE);
            } catch (IOException ex) {

                ex.printStackTrace();
            }
        }
    }

    private void galleryAddPic() {

        Log.i(TAG, "galleryAddPic()");

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imgURI);
        this.sendBroadcast(mediaScanIntent);
    }

    private void uploadImage() {

        Log.i(TAG, "uploadImage()");

        // Connect to Firebase.
        StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("images");
        final StorageReference storageReference = mStorageRef.child(System.currentTimeMillis() + "." + getExtension(imgURI));
        final String input = mInput.getText().toString();
        final Bundle extras = getIntent().getExtras();

        // Show the loading icon while the image is being uploaded to Firebase.
        findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);

        uploadTask = storageReference.putFile(imgURI)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {

                            @Override
                            public void onSuccess(Uri uri) {

                                Log.i(TAG, "uploadImage() -> onSuccess");

                                if (newShape) {

                                    // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                                    messageSent = true;

                                    if (shapeIsCircle) {

                                        // Since the uuid doesn't already exist in Firebase, add the circle.
                                        CircleOptions circleOptions = new CircleOptions()
                                                .center(new LatLng(circleLatitude, circleLongitude))
                                                .clickable(true)
                                                .fillColor(fillColor)
                                                .radius(radius);
                                        CircleInformation circleInformation = new CircleInformation();
                                        circleInformation.setCircleOptions(circleOptions);
                                        circleInformation.setUUID(uuid);
                                        DatabaseReference newFirebaseCircle = FirebaseDatabase.getInstance().getReference().child("circles").push();
                                        newFirebaseCircle.setValue(circleInformation);
                                    } else {

                                        // Since the uuid doesn't already exist in Firebase, add the circle.
                                        if (threeMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }

                                        if (fourMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }

                                        if (fiveMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }

                                        if (sixMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }

                                        if (sevenMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }

                                        if (eightMarkers) {

                                            PolygonOptions polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude), new LatLng(marker7Latitude, marker7Longitude))
                                                    .clickable(true)
                                                    .fillColor(fillColor);
                                            PolygonInformation polygonInformation = new PolygonInformation();
                                            polygonInformation.setPolygonOptions(polygonOptions);
                                            polygonInformation.setArea(polygonArea);
                                            polygonInformation.setUUID(uuid);
                                            DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("polygons").push();
                                            newFirebasePolygon.setValue(polygonInformation);
                                        }
                                    }

                                    newShape = false;
                                }

                                MessageInformation messageInformation = new MessageInformation();
                                messageInformation.setImageURL(uri.toString());
                                messageInformation.setMessage(input);
                                // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                Object date = ServerValue.TIMESTAMP;
                                messageInformation.setDate(date);
                                if (extras != null) {

                                    String uuid = extras.getString("uuid");
                                    messageInformation.setUUID(uuid);
                                }
                                DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("messageThreads").push();
                                newMessage.setValue(messageInformation);
                                mInput.getText().clear();
                                imageView.setVisibility(View.GONE);
                                findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {

                    @Override
                    public void onFailure(@NonNull Exception exception) {

                        // Handle unsuccessful uploads
                        findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                        Toast.makeText(Chat.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "uploadImage() -> onFailure -> " + exception.getMessage());
                    }
                });
    }

    private String getExtension(Uri uri) {

        Log.i(TAG, "getExtension()");

        ContentResolver cr = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cr.getType(uri));
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {

        // Called when the orientation of the screen changes.
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged()");

        // Reloads the popup when the orientation changes to prevent viewing issues.
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && mediaButtonMenuIsOpen) {

            mediaButtonMenu.dismiss();
            mediaButton.performClick();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && mediaButtonMenuIsOpen) {

            mediaButtonMenu.dismiss();
            mediaButton.performClick();
        }
    }
}