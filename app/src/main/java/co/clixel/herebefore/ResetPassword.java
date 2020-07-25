package co.clixel.herebefore;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPassword extends AppCompatActivity {

    private EditText mEmailAddress;
    private String emailAddress;
    private Button sendEmail, goBack;
    private View loadingIcon;
    private boolean theme;
    private Toast shortToast, longToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Update to the user's preferences.
        loadPreferences();
        updatePreferences();

        setContentView(R.layout.resetpassword);

        AdView bannerAd = findViewById(R.id.chatBanner);

        // Search I/Ads: in Logcat to find ID and/or W/Ads for other info.
        // List<String> testDeviceIds = Collections.singletonList("814BF63877CBD71E91F9D7241907F4FF");
        RequestConfiguration requestConfiguration
                = new RequestConfiguration.Builder()
                //.setTestDeviceIds(testDeviceIds)
                .build();
        MobileAds.setRequestConfiguration(requestConfiguration);

        AdRequest adRequest = new AdRequest.Builder().build();
        bannerAd.loadAd(adRequest);

        mEmailAddress = findViewById(R.id.emailAddress);
        sendEmail = findViewById(R.id.sendEmailButton);
        goBack = findViewById(R.id.goBack);
        loadingIcon = findViewById(R.id.loadingIcon);
    }

    protected void loadPreferences() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        theme = sharedPreferences.getBoolean(SettingsFragment.KEY_THEME_SWITCH, false);
    }

    protected void updatePreferences() {

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

        sendEmail.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                emailAddress = mEmailAddress.getText().toString().trim();

                if (emailAddress.equals("")) {

                    toastMessageShort("Email address required");
                    mEmailAddress.requestFocus();
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {

                    toastMessageShort("Please enter a valid email address");
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

                firebaseAuth.sendPasswordResetEmail(emailAddress).addOnCompleteListener(new OnCompleteListener<Void>() {

                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {

                            toastMessageLong("Password reset instructions sent to your email");
                            loadingIcon.setVisibility(View.GONE);
                        }

                        if (!task.isSuccessful() && task.getException() != null) {

                            // Tell the user what happened.
                            loadingIcon.setVisibility(View.GONE);
                            toastMessageLong("Email not sent: " + task.getException().getMessage());
                        } else if (!task.isSuccessful() && task.getException() == null) {

                            // Tell the user something happened.
                            loadingIcon.setVisibility(View.GONE);
                            toastMessageLong("An unknown error occurred. Please try again.");
                        }
                    }
                });
            }
        });

        goBack.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                onBackPressed();
            }
        });
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

    private void toastMessageShort(String message) {

        shortToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        shortToast.show();
    }

    private void toastMessageLong(String message) {

        longToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        longToast.show();
    }
}
