package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class SignIn extends AppCompatActivity {

    private static final String TAG = "SignIn";
    private static final int RC_SIGN_IN = 0;
    private EditText mEmail, mPassword;
    private Button signInButton, resetPasswordButton, goToCreateAccountButton;
    private SignInButton googleSignInButton;
    private String shapeUUID, email, pass;
    private Double polygonArea, circleLatitude, circleLongitude, userLatitude, userLongitude, radius, marker0Latitude, marker0Longitude, marker1Latitude, marker1Longitude, marker2Latitude, marker2Longitude, marker3Latitude, marker3Longitude, marker4Latitude, marker4Longitude, marker5Latitude, marker5Longitude, marker6Latitude, marker6Longitude, marker7Latitude, marker7Longitude;
    private boolean newShape, userIsWithinShape, threeMarkers, fourMarkers, fiveMarkers, sixMarkers, sevenMarkers, eightMarkers;
    private int shapeLat, shapeLon;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private GoogleSignInAccount googleAccount;
    private View loadingIcon;
    private Toast shortToast, longToast;

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
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        goToCreateAccountButton = findViewById(R.id.goToCreateAccountButton);
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

        // Get info from Map.java
        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            newShape = extras.getBoolean("newShape");
            shapeLat = extras.getInt("shapeLat");
            shapeLon = extras.getInt("shapeLon");
            userLatitude = extras.getDouble("userLatitude");
            userLongitude = extras.getDouble("userLongitude");
            shapeUUID = extras.getString("shapeUUID");
            userIsWithinShape = extras.getBoolean("userIsWithinShape");
            // circleLatitude, circleLongitude, and radius will be null if the circle is not new (as a new circle is not being created).
            circleLatitude = extras.getDouble("circleLatitude");
            circleLongitude = extras.getDouble("circleLongitude");
            radius = extras.getDouble("radius");
            // Most of these will be null if the polygon does not have eight markers, or if the polygon is not new.
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
        }
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Give feedback about email and password.
        signInButton.setOnClickListener(view -> {

            Log.i(TAG, "onStart() -> signInButton -> onClick");

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
            if (SignIn.this.getCurrentFocus() != null) {

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {

                    imm.hideSoftInputFromWindow(SignIn.this.getCurrentFocus().getWindowToken(), 0);
                }
            }

            loadingIcon.bringToFront();
            loadingIcon.setVisibility(View.VISIBLE);

            // Check if the account exists in Firebase.
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {

                if (task.isSuccessful()) {

                    Toast signedInToast = Toast.makeText(SignIn.this, "Signed in", Toast.LENGTH_SHORT);
                    signedInToast.show();

                    // Get Firebase FCM token and save it to preferences and Firebase.
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SignIn.this);

                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task1 -> {

                        if (task1.isSuccessful()) {

                            String token = task1.getResult();

                            SharedPreferences.Editor editor = sharedPreferences.edit()
                                    .putString("userToken", email)
                                    .putString("passToken", pass)
                                    .putString("FIREBASE_TOKEN", String.valueOf(token));
                            editor.apply();

                            // Firebase does not allow ".", so replace them with ",".
                            String userEmailFirebase = email.replace(".", ",");

                            FirebaseDatabase.getInstance().getReference().child("Users").child(userEmailFirebase).child("Token").setValue(token);

                            // Go to Chat.java with the extras.
                            Intent Activity = new Intent(SignIn.this, Navigation.class);
                            Activity.putExtra("newShape", newShape);
                            Activity.putExtra("shapeLat", shapeLat);
                            Activity.putExtra("shapeLon", shapeLon);
                            // UserLatitude and userLongitude are used in DirectMentions.
                            Activity.putExtra("userLatitude", userLatitude);
                            Activity.putExtra("userLongitude", userLongitude);
                            Activity.putExtra("shapeUUID", shapeUUID);
                            Activity.putExtra("userIsWithinShape", userIsWithinShape);
                            Activity.putExtra("circleLatitude", circleLatitude);
                            Activity.putExtra("circleLongitude", circleLongitude);
                            Activity.putExtra("radius", radius);
                            Activity.putExtra("polygonArea", polygonArea);
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

                        if (!task1.isSuccessful() && task1.getException() != null) {

                            // Tell the user what happened.
                            loadingIcon.setVisibility(View.GONE);
                            toastMessageLong(task1.getException().getMessage());
                        } else if (!task1.isSuccessful() && task1.getException() == null) {

                            // Tell the user something happened.
                            loadingIcon.setVisibility(View.GONE);
                            toastMessageLong("An unknown error occurred. Please try again.");
                            Log.e(TAG, "onStart() -> signInButton -> OnClick -> FirebaseAuth -> task1.getException == null");
                        }
                    });
                }

                if (!task.isSuccessful() && task.getException() != null) {

                    // Tell the user what happened.
                    loadingIcon.setVisibility(View.GONE);
                    toastMessageLong("User authentication failed: " + task.getException().getMessage());
                } else if (!task.isSuccessful() && task.getException() == null) {

                    // Tell the user something happened.
                    loadingIcon.setVisibility(View.GONE);
                    toastMessageLong("An unknown error occurred. Please try again.");
                    Log.e(TAG, "onStart() -> signInButton -> OnClick -> FirebaseAuth -> task.getException == null");
                }
            });
        });

        // Sign in using Google.
        googleSignInButton.setOnClickListener(view -> {

            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        // Go to ResetPassword.java.
        resetPasswordButton.setOnClickListener(v -> {

            Intent Activity = new Intent(SignIn.this, ResetPassword.class);
            startActivity(Activity);
        });

        // Go to the SignUp.java with the extras.
        goToCreateAccountButton.setOnClickListener(view -> {

            Intent Activity = new Intent(SignIn.this, SignUp.class);
            Activity.putExtra("newShape", newShape);
            Activity.putExtra("shapeLat", shapeLat);
            Activity.putExtra("shapeLon", shapeLon);
            // UserLatitude and userLongitude are used in DirectMentions.
            Activity.putExtra("userLatitude", userLatitude);
            Activity.putExtra("userLongitude", userLongitude);
            Activity.putExtra("shapeUUID", shapeUUID);
            Activity.putExtra("userIsWithinShape", userIsWithinShape);
            Activity.putExtra("circleLatitude", circleLatitude);
            Activity.putExtra("circleLongitude", circleLongitude);
            Activity.putExtra("radius", radius);
            Activity.putExtra("polygonArea", polygonArea);
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
        });
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        loadingIcon.setVisibility(View.GONE);
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
        if (signInButton != null) {

            signInButton.setOnClickListener(null);
        }

        // Remove the listener.
        if (googleSignInButton != null) {

            googleSignInButton.setOnClickListener(null);
        }

        // Remove the listener.
        if (resetPasswordButton != null) {

            resetPasswordButton.setOnClickListener(null);
        }

        // Remove the listener.
        if (goToCreateAccountButton != null) {

            goToCreateAccountButton.setOnClickListener(null);
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
                    toastMessageLong("Sign-in failed. Try again later.");
                }
            } catch (ApiException e) {

                // Google sign in failed, update UI appropriately
                Log.w(TAG, "Google sign-in failed: " + e);
                toastMessageLong("Google sign-in failed: " + e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        Log.d(TAG, "firebaseAuthWithGoogle: " + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        // Save token to sharedPreferences.
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SignIn.this);

                        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task1 -> {

                            if (task1.isSuccessful()) {

                                String token = task1.getResult();

                                SharedPreferences.Editor editor = sharedPreferences.edit()
                                        .putString("googleIdToken", googleAccount.getIdToken())
                                        .putString("FIREBASE_TOKEN", String.valueOf(token));
                                editor.apply();

                                // Firebase does not allow ".", so replace them with ",".
                                String userEmailFirebase = googleAccount.getEmail().replace(".", ",");

                                FirebaseDatabase.getInstance().getReference().child("Users").child(userEmailFirebase).child("Token").setValue(token);

                                // Go to Chat.java with the extras.
                                toastMessageShort("Signed in");
                                Intent Activity = new Intent(SignIn.this, Navigation.class);
                                Activity.putExtra("newShape", newShape);
                                Activity.putExtra("shapeLat", shapeLat);
                                Activity.putExtra("shapeLon", shapeLon);
                                // UserLatitude and userLongitude are used in DirectMentions.
                                Activity.putExtra("userLatitude", userLatitude);
                                Activity.putExtra("userLongitude", userLongitude);
                                Activity.putExtra("shapeUUID", shapeUUID);
                                Activity.putExtra("userIsWithinShape", userIsWithinShape);
                                Activity.putExtra("circleLatitude", circleLatitude);
                                Activity.putExtra("circleLongitude", circleLongitude);
                                Activity.putExtra("radius", radius);
                                Activity.putExtra("polygonArea", polygonArea);
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

                            if (!task1.isSuccessful() && task1.getException() != null) {

                                // Tell the user what happened.
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong(task1.getException().getMessage());
                            } else if (!task1.isSuccessful() && task1.getException() == null) {

                                // Tell the user something happened.
                                loadingIcon.setVisibility(View.GONE);
                                toastMessageLong("An unknown error occurred. Please try again.");
                                Log.e(TAG, "firebaseAuthWithGoogle() -> task.getException == null");
                            }
                        });
                    }

                    if (!task.isSuccessful() && task.getException() != null) {

                        // Tell the user what happened.
                        loadingIcon.setVisibility(View.GONE);
                        toastMessageLong("Google sign-in failed: " + task.getException().getMessage());
                    } else if (!task.isSuccessful() && task.getException() == null) {

                        // Tell the user something happened.
                        loadingIcon.setVisibility(View.GONE);
                        toastMessageLong("An unknown error occurred. Please try again.");
                        Log.e(TAG, "firebaseAuthWithGoogle() -> task.getException == null");
                    }
                });
    }

    private void cancelToasts() {

        // Do not cancel signedInToast, as the activity is always changed right afterward.

        if (shortToast != null) {

            shortToast.cancel();
        }

        if (longToast != null) {

            longToast.cancel();
        }
    }

    private void toastMessageShort(String message) {

        cancelToasts();
        shortToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        shortToast.show();
    }

    private void toastMessageLong(String message) {

        cancelToasts();
        longToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        longToast.show();
    }
}
