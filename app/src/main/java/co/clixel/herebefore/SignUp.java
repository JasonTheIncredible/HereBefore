package co.clixel.herebefore;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignUp extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Button btnCreateAccount;

    //TODO: Go to Chat.java upon completion completion.
    //TODO: Update signup.xml (visuals and add more user information).
    //TODO: Sign in with Google account.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        mEmail = findViewById(R.id.createEmailAddress);
        mPassword = findViewById(R.id.createPassword);
        btnCreateAccount = findViewById(R.id.createAccountButton);
    }

    @Override
    protected void onStart() {

        super.onStart();

        // Give feedback about email and password.
        btnCreateAccount.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                String email = mEmail.getText().toString().toLowerCase();
                String pass = mPassword.getText().toString();

                if (email.equals("") && !pass.equals("")) {

                    toastMessage("Email address required");
                    mEmail.requestFocus();
                    return;
                } if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                    toastMessage("Please enter a valid email address");
                    mEmail.requestFocus();
                    return;
                } if (pass.equals("") && !email.equals("")) {

                    toastMessage("Password required");
                    mPassword.requestFocus();
                    return;
                } if (pass.length() < 6) {

                    toastMessage("Password must be at least 6 characters long");
                    mPassword.requestFocus();
                    return;
                }

                // Try adding the new account to Firebase.
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            toastMessage("Signed up");
                            startActivity(new Intent(SignUp.this, Chat.class));
                        } if (task.getException() != null) {

                            // Tell the user what happened.
                            Toast.makeText(getApplicationContext(), "Account Creation Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();

                            // Send the information to Crashlytics for future debugging. NOTE: This will only be sent after the user restarts the app.
                            Crashlytics.logException(new RuntimeException( task.getException().getMessage() ));
                        } else {

                            // Tell the user something happened.
                            toastMessage("An unknown error occurred. Please try again.");
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onRestart() {

        super.onRestart();
    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onStop() {

        super.onStop();

        // Remove the listener.
        if (btnCreateAccount != null) {

            btnCreateAccount.setOnClickListener(null);
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    private void toastMessage(String message) {

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
