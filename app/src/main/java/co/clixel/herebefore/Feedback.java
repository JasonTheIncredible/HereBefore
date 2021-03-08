package co.clixel.herebefore;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

public class Feedback extends AppCompatActivity {

    private EditText mFeedback;
    private String feedback;
    private Button sendFeedback, goBack;
    private View loadingIcon, rootView;
    private Toast shortToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Update to the user's preferences.
        updatePreferences();

        setContentView(R.layout.feedback);

        rootView = findViewById(R.id.rootViewFeedback);

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

        mFeedback = findViewById(R.id.feedbackEditText);
        sendFeedback = findViewById(R.id.sendFeedbackButton);
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

        sendFeedback.setOnClickListener(v -> {

            feedback = mFeedback.getText().toString().trim();

            if (feedback.equals("")) {

                showMessageShort("Please enter feedback");
                mFeedback.requestFocus();
                return;
            }

            String input = feedback.trim();

            loadingIcon.bringToFront();
            loadingIcon.setVisibility(View.VISIBLE);

            // Close the keyboard.
            if (Feedback.this.getCurrentFocus() != null) {

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {

                    imm.hideSoftInputFromWindow(Feedback.this.getCurrentFocus().getWindowToken(), 0);
                }
            }

            FeedbackInformation feedbackInformation = new FeedbackInformation();
            feedbackInformation.setFeedback(input);
            feedbackInformation.setDate(ServerValue.TIMESTAMP);
            DatabaseReference newFeedback = FirebaseDatabase.getInstance().getReference().child("Feedback").push();
            newFeedback.setValue(feedbackInformation);
            mFeedback.getText().clear();
            RelativeLayout layout = findViewById(R.id.layout);
            layout.requestFocus();
            loadingIcon.setVisibility(View.GONE);
            showMessageShort("Feedback sent. Thank you!");
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

        if (sendFeedback != null) {

            sendFeedback.setOnClickListener(null);
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
}
