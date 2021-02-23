package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;

public class SignIn extends AppCompatActivity {

    private static final String TAG = "SignIn";
    private static final int RC_SIGN_IN = 0;
    private EditText mEmail, mPassword;
    private Button signInButton, resetPasswordButton, goToCreateAccountButton;
    private SignInButton googleSignInButton;
    private String shapeUUID, email, pass, imageFile, videoFile, lastKnownKey;
    private Double shapeLat, shapeLon;
    private Boolean newShape;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private View loadingIcon, rootView;
    private Toast shortToast, longToast;
    private ArrayList<String> circleUUIDsAL = new ArrayList<>();
    private ArrayList<LatLng> circleCentersAL = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        setContentView(R.layout.signin);

        rootView = findViewById(R.id.rootViewSignIn);

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
            if (!newShape) {
                shapeLat = extras.getDouble("shapeLat");
                shapeLon = extras.getDouble("shapeLon");
            } else {
                imageFile = extras.getString("imageFile");
                videoFile = extras.getString("videoFile");
                //noinspection unchecked
                circleUUIDsAL = (ArrayList<String>) extras.getSerializable("circleUUIDsAL");
                //noinspection unchecked
                circleCentersAL = (ArrayList<LatLng>) extras.getSerializable("circleCentersAL");
                lastKnownKey = extras.getString("lastKnownKey");
            }
            shapeUUID = extras.getString("shapeUUID");
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

