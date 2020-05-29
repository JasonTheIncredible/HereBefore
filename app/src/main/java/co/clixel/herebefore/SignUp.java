package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class SignUp extends AppCompatActivity {

    private static final String TAG = "SignUp";
    private static final int RC_SIGN_IN = 0;
    private EditText mEmail, mPassword;
    private Button createAccountButton;
    private SignInButton googleSignInButton;
    private String uuid, email, pass;
    private Double polygonArea, circleLatitude, circleLongitude, radius, marker0Latitude, marker0Longitude, marker1Latitude, marker1Longitude, marker2Latitude, marker2Longitude, marker3Latitude, marker3Longitude, marker4Latitude, marker4Longitude, marker5Latitude, marker5Longitude, marker6Latitude, marker6Longitude, marker7Latitude, marker7Longitude;
    private boolean newShape, userIsWithinShape, threeMarkers, fourMarkers, fiveMarkers, sixMarkers, sevenMarkers, eightMarkers, shapeIsCircle;
    private int fillColor;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private GoogleSignInAccount googleAccount;
    private View loadingIcon;
    private Toast shortToast, longToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.signup);

        mEmail = findViewById(R.id.createEmailAddress);
        mPassword = findViewById(R.id.createPassword);
        createAccountButton = findViewById(R.id.createAccountButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        // Set the color scheme for the Google sign-in button. Documentation found here:
        // developers.google.com/android/reference/com/google/android/gms/common/SignInButton.html#COLOR_DARK
        googleSignInButton.setColorScheme(0);
        loadingIcon = findViewById(R.id.loadingIcon);

        // Configure Google Sign In.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Firebase Auth.
        mAuth = FirebaseAuth.getInstance();

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

                email = mEmail.getText().toString().toLowerCase().trim();
                pass = mPassword.getText().toString();

                if (email.equals("") && !pass.equals("")) {

                    toastMessageShort("Email address required");
                    mEmail.requestFocus();
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                    toastMessageShort("Please enter a valid email address");
                    mEmail.requestFocus();
                    return;
                }
                if (pass.equals("") && !email.equals("")) {

                    toastMessageShort("Password required");
                    mPassword.requestFocus();
                    return;
                }
                if (pass.length() < 6) {

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

                loadingIcon.bringToFront();
                loadingIcon.setVisibility(View.VISIBLE);

                // Try adding the new account to Firebase.
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            // Get Firebase FCM token and save it to preferences.
                            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(SignUp.this, new OnSuccessListener<InstanceIdResult>() {

                                @Override
                                public void onSuccess(InstanceIdResult instanceIdResult) {

                                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SignUp.this);

                                    String token = instanceIdResult.getToken();

                                    SharedPreferences.Editor editor = sharedPreferences.edit()
                                            .putString("userToken", email)
                                            .putString("passToken", pass)
                                            .putString("FIREBASE_TOKEN", token);
                                    editor.apply();

                                    // Go to Chat.java with the extras.
                                    Toast signedUpToast = Toast.makeText(SignUp.this, "Signed up", Toast.LENGTH_SHORT);
                                    signedUpToast.show();
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
                                    loadingIcon.setVisibility(View.GONE);
                                    startActivity(Activity);
                                    finish();
                                }
                            });
                        }

                        if (!task.isSuccessful() && task.getException() != null) {

                            // Tell the user what happened.
                            loadingIcon.setVisibility(View.GONE);
                            toastMessageLong("Account Creation Failed: " + task.getException().getMessage());
                        } else if (!task.isSuccessful() && task.getException() == null) {

                            // Tell the user something happened.
                            loadingIcon.setVisibility(View.GONE);
                            toastMessageLong("An unknown error occurred. Please try again.");

                            // Send the information to Crashlytics for future debugging.
                            Log.e(TAG, "onStart() -> createAccountButton -> OnClick -> FirebaseAuth -> task.getException == null");
                            Crashlytics.logException(new RuntimeException("onStart() -> createAccountButton -> OnClick -> FirebaseAuth -> task.getException == null"));
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
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        loadingIcon.setVisibility(View.GONE);
    }

    @Override
    protected void onStop() {

        Log.i(TAG, "onStop()");

        // Remove the listener.
        if (createAccountButton != null) {

            createAccountButton.setOnClickListener(null);
        }

        // Remove the listener.
        if (googleSignInButton != null) {

            googleSignInButton.setOnClickListener(null);
        }

        cancelToasts();

        super.onStop();
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
                googleAccount = task.getResult(ApiException.class);
                if (googleAccount != null) {

                    firebaseAuthWithGoogle(googleAccount);
                    loadingIcon.bringToFront();
                    loadingIcon.setVisibility(View.VISIBLE);
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

                            // Get Firebase FCM token and save it to preferences.
                            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(SignUp.this, new OnSuccessListener<InstanceIdResult>() {

                                @Override
                                public void onSuccess(InstanceIdResult instanceIdResult) {

                                    // Sign-in success, update UI with the signed-in user's information
                                    Log.d(TAG, "signInWithCredential:success");

                                    // Save token to sharedPreferences.
                                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SignUp.this);

                                    String token = instanceIdResult.getToken();

                                    SharedPreferences.Editor editor = sharedPreferences.edit()
                                            .putString("googleIdToken", googleAccount.getIdToken())
                                            .putString("FIREBASE_TOKEN", token);
                                    editor.apply();

                                    // Go to Chat.java with the extras.
                                    toastMessageShort("Signed in");
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
                                    loadingIcon.setVisibility(View.GONE);
                                    startActivity(Activity);
                                    finish();
                                }
                            });
                        } else {

                            // If sign in fails, display a recyclerviewlayout to the user.
                            loadingIcon.setVisibility(View.GONE);
                            Log.w(TAG, "firebaseAuthWithGoogle() -> failed: " + task.getException());
                            toastMessageLong("Authentication failed. Try again later.");
                            Crashlytics.logException(new Exception("firebaseAuthWithGoogle() -> failed: " + task.getException()));
                        }
                    }
                });
    }

    private void cancelToasts() {

        // Do not cancel signUpToast, as the activity is always changed right afterward.

        if (shortToast != null) {

            shortToast.cancel();
        }

        if (longToast != null) {

            longToast.cancel();
        }
    }

    private void toastMessageShort(String message) {

        shortToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        shortToast.show();
    }

    private void toastMessageLong(String message) {

        longToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        longToast.show();
    }
}
