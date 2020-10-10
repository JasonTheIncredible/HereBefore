package co.clixel.herebefore;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.Parcel;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
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

import static android.app.Activity.RESULT_OK;
import static java.text.DateFormat.getDateTimeInstance;

public class Chat extends Fragment implements
        PopupMenu.OnMenuItemClickListener,
        QueryTokenReceiver,
        SuggestionsVisibilityManager {

    private static final String TAG = "Chat";
    private static final int Request_ID_Take_Photo = 1700, Request_ID_Record_Video = 1800;
    private MentionsEditText mInput;
    private ArrayList<String> mTime, mUser, mImage, mVideo, mText, mSuggestions, allMentions, userUUIDAL, userEmailAL;
    private ArrayList<Boolean> mUserIsWithinShape;
    private ArrayList<String> removedDuplicatesMentions;
    private RecyclerView chatRecyclerView, mentionsRecyclerView;
    private static int index = -1, top = -1, last;
    private ChildEventListener childEventListener;
    private FloatingActionButton sendButton, mediaButton;
    private boolean firstLoad, needLoadingIcon = false, reachedEndOfRecyclerView = false, recyclerViewHasScrolled = false, messageSent = false, sendButtonClicked = false, mediaButtonMenuIsOpen, fileIsImage, checkPermissionsPicture,
            newShape, threeMarkers, fourMarkers, fiveMarkers, sixMarkers, sevenMarkers, eightMarkers;
    private Boolean userIsWithinShape;
    private View.OnLayoutChangeListener onLayoutChangeListener;
    private String shapeUUID, reportedUser;
    private double directMentionsPosition, polygonArea, circleLatitude, circleLongitude, radius,
            marker0Latitude, marker0Longitude, marker1Latitude, marker1Longitude, marker2Latitude, marker2Longitude, marker3Latitude, marker3Longitude, marker4Latitude, marker4Longitude, marker5Latitude, marker5Longitude, marker6Latitude, marker6Longitude, marker7Latitude, marker7Longitude;
    private PopupMenu mediaButtonMenu;
    private ImageView imageView, videoImageView;
    private Uri imageURI, videoURI;
    private StorageTask<?> uploadTask;
    private LinearLayoutManager chatRecyclerViewLinearLayoutManager;
    private LinearLayoutManager mentionsRecyclerViewLinearLayoutManager;
    private File image, video;
    private byte[] byteArray;
    private View loadingIcon;
    private Toast shortToast, longToast;
    private static final String BUCKET = "text-suggestions";
    public View rootView;
    private Context mContext;
    private Activity mActivity;
    private AdView bannerAd;
    private Query query;
    private Drawable imageDrawable, videoDrawable;
    private int latFirebaseValue, lonFirebaseValue;
    private WordTokenizerConfig tokenizerConfig = new WordTokenizerConfig
            .Builder()
            .setWordBreakChars(", ")
            .setExplicitChars("@")
            .setThreshold(1)
            .build();

    @Override
    public void onAttach(@NonNull Context context) {

        super.onAttach(context);
        Log.i(TAG, "onAttach()");

        mContext = context;
        mActivity = getActivity();

        // Get info from Map.java
        if (mActivity != null) {

            Bundle extras = mActivity.getIntent().getExtras();
            if (extras != null) {

                directMentionsPosition = extras.getLong("directMentionsPosition");
                newShape = extras.getBoolean("newShape");
                latFirebaseValue = extras.getInt("shapeLat");
                lonFirebaseValue = extras.getInt("shapeLon");
                shapeUUID = extras.getString("shapeUUID");
                userIsWithinShape = extras.getBoolean("userIsWithinShape");
                // circleLatitude, circleLongitude, and radius will be null if the circle is not new (as a new circle is not being created).
                circleLatitude = extras.getDouble("circleLatitude");
                circleLongitude = extras.getDouble("circleLongitude");
                radius = extras.getDouble("radius");
                // Most of these will be null if the polygon does not have eight markers, or if the polygon is not new.
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

                Log.e(TAG, "onCreateView() -> extras == null");
            }
        }
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(TAG, "onCreateView()");

        rootView = inflater.inflate(R.layout.chat, container, false);

        bannerAd = rootView.findViewById(R.id.chatBanner);

        // Search I/Ads: in Logcat to find ID and/or W/Ads for other info.
        // List<String> testDeviceIds = Collections.singletonList("814BF63877CBD71E91F9D7241907F4FF");
        RequestConfiguration requestConfiguration
                = new RequestConfiguration.Builder()
                //.setTestDeviceIds(testDeviceIds)
                .build();
        MobileAds.setRequestConfiguration(requestConfiguration);

        AdRequest adRequest = new AdRequest.Builder().build();
        bannerAd.loadAd(adRequest);

        mediaButton = rootView.findViewById(R.id.mediaButton);
        imageView = rootView.findViewById(R.id.imageView);
        videoImageView = rootView.findViewById(R.id.videoImageView);
        mInput = rootView.findViewById(R.id.input);
        sendButton = rootView.findViewById(R.id.sendButton);
        chatRecyclerView = rootView.findViewById(R.id.messageList);
        mentionsRecyclerView = rootView.findViewById(R.id.suggestionsList);
        loadingIcon = rootView.findViewById(R.id.loadingIcon);

        chatRecyclerViewLinearLayoutManager = new LinearLayoutManager(mActivity);
        mentionsRecyclerViewLinearLayoutManager = new LinearLayoutManager(mActivity);

        userUUIDAL = new ArrayList<>();
        userEmailAL = new ArrayList<>();

        mTime = new ArrayList<>();
        mUser = new ArrayList<>();
        mImage = new ArrayList<>();
        mVideo = new ArrayList<>();
        mText = new ArrayList<>();
        mSuggestions = new ArrayList<>();
        allMentions = new ArrayList<>();
        mUserIsWithinShape = new ArrayList<>();
        // Prevents clearing this list if user adds a DM and takes a picture.
        if (removedDuplicatesMentions == null) {
            removedDuplicatesMentions = new ArrayList<>();
        }

        // Make the loadingIcon visible upon the first load, as it can sometimes take a while to show anything. It should be made invisible in initChatAdapter().
        if (loadingIcon != null) {

            loadingIcon.setVisibility(View.VISIBLE);
        }

        if (userIsWithinShape) {

            mInput.setHint("Message from within shape...");
        } else {

            mInput.setHint("Message from outside shape...");
        }

        return rootView;
    }

    @Override
    public void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Set to true to scroll to the bottom of chatRecyclerView. Also prevents duplicate items in addQuery.
        firstLoad = true;

        // Clear text and prevent keyboard from opening.
        if (mInput != null) {

            mInput.clearFocus();
        }

        // Reload the image if it exists.
        if (imageDrawable != null) {

            videoImageView.setVisibility(View.GONE);
            videoDrawable = null;
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageDrawable(imageDrawable);
        }

        // Reload the video if it exists.
        if (videoDrawable != null && mInput != null) {

            imageView.setVisibility(View.GONE);
            imageDrawable = null;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mInput.getLayoutParams();
            params.addRule(RelativeLayout.END_OF, R.id.videoImageView);
            mInput.setLayoutParams(params);
            videoImageView.setVisibility(View.VISIBLE);
            videoImageView.setImageDrawable(videoDrawable);
        }

        // Hide and clear recyclerView if necessary.
        if (mentionsRecyclerView != null) {

            mentionsRecyclerView.setVisibility(View.GONE);
        }

        DatabaseReference firebaseMessages = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID);
        firebaseMessages.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                fillArrayLists(snapshot);

                fillRecyclerView(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        // Hide the imageView or videoImageView if user presses the delete button.
        mInput.setOnKeyListener((v, keyCode, event) -> {

            if (keyCode == KeyEvent.KEYCODE_DEL && (imageView.getVisibility() == View.VISIBLE || videoImageView.getVisibility() == View.VISIBLE) &&
                    (mInput.getText().toString().trim().length() == 0 || mInput.getSelectionStart() == 0)) {

                imageView.setVisibility(View.GONE);
                imageView.setImageDrawable(null);
                videoImageView.setVisibility(View.GONE);
                videoImageView.setImageDrawable(null);
            }

            // Keep "return false" or the enter key will not go to the next line.
            return false;
        });

        mInput.setTokenizer(new WordTokenizer(tokenizerConfig));
        mInput.setQueryTokenReceiver(this);
        mInput.setSuggestionsVisibilityManager(this);

        mediaButton.setOnClickListener(view -> {

            Log.i(TAG, "onStart() -> mediaButton -> onClick");

            mediaButtonMenu = new PopupMenu(mContext, mediaButton);
            mediaButtonMenu.setOnMenuItemClickListener(Chat.this);
            mediaButtonMenu.inflate(R.menu.mediabutton_menu);
            mediaButtonMenu.show();
            mediaButtonMenuIsOpen = true;

            // Changes boolean value (used in OnConfigurationChanged) to determine whether menu is currently open.
            mediaButtonMenu.setOnDismissListener(popupMenu -> {

                Log.i(TAG, "onStart() -> mediaButton -> onDismiss");

                mediaButtonMenuIsOpen = false;
                mediaButtonMenu.setOnDismissListener(null);
            });
        });

        // onClickListener for sending recyclerviewlayout to Firebase.
        sendButton.setOnClickListener(view -> {

            Log.i(TAG, "onStart() -> sendButton -> onClick");

            // Close keyboard.
            if (mActivity.getCurrentFocus() != null) {

                InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {

                    imm.hideSoftInputFromWindow(mActivity.getCurrentFocus().getWindowToken(), 0);
                } else {

                    Log.e(TAG, "onStart() -> sendButton -> imm == null");
                }
                if (mInput != null) {

                    mInput.clearFocus();
                }
            }

            // Prevent double clicking the send button.
            if (sendButtonClicked) {

                return;
            }

            sendButtonClicked = true;

            String input = mInput.getText().toString().trim();

            // Send recyclerviewlayout to Firebase.
            if (!input.equals("") || imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                if (imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                    // Upload the image to Firebase if it exists and is not already in the process of sending an image.
                    if (uploadTask != null && uploadTask.isInProgress()) {

                        toastMessageShort("Upload in progress");
                    } else {

                        firebaseUpload();
                    }
                } else {

                    // Change boolean to true - scrolls to the bottom of the recyclerView (in initChatAdapter()).
                    messageSent = true;

                    if (newShape) {

                        DatabaseReference newFirebaseShape = null;
                        if (circleLatitude != 0 && circleLongitude != 0) {

                            // Shape is a circle.

                            // Since the uuid doesn't already exist in Firebase, add the circle.
                            CircleOptions circleOptions = new CircleOptions()
                                    .center(new LatLng(circleLatitude, circleLongitude))
                                    .clickable(true)
                                    .radius(radius);
                            CircleInformation circleInformation = new CircleInformation();
                            circleInformation.setCircleOptions(circleOptions);
                            circleInformation.setShapeUUID(shapeUUID);

                            if (radius == 1) {

                                newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Point").push();
                            } else if (1 < radius && radius <= 10) {

                                newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Small").push();
                            } else if (10 < radius && radius <= 50) {

                                newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Medium").push();
                            } else if (50 < radius) {

                                newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Large").push();
                            }

                            if (newFirebaseShape != null) {

                                newFirebaseShape.setValue(circleInformation);
                            }
                        } else if (polygonArea != 0) {

                            // Shape is a polygon.

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
                            polygonInformation.setArea(polygonArea);
                            polygonInformation.setPolygonOptions(polygonOptions);
                            polygonInformation.setShapeUUID(shapeUUID);

                            if (polygonArea <= Math.PI * (Math.pow(10, 2))) {

                                newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Small").push();
                            } else if (Math.PI * (Math.pow(10, 2)) < polygonArea && polygonArea <= Math.PI * (Math.pow(50, 2))) {

                                newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Medium").push();
                            } else if (Math.PI * (Math.pow(50, 2)) < polygonArea) {

                                newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Large").push();
                            }

                            if (newFirebaseShape != null) {

                                newFirebaseShape.setValue(polygonInformation);
                            }
                        } else {

                            // Both radius and polygonArea are null.
                            toastMessageLong("Oops! Something went wrong!");
                            return;
                        }
                    }

                    String userUUID = UUID.randomUUID().toString();

                    // If mentions exist, add to the user's DMs.
                    if (removedDuplicatesMentions != null) {

                        ArrayList<String> messagedUsersAL = new ArrayList<>();
                        for (String mention : removedDuplicatesMentions) {

                            for (int i = 0; i < userUUIDAL.size(); i++) {

                                String userEmail = userEmailAL.get(i);
                                if (userUUIDAL.get(i).equals(mention) && !messagedUsersAL.contains(userEmail)) {

                                    // Prevent sending the same DM to a user multiple times.
                                    messagedUsersAL.add(userEmail);

                                    String email = userEmailAL.get(i);

                                    DMInformation dmInformation = new DMInformation();
                                    // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                    // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                    Object date = ServerValue.TIMESTAMP;
                                    dmInformation.setDate(date);
                                    dmInformation.setLat(latFirebaseValue);
                                    dmInformation.setLon(lonFirebaseValue);
                                    dmInformation.setMessage(input);
                                    dmInformation.setPosition(mUser.size());
                                    dmInformation.setSeenByUser(false);
                                    if (radius != 0) {
                                        dmInformation.setSize(radius);
                                        dmInformation.setShapeIsCircle(true);
                                    } else {
                                        dmInformation.setSize(polygonArea);
                                        dmInformation.setShapeIsCircle(false);
                                    }
                                    dmInformation.setShapeUUID(shapeUUID);
                                    dmInformation.setUserIsWithinShape(userIsWithinShape);
                                    dmInformation.setUserUUID(userUUID);

                                    // Firebase does not allow ".", so replace them with ",".
                                    String receiverEmailFirebase = email.replace(".", ",");
                                    DatabaseReference newDM = FirebaseDatabase.getInstance().getReference().child("Users").child(receiverEmailFirebase).child("ReceivedDMs").push();
                                    newDM.setValue(dmInformation);
                                    break;
                                }
                            }
                        }
                    }

                    MessageInformation messageInformation = new MessageInformation();
                    // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                    // This will cause onDataChange to fire twice; optimizations could be made in the future.
                    Object date = ServerValue.TIMESTAMP;
                    messageInformation.setDate(date);
                    // If user has a Google account, get email one way. Else, get email another way.
                    GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(mContext);
                    String email;
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                    if (acct != null) {
                        email = acct.getEmail();
                    } else {
                        email = sharedPreferences.getString("userToken", "null");
                    }
                    messageInformation.setEmail(email);
                    messageInformation.setMessage(input);
                    messageInformation.setPosition(mUser.size());
                    messageInformation.setUserIsWithinShape(userIsWithinShape);
                    messageInformation.setUserUUID(userUUID);
                    DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID).push();
                    newMessage.setValue(messageInformation);

                    mInput.getText().clear();
                    if (!removedDuplicatesMentions.isEmpty()) {
                        removedDuplicatesMentions.clear();
                    }
                    // For some reason, if the text begins with a mention and onCreateView was called after the mention was added, the mention is not cleared with one call to clear().
                    mInput.getText().clear();
                    sendButtonClicked = false;
                }
            }
        });

        imageView.setOnClickListener(v -> {

            Log.i(TAG, "imageView -> onClick");

            cancelToasts();

            Intent Activity = new Intent(mContext, PhotoView.class);
            Activity.putExtra("imgURL", imageURI.toString());
            Chat.this.startActivity(Activity);
        });

        videoImageView.setOnClickListener(v -> {

            Log.i(TAG, "videoImageView -> onClick");

            cancelToasts();

            Intent Activity = new Intent(mContext, VideoView.class);
            Activity.putExtra("videoURL", videoURI.toString());
            Chat.this.startActivity(Activity);
        });
    }

    private void fillArrayLists(DataSnapshot snapshot) {

        Log.i(TAG, "fillArrayLists()");

        for (DataSnapshot ds : snapshot.getChildren()) {

            userUUIDAL.add((String) ds.child("userUUID").getValue());
            userEmailAL.add((String) ds.child("email").getValue());
        }
    }

    private void fillRecyclerView(DataSnapshot snapshot) {

        Log.i(TAG, "fillRecyclerView()");

        for (DataSnapshot ds : snapshot.getChildren()) {

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
            }
            mUser.add(user);
            mImage.add(imageURL);
            mVideo.add(videoURL);
            mText.add(messageText);
            mUserIsWithinShape.add(userIsWithinShape);
        }

        // Read RecyclerView scroll position (for use in initChatAdapter).
        if (chatRecyclerViewLinearLayoutManager != null && chatRecyclerView != null) {

            index = chatRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
            last = chatRecyclerViewLinearLayoutManager.findLastCompletelyVisibleItemPosition();
            View v = chatRecyclerView.getChildAt(0);
            top = (v == null) ? 0 : (v.getTop() - chatRecyclerView.getPaddingTop());
        }

        addQuery();

        // Check RecyclerView scroll state (to allow the layout to move up when keyboard appears).
        if (chatRecyclerView != null) {

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
            chatRecyclerView.addOnLayoutChangeListener(onLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {

                if (reachedEndOfRecyclerView || !recyclerViewHasScrolled) {

                    if (bottom < oldBottom) {

                        if (chatRecyclerView.getAdapter() != null && chatRecyclerView.getAdapter().getItemCount() > 0) {

                            chatRecyclerView.postDelayed(() -> chatRecyclerView.smoothScrollToPosition(

                                    chatRecyclerView.getAdapter().getItemCount() - 1), 100);
                        }
                    }
                }
            });
        }
    }

    // Change to .limitToLast(1) to cut down on data usage. Otherwise, EVERY child at this node will be downloaded every time the child is updated.
    private void addQuery() {

        // This prevents duplicates when loading into Settings fragment then switched back into Chat (as onStop is never called but onStart is called).
        if (query != null) {

            query.removeEventListener(childEventListener);
        }

        // If this is the first time calling this eventListener and it's a new shape, initialize the adapter (but don't return, as the childEventListener should still be added), as onChildAdded won't be called the first time.
        if (firstLoad && newShape) {

            initChatAdapter();
        }

        query = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID).limitToLast(1);
        childEventListener = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.i(TAG, "addQuery()");

                // If this is the first time calling this eventListener, prevent double posts (as onStart() already added the last item).
                if (firstLoad) {

                    initChatAdapter();
                    return;
                }

                Long serverDate = (Long) snapshot.child("date").getValue();
                String user = (String) snapshot.child("userUUID").getValue();
                // Used when a user mentions another user with "@".
                mSuggestions.add(user);
                String imageURL = (String) snapshot.child("imageURL").getValue();
                String videoURL = (String) snapshot.child("videoURL").getValue();
                String messageText = (String) snapshot.child("message").getValue();
                Boolean userIsWithinShape = (Boolean) snapshot.child("userIsWithinShape").getValue();
                DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                // This will cause onDataChange to fire twice; optimizations could be made in the future.
                if (serverDate != null) {

                    Date netDate = (new Date(serverDate));
                    String messageTime = dateFormat.format(netDate);
                    mTime.add(messageTime);
                } else {

                    Log.e(TAG, "onStart() -> serverDate == null");
                }
                mUser.add(user);
                mImage.add(imageURL);
                mVideo.add(videoURL);
                mText.add(messageText);
                mUserIsWithinShape.add(userIsWithinShape);

                // Read RecyclerView scroll position (for use in initChatAdapter).
                if (chatRecyclerViewLinearLayoutManager != null) {

                    index = chatRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
                    last = chatRecyclerViewLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                    View v = chatRecyclerView.getChildAt(0);
                    top = (v == null) ? 0 : (v.getTop() - chatRecyclerView.getPaddingTop());
                }

                userUUIDAL.add((String) snapshot.child("userUUID").getValue());
                userEmailAL.add((String) snapshot.child("email").getValue());

                initChatAdapter();
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

        query.addChildEventListener(childEventListener);
    }

    @Override
    public void onStop() {

        Log.i(TAG, "onStop()");

        if (query != null) {

            query.removeEventListener(childEventListener);
        }

        if (chatRecyclerView != null) {

            chatRecyclerView.clearOnScrollListeners();
            chatRecyclerView.removeOnLayoutChangeListener(onLayoutChangeListener);
            chatRecyclerView.setAdapter(null);
        }

        if (mentionsRecyclerView != null) {

            mentionsRecyclerView.clearOnScrollListeners();
            mentionsRecyclerView.setAdapter(null);
        }

        if (mInput != null) {

            mInput.setOnKeyListener(null);
            mInput.setQueryTokenReceiver(null);
            mInput.setSuggestionsVisibilityManager(null);
            mInput.setTokenizer(null);
        }

        if (mediaButton != null) {

            mediaButton.setOnClickListener(null);
        }

        if (mediaButtonMenu != null) {

            mediaButtonMenu.setOnMenuItemClickListener(null);
            mediaButtonMenu = null;
        }

        if (sendButton != null) {

            sendButton.setOnClickListener(null);
        }

        if (imageView != null) {

            imageDrawable = imageView.getDrawable();
        }

        if (videoImageView != null) {

            videoDrawable = videoImageView.getDrawable();
        }

        cancelToasts();

        super.onStop();
    }

    @Override
    public void onDestroyView() {

        Log.i(TAG, "onDestroyView()");

        if (bannerAd != null) {

            bannerAd = null;
        }

        if (mediaButton != null) {

            mediaButton = null;
        }

        if (mInput != null) {

            mInput = null;
        }

        if (sendButton != null) {

            sendButton = null;
        }

        if (loadingIcon != null) {

            loadingIcon = null;
        }

        if (chatRecyclerView != null) {

            chatRecyclerView = null;
        }

        if (mentionsRecyclerView != null) {

            mentionsRecyclerView = null;
        }

        if (chatRecyclerViewLinearLayoutManager != null) {

            chatRecyclerViewLinearLayoutManager = null;
        }

        if (mentionsRecyclerViewLinearLayoutManager != null) {

            mentionsRecyclerViewLinearLayoutManager = null;
        }

        if (rootView != null) {

            rootView = null;
        }

        super.onDestroyView();
    }

    @Override
    public void onDetach() {

        super.onDetach();
        Log.i(TAG, "onDetach()");

        mContext = null;
        mActivity = null;
    }

    private void initChatAdapter() {

        // Initialize the RecyclerView.
        Log.i(TAG, "initChatAdapter()");

        ChatAdapter adapter = new ChatAdapter(mContext, mTime, mUser, mImage, mVideo, mText, mUserIsWithinShape);
        if (chatRecyclerView != null) {

            chatRecyclerView.setAdapter(adapter);
            chatRecyclerView.setHasFixedSize(true);
            chatRecyclerView.setLayoutManager(chatRecyclerViewLinearLayoutManager);

            if (directMentionsPosition != 0 && !firstLoad) {

                chatRecyclerView.scrollToPosition((int) directMentionsPosition);
            } else if (last == (mUser.size() - 2) || firstLoad || messageSent) {

                // Scroll to bottom of recyclerviewlayout after first initialization and after sending a recyclerviewlayout.
                chatRecyclerView.scrollToPosition(mUser.size() - 1);
                messageSent = false;
                firstLoad = false;
            } else {

                // Set RecyclerView scroll position to prevent position change when Firebase gets updated and after screen orientation change.
                chatRecyclerViewLinearLayoutManager.scrollToPositionWithOffset(index, top);
            }
        }

        // After the initial load, make the loadingIcon invisible.
        if (loadingIcon != null && !needLoadingIcon) {

            loadingIcon.setVisibility(View.INVISIBLE);
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

        private Context mContext;
        private ArrayList<String> mMessageTime, mMessageUser, mMessageImage, mMessageImageVideo, mMessageText;
        private ArrayList<Boolean> mUserIsWithinShape;
        private boolean theme;

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView messageTimeInside, messageTimeOutside, messageUserInside, messageUserOutside, messageTextInside, messageTextOutside;
            ImageView messageImageInside, messageImageOutside, messageImageVideoInside, messageImageVideoOutside;
            FrameLayout videoFrameInside, videoFrameOutside;
            RelativeLayout messageItem;

            ViewHolder(@NonNull final View itemView) {

                super(itemView);
                messageTimeInside = itemView.findViewById(R.id.messageTimeInside);
                messageTimeOutside = itemView.findViewById(R.id.messageTimeOutside);
                messageUserInside = itemView.findViewById(R.id.messageUserInside);
                messageUserOutside = itemView.findViewById(R.id.messageUserOutside);
                messageImageInside = itemView.findViewById(R.id.messageImageInside);
                messageImageOutside = itemView.findViewById(R.id.messageImageOutside);
                videoFrameInside = itemView.findViewById(R.id.videoFrameInside);
                videoFrameOutside = itemView.findViewById(R.id.videoFrameOutside);
                messageImageVideoInside = itemView.findViewById(R.id.messageImageVideoInside);
                messageImageVideoOutside = itemView.findViewById(R.id.messageImageVideoOutside);
                ImageButton playButtonInside = itemView.findViewById(R.id.playButtonInside);
                ImageButton playButtonOutside = itemView.findViewById(R.id.playButtonOutside);
                messageTextInside = itemView.findViewById(R.id.messageTextInside);
                messageTextOutside = itemView.findViewById(R.id.messageTextOutside);
                messageItem = itemView.findViewById(R.id.message);

                itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {

                    // Get a unique identifier (the user's name) to search for in Firebase.
                    reportedUser = mMessageUser.get(getAdapterPosition());
                    menu.add(0, R.string.report_post, 0, R.string.report_post);
                });

                if (messageImageInside != null) {

                    messageImageInside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, PhotoView.class);
                        Activity.putExtra("imgURL", mMessageImage.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
                    });
                }

                if (messageImageOutside != null) {

                    messageImageOutside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, PhotoView.class);
                        Activity.putExtra("imgURL", mMessageImage.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
                    });
                }

                if (messageImageVideoInside != null) {

                    messageImageVideoInside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoURL", mMessageImageVideo.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
                    });
                }

                if (messageImageVideoOutside != null) {

                    messageImageVideoOutside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoURL", mMessageImageVideo.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
                    });
                }

                if (playButtonInside != null) {

                    playButtonInside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoURL", mMessageImageVideo.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
                    });
                }

                if (playButtonOutside != null) {

                    playButtonOutside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoURL", mMessageImageVideo.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
                    });
                }
            }
        }

        ChatAdapter(Context context, ArrayList<String> mMessageTime, ArrayList<String> mMessageUser, ArrayList<String> mMessageImage, ArrayList<String> mMessageImageVideo, ArrayList<String> mMessageText, ArrayList<Boolean> mUserIsWithinShape) {

            this.mContext = context;
            this.mMessageTime = mMessageTime;
            this.mMessageUser = mMessageUser;
            this.mMessageImage = mMessageImage;
            this.mMessageImageVideo = mMessageImageVideo;
            this.mMessageText = mMessageText;
            this.mUserIsWithinShape = mUserIsWithinShape;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(mContext).inflate(R.layout.chatadapterlayout, parent, false);

            loadPreferences();

            Bundle extras = mActivity.getIntent().getExtras();
            if (extras != null) {

                directMentionsPosition = extras.getLong("directMentionsPosition");
            } else {

                Log.e(TAG, "ChatAdapter -> extras == null");
            }

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

            // Set the left side if the user sent the message from inside the shape.
            if (mUserIsWithinShape.get(position)) {

                holder.messageTimeInside.setText(mMessageTime.get(position));

                holder.messageUserInside.setText("@" + mMessageUser.get(position));

                // Set messageImage, messageImageVideo, or messageText to gone if an image or text doesn't exist, for spacing consistency.
                if (mMessageImage.get(position) == null) {

                    holder.messageImageInside.setVisibility(View.GONE);
                } else {

                    Glide.with(mContext)
                            .load(mMessageImage.get(position))
                            .apply(new RequestOptions().override(5000, 640).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                            .into(holder.messageImageInside);

                    holder.messageImageInside.setVisibility(View.VISIBLE);
                }

                if (mMessageImageVideo.get(position) == null) {

                    holder.videoFrameInside.setVisibility(View.GONE);
                } else {

                    Glide.with(mContext)
                            .load(mMessageImageVideo.get(position))
                            .apply(new RequestOptions().override(5000, 640).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                            .into(holder.messageImageVideoInside);

                    holder.videoFrameInside.setVisibility(View.VISIBLE);
                }

                if (mMessageText.get(position) == null) {

                    holder.messageTextInside.setVisibility(View.GONE);
                } else {

                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.messageTextInside.getLayoutParams();

                    if (holder.messageImageInside.getVisibility() == View.VISIBLE) {

                        params.addRule(RelativeLayout.BELOW, R.id.messageImageInside);
                        holder.messageTextInside.setLayoutParams(params);
                    } else if (holder.messageImageVideoInside.getVisibility() == View.VISIBLE) {

                        params.addRule(RelativeLayout.BELOW, R.id.videoFrameInside);
                        holder.messageTextInside.setLayoutParams(params);
                    }
                    holder.messageTextInside.setText(mMessageText.get(position));
                    holder.messageTextInside.setVisibility(View.VISIBLE);
                }
            } else {

                // User sent the message from outside the shape. Setup the right side.
                holder.messageTimeOutside.setText(mMessageTime.get(position));

                holder.messageUserOutside.setText("@" + mMessageUser.get(position));

                // Set messageImage, messageImageVideo, or messageText to gone if an image or text doesn't exist, for spacing consistency.
                if (mMessageImage.get(position) == null) {

                    holder.messageImageOutside.setVisibility(View.GONE);
                } else {

                    Glide.with(mContext)
                            .load(mMessageImage.get(position))
                            .apply(new RequestOptions().override(5000, 640).placeholder(R.drawable.ic_recyclerview_image_placeholder).centerInside())
                            .into(holder.messageImageOutside);

                    holder.messageImageOutside.setVisibility(View.VISIBLE);
                }

                if (mMessageImageVideo.get(position) == null) {

                    holder.videoFrameOutside.setVisibility(View.GONE);
                } else {

                    Glide.with(mContext)
                            .load(mMessageImageVideo.get(position))
                            .apply(new RequestOptions().override(5000, 640).placeholder(R.drawable.ic_recyclerview_image_placeholder).centerInside())
                            .into(holder.messageImageVideoOutside);

                    holder.videoFrameOutside.setVisibility(View.VISIBLE);
                }

                if (mMessageText.get(position) == null) {

                    holder.messageTextOutside.setVisibility(View.GONE);
                } else {

                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.messageTextOutside.getLayoutParams();

                    if (holder.messageImageOutside.getVisibility() == View.VISIBLE) {

                        params.addRule(RelativeLayout.BELOW, R.id.messageImageOutside);
                        holder.messageTextOutside.setLayoutParams(params);
                    } else if (holder.messageImageVideoOutside.getVisibility() == View.VISIBLE) {

                        params.addRule(RelativeLayout.BELOW, R.id.videoFrameOutside);
                        holder.messageTextOutside.setLayoutParams(params);
                    }

                    holder.messageTextOutside.setText(mMessageText.get(position));
                    holder.messageTextOutside.setVisibility(View.VISIBLE);
                }
            }

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

            // "Highlight" the directMention message.
            if (directMentionsPosition != 0) {

                if (position == directMentionsPosition) {

                    if (theme) {

                        holder.itemView.setBackgroundColor(Color.parseColor("#859FFF"));
                    } else {

                        holder.itemView.setBackgroundColor(Color.parseColor("#1338BE"));
                    }
                }
            }
        }

        @Override
        public int getItemCount() {

            return mMessageTime.size();
        }

        @Override
        public int getItemViewType(int position) {

            return position;
        }

        protected void loadPreferences() {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

            theme = sharedPreferences.getBoolean(SettingsFragment.KEY_THEME_SWITCH, false);
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

    private List<String> getSuggestions(QueryToken queryToken) {

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

    private void initMentionsAdapter(@NonNull List<String> suggestions) {

        Log.i(TAG, "initMentionsAdapter()");

        MentionsAdapter adapter = new MentionsAdapter(mContext, suggestions);
        mentionsRecyclerView.swapAdapter(adapter, true);
        mentionsRecyclerView.setHasFixedSize(true);
        mentionsRecyclerView.setLayoutManager(mentionsRecyclerViewLinearLayoutManager);
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

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

            holder.suggestion.setText(mSuggestions.get(position));

            holder.itemView.setOnClickListener(v -> {

                // Convert the string into a mentionable to be inserted into the MentionsEditText.
                Mentionable mentionable = new Mentionable() {

                    @NonNull
                    @Override
                    public String getTextForDisplayMode(@NonNull MentionDisplayMode mode) {

                        // Add mentions to this list. Duplicates will be added and later cleared from this list.
                        allMentions.add(mSuggestions.get(position));

                        return "@" + (mSuggestions.get(position)).substring(0, 10) + "...";
                    }

                    @NonNull
                    @Override
                    public MentionDeleteStyle getDeleteStyle() {

                        removedDuplicatesMentions.remove(mSuggestions.get(position));

                        return MentionDeleteStyle.FULL_DELETE;
                    }

                    @Override
                    public int getSuggestibleId() {

                        return 0;
                    }

                    @NonNull
                    @Override
                    public String getSuggestiblePrimaryText() {

                        return mSuggestions.get(position);
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
                removedDuplicatesMentions = new ArrayList<>(hashSet);

                // Add a space after inserting mention.
                mInput.append(" ");
            });

            loadPreferences();

            // Clear list so if user deletes a mention, it won't appear in this list.
            allMentions.clear();

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

            theme = sharedPreferences.getBoolean(SettingsFragment.KEY_THEME_SWITCH, false);
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {

        if (item.getItemId() == R.string.report_post) {

            loadingIcon.setVisibility(View.VISIBLE);

            DatabaseReference firebaseMessages = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID);
            firebaseMessages.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    for (DataSnapshot ds : snapshot.getChildren()) {

                        String userUUID = (String) ds.child("userUUID").getValue();
                        if (userUUID != null) {

                            if (userUUID.equals(reportedUser)) {

                                String pushID = ds.getKey();
                                ReportPostInformation reportPostInformation = new ReportPostInformation();
                                reportPostInformation.setLat(latFirebaseValue);
                                reportPostInformation.setLon(lonFirebaseValue);
                                reportPostInformation.setPushID(pushID);
                                reportPostInformation.setShapeUUID(shapeUUID);
                                DatabaseReference newReportedPost = FirebaseDatabase.getInstance().getReference().child("ReportedPost").push();
                                newReportedPost.setValue(reportPostInformation);
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageShort("Post reported. Thank you!");
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                    toastMessageLong(databaseError.getMessage());
                }
            });
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

    private boolean checkPermissionsPicture() {

        Log.i(TAG, "checkPermissionsPicture()");

        // boolean to loop back to this point and start activity after xPermissionAlertDialog.
        checkPermissionsPicture = true;

        int permissionCamera = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
        int permissionWriteExternalStorage = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
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

        int permissionCamera = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
        int permissionWriteExternalStorage = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionRecordAudio = ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO);
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
        if (cameraIntent.resolveActivity(mActivity.getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {

                photoFile = createImageFile();
            } catch (IOException ex) {

                // Error occurred while creating the File
                ex.printStackTrace();
                toastMessageLong(ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                imageURI = FileProvider.getUriForFile(mContext,
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
        if (videoIntent.resolveActivity(mActivity.getPackageManager()) != null) {

            // Create the File where the video should go
            File videoFile = null;
            try {

                videoFile = createVideoFile();
            } catch (IOException ex) {

                // Error occurred while creating the File
                ex.printStackTrace();
                toastMessageLong(ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (videoFile != null) {

                videoURI = FileProvider.getUriForFile(mContext,
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

                builder = new AlertDialog.Builder(activityWeakRef.get().getContext());

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                builder.setCancelable(false)
                        .setTitle("Camera Permission Required")
                        .setMessage("Here Before needs permission to use your camera to take pictures or video. " +
                                "You may need to enable permission manually through the settings menu.")
                        .setPositiveButton("OK", (dialogInterface, i) -> {

                            if (checkPermissionsPicture) {

                                activityWeakRef.get().checkPermissionsPicture();
                            } else {

                                activityWeakRef.get().checkPermissionsVideo();
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

                builder = new AlertDialog.Builder(activityWeakRef.get().getContext());

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                builder.setCancelable(false)
                        .setTitle("Storage Permission Required")
                        .setMessage("Here Before needs permission to use your storage to save photos or video. " +
                                "You may need to enable permission manually through the settings menu.")
                        .setPositiveButton("OK", (dialogInterface, i) -> {

                            if (checkPermissionsPicture) {

                                activityWeakRef.get().checkPermissionsPicture();
                            } else {

                                activityWeakRef.get().checkPermissionsVideo();
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

                builder = new AlertDialog.Builder(activityWeakRef.get().getContext());

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                builder.setCancelable(false)
                        .setTitle("Audio Permission Required")
                        .setMessage("Here Before needs permission to record audio during video recording. " +
                                "You may need to enable permission manually through the settings menu.")
                        .setPositiveButton("OK", (dialogInterface, i) -> {

                            if (checkPermissionsPicture) {

                                activityWeakRef.get().checkPermissionsPicture();
                            } else {

                                activityWeakRef.get().checkPermissionsVideo();
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
        File storageDir = mActivity.getCacheDir();
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
        File storageDir = mActivity.getCacheDir();
        video = File.createTempFile(
                videoFileName,  /* prefix */
                ".mp4",         /* suffix */
                storageDir      /* directory */
        );

        return video;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

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

            // Reset these values to there's no problem with overlap.
            imageDrawable = null;
            videoDrawable = null;

            // Prevents the loadingIcon from being removed by initChatAdapter().
            needLoadingIcon = true;

            fileIsImage = true;

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

            // Reset these values to there's no problem with overlap.
            imageDrawable = null;
            videoDrawable = null;

            // Prevents the loadingIcon from being removed by initChatAdapter().
            needLoadingIcon = true;

            fileIsImage = false;

            new videoCompressAndAddToGalleryAsyncTask(this).execute(video.getAbsolutePath(), video.getParent());
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
            if (activity == null || activity.isRemoving()) return;

            // Show the loading icon while the image is being compressed.
            activity.loadingIcon.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... paths) {

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isRemoving()) return "2";

            try {

                if (activity.getContext() != null) {

                    // Save a non-compressed image to the gallery.
                    Bitmap imageBitmapFull = new Compressor(activity.getContext())
                            .setMaxWidth(10000)
                            .setMaxHeight(10000)
                            .setQuality(100)
                            .setCompressFormat(Bitmap.CompressFormat.PNG)
                            .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES).getAbsolutePath())
                            .compressToBitmap(activity.image);
                    MediaStore.Images.Media.insertImage(activity.getContext().getContentResolver(), imageBitmapFull, "HereBefore_" + System.currentTimeMillis() + "_PNG", null);

                    // Create a compressed image.
                    Bitmap mImageBitmap = new Compressor(activity.getContext())
                            .setMaxWidth(480)
                            .setMaxHeight(640)
                            .setQuality(50)
                            .setCompressFormat(Bitmap.CompressFormat.JPEG)
                            .compressToBitmap(activity.image);

                    // Convert the bitmap to a byteArray for use in uploadImage().
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream(mImageBitmap.getWidth() * mImageBitmap.getHeight());
                    mImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, buffer);
                    activity.byteArray = buffer.toByteArray();
                }
            } catch (IOException ex) {

                ex.printStackTrace();
                activity.toastMessageLong(ex.getMessage());
            }

            return "2";
        }

        @Override
        protected void onPostExecute(String meaninglessString) {

            super.onPostExecute(meaninglessString);

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isRemoving()) return;

            Glide.with(activity)
                    .load(activity.byteArray)
                    .apply(new RequestOptions().override(480, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                    .into(activity.imageView);

            activity.imageView.setVisibility(View.VISIBLE);
            activity.loadingIcon.setVisibility(View.INVISIBLE);
            // Allow initChatAdapter() to get rid of the loadingIcon with this boolean.
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
            if (activity == null || activity.isRemoving()) return;

            // Show the loading icon while the video is being compressed.
            activity.loadingIcon.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... paths) {

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isRemoving()) return "2";

            String filePath = null;
            try {

                filePath = SiliCompressor.with(activity.getContext()).compressVideo(paths[0], paths[1], 0, 0, 3000000);
            } catch (URISyntaxException e) {

                e.printStackTrace();
            }

            // Add uncompressed video to gallery.
            // Save the name and description of a video in a ContentValues map.
            ContentValues values = new ContentValues(3);
            values.put(MediaStore.Video.Media.TITLE, "Here_Before_" + System.currentTimeMillis());
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, activity.video.getAbsolutePath());

            if (activity.getContext() != null) {

                // Add a new record (identified by uri) without the video, but with the values just set.
                Uri uri = activity.getContext().getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

                // Now get a handle to the file for that record, and save the data into it.
                try {

                    InputStream is = new FileInputStream(activity.video);
                    if (uri != null) {

                        OutputStream os = activity.getContext().getContentResolver().openOutputStream(uri);
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

                activity.getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

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
                }
            }
            return "2";
        }

        @Override
        protected void onPostExecute(String meaninglessString) {

            super.onPostExecute(meaninglessString);

            Chat activity = activityWeakRef.get();
            if (activity == null || activity.isRemoving()) return;

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
            // Allow initChatAdapter() to get rid of the loadingIcon with this boolean.
            activity.needLoadingIcon = false;
        }
    }

    private void firebaseUpload() {

        Log.i(TAG, "firebaseUploadImage()");

        // Show the loading icon while the image is being uploaded to Firebase.
        loadingIcon.setVisibility(View.VISIBLE);

        if (!fileIsImage) {

            // Video.
            final StorageReference storageReferenceVideo = FirebaseStorage.getInstance().getReference("Video").child(String.valueOf(System.currentTimeMillis()));
            uploadTask = storageReferenceVideo.putFile(videoURI);

            storageReferenceVideo.putFile(videoURI).addOnSuccessListener(taskSnapshot -> storageReferenceVideo.getDownloadUrl().addOnSuccessListener(uri -> {

                Log.i(TAG, "uploadImage() -> onSuccess");

                // Change boolean to true - scrolls to the bottom of the recyclerView (in initChatAdapter()).
                messageSent = true;

                if (newShape) {

                    DatabaseReference newFirebaseShape = null;
                    if (radius != 0) {

                        // Shape is a circle.

                        // Since the uuid doesn't already exist in Firebase, add the circle.
                        CircleOptions circleOptions = new CircleOptions()
                                .center(new LatLng(circleLatitude, circleLongitude))
                                .clickable(true)
                                .radius(radius);
                        CircleInformation circleInformation = new CircleInformation();
                        circleInformation.setCircleOptions(circleOptions);
                        circleInformation.setShapeUUID(shapeUUID);

                        if (radius == 1) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Point").push();
                        } else if (1 < radius && radius <= 10) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Small").push();
                        } else if (10 < radius && radius <= 50) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Medium").push();
                        } else if (50 < radius) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Large").push();
                        }

                        if (newFirebaseShape != null) {

                            newFirebaseShape.setValue(circleInformation);
                        }
                    } else if (polygonArea != 0) {

                        // Shape is a polygon.

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
                        polygonInformation.setArea(polygonArea);
                        polygonInformation.setPolygonOptions(polygonOptions);
                        polygonInformation.setShapeUUID(shapeUUID);

                        if (polygonArea <= Math.PI * (Math.pow(10, 2))) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Small").push();
                        } else if (Math.PI * (Math.pow(10, 2)) < polygonArea && polygonArea <= Math.PI * (Math.pow(50, 2))) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Medium").push();
                        } else if (Math.PI * (Math.pow(50, 2)) < polygonArea) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Large").push();
                        }

                        if (newFirebaseShape != null) {

                            newFirebaseShape.setValue(polygonInformation);
                        }
                    } else {

                        // Both radius and polygonArea are null.
                        toastMessageLong("Oops! Something went wrong!");
                        return;
                    }

                    newShape = false;
                }

                String userUUID = UUID.randomUUID().toString();

                // If mentions exist, add to the user's DMs.
                if (removedDuplicatesMentions != null) {

                    ArrayList<String> messagedUsersAL = new ArrayList<>();
                    for (String mention : removedDuplicatesMentions) {

                        for (int i = 0; i < userUUIDAL.size(); i++) {

                            String userEmail = userEmailAL.get(i);
                            if (userUUIDAL.get(i).equals(mention) && !messagedUsersAL.contains(userEmail)) {

                                // Prevent sending the same DM to a user multiple times.
                                messagedUsersAL.add(userEmail);

                                String email = userEmailAL.get(i);

                                DMInformation dmInformation = new DMInformation();
                                // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                Object date = ServerValue.TIMESTAMP;
                                dmInformation.setDate(date);
                                dmInformation.setLat(latFirebaseValue);
                                dmInformation.setLon(lonFirebaseValue);
                                if (mInput.getText().toString().trim().length() != 0) {
                                    dmInformation.setMessage(mInput.getText().toString().trim());
                                }
                                dmInformation.setPosition(mUser.size());
                                dmInformation.setSeenByUser(false);
                                if (radius != 0) {
                                    dmInformation.setSize(radius);
                                    dmInformation.setShapeIsCircle(true);
                                } else {
                                    dmInformation.setSize(polygonArea);
                                    dmInformation.setShapeIsCircle(false);
                                }
                                dmInformation.setShapeUUID(shapeUUID);
                                dmInformation.setUserIsWithinShape(userIsWithinShape);
                                dmInformation.setUserUUID(userUUID);
                                dmInformation.setVideoURL(uri.toString());

                                // Firebase does not allow ".", so replace them with ",".
                                String receiverEmailFirebase = email.replace(".", ",");
                                DatabaseReference newDM = FirebaseDatabase.getInstance().getReference().child("Users").child(receiverEmailFirebase).child("ReceivedDMs").push();
                                newDM.setValue(dmInformation);
                                break;
                            }
                        }
                    }
                }

                MessageInformation messageInformation = new MessageInformation();
                // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                // This will cause onDataChange to fire twice; optimizations could be made in the future.
                Object date = ServerValue.TIMESTAMP;
                messageInformation.setDate(date);
                // If user has a Google account, get email one way. Else, get email another way.
                GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(mContext);
                String email;
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                if (acct != null) {
                    email = acct.getEmail();
                } else {
                    email = sharedPreferences.getString("userToken", "null");
                }
                messageInformation.setEmail(email);
                if (mInput.getText().toString().trim().length() != 0) {

                    messageInformation.setMessage(mInput.getText().toString());
                }
                messageInformation.setPosition(mUser.size());
                messageInformation.setUserIsWithinShape(userIsWithinShape);
                messageInformation.setUserUUID(userUUID);
                messageInformation.setVideoURL(uri.toString());
                DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID).push();
                newMessage.setValue(messageInformation);

                mInput.getText().clear();
                if (!removedDuplicatesMentions.isEmpty()) {
                    removedDuplicatesMentions.clear();
                }
                // For some reason, if the text begins with a mention and onCreateView was called after the mention was added, the mention is not cleared with one call to clear().
                mInput.getText().clear();
                videoImageView.setVisibility(View.GONE);
                videoImageView.setImageDrawable(null);
                if (video != null) {

                    deleteDirectory(video);
                }
                sendButtonClicked = false;
                loadingIcon.setVisibility(View.GONE);
            }))
                    .addOnFailureListener(ex -> {

                        // Handle unsuccessful uploads
                        loadingIcon.setVisibility(View.GONE);
                        toastMessageLong(ex.getMessage());
                        Log.e(TAG, "firebaseUpload() -> !fileIsImage -> onFailure -> " + ex.getMessage());
                    });
        } else {

            // byteArray and image.
            final StorageReference storageReferenceImage = FirebaseStorage.getInstance().getReference("Images").child(String.valueOf(System.currentTimeMillis()));
            uploadTask = storageReferenceImage.putBytes(byteArray);

            storageReferenceImage.putBytes(byteArray).addOnSuccessListener(taskSnapshot -> storageReferenceImage.getDownloadUrl().addOnSuccessListener(uri -> {

                Log.i(TAG, "uploadImage() -> onSuccess");

                // Change boolean to true - scrolls to the bottom of the recyclerView (in initChatAdapter()).
                messageSent = true;

                if (newShape) {

                    DatabaseReference newFirebaseShape = null;
                    if (radius != 0) {

                        // Shape is a circle.

                        // Since the uuid doesn't already exist in Firebase, add the circle.
                        CircleOptions circleOptions = new CircleOptions()
                                .center(new LatLng(circleLatitude, circleLongitude))
                                .clickable(true)
                                .radius(radius);
                        CircleInformation circleInformation = new CircleInformation();
                        circleInformation.setCircleOptions(circleOptions);
                        circleInformation.setShapeUUID(shapeUUID);

                        if (radius == 1) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Point").push();
                        } else if (1 < radius && radius <= 10) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Small").push();
                        } else if (10 < radius && radius <= 50) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Medium").push();
                        } else if (50 < radius) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Large").push();
                        }

                        if (newFirebaseShape != null) {

                            newFirebaseShape.setValue(circleInformation);
                        }
                    } else if (polygonArea != 0) {

                        // Shape is a polygon.

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
                        polygonInformation.setArea(polygonArea);
                        polygonInformation.setPolygonOptions(polygonOptions);
                        polygonInformation.setShapeUUID(shapeUUID);

                        if (polygonArea <= Math.PI * (Math.pow(10, 2))) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Small").push();
                        } else if (Math.PI * (Math.pow(10, 2)) < polygonArea && polygonArea <= Math.PI * (Math.pow(50, 2))) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Medium").push();
                        } else if (Math.PI * (Math.pow(50, 2)) < polygonArea) {

                            newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Large").push();
                        }

                        if (newFirebaseShape != null) {

                            newFirebaseShape.setValue(polygonInformation);
                        }
                    } else {

                        // Both radius and polygonArea are null.
                        toastMessageLong("Oops! Something went wrong!");
                        return;
                    }

                    newShape = false;
                }

                String userUUID = UUID.randomUUID().toString();

                // If mentions exist, add to the user's DMs.
                if (removedDuplicatesMentions != null) {

                    ArrayList<String> messagedUsersAL = new ArrayList<>();
                    for (String mention : removedDuplicatesMentions) {

                        for (int i = 0; i < userUUIDAL.size(); i++) {

                            String userEmail = userEmailAL.get(i);
                            if (userUUIDAL.get(i).equals(mention) && !messagedUsersAL.contains(userEmail)) {

                                // Prevent sending the same DM to a user multiple times.
                                messagedUsersAL.add(userEmail);

                                String email = userEmailAL.get(i);

                                DMInformation dmInformation = new DMInformation();
                                // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                Object date = ServerValue.TIMESTAMP;
                                dmInformation.setDate(date);
                                dmInformation.setImageURL(uri.toString());
                                dmInformation.setLat(latFirebaseValue);
                                dmInformation.setLon(lonFirebaseValue);
                                if (mInput.getText().toString().trim().length() != 0) {
                                    dmInformation.setMessage(mInput.getText().toString().trim());
                                }
                                dmInformation.setPosition(mUser.size());
                                dmInformation.setSeenByUser(false);
                                if (radius != 0) {
                                    dmInformation.setSize(radius);
                                    dmInformation.setShapeIsCircle(true);
                                } else {
                                    dmInformation.setSize(polygonArea);
                                    dmInformation.setShapeIsCircle(false);
                                }
                                dmInformation.setShapeUUID(shapeUUID);
                                dmInformation.setUserIsWithinShape(userIsWithinShape);
                                dmInformation.setUserUUID(userUUID);

                                // Firebase does not allow ".", so replace them with ",".
                                String receiverEmailFirebase = email.replace(".", ",");
                                DatabaseReference newDM = FirebaseDatabase.getInstance().getReference().child("Users").child(receiverEmailFirebase).child("ReceivedDMs").push();
                                newDM.setValue(dmInformation);
                                break;
                            }
                        }
                    }
                }

                MessageInformation messageInformation = new MessageInformation();
                // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                // This will cause onDataChange to fire twice; optimizations could be made in the future.
                Object date = ServerValue.TIMESTAMP;
                messageInformation.setDate(date);
                // If user has a Google account, get email one way. Else, get email another way.
                GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(mContext);
                String email;
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                if (acct != null) {
                    email = acct.getEmail();
                } else {
                    email = sharedPreferences.getString("userToken", "null");
                }
                messageInformation.setEmail(email);
                messageInformation.setImageURL(uri.toString());
                if (mInput.getText().toString().trim().length() != 0) {

                    messageInformation.setMessage(mInput.getText().toString());
                }
                messageInformation.setPosition(mUser.size());
                messageInformation.setUserIsWithinShape(userIsWithinShape);
                messageInformation.setUserUUID(userUUID);
                DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID).push();
                newMessage.setValue(messageInformation);

                mInput.getText().clear();
                if (!removedDuplicatesMentions.isEmpty()) {
                    removedDuplicatesMentions.clear();
                }
                // For some reason, if the text begins with a mention and onCreateView was called after the mention was added, the mention is not cleared with one call to clear().
                mInput.getText().clear();
                imageView.setVisibility(View.GONE);
                imageView.setImageDrawable(null);
                if (image != null) {

                    deleteDirectory(image);
                }
                sendButtonClicked = false;
                loadingIcon.setVisibility(View.GONE);
            }))
                    .addOnFailureListener(ex -> {

                        // Handle unsuccessful uploads
                        loadingIcon.setVisibility(View.GONE);
                        toastMessageLong(ex.getMessage());
                        Log.e(TAG, "firebaseUpload() -> else -> onFailure -> " + ex.getMessage());
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

        shortToast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT);
        shortToast.setGravity(Gravity.CENTER, 0, 0);
        shortToast.show();
    }

    private void toastMessageLong(String message) {

        longToast = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
        longToast.setGravity(Gravity.CENTER, 0, 0);
        longToast.show();
    }
}