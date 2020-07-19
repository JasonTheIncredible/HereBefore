package co.clixel.herebefore;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class Settings extends AppCompatActivity {

    //public static final String KEY_NOTIFICATIONS_SWITCH = "notifications";
    public static final String KEY_THEME_SWITCH = "toggleTheme";
    public static final String KEY_MAP_TYPE = "mapTypePreference";
    public static final String KEY_SIGN_OUT = "signOut";
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
}