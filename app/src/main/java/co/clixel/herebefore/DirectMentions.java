package co.clixel.herebefore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.text.DateFormat.getDateTimeInstance;

public class DirectMentions extends Fragment {

    private static final String TAG = "DirectMentions";
    private String firebaseUid;
    private ArrayList<String> mTime, mUser, mImage, mVideo, mText, mShapeUUID;
    private ArrayList<Boolean> mUserIsWithinShape, mSeenByUser;
    private ArrayList<Double> mShapeLat, mShapeLon;
    private ArrayList<String> circleUUIDsAL = new ArrayList<>();
    private ArrayList<LatLng> circleCentersAL = new ArrayList<>();
    private RecyclerView dmsRecyclerView;
    private static int index = -1, top = -1;
    private ChildEventListener childEventListener;
    private LinearLayoutManager dmsRecyclerViewLinearLayoutManager;
    private boolean firstLoad, loadingOlderMessages, noMoreMessages = false, reachedEndOfRecyclerView = true;
    private View loadingIcon;
    private Toast longToast;
    private Integer UUIDDatesPairsSize;
    private Context mContext;
    private Activity mActivity;
    private View rootView;
    private TextView noDmsTextView;
    private Query query;
    private List<Pair<String, Long>> UUIDDatesPairs;

