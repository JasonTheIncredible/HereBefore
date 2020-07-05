package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.PolyUtil;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static java.text.DateFormat.getDateTimeInstance;

public class DirectMentions extends AppCompatActivity {

    private static final String TAG = "DirectMentions";
    private String email;
    private ArrayList<String> mTime = new ArrayList<>(), mUser = new ArrayList<>(), mImage = new ArrayList<>(), mVideo = new ArrayList<>(), mText = new ArrayList<>(), mShapeUUID = new ArrayList<>();
    private ArrayList<Boolean> mUserIsWithinShape = new ArrayList<>(), mShapeIsCircle = new ArrayList<>();
    private ArrayList<Integer> mPosition = new ArrayList<>();
    private RecyclerView directMentionsRecyclerView;
    private static int index = -1, top = -1, last, mentionCount = 0, mentionCount1 = 0;
    private DatabaseReference databaseReferenceOne, databaseReferenceTwo, databaseReferenceCircles, databaseReferencePolygons;
    private ValueEventListener eventListenerOne, eventListenerTwo, eventListenerCircles, eventListenerPolygons;
    private LinearLayoutManager directMentionsRecyclerViewLinearLayoutManager = new LinearLayoutManager(this);
    private boolean theme, firstLoad, userIsWithinShape;
    private View loadingIcon;
    private SharedPreferences sharedPreferences;
    private Toast longToast, noDMsToast;
    private Double userLatitude, userLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        setContentView(R.layout.directmentions);

        directMentionsRecyclerView = findViewById(R.id.mentionsList);
        loadingIcon = findViewById(R.id.loadingIcon);

