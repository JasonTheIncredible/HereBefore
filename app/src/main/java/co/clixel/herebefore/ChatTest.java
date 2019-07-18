package co.clixel.herebefore;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChatTest extends AppCompatActivity{

    private FirebaseAuth mAuth;

    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        setContentView(R.layout.signin);
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);


    }

}
