package co.clixel.herebefore;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntroFragment;

public class MyAppIntro extends AppIntro {

    private static final String TAG = "AppIntro";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        addSlide(AppIntroFragment.newInstance(
                "Welcome!",
                "Thanks for downloading Here Before!",
                R.drawable.ic_logo_foreground,
                0xFF663399
        ));

        addSlide(AppIntroFragment.newInstance(
                "Overview",
                "Here Before enables users to create permanent location-based chat rooms and anonymously leave messages / pictures / videos in the world for others to find.",
                R.drawable.ic_logo_foreground,
                0xFF5C3394
        ));

        addSlide(AppIntroFragment.newInstance(
                "Creating Chats",
                "Use the seekbar at the bottom of the map to create a circular Chat, or use the menu to the left of the seekbar to create a polygonal, circular, or 'point' (small and focused around your location) Chat - choosing this option will immediately create and enter that shape's chat room.",
                R.drawable.myappintro_creating_chats,
                0xFF52338F
        ));

        addSlide(AppIntroFragment.newInstance(
                "Customizing and Entering Chat",
                "Drag the markers connected to a Chat to customize its size, shape, and location. Tap on a Chat to enter the corresponding chat room.",
                R.drawable.myappintro_customizing_and_entering,
                0xFF47338A
        ));

        addSlide(AppIntroFragment.newInstance(
                "Chat, Direct Messages, and Settings",
                "After entering a Chat, you will see a bar at the bottom with three options. The first one is the chat room. The second shows any Direct Messages (DMs) you have received. The third is the Settings menu.",
                R.drawable.myappintro_chat_dms_settings,
                0xFF3D3385
        ));

        addSlide(AppIntroFragment.newInstance(
                "Messaging Inside vs Outside the Chat",
                "If you entered a Chat while your location was inside the shape on the map, your message will appear on the left side of the chat room. If your location was outside the shape, your message will appear on the right side.",
                R.drawable.myappintro_inside_vs_outside,
                0xFF333380
        ));

        addSlide(AppIntroFragment.newInstance(
                "Report Inappropriate Content",
                "To anonymously report inappropriate content, long press anywhere on the message until the 'Report Post' popup appears. Click the popup and Here Before will be notified.",
                R.drawable.myappintro_report_content,
                0xFF29337A
        ));

        addSlide(AppIntroFragment.newInstance(
                "Sending a Direct Message",
                "Messages left by other users will include 'xxxxx...'. Type '@' followed by 'xxxxx' and select the corresponding option from the popup menu to message that user directly.",
                R.drawable.myappintro_sending_dm,
                0xFF1F3375
        ));

        addSlide(AppIntroFragment.newInstance(
                "Your Direct Messages",
                "Any DMs you receive can by found by clicking on the middle section of the bottom bar. Any new and unread DMs will be highlighted. You can click on a specific DM to immediately go to the Chat where that DM was originally posted.",
                R.drawable.myappintro_your_dms,
                0xFF143370
        ));

        addSlide(AppIntroFragment.newInstance(
                "DMs and Settings from Map",
                "You can access the DMs and Settings menu without entering a Chat by clicking on the corresponding buttons near the top right of the map. If you have any unread DMs, a number will appear over the DMs button.",
                R.drawable.myappintro_dms_settings_map,
                0xFF0A336B
        ));

        addSlide(AppIntroFragment.newInstance(
                "Map Extras",
                "The button near the bottom right of the map screen will take you to a random Chat somewhere in the world. The green button near the top left will change the appearance of the map. The button below the green button will change which Chats the map will display.",
                R.drawable.myappintro_map_extras,
                0xFF003366
        ));
    }

    @Override
    protected void onSkipPressed(Fragment currentFragment) {

        super.onSkipPressed(currentFragment);
        Log.i(TAG, "onSkipPressed()");

        sharedPreferences.edit().putBoolean(SettingsFragment.KEY_SHOW_INTRO, false).apply();

        Intent Activity = new Intent(this, Map.class);
        startActivity(Activity);
        finish();
    }

    @Override
    protected void onDonePressed(Fragment currentFragment) {

        super.onDonePressed(currentFragment);
        Log.i(TAG, "onDonePressed()");

        sharedPreferences.edit().putBoolean(SettingsFragment.KEY_SHOW_INTRO, false).apply();

        Intent Activity = new Intent(this, Map.class);
        startActivity(Activity);
        finish();
    }
}
