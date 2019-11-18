package co.clixel.herebefore;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static java.text.DateFormat.getDateTimeInstance;

public class Chat extends AppCompatActivity {

    private static final String TAG = "Chat";
    private EditText mInput;
    private ArrayList<String> mUser = new ArrayList<>();
    private ArrayList<String> mTime = new ArrayList<>();
    private ArrayList<String> mText = new ArrayList<>();
    private RecyclerView recyclerView;
    private static int index = -1;
    private static int top = -1;
    private DatabaseReference databaseReference;
    private ValueEventListener eventListener;
    private FloatingActionButton sendButton;
    private boolean reachedEndOfRecyclerView = false;
    private boolean recyclerViewHasScrolled = false;
    private boolean messageSent = false;
    private boolean newShape, threeMarkers, fourMarkers, fiveMarkers, sixMarkers, sevenMarkers, eightMarkers, shapeIsCircle;
    private Boolean userIsWithinShape;
    private View.OnLayoutChangeListener onLayoutChangeListener;
    private String uuid;
    private Double polygonArea, circleLatitude, circleLongitude, radius, marker0Latitude, marker0Longitude, marker1Latitude, marker1Longitude, marker2Latitude, marker2Longitude, marker3Latitude, marker3Longitude, marker4Latitude, marker4Longitude, marker5Latitude, marker5Longitude, marker6Latitude, marker6Longitude, marker7Latitude, marker7Longitude;
    private int fillColor;

