package co.clixel.herebefore;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
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
import androidx.exifinterface.media.ExifInterface;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.Parcel;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
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
import com.linkedin.android.spyglass.mentions.Mentionable;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsVisibilityManager;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizer;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizerConfig;
import com.linkedin.android.spyglass.tokenization.interfaces.QueryTokenReceiver;
import com.linkedin.android.spyglass.ui.MentionsEditText;

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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import id.zelory.compressor.Compressor;

import static java.text.DateFormat.getDateTimeInstance;

public class Chat extends AppCompatActivity implements
        PopupMenu.OnMenuItemClickListener,
        QueryTokenReceiver,
        SuggestionsVisibilityManager {

    private static final String TAG = "Chat";
    private static final int Request_ID_Take_Photo = 1700, Request_ID_Record_Video = 1800;
    private MentionsEditText mInput;
    private ArrayList<String> mTime = new ArrayList<>(), mUser = new ArrayList<>(), mImage = new ArrayList<>(), mVideo = new ArrayList<>(), mText = new ArrayList<>(), mSuggestions = new ArrayList<>(), allMentions = new ArrayList<>();
    private ArrayList<Boolean> mUserIsWithinShape = new ArrayList<>();
    private ArrayList<String> removedMentionDuplicates;
    private RecyclerView chatRecyclerView, mentionsRecyclerView;
    private static int index = -1, top = -1;
    private DatabaseReference databaseReference;
    private ValueEventListener eventListener;
    private FloatingActionButton sendButton, mediaButton;
    private boolean theme, needLoadingIcon = false, reachedEndOfRecyclerView = false, recyclerViewHasScrolled = false, messageSent = false, sendButtonClicked = false, mediaButtonMenuIsOpen, fileIsImage, checkPermissionsPicture, URIisFile,
            newShape, threeMarkers, fourMarkers, fiveMarkers, sixMarkers, sevenMarkers, eightMarkers, shapeIsCircle;
    private Boolean userIsWithinShape;
    private View.OnLayoutChangeListener onLayoutChangeListener;
    private String uuid;
    private Double polygonArea, circleLatitude, circleLongitude, radius,
            marker0Latitude, marker0Longitude, marker1Latitude, marker1Longitude, marker2Latitude, marker2Longitude, marker3Latitude, marker3Longitude, marker4Latitude, marker4Longitude, marker5Latitude, marker5Longitude, marker6Latitude, marker6Longitude, marker7Latitude, marker7Longitude;
    private PopupMenu mediaButtonMenu;
    private ImageView imageView, videoImageView;
    public Uri imageURI, videoURI;
    private StorageTask uploadTask;
    private LinearLayoutManager chatRecyclerViewLinearLayoutManager = new LinearLayoutManager(this);
    private File image, video;
    private byte[] byteArray;
    private View loadingIcon;
    private SharedPreferences sharedPreferences;
    private Toast shortToast, longToast;
    private static final String BUCKET = "text-suggestions";
    private static final WordTokenizerConfig tokenizerConfig = new WordTokenizerConfig
            .Builder()
            .setWordBreakChars(", ")
            .setExplicitChars("@")
            .setThreshold(1)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        setContentView(R.layout.chat);

        AdView bannerAd = findViewById(R.id.chatBanner);

        // Search I/Ads: in Logcat to find ID and/or W/Ads for other info.
        // List<String> testDeviceIds = Collections.singletonList("814BF63877CBD71E91F9D7241907F4FF");
        RequestConfiguration requestConfiguration
                = new RequestConfiguration.Builder()
                //.setTestDeviceIds(testDeviceIds)
                .build();
        MobileAds.setRequestConfiguration(requestConfiguration);

        AdRequest adRequest = new AdRequest.Builder().build();
        bannerAd.loadAd(adRequest);

        mediaButton = findViewById(R.id.mediaButton);
        imageView = findViewById(R.id.imageView);
        videoImageView = findViewById(R.id.videoImageView);
        mInput = findViewById(R.id.input);
        sendButton = findViewById(R.id.sendButton);
        chatRecyclerView = findViewById(R.id.messageList);
        mentionsRecyclerView = findViewById(R.id.suggestionsList);
        loadingIcon = findViewById(R.id.loadingIcon);

        // Make the loadingIcon visible upon the first load, as it can sometimes take a while to show anything. It should be made invisible in initRecyclerView().
        if (loadingIcon != null) {

            loadingIcon.setVisibility(View.VISIBLE);
        }

        // Get info from Map.java
        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            newShape = extras.getBoolean("newShape");
            uuid = extras.getString("uuid");
            userIsWithinShape = extras.getBoolean("userIsWithinShape");
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

        if (userIsWithinShape) {

            mInput.setHint("Message from within shape...");
        } else {

            mInput.setHint("Message from outside shape...");
        }
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Update to the user's preferences.
        loadPreferences();
        updatePreferences();

        // Connect to Firebase.
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        databaseReference = rootRef.child("MessageThreads");
        eventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                // Clear the RecyclerView before adding new entries to prevent duplicates.
                if (chatRecyclerViewLinearLayoutManager != null) {

                    mTime.clear();
                    mUser.clear();
                    mSuggestions.clear();
                    mImage.clear();
                    mVideo.clear();
                    mText.clear();
                    mUserIsWithinShape.clear();
                }

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    // If the uuid brought from Map.java equals the uuid attached to the recyclerviewlayout in Firebase, load it into the RecyclerView.
                    String shapeUUID = (String) ds.child("uuid").getValue();
                    if (shapeUUID != null) {

                        if (shapeUUID.equals(uuid)) {

                            Long serverDate = (Long) ds.child("date").getValue();
                            String user = (String) ds.child("userUUID").getValue();
                            // Used when a user mentions another user with "@".
                            mSuggestions.add(user);
                            String imageURL = (String) ds.child("imageURL").getValue();
                            String videoURL = (String) ds.child("videoURL").getValue();
                            String messageText = (String) ds.child("message").getValue();
                            Boolean userIsWithinShape = (Boolean) ds.child("userIsWithinShape").getValue();
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
                            mUser.add(user);
                            mImage.add(imageURL);
                            mVideo.add(videoURL);
                            mText.add(messageText);
                            mUserIsWithinShape.add(userIsWithinShape);
                        }
                    }
                }

                // Read RecyclerView scroll position (for use in initRecyclerView).
                if (chatRecyclerViewLinearLayoutManager != null) {

                    index = chatRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
                    View v = chatRecyclerView.getChildAt(0);
                    top = (v == null) ? 0 : (v.getTop() - chatRecyclerView.getPaddingTop());
                }

                initChatAdapter();

                // Check RecyclerView scroll state (to allow the layout to move up when keyboard appears).
                chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

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
                chatRecyclerView.addOnLayoutChangeListener(onLayoutChangeListener = new View.OnLayoutChangeListener() {

                    @Override
                    public void onLayoutChange(View v,
                                               int left, int top, int right, int bottom,
                                               int oldLeft, int oldTop, int oldRight, int oldBottom) {

                        if (reachedEndOfRecyclerView || !recyclerViewHasScrolled) {

                            if (bottom < oldBottom) {

                                if (chatRecyclerView.getAdapter() != null && chatRecyclerView.getAdapter().getItemCount() > 0) {

                                    chatRecyclerView.postDelayed(new Runnable() {

                                        @Override
                                        public void run() {

                                            chatRecyclerView.smoothScrollToPosition(

                                                    chatRecyclerView.getAdapter().getItemCount() - 1);
                                        }
                                    }, 100);
                                }
                            }
                        }
                    }
                });
            }

            public void onCancelled(@NonNull DatabaseError databaseError) {

                toastMessageLong(databaseError.getMessage());
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
                    imageView.setImageDrawable(null);
                    videoImageView.setVisibility(View.GONE);
                    videoImageView.setImageDrawable(null);
                }

                // Keep "return false" or the enter key will not go to the next line.
                return false;
            }
        });

        mInput.setTokenizer(new WordTokenizer(tokenizerConfig));
        mInput.setQueryTokenReceiver(this);
        mInput.setSuggestionsVisibilityManager(this);

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

                // Prevent double clicking the send button.
                if (sendButtonClicked) {

                    return;
                }

                sendButtonClicked = true;

                final String input = mInput.getText().toString();

                // Send recyclerviewlayout to Firebase.
                if (!input.equals("") || imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                    // Check Boolean value from onStart();
                    if (newShape) {

                        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

                        if (shapeIsCircle) {

                            DatabaseReference firebaseCircles = rootRef.child("Circles");
                            firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    if (imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                                        // Upload the image to Firebase if it exists and is not already in the process of sending an image.
                                        if (uploadTask != null && uploadTask.isInProgress()) {

                                            toastMessageShort("Upload in progress");
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
                                                .radius(radius);
                                        CircleInformation circleInformation = new CircleInformation();
                                        circleInformation.setCircleOptions(circleOptions);
                                        circleInformation.setUUID(uuid);
                                        DatabaseReference newFirebaseCircle = FirebaseDatabase.getInstance().getReference().child("Circles").push();
                                        newFirebaseCircle.setValue(circleInformation);

                                        MessageInformation messageInformation = new MessageInformation();
                                        messageInformation.setMessage(input);
                                        // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                        // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                        Object date = ServerValue.TIMESTAMP;
                                        messageInformation.setDate(date);
                                        String userUUID = UUID.randomUUID().toString();
                                        messageInformation.setUserUUID(userUUID);
                                        messageInformation.setUUID(uuid);
                                        messageInformation.setUserIsWithinShape(userIsWithinShape);
                                        DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").push();
                                        newMessage.setValue(messageInformation);

                                        // Get user info for user-user messaging purposes.
                                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                                            UserInformation userInformation = new UserInformation();
                                            userInformation.setEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                                            userInformation.setUserUUID(userUUID);
                                            // Get the token assigned by Firebase when the user signed up / signed in.
                                            String token = sharedPreferences.getString("FIREBASE_TOKEN", "null");
                                            userInformation.setToken(token);
                                            DatabaseReference userInfo = FirebaseDatabase.getInstance().getReference().child("Users").push();
                                            userInfo.setValue(userInformation);
                                        }

                                        mInput.getText().clear();
                                        newShape = false;
                                        sendButtonClicked = false;

                                        messageUserIfNeeded();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                    toastMessageLong(databaseError.getMessage());

                                    sendButtonClicked = false;
                                }
                            });
                        } else {

                            // Shape is not a circle.

                            DatabaseReference firebasePolygons = rootRef.child("Polygons");
                            firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    if (imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                                        // Upload the image to Firebase if it exists and is not already in the process of sending an image.
                                        if (uploadTask != null && uploadTask.isInProgress()) {

                                            toastMessageShort("Upload in progress");

                                            sendButtonClicked = false;
                                        } else {

                                            firebaseUpload();
                                        }
                                    } else {

                                        PolygonOptions polygonOptions = null;

                                        // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                                        messageSent = true;

                                        // Since the uuid doesn't already exist in Firebase, add the circle.
                                        if (threeMarkers) {

                                            polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude))
                                                    .clickable(true);
                                        }

                                        if (fourMarkers) {

                                            polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude))
                                                    .clickable(true);
                                        }

                                        if (fiveMarkers) {

                                            polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude))
                                                    .clickable(true);
                                        }

                                        if (sixMarkers) {

                                            polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude))
                                                    .clickable(true);
                                        }

                                        if (sevenMarkers) {

                                            polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude))
                                                    .clickable(true);
                                        }

                                        if (eightMarkers) {

                                            polygonOptions = new PolygonOptions()
                                                    .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude), new LatLng(marker7Latitude, marker7Longitude))
                                                    .clickable(true);
                                        }

                                        PolygonInformation polygonInformation = new PolygonInformation();
                                        polygonInformation.setPolygonOptions(polygonOptions);
                                        polygonInformation.setArea(polygonArea);
                                        polygonInformation.setUUID(uuid);
                                        DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("Polygons").push();
                                        newFirebasePolygon.setValue(polygonInformation);

                                        MessageInformation messageInformation = new MessageInformation();
                                        messageInformation.setMessage(input);
                                        // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                        // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                        Object date = ServerValue.TIMESTAMP;
                                        messageInformation.setDate(date);
                                        String userUUID = UUID.randomUUID().toString();
                                        messageInformation.setUserUUID(userUUID);
                                        messageInformation.setUUID(uuid);
                                        messageInformation.setUserIsWithinShape(userIsWithinShape);
                                        DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").push();
                                        newMessage.setValue(messageInformation);

                                        // Get user info for user-user messaging purposes.
                                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                                            UserInformation userInformation = new UserInformation();
                                            userInformation.setEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                                            userInformation.setUserUUID(userUUID);
                                            // Get the token assigned by Firebase when the user signed up / signed in.
                                            String token = sharedPreferences.getString("FIREBASE_TOKEN", "null");
                                            userInformation.setToken(token);
                                            DatabaseReference userInfo = FirebaseDatabase.getInstance().getReference().child("Users").push();
                                            userInfo.setValue(userInformation);
                                        }

                                        mInput.getText().clear();
                                        newShape = false;
                                        sendButtonClicked = false;

                                        messageUserIfNeeded();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                    toastMessageLong(databaseError.getMessage());

                                    sendButtonClicked = false;
                                }
                            });
                        }
                    } else {

                        // Shape is not new.

                        if (imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                            // Upload the image to Firebase if it exists and is not already in the process of sending an image.
                            if (uploadTask != null && uploadTask.isInProgress()) {

                                toastMessageShort("Upload in progress");

                                sendButtonClicked = false;
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
                            String userUUID = UUID.randomUUID().toString();
                            messageInformation.setUserUUID(userUUID);
                            messageInformation.setUUID(uuid);
                            messageInformation.setUserIsWithinShape(userIsWithinShape);
                            DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").push();
                            newMessage.setValue(messageInformation);

                            // Get user info for user-user messaging purposes.
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                                UserInformation userInformation = new UserInformation();
                                userInformation.setEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                                userInformation.setUserUUID(userUUID);
                                // Get the token assigned by Firebase when the user signed up / signed in.
                                String token = sharedPreferences.getString("FIREBASE_TOKEN", "null");
                                userInformation.setToken(token);
                                DatabaseReference userInfo = FirebaseDatabase.getInstance().getReference().child("Users").push();
                                userInfo.setValue(userInformation);
                            }

                            mInput.getText().clear();
                            sendButtonClicked = false;

                            messageUserIfNeeded();
                        }
                    }
                }

                // Close keyboard.
                if (Chat.this.getCurrentFocus() != null) {

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {

                        imm.hideSoftInputFromWindow(Chat.this.getCurrentFocus().getWindowToken(), 0);
                    } else {

                        Log.e(TAG, "onStart() -> sendButton -> imm == null");
                        Crashlytics.logException(new RuntimeException("onStart() -> sendButton -> imm == null"));
                    }
                    if (mInput != null) {

                        mInput.clearFocus();
                    }
                }
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Log.i(TAG, "imageView -> onClick");

                cancelToasts();

                Intent Activity = new Intent(Chat.this, PhotoView.class);
                Activity.putExtra("imgURL", imageURI.toString());
                Chat.this.startActivity(Activity);
            }
        });

        videoImageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Log.i(TAG, "videoImageView -> onClick");

                cancelToasts();

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

        // If the user logged out in settings and returns to this screen, send them back to Map.java.
        if (FirebaseAuth.getInstance().getCurrentUser() == null && !(GoogleSignIn.getLastSignedInAccount(this) instanceof GoogleSignInAccount)) {

            Intent Activity = new Intent(Chat.this, Map.class);
            startActivity(Activity);
        }

        // Clear text and prevent keyboard from opening.
        if (mInput != null) {

            mInput.getText().clear();
            mInput.clearFocus();
        }

        // Hide and clear recyclerView if necessary.
        if (mentionsRecyclerView != null) {

            mentionsRecyclerView.setVisibility(View.GONE);
        }

        needLoadingIcon = false;
        sendButtonClicked = false;
    }

    @Override
    protected void onStop() {

        Log.i(TAG, "onStop()");

        if (databaseReference != null) {

            databaseReference.removeEventListener(eventListener);
        }

        if (chatRecyclerView != null) {

            chatRecyclerView.clearOnScrollListeners();
            chatRecyclerView.removeOnLayoutChangeListener(onLayoutChangeListener);
        }

        if (mentionsRecyclerView != null) {

            mentionsRecyclerView.clearOnScrollListeners();
            mentionsRecyclerView.removeOnLayoutChangeListener(onLayoutChangeListener);
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
            mInput.setQueryTokenReceiver(null);
            mInput.setSuggestionsVisibilityManager(null);
        }

        cancelToasts();

        super.onStop();
    }

    protected void loadPreferences() {

        Log.i(TAG, "loadPreferences()");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        theme = sharedPreferences.getBoolean(co.clixel.herebefore.Settings.KEY_THEME_SWITCH, false);
    }

    protected void updatePreferences() {

        Log.i(TAG, "updatePreferences()");

        if (theme) {

            // Set to light mode.
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO);
        } else {

            // Set to dark mode.
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES);
        }

        // This will allow the settings button to appear in Map.java.
        sharedPreferences.edit().putBoolean(Settings.KEY_SIGN_OUT, true).apply();
    }

    private void initChatAdapter() {

        // Initialize the RecyclerView.
        Log.i(TAG, "initChatAdapter");

        ChatAdapter adapter = new ChatAdapter(this, mTime, mUser, mImage, mVideo, mText, mUserIsWithinShape);
        chatRecyclerView.swapAdapter(adapter, true);
        chatRecyclerView.setLayoutManager(chatRecyclerViewLinearLayoutManager);

        if (index == -1 || messageSent) {

            // Scroll to bottom of recyclerviewlayout after first initialization and after sending a recyclerviewlayout.
            chatRecyclerView.scrollToPosition(mTime.size() - 1);
            messageSent = false;
        } else {

            // Set RecyclerView scroll position to prevent position change when Firebase gets updated and after screen orientation change.
            chatRecyclerViewLinearLayoutManager.scrollToPositionWithOffset(index, top);
        }

        // After the initial load, make the loadingIcon invisible.
        if (loadingIcon != null && !needLoadingIcon) {

            loadingIcon.setVisibility(View.INVISIBLE);
        }
    }

    // Brings up suggestions when a users inputs "@".
    @NonNull
    @Override
    public List<String> onQueryReceived(@NonNull QueryToken queryToken) {

        Log.i(TAG, "onQueryReceived()");

        List<String> buckets = Collections.singletonList(BUCKET);
        List<String> suggestions = getSuggestions(queryToken);

        if (suggestions != null) {

            initMentionsAdapter(suggestions);
        }

        return buckets;
    }

    public List<String> getSuggestions(QueryToken queryToken) {

        Log.i(TAG, "getSuggestions()");

        if (queryToken.isExplicit()) {

            String prefix = queryToken.getKeywords().toLowerCase();
            List<String> suggestions = new ArrayList<>();

            for (String suggestion : mSuggestions) {

                String name = suggestion.toLowerCase();

                if (name.startsWith(prefix)) {

                    suggestions.add(suggestion);
                }
            }

            return suggestions;
        } else {

            return null;
        }
    }

    public void initMentionsAdapter(@NonNull List<String> suggestions) {

        Log.i(TAG, "initMentionsAdapter()");

        MentionsAdapter adapter = new MentionsAdapter(this, suggestions);
        mentionsRecyclerView.swapAdapter(adapter, true);
        mentionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        boolean display = suggestions.size() > 0;
        displaySuggestions(display);
    }

    @Override
    public void displaySuggestions(boolean display) {

        Log.i(TAG, "displaySuggestions()");

        if (display) {

            mentionsRecyclerView.setVisibility(View.VISIBLE);
        } else {

            mentionsRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean isDisplayingSuggestions() {

        Log.i(TAG, "isDisplayingSuggestions()");

        return mentionsRecyclerView.getVisibility() == View.VISIBLE;
    }

    // The mentionsAdapter for the mentions recyclerView. I'm not sure how to make "mInput" work when it's in another activity so I added it here.
    private class MentionsAdapter extends RecyclerView.Adapter<MentionsAdapter.ViewHolder> {

        private Context mContext;
        private List<String> mSuggestions;
        private boolean theme;

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView suggestion;

            ViewHolder(@NonNull View itemView) {

                super(itemView);
                suggestion = itemView.findViewById(R.id.suggestion);
            }
        }

        MentionsAdapter(Context mContext, List<String> mSuggestions) {

            this.mContext = mContext;
            this.mSuggestions = mSuggestions;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(mContext).inflate(R.layout.mentionsadapterlayout, parent, false);

            loadPreferences();

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

            holder.suggestion.setText(mSuggestions.get(position));

            // Clear list so if user deletes a mention, it won't appear in this list.
            allMentions.clear();

            holder.itemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    // Convert the string into a mentionable to be inserted into the MentionsEditText.
                    Mentionable mentionable = new Mentionable() {

                        @NonNull
                        @Override
                        public String getTextForDisplayMode(@NonNull MentionDisplayMode mode) {

                            if (mSuggestions.get(position) != null) {

                                // Add mentions to this list. Duplicates will be added and later cleared from this list.
                                allMentions.add(mSuggestions.get(position));

                                return "@" + mSuggestions.get(position).substring(0, 10) + "...";
                            } else {

                                return "ERROR";
                            }
                        }

                        @NonNull
                        @Override
                        public MentionDeleteStyle getDeleteStyle() {

                            return MentionDeleteStyle.FULL_DELETE;
                        }

                        @Override
                        public int getSuggestibleId() {

                            return 0;
                        }

                        @NonNull
                        @Override
                        public String getSuggestiblePrimaryText() {

                            if (mSuggestions.get(position) != null) {

                                return mSuggestions.get(position);
                            } else {

                                return "ERROR";
                            }
                        }

                        @Override
                        public int describeContents() {

                            return 0;
                        }

                        @Override
                        public void writeToParcel(Parcel dest, int flags) {
                        }
                    };

                    mInput.insertMention(mentionable);

                    // A set will not allow duplicates, so this will get rid of any duplicates before they are added to the final list.
                    // The final list will be sent to Firebase to notify users of messages.
                    Set<String> hashSet = new LinkedHashSet<>(allMentions);
                    removedMentionDuplicates = new ArrayList<>(hashSet);

                    // Add a space after inserting mention.
                    mInput.append(" ");
                }
            });

            // Change the color of every other row for visual purposes.
            if (!theme) {

                if (position % 2 == 0) {

                    holder.itemView.setBackgroundColor(Color.parseColor("#222222"));
                } else {

                    holder.itemView.setBackgroundColor(Color.parseColor("#292929"));
                }
            } else {

                if (position % 2 == 0) {

                    holder.itemView.setBackgroundColor(Color.parseColor("#D9D9D9"));
                } else {

                    holder.itemView.setBackgroundColor(Color.parseColor("#F2F2F2"));
                }
            }
        }

        @Override
        public int getItemCount() {

            // If the size is greater than 3, just return 3 results.
            return Math.min(mSuggestions.size(), 3);
        }

        protected void loadPreferences() {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

            theme = sharedPreferences.getBoolean(co.clixel.herebefore.Settings.KEY_THEME_SWITCH, false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        Log.i(TAG, "onCreateOptionsMenu()");

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chatsettings_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.settingsButton) {

            Log.i(TAG, "onOptionsItemSelected() -> settingsButton");

            cancelToasts();

            Intent Activity = new Intent(Chat.this, co.clixel.herebefore.Settings.class);

            startActivity(Activity);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {

        if (item.getItemId() == R.string.report_post) {

            loadingIcon.setVisibility(View.VISIBLE);

            ReportPost reportPost = new ReportPost();
            reportPost.setUUID(uuid);
            reportPost.setPosition(item.getGroupId());
            DatabaseReference newReportedPost = FirebaseDatabase.getInstance().getReference().child("Reported_Post").push();
            newReportedPost.setValue(reportPost);
            loadingIcon.setVisibility(View.GONE);
            toastMessageShort("Post reported. Thank you!");
        }

        return super.onContextItemSelected(item);
    }

    private void cancelToasts() {

        if (shortToast != null) {

            shortToast.cancel();
        }

        if (longToast != null) {

            longToast.cancel();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        switch (menuItem.getItemId()) {

            case R.id.browseGallery:

                Log.i(TAG, "onMenuItemClick() -> browseGallery");

                cancelToasts();

                chooseFromGallery();

                mediaButtonMenuIsOpen = false;

                return true;

            case R.id.takePhoto:

                Log.i(TAG, "onMenuItemClick() -> takePhoto");

                if (checkPermissionsPicture()) {

                    cancelToasts();

                    startActivityTakePhoto();
                }

                mediaButtonMenuIsOpen = false;

                return true;

            case R.id.recordVideo:

                Log.i(TAG, "onMenuItemClick() -> recordVideo");

                if (checkPermissionsVideo()) {

                    cancelToasts();

                    startActivityRecordVideo();
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
                toastMessageLong(ex.getMessage());
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
                toastMessageLong(ex.getMessage());
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

            // Set the views to GONE to prevent anything else from being sent to Firebase.
            if (videoImageView != null) {

                videoImageView.setVisibility(View.GONE);
                videoImageView.setImageResource(0);
            }

            if (imageView != null) {

                imageView.setVisibility(View.GONE);
                imageView.setImageResource(0);
            }

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

                imageView.setVisibility(View.VISIBLE);
            } else {

                // Prevents the loadingIcon from being removed by initRecyclerView().
                needLoadingIcon = true;

                // For use in uploadImage().
                URIisFile = false;

                // Not GIF. Needs compression.
                new imageCompressAsyncTask(this).execute(imageURI.toString());
            }

            // Change textView to be to the right of imageView.
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mInput.getLayoutParams();
            params.addRule(RelativeLayout.END_OF, R.id.imageView);
            mInput.setLayoutParams(params);
        }

        if (requestCode == 3 && resultCode == RESULT_OK) {

            Log.i(TAG, "onActivityResult() -> Camera");

            // Set the views to GONE to prevent anything else from being sent to Firebase.
            if (videoImageView != null) {

                videoImageView.setVisibility(View.GONE);
                videoImageView.setImageResource(0);
            }

            if (imageView != null) {

                imageView.setVisibility(View.GONE);
                imageView.setImageResource(0);
            }

            // Prevents the loadingIcon from being removed by initRecyclerView().
            needLoadingIcon = true;

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

            // Set the views to GONE to prevent anything else from being sent to Firebase.
            if (videoImageView != null) {

                videoImageView.setVisibility(View.GONE);
                videoImageView.setImageResource(0);
            }

            if (imageView != null) {

                imageView.setVisibility(View.GONE);
                imageView.setImageResource(0);
            }

            // Prevents the loadingIcon from being removed by initRecyclerView().
            needLoadingIcon = true;

            fileIsImage = false;

            new videoCompressAndAddToGalleryAsyncTask(this).execute(video.getAbsolutePath(), video.getParent());
        }
    }

    private String getExtension(Uri uri) {

        Log.i(TAG, "getExtension()");

        ContentResolver cr = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cr.getType(uri));
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
            activity.loadingIcon.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... paths) {

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isFinishing()) return "2";

            Uri mImageURI = Uri.parse(paths[0]);
            int rotation = 0;

            InputStream imageStream0 = null;
            InputStream imageStream1;
            try {

                // Create 2 inputStreams - imageStream0 to decode into a bitmap and imageStream1 to find the necessary rotation.
                imageStream0 = activity.getContentResolver().openInputStream(mImageURI);

                imageStream1 = activity.getContentResolver().openInputStream(mImageURI);
                if (imageStream1 != null) {

                    ExifInterface exifInterface = new ExifInterface(imageStream1);
                    int orientation = exifInterface.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);
                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            rotation = 90;
                            imageStream1.close();
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            rotation = 180;
                            imageStream1.close();
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            rotation = 270;
                            imageStream1.close();
                            break;
                    }
                }
            } catch (FileNotFoundException ex) {

                ex.printStackTrace();
                activity.toastMessageLong(ex.getMessage());
                Crashlytics.logException(new RuntimeException("onActivityResult() -> gallery imageStream error"));
            } catch (IOException e) {

                e.printStackTrace();
            }

            Bitmap bmp = BitmapFactory.decodeStream(imageStream0);
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, false);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);

            activity.byteArray = stream.toByteArray();
            bmp.recycle();
            rotatedBitmap.recycle();
            try {

                stream.close();
            } catch (IOException ex) {

                ex.printStackTrace();
                activity.toastMessageLong(ex.getMessage());
                Crashlytics.logException(new RuntimeException("onActivityResult() -> gallery stream.close()"));
            }

            if (imageStream0 != null) {

                try {
                    imageStream0.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
                    .apply(new RequestOptions().override(480, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                    .into(activity.imageView);

            activity.imageView.setVisibility(View.VISIBLE);
            activity.loadingIcon.setVisibility(View.INVISIBLE);
            // Allow initRecyclerView() to get rid of the loadingIcon with this boolean.
            activity.needLoadingIcon = false;
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
            activity.loadingIcon.setVisibility(View.VISIBLE);
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
                activity.toastMessageLong(ex.getMessage());
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
                    .apply(new RequestOptions().override(480, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                    .into(activity.imageView);

            activity.imageView.setVisibility(View.VISIBLE);
            activity.loadingIcon.setVisibility(View.INVISIBLE);
            // Allow initRecyclerView() to get rid of the loadingIcon with this boolean.
            activity.needLoadingIcon = false;
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
            activity.loadingIcon.setVisibility(View.VISIBLE);
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
                } catch (IOException ex) {

                    ex.printStackTrace();
                    activity.toastMessageLong(ex.getMessage());
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

            } catch (IOException ex) {

                ex.printStackTrace();
                activity.toastMessageLong(ex.getMessage());
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

            activity.loadingIcon.setVisibility(View.INVISIBLE);
            // Allow initRecyclerView() to get rid of the loadingIcon with this boolean.
            activity.needLoadingIcon = false;
        }
    }

    private void firebaseUpload() {

        Log.i(TAG, "firebaseUploadImage()");

        // Show the loading icon while the image is being uploaded to Firebase.
        loadingIcon.setVisibility(View.VISIBLE);

        if (URIisFile && fileIsImage) {

            // File and image.
            final StorageReference storageReferenceImage = FirebaseStorage.getInstance().getReference("Images").child(String.valueOf(System.currentTimeMillis()));
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
                                            .radius(radius);
                                    CircleInformation circleInformation = new CircleInformation();
                                    circleInformation.setCircleOptions(circleOptions);
                                    circleInformation.setUUID(uuid);
                                    DatabaseReference newFirebaseCircle = FirebaseDatabase.getInstance().getReference().child("Circles").push();
                                    newFirebaseCircle.setValue(circleInformation);
                                } else {

                                    PolygonOptions polygonOptions = null;
                                    // Since the uuid doesn't already exist in Firebase, add the circle.
                                    if (threeMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude))
                                                .clickable(true);
                                    }

                                    if (fourMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude))
                                                .clickable(true);
                                    }

                                    if (fiveMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude))
                                                .clickable(true);
                                    }

                                    if (sixMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude))
                                                .clickable(true);
                                    }

                                    if (sevenMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude))
                                                .clickable(true);
                                    }

                                    if (eightMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude), new LatLng(marker7Latitude, marker7Longitude))
                                                .clickable(true);
                                    }

                                    PolygonInformation polygonInformation = new PolygonInformation();
                                    polygonInformation.setPolygonOptions(polygonOptions);
                                    polygonInformation.setArea(polygonArea);
                                    polygonInformation.setUUID(uuid);
                                    DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("Polygons").push();
                                    newFirebasePolygon.setValue(polygonInformation);
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
                            String userUUID = UUID.randomUUID().toString();
                            messageInformation.setUserUUID(userUUID);
                            messageInformation.setUUID(uuid);
                            messageInformation.setUserIsWithinShape(userIsWithinShape);
                            DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").push();
                            newMessage.setValue(messageInformation);

                            // Get user info for user-user messaging purposes.
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                                UserInformation userInformation = new UserInformation();
                                userInformation.setEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                                userInformation.setUserUUID(userUUID);
                                // Get the token assigned by Firebase when the user signed up / signed in.
                                String token = sharedPreferences.getString("FIREBASE_TOKEN", "null");
                                userInformation.setToken(token);
                                DatabaseReference userInfo = FirebaseDatabase.getInstance().getReference().child("Users").push();
                                userInfo.setValue(userInformation);
                            }

                            mInput.getText().clear();
                            imageView.setVisibility(View.GONE);
                            imageView.setImageDrawable(null);
                            if (image != null) {

                                deleteDirectory(image);
                            }
                            sendButtonClicked = false;
                            loadingIcon.setVisibility(View.GONE);

                            messageUserIfNeeded();
                        }
                    });
                }
            })
                    .addOnFailureListener(new OnFailureListener() {

                        @Override
                        public void onFailure(@NonNull Exception ex) {

                            // Handle unsuccessful uploads
                            loadingIcon.setVisibility(View.GONE);
                            toastMessageLong(ex.getMessage());
                            Log.e(TAG, "firebaseUpload() -> URIisFile && fileIsImage -> onFailure -> " + ex.getMessage());
                        }
                    });
        } else if (!fileIsImage) {

            // Video.
            final StorageReference storageReferenceVideo = FirebaseStorage.getInstance().getReference("Video").child(String.valueOf(System.currentTimeMillis()));
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
                                            .radius(radius);
                                    CircleInformation circleInformation = new CircleInformation();
                                    circleInformation.setCircleOptions(circleOptions);
                                    circleInformation.setUUID(uuid);
                                    DatabaseReference newFirebaseCircle = FirebaseDatabase.getInstance().getReference().child("Circles").push();
                                    newFirebaseCircle.setValue(circleInformation);
                                } else {

                                    PolygonOptions polygonOptions = null;
                                    // Since the uuid doesn't already exist in Firebase, add the circle.
                                    if (threeMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude))
                                                .clickable(true);
                                    }

                                    if (fourMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude))
                                                .clickable(true);
                                    }

                                    if (fiveMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude))
                                                .clickable(true);
                                    }

                                    if (sixMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude))
                                                .clickable(true);
                                    }

                                    if (sevenMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude))
                                                .clickable(true);
                                    }

                                    if (eightMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude), new LatLng(marker7Latitude, marker7Longitude))
                                                .clickable(true);
                                    }

                                    PolygonInformation polygonInformation = new PolygonInformation();
                                    polygonInformation.setPolygonOptions(polygonOptions);
                                    polygonInformation.setArea(polygonArea);
                                    polygonInformation.setUUID(uuid);
                                    DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("Polygons").push();
                                    newFirebasePolygon.setValue(polygonInformation);
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
                            String userUUID = UUID.randomUUID().toString();
                            messageInformation.setUserUUID(userUUID);
                            messageInformation.setUUID(uuid);
                            messageInformation.setUserIsWithinShape(userIsWithinShape);
                            DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").push();
                            newMessage.setValue(messageInformation);

                            // Get user info for user-user messaging purposes.
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                                UserInformation userInformation = new UserInformation();
                                userInformation.setEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                                userInformation.setUserUUID(userUUID);
                                // Get the token assigned by Firebase when the user signed up / signed in.
                                String token = sharedPreferences.getString("FIREBASE_TOKEN", "null");
                                userInformation.setToken(token);
                                DatabaseReference userInfo = FirebaseDatabase.getInstance().getReference().child("Users").push();
                                userInfo.setValue(userInformation);
                            }

                            mInput.getText().clear();
                            videoImageView.setVisibility(View.GONE);
                            imageView.setImageDrawable(null);
                            if (video != null) {

                                deleteDirectory(video);
                            }
                            sendButtonClicked = false;
                            loadingIcon.setVisibility(View.GONE);

                            messageUserIfNeeded();
                        }
                    });
                }
            })
                    .addOnFailureListener(new OnFailureListener() {

                        @Override
                        public void onFailure(@NonNull Exception ex) {

                            // Handle unsuccessful uploads
                            loadingIcon.setVisibility(View.GONE);
                            toastMessageLong(ex.getMessage());
                            Log.e(TAG, "firebaseUpload() -> !fileIsImage -> onFailure -> " + ex.getMessage());
                        }
                    });
        } else {

            // byteArray and image.
            final StorageReference storageReferenceImage = FirebaseStorage.getInstance().getReference("Images").child(String.valueOf(System.currentTimeMillis()));
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
                                            .radius(radius);
                                    CircleInformation circleInformation = new CircleInformation();
                                    circleInformation.setCircleOptions(circleOptions);
                                    circleInformation.setUUID(uuid);
                                    DatabaseReference newFirebaseCircle = FirebaseDatabase.getInstance().getReference().child("Circles").push();
                                    newFirebaseCircle.setValue(circleInformation);
                                } else {

                                    PolygonOptions polygonOptions = null;
                                    // Since the uuid doesn't already exist in Firebase, add the circle.
                                    if (threeMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude))
                                                .clickable(true);
                                    }

                                    if (fourMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude))
                                                .clickable(true);
                                    }

                                    if (fiveMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude))
                                                .clickable(true);
                                    }

                                    if (sixMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude))
                                                .clickable(true);
                                    }

                                    if (sevenMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude))
                                                .clickable(true);
                                    }

                                    if (eightMarkers) {

                                        polygonOptions = new PolygonOptions()
                                                .add(new LatLng(marker0Latitude, marker0Longitude), new LatLng(marker1Latitude, marker1Longitude), new LatLng(marker2Latitude, marker2Longitude), new LatLng(marker3Latitude, marker3Longitude), new LatLng(marker4Latitude, marker4Longitude), new LatLng(marker5Latitude, marker5Longitude), new LatLng(marker6Latitude, marker6Longitude), new LatLng(marker7Latitude, marker7Longitude))
                                                .clickable(true);
                                    }

                                    PolygonInformation polygonInformation = new PolygonInformation();
                                    polygonInformation.setPolygonOptions(polygonOptions);
                                    polygonInformation.setArea(polygonArea);
                                    polygonInformation.setUUID(uuid);
                                    DatabaseReference newFirebasePolygon = FirebaseDatabase.getInstance().getReference().child("Polygons").push();
                                    newFirebasePolygon.setValue(polygonInformation);
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
                            String userUUID = UUID.randomUUID().toString();
                            messageInformation.setUserUUID(userUUID);
                            messageInformation.setUUID(uuid);
                            messageInformation.setUserIsWithinShape(userIsWithinShape);
                            DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").push();
                            newMessage.setValue(messageInformation);

                            // Get user info for user-user messaging purposes.
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                                UserInformation userInformation = new UserInformation();
                                userInformation.setEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                                userInformation.setUserUUID(userUUID);
                                // Get the token assigned by Firebase when the user signed up / signed in.
                                String token = sharedPreferences.getString("FIREBASE_TOKEN", "null");
                                userInformation.setToken(token);
                                DatabaseReference userInfo = FirebaseDatabase.getInstance().getReference().child("Users").push();
                                userInfo.setValue(userInformation);
                            }

                            mInput.getText().clear();
                            imageView.setVisibility(View.GONE);
                            imageView.setImageDrawable(null);
                            if (image != null) {

                                deleteDirectory(image);
                            }
                            sendButtonClicked = false;
                            loadingIcon.setVisibility(View.GONE);

                            messageUserIfNeeded();
                        }
                    });
                }
            })
                    .addOnFailureListener(new OnFailureListener() {

                        @Override
                        public void onFailure(@NonNull Exception ex) {

                            // Handle unsuccessful uploads
                            loadingIcon.setVisibility(View.GONE);
                            toastMessageLong(ex.getMessage());
                            Log.e(TAG, "firebaseUpload() -> else -> onFailure -> " + ex.getMessage());
                        }
                    });
        }
    }

    private void messageUserIfNeeded() {

        if (removedMentionDuplicates != null) {

            Log.i(TAG, "messageUserIfNeeded()");

            // Connect to Firebase.
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            DatabaseReference firebaseUsers = rootRef.child("Users");

            // Load Firebase circles.
            firebaseUsers.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    for (DataSnapshot ds : dataSnapshot.getChildren()) {

                        if (dataSnapshot.getValue() != null) {

                            String userUUID = (String) ds.child("userUUID").getValue();

                            if (userUUID != null) {

                                for (String removedMentionDuplicate : removedMentionDuplicates) {

                                    if (userUUID.equals(removedMentionDuplicate)) {

                                        toastMessageShort("hello");
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                    Log.e(TAG, "DatabaseError");
                    Crashlytics.logException(new Exception("DatabaseError"));
                    toastMessageLong(databaseError.getMessage());
                }
            });
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

    private void toastMessageShort(String message) {

        shortToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        shortToast.show();
    }

    private void toastMessageLong(String message) {

        longToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        longToast.show();
    }
}