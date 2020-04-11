package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class DeleteAccount extends AppCompatActivity {

    private EditText password;
    private Button deleteAccount, goBack;
    private View loadingIcon;
    private boolean theme, googleAccount;
    private SharedPreferences sharedPreferences;
    private String googleIdToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.deleteaccount);

        password = findViewById(R.id.password);
        deleteAccount = findViewById(R.id.confirmDeleteAccount);
        goBack = findViewById(R.id.goBack);
        loadingIcon = findViewById(R.id.loadingIcon);

        // Add a textView at the top with the user's private email address if they don't have a Google account.

        // Check if the account is a Google account.
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(DeleteAccount.this);
        googleIdToken = sharedPreferences.getString("googleIdToken", "");
        googleAccount = !googleIdToken.equals("");
        if (!googleAccount) {

            RelativeLayout relativeLayout = findViewById(R.id.deleteAccountLayout);
            TextView textView = new TextView(this);
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(DeleteAccount.this);
            String userToken = sharedPreferences.getString("userToken", "");
            // Only show part of the email for privacy.
            // Divide the email address into two parts.
            String firstPartOfEmail = userToken.substring(0, userToken.indexOf("@"));
            String secondPartOfEmail = userToken.substring(userToken.indexOf("@"));
            // Cut the first part of the email in half and add asterisks for the second part.
            StringBuilder privateEmail = new StringBuilder(firstPartOfEmail.substring(0, firstPartOfEmail.length() / 2));
            if (firstPartOfEmail.length() % 2 == 0) {

                // Number is even.
                for (int i = 0; i < firstPartOfEmail.length() / 2; i++) {

                    privateEmail.append("*");
                }

                // Add everything after "@".
                privateEmail.append(secondPartOfEmail);
            } else {

                // Number is odd. Add an extra asterisk.
                for (int i = 0; i < (firstPartOfEmail.length() / 2) + 1; i++) {

                    privateEmail.append("*");
                }

                // Add everything after "@".
                privateEmail.append(secondPartOfEmail);
            }
            textView.setText(getResources().getString(R.string.email) + privateEmail);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setTextSize(20);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            textView.setLayoutParams(params);
            relativeLayout.addView(textView);
        } else {

            password.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {

        super.onStart();

        // Update to the user's preferences.
        loadPreferences();
        updatePreferences();

        deleteAccount.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Get auth credentials from the user for re-authentication. The example below shows
                // email and password credentials but there are multiple possible providers,
                // such as GoogleAuthProvider or FacebookAuthProvider.
                AuthCredential credential = null;

                String userToken = sharedPreferences.getString("userToken", "");
                String passToken = sharedPreferences.getString("passToken", "");

                // Check if the account is a Google account.
                if (!googleIdToken.equals("")) {

                    credential = GoogleAuthProvider.getCredential(googleIdToken, null);
                } else {

                    credential = EmailAuthProvider.getCredential(userToken, passToken);
                }

                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user != null) {

                    // Sign out the user without asking for a password, as the user has a Google account.
                    if (googleAccount) {

                        loadingIcon.bringToFront();
                        loadingIcon.setVisibility(View.VISIBLE);

                        // Close the keyboard.
                        if (DeleteAccount.this.getCurrentFocus() != null) {

                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {

                                imm.hideSoftInputFromWindow(DeleteAccount.this.getCurrentFocus().getWindowToken(), 0);
                            }
                        }

                        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {

                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {

                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful()) {

                                            // Sign-out the user.
                                            AuthUI.getInstance().signOut(DeleteAccount.this);

                                            PreferenceManager.getDefaultSharedPreferences(DeleteAccount.this).edit().clear().commit();

                                            Toast.makeText(DeleteAccount.this, "Account deleted", Toast.LENGTH_LONG).show();

                                            Intent Activity = new Intent(DeleteAccount.this, Map.class);

                                            loadingIcon.setVisibility(View.GONE);

                                            startActivity(Activity);

                                            finishAffinity();
                                        }

                                        if (!task.isSuccessful() && task.getException() != null) {

                                            // Tell the user what happened.
                                            loadingIcon.setVisibility(View.GONE);
                                            Toast.makeText(DeleteAccount.this, "Account Deletion Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        } else if (!task.isSuccessful() && task.getException() == null) {

                                            // Tell the user something happened.
                                            loadingIcon.setVisibility(View.GONE);
                                            Toast.makeText(DeleteAccount.this, "An unknown error occurred. Please try again.", Toast.LENGTH_LONG).show();

                                            // Send the information to Crashlytics for future debugging.
                                            Crashlytics.logException(new RuntimeException("onStart() -> deleteAccount -> OnClick -> task.getException == null"));
                                        }
                                    }
                                });
                            }
                        });
                    }
                    // Sign out the user by asking for the password, as the user has a Firebase account.
                    else if (password.getText().toString().equals(passToken)) {

                        loadingIcon.bringToFront();
                        loadingIcon.setVisibility(View.VISIBLE);

                        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {

                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {

                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful()) {

                                            // Sign-out the user.
                                            AuthUI.getInstance().signOut(DeleteAccount.this);

                                            PreferenceManager.getDefaultSharedPreferences(DeleteAccount.this).edit().clear().commit();

                                            Toast.makeText(DeleteAccount.this, "Account deleted", Toast.LENGTH_LONG).show();

                                            Intent Activity = new Intent(DeleteAccount.this, Map.class);

                                            loadingIcon.setVisibility(View.GONE);

                                            startActivity(Activity);

                                            finish();
                                        }

                                        if (!task.isSuccessful() && task.getException() != null) {

                                            // Tell the user what happened.
                                            loadingIcon.setVisibility(View.GONE);
                                            Toast.makeText(DeleteAccount.this, "Account Deletion Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        } else if (!task.isSuccessful() && task.getException() == null) {

                                            // Tell the user something happened.
                                            loadingIcon.setVisibility(View.GONE);
                                            Toast.makeText(DeleteAccount.this, "An unknown error occurred. Please try again.", Toast.LENGTH_LONG).show();

                                            // Send the information to Crashlytics for future debugging.
                                            Crashlytics.logException(new RuntimeException("onStart() -> deleteAccount -> OnClick -> task.getException == null"));
                                        }
                                    }
                                });
                            }
                        });
                    } else if (password.getText().toString().equals("")) {

                        password.requestFocus();
                        Toast.makeText(DeleteAccount.this, "Please re-enter password for this account", Toast.LENGTH_SHORT).show();
                    } else {

                        password.getText().clear();
                        Toast.makeText(DeleteAccount.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                    }
                }
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

        if (deleteAccount != null) {

            deleteAccount.setOnClickListener(null);
        }

        if (goBack != null) {

            goBack.setOnClickListener(null);
        }

        super.onStop();
    }
}