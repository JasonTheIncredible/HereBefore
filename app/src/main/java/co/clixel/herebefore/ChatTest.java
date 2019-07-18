package co.clixel.herebefore;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChatTest extends AppCompatActivity{

    private FirebaseAuth mAuth;
    private EditText mEmail, mPassword;
    private Button btnSignIn, btnLogOut;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin);
        //declare buttons and edit texts in onCreate
        mEmail = findViewById(R.id.emailAddress);
        mPassword = findViewById(R.id.password);
        btnSignIn = findViewById(R.id.signInButton);
        btnLogOut = findViewById(R.id.logOutButton);

        //TODO: check if account exists. If not, make a toast message.
        //TODO: make button for creating account.
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                String email = mEmail.getText().toString();
                String pass = mPassword.getText().toString();
                if(!email.equals("") && !pass.equals("")) {
                    mAuth.signInWithEmailAndPassword(email,pass);
                    toastMessage("Signing in...");
                    //TODO: move to chat once signed in.
                }else{
                    toastMessage("Please fill in all the fields to continue.");
                }
            }
        });

        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                mAuth.signOut();
                toastMessage("Signing out...");
                finish();
                startActivity(getIntent());
            }
        });
    }

    //TODO: needs to go to chat if user is signed in. If not signed in, needs to go to sign in page.
    protected void updateUI(FirebaseUser currentUser) {
        if (currentUser != null) {
            findViewById(R.id.signInButton).setVisibility(View.GONE);
            findViewById(R.id.logOutButton).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.signInButton).setVisibility(View.VISIBLE);
            findViewById(R.id.logOutButton).setVisibility(View.GONE);
        }}

    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    //add a toast to show when successfully signed in
    /**
     * * customizable toast
     * @param message
     */
    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }
}
