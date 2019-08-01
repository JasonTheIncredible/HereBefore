package co.clixel.herebefore;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.FloatingActionButton;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Chat extends AppCompatActivity {

    private static final String TAG = "Chat";
    private EditText mInput;
    private ArrayList<String> mUser = new ArrayList<>();
    private ArrayList<String> mTime = new ArrayList<>();
    private ArrayList<String> mText = new ArrayList<>();

    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.messaginglayout);

        mInput = findViewById(R.id.input);

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference databaseReference = database.getReference();
        databaseReference.child("messageThreads").addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // get all of the children at this level
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();

                //TODO: Only update the thread with the newest input

                final Bundle extras = getIntent().getExtras();
                final String circleID = extras.getString("circleID");

                databaseReference.child("messageThreads").addChildEventListener(new ChildEventListener(){
                    @Override
                    public void onChildAdded(DataSnapshot ds, String children) {
                        if (( ds.child("circleID").getValue() ).equals(circleID)) {
                            String messageText = (String) ds.child("message").getValue();
                            mText.add(messageText);
                            initRecyclerView();
                        }
                        // TODO: update the view with the newest message rather than initialize the whole recyclerView again?
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

        FloatingActionButton sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = mInput.getText().toString();
                Bundle extras = getIntent().getExtras();
                if ((!input.equals("")) && (extras != null)){
                    MessageInformation messageInformation = new MessageInformation();
                    messageInformation.setMessage(input);
                    String circleID = extras.getString("circleID");
                    messageInformation.setCircleID(circleID);
                    DatabaseReference newMessage = FirebaseDatabase.getInstance().getReference().child("messageThreads").push();
                    newMessage.setValue(messageInformation);
                    mInput.getText().clear();
                }else {
                    toastMessage("Something went wrong. Please try again later.");
                }
            }
        });

    }

    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }

    private void initRecyclerView(){
        Log.d(TAG, "initRecyclerView: init recyclerView.");
        final RecyclerView recyclerView = findViewById(R.id.messageList);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mUser, mTime, mText);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.scrollToPosition(mText.size() - 1);
        //TODO: move this line inside addListenerForSingleEvent?
        recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    recyclerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.smoothScrollToPosition(
                                    recyclerView.getAdapter().getItemCount() - 1);
                        }
                    }, 100);
                }
            }
        });

    }

    @Override
    public void onStart(){

        super.onStart();

    }

}
