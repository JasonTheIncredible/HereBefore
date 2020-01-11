package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
    private Button createAccountButton;
    private String uuid;
    private Double polygonArea, circleLatitude, circleLongitude, radius, marker0Latitude, marker0Longitude, marker1Latitude, marker1Longitude, marker2Latitude, marker2Longitude, marker3Latitude, marker3Longitude, marker4Latitude, marker4Longitude, marker5Latitude, marker5Longitude, marker6Latitude, marker6Longitude, marker7Latitude, marker7Longitude;
    private boolean newShape, userIsWithinShape, threeMarkers, fourMarkers, fiveMarkers, sixMarkers, sevenMarkers, eightMarkers, shapeIsCircle;
    private int fillColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.signup);

        mEmail = findViewById(R.id.createEmailAddress);
        mPassword = findViewById(R.id.createPassword);
        createAccountButton = findViewById(R.id.createAccountButton);

        // Set to dark mode.
        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES);

        // Get info from Map.java -> SignIn.java.
        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            newShape = extras.getBoolean("newShape");
            uuid = extras.getString("uuid");
            userIsWithinShape = extras.getBoolean("userIsWithinShape");
            // fillColor will be null if the shape is not a point.
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
        } else {

            Log.e(TAG, "onStart() -> extras == null");
            Crashlytics.logException(new RuntimeException("onStart() -> extras == null"));
        }
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Give feedback about email and password.
        createAccountButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Log.i(TAG, "onStart() -> createAccountButton -> onClick");

                String email = mEmail.getText().toString().toLowerCase();
                String pass = mPassword.getText().toString();

                if (email.equals("") && !pass.equals("")) {

                    toastMessageShort("Email address required");
                    mEmail.requestFocus();
                    return;
                } if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                    toastMessageShort("Please enter a valid email address");
                    mEmail.requestFocus();
                    return;
                } if (pass.equals("") && !email.equals("")) {

                    toastMessageShort("Password required");
                    mPassword.requestFocus();
                    return;
                } if (pass.length() < 6) {

                    toastMessageShort("Password must be at least 6 characters long");
                    mPassword.requestFocus();
                    return;
                }

                // Close the keyboard.
                if (SignUp.this.getCurrentFocus() != null) {

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {

                        imm.hideSoftInputFromWindow(SignUp.this.getCurrentFocus().getWindowToken(), 0);
                    }
                }

                findViewById(R.id.loadingIcon).bringToFront();
                findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);

                // Try adding the new account to Firebase.
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            // Go to Chat.java with the extras.
                            toastMessageShort("Signed up");
                            Intent Activity = new Intent(SignUp.this, Chat.class);
                            Activity.putExtra("newShape", newShape);
                            Activity.putExtra("uuid", uuid);
                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                            Activity.putExtra("circleLatitude", circleLatitude);
                            Activity.putExtra("circleLongitude", circleLongitude);
                            Activity.putExtra("fillColor", fillColor);
                            Activity.putExtra("polygonArea", polygonArea);
                            Activity.putExtra("radius", radius);
                            Activity.putExtra("shapeIsCircle", shapeIsCircle);
                            Activity.putExtra("threeMarkers", threeMarkers);
                            Activity.putExtra("fourMarkers", fourMarkers);
                            Activity.putExtra("fiveMarkers", fiveMarkers);
                            Activity.putExtra("sixMarkers", sixMarkers);
                            Activity.putExtra("sevenMarkers", sevenMarkers);
                            Activity.putExtra("eightMarkers", eightMarkers);
                            Activity.putExtra("marker0Latitude", marker0Latitude);
                            Activity.putExtra("marker0Longitude", marker0Longitude);
                            Activity.putExtra("marker1Latitude", marker1Latitude);
                            Activity.putExtra("marker1Longitude", marker1Longitude);
                            Activity.putExtra("marker2Latitude", marker2Latitude);
                            Activity.putExtra("marker2Longitude", marker2Longitude);
                            Activity.putExtra("marker3Latitude", marker3Latitude);
                            Activity.putExtra("marker3Longitude", marker3Longitude);
                            Activity.putExtra("marker4Latitude", marker4Latitude);
                            Activity.putExtra("marker4Longitude", marker4Longitude);
                            Activity.putExtra("marker5Latitude", marker5Latitude);
                            Activity.putExtra("marker5Longitude", marker5Longitude);
                            Activity.putExtra("marker6Latitude", marker6Latitude);
                            Activity.putExtra("marker6Longitude", marker6Longitude);
                            Activity.putExtra("marker7Latitude", marker7Latitude);
                            Activity.putExtra("marker7Longitude", marker7Longitude);
                            findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                            startActivity(Activity);
                            finish();
                        }

                        if (!task.isSuccessful() && task.getException() != null) {

                            // Tell the user what happened.
                            findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                            toastMessageLong("Account Creation Failed: " + task.getException().getMessage());
                        } else if (!task.isSuccessful() && task.getException() == null) {

                            // Tell the user something happened.
                            findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                            toastMessageLong("An unknown error occurred. Please try again.");

                            // Send the information to Crashlytics for future debugging.
                            Log.e(TAG, "onStart() -> createAccountButton -> OnClick -> FirebaseAuth -> task.getException == null");
                            Crashlytics.logException(new RuntimeException("onStart() -> createAccountButton -> OnClick -> FirebaseAuth -> task.getException == null"));
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

        findViewById(R.id.loadingIcon).setVisibility(View.GONE);
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
        if (createAccountButton != null) {

            createAccountButton.setOnClickListener(null);
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

    private void toastMessageShort(String message){

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void toastMessageLong(String message){

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
