package co.clixel.herebefore;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

public class Feedback extends AppCompatActivity {

    private EditText mFeedback;
    private String feedback;
    private Button sendFeedback, goBack;
    private View loadingIcon;
    private Toast shortToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Update to the user's preferences.
        updatePreferences();

        setContentView(R.layout.feedback);

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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean theme = sharedPreferences.getBoolean(SettingsFragment.KEY_THEME_SWITCH, false);

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

                toastMessageShort("Please enter feedback");
                mFeedback.requestFocus();
                return;
            }

            loadingIcon.bringToFront();
            loadingIcon.setVisibility(View.VISIBLE);

            // Close the keyboard.
            if (Feedback.this.getCurrentFocus() != null) {

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {

                    imm.hideSoftInputFromWindow(Feedback.this.getCurrentFocus().getWindowToken(), 0);
                }
            }

            DatabaseReference Feedback = FirebaseDatabase.getInstance().getReference().child("Feedback");
            Feedback.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    FeedbackInformation feedbackInformation = new FeedbackInformation();
                    feedbackInformation.setFeedback(feedback);
                    feedbackInformation.setDate(ServerValue.TIMESTAMP);
                    DatabaseReference newFeedback = FirebaseDatabase.getInstance().getReference().child("Feedback").push();
                    newFeedback.setValue(feedbackInformation);
                    mFeedback.getText().clear();
                    RelativeLayout layout = findViewById(R.id.layout);
                    layout.requestFocus();
                    loadingIcon.setVisibility(View.GONE);
                    toastMessageShort("Feedback sent. Thank you!");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                    loadingIcon.setVisibility(View.GONE);
                    toastMessageShort("Feedback sent. Thank you!");
                }
            });
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

    private void toastMessageShort(String message) {

        shortToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        shortToast.show();
    }
}
