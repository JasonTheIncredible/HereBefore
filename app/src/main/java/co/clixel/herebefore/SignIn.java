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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignIn extends AppCompatActivity {

    private static final String TAG = "SignIn";
    private EditText mEmail, mPassword;
    private Button btnSignIn, btnGoToCreateAccount;
    private String uuid;
    private Double circleLatitude, circleLongitude, radius, marker0Latitude, marker0Longitude, marker1Latitude, marker1Longitude, marker2Latitude, marker2Longitude, marker3Latitude, marker3Longitude, marker4Latitude, marker4Longitude, marker5Latitude, marker5Longitude, marker6Latitude, marker6Longitude, marker7Latitude, marker7Longitude;
    private boolean newShape, userIsWithinShape, threeMarkers, fourMarkers, fiveMarkers, sixMarkers, sevenMarkers, eightMarkers, shapeIsCircle;
    private int fillColor;

    //TODO: Update signin.xml (visuals).
    //TODO: Sign in with Google account.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.signin);

        mEmail = findViewById(R.id.emailAddress);
        mPassword = findViewById(R.id.password);
        btnSignIn = findViewById(R.id.signInButton);
        btnGoToCreateAccount = findViewById(R.id.goToCreateAccountButton);
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
                            Activity.putExtra("newShape", newShape);
                            Activity.putExtra("uuid", uuid);
                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                            Activity.putExtra("circleLatitude", circleLatitude);
                            Activity.putExtra("circleLongitude", circleLongitude);
                            Activity.putExtra("fillColor", fillColor);
                            Activity.putExtra("radius", radius);
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
                Activity.putExtra("newShape", newShape);
                Activity.putExtra("uuid", uuid);
                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                Activity.putExtra("circleLatitude", circleLatitude);
                Activity.putExtra("circleLongitude", circleLongitude);
                Activity.putExtra("fillColor", fillColor);
                Activity.putExtra("radius", radius);
                startActivity(Activity);
                finish();
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
    protected void onStop(){

        Log.i(TAG, "onStop()");

        // Remove the listener.
        if (btnSignIn != null) {

            btnSignIn.setOnClickListener(null);
        }

        // Remove the listener.
        if (btnGoToCreateAccount != null) {

            btnGoToCreateAccount.setOnClickListener(null);
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

    private void toastMessage(String message){

        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }
}
