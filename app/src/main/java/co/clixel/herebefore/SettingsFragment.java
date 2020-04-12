package co.clixel.herebefore;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.firebase.ui.auth.AuthUI;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements
        PreferenceManager.OnPreferenceTreeClickListener {

    private DialogInterface.OnClickListener dialogClickListener;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preferences, rootKey);
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

                case "signOut":

                    boolean signOut = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Settings.KEY_SIGN_OUT, false);

                    if (signOut) {

                        AuthUI.getInstance().signOut(getActivity());
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().clear().commit();

                        Toast.makeText(getActivity(), "Signed out", Toast.LENGTH_SHORT).show();

                        Intent Activity = new Intent(getActivity(), Map.class);

                        startActivity(Activity);

                        getActivity().finishAffinity();
                    } else {

                        Toast.makeText(getActivity(), "No user signed in", Toast.LENGTH_SHORT).show();
                    }

                    break;

                case "deleteAccount": {

                    Intent Activity = new Intent(getActivity(), DeleteAccount.class);

                    startActivity(Activity);

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
