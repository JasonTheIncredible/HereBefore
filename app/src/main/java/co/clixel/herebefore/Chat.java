package co.clixel.herebefore;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
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
import android.widget.RelativeLayout;
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
import com.iceteck.silicompressorr.SiliCompressor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import id.zelory.compressor.Compressor;

import static java.text.DateFormat.getDateTimeInstance;

public class Chat extends AppCompatActivity implements
        PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "Chat";
    private static final int Request_ID_Take_Photo = 1700;
    private static final int Request_ID_Record_Video = 1800;
    private EditText mInput;
    private ArrayList<String> mTime = new ArrayList<>();
    private ArrayList<String> mImage = new ArrayList<>();
    private ArrayList<String> mVideo = new ArrayList<>();
    private ArrayList<String> mText = new ArrayList<>();
    private RecyclerView recyclerView;
    private static int index = -1;
    private static int top = -1;
    private DatabaseReference databaseReference;
    private ValueEventListener eventListener;
    private FloatingActionButton sendButton, mediaButton;
    private boolean reachedEndOfRecyclerView = false;
    private boolean recyclerViewHasScrolled = false;
    private boolean messageSent = false;
    private boolean mediaButtonMenuIsOpen, fileIsImage, checkPermissionsPicture, URIisFile, newShape, threeMarkers, fourMarkers, fiveMarkers, sixMarkers, sevenMarkers, eightMarkers, shapeIsCircle;
    private Boolean userIsWithinShape;
    private View.OnLayoutChangeListener onLayoutChangeListener;
    private String uuid;
    private Double polygonArea, circleLatitude, circleLongitude, radius,
            marker0Latitude, marker0Longitude, marker1Latitude, marker1Longitude, marker2Latitude, marker2Longitude, marker3Latitude, marker3Longitude, marker4Latitude, marker4Longitude, marker5Latitude, marker5Longitude, marker6Latitude, marker6Longitude, marker7Latitude, marker7Longitude;
    private int fillColor;
    private PopupMenu mediaButtonMenu;
    private ImageView imageView, videoImageView;
    public Uri imageURI, videoURI;
    private StorageTask uploadTask;
    private LinearLayoutManager recyclerViewLinearLayoutManager = new LinearLayoutManager(this);
    private File image, video;
    private byte[] byteArray;

    //TODO: Keep checking user's location while user is in recyclerviewlayout to see if they can keep messaging, add a recyclerviewlayout at the top notifying user of this. Add differentiation between messaging within area vs not.
    //TODO: When data gets changed, try to update only the affected items: https://stackoverflow.com/questions/27188536/recyclerview-scrolling-performance. Also, fix issue where images / videos are changing size with orientation change.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        setContentView(R.layout.chat);

        mediaButton = findViewById(R.id.mediaButton);
        imageView = findViewById(R.id.imageView);
        videoImageView = findViewById(R.id.videoImageView);
        mInput = findViewById(R.id.input);
        sendButton = findViewById(R.id.sendButton);
        recyclerView = findViewById(R.id.messageList);
        recyclerView.setItemViewCacheSize(20);

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
                if (recyclerViewLinearLayoutManager != null) {

                    mTime.clear();
                    mImage.clear();
                    mVideo.clear();
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
                                String imageURL = (String) ds.child("imageURL").getValue();
                                String videoURL = (String) ds.child("videoURL").getValue();
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
                                mImage.add(imageURL);
                                mVideo.add(videoURL);
                                mText.add(messageText);
                            }
                        }
                    }
                }

                // Read RecyclerView scroll position (for use in initRecyclerView).
                if (recyclerViewLinearLayoutManager != null) {

                    index = recyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
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

        // Hide the imageView or videoImageView if user presses the delete button.
        mInput.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_DEL && (imageView.getVisibility() == View.VISIBLE || videoImageView.getVisibility() == View.VISIBLE) &&
                        (mInput.getText().toString().trim().length() == 0 || mInput.getSelectionStart() == 0)) {

                    imageView.setVisibility(View.GONE);
                    videoImageView.setVisibility(View.GONE);
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
                if (!input.equals("") || imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                    // Check Boolean value from onStart();
                    if (newShape) {

                        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

                        if (shapeIsCircle) {

                            DatabaseReference firebaseCircles = rootRef.child("circles");
                            firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    if (imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                                        // Upload the image to Firebase if it exists and is not already in the process of sending an image.
                                        if (uploadTask != null && uploadTask.isInProgress()) {

                                            Toast.makeText(Chat.this, "Upload in progress", Toast.LENGTH_SHORT).show();
                                        } else {

                                            firebaseUpload();
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

                                    if (imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                                        // Upload the image to Firebase if it exists and is not already in the process of sending an image.
                                        if (uploadTask != null && uploadTask.isInProgress()) {

                                            Toast.makeText(Chat.this, "Upload in progress", Toast.LENGTH_SHORT).show();
                                        } else {

                                            firebaseUpload();
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

                        if (imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                            // Upload the image to Firebase if it exists and is not already in the process of sending an image.
                            if (uploadTask != null && uploadTask.isInProgress()) {

                                Toast.makeText(Chat.this, "Upload in progress", Toast.LENGTH_SHORT).show();
                            } else {

                                firebaseUpload();
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

        imageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Log.i(TAG, "imageView -> onClick");

                Intent Activity = new Intent(Chat.this, PhotoView.class);
                Activity.putExtra("imgURL", imageURI.toString());
                Chat.this.startActivity(Activity);
            }
        });

        videoImageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Log.i(TAG, "videoImageView -> onClick");

                Intent Activity = new Intent(Chat.this, co.clixel.herebefore.VideoView.class);
                Activity.putExtra("videoURL", videoURI.toString());
                Chat.this.startActivity(Activity);
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

        if (imageView != null) {

            imageView.setOnClickListener(null);
        }

        if (videoImageView != null) {

            videoImageView.setOnClickListener(null);
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

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mTime, mImage, mVideo, mText);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(recyclerViewLinearLayoutManager);

        if (index == -1 || messageSent) {

            // Scroll to bottom of recyclerviewlayout after first initialization and after sending a recyclerviewlayout.
            recyclerView.scrollToPosition(mTime.size() - 1);
            messageSent = false;
        } else {

            // Set RecyclerView scroll position to prevent position change when Firebase gets updated and after screen orientation change.
            recyclerViewLinearLayoutManager.scrollToPositionWithOffset(index, top);
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

            case R.id.browseGallery:

                Log.i(TAG, "onMenuItemClick() -> browseGallery");

                chooseFromGallery();
                // Set the views to GONE to prevent anything else from being sent to Firebase.
                if (videoImageView != null) {

                    videoImageView.setVisibility(View.GONE);
                }
                if (imageView != null) {

                    imageView.setVisibility(View.GONE);
                }
                mediaButtonMenuIsOpen = false;
                return true;

            case R.id.takePhoto:

                Log.i(TAG, "onMenuItemClick() -> takePhoto");

                if (checkPermissionsPicture()) {

                    startActivityTakePhoto();
                }
                // Set the views to GONE to prevent anything else from being sent to Firebase.
                if (videoImageView != null) {

                    videoImageView.setVisibility(View.GONE);
                }
                if (imageView != null) {

                    imageView.setVisibility(View.GONE);
                }
                mediaButtonMenuIsOpen = false;
                return true;

            case R.id.recordVideo:

                Log.i(TAG, "onMenuItemClick() -> recordVideo");

                if (checkPermissionsVideo()) {

                    startActivityRecordVideo();
                }
                // Set the views to GONE to prevent anything else from being sent to Firebase.
                if (videoImageView != null) {

                    videoImageView.setVisibility(View.GONE);
                }
                if (imageView != null) {

                    imageView.setVisibility(View.GONE);
                }
                mediaButtonMenuIsOpen = false;
                return true;

            default:
                return false;
        }
    }

    private void chooseFromGallery() {

        Log.i(TAG, "chooseFromGallery");

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 2);
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

            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), Request_ID_Take_Photo);
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

            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), Request_ID_Record_Video);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

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

                        if (cameraPermissions == PackageManager.PERMISSION_GRANTED
                                && externalStoragePermissions == PackageManager.PERMISSION_GRANTED) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Camera and Write External Storage permission granted.");
                            // Process the normal workflow.
                            startActivityTakePhoto();
                        } else {

                            if (cameraPermissions != PackageManager.PERMISSION_GRANTED) {

                                Log.d(TAG, "Request_ID_Take_Photo -> Camera permissions were not granted. Ask again.");

                                // Show an explanation to the user *asynchronously* -- don't block
                                // this thread waiting for the user's response! After the user
                                // sees the explanation, try again to request the permission.
                                new cameraPermissionAlertDialog(this).execute(checkPermissionsPicture);
                            } else {

                                Log.d(TAG, "Request_ID_Take_Photo -> Storage permissions were not granted. Ask again.");

                                // Show an explanation to the user *asynchronously* -- don't block
                                // this thread waiting for the user's response! After the user
                                // sees the explanation, try again to request the permission.
                                new writeExternalStoragePermissionAlertDialog(this).execute(checkPermissionsPicture);
                            }
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

                        if (cameraPermissions == PackageManager.PERMISSION_GRANTED
                                && externalStoragePermissions == PackageManager.PERMISSION_GRANTED
                                && audioPermissions == PackageManager.PERMISSION_GRANTED) {

                            Log.d(TAG, "Request_ID_Record_Video -> Camera, Write External Storage, and Record Audio permission granted.");
                            // Process the normal workflow.
                            startActivityRecordVideo();
                        } else {

                            if (cameraPermissions != PackageManager.PERMISSION_GRANTED) {

                                Log.d(TAG, "Request_ID_Record_Video -> Camera permissions were not granted. Ask again.");

                                // Show an explanation to the user *asynchronously* -- don't block
                                // this thread waiting for the user's response! After the user
                                // sees the explanation, try again to request the permission.
                                new cameraPermissionAlertDialog(this).execute(checkPermissionsPicture);
                            } else if (externalStoragePermissions != PackageManager.PERMISSION_GRANTED) {

                                Log.d(TAG, "Request_ID_Record_Video -> Storage permissions were not granted. Ask again.");

                                // Show an explanation to the user *asynchronously* -- don't block
                                // this thread waiting for the user's response! After the user
                                // sees the explanation, try again to request the permission.
                                new writeExternalStoragePermissionAlertDialog(this).execute(checkPermissionsPicture);
                            } else {

                                Log.d(TAG, "Request_ID_Record_Video -> Audio permissions were not granted. Ask again.");

                                // Show an explanation to the user *asynchronously* -- don't block
                                // this thread waiting for the user's response! After the user
                                // sees the explanation, try again to request the permission.
                                new audioPermissionAlertDialog(this).execute(checkPermissionsPicture);
                            }
                        }
                    }
                }

                break;
            }
        }
    }

    private void startActivityTakePhoto() {

        Log.i(TAG, "startActivityTakePhoto()");

        // Permission was granted, yay! Do the task you need to do.
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {

                photoFile = createImageFile();
            } catch (IOException ex) {

                // Error occurred while creating the File
                ex.printStackTrace();
                Toast.makeText(Chat.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                Crashlytics.logException(new RuntimeException("startActivityTakePhoto() -> photoFile error"));
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                imageURI = FileProvider.getUriForFile(Chat.this,
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
        if (videoIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the video should go
            File videoFile = null;
            try {

                videoFile = createVideoFile();
            } catch (IOException ex) {

                // Error occurred while creating the File
                ex.printStackTrace();
                Toast.makeText(Chat.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                Crashlytics.logException(new RuntimeException("startActivityRecordVideo() -> videoFile error"));
            }
            // Continue only if the File was successfully created
            if (videoFile != null) {

                videoURI = FileProvider.getUriForFile(Chat.this,
                        "com.example.android.fileprovider",
                        videoFile);
                videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI);
                // Limit the amount of time a video can be recorded (in seconds).
                videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
                startActivityForResult(videoIntent, 4);
            }
        }
    }

    // Show an explanation as to why permission is necessary.
    private static class cameraPermissionAlertDialog extends AsyncTask<Boolean, Void, Boolean> {

        private WeakReference<Chat> activityWeakRef;

        cameraPermissionAlertDialog(Chat activity) {

            activityWeakRef = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Boolean... booleans) {

            Boolean checkPermissionsPicture;
            checkPermissionsPicture = booleans[0]; // checkPermissionsPicture
            return checkPermissionsPicture;
        }

        @Override
        protected void onPostExecute(final Boolean checkPermissionsPicture) {

            super.onPostExecute(checkPermissionsPicture);

            Log.i(TAG, "cameraPermissionAlertDialog -> onPostExecute()");

            if (activityWeakRef != null && activityWeakRef.get() != null) {

                AlertDialog.Builder builder;

                builder = new AlertDialog.Builder(activityWeakRef.get());

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                builder.setCancelable(false)
                        .setTitle("Camera Permission Required")
                        .setMessage("Here Before needs permission to use your camera to take pictures or video. " +
                                "You may need to enable permission manually through the settings menu.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if (checkPermissionsPicture) {

                                    activityWeakRef.get().checkPermissionsPicture();
                                } else {

                                    activityWeakRef.get().checkPermissionsVideo();
                                }
                            }
                        })
                        .create()
                        .show();
            }
        }
    }

    // Show an explanation as to why permission is necessary.
    private static class writeExternalStoragePermissionAlertDialog extends AsyncTask<Boolean, Void, Boolean> {

        AlertDialog.Builder builder;
        private WeakReference<Chat> activityWeakRef;

        writeExternalStoragePermissionAlertDialog(Chat activity) {

            activityWeakRef = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Boolean... booleans) {

            Boolean checkPermissionsPicture;
            checkPermissionsPicture = booleans[0]; // checkPermissionsPicture
            return checkPermissionsPicture;
        }

        @Override
        protected void onPostExecute(final Boolean checkPermissionsPicture) {

            super.onPostExecute(checkPermissionsPicture);

            Log.i(TAG, "writeExternalStoragePermissionAlertDialog -> onPostExecute()");

            if (activityWeakRef != null && activityWeakRef.get() != null) {

                builder = new AlertDialog.Builder(activityWeakRef.get());

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                builder.setCancelable(false)
                        .setTitle("Storage Permission Required")
                        .setMessage("Here Before needs permission to use your storage to save photos or video. " +
                                "You may need to enable permission manually through the settings menu.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if (checkPermissionsPicture) {

                                    activityWeakRef.get().checkPermissionsPicture();
                                } else {

                                    activityWeakRef.get().checkPermissionsVideo();
                                }
                            }
                        })
                        .create()
                        .show();
            }
        }
    }

    // Show an explanation as to why permission is necessary.
    private static class audioPermissionAlertDialog extends AsyncTask<Boolean, Void, Boolean> {

        AlertDialog.Builder builder;
        private WeakReference<Chat> activityWeakRef;

        audioPermissionAlertDialog(Chat activity) {

            activityWeakRef = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Boolean... booleans) {

            Boolean checkPermissionsPicture;
            checkPermissionsPicture = booleans[0]; // checkPermissionsPicture
            return checkPermissionsPicture;
        }

        @Override
        protected void onPostExecute(final Boolean checkPermissionsPicture) {

            super.onPostExecute(checkPermissionsPicture);

            Log.i(TAG, "audioPermissionAlertDialog -> onPostExecute()");

            if (activityWeakRef != null && activityWeakRef.get() != null) {

                builder = new AlertDialog.Builder(activityWeakRef.get());

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                builder.setCancelable(false)
                        .setTitle("Audio Permission Required")
                        .setMessage("Here Before needs permission to record audio during video recording. " +
                                "You may need to enable permission manually through the settings menu.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if (checkPermissionsPicture) {

                                    activityWeakRef.get().checkPermissionsPicture();
                                } else {

                                    activityWeakRef.get().checkPermissionsVideo();
                                }
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
        String imageFileName = "HereBefore_" + System.currentTimeMillis() + "_jpeg";
        File storageDir = this.getCacheDir();
        image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpeg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    private File createVideoFile() throws IOException {

        Log.i(TAG, "createVideoFile()");

        // Create a video file name
        String videoFileName = "HereBefore_" + System.currentTimeMillis() + "_mp4";
        File storageDir = this.getCacheDir();
        video = File.createTempFile(
                videoFileName,  /* prefix */
                ".mp4",         /* suffix */
                storageDir      /* directory */
        );

        return video;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Log.i(TAG, "onActivityResult() -> Gallery");

            fileIsImage = true;

            imageURI = data.getData();

            if (getExtension(imageURI).equals("gif")) {

                // For use in uploadImage().
                URIisFile = true;

                // GIF. No need for compression.
                Glide.with(this)
                        .load(imageURI)
                        .apply(new RequestOptions().override(640, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                        .into(imageView);
            } else {

                // For use in uploadImage().
                URIisFile = false;

                // Not GIF. Needs compression.
                new imageCompressAsyncTask(this).execute(imageURI.toString());
            }

            // Change textView to be to the right of imageView.
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mInput.getLayoutParams();
            params.addRule(RelativeLayout.END_OF, R.id.imageView);
            mInput.setLayoutParams(params);

            imageView.setVisibility(View.VISIBLE);
        }

        if (requestCode == 3 && resultCode == RESULT_OK) {

            Log.i(TAG, "onActivityResult() -> Camera");

            fileIsImage = true;

            // For use in uploadImage().
            URIisFile = false;

            // Change textView to be to the right of imageView.
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mInput.getLayoutParams();
            params.addRule(RelativeLayout.END_OF, R.id.imageView);
            mInput.setLayoutParams(params);

            new imageCompressAndAddToGalleryAsyncTask(this).execute();
        }

        if (requestCode == 4 && resultCode == RESULT_OK) {

            Log.i(TAG, "onActivityResult() -> Video");

            fileIsImage = false;

            new videoCompressAndAddToGalleryAsyncTask(this).execute(video.getAbsolutePath(), video.getParent());
        }
    }

    private static class imageCompressAsyncTask extends AsyncTask<String, String, String> {

        private WeakReference<Chat> activityWeakRef;

        private imageCompressAsyncTask(Chat activity) {

            activityWeakRef = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isFinishing()) return;

            // Show the loading icon while the image is being compressed.
            activity.findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... paths) {

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isFinishing()) return "2";

            Uri mImageURI = Uri.parse(paths[0]);

            InputStream imageStream = null;
            try {

                imageStream = activity.getContentResolver().openInputStream(mImageURI);
            } catch (FileNotFoundException ex) {

                ex.printStackTrace();
                Toast.makeText(activity, ex.getMessage(), Toast.LENGTH_LONG).show();
                Crashlytics.logException(new RuntimeException("onActivityResult() -> gallery imageStream error"));
            }
            Bitmap bmp = BitmapFactory.decodeStream(imageStream);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 50, stream);
            activity.byteArray = stream.toByteArray();
            bmp.recycle();
            try {

                stream.close();
            } catch (IOException ex) {

                ex.printStackTrace();
                Toast.makeText(activity, ex.getMessage(), Toast.LENGTH_LONG).show();
                Crashlytics.logException(new RuntimeException("onActivityResult() -> gallery stream.close()"));
            }

            return "2";
        }

        @Override
        protected void onPostExecute(String meaninglessString) {

            super.onPostExecute(meaninglessString);

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isFinishing()) return;

            // Show the preview for the lowest quality image to save time and resources.
            if (activity.URIisFile) {

                Glide.with(activity)
                        .load(activity.imageURI)
                        .apply(new RequestOptions().override(640, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                        .into(activity.imageView);
            } else {

                Glide.with(activity)
                        .load(activity.byteArray)
                        .apply(new RequestOptions().override(640, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                        .into(activity.imageView);
            }

            activity.findViewById(R.id.loadingIcon).setVisibility(View.INVISIBLE);
        }
    }

    private static class imageCompressAndAddToGalleryAsyncTask extends AsyncTask<String, String, String> {

        private WeakReference<Chat> activityWeakRef;

        private imageCompressAndAddToGalleryAsyncTask(Chat activity) {

            activityWeakRef = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isFinishing()) return;

            // Show the loading icon while the image is being compressed.
            activity.findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... paths) {

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isFinishing()) return "2";

            try {
                // Save a non-compressed image to the gallery.
                Bitmap imageBitmapFull = new Compressor(activity)
                        .setMaxWidth(10000)
                        .setMaxHeight(10000)
                        .setQuality(100)
                        .setCompressFormat(Bitmap.CompressFormat.PNG)
                        .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES).getAbsolutePath())
                        .compressToBitmap(activity.image);
                MediaStore.Images.Media.insertImage(activity.getContentResolver(), imageBitmapFull, "HereBefore_" + System.currentTimeMillis() + "_PNG", null);

                // Create a compressed image.
                Bitmap mImageBitmap = new Compressor(activity)
                        .setMaxWidth(480)
                        .setMaxHeight(640)
                        .setQuality(50)
                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                        .compressToBitmap(activity.image);

                // Convert the bitmap to a byteArray for use in uploadImage().
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(mImageBitmap.getWidth() * mImageBitmap.getHeight());
                mImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, buffer);
                activity.byteArray = buffer.toByteArray();
            } catch (IOException ex) {

                ex.printStackTrace();
                Toast.makeText(activity, ex.getMessage(), Toast.LENGTH_LONG).show();
                Crashlytics.logException(new RuntimeException("onActivityResult() -> Request_User_Camera_Code exception"));
            }

            return "2";
        }

        @Override
        protected void onPostExecute(String meaninglessString) {

            super.onPostExecute(meaninglessString);

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isFinishing()) return;

            Glide.with(activity)
                    .load(activity.byteArray)
                    .apply(new RequestOptions().override(640, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                    .into(activity.imageView);

            activity.imageView.setVisibility(View.VISIBLE);

            activity.findViewById(R.id.loadingIcon).setVisibility(View.INVISIBLE);
        }
    }

    private static class videoCompressAndAddToGalleryAsyncTask extends AsyncTask<String, String, String> {

        private WeakReference<Chat> activityWeakRef;
        private Bitmap mBmp;
        private int mBmpWidth;
        private int mBmpHeight;

        private videoCompressAndAddToGalleryAsyncTask(Chat activity) {

            activityWeakRef = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isFinishing()) return;

            // Show the loading icon while the video is being compressed.
            activity.findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... paths) {

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isFinishing()) return "2";

            String filePath = null;
            try {

                filePath = SiliCompressor.with(activity).compressVideo(paths[0], paths[1], 0, 0, 3000000);
            } catch (URISyntaxException e) {

                e.printStackTrace();
            }

            // Add uncompressed video to gallery.
            // Save the name and description of a video in a ContentValues map.
            ContentValues values = new ContentValues(3);
            values.put(MediaStore.Video.Media.TITLE, "Here_Before_" + System.currentTimeMillis());
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, activity.video.getAbsolutePath());

            // Add a new record (identified by uri) without the video, but with the values just set.
            Uri uri = activity.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

            // Now get a handle to the file for that record, and save the data into it.
            try {

                InputStream is = new FileInputStream(activity.video);
                if (uri != null) {

                    OutputStream os = activity.getContentResolver().openOutputStream(uri);
                    byte[] buffer = new byte[4096]; // tweaking this number may increase performance
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        if (os != null) {

                            os.write(buffer, 0, len);
                            os.flush();
                        }
                    }
                    is.close();
                    if (os != null) {

                        os.close();
                    }
                }
            } catch (Exception e) {

                Log.e(TAG, "Exception while writing video: ", e);
            }

            activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

            if (filePath == null) {

                return "2";
            }

            // Turn the compressed video into a bitmap.
            File videoFile = new File(filePath);

            float length = videoFile.length() / 1024f; // Size in KB
            String value;
            if (length >= 1024) {

                value = length / 1024f + " MB";
            } else {

                value = length + " KB";
            }

            String text = String.format(Locale.US, "%s\nName: %s\nSize: %s", "Video compression complete", videoFile.getName(), value);
            Log.e(TAG, "text: " + text);
            Log.e(TAG, "imageFile.getName(): " + videoFile.getName());
            Log.e(TAG, "Path 0: " + filePath);

            try {

                File file = new File(filePath);
                InputStream inputStream; //You can get an inputStream using any IO API
                inputStream = new FileInputStream(file.getAbsolutePath());
                byte[] buffer = new byte[8192];
                int bytesRead;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);
                activity.videoURI = Uri.fromFile(videoFile);
                try {

                    while ((bytesRead = inputStream.read(buffer)) != -1) {

                        output64.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {

                    e.printStackTrace();
                    Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
                    Crashlytics.logException(new RuntimeException("videoCompressAsyncTask -> onPostExecute -> inner"));
                }
                output64.close();

                // Change the videoImageView's orientation depending on the orientation of the video.
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                // Set the video Uri as data source for MediaMetadataRetriever
                retriever.setDataSource(filePath);
                // Get one "frame"/bitmap - * NOTE - no time was set, so the first available frame will be used
                mBmp = retriever.getFrameAtTime(1);
                // Get the bitmap width and height
                mBmpWidth = mBmp.getWidth();
                mBmpHeight = mBmp.getHeight();

            } catch (IOException e) {

                e.printStackTrace();
                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
                Crashlytics.logException(new RuntimeException("videoCompressAsyncTask -> onPostExecute -> outer"));
            }
            return "2";
        }

        @Override
        protected void onPostExecute(String meaninglessString) {

            super.onPostExecute(meaninglessString);

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isFinishing()) return;

            if (mBmp != null && mBmpHeight != 0 && mBmpWidth != 0) {

                // Change textView to be to the right of videoImageView.
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) activity.mInput.getLayoutParams();
                params.addRule(RelativeLayout.END_OF, R.id.videoImageView);
                activity.mInput.setLayoutParams(params);

                final float scale = activity.getResources().getDisplayMetrics().density;
                if (mBmpWidth > mBmpHeight) {

                    activity.videoImageView.getLayoutParams().width = (int) (50 * scale + 0.5f); // Convert 50dp to px.
                } else {

                    activity.videoImageView.getLayoutParams().width = (int) (30 * scale + 0.5f); // Convert 30dp to px.
                }

                activity.videoImageView.setImageBitmap(mBmp);
                activity.videoImageView.setVisibility(View.VISIBLE);
            }

            activity.findViewById(R.id.loadingIcon).setVisibility(View.INVISIBLE);
        }
    }

    private void firebaseUpload() {

        Log.i(TAG, "firebaseUploadImage()");

        final Bundle extras = getIntent().getExtras();

        // Show the loading icon while the image is being uploaded to Firebase.
        findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);

        if (URIisFile && fileIsImage) {

            // File and image.

            final StorageReference storageReferenceImage = FirebaseStorage.getInstance().getReference("images").child(System.currentTimeMillis() + "." + getExtension(imageURI));
            uploadTask = storageReferenceImage.putFile(imageURI);

            storageReferenceImage.putFile(imageURI).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    storageReferenceImage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {

                        @Override
                        public void onSuccess(Uri uri) {

                            Log.i(TAG, "uploadImage() -> onSuccess");

                            if (newShape) {

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

                            // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                            messageSent = true;

                            MessageInformation messageInformation = new MessageInformation();
                            messageInformation.setImageURL(uri.toString());
                            if (mInput.getText().toString().trim().length() != 0) {

                                messageInformation.setMessage(mInput.getText().toString());
                            }
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
                            if (image != null) {

                                deleteDirectory(image);
                            }
                            findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                        }
                    });
                }
            })
                    .addOnFailureListener(new OnFailureListener() {

                        @Override
                        public void onFailure(@NonNull Exception ex) {

                            // Handle unsuccessful uploads
                            findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                            Toast.makeText(Chat.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "firebaseUpload() -> URIisFile && fileIsImage -> onFailure -> " + ex.getMessage());
                        }
                    });
        } else if (!fileIsImage) {

            // Video.

            final StorageReference storageReferenceVideo = FirebaseStorage.getInstance().getReference("videos").child(System.currentTimeMillis() + "." + getExtension(videoURI));
            uploadTask = storageReferenceVideo.putFile(videoURI);

            storageReferenceVideo.putFile(videoURI).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    storageReferenceVideo.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {

                        @Override
                        public void onSuccess(Uri uri) {

                            Log.i(TAG, "uploadImage() -> onSuccess");

                            if (newShape) {

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

                            // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                            messageSent = true;

                            MessageInformation messageInformation = new MessageInformation();
                            messageInformation.setVideoURL(uri.toString());
                            if (mInput.getText().toString().trim().length() != 0) {

                                messageInformation.setMessage(mInput.getText().toString());
                            }
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
                            videoImageView.setVisibility(View.GONE);
                            if (video != null) {

                                deleteDirectory(video);
                            }
                            findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                        }
                    });
                }
            })
                    .addOnFailureListener(new OnFailureListener() {

                        @Override
                        public void onFailure(@NonNull Exception ex) {

                            // Handle unsuccessful uploads
                            findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                            Toast.makeText(Chat.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "firebaseUpload() -> !fileIsImage -> onFailure -> " + ex.getMessage());
                        }
                    });
        } else {

            // byteArray and image.

            final StorageReference storageReferenceImage = FirebaseStorage.getInstance().getReference("images").child(System.currentTimeMillis() + "." + getExtension(imageURI));
            uploadTask = storageReferenceImage.putBytes(byteArray);

            storageReferenceImage.putBytes(byteArray).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    storageReferenceImage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {

                        @Override
                        public void onSuccess(Uri uri) {

                            Log.i(TAG, "uploadImage() -> onSuccess");

                            if (newShape) {

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

                            // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                            messageSent = true;

                            MessageInformation messageInformation = new MessageInformation();
                            messageInformation.setImageURL(uri.toString());
                            if (mInput.getText().toString().trim().length() != 0) {

                                messageInformation.setMessage(mInput.getText().toString());
                            }
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
                            if (image != null) {

                                deleteDirectory(image);
                            }
                            findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                        }
                    });
                }
            })
                    .addOnFailureListener(new OnFailureListener() {

                        @Override
                        public void onFailure(@NonNull Exception ex) {

                            // Handle unsuccessful uploads
                            findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                            Toast.makeText(Chat.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "firebaseUpload() -> else -> onFailure -> " + ex.getMessage());
                        }
                    });
        }
    }

    private String getExtension(Uri uri) {

        Log.i(TAG, "getExtension()");

        ContentResolver cr = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cr.getType(uri));
    }

    private void deleteDirectory(File file) {

        Log.i(TAG, "deleteDirectory()");

        if(file.exists()) {

            if (file.isDirectory()) {

                File[] files = file.listFiles();

                if (files != null) {

                    for (File value : files) {

                        if (value.isDirectory()) {

                            deleteDirectory(value);
                        } else {

                            if (value.delete()) {
                            } else {}
                        }
                    }
                }
            }

            if (file.delete()) {
            } else {}
        }
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