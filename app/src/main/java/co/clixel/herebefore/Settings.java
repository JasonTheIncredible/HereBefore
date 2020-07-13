package co.clixel.herebefore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;

public class Settings extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    //public static final String KEY_NOTIFICATIONS_SWITCH = "notifications";
    public static final String KEY_THEME_SWITCH = "toggleTheme";
    public static final String KEY_MAP_TYPE = "mapTypePreference";
    public static final String KEY_SIGN_OUT = "signOut";
    private boolean theme;
    // "FIREBASE_TOKEN" to find Firebase token for messaging.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void onStart() {

        super.onStart();

        // Update to the user's preferences.
        loadPreferences();
        updatePreferences();

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    protected void loadPreferences() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        theme = sharedPreferences.getBoolean(KEY_THEME_SWITCH, false);
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

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        theme = sharedPreferences.getBoolean(KEY_THEME_SWITCH, false);

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
    protected void onStop() {

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

        super.onStop();
    }
}