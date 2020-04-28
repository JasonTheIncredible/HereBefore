package co.clixel.herebefore;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

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

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        if (getActivity() != null) {

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

                    startActivity(Activity);

                    getActivity().finishAffinity();

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
                            .withAboutDescription("This is a small sample which can be set in the about my app description file.<br /><b>You can style this with html markup :D</b>")
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