                showMessageShort("Email address required");
                mEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                showMessageShort("Please enter a valid email address");
                mEmail.requestFocus();
                return;
            }
            if (pass.equals("") && !email.equals("")) {

                showMessageShort("Password required");
                mPassword.requestFocus();
                return;
            }
            if (pass.length() < 6) {

                showMessageShort("Password must be at least 6 characters long");
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

                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task1 -> {

                        if (task1.isSuccessful()) {

                            String token = task1.getResult();

                            String firebaseUid = FirebaseAuth.getInstance().getUid();

                            FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("Token").setValue(token);

                            // Go to Chat.java with the extras.
                            Intent Activity = new Intent(SignIn.this, Navigation.class);
                            Activity.putExtra("newShape", newShape);
                            if (!newShape) {
                                Activity.putExtra("shapeLat", shapeLat);
                                Activity.putExtra("shapeLon", shapeLon);
                            } else {
                                Activity.putExtra("imageFile", imageFile);
                                Activity.putExtra("videoFile", videoFile);
                                Activity.putExtra("circleUUIDsAL", circleUUIDsAL);
                                Activity.putExtra("circleCentersAL", circleCentersAL);
                                Activity.putExtra("lastKnownKey", lastKnownKey);
                            }
                            Activity.putExtra("shapeUUID", shapeUUID);
                            loadingIcon.setVisibility(View.GONE);
                            startActivity(Activity);
                            finish();
                        }

                        if (!task1.isSuccessful() && task1.getException() != null) {

                            // Tell the user what happened.
                            loadingIcon.setVisibility(View.GONE);
                            showMessageLong(task1.getException().getMessage());
                        } else if (!task1.isSuccessful() && task1.getException() == null) {

                            // Tell the user something happened.
                            loadingIcon.setVisibility(View.GONE);
                            showMessageLong("An unknown error occurred. Please try again.");
                            Log.e(TAG, "onStart() -> signInButton -> OnClick -> FirebaseAuth -> task1.getException == null");
                        }
                    });
                }

                if (!task.isSuccessful() && task.getException() != null) {

                    // Tell the user what happened.
                    loadingIcon.setVisibility(View.GONE);
                    showMessageLong("User authentication failed: " + task.getException().getMessage());
                } else if (!task.isSuccessful() && task.getException() == null) {

                    // Tell the user something happened.
                    loadingIcon.setVisibility(View.GONE);
                    showMessageLong("An unknown error occurred. Please try again.");
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
            if (!newShape) {
                Activity.putExtra("shapeLat", shapeLat);
                Activity.putExtra("shapeLon", shapeLon);
            } else {
                Activity.putExtra("imageFile", imageFile);
                Activity.putExtra("videoFile", videoFile);
                Activity.putExtra("circleUUIDsAL", circleUUIDsAL);
                Activity.putExtra("circleCentersAL", circleCentersAL);
                Activity.putExtra("lastKnownKey", lastKnownKey);
            }
            Activity.putExtra("shapeUUID", shapeUUID);
            loadingIcon.setVisibility(View.GONE);
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
                GoogleSignInAccount googleAccount = task.getResult(ApiException.class);
                if (googleAccount != null) {

                    firebaseAuthWithGoogle(googleAccount);
                    loadingIcon.bringToFront();
                    loadingIcon.setVisibility(View.VISIBLE);
                } else {

                    Log.w(TAG, "onActivityResult() -> account == null");
                    showMessageLong("Sign-in failed. Try again later.");
                }
            } catch (ApiException e) {

                // Google sign in failed, update UI appropriately
                Log.w(TAG, "Google sign-in failed: " + e);
                showMessageLong("Google sign-in failed: " + e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        Log.d(TAG, "firebaseAuthWithGoogle: " + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task1 -> {

                            if (task1.isSuccessful()) {

                                String token = task1.getResult();

                                String firebaseUid = FirebaseAuth.getInstance().getUid();

                                FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("Token").setValue(token);

                                // Go to Chat.java with the extras.
                                Intent Activity = new Intent(SignIn.this, Navigation.class);
                                Activity.putExtra("newShape", newShape);
                                if (!newShape) {
                                    Activity.putExtra("shapeLat", shapeLat);
                                    Activity.putExtra("shapeLon", shapeLon);
                                } else {
                                    Activity.putExtra("imageFile", imageFile);
                                    Activity.putExtra("videoFile", videoFile);
                                    Activity.putExtra("circleUUIDsAL", circleUUIDsAL);
                                    Activity.putExtra("circleCentersAL", circleCentersAL);
                                    Activity.putExtra("lastKnownKey", lastKnownKey);
                                }
                                Activity.putExtra("shapeUUID", shapeUUID);
                                loadingIcon.setVisibility(View.GONE);
                                startActivity(Activity);
                                finish();
                            }

                            if (!task1.isSuccessful() && task1.getException() != null) {

                                // Tell the user what happened.
                                loadingIcon.setVisibility(View.GONE);
                                showMessageLong(task1.getException().getMessage());
                            } else if (!task1.isSuccessful() && task1.getException() == null) {

                                // Tell the user something happened.
                                loadingIcon.setVisibility(View.GONE);
                                showMessageLong("An unknown error occurred. Please try again.");
                                Log.e(TAG, "firebaseAuthWithGoogle() -> task.getException == null");
                            }
                        });
                    }

                    if (!task.isSuccessful() && task.getException() != null) {

                        // Tell the user what happened.
                        loadingIcon.setVisibility(View.GONE);
                        showMessageLong("Google sign-in failed: " + task.getException().getMessage());
                    } else if (!task.isSuccessful() && task.getException() == null) {

                        // Tell the user something happened.
                        loadingIcon.setVisibility(View.GONE);
                        showMessageLong("An unknown error occurred. Please try again.");
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

    private void showMessageShort(String message) {

        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

            Snackbar snackBar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
            View snackBarView = snackBar.getView();
            TextView snackTextView = (TextView) snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
            snackTextView.setMaxLines(10);
            snackBar.show();
        } else {

            cancelToasts();
            shortToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            shortToast.setGravity(Gravity.CENTER, 0, 0);
            shortToast.show();
        }
    }

    private void showMessageLong(String message) {

        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

            Snackbar snackBar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
            View snackBarView = snackBar.getView();
            TextView snackTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
            snackTextView.setMaxLines(10);
            snackBar.show();
        } else {

            cancelToasts();
            longToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
            longToast.setGravity(Gravity.CENTER, 0, 0);
            longToast.show();
        }
    }
}
