package co.clixel.herebefore;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategies;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private ArrayList<String> removedDuplicatesMentions, mTime, mUser, mImage, mVideo, mSuggestions, allMentions, emailsAL;
    private ArrayList<SpannableString> mText;
    private ArrayList<Boolean> mUserIsWithinShape;
    private ArrayList<Long> datesAL;
    private RecyclerView chatRecyclerView, mentionsRecyclerView;
    private Integer index, top, last, datesALSize;
    private ChildEventListener childEventListener;
    private FloatingActionButton sendButton, mediaButton;
    private boolean firstLoad, loadingOlderMessages = false, clickedOnMention = false, fromDms = false, noMoreMessages = false, fromVideo, needProgressIconIndeterminate, reachedEndOfRecyclerView = false, messageSent = false, fileIsImage, checkPermissionsPicture, newShape;
    private Boolean userIsWithinShape;
    private View.OnLayoutChangeListener onLayoutChangeListener;
    private String shapeUUID, reportedUser, UUIDToHighlight;
    private double circleLatitude, circleLongitude;
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
    public View rootView;
    private Context mContext;
    private Activity mActivity;
    private AdView bannerAd;
    private Query mQuery;
    private Drawable imageDrawable, videoDrawable;
    private int latFirebaseValue, lonFirebaseValue;
    private Integer previouslyHighlightedPosition;
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

        // Get info from Map.java
        if (mActivity != null) {

            Bundle extras = mActivity.getIntent().getExtras();
            if (extras != null) {

                UUIDToHighlight = extras.getString("UUIDToHighlight");
                fromDms = extras.getBoolean("fromDms");
                newShape = extras.getBoolean("newShape");
                latFirebaseValue = extras.getInt("shapeLat");
                lonFirebaseValue = extras.getInt("shapeLon");
                shapeUUID = extras.getString("shapeUUID");
                userIsWithinShape = extras.getBoolean("userIsWithinShape");
                // circleLatitude, circleLongitude, and radius will be null if the circle is not new (as a new circle is not being created).
                circleLatitude = extras.getDouble("circleLatitude");
                circleLongitude = extras.getDouble("circleLongitude");
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
        progressIconIndeterminate = rootView.findViewById(R.id.progressIconIndeterminate);
        progressIcon = rootView.findViewById(R.id.progressIcon);

        chatRecyclerViewLinearLayoutManager = new LinearLayoutManager(mActivity);
        mentionsRecyclerViewLinearLayoutManager = new LinearLayoutManager(mActivity);

        if (datesALSize == null) {

            datesAL = new ArrayList<>();
            emailsAL = new ArrayList<>();

            mTime = new ArrayList<>();
            mUser = new ArrayList<>();
            mImage = new ArrayList<>();
            mVideo = new ArrayList<>();
            mText = new ArrayList<>();
            mSuggestions = new ArrayList<>();
            allMentions = new ArrayList<>();
            mUserIsWithinShape = new ArrayList<>();
        }

        // Prevents clearing this list if user adds a DM and takes a picture.
        if (removedDuplicatesMentions == null) {
            removedDuplicatesMentions = new ArrayList<>();
        }

        // Make the progressIconIndeterminate visible upon the first load, as it can sometimes take a while to show anything. It should be made invisible in initChatAdapter.
        if (progressIconIndeterminate != null && !fromVideo) {

            progressIconIndeterminate.setVisibility(View.VISIBLE);
        }

        fromVideo = false;

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

        // If the value isn't null, check if the latest date is the same as the one in the recyclerView. If the value is null, it's the first time loading.
        if (datesALSize != null) {

            Query query = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID).limitToLast(1);
            query.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    // Edge case where user reloading into a chat that no longer exists.
                    if (snapshot.getChildrenCount() == 0 && !newShape) {

                        progressIconIndeterminate.setVisibility(View.GONE);
                        mInput.setFocusable(false);
                        toastMessageLong("Shape was deleted. Please return to map.");
                    }

                    for (DataSnapshot ds : snapshot.getChildren()) {

                        Long date = (Long) ds.child("date").getValue();
                        // If the saved date and the latest date match, then there's no need to re-download everything from Firebase.
                        if (date != null && datesAL != null && !date.equals(datesAL.get(datesAL.size() - 1))) {

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
                }
            });
        } else {

            getFirebaseMessages(null);
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
            chatRecyclerView.addOnLayoutChangeListener(onLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {

                if (reachedEndOfRecyclerView) {

                    if (bottom < oldBottom) {

                        if (chatRecyclerView.getAdapter() != null && chatRecyclerView.getAdapter().getItemCount() > 0) {

                            chatRecyclerView.postDelayed(() -> chatRecyclerView.smoothScrollToPosition(

                                    chatRecyclerView.getAdapter().getItemCount() - 1), 100);
                        }
                    }
                }
            });
        }

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

            mediaButtonMenu = new PopupMenu(mActivity, mediaButton);
            mediaButtonMenu.setOnMenuItemClickListener(Chat.this);
            mediaButtonMenu.inflate(R.menu.mediabutton_menu);
            mediaButtonMenu.show();
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

            String input = mInput.getText().toString().trim();

            // Send recyclerviewlayout to Firebase.
            if (!input.equals("") || imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                sendButton.setEnabled(false);

                if (imageView.getVisibility() != View.GONE || videoImageView.getVisibility() != View.GONE) {

                    firebaseUpload();
                } else {

                    // Change boolean to true - scrolls to the bottom of the recyclerView (in initChatAdapter).
                    messageSent = true;

                    if (newShape) {

                        // Since the UUID doesn't already exist in Firebase, add the circle.
                        CircleOptions circleOptions = new CircleOptions()
                                .center(new LatLng(circleLatitude, circleLongitude))
                                .clickable(true)
                                .radius(1.0);
                        CircleInformation circleInformation = new CircleInformation();
                        circleInformation.setCircleOptions(circleOptions);
                        circleInformation.setShapeUUID(shapeUUID);
                        DatabaseReference newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Points").push();
                        newFirebaseShape.setValue(circleInformation);

                        newShape = false;
                    }

                    String userUUID = UUID.randomUUID().toString();

                    // If mentions exist, add to the user's DMs.
                    if (removedDuplicatesMentions != null) {

                        ArrayList<String> messagedUsersAL = new ArrayList<>();
                        for (String mention : removedDuplicatesMentions) {

                            for (int i = 0; i < mUser.size(); i++) {

                                String userEmail = emailsAL.get(i);
                                if (mUser.get(i).equals(mention) && !messagedUsersAL.contains(userEmail)) {

                                    // Prevent sending the same DM to a user multiple times.
                                    messagedUsersAL.add(userEmail);

                                    String email = emailsAL.get(i);

                                    DmInformation dmInformation = new DmInformation();
                                    // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                    // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                    Object date = ServerValue.TIMESTAMP;
                                    dmInformation.setDate(date);
                                    dmInformation.setLat(latFirebaseValue);
                                    dmInformation.setLon(lonFirebaseValue);
                                    dmInformation.setMessage(input);
                                    dmInformation.setSeenByUser(false);
                                    dmInformation.setShapeUUID(shapeUUID);
                                    dmInformation.setUserIsWithinShape(userIsWithinShape);
                                    dmInformation.setUserUUID(userUUID);

                                    // Firebase does not allow ".", so replace them with ",".
                                    String receiverEmailFirebase = email.replace(".", ",");
                                    DatabaseReference newDm = FirebaseDatabase.getInstance().getReference().child("Users").child(receiverEmailFirebase).child("ReceivedDms").push();
                                    newDm.setValue(dmInformation);
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
                    messageInformation.setUserIsWithinShape(userIsWithinShape);
                    messageInformation.setUserUUID(userUUID);
                    DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID).push();
                    newMessage.setValue(messageInformation);

                    mInput.getText().clear();

                    if (removedDuplicatesMentions != null && !removedDuplicatesMentions.isEmpty()) {

                        removedDuplicatesMentions.clear();
                    }

                    // For some reason, if the text begins with a mention and onCreateView was called after the mention was added, the mention is not cleared with one call to clear().
                    mInput.getText().clear();
                    sendButton.setEnabled(true);
                }
            }
        });

        imageView.setOnClickListener(v -> {

            Log.i(TAG, "imageView -> onClick");

            cancelToasts();

            Intent Activity = new Intent(mContext, PhotoView.class);
            Activity.putExtra("imgUrl", imageURI.toString());
            Chat.this.startActivity(Activity);
        });

        videoImageView.setOnClickListener(v -> {

            Log.i(TAG, "videoImageView -> onClick");

            cancelToasts();

            Intent Activity = new Intent(mContext, VideoView.class);
            Activity.putExtra("videoUrl", videoURI.toString());
            Chat.this.startActivity(Activity);
        });
    }

    private void getFirebaseMessages(String referenceUserUUID) {

        Log.i(TAG, "getFirebaseMessages()");

        Query query;
        if (!fromDms && !clickedOnMention && datesALSize != null) {

            query = FirebaseDatabase.getInstance().getReference()
                    .child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID)
                    .orderByChild("date")
                    .startAt(datesAL.get(datesALSize));
        } else if (referenceUserUUID == null) {

            query = FirebaseDatabase.getInstance().getReference()
                    .child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID)
                    .limitToLast(20);
        } else {

            // If pagination needs to happen multiple times, mUser will not contain the referenceUserUUID.
            if (!mUser.contains(referenceUserUUID)) {

                // Use example: clicking on a DM and pagination occurs multiple times.
                query = FirebaseDatabase.getInstance().getReference()
                        .child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID)
                        .orderByChild("date")
                        .endAt(datesAL.get(0))
                        .limitToLast(20);
            } else {

                // Use example: manually scrolling to the top and loading older messages.
                query = FirebaseDatabase.getInstance().getReference()
                        .child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID)
                        .orderByChild("date")
                        .endAt(datesAL.get(mUser.indexOf(referenceUserUUID)))
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
                    toastMessageLong("Shape was deleted. Please return to map.");
                }

                if (datesALSize == null && snapshot.getChildrenCount() < 20) {

                    noMoreMessages = true;

                    // If 20 messages exist and the user scrolls to the top, a duplicate of the first item will be added. The following prevents that.
                    if (loadingOlderMessages && snapshot.getChildrenCount() == 1) {

                        progressIconIndeterminate.setVisibility(View.GONE);
                        return;
                    }
                }

                int i;
                if (!fromDms && !loadingOlderMessages && datesAL != null) {

                    i = datesAL.size();
                } else {

                    i = 0;
                }

                for (DataSnapshot ds : snapshot.getChildren()) {

                    // Prevents duplicates during getFirebaseMessages.
                    if (mUser.contains(ds.child("userUUID").getValue())) {

                        continue;
                    }

                    // Add dates to this AL for use in pagination.
                    datesAL.add(i, (Long) ds.child("date").getValue());
                    emailsAL.add(i, (String) ds.child("email").getValue());

                    Long serverDate = (Long) ds.child("date").getValue();
                    String user = (String) ds.child("userUUID").getValue();

                    // Used when a user mentions another user with "@".
                    mSuggestions.add(i, user);
                    String imageUrl = (String) ds.child("imageUrl").getValue();
                    String videoUrl = (String) ds.child("videoUrl").getValue();
                    String messageText = (String) ds.child("message").getValue();

                    SpannableString spannableMessageText = createSpannableMessage(messageText);

                    Boolean userIsWithinShape = (Boolean) ds.child("userIsWithinShape").getValue();
                    DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                    // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                    // This will cause onDataChange to fire twice; optimizations could be made in the future.
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
                    if (i == snapshot.getChildrenCount() - 1 && !firstLoad) {

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

                if (!loadingOlderMessages) {

                    addQuery();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                toastMessageLong(error.getMessage());
            }
        });
    }

    // Change to .limitToLast(1) to cut down on data usage. Otherwise, EVERY child at this node will be downloaded every time the child is updated.
    private void addQuery() {

        // This prevents duplicates when loading into Settings fragment then switched back into Chat (as onStop is never called but onStart is called).
        if (mQuery != null) {

            mQuery.removeEventListener(childEventListener);
        }

        // If this is the first time calling this eventListener and it's a new shape, initialize the adapter (but don't return, as the childEventListener should still be added), as onChildAdded won't be called the first time.
        if (firstLoad && newShape) {

            initChatAdapter();
        }

        mQuery = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID).limitToLast(1);
        childEventListener = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.i(TAG, "addQuery()");

                // Read RecyclerView scroll position (for use in initChatAdapter to prevent scrolling after Chat gets updated by another user).
                if (!firstLoad && chatRecyclerViewLinearLayoutManager != null && chatRecyclerView != null) {

                    index = chatRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
                    last = chatRecyclerViewLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                    View v = chatRecyclerView.getChildAt(0);
                    top = (v == null) ? 0 : (v.getTop() - chatRecyclerView.getPaddingTop());
                }

                // If this is the first time calling this eventListener, prevent double posts (as onStart() already added the last item).
                if (firstLoad) {

                    initChatAdapter();
                    return;
                }

                emailsAL.add((String) snapshot.child("email").getValue());
                datesAL.add((Long) snapshot.child("date").getValue());

                String user = (String) snapshot.child("userUUID").getValue();
                // Prevents duplicates when user adds a message to a new shape then switches between light / dark mode.
                if (mUser.contains(user)) {

                    return;
                }
                Long serverDate = (Long) snapshot.child("date").getValue();
                // Used when a user mentions another user with "@".
                mSuggestions.add(user);
                String imageUrl = (String) snapshot.child("imageUrl").getValue();
                String videoUrl = (String) snapshot.child("videoUrl").getValue();
                String messageText = (String) snapshot.child("message").getValue();

                SpannableString spannableMessageText = createSpannableMessage(messageText);

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
                mImage.add(imageUrl);
                mVideo.add(videoUrl);
                mText.add(spannableMessageText);
                mUserIsWithinShape.add(userIsWithinShape);

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

        mQuery.addChildEventListener(childEventListener);
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

                                    if (previouslyHighlightedPosition != null) {

                                        chatRecyclerView.getAdapter().notifyItemChanged(previouslyHighlightedPosition);
                                    }

                                    chatRecyclerView.getAdapter().notifyItemChanged(mUser.indexOf(fullLengthMention.get(finalI)));
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
        } else {

            spannableMessageText = new SpannableString(messageText);
        }

        return spannableMessageText;
    }

    @Override
    public void onStop() {

        Log.i(TAG, "onStop()");

        // Read RecyclerView scroll position (for use in initChatAdapter if user reload the activity).
        if (chatRecyclerViewLinearLayoutManager != null && chatRecyclerView != null) {

            index = chatRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
            last = chatRecyclerViewLinearLayoutManager.findLastCompletelyVisibleItemPosition();
            View v = chatRecyclerView.getChildAt(0);
            top = (v == null) ? 0 : (v.getTop() - chatRecyclerView.getPaddingTop());
        }

        datesALSize = datesAL.size() - 1;

        if (uploadTask != null) {

            uploadTask.cancel();
        }

        if (mQuery != null) {

            mQuery.removeEventListener(childEventListener);
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
        } else if ((last != null && last == (mUser.size() - 2)) || firstLoad && datesALSize == null || messageSent || reachedEndOfRecyclerView) {

            // Scroll to bottom of recyclerviewlayout after first initialization and after sending a recyclerviewlayout.
            chatRecyclerView.scrollToPosition(mUser.size() - 1);
            messageSent = false;
        } else if (index != null && top != null) {

            chatRecyclerViewLinearLayoutManager.scrollToPositionWithOffset(index, top);
        }

        firstLoad = false;
        loadingOlderMessages = false;
        // Need to make datesALSize null so user can load older messages after restarting the app.
        datesALSize = null;

        // After the initial load, make the progressIconIndeterminate invisible.
        if (progressIconIndeterminate != null && !needProgressIconIndeterminate) {

            progressIconIndeterminate.setVisibility(View.GONE);
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

        private final Context mContext;
        private final ArrayList<String> mMessageTime, mMessageUser, mMessageImage, mMessageImageVideo;
        private final ArrayList<SpannableString> mMessageText;
        private final ArrayList<Boolean> mUserIsWithinShape;
        // theme == true is light mode.
        private boolean theme;

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

                if (messageUserInside != null) {

                    messageUserInside.setOnClickListener(v -> {

                        ViewGroup.LayoutParams params = messageUserInside.getLayoutParams();
                        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

                        if (params.width == screenWidth) {

                            // params is in pixels, so convert it to dp.
                            params.width = (int) (120 * (itemView.getContext().getResources().getDisplayMetrics().density));
                        } else {

                            params.width = screenWidth;
                        }
                        messageUserInside.setLayoutParams(params);
                    });
                }

                if (messageUserOutside != null) {

                    messageUserOutside.setOnClickListener(v -> {

                        ViewGroup.LayoutParams params = messageUserOutside.getLayoutParams();
                        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

                        if (params.width == screenWidth) {

                            // params is in pixels, so convert it to dp.
                            params.width = (int) (120 * (itemView.getContext().getResources().getDisplayMetrics().density));
                        } else {

                            params.width = screenWidth;
                        }
                        messageUserOutside.setLayoutParams(params);
                    });
                }

                if (messageImageInside != null) {

                    messageImageInside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, PhotoView.class);
                        Activity.putExtra("imgUrl", mMessageImage.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
                    });
                }

                if (messageImageOutside != null) {

                    messageImageOutside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, PhotoView.class);
                        Activity.putExtra("imgUrl", mMessageImage.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
                    });
                }

                if (messageImageVideoInside != null) {

                    messageImageVideoInside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoUrl", mMessageImageVideo.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
                    });
                }

                if (messageImageVideoOutside != null) {

                    messageImageVideoOutside.setOnClickListener(v -> {

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoUrl", mMessageImageVideo.get(getAdapterPosition()));
                        mContext.startActivity(Activity);
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

            loadPreferences();

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

        protected void loadPreferences() {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

            theme = sharedPreferences.getBoolean(SettingsFragment.KEY_THEME_SWITCH, false);
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
        private boolean theme;

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

                        // Add mentions to this list. Duplicates will be added and later cleared from this list.
                        allMentions.add(mSuggestions.get(position));

                        return "@" + mSuggestions.get(position);
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

                // Don't add the mentionable if it already exists.
                if (mInput.getText().toString().trim().contains(mSuggestions.get(position))) {

                    toastMessageShort("User has already been mentioned.");
                    return;
                }

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

            progressIconIndeterminate.setVisibility(View.VISIBLE);

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
                                progressIconIndeterminate.setVisibility(View.GONE);
                                toastMessageShort("Post reported. Thank you!");
                                return;
                            }
                        }
                    }

                    // Edge case: user tries to delete a post that has already been manually deleted.
                    progressIconIndeterminate.setVisibility(View.GONE);
                    toastMessageShort("Post already deleted. Thank you!");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                    progressIconIndeterminate.setVisibility(View.GONE);
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

        Log.i(TAG, "onRequestPermissionsResult()");

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

                        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.CAMERA)) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Camera permissions were not granted. Ask again.");

                            cameraPermissionAlertAsync(checkPermissionsPicture);
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

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

                        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.CAMERA)) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Camera permissions were not granted. Ask again.");

                            cameraPermissionAlertAsync(checkPermissionsPicture);
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                            Log.d(TAG, "Request_ID_Take_Photo -> Storage permissions were not granted. Ask again.");

                            writeExternalStoragePermissionAlertAsync(checkPermissionsPicture);
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.RECORD_AUDIO)) {

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

        fromVideo = true;
    }

    private void cameraPermissionAlertAsync(Boolean checkPermissionsPicture) {

        AlertDialog.Builder alert;
        alert = new AlertDialog.Builder(mContext);

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
        alert = new AlertDialog.Builder(mContext);

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
        alert = new AlertDialog.Builder(mContext);

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

        // Disable the sendButton while content is being compressed.
        sendButton.setEnabled(false);

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

            // Prevents the progressIconIndeterminate from being removed by initChatAdapter().
            needProgressIconIndeterminate = true;

            fileIsImage = true;

            // Change textView to be to the right of imageView.
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mInput.getLayoutParams();
            params.addRule(RelativeLayout.END_OF, R.id.imageView);
            mInput.setLayoutParams(params);

            imageCompressAndAddToGalleryAsync();
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

            fileIsImage = false;

            videoCompressAndAddToGalleryAsync();
        }
    }

    // Save a non-compressed version to the gallery and add a compressed version to the imageView.
    private void imageCompressAndAddToGalleryAsync() {

        progressIconIndeterminate.setVisibility(View.VISIBLE);

        HandlerThread imageCompressAndAddToGalleryHandlerThread = new HandlerThread("imageCompressAndAddToGalleryHandlerThread");
        imageCompressAndAddToGalleryHandlerThread.start();
        Handler mHandler = new Handler(imageCompressAndAddToGalleryHandlerThread.getLooper());
        Runnable runnable = () -> {

            // Save a non-compressed image to the gallery.
            try {

                Bitmap imageBitmapFull = new Compressor(mContext)
                        .setMaxWidth(10000)
                        .setMaxHeight(10000)
                        .setQuality(100)
                        .setCompressFormat(Bitmap.CompressFormat.PNG)
                        .setDestinationDirectoryPath(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath())
                        .compressToBitmap(image);

                MediaStore.Images.Media.insertImage(mContext.getContentResolver(), imageBitmapFull, "HereBefore_" + System.currentTimeMillis() + "_PNG", null);

                // Create a compressed image.
                Bitmap mImageBitmap = new Compressor(mContext)
                        .setMaxWidth(480)
                        .setMaxHeight(640)
                        .setQuality(50)
                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                        .compressToBitmap(image);

                // Convert the bitmap to a byteArray for use in uploadImage().
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(mImageBitmap.getWidth() * mImageBitmap.getHeight());
                mImageBitmap.compress(Bitmap.CompressFormat.JPEG, 50, buffer);
                byteArray = buffer.toByteArray();

                // Update UI thread.
                new Handler(Looper.getMainLooper()).post(() -> {

                    Glide.with(mContext)
                            .load(byteArray)
                            .apply(new RequestOptions().override(480, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                            .into(imageView);

                    imageView.setVisibility(View.VISIBLE);
                    progressIconIndeterminate.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    // Allow initChatAdapter() to get rid of the progressIconIndeterminate with this boolean.
                    needProgressIconIndeterminate = false;
                });
            } catch (IOException ex) {

                toastMessageLong(ex.getMessage());
            }
        };

        mHandler.post(runnable);
    }

    // Save a non-compressed version to the gallery and add a compressed version to the imageView.
    private void videoCompressAndAddToGalleryAsync() {

        progressIcon.setProgress(0);
        progressIcon.setVisibility(View.VISIBLE);

        HandlerThread videoCompressAndAddToGalleryHandlerThread = new HandlerThread("videoCompressAndAddToGalleryHandlerThread");
        videoCompressAndAddToGalleryHandlerThread.start();
        Handler handler = new Handler(videoCompressAndAddToGalleryHandlerThread.getLooper());
        Runnable runnable = () -> {

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

            String filePath = videoTemp.getAbsolutePath();

            Transcoder.into(filePath)
                    .addDataSource(video.getAbsolutePath())
                    .setVideoTrackStrategy(DefaultVideoStrategies.for720x1280())
                    .setListener(new TranscoderListener() {

                        public void onTranscodeProgress(double progress) {

                            int mProgress = (int) (progress * 100);

                            progressIcon.setProgress(mProgress, true);
                        }

                        public void onTranscodeCompleted(int successCode) {

                            if (successCode == Transcoder.SUCCESS_TRANSCODED) {

                                Log.i(TAG, "Transcoder.SUCCESS_TRANSCODED");
                                onTranscodeFinished(filePath);
                            } else if (successCode == Transcoder.SUCCESS_NOT_NEEDED) {

                                Log.i(TAG, "Transcoder.SUCCESS_NOT_NEEDED");
                                onTranscodeFinished(video.getAbsolutePath());
                            }
                        }

                        public void onTranscodeCanceled() {

                            sendButton.setEnabled(true);
                        }

                        public void onTranscodeFailed(@NonNull Throwable exception) {

                            sendButton.setEnabled(true);
                            Log.e(TAG, "Transcoder error occurred: " + exception.getMessage());
                            toastMessageLong("Error compressing video: " + exception.getMessage());
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

                toastMessageLong(ex.getMessage());
            }
            output64.close();

            // Change the videoImageView's orientation depending on the orientation of the video.
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            // Set the video Uri as data source for MediaMetadataRetriever
            retriever.setDataSource(absolutePath);
            // Get one "frame"/bitmap - * NOTE - no time was set, so the first available frame will be used
            mBitmap = retriever.getFrameAtTime(1);
            // Get the bitmap width and height
            mBitmapWidth = mBitmap.getWidth();
            mBitmapHeight = mBitmap.getHeight();

        } catch (IOException ex) {

            toastMessageLong(ex.getMessage());
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
            }
        });
    }

    private void firebaseUpload() {

        Log.i(TAG, "firebaseUpload()");

        // Show the loading icon while the image is being uploaded to Firebase.
        progressIconIndeterminate.setVisibility(View.VISIBLE);
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
            final StorageReference storageReferenceVideo = FirebaseStorage.getInstance().getReference("Videos").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(String.valueOf(System.currentTimeMillis()));
            storageReferenceVideo.putFile(videoURI);

            uploadTask = storageReferenceVideo.putFile(videoURI).addOnSuccessListener(taskSnapshot -> storageReferenceVideo.getDownloadUrl()

                    .addOnSuccessListener(uri -> {

                        // Prevents a crash if user changed activity, as this is an asynchronous task.
                        if (mActivity == null || mContext == null) {

                            return;
                        }

                        Log.i(TAG, "firebaseUpload() -> onSuccess");

                        // Change boolean to true - scrolls to the bottom of the recyclerView (in initChatAdapter()).
                        messageSent = true;

                        if (newShape) {

                            // Since the UUID doesn't already exist in Firebase, add the circle.
                            CircleOptions circleOptions = new CircleOptions()
                                    .center(new LatLng(circleLatitude, circleLongitude))
                                    .clickable(true)
                                    .radius(1.0);
                            CircleInformation circleInformation = new CircleInformation();
                            circleInformation.setCircleOptions(circleOptions);
                            circleInformation.setShapeUUID(shapeUUID);
                            DatabaseReference newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Points").push();
                            newFirebaseShape.setValue(circleInformation);

                            newShape = false;
                        }

                        String userUUID = UUID.randomUUID().toString();

                        // If mentions exist, add to the user's DMs.
                        if (removedDuplicatesMentions != null) {

                            ArrayList<String> messagedUsersAL = new ArrayList<>();
                            for (String mention : removedDuplicatesMentions) {

                                for (int i = 0; i < mUser.size(); i++) {

                                    String userEmail = emailsAL.get(i);
                                    if (mUser.get(i).equals(mention) && !messagedUsersAL.contains(userEmail)) {

                                        // Prevent sending the same DM to a user multiple times.
                                        messagedUsersAL.add(userEmail);

                                        String email = emailsAL.get(i);

                                        DmInformation dmInformation = new DmInformation();
                                        // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                        // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                        Object date = ServerValue.TIMESTAMP;
                                        dmInformation.setDate(date);
                                        dmInformation.setLat(latFirebaseValue);
                                        dmInformation.setLon(lonFirebaseValue);
                                        if (input.length() != 0) {
                                            dmInformation.setMessage(input);
                                        }
                                        dmInformation.setSeenByUser(false);
                                        dmInformation.setShapeUUID(shapeUUID);
                                        dmInformation.setUserIsWithinShape(userIsWithinShape);
                                        dmInformation.setUserUUID(userUUID);
                                        dmInformation.setVideoUrl(uri.toString());

                                        // Firebase does not allow ".", so replace them with ",".
                                        String receiverEmailFirebase = email.replace(".", ",");
                                        DatabaseReference newDm = FirebaseDatabase.getInstance().getReference().child("Users").child(receiverEmailFirebase).child("ReceivedDms").push();
                                        newDm.setValue(dmInformation);
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
                        if (input.length() != 0) {

                            messageInformation.setMessage(input);
                        }
                        messageInformation.setUserIsWithinShape(userIsWithinShape);
                        messageInformation.setUserUUID(userUUID);
                        messageInformation.setVideoUrl(uri.toString());
                        DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID).push();
                        newMessage.setValue(messageInformation);

                        if (removedDuplicatesMentions != null && !removedDuplicatesMentions.isEmpty()) {

                            removedDuplicatesMentions.clear();
                        }

                        if (video != null) {

                            deleteDirectory(video);
                        }

                        sendButton.setEnabled(true);
                        progressIcon.setVisibility(View.GONE);
                    }))

                    .addOnProgressListener(snapshot -> {

                        if (snapshot.getBytesTransferred() != 0) {

                            progressIconIndeterminate.setVisibility(View.GONE);
                        } else if (progressIcon.getVisibility() != View.VISIBLE) {

                            progressIcon.setVisibility(View.VISIBLE);
                        }

                        int progress = (int) ((int) (snapshot.getBytesTransferred() * 100) / snapshot.getTotalByteCount());

                        progressIcon.setProgress(progress, true);
                    })

                    .addOnFailureListener(ex -> {

                        sendButton.setEnabled(true);
                        progressIcon.setProgress(0);
                        progressIcon.setVisibility(View.GONE);
                        progressIconIndeterminate.setVisibility(View.GONE);
                        toastMessageLong(ex.getMessage());
                        Log.e(TAG, "firebaseUpload() -> !fileIsImage -> onFailure -> " + ex.getMessage());
                    });
        } else {

            // byteArray and image.
            final StorageReference storageReferenceImage = FirebaseStorage.getInstance().getReference("Images").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(String.valueOf(System.currentTimeMillis()));
            storageReferenceImage.putBytes(byteArray);

            uploadTask = storageReferenceImage.putBytes(byteArray).addOnSuccessListener(taskSnapshot -> storageReferenceImage.getDownloadUrl()

                    .addOnSuccessListener(uri -> {

                        // Prevents a crash if user changed activity, as this is an asynchronous task.
                        if (mActivity == null || mContext == null) {

                            return;
                        }

                        Log.i(TAG, "uploadImage() -> onSuccess");

                        // Change boolean to true - scrolls to the bottom of the recyclerView (in initChatAdapter()).
                        messageSent = true;

                        if (newShape) {

                            // Since the UUID doesn't already exist in Firebase, add the circle.
                            CircleOptions circleOptions = new CircleOptions()
                                    .center(new LatLng(circleLatitude, circleLongitude))
                                    .clickable(true)
                                    .radius(1.0);
                            CircleInformation circleInformation = new CircleInformation();
                            circleInformation.setCircleOptions(circleOptions);
                            circleInformation.setShapeUUID(shapeUUID);
                            DatabaseReference newFirebaseShape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child("Points").push();
                            newFirebaseShape.setValue(circleInformation);

                            newShape = false;
                        }

                        String userUUID = UUID.randomUUID().toString();

                        // If mentions exist, add to the user's DMs.
                        if (removedDuplicatesMentions != null) {

                            ArrayList<String> messagedUsersAL = new ArrayList<>();
                            for (String mention : removedDuplicatesMentions) {

                                for (int i = 0; i < mUser.size(); i++) {

                                    String userEmail = emailsAL.get(i);
                                    if (mUser.get(i).equals(mention) && !messagedUsersAL.contains(userEmail)) {

                                        // Prevent sending the same DM to a user multiple times.
                                        messagedUsersAL.add(userEmail);

                                        String email = emailsAL.get(i);

                                        DmInformation dmInformation = new DmInformation();
                                        // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                        // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                        Object date = ServerValue.TIMESTAMP;
                                        dmInformation.setDate(date);
                                        dmInformation.setImageUrl(uri.toString());
                                        dmInformation.setLat(latFirebaseValue);
                                        dmInformation.setLon(lonFirebaseValue);
                                        if (input.length() != 0) {
                                            dmInformation.setMessage(input);
                                        }
                                        dmInformation.setSeenByUser(false);
                                        dmInformation.setShapeUUID(shapeUUID);
                                        dmInformation.setUserIsWithinShape(userIsWithinShape);
                                        dmInformation.setUserUUID(userUUID);

                                        // Firebase does not allow ".", so replace them with ",".
                                        String receiverEmailFirebase = email.replace(".", ",");
                                        DatabaseReference newDm = FirebaseDatabase.getInstance().getReference().child("Users").child(receiverEmailFirebase).child("ReceivedDms").push();
                                        newDm.setValue(dmInformation);
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
                        messageInformation.setImageUrl(uri.toString());
                        if (input.length() != 0) {

                            messageInformation.setMessage(input);
                        }
                        messageInformation.setUserIsWithinShape(userIsWithinShape);
                        messageInformation.setUserUUID(userUUID);
                        DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("MessageThreads").child("(" + latFirebaseValue + ", " + lonFirebaseValue + ")").child(shapeUUID).push();
                        newMessage.setValue(messageInformation);

                        if (removedDuplicatesMentions != null && !removedDuplicatesMentions.isEmpty()) {

                            removedDuplicatesMentions.clear();
                        }

                        if (image != null) {

                            deleteDirectory(image);
                        }

                        sendButton.setEnabled(true);
                        progressIcon.setVisibility(View.GONE);
                    }))

                    .addOnProgressListener(snapshot -> {

                        if (snapshot.getBytesTransferred() != 0) {

                            progressIconIndeterminate.setVisibility(View.GONE);
                        } else if (progressIcon.getVisibility() != View.VISIBLE) {

                            progressIcon.setVisibility(View.VISIBLE);
                        }

                        int progress = (int) ((int) (snapshot.getBytesTransferred() * 100) / snapshot.getTotalByteCount());

                        progressIcon.setProgress(progress, true);
                    })

                    .addOnFailureListener(ex -> {

                        sendButton.setEnabled(true);
                        progressIcon.setProgress(0);
                        progressIcon.setVisibility(View.GONE);
                        progressIconIndeterminate.setVisibility(View.GONE);
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

    private void toastMessageShort(String message) {

        // Prevents a crash if the user backed out of activity and a toast message occurs from another thread.
        if (mActivity != null) {

            cancelToasts();
            shortToast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT);
            shortToast.setGravity(Gravity.CENTER, 0, 0);
            shortToast.show();
        }
    }

    private void toastMessageLong(String message) {

        // Prevents a crash if the user backed out of activity and a toast message occurs from another thread.
        if (mActivity != null) {

            cancelToasts();
            longToast = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
            longToast.setGravity(Gravity.CENTER, 0, 0);
            longToast.show();
        }
    }
}