package co.clixel.herebefore;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
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

public class SignUp extends AppCompatActivity {

    private static final String TAG = "SignUp";
    private EditText mEmail, mPassword;
    private Button btnCreateAccount;
    private String uuid;
    private Double latitude;
    private Double longitude;
    private Double radius;
    private boolean newCircle;
    private int fillColor;

    //TODO: Update signup.xml (visuals and add more user information).
    //TODO: Sign in with Google account.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.signup);

        mEmail = findViewById(R.id.createEmailAddress);
        mPassword = findViewById(R.id.createPassword);
        btnCreateAccount = findViewById(R.id.createAccountButton);
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Get info from Map.java -> SignIn.java.
        Bundle extras = getIntent().getExtras();
        newCircle = extras.getBoolean("newCircle");
        uuid = extras.getString("uuid");
        // latitude, longitude, and radius will be null if the circle is not new (as a new circle is not being created).
        latitude = extras.getDouble("latitude");
        longitude = extras.getDouble("longitude");
        fillColor = extras.getInt("fillColor");
        radius = extras.getDouble("radius");


        // Give feedback about email and password.
        btnCreateAccount.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

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
                } if (pass.length() < 6) {

                    toastMessage("Password must be at least 6 characters long");
                    mPassword.requestFocus();
                    return;
                }

                // Try adding the new account to Firebase.
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            // Go to Chat.java with the circleID.
                            toastMessage("Signed up");
                            Intent Activity = new Intent(SignUp.this, Chat.class);
                            Activity.putExtra("newCircle", newCircle);
                            Activity.putExtra("uuid", uuid);
                            Activity.putExtra("latitude", latitude);
                            Activity.putExtra("longitude", longitude);
                            Activity.putExtra("fillColor", fillColor);
                            Activity.putExtra("radius", radius);
                            startActivity(Activity);
                            finish();
                        } if (task.getException() != null) {

                            // Tell the user what happened.
                            Toast.makeText(getApplicationContext(), "Account Creation Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();

                            // Send the information to Crashlytics for future debugging. NOTE: This will only be sent after the user restarts the app.
                            Crashlytics.logException(new RuntimeException( task.getException().getMessage() ));
                        } if ( (!task.isSuccessful()) && (task.getException() == null) ) {

                            // Tell the user something happened.
                            toastMessage("An unknown error occurred. Please try again.");
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");
    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.i(TAG, "onResume()");
    }

    @Override
    protected void onPause() {

        Log.i(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {

        Log.i(TAG, "onStop()");

        // Remove the listener.
        if (btnCreateAccount != null) {

            btnCreateAccount.setOnClickListener(null);
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        Log.i(TAG, "onDestroy()");
        super.onDestroy();
    }

    private void toastMessage(String message) {

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
