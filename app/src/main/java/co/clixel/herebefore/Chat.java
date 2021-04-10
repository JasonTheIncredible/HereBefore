package co.clixel.herebefore;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcel;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
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
import com.linkedin.android.spyglass.mentions.Mentionable;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsVisibilityManager;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizer;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizerConfig;
import com.linkedin.android.spyglass.tokenization.interfaces.QueryTokenReceiver;
import com.linkedin.android.spyglass.ui.MentionsEditText;
import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderListener;
import com.otaliastudios.transcoder.resize.AspectRatioResizer;
import com.otaliastudios.transcoder.resize.FractionResizer;
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import id.zelory.compressor.Compressor;

import static android.app.Activity.RESULT_OK;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;
import static java.text.DateFormat.getDateTimeInstance;

public class Chat extends Fragment implements
        PopupMenu.OnMenuItemClickListener,
        LocationListener,
        QueryTokenReceiver,
        SuggestionsVisibilityManager {

    private static final String TAG = "Chat";
    private static final int Request_User_Location_Code = 42, Request_ID_Take_Photo = 69, Request_ID_Record_Video = 420, Media_Code = 27, Update_Interval = 0, Fastest_Interval = 0;
    private MentionsEditText mInput;
    private ArrayList<String> mTime, mUser, mImage, mVideo, mSuggestions;
    private ArrayList<SpannableString> mText;
    private ArrayList<Boolean> mUserIsWithinShape;
    private ArrayList<String> circleUUIDsAL = new ArrayList<>();
    private ArrayList<LatLng> circleCentersAL = new ArrayList<>();
    private RecyclerView chatRecyclerView, mentionsRecyclerView;
    private Integer index, top, UUIDDatesPairsSize;
    private ChildEventListener childEventListener;
    private FloatingActionButton sendButton, mediaButton;
    private boolean theme, onStartJustCalled, continueWithODC = true, loadingOlderMessages = false, clickedOnMention = false, fromDms = false, noMoreMessages = false, showProgressIndeterminate = true, reachedEndOfRecyclerView = true, messageSent = false, fileIsImage,
            checkPermissionsPicture, newShape, uploadNeeded = false, imageCompressionProcessComplete = false, videoCompressionProcessComplete = false;
    private Boolean userIsWithinShape;
    private View.OnLayoutChangeListener onLayoutChangeListener;
    private String shapeUUID, reportedUser, UUIDToHighlight, imageFile, videoFile, lastKnownKey;
    private Double shapeLat, shapeLon;
    private PopupMenu mediaButtonMenu;
    private ImageView imageView, videoImageView;
    private Uri imageURI, videoURI;
    private StorageTask<?> uploadTask;
    private LinearLayoutManager chatRecyclerViewLinearLayoutManager;
    private LinearLayoutManager mentionsRecyclerViewLinearLayoutManager;
    private File image, video;
    private byte[] byteArray;
    private ProgressBar progressIcon, progressIconIndeterminate;
    private Toast shortToast, longToast;
    private static final String BUCKET = "text-suggestions";
    private View rootView;
    private Context mContext;
    private Activity mActivity;
    private Query mQuery;
    private Drawable imageDrawable, videoDrawable;
    private int shapeLatInt, shapeLonInt, showInterstitialAdCounterMedia = 0, showInterstitialAdCounterText = 0, fromMediaCounter = 0;
    private Integer previouslyHighlightedPosition;
    private LocationManager locationManager;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private TextView newShapeTextView, positionRelativeToShapeTextView;
    private List<Pair<String, Long>> UUIDDatesPairs;
    private Snackbar snackBar;
    private final WordTokenizerConfig tokenizerConfig = new WordTokenizerConfig
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

        // theme == true is light mode.
        theme = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(getString(R.string.prefTheme), false);

        // Get info from Map.java
        if (mActivity != null) {

            Bundle extras = mActivity.getIntent().getExtras();
            if (extras != null) {

                UUIDToHighlight = extras.getString("UUIDToHighlight");
                fromDms = extras.getBoolean("fromDms");
                newShape = extras.getBoolean("newShape");
                // No need to get these if it's a new shape, as the most up-to-date location information will be received here.
                if (!newShape) {
                    shapeLat = extras.getDouble("shapeLat");
                    shapeLon = extras.getDouble("shapeLon");
                } else {
                    //noinspection unchecked
                    circleUUIDsAL = (ArrayList<String>) extras.getSerializable("circleUUIDsAL");
                    //noinspection unchecked
                    circleCentersAL = (ArrayList<LatLng>) extras.getSerializable("circleCentersAL");
                }
                shapeUUID = extras.getString("shapeUUID");
                imageFile = extras.getString("imageFile");
                videoFile = extras.getString("videoFile");
                lastKnownKey = extras.getString("lastKnownKey");
            } else {

                Log.e(TAG, "onCreateView() -> extras == null");
            }
        }
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.i(TAG, "onCreateView()");

        rootView = inflater.inflate(R.layout.chat, container, false);

        mediaButton = rootView.findViewById(R.id.mediaButton);
        imageView = rootView.findViewById(R.id.imageView);
        videoImageView = rootView.findViewById(R.id.videoImageView);
        mInput = rootView.findViewById(R.id.input);
        sendButton = rootView.findViewById(R.id.sendButton);
        chatRecyclerView = rootView.findViewById(R.id.messageList);
        mentionsRecyclerView = rootView.findViewById(R.id.suggestionsList);
        progressIconIndeterminate = rootView.findViewById(R.id.progressIconIndeterminate);
        progressIcon = rootView.findViewById(R.id.progressIcon);
        newShapeTextView = rootView.findViewById(R.id.newShapeTextView);
        positionRelativeToShapeTextView = rootView.findViewById(R.id.positionRelativeToShapeTextView);

        chatRecyclerViewLinearLayoutManager = new LinearLayoutManager(mActivity);
        mentionsRecyclerViewLinearLayoutManager = new LinearLayoutManager(mActivity);


        // If user takes a picture while in Chat, the fragment's lifecycle restarts, so make progressIcon visible. Else, user restarted the app while the shape was still new (and once again, restarted the lifecycle) so set the newShapeTextView to visible.
        if (image != null && !imageCompressionProcessComplete) {

            showProgressIndeterminate = true;
            newShapeTextView.setVisibility(View.GONE);
        } else if (image != null && newShape) {

            newShapeTextView.setVisibility(View.VISIBLE);
        }

        // If user takes a video while in Chat, the fragment's lifecycle restarts, so make progressIcon visible. Else, user restarted the app while the shape was still new (and once again, restarted the lifecycle) so set the newShapeTextView to visible.
        if (video != null && !videoCompressionProcessComplete) {

            progressIcon.setVisibility(View.VISIBLE);
            newShapeTextView.setVisibility(View.GONE);
        } else if (video != null && newShape) {

            progressIconIndeterminate.setVisibility(View.GONE);
            newShapeTextView.setVisibility(View.VISIBLE);
        }

        // Show progressIndeterminate after restart until initRecyclerView.
        if (!newShape) {

            showProgressIndeterminate = true;
        }

        // If file was created in Map, process it here.
        if (imageFile != null) {

            imageCompressAndAddToGalleryAsync();
            fileIsImage = true;
        } else if (videoFile != null) {

            videoCompressAndAddToGalleryAsync();
            fileIsImage = false;
            showProgressIndeterminate = false;
        }

        if (UUIDDatesPairsSize == null) {

            UUIDDatesPairs = new ArrayList<>();

            mTime = new ArrayList<>();
            mUser = new ArrayList<>();
            mImage = new ArrayList<>();
            mVideo = new ArrayList<>();
            mText = new ArrayList<>();
            mSuggestions = new ArrayList<>();
            mUserIsWithinShape = new ArrayList<>();
        }

        // Make the progressIconIndeterminate visible upon the first load, as it can sometimes take a while to show anything. It should be made invisible in initChatAdapter.
        if (progressIconIndeterminate != null && showProgressIndeterminate) {

            progressIconIndeterminate.setVisibility(View.VISIBLE);
        }

        if (!newShape && userIsWithinShape == null) {

            mInput.setHint(R.string.calculating_distance);
            positionRelativeToShapeTextView.setText(R.string.calculating_distance);
        } else if (newShape || userIsWithinShape) {

            mInput.setHint(R.string.message_from_within_circle);
            positionRelativeToShapeTextView.setText(R.string.inside_circle);
        } else {

            mInput.setHint(R.string.message_from_outside_circle);
            positionRelativeToShapeTextView.setText(R.string.outside_circle);
        }

        return rootView;
    }

    @Override
    public void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        if (!newShape) {

            // Get a value with 1 decimal point and use it for Firebase.
            double nearLeftPrecisionLat = Math.pow(10, 1);
            // Can't create a firebase path with '.', so get rid of decimal.
            double nearLeftLatTemp = (int) (nearLeftPrecisionLat * shapeLat) / nearLeftPrecisionLat;
            nearLeftLatTemp *= 10;
            shapeLatInt = (int) nearLeftLatTemp;

            double nearLeftPrecisionLon = Math.pow(10, 1);
            // Can't create a firebase path with '.', so get rid of decimal.
            double nearLeftLonTemp = (int) (nearLeftPrecisionLon * shapeLon) / nearLeftPrecisionLon;
            nearLeftLonTemp *= 10;
            shapeLonInt = (int) nearLeftLonTemp;
        }

        // Set to true to scroll to the bottom of chatRecyclerView. Also prevents duplicates in addQuery.
        onStartJustCalled = true;

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

        // If the value isn't null, check if the latest date is the same as the one in the recyclerView. If the value is null, it's the first time loading.
        if (UUIDDatesPairsSize != null && UUIDDatesPairsSize != -1) {

            Query query = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(shapeUUID).limitToLast(1);
            query.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    // This is to prevent a bug where ODC for this ListenerForSingleValueEvent gets called twice. I have no idea what's causing it, so it should be addressed in the future.
                    if (continueWithODC) {

                        continueWithODC = false;
                    } else {

                        return;
                    }

                    // Edge case where user reloading into a chat that no longer exists.
                    if (snapshot.getChildrenCount() == 0 && !newShape) {

                        progressIconIndeterminate.setVisibility(View.GONE);
                        mInput.setFocusable(false);
                        mediaButton.setEnabled(false);
                        sendButton.setEnabled(false);
                        showMessageLong("Shape was deleted. Please return to map.");
                        return;
                    } else if (snapshot.getChildrenCount() == 0) {

                        // If the user has to leave this activity (when no messages exist) to turn on GPS and re-enters, this will trigger.
                        progressIconIndeterminate.setVisibility(View.GONE);
                    }

                    for (DataSnapshot ds : snapshot.getChildren()) {

                        Long date = (Long) ds.child("date").getValue();
                        // If the saved date and the latest date match, then there's no need to re-download everything from Firebase.
                        if (date != null && UUIDDatesPairs != null && !date.equals(UUIDDatesPairs.get(UUIDDatesPairs.size() - 1).second)) {

                            Log.i(TAG, "onStart() -> new messages since app restarted");

                            getFirebaseMessages(null);
                        } else {

                            Log.i(TAG, "onStart() -> no new messages");

                            addQuery();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    Log.e(TAG, "DatabaseError");
                    sendButton.setEnabled(true);
                    progressIconIndeterminate.setVisibility(View.GONE);
                    showMessageLong(error.getMessage());
                }
            });
        } else if (!newShape) {

            getFirebaseMessages(null);
        } else if (UUIDDatesPairsSize != null && imageCompressionProcessComplete) {

            // Case where user clicks on image / video multiple times before sending it.
            progressIconIndeterminate.setVisibility(View.GONE);
        }

        // Check RecyclerView scroll state (to allow the layout to move up when keyboard appears).
        if (chatRecyclerView != null) {

            chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {

                    super.onScrollStateChanged(recyclerView, newState);

                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {

                        reachedEndOfRecyclerView = false;
                    }

                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                        // Get the top visible position. If it is (almost) the last loaded item, load more.
                        int firstCompletelyVisibleItemPosition = chatRecyclerViewLinearLayoutManager.findFirstCompletelyVisibleItemPosition();

                        if (firstCompletelyVisibleItemPosition == 0 && !noMoreMessages) {

                            progressIconIndeterminate.setVisibility(View.VISIBLE);
                            loadingOlderMessages = true;
                            getFirebaseMessages(mUser.get(0));
                        }

                        // If RecyclerView can't be scrolled down, reachedEndOfRecyclerView = true.
                        reachedEndOfRecyclerView = !recyclerView.canScrollVertically(1);
                    }
                }
            });

            // If RecyclerView is scrolled to the bottom, move the layout up when the keyboard appears.
            if (onLayoutChangeListener == null) {

                onLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {

                    if (reachedEndOfRecyclerView) {

                        if (bottom < oldBottom) {

                            if (chatRecyclerView.getAdapter() != null && chatRecyclerView.getAdapter().getItemCount() > 0) {

                                chatRecyclerView.postDelayed(() -> chatRecyclerView.smoothScrollToPosition(

                                        chatRecyclerView.getAdapter().getItemCount() - 1), 100);
                            }
                        }
                    }
                };
            }

            chatRecyclerView.addOnLayoutChangeListener(onLayoutChangeListener);
        }

        // Hide the imageView or videoImageView if user presses the delete button.
        if (mInput != null) {

            mInput.setOnKeyListener((v, keyCode, event) -> {

                if (keyCode == KeyEvent.KEYCODE_DEL && (imageView.getVisibility() == View.VISIBLE || videoImageView.getVisibility() == View.VISIBLE) &&
                        (mInput.getText().toString().trim().length() == 0 || mInput.getSelectionStart() == 0)) {

                    imageView.setVisibility(View.GONE);
                    imageView.setImageDrawable(null);
                    videoImageView.setVisibility(View.GONE);
                    videoImageView.setImageDrawable(null);

                    if (image != null) {

                        deleteDirectory(image);
                    } else if (video != null) {

                        deleteDirectory(video);
                    }
                }

                // Keep "return false" or the enter key will not go to the next line.
                return false;
            });

            mInput.setTokenizer(new WordTokenizer(tokenizerConfig));
            mInput.setQueryTokenReceiver(this);
            mInput.setSuggestionsVisibilityManager(this);
        }

        mediaButton.setOnClickListener(view -> {

            Log.i(TAG, "onStart() -> mediaButton -> onClick");

            mediaButtonMenu = new PopupMenu(mActivity, mediaButton);
            mediaButtonMenu.setOnMenuItemClickListener(Chat.this);
            mediaButtonMenu.inflate(R.menu.mediabutton_menu);
            mediaButtonMenu.show();
        });

        // onClickListener for sending recyclerviewlayout to Firebase.
        sendButton.setOnClickListener(view -> {

            Log.i(TAG, "onStart() -> sendButton -> onClick");

            if (mInput == null) {

                return;
            }

            // Close keyboard.
            if (view != null) {

                InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {

                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                } else {

                    Log.e(TAG, "onStart() -> sendButton -> imm == null");
                }
                if (mInput != null) {

                    mInput.clearFocus();
                }
            }

            if (mInput != null && mInput.getText().toString().trim().isEmpty() && imageView.getVisibility() == View.GONE && videoImageView.getVisibility() == View.GONE) {

                return;
            }

            // Prevent creating a new shape if image / video doesn't exist.
            if (newShape && (imageView.getDrawable() == null && videoImageView.getDrawable() == null)) {

                showMessageShort("Picture or video required.");
                return;
            }

            sendButton.setEnabled(false);

            progressIconIndeterminate.setVisibility(View.VISIBLE);

            // Check location permissions.
            if (ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                FusedLocationProviderClient mFusedLocationClient = getFusedLocationProviderClient(mContext);

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(mActivity, location -> {

                            if (location != null) {

                                if (newShape) {

                                    newShapeTextView.setVisibility(View.GONE);
                                }

                                float[] newDistance = new float[2];
                                if (!newShape) {

                                    Location.distanceBetween(shapeLat, shapeLon, location.getLatitude(), location.getLongitude(), newDistance);
                                }

                                if (!newShape && location.getAccuracy() < 50 && newDistance[0] > 50) {
                                    // User is far outside of the circle, so no need to wait for an accurate location.
                                } else if (location.getAccuracy() >= 10) {

                                    showMessageLong("Please wait for better location accuracy." + "\n" + "Moving your phone around should help." + "\n" + "Current: " + location.getAccuracy() + "\n" + "Required: < 10");
                                    sendButton.setEnabled(true);
                                    progressIconIndeterminate.setVisibility(View.GONE);
                                    return;
                                }

                                // Cancel any of the previous 2 toasts.
                                cancelToasts();

                                if (snackBar != null && snackBar.isShown()) {

                                    snackBar.dismiss();
                                }

                                if (newShape) {

                                    uploadNeeded = true;
                                    userIsWithinShape = true;

                                    shapeLat = location.getLatitude();
                                    shapeLon = location.getLongitude();

                                    // Get a value with 1 decimal point and use it for Firebase.
                                    double nearLeftPrecisionLat = Math.pow(10, 1);
                                    // Can't create a firebase path with '.', so get rid of decimal.
                                    double nearLeftLatTemp = (int) (nearLeftPrecisionLat * shapeLat) / nearLeftPrecisionLat;
                                    nearLeftLatTemp *= 10;
                                    shapeLatInt = (int) nearLeftLatTemp;

                                    double nearLeftPrecisionLon = Math.pow(10, 1);
                                    // Can't create a firebase path with '.', so get rid of decimal.
                                    double nearLeftLonTemp = (int) (nearLeftPrecisionLon * shapeLon) / nearLeftPrecisionLon;
                                    nearLeftLonTemp *= 10;
                                    shapeLonInt = (int) nearLeftLonTemp;

                                    // If circleUUIDsAL == null or circleCentersAL == null, load them and check whether a new circle should be created. Else, check for new circles at the user's location and possibly add to an existing shape.
                                    if (circleUUIDsAL == null || circleCentersAL == null) {

                                        DatabaseReference firebasePoints = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child("Points");
                                        firebasePoints.addListenerForSingleValueEvent(new ValueEventListener() {

                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                loadCirclesODC(snapshot, location);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                                Log.e(TAG, "DatabaseError");
                                                sendButton.setEnabled(true);
                                                progressIconIndeterminate.setVisibility(View.GONE);
                                                showMessageLong(error.getMessage());
                                            }
                                        });
                                    } else {

                                        Query query = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child("Points").limitToLast(1);
                                        query.addListenerForSingleValueEvent(new ValueEventListener() {

                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                // If childrenCount == 0, this is a new shape.
                                                if (snapshot.getChildrenCount() == 0) {

                                                    addQuery();
                                                    return;
                                                }

                                                for (DataSnapshot ds : snapshot.getChildren()) {

                                                    String shapeUUID = (String) ds.child("shapeUUID").getValue();

                                                    if (shapeUUID != null) {

                                                        // If new circles exist, add them to the map. Else, add the query to add new shapes in the future.
                                                        if (!circleUUIDsAL.contains(shapeUUID)) {

                                                            // If lastKnownKey is null, no shapes existed before user put app into the background and all shapes need to be retrieved.
                                                            Query query;
                                                            if (lastKnownKey == null) {

                                                                query = FirebaseDatabase.getInstance().getReference()
                                                                        .child("Shapes").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child("Points");
                                                            } else {

                                                                query = FirebaseDatabase.getInstance().getReference()
                                                                        .child("Shapes").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child("Points")
                                                                        .orderByKey()
                                                                        .startAt(lastKnownKey);
                                                            }

                                                            query.addListenerForSingleValueEvent(new ValueEventListener() {

                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                                    loadCirclesODC(snapshot, location);
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError error) {

                                                                    Log.e(TAG, "DatabaseError");
                                                                    sendButton.setEnabled(true);
                                                                    progressIconIndeterminate.setVisibility(View.GONE);
                                                                    showMessageLong(error.getMessage());
                                                                }
                                                            });
                                                        } else {

                                                            compareShapesToLocation(location);
                                                        }
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                                Log.e(TAG, "DatabaseError");
                                                sendButton.setEnabled(true);
                                                progressIconIndeterminate.setVisibility(View.GONE);
                                                showMessageLong(error.getMessage());
                                            }
                                        });
                                    }
                                } else {

                                    // Warn the user that their location just changed between the time they clicked the send button and now.
                                    if ((userIsWithinShape == null || !userIsWithinShape) && newDistance[0] < 5) {

                                        showMessageShort("You are now inside the circle. Click send again.");
                                        mInput.setHint(R.string.message_from_within_circle);
                                        positionRelativeToShapeTextView.setText(R.string.inside_circle);
                                        userIsWithinShape = true;
                                        sendButton.setEnabled(true);
                                        progressIconIndeterminate.setVisibility(View.GONE);
                                        return;
                                    } else if ((userIsWithinShape == null || userIsWithinShape) && newDistance[0] >= 5) {

                                        showMessageShort("You are now outside the circle. Click send again.");
                                        mInput.setHint(R.string.message_from_outside_circle);
                                        int distanceOutsideCircle = (int) newDistance[0] - 5;
                                        if (distanceOutsideCircle == 0) {

                                            distanceOutsideCircle = 1;
                                        }
                                        positionRelativeToShapeTextView.setText(getString(R.string.distance_outside_circle, distanceOutsideCircle));
                                        userIsWithinShape = false;
                                        sendButton.setEnabled(true);
                                        progressIconIndeterminate.setVisibility(View.GONE);
                                        return;
                                    }

                                    // Send recyclerviewlayout to Firebase.
                                    if (imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                                        firebaseUpload();
                                    } else {

                                        showInterstitialAdCounterText++;
                                        if (showInterstitialAdCounterText == 10) {

                                            showInterstitialAdCounterText = 0;
                                            showInterstitialAdCounterMedia = 0;
                                            ((Navigation) requireActivity()).showInterstitialAd();
                                        }

                                        // Change boolean to true - scrolls to the bottom of the recyclerView (in initChatAdapter).
                                        messageSent = true;

                                        MessageInformation messageInformation = new MessageInformation();
                                        Object date = ServerValue.TIMESTAMP;
                                        messageInformation.setDate(date);
                                        String firebaseUid = FirebaseAuth.getInstance().getUid();
                                        messageInformation.setFirebaseUid(firebaseUid);
                                        String input = mInput.getText().toString().trim();
                                        messageInformation.setMessage(input);
                                        messageInformation.setUserIsWithinShape(userIsWithinShape);
                                        String userUUID = UUID.randomUUID().toString();
                                        messageInformation.setUserUUID(userUUID);
                                        DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(shapeUUID).push();
                                        newMessage.setValue(messageInformation);

                                        mInput.getText().clear();

                                        // For some reason, if the text begins with a mention and onCreateView was called after the mention was added, the mention is not cleared with one call to clear().
                                        mInput.getText().clear();
                                        progressIconIndeterminate.setVisibility(View.GONE);
                                        sendButton.setEnabled(true);
                                    }
                                }
                            } else {

                                // If user leaves this activity to turn on GPS and returns, GPS will originally be null so this will trigger at first.
                                if (newShape) {

                                    newShapeTextView.setVisibility(View.GONE);
                                }
                                sendButton.setEnabled(true);
                                progressIconIndeterminate.setVisibility(View.GONE);
                                showMessageLong("Please wait a moment and try again.");
                            }
                        });
            } else {

                progressIconIndeterminate.setVisibility(View.GONE);
                sendButton.setEnabled(true);
                checkLocationPermissions();
            }
        });

        imageView.setOnClickListener(v -> {

            Log.i(TAG, "imageView -> onClick");

            cancelToasts();

            Intent Activity = new Intent(mContext, PhotoView.class);
            Activity.putExtra("imgUrl", imageURI.toString());
            startActivity(Activity);
        });

        videoImageView.setOnClickListener(v -> {

            Log.i(TAG, "videoImageView -> onClick");

            cancelToasts();

            Intent Activity = new Intent(mContext, VideoView.class);
            Activity.putExtra("videoUrl", videoURI.toString());
            startActivity(Activity);
        });
    }

    private void loadCirclesODC(DataSnapshot snapshot, Location location) {

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
                }
            }
        }

        compareShapesToLocation(location);
    }

    private void compareShapesToLocation(Location location) {

        Log.i(TAG, "compareShapesToLocation()");

        // If user is within a circle, enter it. Else, enter a new one.
        float[] oldDistance = new float[2];
        oldDistance[0] = 3f;
        for (int i = 0; i < circleCentersAL.size(); i++) {

            float[] newDistance = new float[2];
            Location.distanceBetween(circleCentersAL.get(i).latitude, circleCentersAL.get(i).longitude, location.getLatitude(), location.getLongitude(), newDistance);

            if (newDistance[0] <= 1) {

                newShape = false;
                shapeLat = circleCentersAL.get(i).latitude;
                shapeLon = circleCentersAL.get(i).longitude;
                shapeUUID = circleUUIDsAL.get(i);
                break;
            } else if (newDistance[0] <= 2 && newDistance[0] <= oldDistance[0]) {

                oldDistance[0] = newDistance[0];
                newShape = false;
                shapeLat = circleCentersAL.get(i).latitude;
                shapeLon = circleCentersAL.get(i).longitude;
                shapeUUID = circleUUIDsAL.get(i);
            }
        }

        if (newShape) {

            addQuery();
        } else {

            getFirebaseMessages(null);
        }

        // Message will be uploaded in addQuery.
    }

    private void getFirebaseMessages(String referenceUserUUID) {

        Log.i(TAG, "getFirebaseMessages()");

        Query query;
        if (!fromDms && !clickedOnMention && UUIDDatesPairsSize != null && UUIDDatesPairsSize != -1) {

            query = FirebaseDatabase.getInstance().getReference()
                    .child("MessageThreads").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(shapeUUID)
                    .orderByChild("date")
                    .startAt(UUIDDatesPairs.get(UUIDDatesPairsSize).second);
        } else if (referenceUserUUID == null) {

            query = FirebaseDatabase.getInstance().getReference()
                    .child("MessageThreads").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(shapeUUID)
                    .limitToLast(20);
        } else {

            // If pagination needs to happen multiple times, mUser will not contain the referenceUserUUID.
            if (!mUser.contains(referenceUserUUID)) {

                // Use example: clicking on a DM and pagination occurs multiple times.
                query = FirebaseDatabase.getInstance().getReference()
                        .child("MessageThreads").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(shapeUUID)
                        .orderByChild("date")
                        .endAt(UUIDDatesPairs.get(0).second)
                        .limitToLast(20);
            } else {

                // Use example: manually scrolling to the top and loading older messages.
                query = FirebaseDatabase.getInstance().getReference()
                        .child("MessageThreads").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(shapeUUID)
                        .orderByChild("date")
                        .endAt(UUIDDatesPairs.get(mUser.indexOf(referenceUserUUID)).second)
                        .limitToLast(20);
            }
        }

        fillRecyclerView(query);
    }

    private void fillRecyclerView(Query query) {

        Log.i(TAG, "fillRecyclerView()");

        query.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // Edge case where user clicks into a shape that no longer exists.
                if (snapshot.getChildrenCount() == 0 && !newShape) {

                    progressIconIndeterminate.setVisibility(View.GONE);
                    mInput.setFocusable(false);
                    mediaButton.setEnabled(false);
                    sendButton.setEnabled(false);
                    showMessageLong("Shape was deleted. Please return to map.");
                }

                if ((UUIDDatesPairsSize == null || UUIDDatesPairsSize == -1) && snapshot.getChildrenCount() < 20) {

                    noMoreMessages = true;

                    // If 20 messages exist and the user scrolls to the top, a duplicate of the first item will be added. The following prevents that.
                    if (loadingOlderMessages && snapshot.getChildrenCount() == 1) {

                        progressIconIndeterminate.setVisibility(View.GONE);
                        return;
                    }
                }

                int i;
                if (!fromDms && !loadingOlderMessages && UUIDDatesPairs != null) {

                    i = UUIDDatesPairs.size();
                } else {

                    i = 0;
                }

                for (DataSnapshot ds : snapshot.getChildren()) {

                    String user = (String) ds.child("userUUID").getValue();

                    // Prevents duplicates during getFirebaseMessages.
                    if (mUser.contains(user)) {

                        continue;
                    }

                    Long serverDate = (Long) ds.child("date").getValue();

                    // Add dates to this AL for use in pagination.
                    UUIDDatesPairs.add(i, new Pair<>(user, serverDate));

                    // Used when a user mentions another user with "@".
                    mSuggestions.add(i, user);
                    String imageUrl = (String) ds.child("imageUrl").getValue();
                    String videoUrl = (String) ds.child("videoUrl").getValue();
                    String messageText = (String) ds.child("message").getValue();

                    SpannableString spannableMessageText = null;
                    if (messageText != null) {

                        spannableMessageText = createSpannableMessage(messageText);
                    }

                    Boolean userIsWithinShape = (Boolean) ds.child("userIsWithinShape").getValue();
                    DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                    if (serverDate != null) {

                        Date netDate = (new Date(serverDate));
                        String messageTime = dateFormat.format(netDate);
                        mTime.add(i, messageTime);
                    } else {

                        Log.e(TAG, "onStart() -> serverDate == null");
                    }
                    mUser.add(i, user);
                    mImage.add(i, imageUrl);
                    mVideo.add(i, videoUrl);
                    mText.add(i, spannableMessageText);
                    mUserIsWithinShape.add(i, userIsWithinShape);

                    i++;

                    // Prevent duplicates.
                    if (i == snapshot.getChildrenCount() - 1 && !onStartJustCalled) {

                        break;
                    }
                }

                // Get the X'th previous message from UUIDToHighlight. If this number is 0, the recyclerView needs to paginate.
                String stringToScrollTo = null;
                if (UUIDToHighlight != null) {

                    int scrollPosition = mUser.indexOf(UUIDToHighlight) - 5;
                    if (scrollPosition < 0 && !noMoreMessages) {

                        getFirebaseMessages(UUIDToHighlight);
                        return;
                    } else {

                        scrollPosition = 0;
                    }

                    stringToScrollTo = mUser.get(scrollPosition);
                }

                // If after cycling through all entries, if the message needing to be highlighted does not exist yet, get more Firebase values.
                if (UUIDToHighlight != null && stringToScrollTo != null && !mUser.contains(stringToScrollTo)) {

                    loadingOlderMessages = true;
                    getFirebaseMessages(stringToScrollTo);
                    return;
                }

                // Prevents crash when user toggles between light / dark mode.
                if (chatRecyclerView != null) {

                    if (chatRecyclerView.getAdapter() != null && loadingOlderMessages) {

                        chatRecyclerView.getAdapter().notifyItemRangeInserted(0, (int) snapshot.getChildrenCount() - 1);
                        progressIconIndeterminate.setVisibility(View.GONE);
                    }
                }

                if (!loadingOlderMessages && !newShape) {

                    addQuery();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                showMessageLong(error.getMessage());
            }
        });
    }

    // Change to .limitToLast(1) to cut down on data usage. Otherwise, EVERY child at this node will be downloaded every time the child is updated.
    private void addQuery() {

        // This prevents duplicates when loading into Settings fragment then switched back into Chat (as onStop is never called but onStart is called).
        if (mQuery != null) {

            if (childEventListener != null) {

                mQuery.removeEventListener(childEventListener);
            }
        }

        // If this is the first time calling this eventListener and it's a new shape, initialize the adapter (but don't return, as the childEventListener should still be added), as onChildAdded won't be called the first time.
        if (onStartJustCalled && newShape) {

            initChatAdapter();
        }

        // Add new values to arrayLists one at a time. This prevents the need to download the whole dataSnapshot every time this information is needed.
        if (mQuery == null) {

            mQuery = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(shapeUUID).limitToLast(1);
        }

        if (childEventListener == null) {

            childEventListener = new ChildEventListener() {

                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    Log.i(TAG, "addQuery() -> onChildAdded()");

                    // Read RecyclerView scroll position (for use in initChatAdapter to prevent scrolling after Chat gets updated by another user).
                    if (!onStartJustCalled && chatRecyclerViewLinearLayoutManager != null && chatRecyclerView != null) {

                        index = chatRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
                        View v = chatRecyclerView.getChildAt(0);
                        top = (v == null) ? 0 : (v.getTop() - chatRecyclerView.getPaddingTop());
                    }

                    // If this is the first time calling this eventListener, prevent double posts (as onStart() already added the last item).
                    if (onStartJustCalled) {

                        initChatAdapter();
                        return;
                    }

                    String user = (String) snapshot.child("userUUID").getValue();
                    Long serverDate = (Long) snapshot.child("date").getValue();

                    UUIDDatesPairs.add(new Pair<>(user, serverDate));

                    // Prevents duplicates when user adds a message to a new shape then switches between light / dark mode.
                    if (mUser.contains(user)) {

                        return;
                    }
                    // Used when a user mentions another user with "@".
                    mSuggestions.add(user);
                    String imageUrl = (String) snapshot.child("imageUrl").getValue();
                    String videoUrl = (String) snapshot.child("videoUrl").getValue();
                    String messageText = (String) snapshot.child("message").getValue();

                    SpannableString spannableMessageText = null;
                    if (messageText != null) {

                        spannableMessageText = createSpannableMessage(messageText);
                    }

                    Boolean userIsWithinShape = (Boolean) snapshot.child("userIsWithinShape").getValue();
                    DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                    if (serverDate != null) {

                        Date netDate = (new Date(serverDate));
                        String messageTime = dateFormat.format(netDate);
                        mTime.add(messageTime);
                    } else {

                        Log.e(TAG, "onStart() -> serverDate == null");
                    }
                    mUser.add(user);
                    mImage.add(imageUrl);
                    mVideo.add(videoUrl);
                    mText.add(spannableMessageText);
                    mUserIsWithinShape.add(userIsWithinShape);

                    initChatAdapter();
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    Log.i(TAG, "addQuery() -> onChildChanged()");

                    // Update the serverDate in UUIDDatesPairs, as the original serverDate will be an estimate and a callback from the server will always be made with an accurate time.
                    for (Pair<String, Long> pair : UUIDDatesPairs) {

                        String user = (String) snapshot.child("userUUID").getValue();

                        if (pair.first.equals(user)) {

                            Long serverDate = (Long) snapshot.child("date").getValue();

                            UUIDDatesPairs.set(UUIDDatesPairs.indexOf(pair), new Pair<>(user, serverDate));
                            return;
                        }
                    }
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
        }

        mQuery.addChildEventListener(childEventListener);

        // This is called after user enters a new shape and tries to send a message for the first time.
        if (uploadNeeded) {

            firebaseUpload();
            uploadNeeded = false;
        } else {

            // If image != null && !imageCompressionProcessComplete, showProgressIndeterminate = true. Else, false.
            showProgressIndeterminate = image != null && !imageCompressionProcessComplete;
        }
    }

    private SpannableString createSpannableMessage(String messageText) {

        ArrayList<String> possibleMentions = new ArrayList<>();
        // Pattern matches UUID for mentions.
        Pattern pattern = Pattern.compile("\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b");
        Matcher matcher = pattern.matcher(messageText);
        while (matcher.find()) {

            // Add the strings that match the UUID pattern to an arrayList. This ensures they are in order from the beginning of the sentence to the end.
            possibleMentions.add(matcher.group());
        }

        // Truncate mentions from Firebase.
        String replacedMessageText = null;
        SpannableString spannableMessageText = null;
        if (!possibleMentions.isEmpty()) {

            ArrayList<String> fullLengthMention = new ArrayList<>();
            ArrayList<Integer> indexOfMention = new ArrayList<>();
            for (String possibleMention : possibleMentions) {

                // The "else" loop will go first - it will create replacedMessageText with truncated mentions and then replacedMessageText will continue to truncate mentions within itself.
                fullLengthMention.add(possibleMention);
                String replacement = possibleMention.substring(0, 10) + "...";
                if (replacedMessageText != null) {

                    replacedMessageText = replacedMessageText.replace(possibleMention, replacement);
                } else {

                    replacedMessageText = messageText.replace(possibleMention, replacement);
                }
                indexOfMention.add(replacedMessageText.indexOf(replacement));
                spannableMessageText = new SpannableString(replacedMessageText);
            }

            // Clear the list to prevent unnecessary memory buildup.
            possibleMentions.clear();

            if (spannableMessageText != null) {

                for (int i = 0; i < fullLengthMention.size(); i++) {

                    int finalI = i;
                    ClickableSpan clickableSpan = new ClickableSpan() {

                        @Override
                        public void onClick(@NonNull View widget) {

                            clickedOnMention = true;

                            // Show a couple messages above the position, as this seems to be better visually.
                            if (mUser.contains(fullLengthMention.get(finalI))) {

                                UUIDToHighlight = fullLengthMention.get(finalI);

                                int scrollPosition = mUser.indexOf(UUIDToHighlight) - 5;
                                if (scrollPosition < 0) {

                                    scrollPosition = 0;
                                }

                                int finalScrollPosition = scrollPosition;

                                if (chatRecyclerView != null) {

                                    if (chatRecyclerView.getAdapter() != null) {

                                        // No need to paginate.
                                        chatRecyclerView.postDelayed(() -> chatRecyclerView.smoothScrollToPosition(

                                                finalScrollPosition), 100);

                                        if (chatRecyclerView.getAdapter() != null) {

                                            if (previouslyHighlightedPosition != null) {

                                                chatRecyclerView.getAdapter().notifyItemChanged(previouslyHighlightedPosition);
                                            }

                                            chatRecyclerView.getAdapter().notifyItemChanged(mUser.indexOf(fullLengthMention.get(finalI)));
                                        }
                                    }
                                }
                            } else {

                                // Pagination needed.
                                UUIDToHighlight = fullLengthMention.get(finalI);
                                loadingOlderMessages = true;
                                getFirebaseMessages(mUser.get(0));
                            }
                        }
                    };

                    spannableMessageText.setSpan(clickableSpan, indexOfMention.get(i) - 1, indexOfMention.get(i) + 13, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        } else {

            spannableMessageText = new SpannableString(messageText);
        }

        return spannableMessageText;
    }

    @Override
    public void onResume() {

        super.onResume();
        Log.i(TAG, "onResume()");

        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        // Start updating location.
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            String provider = LocationManager.NETWORK_PROVIDER;
            if (locationManager != null) {

                locationManager.requestLocationUpdates(provider, Fastest_Interval, 0, this);
                startLocationUpdates();
            } else {

                Log.e(TAG, "onResume() -> locationManager == null");
                showMessageLong("Error retrieving your location.");
            }
        } else {

            checkLocationPermissions();
        }
    }

    @Override
    public void onPause() {

        Log.i(TAG, "onPause()");

        if (locationManager != null) {

            locationManager.removeUpdates(this);
        }

        if (mFusedLocationClient != null) {

            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

        // Read RecyclerView scroll position (for use in initChatAdapter if user reload the activity).
        if (chatRecyclerViewLinearLayoutManager != null && chatRecyclerView != null) {

            index = chatRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
            View v = chatRecyclerView.getChildAt(0);
            top = (v == null) ? 0 : (v.getTop() - chatRecyclerView.getPaddingTop());
        }

        super.onPause();
    }

    @Override
    public void onStop() {

        Log.i(TAG, "onStop()");

        UUIDDatesPairsSize = UUIDDatesPairs.size() - 1;

        if (uploadTask != null) {

            uploadTask.cancel();
        }

        if (mQuery != null) {

            if (childEventListener != null) {

                mQuery.removeEventListener(childEventListener);
            }
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

            // dismiss and null (seemingly) needs to be called to prevent a leak (possibly due to it being a fragment?).
            mediaButtonMenu.dismiss();
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

        if (mediaButton != null) {

            mediaButton = null;
        }

        if (mInput != null) {

            mInput = null;
        }

        if (sendButton != null) {

            sendButton = null;
        }

        if (progressIconIndeterminate != null) {

            progressIconIndeterminate = null;
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

        if (newShapeTextView != null) {

            newShapeTextView = null;
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

        // Prevents crash when user toggles between light / dark mode.
        if (chatRecyclerView == null) {

            return;
        }

        ChatAdapter adapter = new ChatAdapter(mContext, mTime, mUser, mImage, mVideo, mText, mUserIsWithinShape);
        chatRecyclerView.setAdapter(adapter);
        chatRecyclerView.setHasFixedSize(true);
        chatRecyclerView.setLayoutManager(chatRecyclerViewLinearLayoutManager);

        if (UUIDToHighlight != null && !reachedEndOfRecyclerView && (clickedOnMention || fromDms)) {

            // Show a couple messages above the position, as this seems to be better visually.
            int scrollPosition = mUser.indexOf(UUIDToHighlight) - 5;
            if (scrollPosition < 0) {

                scrollPosition = 0;
            }
            chatRecyclerView.scrollToPosition(scrollPosition);
            clickedOnMention = false;
            fromDms = false;
            if (index != null && top != null) {

                // Set RecyclerView scroll position to prevent position change when Firebase gets updated and after screen orientation change.
                chatRecyclerViewLinearLayoutManager.scrollToPositionWithOffset(index, top);
            }
        } else if (onStartJustCalled && UUIDDatesPairsSize == null || messageSent || reachedEndOfRecyclerView) {

            // Scroll to bottom of recyclerviewlayout after first initialization and after sending a recyclerviewlayout.
            chatRecyclerView.scrollToPosition(mUser.size() - 1);
            messageSent = false;
        } else if (index != null && top != null) {

            chatRecyclerViewLinearLayoutManager.scrollToPositionWithOffset(index, top);
        }

        onStartJustCalled = false;
        loadingOlderMessages = false;
        // Need to make UUIDDatesPairsSize null so user can load older messages after restarting the app.
        UUIDDatesPairsSize = null;
        continueWithODC = true;

        // After the initial load, make the progressIconIndeterminate invisible.
        if (!newShape && progressIconIndeterminate != null && !showProgressIndeterminate) {

            progressIconIndeterminate.setVisibility(View.GONE);
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

        private final Context mContext;
        private final ArrayList<String> mMessageTime, mMessageUser, mMessageImage, mMessageImageVideo;
        private final ArrayList<SpannableString> mMessageText;
        private final ArrayList<Boolean> mUserIsWithinShape;

        class ViewHolder extends RecyclerView.ViewHolder {

            final TextView messageTimeInside, messageTimeOutside, messageUserInside, messageUserOutside, messageTextInside, messageTextOutside;
            final ImageView messageImageInside, messageImageOutside, messageImageVideoInside, messageImageVideoOutside;
            final FrameLayout videoFrameInside, videoFrameOutside;

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

                itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {

                    // Get a unique identifier (the user's name) to search for in Firebase.
                    reportedUser = mMessageUser.get(getAdapterPosition());
                    menu.add(0, R.string.report_post, 0, R.string.report_post);
                });

                // Expand userUUID.
                if (messageUserInside != null) {

                    messageUserInside.setOnClickListener(v -> {

                        ViewGroup.LayoutParams params = messageUserInside.getLayoutParams();
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                        messageUserInside.setLayoutParams(params);
                    });
                }

                // Expand userUUID.
                if (messageUserOutside != null) {

                    messageUserOutside.setOnClickListener(v -> {

                        ViewGroup.LayoutParams params = messageUserOutside.getLayoutParams();
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                        messageUserOutside.setLayoutParams(params);
                    });
                }

                if (messageImageInside != null) {

                    messageImageInside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, PhotoView.class);
                        Activity.putExtra("imgUrl", mMessageImage.get(getAdapterPosition()));
                        startActivityForResult(Activity, Media_Code);
                    });
                }

                if (messageImageOutside != null) {

                    messageImageOutside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, PhotoView.class);
                        Activity.putExtra("imgUrl", mMessageImage.get(getAdapterPosition()));
                        startActivityForResult(Activity, Media_Code);
                    });
                }

                if (messageImageVideoInside != null) {

                    messageImageVideoInside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoUrl", mMessageImageVideo.get(getAdapterPosition()));
                        startActivityForResult(Activity, Media_Code);
                    });
                }

                if (messageImageVideoOutside != null) {

                    messageImageVideoOutside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoUrl", mMessageImageVideo.get(getAdapterPosition()));
                        startActivityForResult(Activity, Media_Code);
                    });
                }

                if (playButtonInside != null) {

                    playButtonInside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoUrl", mMessageImageVideo.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
                    });
                }

                if (playButtonOutside != null) {

                    playButtonOutside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoUrl", mMessageImageVideo.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
                    });
                }
            }
        }

        ChatAdapter(Context context, ArrayList<String> mMessageTime, ArrayList<String> mMessageUser, ArrayList<String> mMessageImage, ArrayList<String> mMessageImageVideo, ArrayList<SpannableString> mMessageText, ArrayList<Boolean> mUserIsWithinShape) {

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

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

            // Set the left side if the user sent the message from inside the shape.
            if (mUserIsWithinShape.get(position)) {

                // Prevent overlapping while paginating.
                holder.messageTimeOutside.setVisibility(View.GONE);
                holder.messageUserOutside.setVisibility(View.GONE);
                holder.messageImageOutside.setVisibility(View.GONE);
                holder.messageImageVideoOutside.setVisibility(View.GONE);
                holder.messageTextOutside.setVisibility(View.GONE);
                holder.messageTimeInside.setVisibility(View.VISIBLE);
                holder.messageUserInside.setVisibility(View.VISIBLE);

                holder.messageTimeInside.setText(mMessageTime.get(position));

                holder.messageUserInside.setText(getString(R.string.atUsername, mMessageUser.get(position)));

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
                    if (!theme) {
                        holder.messageTextInside.setLinkTextColor(Color.GREEN);
                    } else {
                        holder.messageTextInside.setLinkTextColor(Color.BLUE);
                    }
                    holder.messageTextInside.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.messageTextInside.setVisibility(View.VISIBLE);
                }
            } else {

                // Prevent overlapping while paginating.
                holder.messageTimeInside.setVisibility(View.GONE);
                holder.messageUserInside.setVisibility(View.GONE);
                holder.messageImageInside.setVisibility(View.GONE);
                holder.messageImageVideoInside.setVisibility(View.GONE);
                holder.messageTextInside.setVisibility(View.GONE);
                holder.messageTimeOutside.setVisibility(View.VISIBLE);
                holder.messageUserOutside.setVisibility(View.VISIBLE);

                // User sent the message from outside the shape. Setup the right side.
                holder.messageTimeOutside.setText(mMessageTime.get(position));

                holder.messageUserOutside.setText(getString(R.string.atUsername, mMessageUser.get(position)));

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
                    if (!theme) {
                        holder.messageTextOutside.setLinkTextColor(Color.GREEN);
                    } else {
                        holder.messageTextOutside.setLinkTextColor(Color.BLUE);
                    }
                    holder.messageTextOutside.setMovementMethod(LinkMovementMethod.getInstance());
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

            // "Highlight" the message.
            if (UUIDToHighlight != null) {

                if (mUser.get(position).equals(UUIDToHighlight)) {

                    if (theme) {

                        holder.itemView.setBackgroundColor(Color.parseColor("#859FFF"));
                    } else {

                        holder.itemView.setBackgroundColor(Color.parseColor("#1338BE"));
                    }

                    previouslyHighlightedPosition = mUser.indexOf(UUIDToHighlight);
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
    }

    // Brings up suggestions when a users inputs "@".
    @NonNull
    @Override
    public List<String> onQueryReceived(@NonNull QueryToken queryToken) {

        List<String> buckets = Collections.singletonList(BUCKET);
        List<String> suggestions = getSuggestions(queryToken);

        if (suggestions != null) {

            initMentionsAdapter(suggestions);
        }

        return buckets;
    }

    private List<String> getSuggestions(QueryToken queryToken) {

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

        private final Context mContext;
        private final List<String> mSuggestions;

        class ViewHolder extends RecyclerView.ViewHolder {

            final TextView suggestion;

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

                        return "@" + mSuggestions.get(position);
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

                // Don't add the mentionable if it already exists.
                if (mInput.getText().toString().trim().contains(mSuggestions.get(position))) {

                    showMessageShort("User has already been mentioned.");
                    return;
                }

                mInput.insertMention(mentionable);

                // Add a space after inserting mention.
                mInput.append(" ");
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
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {

        if (item.getItemId() == R.string.report_post) {

            progressIconIndeterminate.setVisibility(View.VISIBLE);

            DatabaseReference firebaseMessages = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(shapeUUID);
            Query firebaseMessagesQuery = firebaseMessages.orderByChild("userUUID").equalTo(reportedUser);
            firebaseMessagesQuery.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    for (DataSnapshot ds : snapshot.getChildren()) {

                        String pushID = ds.getKey();
                        ReportPostInformation reportPostInformation = new ReportPostInformation();
                        reportPostInformation.setLat(shapeLatInt);
                        reportPostInformation.setLon(shapeLonInt);
                        reportPostInformation.setPushID(pushID);
                        reportPostInformation.setShapeUUID(shapeUUID);
                        DatabaseReference newReportedPost = FirebaseDatabase.getInstance().getReference().child("ReportedPost").push();
                        newReportedPost.setValue(reportPostInformation);
                        progressIconIndeterminate.setVisibility(View.GONE);
                        showMessageShort("Post reported. Thank you!");
                        // "return" is not strictly necessary (as there should only be one child), but it keeps the data usage and processing to a minimum in the event of strange behavior.
                        return;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    progressIconIndeterminate.setVisibility(View.GONE);
                    showMessageLong(error.getMessage());
                }
            });
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        int id = menuItem.getItemId();

        if (id == R.id.takePhoto) {

            Log.i(TAG, "onMenuItemClick() -> takePhoto");

            if (checkPermissionsPicture()) {

                cancelToasts();

                startActivityTakePhoto();
            }

        } else if (id == R.id.recordVideo) {

            Log.i(TAG, "onMenuItemClick() -> recordVideo");

            if (checkPermissionsVideo()) {

                cancelToasts();

                startActivityRecordVideo();
            }
        }

        return false;
    }

    private boolean checkPermissionsPicture() {

        Log.i(TAG, "checkPermissionsPicture()");

        // boolean to loop back to this point and start activity after xPermissionAlertDialog.
        checkPermissionsPicture = true;

        int permissionCamera = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
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

        int permissionCamera = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
        int permissionRecordAudio = ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.i(TAG, "onRequestPermissionsResult()");

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case Request_User_Location_Code: {

                if (grantResults.length > 0) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {

                        locationPermissionAlertAsync();
                    } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        // Permission was granted, yay! Do the location-related task you need to do.
                        if (ContextCompat.checkSelfPermission(mContext,
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

                        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.CAMERA)) {

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

                        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.CAMERA)) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Camera permissions were not granted. Ask again.");

                            cameraPermissionAlertAsync(checkPermissionsPicture);
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.RECORD_AUDIO)) {

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
        SettingsClient settingsClient = LocationServices.getSettingsClient(mContext);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // New Google API SDK v11 uses getFusedLocationProviderClient(this).
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mFusedLocationClient = getFusedLocationProviderClient(mContext);

            mLocationCallback = new LocationCallback() {

                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {

                    onLocationChanged(locationResult.getLastLocation());
                }
            };

            getFusedLocationProviderClient(mContext).requestLocationUpdates(locationRequest, mLocationCallback, Objects.requireNonNull(Looper.myLooper()));
        } else {

            checkLocationPermissions();
        }
    }

    private void checkLocationPermissions() {

        Log.i(TAG, "checkLocationPermissions()");

        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Request_User_Location_Code);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

        if (!newShape) {

            float[] newDistance = new float[2];
            Location.distanceBetween(shapeLat, shapeLon, location.getLatitude(), location.getLongitude(), newDistance);

            // Only update these parameters if the location accuracy is reasonable.
            if (location.getAccuracy() < 10) {

                if (newDistance[0] < 5) {

                    if (userIsWithinShape == null) {

                        mInput.setHint(R.string.message_from_within_circle);
                        positionRelativeToShapeTextView.setText(R.string.inside_circle);
                        userIsWithinShape = true;
                    } else if (!userIsWithinShape) {

                        showMessageShort("You are now inside the circle.");
                        mInput.setHint(R.string.message_from_within_circle);
                        positionRelativeToShapeTextView.setText(R.string.inside_circle);
                        userIsWithinShape = true;
                    }
                } else {

                    if (userIsWithinShape == null) {

                        mInput.setHint(R.string.message_from_outside_circle);
                        userIsWithinShape = false;
                    } else if (userIsWithinShape) {

                        showMessageShort("You are now outside the circle.");
                        mInput.setHint(R.string.message_from_outside_circle);
                        userIsWithinShape = false;
                    }

                    // Update the user's location for reference.
                    int distanceOutsideCircle = (int) newDistance[0] - 5;
                    if (distanceOutsideCircle == 0) {

                        distanceOutsideCircle = 1;
                    }
                    positionRelativeToShapeTextView.setText(getString(R.string.distance_outside_circle, distanceOutsideCircle));
                }
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
        alert = new AlertDialog.Builder(mContext);

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

    private void locationPermissionAlertAsync() {

        AlertDialog.Builder alert;
        alert = new AlertDialog.Builder(mContext);

        HandlerThread locationPermissionHandlerThread = new HandlerThread("locationPermissionHandlerThread");
        locationPermissionHandlerThread.start();
        Handler mHandler = new Handler(locationPermissionHandlerThread.getLooper());
        Runnable runnable = () ->

                alert.setCancelable(false)
                        .setTitle(R.string.location_permission_required)
                        .setMessage(R.string.location_permission_required_explanation)
                        .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(mActivity,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                Request_User_Location_Code))
                        .create()
                        .show();

        mHandler.post(runnable);
    }

    private void cameraPermissionAlertAsync(Boolean checkPermissionsPicture) {

        AlertDialog.Builder alert;
        alert = new AlertDialog.Builder(mContext);

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
        alert = new AlertDialog.Builder(mContext);

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

    private void startActivityTakePhoto() {

        Log.i(TAG, "startActivityTakePhoto()");

        // Permission was granted, yay! Do the task you need to do.
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(mActivity.getPackageManager()) != null) {

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
                showMessageLong(ex.getMessage());
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

    private File createImageFile() throws IOException {

        Log.i(TAG, "createImageFile()");

        // Create an image file name
        String fileName = "HereBefore_" + System.currentTimeMillis();
        File storageDir = mActivity.getCacheDir();
        image = File.createTempFile(
                fileName,  /* prefix */
                ".jpeg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    private File createVideoFile() throws IOException {

        Log.i(TAG, "createVideoFile()");

        // Create a video file name
        String fileName = "HereBefore_" + System.currentTimeMillis();
        File storageDir = mActivity.getCacheDir();
        video = File.createTempFile(
                fileName,  /* prefix */
                ".mp4",         /* suffix */
                storageDir      /* directory */
        );

        return video;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {

            // If user is clicking on multiple images, show an ad to pay for the bandwidth.
            // Backing out of PhotoView and VideoView causes resultCode != RESULT_OK, so this code must be stored here.
            // The "return" needs to stay to prevent the rest of the code from executing if the user backs out of taking a picture / video.
            if (requestCode == Media_Code) {

                Log.i(TAG, "onActivityResult() -> Returned from viewing media");

                fromMediaCounter++;
                if (fromMediaCounter == 3) {

                    fromMediaCounter = 0;
                    ((Navigation) requireActivity()).showInterstitialAd();
                }
            }

            return;
        }

        if (requestCode == 3) {

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

            // Prevents the progressIconIndeterminate from being removed by initChatAdapter.
            showProgressIndeterminate = true;

            fileIsImage = true;

            // Change textView to be to the right of imageView.
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mInput.getLayoutParams();
            params.addRule(RelativeLayout.END_OF, R.id.imageView);
            mInput.setLayoutParams(params);

            imageCompressAndAddToGalleryAsync();
        }

        if (requestCode == 4) {

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

            fileIsImage = false;

            videoCompressAndAddToGalleryAsync();
        }
    }

    // Save a non-compressed version to the gallery and add a compressed version to the imageView.
    private void imageCompressAndAddToGalleryAsync() {

        Log.i(TAG, "imageCompressAndAddToGalleryAsync()");

        imageCompressionProcessComplete = false;
        progressIconIndeterminate.setVisibility(View.VISIBLE);

        // Disable the sendButton while content is being compressed.
        sendButton.setEnabled(false);

        HandlerThread imageCompressAndAddToGalleryHandlerThread = new HandlerThread("imageCompressAndAddToGalleryHandlerThread");
        imageCompressAndAddToGalleryHandlerThread.start();
        Handler handler = new Handler(imageCompressAndAddToGalleryHandlerThread.getLooper());
        Runnable runnable = () -> {

            // Save a non-compressed image to the gallery.
            try {

                // If fileName is not null, file was created in Map and needs to be found here.
                if (imageFile != null && mContext != null) {

                    image = new File(imageFile);
                    imageURI = FileProvider.getUriForFile(mContext,
                            "com.example.android.fileprovider",
                            image);
                }

                // If user changes theme then re-enters Chat with a picture, this runnable will cause the app to crash because mContext is null. While (mContext != null) does not work to prevent the crash, so these constant mContext checks will be a patch until a more elegant solution is created.
                if (mContext == null) {

                    return;
                }

                Bitmap imageBitmapFull = new Compressor(mContext)
                        .setMaxWidth(10000)
                        .setMaxHeight(10000)
                        .setQuality(100)
                        .setCompressFormat(Bitmap.CompressFormat.PNG)
                        .setDestinationDirectoryPath(Objects.requireNonNull(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)).getAbsolutePath())
                        .compressToBitmap(image);

                if (mContext == null) {

                    return;
                }

                MediaStore.Images.Media.insertImage(mContext.getContentResolver(), imageBitmapFull, "HereBefore_" + System.currentTimeMillis() + "_PNG", null);

                if (mContext == null) {

                    return;
                }

                // Create a compressed image.
                Bitmap mImageBitmap = new Compressor(mContext)
                        .setMaxWidth(480)
                        .setMaxHeight(640)
                        .setQuality(25)
                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                        .compressToBitmap(image);

                // Convert the bitmap to a byteArray for use in uploadImage().
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(mImageBitmap.getWidth() * mImageBitmap.getHeight());
                mImageBitmap.compress(Bitmap.CompressFormat.JPEG, 25, buffer);
                byteArray = buffer.toByteArray();

                // Prevent re-compression on restart.
                imageFile = null;
                // Make the value in Map null so it does not have a value when backing into Map and entering a circle.
                Map.imageFile = null;

                // Prevent further processing if file (in KB) is too big.
                if (((float) byteArray.length / 1024) > 1000) {

                    new Handler(Looper.getMainLooper()).post(() -> {

                        showMessageLong("Error during image compression. Uncompressed image saved to gallery.");

                        imageCompressionProcessComplete = true;
                        progressIconIndeterminate.setVisibility(View.GONE);
                        if (newShape) {

                            newShapeTextView.setVisibility(View.VISIBLE);
                        }
                        sendButton.setEnabled(true);
                        // Allow initChatAdapter to get rid of the progressIconIndeterminate with this boolean.
                        showProgressIndeterminate = false;
                    });

                    return;
                }

                // Update UI thread.
                new Handler(Looper.getMainLooper()).post(() -> {

                    if (mContext == null) {

                        return;
                    }

                    Glide.with(mContext)
                            .load(byteArray)
                            .apply(new RequestOptions().override(480, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                            .into(imageView);

                    imageCompressionProcessComplete = true;
                    progressIconIndeterminate.setVisibility(View.GONE);
                    if (newShape) {

                        newShapeTextView.setVisibility(View.VISIBLE);
                    }
                    imageView.setVisibility(View.VISIBLE);
                    sendButton.setEnabled(true);
                    // Allow initChatAdapter to get rid of the progressIconIndeterminate with this boolean.
                    showProgressIndeterminate = false;
                });
            } catch (IOException ex) {

                showMessageLong(ex.getMessage());
            }
        };

        handler.post(runnable);
    }

    // Save a non-compressed version to the gallery and add a compressed version to the imageView.
    private void videoCompressAndAddToGalleryAsync() {

        Log.i(TAG, "videoCompressAndAddToGalleryAsync()");

        videoCompressionProcessComplete = false;
        progressIcon.setProgress(0);
        progressIcon.setVisibility(View.VISIBLE);

        // Disable the sendButton while content is being compressed.
        sendButton.setEnabled(false);

        HandlerThread videoCompressAndAddToGalleryHandlerThread = new HandlerThread("videoCompressAndAddToGalleryHandlerThread");
        videoCompressAndAddToGalleryHandlerThread.start();
        Handler handler = new Handler(videoCompressAndAddToGalleryHandlerThread.getLooper());
        Runnable runnable = () -> {

            // If fileName is not null, file was created in Map and needs to be found here.
            if (videoFile != null && mContext != null) {

                video = new File(videoFile);
                videoURI = FileProvider.getUriForFile(mContext,
                        "com.example.android.fileprovider",
                        video);
            }

            File videoTemp = null;
            String filePathTemp = "HereBefore_" + System.currentTimeMillis() + "_mp4";
            File storageDir = mActivity.getCacheDir();
            try {
                videoTemp = File.createTempFile(
                        filePathTemp,   /* prefix */
                        ".mp4",  /* suffix */
                        storageDir     /* directory */
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (videoTemp == null) {

                return;
            }

            String filePath = videoTemp.getAbsolutePath();

            DefaultVideoStrategy mTranscodeVideoStrategy = new DefaultVideoStrategy.Builder()
                    .addResizer(new AspectRatioResizer(16F / 9F))
                    .addResizer(new FractionResizer(1F / 4F))
                    .frameRate(24)
                    .build();

            Transcoder.into(filePath)
                    .addDataSource(video.getAbsolutePath())
                    .setVideoTrackStrategy(mTranscodeVideoStrategy)
                    .setListener(new TranscoderListener() {

                        public void onTranscodeProgress(double progress) {

                            int mProgress = (int) (progress * 100);

                            progressIcon.setProgress(mProgress, true);
                        }

                        public void onTranscodeCompleted(int successCode) {

                            videoCompressionProcessComplete = true;

                            // Prevent re-compression on restart.
                            videoFile = null;
                            // Make the value in Map null so it does not have a value when backing into Map and entering a circle.
                            Map.videoFile = null;

                            progressIcon.setProgress(0);

                            if (successCode == Transcoder.SUCCESS_TRANSCODED) {

                                Log.i(TAG, "Transcoder.SUCCESS_TRANSCODED");
                                onTranscodeFinished(filePath);
                            } else if (successCode == Transcoder.SUCCESS_NOT_NEEDED) {

                                Log.i(TAG, "Transcoder.SUCCESS_NOT_NEEDED");
                                onTranscodeFinished(video.getAbsolutePath());
                            }
                        }

                        public void onTranscodeCanceled() {

                            videoCompressionProcessComplete = true;

                            // Prevent re-compression on restart.
                            videoFile = null;
                            // Make the value in Map null so it does not have a value when backing into Map and entering a circle.
                            Map.videoFile = null;

                            sendButton.setEnabled(true);

                            progressIcon.setProgress(0);
                        }

                        public void onTranscodeFailed(@NonNull Throwable exception) {

                            videoCompressionProcessComplete = true;

                            // Prevent re-compression on restart.
                            videoFile = null;
                            // Make the value in Map null so it does not have a value when backing into Map and entering a circle.
                            Map.videoFile = null;

                            sendButton.setEnabled(true);

                            progressIcon.setProgress(0);

                            Log.e(TAG, "Transcoder error occurred: " + exception.getMessage());
                            showMessageLong("Error compressing video: " + exception.getMessage());
                        }
                    }).transcode();

            // Add uncompressed video to gallery.
            // Save the name and description of a video in a ContentValues map.
            ContentValues values = new ContentValues(3);
            values.put(MediaStore.Video.Media.TITLE, "Here_Before_" + System.currentTimeMillis());
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, video.getAbsolutePath());

            // Add a new record (identified by uri) without the video, but with the values just set.
            Uri uri = mContext.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

            // Now get a handle to the file for that record, and save the data into it.
            try {

                InputStream is = new FileInputStream(video);
                if (uri != null) {

                    OutputStream os = mContext.getContentResolver().openOutputStream(uri);
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
        };

        handler.post(runnable);
    }

    private void onTranscodeFinished(String absolutePath) {

        Log.i(TAG, "onTranscodeFinished()");

        Bitmap mBitmap = null;
        int mBitmapWidth = 0;
        int mBitmapHeight = 0;

        // Turn the compressed video into a bitmap.
        File videoFile = new File(absolutePath);

        // Prevent further processing if file size (in MB) is too big.
        if (((float) videoFile.length() / (float) (1024 * 1024)) > 10) {

            new Handler(Looper.getMainLooper()).post(() -> {

                // Prevents a crash if the user backed out of activity while video was compressing.
                if (mActivity != null) {

                    showMessageLong("Error during video compression. Uncompressed video saved to gallery.");

                    sendButton.setEnabled(true);
                    progressIcon.setVisibility(View.GONE);
                    if (newShape) {

                        newShapeTextView.setVisibility(View.VISIBLE);
                    }
                }
            });

            return;
        }

        try {

            File file = new File(absolutePath);
            InputStream inputStream;
            inputStream = new FileInputStream(file.getAbsolutePath());
            byte[] buffer = new byte[8192];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);
            videoURI = Uri.fromFile(videoFile);
            try {

                while ((bytesRead = inputStream.read(buffer)) != -1) {

                    output64.write(buffer, 0, bytesRead);
                }
            } catch (IOException ex) {

                showMessageLong(ex.getMessage());
            }
            output64.close();

            // Change the videoImageView's orientation depending on the orientation of the video.
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            // Set the video Uri as data source for MediaMetadataRetriever
            retriever.setDataSource(absolutePath);
            // Get one "frame"/bitmap - * NOTE - no time was set, so the first available frame will be used
            mBitmap = retriever.getFrameAtTime(1);
            if (mBitmap == null) {
                return;
            }
            // Get the bitmap width and height
            mBitmapWidth = mBitmap.getWidth();
            mBitmapHeight = mBitmap.getHeight();
        } catch (IOException ex) {

            showMessageLong(ex.getMessage());
        }

        // Update UI thread.
        Bitmap finalBitmap = mBitmap;
        int finalBitmapHeight = mBitmapHeight;
        int finalBitmapWidth = mBitmapWidth;

        new Handler(Looper.getMainLooper()).post(() -> {

            // Prevents a crash if the user backed out of activity while video was compressing.
            if (mActivity != null) {

                if (finalBitmap != null && finalBitmapHeight != 0 && finalBitmapWidth != 0) {

                    // Change textView to be to the right of videoImageView.
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mInput.getLayoutParams();
                    params.addRule(RelativeLayout.END_OF, R.id.videoImageView);
                    mInput.setLayoutParams(params);

                    final float scale = getResources().getDisplayMetrics().density;
                    if (finalBitmapWidth > finalBitmapHeight) {

                        videoImageView.getLayoutParams().width = (int) (50 * scale + 0.5f); // Convert 50dp to px.
                    } else {

                        videoImageView.getLayoutParams().width = (int) (30 * scale + 0.5f); // Convert 30dp to px.
                    }

                    videoImageView.setImageBitmap(finalBitmap);
                    videoImageView.setVisibility(View.VISIBLE);
                }

                sendButton.setEnabled(true);
                progressIcon.setVisibility(View.GONE);
                if (newShape) {

                    newShapeTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void firebaseUpload() {

        Log.i(TAG, "firebaseUpload()");

        showInterstitialAdCounterMedia++;

        // If user is uploading multiple media to Firebase, show an ad to pay for the bandwidth.
        if (showInterstitialAdCounterMedia == 3) {

            showInterstitialAdCounterMedia = 0;
            showInterstitialAdCounterText = 0;
            ((Navigation) requireActivity()).showInterstitialAd();
        }

        progressIcon.setProgress(0);

        // Get the input early so we can clear it before the content begins uploading.
        String input = mInput.getText().toString().trim();

        // For some reason, if the text begins with a mention and onCreateView was called after the mention was added, the mention is not cleared with one call to clear().
        mInput.getText().clear();
        mInput.getText().clear();
        imageView.setVisibility(View.GONE);
        imageView.setImageDrawable(null);
        videoImageView.setVisibility(View.GONE);
        videoImageView.setImageDrawable(null);

        if (!fileIsImage) {

            // Video.
            final StorageReference storageReferenceVideo = FirebaseStorage.getInstance().getReference("Videos").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(String.valueOf(System.currentTimeMillis()));
            storageReferenceVideo.putFile(videoURI);

            uploadTask = storageReferenceVideo.putFile(videoURI).addOnSuccessListener(taskSnapshot -> storageReferenceVideo.getDownloadUrl()

                    .addOnSuccessListener(uri -> {

                        // Prevents a crash if user changed activity, as this is an asynchronous task.
                        if (mActivity == null || mContext == null) {

                            return;
                        }

                        Log.i(TAG, "firebaseUpload() -> onSuccess");

                        // Change boolean to true - scrolls to the bottom of the recyclerView (in initChatAdapter).
                        messageSent = true;

                        if (newShape) {

                            // Since the UUID doesn't already exist in Firebase, add the circle.
                            CircleOptions circleOptions = new CircleOptions()
                                    .center(new LatLng(shapeLat, shapeLon))
                                    .clickable(true)
                                    .radius(1.0);
                            CircleInformation circleInformation = new CircleInformation();
                            circleInformation.setCircleOptions(circleOptions);
                            circleInformation.setShapeUUID(shapeUUID);
                            DatabaseReference newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child("Points").push();
                            newFirebaseShape.setValue(circleInformation);

                            newShape = false;
                        }

                        MessageInformation messageInformation = new MessageInformation();
                        Object date = ServerValue.TIMESTAMP;
                        messageInformation.setDate(date);
                        String firebaseUid = FirebaseAuth.getInstance().getUid();
                        messageInformation.setFirebaseUid(firebaseUid);
                        if (input.length() != 0) {

                            messageInformation.setMessage(input);
                        }
                        messageInformation.setUserIsWithinShape(userIsWithinShape);
                        String userUUID = UUID.randomUUID().toString();
                        messageInformation.setUserUUID(userUUID);
                        messageInformation.setVideoUrl(uri.toString());
                        DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(shapeUUID).push();
                        newMessage.setValue(messageInformation);

                        if (video != null) {

                            deleteDirectory(video);
                        }

                        videoFile = null;
                        sendButton.setEnabled(true);
                        progressIcon.setVisibility(View.GONE);
                        progressIconIndeterminate.setVisibility(View.VISIBLE);
                        showProgressIndeterminate = false;
                    }))

                    .addOnProgressListener(snapshot -> {

                        if (snapshot.getBytesTransferred() != 0) {

                            if (progressIcon.getVisibility() != View.VISIBLE) {

                                progressIconIndeterminate.setVisibility(View.GONE);
                                progressIcon.setVisibility(View.VISIBLE);
                            }

                            int progress = (int) ((int) (snapshot.getBytesTransferred() * 100) / snapshot.getTotalByteCount());

                            progressIcon.setProgress(progress, true);
                        }
                    })

                    .addOnFailureListener(ex -> {

                        sendButton.setEnabled(true);
                        progressIcon.setProgress(0);
                        progressIcon.setVisibility(View.GONE);
                        progressIconIndeterminate.setVisibility(View.GONE);
                        showMessageLong(ex.getMessage());
                        Log.e(TAG, "firebaseUpload() -> !fileIsImage -> onFailure -> " + ex.getMessage());
                    });
        } else {

            // byteArray and image.
            final StorageReference storageReferenceImage = FirebaseStorage.getInstance().getReference("Images").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(String.valueOf(System.currentTimeMillis()));
            storageReferenceImage.putBytes(byteArray);

            uploadTask = storageReferenceImage.putBytes(byteArray).addOnSuccessListener(taskSnapshot -> storageReferenceImage.getDownloadUrl()

                    .addOnSuccessListener(uri -> {

                        // Prevents a crash if user changed activity, as this is an asynchronous task.
                        if (mActivity == null || mContext == null) {

                            return;
                        }

                        Log.i(TAG, "uploadImage() -> onSuccess");

                        // Change boolean to true - scrolls to the bottom of the recyclerView (in initChatAdapter).
                        messageSent = true;

                        if (newShape) {

                            // Since the UUID doesn't already exist in Firebase, add the circle.
                            CircleOptions circleOptions = new CircleOptions()
                                    .center(new LatLng(shapeLat, shapeLon))
                                    .clickable(true)
                                    .radius(1.0);
                            CircleInformation circleInformation = new CircleInformation();
                            circleInformation.setCircleOptions(circleOptions);
                            circleInformation.setShapeUUID(shapeUUID);
                            DatabaseReference newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child("Points").push();
                            newFirebaseShape.setValue(circleInformation);

                            newShape = false;
                        }

                        MessageInformation messageInformation = new MessageInformation();
                        Object date = ServerValue.TIMESTAMP;
                        messageInformation.setDate(date);
                        String firebaseUid = FirebaseAuth.getInstance().getUid();
                        messageInformation.setFirebaseUid(firebaseUid);
                        messageInformation.setImageUrl(uri.toString());
                        if (input.length() != 0) {

                            messageInformation.setMessage(input);
                        }
                        messageInformation.setUserIsWithinShape(userIsWithinShape);
                        String userUUID = UUID.randomUUID().toString();
                        messageInformation.setUserUUID(userUUID);
                        DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child(shapeUUID).push();
                        newMessage.setValue(messageInformation);

                        if (image != null) {

                            deleteDirectory(image);
                        }

                        imageFile = null;
                        sendButton.setEnabled(true);
                        progressIcon.setVisibility(View.GONE);
                        progressIconIndeterminate.setVisibility(View.VISIBLE);
                        showProgressIndeterminate = false;
                    }))

                    .addOnProgressListener(snapshot -> {

                        if (snapshot.getBytesTransferred() != 0) {

                            if (progressIcon.getVisibility() != View.VISIBLE) {

                                progressIconIndeterminate.setVisibility(View.GONE);
                                progressIcon.setVisibility(View.VISIBLE);
                            }

                            int progress = (int) ((int) (snapshot.getBytesTransferred() * 100) / snapshot.getTotalByteCount());

                            progressIcon.setProgress(progress, true);
                        }
                    })

                    .addOnFailureListener(ex -> {

                        sendButton.setEnabled(true);
                        progressIcon.setProgress(0);
                        progressIcon.setVisibility(View.GONE);
                        progressIconIndeterminate.setVisibility(View.GONE);
                        showMessageLong(ex.getMessage());
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

    private void showMessageShort(String message) {

        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

            if (rootView != null) {

                snackBar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
                snackBar.setAnchorView(sendButton);
                View snackBarView = snackBar.getView();
                TextView snackTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
                snackTextView.setMaxLines(10);
                snackBar.show();
            }
        } else {

            // Prevents a crash if the user backed out of activity and a toast message occurs from another thread.
            if (mActivity != null) {

                cancelToasts();
                shortToast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT);
                shortToast.setGravity(Gravity.CENTER, 0, 0);
                shortToast.show();
            }
        }
    }

    private void showMessageLong(String message) {

        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

            if (rootView != null) {

                snackBar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
                snackBar.setAnchorView(sendButton);
                View snackBarView = snackBar.getView();
                TextView snackTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
                snackTextView.setMaxLines(10);
                snackBar.show();
            }
        } else {

            // Prevents a crash if the user backed out of activity and a toast message occurs from another thread.
            if (mActivity != null) {

                cancelToasts();
                longToast = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
                longToast.setGravity(Gravity.CENTER, 0, 0);
                longToast.show();
            }
        }
    }

    private void cancelToasts() {

        if (shortToast != null) {

            shortToast.cancel();
        }

        if (longToast != null) {

            longToast.cancel();
        }
    }
}