package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPassword extends AppCompatActivity {

    private EditText mEmailAddress;
    private String emailAddress;
    private Button sendEmail, goBack;
    private View loadingIcon, rootView;
    private Toast shortToast, longToast;
    private int showInterstitialAdCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        updatePreferences();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.resetpassword);

        rootView = findViewById(R.id.rootViewResetPassword);

        mEmailAddress = findViewById(R.id.emailAddress);
        sendEmail = findViewById(R.id.sendEmailButton);
        goBack = findViewById(R.id.goBack);
        loadingIcon = findViewById(R.id.loadingIcon);
    }

    protected void updatePreferences() {

        // theme == true is light mode.
        boolean theme = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.prefTheme), false);

        if (theme) {

            // Set to light mode.
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO);
        } else {

            // Set to dark mode.
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    @Override
    protected void onStart() {

        super.onStart();

        sendEmail.setOnClickListener(v -> {

            emailAddress = mEmailAddress.getText().toString().trim();

            if (emailAddress.equals("")) {

                showMessageShort("Email address required");
                mEmailAddress.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {

                showMessageShort("Please enter a valid email address");
                mEmailAddress.requestFocus();
                return;
            }

            loadingIcon.bringToFront();
            loadingIcon.setVisibility(View.VISIBLE);

            // Close the keyboard.
            if (ResetPassword.this.getCurrentFocus() != null) {

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {

                    imm.hideSoftInputFromWindow(ResetPassword.this.getCurrentFocus().getWindowToken(), 0);
                }
            }

            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

            firebaseAuth.sendPasswordResetEmail(emailAddress).addOnCompleteListener(task -> {

                showInterstitialAdCounter++;

                if (task.isSuccessful()) {

                    showMessageLong("Password reset instructions sent to your email");
                    loadingIcon.setVisibility(View.GONE);
                }

                // If user tries resetting password multiple times, pay for the bandwidth by showing an ad.
                if (showInterstitialAdCounter >= 20) {

                    showInterstitialAdCounter = 0;

                    Intent Activity = new Intent(this, MyInterstitialAd.class);
                    startActivity(Activity);
                    return;
                }

                if (!task.isSuccessful() && task.getException() != null) {

                    // Tell the user what happened.
                    loadingIcon.setVisibility(View.GONE);
                    showMessageLong("Email not sent: " + task.getException().getMessage());
                } else if (!task.isSuccessful() && task.getException() == null) {

                    // Tell the user something happened.
                    loadingIcon.setVisibility(View.GONE);
                    showMessageLong("An unknown error occurred. Please try again.");
                }
            });

            sendEmail.setEnabled(false);
        });

        goBack.setOnClickListener(v -> onBackPressed());
    }

    @Override
    protected void onRestart() {

        super.onRestart();

        loadingIcon.setVisibility(View.GONE);
    }

    @Override
    protected void onStop() {

        if (sendEmail != null) {

            sendEmail.setOnClickListener(null);
        }

        if (goBack != null) {

            goBack.setOnClickListener(null);
        }

        cancelToasts();

        super.onStop();
    }

    private void cancelToasts() {

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