    @Override
    public void onAttach(@NonNull Context context) {

        super.onAttach(context);
        Log.i(TAG, "onAttach()");

        mContext = context;
        mActivity = getActivity();
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(TAG, "onCreateView()");
        rootView = inflater.inflate(R.layout.directmentions, container, false);

        dmsRecyclerView = rootView.findViewById(R.id.mentionsList);
        loadingIcon = rootView.findViewById(R.id.loadingIcon);
        noDmsTextView = rootView.findViewById(R.id.noDmsTextView);

        dmsRecyclerViewLinearLayoutManager = new LinearLayoutManager(mActivity);

        if (UUIDDatesPairsSize == null) {

            mTime = new ArrayList<>();
            mUser = new ArrayList<>();
            mImage = new ArrayList<>();
            mVideo = new ArrayList<>();
            mText = new ArrayList<>();
            mShapeUUID = new ArrayList<>();
            mUserIsWithinShape = new ArrayList<>();
            mShapeLat = new ArrayList<>();
            mShapeLon = new ArrayList<>();
            mSeenByUser = new ArrayList<>();

            UUIDDatesPairs = new ArrayList<>();
        }

        if (mActivity != null) {

            Bundle extras = mActivity.getIntent().getExtras();
            if (extras != null) {

                //noinspection unchecked
                circleUUIDsAL = (ArrayList<String>) extras.getSerializable("circleUUIDsAL");
                //noinspection unchecked
                circleCentersAL = (ArrayList<LatLng>) extras.getSerializable("circleCentersAL");
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

        firebaseUid = FirebaseAuth.getInstance().getUid();

        // If the string doesn't equal null, check if the latest user is the same as the one in the recyclerView. If string is null, it's the first time loading.
        if (UUIDDatesPairsSize != null) {

            Query query = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("ReceivedDms").limitToLast(1);
            query.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    // User reloaded app but user has no DMs.
                    if (snapshot.getChildrenCount() == 0) {

                        loadingIcon.setVisibility(View.GONE);
                        noDmsTextView.setVisibility(View.VISIBLE);
                        addQuery();
                        return;
                    }

                    for (DataSnapshot ds : snapshot.getChildren()) {

                        Long date = (Long) ds.child("date").getValue();
                        // If the saved date and the latest date match, then there's no need to re-download everything from Firebase.
                        if (date != null && UUIDDatesPairs != null && !date.equals(UUIDDatesPairs.get(UUIDDatesPairs.size() - 1).second)) {

                            Log.i(TAG, "onStart() -> new DMs since app restarted");

                            getFirebaseDms(null);
                        } else {

                            Log.i(TAG, "onStart() -> no new DMs");

                            addQuery();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        } else {

            getFirebaseDms(null);
        }

        dmsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {

                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {

                    reachedEndOfRecyclerView = false;
                }

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                    // Get the top visible position. If it is (almost) the last loaded item, load more.
                    int firstCompletelyVisibleItemPosition = dmsRecyclerViewLinearLayoutManager.findFirstCompletelyVisibleItemPosition();

                    if (firstCompletelyVisibleItemPosition == 0 && !noMoreMessages) {

                        loadingIcon.setVisibility(View.VISIBLE);
                        loadingOlderMessages = true;
                        getFirebaseDms(UUIDDatesPairs.get(0).second);
                    }

                    // If RecyclerView can't be scrolled down, reachedEndOfRecyclerView = true.
                    reachedEndOfRecyclerView = !recyclerView.canScrollVertically(1);
                }
            }
        });
    }

    private void getFirebaseDms(Long nodeID) {

        Log.i(TAG, "getFirebaseDms()");

        Query query;
        if (UUIDDatesPairsSize != null) {

            query = FirebaseDatabase.getInstance().getReference()
                    .child("Users").child(firebaseUid).child("ReceivedDms")
                    .orderByChild("date")
                    .startAt(UUIDDatesPairs.get(UUIDDatesPairsSize).second);
        } else if (nodeID == null) {

            query = FirebaseDatabase.getInstance().getReference()
                    .child("Users").child(firebaseUid).child("ReceivedDms")
                    .limitToLast(20);
        } else {

            query = FirebaseDatabase.getInstance().getReference()
                    .child("Users").child(firebaseUid).child("ReceivedDms")
                    .orderByChild("date")
                    .endAt(nodeID)
                    .limitToLast(20);
        }

        fillRecyclerView(query);
    }

    private void fillRecyclerView(Query query) {

        Log.i(TAG, "fillRecyclerView()");

        query.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (UUIDDatesPairsSize == null && snapshot.getChildrenCount() < 20) {

                    noMoreMessages = true;
                }

                int i;
                if (!loadingOlderMessages && UUIDDatesPairs != null) {

                    i = UUIDDatesPairs.size();
                } else {

                    i = 0;
                }

                for (DataSnapshot ds : snapshot.getChildren()) {

                    String user = (String) ds.child("userUUID").getValue();

                    // Prevents duplicates during getFirebaseDMs.
                    if (mUser.contains(user)) {

                        continue;
                    }

                    Long serverDate = (Long) ds.child("date").getValue();

                    UUIDDatesPairs.add(i, new Pair<>(user, serverDate));

                    String imageUrl = (String) ds.child("imageUrl").getValue();
                    String videoUrl = (String) ds.child("videoUrl").getValue();
                    String messageText = (String) ds.child("message").getValue();

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
                    if (!possibleMentions.isEmpty()) {

                        for (String possibleMention : possibleMentions) {

                            // The "else" loop will go first - it will create replacedMessageText with truncated mentions and then replacedMessageText will continue to truncate mentions within itself.
                            String replacement = possibleMention.substring(0, 10) + "...";
                            if (replacedMessageText != null) {

                                replacedMessageText = replacedMessageText.replace(possibleMention, replacement);
                            } else {

                                replacedMessageText = messageText.replace(possibleMention, replacement);
                            }
                        }
                    }

                    String shapeUUID = (String) ds.child("shapeUUID").getValue();
                    Boolean userIsWithinShape = (Boolean) ds.child("userIsWithinShape").getValue();
                    Double lat = (Double) ds.child("lat").getValue();
                    Double lon = (Double) ds.child("lon").getValue();
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
                    mShapeLat.add(i, lat);
                    mShapeLon.add(i, lon);
                    mSeenByUser.add(i, seenByUser);
                    i++;

                    // Prevent duplicates.
                    if (i == snapshot.getChildrenCount() - 1 && !firstLoad) {

                        break;
                    }
                }

                // Prevents crash when user toggles between light / dark mode.
                if (dmsRecyclerView != null) {

                    if (dmsRecyclerView.getAdapter() != null && loadingOlderMessages) {

                        dmsRecyclerView.getAdapter().notifyItemRangeInserted(0, (int) snapshot.getChildrenCount() - 1);
                        loadingIcon.setVisibility(View.GONE);
                    }
                }

                if (!loadingOlderMessages) {

                    addQuery();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                showMessageLong(databaseError.getMessage());
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
        query = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("ReceivedDms").limitToLast(1);
        childEventListener = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.i(TAG, "addQuery() -> onChildAdded()");

                // If this is the first time calling this eventListener, prevent double posts (as onStart() already added the last item).
                if (firstLoad) {

                    initDirectMentionsAdapter();
                    return;
                }

                // Read RecyclerView scroll position (for use in initChatAdapter to prevent scrolling after recyclerView gets updated by another user).
                if (dmsRecyclerViewLinearLayoutManager != null && dmsRecyclerView != null) {

                    index = dmsRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
                    View v = dmsRecyclerView.getChildAt(0);
                    top = (v == null) ? 0 : (v.getTop() - dmsRecyclerView.getPaddingTop());
                }

                String user = (String) snapshot.child("userUUID").getValue();
                Long serverDate = (Long) snapshot.child("date").getValue();

                UUIDDatesPairs.add(new Pair<>(user, serverDate));

                String imageUrl = (String) snapshot.child("imageUrl").getValue();
                String videoUrl = (String) snapshot.child("videoUrl").getValue();
                String messageText = (String) snapshot.child("message").getValue();

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
                if (!possibleMentions.isEmpty()) {

                    for (String possibleMention : possibleMentions) {

                        // The "else" loop will go first - it will create replacedMessageText with truncated mentions and then replacedMessageText will continue to truncate mentions within itself.
                        String replacement = possibleMention.substring(0, 10) + "...";
                        if (replacedMessageText != null) {

                            replacedMessageText = replacedMessageText.replace(possibleMention, replacement);
                        } else {

                            replacedMessageText = messageText.replace(possibleMention, replacement);
                        }
                    }
                }

                String shapeUUID = (String) snapshot.child("shapeUUID").getValue();
                Boolean userIsWithinShape = (Boolean) snapshot.child("userIsWithinShape").getValue();
                Double lat = (Double) snapshot.child("lat").getValue();
                Double lon = (Double) snapshot.child("lon").getValue();
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
                mShapeLat.add(lat);
                mShapeLon.add(lon);
                mSeenByUser.add(seenByUser);

                initDirectMentionsAdapter();
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

        query.addChildEventListener(childEventListener);
    }

    private void initDirectMentionsAdapter() {

        // Initialize the RecyclerView.
        Log.i(TAG, "initDirectMentionsAdapter()");

        // Prevents crash when user toggles between light / dark mode.
        if (dmsRecyclerView == null) {

            return;
        }

        DirectMentionsAdapter adapter = new DirectMentionsAdapter(mContext, mTime, mUser, mImage, mVideo, mText, mShapeUUID, mUserIsWithinShape, mShapeLat, mShapeLon, mSeenByUser);
        dmsRecyclerView.setAdapter(adapter);
        dmsRecyclerView.setHasFixedSize(true);
        dmsRecyclerView.setLayoutManager(dmsRecyclerViewLinearLayoutManager);

        if (firstLoad && UUIDDatesPairsSize == null || reachedEndOfRecyclerView) {

            // Scroll to bottom of recyclerviewlayout after first initialization and after sending a recyclerviewlayout.
            dmsRecyclerView.scrollToPosition(mTime.size() - 1);
        } else {

            // Set RecyclerView scroll position to prevent position change when Firebase gets updated and after screen orientation change.
            dmsRecyclerViewLinearLayoutManager.scrollToPositionWithOffset(index, top);
        }

        firstLoad = false;
        loadingOlderMessages = false;
        // Need to make UUIDDatesPairsSize null so user can load older messages after restarting the app.
        UUIDDatesPairsSize = null;

        // After the initial load, make the loadingIcon invisible.
        if (loadingIcon != null) {

            loadingIcon.setVisibility(View.GONE);
        }

        if (mUser.size() == 0) {

            noDmsTextView.setVisibility(View.VISIBLE);
        } else {

            noDmsTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStop() {

        Log.i(TAG, "onStop()");

        // Read RecyclerView scroll position (for use in initChatAdapter if user reload the activity).
        if (dmsRecyclerViewLinearLayoutManager != null && dmsRecyclerView != null) {

            index = dmsRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
            View v = dmsRecyclerView.getChildAt(0);
            top = (v == null) ? 0 : (v.getTop() - dmsRecyclerView.getPaddingTop());
        }

        UUIDDatesPairsSize = UUIDDatesPairs.size() - 1;

        if (query != null) {

            query.removeEventListener(childEventListener);
        }

        if (dmsRecyclerView != null) {

            dmsRecyclerView.clearOnScrollListeners();
            dmsRecyclerView.setAdapter(null);
        }

        cancelToasts();

        super.onStop();
    }

    @Override
    public void onDestroyView() {

        Log.i(TAG, "onDestroyView()");

        if (dmsRecyclerView != null) {

            dmsRecyclerView = null;
        }

        if (loadingIcon != null) {

            loadingIcon = null;
        }

        if (noDmsTextView != null) {

            noDmsTextView = null;
        }

        if (dmsRecyclerViewLinearLayoutManager != null) {

            dmsRecyclerViewLinearLayoutManager = null;
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
        private final ArrayList<Boolean> mUserIsWithinShape, mSeenByUser;
        private final ArrayList<Double> mShapeLat, mShapeLon;
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

                itemView.setOnClickListener(v -> {

                    loadingIcon.setVisibility(View.VISIBLE);

                    // When user clicks on a DM, set "seenByUser" to false so it is not highlighted in the future.
                    if (!mSeenByUser.get(getAdapterPosition())) {

                        DatabaseReference Dms = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("ReceivedDms");
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

                                showMessageLong(error.getMessage());
                            }
                        });
                    }

                    // Get a value with 1 decimal point and use it for Firebase.
                    double nearLeftPrecisionLat = Math.pow(10, 1);
                    // Can't create a firebase path with '.', so get rid of decimal.
                    double nearLeftLatTemp = (int) (nearLeftPrecisionLat * mShapeLat.get(getAdapterPosition())) / nearLeftPrecisionLat;
                    nearLeftLatTemp *= 10;
                    int shapeLatInt = (int) nearLeftLatTemp;

                    double nearLeftPrecisionLon = Math.pow(10, 1);
                    // Can't create a firebase path with '.', so get rid of decimal.
                    double nearLeftLonTemp = (int) (nearLeftPrecisionLon * mShapeLon.get(getAdapterPosition())) / nearLeftPrecisionLon;
                    nearLeftLonTemp *= 10;
                    int shapeLonInt = (int) nearLeftLonTemp;

                    DatabaseReference shape = FirebaseDatabase.getInstance().getReference().child("Shapes").child("(" + shapeLatInt + ", " + shapeLonInt + ")").child("Points");
                    shape.addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            for (DataSnapshot ds : snapshot.getChildren()) {

                                String shapeUUID = (String) ds.child("shapeUUID").getValue();
                                if (shapeUUID != null) {

                                    if (shapeUUID.equals(mShapeUUID.get(getAdapterPosition()))) {

                                        cancelToasts();

                                        Intent Activity = new Intent(mContext, Navigation.class);
                                        Activity.putExtra("shapeLat", mShapeLat.get(getAdapterPosition()));
                                        Activity.putExtra("shapeLon", mShapeLon.get(getAdapterPosition()));
                                        Activity.putExtra("newShape", false);
                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                        Activity.putExtra("UUIDToHighlight", mUser.get(getAdapterPosition()));
                                        Activity.putExtra("circleUUIDsAL", circleUUIDsAL);
                                        Activity.putExtra("circleCentersAL", circleCentersAL);

                                        loadingIcon.setVisibility(View.GONE);

                                        mContext.startActivity(Activity);

                                        mActivity.finish();
                                        return;
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                            loadingIcon.setVisibility(View.GONE);
                            showMessageLong(error.getMessage());
                        }
                    });
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

        DirectMentionsAdapter(Context context, ArrayList<String> mMessageTime, ArrayList<String> mMessageUser, ArrayList<String> mMessageImage, ArrayList<String> mMessageImageVideo, ArrayList<String> mMessageText, ArrayList<String> mShapeUUID, ArrayList<Boolean> mUserIsWithinShape, ArrayList<Double> mShapeLat, ArrayList<Double> mShapeLon, ArrayList<Boolean> mSeenByUser) {

            this.mContext = context;
            this.mMessageTime = mMessageTime;
            this.mMessageUser = mMessageUser;
            this.mMessageImage = mMessageImage;
            this.mMessageImageVideo = mMessageImageVideo;
            this.mMessageText = mMessageText;
            this.mShapeUUID = mShapeUUID;
            this.mUserIsWithinShape = mUserIsWithinShape;
            this.mShapeLat = mShapeLat;
            this.mShapeLon = mShapeLon;
            this.mSeenByUser = mSeenByUser;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(mContext).inflate(R.layout.directmentionsadapterlayout, parent, false);

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

    private void showMessageLong(String message) {

        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

            Snackbar snackBar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
            View snackBarView = snackBar.getView();
            TextView snackTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
            snackTextView.setMaxLines(10);
            snackBar.show();
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
}