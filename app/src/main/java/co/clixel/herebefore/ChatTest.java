package co.clixel.herebefore;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChatTest extends AppCompatActivity {

    private View messageView;
    private RecyclerView messageList;
    private Button mSendButton;
    private EditText mInput;

    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        mInput = findViewById(R.id.input);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference();
        databaseReference.child("messageThread").addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // get all of the children at this level
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();

                // shake hands with all of them.
                for (DataSnapshot ds : children) {
                }
            }

            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Button sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = mInput.getText().toString();
                if (input != ""){
                    DatabaseReference newMessageThread = FirebaseDatabase.getInstance().getReference().child("messageThread").push();
                }
            }
        });
    }

    @Override
    public void onStart(){

        super.onStart();


    }

}
