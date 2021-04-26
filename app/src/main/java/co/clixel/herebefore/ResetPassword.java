package co.clixel.herebefore;

import android.os.Bundle;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;

public class ResetPassword extends AppCompatActivity {

    private EditText mEmailAddress;
    private String emailAddress;
    private Button sendEmail, goBack;
    private View loadingIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        updatePreferences();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.resetpassword);

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

            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            firebaseAuth.sendPasswordResetEmail(emailAddress).addOnCompleteListener(task -> {

                if (task.isSuccessful()) {

                    showMessageLong("Password reset instructions sent to your email");
                } else if (!task.isSuccessful() && task.getException() != null) {

                    // Tell the user what happened.
                    showMessageLong("Error: " + task.getException().getMessage());
                } else if (!task.isSuccessful() && task.getException() == null) {

                    // Tell the user something happened.
                    showMessageLong("An unknown error occurred. Please try again later.");
                }

                finish();
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

        if (sendEmail != null) {

            sendEmail.setOnClickListener(null);
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

    private void showMessageLong(String message) {

        Toast longToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        longToast.setGravity(Gravity.CENTER, 0, 0);
        longToast.show();
    }
}
