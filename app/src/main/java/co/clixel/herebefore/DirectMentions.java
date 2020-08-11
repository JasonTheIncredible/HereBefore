package co.clixel.herebefore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.PolyUtil;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static java.text.DateFormat.getDateTimeInstance;

public class DirectMentions extends Fragment {

    private static final String TAG = "DirectMentions";
    private String email;
    private ArrayList<String> mTime, mUser, mImage, mVideo, mText, mShapeUUID, userAL, imageURLAL, videoURLAL, messageTextAL, shapeUUIDAL, userUUIDAL, userEmailAL, timeAL;
    private ArrayList<Boolean> mUserIsWithinShape, mShapeIsCircle, mSeenByUser, userIsWithinShapeAL, shapeIsCircleAL, seenByUserAL;
    private ArrayList<Long> mPosition, notSeenByUserList, positionAL;
    private RecyclerView directMentionsRecyclerView;
    private static int index = -1, top = -1, last;
    private DatabaseReference rootRef, databaseReference, databaseReferenceCircles, databaseReferencePolygons;
    private ValueEventListener eventListener, eventListenerCircles, eventListenerPolygons;
    private LinearLayoutManager directMentionsRecyclerViewLinearLayoutManager;
    private boolean firstLoad, userIsWithinShape, alreadyInitialized = false;
    private View loadingIcon;
    private Toast longToast;
    private Double userLatitude, userLongitude;
    private Context mContext;
    private Activity mActivity;
    private View rootView;
    private TextView noDMsTextView;
    private Query query;

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(TAG, "onCreateView()");
        rootView = inflater.inflate(R.layout.directmentions, container, false);

        mContext = getContext();
        mActivity = getActivity();

        directMentionsRecyclerView = rootView.findViewById(R.id.mentionsList);
        loadingIcon = rootView.findViewById(R.id.loadingIcon);
        noDMsTextView = rootView.findViewById(R.id.noDMsTextView);

        directMentionsRecyclerViewLinearLayoutManager = new LinearLayoutManager(mActivity);

        mTime = new ArrayList<>();
        mUser = new ArrayList<>();
        mImage = new ArrayList<>();
        mVideo = new ArrayList<>();
        mText = new ArrayList<>();
        mShapeUUID = new ArrayList<>();
        mUserIsWithinShape = new ArrayList<>();
        mShapeIsCircle = new ArrayList<>();
        mSeenByUser = new ArrayList<>();
        mPosition = new ArrayList<>();
        notSeenByUserList = new ArrayList<>();

        userAL = new ArrayList<>();
        imageURLAL = new ArrayList<>();
        timeAL = new ArrayList<>();
        videoURLAL = new ArrayList<>();
        messageTextAL = new ArrayList<>();
        shapeUUIDAL = new ArrayList<>();
        userUUIDAL = new ArrayList<>();
        userEmailAL = new ArrayList<>();
        userIsWithinShapeAL = new ArrayList<>();
        shapeIsCircleAL = new ArrayList<>();
        seenByUserAL = new ArrayList<>();
        positionAL = new ArrayList<>();

        // Set to true to scroll to the bottom of directMentionsRecyclerView.
        firstLoad = true;

        if (mActivity != null) {

            Bundle extras = mActivity.getIntent().getExtras();
            if (extras != null) {

                userLatitude = extras.getDouble("userLatitude");
                userLongitude = extras.getDouble("userLongitude");
            } else {

                Log.e(TAG, "onCreateView() -> extras == null");
            }

            // Make the loadingIcon visible upon the first load, as it can sometimes take a while to show anything. It should be made invisible in initDirectMentionsAdapter().
            if (loadingIcon != null) {

                loadingIcon.setVisibility(View.VISIBLE);
            }
        } else {

            Log.e(TAG, "onCreateView() -> activity == null");
        }

