package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class DeleteAccount extends AppCompatActivity {

    private EditText mPassword;
    private Button deleteAccount, goBack;
    private View loadingIcon, rootView;
    private boolean googleAccount;
    private SharedPreferences sharedPreferences;
    private String googleIdToken;
    private Toast longToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(DeleteAccount.this);

        updatePreferences();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.deleteaccount);

        rootView = findViewById(R.id.rootViewDeleteAccount);

        mPassword = findViewById(R.id.password);
        deleteAccount = findViewById(R.id.confirmDeleteAccount);
        goBack = findViewById(R.id.goBack);
        loadingIcon = findViewById(R.id.loadingIcon);

        // Add a textView at the top with the user's private email address if they don't have a Google account.
        // Check if the account is a Google account.
        if (GoogleSignIn.getLastSignedInAccount(this) == null) {

            googleAccount = false;

            RelativeLayout relativeLayout = findViewById(R.id.deleteAccountLayout);
            TextView textView = new TextView(this);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {

                return;
            }

            String userEmail = user.getEmail();
            if (userEmail == null) {

                return;
            }

            String firstPartOfEmail = userEmail.substring(0, userEmail.indexOf("@"));
            String secondPartOfEmail = userEmail.substring(userEmail.indexOf("@"));
            // Cut the first part of the email in half and add asterisks for the second part.
            StringBuilder privateEmail = new StringBuilder(firstPartOfEmail.substring(0, firstPartOfEmail.length() / 2));
            if (firstPartOfEmail.length() % 2 == 0) {

                // Number is even.
                for (int i = 0; i < firstPartOfEmail.length() / 2; i++) {

                    privateEmail.append("*");
                }
            } else {

                // Number is odd. Add an extra asterisk.
                for (int i = 0; i < (firstPartOfEmail.length() / 2) + 1; i++) {

                    privateEmail.append("*");
                }
            }
            // Add everything after "@".
            privateEmail.append(secondPartOfEmail);
            textView.setText(getString(R.string.email, privateEmail));
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setTextSize(20);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            textView.setLayoutParams(params);
            relativeLayout.addView(textView);
        } else {

            googleIdToken = Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)).getIdToken();
            googleAccount = true;
            mPassword.setVisibility(View.GONE);
        }
    }

    protected void updatePreferences() {

        // theme == true is light mode.
        boolean theme = sharedPreferences.getBoolean(getString(R.string.prefTheme), false);

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

        deleteAccount.setOnClickListener(v -> {

            // Get auth credentials from the user for re-authentication. The example below shows
            // email and password credentials but there are multiple possible providers,
            // such as GoogleAuthProvider or FacebookAuthProvider.
            AuthCredential credential;

            // Check if the account is a Google account.
            if (googleAccount) {

                if (googleIdToken == null) {

                    showMessageLong("An error occurred. Please try again later.");
                    return;
                }

                credential = GoogleAuthProvider.getCredential(googleIdToken, null);
            } else {

                if (FirebaseAuth.getInstance().getCurrentUser() == null) {

                    return;
                }
                String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                if (userEmail == null) {

                    return;
                }
                String userPassword = mPassword.getText().toString().trim();
                if (userPassword.equals("")) {

                    mPassword.requestFocus();
                    showMessageLong("Please re-enter password for this account");
                    return;
                }
                credential = EmailAuthProvider.getCredential(userEmail, userPassword);
            }

            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {

                loadingIcon.bringToFront();
                loadingIcon.setVisibility(View.VISIBLE);

                // Close the keyboard.
                if (DeleteAccount.this.getCurrentFocus() != null) {

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {

                        imm.hideSoftInputFromWindow(DeleteAccount.this.getCurrentFocus().getWindowToken(), 0);
                    }
                }

                // Need to get Uid before account deletion.
                String firebaseUid = FirebaseAuth.getInstance().getUid();
                if (firebaseUid == null) {

                    return;
                }

                user.reauthenticate(credential)
                        .addOnCompleteListener(task -> {

                            if (task.isSuccessful()) {

                                sharedPreferences.edit().clear().apply();

                                FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).removeValue(

                                        (error, ref) -> {

                                            if (error == null) {

                                                user.delete().addOnCompleteListener(task1 -> {

                                                    if (task1.isSuccessful()) {

                                                        loadingIcon.setVisibility(View.GONE);

                                                        // Sign-out the user.
                                                        AuthUI.getInstance().signOut(DeleteAccount.this);

                                                        Toast accountDeletedToast = Toast.makeText(DeleteAccount.this, "Account Deleted", Toast.LENGTH_LONG);
                                                        accountDeletedToast.show();

                                                        Intent Activity = new Intent(DeleteAccount.this, MyAppIntro.class);

                                                        finishAffinity();

                                                        startActivity(Activity);
                                                    } else if (task1.getException() != null) {

                                                        // Tell the user what happened.
                                                        loadingIcon.setVisibility(View.GONE);
                                                        showMessageLong("Account Deletion Failed: " + task1.getException().getMessage());
                                                    } else {

                                                        // Tell the user something happened.
                                                        loadingIcon.setVisibility(View.GONE);
                                                        showMessageLong("An unknown error occurred. Please try again.");
                                                    }
                                                });
                                            } else {

                                                // Tell the user what happened.
                                                loadingIcon.setVisibility(View.GONE);
                                                showMessageLong("Account Deletion Failed: " + error.getMessage());
                                            }
                                        });
                            } else if (task.getException() != null) {

                                // Tell the user what happened.
                                loadingIcon.setVisibility(View.GONE);
                                showMessageLong("Account Deletion Failed: " + task.getException().getMessage());
                            } else {

                                // Tell the user something happened.
                                loadingIcon.setVisibility(View.GONE);
                                showMessageLong("An unknown error occurred. Please try again.");
                            }
                        });

                deleteAccount.setEnabled(false);
            }
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

        if (deleteAccount != null) {

            deleteAccount.setOnClickListener(null);
        }

        if (goBack != null) {

            goBack.setOnClickListener(null);
        }

        cancelToasts();

        super.onStop();
    }

    private void cancelToasts() {

        // Do not cancel the accountDeletedToast, as the activity is always changed right afterward.

        if (longToast != null) {

            longToast.cancel();
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