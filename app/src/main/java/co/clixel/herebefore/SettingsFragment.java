package co.clixel.herebefore;

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

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {

        String key = preference.getKey();

        if (key.equals("loggedIn")) {

            if (getActivity() != null) {

                boolean loggedIn = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("loggedIn", false);

                if (loggedIn) {

                    AuthUI.getInstance().signOut(getActivity());
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().clear().apply();
                    Toast.makeText(getActivity(), "Signed out", Toast.LENGTH_SHORT).show();
                } else {

                    Toast.makeText(getActivity(), "No user signed in", Toast.LENGTH_SHORT).show();
                }
            }
        }

        return true;
    }
}
