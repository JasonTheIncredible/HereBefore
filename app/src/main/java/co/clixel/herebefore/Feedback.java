package co.clixel.herebefore;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

public class Feedback extends AppCompatActivity {

    private EditText mFeedback;
    private String feedback;
    private Button sendFeedback, goBack;
    private View loadingIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        updatePreferences();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.feedback);

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
            showMessageShort("Feedback sent. Thank you!");
            finish();
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

        super.onStop();
    }

    private void showMessageShort(String message) {

        Toast shortToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        shortToast.setGravity(Gravity.CENTER, 0, 0);
        shortToast.show();
    }
}
