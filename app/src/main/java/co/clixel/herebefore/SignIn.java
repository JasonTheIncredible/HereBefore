package co.clixel.herebefore;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignIn extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Button btnSignIn, btnGoToCreateAccount;
    private String circleID;

    //TODO: Update signin.xml (visuals).
    //TODO: Sign in with Google account.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin);

        mEmail = findViewById(R.id.emailAddress);
        mPassword = findViewById(R.id.password);
        btnSignIn = findViewById(R.id.signInButton);
        btnGoToCreateAccount = findViewById(R.id.goToCreateAccountButton);
    }

    @Override
    protected void onStart() {

        super.onStart();

        // Get info from Map.java.
        Bundle extras = getIntent().getExtras();
        circleID = extras.getString("circleID");

        // Give feedback about email and password.
        btnSignIn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view){

                String email = mEmail.getText().toString().toLowerCase();
                String pass = mPassword.getText().toString();

                if (email.equals("") && !pass.equals("")) {

                    toastMessage("Email address required");
                    mEmail.requestFocus();
                    return;
                } if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                    toastMessage("Please enter a valid email address");
                    mEmail.requestFocus();
                    return;
                } if (pass.equals("") && !email.equals("")) {

                    toastMessage("Password required");
                    mPassword.requestFocus();
                    return;
                } if (pass.length()<6) {

                    toastMessage("Password must be at least 6 characters long");
                    mPassword.requestFocus();
                    return;
                }

                // Check if the account exists in Firebase.
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            // Go to Chat.java ith the circleID.
                            toastMessage("Signed in");
                            Intent Activity = new Intent(SignIn.this, Chat.class);
                            Activity.putExtra("circleID", circleID);
                            startActivity(Activity);
                            finish();
                        } if (task.getException() != null) {

                            // Tell the user what happened.
                            Toast.makeText(getApplicationContext(), "User Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();

                            // Send the information to Crashlytics for future debugging. NOTE: This will only be sent after the user restarts the app.
                            Crashlytics.logException(new RuntimeException( task.getException().getMessage() ));
                        } if ( (!task.isSuccessful() ) && ( task.getException() == null) ) {

                            // Tell the user something happened.
                            toastMessage("An unknown error occurred. Please try again.");
                        }
                    }
                });
            }
        });

        // Go to the SignUp activity with the circleID.
        btnGoToCreateAccount.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view){

                Intent Activity = new Intent(SignIn.this, SignUp.class);
                Activity.putExtra("circleID", circleID);
                startActivity(Activity);
                finish();
            }
        });
    }

    @Override
    protected void onRestart() {

        super.onRestart();
    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onStop(){

        super.onStop();

        // Remove the listener.
        if (btnSignIn != null) {

            btnSignIn.setOnClickListener(null);
        }

        // Remove the listener.
        if (btnGoToCreateAccount != null) {

            btnGoToCreateAccount.setOnClickListener(null);
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    private void toastMessage(String message){

        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }
}