        return rootView;
    }

    @Override
    public void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // If user has a Google account, get email one way. Else, get email another way.
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(mContext);
        if (acct != null) {

            email = acct.getEmail();
        } else {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            email = sharedPreferences.getString("userToken", "null");
        }

        // Search Firebase for DirectMentions.
        // If Chat doesn't exist, use the listeners here. Otherwise, use the snapShot from Chat.
        int chatLayout = mContext.getResources().getIdentifier("chat", "chat", mContext.getPackageName());
        if (chatLayout == 0) {

            rootRef = FirebaseDatabase.getInstance().getReference();
            databaseReference = rootRef.child("MessageThreads");
            eventListener = new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    addEventListener(dataSnapshot);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                    toastMessageLong(databaseError.getMessage());
                }
            };

            databaseReference.addListenerForSingleValueEvent(eventListener);
        }
    }

    // Search Firebase for DirectMentions. As these calls are asynchronous, add them one at a time.
    public void addEventListener(DataSnapshot dataSnapshot) {

        Log.i(TAG, "addEventListener()");

        // First, fill the arrayLists. Then, use the arrayLists to fill out recyclerView.
        // Using the same dataSnapshot cuts down on data usage.
        if (email != null) {

            fillArrayLists(dataSnapshot);

            fillRecyclerView(dataSnapshot);
        }
    }

    private void fillArrayLists(@NonNull DataSnapshot dataSnapshot) {

        Log.i(TAG, "fillArrayLists()");

        for (DataSnapshot ds : dataSnapshot.getChildren()) {

            // Only add necessary items to arrayLists.
            if (ds.child("email").getValue() != null) {

                if (ds.child("removedMentionDuplicates").getValue() == null && !ds.child("email").getValue().equals(email)) {

                    return;
                }
            }

            userUUIDAL.add((String) ds.child("userUUID").getValue());
            userEmailAL.add((String) ds.child("email").getValue());

            Long serverDate = (Long) ds.child("date").getValue();
            DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
            if (serverDate != null) {

                Date netDate = (new Date(serverDate));
                String messageTime = dateFormat.format(netDate);
                timeAL.add(messageTime);
            } else {

                Log.e(TAG, "fillArrayList() -> serverDate == null");
            }
            userAL.add((String) ds.child("userUUID").getValue());
            imageURLAL.add((String) ds.child("imageURL").getValue());
            videoURLAL.add((String) ds.child("videoURL").getValue());
            messageTextAL.add((String) ds.child("message").getValue());
            shapeUUIDAL.add((String) ds.child("shapeUUID").getValue());
            userIsWithinShapeAL.add((Boolean) ds.child("userIsWithinShape").getValue());
            shapeIsCircleAL.add((Boolean) ds.child("shapeIsCircle").getValue());
            positionAL.add((Long) ds.child("position").getValue());
            seenByUserAL.add((Boolean) ds.child("seenByUser").getValue());
        }
    }

    private void fillRecyclerView(@NonNull DataSnapshot dataSnapshot) {

        Log.i(TAG, "fillRecyclerView()");

        // Clear the RecyclerView before adding new entries to prevent duplicates,
        // and read RecyclerView scroll position (for use in initDirectMentionsAdapter())
        if (directMentionsRecyclerViewLinearLayoutManager != null) {

            index = directMentionsRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
            last = directMentionsRecyclerViewLinearLayoutManager.findLastCompletelyVisibleItemPosition();
            View v = directMentionsRecyclerView.getChildAt(0);
            top = (v == null) ? 0 : (v.getTop() - directMentionsRecyclerView.getPaddingTop());
        }

        for (DataSnapshot ds : dataSnapshot.getChildren()) {

            if (ds.child("removedMentionDuplicates").getValue() != null) {

                for (DataSnapshot mention : ds.child("removedMentionDuplicates").getChildren()) {

                    for (int i = 0; i < userUUIDAL.size(); i++) {

                        if (mention.getValue() != null) {

                            if (mention.getValue().toString().equals(userUUIDAL.get(i))) {

                                if (userEmailAL.get(i).equals(email)) {

                                    Long serverDate = (Long) ds.child("date").getValue();
                                    DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                                    if (serverDate != null) {

                                        Date netDate = (new Date(serverDate));
                                        String messageTime = dateFormat.format(netDate);
                                        mTime.add(messageTime);
                                    } else {

                                        Log.e(TAG, "fillRecyclerView() -> serverDate == null");
                                    }
                                    mUser.add((String) ds.child("userUUID").getValue());
                                    mImage.add((String) ds.child("imageURL").getValue());
                                    mVideo.add((String) ds.child("videoURL").getValue());
                                    mText.add((String) ds.child("message").getValue());
                                    mShapeUUID.add((String) ds.child("shapeUUID").getValue());
                                    mUserIsWithinShape.add((Boolean) ds.child("userIsWithinShape").getValue());
                                    mShapeIsCircle.add((Boolean) ds.child("shapeIsCircle").getValue());
                                    mPosition.add((Long) ds.child("position").getValue());
                                    mSeenByUser.add((Boolean) ds.child("seenByUser").getValue());

                                    if (!(Boolean) ds.child("seenByUser").getValue()) {

                                        notSeenByUserList.add((Long) ds.child("position").getValue());
                                        ds.child("seenByUser").getRef().setValue(true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        addQueryPartOne();
    }

    // Change to .limitToLast(1) to cut down on data usage. Otherwise, EVERY child at this node will be downloaded every time the child is updated.
    private void addQueryPartOne() {

        // Add new values to arrayLists one at a time. This prevents the need to download the whole dataSnapshot every time this information is needed in eventListenerThree.
        query = rootRef.child("MessageThreads").limitToLast(1);
        eventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Log.i(TAG, "addQueryPartOne()");

                // Prevent the eventListener from getting called twice.
                if (alreadyInitialized) {

                    alreadyInitialized = false;
                    return;
                }

                // If this is the first time calling this eventListener, prevent double posts (as onStart() already added the last item).
                if (firstLoad) {

                    addQueryPartTwo(dataSnapshot);
                    return;
                }

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    // Only add necessary items to arrayLists.
                    if (ds.child("email").getValue() != null) {

                        if (ds.child("removedMentionDuplicates").getValue() == null && !ds.child("email").getValue().equals(email)) {

                            return;
                        }
                    }

                    userUUIDAL.add((String) ds.child("userUUID").getValue());
                    userEmailAL.add((String) ds.child("email").getValue());

                    Long serverDate = (Long) ds.child("date").getValue();
                    DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                    if (serverDate != null) {

                        Date netDate = (new Date(serverDate));
                        String messageTime = dateFormat.format(netDate);
                        timeAL.add(messageTime);
                    } else {

                        Log.e(TAG, "addQueryPartOne() -> serverDate == null");
                    }
                    userAL.add((String) ds.child("userUUID").getValue());
                    imageURLAL.add((String) ds.child("imageURL").getValue());
                    videoURLAL.add((String) ds.child("videoURL").getValue());
                    messageTextAL.add((String) ds.child("message").getValue());
                    shapeUUIDAL.add((String) ds.child("shapeUUID").getValue());
                    userIsWithinShapeAL.add((Boolean) ds.child("userIsWithinShape").getValue());
                    shapeIsCircleAL.add((Boolean) ds.child("shapeIsCircle").getValue());
                    positionAL.add((Long) ds.child("position").getValue());
                    seenByUserAL.add((Boolean) ds.child("seenByUser").getValue());

                    addQueryPartTwo(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                toastMessageLong(databaseError.getMessage());
            }
        };

        query.addValueEventListener(eventListener);
    }

    private void addQueryPartTwo(@NonNull DataSnapshot dataSnapshot) {

        Log.i(TAG, "addQueryPartTwo()");

        // If this is the first time calling this eventListener, prevent double posts (as onStart() already added the last item).
        if (firstLoad) {

            firstLoad = false;
            initDirectMentionsAdapter();
            return;
        }

        // Read RecyclerView scroll position (for use in initDirectMentionsAdapter()).
        if (directMentionsRecyclerViewLinearLayoutManager != null) {

            index = directMentionsRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
            last = directMentionsRecyclerViewLinearLayoutManager.findLastCompletelyVisibleItemPosition();
            View v = directMentionsRecyclerView.getChildAt(0);
            top = (v == null) ? 0 : (v.getTop() - directMentionsRecyclerView.getPaddingTop());
        }

        for (DataSnapshot ds : dataSnapshot.getChildren()) {

            if (ds.child("removedMentionDuplicates").getValue() != null) {

                for (DataSnapshot mention : ds.child("removedMentionDuplicates").getChildren()) {

                    if (mention.getValue() != null) {

                        for (int i = 0; i < userUUIDAL.size(); i++) {

                            if (mention.getValue().toString().equals(userUUIDAL.get(i))) {

                                if (userEmailAL.get(i).equals(email)) {

                                    alreadyInitialized = true;

                                    mTime.add(timeAL.get(userUUIDAL.size() - 1));
                                    mUser.add(userAL.get(userUUIDAL.size() - 1));
                                    mImage.add(imageURLAL.get(userUUIDAL.size() - 1));
                                    mVideo.add(videoURLAL.get(userUUIDAL.size() - 1));
                                    mText.add(messageTextAL.get(userUUIDAL.size() - 1));
                                    mShapeUUID.add(shapeUUIDAL.get(userUUIDAL.size() - 1));
                                    mUserIsWithinShape.add(userIsWithinShapeAL.get(userUUIDAL.size() - 1));
                                    mShapeIsCircle.add(shapeIsCircleAL.get(userUUIDAL.size() - 1));
                                    mPosition.add(positionAL.get(userUUIDAL.size() - 1));
                                    mSeenByUser.add(seenByUserAL.get(userUUIDAL.size() - 1));
                                    // All DMs will be "new", so add them to notSeenByUserList and update their value in Firebase.
                                    notSeenByUserList.add(positionAL.get(userUUIDAL.size() - 1));
                                    ds.child("seenByUser").getRef().setValue(true);

                                    initDirectMentionsAdapter();
                                    // Only updating one value, so return.
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void initDirectMentionsAdapter() {

        // Initialize the RecyclerView.
        Log.i(TAG, "initDirectMentionsAdapter()");

        DirectMentionsAdapter adapter = new DirectMentionsAdapter(mContext, mTime, mUser, mImage, mVideo, mText, mShapeUUID, mUserIsWithinShape, mShapeIsCircle, mPosition, mSeenByUser);
        directMentionsRecyclerView.setAdapter(adapter);
        directMentionsRecyclerView.setLayoutManager(directMentionsRecyclerViewLinearLayoutManager);

        if (last == (mTime.size() - 2) || firstLoad) {

            // Scroll to bottom of recyclerviewlayout after first initialization and after sending a recyclerviewlayout.
            directMentionsRecyclerView.scrollToPosition(mTime.size() - 1);
        } else {

            // Set RecyclerView scroll position to prevent position change when Firebase gets updated and after screen orientation change.
            directMentionsRecyclerViewLinearLayoutManager.scrollToPositionWithOffset(index, top);
        }

        // After the initial load, make the loadingIcon invisible.
        if (loadingIcon != null) {

            loadingIcon.setVisibility(View.GONE);
        }

        if (mUser.size() == 0) {

            noDMsTextView.setVisibility(View.VISIBLE);
        } else {

            noDMsTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStop() {

        Log.i(TAG, "onStop()");

        if (rootRef != null) {

            rootRef = null;
        }

        if (databaseReferenceCircles != null) {

            if (eventListenerCircles != null) {

                databaseReferenceCircles.removeEventListener(eventListenerCircles);
            }
        }

        if (databaseReferencePolygons != null) {

            if (eventListenerPolygons != null) {

                databaseReferencePolygons.removeEventListener(eventListenerPolygons);
            }
        }

        if (eventListenerCircles != null) {

            eventListenerCircles = null;
        }

        if (eventListenerPolygons != null) {

            eventListenerPolygons = null;
        }

        if (databaseReference != null) {

            databaseReference.removeEventListener(eventListener);
            databaseReference = null;
        }

        if (query != null) {

            query.removeEventListener(eventListener);
            query = null;
        }

        alreadyInitialized = false;

        if (directMentionsRecyclerView != null) {

            directMentionsRecyclerView.clearOnScrollListeners();
            directMentionsRecyclerView.setAdapter(null);
        }

        if (eventListener != null) {

            eventListener = null;
        }

        cancelToasts();

        super.onStop();
    }

    @Override
    public void onDestroyView() {

        Log.i(TAG, "onDestroyView()");

        if (rootView != null) {

            rootView = null;
        }

        if (mContext != null) {

            mContext = null;
        }

        if (mActivity != null) {

            mActivity = null;
        }

        if (directMentionsRecyclerView != null) {

            directMentionsRecyclerView = null;
        }

        if (directMentionsRecyclerViewLinearLayoutManager != null) {

            directMentionsRecyclerViewLinearLayoutManager = null;
        }

        mTime = null;
        mUser = null;
        mImage = null;
        mVideo = null;
        mText = null;
        mShapeUUID = null;
        mUserIsWithinShape = null;
        mShapeIsCircle = null;
        mSeenByUser = null;
        mPosition = null;
        notSeenByUserList = null;

        userAL = null;
        imageURLAL = null;
        timeAL = null;
        videoURLAL = null;
        messageTextAL = null;
        shapeUUIDAL = null;
        userUUIDAL = null;
        userEmailAL = null;
        userIsWithinShapeAL = null;
        shapeIsCircleAL = null;
        seenByUserAL = null;
        positionAL = null;

        loadingIcon = null;
        noDMsTextView = null;

        super.onDestroyView();
    }

    private void cancelToasts() {

        if (longToast != null) {

            longToast.cancel();
        }
    }

    public class DirectMentionsAdapter extends RecyclerView.Adapter<DirectMentionsAdapter.ViewHolder> {

        private Context mContext;
        private ArrayList<String> mMessageTime, mMessageUser, mMessageImage, mMessageImageVideo, mMessageText, mShapeUUID;
        private ArrayList<Boolean> mUserIsWithinShape, mShapeIsCircle;
        private ArrayList<Long> mPosition;
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

                itemView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        loadingIcon.setVisibility(View.VISIBLE);

                        if (mShapeIsCircle.get(getAdapterPosition())) {

                            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                            databaseReferenceCircles = rootRef.child("Circles");
                            eventListenerCircles = new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    for (DataSnapshot ds : snapshot.getChildren()) {

                                        String shapeUUID = (String) ds.child("shapeUUID").getValue();
                                        if (shapeUUID != null) {

                                            if (shapeUUID.equals(mShapeUUID.get(getAdapterPosition()))) {

                                                Double mLatitude = (Double) ds.child("circleOptions").child("center").child("latitude").getValue();
                                                Double mLongitude = (Double) ds.child("circleOptions").child("center").child("longitude").getValue();
                                                if (mLatitude != null && mLongitude != null) {

                                                    double mRadius = (double) (long) ds.child("circleOptions").child("radius").getValue();
                                                    if (mRadius != 0) {

                                                        float[] distance = new float[2];

                                                        Location.distanceBetween(mLatitude, mLongitude,
                                                                userLatitude, userLongitude, distance);

                                                        // Boolean; will be true if user is within the circle upon circle click.
                                                        userIsWithinShape = !(distance[0] > mRadius);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                    loadingIcon.setVisibility(View.GONE);
                                    toastMessageLong(error.getMessage());
                                }
                            };

                            databaseReferenceCircles.addListenerForSingleValueEvent(eventListenerCircles);
                        } else {

                            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                            databaseReferencePolygons = rootRef.child("Polygons");
                            eventListenerPolygons = new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    for (DataSnapshot ds : snapshot.getChildren()) {

                                        String shapeUUID = (String) ds.child("shapeUUID").getValue();
                                        if (shapeUUID != null) {

                                            if (shapeUUID.equals(mShapeUUID.get(getAdapterPosition()))) {

                                                LatLng marker3Position = null;
                                                LatLng marker4Position = null;
                                                LatLng marker5Position = null;
                                                LatLng marker6Position = null;
                                                LatLng marker7Position;
                                                List<LatLng> polygon = new ArrayList<>();

                                                LatLng marker0Position = new LatLng((double) ds.child("polygonOptions/points/0/latitude/").getValue(), (double) ds.child("polygonOptions/points/0/longitude/").getValue());
                                                LatLng marker1Position = new LatLng((double) ds.child("polygonOptions/points/1/latitude/").getValue(), (double) ds.child("polygonOptions/points/1/longitude/").getValue());
                                                LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                                                if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                    marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                                                }
                                                if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                    marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                                                }
                                                if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                    marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                                                }
                                                if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                    marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                                                }
                                                if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                                    marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                                                    polygon.add(marker7Position);
                                                    polygon.add(marker6Position);
                                                    polygon.add(marker5Position);
                                                    polygon.add(marker4Position);
                                                    polygon.add(marker3Position);
                                                    polygon.add(marker2Position);
                                                    polygon.add(marker1Position);
                                                    polygon.add(marker0Position);

                                                    userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                    cancelToasts();

                                                    Intent Activity = new Intent(mContext, Navigation.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);

                                                    mActivity.finish();
                                                } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                    polygon.add(marker6Position);
                                                    polygon.add(marker5Position);
                                                    polygon.add(marker4Position);
                                                    polygon.add(marker3Position);
                                                    polygon.add(marker2Position);
                                                    polygon.add(marker1Position);
                                                    polygon.add(marker0Position);

                                                    userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                    cancelToasts();

                                                    Intent Activity = new Intent(mContext, Navigation.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);

                                                    mActivity.finish();
                                                } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                    polygon.add(marker5Position);
                                                    polygon.add(marker4Position);
                                                    polygon.add(marker3Position);
                                                    polygon.add(marker2Position);
                                                    polygon.add(marker1Position);
                                                    polygon.add(marker0Position);

                                                    userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                    cancelToasts();

                                                    Intent Activity = new Intent(mContext, Navigation.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);

                                                    mActivity.finish();
                                                } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                    polygon.add(marker4Position);
                                                    polygon.add(marker3Position);
                                                    polygon.add(marker2Position);
                                                    polygon.add(marker1Position);
                                                    polygon.add(marker0Position);

                                                    userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                    cancelToasts();

                                                    Intent Activity = new Intent(mContext, Navigation.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);

                                                    mActivity.finish();
                                                } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                    polygon.add(marker3Position);
                                                    polygon.add(marker2Position);
                                                    polygon.add(marker1Position);
                                                    polygon.add(marker0Position);

                                                    userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                    cancelToasts();

                                                    Intent Activity = new Intent(mContext, Navigation.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);

                                                    mActivity.finish();
                                                } else {
                                                    polygon.add(marker2Position);
                                                    polygon.add(marker1Position);
                                                    polygon.add(marker0Position);

                                                    userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                    cancelToasts();

                                                    Intent Activity = new Intent(mContext, Navigation.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);

                                                    mActivity.finish();
                                                }
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                    loadingIcon.setVisibility(View.GONE);
                                    toastMessageLong(error.getMessage());
                                }
                            };

                            databaseReferencePolygons.addListenerForSingleValueEvent(eventListenerPolygons);
                        }
                    }
                });

                if (playButtonInside != null) {

                    playButtonInside.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            loadingIcon.setVisibility(View.VISIBLE);

                            if (mShapeIsCircle.get(getAdapterPosition())) {

                                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                databaseReferenceCircles = rootRef.child("Circles");
                                eventListenerCircles = new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        for (DataSnapshot ds : snapshot.getChildren()) {

                                            String shapeUUID = (String) ds.child("shapeUUID").getValue();
                                            if (shapeUUID != null) {

                                                if (shapeUUID.equals(mShapeUUID.get(getAdapterPosition()))) {

                                                    Double mLatitude = (Double) ds.child("circleOptions").child("center").child("latitude").getValue();
                                                    Double mLongitude = (Double) ds.child("circleOptions").child("center").child("longitude").getValue();
                                                    if (mLatitude != null && mLongitude != null) {

                                                        double mRadius = (double) (long) ds.child("circleOptions").child("radius").getValue();
                                                        if (mRadius != 0) {

                                                            float[] distance = new float[2];

                                                            Location.distanceBetween(mLatitude, mLongitude,
                                                                    userLatitude, userLongitude, distance);

                                                            // Boolean; will be true if user is within the circle upon circle click.
                                                            userIsWithinShape = !(distance[0] > mRadius);

                                                            cancelToasts();

                                                            Intent Activity = new Intent(mContext, Navigation.class);
                                                            Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                            Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                            Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                            loadingIcon.setVisibility(View.GONE);

                                                            mContext.startActivity(Activity);

                                                            mActivity.finish();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                        loadingIcon.setVisibility(View.GONE);
                                        toastMessageLong(error.getMessage());
                                    }
                                };

                                databaseReferenceCircles.addListenerForSingleValueEvent(eventListenerCircles);
                            } else {

                                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                databaseReferencePolygons = rootRef.child("Polygons");
                                eventListenerPolygons = new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        for (DataSnapshot ds : snapshot.getChildren()) {

                                            String shapeUUID = (String) ds.child("shapeUUID").getValue();
                                            if (shapeUUID != null) {

                                                if (shapeUUID.equals(mShapeUUID.get(getAdapterPosition()))) {

                                                    LatLng marker3Position = null;
                                                    LatLng marker4Position = null;
                                                    LatLng marker5Position = null;
                                                    LatLng marker6Position = null;
                                                    LatLng marker7Position;
                                                    List<LatLng> polygon = new ArrayList<>();

                                                    LatLng marker0Position = new LatLng((double) ds.child("polygonOptions/points/0/latitude/").getValue(), (double) ds.child("polygonOptions/points/0/longitude/").getValue());
                                                    LatLng marker1Position = new LatLng((double) ds.child("polygonOptions/points/1/latitude/").getValue(), (double) ds.child("polygonOptions/points/1/longitude/").getValue());
                                                    LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                                                    if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                        marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                                                    }
                                                    if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                        marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                                                    }
                                                    if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                        marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                                                    }
                                                    if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                        marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                                                    }
                                                    if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                                        marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                                                        polygon.add(marker7Position);
                                                        polygon.add(marker6Position);
                                                        polygon.add(marker5Position);
                                                        polygon.add(marker4Position);
                                                        polygon.add(marker3Position);
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                        polygon.add(marker6Position);
                                                        polygon.add(marker5Position);
                                                        polygon.add(marker4Position);
                                                        polygon.add(marker3Position);
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                        polygon.add(marker5Position);
                                                        polygon.add(marker4Position);
                                                        polygon.add(marker3Position);
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                        polygon.add(marker4Position);
                                                        polygon.add(marker3Position);
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                        polygon.add(marker3Position);
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    } else {
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                        loadingIcon.setVisibility(View.GONE);
                                        toastMessageLong(error.getMessage());
                                    }
                                };

                                databaseReferencePolygons.addListenerForSingleValueEvent(eventListenerPolygons);
                            }
                        }
                    });
                }

                if (playButtonOutside != null) {

                    playButtonOutside.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            loadingIcon.setVisibility(View.VISIBLE);

                            if (mShapeIsCircle.get(getAdapterPosition())) {

                                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                databaseReferenceCircles = rootRef.child("Circles");
                                eventListenerCircles = new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        for (DataSnapshot ds : snapshot.getChildren()) {

                                            String shapeUUID = (String) ds.child("shapeUUID").getValue();
                                            if (shapeUUID != null) {

                                                if (shapeUUID.equals(mShapeUUID.get(getAdapterPosition()))) {

                                                    Double mLatitude = (Double) ds.child("circleOptions").child("center").child("latitude").getValue();
                                                    Double mLongitude = (Double) ds.child("circleOptions").child("center").child("longitude").getValue();
                                                    if (mLatitude != null && mLongitude != null) {

                                                        double mRadius = (double) (long) ds.child("circleOptions").child("radius").getValue();
                                                        if (mRadius != 0) {

                                                            float[] distance = new float[2];

                                                            Location.distanceBetween(mLatitude, mLongitude,
                                                                    userLatitude, userLongitude, distance);

                                                            // Boolean; will be true if user is within the circle upon circle click.
                                                            userIsWithinShape = !(distance[0] > mRadius);

                                                            cancelToasts();

                                                            Intent Activity = new Intent(mContext, Navigation.class);
                                                            Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                            Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                            Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                            loadingIcon.setVisibility(View.GONE);

                                                            mContext.startActivity(Activity);

                                                            mActivity.finish();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                        loadingIcon.setVisibility(View.GONE);
                                        toastMessageLong(error.getMessage());
                                    }
                                };

                                databaseReferenceCircles.addListenerForSingleValueEvent(eventListenerCircles);
                            } else {

                                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                databaseReferencePolygons = rootRef.child("Polygons");
                                eventListenerPolygons = new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        for (DataSnapshot ds : snapshot.getChildren()) {

                                            String shapeUUID = (String) ds.child("shapeUUID").getValue();
                                            if (shapeUUID != null) {

                                                if (shapeUUID.equals(mShapeUUID.get(getAdapterPosition()))) {

                                                    LatLng marker3Position = null;
                                                    LatLng marker4Position = null;
                                                    LatLng marker5Position = null;
                                                    LatLng marker6Position = null;
                                                    LatLng marker7Position;
                                                    List<LatLng> polygon = new ArrayList<>();

                                                    LatLng marker0Position = new LatLng((double) ds.child("polygonOptions/points/0/latitude/").getValue(), (double) ds.child("polygonOptions/points/0/longitude/").getValue());
                                                    LatLng marker1Position = new LatLng((double) ds.child("polygonOptions/points/1/latitude/").getValue(), (double) ds.child("polygonOptions/points/1/longitude/").getValue());
                                                    LatLng marker2Position = new LatLng((double) ds.child("polygonOptions/points/2/latitude/").getValue(), (double) ds.child("polygonOptions/points/2/longitude/").getValue());
                                                    if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                        marker3Position = new LatLng((double) ds.child("polygonOptions/points/3/latitude/").getValue(), (double) ds.child("polygonOptions/points/3/longitude/").getValue());
                                                    }
                                                    if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                        marker4Position = new LatLng((double) ds.child("polygonOptions/points/4/latitude/").getValue(), (double) ds.child("polygonOptions/points/4/longitude/").getValue());
                                                    }
                                                    if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                        marker5Position = new LatLng((double) ds.child("polygonOptions/points/5/latitude/").getValue(), (double) ds.child("polygonOptions/points/5/longitude/").getValue());
                                                    }
                                                    if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                        marker6Position = new LatLng((double) ds.child("polygonOptions/points/6/latitude/").getValue(), (double) ds.child("polygonOptions/points/6/longitude/").getValue());
                                                    }
                                                    if (ds.child("polygonOptions/points/7/latitude/").getValue() != null) {
                                                        marker7Position = new LatLng((double) ds.child("polygonOptions/points/7/latitude/").getValue(), (double) ds.child("polygonOptions/points/7/longitude/").getValue());

                                                        polygon.add(marker7Position);
                                                        polygon.add(marker6Position);
                                                        polygon.add(marker5Position);
                                                        polygon.add(marker4Position);
                                                        polygon.add(marker3Position);
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    } else if (ds.child("polygonOptions/points/6/latitude/").getValue() != null) {
                                                        polygon.add(marker6Position);
                                                        polygon.add(marker5Position);
                                                        polygon.add(marker4Position);
                                                        polygon.add(marker3Position);
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                        polygon.add(marker5Position);
                                                        polygon.add(marker4Position);
                                                        polygon.add(marker3Position);
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                        polygon.add(marker4Position);
                                                        polygon.add(marker3Position);
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                        polygon.add(marker3Position);
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    } else {
                                                        polygon.add(marker2Position);
                                                        polygon.add(marker1Position);
                                                        polygon.add(marker0Position);

                                                        userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                        cancelToasts();

                                                        Intent Activity = new Intent(mContext, Navigation.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);

                                                        mActivity.finish();
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                        loadingIcon.setVisibility(View.GONE);
                                        toastMessageLong(error.getMessage());
                                    }
                                };

                                databaseReferencePolygons.addListenerForSingleValueEvent(eventListenerPolygons);
                            }
                        }
                    });
                }
            }
        }

        DirectMentionsAdapter(Context context, ArrayList<String> mMessageTime, ArrayList<String> mMessageUser, ArrayList<String> mMessageImage, ArrayList<String> mMessageImageVideo, ArrayList<String> mMessageText, ArrayList<String> mShapeUUID, ArrayList<Boolean> mUserIsWithinShape, ArrayList<Boolean> mShapeIsCircle, ArrayList<Long> mPosition, ArrayList<Boolean> mSeenByUser) {

            this.mContext = context;
            this.mMessageTime = mMessageTime;
            this.mMessageUser = mMessageUser;
            this.mMessageImage = mMessageImage;
            this.mMessageImageVideo = mMessageImageVideo;
            this.mMessageText = mMessageText;
            this.mShapeUUID = mShapeUUID;
            this.mUserIsWithinShape = mUserIsWithinShape;
            this.mShapeIsCircle = mShapeIsCircle;
            this.mPosition = mPosition;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(mContext).inflate(R.layout.directmentionsadapterlayout, parent, false);

            loadPreferences();

            Bundle extras = mActivity.getIntent().getExtras();
            if (extras != null) {

                userLatitude = extras.getDouble("userLatitude");
                userLongitude = extras.getDouble("userLongitude");
            } else {

                Log.e(TAG, "DirectMentionsAdapter() -> extras == null");
            }

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

            // Set the left side if the user sent the message from inside the shape.
            if (mUserIsWithinShape.get(position)) {

                holder.messageTimeInside.setText(mMessageTime.get(position));

                holder.messageUserInside.setText(mMessageUser.get(position));

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

                holder.messageUserOutside.setText(mMessageUser.get(position));

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

            if (notSeenByUserList.contains(mPosition.get(position))) {

                if (theme) {

                    holder.itemView.setBackgroundColor(Color.parseColor("#859FFF"));
                } else {

                    holder.itemView.setBackgroundColor(Color.parseColor("#1338BE"));
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

    private void toastMessageLong(String message) {

        longToast = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
        longToast.setGravity(Gravity.CENTER, 0, 0);
        longToast.show();
    }
}