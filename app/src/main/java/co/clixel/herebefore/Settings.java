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

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        boolean theme = sharedPreferences.getBoolean("toggleTheme", false);

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
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onStart() {

        super.onStart();

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

        super.onStop();
    }
}
