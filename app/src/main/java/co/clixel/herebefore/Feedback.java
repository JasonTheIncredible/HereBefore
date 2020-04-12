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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Feedback extends AppCompatActivity {

    private EditText mFeedback;
    private String feedback;
    private Button sendFeedback, goBack;
    private View loadingIcon;
    private boolean theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback);

        mFeedback = findViewById(R.id.feedbackEditText);
        sendFeedback = findViewById(R.id.sendFeedbackButton);
        goBack = findViewById(R.id.goBack);
        loadingIcon = findViewById(R.id.loadingIcon);
    }

    @Override
    protected void onStart() {

        super.onStart();

        // Update to the user's preferences.
        loadPreferences();
        updatePreferences();

        sendFeedback.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                feedback = mFeedback.getText().toString().trim();

                if (feedback.equals("")) {

                    Toast.makeText(getApplication(), "Please enter feedback", Toast.LENGTH_SHORT).show();
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

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

                DatabaseReference Feedback = rootRef.child("feedback");
                Feedback.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        DatabaseReference newFirebaseFeedback = FirebaseDatabase.getInstance().getReference().child("Feedback").push();
                        newFirebaseFeedback.setValue(feedback);
                        mFeedback.getText().clear();
                        RelativeLayout layout = findViewById(R.id.layout);
                        layout.requestFocus();
                        loadingIcon.setVisibility(View.GONE);
                        Toast.makeText(getApplication(), "Thank you :)", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                        loadingIcon.setVisibility(View.GONE);
                        Toast.makeText(Feedback.this, databaseError.getMessage(), Toast.LENGTH_LONG).show();
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

    protected void loadPreferences() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        theme = sharedPreferences.getBoolean(co.clixel.herebefore.Settings.KEY_THEME_SWITCH, false);
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
}