        // Set to true to scroll to the bottom of directMentionsRecyclerView.
        firstLoad = true;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            userLatitude = extras.getDouble("userLatitude");
            userLongitude = extras.getDouble("userLongitude");
        } else {

            Log.e(TAG, "onStart() -> extras == null");
        }

        // Make the loadingIcon visible upon the first load, as it can sometimes take a while to show anything. It should be made invisible in initDirectMentionsAdapter().
        if (loadingIcon != null) {

            loadingIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Update to the user's preferences.
        loadPreferences();
        updatePreferences();

        mentionCount = 0;
        mentionCount1 = 0;

        // If user has a Google account, get email one way. Else, get email another way.
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getBaseContext());
        if (acct != null) {

            email = acct.getEmail();
        } else {

            email = sharedPreferences.getString("userToken", "null");
        }

        // Connect to Firebase.
        final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        databaseReferenceOne = rootRef.child("MessageThreads");
        eventListenerOne = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                // Clear the RecyclerView before adding new entries to prevent duplicates.
                if (directMentionsRecyclerViewLinearLayoutManager != null) {

                    mTime.clear();
                    mUser.clear();
                    mImage.clear();
                    mVideo.clear();
                    mText.clear();
                    mShapeUUID.clear();
                    mUserIsWithinShape.clear();
                    mShapeIsCircle.clear();
                    mPosition.clear();
                }

                // Read RecyclerView scroll position (for use in initDirectMentionsAdapter()).
                if (directMentionsRecyclerViewLinearLayoutManager != null) {

                    index = directMentionsRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
                    last = directMentionsRecyclerViewLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                    View v = directMentionsRecyclerView.getChildAt(0);
                    top = (v == null) ? 0 : (v.getTop() - directMentionsRecyclerView.getPaddingTop());
                }

                for (final DataSnapshot ds : dataSnapshot.getChildren()) {

                    if (ds.child("removedMentionDuplicates").getValue() != null) {

                        for (final DataSnapshot mention : ds.child("removedMentionDuplicates").getChildren()) {

                            if (mention.getValue() != null) {

                                // If mentionCount == mentionCount1, initialize the adapter.
                                mentionCount++;
                                databaseReferenceTwo = rootRef.child("MessageThreads");
                                eventListenerTwo = new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        for (DataSnapshot dss : dataSnapshot.getChildren()) {

                                            String userUUID = (String) dss.child("userUUID").getValue();
                                            if (mention.getValue().toString().equals(userUUID)) {

                                                // If mentionCount == mentionCount1, initialize the adapter.
                                                mentionCount1++;
                                                String userEmail = (String) dss.child("email").getValue();
                                                if (userEmail != null) {

                                                    if (userEmail.equals(email)) {

                                                        Long serverDate = (Long) ds.child("date").getValue();
                                                        String user = (String) ds.child("userUUID").getValue();
                                                        String imageURL = (String) ds.child("imageURL").getValue();
                                                        String videoURL = (String) ds.child("videoURL").getValue();
                                                        String messageText = (String) ds.child("message").getValue();
                                                        String shapeUUID = (String) ds.child("shapeUUID").getValue();
                                                        Boolean userIsWithinShape = (Boolean) ds.child("userIsWithinShape").getValue();
                                                        Boolean shapeIsCircle = (Boolean) ds.child("shapeIsCircle").getValue();
                                                        Integer position = ((Long) ds.child("position").getValue()).intValue();
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
                                                        mShapeUUID.add(shapeUUID);
                                                        mUserIsWithinShape.add(userIsWithinShape);
                                                        mShapeIsCircle.add(shapeIsCircle);
                                                        mPosition.add(position);
                                                    }
                                                }
                                            }
                                        }

                                        // Prevent recyclerView from getting initialized more than once,
                                        // as the loading icon / toast is dependant on this happening only once.
                                        if (mentionCount == mentionCount1) {

                                            initDirectMentionsAdapter();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                        loadingIcon.setVisibility(View.GONE);
                                        toastMessageLong(databaseError.getMessage());
                                    }
                                };

                                // Add the second Firebase listener.
                                databaseReferenceTwo.addListenerForSingleValueEvent(eventListenerTwo);
                            }
                        }
                    }
                }
            }

            public void onCancelled(@NonNull DatabaseError databaseError) {

                loadingIcon.setVisibility(View.GONE);
                toastMessageLong(databaseError.getMessage());
            }
        };

        // Add the first Firebase listener.
        databaseReferenceOne.addValueEventListener(eventListenerOne);
    }

    @Override
    public void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        mentionCount = 0;
        mentionCount1 = 0;
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

            Intent Activity = new Intent(getBaseContext(), co.clixel.herebefore.Settings.class);

            startActivity(Activity);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {

        Log.i(TAG, "onStop()");

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

        if (databaseReferenceOne != null) {

            databaseReferenceOne.removeEventListener(eventListenerOne);
        }

        if (databaseReferenceTwo != null) {

            databaseReferenceTwo.removeEventListener(eventListenerTwo);
        }

        if (directMentionsRecyclerView != null) {

            directMentionsRecyclerView.clearOnScrollListeners();
        }

        if (eventListenerOne != null) {

            eventListenerOne = null;
        }

        if (eventListenerTwo != null) {

            eventListenerTwo = null;
        }

        cancelToasts();

        super.onStop();
    }

    private void cancelToasts() {

        if (longToast != null) {

            longToast.cancel();
        }

        if (noDMsToast != null) {

            noDMsToast.cancel();
        }
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

    private void initDirectMentionsAdapter() {

        // Initialize the RecyclerView.
        Log.i(TAG, "initDirectMentionsAdapter()");

        DirectMentionsAdapter adapter = new DirectMentionsAdapter(this, mTime, mUser, mImage, mVideo, mText, mShapeUUID, mUserIsWithinShape, mShapeIsCircle, mPosition);
        directMentionsRecyclerView.setAdapter(adapter);
        directMentionsRecyclerView.setLayoutManager(directMentionsRecyclerViewLinearLayoutManager);

        if (last == (mTime.size() - 2) || firstLoad) {

            // Scroll to bottom of recyclerviewlayout after first initialization and after sending a recyclerviewlayout.
            directMentionsRecyclerView.scrollToPosition(mTime.size() - 1);
            firstLoad = false;
        } else {

            // Set RecyclerView scroll position to prevent position change when Firebase gets updated and after screen orientation change.
            directMentionsRecyclerViewLinearLayoutManager.scrollToPositionWithOffset(index, top);
        }

        // After the initial load, make the loadingIcon invisible.
        if (loadingIcon != null) {

            loadingIcon.setVisibility(View.GONE);
        }

        if (mUser.size() == 0) {

            noDMsToast = Toast.makeText(getBaseContext(), "You have no direct mentions", Toast.LENGTH_LONG);
            noDMsToast.setGravity(Gravity.CENTER, 0, 0);
            noDMsToast.show();
        }

        mentionCount = 0;
        mentionCount1 = 0;
    }

    public class DirectMentionsAdapter extends RecyclerView.Adapter<DirectMentionsAdapter.ViewHolder> {

        private Context mContext;
        private ArrayList<String> mMessageTime, mMessageUser, mMessageImage, mMessageImageVideo, mMessageText, mShapeUUID;
        private ArrayList<Boolean> mUserIsWithinShape, mShapeIsCircle;
        private ArrayList<Integer> mPosition;
        private ImageButton playButtonInside, playButtonOutside;
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
                playButtonInside = itemView.findViewById(R.id.playButtonInside);
                playButtonOutside = itemView.findViewById(R.id.playButtonOutside);
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

                                                        Intent Activity = new Intent(mContext, Chat.class);
                                                        Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                        Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                        Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                        loadingIcon.setVisibility(View.GONE);

                                                        mContext.startActivity(Activity);
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

                                                    Intent Activity = new Intent(mContext, Chat.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);
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

                                                    Intent Activity = new Intent(mContext, Chat.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);
                                                } else if (ds.child("polygonOptions/points/5/latitude/").getValue() != null) {
                                                    polygon.add(marker5Position);
                                                    polygon.add(marker4Position);
                                                    polygon.add(marker3Position);
                                                    polygon.add(marker2Position);
                                                    polygon.add(marker1Position);
                                                    polygon.add(marker0Position);

                                                    userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                    cancelToasts();

                                                    Intent Activity = new Intent(mContext, Chat.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);
                                                } else if (ds.child("polygonOptions/points/4/latitude/").getValue() != null) {
                                                    polygon.add(marker4Position);
                                                    polygon.add(marker3Position);
                                                    polygon.add(marker2Position);
                                                    polygon.add(marker1Position);
                                                    polygon.add(marker0Position);

                                                    userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                    cancelToasts();

                                                    Intent Activity = new Intent(mContext, Chat.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);
                                                } else if (ds.child("polygonOptions/points/3/latitude/").getValue() != null) {
                                                    polygon.add(marker3Position);
                                                    polygon.add(marker2Position);
                                                    polygon.add(marker1Position);
                                                    polygon.add(marker0Position);

                                                    userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                    cancelToasts();

                                                    Intent Activity = new Intent(mContext, Chat.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);
                                                } else {
                                                    polygon.add(marker2Position);
                                                    polygon.add(marker1Position);
                                                    polygon.add(marker0Position);

                                                    userIsWithinShape = PolyUtil.containsLocation(userLatitude, userLongitude, polygon, false);

                                                    cancelToasts();

                                                    Intent Activity = new Intent(mContext, Chat.class);
                                                    Activity.putExtra("shapeUUID", mShapeUUID.get(getAdapterPosition()));
                                                    Activity.putExtra("directMentionsPosition", mPosition.get(getAdapterPosition()));
                                                    Activity.putExtra("userIsWithinShape", userIsWithinShape);

                                                    loadingIcon.setVisibility(View.GONE);

                                                    mContext.startActivity(Activity);
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

        DirectMentionsAdapter(Context context, ArrayList<String> mMessageTime, ArrayList<String> mMessageUser, ArrayList<String> mMessageImage, ArrayList<String> mMessageImageVideo, ArrayList<String> mMessageText, ArrayList<String> mShapeUUID, ArrayList<Boolean> mUserIsWithinShape, ArrayList<Boolean> mShapeIsCircle, ArrayList<Integer> mPosition) {

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

            Bundle extras = getIntent().getExtras();
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

            theme = sharedPreferences.getBoolean(co.clixel.herebefore.Settings.KEY_THEME_SWITCH, false);
        }
    }

    private void toastMessageLong(String message) {

        longToast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG);
        longToast.setGravity(Gravity.CENTER, 0, 0);
        longToast.show();
    }
}
