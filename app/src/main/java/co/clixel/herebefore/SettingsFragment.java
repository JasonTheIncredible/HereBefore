package co.clixel.herebefore;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.mikepenz.aboutlibraries.LibsBuilder;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements
        PreferenceManager.OnPreferenceTreeClickListener {
    
    private SwitchPreferenceCompat toggleTheme, toggleNotifications;
    // "FIREBASE_TOKEN" to find Firebase token for messaging.

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Check if the account is a Google account. If not, hide "Reset Password".
        if (GoogleSignIn.getLastSignedInAccount(requireContext()) == null) {

            setPreferencesFromResource(R.xml.preferences_herebefore, rootKey);
        } else {

            setPreferencesFromResource(R.xml.preferences_google, rootKey);
        }

        toggleNotifications = (SwitchPreferenceCompat) findPreference("toggleNotifications");
        toggleTheme = (SwitchPreferenceCompat) findPreference("toggleTheme");

        if (toggleNotifications != null) {

            toggleNotifications.setOnPreferenceClickListener((Preference pref) -> {

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.prefNotifications), toggleNotifications.isChecked());
                editor.apply();

                return true;
            });
        }

        if (toggleTheme != null) {

            toggleTheme.setOnPreferenceClickListener((Preference pref) -> {

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.prefTheme), toggleTheme.isChecked());
                editor.apply();

                if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

                    Snackbar snackBar = Snackbar.make(requireView(), "Theme will change on activity reload.", Snackbar.LENGTH_LONG);
                    snackBar.setAnchorView(getActivity().findViewById(R.id.bottom_navigation_constraint));
                    View snackBarView = snackBar.getView();
                    TextView snackTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
                    snackTextView.setMaxLines(10);
                    snackBar.show();
                } else {

                    Toast longToast = Toast.makeText(getContext(), "Theme will change on activity reload.", Toast.LENGTH_LONG);
                    longToast.setGravity(Gravity.CENTER, 0, 0);
                    longToast.show();
                }

                return true;
            });
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {

        String key = preference.getKey();

        if (getActivity() != null) {

            switch (key) {

                case "introduction": {

                    Intent Activity = new Intent(getActivity(), MyAppIntro.class);

                    startActivity(Activity);

                    break;
                }

                case "resetPassword": {

                    Intent Activity = new Intent(getActivity(), ResetPassword.class);

                    startActivity(Activity);

                    break;
                }

                case "feedback": {

                    Intent Activity = new Intent(getActivity(), Feedback.class);

                    startActivity(Activity);

                    break;
                }

                case "signOut": {

                    AuthUI.getInstance().signOut(getActivity());
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().clear().apply();

                    // Remove the token so user will not get notifications while they are not logged into their account.
                    String firebaseUid = FirebaseAuth.getInstance().getUid();
                    FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("Token").removeValue();

                    Toast signedOutToast = Toast.makeText(getActivity(), "Signed Out", Toast.LENGTH_SHORT);
                    signedOutToast.setGravity(Gravity.CENTER, 0, 250);
                    signedOutToast.show();

                    Intent Activity = new Intent(getActivity(), Map.class);

                    getActivity().finishAffinity();

                    startActivity(Activity);

                    break;
                }

                case "deleteAccount": {

                    Intent Activity = new Intent(getActivity(), DeleteAccount.class);

                    startActivity(Activity);

                    break;
                }

                case "about": {

                    new LibsBuilder()
                            .withAboutVersionShown(true)
                            .withAboutIconShown(true)
                            .withLicenseShown(true)
                            .withVersionShown(true)
                            .start(getActivity());

                    break;
                }
            }
        }

        return true;
    }

    @Override
    public void onDestroy() {

        if (toggleNotifications != null) {

            toggleNotifications.setOnPreferenceClickListener(null);
        }

        if (toggleTheme != null) {

            toggleTheme.setOnPreferenceClickListener(null);
        }

        super.onDestroy();
    }
}
