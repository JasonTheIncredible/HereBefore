package co.clixel.herebefore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;

public class Settings extends AppCompatActivity {

    public static final String KEY_NOTIFICATIONS_SWITCH = "notifications";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Boolean notificationsSwitch = sharedPreferences.getBoolean(Settings.KEY_NOTIFICATIONS_SWITCH, false);
    }
}
