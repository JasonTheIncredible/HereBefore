package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
//developers.google.com/identity/sign-in/android/sign-in
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class SignIn extends AppCompatActivity {

    private static final String TAG = "SignIn";
    private static final int RC_SIGN_IN = 0;
    private EditText mEmail, mPassword;
    private Button signInButton, goToCreateAccountButton;
    private SignInButton googleSignInButton;
    private String uuid;
    private Double polygonArea, circleLatitude, circleLongitude, radius, marker0Latitude, marker0Longitude, marker1Latitude, marker1Longitude, marker2Latitude, marker2Longitude, marker3Latitude, marker3Longitude, marker4Latitude, marker4Longitude, marker5Latitude, marker5Longitude, marker6Latitude, marker6Longitude, marker7Latitude, marker7Longitude;
    private boolean newShape, userIsWithinShape, threeMarkers, fourMarkers, fiveMarkers, sixMarkers, sevenMarkers, eightMarkers, shapeIsCircle;
    private int fillColor;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        setContentView(R.layout.signin);

        mEmail = findViewById(R.id.emailAddress);
        mPassword = findViewById(R.id.password);
        signInButton = findViewById(R.id.signInButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        // Set the color scheme for the Google sign-in button. Documentation found here:
        // developers.google.com/android/reference/com/google/android/gms/common/SignInButton.html#COLOR_DARK
        googleSignInButton.setColorScheme(0);
        goToCreateAccountButton = findViewById(R.id.goToCreateAccountButton);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Get info from Map.java
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

        // Give feedback about email and password.
        signInButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view){

                Log.i(TAG, "onStart() -> signInButton -> onClick");

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
                if (SignIn.this.getCurrentFocus() != null) {

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {

                        imm.hideSoftInputFromWindow(SignIn.this.getCurrentFocus().getWindowToken(), 0);
                    }
                }

                findViewById(R.id.loadingIcon).bringToFront();
                findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);

                // Check if the account exists in Firebase.
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            // Go to Chat.java with the extras.
                            toastMessageShort("Signed in");
                            Intent Activity = new Intent(SignIn.this, Chat.class);
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
                            toastMessageLong("User Authentication Failed: " + task.getException().getMessage());
                        } else if (!task.isSuccessful() && task.getException() == null) {

                            // Tell the user something happened.
                            findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                            toastMessageLong("An unknown error occurred. Please try again.");

                            // Send the information to Crashlytics for future debugging.
                            Log.e(TAG, "onStart() -> signInButton -> OnClick -> FirebaseAuth -> task.getException == null");
                            Crashlytics.logException(new RuntimeException("onStart() -> signInButton -> OnClick -> FirebaseAuth -> task.getException == null"));
                        }
                    }
                });
            }
        });

        // Sign in using Google.
        googleSignInButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        // Go to the SignUp.java with the extras.
        goToCreateAccountButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view){

                Intent Activity = new Intent(SignIn.this, SignUp.class);
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
                startActivity(Activity);
                finish();
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
    protected void onStop(){

        Log.i(TAG, "onStop()");

        // Remove the listener.
        if (signInButton != null) {

            signInButton.setOnClickListener(null);
        }

        // Remove the listener.
        if (googleSignInButton != null) {

            googleSignInButton.setOnClickListener(null);
        }

        // Remove the listener.
        if (goToCreateAccountButton != null) {

            goToCreateAccountButton.setOnClickListener(null);
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

    // Sign in using Google.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {

                // Google sign in was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {

                    firebaseAuthWithGoogle(account);
                    findViewById(R.id.loadingIcon).bringToFront();
                    findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
                } else {

                    Log.w(TAG, "onActivityResult() -> account == null");
                    Crashlytics.logException(new Exception("onActivityResult() -> account == null"));
                    toastMessageLong("Sign-in failed. Try again later.");
                }
            } catch (ApiException e) {

                // Google sign in failed, update UI appropriately
                Log.w(TAG, "Google sign in failed: " + e);
                Crashlytics.logException(new Exception("Google sign in failed: " + e));
                toastMessageLong("Google sign in failed: " + e);
            }
        }
    }

    // Used by onActivityResult().
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        Log.d(TAG, "firebaseAuthWithGoogle: " + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            // Sign-in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            // Go to Chat.java with the extras.
                            toastMessageShort("Signed in");
                            Intent Activity = new Intent(SignIn.this, Chat.class);
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
                        } else {

                            // If sign in fails, display a recyclerviewlayout to the user.
                            findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                            Log.w(TAG, "firebaseAuthWithGoogle() -> failed: " + task.getException());
                            toastMessageLong("Authentication failed. Try again later.");
                            Crashlytics.logException(new Exception("firebaseAuthWithGoogle() -> failed: " + task.getException()));
                        }
                    }
                });
    }

    private void toastMessageShort(String message){

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void toastMessageLong(String message){

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
