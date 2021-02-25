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

public class SignUp extends AppCompatActivity {

    private static final String TAG = "SignUp";
    private static final int RC_SIGN_IN = 0;
    private EditText mEmail, mPassword, mConfirmPassword;
    private Button createAccountButton;
    private SignInButton googleSignInButton;
    private String shapeUUID, email, pass, confirmPass, imageFile, videoFile, lastKnownKey;
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

        setContentView(R.layout.signup);

        rootView = findViewById(R.id.rootViewSignUp);

        mEmail = findViewById(R.id.createEmailAddress);
        mPassword = findViewById(R.id.createPassword);
        mConfirmPassword = findViewById(R.id.confirmPassword);
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
        createAccountButton.setOnClickListener(view -> {

            Log.i(TAG, "onStart() -> createAccountButton -> onClick");

            email = mEmail.getText().toString().toLowerCase().trim();
            pass = mPassword.getText().toString();
            confirmPass = mConfirmPassword.getText().toString();

            if (email.isEmpty()) {

                showMessageShort("Email address required");
                mEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                showMessageShort("Please enter a valid email address");
                mEmail.requestFocus();
                return;
            }
            if (pass.isEmpty()) {

                showMessageShort("Password required");
                mPassword.requestFocus();
                return;
            }
            if (pass.length() < 6) {

                showMessageShort("Password must be at least 6 characters long");
                mPassword.requestFocus();
                return;
            }
            if (!pass.equals(pass.trim())) {

                showMessageShort("Password cannot contain spaces");
                mPassword.requestFocus();
                return;
            }
            if (confirmPass.isEmpty()) {

                showMessageShort("Please enter password again");
                mConfirmPassword.requestFocus();
                return;
            }
            if (!confirmPass.equals(pass)) {

                showMessageShort("Passwords must match");
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
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {

                if (task.isSuccessful()) {

                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task1 -> {

                        if (task1.isSuccessful()) {

                            String token = task1.getResult();

                            String firebaseUid = FirebaseAuth.getInstance().getUid();

                            FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("Token").setValue(token);

                            // Go to Chat.java with the extras.
                            Intent Activity = new Intent(SignUp.this, Navigation.class);
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
                            Log.e(TAG, "onStart() -> createAccountButton -> OnClick -> FirebaseAuth -> task1.getException == null");
                        }
                    });
                }

                if (!task.isSuccessful() && task.getException() != null) {

                    // Tell the user what happened.
                    loadingIcon.setVisibility(View.GONE);
                    showMessageLong("Account creation failed: " + task.getException().getMessage());
                } else if (!task.isSuccessful() && task.getException() == null) {

                    // Tell the user something happened.
                    loadingIcon.setVisibility(View.GONE);
                    showMessageLong("An unknown error occurred. Please try again.");
                    Log.e(TAG, "onStart() -> createAccountButton -> OnClick -> FirebaseAuth -> task.getException == null");
                }
            });
        });

        // Sign in using Google.
        googleSignInButton.setOnClickListener(view -> {

            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
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
                                Intent Activity = new Intent(SignUp.this, Navigation.class);
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

        // Do not cancel signUpToast, as the activity is always changed right afterward.

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
            TextView snackTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
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