    //TODO: Keep checking user's location while user is in chat to see if they can keep messaging?
    //TODO: Keep users from adding messages if userIsWithinShape == false, and add a message at the top notifying user of this.
    //TODO: Too much work on main thread.
    //TODO: Add a username (in message.xml).
    //TODO: Add ability to add pictures and video to RecyclerView.
    //TODO: Work on possible NullPointerExceptions.
    //TODO: Check updating in different states with another device.

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.messaginglayout);

        mInput = findViewById(R.id.input);
        sendButton = findViewById(R.id.sendButton);
        recyclerView = findViewById(R.id.messageList);
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Get info from Map.java
        Bundle extras = getIntent().getExtras();
        newShape = extras.getBoolean("newShape");
        uuid = extras.getString("uuid");
        userIsWithinShape = extras.getBoolean("userIsWithinShape");
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

        // Connect to Firebase.
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        databaseReference = rootRef.child("messageThreads");
        eventListener = new ValueEventListener(){

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                // Clear the RecyclerView before adding new entries to prevent duplicates.
                if (recyclerView.getLayoutManager() != null){

                    mUser.clear();
                    mTime.clear();
                    mText.clear();
                }

                // If the value from Map.java is false, check Firebase and load any existing messages.
                if (!newShape) {

                    for (DataSnapshot ds : dataSnapshot.getChildren()) {

                        // If the circle identifier brought from Map equals the uuid attached to the message in Firebase, load it into the RecyclerView.
                        if (ds.child("uuid").getValue().equals(uuid)) {

                            String messageText = (String) ds.child("message").getValue();
                            Long serverDate = (Long) ds.child("date").getValue();
                            DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                            // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                            // This will cause onDataChange to fire twice; optimizations could be made in the future.
                            Date netDate = (new Date(serverDate));
                            String messageTime = dateFormat.format(netDate);
                            mText.add(messageText);
                            mTime.add(messageTime);
                        }
                    }
                }

                // Read RecyclerView scroll position (for use in initRecyclerView).
                if ( recyclerView.getLayoutManager() != null ){

                    index = ( (LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
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

                        // Used to detect if user has just entered the chat (so layout needs to move up when keyboard appears).
                        recyclerViewHasScrolled = true;
                    }
                });

                // If RecyclerView is scrolled to the bottom, move the layout up when the keyboard appears.
                recyclerView.addOnLayoutChangeListener(onLayoutChangeListener = new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v,
                                               int left, int top, int right, int bottom,
                                               int oldLeft, int oldTop, int oldRight, int oldBottom) {

                        if (reachedEndOfRecyclerView || !recyclerViewHasScrolled){

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

            }
        };

        // Add the Firebase listener.
        databaseReference.addValueEventListener(eventListener);

        // onClickListener for sending message to Firebase.
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String input = mInput.getText().toString();
                final Bundle extras = getIntent().getExtras();

                // Send message to Firebase.
                if ( !input.equals("") ) {

                    // Check Boolean value from onStart();
                    if (newShape) {

                        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

                        // If circleLatitude != null, the shape is a circle.
                        if (shapeIsCircle) {

                            DatabaseReference firebaseCircles = rootRef.child("circles");
                            firebaseCircles.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

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

                                    // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                                    messageSent = true;
                                    MessageInformation messageInformation = new MessageInformation();
                                    messageInformation.setMessage(input);
                                    // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                    // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                    Object date = ServerValue.TIMESTAMP;
                                    messageInformation.setDate(date);
                                    String uuid = extras.getString("uuid");
                                    messageInformation.setUUID(uuid);
                                    DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("messageThreads").push();
                                    newMessage.setValue(messageInformation);
                                    mInput.getText().clear();
                                    newShape = false;
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }

                        // If marker0Latitude != null, the shape is a polygon.
                        if (!shapeIsCircle) {

                            DatabaseReference firebasePolygons = rootRef.child("polygons");
                            firebasePolygons.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

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

                                    // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                                    messageSent = true;
                                    MessageInformation messageInformation = new MessageInformation();
                                    messageInformation.setMessage(input);
                                    // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                    // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                    Object date = ServerValue.TIMESTAMP;
                                    messageInformation.setDate(date);
                                    String uuid = extras.getString("uuid");
                                    messageInformation.setUUID(uuid);
                                    DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("messageThreads").push();
                                    newMessage.setValue(messageInformation);
                                    mInput.getText().clear();
                                    newShape = false;
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    } else {

                        if (shapeIsCircle) {

                            // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                            messageSent = true;
                            MessageInformation messageInformation = new MessageInformation();
                            messageInformation.setMessage(input);
                            // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                            // This will cause onDataChange to fire twice; optimizations could be made in the future.
                            Object date = ServerValue.TIMESTAMP;
                            messageInformation.setDate(date);
                            String uuid = extras.getString("uuid");
                            messageInformation.setUUID(uuid);
                            DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("messageThreads").push();
                            newMessage.setValue(messageInformation);
                            mInput.getText().clear();
                        }

                        if (!shapeIsCircle) {

                            // Change boolean to true - scrolls to the bottom of the recyclerView (in initRecyclerView()).
                            messageSent = true;
                            MessageInformation messageInformation = new MessageInformation();
                            messageInformation.setMessage(input);
                            // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                            // This will cause onDataChange to fire twice; optimizations could be made in the future.
                            Object date = ServerValue.TIMESTAMP;
                            messageInformation.setDate(date);
                            String uuid = extras.getString("uuid");
                            messageInformation.setUUID(uuid);
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
        if (mInput != null){

            mInput.clearFocus();
        }
    }

    @Override
    protected void onResume(){

        super.onResume();
        Log.i(TAG, "onResume()");
    }

    @Override
    protected void onPause(){

        Log.i(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {

        Log.i(TAG, "onStop()");

        // Remove the Firebase event listener.
        if (databaseReference != null){

            databaseReference.removeEventListener(eventListener);
        }

        // Remove RecyclerView listeners.
        if (recyclerView != null){

            recyclerView.clearOnScrollListeners();
            recyclerView.removeOnLayoutChangeListener(onLayoutChangeListener);
        }

        // Remove the Firebase event listener.
        if (eventListener != null){

            eventListener = null;
        }

        // Remove the button listener.
        if (sendButton != null){

            sendButton.setOnClickListener(null);
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

    private void initRecyclerView(){

        // Initialize the RecyclerView
        Log.i(TAG, "initRecyclerView()");

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mUser, mTime, mText);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (index == -1 || messageSent) {

            // Scroll to bottom of chat after first initialization and after sending a message.
            recyclerView.scrollToPosition(mText.size() - 1);
            messageSent = false;
        } else{

            // Set RecyclerView scroll position to prevent movement when Firebase gets updated and after screen orientation change.
            ( (LinearLayoutManager) recyclerView.getLayoutManager() ).scrollToPositionWithOffset( index, top);
        }

        // Close keyboard after sending a message.
        View view = this.getCurrentFocus();
        if (view != null) {

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {

                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            if (mInput != null){

                mInput.clearFocus();
            }
        }
    }
}