package co.clixel.herebefore;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private Parcelable recyclerViewState;
    private static int index = -1;
    private static int top = -1;
    private DatabaseReference databaseReference;
    private ValueEventListener eventListener;
    private FloatingActionButton sendButton;

    //TODO: If user is on the bottom of RecyclerView and keyboard comes up, move to bottom of RecyclerView up.
    //TODO: Update the RecyclerView with the newest message rather than initialize the whole recyclerView again (notifyDataSetChanged).
    //TODO: Move name and time to the middle of the RecyclerView (in message.xml file)?
    //TODO: Add a username (in message.xml).
    //TODO: Add ability to add pictures and video to RecyclerView.
    //TODO: Check updating in different states with another device.

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.messaginglayout);

        mInput = findViewById(R.id.input);
        sendButton = findViewById(R.id.sendButton);

        // Get RecyclerView scroll position after screen orientation change.
        if ( (savedInstanceState != null) && (savedInstanceState.getParcelable("recyclerView") != null) ) {
            recyclerViewState = savedInstanceState.getParcelable("recyclerView");
        }
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Load messages from Firebase.
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        databaseReference = rootRef.child("messageThreads");
        eventListener = new ValueEventListener(){

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                final Bundle extras = getIntent().getExtras();
                final String circleID = extras.getString("circleID");

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    if (ds.child("circleID").getValue().equals(circleID)) {

                        String messageText = (String) ds.child("message").getValue();
                        Long serverDate = (Long) ds.child("date").getValue();
                        DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                        Date netDate = (new Date(serverDate));
                        String messageTime = dateFormat.format(netDate);
                        mText.add(messageText);
                        mTime.add(messageTime);

                        // Read RecyclerView scroll position.
                        if ( recyclerView != null ){

                            index = ( (LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                            View v = recyclerView.getChildAt(0);
                            top = (v == null) ? 0 : (v.getTop() - recyclerView.getPaddingTop());
                        }

                        initRecyclerView();
                    }
                }
            }

            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        databaseReference.addValueEventListener(eventListener);

        // onClickListener for sending message to Firebase.
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String input = mInput.getText().toString();
                Bundle extras = getIntent().getExtras();

                // Send message to Firebase.
                if ( !input.equals("") ) {

                    MessageInformation messageInformation = new MessageInformation();
                    messageInformation.setMessage(input);
                    Object date = ServerValue.TIMESTAMP;
                    messageInformation.setDate(date);
                    String circleID = extras.getString("circleID");
                    messageInformation.setCircleID(circleID);
                    DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("messageThreads").push();
                    newMessage.setValue(messageInformation);
                    mInput.getText().clear();
                }else {
                }
            }
        });
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        // Clear RecyclerView before adding new items in onStart().
        if (recyclerView != null){

            mUser.clear();
            mTime.clear();
            mText.clear();
        }
    }

    @Override
    protected void onResume(){

        super.onResume();
        Log.i(TAG, "onResume()");
    }

    @Override
    protected void onPause(){

        super.onPause();
        Log.i(TAG, "onPause()");
    }

    @Override
    protected void onStop() {

        super.onStop();
        Log.i(TAG, "onStop()");

        // Stop the Firebase event listener.
        if (databaseReference != null){

            databaseReference.removeEventListener(eventListener);
        }

        // Stop the Firebase event listener.
        if (eventListener != null){

            eventListener = null;
        }

        // Stop the listener to save resources.
        if (sendButton != null){

            sendButton.setOnClickListener(null);
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        Log.i(TAG, "onDestroy()");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        Log.i(TAG, "onSaveInstanceState()");

        // Save RecyclerView scroll position upon screen orientation change.
        if (recyclerView != null) {

            outState.putParcelable("recyclerView", recyclerView.getLayoutManager().onSaveInstanceState());
            super.onSaveInstanceState(outState);
        }
    }

    private void initRecyclerView(){

        // Initialize the RecyclerView
        Log.i(TAG, "initRecyclerView()");

        recyclerView = findViewById(R.id.messageList);

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mUser, mTime, mText);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if ( index == -1) {

            // Scroll to bottom of chat after first initialization and after onRestart() is called.
            recyclerView.scrollToPosition(mText.size() - 1);
        }else{

            // Set RecyclerView scroll position to prevent movement when Firebase gets updated and after screen orientation change.
            ( (LinearLayoutManager) recyclerView.getLayoutManager() ).scrollToPositionWithOffset( index, top);
        }

        // Restore RecyclerView scroll position upon screen orientation change.
        if ( recyclerViewState != null ) {

            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }
    }
}