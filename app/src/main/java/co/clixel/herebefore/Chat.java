package co.clixel.herebefore;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
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

    //TODO: Save and restore scroll position onPause, onResume, and on screen rotation.
    //TODO: Do something with onPause and onResume.
    //TODO: Update the RecyclerView with the newest message rather than initialize the whole recyclerView again (notifyDataSetChanged).
    //TODO: Move name and time to the middle of the RecyclerView (in message.xml file)?
    //TODO: Add a username (in message.xml).
    //TODO: Add ability to add pictures and video to RecyclerView.

    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.messaginglayout);

        mInput = findViewById(R.id.input);
        FloatingActionButton sendButton = findViewById(R.id.sendButton);

        // Load messages from Firebase.
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference databaseReference = database.getReference();
        databaseReference.child("messageThreads").addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                final Bundle extras = getIntent().getExtras();
                final String circleID = extras.getString("circleID");

                databaseReference.child("messageThreads").addChildEventListener(new ChildEventListener(){
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot ds, String children) {

                        if (ds.child("circleID").getValue().equals(circleID)) {

                            String messageText = (String) ds.child("message").getValue();
                            Long serverDate = (Long) ds.child("date").getValue();
                            DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                            Date netDate = (new Date(serverDate));
                            String messageTime = dateFormat.format(netDate);
                            mText.add(messageText);
                            mTime.add(messageTime);
                            initRecyclerView();
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }

                });
            }

            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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

    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }

    private void initRecyclerView(){

        // Initialize the RecyclerView
        Log.d(TAG, "initRecyclerView: init recyclerView.");

        final RecyclerView recyclerView = findViewById(R.id.messageList);

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mUser, mTime, mText);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Scroll to bottom of chat after initialization.
        recyclerView.scrollToPosition(mText.size() - 1);
    }

    @Override
    public void onPause(){

        super.onPause();
    }

    @Override
    public void onResume(){

        super.onResume();
    }
}