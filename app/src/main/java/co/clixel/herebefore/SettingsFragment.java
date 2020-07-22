package co.clixel.herebefore;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.firebase.ui.auth.AuthUI;
import com.mikepenz.aboutlibraries.LibsBuilder;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements
        PreferenceManager.OnPreferenceTreeClickListener {

    private DialogInterface.OnClickListener dialogClickListener;
    public static boolean themeToggled = false;
    public static final String KEY_NOTIFICATIONS_SWITCH = "notifications";
    public static final String KEY_THEME_SWITCH = "toggleTheme";
    public static final String KEY_MAP_TYPE = "mapTypePreference";
    public static final String KEY_SIGN_OUT = "signOut";
    // "FIREBASE_TOKEN" to find Firebase token for messaging.

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        if (getActivity() != null) {

            // userLatitude and userLongitude are used in the DM fragment. Do not get rid of them!
            Bundle extras = getActivity().getIntent().getExtras();
            if (extras != null) {

                Double userLatitude = extras.getDouble("userLatitude");
                Double userLongitude = extras.getDouble("userLongitude");
            }

            // Check if the account is a Google account. If not, hide "Reset Password".
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String googleIdToken = sharedPreferences.getString("googleIdToken", "");
            boolean googleAccount = !googleIdToken.equals("");
            if (!googleAccount) {

                setPreferencesFromResource(R.xml.preferences_herebefore, rootKey);
            } else {

                setPreferencesFromResource(R.xml.preferences_google, rootKey);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {

        String key = preference.getKey();

        if (getActivity() != null) {

            switch (key) {

                case "toggleTheme": {

                    if (getContext() != null) {

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

                        boolean theme = sharedPreferences.getBoolean(KEY_THEME_SWITCH, false);

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

                    // Used in Navigation.
                    themeToggled = true;

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

                    Toast.makeText(getActivity(), "Signed out", Toast.LENGTH_SHORT).show();

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
    public void onStop() {

        if (dialogClickListener != null) {

            dialogClickListener = null;
        }

        super.onStop();
    }
}
