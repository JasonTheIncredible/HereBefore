package co.clixel.herebefore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.ChildEventListener;
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
    private String userEmailFirebase;
    private ArrayList<String> mTime, mUser, mImage, mVideo, mText, mShapeUUID;
    private ArrayList<Boolean> mUserIsWithinShape, mShapeIsCircle, mSeenByUser;
    private ArrayList<Long> mShapeSize, mShapeLat, mShapeLon, datesAL;
    private ArrayList<Integer> mPosition;
    private ArrayList<Pair<String, Integer>> userPositionPairsFromFirebase;
    private RecyclerView DmsRecyclerView;
    private static int index = -1, top = -1, last;
    private ChildEventListener childEventListener;
    private LinearLayoutManager DmsRecyclerViewLinearLayoutManager;
    private boolean firstLoad, loadingOlderMessages, userIsWithinShape, noMoreMessages = false;
    private int latUser, lonUser;
    private View loadingIcon;
    private Toast longToast;
    private Double userLatitude, userLongitude;
    private Context mContext;
    private Activity mActivity;
    private View rootView;
    private TextView noDmsTextView;
    private Query query;

    @Override
    public void onAttach(@NonNull Context context) {

        super.onAttach(context);
        Log.i(TAG, "onAttach()");

        mContext = context;
        mActivity = getActivity();

        userPositionPairsFromFirebase = new ArrayList<>();
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(TAG, "onCreateView()");
        rootView = inflater.inflate(R.layout.directmentions, container, false);

        DmsRecyclerView = rootView.findViewById(R.id.mentionsList);
        loadingIcon = rootView.findViewById(R.id.loadingIcon);
        noDmsTextView = rootView.findViewById(R.id.noDmsTextView);

        DmsRecyclerViewLinearLayoutManager = new LinearLayoutManager(mActivity);

        mTime = new ArrayList<>();
        mUser = new ArrayList<>();
        mImage = new ArrayList<>();
        mVideo = new ArrayList<>();
        mText = new ArrayList<>();
        mShapeUUID = new ArrayList<>();
        mUserIsWithinShape = new ArrayList<>();
        mShapeIsCircle = new ArrayList<>();
        mShapeSize = new ArrayList<>();
        mShapeLat = new ArrayList<>();
        mShapeLon = new ArrayList<>();
        mPosition = new ArrayList<>();
        mSeenByUser = new ArrayList<>();

        datesAL = new ArrayList<>();

        if (mActivity != null) {

            Bundle extras = mActivity.getIntent().getExtras();
            if (extras != null) {

                userLatitude = extras.getDouble("userLatitude");
                userLongitude = extras.getDouble("userLongitude");

                // Get a value with 1 decimal point and use it for Firebase.
                double nearLeftPrecisionLat = Math.pow(10, 1);
                // Can't create a firebase path with '.', so get rid of decimal.
                double nearLeftLatTemp = (int) (nearLeftPrecisionLat * userLatitude) / nearLeftPrecisionLat;
                nearLeftLatTemp *= 10;
                latUser = (int) nearLeftLatTemp;

                double nearLeftPrecisionLon = Math.pow(10, 1);
                // Can't create a firebase path with '.', so get rid of decimal.
                double nearLeftLonTemp = (int) (nearLeftPrecisionLon * userLongitude) / nearLeftPrecisionLon;
                nearLeftLonTemp *= 10;
                lonUser = (int) nearLeftLonTemp;
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

        // Set to true to scroll to the bottom of chatRecyclerView. Also prevents duplicate items in addQuery.
        firstLoad = true;
        loadingOlderMessages = false;

        // If user has a Google account, get email one way. Else, get email another way.
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(mContext);
        String email;
        if (acct != null) {

            email = acct.getEmail();
        } else {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            email = sharedPreferences.getString("userToken", "null");
        }
        // Firebase does not allow ".", so replace them with ",".
        userEmailFirebase = email.replace(".", ",");

        getFirebaseDms(null);

        DmsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {

                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                    // Get the top visible position. If it is (almost) the last loaded item, load more.
                    int firstCompletelyVisibleItemPosition = DmsRecyclerViewLinearLayoutManager.findFirstCompletelyVisibleItemPosition();

                    if (firstCompletelyVisibleItemPosition == 0 && !noMoreMessages) {

                        loadingIcon.setVisibility(View.VISIBLE);
                        loadingOlderMessages = true;
                        getFirebaseDms(datesAL.get(0));
                    }
                }
            }
        });
    }

    private void getFirebaseDms(Long nodeID) {

        Log.i(TAG, "getFirebaseDms()");

        Query query0;

        if (nodeID == null) {

            query0 = FirebaseDatabase.getInstance().getReference()
                    .child("Users").child(userEmailFirebase).child("ReceivedDms")
                    .limitToLast(20);
        } else {

            query0 = FirebaseDatabase.getInstance().getReference()
                    .child("Users").child(userEmailFirebase).child("ReceivedDms")
                    .orderByChild("date")
                    .endAt(nodeID)
                    .limitToLast(20);
        }

        query0.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.getChildrenCount() < 20 && loadingOlderMessages) {

                    noMoreMessages = true;
                }

                int i = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {

                    datesAL.add(i, (Long) ds.child("date").getValue());

                    for (DataSnapshot dss : ds.child("userPositionPairs").getChildren()) {

                        Pair<String, Integer> pair = new Pair<>((String) dss.child("first").getValue(), (int) (long) dss.child("second").getValue());
                        if (!userPositionPairsFromFirebase.contains(pair)) {

                            userPositionPairsFromFirebase.add(pair);
                        }
                    }

                    Long serverDate = (Long) ds.child("date").getValue();
                    String user = (String) ds.child("userUUID").getValue();
                    String imageUrl = (String) ds.child("imageUrl").getValue();
                    String videoUrl = (String) ds.child("videoUrl").getValue();
                    String messageText = (String) ds.child("message").getValue();

                    // Truncate mentions from Firebase.
                    String replacedMessageText = null;
                    if (!userPositionPairsFromFirebase.isEmpty()) {

                        for (Pair<String, Integer> pair : userPositionPairsFromFirebase) {

                            if (replacedMessageText != null) {

                                if (replacedMessageText.contains(pair.first)) {

                                    replacedMessageText = replacedMessageText.replace(pair.first, pair.first.substring(0, 10) + "...");
                                }
                            } else {

                                if (messageText.contains(pair.first)) {

                                    replacedMessageText = messageText.replace(pair.first, pair.first.substring(0, 10) + "...");
                                }
                            }
                        }
                    }

                    String shapeUUID = (String) ds.child("shapeUUID").getValue();
                    Boolean userIsWithinShape = (Boolean) ds.child("userIsWithinShape").getValue();
                    Boolean shapeIsCircle = (Boolean) ds.child("shapeIsCircle").getValue();
                    Long size = (Long) ds.child("size").getValue();
                    Long lat = (Long) ds.child("lat").getValue();
                    Long lon = (Long) ds.child("lon").getValue();
                    Integer position = ((Long) ds.child("position").getValue()).intValue();
                    Boolean seenByUser = (Boolean) ds.child("seenByUser").getValue();
                    DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                    // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                    // This will cause onDataChange to fire twice; optimizations could be made in the future.
                    if (serverDate != null) {

                        Date netDate = (new Date(serverDate));
                        String messageTime = dateFormat.format(netDate);
                        mTime.add(i, messageTime);
                    } else {

                        Log.e(TAG, "fillRecyclerView() -> serverDate == null");
                    }
                    mUser.add(i, user);
                    mImage.add(i, imageUrl);
                    mVideo.add(i, videoUrl);

                    if (replacedMessageText != null) {

                        mText.add(i, replacedMessageText);
                    } else {

                        mText.add(i, messageText);
                    }

                    mShapeUUID.add(i, shapeUUID);
                    mUserIsWithinShape.add(i, userIsWithinShape);
                    mShapeIsCircle.add(i, shapeIsCircle);
                    mShapeSize.add(i, size);
                    mShapeLat.add(i, lat);
                    mShapeLon.add(i, lon);
                    mPosition.add(i, position);
                    mSeenByUser.add(i, seenByUser);
                    i++;

                    // Prevent duplicates.
                    if (i == snapshot.getChildrenCount() - 1 && !firstLoad) {

                        break;
                    }
                }

                // Read RecyclerView scroll position (for use in initDirectMentionsAdapter())
                if (DmsRecyclerViewLinearLayoutManager != null) {

                    index = DmsRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
                    last = DmsRecyclerViewLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                    View v = DmsRecyclerView.getChildAt(0);
                    top = (v == null) ? 0 : (v.getTop() - DmsRecyclerView.getPaddingTop());
                }

                // Prevents crash when user toggles between light / dark mode.
                if (DmsRecyclerView != null) {

                    if (DmsRecyclerView.getAdapter() != null && loadingOlderMessages) {

                        DmsRecyclerView.getAdapter().notifyItemRangeInserted(0, (int) snapshot.getChildrenCount() - 1);
                        loadingIcon.setVisibility(View.GONE);
                    }
                }

                if (!loadingOlderMessages) {

                    addQuery();
                }

                loadingOlderMessages = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                toastMessageLong(databaseError.getMessage());
            }
        });
    }

    // Change to .limitToLast(1) to cut down on data usage. Otherwise, EVERY child at this node will be downloaded every time the child is updated.
    private void addQuery() {

        // This prevents duplicates when loading into Settings fragment then switched back into Chat (as onStop is never called but onStart is called).
        if (query != null) {

            query.removeEventListener(childEventListener);
        }

        // If this is the first time calling this eventListener and it's a new shape, initialize the adapter (but don't return, as the childEventListener should still be added), as onChildAdded won't be called the first time.
        if (firstLoad && mUser.size() == 0) {

            initDirectMentionsAdapter();
        }

        // Add new values to arrayLists one at a time. This prevents the need to download the whole dataSnapshot every time this information is needed.
        query = FirebaseDatabase.getInstance().getReference().child("Users").child(userEmailFirebase).child("ReceivedDms").limitToLast(1);
        childEventListener = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.i(TAG, "addQuery()");

                // If this is the first time calling this eventListener, prevent double posts (as onStart() already added the last item).
                if (firstLoad) {

                    initDirectMentionsAdapter();
                    return;
                }

                // Read RecyclerView scroll position (for use in initDirectMentionsAdapter()).
                if (DmsRecyclerViewLinearLayoutManager != null) {

                    index = DmsRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
                    last = DmsRecyclerViewLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                    View v = DmsRecyclerView.getChildAt(0);
                    top = (v == null) ? 0 : (v.getTop() - DmsRecyclerView.getPaddingTop());
                }

                for (DataSnapshot dss : snapshot.child("userPositionPairs").getChildren()) {

                    Pair<String, Integer> pair = new Pair<>((String) dss.child("first").getValue(), (int) (long) dss.child("second").getValue());
                    if (!userPositionPairsFromFirebase.contains(pair)) {

                        userPositionPairsFromFirebase.add(pair);
                    }
                }

                Long serverDate = (Long) snapshot.child("date").getValue();
                String user = (String) snapshot.child("userUUID").getValue();
                String imageUrl = (String) snapshot.child("imageUrl").getValue();
                String videoUrl = (String) snapshot.child("videoUrl").getValue();
                String messageText = (String) snapshot.child("message").getValue();

                // Truncate mentions from Firebase.
                String replacedMessageText = null;
                if (!userPositionPairsFromFirebase.isEmpty()) {

                    for (Pair<String, Integer> pair : userPositionPairsFromFirebase) {

                        if (replacedMessageText != null) {

                            if (replacedMessageText.contains(pair.first)) {

                                replacedMessageText = replacedMessageText.replace(pair.first, pair.first.substring(0, 10) + "...");
                            }
                        } else {

                            if (messageText.contains(pair.first)) {

                                replacedMessageText = messageText.replace(pair.first, pair.first.substring(0, 10) + "...");
                            }
                        }
                    }
                }

                String shapeUUID = (String) snapshot.child("shapeUUID").getValue();
                Boolean userIsWithinShape = (Boolean) snapshot.child("userIsWithinShape").getValue();
                Boolean shapeIsCircle = (Boolean) snapshot.child("shapeIsCircle").getValue();
                Long size = (Long) snapshot.child("size").getValue();
                Long lat = (Long) snapshot.child("lat").getValue();
                Long lon = (Long) snapshot.child("lon").getValue();
                Integer position = ((Long) snapshot.child("position").getValue()).intValue();
                Boolean seenByUser = (Boolean) snapshot.child("seenByUser").getValue();
                DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                // This will cause onDataChange to fire twice; optimizations could be made in the future.
                if (serverDate != null) {

                    Date netDate = (new Date(serverDate));
                    String messageTime = dateFormat.format(netDate);
                    mTime.add(messageTime);
                } else {

                    Log.e(TAG, "fillRecyclerView() -> serverDate == null");
                }
                mUser.add(user);
                mImage.add(imageUrl);
                mVideo.add(videoUrl);

                if (replacedMessageText != null) {

                    mText.add(replacedMessageText);
                } else {

                    mText.add(messageText);
                }

                mShapeUUID.add(shapeUUID);
                mUserIsWithinShape.add(userIsWithinShape);
                mShapeIsCircle.add(shapeIsCircle);
                mShapeSize.add(size);
                mShapeLat.add(lat);
                mShapeLon.add(lon);
                mPosition.add(position);
                mSeenByUser.add(seenByUser);

                initDirectMentionsAdapter();
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

    private void initDirectMentionsAdapter() {

        // Initialize the RecyclerView.
        Log.i(TAG, "initDirectMentionsAdapter()");

        // Prevents crash when user toggles between light / dark mode.
        if (DmsRecyclerView == null) {

            return;
        }

        DirectMentionsAdapter adapter = new DirectMentionsAdapter(mContext, mTime, mUser, mImage, mVideo, mText, mShapeUUID, mUserIsWithinShape, mShapeIsCircle, mShapeSize, mShapeLat, mShapeLon, mPosition, mSeenByUser);
        DmsRecyclerView.setAdapter(adapter);
        DmsRecyclerView.setHasFixedSize(true);
        DmsRecyclerView.setLayoutManager(DmsRecyclerViewLinearLayoutManager);

        if (last == (mTime.size() - 2) || firstLoad) {

            // Scroll to bottom of recyclerviewlayout after first initialization and after sending a recyclerviewlayout.
            DmsRecyclerView.scrollToPosition(mTime.size() - 1);
        } else {

            // Set RecyclerView scroll position to prevent position change when Firebase gets updated and after screen orientation change.
            DmsRecyclerViewLinearLayoutManager.scrollToPositionWithOffset(index, top);
        }

        // After the initial load, make the loadingIcon invisible.
        if (loadingIcon != null) {

            loadingIcon.setVisibility(View.GONE);
        }

        if (mUser.size() == 0) {

            noDmsTextView.setVisibility(View.VISIBLE);
        } else {

            noDmsTextView.setVisibility(View.GONE);
        }

        firstLoad = false;
    }

    @Override
    public void onStop() {

        Log.i(TAG, "onStop()");

        if (query != null) {

            query.removeEventListener(childEventListener);
        }

        if (DmsRecyclerView != null) {

            DmsRecyclerView.clearOnScrollListeners();
            DmsRecyclerView.setAdapter(null);
        }

        cancelToasts();

        super.onStop();
    }

    @Override
    public void onDestroyView() {

        Log.i(TAG, "onDestroyView()");

        if (DmsRecyclerView != null) {

            DmsRecyclerView = null;
        }

        if (loadingIcon != null) {

            loadingIcon = null;
        }

        if (noDmsTextView != null) {

            noDmsTextView = null;
        }

        if (DmsRecyclerViewLinearLayoutManager != null) {

            DmsRecyclerViewLinearLayoutManager = null;
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

    private void cancelToasts() {

        if (longToast != null) {

            longToast.cancel();
        }
    }

    public class DirectMentionsAdapter extends RecyclerView.Adapter<DirectMentionsAdapter.ViewHolder> {

        private final Context mContext;
        private final ArrayList<String> mMessageTime, mMessageUser, mMessageImage, mMessageImageVideo, mMessageText, mShapeUUID;
        private final ArrayList<Boolean> mUserIsWithinShape, mShapeIsCircle, mSeenByUser;
        private final ArrayList<Long> mShapeSize, mShapeLat, mShapeLon;
        private final ArrayList<Integer> mPosition;
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

                itemView.setOnClickListener(v -> {

                    loadingIcon.setVisibility(View.VISIBLE);

                    // When user clicks on a DM, set "seenByUser" to false so it is not highlighted in the future.
                    if (!mSeenByUser.get(getAdapterPosition())) {

                        DatabaseReference Dms = FirebaseDatabase.getInstance().getReference().child("Users").child(userEmailFirebase).child("ReceivedDms");
                        Dms.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                for (DataSnapshot ds : snapshot.getChildren()) {

                                    String userUUID = (String) ds.child("userUUID").getValue();

                                    if (userUUID != null) {

                                        if (userUUID.equals(mMessageUser.get(getAdapterPosition()))) {

                                            if (!(Boolean) ds.child("seenByUser").getValue()) {

                                                ds.child("seenByUser").getRef().setValue(true);
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
                    }

                    DatabaseReference shape = null;
                    double shapeSize = mShapeSize.get(getAdapterPosition());

                    if (mShapeIsCircle.get(getAdapterPosition())) {

                        if (shapeSize == 1) {

                            shape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latUser + ", " + lonUser + ")").child("Point");
                        } else if (1 < shapeSize && shapeSize <= 10) {

                            shape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latUser + ", " + lonUser + ")").child("Small");
                        } else if (10 < shapeSize && shapeSize <= 50) {

                            shape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latUser + ", " + lonUser + ")").child("Medium");
                        } else if (50 < shapeSize) {

                            shape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latUser + ", " + lonUser + ")").child("Large");
                        }

                        shape.addListenerForSingleValueEvent(new ValueEventListener() {

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
                                                    Activity.putExtra("radius", mShapeSize.get(getAdapterPosition()).doubleValue());
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                                    Activity.putExtra("shapeLat", mShapeLat.get(getAdapterPosition()).intValue());
                                                    Activity.putExtra("shapeLon", mShapeLon.get(getAdapterPosition()).intValue());
                                                    Activity.putExtra("userLatitude", userLatitude);
                                                    Activity.putExtra("userLongitude", userLongitude);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);

                                                    mActivity.finish();
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }

                                // If this part is reached, the user is not within the shape because the user's location is not in the same loadable map area as the shape.
                                cancelToasts();

                                Intent Activity = new Intent(mContext, Navigation.class);
                                Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                Activity.putExtra("radius", mShapeSize.get(getAdapterPosition()).doubleValue());
                                Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                Activity.putExtra("userIsWithinShape", false);
                                Activity.putExtra("shapeLat", mShapeLat.get(getAdapterPosition()).intValue());
                                Activity.putExtra("shapeLon", mShapeLon.get(getAdapterPosition()).intValue());
                                Activity.putExtra("userLatitude", userLatitude);
                                Activity.putExtra("userLongitude", userLongitude);

                                loadingIcon.setVisibility(View.GONE);

                                mContext.startActivity(Activity);

                                mActivity.finish();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(error.getMessage());
                            }
                        });
                    } else {

                        // Shape is not a circle.

                        if (shapeSize <= Math.PI * (Math.pow(10, 2))) {

                            shape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latUser + ", " + lonUser + ")").child("Small");
                        } else if (Math.PI * (Math.pow(10, 2)) < shapeSize && shapeSize <= Math.PI * (Math.pow(50, 2))) {

                            shape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latUser + ", " + lonUser + ")").child("Medium");
                        } else if (Math.PI * (Math.pow(50, 2)) < shapeSize) {

                            shape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + latUser + ", " + lonUser + ")").child("Large");
                        }

                        shape.addListenerForSingleValueEvent(new ValueEventListener() {

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
                                                Activity.putExtra("polygonArea", mShapeSize.get(getAdapterPosition()).doubleValue());
                                                Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                                Activity.putExtra("shapeLat", mShapeLat.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("shapeLon", mShapeLon.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("userLatitude", userLatitude);
                                                Activity.putExtra("userLongitude", userLongitude);

                                                loadingIcon.setVisibility(View.GONE);

                                                mContext.startActivity(Activity);

                                                mActivity.finish();
                                                return;
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
                                                Activity.putExtra("polygonArea", mShapeSize.get(getAdapterPosition()).doubleValue());
                                                Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                                Activity.putExtra("shapeLat", mShapeLat.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("shapeLon", mShapeLon.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("userLatitude", userLatitude);
                                                Activity.putExtra("userLongitude", userLongitude);

                                                loadingIcon.setVisibility(View.GONE);

                                                mContext.startActivity(Activity);

                                                mActivity.finish();
                                                return;
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
                                                Activity.putExtra("polygonArea", mShapeSize.get(getAdapterPosition()).doubleValue());
                                                Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                                Activity.putExtra("shapeLat", mShapeLat.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("shapeLon", mShapeLon.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("userLatitude", userLatitude);
                                                Activity.putExtra("userLongitude", userLongitude);

                                                loadingIcon.setVisibility(View.GONE);

                                                mContext.startActivity(Activity);

                                                mActivity.finish();
                                                return;
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
                                                Activity.putExtra("polygonArea", mShapeSize.get(getAdapterPosition()).doubleValue());
                                                Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                                Activity.putExtra("shapeLat", mShapeLat.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("shapeLon", mShapeLon.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("userLatitude", userLatitude);
                                                Activity.putExtra("userLongitude", userLongitude);

                                                loadingIcon.setVisibility(View.GONE);

                                                mContext.startActivity(Activity);

                                                mActivity.finish();
                                                return;
                                            } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                polygon.add(marker3Position);
                                                polygon.add(marker2Position);
                                                polygon.add(marker1Position);
                                                polygon.add(marker0Position);

                                                userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                cancelToasts();

                                                Intent Activity = new Intent(mContext, Navigation.class);
                                                Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                Activity.putExtra("polygonArea", mShapeSize.get(getAdapterPosition()).doubleValue());
                                                Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                                Activity.putExtra("shapeLat", mShapeLat.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("shapeLon", mShapeLon.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("userLatitude", userLatitude);
                                                Activity.putExtra("userLongitude", userLongitude);

                                                loadingIcon.setVisibility(View.GONE);

                                                mContext.startActivity(Activity);

                                                mActivity.finish();
                                                return;
                                            } else {
                                                polygon.add(marker2Position);
                                                polygon.add(marker1Position);
                                                polygon.add(marker0Position);

                                                userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                cancelToasts();

                                                Intent Activity = new Intent(mContext, Navigation.class);
                                                Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                Activity.putExtra("polygonArea", mShapeSize.get(getAdapterPosition()).doubleValue());
                                                Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                                Activity.putExtra("shapeLat", mShapeLat.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("shapeLon", mShapeLon.get(getAdapterPosition()).intValue());
                                                Activity.putExtra("userLatitude", userLatitude);
                                                Activity.putExtra("userLongitude", userLongitude);

                                                loadingIcon.setVisibility(View.GONE);

                                                mContext.startActivity(Activity);

                                                mActivity.finish();
                                                return;
                                            }
                                        }
                                    }
                                }

                                // If this part is reached, the user is not within the shape because the user's location is not in the same loadable map area as the shape.
                                cancelToasts();

                                Intent Activity = new Intent(mContext, Navigation.class);
                                Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                Activity.putExtra("radius", mShapeSize.get(getAdapterPosition()).doubleValue());
                                Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                Activity.putExtra("userIsWithinShape", false);
                                Activity.putExtra("shapeLat", mShapeLat.get(getAdapterPosition()).intValue());
                                Activity.putExtra("shapeLon", mShapeLon.get(getAdapterPosition()).intValue());
                                Activity.putExtra("userLatitude", userLatitude);
                                Activity.putExtra("userLongitude", userLongitude);

                                loadingIcon.setVisibility(View.GONE);

                                mContext.startActivity(Activity);

                                mActivity.finish();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(error.getMessage());
                            }
                        });
                    }
                });

                if (messageImageInside != null) {

                    messageImageInside.setOnClickListener(view -> {

                        cancelToasts();

                        Intent Activity = new Intent(mContext, PhotoView.class);
                        Activity.putExtra("imgUrl", mImage.get(getAdapterPosition()));

                        mContext.startActivity(Activity);
                    });
                }

                if (messageImageOutside != null) {

                    messageImageOutside.setOnClickListener(view -> {

                        cancelToasts();

                        Intent Activity = new Intent(mContext, PhotoView.class);
                        Activity.putExtra("imgUrl", mImage.get(getAdapterPosition()));

                        mContext.startActivity(Activity);
                    });
                }

                if (playButtonInside != null) {

                    playButtonInside.setOnClickListener(v -> {

                        cancelToasts();

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoUrl", mVideo.get(getAdapterPosition()));

                        mContext.startActivity(Activity);
                    });
                }

                if (playButtonOutside != null) {

                    playButtonOutside.setOnClickListener(v -> {

                        cancelToasts();

                        Intent Activity = new Intent(mContext, VideoView.class);
                        Activity.putExtra("videoUrl", mVideo.get(getAdapterPosition()));

                        mContext.startActivity(Activity);
                    });
                }
            }
        }

        DirectMentionsAdapter(Context context, ArrayList<String> mMessageTime, ArrayList<String> mMessageUser, ArrayList<String> mMessageImage, ArrayList<String> mMessageImageVideo, ArrayList<String> mMessageText, ArrayList<String> mShapeUUID, ArrayList<Boolean> mUserIsWithinShape, ArrayList<Boolean> mShapeIsCircle, ArrayList<Long> mShapeSize, ArrayList<Long> mShapeLat, ArrayList<Long> mShapeLon, ArrayList<Integer> mPosition, ArrayList<Boolean> mSeenByUser) {

            this.mContext = context;
            this.mMessageTime = mMessageTime;
            this.mMessageUser = mMessageUser;
            this.mMessageImage = mMessageImage;
            this.mMessageImageVideo = mMessageImageVideo;
            this.mMessageText = mMessageText;
            this.mShapeUUID = mShapeUUID;
            this.mUserIsWithinShape = mUserIsWithinShape;
            this.mShapeIsCircle = mShapeIsCircle;
            this.mShapeSize = mShapeSize;
            this.mShapeLat = mShapeLat;
            this.mShapeLon = mShapeLon;
            this.mPosition = mPosition;
            this.mSeenByUser = mSeenByUser;
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

            if (!mSeenByUser.get(position)) {

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

        cancelToasts();
        longToast = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
        longToast.setGravity(Gravity.CENTER, 0, 0);
        longToast.show();
    }
}